package com.segway.speechdemo;

import android.app.ActionBar;
import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClientWithIntent;
import com.microsoft.cognitiveservices.speechrecognition.RecognizedPhrase;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.voice.Languages;
import com.segway.robot.sdk.voice.Recognizer;
import com.segway.robot.sdk.voice.Speaker;
import com.segway.robot.sdk.voice.VoiceException;
import com.segway.robot.sdk.voice.audiodata.RawDataListener;
import com.segway.robot.sdk.voice.grammar.GrammarConstraint;
import com.segway.robot.sdk.voice.grammar.Slot;
import com.segway.robot.sdk.voice.recognition.RecognitionListener;
import com.segway.robot.sdk.voice.recognition.RecognitionResult;
import com.segway.robot.sdk.voice.recognition.WakeupListener;
import com.segway.robot.sdk.voice.recognition.WakeupResult;
import com.segway.robot.sdk.voice.tts.TtsListener;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements ISpeechRecognitionServerEvents {
    private static final String TAG = "MainActivity";
    private static final String FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    private static final int SHOW_MSG = 0x0001;
    private static final int APPEND = 0x000f;
    private static final int CLEAR = 0x00f0;
    private ServiceBinder.BindStateListener mRecognitionBindStateListener;
    private ServiceBinder.BindStateListener mSpeakerBindStateListener;
    private boolean isBeamForming = false;
    private boolean bindSpeakerService;
    private boolean bindRecognitionService;
    private int mSpeakerLanguage;
    private int mRecognitionLanguage;
    private TextView mStatusTextView;
    private TextView mStatus;
    private Recognizer mRecognizer;
    private Speaker mSpeaker;
    private WakeupListener mWakeupListener;
    private RecognitionListener mRecognitionListener;
    private RawDataListener mRawDataListener;
    private TtsListener mTtsListener;
    private GrammarConstraint mTwoSlotGrammar;
    private GrammarConstraint mThreeSlotGrammar;
    private VoiceHandler mHandler = new VoiceHandler(this);
    private MicrophoneRecognitionClientWithIntent m_micClient;



    public static class VoiceHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        private VoiceHandler(MainActivity instance) {
            mActivity = new WeakReference<>(instance);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = mActivity.get();
            if (mainActivity != null) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case SHOW_MSG:
                        mainActivity.showMessage((String) msg.obj, msg.arg1);
                        break;
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        switchLanguage(Locale.getDefault());
        mRecognizer = Recognizer.getInstance();
        mSpeaker = Speaker.getInstance();
        initListeners();
        initRecognitionClient();

        //init textviews
        mStatusTextView = (TextView) findViewById(R.id.textView_status_msg);
        mStatusTextView.setMovementMethod(new ScrollingMovementMethod());
        mStatus = (TextView) findViewById(R.id.textView_status);

        //binding
        mRecognizer.bindService(MainActivity.this, mRecognitionBindStateListener);
        mSpeaker.bindService(MainActivity.this, mSpeakerBindStateListener);

    }

    @Override
    protected void onDestroy() {
        if (mRecognizer != null) {
            mRecognizer = null;
        }
        if (mSpeaker != null) {
            mSpeaker = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mStatusTextView.setText("");
    }

    //init listeners.
    private void initListeners() {

        mRecognitionBindStateListener = new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                Message connectMsg = mHandler.obtainMessage(SHOW_MSG, APPEND, 0,
                        getString(R.string.recognition_connected));
                mHandler.sendMessage(connectMsg);
                try {
                    //get recognition language when service bind.
                    mRecognitionLanguage = mRecognizer.getLanguage();
                    initControlGrammar();
                    switch (mRecognitionLanguage) {
                        case Languages.EN_US:
                            addEnglishGrammar();
                            break;
                        case Languages.ZH_CN:
                            addChineseGrammar();
                            break;
                    }
                } catch (VoiceException e) {
                    Log.e(TAG, "Exception: ", e);
                }

                //start the wakeup
                try {
                    mRecognizer.startWakeupMode(mWakeupListener);
                } catch (VoiceException e) {
                    Log.e(TAG, "Exception: ", e);
                }
            }

            @Override
            public void onUnbind(String s) {
                //speaker service or recognition service unbind, disable function buttons.
                Log.d(TAG, "recognition service onUnbind");
                Message connectMsg = mHandler.obtainMessage(SHOW_MSG, APPEND, 0, getString(R.string.recognition_disconnected));
                mHandler.sendMessage(connectMsg);
            }
        };

        mSpeakerBindStateListener = new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                Log.d(TAG, "speaker service onBind");
                try {
                    Message connectMsg = mHandler.obtainMessage(SHOW_MSG, APPEND, 0,
                            getString(R.string.speaker_connected));
                    mHandler.sendMessage(connectMsg);
                    //get speaker service language.
                    mSpeakerLanguage = mSpeaker.getLanguage();
                } catch (VoiceException e) {
                    Log.e(TAG, "Exception: ", e);
                }
                bindSpeakerService = true;

                // set the volume of TTS
                try {
                    mSpeaker.setVolume(50);
                } catch (VoiceException e) {
                    e.printStackTrace();
                }

                if (bindRecognitionService) {
                    //both speaker service and recognition service bind, enable function buttons.
                }
            }

            @Override
            public void onUnbind(String s) {
                Log.d(TAG, "speaker service onUnbind");
                //speaker service or recognition service unbind, disable function buttons.
                Message connectMsg = mHandler.obtainMessage(SHOW_MSG, APPEND, 0, getString(R.string.speaker_disconnected));
                mHandler.sendMessage(connectMsg);
            }
        };


        mWakeupListener = new WakeupListener() {
            @Override
            public void onStandby() {
                Log.d(TAG, "onStandby");
                Message statusMsg = mHandler.obtainMessage(SHOW_MSG, APPEND, 0, getString(R.string.wakeup_standby));
                mHandler.sendMessage(statusMsg);
            }

            @Override
            public void onWakeupResult(WakeupResult wakeupResult) {
                //show the wakeup result and wakeup angle.
                Log.d(TAG, "Wakeup word:" + wakeupResult.getResult() + ", angle " + wakeupResult.getAngle());
                Message resultMsg = mHandler.obtainMessage(SHOW_MSG, APPEND, 0, getString(R.string.wakeup_result) + wakeupResult.getResult() + getString(R.string.wakeup_angle) + wakeupResult.getAngle());
                mHandler.sendMessage(resultMsg);

                //start azure recognition
                m_micClient.startMicAndRecognition();
            }

            @Override
            public void onWakeupError(String s) {
                //show the wakeup error reason.
                Log.d(TAG, "onWakeupError");
                Message errorMsg = mHandler.obtainMessage(SHOW_MSG, APPEND, 0, getString(R.string.wakeup_error) + s);
                mHandler.sendMessage(errorMsg);
            }
        };

        mRawDataListener = new RawDataListener() {
            @Override
            public void onRawData(byte[] bytes, int i) {
                createFile(bytes, "raw.pcm");
            }
        };

        mTtsListener = new TtsListener() {
            @Override
            public void onSpeechStarted(String s) {
                //s is speech content, callback this method when speech is starting.
                Log.d(TAG, "onSpeechStarted() called with: s = [" + s + "]");
                Message statusMsg = mHandler.obtainMessage(SHOW_MSG, APPEND, 0, getString(R.string.speech_start));
                mHandler.sendMessage(statusMsg);
            }

            @Override
            public void onSpeechFinished(String s) {
                //s is speech content, callback this method when speech is finish.
                Log.d(TAG, "onSpeechFinished() called with: s = [" + s + "]");
                Message statusMsg = mHandler.obtainMessage(SHOW_MSG, APPEND, 0, getString(R.string.speech_end));
                mHandler.sendMessage(statusMsg);
            }

            @Override
            public void onSpeechError(String s, String s1) {
                //s is speech content, callback this method when speech occurs error.
                Log.d(TAG, "onSpeechError() called with: s = [" + s + "], s1 = [" + s1 + "]");
                Message statusMsg = mHandler.obtainMessage(SHOW_MSG, APPEND, 0, getString(R.string.speech_error) + s1);
                mHandler.sendMessage(statusMsg);
            }
        };
    }

    private void addEnglishGrammar() throws VoiceException {
        String grammarJson = "{\n" +
                "         \"name\": \"play_media\",\n" +
                "         \"slotList\": [\n" +
                "             {\n" +
                "                 \"name\": \"play_cmd\",\n" +
                "                 \"isOptional\": false,\n" +
                "                 \"word\": [\n" +
                "                     \"play\",\n" +
                "                     \"close\",\n" +
                "                     \"pause\"\n" +
                "                 ]\n" +
                "             },\n" +
                "             {\n" +
                "                 \"name\": \"media\",\n" +
                "                 \"isOptional\": false,\n" +
                "                 \"word\": [\n" +
                "                     \"the music\",\n" +
                "                     \"the video\"\n" +
                "                 ]\n" +
                "             }\n" +
                "         ]\n" +
                "     }";
        mTwoSlotGrammar = mRecognizer.createGrammarConstraint(grammarJson);
        mRecognizer.addGrammarConstraint(mTwoSlotGrammar);
        mRecognizer.addGrammarConstraint(mThreeSlotGrammar);
    }

    private void addChineseGrammar() throws VoiceException {
        Slot play = new Slot("play", false, Arrays.asList("播放", "打开", "关闭", "暂停"));
        Slot media = new Slot("media", false, Arrays.asList("音乐", "视频", "电影"));
        List<Slot> slotList = new LinkedList<>();
        slotList.add(play);
        slotList.add(media);
        mTwoSlotGrammar = new GrammarConstraint("play_media", slotList);
        mRecognizer.addGrammarConstraint(mTwoSlotGrammar);
        mRecognizer.addGrammarConstraint(mThreeSlotGrammar);
    }

    // init control grammar, it can't control robot. :)
    private void initControlGrammar() {

        switch (mRecognitionLanguage) {
            case Languages.EN_US:
                Slot moveSlot = new Slot("move");
                Slot toSlot = new Slot("to");
                Slot orientationSlot = new Slot("orientation");
                List<Slot> controlSlotList = new LinkedList<>();
                moveSlot.setOptional(false);
                moveSlot.addWord("turn");
                moveSlot.addWord("move");
                controlSlotList.add(moveSlot);

                toSlot.setOptional(true);
                toSlot.addWord("to the");
                controlSlotList.add(toSlot);

                orientationSlot.setOptional(false);
                orientationSlot.addWord("right");
                orientationSlot.addWord("left");
                controlSlotList.add(orientationSlot);

                mThreeSlotGrammar = new GrammarConstraint("three slots grammar", controlSlotList);
                break;
        }
    }

    private void showMessage(String msg, final int pattern) {
        switch (pattern) {
            case CLEAR:
                mStatusTextView.setText(msg);
                break;
            case APPEND:
                mStatusTextView.append("\n" + msg);
                break;
        }
    }

    private void createFile(byte[] buffer, String fileName) {
        RandomAccessFile randomFile = null;
        try {
            randomFile = new RandomAccessFile(FILE_PATH + fileName, "rw");
            long fileLength = randomFile.length();
            randomFile.seek(fileLength);
            randomFile.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (randomFile != null) {
                try {
                    randomFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void switchLanguage(Locale locale) {
        Configuration config = getResources().getConfiguration();
        Resources resources = getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        config.locale = locale;
        resources.updateConfiguration(config, dm);
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
        if (null != this.getCurrentFocus()) {
            InputMethodManager mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            return mInputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
        }
        return super.onTouchEvent(event);
    }


/*--------------------------------------AZURE-STUFF-------------------------------------------------*/

    @Override
    public void onPartialResponseReceived(String s) {
        String msg = "Partial response:" + s;
        Log.d(TAG, msg);
        Message resultMsg = mHandler.obtainMessage(SHOW_MSG, APPEND, 0, msg);
        mHandler.sendMessage(resultMsg);
    }

    @Override
    public void onFinalResponseReceived(com.microsoft.cognitiveservices.speechrecognition.RecognitionResult recognitionResult) {
        m_micClient.endMicAndRecognition();

        String msg = "Final response:" + recognitionResult.toString();
        for (RecognizedPhrase el: recognitionResult.Results) {
            msg = msg.concat("\nConfidence: " + el.Confidence + " Text: \"" + el.DisplayText + "\"");
        }

        Log.d(TAG, msg);
        mHandler.sendMessage(mHandler.obtainMessage(SHOW_MSG, APPEND, 0, msg));
    }

    @Override
    public void onIntentReceived(String s) {
        String msg = "Intent:" + s;
//        Log.d(TAG, msg);
        Log.d(TAG, "Intent received!!!");
        Message resultMsg = mHandler.obtainMessage(SHOW_MSG, APPEND, 0, msg);
        mHandler.sendMessage(resultMsg);
        m_micClient.endMicAndRecognition();



        //start the wakeup
        try {
            mRecognizer.startWakeupMode(mWakeupListener);
        } catch (VoiceException e) {
            Log.e(TAG, "Exception: ", e);
        }
    }

    @Override
    public void onError(int i, String s) {
        String msg = "Error:" + i + " - " + s;
        Log.d(TAG, msg);
        Message resultMsg = mHandler.obtainMessage(SHOW_MSG, APPEND, 0, msg);
        mHandler.sendMessage(resultMsg);
    }

    @Override
    public void onAudioEvent(boolean isRecording) {
        String msg = "Microphone status: " + isRecording;
        if (isRecording) { msg += "\nPlease start speaking..."; }
        mHandler.sendMessage(mHandler.obtainMessage(SHOW_MSG, APPEND, 0, msg));

        if (!isRecording) {
            m_micClient.endMicAndRecognition();
        }
    }

    void initRecognitionClient()
    {
        if(m_micClient != null) {
            return;
        }
        String language = getResources().getConfiguration().locale.toString();

        String subscriptionKey = this.getString(R.string.subscriptionKey);
        String luisAppID = this.getString(R.string.luisAppID);
        String luisSubscriptionID = this.getString(R.string.luisSubscriptionID);

        m_micClient = SpeechRecognitionServiceFactory.createMicrophoneClientWithIntent(this, language, this, subscriptionKey, luisAppID, luisSubscriptionID);
    }
}
