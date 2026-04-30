# Editor UX Fixes Design

Three bug fixes / UX improvements for the KZCollision Editor.

## 1. Split Functionality Fix

### Problem
`SourceImagesPanel.performSplit()` requires `SourceRegion.bounds != null` to compute sub-regions, but imported SINGLE images create regions with `bounds = null`. No image dimension information is available.

### Solution
In `processPendingImports()`, after pending assets are added to the project, load each non-atlas asset as a LibGDX `Texture` to obtain its pixel dimensions. Set the `SourceRegion.bounds` to a new `Rect(0, 0, textureWidth, textureHeight)`. The loaded texture is cached in `EditorState.textureCache` using the absolute file path as key, so `ViewportRenderer.getTexture()` will find it without reloading.

### Files changed
- `SourceImagesPanel.java` — `processPendingImports()` adds dimension loading

### Edge cases
- Texture load failure: log a warning, skip setting bounds (split will still not work for that image, but the app won't crash)
- Atlas assets: skipped (atlas parsing is a future feature)

---

## 2. Viewport Full-Screen

### Problem
Viewport bounds are hardcoded to `vpX=220, vpY=0, vpW=820, vpH=660` in `KZCollisionEditor.render()`. When the window is resized, the viewport does not adapt.

### Solution
Replace hardcoded bounds with `(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight())`. The viewport renders behind everything at full window size. ImGui panels float on top at their fixed positions (unchanged).

`ViewportInputHandler` also uses the full window as its bounds. Input routing is handled by checking `ImGui.getIO().getWantCaptureMouse()` — when ImGui panels are capturing the mouse, viewport input is suppressed.

### Files changed
- `KZCollisionEditor.java` — viewport bounds use window dimensions
- `ViewportInputHandler.java` — viewport bounds = full window, plus WantCaptureMouse check

---

## 3. Collision Box Drag & Resize Handles

### Problem
Collision boxes cannot be dragged in the viewport. The root cause is that `ImGuiImplGlfw` (initialized with callbacks) and LibGDX's `InputProcessor` compete for mouse events, and there is no proper input routing between ImGui panels and the viewport.

### Solution

#### Input routing
Remove the LibGDX `InputProcessor` approach. Instead, handle viewport input directly in the render loop via `Gdx.input` polling:

- Before `imGuiGl3.newFrame()`: capture current mouse state (position, button states)
- After ImGui renders: if `ImGui.getIO().getWantCaptureMouse()` is false, process viewport interactions using the captured state
- This ensures ImGui always gets first priority for input

This replaces the `ViewportInputHandler implements InputProcessor` class with a simpler polling-based approach called from `KZCollisionEditor.render()`.

#### Interaction model
Three drag modes, determined by what is under the cursor on mouse-down:

| Hit target | Mode | Behavior |
|---|---|---|
| Resize handle (corner of selected box) | RESIZE | Drag changes box x/y/width/height depending on which corner |
| Collision box interior | MOVE | Drag moves the entire box |
| Origin marker | ORIGIN | Drag moves the frame origin |
| Empty space | NONE | Deselects current box |

#### Resize handles
Four corner handles drawn as small filled squares (6x6 pixels in screen space, scaled by zoom) on the selected collision box. Each handle has a hit area of 12x12 pixels for easier grabbing.

Handle drag logic per corner:
- **Top-left**: adjusts x, y, width (x moves right → width shrinks), height (y moves down → height shrinks)
- **Top-right**: adjusts width, y, height
- **Bottom-left**: adjusts x, width, height
- **Bottom-right**: adjusts width, height

Minimum box size enforced: 4x4 pixels.

#### Middle-click panning and scroll zoom
Preserved from the existing implementation, converted to polling.

### Files changed
- `ViewportInputHandler.java` — rewrite from InputProcessor to polling-based interaction handler
- `ViewportRenderer.java` — draw resize handles on selected collision box
- `KZCollisionEditor.java` — call input handler in render loop, remove InputProcessor registration
