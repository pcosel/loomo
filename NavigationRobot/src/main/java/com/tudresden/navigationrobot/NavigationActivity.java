package com.tudresden.navigationrobot;

import android.app.Activity;
import android.os.Build;
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

    private Gson mGson = new Gson();

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

    public LinkedList<Position> calculateScreenCoordinates(int width, int height) {
        double greatestAbsX = 0;
        double greatestAbsY = 0;

        LinkedList<Position> screenPositions = new LinkedList<>();

        if(isFilePresent()) {
            LinkedList<Position> inputPositions = mGson.fromJson(read(), mListType);

            for(Position p : inputPositions) {
                if(Math.abs(p.getX()) > greatestAbsX) {
                    greatestAbsX = Math.abs(p.getX());
                }
                if(Math.abs(p.getY()) > greatestAbsY) {
                    greatestAbsY = Math.abs(p.getY());
                }
            }

            int distance;
            if(greatestAbsX == 0 && greatestAbsY == 0) {
                distance = 0;
            } else if(greatestAbsX == 0) {
                distance = (int)(((height - 20) / 2) / greatestAbsY);
            } else if(greatestAbsY == 0) {
                distance = (int)(((width - 20) / 2) / greatestAbsX);
            } else if(((width - 20) / 2) / greatestAbsX <= ((height - 20) / 2) / greatestAbsY) {
                distance = (int)(((width - 20) / 2) / greatestAbsX);
            } else {
                distance = (int)(((height - 20) / 2) / greatestAbsY);
            }

            for(Position p : inputPositions) {
                double x;
                double y;
                if(distance == 0) {
                    x = width / 2;
                    y = height / 2;
                } else {
                    if(p.getX() <= 0) {
                        x = (width / 2) + (Math.abs(p.getX()) * distance);
                    } else {
                        x = (width / 2) - (p.getX() * distance);
                    }
                    if(p.getY() <= 0) {
                        y = (height / 2) + (Math.abs(p.getY()) * distance);
                    } else {
                        y = (height / 2) - (p.getY() * distance);
                    }
                }
                screenPositions.add(new Position(x, y));
            }
        }
        return screenPositions;
    }

    public Position calculateRealCoordinates(float x, float y) {
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

                LinkedList<Position> screenPositions = calculateScreenCoordinates(width, height);

                mFrameLayout.addView(new MapView(NavigationActivity.this, screenPositions, width, height));
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mFrameLayout = (FrameLayout)findViewById(R.id.frameLayout);

        initFrameLayoutListener();
    }
}
