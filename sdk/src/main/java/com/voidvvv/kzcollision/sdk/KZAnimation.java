package com.voidvvv.kzcollision.sdk;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.voidvvv.kzcollision.core.model.Animation;
import com.voidvvv.kzcollision.core.model.AnimationFrame;
import com.voidvvv.kzcollision.core.model.AssetType;
import com.voidvvv.kzcollision.core.model.Project;
import com.voidvvv.kzcollision.core.model.Rect;
import com.voidvvv.kzcollision.core.model.SourceAsset;
import com.voidvvv.kzcollision.core.model.SpriteFrame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A resolved animation with TextureRegions and collision boxes for each frame.
 */
public class KZAnimation {
    private final String name;
    private final int frameCount;
    private final TextureRegion[] regions;
    private final float[] durations;
    private final float[] originXs;
    private final float[] originYs;
    private final List<List<CollisionBox>> frameCollisionBoxes;

    KZAnimation(Animation animation, Project project,
                Map<String, Texture> textureCache, Map<String, TextureAtlas> atlasCache) {
        this.name = animation.getName();
        this.frameCount = animation.getFrames().size();
        this.regions = new TextureRegion[frameCount];
        this.durations = new float[frameCount];
        this.originXs = new float[frameCount];
        this.originYs = new float[frameCount];
        this.frameCollisionBoxes = new ArrayList<>();

        for (int i = 0; i < frameCount; i++) {
            AnimationFrame frame = animation.getFrames().get(i);
            durations[i] = frame.getDuration();
            originXs[i] = frame.getOriginX();
            originYs[i] = frame.getOriginY();

            // Resolve collision boxes
            List<CollisionBox> boxes = new ArrayList<>();
            for (com.voidvvv.kzcollision.core.model.CollisionBox box : frame.getCollisionBoxes()) {
                boxes.add(new CollisionBox(box.getX(), box.getY(), box.getWidth(), box.getHeight(), box.getLabel()));
            }
            frameCollisionBoxes.add(Collections.unmodifiableList(boxes));

            // Resolve TextureRegion
            regions[i] = resolveRegion(frame, project, textureCache, atlasCache);
        }
    }

    private TextureRegion resolveRegion(AnimationFrame frame, Project project,
                                         Map<String, Texture> textureCache, Map<String, TextureAtlas> atlasCache) {
        SpriteFrame spriteFrame = project.findSpriteFrame(frame.getSpriteFrameId());
        if (spriteFrame == null) return null;

        SourceAsset sourceAsset = project.findSourceAsset(spriteFrame.getSourceAssetId());
        if (sourceAsset == null || sourceAsset.getInternalPath() == null) return null;

        if (sourceAsset.getType() == AssetType.SINGLE) {
            Texture texture = textureCache.get(sourceAsset.getInternalPath());
            if (texture == null) return null;
            Rect sub = spriteFrame.getSubRegion();
            if (sub != null) {
                return new TextureRegion(texture, (int) sub.x, (int) sub.y, (int) sub.width, (int) sub.height);
            }
            return new TextureRegion(texture);
        } else if (sourceAsset.getType() == AssetType.ATLAS) {
            TextureAtlas atlas = atlasCache.get(sourceAsset.getInternalPath());
            if (atlas == null) return null;
            String regionName = spriteFrame.getSourceRegionName();
            TextureAtlas.AtlasRegion atlasRegion = atlas.findRegion(regionName);
            if (atlasRegion == null) return null;
            Rect sub = spriteFrame.getSubRegion();
            if (sub != null) {
                return new TextureRegion(atlasRegion.getTexture(),
                        atlasRegion.getRegionX() + (int) sub.x,
                        atlasRegion.getRegionY() + (int) sub.y,
                        (int) sub.width, (int) sub.height);
            }
            return new TextureRegion(atlasRegion);
        }
        return null;
    }

    public String getName() { return name; }
    public int getFrameCount() { return frameCount; }
    public float getDuration(int frameIndex) { return durations[frameIndex]; }
    public TextureRegion getRegion(int frameIndex) { return regions[frameIndex]; }
    public float getOriginX(int frameIndex) { return originXs[frameIndex]; }
    public float getOriginY(int frameIndex) { return originYs[frameIndex]; }
    public List<CollisionBox> getCollisionBoxes(int frameIndex) { return frameCollisionBoxes.get(frameIndex); }
    public float getRegionWidth(int frameIndex) { return regions[frameIndex].getRegionWidth(); }
    public float getRegionHeight(int frameIndex) { return regions[frameIndex].getRegionHeight(); }
}
