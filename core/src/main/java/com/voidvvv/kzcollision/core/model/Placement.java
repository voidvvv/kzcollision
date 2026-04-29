package com.voidvvv.kzcollision.core.model;

public class Placement {
    private String spriteFrameId;
    private float offsetX;
    private float offsetY;

    public Placement() {}

    public Placement(String spriteFrameId, float offsetX, float offsetY) {
        this.spriteFrameId = spriteFrameId;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public String getSpriteFrameId() { return spriteFrameId; }
    public void setSpriteFrameId(String spriteFrameId) { this.spriteFrameId = spriteFrameId; }
    public float getOffsetX() { return offsetX; }
    public void setOffsetX(float offsetX) { this.offsetX = offsetX; }
    public float getOffsetY() { return offsetY; }
    public void setOffsetY(float offsetY) { this.offsetY = offsetY; }
}
