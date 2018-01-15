package com.tudresden.navigationphone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.segway.robot.mobile.sdk.connectivity.BufferMessage;
import com.segway.robot.mobile.sdk.connectivity.MobileException;
import com.segway.robot.mobile.sdk.connectivity.MobileMessageRouter;
import com.segway.robot.mobile.sdk.connectivity.StringMessage;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.baseconnectivity.Message;
import com.segway.robot.sdk.baseconnectivity.MessageConnection;
import com.segway.robot.sdk.baseconnectivity.MessageRouter;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.Random;

public class TrackActivity extends Activity {
    private static final String TAG = "Show_Track_On_The_Phone";
    private Draw mDraw;
    private FrameLayout mFrameLayout;
    private EditText mEditText;
    private TextView mTextView;
    private Button mBindButton;
    private Button mResetButton;
    private Button mStartButton;
    private Button mStopButton;
    private Button mScaleButton;
    private String mRobotIP;
    private MobileMessageRouter mMobileMessageRouter = null;
    private MessageConnection mMessageConnection = null;
    private LinkedList<PointF> mPointList;
    private float pixelToMeter = 0.01f;
    private Canvas mCanvas;
    private float lastX = 0;
    private float lastY = 0;
    private float x = 0;
    private float y = 0;

    // called when connection state change
    private MessageConnection.ConnectionStateListener mConnectionStateListener = new MessageConnection.ConnectionStateListener() {
        @Override
        public void onOpened() {
            //connection between mobile application and robot application is opened.
            //Now can send messages to each other.
            Log.d(TAG, "onOpened: " + mMessageConnection.getName());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    enableButtons();
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
                    disableButtons();
                    Toast.makeText(getApplicationContext(), "disconnected to: " + mMessageConnection.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    };
    // called when message received/sent/sentError
    private MessageConnection.MessageListener mMessageListener = new MessageConnection.MessageListener() {
        @Override
        public void onMessageReceived(final Message message) {
            if (message instanceof StringMessage && message.getContent().toString().contains("(")) {
                // If the message contains "(", it can only be a position, because all the other messages don't contain brackets
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LinkedList position = messageAnalyze(message.getContent().toString());
//                    mCanvas.drawLine(lastX, lastY, (float) position.getFirst(), (float) position.getLast(), new Paint());
                        System.out.println("lastX = " + lastX + " lastY = " + lastY + "\t" + position.toString());
                        mDraw.drawLine(lastX, lastY, lastX + (float) position.getFirst(), lastY + (float) position.getLast());
                    }
                });
            }
        }

        @Override
        public void onMessageSentError(Message message, String error) {
            //the message  that is sent failed
            Log.d(TAG, "Message send error");
        }

        @Override
        public void onMessageSent(Message message) {
            //the message  that is sent successfully
            Log.d(TAG, "onMessageSent: id=" + message.getId() + ";timestamp=" + message.getTimestamp());
        }
    };
    // called when connection created, set ConnectionStateListener and MessageListener in onConnectionCreated
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
    // called when service bind success or failed, register MessageConnectionListener in onBind
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

    // parse String to coordinates
    private LinkedList messageAnalyze(String message) {
        float posX, posY;
        int msgL = message.length();
        message = message.substring(1, msgL - 1);
        String[] messageList = message.split("  |  ");
        for (int i = 0; i < messageList.length; i++) {
            System.out.println("messageList[" + i + "] = " + messageList[i]);
        }
        posX = Float.parseFloat(messageList[0]);
        System.out.println("The length of messageList is: " + messageList.length);
        System.out.println("Current posX is: " + posX);
        posY = Float.parseFloat(messageList[2]);
        System.out.println("Current posY is: " + posY);
        LinkedList position = new LinkedList();
        position.add(posX);
        position.add(posY);
        return position;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        // find view
        findView();

        // init canvas
        initCanvas();

        // disable buttons
        //disableButtons();

        // show scale hint
        showScale();
    }

    public void onClick(final View v) {
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
            case R.id.btnSend:
                sendMessageToRobot("start");
                //simulateSendPointToHandy();
                break;
            case R.id.button_bind:
                //init connection to Robot
                initConnection();
                break;
            case R.id.btnStop:
                // send STOP instruction to robot
                sendMessageToRobot("stop");
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

    private void sendMessageToRobot(String content) {
        if (mMessageConnection != null) {
            try {
                mMessageConnection.sendMessage(new StringMessage(content));
            } catch (Exception e) {
                Log.e(TAG, "Sending message (" + content + ") failed", e);
            }
        }
    }

    private void simulateSendPointToHandy() {
        // send point list to Handy
        int min = -50;
        int max = 50;

        Random r = new Random();
        int step = r.nextInt(max - min + 1) + min;
        x = lastX + step;
        step = r.nextInt(max - min + 1) + min;
        y = lastY + step;
        String coor_str = "(" + x + " | " + y + " | 2)";
//                lastX = 100;
//                lastY = 100;
        LinkedList position = messageAnalyze(coor_str);
        for (int i = 0; i < position.size(); i++) {
            System.out.println("position[" + i + "] = " + position.get(i));
        }

        Log.d("Position: ", "" + position.getFirst());
        System.out.println("The height of the map is: " + mDraw.getMapHeight());
        mDraw.drawLine(lastX, lastY, (float) position.getFirst(), (float) position.getLast());
        lastX = x;
        lastY = y;

//      mTextView.setText("change text");
        showScale();
        mPointList = mDraw.getPointList();
        byte[] messageByte = packFile();
        if (mMessageConnection != null) {
            try {
                //message sent is BufferMessage, used a txt file to test sending BufferMessage
                mMessageConnection.sendMessage(new BufferMessage(messageByte));
            } catch (MobileException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // find view
    private void findView() {
        mFrameLayout = (FrameLayout) findViewById(R.id.flMap);
        mEditText = (EditText) findViewById(R.id.edit_ip);
        mTextView = (TextView) findViewById(R.id.tvScale);
        mBindButton = (Button) findViewById(R.id.button_bind);
        mResetButton = (Button) findViewById(R.id.btnReset);
        mStartButton = (Button) findViewById(R.id.btnSend);
        mStopButton = (Button) findViewById(R.id.btnStop);
        mScaleButton = (Button) findViewById(R.id.btnScale);
    }

    // init canvas for drawing
    private void initCanvas() {
        Point3D pt = getWindowSize();
        int gridWidthInPixel = (int) (1 / pixelToMeter);
        mDraw = new Draw(TrackActivity.this, pt.width, pt.height, pt.density, gridWidthInPixel);
        mFrameLayout.addView(mDraw);
        System.out.println("Init map height: " + mDraw.getMapHeight());
        lastX = mDraw.getMapWidth() / 2;
        lastY = mDraw.getMapHeight() / 2;
    }

    // init connection to Robot
    private void initConnection() {
        // get the MobileMessageRouter instance
        mMobileMessageRouter = MobileMessageRouter.getInstance();

        // you can read the IP from the robot app.
        mRobotIP = mEditText.getText().toString();
        try {
            mMobileMessageRouter.setConnectionIp(mRobotIP);

            // bind the connection service in robot
            mMobileMessageRouter.bindService(this, mBindStateListener);
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "Connection init FAILED", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Connection init FAILED", e);
        }
    }

    // pack file to byte[]
    private byte[] packFile() {
        ByteBuffer buffer = ByteBuffer.allocate(mPointList.size() * 2 * 4 + 4);
        // protocol: the first 4 bytes is indicator of data or STOP message
        // 1 represent tracking data, 0 represent STOP message
        buffer.putInt(1);
        for (PointF pf : mPointList) {
            //System.out.println(pf.x + " " + pf.y);
            Log.d(TAG, "Send " + pixelToMeter * pf.x + "< >" + pixelToMeter * pf.y);
            buffer.putFloat(pixelToMeter * pf.x);
            buffer.putFloat(pixelToMeter * pf.y);
        }
        buffer.flip();
        byte[] messageByte = buffer.array();
        return messageByte;
    }

    // show area width and height
    private void showScale() {
        DecimalFormat decimalFormat = new DecimalFormat(".00");
        mTextView.setText(decimalFormat.format(pixelToMeter * mDraw.getCanvasWidth()) + " X " + decimalFormat.format(pixelToMeter * mDraw.getCanvasHeight()) + " m");
    }

    // get window size
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

    // enable buttons
    private void enableButtons() {
        mResetButton.setEnabled(true);
        mStartButton.setEnabled(true);
        mStopButton.setEnabled(true);
    }

    // disable buttons
    private void disableButtons() {
        mResetButton.setEnabled(false);
        mStartButton.setEnabled(false);
        mStopButton.setEnabled(false);
    }

    class Point3D {
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
