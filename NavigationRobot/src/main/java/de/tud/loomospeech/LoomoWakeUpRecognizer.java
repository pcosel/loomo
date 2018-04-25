package de.tud.loomospeech;


import android.util.Log;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.voice.Recognizer;
import com.segway.robot.sdk.voice.recognition.WakeupListener;
import com.segway.robot.sdk.voice.recognition.WakeupResult;
import com.tudresden.navigationrobot.R;


public class LoomoWakeUpRecognizer {
    private static final String TAG = "LoomoWakeUpRecognizer";

    private MainActivity activity;
    private Recognizer mRecognizer;
    private WakeupListener mWakeupListener;
    private boolean isRecognizerBound = false;


    public LoomoWakeUpRecognizer(MainActivity myActivity) {
        activity = myActivity;

        initRecognizer();
        initWakeUp();
    }


    private void initRecognizer() {
        mRecognizer = Recognizer.getInstance();
        mRecognizer.bindService(activity, new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                Log.d(TAG, "Recognition service onBind");
                activity.mHandler.sendMessage(
                        activity.mHandler.obtainMessage(
                                MessageHandler.INFO
                                , MessageHandler.APPEND
                                , MessageHandler.OUTPUT
                                , activity.getString(R.string.recognition_connected)
                        )
                );

                isRecognizerBound = true;
                startWakeUpListener();
            }

            @Override
            public void onUnbind(String s) {
                Log.d(TAG, "Recognition service onUnbind");
                activity.mHandler.sendMessage(
                        activity.mHandler.obtainMessage(
                                MessageHandler.INFO
                                , MessageHandler.APPEND
                                , MessageHandler.OUTPUT
                                , activity.getString(R.string.recognition_disconnected)
                        )
                );

                isRecognizerBound = false;
            }
        });
    }

    private void initWakeUp() {
        mWakeupListener = new WakeupListener() {
            @Override
            public void onStandby() {
                Log.d(TAG, "WakeUp onStandby");
                activity.mHandler.sendMessage(
                        activity.mHandler.obtainMessage(
                                MessageHandler.INFO
                                , MessageHandler.APPEND
                                , MessageHandler.OUTPUT
                                , "\n" + activity.getString(R.string.wakeup_standby)
                        )
                );
                activity.mHandler.sendMessage(
                        activity.mHandler.obtainMessage(
                                MessageHandler.INFO
                                , MessageHandler.SET
                                , MessageHandler.STATUS
                                , activity.getString(R.string.statusReady)
                        )
                );
            }

            @Override
            public void onWakeupResult(WakeupResult wakeupResult) {
                //show the wakeup result and wakeup angle.
                Log.d(TAG, "Wakeup result:" + wakeupResult.getResult() + ", angle " + wakeupResult.getAngle());
                activity.mHandler.sendMessage(
                        activity.mHandler.obtainMessage(
                                MessageHandler.INFO
                                , MessageHandler.APPEND
                                , MessageHandler.OUTPUT
                                , activity.getString(R.string.wakeup_result) + wakeupResult.getResult() + activity.getString(R.string.wakeup_angle) + wakeupResult.getAngle()
                        )
                );
                activity.mHandler.sendMessage(
                        activity.mHandler.obtainMessage(
                                MessageHandler.INFO
                                , MessageHandler.SET
                                , MessageHandler.STATUS
                                , activity.getString(R.string.statusListening)
                        )
                );
                //start azure recognition
                activity.azureSpeechRecognition.startMicAndRecognitionWithIntent();
            }

            @Override
            public void onWakeupError(String s) {
                //show the wakeup error reason.
                Log.d(TAG, "WakeUp onWakeupError");
                activity.mHandler.sendMessage(
                        activity.mHandler.obtainMessage(
                                MessageHandler.INFO
                                , MessageHandler.APPEND
                                , MessageHandler.OUTPUT
                                , activity.getString(R.string.wakeup_error) + s
                        )
                );
            }
        };
    }

    void startWakeUpListener() {
        if (mRecognizer == null) {
            initRecognizer();
        }
        if (isRecognizerBound) {
            try {
                mRecognizer.startWakeupMode(mWakeupListener);
            } catch (Exception e) {
                Log.e(TAG, "Exception - startWakeUpListener: ", e);
            }
        }
    }
}
