package com.voidvvv.kzcollision.editor.panels;

import com.voidvvv.kzcollision.core.model.Animation;
import com.voidvvv.kzcollision.core.model.AnimationFrame;
import com.voidvvv.kzcollision.core.model.SpriteFrame;
import com.voidvvv.kzcollision.editor.EditorState;
import imgui.ImGui;

import java.util.ArrayList;
import java.util.List;

public class SpriteFramesPanel {

    private final EditorStateProvider stateProvider;

    public SpriteFramesPanel(EditorStateProvider stateProvider) {
        this.stateProvider = stateProvider;
    }

    public void render() {
        if (ImGui.begin("Sprite Frames")) {
            renderFrameList();
        }
        ImGui.end();
    }

    private void renderFrameList() {
        EditorState state = stateProvider.getState();
        List<SpriteFrame> frames = state.getProject().getSpriteFrames();

        // Copy to avoid ConcurrentModificationException when removing during iteration
        List<SpriteFrame> snapshot = new ArrayList<>(frames);
        boolean removed = false;

        for (SpriteFrame frame : snapshot) {
            String label = buildLabel(frame);

            if (ImGui.selectable(label)) {
                state.setSelectedSpriteFrameId(frame.getId());
            }

            // Double-click: add to current animation
            if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0)) {
                addToCurrentAnimation(frame);
            }

            // Right-click context menu
            String popupId = "frame_ctx_" + frame.getId();
            if (ImGui.beginPopupContextItem(popupId)) {
                if (ImGui.menuItem("Add to Animation")) {
                    addToCurrentAnimation(frame);
                }
                if (ImGui.menuItem("Delete")) {
                    frames.remove(frame);
                    removed = true;
                }
                ImGui.endPopup();
            }

            if (removed) {
                break;
            }
        }
    }

    private String buildLabel(SpriteFrame frame) {
        return frame.getSourceRegionName() != null ? frame.getSourceRegionName() : "(unnamed)";
    }

    private void addToCurrentAnimation(SpriteFrame frame) {
        EditorState state = stateProvider.getState();
        Animation anim = state.getSelectedAnimation();
        if (anim == null) {
            return;
        }

        AnimationFrame animFrame = new AnimationFrame();
        animFrame.setSpriteFrameId(frame.getId());
        anim.getFrames().add(animFrame);
    }
}
