package com.voidvvv.kzcollision.editor.panels;

import com.voidvvv.kzcollision.core.model.SourceAsset;
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
                    Texture tex = state.getTextureCache().get(asset.getFilePath());
                    if (tex == null) {
                        ImGui.textDisabled("Texture not loaded");
                    } else {
                        renderPreview(tex);
                        renderInfo(asset, tex);
                    }
                }
            }
        }
        ImGui.end();
    }

    private void renderPreview(Texture tex) {
        float availWidth = ImGui.getContentRegionAvailX();
        float imgWidth = tex.getWidth();
        float imgHeight = tex.getHeight();
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
        ImGui.image(texId, displayWidth, displayHeight, 0, 1, 1, 0);
    }

    private void renderInfo(SourceAsset asset, Texture tex) {
        String displayName = asset.getFilePath();
        int lastSep = Math.max(displayName.lastIndexOf('/'), displayName.lastIndexOf('\\'));
        if (lastSep >= 0) {
            displayName = displayName.substring(lastSep + 1);
        }
        ImGui.text(displayName);
        ImGui.sameLine();
        ImGui.textDisabled(tex.getWidth() + " x " + tex.getHeight());
    }
}
