package com.tudresden.navigationrobot;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedList;

/**
 * This View displays a map of the positions that the robot has reached during the exploration
 * phase.
 *
 * @author Nadja Konrad
 */
public class MapView extends View implements View.OnTouchListener {

    /**
     * The radius for the circles that represent the positions.
     */
    private static final int RADIUS = 10;

    /**
     * The canvas that contains the Bitmap that contains the map.
     */
    private Canvas mCanvas;

    /**
     * The bitmap that contains the map.
     */
    private Bitmap mBitmap;

    /**
     * White paint for the background of the map.
     */
    private Paint mPaintBackground = new Paint();

    /**
     * Red paint for the starting point.
     */
    private Paint mPaintStartPoint = new Paint();

    /**
     * Black paint for all positions other than the starting point.
     */
    private Paint mPaintPoint = new Paint();

    /**
     * Green paint for the selected target position.
     */
    private Paint mPaintTouchPoint = new Paint();

    /**
     * Black paint for the lines between the positions.
     */
    private Paint mPaintLine = new Paint();

    /**
     * Black paint for the text next to the positions.
     */
    private Paint mPaintText = new Paint();

    /**
     * All the positions that need to be marked with a circle.
     */
    private LinkedList<Position> mScreenPositions = new LinkedList<>();

    /**
     * The width of this view and the bitmap.
     */
    private int mWidth;

    /**
     * The height of this view and the bitmap.
     */
    private int mHeight;

    /**
     * A reference to the activity that contains this view.
     */
    private MapActivity mNavigationActivity;

    /**
     * Indicates whether a touch event has occured.
     */
    private boolean mTouchEvent = false;

    public MapView(Context context) {
        super(context);
    }

    /**
     * Creates a MapView and initializes the different paint objects.
     * @param context the activity that contains this view
     * @param positions the list of the positions that need to be marked with a circle in the map
     * @param width the width of this view and the contained bitmap
     * @param height the height of this view and the contained bitmap
     */
    public MapView(Context context, LinkedList<Position> positions, int width, int height){
        this(context);

        setFocusable(true);
        setFocusableInTouchMode(true);
        setOnTouchListener(this);

        mNavigationActivity = (MapActivity)context;

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
        mPaintLine.setStrokeWidth(5);

        mPaintLine.setAntiAlias(true);
        mPaintText.setColor(Color.BLACK);
        mPaintText.setTextSize(20);
    }

    /**
     * Draws the actual map of the positions that the robot has reached during the exploration phase.
     */
    private void initMap() {
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.RGB_565);
        mCanvas = new Canvas(mBitmap);

        // Draw white background
        mCanvas.drawPaint(mPaintBackground);

        for(Position p : mScreenPositions) {
            if(mScreenPositions.indexOf(p) == 0) {
                // Draw red circle for starting point
                mCanvas.drawCircle((float)p.getX(), (float)p.getY(), RADIUS, mPaintStartPoint);
            } else {
                // Draw black circle for every point other than the starting point
                mCanvas.drawCircle((float)p.getX(), (float)p.getY(), RADIUS, mPaintPoint);
                // Draw black line between the current point and the previous point
                Position start = mScreenPositions.get(mScreenPositions.indexOf(p) - 1);
                mCanvas.drawLine((float)start.getX(), (float)start.getY(), (float)p.getX(), (float)p.getY(), mPaintLine);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(mTouchEvent) {
            // Don't redraw the whole map after a touch event because otherwise the green circle
            // for the selected target position isn't displayed
            mTouchEvent = false;
        } else {
            initMap();
        }
        canvas.drawBitmap(mBitmap, 0, 0, mPaintBackground);
    }

    public boolean onTouch(View view, MotionEvent event) {
        mTouchEvent = true;
        // Redraw the whole map
        initMap();
        Position p = mNavigationActivity.calculateRealPosition(event.getX(), event.getY());
        // Add green circle for the selected target position
        mCanvas.drawCircle(event.getX(), event.getY(), RADIUS, mPaintTouchPoint);
        // Show the coordinates of the position next to the circle
        mCanvas.drawText("(" + p.getX() + " , " + p.getY() + ")", event.getX() + RADIUS, event.getY() + 6, mPaintText);
        // Force the view to draw (onDraw() is called)
        invalidate();
        return true;
    }
}