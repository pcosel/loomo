package com.tudresden.navigationrobot;

import android.app.Activity;
import android.os.Bundle;
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
import java.util.LinkedList;

public class NavigationActivity extends Activity {

    private FrameLayout mFrameLayout;

    private Position mTargetPosition;

    private double mDistance;

    private Gson mGson = new Gson();

    private LinkedList<Position> mInputPositions = new LinkedList<>();
    private LinkedList<Position> mScreenPositions = new LinkedList<>();

    private Type mListType = new TypeToken<LinkedList<Position>>(){}.getType();

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
        double greatestAbsX = 0.0;
        double greatestAbsY = 0.0;

        for(Position p : mInputPositions) {
            if(Math.abs(p.getX()) > greatestAbsX) {
                greatestAbsX = Math.abs(p.getX());
            }
            if(Math.abs(p.getY()) > greatestAbsY) {
                greatestAbsY = Math.abs(p.getY());
            }
        }

        if(Double.compare(greatestAbsX, 0.0) == 0 && Double.compare(greatestAbsY, 0.0) == 0) {
            mDistance = 0.0;
        } else if(Double.compare(greatestAbsX, 0.0) == 0) {
            mDistance = ((height - 20) / 2) / greatestAbsY;
        } else if(Double.compare(greatestAbsY, 0.0) == 0) {
            mDistance = ((width - 20) / 2) / greatestAbsX;
        } else if(Double.compare(((width - 20) / 2) / greatestAbsX, ((height - 20) / 2) / greatestAbsY) > 0) {
            mDistance = ((height - 20) / 2) / greatestAbsY;
        } else {
            mDistance = ((width - 20) / 2) / greatestAbsX;
        }

        for(Position p : mInputPositions) {
            double x;
            double y;
            if(Double.compare(mDistance, 0.0) == 0) {
                x = width / 2;
                y = height / 2;
            } else {
                if(Double.compare(p.getX(), 0.0) > 0) {
                    x = (width / 2) - (p.getX() * mDistance);
                } else {
                    x = (width / 2) + (Math.abs(p.getX()) * mDistance);
                }
                if(Double.compare(p.getY(), 0.0) > 0) {
                    y = (height / 2) - (p.getY() * mDistance);
                } else {
                    y = (height / 2) + (Math.abs(p.getY()) * mDistance);
                }
            }
            mScreenPositions.add(new Position(x, y));
        }
    }

    public Position calculateRealPosition(float x, float y) {
        return mTargetPosition;
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
        setContentView(R.layout.activity_map);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mFrameLayout = (FrameLayout)findViewById(R.id.frameLayout);

        if(isFilePresent()) {
            mInputPositions = mGson.fromJson(read(), mListType);
        }

        initFrameLayoutListener();
    }
}
