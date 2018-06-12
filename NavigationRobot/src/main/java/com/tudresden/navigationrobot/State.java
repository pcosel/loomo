package com.tudresden.navigationrobot;

/**
 * Represents the current state of the robot.
 *
 * @author Nadja Konrad
 */
public enum State {

    /**
     * The robot is in the initial phase of the exploration process (no wall has been found yet).
     */
    START,

    /**
     * The robot is walking forward to the next checkpoint.
     */
    WALKING,

    /**
     * The robot has just performed a right turn in order to check if the wall next to it has ended.
     */
    CHECKING_WALL,

    /**
     * The robot detected an obstacle and has just performed a turn in order to avoid running into
     * the obstacle.
     */
    OBSTACLE_DETECTED,

    /**
     * The robot detected a corner during the regular wall check and has just turned left to start
     * walking around the corner.
     */
    CORNER_LEFT,

    /**
     * The robot has just walked a bit further in order to avoid getting stuck at the corner with its
     * right wheel.
     */
    CORNER_FORWARD,

    /**
     * The robot has just turned right in order to pass the corner.
     */
    CORNER_RIGHT,

    /**
     * The robot hast just passed the corner and is now following the new wall.
     */
    CORNER_DONE,

    /**
     * This is the state for demonstrating the basic movements of the robot (move, turn).
     * In this case nothing should happen when a checkpoint is reached or an obstacle appears.
     * Otherwise the robot would start performing exploration routines.
     */
    BASIC_MOVEMENTS;

}
