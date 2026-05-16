package com.voidvvv.kzcollision.editor.project;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.ObjectSet;
import com.voidvvv.kzcollision.core.model.AssetType;
import com.voidvvv.kzcollision.core.model.Project;
import com.voidvvv.kzcollision.core.model.Rect;
import com.voidvvv.kzcollision.core.model.SourceAsset;
import com.voidvvv.kzcollision.core.model.SourceRegion;
import com.voidvvv.kzcollision.editor.EditorState;

import java.io.File;

public class EditorTextureLoader {
    public void reload(EditorState state) {
        clearTextureCache(state);
        EditorProjectContext context = state.getProjectContext();
        Project project = state.getProject();
        if (context == null || project == null) {
            return;
        }

        for (SourceAsset asset : project.getSourceAssets()) {
            if (asset.getInternalPath() == null) {
                continue;
            }
            try {
                File file = new File(context.getAssetsRoot(), AssetIndex.normalize(asset.getInternalPath()));
                if (asset.getType() == AssetType.SINGLE) {
                    Texture texture = new Texture(Gdx.files.absolute(file.getAbsolutePath()));
                    state.getTextureCache().put(asset.getInternalPath(), texture);
                    ensureSingleRegionBounds(asset, texture);
                } else if (asset.getType() == AssetType.ATLAS) {
                    TextureAtlas atlas = new TextureAtlas(Gdx.files.absolute(file.getAbsolutePath()));
                    ObjectSet<Texture> textures = atlas.getTextures();
                    if (textures.size > 0) {
                        state.getTextureCache().put(asset.getInternalPath(), textures.first());
                    }
                }
            } catch (Exception e) {
                Gdx.app.log("EditorTextureLoader", "Failed to load texture: " + asset.getInternalPath(), e);
            }
        }
    }

    public void clearTextureCache(EditorState state) {
        for (Texture texture : state.getTextureCache().values()) {
            texture.dispose();
        }
        state.getTextureCache().clear();
    }

    private void ensureSingleRegionBounds(SourceAsset asset, Texture texture) {
        if (asset.getRegions().isEmpty()) {
            asset.getRegions().add(new SourceRegion(asset.getInternalPath(), asset.getId(),
                    new Rect(0, 0, texture.getWidth(), texture.getHeight())));
            return;
        }
        SourceRegion region = asset.getRegions().get(0);
        if (region.getBounds() == null) {
            region.setBounds(new Rect(0, 0, texture.getWidth(), texture.getHeight()));
        }
    }
}
