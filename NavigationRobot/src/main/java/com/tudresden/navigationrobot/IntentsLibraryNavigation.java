package com.tudresden.navigationrobot;

import de.tud.loomospeech.IntentsLibrary;

public class IntentsLibraryNavigation extends de.tud.loomospeech.IntentsLibrary {
    protected static final String TAG = "IntentsLibraryNavigation";
    protected MainActivity activity;

    public IntentsLibraryNavigation(MainActivity myActivity) {
        super(myActivity);

        activity = myActivity;
    }

    public void ExplorationStart() {
        Speak("I'm going to look around.", "ExplorationStart", new Runnable() {
            @Override
            public void run() {
                activity.mExploration.startExploration();
                activity.loomoRecognizer.startWakeUpListener();
            }
        });
    }

    public void ExplorationStop() {
        Speak("I stop to look around.", "ExplorationStart", new Runnable() {
            @Override
            public void run() {
                activity.stopExploration();
                activity.loomoRecognizer.startWakeUpListener();
            }
        });
    }
}
