package com.tudresden.navigationrobot;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.segway.robot.algo.Pose2D;
import com.segway.robot.algo.minicontroller.CheckPoint;
import com.segway.robot.algo.minicontroller.CheckPointStateListener;
import com.segway.robot.algo.minicontroller.ObstacleStateChangedListener;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.perception.sensor.Sensor;

import java.util.LinkedList;

/**
 * This class handles the exploration process.
 */
public class Exploration {

    /**
     * The tag that is used for log messages.
     */
    private static final String TAG = "Exploration";

    /**
     * Indicates that a left turn is about to be performed.
     */
    private static final String LEFT_TURN = "left turn";

    /**
     * Indicates that a right turn is about to be performed.
     */
    private static final String RIGHT_TURN = "right turn";

    private static final int DELAY = 300;

    private static final float ULTRASONIC_MAX = 1.5f;

    /**
     * The distance that the robot keeps to obstacles.
     */
    private static final float OBSTACLE_AVOIDANCE_DISTANCE = 1f;

    private static final float WALL_DISTANCE = 0.8f;

    private static final float WALL_DISTANCE_CORRECTION = 0.1f;

    /**
     * The distance that the robot walks from one checkpoint to the next.
     */
    private static final float WALKING_DISTANCE = 0.5f;

    /**
     * The theta value for adding a checkpoint that makes the robot rotate 90° to the left.
     */
    private static final float LEFT_90 = (float) (Math.PI / 2);

    /**
     * The theta value for adding a checkpoint that makes the robot rotate 90° to the right.
     */
    private static final float RIGHT_90 = (float) -(Math.PI / 2);

    /**
     * A handler for delaying the execution of the code in #obstacleDetected() in order to reduce
     * the number of false positives in obstacle detection.
     */
    private Handler mHandler = new Handler();

    /**
     * The listener for the bind status of the base instance.
     */
    private ServiceBinder.BindStateListener mBaseBindStateListener;

    /**
     * The listener for the bind status of the sensor instance.
     */
    private ServiceBinder.BindStateListener mSensorBindStateListener;

    /**
     * The base instance that is used for controlling the robots movements.
     */
    private Base mBase = null;

    /**
     * The sensor instance that is used for actions related to the ultrasonic sensor.
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
    private double mDistanceFront = 0.0;

    /**
     * The distance from the wall to the right.
     */
    private double mDistanceWall = 0.0;

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

    private Context mContext;

    public Exploration(Context context) {
        this.mContext = context;
    }

    public LinkedList<Position> getPositions() {
        return mPositions;
    }

    public void startExploration() {
        mState = State.START;
        mOrientation = Orientation.FORWARD;
        mBase.clearCheckPointsAndStop();
        mBase.cleanOriginalPoint();
        Pose2D pos = mBase.getOdometryPose(-1);
        mBase.setOriginalPoint(pos);
        mBase.setUltrasonicObstacleAvoidanceEnabled(true);
        mBase.setUltrasonicObstacleAvoidanceDistance(OBSTACLE_AVOIDANCE_DISTANCE);
        // It is necessary to set 2 checkpoints in the beginning
        // With just one checkpoint, the OnCheckPointArrivedListener is not called correctly
        mBase.addCheckPoint(0, 0);
        mBase.addCheckPoint(0, 0);
    }

    public void stopExploration() {
        mBase.clearCheckPointsAndStop();
    }

    /**
     * Sets a new checkpoint depending on the current state of the robot.
     * The strategy for setting a new checkpoint is as follows:
     * In the initial phase of the exploration process (before the first wall is found) the new
     * checkpoint is set to make the robot walk forward (1 meter) in order to eventually reach the
     * first wall.
     * In case an obstacle was detected and the left turn was performed ({@see #obstacleDetected()}),
     * the new checkpoint needs to be set to make the robot walk forward (1 meter) in order to reach
     * the next wall.
     * After every meter that the robot walks in search of the next wall, it needs to rotate 90° to
     * the right in order to check if the wall on its right has ended.
     * If the wall has not ended, the robot rotates back 90° to the left and continues to walk
     * forward along the wall.
     * If the wall has ended, the robot does not turn back but instead follows the new wall.
     */
    public void arrivedAtCheckpoint() {
        mBase.clearCheckPointsAndStop();
        mBase.cleanOriginalPoint();
        Pose2D pos = mBase.getOdometryPose(-1);
        mBase.setOriginalPoint(pos);
        switch(mState) {
            case START:
                // As long as no wall has been found yet, keep walking forward (1 meter)
                mBase.addCheckPoint(WALKING_DISTANCE, 0);
                break;
            case WALKING:
                if(mCheckingWall) {
                    mCheckingWall = false;
                }
                updateCoordinates();
                mState = State.CHECKING_WALL;
                mDistanceFront = mSensor.getUltrasonicDistance().getDistance() / 1000; // convert mm to m
                updateOrientation(RIGHT_TURN);
                // After every meter, rotate 90° to the right to check the wall
                mBase.addCheckPoint(0, 0, RIGHT_90);
                break;
            case CHECKING_WALL:
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
                // In both cases the state of the robot is WALKING, but in the ObstacleStateChangeListener
                // different things need to happen.
                // Without setting the state to WALK in this switch case, the robot would not continue
                // walking if after the right turn no obstacle is found.
                mCheckingWall = true;
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        mDistanceWall = mSensor.getUltrasonicDistance().getDistance() / 1000;
                        if(mDistanceWall == ULTRASONIC_MAX) {
                            mState = State.CORNER_LEFT;
                            updateOrientation(LEFT_TURN);
                            mBase.addCheckPoint(0, 0, LEFT_90);
                        } else {
                            mState = State.WALKING;
                            // Walk forward (1 meter)
                            mBase.addCheckPoint(WALKING_DISTANCE, 0);
                        }
                    }
                }, DELAY);
                // In case the robot is further away from the wall than OBSTACLE_AVOIDANCE_DISTANCE
                // this code makes it walk towards the wall again. Because the checkpoint is not
                // reached (because normally the robot hasn't walked further away from the wall than
                // a meter), this doesn't affect the setting of the coordinates. This is because
                // the coordinates are only updated in arrivedAtCheckpoint() which of course isn't
                // called if the checkpoint isn't reached.
                break;
            case OBSTACLE_DETECTED:
                mDistanceFront = mSensor.getUltrasonicDistance().getDistance() / 1000; // convert mm to m
                if(mDistanceFront <= OBSTACLE_AVOIDANCE_DISTANCE) {
                    // Obstacle right after obstacle --> Turn left
                    updateOrientation(LEFT_TURN);
                    mBase.addCheckPoint(0, 0, LEFT_90);
                } else {
                    mState = State.WALKING;
                    if(mDistanceWall <= WALL_DISTANCE) {
                        // Increase the distance from the wall to the right
                        mBase.addCheckPoint(WALKING_DISTANCE, WALL_DISTANCE_CORRECTION);
                    } else {
                        // Decrease the distance from the wall to the right
                        mBase.addCheckPoint(WALKING_DISTANCE, 0);
                    }
                }
                break;
            case CORNER_LEFT:
                mDistanceFront = mSensor.getUltrasonicDistance().getDistance() / 1000; // convert mm to m
                mState = State.CORNER_FORWARD;
                mBase.addCheckPoint(WALKING_DISTANCE, 0);
                break;
            case CORNER_FORWARD:
                mDistanceFront = mSensor.getUltrasonicDistance().getDistance() / 1000; // convert mm to m
                updateCoordinates();
                mState = State.CORNER_RIGHT;
                updateOrientation(RIGHT_TURN);
                mBase.addCheckPoint(0, 0, RIGHT_90);
                break;
            case CORNER_RIGHT:
                mDistanceFront = mSensor.getUltrasonicDistance().getDistance() / 1000; // convert mm to m
                // In case an obstacle appears
                mDistanceWall = mDistanceFront;
                mState = State.CORNER_DONE;
                // TODO: Find best correction value
                mBase.addCheckPoint(WALKING_DISTANCE * 2, -0.1f);
                break;
            case CORNER_DONE:
                mDistanceFront = mSensor.getUltrasonicDistance().getDistance() / 1000; // convert mm to m
                updateCoordinates();
                mState = State.START;
                updateOrientation(RIGHT_TURN);
                mBase.addCheckPoint(0, 0, RIGHT_90);
            default:
                // All possible cases are handled above
        }
    }

    /**
     * Sets a new checkpoint to make the robot rotate 90° to the left in case an obstacle was detected.
     */
    public void obstacleDetected() {
        // Delay the execution of the code for 1 second, then check if there really is an obstacle
        mHandler.postDelayed(new Runnable() {
            public void run() {
                if(mSensor.getUltrasonicDistance().getDistance() / 1000 <= OBSTACLE_AVOIDANCE_DISTANCE) {
                    // The robot detects an obstacle before it reaches the current checkpoint. When an obstacle
                    // is detected, a new checkpoint is set for the left turn but the robot still tries to reach
                    // the last checkpoint first. That checkpoint obviously can't be reached, because there is an
                    // obstacle in front of the robot, so the robot just stops walking completely.
                    // Therefore the last checkpoint needs to be deleted before the new one is set.
                    mBase.clearCheckPointsAndStop();
                    mBase.cleanOriginalPoint();
                    Pose2D pos = mBase.getOdometryPose(-1);
                    mBase.setOriginalPoint(pos);
                    if(mState == State.START) {
                        // This is the first obstacle that the robot has detected (the coordinates are 0.0)
                        Log.d(TAG, "State: " + mState +
                                " | Orientation: " + mOrientation +
                                " | Position: (" + mXCoordinate + " , " + mYCoordinate + ")");
                        mPositions.add(new Position(mXCoordinate, mYCoordinate, mOrientation));
                        mState = State.OBSTACLE_DETECTED;
                        mDistanceWall = mSensor.getUltrasonicDistance().getDistance() / 1000;
                        mState = State.OBSTACLE_DETECTED;
                        updateOrientation(LEFT_TURN);
                        mBase.addCheckPoint(0, 0, LEFT_90);
                        // When the turn is finished, {@see #arrivedAtCheckpoint()} is called and the robot walks
                        // forward to the next wall
                    } else if(mState == State.WALKING) {
                        mState = State.OBSTACLE_DETECTED;
                        // Concerning mCheckingWall see explanation in arrivedAtCheckpoint() (case CHECKING_WALL)
                        if(!mCheckingWall) {
                            updateCoordinates();
                        } else {
                            mCheckingWall = false;
                        }
                        updateOrientation(LEFT_TURN);
                        mBase.addCheckPoint(0, 0, LEFT_90);
                        // When the turn is finished, {@see #arrivedAtCheckpoint()} is called and the robot walks
                        // forward to the next wall
                    } else if(mState == State.CORNER_FORWARD) {
                        mState = State.CORNER_RIGHT;
                        updateOrientation(RIGHT_TURN);
                        mBase.addCheckPoint(0, 0, RIGHT_90);
                    } else if(mState == State.CORNER_RIGHT) {
                        mState = State.OBSTACLE_DETECTED;
                        updateOrientation(LEFT_TURN);
                        mBase.addCheckPoint(0, 0, LEFT_90);
                    } else if(mState == State.CORNER_DONE) {
                        mState = State.OBSTACLE_DETECTED;
                        updateCoordinates();
                        updateOrientation(LEFT_TURN);
                        mBase.addCheckPoint(0, 0, LEFT_90);
                    } else if(mState == State.OBSTACLE_DETECTED) {
                        mState = State.OBSTACLE_DETECTED;
                        updateOrientation(LEFT_TURN);
                        mBase.addCheckPoint(0, 0, LEFT_90);
                    }
                }
            }
        }, DELAY);
    }

    /**
     * Sets the x- or y-coordinate according to the current orientation of the robot.
     */
    public void updateCoordinates() {
        if(mState == State.WALKING || mState == State.CORNER_FORWARD) {
            switch(mOrientation) {
                case FORWARD:
                    mXCoordinate += WALKING_DISTANCE;
                    break;
                case BACKWARD:
                    mXCoordinate -= WALKING_DISTANCE;
                    break;
                case LEFT:
                    mYCoordinate += WALKING_DISTANCE;
                    break;
                case RIGHT:
                    mYCoordinate -= WALKING_DISTANCE;
                    break;
                default:
                    // All possible cases are handled above
            }
        } else if(mState == State.OBSTACLE_DETECTED) {
            switch(mOrientation) {
                case FORWARD:
                    mXCoordinate += (mDistanceFront - mSensor.getUltrasonicDistance().getDistance() / 1000); // convert mm to m
                    break;
                case BACKWARD:
                    mXCoordinate -= (mDistanceFront - mSensor.getUltrasonicDistance().getDistance() / 1000); // convert mm to m
                    break;
                case LEFT:
                    mYCoordinate += (mDistanceFront - mSensor.getUltrasonicDistance().getDistance() / 1000); // convert mm to m
                    break;
                case RIGHT:
                    mYCoordinate -= (mDistanceFront - mSensor.getUltrasonicDistance().getDistance() / 1000); // convert mm to m
                    break;
                default:
                    // All possible cases are handled above
            }
        } else if(mState == State.CORNER_DONE) {
            switch (mOrientation) {
                case FORWARD:
                    mXCoordinate += WALKING_DISTANCE * 2.5;
                    break;
                case BACKWARD:
                    mXCoordinate -= WALKING_DISTANCE * 2.5;
                    break;
                case LEFT:
                    mYCoordinate += WALKING_DISTANCE * 2.5;
                    break;
                case RIGHT:
                    mYCoordinate -= WALKING_DISTANCE * 2.5;
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
     * Sets the orientation of the robot. The new orientation of the robot depends on whether a left
     * or a right turn is to be performed.
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

    /**
     * Initializes the listeners for the base instance and the sensor instance. For the base
     * instance the Visual Localization System is started, the CheckPointStateListener is registered
     * and the ObstacleStateChangeListener is registered.
     */
    public void initListeners() {
        mBaseBindStateListener = new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                mBase.setControlMode(Base.CONTROL_MODE_NAVIGATION);
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

    /**
     * Binds the base instance and the sensor instance to the respective services.
     */
    public void bindServices() {
        mSensor = Sensor.getInstance();
        mSensor.bindService(mContext, mSensorBindStateListener);
        mBase = Base.getInstance();
        mBase.bindService(mContext, mBaseBindStateListener);
    }

    /**
     * Binds the base instance and the sensor instance to the respective services.
     */
    public void unbindServices() {
        mBase.unbindService();
        mSensor.unbindService();
    }

}
