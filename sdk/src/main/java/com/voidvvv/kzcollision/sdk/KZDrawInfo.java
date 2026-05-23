package com.voidvvv.kzcollision.sdk;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Immutable snapshot of all parameters needed to draw the current animation frame.
 * Produced by {@link KZAnimationPlayer#computeDrawInfo()}.
 *
 * <p>Usage:
 * <pre>
 *   KZDrawInfo info = player.computeDrawInfo();
 *   info.draw(batch);
 *   // or equivalently:
 *   batch.draw(info.region, info.drawX, info.drawY, info.originX, info.originY,
 *              info.width, info.height, info.scaleX, info.scaleY, info.rotation);
 * </pre>
 */
public final class KZDrawInfo {
    /** The texture region for the current frame. */
    public final TextureRegion region;

    /** Draw position — the KZ-origin point lands here on screen. */
    public final float x;
    public final float y;

    /** Bottom-left draw position to pass to SpriteBatch. */
    public final float drawX;
    public final float drawY;

    /** Rotation/scale pivot relative to the sprite's bottom-left corner. */
    public final float originX;
    public final float originY;

    /** Draw dimensions in pixels. */
    public final float width;
    public final float height;

    /** Scale factors. Negative values indicate mirroring (flip). */
    public final float scaleX;
    public final float scaleY;

    /** Rotation in degrees (counter-clockwise). */
    public final float rotation;

    public KZDrawInfo(TextureRegion region, float x, float y,
                      float originX, float originY,
                      float width, float height,
                      float scaleX, float scaleY, float rotation) {
        this.region = region;
        this.x = x;
        this.y = y;
        this.drawX = x - originX;
        this.drawY = y - originY;
        this.originX = originX;
        this.originY = originY;
        this.width = width;
        this.height = height;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.rotation = rotation;
    }

    /**
     * Convenience method: draws this frame using the full-featured
     * {@link SpriteBatch#draw(TextureRegion, float, float, float, float, float, float, float, float, float)}
     * overload with origin, scale, and rotation.
     */
    public void draw(SpriteBatch batch) {
        batch.draw(region, drawX, drawY, originX, originY, width, height, scaleX, scaleY, rotation);
    }
}
