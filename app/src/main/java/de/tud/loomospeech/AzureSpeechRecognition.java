package de.tud.loomospeech;

import android.util.Log;

import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClientWithIntent;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionStatus;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;

import org.json.JSONArray;
import org.json.JSONObject;


class AzureSpeechRecognition implements ISpeechRecognitionServerEvents {
    private static final String TAG = "AzureSpeechRecognition";

    private MainActivity activity;
    private MessageHandler mHandler;
    private MicrophoneRecognitionClientWithIntent recognitionClientWithIntent;

    AzureSpeechRecognition(MainActivity myActivity) {
        activity = myActivity;
        mHandler = myActivity.mHandler;
    }

    @Override
    public void onPartialResponseReceived(String s) {
        /* String msg = "Partial response: " + s;
        Log.d(TAG, msg);
        mHandler.sendMessage(mHandler.obtainMessage(MessageHandler.INFO, MessageHandler.APPEND, MessageHandler.OUTPUT, msg));
        */
    }

    @Override
    public void onFinalResponseReceived(final RecognitionResult recognitionResult) {
        recognitionClientWithIntent.endMicAndRecognition();

        String msg = "Final response: " + recognitionResult.RecognitionStatus;
//        for (RecognizedPhrase el: recognitionResult.Results) {
//            msg += "\nConfidence: " + el.Confidence + " Text: \"" + el.DisplayText + "\"";
//        }

        Log.d(TAG, msg);
        mHandler.sendMessage(mHandler.obtainMessage(MessageHandler.INFO, MessageHandler.APPEND, MessageHandler.OUTPUT, msg));
        mHandler.sendMessage(mHandler.obtainMessage(MessageHandler.INFO, MessageHandler.SET, MessageHandler.STATUS, activity.getString(R.string.statusProcessing)));
        if (recognitionResult.RecognitionStatus != RecognitionStatus.RecognitionSuccess) {
            activity.startWakeUpListener();
        }
    }

    @Override
    public void onIntentReceived(String s) {
        String msg = "";

        Log.d(TAG, "Intent received!!!");

        try {
            msg = prettyPrintResponse(s);
        } catch (Exception e) {
            Log.d(TAG, "Exception onIntentReceived: ", e);
        }

        mHandler.sendMessage(mHandler.obtainMessage(MessageHandler.INFO, MessageHandler.APPEND, MessageHandler.OUTPUT, msg));
        activity.loomoSoundPool.play("success");

//        recognitionClientWithIntent.endMicAndRecognition();
        activity.startWakeUpListener();
    }

    @Override
    public void onError(int i, String s) {
        String msg = "Error: " + i + " - " + s;
        Log.e(TAG, msg);
        mHandler.sendMessage(mHandler.obtainMessage(MessageHandler.INFO, MessageHandler.APPEND, MessageHandler.OUTPUT, msg));
        activity.loomoSoundPool.play("error");

        activity.startWakeUpListener();
    }

    @Override
    public void onAudioEvent(boolean isRecording) {
        String msg = "Microphone status: " + isRecording;
        if (isRecording) {
            activity.loomoSoundPool.play("listening");
            msg += "\nPlease start speaking...";
        }
        mHandler.sendMessage(mHandler.obtainMessage(MessageHandler.INFO, MessageHandler.APPEND, MessageHandler.OUTPUT, msg));

        if (!isRecording) {
            recognitionClientWithIntent.endMicAndRecognition();
//            activity.startWakeUpListener();
//            this._startButton.setEnabled(true);
        }
    }

    MicrophoneRecognitionClientWithIntent getRecognitionClientWithIntent() {
        if (recognitionClientWithIntent != null) {
            return recognitionClientWithIntent;
        }

        String language = activity.getResources().getConfiguration().locale.toString();
        String subscriptionKey = activity.getString(R.string.subscriptionKey);
        String luisAppID = activity.getString(R.string.luisAppID);
        String luisSubscriptionID = activity.getString(R.string.luisSubscriptionID);

        recognitionClientWithIntent = SpeechRecognitionServiceFactory.createMicrophoneClientWithIntent(activity, language, this, subscriptionKey, luisAppID, luisSubscriptionID);
        recognitionClientWithIntent.setAuthenticationUri(activity.getString(R.string.authenticationUri));

        return recognitionClientWithIntent;
    }

    String prettyPrintResponse(String response) throws Exception {
        StringBuilder sb = new StringBuilder();
        JSONObject json;
        JSONArray intentsArray, entitiesArray;

        json = new JSONObject(response);
        intentsArray = json.getJSONArray("intents");
        sb.append("Text: ").append(json.get("query"));

        for(int i = 0; i<= 2; i++) {
            Double score = (double)intentsArray.getJSONObject(i).get("score") * 100;
            sb.append("\n")
                    .append(score.intValue()).append("%")
                    .append(" ")
                    .append(intentsArray.getJSONObject(i).get("intent"));
        }

        entitiesArray = json.getJSONArray("entities");

        if(entitiesArray.length() > 0) {
            sb.append("\nParameters: ");
            for(int i = 0; i < entitiesArray.length(); i++) {
                Double score = (double)entitiesArray.getJSONObject(i).get("score") * 100;
                sb.append("\n")
                        .append(score.intValue()).append("%")
                        .append(" ")
                        .append(entitiesArray.getJSONObject(i).get("type"))
                        .append(" ")
                        .append(entitiesArray.getJSONObject(i).get("entity"));
            }
        }

        return sb.toString();
    }

}
