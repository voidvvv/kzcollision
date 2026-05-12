package com.voidvvv.kzcollision.editor.panels;

import imgui.ImGui;
import imgui.flag.ImGuiCond;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class PanelManager {
    private final EditorStateProvider stateProvider;
    private final FileMenuHandler fileMenuHandler;
    private final SourceImagesPanel sourceImagesPanel;
    private final SpriteFramesPanel spriteFramesPanel;
    private final AnimationsPanel animationsPanel;
    private final AnimationControlsPanel animationControlsPanel;
    private final PropertiesPanel propertiesPanel;

    public PanelManager(EditorStateProvider stateProvider, FileMenuHandler fileMenuHandler) {
        this.stateProvider = stateProvider;
        this.fileMenuHandler = fileMenuHandler;
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
                if (ImGui.menuItem("New Project")) {
                    fileMenuHandler.newProject();
                }
                if (ImGui.menuItem("Open Project...")) {
                    showOpenDialog();
                }
                ImGui.separator();
                if (ImGui.menuItem("Save Project")) {
                    if (fileMenuHandler.hasCurrentFile()) {
                        fileMenuHandler.saveCurrentProject();
                    } else {
                        showSaveAsDialog();
                    }
                }
                if (ImGui.menuItem("Save As...")) {
                    showSaveAsDialog();
                }
                ImGui.separator();
                if (ImGui.menuItem("Export Collision JSON")) {
                    showExportDialog();
                }
                ImGui.endMenu();
            }
            ImGui.endMainMenuBar();
        }
    }

    private void showOpenDialog() {
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter(
                "KZCollision Project (*.kzcollision)", "kzcollision"));
            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                fileMenuHandler.openProject(chooser.getSelectedFile());
            }
        });
    }

    private void showSaveAsDialog() {
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter(
                "KZCollision Project (*.kzcollision)", "kzcollision"));
            int result = chooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = ensureExtension(chooser.getSelectedFile(), ".kzcollision");
                fileMenuHandler.saveProject(file);
            }
        });
    }

    private void showExportDialog() {
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter(
                "JSON File (*.json)", "json"));
            int result = chooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = ensureExtension(chooser.getSelectedFile(), ".json");
                fileMenuHandler.exportCollisionJson(file);
            }
        });
    }

    private static File ensureExtension(File file, String extension) {
        String path = file.getAbsolutePath();
        if (!path.toLowerCase().endsWith(extension)) {
            return new File(path + extension);
        }
        return file;
    }
}
