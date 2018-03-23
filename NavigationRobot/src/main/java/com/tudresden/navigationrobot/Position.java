package com.tudresden.navigationrobot;

/**
 * Represents a position in the coordinate system.
 *
 * @author Nadja Konrad
 */
public class Position {

    /**
     * The x-coordinate of this Position.
     */
    private double x;

    /**
     * The y-coordinate of this Position.
     */
    private double y;

    /**
     * The current orientation (respectively the direction of movement) of the robot. At the
     * starting point the orientation is always FORWARD.
     */
    private Orientation orientation;

    /**
     * Creates a new Position.
     * @param x the x-coordinate of this Position
     * @param y the y-coordinate of this Position
     * @param orientation the orientation of the robot at this Position
     */
    public Position(double x, double y, Orientation orientation) {
        this.x = x;
        this.y = y;
        this.orientation = orientation;
    }

    /**
     * Creates a new Position without orientation.
     * @param x the x-coordinate of this Position
     * @param y the y-coordinate of this Position
     */
    public Position(double x, double y) {
        this(x, y, null);
    }

    /**
     * @return the x-coordinate of this Position
     */
    public double getX() {
        return x;
    }

    /**
     * @return the y-coordinate of this Position
     */
    public double getY() {
        return y;
    }

    /**
     * @return the orientation of the robot at this Position
     */
    public Orientation getOrientation() {
        return orientation;
    }

}
