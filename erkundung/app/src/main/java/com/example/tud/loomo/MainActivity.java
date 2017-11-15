package com.example.tud.loomo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.vision.frame.Frame;
import com.segway.robot.sdk.vision.stream.StreamInfo;
import com.segway.robot.sdk.vision.stream.StreamType;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
    private static final String TAG = "BaseFragment";

    Base mBase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: ");
        mBase = Base.getInstance();
        mBase.bindService(getApplicationContext(), new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {

            }

            @Override
            public void onUnbind(String reason) {
            }
        });
        Button button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mBase.setControlMode(Base.CONTROL_MODE_RAW);
                switch (view.getId()) {
                    case R.id.set_linear_speed:
                        isStop = false;
                        new Thread() {
                            @Override
                            public void run() {
                                float startX = mBase.getOdometryPose(-1).getX();
                                float startY = mBase.getOdometryPose(-1).getY();
//                        while ((getDistance(startX, startY, mBase.getOdometryPose(-1).getX(), mBase.getOdometryPose(-1).getY()) < 1) && !isStop) {
                                Log.d(TAG, "run: " + getDistance(startX, startY, mBase.getOdometryPose(-1).getX(), mBase.getOdometryPose(-1).getY()));
                                double Distance = Double.parseDouble(mBase.getUltrasonicDistance());

                                while(mBase.getUltrasonicDistance().getDistance() > 1500)
                                {
                                    mBase.setControlMode(Base.CONTROL_MODE_RAW);
                                    // the unit is mm.
                                    if (mBase.getUltrasonicDistance().getDistance() > 1500) {
                                        // set robot base linearVelocity, unit is rad/s, rand is -PI ~ PI.

                                        ArrayList<Pose2D> position;
                                        position.add(mBase.getOdometryPose(System.currentTimeMillseconds() * 1000));
                                        mBase.setLinearVelocity(1);
                                    } else {
                                        //turn left
                                        mBase.setControlMode(Base.CONTROL_MODE_NAVIGATION);
                                        public CheckPoint addCheckPoint(0.0, 1.0, Math.PI/2);
                                    }
                                }
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
//                        }
                                mBase.setLinearVelocity(0);
                            }
                        }.start();
                        break;
                    case R.id.set_v_speed:
                        isStop = false;
                        new Thread() {
                            @Override
                            public void run() {
                                float start = mBase.getOdometryPose(-1).getTheta();
//                        while ((Math.abs(mBase.getOdometryPose(-1).getTheta() - start) < Math.PI) && !isStop) {
                                if (!Util.isEditTextEmpty(mAngularVelocity)) {
                                    // set robot base ANGULARVelocity, unit is rad/s, rand is -PI ~ PI.
                                    mBase.setAngularVelocity(Util.getEditTextFloatValue(mAngularVelocity));
                                } else {
                                    mBase.setAngularVelocity(1);
                                }
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
//                        }
                                // stop
                                mBase.setAngularVelocity(0);
                            }
                        }.start();
                        break;
                    case R.id.stop:
                        // stop robot
                        isStop = true;
                        mBase.stop();
                        break;
                    default:
                        break;
                }
            }
        });
        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());

        //TODO
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
