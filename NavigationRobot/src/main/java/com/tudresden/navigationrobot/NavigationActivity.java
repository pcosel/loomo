package com.tudresden.navigationrobot;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedList;

public class NavigationActivity extends Activity {

    private static final int PADDING = 30;

    private FrameLayout mFrameLayout;

    private Position mStartPosition;

    private Position mTargetPosition;

    private double mDistanceBetweenPoints;

    private Gson mGson = new Gson();

    private LinkedList<Position> mInputPositions = new LinkedList<>();

    private LinkedList<Position> mScreenPositions = new LinkedList<>();

    private Type mListType = new TypeToken<LinkedList<Position>>(){}.getType();

    DecimalFormatSymbols mDecimalFormatSymbols = DecimalFormatSymbols.getInstance();

    public boolean isFilePresent() {
        String path = getApplicationContext().getFilesDir().getAbsolutePath() + "/positions.json";
        File file = new File(path);
        return file.exists();
    }

    public String read() {
        try {
            FileInputStream fileInputStream = getApplicationContext().openFileInput("positions.json");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch(IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "File not found!", Toast.LENGTH_SHORT).show();
            return "Error!";
        }
    }

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

        double sumX = greatestPosX + greatestNegX;
        double sumY = greatestPosY + greatestNegY;

        if(Double.compare(sumX, 0.0) == 0 && Double.compare(sumY, 0.0) == 0) {
            mDistanceBetweenPoints = 0.0;
        } else if(Double.compare(sumX, 0.0) == 0) {
            mDistanceBetweenPoints = (height - PADDING) / sumY;
        } else if(Double.compare(sumY, 0.0) == 0) {
            mDistanceBetweenPoints = (width - PADDING) / sumX;
        } else if(Double.compare((width - PADDING) / sumX, (height - PADDING) / sumY) > 0) {
            mDistanceBetweenPoints = (height - PADDING) / sumY;
        } else {
            mDistanceBetweenPoints = (width - PADDING) / sumX;
        }

        double startX = mDistanceBetweenPoints * greatestPosX + (PADDING / 2);
        double startY = mDistanceBetweenPoints * greatestPosY + (PADDING / 2);

        mStartPosition = new Position(startX, startY);

        for(Position p : mInputPositions) {
            double inX = p.getX();
            double inY = p.getY();
            double outX;
            double outY;
            if(Double.compare(mDistanceBetweenPoints, 0.0) == 0) {
                outX = width / 2;
                outY = height / 2;
            } else {
                if(Double.compare(inX, 0.0) > 0) {
                    outX = startX - (inX * mDistanceBetweenPoints);
                } else {
                    outX = startX + (Math.abs(inX) * mDistanceBetweenPoints);
                }
                if(Double.compare(p.getY(), 0.0) > 0) {
                    outY = startY - (inY * mDistanceBetweenPoints);
                } else {
                    outY = startY + (Math.abs(inY) * mDistanceBetweenPoints);
                }
            }
            mScreenPositions.add(new Position(outX, outY));
        }
    }

    public Position calculateRealPosition(float x, float y) {
        double targetX = 0.0;
        double targetY = 0.0;

        if (Double.compare(mDistanceBetweenPoints, 0.0) != 0) {
            if (Double.compare(x, mStartPosition.getX()) == 0) {
                targetX = 0;
            } else if (Double.compare(x, mStartPosition.getX()) < 0) {
                targetX = -((x - mStartPosition.getX()) / mDistanceBetweenPoints);
            } else {
                targetX = (mStartPosition.getX() - x) / mDistanceBetweenPoints;
            }

            if (Double.compare(y, mStartPosition.getY()) == 0) {
                targetY = 0;
            } else if (Double.compare(y, mStartPosition.getY()) < 0) {
                targetY = -((y - mStartPosition.getY()) / mDistanceBetweenPoints);
            } else {
                targetY = (mStartPosition.getY() - y) / mDistanceBetweenPoints;
            }
        }

        mDecimalFormatSymbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("#.##", mDecimalFormatSymbols);
        targetX = Double.parseDouble(df.format(targetX));
        targetY = Double.parseDouble(df.format(targetY));

        mTargetPosition = new Position(targetX, targetY);
        return mTargetPosition;
    }

    public void startNavigation(View view) {
        if(mTargetPosition == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("No destination selected!");
            builder.setMessage("Please tap a point on the map to select your destination.");
            builder.setPositiveButton("OK", null);
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            //TODO: Implement Navigation
        }
    }

    private void initFrameLayoutListener() {
        final FrameLayout layout = (FrameLayout)findViewById(R.id.frameLayout);
        layout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                int width = layout.getWidth();
                int height = layout.getHeight();

                calculateScreenPositions(width, height);

                mFrameLayout.addView(new MapView(NavigationActivity.this, mScreenPositions, width, height));
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mFrameLayout = (FrameLayout)findViewById(R.id.frameLayout);

        if(isFilePresent()) {
            mInputPositions = mGson.fromJson(read(), mListType);
        }

        initFrameLayoutListener();
    }
}
