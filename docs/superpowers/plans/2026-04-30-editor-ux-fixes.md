# Editor UX Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix three editor UX issues — split functionality, viewport fullscreen, and collision box drag with resize handles.

**Architecture:** Replace the LibGDX InputProcessor approach with polling-based input handling in the render loop. ImGui gets first priority for input via `WantCaptureMouse` check. The viewport renders full-screen behind floating ImGui panels.

**Tech Stack:** Java 11, LibGDX 1.14.0, Dear ImGui (imgui-java 1.87.0)

---

## File Structure

| File | Action | Responsibility |
|---|---|---|
| `editor/.../panels/SourceImagesPanel.java` | Modify | Load texture dimensions on import to populate `SourceRegion.bounds` |
| `editor/.../KZCollisionEditor.java` | Modify | Viewport fullscreen; call input handler `update()` in render loop |
| `editor/.../viewport/ViewportInputHandler.java` | Rewrite | Polling-based interaction: pan, zoom, drag box, resize box, drag origin |
| `editor/.../viewport/ViewportRenderer.java` | Modify | Draw resize handles on selected collision box |

---

### Task 1: Fix Split Functionality

**Files:**
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagesPanel.java:232-242`

The root cause: imported images create `SourceRegion` with `bounds = null`. `performSplit()` exits early when bounds are null. Fix by loading the texture during `processPendingImports()` to obtain dimensions.

- [ ] **Step 1: Modify `processPendingImports()` to load texture dimensions**

Replace the `processPendingImports()` method (lines 232-242) with:

```java
private void processPendingImports() {
    List<SourceAsset> toAdd;
    synchronized (pendingImports) {
        toAdd = new ArrayList<>(pendingImports);
        pendingImports.clear();
    }
    if (!toAdd.isEmpty()) {
        EditorState state = stateProvider.getState();
        Project project = state.getProject();
        for (SourceAsset asset : toAdd) {
            project.getSourceAssets().add(asset);

            if (asset.getType() == AssetType.SINGLE) {
                try {
                    Texture tex = new Texture(Gdx.files.absolute(asset.getFilePath()));
                    state.getTextureCache().put(asset.getFilePath(), tex);
                    if (!asset.getRegions().isEmpty()) {
                        SourceRegion region = asset.getRegions().get(0);
                        region.setBounds(new Rect(0, 0, tex.getWidth(), tex.getHeight()));
                    }
                } catch (Exception e) {
                    Gdx.app.log("SourceImagesPanel", "Failed to load texture: " + asset.getFilePath(), e);
                }
            }
        }
    }
}
```

Add imports at the top of the file:

```java
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
```

- [ ] **Step 2: Verify compilation**

Run: `gradle editor:compileJava 2>&1`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagesPanel.java
git commit -m "fix(editor): load texture dimensions on import to enable split functionality"
```

---

### Task 2: Make Viewport Full-Screen

**Files:**
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/KZCollisionEditor.java:44-51`

- [ ] **Step 1: Replace hardcoded viewport bounds with window dimensions**

In `KZCollisionEditor.render()`, replace lines 49-51:

```java
        // Render viewport (space between ImGui docked panels)
        float vpX = 220, vpY = 0, vpW = 820, vpH = 660;
        viewportInputHandler.setViewportBounds(vpX, vpY, vpW, vpH);
        viewportRenderer.render(vpX, vpY, vpW, vpH);
```

with:

```java
        // Render viewport (full-screen behind ImGui panels)
        float vpW = Gdx.graphics.getWidth();
        float vpH = Gdx.graphics.getHeight();
        viewportRenderer.render(0, 0, vpW, vpH);
```

Note: `viewportInputHandler.setViewportBounds(...)` call is removed. The handler will use `Gdx.graphics.getWidth/Height()` directly in its `update()` method.

- [ ] **Step 2: Verify compilation**

Run: `gradle editor:compileJava 2>&1`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/KZCollisionEditor.java
git commit -m "fix(editor): use window dimensions for fullscreen viewport"
```

---

### Task 3: Rewrite ViewportInputHandler

**Files:**
- Rewrite: `editor/src/main/java/com/voidvvv/kzcollision/editor/viewport/ViewportInputHandler.java`

This is the largest change. The class switches from `InputProcessor` callbacks to a polling-based `update()` method called from the render loop. It still implements `InputProcessor` but only uses `scrolled()` to accumulate scroll events.

- [ ] **Step 1: Write the complete rewritten ViewportInputHandler**

Replace the entire file content with:

```java
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
```

- [ ] **Step 2: Verify compilation**

Run: `gradle editor:compileJava 2>&1`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/viewport/ViewportInputHandler.java
git commit -m "refactor(editor): rewrite ViewportInputHandler to polling-based input with resize handles"
```

---

### Task 4: Draw Resize Handles

**Files:**
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/viewport/ViewportRenderer.java:78-104`

Add resize handle rendering after the collision box outlines, before the origin marker. The handles are 6px screen-space filled white squares at each corner of the selected collision box.

- [ ] **Step 1: Add resize handle drawing**

In `ViewportRenderer.render()`, after the collision box `shapes.end()` (line 89, after `shapes.end()` following the loop) and before the origin marker `shapes.begin(ShapeRenderer.ShapeType.Filled)` (line 93), insert:

```java
            // Draw resize handles on selected box
            if (selectedBoxId != null) {
                for (CollisionBox box : frame.getCollisionBoxes()) {
                    if (box.getId().equals(selectedBoxId)) {
                        float handleSize = 6f / camera.getZoom();
                        float half = handleSize / 2f;
                        float bx = box.getX() - originX;
                        float by = box.getY() - originY;

                        shapes.begin(ShapeRenderer.ShapeType.Filled);
                        shapes.setColor(1f, 1f, 1f, 1f);
                        shapes.rect(bx - half, by - half, handleSize, handleSize);
                        shapes.rect(bx + box.getWidth() - half, by - half, handleSize, handleSize);
                        shapes.rect(bx - half, by + box.getHeight() - half, handleSize, handleSize);
                        shapes.rect(bx + box.getWidth() - half, by + box.getHeight() - half, handleSize, handleSize);
                        shapes.end();
                        break;
                    }
                }
            }
```

Note: The `selectedBoxId` variable is already defined on line 81 as `String selectedBoxId = state.getSelectedCollisionBoxId();`.

- [ ] **Step 2: Verify compilation**

Run: `gradle editor:compileJava 2>&1`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/viewport/ViewportRenderer.java
git commit -m "feat(editor): draw resize handles on selected collision box"
```

---

### Task 5: Wire Input Handler into Render Loop

**Files:**
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/KZCollisionEditor.java`

Remove the old `Gdx.input.setInputProcessor(viewportInputHandler)` call from `create()`. Add `viewportInputHandler.update()` call after `ImGui.render()` in `render()`. Keep `Gdx.input.setInputProcessor(viewportInputHandler)` in `create()` for scroll events only.

- [ ] **Step 1: Add `update()` call in render loop**

In `KZCollisionEditor.render()`, after `ImGui.render()` (line 59) and before `imGuiGl3.renderDrawData(ImGui.getDrawData())` (line 60), insert:

```java
        viewportInputHandler.update();
```

The `create()` method already has `Gdx.input.setInputProcessor(viewportInputHandler)` on line 40 — keep this. The handler now uses `scrolled()` for scroll accumulation and `update()` for all other interactions.

The full `render()` method should look like:

```java
    @Override
    public void render() {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);

        // Render viewport (full-screen behind ImGui panels)
        float vpW = Gdx.graphics.getWidth();
        float vpH = Gdx.graphics.getHeight();
        viewportRenderer.render(0, 0, vpW, vpH);

        // Start ImGui frame
        imGuiGl3.newFrame();
        imGuiGlfw.newFrame();
        ImGui.newFrame();

        panelManager.render();

        ImGui.render();
        viewportInputHandler.update();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }
```

- [ ] **Step 2: Verify compilation**

Run: `gradle editor:compileJava 2>&1`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Manual test**

Run: `gradle editor:run 2>&1`

Verify:
1. Import an image, right-click → "Split 2x2" → Split popup appears and split creates frames
2. Resize the window — viewport fills the entire window
3. Add a collision box, click it in viewport → drag to move, drag corners to resize
4. Middle-click drag → pan, scroll → zoom
5. ImGui panels still work — clicking buttons, typing in fields

- [ ] **Step 4: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/KZCollisionEditor.java
git commit -m "feat(editor): wire polling input handler into render loop"
```
