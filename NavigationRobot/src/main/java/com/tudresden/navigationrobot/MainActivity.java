package com.tudresden.navigationrobot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.segway.robot.algo.Pose2D;
import com.segway.robot.algo.PoseVLS;
import com.segway.robot.algo.minicontroller.CheckPoint;
import com.segway.robot.algo.minicontroller.ObstacleStateChangedListener;
import com.segway.robot.algo.minicontroller.CheckPointStateListener;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.locomotion.sbv.StartVLSListener;
import com.segway.robot.sdk.perception.sensor.Sensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedList;

/**
 * This Activity handles the exploration process.
 */
public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final String LEFT_TURN = "left turn";
    private static final String RIGHT_TURN = "right turn";
    private static final String FILENAME = "positions.json";

    private int mPress = 0;

    /**
     * A handler for delaying the execution of the code in #obstacleDetected() in order to reduce
     * the number of false positives in obstacle detection.
     */
    private Handler mHandler = new Handler();

    private ServiceBinder.BindStateListener mBaseBindStateListener;
    private ServiceBinder.BindStateListener mSensorBindStateListener;

    /**
     * The Base instance that is used for controlling the robots movements.
     */
    private Base mBase = null;

    /**
     * The Sensor instance that is used for every action related to the ultrasonic sensor.
     */
    private Sensor mSensor = null;

    /**
     * Indicates whether the robot is currently in the process of checking the wall on its right.
     */
    private boolean mCheckingWall = false;

    /**
     * The current state of the robot.
     */
    private State mState;

    /**
     * The current orientation (respectively the direction of movement) of the robot. At the
     * starting point the orientation is always FORWARD.
     */
    private Orientation mOrientation;

    /**
     * The current ultrasonic distance from the next obstacle in front of the robot.
     */
    private double mDistance;

    /**
     * The current x-coordinate of the robot.
     */
    private double mXCoordinate = 0.0;

    /**
     * The current y-coordinate of the robot.
     */
    private double mYCoordinate = 0.0;

    /**
     * All the positions that the robot has reached so far.
     */
    private LinkedList<Position> mPositions = new LinkedList<>();

    /**
     * The Gson instance for serialization and deserialization.
     */
    private Gson mGson = new Gson();

    /**
     * The Type LinkedList<Position> that is needed for serialization and deserialization with Gson.
     */
    private Type mListType = new TypeToken<LinkedList<Position>>(){}.getType();

    public boolean isFilePresent() {
        String path = getApplicationContext().getFilesDir().getAbsolutePath() + "/" + FILENAME;
        File file = new File(path);
        return file.exists();
    }

    /**
     * Serializes the list that contains all the positions that the robot has reached so far with
     * Gson and writes the String to the file "positions.json" in the internal storage.
     */
    public void storePositions() {
        String json = mGson.toJson(mPositions, mListType);
        try {
            FileOutputStream fileOutputStream = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            if(json != null) {
                fileOutputStream.write(json.getBytes());
            }
            fileOutputStream.close();
        } catch(IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "File not found!", Toast.LENGTH_SHORT).show();
        }
    }

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
                if(mCheckingWall) {
                    mCheckingWall = false;
                }
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
        // Delay the execution of the code for 1 second, then check if there really is an obstacle
        mHandler.postDelayed(new Runnable() {
            public void run() {
                if(mSensor.getUltrasonicDistance().getDistance() <= 1300) {
                    if(mState == State.START) {
                        // This is the first obstacle that the robot has detected (the coordinates are 0.0)
                        Log.d(TAG, "State: " + mState +
                                " | Orientation: " + mOrientation +
                                " | Position: (" + mXCoordinate + " , " + mYCoordinate + ")");
                        mPositions.add(new Position(mXCoordinate, mYCoordinate, mOrientation));
                        mState = State.OBSTACLE;
                    } else if(mState == State.WALK) {
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
                    mBase.addCheckPoint(0, 0, (float) (Math.PI / 2));
                    // When the turn is finished, {@see #arrivedAtCheckpoint()} is called and the robot walks
                    // forward to the next wall
                }
            }
        }, 1000);
    }

    /**
     * Sets the x- or y-coordinate according to the current orientation of the robot.
     */
    public void updateCoordinates() {
        if(mState == State.WALK) {
            switch(mOrientation) {
                case FORWARD:
                    mXCoordinate += 1;
                    break;
                case BACKWARD:
                    mXCoordinate += -1;
                    break;
                case LEFT:
                    mYCoordinate += 1;
                    break;
                case RIGHT:
                    mYCoordinate += -1;
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
                " | Position: (" + mXCoordinate + " , " + mYCoordinate + ")");
        mPositions.add(new Position(mXCoordinate, mYCoordinate, mOrientation));
    }

    /**
     * Sets the orientation of the robot in relation to the origin of the coordinate system.
     * The new orientation of the robot depends on whether a left or a right turn is to be performed.
     *
     * @param direction indicates whether the turn to be performed is a left turn or a right turn
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startButton = (Button)findViewById(R.id.buttonStart);
        startButton.setOnClickListener(this);
        Button stopButton = (Button)findViewById(R.id.buttonStop);
        stopButton.setOnClickListener(this);

        initListeners();
    }

    private void initListeners() {
        mBaseBindStateListener = new ServiceBinder.BindStateListener() {
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
                mBase.setOnCheckPointArrivedListener(new CheckPointStateListener() {
                    @Override
                    public void onCheckPointArrived(final CheckPoint checkPoint, Pose2D realPose, boolean isLast) {
                        arrivedAtCheckpoint();
                    }

                    @Override
                    public void onCheckPointMiss(CheckPoint checkPoint, Pose2D realPose, boolean isLast, int reason) {}
                });
                mBase.setObstacleStateChangeListener(new ObstacleStateChangedListener() {
                    @Override
                    public void onObstacleStateChanged(int ObstacleAppearance) {
                        if(ObstacleAppearance == ObstacleStateChangedListener.OBSTACLE_APPEARED) {
                            obstacleDetected();
                        }
                    }
                });
            }

            @Override
            public void onUnbind(String reason) {}
        };

        mSensorBindStateListener = new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {}

            @Override
            public void onUnbind(String reason) {}
        };
    }

    private void bindServices() {
        mSensor = Sensor.getInstance();
        mSensor.bindService(this, mSensorBindStateListener);
        mBase = Base.getInstance();
        mBase.bindService(this, mBaseBindStateListener);
    }

    private void unbindServices() {
        mBase.unbindService();
        mSensor.unbindService();
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.buttonStart:
                startExploration();
                break;
            case R.id.buttonStop:
                mBase.clearCheckPointsAndStop();
                if(mPositions.size() == 0) {
                    if(!isFilePresent()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("No map could be created!");
                        builder.setMessage("Please run the exploration process first.");
                        builder.setPositiveButton("OK", null);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } else {
                        Intent intent = new Intent(this, NavigationActivity.class);
                        startActivity(intent);
                    }
                } else {
                    storePositions();
                    Intent intent = new Intent(this, NavigationActivity.class);
                    startActivity(intent);
                }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unbindServices();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindServices();
    }

    @Override
    public void onBackPressed() {
        if(mPress == 0) {
            Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show();
        }
        mPress++;
        if(mPress == 2) {
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
}