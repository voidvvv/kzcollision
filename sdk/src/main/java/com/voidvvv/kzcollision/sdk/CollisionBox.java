package com.voidvvv.kzcollision.sdk;

public final class CollisionBox {
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final String label;

    public CollisionBox(float x, float y, float width, float height, String label) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public String getLabel() { return label; }
}
