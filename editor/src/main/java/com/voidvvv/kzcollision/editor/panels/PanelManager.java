package com.voidvvv.kzcollision.editor.panels;

import imgui.ImGui;
import imgui.flag.ImGuiCond;

public class PanelManager {
    private final EditorStateProvider stateProvider;
    private final SourceImagesPanel sourceImagesPanel;
    private final SpriteFramesPanel spriteFramesPanel;
    private final AnimationsPanel animationsPanel;
    private final AnimationControlsPanel animationControlsPanel;
    private final PropertiesPanel propertiesPanel;

    public PanelManager(EditorStateProvider stateProvider) {
        this.stateProvider = stateProvider;
        this.sourceImagesPanel = new SourceImagesPanel(stateProvider);
        this.spriteFramesPanel = new SpriteFramesPanel(stateProvider);
        this.animationsPanel = new AnimationsPanel(stateProvider);
        this.animationControlsPanel = new AnimationControlsPanel(stateProvider);
        this.propertiesPanel = new PropertiesPanel(stateProvider);
    }

    public void render() {
        renderMenuBar();

        // Source Images panel (left-top)
        ImGui.setNextWindowPos(0, 20, ImGuiCond.Once);
        ImGui.setNextWindowSize(220, 350, ImGuiCond.Once);
        sourceImagesPanel.render();

        // Sprite Frames panel (left-bottom)
        ImGui.setNextWindowPos(0, 370, ImGuiCond.Once);
        ImGui.setNextWindowSize(220, 350, ImGuiCond.Once);
        spriteFramesPanel.render();

        // Animations panel (right-top)
        ImGui.setNextWindowPos(1040, 20, ImGuiCond.Once);
        ImGui.setNextWindowSize(240, 200, ImGuiCond.Once);
        animationsPanel.render();

        // Properties panel (right-bottom)
        ImGui.setNextWindowPos(1040, 220, ImGuiCond.Once);
        ImGui.setNextWindowSize(240, 350, ImGuiCond.Once);
        propertiesPanel.render();

        // Controls panel (bottom bar)
        ImGui.setNextWindowPos(220, 680, ImGuiCond.Once);
        ImGui.setNextWindowSize(820, 40, ImGuiCond.Once);
        animationControlsPanel.render();
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
