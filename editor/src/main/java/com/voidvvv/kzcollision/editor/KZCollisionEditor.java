package com.voidvvv.kzcollision.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.GL30;

import com.voidvvv.kzcollision.core.model.Project;
import com.voidvvv.kzcollision.core.serialization.ProjectSerializer;
import com.voidvvv.kzcollision.editor.panels.FileMenuHandler;
import com.voidvvv.kzcollision.editor.panels.PanelManager;
import com.voidvvv.kzcollision.editor.viewport.ViewportInputHandler;
import com.voidvvv.kzcollision.editor.viewport.ViewportRenderer;

import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;

import java.io.File;

public class KZCollisionEditor extends com.badlogic.gdx.ApplicationAdapter implements FileMenuHandler {
    private EditorState state;
    private ImGuiImplGlfw imGuiGlfw;
    private ImGuiImplGl3 imGuiGl3;
    private PanelManager panelManager;
    private ViewportRenderer viewportRenderer;
    private ViewportInputHandler viewportInputHandler;

    private final ProjectSerializer projectSerializer = new ProjectSerializer();
    private File currentProjectFile;

    private boolean imguiInitialized = false;

    @Override
    public void create() {
        state = new EditorState();
        panelManager = new PanelManager(() -> state, this);
        viewportRenderer = new ViewportRenderer(state);
        viewportInputHandler = new ViewportInputHandler(state, viewportRenderer.getCamera());
        Gdx.input.setInputProcessor(viewportInputHandler);
    }

    private void initImGui() {
        long windowHandle = ((Lwjgl3Graphics) Gdx.graphics)
                .getWindow().getWindowHandle();

        ImGui.createContext();
        imGuiGlfw = new ImGuiImplGlfw();
        imGuiGlfw.init(windowHandle, true);
        imGuiGl3 = new ImGuiImplGl3();
        imGuiGl3.init("#version 150");
        imguiInitialized = true;
    }

    @Override
    public void render() {
        // Lazy-init ImGui on first render frame to ensure GL context is ready
        if (!imguiInitialized) {
            initImGui();
        }

        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);

        // Render viewport first (background layer)
        float vpX = 220, vpY = 0, vpW = 820, vpH = 660;
        viewportInputHandler.setViewportBounds(vpX, vpY, vpW, vpH);
        viewportRenderer.render(vpX, vpY, vpW, vpH);

        // ImGui on top
        imGuiGlfw.newFrame();
        ImGui.newFrame();

        panelManager.render();

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    @Override
    public void dispose() {
        viewportRenderer.dispose();
        imGuiGl3.shutdown();
        imGuiGlfw.shutdown();
        ImGui.destroyContext();
    }

    public EditorState getState() {
        return state;
    }

    // ---- FileMenuHandler implementation ----

    @Override
    public void newProject() {
        state.setProject(new Project("Untitled"));
        state.setSelectedAnimationId(null);
        state.setCurrentFrameIndex(0);
        state.setSelectedCollisionBoxId(null);
        state.setPlaying(false);
        currentProjectFile = null;
    }

    @Override
    public void openProject(File file) {
        try {
            Project loaded = projectSerializer.load(file);
            state.setProject(loaded);
            state.setSelectedAnimationId(null);
            state.setCurrentFrameIndex(0);
            state.setSelectedCollisionBoxId(null);
            state.setPlaying(false);
            currentProjectFile = file;
        } catch (Exception e) {
            Gdx.app.error("KZCollisionEditor", "Failed to open project: " + file.getAbsolutePath(), e);
        }
    }

    @Override
    public void saveProject(File file) {
        try {
            projectSerializer.save(state.getProject(), file);
            currentProjectFile = file;
        } catch (Exception e) {
            Gdx.app.error("KZCollisionEditor", "Failed to save project: " + file.getAbsolutePath(), e);
        }
    }

    @Override
    public void exportCollisionJson(File file) {
        try {
            projectSerializer.save(state.getProject(), file);
        } catch (Exception e) {
            Gdx.app.error("KZCollisionEditor", "Failed to export collision JSON: " + file.getAbsolutePath(), e);
        }
    }

    @Override
    public void saveCurrentProject() {
        if (currentProjectFile != null) {
            saveProject(currentProjectFile);
        }
        // If no current file, PanelManager is responsible for showing Save As dialog
    }

    @Override
    public boolean hasCurrentFile() {
        return currentProjectFile != null;
    }
}
