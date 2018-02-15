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
     * Creates a new Position.
     * @param x the x-coordinate of this Position
     * @param y the y-coordinate of this Position
     */
    public Position(double x, double y) {
        this.x = x;
        this.y = y;
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

}
