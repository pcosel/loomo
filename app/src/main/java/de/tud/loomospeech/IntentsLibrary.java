package de.tud.loomospeech;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.WindowManager;

import org.json.JSONArray;
import org.json.JSONObject;


class IntentsLibrary {
    public boolean dialogStarted;
    public String dialogContext;

    private static final String TAG = "IntentsLibrary";
    private MainActivity activity;

    IntentsLibrary(MainActivity myActivity) {
        activity = myActivity;
    }

    public void callByName(String functionName, JSONArray entities) {
        //Ignoring any possible result
        functionName = functionName.replace(".", "");
        /* try {
            this.getClass().getDeclaredMethod(functionName, JSONArray.class).invoke(this, entities);
        } catch (Exception e) {
//          Log.d(TAG, e.getMessage());
            Log.d(TAG, "Exception: ", e);
            this.None();
        } */

        try {
            this.getClass().getDeclaredMethod(functionName, JSONArray.class).invoke(this, entities);
        } catch (Exception e) {
            try {
                this.getClass().getDeclaredMethod(functionName).invoke(this);
            } catch (Exception e2) {
                Log.d(TAG, "Exception: ", e2);
                this.None();
            }
        }
    }

    public void callByName(String functionName, String entity) {
        //Ignoring any possible result
        functionName = functionName.replace(".", "");
        try {
            this.getClass().getDeclaredMethod(functionName, String.class).invoke(this, entity);
        } catch (Exception e) {
            Log.d(TAG, "Exception: ", e);
            this.None();
        }
    }

    private void Speak (String msg, String utteranceId) {
        if (activity.ttsIsReady) activity.tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        activity.mHandler.sendMessage(activity.mHandler.obtainMessage(MessageHandler.INFO, MessageHandler.APPEND, MessageHandler.OUTPUT, msg));
    }

    private static boolean isNumeric(String str)
    {
        try
        {
            double d = Double.parseDouble(str);
        }
        catch(NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }

    private void None() {
        // No suitable action for command found.
        Log.d(TAG, "No suitable action for command found.");
        Speak("Pardon? I didn't understand that.", "None");
    }

    private void OnDeviceAreYouListening() {
        Speak("Yes!", "OnDeviceAreYouListening");
    }

    private void OnDeviceSetBrightness (JSONArray entities) {
        int brightness = activity.brightness;
        String value = "";
        try {
            JSONObject entity = entities.getJSONObject(0);
            if(entity.get("type").toString().equals("OnDevice.BrightnessLevel")) {
                value = entity.get("entity").toString();
                switch(value) {
                    case "low":
                        brightness = 0;
                        break;
                    case "medium":
                        brightness = 127;
                        break;
                    case "high":
                        brightness = 255;
                        break;
                    default:
                        int result = Math.max(Math.min(Integer.parseInt(value), 100), 0);
                        brightness = (int) (result * 2.55);
                }
                //brightness = entity.getInt("entity");
            }
        }
        catch (NumberFormatException e) {
            Log.d(TAG, "NaN", e);
        }
        catch (Exception e) {
            Log.d(TAG, "Exception: ", e);
        }
        Log.d(TAG, "OnDeviceSetBrightness" + entities.toString());

        //Set the system brightness using the brightness variable value
        Settings.System.putInt(activity.cResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
        //Get the current window attributes
        WindowManager.LayoutParams layoutpars = activity.window.getAttributes();
        //Set the brightness of this window
        layoutpars.screenBrightness = brightness / (float)255;
        //Apply attribute changes to this window
        activity.window.setAttributes(layoutpars);

        String msg = "Okay, the brightness is set to " + value;
        Speak(msg, "OnDeviceSetBrightness");
    }

    private void OnDeviceSetVolume (JSONArray entities) {
        if (entities.length() > 0) {
            AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);

            String value = "";
            int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

            try {
                JSONObject entity = entities.getJSONObject(0);
                if (entity.get("type").toString().equals("OnDevice.Volume")) {
                    value = entity.get("entity").toString();
                    if(!isNumeric(value)) {
                        switch (value) {
                            case "muted":
                                volume = 0;
                                break;
                            case "low":
                                volume = 1;
                                break;
                            case "medium":
                                volume = Math.round(maxVolume / 2);
                                break;
                            case "high":
                                volume = maxVolume;
                                break;
                            default:
                                String msg = "I can't set the volume to " + value;
                                Speak(msg, "OnDeviceSetVolume");

                                activity.startWakeUpListener();
                                return;
                        }

                        Log.d(TAG, "OnDeviceSetVolume" + entities.toString());

                        //set volume
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);

                        String msg = "Okay, the volume is set to " + value;
                        Speak(msg, "OnDeviceSetVolume");

                        activity.startWakeUpListener();
                    } else {
                        volume = Math.max(Math.min(Integer.parseInt(value), 100), 0);
                        volume = Math.round(volume * maxVolume / 100);

                        Log.d(TAG, "OnDeviceSetVolume" + entities.toString());

                        //set volume
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);

                        String msg = "Okay, the volume is set to " + value + " percent";
                        Speak(msg, "OnDeviceSetVolume");

                        activity.startWakeUpListener();
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception: ", e);
            }

        } else {
            //start dialog
            dialogContext = "DialogOnDeviceSetVolume";
            dialogStarted = true;
            Speak("What to?", dialogContext);

            //start recognition without intent detection
            activity.azureSpeechRecognition.getRecognitionClient().startMicAndRecognition();
        }
    }

    private void DialogOnDeviceSetVolume (String entity) {

        if(entity != null) {

            AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);

            int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

            try {
                if(!isNumeric(entity)) {
                    switch (entity) {
                        case "muted":
                            volume = 0;
                            break;
                        case "low":
                            volume = 1;
                            break;
                        case "medium":
                            volume = Math.round(maxVolume / 2);
                            break;
                        case "high":
                            volume = maxVolume;
                            break;
                        default:
                            String msg = "I can't set the volume to " + entity;
                            Speak(msg, "OnDeviceSetVolume");

                            activity.startWakeUpListener();
                            return;
                    }

                    Log.d(TAG, "DialogOnDeviceSetVolume" + entity);

                    //set volume
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);

                    String msg = "Okay, the volume is set to " + entity;
                    Speak(msg, "DialogOnDeviceSetVolume");

                    activity.startWakeUpListener();
                } else {
                    volume = Math.max(Math.min(Integer.parseInt(entity), 100), 0);
                    volume = Math.round(volume * maxVolume / 100);

                    Log.d(TAG, "DialogOnDeviceSetVolume" + entity);

                    //set volume
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);

                    String msg = "Okay, the volume is set to " + entity + " percent";
                    Speak(msg, "DialogOnDeviceSetVolume");

                    activity.startWakeUpListener();
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception: ", e);
            }
        }

    }
         /* if (entity != null) {
            AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);

            int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

            try {
                if (!isNumeric(entity)) {
                    switch (entity) {
                        case "muted":
                            volume = 0;
                            break;
                        case "low":
                            volume = 1;
                            break;
                        case "medium":
                            volume = Math.round(maxVolume / 2);
                            break;
                        case "high":
                            volume = maxVolume;
                            break;
                        default:
                            String msg = "I can't set the volume to " + entity;
                            Speak(msg, "OnDeviceSetVolume");

                            activity.startWakeUpListener();
                            return;
                    }

                    Log.d(TAG, "OnDeviceSetVolume" + entities.toString());

                    //set volume
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);

                    String msg = "Okay, the volume is set to " + entity;
                    Speak(msg, "OnDeviceSetVolume");

                    activity.startWakeUpListener();
                    //start recognition without intent detection
                    activity.azureSpeechRecognition.getRecognitionClient().startMicAndRecognition();

                Log.d(TAG, "DialogOnDeviceSetVolume" + entity);

                dialogStarted = false;
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);

                String msg = "Okay, the volume is set to " + entity;
                Speak(msg, "OnDeviceSetVolume");

            } else {}
            } catch(Exception e){
            Log.d(TAG, "Exception: ", e);
        }
    } */
}
