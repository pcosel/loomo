package com.tudresden.navigationrobot;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedList;

/**
 * This Activity shows the map of the previously explored room.
 *
 * @author Nadja Konrad
 */
public class MapActivity extends Activity {

    /**
     * The padding for the map.
     */
    private static final int PADDING = 30;

    /**
     * The layout that contains the map.
     */
    private FrameLayout mFrameLayout;

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
     * All the positions that the robot has reached during the exploration phase converted to
     * positions that can be displayed on the screen.
     */
    private LinkedList<Position> mScreenPositions = new LinkedList<>();

    private FileHelper mFileHelper;

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
                // corner of the screen
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
     * Retrieves the width and the height of the layout that contains the map as soon as said layout
     * is created.
     * The listener is needed because problems occur if the width and the height of the layout are
     * queried before the UI has been successfully created.
     */
    private void initFrameLayoutListener() {
        final FrameLayout layout = (FrameLayout)findViewById(R.id.frameLayout);
        layout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                int width = layout.getWidth();
                int height = layout.getHeight();

                calculateScreenPositions(width, height);

                mFrameLayout.addView(new MapView(MapActivity.this, mScreenPositions, width, height));
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mFrameLayout = (FrameLayout)findViewById(R.id.frameLayout);

        if(mFileHelper == null) {
            mFileHelper = new FileHelper(this);
        }

        if(mFileHelper.fileExists()) {
            mInputPositions = mFileHelper.convertPositions();
        }

        initFrameLayoutListener();
    }
}
