package de.tud.loomospeech;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import com.tudresden.navigationrobot.R;

import java.lang.ref.WeakReference;

public class MessageHandler extends Handler {
    static final int ASSERT = 7;
    static final int DEBUG = 3;
    static final int ERROR = 6;
    static final int INFO = 4;
    static final int VERBOSE = 2;
    static final int WARN = 5;

    static final int APPEND = 10;
    static final int SET = 11;

    static final int OUTPUT = 12;
    static final int STATUS = 13;

    private final WeakReference<Activity> activityWeakReference;

    public MessageHandler(Activity instance) {
        activityWeakReference = new WeakReference<>(instance);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        TextView textView;
        TextView statusView;
        if (((textView = getTextView(R.id.output)) == null) || ((statusView = getTextView(R.id.status_msg)) == null)) {
            return;
        }

        if (msg.arg2 == OUTPUT) {
            textView.setText(msg.obj.toString());
        } else if (msg.arg2 == STATUS) {
            statusView.setText(msg.obj.toString());
        } else {
            return;
        }

//        if (msg.arg1 == APPEND) {
//            if (msg.arg2 == OUTPUT) {
//                textView.append("\n" + msg.obj.toString());
//            } else if (msg.arg2 == STATUS) {
//                statusView.append("\n" + msg.obj.toString());
//            } else {
//                return;
//            }
//        }
//        else {
//            if (msg.arg2 == OUTPUT) {
//                textView.setText(msg.obj.toString());
//            } else if (msg.arg2 == STATUS) {
//                statusView.setText(msg.obj.toString());
//            } else {
//                return;
//            }
//        }
    }

    private TextView getTextView(int myId) {
        TextView myTextView = null;
        Activity myActivity = activityWeakReference.get();

        if (myActivity != null)
            myTextView = (TextView) myActivity.findViewById(myId);

        return myTextView;
    }
}
