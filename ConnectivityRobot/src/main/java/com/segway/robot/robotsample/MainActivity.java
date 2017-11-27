package com.segway.robot.robotsample;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.segway.robot.algo.Pose2D;
import com.segway.robot.algo.minicontroller.CheckPoint;
import com.segway.robot.algo.minicontroller.ObstacleStateChangedListener;
import com.segway.robot.algo.minicontroller.CheckPointStateListener;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.baseconnectivity.Message;
import com.segway.robot.sdk.baseconnectivity.MessageConnection;
import com.segway.robot.sdk.baseconnectivity.MessageRouter;
import com.segway.robot.sdk.connectivity.RobotException;
import com.segway.robot.sdk.connectivity.RobotMessageRouter;
import com.segway.robot.sdk.connectivity.StringMessage;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.perception.sensor.Sensor;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "RobotActivity";
    private int press = 0;
    private TextView textViewIp;
    private TextView textViewId;
    private TextView textViewTime;
    private TextView textViewContent;
    private TextView textViewTest;
    private RobotMessageRouter mRobotMessageRouter = null;
    private MessageConnection mMessageConnection = null;

    private Base base = null;
    private Sensor sensor = null;

    /**
     * This BindStateListener is used for the Base Service.
     **/
    private ServiceBinder.BindStateListener baseBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {

        }

        @Override
        public void onUnbind(String reason) {
        }
    };

    /**
     * This BindStateListener is used for the Sensor Service.
     **/
    private ServiceBinder.BindStateListener sensorBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {

        }

        @Override
        public void onUnbind(String reason) {
        }
    };

    /**
     * This BindStateListener is used for the Connectivity Service
     **/
    private ServiceBinder.BindStateListener mBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.d(TAG, "onBind: ");
            try {
                //register MessageConnectionListener in the RobotMessageRouter
                mRobotMessageRouter.register(mMessageConnectionListener);
            } catch (RobotException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUnbind(String reason) {
            Log.e(TAG, "onUnbind: " + reason);
        }
    };

    private MessageRouter.MessageConnectionListener mMessageConnectionListener = new RobotMessageRouter.MessageConnectionListener() {
        @Override
        public void onConnectionCreated(final MessageConnection connection) {
            Log.d(TAG, "onConnectionCreated: " + connection.getName());
            mMessageConnection = connection;
            try {
                mMessageConnection.setListeners(mConnectionStateListener, mMessageListener);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };

    private MessageConnection.ConnectionStateListener mConnectionStateListener = new MessageConnection.ConnectionStateListener() {
        @Override
        public void onOpened() {
            Log.d(TAG, "onOpened: ");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "connected to: " + mMessageConnection.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onClosed(String error) {
            Log.e(TAG, "onClosed: " + error);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "disconnected to: " + mMessageConnection.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private MessageConnection.MessageListener mMessageListener = new MessageConnection.MessageListener() {
        @Override
        public void onMessageSentError(Message message, String error) {

        }

        @Override
        public void onMessageSent(Message message) {
            Log.d(TAG, "onBufferMessageSent: id=" + message.getId() + ";timestamp=" + message.getTimestamp());
        }

        @Override
        public void onMessageReceived(final Message message) {
            Log.d(TAG, "onMessageReceived: id=" + message.getId() + ";timestamp=" + message.getTimestamp());
            if (message instanceof StringMessage) {
                //message received is StringMessage
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textViewId.setText(Integer.toString(message.getId()));
                        textViewTime.setText(Long.toString(message.getTimestamp()));
                        textViewContent.setText(message.getContent().toString());

                        if (message.getContent().equals("start")) {
                            if(base.getControlMode() != Base.CONTROL_MODE_NAVIGATION)
                                base.setControlMode(Base.CONTROL_MODE_NAVIGATION);
                            base.cleanOriginalPoint();
                            Pose2D pos = base.getOdometryPose(-1);
                            base.setOriginalPoint(pos);
                            base.setUltrasonicObstacleAvoidanceEnabled(true);
                            base.setUltrasonicObstacleAvoidanceDistance(0.5f);
                            base.addCheckPoint(1f, 0);

                            base.setObstacleStateChangeListener(new ObstacleStateChangedListener() {
                                @Override
                                public void onObstacleStateChanged(int ObstacleAppearance) {
                                    if (ObstacleAppearance == ObstacleStateChangedListener.OBSTACLE_APPEARED) {
                                        base.addCheckPoint(0, 1f);
                                    }
                                }
                            });
                        } else if (message.getContent().equals("stop")) {
                            base.stop();
                        }
                    }
                });
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        textViewIp = (TextView) findViewById(R.id.textView_ip);
        textViewId = (TextView) findViewById(R.id.textView_id);
        textViewTime = (TextView) findViewById(R.id.textView_time);
        textViewContent = (TextView) findViewById(R.id.textView_content);
        textViewTest = (TextView) findViewById(R.id.textView_test);
        textViewContent.setMovementMethod(ScrollingMovementMethod.getInstance());

        textViewIp.setText(getDeviceIp());

        //get RobotMessageRouter
        mRobotMessageRouter = RobotMessageRouter.getInstance();
        //bind to connection service in robot
        mRobotMessageRouter.bindService(this, mBindStateListener);

        base = Base.getInstance();
        base.bindService(getApplicationContext(), baseBindStateListener);

        sensor = Sensor.getInstance();
        sensor.bindService(getApplicationContext(), sensorBindStateListener);

        base.setOnCheckPointArrivedListener(new CheckPointStateListener() {
            @Override
            public void onCheckPointArrived(CheckPoint checkPoint, Pose2D realPose, boolean isLast) {
                if(isLast) {
                    textViewTest.setText("TRUE");
                    Pose2D pos = base.getOdometryPose(-1);
                    base.setOriginalPoint(pos);
                    base.addCheckPoint(1f, 0);
                } else {
                    textViewTest.setText("FALSE");
                }
            }

            @Override
            public void onCheckPointMiss(CheckPoint checkPoint, Pose2D realPose, boolean isLast, int reason) {
                textViewTest.setText("MISSED CHECKPOINT");
            }
        });
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        if (press == 0) {
            Toast.makeText(this, "press again to exit", Toast.LENGTH_SHORT).show();
        }
        press++;
        if (press == 2) {
            super.onBackPressed();
        }
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            mRobotMessageRouter.unregister();
        } catch (RobotException e) {
            e.printStackTrace();
        }
        mRobotMessageRouter.unbindService();
        Log.d(TAG, "onDestroy: ");

        base.unbindService();
        sensor.unbindService();

    }

    private String getDeviceIp() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        return (ipAddress & 0xFF) + "." +
                ((ipAddress >> 8) & 0xFF) + "." +
                ((ipAddress >> 16) & 0xFF) + "." +
                (ipAddress >> 24 & 0xFF);
    }

}
