package com.voidvvv.kzcollision.editor.panels;

import com.voidvvv.kzcollision.core.model.Animation;
import com.voidvvv.kzcollision.editor.EditorState;
import imgui.ImGui;
import imgui.type.ImString;

import java.util.ArrayList;
import java.util.List;

public class AnimationsPanel {
    private final EditorStateProvider stateProvider;
    private String renamingId = null;
    private final ImString renameBuffer = new ImString(64);

    public AnimationsPanel(EditorStateProvider stateProvider) {
        this.stateProvider = stateProvider;
    }

    public void render() {
        EditorState state = stateProvider.getState();
        if (ImGui.begin("Animations")) {
            if (ImGui.button("+ New Animation")) {
                Animation anim = new Animation("New Animation");
                state.getProject().getAnimations().add(anim);
                state.setSelectedAnimationId(anim.getId());
                state.setCurrentFrameIndex(0);
            }
            ImGui.separator();

            String selectedId = state.getSelectedAnimationId();
            // Use indexed loop to allow safe removal via break
            List<Animation> animations = state.getProject().getAnimations();
            for (int i = 0; i < animations.size(); i++) {
                Animation anim = animations.get(i);
                boolean isSelected = anim.getId().equals(selectedId);
                if (isSelected) {
                    ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 1.0f, 0.8f, 0.4f, 1.0f);
                }

                String displayName = anim.getName() + " (" + anim.getFrames().size() + " frames)";
                if (ImGui.selectable(displayName, isSelected)) {
                    state.setSelectedAnimationId(anim.getId());
                    state.setCurrentFrameIndex(0);
                }

                if (isSelected) {
                    ImGui.popStyleColor();
                }

                // Right-click context menu
                if (ImGui.beginPopupContextItem("anim_ctx_" + anim.getId())) {
                    if (ImGui.menuItem("Rename")) {
                        renamingId = anim.getId();
                        renameBuffer.set(anim.getName());
                    }
                    if (ImGui.menuItem("Delete")) {
                        animations.remove(i);
                        if (anim.getId().equals(selectedId)) {
                            state.setSelectedAnimationId(null);
                        }
                        ImGui.endPopup();
                        break;
                    }
                    ImGui.endPopup();
                }
            }

            // Rename popup
            if (renamingId != null) {
                ImGui.openPopup("Rename Animation");
                if (ImGui.beginPopupModal("Rename Animation")) {
                    ImGui.inputText("Name", renameBuffer);
                    if (ImGui.button("OK")) {
                        Animation anim = state.getProject().findAnimation(renamingId);
                        if (anim != null) anim.setName(renameBuffer.get());
                        renamingId = null;
                        ImGui.closeCurrentPopup();
                    }
                    ImGui.sameLine();
                    if (ImGui.button("Cancel")) {
                        renamingId = null;
                        ImGui.closeCurrentPopup();
                    }
                    ImGui.endPopup();
                }
            }
        }
        ImGui.end();
    }
}
