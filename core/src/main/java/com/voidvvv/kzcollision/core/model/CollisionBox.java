package com.voidvvv.kzcollision.core.model;

import java.util.UUID;

public class CollisionBox {
    private String id;
    private float x;
    private float y;
    private float width;
    private float height;
    private String label;

    public CollisionBox() {
        this.id = UUID.randomUUID().toString();
    }

    public CollisionBox(float x, float y, float width, float height, String label) {
        this.id = UUID.randomUUID().toString();
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }
    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public boolean containsPoint(float px, float py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
}
