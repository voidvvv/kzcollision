package com.voidvvv.kzcollision.editor.panels;

import imgui.ImGui;
import imgui.flag.ImGuiCond;

import com.voidvvv.kzcollision.core.model.Project;
import com.voidvvv.kzcollision.core.model.SourceAsset;
import com.voidvvv.kzcollision.core.model.SourceRegion;
import com.voidvvv.kzcollision.core.model.AssetType;
import com.voidvvv.kzcollision.core.model.Rect;
import com.voidvvv.kzcollision.core.serialization.ProjectSerializer;
import com.voidvvv.kzcollision.editor.EditorState;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PanelManager {
    private final EditorStateProvider stateProvider;
    private final SourceImagesPanel sourceImagesPanel;
    private final SpriteFramesPanel spriteFramesPanel;
    private final AnimationsPanel animationsPanel;
    private final AnimationControlsPanel animationControlsPanel;
    private final PropertiesPanel propertiesPanel;
    private final SourceImagePreviewPanel sourceImagePreviewPanel;
    private final SpriteFramePreviewPanel spriteFramePreviewPanel;

    private final ProjectSerializer serializer = new ProjectSerializer();
    private String currentFilePath;
    private final List<Runnable> pendingFileActions = new ArrayList<>();

    public PanelManager(EditorStateProvider stateProvider) {
        this.stateProvider = stateProvider;
        this.sourceImagesPanel = new SourceImagesPanel(stateProvider);
        this.spriteFramesPanel = new SpriteFramesPanel(stateProvider);
        this.animationsPanel = new AnimationsPanel(stateProvider);
        this.animationControlsPanel = new AnimationControlsPanel(stateProvider);
        this.propertiesPanel = new PropertiesPanel(stateProvider);
        this.sourceImagePreviewPanel = new SourceImagePreviewPanel(stateProvider);
        this.spriteFramePreviewPanel = new SpriteFramePreviewPanel(stateProvider);
    }

    public void render() {
        processPendingFileActions();
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

        // Source Image Preview panel (bottom-left)
        ImGui.setNextWindowPos(220, 500, ImGuiCond.Once);
        ImGui.setNextWindowSize(300, 200, ImGuiCond.Once);
        sourceImagePreviewPanel.render();

        // SpriteFrame Preview panel (bottom-center)
        ImGui.setNextWindowPos(520, 500, ImGuiCond.Once);
        ImGui.setNextWindowSize(300, 200, ImGuiCond.Once);
        spriteFramePreviewPanel.render();

        // Controls panel (bottom-right)
        ImGui.setNextWindowPos(820, 500, ImGuiCond.Once);
        ImGui.setNextWindowSize(220, 200, ImGuiCond.Once);
        animationControlsPanel.render();
    }

    private void processPendingFileActions() {
        List<Runnable> actions;
        synchronized (pendingFileActions) {
            actions = new ArrayList<>(pendingFileActions);
            pendingFileActions.clear();
        }
        for (Runnable action : actions) {
            action.run();
        }
    }

    private void renderMenuBar() {
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("New Project")) {
                    EditorState state = stateProvider.getState();
                    state.reset();
                    currentFilePath = null;
                }
                if (ImGui.menuItem("Open Project...")) {
                    openProjectDialog();
                }
                ImGui.separator();
                if (ImGui.menuItem("Save Project")) {
                    if (currentFilePath != null) {
                        saveProject(currentFilePath);
                    } else {
                        saveAsDialog();
                    }
                }
                if (ImGui.menuItem("Save As...")) {
                    saveAsDialog();
                }
                ImGui.separator();
                if (ImGui.menuItem("Export Collision JSON")) {
                    exportCollisionDialog();
                }
                ImGui.endMenu();
            }
            ImGui.endMainMenuBar();
        }
    }

    private void openProjectDialog() {
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Project Files (*.json)", "json"));
            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                synchronized (pendingFileActions) {
                    pendingFileActions.add(() -> loadProject(file));
                }
            }
        });
    }

    private void loadProject(File file) {
        try {
            Project project = serializer.load(file);
            EditorState state = stateProvider.getState();
            state.reset();
            state.setProject(project);
            currentFilePath = file.getAbsolutePath();

            for (SourceAsset asset : project.getSourceAssets()) {
                if (asset.getFilePath() != null) {
                    try {
                        Texture tex = new Texture(Gdx.files.absolute(asset.getFilePath()));
                        state.getTextureCache().put(asset.getFilePath(), tex);
                        if (asset.getType() == AssetType.SINGLE && !asset.getRegions().isEmpty()) {
                            SourceRegion region = asset.getRegions().get(0);
                            if (region.getBounds() == null) {
                                region.setBounds(new Rect(0, 0, tex.getWidth(), tex.getHeight()));
                            }
                        }
                    } catch (Exception e) {
                        Gdx.app.log("PanelManager", "Failed to load texture: " + asset.getFilePath(), e);
                    }
                }
            }
        } catch (Exception e) {
            Gdx.app.log("PanelManager", "Failed to load project: " + file.getAbsolutePath(), e);
        }
    }

    private void saveAsDialog() {
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Project Files (*.json)", "json"));
            int result = chooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (!file.getName().endsWith(".json")) {
                    file = new File(file.getAbsolutePath() + ".json");
                }
                final File saveFile = file;
                synchronized (pendingFileActions) {
                    pendingFileActions.add(() -> {
                        saveProject(saveFile.getAbsolutePath());
                        currentFilePath = saveFile.getAbsolutePath();
                    });
                }
            }
        });
    }

    private void saveProject(String path) {
        try {
            serializer.save(stateProvider.getState().getProject(), new File(path));
        } catch (Exception e) {
            Gdx.app.log("PanelManager", "Failed to save project: " + path, e);
        }
    }

    private void exportCollisionDialog() {
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Collision JSON (*.json)", "json"));
            int result = chooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (!file.getName().endsWith(".json")) {
                    file = new File(file.getAbsolutePath() + ".json");
                }
                final File exportFile = file;
                synchronized (pendingFileActions) {
                    pendingFileActions.add(() -> {
                        try {
                            Project src = stateProvider.getState().getProject();
                            Project export = new Project(src.getName());
                            export.setAnimations(src.getAnimations());
                            export.setSpriteFrames(src.getSpriteFrames());
                            serializer.save(export, exportFile);
                        } catch (Exception e) {
                            Gdx.app.log("PanelManager", "Failed to export collision JSON: " + exportFile.getAbsolutePath(), e);
                        }
                    });
                }
            }
        });
    }
}
