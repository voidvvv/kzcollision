package com.voidvvv.kzcollision.editor.panels;

import com.voidvvv.kzcollision.core.model.Animation;
import com.voidvvv.kzcollision.core.model.AnimationFrame;
import com.voidvvv.kzcollision.editor.EditorState;
import imgui.ImGui;
import imgui.type.ImFloat;

public class AnimationControlsPanel {
    private final EditorStateProvider stateProvider;
    private final ImFloat durationBuf = new ImFloat(0.15f);

    public AnimationControlsPanel(EditorStateProvider stateProvider) {
        this.stateProvider = stateProvider;
    }

    public void render() {
        EditorState state = stateProvider.getState();
        Animation anim = state.getSelectedAnimation();
        int frameCount = (anim != null) ? anim.getFrames().size() : 0;
        int currentIdx = state.getCurrentFrameIndex();

        if (ImGui.begin("Controls")) {
            // Playback buttons
            if (ImGui.button("Play")) {
                state.setPlaying(true);
            }
            ImGui.sameLine();
            if (ImGui.button("Pause")) {
                state.setPlaying(false);
            }
            ImGui.sameLine();
            if (ImGui.button("Stop")) {
                state.setPlaying(false);
                state.setCurrentFrameIndex(0);
                state.setPlaybackTimer(0f);
            }

            ImGui.sameLine();
            ImGui.text(" | ");
            ImGui.sameLine();

            // Prev/Next
            if (ImGui.button("|<")) {
                state.setCurrentFrameIndex(Math.max(0, currentIdx - 1));
                state.setPlaybackTimer(0f);
            }
            ImGui.sameLine();
            if (ImGui.button(">|")) {
                state.setCurrentFrameIndex(Math.min(frameCount - 1, currentIdx + 1));
                state.setPlaybackTimer(0f);
            }

            ImGui.sameLine();
            ImGui.text(" | ");
            ImGui.sameLine();

            // Frame counter
            ImGui.text("Frame: " + (currentIdx + 1) + " / " + frameCount);

            ImGui.sameLine();
            ImGui.text(" | ");
            ImGui.sameLine();

            // Duration
            AnimationFrame frame = state.getCurrentFrame();
            if (frame != null) {
                durationBuf.set(frame.getDuration());
                if (ImGui.inputFloat("Duration (s)", durationBuf, 0.01f, 0.1f, "%.2f")) {
                    frame.setDuration(Math.max(0.01f, durationBuf.get()));
                }
            }

            // Playback logic: advance frame when timer exceeds duration
            if (state.isPlaying() && frame != null && frameCount > 0) {
                float delta = ImGui.getIO().getDeltaTime();
                state.setPlaybackTimer(state.getPlaybackTimer() + delta);
                if (state.getPlaybackTimer() >= frame.getDuration()) {
                    state.setPlaybackTimer(0f);
                    int nextIdx = (currentIdx + 1) % frameCount;
                    state.setCurrentFrameIndex(nextIdx);
                }
            }
        }
        ImGui.end();
    }
}
