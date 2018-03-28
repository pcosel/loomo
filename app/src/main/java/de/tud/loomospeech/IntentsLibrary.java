package de.tud.loomospeech;

import android.provider.Settings;
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
        try {
            this.getClass().getDeclaredMethod(functionName, JSONArray.class).invoke(this, entities);
        } catch (Exception e) {
//            Log.d(TAG, e.getMessage());
            Log.d(TAG, "Exception: ", e);
            this.None();
        }
    }

    private void None() {
        // No suitable action for command found.
        Log.d(TAG, "No suitable action for command found.");
    }

    private void HomeAutomationTurnOn() {}

    private void HomeAutomationTurnOff() {}

    private void OnDeviceOpenApplication() {}

    private void OnDeviceCloseApplication() {}

    private void UtilitiesStop() {}

    private void OnDeviceSetBrightness (JSONArray entities) {
        int brightness = activity.brightness;
        try {
            JSONObject entity = entities.getJSONObject(0);
            if(entity.get("type").toString() == "OnDevice.Brightnesslevel") {
                String value = entity.get("entity").toString();
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
                brightness = entity.getInt("entity");
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
    }
}
