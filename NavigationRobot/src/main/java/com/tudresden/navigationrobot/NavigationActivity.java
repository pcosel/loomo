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

public class NavigationActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static final String FILENAME = "positions.json";
    private static final String FORWARD = "forward";
    private static final String BACKWARD = "backward";

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

    private static final int PADDING = 30;

    private FrameLayout mFrameLayout;

    private Position mStartPosition;

    private Position mTargetPosition;

    private Position mClosestKnownPosition;

    private Position mCurrentPosition;

    private String mDirection;

    private double mDistanceBetweenPoints;

    private Gson mGson = new Gson();

    private LinkedList<Position> mInputPositions = new LinkedList<>();

    private LinkedList<Position> mScreenPositions = new LinkedList<>();

    private Type mListType = new TypeToken<LinkedList<Position>>(){}.getType();

    DecimalFormatSymbols mDecimalFormatSymbols = DecimalFormatSymbols.getInstance();

    public boolean isFilePresent() {
        String path = getApplicationContext().getFilesDir().getAbsolutePath() + "/" + FILENAME;
        File file = new File(path);
        return file.exists();
    }

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

        double sumX = greatestPosX + greatestNegX;
        double sumY = greatestPosY + greatestNegY;

        if(Double.compare(sumX, 0.0) == 0 && Double.compare(sumY, 0.0) == 0) {
            mDistanceBetweenPoints = 0.0;
        } else if(Double.compare(sumX, 0.0) == 0) {
            mDistanceBetweenPoints = (width - PADDING) / sumY;
        } else if(Double.compare(sumY, 0.0) == 0) {
            mDistanceBetweenPoints = (height - PADDING) / sumX;
        } else if(Double.compare((height - PADDING) / sumX, (width - PADDING) / sumY) > 0) {
            mDistanceBetweenPoints = (width - PADDING) / sumY;
        } else {
            mDistanceBetweenPoints = (height - PADDING) / sumX;
        }

        double startX = mDistanceBetweenPoints * greatestPosY + (PADDING / 2);
        double startY = mDistanceBetweenPoints * greatestPosX + (PADDING / 2);

        mStartPosition = new Position(startX, startY);

        for(Position p : mInputPositions) {
            double inX = p.getX();
            double inY = p.getY();
            double outX;
            double outY;
            if(Double.compare(mDistanceBetweenPoints, 0.0) == 0) {
                outX = width / 2;
                outY = height / 2;
            } else {
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

    public Position calculateRealPosition(float x, float y) {
        double targetX = 0.0;
        double targetY = 0.0;

        if (Double.compare(mDistanceBetweenPoints, 0.0) != 0) {
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

        mDecimalFormatSymbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("#.##", mDecimalFormatSymbols);
        targetX = Double.parseDouble(df.format(targetX));
        targetY = Double.parseDouble(df.format(targetY));

        mTargetPosition = new Position(targetX, targetY);
        return mTargetPosition;
    }

    public void findClosestKnownPosition() {
        double distance = Double.MAX_VALUE;
        double targetX = mTargetPosition.getX();
        double targetY = mTargetPosition.getY();
        for(Position p : mInputPositions) {
            double x = p.getX() - targetX;
            double y = p.getY() - targetY;
            double result = Math.sqrt(x * x + y * y);
            if(result < distance) {
                distance = result;
                mClosestKnownPosition = p;
            }
        }
    }

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

    public void goToTargetPosition() {
        //TODO: Implement Navigation
    }

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
            goToTargetPosition();
        }
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
                        // TODO: Go to next checkpoint
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mFrameLayout = (FrameLayout)findViewById(R.id.frameLayout);

        if(isFilePresent()) {
            mInputPositions = mGson.fromJson(read(), mListType);
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
