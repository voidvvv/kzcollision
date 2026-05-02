# Preview Panels & Split Frame Naming Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add two preview panels (Source Image and SpriteFrame) at the bottom of the editor, and add sequential numbering to split-generated SpriteFrame names.

**Architecture:** Two new ImGui panel classes that monitor selection state in EditorState. Each panel reads the selected ID, looks up the corresponding asset/frame, retrieves the cached libGDX Texture, and renders it via `ImGui.image()` using the OpenGL texture handle. The split naming change is a localized update to the existing `performSplit()` method.

**Tech Stack:** Java 11, libGDX 1.14.0, Dear ImGui (imgui-java 1.87.0), LWJGL3

---

## File Structure

| Action | File | Responsibility |
|--------|------|----------------|
| Modify | `editor/.../EditorState.java` | Add `selectedSpriteFrameId`, `selectedSourceAssetId` fields |
| Modify | `editor/.../panels/SourceImagesPanel.java` | Set `selectedSourceAssetId` on asset click |
| Modify | `editor/.../panels/SpriteFramesPanel.java` | Set `selectedSpriteFrameId` on frame click, update `buildLabel()` |
| Modify | `editor/.../panels/PanelManager.java` | Instantiate and position two new preview panels |
| Create | `editor/.../panels/SourceImagePreviewPanel.java` | Render selected source image preview |
| Create | `editor/.../panels/SpriteFramePreviewPanel.java` | Render selected sprite frame preview |

---

### Task 1: Add selection state fields to EditorState

**Files:**
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/EditorState.java`

- [ ] **Step 1: Add two new fields with getters/setters**

Add after the `playbackTimer` field (line 17):

```java
private String selectedSpriteFrameId;
private String selectedSourceAssetId;
```

Add after `setPlaybackTimer` (line 36):

```java
public String getSelectedSpriteFrameId() { return selectedSpriteFrameId; }
public void setSelectedSpriteFrameId(String id) { this.selectedSpriteFrameId = id; }
public String getSelectedSourceAssetId() { return selectedSourceAssetId; }
public void setSelectedSourceAssetId(String id) { this.selectedSourceAssetId = id; }
```

- [ ] **Step 2: Clear new fields in `reset()`**

In the `reset()` method (around line 52), add these two lines after `this.playbackTimer = 0f;`:

```java
this.selectedSpriteFrameId = null;
this.selectedSourceAssetId = null;
```

- [ ] **Step 3: Build to verify compilation**

Run: `cd C:/myWareHouse/dev/java/imageedit && ./gradlew editor:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/EditorState.java
git commit -m "feat(editor): add selectedSpriteFrameId and selectedSourceAssetId to EditorState"
```

---

### Task 2: Add click selection to SpriteFramesPanel

**Files:**
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SpriteFramesPanel.java`

- [ ] **Step 1: Wire click to set selectedSpriteFrameId**

In `renderFrameList()`, replace the existing selectable click handler (line 38):

```java
if (ImGui.selectable(label)) {
    // Single click selects the frame (future use)
}
```

With:

```java
if (ImGui.selectable(label)) {
    state.setSelectedSpriteFrameId(frame.getId());
}
```

- [ ] **Step 2: Build to verify**

Run: `cd C:/myWareHouse/dev/java/imageedit && ./gradlew editor:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SpriteFramesPanel.java
git commit -m "feat(editor): select SpriteFrame on click, store in EditorState"
```

---

### Task 3: Add click selection to SourceImagesPanel

**Files:**
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagesPanel.java`

- [ ] **Step 1: Wire click to set selectedSourceAssetId**

In `renderSingleAsset()`, replace the existing selectable click handler (line 93):

```java
if (ImGui.selectable(getDisplayName(asset))) {
    // Selection handled via context menu
}
```

With:

```java
if (ImGui.selectable(getDisplayName(asset))) {
    stateProvider.getState().setSelectedSourceAssetId(asset.getId());
}
```

In `renderAtlasAsset()`, replace the existing selectable click handler (line 84):

```java
if (ImGui.selectable("  " + region.getName())) {
    // Selection handled via context menu
}
```

With:

```java
if (ImGui.selectable("  " + region.getName())) {
    stateProvider.getState().setSelectedSourceAssetId(asset.getId());
}
```

Note: `renderAtlasAsset` already has the `asset` variable in scope from the for-loop parameter.

- [ ] **Step 2: Build to verify**

Run: `cd C:/myWareHouse/dev/java/imageedit && ./gradlew editor:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagesPanel.java
git commit -m "feat(editor): select SourceAsset on click, store in EditorState"
```

---

### Task 4: Create SourceImagePreviewPanel

**Files:**
- Create: `editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagePreviewPanel.java`

- [ ] **Step 1: Create the panel class**

```java
package com.voidvvv.kzcollision.editor.panels;

import com.voidvvv.kzcollision.core.model.SourceAsset;
import com.voidvvv.kzcollision.core.model.Project;
import com.voidvvv.kzcollision.editor.EditorState;
import com.badlogic.gdx.graphics.Texture;
import imgui.ImGui;

public class SourceImagePreviewPanel {
    private final EditorStateProvider stateProvider;

    public SourceImagePreviewPanel(EditorStateProvider stateProvider) {
        this.stateProvider = stateProvider;
    }

    public void render() {
        if (ImGui.begin("Source Image Preview")) {
            EditorState state = stateProvider.getState();
            String assetId = state.getSelectedSourceAssetId();

            if (assetId == null) {
                ImGui.textDisabled("Select a source image to preview");
            } else {
                SourceAsset asset = state.getProject().findSourceAsset(assetId);
                if (asset == null) {
                    ImGui.textDisabled("Source image not found");
                } else {
                    Texture tex = state.getTextureCache().get(asset.getFilePath());
                    if (tex == null) {
                        ImGui.textDisabled("Texture not loaded");
                    } else {
                        renderPreview(tex);
                        renderInfo(asset, tex);
                    }
                }
            }
        }
        ImGui.end();
    }

    private void renderPreview(Texture tex) {
        float availWidth = ImGui.getContentRegionAvailX();
        float imgWidth = tex.getWidth();
        float imgHeight = tex.getHeight();
        float scale = availWidth / imgWidth;
        float displayWidth = imgWidth * scale;
        float displayHeight = imgHeight * scale;

        // Cap height to reasonable max
        float maxHeight = 160f;
        if (displayHeight > maxHeight) {
            float heightScale = maxHeight / displayHeight;
            displayWidth *= heightScale;
            displayHeight *= heightScale;
        }

        int texId = tex.getTextureObjectHandle();
        ImGui.image(texId, displayWidth, displayHeight, 0, 1, 1, 0);
    }

    private void renderInfo(SourceAsset asset, Texture tex) {
        String displayName = asset.getFilePath();
        int lastSep = Math.max(displayName.lastIndexOf('/'), displayName.lastIndexOf('\\'));
        if (lastSep >= 0) {
            displayName = displayName.substring(lastSep + 1);
        }
        ImGui.text(displayName);
        ImGui.sameLine();
        ImGui.textDisabled(tex.getWidth() + " x " + tex.getHeight());
    }
}
```

Note on UV coordinates: ImGui uses (0,0) as top-left and (1,1) as bottom-right for UV. libGDX textures have origin at bottom-left. So `uv0y=1, uv1y=0` flips the Y axis to match ImGui's coordinate system. This matches the existing pattern in `ViewportRenderer` where the Y-flip is done via `texture.getHeight() - sr.y - sr.height`.

- [ ] **Step 2: Build to verify**

Run: `cd C:/myWareHouse/dev/java/imageedit && ./gradlew editor:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagePreviewPanel.java
git commit -m "feat(editor): add SourceImagePreviewPanel for selected image preview"
```

---

### Task 5: Create SpriteFramePreviewPanel

**Files:**
- Create: `editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SpriteFramePreviewPanel.java`

- [ ] **Step 1: Create the panel class**

```java
package com.voidvvv.kzcollision.editor.panels;

import com.voidvvv.kzcollision.core.model.SpriteFrame;
import com.voidvvv.kzcollision.core.model.SourceAsset;
import com.voidvvv.kzcollision.core.model.Rect;
import com.voidvvv.kzcollision.editor.EditorState;
import com.badlogic.gdx.graphics.Texture;
import imgui.ImGui;

public class SpriteFramePreviewPanel {
    private final EditorStateProvider stateProvider;

    public SpriteFramePreviewPanel(EditorStateProvider stateProvider) {
        this.stateProvider = stateProvider;
    }

    public void render() {
        if (ImGui.begin("SpriteFrame Preview")) {
            EditorState state = stateProvider.getState();
            String frameId = state.getSelectedSpriteFrameId();

            if (frameId == null) {
                ImGui.textDisabled("Select a sprite frame to preview");
            } else {
                SpriteFrame frame = state.getProject().findSpriteFrame(frameId);
                if (frame == null) {
                    ImGui.textDisabled("Sprite frame not found");
                } else {
                    Texture tex = resolveTexture(state, frame);
                    if (tex == null) {
                        ImGui.textDisabled("Texture not loaded");
                    } else {
                        renderPreview(tex, frame);
                        renderInfo(frame, tex);
                    }
                }
            }
        }
        ImGui.end();
    }

    private Texture resolveTexture(EditorState state, SpriteFrame frame) {
        SourceAsset asset = state.getProject().findSourceAsset(frame.getSourceAssetId());
        if (asset == null) return null;
        return state.getTextureCache().get(asset.getFilePath());
    }

    private void renderPreview(Texture tex, SpriteFrame frame) {
        float availWidth = ImGui.getContentRegionAvailX();
        Rect sub = frame.getSubRegion();

        float imgWidth, imgHeight;
        float uv0x, uv0y, uv1x, uv1y;

        if (sub != null) {
            imgWidth = sub.width;
            imgHeight = sub.height;
            float texW = tex.getWidth();
            float texH = tex.getHeight();
            uv0x = sub.x / texW;
            uv0y = 1f - (sub.y + sub.height) / texH;
            uv1x = (sub.x + sub.width) / texW;
            uv1y = 1f - sub.y / texH;
        } else {
            imgWidth = tex.getWidth();
            imgHeight = tex.getHeight();
            uv0x = 0;
            uv0y = 1;
            uv1x = 1;
            uv1y = 0;
        }

        float scale = availWidth / imgWidth;
        float displayWidth = imgWidth * scale;
        float displayHeight = imgHeight * scale;

        float maxHeight = 160f;
        if (displayHeight > maxHeight) {
            float heightScale = maxHeight / displayHeight;
            displayWidth *= heightScale;
            displayHeight *= heightScale;
        }

        int texId = tex.getTextureObjectHandle();
        ImGui.image(texId, displayWidth, displayHeight, uv0x, uv0y, uv1x, uv1y);
    }

    private void renderInfo(SpriteFrame frame, Texture tex) {
        String name = frame.getSourceRegionName() != null ? frame.getSourceRegionName() : "(unnamed)";
        Rect sub = frame.getSubRegion();
        int w = sub != null ? (int) sub.width : tex.getWidth();
        int h = sub != null ? (int) sub.height : tex.getHeight();

        ImGui.text(name);
        ImGui.sameLine();
        ImGui.textDisabled(w + " x " + h);
    }
}
```

- [ ] **Step 2: Build to verify**

Run: `cd C:/myWareHouse/dev/java/imageedit && ./gradlew editor:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SpriteFramePreviewPanel.java
git commit -m "feat(editor): add SpriteFramePreviewPanel for selected frame preview"
```

---

### Task 6: Integrate preview panels into PanelManager layout

**Files:**
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/panels/PanelManager.java`

- [ ] **Step 1: Add fields for new panels**

Add after the `propertiesPanel` field (line 27):

```java
private final SourceImagePreviewPanel sourceImagePreviewPanel;
private final SpriteFramePreviewPanel spriteFramePreviewPanel;
```

- [ ] **Step 2: Instantiate in constructor**

Add after the `propertiesPanel` initialization (line 40):

```java
this.sourceImagePreviewPanel = new SourceImagePreviewPanel(stateProvider);
this.spriteFramePreviewPanel = new SpriteFramePreviewPanel(stateProvider);
```

- [ ] **Step 3: Update render() method — reposition panels**

Replace the entire `render()` method body (lines 43-71) with:

```java
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
```

- [ ] **Step 4: Add import for new panel classes**

The file already imports from `com.voidvvv.kzcollision.editor.panels.*` implicitly since all panels are in the same package. No new imports needed.

- [ ] **Step 5: Build to verify**

Run: `cd C:/myWareHouse/dev/java/imageedit && ./gradlew editor:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Run the application to test**

Run: `cd C:/myWareHouse/dev/java/imageedit && ./gradlew editor:run`
Expected: Window opens with two new preview panels at the bottom. Click a source image → preview shows in left preview panel. Click a sprite frame → preview shows in right preview panel.

- [ ] **Step 7: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/panels/PanelManager.java
git commit -m "feat(editor): integrate SourceImagePreview and SpriteFramePreview panels into layout"
```

---

### Task 7: Update split naming to include sequence numbers

**Files:**
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagesPanel.java`

- [ ] **Step 1: Update performSplit() to generate numbered names**

Replace the entire `performSplit()` method (lines 154-181) with:

```java
private void performSplit(int rows, int cols) {
    SourceRegion region = findRegionById(splitRegionId);
    if (region == null) {
        return;
    }

    Rect bounds = region.getBounds();
    if (bounds == null) {
        return;
    }

    String baseName = region.getName();
    float tileWidth = bounds.width / cols;
    float tileHeight = bounds.height / rows;
    int index = 1;

    for (int r = 0; r < rows; r++) {
        for (int c = 0; c < cols; c++) {
            float subX = bounds.x + c * tileWidth;
            float subY = bounds.y + r * tileHeight;
            Rect subRegion = new Rect(subX, subY, tileWidth, tileHeight);

            SpriteFrame frame = new SpriteFrame();
            frame.setSourceAssetId(region.getAssetId());
            frame.setSourceRegionName(baseName + "_" + String.format("%03d", index));
            frame.setSubRegion(subRegion);
            stateProvider.getState().getProject().getSpriteFrames().add(frame);
            index++;
        }
    }
}
```

Key changes: added `baseName` variable, `index` counter starting at 1, and `sourceRegionName` is now `baseName + "_" + String.format("%03d", index)`.

- [ ] **Step 2: Build to verify**

Run: `cd C:/myWareHouse/dev/java/imageedit && ./gradlew editor:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagesPanel.java
git commit -m "feat(editor): add sequential numbering to split frame names"
```

---

### Task 8: Update SpriteFramesPanel buildLabel to drop [split] suffix

**Files:**
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SpriteFramesPanel.java`

- [ ] **Step 1: Simplify buildLabel()**

Replace the `buildLabel()` method (lines 66-73) with:

```java
private String buildLabel(SpriteFrame frame) {
    return frame.getSourceRegionName() != null ? frame.getSourceRegionName() : "(unnamed)";
}
```

Since split frames now have unique numbered names (e.g., "image.png_001"), the `[split]` suffix is no longer needed for differentiation.

- [ ] **Step 2: Build to verify**

Run: `cd C:/myWareHouse/dev/java/imageedit && ./gradlew editor:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run the application to test full workflow**

Run: `cd C:/myWareHouse/dev/java/imageedit && ./gradlew editor:run`

Test steps:
1. Import a sprite sheet image
2. Right-click the image → Split 3x3
3. Verify Sprite Frames panel shows numbered names like "image.png_001" through "image.png_009"
4. Click a sprite frame → verify preview appears in SpriteFrame Preview panel
5. Click the source image → verify preview appears in Source Image Preview panel

- [ ] **Step 4: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SpriteFramesPanel.java
git commit -m "refactor(editor): remove [split] suffix from frame labels, now redundant"
```

---

### Task 9: Final integration test and cleanup

- [ ] **Step 1: Full build**

Run: `cd C:/myWareHouse/dev/java/imageedit && ./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run and manually verify all three features**

Run: `cd C:/myWareHouse/dev/java/imageedit && ./gradlew editor:run`

Checklist:
- [ ] Source Images: clicking an asset shows its preview in Source Image Preview panel
- [ ] Sprite Frames: clicking a frame shows its preview in SpriteFrame Preview panel
- [ ] Sub-region frames: preview shows only the cropped portion
- [ ] Full frames (non-split): preview shows the entire texture
- [ ] Split: frames get numbered names (e.g., "name_001", "name_002", ...)
- [ ] Split naming order: top-left is 001, bottom-right is last
- [ ] Panel layout: all panels visible, no overlap on initial load (1280x720 window)
- [ ] Panel dragging/resizing still works

- [ ] **Step 3: Final commit if any fixes were needed**

```bash
git add -A
git commit -m "fix(editor): address integration issues from final testing"
```
