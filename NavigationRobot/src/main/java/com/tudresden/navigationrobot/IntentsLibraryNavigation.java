package com.tudresden.navigationrobot;

public class IntentsLibraryNavigation extends de.tud.loomospeech.IntentsLibrary {

    protected MainActivity activity;

    public IntentsLibraryNavigation(MainActivity myActivity) {
        super(myActivity);
        activity = myActivity;
    }

    public void ExplorationStart() {
        Speak("I'm going to look around.", "ExplorationStart", new Runnable() {
            @Override
            public void run() {
                activity.getExploration().startExploration();
                activity.loomoRecognizer.startWakeUpListener();
            }
        });
    }

    public void ExplorationStop() {
        Speak("I stop to look around.", "ExplorationStart", new Runnable() {
            @Override
            public void run() {
                activity.getExploration().stopExploration();
                activity.startMapActivity();
                activity.loomoRecognizer.startWakeUpListener();
            }
        });
    }
}
