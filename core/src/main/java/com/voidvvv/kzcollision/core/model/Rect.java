package com.voidvvv.kzcollision.core.model;

public class Rect {
    public final float x;
    public final float y;
    public final float width;
    public final float height;

    public Rect(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean contains(float px, float py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
}
