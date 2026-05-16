package com.voidvvv.kzcollision.editor.panels;

import com.voidvvv.kzcollision.core.model.SourceAsset;
import com.voidvvv.kzcollision.core.model.SourceRegion;
import com.voidvvv.kzcollision.core.model.Rect;
import com.voidvvv.kzcollision.editor.EditorState;
import com.badlogic.gdx.graphics.Texture;
import imgui.ImGui;

public class SourceImagePreviewPanel {
    private final EditorStateProvider stateProvider;

    public SourceImagePreviewPanel(EditorStateProvider stateProvider) {
        this.stateProvider = stateProvider;
    }

    public void render() {
        if (ImGui.begin("Source Image Preview")) {
            EditorState state = stateProvider.getState();
            String assetId = state.getSelectedSourceAssetId();

            if (assetId == null) {
                ImGui.textDisabled("Select a source image to preview");
            } else {
                SourceAsset asset = state.getProject().findSourceAsset(assetId);
                if (asset == null) {
                    ImGui.textDisabled("Source image not found");
                } else {
                    String cacheKey = asset.getInternalPath() != null ? asset.getInternalPath() : asset.getFilePath();
                    Texture tex = state.getTextureCache().get(cacheKey);
                    if (tex == null) {
                        String path = asset.getInternalPath() != null ? asset.getInternalPath() : asset.getFilePath();
                        ImGui.textDisabled("Texture not loaded: " + path);
                    } else {
                        SourceRegion region = findSelectedRegion(asset, state.getSelectedSourceRegionId());
                        renderPreview(tex, region);
                        renderInfo(asset, tex, region);
                    }
                }
            }
        }
        ImGui.end();
    }

    private SourceRegion findSelectedRegion(SourceAsset asset, String regionId) {
        if (regionId == null || asset.getRegions().isEmpty()) {
            return null;
        }
        for (SourceRegion region : asset.getRegions()) {
            if (region.getId().equals(regionId)) {
                return region;
            }
        }
        return null;
    }

    private void renderPreview(Texture tex, SourceRegion region) {
        float availWidth = ImGui.getContentRegionAvailX();
        float imgWidth, imgHeight;
        float uv0x, uv0y, uv1x, uv1y;

        if (region != null && region.getBounds() != null) {
            Rect bounds = region.getBounds();
            imgWidth = bounds.width;
            imgHeight = bounds.height;
            float texW = tex.getWidth();
            float texH = tex.getHeight();
            uv0x = bounds.x / texW;
            uv0y = 1f - (bounds.y + bounds.height) / texH;
            uv1x = (bounds.x + bounds.width) / texW;
            uv1y = 1f - bounds.y / texH;
        } else {
            imgWidth = tex.getWidth();
            imgHeight = tex.getHeight();
            uv0x = 0;
            uv0y = 1;
            uv1x = 1;
            uv1y = 0;
        }

        float scale = availWidth / imgWidth;
        float displayWidth = imgWidth * scale;
        float displayHeight = imgHeight * scale;

        float maxHeight = 160f;
        if (displayHeight > maxHeight) {
            float heightScale = maxHeight / displayHeight;
            displayWidth *= heightScale;
            displayHeight *= heightScale;
        }

        int texId = tex.getTextureObjectHandle();
        ImGui.image(texId, displayWidth, displayHeight, uv0x, uv0y, uv1x, uv1y);
    }

    private void renderInfo(SourceAsset asset, Texture tex, SourceRegion region) {
        if (region != null && region.getName() != null) {
            ImGui.text(region.getName());
        } else {
            String displayName = asset.getInternalPath() != null ? asset.getInternalPath() : asset.getFilePath();
            int lastSep = Math.max(displayName.lastIndexOf('/'), displayName.lastIndexOf('\\'));
            if (lastSep >= 0) {
                displayName = displayName.substring(lastSep + 1);
            }
            ImGui.text(displayName);
        }
        ImGui.sameLine();
        if (region != null && region.getBounds() != null) {
            Rect b = region.getBounds();
            ImGui.textDisabled((int) b.width + " x " + (int) b.height);
        } else {
            ImGui.textDisabled(tex.getWidth() + " x " + tex.getHeight());
        }
    }
}
