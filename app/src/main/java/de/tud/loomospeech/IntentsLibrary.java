package de.tud.loomospeech;

import android.util.Log;

import org.json.JSONArray;

class IntentsLibrary {
    private static final String TAG = "IntentsLibrary";

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

    }
}
