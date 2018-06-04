package de.tud.loomospeech;

import android.app.ActionBar;
import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MenuItem;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    public LoomoWakeUpRecognizer loomoRecognizer;
    AzureSpeechRecognition azureSpeechRecognition;
    MessageHandler mHandler;
    LoomoSoundPool loomoSoundPool;
    LoomoTextToSpeech loomoTextToSpeech;
    public IntentsLibrary intentsLibrary;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        switchLanguage(Locale.getDefault());
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mHandler == null) mHandler = new MessageHandler(this);
        if (loomoSoundPool == null) loomoSoundPool = new LoomoSoundPool(this);
        if (intentsLibrary == null) intentsLibrary  = new IntentsLibrary(this);
        if (azureSpeechRecognition == null) azureSpeechRecognition = new AzureSpeechRecognition(this);
        if (loomoRecognizer == null) loomoRecognizer = new LoomoWakeUpRecognizer(this);
        if (loomoTextToSpeech == null) loomoTextToSpeech = new LoomoTextToSpeech(this);
    }

    @Override
    protected void onStop() {
        if (azureSpeechRecognition != null) azureSpeechRecognition = null;
        if (loomoSoundPool != null) loomoSoundPool = null;
        if (loomoRecognizer != null) loomoRecognizer = null;
//        if (loomoTextToSpeech != null) loomoTextToSpeech.shutdown();
        if (loomoTextToSpeech != null) loomoTextToSpeech = null;

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null) mHandler = null;

        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /* ----------------------------- Helper functions -------------------------------------- */

    void switchLanguage(Locale locale) {
        Configuration config = getResources().getConfiguration();
        Resources resources = getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        config.locale = locale;
        resources.updateConfiguration(config, dm);
    }
}
