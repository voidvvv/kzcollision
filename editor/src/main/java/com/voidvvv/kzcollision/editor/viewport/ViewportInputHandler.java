package com.voidvvv.kzcollision.editor.viewport;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.voidvvv.kzcollision.core.model.AnimationFrame;
import com.voidvvv.kzcollision.core.model.CollisionBox;
import com.voidvvv.kzcollision.editor.EditorState;

public class ViewportInputHandler implements InputProcessor {
    private final EditorState state;
    private final ViewportCamera camera;
    private float viewportX, viewportY, viewportWidth, viewportHeight;

    // Drag state
    private boolean panning = false;
    private boolean draggingBox = false;
    private boolean draggingOrigin = false;
    private float dragStartX, dragStartY;
    private float dragOrigBoxX, dragOrigBoxY;
    private float dragOrigOriginX, dragOrigOriginY;

    public ViewportInputHandler(EditorState state, ViewportCamera camera) {
        this.state = state;
        this.camera = camera;
    }

    public void setViewportBounds(float x, float y, float w, float h) {
        this.viewportX = x;
        this.viewportY = y;
        this.viewportWidth = w;
        this.viewportHeight = h;
    }

    private boolean isInViewport(int screenX, int screenY) {
        // Convert screenY (top-down) to OpenGL coords (bottom-up)
        int glY = Gdx.graphics.getHeight() - screenY;
        return screenX >= viewportX && screenX <= viewportX + viewportWidth &&
               glY >= viewportY && glY <= viewportY + viewportHeight;
    }

    private float toLocalX(int screenX) {
        return screenX - viewportX;
    }

    private float toLocalY(int screenY) {
        return (Gdx.graphics.getHeight() - screenY) - viewportY;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!isInViewport(screenX, screenY)) return false;

        float localX = toLocalX(screenX);
        float localY = toLocalY(screenY);

        if (button == 2) { // Middle click - pan
            panning = true;
            dragStartX = localX;
            dragStartY = localY;
            return true;
        }

        if (button == 0) { // Left click - select/drag
            AnimationFrame frame = state.getCurrentFrame();
            if (frame == null) return false;

            // Convert to world coordinates
            float worldX = camera.screenToWorldX(localX, viewportWidth) + frame.getOriginX();
            float worldY = camera.screenToWorldY(localY, viewportHeight) + frame.getOriginY();

            // Check origin hit (small radius)
            float originDist = (float) Math.sqrt(worldX * worldX + worldY * worldY);
            if (originDist < 10 / camera.getZoom()) {
                draggingOrigin = true;
                dragOrigOriginX = frame.getOriginX();
                dragOrigOriginY = frame.getOriginY();
                dragStartX = worldX;
                dragStartY = worldY;
                return true;
            }

            // Check collision boxes (reverse order for top-most first)
            for (int i = frame.getCollisionBoxes().size() - 1; i >= 0; i--) {
                CollisionBox box = frame.getCollisionBoxes().get(i);
                if (box.containsPoint(worldX, worldY)) {
                    state.setSelectedCollisionBoxId(box.getId());
                    draggingBox = true;
                    dragOrigBoxX = box.getX();
                    dragOrigBoxY = box.getY();
                    dragStartX = worldX;
                    dragStartY = worldY;
                    return true;
                }
            }

            // Clicked empty space - deselect
            state.setSelectedCollisionBoxId(null);
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        float localX = toLocalX(screenX);
        float localY = toLocalY(screenY);

        if (panning) {
            float dx = localX - dragStartX;
            float dy = localY - dragStartY;
            camera.pan(dx, dy);
            dragStartX = localX;
            dragStartY = localY;
            return true;
        }

        AnimationFrame frame = state.getCurrentFrame();
        if (frame == null) return false;

        float worldX = camera.screenToWorldX(localX, viewportWidth) + frame.getOriginX();
        float worldY = camera.screenToWorldY(localY, viewportHeight) + frame.getOriginY();

        if (draggingBox) {
            CollisionBox box = findSelectedBox(frame);
            if (box != null) {
                box.setX(dragOrigBoxX + (worldX - dragStartX));
                box.setY(dragOrigBoxY + (worldY - dragStartY));
            }
            return true;
        }

        if (draggingOrigin) {
            frame.setOriginX(dragOrigOriginX + (worldX - dragStartX));
            frame.setOriginY(dragOrigOriginY + (worldY - dragStartY));
            return true;
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        panning = false;
        draggingBox = false;
        draggingOrigin = false;
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (!isInViewport(Gdx.input.getX(), Gdx.input.getY())) return false;
        float localX = toLocalX(Gdx.input.getX());
        float localY = toLocalY(Gdx.input.getY());
        camera.zoom(amountY, localX, localY, viewportWidth, viewportHeight);
        return true;
    }

    private CollisionBox findSelectedBox(AnimationFrame frame) {
        String id = state.getSelectedCollisionBoxId();
        if (id == null) return null;
        return frame.getCollisionBoxes().stream()
            .filter(b -> b.getId().equals(id)).findFirst().orElse(null);
    }

    // Unused InputProcessor methods
    @Override public boolean keyDown(int keycode) { return false; }
    @Override public boolean keyUp(int keycode) { return false; }
    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
}
