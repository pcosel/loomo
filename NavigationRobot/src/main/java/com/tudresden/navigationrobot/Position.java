package com.tudresden.navigationrobot;

public class Position {

    public float x;
    public float y;
    public Position start;

    public Position(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Position getStart() {
        return this.start;
    }

    public void setStart(Position start) {
        this.start = start;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }

}
