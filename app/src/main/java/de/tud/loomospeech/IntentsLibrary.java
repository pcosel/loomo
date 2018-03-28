package de.tud.loomospeech;

import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

import org.json.JSONArray;

class IntentsLibrary {
    private static final String TAG = "IntentsLibrary";
    private MainActivity activity;

    IntentsLibrary(MainActivity myActivity) {
        activity = myActivity;
    }

    public void callByName(String functionName, JSONArray entities) {
        //Ignoring any possible result
        //functionName.replace(".", "");
        try {
            this.getClass().getDeclaredMethod(functionName).invoke(this, entities);
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
        int brightness = 50;

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
