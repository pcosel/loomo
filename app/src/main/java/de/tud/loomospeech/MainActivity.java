package de.tud.loomospeech;

import android.app.ActionBar;
import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private Button btnAction;

    LoomoWakeUpRecognizer loomoRecognizer;
    AzureSpeechRecognition azureSpeechRecognition;
    MessageHandler mHandler;
    LoomoSoundPool loomoSoundPool;
    LoomoTextToSpeech loomoTextToSpeech;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        btnAction = (Button) findViewById(R.id.btn_action);
        TextView mOutputTextView = (TextView) findViewById(R.id.output);
        mOutputTextView.setMovementMethod(new ScrollingMovementMethod());
        switchLanguage(Locale.getDefault());
        mHandler = new MessageHandler(this);
        loomoSoundPool = new LoomoSoundPool(this);
        azureSpeechRecognition = new AzureSpeechRecognition(this);
        loomoRecognizer = new LoomoWakeUpRecognizer(this);


        initBtnAction();

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (loomoTextToSpeech == null) loomoTextToSpeech = new LoomoTextToSpeech(this);
    }

    @Override
    protected void onStop() {
        if (loomoTextToSpeech != null) loomoTextToSpeech.shutdown();

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (azureSpeechRecognition != null) azureSpeechRecognition = null;
        if (mHandler != null) mHandler = null;
        if (loomoSoundPool != null) loomoSoundPool = null;
        if (loomoRecognizer != null) loomoRecognizer = null;

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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        if (null != this.getCurrentFocus()) {
//            InputMethodManager mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
//            return mInputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
//        }
        return super.onTouchEvent(event);
    }

    protected void initBtnAction() {
//        btnAction.setEnabled(true);
        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                btnAction.setEnabled(false);
                azureSpeechRecognition.startMicAndRecognitionWithIntent();
            }
        });
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
