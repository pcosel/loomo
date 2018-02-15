package com.tudresden.navigationrobot;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.view.View;

import java.util.LinkedList;

public class Draw extends View {

    private static final int GRID_COL = 5;
    private static final int GRID_ROW = 3;
    private int mapWidth;
    private int mapHeight;
    private int gridWidthInPixel;
    private float mStartX;
    private float mStartY;
    private float mEndX;
    private float mEndY;
    private Paint mPaintTrack;
    private Paint mPaintJoint;
    private Paint mPaintGrid;
    private Paint mPaintCircle;
    private Paint mPaintAxis;
    private Paint mPaintText;
    private Bitmap mBitmap;
    private Bitmap mBitmapCurrent;
    private Canvas mCanvas;
    private Canvas mCanvasCurrent;

    private LinkedList<PointF> positionLinkedList;

    public Draw(Context context, int width, int height, float density, int gridWidthInPixel) {
        super(context);
        this.mapWidth = width - (int) (100 * density);
        this.mapHeight = height - (int) (60 * density);
        this.gridWidthInPixel = gridWidthInPixel;

        positionLinkedList = new LinkedList<>();

        // init canvas
        System.out.println("The width of the map in class Draw is: " + mapWidth);
        System.out.println("The height of the map in class Draw is: " + mapHeight);

        mBitmap = Bitmap.createBitmap(mapWidth, mapHeight, Bitmap.Config.RGB_565);
        mBitmapCurrent = Bitmap.createBitmap(mapWidth, mapHeight, Bitmap.Config.RGB_565);
        mCanvas = new Canvas(mBitmap);
        mCanvasCurrent = new Canvas(mBitmapCurrent);

        // trajectory draw pen
        mPaintTrack = new Paint(Paint.DITHER_FLAG);
        mPaintTrack.setStyle(Paint.Style.STROKE);
        mPaintTrack.setStrokeWidth(3);
        mPaintTrack.setColor(Color.RED);
        mPaintTrack.setAntiAlias(true);
        mPaintTrack.setDither(true);
        mPaintTrack.setStrokeJoin(Paint.Join.ROUND);
        mPaintTrack.setStrokeCap(Paint.Cap.ROUND);

        // joint draw pen
        mPaintJoint = new Paint(Paint.DITHER_FLAG);
        mPaintJoint.setStyle(Paint.Style.STROKE);
        mPaintJoint.setStrokeWidth(5);
        mPaintJoint.setColor(Color.GREEN);
        mPaintJoint.setAntiAlias(true);
        mPaintJoint.setDither(true);
        mPaintJoint.setStrokeJoin(Paint.Join.ROUND);
        mPaintJoint.setStrokeCap(Paint.Cap.ROUND);

        // grid draw pen
        mPaintGrid = new Paint(Paint.DITHER_FLAG);
        mPaintGrid.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaintGrid.setStrokeWidth(1);
        mPaintGrid.setColor(Color.GRAY);
        mPaintGrid.setAntiAlias(true);
        mPaintGrid.setDither(true);
        mPaintGrid.setStrokeJoin(Paint.Join.ROUND);
        mPaintGrid.setStrokeCap(Paint.Cap.ROUND);
        mPaintGrid.setTextSize(mapHeight / 25);

        // text draw pen
        mPaintText = new Paint(Paint.LINEAR_TEXT_FLAG);
        mPaintText.setStyle(Paint.Style.FILL);
        mPaintText.setColor(Color.WHITE);
        mPaintText.setAntiAlias(true);
        mPaintText.setDither(true);
        mPaintText.setTextSize(mapHeight / 25);

        // axis draw pen
        mPaintAxis = new Paint(Paint.DITHER_FLAG);
        mPaintAxis.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaintAxis.setStrokeWidth(1);
        mPaintAxis.setColor(Color.YELLOW);
        mPaintAxis.setAntiAlias(true);
        mPaintAxis.setDither(true);
        mPaintAxis.setStrokeJoin(Paint.Join.ROUND);
        mPaintAxis.setStrokeCap(Paint.Cap.ROUND);
        mPaintAxis.setTextSize(mapHeight / 25);

        // circle draw pen
        mPaintCircle = new Paint(Paint.DITHER_FLAG);
        mPaintCircle.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaintCircle.setStrokeWidth(1);
        mPaintCircle.setColor(Color.YELLOW);
        mPaintCircle.setAntiAlias(true);
        mPaintCircle.setDither(true);

        // draw grid
        drawGrid();
        invalidate();
    }

    public void drawLine(float lastX, float lastY, float x, float y) {

        mPaintCircle.setColor(Color.YELLOW);
        mCanvas.drawCircle(lastX, lastY, 7, mPaintCircle);
        mCanvas.drawLine(lastX, lastY, x, y, mPaintTrack);
//        mCanvasCurrent.drawColor(0, PorterDuff.Mode.CLEAR);
        mPaintCircle.setColor(Color.WHITE);
        mCanvas.drawCircle(x, y, 7, mPaintCircle);
        System.out.println("Tracking Loomo ...");
    }

    public float getmStartX() {
        return mStartX;
    }

    public void setmStartX(float mStartX) {
        this.mStartX = mStartX;
    }

    public float getmStartY() {
        return mStartY;
    }

    public void setmStartY(float mStartY) {
        this.mStartY = mStartY;
    }

    public float getmEndX() {
        return mEndX;
    }

    public void setmEndX(float mEndX) {
        this.mEndX = mEndX;
    }

    public float getmEndY() {
        return mEndY;
    }

    public void setmEndY(float mEndY) {
        this.mEndY = mEndY;
    }


    public int getMapWidth() {
        return mapWidth;
    }

    public int getMapHeight() {
        return mapHeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap, 0, 0, mPaintTrack);
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//
//        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            resetPaint();
//            positionLinkedList.clear();
//            mStartX = event.getX();
//            mStartY = event.getY();
//            positionLinkedList.add(new PointF(mStartX, mStartY));
//            mCanvas.drawPoint(mStartX, mStartY, mPaintTrack);
//            mCanvas.drawText("start", mStartX, mStartY, mPaintText);
//        }
//
//        if (event.getAction() == MotionEvent.ACTION_MOVE) {
//            mEndX = event.getX();
//            mEndY = event.getY();
//            mCanvas.drawPoint(mEndX, mEndY, mPaintJoint);
//            positionLinkedList.add(new PointF(mEndX, mEndY));
//            mCanvas.drawLine(mStartX, mStartY, mEndX, mEndY, mPaintTrack);
//            mStartX = mEndX;
//            mStartY = mEndY;
//        }
//
//        if (event.getAction() == MotionEvent.ACTION_UP) {
//            mCanvas.drawText("end", mStartX, mStartY, mPaintText);
//        }
//
//        invalidate();
//
//        return true;
//    }

    // reset paint to empty
    public void resetPaint() {
        mBitmap.recycle();
        mBitmapCurrent.recycle();
        mBitmap = Bitmap.createBitmap(mapWidth, mapHeight, Bitmap.Config.RGB_565);
        mBitmapCurrent = Bitmap.createBitmap(mapWidth, mapHeight, Bitmap.Config.RGB_565);
        mCanvas = new Canvas(mBitmap);
        mCanvasCurrent = new Canvas(mBitmapCurrent);
        drawGrid();

        this.invalidate();
    }

    // get point list in time order
    public LinkedList<PointF> getPointList() {
        return positionLinkedList;
    }

    // get canvas width
    public int getCanvasWidth() {
        return mapWidth;
    }

    // get canvas height
    public int getCanvasHeight() {
        return mapHeight;
    }

    // draw grid line and axes
    private void drawGrid() {
        // draw grid
        for (int i = 1; i * gridWidthInPixel < mapWidth; i++) {
            mCanvas.drawLine(gridWidthInPixel * i, 0, gridWidthInPixel * i, mapHeight, mPaintGrid);
        }
        for (int j = 1; j * gridWidthInPixel < mapHeight; j++) {
            mCanvas.drawLine(0, gridWidthInPixel * j, mapWidth, gridWidthInPixel * j, mPaintGrid);
        }
        float xAxis = mapHeight / 2;
        float yAxis = mapWidth / 2;
        mCanvas.drawLine(yAxis, 0, yAxis, mapHeight, mPaintAxis);
        mCanvas.drawLine(0, xAxis, mapWidth, xAxis, mPaintAxis);

        // draw direction and coordinate hint
        //mCanvas.drawText("(0, 0)", mapWidth / 2, mPaintText.getTextSize(), mPaintGrid);
        //detect the upper left:
        mCanvas.drawText("UL", 5, 10, mPaintGrid);
        mCanvas.drawText("y", yAxis + 5, mapHeight - 15, mPaintAxis);
        //detect upper right coordinate:
        mCanvas.drawText("x", mapWidth - 15, xAxis - 5, mPaintAxis);
//        mCanvas.drawText("rear", mapWidth/2, mapHeight - mPaintText.getTextSize()*3, mPaintGrid);
//        mCanvas.drawText("left", 10, mapHeight/2, mPaintGrid);
//        mCanvas.drawText("right", mapWidth - mPaintText.getTextSize()*3, mapHeight/2, mPaintGrid);
    }
}
