package com.voidvvv.kzcollision.editor.panels;

import imgui.ImGui;
import imgui.flag.ImGuiCond;

public class PanelManager {
    private final EditorStateProvider stateProvider;

    public PanelManager(EditorStateProvider stateProvider) {
        this.stateProvider = stateProvider;
    }

    public void render() {
        renderMenuBar();

        // Placeholder panels -- will be replaced in Tasks 7-11
        ImGui.setNextWindowPos(0, 20, ImGuiCond.Once);
        ImGui.setNextWindowSize(220, 350, ImGuiCond.Once);
        if (ImGui.begin("Source Images")) {
            ImGui.text("Import images here");
            ImGui.end();
        }

        ImGui.setNextWindowPos(0, 370, ImGuiCond.Once);
        ImGui.setNextWindowSize(220, 350, ImGuiCond.Once);
        if (ImGui.begin("Sprite Frames")) {
            ImGui.text("Sprite frames here");
            ImGui.end();
        }

        ImGui.setNextWindowPos(1040, 20, ImGuiCond.Once);
        ImGui.setNextWindowSize(240, 200, ImGuiCond.Once);
        if (ImGui.begin("Animations")) {
            ImGui.text("Animations here");
            ImGui.end();
        }

        ImGui.setNextWindowPos(1040, 220, ImGuiCond.Once);
        ImGui.setNextWindowSize(240, 350, ImGuiCond.Once);
        if (ImGui.begin("Properties")) {
            ImGui.text("Properties here");
            ImGui.end();
        }

        ImGui.setNextWindowPos(220, 680, ImGuiCond.Once);
        ImGui.setNextWindowSize(820, 40, ImGuiCond.Once);
        if (ImGui.begin("Controls")) {
            ImGui.text("Controls here");
            ImGui.end();
        }
    }

    private void renderMenuBar() {
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("File")) {
                ImGui.menuItem("New Project");
                ImGui.menuItem("Open Project...");
                ImGui.separator();
                ImGui.menuItem("Save Project");
                ImGui.menuItem("Save As...");
                ImGui.separator();
                ImGui.menuItem("Export Collision JSON");
                ImGui.endMenu();
            }
            ImGui.endMainMenuBar();
        }
    }
}
