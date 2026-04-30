package com.voidvvv.kzcollision.editor.viewport;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.voidvvv.kzcollision.core.model.AnimationFrame;
import com.voidvvv.kzcollision.core.model.CollisionBox;
import com.voidvvv.kzcollision.editor.EditorState;

import imgui.ImGui;

public class ViewportInputHandler implements InputProcessor {
    private final EditorState state;
    private final ViewportCamera camera;

    private enum DragMode { NONE, PAN, MOVE_BOX, RESIZE_BOX, ORIGIN }

    enum ResizeCorner { BOTTOM_LEFT, BOTTOM_RIGHT, TOP_LEFT, TOP_RIGHT }

    private DragMode dragMode = DragMode.NONE;
    private ResizeCorner activeCorner;

    private float dragStartWorldX, dragStartWorldY;
    private float dragOrigBoxX, dragOrigBoxY, dragOrigBoxW, dragOrigBoxH;
    private float dragOrigOriginX, dragOrigOriginY;
    private float panStartLocalX, panStartLocalY;

    private float scrollAccumulator;

    private static final float HANDLE_HIT_SCREEN_SIZE = 12f;
    private static final float MIN_BOX_SIZE = 4f;

    public ViewportInputHandler(EditorState state, ViewportCamera camera) {
        this.state = state;
        this.camera = camera;
    }

    public void update() {
        if (ImGui.getIO().getWantCaptureMouse()) {
            scrollAccumulator = 0f;
            return;
        }

        int screenX = Gdx.input.getX();
        int screenY = Gdx.input.getY();
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        float localX = screenX;
        float localY = screenHeight - screenY;

        // Scroll zoom
        if (scrollAccumulator != 0f) {
            camera.zoom(scrollAccumulator, localX, localY, screenWidth, screenHeight);
            scrollAccumulator = 0f;
        }

        // Middle-click pan
        if (Gdx.input.isButtonPressed(2)) {
            if (dragMode != DragMode.PAN) {
                dragMode = DragMode.PAN;
                panStartLocalX = localX;
                panStartLocalY = localY;
            } else {
                camera.pan(localX - panStartLocalX, localY - panStartLocalY);
                panStartLocalX = localX;
                panStartLocalY = localY;
            }
            return;
        } else if (dragMode == DragMode.PAN) {
            dragMode = DragMode.NONE;
            return;
        }

        AnimationFrame frame = state.getCurrentFrame();
        float worldX = 0, worldY = 0;
        if (frame != null) {
            worldX = camera.screenToWorldX(localX, screenWidth) + frame.getOriginX();
            worldY = camera.screenToWorldY(localY, screenHeight) + frame.getOriginY();
        }

        // Left button just pressed
        if (Gdx.input.isButtonJustPressed(0)) {
            if (frame == null) return;

            // Check resize handles on selected box
            String selectedBoxId = state.getSelectedCollisionBoxId();
            if (selectedBoxId != null) {
                CollisionBox selectedBox = findBoxById(frame, selectedBoxId);
                if (selectedBox != null) {
                    ResizeCorner corner = hitTestHandle(worldX, worldY, selectedBox);
                    if (corner != null) {
                        startResize(corner, selectedBox, worldX, worldY);
                        return;
                    }
                }
            }

            // Check origin marker
            float originDist = (float) Math.sqrt(worldX * worldX + worldY * worldY);
            if (originDist < 10 / camera.getZoom()) {
                dragMode = DragMode.ORIGIN;
                dragStartWorldX = worldX;
                dragStartWorldY = worldY;
                dragOrigOriginX = frame.getOriginX();
                dragOrigOriginY = frame.getOriginY();
                return;
            }

            // Check collision boxes (reverse order for top-most first)
            for (int i = frame.getCollisionBoxes().size() - 1; i >= 0; i--) {
                CollisionBox box = frame.getCollisionBoxes().get(i);
                if (box.containsPoint(worldX, worldY)) {
                    state.setSelectedCollisionBoxId(box.getId());
                    dragMode = DragMode.MOVE_BOX;
                    dragStartWorldX = worldX;
                    dragStartWorldY = worldY;
                    dragOrigBoxX = box.getX();
                    dragOrigBoxY = box.getY();
                    return;
                }
            }

            // Empty space - deselect
            state.setSelectedCollisionBoxId(null);
            return;
        }

        // Left button held - dragging
        if (Gdx.input.isButtonPressed(0) && dragMode != DragMode.NONE) {
            if (frame == null) { dragMode = DragMode.NONE; return; }
            float dx = worldX - dragStartWorldX;
            float dy = worldY - dragStartWorldY;

            switch (dragMode) {
                case MOVE_BOX: {
                    CollisionBox box = findBoxById(frame, state.getSelectedCollisionBoxId());
                    if (box != null) {
                        box.setX(dragOrigBoxX + dx);
                        box.setY(dragOrigBoxY + dy);
                    }
                    break;
                }
                case RESIZE_BOX: {
                    CollisionBox box = findBoxById(frame, state.getSelectedCollisionBoxId());
                    if (box != null) {
                        applyResize(box, dx, dy);
                    }
                    break;
                }
                case ORIGIN: {
                    frame.setOriginX(dragOrigOriginX + dx);
                    frame.setOriginY(dragOrigOriginY + dy);
                    break;
                }
                default: break;
            }
            return;
        }

        // Left button released
        if (!Gdx.input.isButtonPressed(0) && dragMode != DragMode.NONE) {
            dragMode = DragMode.NONE;
        }
    }

    private void startResize(ResizeCorner corner, CollisionBox box, float worldX, float worldY) {
        dragMode = DragMode.RESIZE_BOX;
        activeCorner = corner;
        dragStartWorldX = worldX;
        dragStartWorldY = worldY;
        dragOrigBoxX = box.getX();
        dragOrigBoxY = box.getY();
        dragOrigBoxW = box.getWidth();
        dragOrigBoxH = box.getHeight();
    }

    private ResizeCorner hitTestHandle(float worldX, float worldY, CollisionBox box) {
        float hitRadius = HANDLE_HIT_SCREEN_SIZE / camera.getZoom();
        float[] xs = {box.getX(), box.getX() + box.getWidth(), box.getX(), box.getX() + box.getWidth()};
        float[] ys = {box.getY(), box.getY(), box.getY() + box.getHeight(), box.getY() + box.getHeight()};
        ResizeCorner[] corners = {ResizeCorner.BOTTOM_LEFT, ResizeCorner.BOTTOM_RIGHT,
                                  ResizeCorner.TOP_LEFT, ResizeCorner.TOP_RIGHT};
        for (int i = 0; i < 4; i++) {
            if (Math.abs(worldX - xs[i]) <= hitRadius && Math.abs(worldY - ys[i]) <= hitRadius) {
                return corners[i];
            }
        }
        return null;
    }

    private void applyResize(CollisionBox box, float dx, float dy) {
        float newX = dragOrigBoxX;
        float newY = dragOrigBoxY;
        float newW = dragOrigBoxW;
        float newH = dragOrigBoxH;

        switch (activeCorner) {
            case BOTTOM_LEFT:
                newX += dx; newW -= dx; newY += dy; newH -= dy; break;
            case BOTTOM_RIGHT:
                newW += dx; newY += dy; newH -= dy; break;
            case TOP_LEFT:
                newX += dx; newW -= dx; newH += dy; break;
            case TOP_RIGHT:
                newW += dx; newH += dy; break;
        }

        if (newW < MIN_BOX_SIZE) {
            if (activeCorner == ResizeCorner.BOTTOM_LEFT || activeCorner == ResizeCorner.TOP_LEFT) {
                newX = dragOrigBoxX + dragOrigBoxW - MIN_BOX_SIZE;
            }
            newW = MIN_BOX_SIZE;
        }
        if (newH < MIN_BOX_SIZE) {
            if (activeCorner == ResizeCorner.BOTTOM_LEFT || activeCorner == ResizeCorner.BOTTOM_RIGHT) {
                newY = dragOrigBoxY + dragOrigBoxH - MIN_BOX_SIZE;
            }
            newH = MIN_BOX_SIZE;
        }

        box.setX(newX);
        box.setY(newY);
        box.setWidth(newW);
        box.setHeight(newH);
    }

    private CollisionBox findBoxById(AnimationFrame frame, String id) {
        if (id == null) return null;
        return frame.getCollisionBoxes().stream()
            .filter(b -> b.getId().equals(id)).findFirst().orElse(null);
    }

    // InputProcessor — only scroll is used; everything else is polled in update()
    @Override public boolean scrolled(float amountX, float amountY) {
        scrollAccumulator += amountY;
        return false;
    }
    @Override public boolean keyDown(int keycode) { return false; }
    @Override public boolean keyUp(int keycode) { return false; }
    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
}
