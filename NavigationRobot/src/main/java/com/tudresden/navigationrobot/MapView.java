package com.tudresden.navigationrobot;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedList;

public class MapView extends View implements View.OnTouchListener {

    private Canvas mCanvas;
    private Bitmap mBitmap;
    private Paint mPaint = new Paint();

    private LinkedList<Position> mPositions = new LinkedList<>();

    private int mWidth;
    private int mHeight;

    private NavigationActivity mNavigationActivity;

    private boolean mTouchEvent = false;

    public MapView(Context context) {
        super(context);
    }

    public MapView(Context context, LinkedList<Position> positions, int width, int height){
        this(context);

        setFocusable(true);
        setFocusableInTouchMode(true);
        setOnTouchListener(this);

        mNavigationActivity = (NavigationActivity)context;

        mPositions = positions;
        mWidth = width;
        mHeight = height;
    }

    private void initMap() {
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.RGB_565);
        mCanvas = new Canvas(mBitmap);

        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(4);

        mCanvas.drawPaint(mPaint);

        for(Position p : mPositions) {
            if(mPositions.indexOf(p) == 0) {
                mPaint.setColor(Color.RED);
                mCanvas.drawCircle((float)p.getX(), (float)p.getY(), 8, mPaint);
            } else {
                mPaint.setColor(Color.BLACK);
                mCanvas.drawCircle((float)p.getX(), (float)p.getY(), 8, mPaint);
            }
            if(mPositions.indexOf(p) > 0) {
                mPaint.setColor(Color.BLACK);
                Position start = mPositions.get(mPositions.indexOf(p) - 1);
                mCanvas.drawLine((float)start.getX(), (float)start.getY(), (float)p.getX(), (float)p.getY(), mPaint);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(mTouchEvent) {
            mTouchEvent = false;
        } else {
            initMap();
        }

        canvas.drawBitmap(mBitmap, 0, 0, mPaint);
    }

    public boolean onTouch(View view, MotionEvent event) {
        mTouchEvent = true;
        initMap();
        mPaint.setColor(Color.GREEN);
        Position p = mNavigationActivity.calculateRealCoordinates(event.getX(), event.getY());
        mCanvas.drawCircle(event.getX(), event.getY(), 8, mPaint);
        // mCanvas.drawText("(" + p.getX() + " , " + p.getY() + ")", event.getX(), event.getY(), mPaint);
        invalidate();
        return true;
    }
}