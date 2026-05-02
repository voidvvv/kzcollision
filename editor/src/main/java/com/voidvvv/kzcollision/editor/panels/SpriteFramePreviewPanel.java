package com.voidvvv.kzcollision.editor.panels;

import com.voidvvv.kzcollision.core.model.SpriteFrame;
import com.voidvvv.kzcollision.core.model.SourceAsset;
import com.voidvvv.kzcollision.core.model.Rect;
import com.voidvvv.kzcollision.editor.EditorState;
import com.badlogic.gdx.graphics.Texture;
import imgui.ImGui;

public class SpriteFramePreviewPanel {
    private final EditorStateProvider stateProvider;

    public SpriteFramePreviewPanel(EditorStateProvider stateProvider) {
        this.stateProvider = stateProvider;
    }

    public void render() {
        if (ImGui.begin("SpriteFrame Preview")) {
            EditorState state = stateProvider.getState();
            String frameId = state.getSelectedSpriteFrameId();

            if (frameId == null) {
                ImGui.textDisabled("Select a sprite frame to preview");
            } else {
                SpriteFrame frame = state.getProject().findSpriteFrame(frameId);
                if (frame == null) {
                    ImGui.textDisabled("Sprite frame not found");
                } else {
                    Texture tex = resolveTexture(state, frame);
                    if (tex == null) {
                        ImGui.textDisabled("Texture not loaded");
                    } else {
                        renderPreview(tex, frame);
                        renderInfo(frame, tex);
                    }
                }
            }
        }
        ImGui.end();
    }

    private Texture resolveTexture(EditorState state, SpriteFrame frame) {
        SourceAsset asset = state.getProject().findSourceAsset(frame.getSourceAssetId());
        if (asset == null) return null;
        return state.getTextureCache().get(asset.getFilePath());
    }

    private void renderPreview(Texture tex, SpriteFrame frame) {
        float availWidth = ImGui.getContentRegionAvailX();
        Rect sub = frame.getSubRegion();

        float imgWidth, imgHeight;
        float uv0x, uv0y, uv1x, uv1y;

        if (sub != null) {
            imgWidth = sub.width;
            imgHeight = sub.height;
            float texW = tex.getWidth();
            float texH = tex.getHeight();
            uv0x = sub.x / texW;
            uv0y = 1f - (sub.y + sub.height) / texH;
            uv1x = (sub.x + sub.width) / texW;
            uv1y = 1f - sub.y / texH;
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

    private void renderInfo(SpriteFrame frame, Texture tex) {
        String name = frame.getSourceRegionName() != null ? frame.getSourceRegionName() : "(unnamed)";
        Rect sub = frame.getSubRegion();
        int w = sub != null ? (int) sub.width : tex.getWidth();
        int h = sub != null ? (int) sub.height : tex.getHeight();

        ImGui.text(name);
        ImGui.sameLine();
        ImGui.textDisabled(w + " x " + h);
    }
}
