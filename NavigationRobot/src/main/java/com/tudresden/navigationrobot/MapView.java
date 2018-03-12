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
    private Paint mPaintBackground = new Paint();
    private Paint mPaintStartPoint = new Paint();
    private Paint mPaintPoint = new Paint();
    private Paint mPaintTouchPoint = new Paint();
    private Paint mPaintLine = new Paint();
    private Paint mPaintText = new Paint();

    private LinkedList<Position> mScreenPositions = new LinkedList<>();

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

        mScreenPositions = positions;
        mWidth = width;
        mHeight = height;

        mPaintBackground.setColor(Color.WHITE);

        mPaintStartPoint.setAntiAlias(true);
        mPaintStartPoint.setStyle(Paint.Style.FILL);
        mPaintStartPoint.setColor(Color.RED);

        mPaintPoint.setAntiAlias(true);
        mPaintPoint.setStyle(Paint.Style.FILL);
        mPaintPoint.setColor(Color.BLACK);

        mPaintTouchPoint.setAntiAlias(true);
        mPaintTouchPoint.setStyle(Paint.Style.FILL);
        mPaintTouchPoint.setColor(Color.GREEN);

        mPaintLine.setAntiAlias(true);
        mPaintLine.setStyle(Paint.Style.STROKE);
        mPaintLine.setColor(Color.BLACK);
        mPaintLine.setStrokeWidth(4);

        mPaintLine.setAntiAlias(true);
        mPaintText.setColor(Color.BLACK);
        mPaintText.setTextSize(20);
    }

    private void initMap() {
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.RGB_565);
        mCanvas = new Canvas(mBitmap);

        mCanvas.drawPaint(mPaintBackground);

        for(Position p : mScreenPositions) {
            if(mScreenPositions.indexOf(p) == 0) {
                mCanvas.drawCircle((float)p.getX(), (float)p.getY(), 8, mPaintStartPoint);
            } else {
                mCanvas.drawCircle((float)p.getX(), (float)p.getY(), 8, mPaintPoint);
                Position start = mScreenPositions.get(mScreenPositions.indexOf(p) - 1);
                mCanvas.drawLine((float)start.getX(), (float)start.getY(), (float)p.getX(), (float)p.getY(), mPaintLine);
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
        canvas.drawBitmap(mBitmap, 0, 0, mPaintBackground);
    }

    public boolean onTouch(View view, MotionEvent event) {
        mTouchEvent = true;
        initMap();
        Position p = mNavigationActivity.calculateRealPosition(event.getX(), event.getY());
        mCanvas.drawCircle(event.getX(), event.getY(), 8, mPaintTouchPoint);
        mCanvas.drawText("(" + event.getX() + " , " + event.getY() + ")", event.getX() + 10, event.getY() + 6, mPaintText);
        invalidate();
        return true;
    }
}