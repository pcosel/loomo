package com.tudresden.navigationrobot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.segway.robot.algo.Pose2D;
import com.segway.robot.algo.PoseVLS;
import com.segway.robot.algo.minicontroller.CheckPoint;
import com.segway.robot.algo.minicontroller.CheckPointStateListener;
import com.segway.robot.algo.minicontroller.ObstacleStateChangedListener;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.locomotion.sbv.StartVLSListener;
import com.segway.robot.sdk.perception.sensor.Sensor;

import java.text.DecimalFormat;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private Draw mDraw;
    private FrameLayout mFrameLayout;
    private TextView mTextView;
    private int press = 0;
    private float pixelToMeter = 0.01f;
    private float lastX = 0;
    private float lastY = 0;
    private float x = 0;
    private float y = 0;
    private Base mBase = null;
    private Sensor mSensor = null;

    /**
     * The current state of the robot.
     */
    private State mState;

    /**
     * Starts the exploration process by initialising the obstacle avoidance functionality and
     * setting the first checkpoints.
     */
    public void startExploration() {
        mState = State.START;
        mBase.cleanOriginalPoint();
        PoseVLS pos = mBase.getVLSPose(-1);
        mBase.setOriginalPoint(pos);
        mBase.setUltrasonicObstacleAvoidanceEnabled(true);
        // Keep 1 meter distance from obstacles
        mBase.setUltrasonicObstacleAvoidanceDistance(1.3f);
        // It is necessary to set 2 checkpoints in the beginning
        // With just one checkpoint, the OnCheckPointArrivedListener is not called correctly
        mBase.addCheckPoint(0, 0);
        mBase.addCheckPoint(0, 0);
    }

    /**
     * Sets a new checkpoint with respect to the the current state of the robot.
     * The strategy for setting a new checkpoint is as follows:
     * In the initial phase of the exploration process (before the first wall is found) the new
     * checkpoint is set to make the robot walk forward (1 meter) in order to eventually reach the
     * first wall.
     * In case an obstacle was detected and the left turn was performed {@see #obstacleDetected()},
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
        switch (mState) {
            case START:
                // As long as no wall has been found yet, keep walking forward (1 meter)
                mBase.addCheckPoint(1f, 0);
                break;
            case WALK:
                mState = State.CHECK;
                // After every meter, turn right (90 degrees)
                mBase.addCheckPoint(0, 0, (float) (-Math.PI / 2));
                break;
            case CHECK:
                // If an obstacle is detected after the right turn, the ObstacleStateChangeListener
                // is triggered and the robot performs a left turn so that it then looks forward again.
                // In case after the right turn no obstacle was detected, that means that the wall
                // next to the robot has ended and it needs to walk forward to follow the new wall.
                mState = State.WALK;
                // Walk forward (1 meter)
                mBase.addCheckPoint(1f, 0);
                break;
            case OBSTACLE:
                mState = State.WALK;
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
        mState = State.OBSTACLE;
        // The robot detects an obstacle before it reaches the current checkpoint. When an obstacle
        // is detected, a new checkpoint is set for the left turn but the robot still tries to reach
        // the last checkpoint first. But that checkpoint can't be reached, because there is an
        // obstacle in front of the robot, so the robot just stops walking completely.
        // Therefore the last checkpoint needs to be deleted before the new one is set.
        mBase.clearCheckPointsAndStop();
        mBase.cleanOriginalPoint();
        PoseVLS pos = mBase.getVLSPose(-1);
        mBase.setOriginalPoint(pos);
        // Turn left (90 degrees)
        mBase.addCheckPoint(0, 0, (float) (Math.PI / 2));
        // When the turn is finished, {@see #arrivedAtCheckpoint()} is called and the robot walks
        // forward to the next wall
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        initView();
        initBase();
        initSensor();
        initCanvas();
        // show scale hint
        showScale();
    }

    private void showScale() {
        DecimalFormat decimalFormat = new DecimalFormat(".00");
        mTextView.setText(decimalFormat.format(pixelToMeter * mDraw.getCanvasWidth()) + " X " + decimalFormat.format(pixelToMeter * mDraw.getCanvasHeight()) + " m");
    }

    // init canvas for drawing
    private void initCanvas() {
        Point3D pt = getWindowSize();
        int gridWidthInPixel = (int) (1 / pixelToMeter);
        mDraw = new Draw(MainActivity.this, pt.width, pt.height, pt.density, gridWidthInPixel);
        mFrameLayout.addView(mDraw);
        System.out.println("Init map height: " + mDraw.getMapHeight());
        lastX = mDraw.getMapWidth() / 2;
        lastY = mDraw.getMapHeight() / 2;
    }

    private Point3D getWindowSize() {
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        System.out.println("The width calculated from Layout size is: " + mFrameLayout.getWidth());
        System.out.println("The measured width calculated from Layout size is: " + mFrameLayout.getMeasuredWidth());
        System.out.println("The height calculated from Layout size is: " + mFrameLayout.getHeight());
        System.out.println("The measured height calculated from Layout size is: " + mFrameLayout.getMeasuredHeight());
        int width = metric.widthPixels;
        int height = metric.heightPixels - 150;
        float density = metric.density;
        return new Point3D(width, height, density);
    }

    private void initView() {
        Button mResetButton = (Button) findViewById(R.id.btnReset);
        mResetButton.setOnClickListener(this);

        Button startButton = (Button) findViewById(R.id.button_start);
        startButton.setOnClickListener(this);

        Button stopButton = (Button) findViewById(R.id.button_stop);
        stopButton.setOnClickListener(this);

        Button mScaleButton = (Button) findViewById(R.id.btnScale);
        mScaleButton.setOnClickListener(this);

        mTextView = (TextView) findViewById(R.id.tvScale);

        mFrameLayout = (ZoomLayout) findViewById(R.id.flMap);
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
            public void onUnbind(String reason) {
            }
        });

        mBase.setOnCheckPointArrivedListener(new CheckPointStateListener() {
            @Override
            public void onCheckPointArrived(final CheckPoint checkPoint, Pose2D realPose, boolean isLast) {
                System.out.println("realPose.getX() is: " + realPose.getX());
                System.out.println("realPose.getY() is: " + realPose.getY());
                x = lastX + (float) realPose.getX();
                y = lastY + (float) realPose.getY();
                mDraw.drawLine(lastX, lastY, x, y);
                lastX = x;
                lastY = y;
                arrivedAtCheckpoint();
            }

            @Override
            public void onCheckPointMiss(CheckPoint checkPoint, Pose2D realPose, boolean isLast, int reason) {}
        });

        mBase.setObstacleStateChangeListener(new ObstacleStateChangedListener() {
            @Override
            public void onObstacleStateChanged(int ObstacleAppearance) {
                if (ObstacleAppearance == ObstacleStateChangedListener.OBSTACLE_APPEARED) {
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
        switch (v.getId()) {
            case R.id.btnReset:
                // reset paint to empty
                mDraw.resetPaint();
//                lastX = 0;
//                lastY = 50;
                lastX = mDraw.getMapWidth() / 2;
                lastY = mDraw.getMapHeight() / 2;
                x = 0;
                y = 0;
                break;
            case R.id.button_start:
                startExploration();
                break;
            case R.id.button_stop:
                mBase.clearCheckPointsAndStop();
                break;
            case R.id.btnScale:
                // modify area scale
                final EditText et = new EditText(this);
                new AlertDialog.Builder(this).setTitle("Input width").setIcon(android.R.drawable.ic_dialog_info).setView(et).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        float fWidth = Float.parseFloat(et.getText().toString());
                        pixelToMeter = fWidth / (float) mDraw.getCanvasWidth();
                        showScale();
                        initCanvas();
                    }
                }).setNegativeButton("CANCEL", null).show();
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
        mBase.unbindService();
        mSensor.unbindService();
    }

    private class Point3D {
        public int width;
        public int height;
        public float density;

        public Point3D(int width, int height, float density) {
            this.width = width;
            this.height = height;
            this.density = density;
        }
    }
}