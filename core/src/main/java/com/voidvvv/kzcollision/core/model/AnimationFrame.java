package com.voidvvv.kzcollision.core.model;

import java.util.ArrayList;
import java.util.List;

public class AnimationFrame {
    private String spriteFrameId;
    private float duration;
    private float originX;
    private float originY;
    private List<Placement> placements;
    private List<CollisionBox> collisionBoxes;

    public AnimationFrame() {
        this.duration = 0.15f;
        this.placements = new ArrayList<>();
        this.collisionBoxes = new ArrayList<>();
    }

    public String getSpriteFrameId() { return spriteFrameId; }
    public void setSpriteFrameId(String spriteFrameId) { this.spriteFrameId = spriteFrameId; }
    public float getDuration() { return duration; }
    public void setDuration(float duration) { this.duration = duration; }
    public float getOriginX() { return originX; }
    public void setOriginX(float originX) { this.originX = originX; }
    public float getOriginY() { return originY; }
    public void setOriginY(float originY) { this.originY = originY; }
    public List<Placement> getPlacements() { return placements; }
    public void setPlacements(List<Placement> placements) { this.placements = placements; }
    public List<CollisionBox> getCollisionBoxes() { return collisionBoxes; }
    public void setCollisionBoxes(List<CollisionBox> collisionBoxes) { this.collisionBoxes = collisionBoxes; }
}
