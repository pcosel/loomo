package com.example.tud.loomo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.algo.Pose2D;

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
                mBase.setControlMode(Base.CONTROL_MODE_RAW);
                        new Thread() {
                            @Override
                            public void run() {
                                float startX = mBase.getOdometryPose(-1).getX();
                                float startY = mBase.getOdometryPose(-1).getY();

                                while(mBase.getUltrasonicDistance().getDistance() > 1500)
                                {
                                    mBase.setControlMode(Base.CONTROL_MODE_RAW);
                                    // the unit is mm.
                                    if (mBase.getUltrasonicDistance().getDistance() > 1500) {
                                        // set robot base linearVelocity, unit is rad/s, rand is -PI ~ PI.

                                        ArrayList<Pose2D> position = new ArrayList<>();
                                        position.add(mBase.getOdometryPose(System.currentTimeMillis() * 1000));
                                        mBase.setLinearVelocity(1);
                                    } else {
                                        //turn left
                                        mBase.setControlMode(Base.CONTROL_MODE_NAVIGATION);
                                        mBase.addCheckPoint(0.0f, 1.0f, (float)(Math.PI/2));
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

                }
        });

    }

}
