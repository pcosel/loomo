package com.tudresden.navigationphone;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.segway.robot.mobile.sdk.connectivity.MobileMessageRouter;
import com.segway.robot.mobile.sdk.connectivity.StringMessage;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.baseconnectivity.Message;
import com.segway.robot.sdk.baseconnectivity.MessageConnection;
import com.segway.robot.sdk.baseconnectivity.MessageRouter;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private EditText editTextIp;
    private TextView textViewId;
    private TextView textViewTimestamp;
    private TextView textViewContent;
    private TextView textViewPositions;

    private int press = 0;
    private MobileMessageRouter mMobileMessageRouter = null;
    private MessageConnection mMessageConnection = null;

    private MessageConnection.ConnectionStateListener mConnectionStateListener = new MessageConnection.ConnectionStateListener() {
        @Override
        public void onOpened() {
            // Connection between mobile application and robot application is opened, s now they can send messages to each other
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
        public void onMessageSentError(Message message, String error) {
        }

        @Override
        public void onMessageSent(Message message) {}

        @Override
        public void onMessageReceived(final Message message) {
            if(message instanceof StringMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textViewId.setText("ID: " + Integer.toString(message.getId()));
                        textViewTimestamp.setText("Timestamp: " + Long.toString(message.getTimestamp()));
                        textViewContent.setText("Content: " + message.getContent().toString());
                    }
                });
                // If the message contains "(", it can only be a position, because all the other messages don't contain brackets
                if(message.getContent().toString().contains("(")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textViewPositions.setText(textViewPositions.getText() + message.getContent().toString());
                        }
                    });
                }
            }
        }
    };

    private MessageRouter.MessageConnectionListener mMessageConnectionListener = new MessageRouter.MessageConnectionListener() {
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

    private ServiceBinder.BindStateListener mBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            try {
                mMobileMessageRouter.register(mMessageConnectionListener);
            } catch(Exception e) {
                Log.e(TAG, "Register failed", e);
            }
        }

        @Override
        public void onUnbind(String reason) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        editTextIp = (EditText) findViewById(R.id.edit_ip);
        textViewId = (TextView) findViewById(R.id.edit_id);
        textViewTimestamp = (TextView) findViewById(R.id.edit_timestamp);
        textViewContent = (TextView) findViewById(R.id.edit_content);
        textViewPositions = (TextView) findViewById(R.id.edit_positions);

        mMobileMessageRouter = MobileMessageRouter.getInstance();

        Button bindServiceButton = (Button) findViewById(R.id.button_bind);
        bindServiceButton.setOnClickListener(this);

        Button startButton = (Button) findViewById(R.id.button_start);
        startButton.setOnClickListener(this);

        Button stopButton = (Button) findViewById(R.id.button_stop);
        stopButton.setOnClickListener(this);
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
    public void onDestroy() {
        super.onDestroy();
        mMobileMessageRouter.unregister();
        mMobileMessageRouter.unbindService();
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
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.button_bind:
                if("".equals(editTextIp.getText().toString().trim())) {
                    Toast.makeText(this, "IP can't be null!", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Set the IP of the robot that you want to connect to (you can read the IP from the robot app)
                mMobileMessageRouter.setConnectionIp(editTextIp.getText().toString());
                mMobileMessageRouter.bindService(this, mBindStateListener);
                break;
            case R.id.button_start:
                // Send START instruction to robot
                if(mMessageConnection != null) {
                    try {
                        mMessageConnection.sendMessage(new StringMessage("start"));
                    } catch(Exception e) {
                        Log.e(TAG, "Send START message failed", e);
                    }
                }
                break;
            case R.id.button_stop:
                // Send STOP instruction to robot
                if(mMessageConnection != null) {
                    try {
                        mMessageConnection.sendMessage(new StringMessage("stop"));
                    } catch(Exception e) {
                        Log.e(TAG, "Send STOP message failed", e);
                    }
                }
                break;
        }
    }
}
