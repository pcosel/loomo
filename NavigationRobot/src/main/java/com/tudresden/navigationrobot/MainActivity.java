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
import android.widget.Button;
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

    private static final String LEFT_TURN = "left turn";
    private static final String RIGHT_TURN = "right turn";

    private int press = 0;

    private RobotMessageRouter mRobotMessageRouter = null;
    private MessageConnection mMessageConnection = null;
    private Base mBase = null;
    private Sensor mSensor = null;

    /**
     * Indicates whether the robot is currently in the process of checking the wall on its right.
     */
    private boolean mCheckingWall = false;

    /**
     * The current mState of the robot.
     */
    private State mState;

    /**
     * The current mOrientation of the robot. The mState tells which way the robot is facing in
     * relation to the origin of the coordinate system.
     */
    private Orientation mOrientation;

    /**
     * The current ultrasonic mDistance from the next obstacle in front of the robot.
     */
    private float mDistance;

    /**
     * The current x-coordinate of the robot.
     */
    private double mXCoordinate = 0.0;

    /**
     * The current y-coordinate of the robot.
     */
    private double mYCoordinate = 0.0;

    /**
     * Starts the exploration process by initialising the obstacle avoidance functionality and
     * setting the first checkpoints.
     */
    public void startExploration() {
        mState = State.START;
        mOrientation = Orientation.FORWARD;
        mBase.cleanOriginalPoint();
        PoseVLS pos = mBase.getVLSPose(-1);
        mBase.setOriginalPoint(pos);
        mBase.setUltrasonicObstacleAvoidanceEnabled(true);
        // Keep 1.3 meter mDistance from obstacles
        mBase.setUltrasonicObstacleAvoidanceDistance(1.3f);
        // It is necessary to set 2 checkpoints in the beginning
        // With just one checkpoint, the OnCheckPointArrivedListener is not called correctly
        mBase.addCheckPoint(0, 0);
        mBase.addCheckPoint(0, 0);
    }

    /**
     * Sets a new checkpoint depending on the current mState of the robot.
     * The strategy for setting a new checkpoint is as follows:
     * In the initial phase of the exploration process (before the first wall is found) the new
     * checkpoint is set to make the robot walk forward (1 meter) in order to eventually reach the
     * first wall.
     * In case an obstacle was detected and the left turn was performed ({@see #obstacleDetected()}),
     * the new checkpoint needs to be set to make the robot walk forward (1 meter) in order to reach
     * the next wall.
     * After every meter that the robot walks in search of the next wall, it needs to perform a right
     * turn (90 degrees) in order to check if the wall on its right has ended.
     * If the wall has not ended, the robot performs a left turn (90 degrees) and continues to walk
     * forward along the wall.
     * If the wall has ended, the robot does not turn back but instead follows the new wall.
     */
    public void arrivedAtCheckpoint() {
        mBase.clearCheckPointsAndStop();
        mBase.cleanOriginalPoint();
        PoseVLS pos = mBase.getVLSPose(-1);
        mBase.setOriginalPoint(pos);
        switch(mState) {
            case START:
                // As long as no wall has been found yet, keep walking forward (1 meter)
                mBase.addCheckPoint(1f, 0);
                break;
            case WALK:
                updateCoordinates();
                mState = State.CHECK;
                mDistance = mSensor.getUltrasonicDistance().getDistance() / 1000; // convert mm to m
                // After every meter, turn right (90 degrees)
                mBase.addCheckPoint(0, 0, (float) (-Math.PI / 2));
                break;
            case CHECK:
                // In case after the right turn no obstacle is detected, that means that the wall
                // next to the robot has ended and it needs to walk forward to follow the new wall.
                // In case after the right turn an obstacle is detected, the ObstacleStateChangeListener
                // is triggered (after the code in this switch case is executed) and the robot
                // performs a left turn so that it looks forward again and keeps following the wall.
                // The boolean mCheckingWall is only relevant for the second scenario, because in that
                // case in the ObstacleStateChangeListener the coordinates are not supposed to be
                // updated. mCheckingWall allows to distinguish between the following cases:
                // 1. The robot is walking towards a new wall and an obstacle appears in front of him
                // 2. An obstacle appears while checking the wall next to the robot
                // In both cases the mState of the robot is WALK, but in the ObstacleStateChangeListener
                // different things need to happen.
                // Without setting the mState to WALK in this switch case, the robot would not continue
                // walking if after the right turn no obstacle is found.
                mCheckingWall = true;
                mState = State.WALK;
                updateOrientation(RIGHT_TURN);
                // Walk forward (1 meter)
                mBase.addCheckPoint(1f, 0);
                break;
            case OBSTACLE:
                mState = State.WALK;
                mDistance = mSensor.getUltrasonicDistance().getDistance() / 1000; // convert mm to m
                // After an obstacle was detected and the left turn was performed, walk forward (1 meter)
                mBase.addCheckPoint(1f, 0);
                break;
            default:
                // All possible cases are handled above
        }
    }

    /**
     * Sets a new checkpoint to make the robot perform a left turn (90 degrees) in case an obstacle
     * was detected.
     */
    public void obstacleDetected() {
        if(mState == State.START) {
            // This is the first obstacle that the robot has detected (the coordinates are 0.0)
            Log.d(TAG, "State: " + mState +
                    " | Orientation: " + mOrientation +
                    " | Position: x = " + mXCoordinate + " | y = " + mYCoordinate);
            // TODO: Save position to JSON file
            mState = State.OBSTACLE;
        } else if(mState == State.WALK){
            mState = State.OBSTACLE;
            // Concerning mCheckingWall see explanation in arrivedAtCheckpoint() (case CHECK)
            if(!mCheckingWall) {
                updateCoordinates();
            } else {
                mCheckingWall = false;
            }
        }
        // The robot detects an obstacle before it reaches the current checkpoint. When an obstacle
        // is detected, a new checkpoint is set for the left turn but the robot still tries to reach
        // the last checkpoint first. That checkpoint obviously can't be reached, because there is an
        // obstacle in front of the robot, so the robot just stops walking completely.
        // Therefore the last checkpoint needs to be deleted before the new one is set.
        mBase.clearCheckPointsAndStop();
        mBase.cleanOriginalPoint();
        PoseVLS pos = mBase.getVLSPose(-1);
        mBase.setOriginalPoint(pos);
        updateOrientation(LEFT_TURN);
        // Turn left (90 degrees)
        mBase.addCheckPoint(0, 0, (float) (Math.PI/2));
        // When the turn is finished, {@see #arrivedAtCheckpoint()} is called and the robot walks
        // forward to the next wall
    }

    /**
     * Sets the x- or y-coordinate according to the current mOrientation of the robot.
     */
    public void updateCoordinates() {
        if(mState == State.WALK) {
            switch(mOrientation) {
                case FORWARD:
                    mXCoordinate += 1;
                    break;
                case BACKWARD:
                    mXCoordinate -= 1;
                    break;
                case LEFT:
                    mYCoordinate += 1;
                    break;
                case RIGHT:
                    mYCoordinate -= 1;
                    break;
                default:
                    // All possible cases are handled above
            }
        } else if(mState == State.OBSTACLE) {
            switch(mOrientation) {
                case FORWARD:
                    mXCoordinate += (mDistance - mSensor.getUltrasonicDistance().getDistance() / 1000); // convert mm to m
                    break;
                case BACKWARD:
                    mXCoordinate -= (mDistance - mSensor.getUltrasonicDistance().getDistance() / 1000); // convert mm to m
                    break;
                case LEFT:
                    mYCoordinate += (mDistance - mSensor.getUltrasonicDistance().getDistance() / 1000); // convert mm to m
                    break;
                case RIGHT:
                    mYCoordinate -= (mDistance - mSensor.getUltrasonicDistance().getDistance() / 1000); // convert mm to m
                    break;
                default:
                    // All possible cases are handled above
            }
        }
        Log.d(TAG, "State: " + mState +
                " | Orientation: " + mOrientation +
                " | Position: x = " + mXCoordinate + " | y = " + mYCoordinate);
        // TODO: Save position to JSON file
    }

    /**
     * Sets the mOrientation of the robot in relation to the origin of the coordinate system.
     * The new mOrientation of the robot depends on whether a left or a right turn is to be performed.
     *
     * @param direction Indicates whether the turn to be performed is a left turn or a right turn.
     */
    public void updateOrientation(String direction) {
        if(direction.equals(LEFT_TURN)) {
            switch(mOrientation) {
                case FORWARD:
                    mOrientation = Orientation.LEFT;
                    break;
                case BACKWARD:
                    mOrientation = Orientation.RIGHT;
                    break;
                case LEFT:
                    mOrientation = Orientation.BACKWARD;
                    break;
                case RIGHT:
                    mOrientation = Orientation.FORWARD;
                    break;
                default:
                    // All possible cases are handled above
            }
        } else {
            switch(mOrientation) {
                case FORWARD:
                    mOrientation = Orientation.RIGHT;
                    break;
                case BACKWARD:
                    mOrientation = Orientation.LEFT;
                    break;
                case LEFT:
                    mOrientation = Orientation.FORWARD;
                    break;
                case RIGHT:
                    mOrientation = Orientation.BACKWARD;
                    break;
                default:
                    // All possible cases are handled above
            }
        }
    }

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
                    startExploration();
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

        Button startButton = (Button) findViewById(R.id.button_start);
        startButton.setOnClickListener(this);

        Button stopButton = (Button) findViewById(R.id.button_stop);
        stopButton.setOnClickListener(this);
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
                arrivedAtCheckpoint();
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
                    obstacleDetected();
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
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.button_start:
                startExploration();
                break;
            case R.id.button_stop:
                mBase.clearCheckPointsAndStop();
                break;
        }
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