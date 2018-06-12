package com.tudresden.navigationrobot;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends de.tud.loomospeech.MainActivity implements View.OnClickListener {

    /**
     * The Exploration instance that is used for starting and stopping the exploration process and
     * collecting the positions of the robot.
     */
    private Exploration mExploration;

    /**
     * The StorageHelper instance that is used for storing an retrieving data.
     */
    private StorageHelper mFileHelper;

    /**
     * A counter for handling exiting the app by pressing the back button.
     */
    private int mPress = 0;

    public Exploration getExploration() {
        return mExploration;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.intentsLibrary = new IntentsLibraryNavigation(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ActionBar actionBar = getActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Button startButton = (Button)findViewById(R.id.buttonStart);
        startButton.setOnClickListener(this);
        Button stopButton = (Button)findViewById(R.id.buttonStop);
        stopButton.setOnClickListener(this);

        if(mExploration == null) {
            mExploration = new Exploration(this);
        }

        if(mFileHelper == null) {
            mFileHelper = new StorageHelper(this);
        }

        mExploration.initListeners();
        mExploration.bindServices();
    }

    @Override
    public void onStart() {
        super.onStart();
        mExploration.initListeners();
        mExploration.bindServices();
    }

    @Override
    public void onStop() {
        super.onStop();
        mExploration.unbindServices();
    }

    /**
     * Starts a new MapActivity that displays a map of the last executed exploration, if existent.
     */
    public void startMapActivity() {
        if(mExploration.getPositions().size() == 0) {
            // If no new exploration was performed and no positions from a previous
            // exploration can be found, tell the user to perform an exploration in order
            // to be able to create the map
            if(!mFileHelper.fileExists()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("No map could be created!");
                builder.setMessage("Please run the exploration process first.");
                builder.setPositiveButton("OK", null);
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                // If no new exploration was performed but positions from a previous exploration were
                // found, use the positions from the previous exploration to create the map
                Intent intent = new Intent(this, MapActivity.class);
                startActivity(intent);
            }
        } else {
            // An exploration was performed
            // Store the new positions and use those to create the map
            mFileHelper.storePositions(mExploration.getPositions());
            Intent intent = new Intent(this, MapActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.buttonStart:
                //mExploration.startExploration();
                mExploration.move(1);
                break;
            case R.id.buttonStop:
                mExploration.stopExploration();
                startMapActivity();
                break;

        }
    }

    @Override
    public void onBackPressed() {
        if(mPress == 0) {
            Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show();
        }
        mPress++;
        if(mPress == 2) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(null != this.getCurrentFocus()) {
            InputMethodManager mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            return mInputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
        }
        return super.onTouchEvent(event);
    }
}
