package com.tudresden.navigationrobot;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

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
        Speak("I stop to look around.", "ExplorationStop", new Runnable() {
            @Override
            public void run() {
                activity.getExploration().stopExploration();
                activity.startMapActivity();
                activity.loomoRecognizer.startWakeUpListener();
            }
        });
    }

    protected void MovementMove (JSONArray entities) {
        if (entities.length() > 0) {
            try {
                String directionString = "";
                String degreesString = "";
                String distanceString = "";
                for (int i = 0; i < entities.length(); i++) {
                    JSONObject entity = entities.getJSONObject(i);
                    String value = "";
                    if (entity.get("type").toString().equals("Movement.Direction") && directionString == "") {
                        directionString = entity.get("entity").toString();
                    }

                    if (entity.get("type").toString().equals("Movement.Distance") && distanceString == "") {
                        distanceString = entity.get("entity").toString();
                    }

                    if (entity.get("type").toString().equals("Movement.Degrees") && degreesString == "") {
                        degreesString = entity.get("entity").toString();
                    }
                }

                if(degreesString != "") {
                    float degrees = Float.parseFloat(degreesString);
                    switch (directionString) {
                        case "right":
                            activity.getExploration().turn(-degrees);
                            break;
                        case "left": case "":
                            activity.getExploration().turn(degrees);
                            break;
                        default:
                            Speak("I can't do that.", "Movement.Move", new Runnable() {
                                @Override
                                public void run() {
                                    activity.loomoRecognizer.startWakeUpListener();
                                }
                            });
                    }
                } else if(distanceString == "") {
                    switch(directionString) {
                        case "right":
                            activity.getExploration().turn(-90);
                            break;
                        case "left":
                            activity.getExploration().turn(90);
                            break;
                        case "forward":case "forwards":
                            activity.getExploration().move(1);
                            break;
                        case "backward":case "backwards": case "back":
                            activity.getExploration().move(-1);
                            break;
                        default:
                            Speak("I can't do that.", "Movement.Move", new Runnable() {
                                @Override
                                public void run() {
                                    activity.loomoRecognizer.startWakeUpListener();
                                }
                            });
                    }
                } else {
                    float distance = Float.parseFloat(distanceString);
                    switch(directionString) {
                        case "": case "forward":case "forwards":
                            activity.getExploration().move(distance);
                            break;
                        case "backward":case "backwards": case "back":
                            activity.getExploration().move(-distance);
                            break;
                        default:
                            Speak("I can't do that.", "Movement.Move", new Runnable() {
                                @Override
                                public void run() {
                                    activity.loomoRecognizer.startWakeUpListener();
                                }
                            });
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception: ", e);
            }
        } else {
           Speak("I can't do that.", "Movement.Move", new Runnable() {
                @Override
                public void run() {
                    activity.loomoRecognizer.startWakeUpListener();
                }
            });
        }
    }
}
