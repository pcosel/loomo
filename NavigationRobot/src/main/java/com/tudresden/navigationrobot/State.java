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
     * The robot walks forward to the next checkpoint.
     */
    WALKING,

    /**
     * The robot performs a right turn in order to check if the wall next to it has ended.
     */
    CHECKING_WALL,

    /**
     * The robot detected an obstacle and performs a left turn.
     */
    OBSTACLE_DETECTED;

}
