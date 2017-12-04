package com.segway.robot.robotsample;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
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

    private int press = 0;
    private TextView textViewIp;
    private TextView textViewId;
    private TextView textViewTime;
    private TextView textViewContent;
    private TextView textViewTest;

    private RobotMessageRouter mRobotMessageRouter = null;
    private MessageConnection mMessageConnection = null;
    private Base mBase = null;
    private Sensor mSensor = null;

    private int counter = 0;

    private MessageRouter.MessageConnectionListener mMessageConnectionListener = new RobotMessageRouter.MessageConnectionListener() {
        @Override
        public void onConnectionCreated(final MessageConnection connection) {
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
            if (message instanceof StringMessage) {
                //message received is StringMessage
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textViewId.setText(Integer.toString(message.getId()));
                        textViewTime.setText(Long.toString(message.getTimestamp()));
                        textViewContent.setText(message.getContent().toString());
                    }
                });
                if(message.getContent().equals("start")) {
                    mBase.cleanOriginalPoint();
                    Pose2D pos = mBase.getOdometryPose(-1);
                    mBase.setOriginalPoint(pos);
                    mBase.setUltrasonicObstacleAvoidanceEnabled(true);
                    mBase.setUltrasonicObstacleAvoidanceDistance(1f);
                    mBase.addCheckPoint(1f, 0);
                    mBase.addCheckPoint(2f, 0);
                    mBase.addCheckPoint(3f, 0);
                } else if (message.getContent().equals("stop")) {
                    mBase.clearCheckPointsAndStop();
                }
            }
        }
    };

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
        textViewIp = (TextView) findViewById(R.id.textView_ip);
        textViewId = (TextView) findViewById(R.id.textView_id);
        textViewTime = (TextView) findViewById(R.id.textView_time);
        textViewContent = (TextView) findViewById(R.id.textView_content);
        textViewTest = (TextView) findViewById(R.id.textView_test);
        textViewContent.setMovementMethod(ScrollingMovementMethod.getInstance());
        textViewIp.setText(getDeviceIp());
    }

    private void initMessageConnection() {
        mRobotMessageRouter = RobotMessageRouter.getInstance();
        mRobotMessageRouter.bindService(this, new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                try {
                    //register MessageConnectionListener in the RobotMessageRouter
                    mRobotMessageRouter.register(mMessageConnectionListener);
                } catch (RobotException e) {
                    e.printStackTrace();
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
            }

            @Override
            public void onUnbind(String reason) {}
        });

        mBase.setOnCheckPointArrivedListener(new CheckPointStateListener() {
            @Override
            public void onCheckPointArrived(CheckPoint checkPoint, Pose2D realPose, boolean isLast) {
                //Log.d(TAG, "onCheckPointArrived: x: " + checkPoint.getX() + " y: " + checkPoint.getY());
                counter++;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textViewTest.setText("Arrived at checkpoint " + counter);
                    }
                });
                mBase.cleanOriginalPoint();
                Pose2D pos = mBase.getOdometryPose(-1);
                mBase.setOriginalPoint(pos);
                mBase.addCheckPoint(1f, 0);
            }

            @Override
            public void onCheckPointMiss(CheckPoint checkPoint, Pose2D realPose, boolean isLast, int reason) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textViewTest.setText("Missed checkpoint");
                    }
                });
            }
        });

        mBase.setObstacleStateChangeListener(new ObstacleStateChangedListener() {
            @Override
            public void onObstacleStateChanged(int ObstacleAppearance) {
                if (ObstacleAppearance == ObstacleStateChangedListener.OBSTACLE_APPEARED) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textViewContent.setText("Obstacle detected");
                        }
                    });
                    mBase.cleanOriginalPoint();
                    Pose2D pos = mBase.getOdometryPose(-1);
                    mBase.setOriginalPoint(pos);
                    mBase.addCheckPoint(0, 1f);
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
            public void onUnbind(String reason) {
            }
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
        if (press == 0) {
            Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show();
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
        mBase.unbindService();
        mSensor.unbindService();

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