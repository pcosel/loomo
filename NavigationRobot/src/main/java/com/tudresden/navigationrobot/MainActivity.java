package com.tudresden.navigationrobot;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.segway.robot.algo.Pose2D;
import com.segway.robot.algo.PoseVLS;
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
import com.segway.robot.sdk.locomotion.sbv.StartVLSListener;
import com.segway.robot.sdk.perception.sensor.Sensor;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private int press = 0;

    private RobotMessageRouter mRobotMessageRouter = null;
    private MessageConnection mMessageConnection = null;
    private Base mBase = null;
    private Sensor mSensor = null;

    private MessageRouter.MessageConnectionListener mMessageConnectionListener = new RobotMessageRouter.MessageConnectionListener() {
        @Override
        public void onConnectionCreated(final MessageConnection connection) {
            mMessageConnection = connection;
            try {
                mMessageConnection.setListeners(mConnectionStateListener, mMessageListener);
            } catch(Exception e) {
                Log.e(TAG, "Setting listener failed", e);
            }

        }
    };

    private MessageConnection.ConnectionStateListener mConnectionStateListener = new MessageConnection.ConnectionStateListener() {
        @Override
        public void onOpened() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Connected to: " + mMessageConnection.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onClosed(String error) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Disconnected from: " + mMessageConnection.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private MessageConnection.MessageListener mMessageListener = new MessageConnection.MessageListener() {
        @Override
        public void onMessageSentError(Message message, String error) {}

        @Override
        public void onMessageSent(Message message) {}

        @Override
        public void onMessageReceived(final Message message) {
            if(message instanceof StringMessage) {
                if(message.getContent().equals("start")) {
                    sendMessageToPhone("Received start message");
                    mBase.cleanOriginalPoint();
                    PoseVLS pos = mBase.getVLSPose(-1);
                    mBase.setOriginalPoint(pos);
                    mBase.setUltrasonicObstacleAvoidanceEnabled(true);
                    // Keep 1 meter distance from obstacles
                    mBase.setUltrasonicObstacleAvoidanceDistance(1f);
                    // It is necessary to set 2 checkpoints in the beginning
                    // With just one checkpoint, the OnCheckPointArrivedListener is not called correctly
                    mBase.addCheckPoint(0, 0);
                    mBase.addCheckPoint(0, 0);
                } else if(message.getContent().equals("stop")) {
                    sendMessageToPhone("Received stop message");
                    mBase.clearCheckPointsAndStop();
                }
            }
        }
    };

    private void sendMessageToPhone(String content){
        if(mMessageConnection != null) {
            try {
                mMessageConnection.sendMessage(new StringMessage(content));
            } catch(Exception e) {
                Log.e(TAG, "Sending message (" + content + ") failed", e);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        initView();
        initMessageConnection();
        initBase();
        initSensor();
    }

    private void initView() {
        TextView textViewIp = (TextView) findViewById(R.id.textView_ip);
        textViewIp.setText(getDeviceIp());
    }

    private void initMessageConnection() {
        mRobotMessageRouter = RobotMessageRouter.getInstance();
        mRobotMessageRouter.bindService(this, new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                try {
                    mRobotMessageRouter.register(mMessageConnectionListener);
                } catch(RobotException e) {
                    Log.e(TAG, "Register failed", e);
                }
            }

            @Override
            public void onUnbind(String reason) {}
        });
    }

    private void initBase() {
        mBase = Base.getInstance();

        mBase.bindService(getApplicationContext(), new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                mBase.setControlMode(Base.CONTROL_MODE_NAVIGATION);

                mBase.startVLS(true, true, new StartVLSListener() {
                    @Override
                    public void onOpened() {
                        mBase.setNavigationDataSource(Base.NAVIGATION_SOURCE_TYPE_VLS);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.d(TAG, "onError() called with: errorMessage = [" + errorMessage + "]");
                    }
                });
            }

            @Override
            public void onUnbind(String reason) {}
        });

        mBase.setOnCheckPointArrivedListener(new CheckPointStateListener() {
            @Override
            public void onCheckPointArrived(final CheckPoint checkPoint, Pose2D realPose, boolean isLast) {
                sendMessageToPhone("(" + Float.toString(realPose.getX()) + "  |  " + Float.toString(realPose.getY()) + ")");
                mBase.clearCheckPointsAndStop();
                mBase.cleanOriginalPoint();
                PoseVLS pos = mBase.getVLSPose(-1);
                mBase.setOriginalPoint(pos);
                // Walk forward (1 meter)
                mBase.addCheckPoint(1f, 0);
            }

            @Override
            public void onCheckPointMiss(CheckPoint checkPoint, Pose2D realPose, boolean isLast, int reason) {
                sendMessageToPhone("Missed checkpoint");
            }
        });

        mBase.setObstacleStateChangeListener(new ObstacleStateChangedListener() {
            @Override
            public void onObstacleStateChanged(int ObstacleAppearance) {
                if(ObstacleAppearance == ObstacleStateChangedListener.OBSTACLE_APPEARED) {
                    sendMessageToPhone("Obstacle appeared");
                    // The robot detects an obstacle before it reaches the current checkpoint. When an obstacle
                    // is detected, a new checkpoint is set for the left turn but the robot still tries to reach
                    // the last checkpoint first. But that checkpoint can't be reached, because there's an
                    // obstacle in front of the robot, so the robot just stops walking completely.
                    // Therefore the last checkpoint needs to be deleted before the new one is set.
                    mBase.clearCheckPointsAndStop();
                    mBase.cleanOriginalPoint();
                    PoseVLS pos = mBase.getVLSPose(-1);
                    mBase.setOriginalPoint(pos);
                    // Turn left (90 degrees)
                    mBase.addCheckPoint(0, 0, (float) (Math.PI/2));
                    // When the turn is finished, onCheckPointArrived() is called and Loomo walks forward to the next wall
                }
            }
        });
    }

    private void initSensor() {
        mSensor = Sensor.getInstance();
        mSensor.bindService(getApplicationContext(), new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {}

            @Override
            public void onUnbind(String reason) {}
        });
    }

    @Override
    public void onClick(View v) {}

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
        if(press == 0) {
            Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show();
        }
        press++;
        if(press == 2) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(null != this.getCurrentFocus()) {
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
        } catch(RobotException e) {
            Log.e(TAG, "Unregister failed", e);
        }
        mRobotMessageRouter.unbindService();
        mBase.unbindService();
        mSensor.unbindService();

    }

    private String getDeviceIp() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(!wifiManager.isWifiEnabled()) {
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