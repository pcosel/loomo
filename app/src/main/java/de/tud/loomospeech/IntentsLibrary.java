package de.tud.loomospeech;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;


class IntentsLibrary {
    public boolean dialogStarted;
    public String dialogContext;

    private static final String TAG = "IntentsLibrary";
    private MainActivity activity;
    private WordToNumber wordToNumber;

    IntentsLibrary(MainActivity myActivity) {
        activity = myActivity;
        wordToNumber = new WordToNumber();
    }

    public void callByName(String functionName, JSONArray entities) {
        //Ignoring any possible result
        functionName = functionName.replace(".", "");

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

    private void Speak (String msg, String utteranceId, Runnable callback) {
        activity.loomoTextToSpeech.speak(msg, utteranceId, callback);
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

    private String weekDayToString (int day) {
        switch(day) {
            case Time.MONDAY:
                return "Monday";
            case Time.TUESDAY:
                return "Tuesday";
            case Time.THURSDAY:
                return "Thursday";
            case Time.FRIDAY:
                return "Friday";
            case Time.SATURDAY:
                return "Saturday";
            case Time.SUNDAY:
                return "Sunday";
        }
        return "";
    }

    private String daySuffix(int day) {
        if (day >= 11 && day <= 13) {
            return day + "th";
        }
        switch (day % 10) {
            case 1:  return day + "st";
            case 2:  return day + "nd";
            case 3:  return day + "rd";
            default: return day + "th";
        }
    }

    /*---------- Intents ----------*/

    private void None() {
        // No suitable action for command found.
        Log.d(TAG, "No suitable action for command found.");
        Speak("Pardon? I didn't understand that.", "None", null);
        activity.loomoRecognizer.startWakeUpListener();
    }

    private void OnDeviceCloseApplication() {
        Speak("Dont you love me anymore?", "UtilitiesStop", new Runnable() {
            @Override
            public void run() {
                activity.loomoTextToSpeech.shutdown();
                activity.finish();
                System.exit(0);
            }
        });
    }

    private void OnDeviceAreYouListening() {
        Speak("Yes! I can hear you!", "OnDeviceAreYouListening", null);

        activity.loomoRecognizer.startWakeUpListener();
    }

    private void OnDeviceTime () {
        Date now = new Date();
        now.getTime();

        String msg = "";

        if(now.getMinutes() == 0) {
            msg = "It's " + now.getHours() + " o'clock.";
        } else {
            int random = (int) (Math.random() * 2);

            switch (random) {
                case 0:
                    msg = "It's " + now.getHours() + ":" + now.getMinutes() + ".";
                    break;
                case 1:
                    msg = "It's " + now.getMinutes() + " past " + now.getHours() + ".";
                    break;
            }
        }

        Speak(msg, "OnDeviceTime", new Runnable() {
            @Override
            public void run() {
                activity.loomoRecognizer.startWakeUpListener();
            }
        });
    }

    private void OnDeviceDate () {
        Date now = new Date();
        now.getTime();

        String weekDay = (String) DateFormat.format("EEEE", now);
        String day = daySuffix(now.getDate());
        String month = (String) DateFormat.format("MMMM", now);
        String year = (String) DateFormat.format("yyyy", now);

        String msg = "Today is " + weekDay + " the " + day + " of " + month + " " + year + ".";
        Speak(msg, "OnDeviceTime", new Runnable() {
            @Override
            public void run() {
                activity.loomoRecognizer.startWakeUpListener();
            }
        });
    }

    private void OnDeviceSetBrightness(JSONArray entities) {
        int brightness;
        ContentResolver cResolver = activity.getContentResolver();
        Window window = activity.getWindow();

        if (entities.length() > 0) {
            try {
                // To handle the auto
                Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                brightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS);
            } catch (Settings.SettingNotFoundException e) {
                //Throw an error case it couldn't be retrieved
                Log.e(TAG, "Error: Cannot access system brightness");
                e.printStackTrace();
            }

            String value = "";
            try {
                JSONObject entity = entities.getJSONObject(0);
                if (entity.get("type").toString().equals("OnDevice.BrightnessLevel")) {
                    value = entity.get("entity").toString();
                    if(!isNumeric(value)) {
                        switch (value) {
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
                                String msg = "I can't set the brightness to " + value + ".";
                                Speak(msg, "OnDeviceSetBrightness", null);

                                activity.loomoRecognizer.startWakeUpListener();
                                return;
                        }

                        Log.d(TAG, "OnDeviceSetBrightness" + entities.toString());

                        //Set the system brightness using the brightness variable value
                        Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
                        //Get the current window attributes
                        WindowManager.LayoutParams layoutpars = window.getAttributes();
                        //Set the brightness of this window
                        layoutpars.screenBrightness = brightness / (float)255;
                        //Apply attribute changes to this window
                        window.setAttributes(layoutpars);

                        String msg = "Okay, the brightness is set to " + value + ".";
                        Speak(msg, "OnDeviceSetBrightness", null);

                        activity.loomoRecognizer.startWakeUpListener();
                    } else {
                        brightness = Math.max(Math.min(Integer.parseInt(value), 100), 0);

                        Log.d(TAG, "OnDeviceSetBrightness" + entities.toString());

                        //Set the system brightness using the brightness variable value
                        Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
                        //Get the current window attributes
                        WindowManager.LayoutParams layoutpars = window.getAttributes();
                        //Set the brightness of this window
                        layoutpars.screenBrightness = brightness / (float)255;
                        //Apply attribute changes to this window
                        window.setAttributes(layoutpars);

                        String msg = "Okay, the brightness is set to " + value + " percent.";
                        Speak(msg, "OnDeviceSetBrightness", null);

                        activity.loomoRecognizer.startWakeUpListener();
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception: ", e);
            }

        } else {
            //start dialog
            dialogContext = "DialogOnDeviceSetBrightness";
            dialogStarted = true;
            Speak("What do you want to set the brightness to?", dialogContext, new Runnable() {
                @Override
                public void run() {
                    //start recognition without intent detection
                    activity.azureSpeechRecognition.startMicAndRecognition();
                }
            });
        }
    }

    private void DialogOnDeviceSetBrightness (String entity) {
        int brightness;
        ContentResolver cResolver = activity.getContentResolver();
        Window window = activity.getWindow();

        try {
            // To handle the auto
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            brightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            //Throw an error case it couldn't be retrieved
            Log.e(TAG, "Error: Cannot access system brightness");
            e.printStackTrace();
        }

        if(entity != null) {
            if(entity.contains("percent")) {
                entity = entity.replace("precent", "");
            }

            try {
                Integer number = wordToNumber.wordToNumber(entity);
                if(number == null) {
                    switch (entity) {
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
                            String msg = "I can't set the brightness to " + entity + ".";
                            Speak(msg, "OnDeviceSetBrightness", null);

                            dialogStarted = false;
                            activity.loomoRecognizer.startWakeUpListener();
                            return;
                    }

                    Log.d(TAG, "DialogOnDeviceSetBrighntess" + entity);

                    //Set the system brightness using the brightness variable value
                    Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
                    //Get the current window attributes
                    WindowManager.LayoutParams layoutpars = window.getAttributes();
                    //Set the brightness of this window
                    layoutpars.screenBrightness = brightness / (float)255;
                    //Apply attribute changes to this window
                    window.setAttributes(layoutpars);

                    String msg = "Okay, the brightness is set to " + entity + ".";
                    Speak(msg, "DialogOnDeviceSetBrightness", null);

                    dialogStarted = false;
                    activity.loomoRecognizer.startWakeUpListener();
                } else {
                    brightness = Math.max(Math.min(number, 100), 0);

                    Log.d(TAG, "DialogOnDeviceSetBrightness" + entity);

                    //Set the system brightness using the brightness variable value
                    Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
                    //Get the current window attributes
                    WindowManager.LayoutParams layoutpars = window.getAttributes();
                    //Set the brightness of this window
                    layoutpars.screenBrightness = brightness / (float)255;
                    //Apply attribute changes to this window
                    window.setAttributes(layoutpars);

                    String msg = "Okay, the brightness is set to " + entity + " percent.";
                    Speak(msg, "DialogOnDeviceSetBrightness", null);

                    dialogStarted = false;
                    activity.loomoRecognizer.startWakeUpListener();
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception: ", e);
            }
        }
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
                                Speak(msg, "OnDeviceSetVolume", null);

                                activity.loomoRecognizer.startWakeUpListener();
                                return;
                        }

                        Log.d(TAG, "OnDeviceSetVolume" + entities.toString());

                        //set volume
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);

                        String msg = "Okay, the volume is set to " + value;
                        Speak(msg, "OnDeviceSetVolume", null);

                        activity.loomoRecognizer.startWakeUpListener();
                    } else {
                        volume = Math.max(Math.min(Integer.parseInt(value), 100), 0);
                        volume = Math.round(volume * maxVolume / 100);

                        Log.d(TAG, "OnDeviceSetVolume" + entities.toString());

                        //set volume
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);

                        String msg = "Okay, the volume is set to " + value + " percent";
                        Speak(msg, "OnDeviceSetVolume", null);

                        activity.loomoRecognizer.startWakeUpListener();
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception: ", e);
            }

        } else {
            //start dialog
            dialogContext = "DialogOnDeviceSetVolume";
            dialogStarted = true;
            Speak("What to?", dialogContext, new Runnable() {
                @Override
                public void run() {
                    //start recognition without intent detection
                    activity.azureSpeechRecognition.startMicAndRecognition();
                }
            });
        }
    }

    private void DialogOnDeviceSetVolume (String entity) {

        if(entity != null) {
            AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);

            int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

            if(entity.contains("percent")) {
                entity = entity.replace("precent", "");
            }

            try {
                Integer number = wordToNumber.wordToNumber(entity);
                if(number == null) {
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
                            Speak(msg, "OnDeviceSetVolume", null);

                            dialogStarted = false;
                            activity.loomoRecognizer.startWakeUpListener();
                            return;
                    }

                    Log.d(TAG, "DialogOnDeviceSetVolume" + entity);

                    //set volume
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);

                    String msg = "Okay, the volume is set to " + entity;
                    Speak(msg, "DialogOnDeviceSetVolume", null);

                    dialogStarted = false;
                    activity.loomoRecognizer.startWakeUpListener();
                } else {
                    volume = Math.max(Math.min(number, 100), 0);
                    volume = Math.round(volume * maxVolume / 100);

                    Log.d(TAG, "DialogOnDeviceSetVolume" + entity);

                    //set volume
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);

                    String msg = "Okay, the volume is set to " + entity + " percent";
                    Speak(msg, "DialogOnDeviceSetVolume", null);

                    dialogStarted = false;
                    activity.loomoRecognizer.startWakeUpListener();
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception: ", e);
            }
        }
    }

    private void ExplorationStart() {

    }

    private void ExplorationStop() {

    }
}
