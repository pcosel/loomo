package com.segway.robot.mobilesample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.segway.robot.mobile.sdk.connectivity.MobileException;
import com.segway.robot.mobile.sdk.connectivity.MobileMessageRouter;
import com.segway.robot.mobile.sdk.connectivity.StringMessage;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.baseconnectivity.Message;
import com.segway.robot.sdk.baseconnectivity.MessageConnection;
import com.segway.robot.sdk.baseconnectivity.MessageRouter;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MobileActivity";
    private EditText editTextContext;
    private EditText editTextIp;

    private Button sendStringButton;
    private Button bindServiceButton;
    private Button startButton;
    private Button stopButton;

    private int press = 0;
    private MobileMessageRouter mMobileMessageRouter = null;
    private MessageConnection mMessageConnection = null;

    private MessageConnection.ConnectionStateListener mConnectionStateListener = new MessageConnection.ConnectionStateListener() {
        @Override
        public void onOpened() {
            //connection between mobile application and robot application is opened.
            //Now can send messages to each other.
            Log.d(TAG, "onOpened: " + mMessageConnection.getName());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "connected to: " + mMessageConnection.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onClosed(String error) {
            //connection closed with error
            Log.e(TAG, "onClosed: " + error + ";name=" + mMessageConnection.getName());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "disconnected to: " + mMessageConnection.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    };
    private MessageConnection.MessageListener mMessageListener = new MessageConnection.MessageListener() {
        @Override
        public void onMessageReceived(final Message message) {

        }

        @Override
        public void onMessageSentError(Message message, String error) {

        }

        @Override
        public void onMessageSent(Message message) {
            //the message  that is sent successfully
            Log.d(TAG, "onMessageSent: id=" + message.getId() + ";timestamp=" + message.getTimestamp());
        }
    };
    private MessageRouter.MessageConnectionListener mMessageConnectionListener = new MessageRouter.MessageConnectionListener() {
        @Override
        public void onConnectionCreated(final MessageConnection connection) {
            Log.d(TAG, "onConnectionCreated: " + connection.getName());
            //get the MessageConnection instance
            mMessageConnection = connection;
            try {
                mMessageConnection.setListeners(mConnectionStateListener, mMessageListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private ServiceBinder.BindStateListener mBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.d(TAG, "onBind: ");
            try {
                mMobileMessageRouter.register(mMessageConnectionListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUnbind(String reason) {
            Log.e(TAG, "onUnbind: " + reason);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        editTextContext = (EditText) findViewById(R.id.editText_context);
        editTextIp = (EditText) findViewById(R.id.editView_input_ip);

        //get the MobileMessageRouter instance
        mMobileMessageRouter = MobileMessageRouter.getInstance();

        bindServiceButton = (Button) findViewById(R.id.button_bind);
        bindServiceButton.setOnClickListener(this);

        sendStringButton = (Button) findViewById(R.id.button_send_string);
        sendStringButton.setOnClickListener(this);

        startButton = (Button) findViewById(R.id.button_start);
        startButton.setOnClickListener(this);

        stopButton = (Button) findViewById(R.id.button_stop);
        stopButton.setOnClickListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMobileMessageRouter.unregister();
        mMobileMessageRouter.unbindService();
        Log.d(TAG, "onDestroy: ");
    }

    @Override
    public void onBackPressed() {
        if (press == 0) {
            Toast.makeText(this, "press again to exit", Toast.LENGTH_SHORT).show();
        }
        press++;
        if (press == 2) {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_bind:
                if ("".equals(editTextIp.getText().toString().trim())) {
                    Toast.makeText(this, "IP can't be null!", Toast.LENGTH_SHORT).show();
                    return;
                }
                //set the IP of the robot that you want to connect, if you have installed
                //robot-sample, you can read the IP from the robot app.
                mMobileMessageRouter.setConnectionIp(editTextIp.getText().toString());
                //bind the connection service in robot
                mMobileMessageRouter.bindService(this, mBindStateListener);
                break;
            case R.id.button_start:
                // send START instruction to robot
                startRobot();
                break;
            case R.id.button_stop:
                // send STOP instruction to robot
                stopRobot();
                break;
            case R.id.button_send_string:
                if (mMessageConnection != null) {
                    try {
                        //message sent is StringMessage
                        mMessageConnection.sendMessage(new StringMessage(editTextContext.getText().toString()));
                    } catch (MobileException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    private void startRobot() {
        if (mMessageConnection != null) {
            try {
                //message sent is StringMessage
                mMessageConnection.sendMessage(new StringMessage("start"));
            } catch (MobileException e) {
                e.printStackTrace();
            } catch (Exception e) {
                Log.e(TAG, "send START message failed", e);
            }
        }
    }

    private void stopRobot() {
        if (mMessageConnection != null) {
            try {
                //message sent is StringMessage
                mMessageConnection.sendMessage(new StringMessage("stop"));
            } catch (MobileException e) {
                e.printStackTrace();
            } catch (Exception e) {
                Log.e(TAG, "send STOP message failed", e);
            }
        }
    }

}
