package com.tudresden.navigationrobot;

/**
 * This class models a position in the coordinate system.
 * @author Nadja Konrad
 */
public class Position {

    /**
     * The x-coordinate of this Position.
     */
    private float x;

    /**
     * The y-coordinate of this Position.
     */
    private float y;

    /**
     * Creates a new Position.
     * @param x the x-coordinate of this Position
     * @param y the y-coordinate of this Position
     */
    public Position(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * @return the x-coordinate of this Position
     */
    public float getX() {
        return x;
    }

    /**
     * @return the y-coordinate of this Position
     */
    public float getY() {
        return y;
    }

}
