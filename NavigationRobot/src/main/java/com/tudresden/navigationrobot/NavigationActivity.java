package com.tudresden.navigationrobot;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.segway.robot.algo.Pose2D;
import com.segway.robot.algo.PoseVLS;
import com.segway.robot.algo.minicontroller.CheckPoint;
import com.segway.robot.algo.minicontroller.CheckPointStateListener;
import com.segway.robot.algo.minicontroller.ObstacleStateChangedListener;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.locomotion.sbv.StartVLSListener;
import com.segway.robot.sdk.perception.sensor.Sensor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedList;

/**
 * This Activity handles the navigation process.
 *
 * @author Nadja Konrad
 */
public class NavigationActivity extends Activity {

    /**
     * The tag that is used for log messages.
     */
    private static final String TAG = "NavigationActivity";

    /**
     * The filename of the file that the positions are stored in.
     */
    private static final String FILENAME = "positions.json";

    /**
     * Indicates that the list of positions should be traversed in sequential order.
     */
    private static final String FORWARD = "forward";

    /**
     * Indicates that the list of positions should be traversed in reverse sequential order.
     */
    private static final String BACKWARD = "backward";

    /**
     * The listener for the bind status of the base instance.
     */
    private ServiceBinder.BindStateListener mBaseBindStateListener;

    /**
     * The listener for the bind status of the sensor instance.
     */
    private ServiceBinder.BindStateListener mSensorBindStateListener;

    /**
     * The Base instance that is used for controlling the robots movements.
     */
    private Base mBase = null;

    /**
     * The Sensor instance that is used for actions related to the ultrasonic sensor.
     */
    private Sensor mSensor = null;

    /**
     * The padding for the map.
     */
    private static final int PADDING = 30;

    /**
     * The layout that contains the map.
     */
    private FrameLayout mFrameLayout;

    /**
     * The position that marks the starting point on the screen. Not the real coordinates of the
     * starting point!
     */
    private Position mStartPosition;

    /**
     * The position that the user selected on the screen (real coordinates).
     */
    private Position mTargetPosition;

    /**
     * The position from the list of known positions that is closest to the target position (real
     * coordinates).
     */
    private Position mClosestKnownPosition;

    /**
     * The current position of the robot (real coordinates).
     */
    private Position mCurrentPosition;

    /**
     * Indicates whether the list of positions should be traversed in sequential order or in reverse
     * sequential order.
     */
    private String mDirection;

    /**
     * The distance between the points on the screen (if assumed that the points are evenly
     * distributed across the screen).
     */
    private double mDistanceBetweenPoints;

    /**
     * The Gson instance for serialization and deserialization.
     */
    private Gson mGson = new Gson();

    /**
     * All the positions that the robot has reached during the exploration phase.
     */
    private LinkedList<Position> mInputPositions = new LinkedList<>();

    /**
     * All the positions that the robot has reached during the exploration phase converted to
     * positions that can be displayed on the screen.
     */
    private LinkedList<Position> mScreenPositions = new LinkedList<>();

    /**
     * The Type LinkedList<Position> that is needed for serialization and deserialization with Gson.
     */
    private Type mListType = new TypeToken<LinkedList<Position>>(){}.getType();

    /**
     * Checks whether the file with the filename positions.json already exists.
     * @return true if the file already exists; false otherwise
     */
    public boolean fileExists() {
        String path = getApplicationContext().getFilesDir().getAbsolutePath() + "/" + FILENAME;
        File file = new File(path);
        return file.exists();
    }

    /**
     * Reads the list of positions from the previous exploration from the file positions.json.
     * @return a Json String representation of the list of positions from the previous exploration
     */
    public String read() {
        try {
            FileInputStream fileInputStream = getApplicationContext().openFileInput(FILENAME);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch(IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "File not found!", Toast.LENGTH_SHORT).show();
            return "Error!";
        }
    }

    /**
     * Converts all the positions that the robot has reached during the exploration phase to
     * positions that can be displayed on the screen.
     * @param width the width of the layout that contains the map
     * @param height the height of the layout that contains the map
     */
    public void calculateScreenPositions(int width, int height) {
        double greatestPosX = 0.0;
        double greatestNegX = 0.0;
        double greatestPosY = 0.0;
        double greatestNegY = 0.0;

        for(Position p : mInputPositions) {
            double x = p.getX();
            double y = p.getY();
            if(Double.compare(x, 0.0) > 0) {
                if(Double.compare(x, greatestPosX) > 0) {
                    greatestPosX = x;
                }
            } else {
                if(Double.compare(Math.abs(x), greatestNegX) > 0) {
                    greatestNegX = Math.abs(x);
                }
            }
            if(Double.compare(y, 0.0) > 0) {
                if(Double.compare(y, greatestPosY) > 0) {
                    greatestPosY = y;
                }
            } else {
                if(Double.compare(Math.abs(y), greatestNegY) > 0) {
                    greatestNegY = Math.abs(y);
                }
            }
        }

        // Find out how many points need to fit in the width and height
        double sumX = greatestPosX + greatestNegX;
        double sumY = greatestPosY + greatestNegY;

        // Choose the distance between the points as big as possible in order to make the map as big
        // as possible but also make sure that all the points fit on the screen
        if(Double.compare(sumX, 0.0) == 0 && Double.compare(sumY, 0.0) == 0) {
            // Only the starting point was in the list
            mDistanceBetweenPoints = 0.0;
        } else if(Double.compare(sumX, 0.0) == 0) {
            // No vertical space needed
            mDistanceBetweenPoints = (width - PADDING) / sumY;
        } else if(Double.compare(sumY, 0.0) == 0) {
            // No horizontal space needed
            mDistanceBetweenPoints = (height - PADDING) / sumX;
        } else if(Double.compare((height - PADDING) / sumX, (width - PADDING) / sumY) > 0) {
            mDistanceBetweenPoints = (width - PADDING) / sumY;
        } else {
            mDistanceBetweenPoints = (height - PADDING) / sumX;
        }

        // Calculate the screen position of the starting point (0,0)
        double startX = mDistanceBetweenPoints * greatestPosY + (PADDING / 2);
        double startY = mDistanceBetweenPoints * greatestPosX + (PADDING / 2);

        mStartPosition = new Position(startX, startY);

        for(Position p : mInputPositions) {
            double inX = p.getX();
            double inY = p.getY();
            double outX;
            double outY;
            if(Double.compare(mDistanceBetweenPoints, 0.0) == 0) {
                // Only the starting point was in the list so it needs to be displayed in the center
                // of the screen
                outX = width / 2;
                outY = height / 2;
            } else {
                // Convert the coordinates of the real positions to the coordinates on the screen.
                // Be aware that the coordinate system of the robot is exactly the opposite of the
                // coordinate system of the screen:
                // Robot: Positive x --> forward, positive y --> left
                // Screen: Positive x --> width, positive y --> height (origin in the left upper
                // corner of the screen
                if(Double.compare(inX, 0.0) > 0) {
                    outY = startY - (inX * mDistanceBetweenPoints);
                } else {
                    outY = startY + (Math.abs(inX) * mDistanceBetweenPoints);
                }
                if(Double.compare(inY, 0.0) > 0) {
                    outX = startX - (inY * mDistanceBetweenPoints);
                } else {
                    outX = startX + (Math.abs(inY) * mDistanceBetweenPoints);
                }
            }
            mScreenPositions.add(new Position(outX, outY));
        }
    }

    /**
     * Converts a position on the screen to a real position in the room. Inverts the calculations of
     * {@see #calculateScreenPositions(int width, int height)}
     * @param x the x-coordinate of the selected position on the screen
     * @param y the y-coordinate of the selected position on the screen
     * @return the real position in the room
     */
    public Position calculateRealPosition(float x, float y) {
        double targetX = 0.0;
        double targetY = 0.0;

        // If only the starting point is displayed on the map, all selected positions around the
        // starting point should be treated as point (0,0) because in this case no navigation is
        // possible
        if(Double.compare(mDistanceBetweenPoints, 0.0) != 0) {
            if(Double.compare(x, mStartPosition.getX()) == 0) {
                targetY = 0;
            } else if(Double.compare(x, mStartPosition.getX()) < 0) {
                targetY = -((x - mStartPosition.getX()) / mDistanceBetweenPoints);
            } else {
                targetY = (mStartPosition.getX() - x) / mDistanceBetweenPoints;
            }
            if(Double.compare(y, mStartPosition.getY()) == 0) {
                targetX = 0;
            } else if(Double.compare(y, mStartPosition.getY()) < 0) {
                targetX = -((y - mStartPosition.getY()) / mDistanceBetweenPoints);
            } else {
                targetX = (mStartPosition.getY() - y) / mDistanceBetweenPoints;
            }
        }

        // Set decimal separator as '.' instead of ','
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
        dfs.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("#.##", dfs);
        targetX = Double.parseDouble(df.format(targetX));
        targetY = Double.parseDouble(df.format(targetY));

        mTargetPosition = new Position(targetX, targetY);
        return mTargetPosition;
    }

    /**
     * Finds the position in the list of known positions that is closest to the target position.
     */
    public void findClosestKnownPosition() {
        double distance = Double.MAX_VALUE;
        double targetX = mTargetPosition.getX();
        double targetY = mTargetPosition.getY();
        for(Position p : mInputPositions) {
            // Calculate euclidian distance for every position
            double x = p.getX() - targetX;
            double y = p.getY() - targetY;
            double result = Math.sqrt(x * x + y * y);
            if(result < distance) {
                distance = result;
                mClosestKnownPosition = p;
            }
        }
    }

    /**
     * Calculates whether the list of positions should be traversed in sequential order or in reverse
     * sequential order for shortest navigation.
     */
    public void findShortestPath() {
        int indexCurrentPosition = mInputPositions.indexOf(mCurrentPosition);
        int indexClosestKnownPosition = mInputPositions.indexOf(mClosestKnownPosition);
        int distanceForward;
        int distanceBackward;
        if(indexCurrentPosition < indexClosestKnownPosition) {
            distanceForward = indexClosestKnownPosition - indexCurrentPosition;
            distanceBackward = (mInputPositions.size() - indexClosestKnownPosition) + indexCurrentPosition;
        } else {
            distanceBackward = indexCurrentPosition - indexClosestKnownPosition;
            distanceForward = (mInputPositions.size() - indexCurrentPosition) + indexClosestKnownPosition;
        }
        if(distanceForward < distanceBackward) {
            mDirection = FORWARD;
        } else {
            mDirection = BACKWARD;
        }
    }

    /**
     * Finds the next position and tells the robot to go there.
     */
    public void goToNextPosition() {
        // TODO: Implement Navigation
    }

    /**
     * Starts the navigation process by finding the closest known position to the target position
     * and the shortest path. Also initializes the obstacle avoidance functionality and tells the
     * robot to go to the first position.
     */
    public void startNavigation(View view) {
        if(mTargetPosition == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("No destination selected!");
            builder.setMessage("Please tap a point on the map to select your destination.");
            builder.setPositiveButton("OK", null);
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            findClosestKnownPosition();
            findShortestPath();
            mBase.setUltrasonicObstacleAvoidanceEnabled(true);
            mBase.setUltrasonicObstacleAvoidanceDistance(1.3f);
            goToNextPosition();
        }
    }

    /**
     * Initializes the listeners for the base instance and the sensor instance. For the base
     * instance the Visual Localization System is started, the CheckPointStateListener is registered
     * and the ObstacleStateChangeListener is registered.
     */
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
                        goToNextPosition();
                    }

                    @Override
                    public void onCheckPointMiss(CheckPoint checkPoint, Pose2D realPose, boolean isLast, int reason) {}
                });
                mBase.setObstacleStateChangeListener(new ObstacleStateChangedListener() {
                    @Override
                    public void onObstacleStateChanged(int ObstacleAppearance) {
                        if(ObstacleAppearance == ObstacleStateChangedListener.OBSTACLE_APPEARED) {
                            // TODO: Handle obstacle detection
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
     * Retrieves the width and the height of the layout that contains the map as soon as said layout
     * is created.
     * The listener is needed because problems occur if the width and the height of the layout are
     * queried before the UI has been successfully created.
     */
    private void initFrameLayoutListener() {
        final FrameLayout layout = (FrameLayout)findViewById(R.id.frameLayout);
        layout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                int width = layout.getWidth();
                int height = layout.getHeight();

                calculateScreenPositions(width, height);

                mFrameLayout.addView(new MapView(NavigationActivity.this, mScreenPositions, width, height));
            }
        });
    }

    /**
     * Binds the base instance and the sensor instance to the respective services.
     */
    private void bindServices() {
        mSensor = Sensor.getInstance();
        mSensor.bindService(this, mSensorBindStateListener);
        mBase = Base.getInstance();
        mBase.bindService(this, mBaseBindStateListener);
    }

    /**
     * Unbinds the base instance and the sensor instance from the respective services.
     */
    private void unbindServices() {
        mBase.unbindService();
        mSensor.unbindService();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mFrameLayout = (FrameLayout)findViewById(R.id.frameLayout);

        if(fileExists()) {
            mInputPositions = mGson.fromJson(read(), mListType);
            // When the exploration is finished, the robot stands on the position that it last
            // reached
            mCurrentPosition = mInputPositions.getLast();
        }

        initFrameLayoutListener();
        initListeners();
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
}
