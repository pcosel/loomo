package de.tud.loomospeech;

import android.util.Log;

import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClientWithIntent;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionStatus;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;
import com.tudresden.navigationrobot.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;


public class AzureSpeechRecognition implements ISpeechRecognitionServerEvents {
    private static final String TAG = "AzureSpeechRecognition";

    private MainActivity activity;
    private MessageHandler mHandler;
    private MicrophoneRecognitionClientWithIntent recognitionClientWithIntent;
    private MicrophoneRecognitionClient recognitionClient;
    private IntentsLibrary intentsLibrary;

    private HashMap<String, String> phrases = new HashMap<>();

    public AzureSpeechRecognition(MainActivity myActivity) {
        activity = myActivity;
        mHandler = myActivity.mHandler;
        intentsLibrary = myActivity.intentsLibrary;

        phrases.put("OnDevice.CloseApplication", "Do you want to close the application?");
        phrases.put("OnDevice.Time", "Are you asking for the time?");
        phrases.put("OnDevice.Date", "Are you asking for the date?");
        phrases.put("OnDevice.SetBrightness", "Do you want to set the brightness?");
        phrases.put("OnDevice.SetVolume", "Do you want to set the volume?");
        phrases.put("Exploration.Start", "Do you want me to start exploring?");
        phrases.put("Exploration.Stop", "Do you want me to stop exploring?");

        getRecognitionClient();
        getRecognitionClientWithIntent();
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
        if(recognitionClient != null) {
            recognitionClient.endMicAndRecognition();
        }

        String msg = "Final response: " + recognitionResult.RecognitionStatus;
//        for (RecognizedPhrase el: recognitionResult.Results) {
//            msg += "\nConfidence: " + el.Confidence + " Text: \"" + el.DisplayText + "\"";
//        }

        Log.d(TAG, msg);
        mHandler.sendMessage(mHandler.obtainMessage(MessageHandler.INFO, MessageHandler.SET, MessageHandler.OUTPUT, msg));
        mHandler.sendMessage(mHandler.obtainMessage(MessageHandler.INFO, MessageHandler.SET, MessageHandler.STATUS, activity.getString(R.string.statusProcessing)));

        if(intentsLibrary.dialogStarted && recognitionResult.RecognitionStatus == RecognitionStatus.RecognitionSuccess) {
            intentsLibrary.callByName(intentsLibrary.dialogContext, recognitionResult.Results[0].LexicalForm);
        } else {
            //InitialSilenceTimeout or NoMatch
            if (recognitionResult.RecognitionStatus != RecognitionStatus.RecognitionSuccess) {
                intentsLibrary.dialogStarted = false;

                activity.loomoTextToSpeech.speak(randomPardonMessage(), "NoRecognitionSuccess", new Runnable() {
                    @Override
                    public void run() {
                        startMicAndRecognitionWithIntent();
                    }
                });
            }
        }
    }

    @Override
    public void onIntentReceived(String s) {
        JSONObject json;
        String msg = "";

        Log.d(TAG, "Intent received!!!");

        try {
            json = new JSONObject(s);
            msg = prettyPrintResponse(json);
            final String intent = json.getJSONArray("intents").getJSONObject(0).get("intent").toString();
            JSONArray entities = json.getJSONArray("entities");
            Double score = (double) json.getJSONArray("intents").getJSONObject(0).get("score");

            if(score > 0.4 || intent.equals("Utilities.Confirm") || intent.equals("Utilities.Decline")) {
                intentsLibrary.callByName(intent, entities);
            } else {
                if(phrases.get(intent) != null) {
                    String phrase = phrases.get(intent);
                    activity.loomoTextToSpeech.speak(phrase, "LowConfidence", new Runnable() {
                        @Override
                        public void run() {
                            intentsLibrary.preparedAction = intent;
                            startMicAndRecognitionWithIntent();
                        }
                    });
                } else {
                    activity.loomoTextToSpeech.speak(randomPardonMessage(), "LowConfidence", new Runnable() {
                        @Override
                        public void run() {
                            startMicAndRecognitionWithIntent();
                        }
                    });
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception onIntentReceived: ", e);
        }

        mHandler.sendMessage(mHandler.obtainMessage(MessageHandler.INFO, MessageHandler.APPEND, MessageHandler.OUTPUT, msg));
//        activity.loomoSoundPool.play("success");

//        recognitionClientWithIntent.endMicAndRecognition();
        //activity.loomoRecognizer.startWakeUpListener();
    }

    @Override
    public void onError(int i, String s) {
        String msg = "Error: " + i + " - " + s;
        Log.e(TAG, msg);
        mHandler.sendMessage(mHandler.obtainMessage(MessageHandler.INFO, MessageHandler.APPEND, MessageHandler.OUTPUT, msg));
        activity.loomoSoundPool.play("error");

        activity.loomoRecognizer.startWakeUpListener();
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

    private MicrophoneRecognitionClientWithIntent getRecognitionClientWithIntent() {
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

    private MicrophoneRecognitionClient getRecognitionClient() {
        if (recognitionClient != null) {
            return recognitionClient;
        }

        String language = activity.getResources().getConfiguration().locale.toString();
        String subscriptionKey = activity.getString(R.string.subscriptionKey);

        recognitionClient = SpeechRecognitionServiceFactory.createMicrophoneClient(activity, SpeechRecognitionMode.ShortPhrase, language, this, subscriptionKey);
        recognitionClient.setAuthenticationUri(activity.getString(R.string.authenticationUri));

        return recognitionClient;
    }

    public void startMicAndRecognition() {
        try {
            getRecognitionClient().startMicAndRecognition();
        } catch (Exception e) {
            Log.e(TAG, "Error: ", e);
        }
    }

    public void startMicAndRecognitionWithIntent() {
        try {
            getRecognitionClientWithIntent().startMicAndRecognition();
        } catch (Exception e) {
            Log.e(TAG, "Error: ", e);
        }
    }

   private String prettyPrintResponse(JSONObject json) throws Exception {
        StringBuilder sb = new StringBuilder();
        JSONArray intentsArray, entitiesArray;

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

    private String randomPardonMessage() {

        String msg = "";
        int random = (int) (Math.random() * 3);

        switch (random) {
            case 0:
                msg = "Pardon? I didn't understand that.";
                break;
            case 1:
                msg = "Can you repeat that please?";
                break;
            case 2:
                msg = "What did you say?";
                break;
        }

        return msg;
    }

}
