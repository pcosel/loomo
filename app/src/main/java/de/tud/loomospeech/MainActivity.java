package de.tud.loomospeech;

import android.app.ActionBar;
import android.content.ContentResolver;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Button btnAction;

    LoomoWakeUpRecognizer loomoRecognizer;
    AzureSpeechRecognition azureSpeechRecognition;
    MessageHandler mHandler;
    LoomoSoundPool loomoSoundPool;
    int brightness;
    ContentResolver cResolver;
    Window window;
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
        loomoTextToSpeech = new LoomoTextToSpeech(this);
        //Get the content resolver
        cResolver = getContentResolver();
        //Get the current window
        window = getWindow();

        try {
            // To handle the auto
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            brightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            //Throw an error case it couldn't be retrieved
            Log.e(TAG, "Error: Cannot access system brightness");
            e.printStackTrace();
        }


        initBtnAction();

    }

    @Override
    protected void onDestroy() {
        if (azureSpeechRecognition != null) azureSpeechRecognition = null;
        if (mHandler != null) mHandler = null;
        if (loomoSoundPool != null) loomoSoundPool = null;
        if (loomoRecognizer != null) loomoRecognizer = null;
        if (loomoTextToSpeech != null) loomoTextToSpeech.shutdown();
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
                azureSpeechRecognition.getRecognitionClientWithIntent().startMicAndRecognition();
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
