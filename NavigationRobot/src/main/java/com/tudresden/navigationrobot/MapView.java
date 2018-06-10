package com.tudresden.navigationrobot;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedList;

/**
 * This View draws a map of the positions that the robot has reached during the exploration
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
     * The padding for the map.
     */
    private static final int PADDING = 30;

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
     * The position that marks the starting point on the screen. Not the real coordinates of the
     * starting point!
     */
    private Position mStartPosition;

    /**
     * The distance between the points on the screen (if assumed that the points are evenly
     * distributed across the screen).
     */
    private double mDistanceBetweenPoints;

    /**
     * All the positions that the robot has reached during the exploration phase.
     */
    private LinkedList<Position> mInputPositions = new LinkedList<>();

    /**
     * The width of this view and the bitmap.
     */
    private int mWidth;

    /**
     * The height of this view and the bitmap.
     */
    private int mHeight;

    /**
     * Indicates whether a touch event has occurred.
     */
    private boolean mTouchEvent = false;

    public MapView(Context context) {
        super(context);
    }

    /**
     * Creates a MapView and initializes the different paint objects.
     * @param context the activity that contains this view
     * @param width the width of this view and the contained bitmap
     * @param height the height of this view and the contained bitmap
     */
    public MapView(Context context, LinkedList<Position> inputPositions, int width, int height){
        this(context);

        setFocusable(true);
        setFocusableInTouchMode(true);
        setOnTouchListener(this);

        mInputPositions = inputPositions;

        calculateScreenPositions(width, height);

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
     * Converts all the positions that the robot has reached during the exploration phase to
     * positions that can be displayed on the screen.
     * @param width the width of the layout that contains the map
     * @param height the height of the layout that contains the map
     */
    public void calculateScreenPositions(int width, int height) {
        double greatestPosX = 0.0;
        double greatestNegX = 0.0;
        double greatestPosY = 0.0;
        double greatestNegY = 0.0;

        for(Position p : mInputPositions) {
            double x = p.getX();
            double y = p.getY();

            // Find the greatest positive and negative values for x and y amongst all the positions
            if(Double.compare(x, 0.0) > 0) {
                if(Double.compare(x, greatestPosX) > 0) {
                    greatestPosX = x;
                }
            } else {
                if(Double.compare(Math.abs(x), greatestNegX) > 0) {
                    greatestNegX = Math.abs(x);
                }
            }
            if(Double.compare(y, 0.0) > 0) {
                if(Double.compare(y, greatestPosY) > 0) {
                    greatestPosY = y;
                }
            } else {
                if(Double.compare(Math.abs(y), greatestNegY) > 0) {
                    greatestNegY = Math.abs(y);
                }
            }
        }

        // Find out how many points need to fit in the width and height
        double sumX = greatestPosX + greatestNegX;
        double sumY = greatestPosY + greatestNegY;

        // Choose the distance between the points as big as possible in order to make the map as big
        // as possible but also make sure that all the points fit on the screen
        if(Double.compare(sumX, 0.0) == 0 && Double.compare(sumY, 0.0) == 0) {
            // Only the starting point was in the list
            mDistanceBetweenPoints = 0.0;
        } else if(Double.compare(sumX, 0.0) == 0) {
            // No vertical space needed
            mDistanceBetweenPoints = (width - PADDING) / sumY;
        } else if(Double.compare(sumY, 0.0) == 0) {
            // No horizontal space needed
            mDistanceBetweenPoints = (height - PADDING) / sumX;
        } else if(Double.compare((height - PADDING) / sumX, (width - PADDING) / sumY) > 0) {
            mDistanceBetweenPoints = (width - PADDING) / sumY;
        } else {
            mDistanceBetweenPoints = (height - PADDING) / sumX;
        }

        // Calculate the screen position of the starting point (0,0)
        double startX = mDistanceBetweenPoints * greatestPosY + (PADDING / 2);
        double startY = mDistanceBetweenPoints * greatestPosX + (PADDING / 2);

        mStartPosition = new Position(startX, startY);

        for(Position p : mInputPositions) {
            double inX = p.getX();
            double inY = p.getY();
            double outX;
            double outY;
            if(Double.compare(mDistanceBetweenPoints, 0.0) == 0) {
                // Only the starting point was in the list so it needs to be displayed in the center
                // of the screen
                outX = width / 2;
                outY = height / 2;
            } else {
                // Convert the coordinates of the real positions to the coordinates on the screen.
                // Be aware that the coordinate system of the robot is exactly the opposite of the
                // coordinate system of the screen:
                // Robot: Positive x --> forward, positive y --> left
                // Screen: Positive x --> width, positive y --> height (origin in the left upper
                // corner of the screen)
                if(Double.compare(inX, 0.0) > 0) {
                    outY = startY - (inX * mDistanceBetweenPoints);
                } else {
                    outY = startY + (Math.abs(inX) * mDistanceBetweenPoints);
                }
                if(Double.compare(inY, 0.0) > 0) {
                    outX = startX - (inY * mDistanceBetweenPoints);
                } else {
                    outX = startX + (Math.abs(inY) * mDistanceBetweenPoints);
                }
            }
            mScreenPositions.add(new Position(outX, outY));
        }
    }

    /**
     * Converts a position on the screen to a real position in the room. Inverts the calculations of
     * {@see #calculateScreenPositions(int width, int height)}.
     * @param x the x-coordinate of the selected position on the screen
     * @param y the y-coordinate of the selected position on the screen
     * @return the real position in the room
     */
    public Position calculateRealPosition(float x, float y) {
        double targetX = 0.0;
        double targetY = 0.0;

        // If only the starting point is displayed on the map, all selected positions around the
        // starting point should be treated as point (0,0) because in this case no navigation is
        // possible
        if(Double.compare(mDistanceBetweenPoints, 0.0) != 0) {
            if(Double.compare(x, mStartPosition.getX()) == 0) {
                targetY = 0;
            } else if(Double.compare(x, mStartPosition.getX()) < 0) {
                targetY = -((x - mStartPosition.getX()) / mDistanceBetweenPoints);
            } else {
                targetY = (mStartPosition.getX() - x) / mDistanceBetweenPoints;
            }
            if(Double.compare(y, mStartPosition.getY()) == 0) {
                targetX = 0;
            } else if(Double.compare(y, mStartPosition.getY()) < 0) {
                targetX = -((y - mStartPosition.getY()) / mDistanceBetweenPoints);
            } else {
                targetX = (mStartPosition.getY() - y) / mDistanceBetweenPoints;
            }
        }

        // Set decimal separator as '.' instead of ','
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
        dfs.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("#.##", dfs);
        targetX = Double.parseDouble(df.format(targetX));
        targetY = Double.parseDouble(df.format(targetY));

        return new Position(targetX, targetY);
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
        Position p = calculateRealPosition(event.getX(), event.getY());
        // Add green circle for the selected target position
        mCanvas.drawCircle(event.getX(), event.getY(), RADIUS, mPaintTouchPoint);
        // Show the coordinates of the position next to the circle
        mCanvas.drawText("(" + p.getX() + " , " + p.getY() + ")", event.getX() + RADIUS, event.getY() + 6, mPaintText);
        // Force the view to draw (onDraw() is called)
        invalidate();
        return true;
    }
}
