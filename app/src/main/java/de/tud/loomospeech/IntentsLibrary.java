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

    private void Speak (String msg, String utteranceId) {
        activity.loomoTextToSpeech.speak(msg, utteranceId);
        activity.mHandler.sendMessage(activity.mHandler.obtainMessage(MessageHandler.INFO, MessageHandler.APPEND, MessageHandler.OUTPUT, msg));
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
                            volume = Math.max(Math.min(Integer.parseInt(value), 100), 0);
                            volume = Math.round(volume * maxVolume / 100);
                    }
                }
            } catch (NumberFormatException e) {
                Log.d(TAG, "NaN", e);
            } catch (Exception e) {
                Log.d(TAG, "Exception: ", e);
            }
            Log.d(TAG, "OnDeviceSetVolume" + entities.toString());

            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);

            String msg = "Okay, the volume is set to " + value;
            Speak(msg, "OnDeviceSetVolume");
        } else {
            //dialog
            Speak("What to?", "DialogOnDeviceSetVolume");

            //start recognition without intent detection
            activity.azureSpeechRecognition.getRecognitionClient().startMicAndRecognition();
        }
    }
}
