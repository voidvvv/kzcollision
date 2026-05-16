package com.voidvvv.kzcollision.editor.panels;

import imgui.ImGui;
import imgui.flag.ImGuiCond;

import com.voidvvv.kzcollision.core.model.Project;
import com.voidvvv.kzcollision.core.model.SourceAsset;
import com.voidvvv.kzcollision.core.model.SourceRegion;
import com.voidvvv.kzcollision.editor.EditorState;
import com.voidvvv.kzcollision.editor.project.AssetIndex;
import com.voidvvv.kzcollision.editor.project.AssetIndexBuilder;
import com.voidvvv.kzcollision.editor.project.AssetRecord;
import com.voidvvv.kzcollision.editor.project.AssetRegionRecord;
import com.voidvvv.kzcollision.editor.project.CollisionProjectService;
import com.voidvvv.kzcollision.editor.project.EditorProjectContext;
import com.voidvvv.kzcollision.editor.project.EditorTextureLoader;
import com.voidvvv.kzcollision.editor.project.ResourceRecoveryReport;
import com.voidvvv.kzcollision.editor.project.ResourceResolver;
import com.badlogic.gdx.Gdx;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class PanelManager {
    private final EditorStateProvider stateProvider;
    private final FileMenuHandler fileMenuHandler;
    private final SourceImagesPanel sourceImagesPanel;
    private final SpriteFramesPanel spriteFramesPanel;
    private final AnimationsPanel animationsPanel;
    private final AnimationControlsPanel animationControlsPanel;
    private final PropertiesPanel propertiesPanel;
    private final SourceImagePreviewPanel sourceImagePreviewPanel;
    private final SpriteFramePreviewPanel spriteFramePreviewPanel;

    private final CollisionProjectService projectService = new CollisionProjectService();
    private final AssetIndexBuilder assetIndexBuilder = new AssetIndexBuilder();
    private final ResourceResolver resourceResolver = new ResourceResolver();
    private final EditorTextureLoader textureLoader = new EditorTextureLoader();
    private final List<Runnable> pendingFileActions = new ArrayList<>();

    public PanelManager(EditorStateProvider stateProvider, FileMenuHandler fileMenuHandler) {
        this.stateProvider = stateProvider;
        this.fileMenuHandler = fileMenuHandler;
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
                if (ImGui.menuItem("New Collision Project")) {
                    EditorState state = stateProvider.getState();
                    state.reset();
                }
                if (ImGui.menuItem("Open libGDX Project...")) {
                    openLibgdxProjectDialog();
                }
                if (ImGui.menuItem("Open Assets Folder...")) {
                    openAssetsFolderDialog();
                }
                if (ImGui.menuItem("Open Collision JSON...")) {
                    openCollisionJsonDialog();
                }
                ImGui.separator();
                if (ImGui.menuItem("Save Collision JSON")) {
                    saveCurrentCollisionJson();
                }
                if (ImGui.menuItem("Save Collision JSON As...")) {
                    saveCollisionJsonAsDialog();
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

    private void openLibgdxProjectDialog() {
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File projectRoot = chooser.getSelectedFile();
                synchronized (pendingFileActions) {
                    pendingFileActions.add(() -> openProjectRoot(projectRoot));
                }
            }
        });
    }

    private void openAssetsFolderDialog() {
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File assetsRoot = chooser.getSelectedFile();
                synchronized (pendingFileActions) {
                    pendingFileActions.add(() -> openAssetsFolder(assetsRoot));
                }
            }
        });
    }

    private void openProjectRoot(File projectRoot) {
        try {
            openContext(projectService.openProjectRoot(projectRoot));
        } catch (Exception e) {
            Gdx.app.log("PanelManager", "Failed to open libGDX project: " + projectRoot.getAbsolutePath(), e);
        }
    }

    private void openAssetsFolder(File assetsRoot) {
        try {
            openContext(projectService.openAssetsFolder(assetsRoot));
        } catch (Exception e) {
            Gdx.app.log("PanelManager", "Failed to open assets folder: " + assetsRoot.getAbsolutePath(), e);
        }
    }

    private void openContext(EditorProjectContext context) {
        try {
            Project project = projectService.loadOrCreate(context.getCollisionFile(), context.getAssetsRoot().getName());
            AssetIndex index = assetIndexBuilder.build(context.getAssetsRoot());
            ResourceRecoveryReport report = resourceResolver.resolve(project, index);

            // Populate project assets from index when no collision JSON exists yet
            if (project.getSourceAssets().isEmpty()) {
                for (AssetRecord record : index.getRecords()) {
                    SourceAsset asset = new SourceAsset();
                    asset.setType(record.getType());
                    asset.setInternalPath(record.getInternalPath());
                    for (AssetRegionRecord region : record.getRegions()) {
                        asset.getRegions().add(new SourceRegion(region.getName(), asset.getId(), region.getBounds()));
                    }
                    project.getSourceAssets().add(asset);
                }
            }

            EditorState state = stateProvider.getState();
            state.reset();
            state.setProject(project);
            state.setProjectContext(context);
            state.setAssetIndex(index);
            state.setRecoveryReport(report);
            textureLoader.reload(state);
        } catch (Exception e) {
            Gdx.app.log("PanelManager", "Failed to open collision project: "
                    + context.getAssetsRoot().getAbsolutePath(), e);
        }
    }

    private void openCollisionJsonDialog() {
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Collision JSON (*.json)", "json"));
            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File json = chooser.getSelectedFile();
                synchronized (pendingFileActions) {
                    pendingFileActions.add(() -> openCollisionJson(json));
                }
            }
        });
    }

    private void openCollisionJson(File json) {
        EditorState state = stateProvider.getState();
        EditorProjectContext current = state.getProjectContext();
        if (current == null) {
            Gdx.app.log("PanelManager", "Open an assets folder before opening a collision JSON file.");
            return;
        }
        openContext(projectService.withCollisionFile(current, json));
    }

    private void saveCurrentCollisionJson() {
        EditorState state = stateProvider.getState();
        EditorProjectContext context = state.getProjectContext();
        if (context == null) {
            saveCollisionJsonAsDialog();
            return;
        }
        saveCollisionJson(context.getCollisionFile());
    }

    private void saveCollisionJsonAsDialog() {
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Collision JSON (*.json)", "json"));
            EditorProjectContext context = stateProvider.getState().getProjectContext();
            if (context != null) {
                chooser.setCurrentDirectory(new File(context.getAssetsRoot(), "collision"));
            }
            int result = chooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (!file.getName().endsWith(".json")) {
                    file = new File(file.getAbsolutePath() + ".json");
                }
                final File saveFile = file;
                synchronized (pendingFileActions) {
                    pendingFileActions.add(() -> {
                        saveCollisionJson(saveFile);
                        EditorProjectContext ctx = stateProvider.getState().getProjectContext();
                        if (ctx != null) {
                            stateProvider.getState().setProjectContext(
                                    projectService.withCollisionFile(ctx, saveFile));
                        }
                    });
                }
            }
        });
    }

    private void saveCollisionJson(File file) {
        try {
            projectService.save(stateProvider.getState().getProject(), file);
        } catch (Exception e) {
            Gdx.app.log("PanelManager", "Failed to save collision JSON: " + file.getAbsolutePath(), e);
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
                            projectService.save(export, exportFile);
                        } catch (Exception e) {
                            Gdx.app.log("PanelManager", "Failed to export collision JSON: " + exportFile.getAbsolutePath(), e);
                        }
                    });
                }
            }
        });
    }
}
