# File Menu and Split Popup Fix — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix two bugs: (1) wire all File menu items to real actions, (2) fix the Split popup not opening due to ImGui ID stack mismatch.

**Architecture:** The Split fix is a one-file change in `SourceImagesPanel.java` — move `ImGui.openPopup()` to the same ID stack context as `beginPopupModal()`. The File menu fix adds action handlers to `PanelManager.java`, a `reset()` method to `EditorState.java`, and uses the existing `ProjectSerializer` for save/load with `JFileChooser` on the Swing EDT thread.

**Tech Stack:** Java 11, libGDX, Dear ImGui (imgui-java), JFileChooser (Swing), Gson, JUnit 4

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `editor/.../panels/SourceImagesPanel.java` | Modify | Fix split popup ID context bug |
| `editor/.../panels/PanelManager.java` | Modify | Wire File menu items to actions |
| `editor/.../EditorState.java` | Modify | Add `reset()` method |
| `core/.../serialization/ProjectSerializer.java` | No change | Already supports save/load |
| `core/.../model/Project.java` | No change | Already supports full state |
| `core/src/test/.../ProjectSerializerRoundTripTest.java` | Create | Verify save/load round-trip with realistic project data |
| `core/src/test/.../EditorStateResetTest.java` | Create | Verify EditorState.reset() clears all state |

---

### Task 1: Fix Split Popup ID Context Bug

**Files:**
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagesPanel.java`

- [ ] **Step 1: Add `splitNeedsOpen` field**

Add a new boolean field after `splitCols`:

```java
private boolean splitNeedsOpen;
```

- [ ] **Step 2: Remove `openPopup` from context menu, set flag instead**

In `renderRegionContextMenu`, replace the `ImGui.openPopup` call with the flag. The method currently looks like this (lines 100-115):

```java
private void renderRegionContextMenu(SourceRegion region) {
    String popupId = "region_ctx_" + region.getId();
    if (ImGui.beginPopupContextItem(popupId)) {
        if (ImGui.menuItem("Add as Frame")) {
            addAsFrame(region);
        }
        if (ImGui.menuItem("Split m x n...")) {
            splitRegionId = region.getId();
            splitRows.set(2);
            splitCols.set(2);
            showSplitPopup = true;
            ImGui.openPopup("Split##" + region.getId());
        }
        ImGui.endPopup();
    }
}
```

Change the `ImGui.menuItem("Split m x n...")` block to:

```java
        if (ImGui.menuItem("Split m x n...")) {
            splitRegionId = region.getId();
            splitRows.set(2);
            splitCols.set(2);
            showSplitPopup = true;
            splitNeedsOpen = true;
        }
```

(Remove the `ImGui.openPopup("Split##" + region.getId());` line, add `splitNeedsOpen = true;`.)

- [ ] **Step 3: Call `openPopup` in `renderSplitPopup` before `beginPopupModal`**

Replace the entire `renderSplitPopup` method (lines 124-148) with:

```java
private void renderSplitPopup() {
    String popupId = "Split##" + splitRegionId;
    if (splitNeedsOpen) {
        ImGui.openPopup(popupId);
        splitNeedsOpen = false;
    }
    if (ImGui.beginPopupModal(popupId)) {
        ImGui.text("Split region into grid");
        ImGui.inputInt("Rows", splitRows);
        ImGui.inputInt("Cols", splitCols);

        int rows = Math.max(1, splitRows.get());
        int cols = Math.max(1, splitCols.get());

        ImGui.separator();
        if (ImGui.button("Split")) {
            performSplit(rows, cols);
            showSplitPopup = false;
            ImGui.closeCurrentPopup();
        }
        ImGui.sameLine();
        if (ImGui.button("Cancel")) {
            showSplitPopup = false;
            ImGui.closeCurrentPopup();
        }
        ImGui.endPopup();
    }
}
```

- [ ] **Step 4: Build and verify compilation**

Run: `cd C:/myWareHouse/dev/java/imageedit && ./gradlew editor:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagesPanel.java
git commit -m "fix(editor): open split popup in correct ImGui ID stack context"
```

---

### Task 2: Add `reset()` to EditorState

**Files:**
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/EditorState.java`

- [ ] **Step 1: Add `reset()` method**

Add this method to `EditorState.java` after the `getTextureCache()` method:

```java
public void reset() {
    this.project = new Project("Untitled");
    this.selectedAnimationId = null;
    this.currentFrameIndex = 0;
    this.selectedCollisionBoxId = null;
    this.playing = false;
    this.playbackTimer = 0f;
    this.textureCache.clear();
}
```

- [ ] **Step 2: Build and verify compilation**

Run: `cd C:/myWareHouse/dev/java/imageedit && ./gradlew editor:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/EditorState.java
git commit -m "feat(editor): add reset() method to EditorState"
```

---

### Task 3: Write ProjectSerializer Round-Trip Test

**Files:**
- Create: `core/src/test/java/com/voidvvv/kzcollision/core/serialization/ProjectSerializerRoundTripTest.java`

- [ ] **Step 1: Create test directory structure**

```bash
mkdir -p core/src/test/java/com/voidvvv/kzcollision/core/serialization
```

- [ ] **Step 2: Write the test**

Create `core/src/test/java/com/voidvvv/kzcollision/core/serialization/ProjectSerializerRoundTripTest.java`:

```java
package com.voidvvv.kzcollision.core.serialization;

import com.voidvvv.kzcollision.core.model.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class ProjectSerializerRoundTripTest {

    private final ProjectSerializer serializer = new ProjectSerializer();
    private File tempFile;

    @Before
    public void setUp() throws IOException {
        tempFile = File.createTempFile("test_project", ".json");
        tempFile.deleteOnExit();
    }

    @After
    public void tearDown() {
        if (tempFile != null) tempFile.delete();
    }

    @Test
    public void roundTripEmptyProject() throws IOException {
        Project original = new Project("Empty");
        serializer.save(original, tempFile);
        Project loaded = serializer.load(tempFile);

        assertEquals("Empty", loaded.getName());
        assertTrue(loaded.getSourceAssets().isEmpty());
        assertTrue(loaded.getSpriteFrames().isEmpty());
        assertTrue(loaded.getAnimations().isEmpty());
    }

    @Test
    public void roundTripFullProject() throws IOException {
        Project original = new Project("FullTest");

        // Source asset with region
        SourceAsset asset = new SourceAsset();
        asset.setType(AssetType.SINGLE);
        asset.setFilePath("C:/images/sprite.png");
        SourceRegion region = new SourceRegion("sprite.png", asset.getId(), new Rect(0, 0, 128, 64));
        asset.getRegions().add(region);
        original.getSourceAssets().add(asset);

        // Sprite frame with sub-region
        SpriteFrame frame = new SpriteFrame();
        frame.setSourceAssetId(asset.getId());
        frame.setSourceRegionName("sprite.png");
        frame.setSubRegion(new Rect(0, 0, 32, 32));
        frame.setOffsetX(5f);
        frame.setOffsetY(10f);
        original.getSpriteFrames().add(frame);

        // Animation with frame + collision box
        Animation anim = new Animation("walk");
        AnimationFrame animFrame = new AnimationFrame();
        animFrame.setSpriteFrameId(frame.getId());
        animFrame.setDuration(0.15f);
        animFrame.setOriginX(16f);
        animFrame.setOriginY(8f);
        CollisionBox box = new CollisionBox(-8, -8, 16, 16, "body");
        animFrame.getCollisionBoxes().add(box);
        anim.getFrames().add(animFrame);
        original.getAnimations().add(anim);

        serializer.save(original, tempFile);
        Project loaded = serializer.load(tempFile);

        // Project metadata
        assertEquals("FullTest", loaded.getName());

        // Source assets
        assertEquals(1, loaded.getSourceAssets().size());
        SourceAsset loadedAsset = loaded.getSourceAssets().get(0);
        assertEquals("C:/images/sprite.png", loadedAsset.getFilePath());
        assertEquals(AssetType.SINGLE, loadedAsset.getType());
        assertEquals(1, loadedAsset.getRegions().size());
        SourceRegion loadedRegion = loadedAsset.getRegions().get(0);
        assertEquals("sprite.png", loadedRegion.getName());
        assertEquals(0f, loadedRegion.getBounds().x, 0.001f);
        assertEquals(0f, loadedRegion.getBounds().y, 0.001f);
        assertEquals(128f, loadedRegion.getBounds().width, 0.001f);
        assertEquals(64f, loadedRegion.getBounds().height, 0.001f);

        // Sprite frames
        assertEquals(1, loaded.getSpriteFrames().size());
        SpriteFrame loadedFrame = loaded.getSpriteFrames().get(0);
        assertEquals(loadedAsset.getId(), loadedFrame.getSourceAssetId());
        assertEquals("sprite.png", loadedFrame.getSourceRegionName());
        assertEquals(0f, loadedFrame.getSubRegion().x, 0.001f);
        assertEquals(32f, loadedFrame.getSubRegion().width, 0.001f);
        assertEquals(5f, loadedFrame.getOffsetX(), 0.001f);
        assertEquals(10f, loadedFrame.getOffsetY(), 0.001f);

        // Animations
        assertEquals(1, loaded.getAnimations().size());
        Animation loadedAnim = loaded.getAnimations().get(0);
        assertEquals("walk", loadedAnim.getName());
        assertEquals(1, loadedAnim.getFrames().size());

        AnimationFrame loadedAF = loadedAnim.getFrames().get(0);
        assertEquals(0.15f, loadedAF.getDuration(), 0.001f);
        assertEquals(16f, loadedAF.getOriginX(), 0.001f);
        assertEquals(8f, loadedAF.getOriginY(), 0.001f);

        assertEquals(1, loadedAF.getCollisionBoxes().size());
        CollisionBox loadedBox = loadedAF.getCollisionBoxes().get(0);
        assertEquals("body", loadedBox.getLabel());
        assertEquals(-8f, loadedBox.getX(), 0.001f);
        assertEquals(-8f, loadedBox.getY(), 0.001f);
        assertEquals(16f, loadedBox.getWidth(), 0.001f);
        assertEquals(16f, loadedBox.getHeight(), 0.001f);
    }

    @Test
    public void toJsonAndFromJson() throws IOException {
        Project original = new Project("JsonTest");
        String json = serializer.toJson(original);
        Project loaded = serializer.fromJson(json);
        assertEquals("JsonTest", loaded.getName());
    }
}
```

- [ ] **Step 3: Run the tests**

Run: `cd C:/myWareHouse/dev/java/imageedit && ./gradlew core:test --tests "com.voidvvv.kzcollision.core.serialization.ProjectSerializerRoundTripTest"`
Expected: All 3 tests PASS

- [ ] **Step 4: Commit**

```bash
git add core/src/test/java/com/voidvvv/kzcollision/core/serialization/ProjectSerializerRoundTripTest.java
git commit -m "test(core): add ProjectSerializer round-trip tests"
```

---

### Task 4: Wire File Menu — New Project, Save, Save As

**Files:**
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/panels/PanelManager.java`

- [ ] **Step 1: Add imports and fields**

Add these imports at the top of `PanelManager.java`:

```java
import com.voidvvv.kzcollision.core.model.Project;
import com.voidvvv.kzcollision.core.model.SourceAsset;
import com.voidvvv.kzcollision.core.model.AssetType;
import com.voidvvv.kzcollision.core.serialization.ProjectSerializer;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
```

Add these fields to the `PanelManager` class:

```java
private final ProjectSerializer serializer = new ProjectSerializer();
private String currentFilePath;
private final List<Runnable> pendingFileActions = new ArrayList<>();
```

- [ ] **Step 2: Add `processPendingFileActions` method**

```java
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
```

- [ ] **Step 3: Add `renderMenuBar` call to process pending actions**

At the top of the existing `render()` method, before `renderMenuBar()`, add:

```java
processPendingFileActions();
```

- [ ] **Step 4: Replace `renderMenuBar` with wired version**

Replace the entire `renderMenuBar()` method with:

```java
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
```

- [ ] **Step 5: Add helper methods for file operations**

Add these methods to `PanelManager`:

```java
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

        // Reload textures for source assets
        for (SourceAsset asset : project.getSourceAssets()) {
            if (asset.getType() == AssetType.SINGLE && asset.getFilePath() != null) {
                try {
                    Texture tex = new Texture(Gdx.files.absolute(asset.getFilePath()));
                    state.getTextureCache().put(asset.getFilePath(), tex);
                    if (!asset.getRegions().isEmpty()) {
                        asset.getRegions().get(0).setBounds(
                            new com.voidvvv.kzcollision.core.model.Rect(0, 0, tex.getWidth(), tex.getHeight()));
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
```

- [ ] **Step 6: Build and verify compilation**

Run: `cd C:/myWareHouse/dev/java/imageedit && ./gradlew editor:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/panels/PanelManager.java
git commit -m "feat(editor): wire File menu items to project save/load/export actions"
```

---

### Task 5: Manual Smoke Test

**Files:** None (manual verification)

- [ ] **Step 1: Launch the editor**

Run: `cd C:/myWareHouse/dev/java/imageedit && ./gradlew editor:run`

- [ ] **Step 2: Verify Split popup**

1. Click "+ Import" and import a PNG image
2. Right-click on the imported image in Source Images
3. Click "Split m x n..."
4. Expected: Modal popup appears with Rows/Cols inputs
5. Enter 2 rows, 2 cols and click "Split"
6. Expected: 4 new SpriteFrames appear in the Sprite Frames panel

- [ ] **Step 3: Verify New Project**

1. Import an image and create some frames/animations
2. Click File → New Project
3. Expected: All panels reset to empty state

- [ ] **Step 4: Verify Save/Save As**

1. Create a simple project with one animation
2. Click File → Save As...
3. Choose a location and save
4. Expected: .json file created at chosen location
5. Click File → Save Project
6. Expected: File saved without dialog (same path)

- [ ] **Step 5: Verify Open Project**

1. Click File → Open Project...
2. Select the .json file saved in step 4
3. Expected: Project loads with all data intact, textures reloaded

- [ ] **Step 6: Verify Export Collision JSON**

1. Click File → Export Collision JSON
2. Choose a location and export
3. Expected: .json file created containing only animations/frames/collision data (no source asset paths)
