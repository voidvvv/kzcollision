package com.voidvvv.kzcollision.sdk;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight playback state machine for a KZAnimation.
 * Encapsulates position, scale, rotation, and flip transforms so that
 * callers can draw sprites and retrieve world-space collision boxes
 * without manual coordinate math.
 */
public class KZAnimationPlayer {
    private final KZAnimation animation;
    private int currentFrame;
    private float stateTime;
    private boolean playing = true;
    private boolean looping = true;

    // Transform state
    private float x = 0;
    private float y = 0;
    private float scaleX = 1;
    private float scaleY = 1;
    private float rotation = 0;
    private boolean flipX = false;
    private boolean flipY = false;

    public KZAnimationPlayer(KZAnimation animation) {
        this.animation = animation;
        this.currentFrame = 0;
        this.stateTime = 0f;
    }

    // --- Playback ---

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

    // --- Existing accessors (local-space, unchanged) ---

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

    // --- Transform setters / getters ---

    public void setPosition(float x, float y) { this.x = x; this.y = y; }
    public float getX() { return x; }
    public float getY() { return y; }

    public void setScale(float scaleX, float scaleY) { this.scaleX = scaleX; this.scaleY = scaleY; }
    public void setScale(float scale) { this.scaleX = scale; this.scaleY = scale; }
    public float getScaleX() { return scaleX; }
    public float getScaleY() { return scaleY; }

    public void setRotation(float degrees) { this.rotation = degrees; }
    public float getRotation() { return rotation; }

    public void setFlipX(boolean flipX) { this.flipX = flipX; }
    public boolean isFlipX() { return flipX; }

    public void setFlipY(boolean flipY) { this.flipY = flipY; }
    public boolean isFlipY() { return flipY; }

    // --- Draw info ---

    /**
     * Computes all parameters needed to draw the current frame with the
     * current transform (position, scale, rotation, flip).
     *
     * <p>The returned {@link KZDrawInfo} can be used directly:
     * <pre>
     *   player.computeDrawInfo().draw(batch);
     * </pre>
     */
    public KZDrawInfo computeDrawInfo() {
        float effectiveSX = effectiveScaleX();
        float effectiveSY = effectiveScaleY();
        float ox = animation.getOriginX(currentFrame);
        float oy = animation.getOriginY(currentFrame);
        TextureRegion region = animation.getRegion(currentFrame);
        float w = region != null ? region.getRegionWidth() : 0f;
        float h = region != null ? region.getRegionHeight() : 0f;
        return new KZDrawInfo(region, x, y, ox, oy, w, h,
                effectiveSX, effectiveSY, rotation);
    }

    /**
     * Convenience method: draws the current frame with the current transform.
     * Equivalent to {@code computeDrawInfo().draw(batch)}.
     */
    public void draw(SpriteBatch batch) {
        computeDrawInfo().draw(batch);
    }

    // --- World-space collision boxes ---

    /**
     * Returns collision boxes transformed to world coordinates, accounting for
     * position, scale, flip, and rotation.
     *
     * <p>Each box's four corners are transformed and the enclosing
     * axis-aligned bounding box (AABB) is returned. This produces a
     * conservative approximation for rotation that is compatible with
     * {@link CollisionQuery#overlaps(List, List)}.
     */
    public List<CollisionBox> computeWorldCollisionBoxes() {
        float effectiveSX = effectiveScaleX();
        float effectiveSY = effectiveScaleY();
        float ox = animation.getOriginX(currentFrame);
        float oy = animation.getOriginY(currentFrame);
        List<CollisionBox> localBoxes = animation.getCollisionBoxes(currentFrame);

        List<CollisionBox> result = new ArrayList<>(localBoxes.size());
        float rad = (float) Math.toRadians(rotation);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        for (CollisionBox box : localBoxes) {
            float[] localXs = {
                (box.getX() - ox) * effectiveSX,
                (box.getX() + box.getWidth() - ox) * effectiveSX
            };
            float[] localYs = {
                (box.getY() - oy) * effectiveSY,
                (box.getY() + box.getHeight() - oy) * effectiveSY
            };

            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;

            for (float lx : localXs) {
                for (float ly : localYs) {
                    float rx = lx * cos - ly * sin + x;
                    float ry = lx * sin + ly * cos + y;
                    minX = Math.min(minX, rx);
                    minY = Math.min(minY, ry);
                    maxX = Math.max(maxX, rx);
                    maxY = Math.max(maxY, ry);
                }
            }

            result.add(new CollisionBox(minX, minY, maxX - minX, maxY - minY, box.getLabel()));
        }

        return result;
    }

    private float effectiveScaleX() {
        return scaleX * (flipX ? -1f : 1f);
    }

    private float effectiveScaleY() {
        return scaleY * (flipY ? -1f : 1f);
    }
}
