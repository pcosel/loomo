package com.tudresden.navigationrobot;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import java.util.LinkedList;

/**
 * This Activity shows the map of the previously explored room.
 *
 * @author Nadja Konrad
 */
public class MapActivity extends Activity {

    /**
     * The layout that contains the map.
     */
    private FrameLayout mFrameLayout;

    /**
     * All the positions that the robot has reached during the exploration phase.
     */
    private LinkedList<Position> mInputPositions = new LinkedList<>();

    private StorageHelper mFileHelper;

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

                mFrameLayout.addView(new MapView(MapActivity.this, mInputPositions, width, height));
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
            mFileHelper = new StorageHelper(this);
        }

        if(mFileHelper.fileExists()) {
            mInputPositions = mFileHelper.convertPositions();
        }

        initFrameLayoutListener();
    }
}
