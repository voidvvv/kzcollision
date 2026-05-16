package com.voidvvv.kzcollision.sdk;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.Collections;
import java.util.List;

/**
 * Lightweight playback state machine for a KZAnimation.
 */
public class KZAnimationPlayer {
    private final KZAnimation animation;
    private int currentFrame;
    private float stateTime;
    private boolean playing = true;
    private boolean looping = true;

    public KZAnimationPlayer(KZAnimation animation) {
        this.animation = animation;
        this.currentFrame = 0;
        this.stateTime = 0f;
    }

    public void update(float delta) {
        if (!playing || animation.getFrameCount() == 0) return;
        stateTime += delta;
        float frameDuration = animation.getDuration(currentFrame);
        while (stateTime >= frameDuration) {
            stateTime -= frameDuration;
            int nextFrame = currentFrame + 1;
            if (nextFrame >= animation.getFrameCount()) {
                if (looping) {
                    currentFrame = 0;
                } else {
                    currentFrame = animation.getFrameCount() - 1;
                    playing = false;
                    stateTime = 0f;
                    return;
                }
            } else {
                currentFrame = nextFrame;
            }
            frameDuration = animation.getDuration(currentFrame);
        }
    }

    public TextureRegion getCurrentRegion() { return animation.getRegion(currentFrame); }
    public float getCurrentOriginX() { return animation.getOriginX(currentFrame); }
    public float getCurrentOriginY() { return animation.getOriginY(currentFrame); }
    public List<CollisionBox> getCurrentCollisionBoxes() { return animation.getCollisionBoxes(currentFrame); }
    public float getCurrentDuration() { return animation.getDuration(currentFrame); }
    public int getCurrentFrameIndex() { return currentFrame; }
    public KZAnimation getAnimation() { return animation; }
    public boolean isPlaying() { return playing; }
    public void setPlaying(boolean playing) { this.playing = playing; }
    public boolean isLooping() { return looping; }
    public void setLooping(boolean looping) { this.looping = looping; }
    public void setCurrentFrameIndex(int index) {
        this.currentFrame = Math.max(0, Math.min(index, animation.getFrameCount() - 1));
        this.stateTime = 0f;
    }
    public void reset() {
        this.currentFrame = 0;
        this.stateTime = 0f;
        this.playing = true;
    }
}
