package com.voidvvv.kzcollision.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.GL30;

import com.voidvvv.kzcollision.editor.panels.PanelManager;

import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;

public class KZCollisionEditor extends com.badlogic.gdx.ApplicationAdapter {
    private EditorState state;
    private ImGuiImplGlfw imGuiGlfw;
    private ImGuiImplGl3 imGuiGl3;
    private PanelManager panelManager;

    @Override
    public void create() {
        state = new EditorState();

        long windowHandle = ((Lwjgl3Graphics) Gdx.graphics)
                .getWindow().getWindowHandle();

        ImGui.createContext();
        imGuiGlfw = new ImGuiImplGlfw();
        imGuiGlfw.init(windowHandle, true);
        imGuiGl3 = new ImGuiImplGl3();
        imGuiGl3.init("#version 150");

        panelManager = new PanelManager(() -> state);
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);

        imGuiGlfw.newFrame();
        ImGui.newFrame();

        panelManager.render();

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    @Override
    public void dispose() {
        imGuiGl3.shutdown();
        imGuiGlfw.shutdown();
        ImGui.destroyContext();
    }

    public EditorState getState() {
        return state;
    }
}
