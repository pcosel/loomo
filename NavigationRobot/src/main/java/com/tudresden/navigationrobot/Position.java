package com.tudresden.navigationrobot;

/**
 * Represents a position that the robot reaches during exploration.
 *
 * @author Nadja Konrad
 */
public class Position {

    /**
     * The x-coordinate of this position.
     */
    private double x;

    /**
     * The y-coordinate of this position.
     */
    private double y;

    /**
     * The current orientation (respectively the direction of movement) of the robot. At the
     * starting point the orientation is always FORWARD.
     */
    private Orientation orientation;

    /**
     * Creates a new position.
     * @param x the x-coordinate of this position
     * @param y the y-coordinate of this position
     * @param orientation the orientation of the robot at this position
     */
    public Position(double x, double y, Orientation orientation) {
        this.x = x;
        this.y = y;
        this.orientation = orientation;
    }

    /**
     * Creates a new position without orientation.
     * @param x the x-coordinate of this position
     * @param y the y-coordinate of this position
     */
    public Position(double x, double y) {
        this(x, y, null);
    }

    /**
     * @return the x-coordinate of this position
     */
    public double getX() {
        return x;
    }

    /**
     * @return the y-coordinate of this position
     */
    public double getY() {
        return y;
    }

    /**
     * @return the orientation of the robot at this position
     */
    public Orientation getOrientation() {
        return orientation;
    }

}
