package com.voidvvv.kzcollision.sdk;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Disposable;
import com.voidvvv.kzcollision.core.model.Animation;
import com.voidvvv.kzcollision.core.model.AssetType;
import com.voidvvv.kzcollision.core.model.Project;
import com.voidvvv.kzcollision.core.model.SourceAsset;
import com.voidvvv.kzcollision.core.serialization.ProjectSerializer;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Main SDK entry point. Loads a collision JSON file and resolves textures
 * from the assets root, providing render-ready animations with collision boxes.
 *
 * Usage:
 * <pre>
 *   KZProject project = KZProject.load(Gdx.files.internal("collision/collision.json"));
 *   KZAnimation walk = project.getAnimation("walk");
 *   KZAnimationPlayer player = new KZAnimationPlayer(walk);
 *   // in render loop:
 *   player.update(delta);
 *   batch.draw(player.getCurrentRegion(), x - player.getCurrentOriginX(), y - player.getCurrentOriginY());
 *   List&lt;CollisionBox&gt; boxes = player.getCurrentCollisionBoxes();
 *   // on cleanup:
 *   project.dispose();
 * </pre>
 */
public class KZProject implements Disposable {
    private final Map<String, Texture> textureCache = new HashMap<>();
    private final Map<String, TextureAtlas> atlasCache = new HashMap<>();
    private final Map<String, KZAnimation> animations = new LinkedHashMap<>();

    private KZProject(Project project, FileHandle assetsRoot) {
        // Load textures for all source assets
        for (SourceAsset asset : project.getSourceAssets()) {
            if (asset.getInternalPath() == null) continue;
            try {
                FileHandle handle = assetsRoot.child(asset.getInternalPath());
                if (asset.getType() == AssetType.SINGLE) {
                    textureCache.put(asset.getInternalPath(), new Texture(handle));
                } else if (asset.getType() == AssetType.ATLAS) {
                    atlasCache.put(asset.getInternalPath(), new TextureAtlas(handle));
                }
            } catch (Exception e) {
                Gdx.app.log("KZProject", "Failed to load asset: " + asset.getInternalPath(), e);
            }
        }

        // Build resolved animations
        for (Animation anim : project.getAnimations()) {
            animations.put(anim.getName(), new KZAnimation(anim, project, textureCache, atlasCache));
        }
    }

    /**
     * Loads a collision JSON file. The assets root is inferred as
     * collisionFile.parent().parent() (e.g., "collision/collision.json" -> assets root).
     */
    public static KZProject load(FileHandle collisionFile) {
        FileHandle assetsRoot = collisionFile.parent().parent();
        return load(assetsRoot, collisionFile);
    }

    /**
     * Loads a collision JSON file with an explicit assets root.
     */
    public static KZProject load(FileHandle assetsRoot, FileHandle collisionFile) {
        try {
            ProjectSerializer serializer = new ProjectSerializer();
            Project project = serializer.load(collisionFile.file());
            return new KZProject(project, assetsRoot);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load collision project: " + collisionFile.path(), e);
        }
    }

    /**
     * Loads a collision JSON file from a java.io.File with an explicit assets root.
     */
    public static KZProject loadFromFile(File assetsRoot, File collisionFile) {
        return load(new FileHandle(assetsRoot), new FileHandle(collisionFile));
    }

    public KZAnimation getAnimation(String name) {
        return animations.get(name);
    }

    public Set<String> getAnimationNames() {
        return Collections.unmodifiableSet(animations.keySet());
    }

    @Override
    public void dispose() {
        for (Texture texture : textureCache.values()) {
            texture.dispose();
        }
        textureCache.clear();
        for (TextureAtlas atlas : atlasCache.values()) {
            atlas.dispose();
        }
        atlasCache.clear();
    }
}
