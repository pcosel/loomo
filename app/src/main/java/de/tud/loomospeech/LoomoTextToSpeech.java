package de.tud.loomospeech;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

public class LoomoTextToSpeech implements TextToSpeech.OnInitListener {
    private static final String TAG = "LoomoTextToSpeech";

    private TextToSpeech Tts;
    private boolean isTtsReady = false;
    private Context ctx;
    private UtteranceProgressListener LoomoUtteranceProgressListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
            Log.e(TAG, "Speaking started.");
        }

        @Override
        public void onDone(String utteranceId) {
            Log.e(TAG, "Speaking stopped.");
        }

        @Override
        public void onError(String utteranceId) {
            Log.e(TAG, "There was an error.");
        }
    };


    LoomoTextToSpeech(Context context) {
        ctx = context;
        Tts = new TextToSpeech(ctx, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            Tts.setLanguage(ctx.getResources().getConfiguration().locale);

            isTtsReady = true;
            Tts.speak("This is Sparta.", TextToSpeech.QUEUE_FLUSH, null, null);

            Tts.setOnUtteranceProgressListener(LoomoUtteranceProgressListener);
        }
    }

    public int speak(CharSequence text, String utteranceId) {
        if (!isTtsReady) return -1;
        return Tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

    public void shutdown() {
        Tts.shutdown();
    }
}
