# Atlas File Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Parse libGDX `.atlas` files on import, create a `SourceRegion` per atlas entry, load the associated PNG as a single texture, and enable each region to be added as a sprite frame.

**Architecture:** New `AtlasParser` utility in the core module parses the atlas text format into `AtlasRegionDescriptor` objects. The editor's import flow (`SourceImagesPanel`) calls the parser, creates `SourceRegion` entries with parsed bounds, and loads the PNG texture. Existing rendering and preview code works unchanged because `SourceAsset.filePath` will point to the PNG (not the `.atlas` file).

**Tech Stack:** Java 11, JUnit 4, libGDX TextureAtlas text format, no new dependencies.

---

## File Structure

| Action | File | Responsibility |
|--------|------|---------------|
| Create | `core/src/main/java/com/voidvvv/kzcollision/core/model/AtlasRegionDescriptor.java` | Parsed atlas region data (name, position, size, etc.) |
| Create | `core/src/main/java/com/voidvvv/kzcollision/core/AtlasParser.java` | Parses libGDX `.atlas` text format into `AtlasRegionDescriptor` list |
| Create | `core/src/test/java/com/voidvvv/kzcollision/core/AtlasParserTest.java` | Unit tests for `AtlasParser` |
| Modify | `editor/.../panels/SourceImagesPanel.java` | Atlas import flow: parse atlas, load PNG texture, create regions |
| Modify | `editor/.../panels/SourceImagesPanel.java` | `addAsFrame()` sets `subRegion` from region bounds |

No changes needed to `ViewportRenderer`, `SourceImagePreviewPanel`, or `SpriteFramePreviewPanel` — they already work with the `filePath` → texture cache → sub-region bounds pattern.

**Key design decision:** For atlas assets, `SourceAsset.filePath` stores the PNG path (the actual texture), and `SourceAsset.atlasFilePath` stores the `.atlas` text file path. This way `ViewportRenderer.getTexture()` and both preview panels find the texture via `asset.getFilePath()` without any changes.

---

### Task 1: AtlasRegionDescriptor data class

**Files:**
- Create: `core/src/main/java/com/voidvvv/kzcollision/core/model/AtlasRegionDescriptor.java`

- [ ] **Step 1: Create the data class**

```java
package com.voidvvv.kzcollision.core.model;

public class AtlasRegionDescriptor {
    private final String name;
    private final int index;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int origWidth;
    private final int origHeight;
    private final int offsetX;
    private final int offsetY;
    private final boolean rotate;

    public AtlasRegionDescriptor(String name, int index, int x, int y,
                                 int width, int height, int origWidth, int origHeight,
                                 int offsetX, int offsetY, boolean rotate) {
        this.name = name;
        this.index = index;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.origWidth = origWidth;
        this.origHeight = origHeight;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.rotate = rotate;
    }

    public String getName() { return name; }
    public int getIndex() { return index; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getOrigWidth() { return origWidth; }
    public int getOrigHeight() { return origHeight; }
    public int getOffsetX() { return offsetX; }
    public int getOffsetY() { return offsetY; }
    public boolean isRotate() { return rotate; }
}
```

- [ ] **Step 2: Commit**

```bash
git add core/src/main/java/com/voidvvv/kzcollision/core/model/AtlasRegionDescriptor.java
git commit -m "feat(core): add AtlasRegionDescriptor data class"
```

---

### Task 2: AtlasParser — write failing test

**Files:**
- Create: `core/src/test/java/com/voidvvv/kzcollision/core/AtlasParserTest.java`

- [ ] **Step 1: Write the test**

The test creates a temporary `.atlas` file in the libGDX format, parses it, and asserts the results. This covers the core parsing logic without needing a real atlas file.

```java
package com.voidvvv.kzcollision.core;

import com.voidvvv.kzcollision.core.model.AtlasRegionDescriptor;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static org.junit.Assert.*;

public class AtlasParserTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File createAtlasFile(String content) throws IOException {
        File f = tmp.newFile("test.atlas");
        try (PrintWriter w = new PrintWriter(f, "UTF-8")) {
            w.print(content);
        }
        return f;
    }

    @Test
    public void parsesSingleRegion() throws IOException {
        String atlas = "\n" +
            "test.png\n" +
            "size: 256,256\n" +
            "format: RGBA8888\n" +
            "filter: Nearest,Nearest\n" +
            "repeat: none\n" +
            "hero-idle\n" +
            "  rotate: false\n" +
            "  xy: 10, 20\n" +
            "  size: 32, 48\n" +
            "  orig: 32, 48\n" +
            "  offset: 0, 0\n" +
            "  index: -1\n";

        File f = createAtlasFile(atlas);
        List<AtlasRegionDescriptor> regions = AtlasParser.parse(f);

        assertEquals(1, regions.size());
        AtlasRegionDescriptor r = regions.get(0);
        assertEquals("hero-idle", r.getName());
        assertEquals(-1, r.getIndex());
        assertEquals(10, r.getX());
        assertEquals(20, r.getY());
        assertEquals(32, r.getWidth());
        assertEquals(48, r.getHeight());
        assertEquals(32, r.getOrigWidth());
        assertEquals(48, r.getOrigHeight());
        assertEquals(0, r.getOffsetX());
        assertEquals(0, r.getOffsetY());
        assertFalse(r.isRotate());
    }

    @Test
    public void parsesMultipleRegions() throws IOException {
        String atlas = "\n" +
            "sprites.png\n" +
            "size: 512,512\n" +
            "format: RGBA8888\n" +
            "filter: Linear,Linear\n" +
            "repeat: none\n" +
            "walk-0\n" +
            "  rotate: false\n" +
            "  xy: 0, 0\n" +
            "  size: 64, 64\n" +
            "  orig: 64, 64\n" +
            "  offset: 0, 0\n" +
            "  index: -1\n" +
            "walk-1\n" +
            "  rotate: false\n" +
            "  xy: 64, 0\n" +
            "  size: 64, 64\n" +
            "  orig: 64, 64\n" +
            "  offset: 0, 0\n" +
            "  index: -1\n";

        File f = createAtlasFile(atlas);
        List<AtlasRegionDescriptor> regions = AtlasParser.parse(f);

        assertEquals(2, regions.size());
        assertEquals("walk-0", regions.get(0).getName());
        assertEquals(0, regions.get(0).getX());
        assertEquals("walk-1", regions.get(1).getName());
        assertEquals(64, regions.get(1).getX());
    }

    @Test
    public void parsesIndexedRegion() throws IOException {
        String atlas = "\n" +
            "sheet.png\n" +
            "size: 256,256\n" +
            "format: RGBA8888\n" +
            "filter: Nearest,Nearest\n" +
            "repeat: none\n" +
            "explosion\n" +
            "  rotate: false\n" +
            "  xy: 0, 0\n" +
            "  size: 32, 32\n" +
            "  orig: 32, 32\n" +
            "  offset: 0, 0\n" +
            "  index: 3\n";

        File f = createAtlasFile(atlas);
        List<AtlasRegionDescriptor> regions = AtlasParser.parse(f);

        assertEquals(1, regions.size());
        assertEquals("explosion", regions.get(0).getName());
        assertEquals(3, regions.get(0).getIndex());
    }

    @Test
    public void parsesRotatedRegion() throws IOException {
        String atlas = "\n" +
            "sheet.png\n" +
            "size: 256,256\n" +
            "format: RGBA8888\n" +
            "filter: Nearest,Nearest\n" +
            "repeat: none\n" +
            "corner\n" +
            "  rotate: true\n" +
            "  xy: 100, 50\n" +
            "  size: 16, 32\n" +
            "  orig: 32, 16\n" +
            "  offset: 8, 0\n" +
            "  index: -1\n";

        File f = createAtlasFile(atlas);
        List<AtlasRegionDescriptor> regions = AtlasParser.parse(f);

        assertEquals(1, regions.size());
        assertTrue(regions.get(0).isRotate());
        assertEquals(100, regions.get(0).getX());
        assertEquals(50, regions.get(0).getY());
        assertEquals(16, regions.get(0).getWidth());
        assertEquals(32, regions.get(0).getHeight());
        assertEquals(8, regions.get(0).getOffsetX());
    }

    @Test(expected = IOException.class)
    public void throwsOnMissingFile() throws IOException {
        File missing = new File("/nonexistent/path.atlas");
        AtlasParser.parse(missing);
    }

    @Test
    public void parsesRealUiskinAtlas() throws IOException {
        File atlas = new File("assets/ui/uiskin.atlas");
        if (!atlas.exists()) {
            return; // skip if not running from project root
        }
        List<AtlasRegionDescriptor> regions = AtlasParser.parse(atlas);
        assertFalse("Should parse at least one region", regions.isEmpty());
        // Verify first region has expected structure
        AtlasRegionDescriptor first = regions.get(0);
        assertNotNull(first.getName());
        assertTrue(first.getWidth() > 0);
        assertTrue(first.getHeight() > 0);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:test --tests "com.voidvvv.kzcollision.core.AtlasParserTest" 2>&1 | tail -20`
Expected: Compilation error — `AtlasParser` class does not exist.

- [ ] **Step 3: Commit the test**

```bash
git add core/src/test/java/com/voidvvv/kzcollision/core/AtlasParserTest.java
git commit -m "test(core): add AtlasParser unit tests"
```

---

### Task 3: AtlasParser — implement parser

**Files:**
- Create: `core/src/main/java/com/voidvvv/kzcollision/core/AtlasParser.java`

- [ ] **Step 1: Write the parser implementation**

```java
package com.voidvvv.kzcollision.core;

import com.voidvvv.kzcollision.core.model.AtlasRegionDescriptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AtlasParser {

    public static List<AtlasRegionDescriptor> parse(File atlasFile) throws IOException {
        List<AtlasRegionDescriptor> regions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(atlasFile), StandardCharsets.UTF_8))) {

            // Skip blank lines at the start
            String line = skipBlankLines(reader);

            // Read page sections
            while (line != null) {
                // line is the page image filename
                line = readLine(reader); // size: W,H
                line = readLine(reader); // format: ...
                line = readLine(reader); // filter: ...
                line = readLine(reader); // repeat: ...

                // Read regions until blank line or EOF
                line = readLine(reader);
                while (line != null && !line.trim().isEmpty()) {
                    String regionName = line.trim();
                    line = readRegion(reader, regionName, regions);
                }

                // Skip blank lines between pages
                line = skipBlankLines(reader);
            }
        }

        return regions;
    }

    private static String skipBlankLines(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                return line;
            }
        }
        return null;
    }

    private static String readLine(BufferedReader reader) throws IOException {
        return reader.readLine();
    }

    private static String readRegion(BufferedReader reader, String name,
                                      List<AtlasRegionDescriptor> regions) throws IOException {
        boolean rotate = false;
        int x = 0, y = 0, width = 0, height = 0;
        int origWidth = 0, origHeight = 0;
        int offsetX = 0, offsetY = 0;
        int index = -1;

        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || (!trimmed.startsWith("rotate") && !trimmed.startsWith("xy")
                    && !trimmed.startsWith("size") && !trimmed.startsWith("orig")
                    && !trimmed.startsWith("offset") && !trimmed.startsWith("index"))) {
                // End of region properties — this line is either blank or the next region name
                break;
            }

            String[] parts = trimmed.split(": ", 2);
            if (parts.length < 2) continue;
            String key = parts[0];
            String value = parts[1];

            switch (key) {
                case "rotate":
                    rotate = Boolean.parseBoolean(value);
                    break;
                case "xy":
                    int[] xy = parseIntPair(value);
                    x = xy[0];
                    y = xy[1];
                    break;
                case "size":
                    int[] sz = parseIntPair(value);
                    width = sz[0];
                    height = sz[1];
                    break;
                case "orig":
                    int[] orig = parseIntPair(value);
                    origWidth = orig[0];
                    origHeight = orig[1];
                    break;
                case "offset":
                    int[] off = parseIntPair(value);
                    offsetX = off[0];
                    offsetY = off[1];
                    break;
                case "index":
                    index = Integer.parseInt(value.trim());
                    break;
            }
        }

        regions.add(new AtlasRegionDescriptor(name, index, x, y, width, height,
                origWidth, origHeight, offsetX, offsetY, rotate));

        return line;
    }

    private static int[] parseIntPair(String value) {
        String[] parts = value.split(",");
        return new int[]{
                Integer.parseInt(parts[0].trim()),
                Integer.parseInt(parts[1].trim())
        };
    }
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `./gradlew :core:test --tests "com.voidvvv.kzcollision.core.AtlasParserTest" 2>&1 | tail -20`
Expected: All tests PASS.

- [ ] **Step 3: Commit**

```bash
git add core/src/main/java/com/voidvvv/kzcollision/core/AtlasParser.java
git commit -m "feat(core): implement AtlasParser for libGDX atlas format"
```

---

### Task 4: Update import flow in SourceImagesPanel

**Files:**
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagesPanel.java`
  - Lines ~205-221: `openFileChooser()` — fix atlas asset creation
  - Lines ~242-264: `processPendingImports()` — add ATLAS branch

- [ ] **Step 1: Fix atlas asset creation in openFileChooser()**

Replace the atlas branch in `openFileChooser()` (inside the `for (File file : chooser.getSelectedFiles())` loop, the `if (fileName.endsWith(".atlas"))` block) with:

```java
if (fileName.endsWith(".atlas")) {
    // Resolve the PNG by convention: same directory, same basename
    String pngPath = absolutePath.replaceAll("\\.atlas$", ".png");
    asset = new SourceAsset();
    asset.setFilePath(pngPath);
    asset.setAtlasFilePath(absolutePath);
    asset.setType(AssetType.ATLAS);
} else {
```

This sets `filePath` to the PNG path (for texture cache lookup) and `atlasFilePath` to the .atlas path (for parsing). No region is created here — regions are populated in `processPendingImports()` after parsing.

- [ ] **Step 2: Add ATLAS branch in processPendingImports()**

Add an `else if` branch after the existing `if (asset.getType() == AssetType.SINGLE)` block:

```java
} else if (asset.getType() == AssetType.ATLAS) {
    String atlasPath = asset.getAtlasFilePath();
    String pngPath = asset.getFilePath();

    // Verify PNG exists
    if (!new java.io.File(pngPath).exists()) {
        Gdx.app.log("SourceImagesPanel",
            "Atlas PNG not found: " + pngPath + " (expected next to " + atlasPath + ")");
        project.getSourceAssets().remove(asset);
        continue;
    }

    // Parse atlas file
    List<AtlasRegionDescriptor> descriptors;
    try {
        descriptors = AtlasParser.parse(new java.io.File(atlasPath));
    } catch (Exception e) {
        Gdx.app.log("SourceImagesPanel", "Failed to parse atlas: " + atlasPath, e);
        project.getSourceAssets().remove(asset);
        continue;
    }

    // Load the atlas PNG texture
    try {
        Texture tex = new Texture(Gdx.files.absolute(pngPath));
        state.getTextureCache().put(pngPath, tex);
    } catch (Exception e) {
        Gdx.app.log("SourceImagesPanel", "Failed to load atlas texture: " + pngPath, e);
        project.getSourceAssets().remove(asset);
        continue;
    }

    // Create SourceRegions from parsed descriptors
    for (AtlasRegionDescriptor desc : descriptors) {
        String regionName = desc.getIndex() >= 0
                ? desc.getName() + "_" + desc.getIndex()
                : desc.getName();
        Rect bounds = new Rect(desc.getX(), desc.getY(), desc.getWidth(), desc.getHeight());
        SourceRegion region = new SourceRegion(regionName, asset.getId(), bounds);
        asset.getRegions().add(region);
    }
}
```

- [ ] **Step 3: Add imports to SourceImagesPanel**

Add these imports at the top of the file:

```java
import com.voidvvv.kzcollision.core.AtlasParser;
import com.voidvvv.kzcollision.core.model.AtlasRegionDescriptor;
```

- [ ] **Step 4: Build to verify compilation**

Run: `./gradlew :editor:compileJava 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagesPanel.java
git commit -m "feat(editor): parse atlas files and create regions on import"
```

---

### Task 5: Fix addAsFrame to use region bounds

**Files:**
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagesPanel.java`
  - Lines ~113-119: `addAsFrame()` method

When adding an atlas region as a frame, the `SpriteFrame.subRegion` must be set to the region's bounds so that `ViewportRenderer` and `SpriteFramePreviewPanel` render the cropped area instead of the full atlas texture.

- [ ] **Step 1: Update addAsFrame() to set subRegion from bounds**

Replace the `addAsFrame` method:

```java
private void addAsFrame(SourceRegion region) {
    SpriteFrame frame = new SpriteFrame();
    frame.setSourceAssetId(region.getAssetId());
    frame.setSourceRegionName(region.getName());
    frame.setSubRegion(region.getBounds());
    stateProvider.getState().getProject().getSpriteFrames().add(frame);
}
```

This is safe for all asset types:
- **SINGLE assets:** After import, `region.getBounds()` is `(0, 0, texWidth, texHeight)` — the full image. The viewport renders the full texture, same as before.
- **ATLAS assets:** `region.getBounds()` is the parsed `(x, y, w, h)` from the atlas file — the cropped region.
- **Null bounds:** If `region.getBounds()` is `null` (e.g., texture failed to load), `subRegion` stays `null`, and the viewport falls back to rendering the full texture.

- [ ] **Step 2: Build to verify compilation**

Run: `./gradlew :editor:compileJava 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagesPanel.java
git commit -m "fix(editor): set SpriteFrame subRegion from SourceRegion bounds in addAsFrame"
```

---

### Task 6: Build and manual verification

- [ ] **Step 1: Run full build including tests**

Run: `./gradlew build 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 2: Run the editor and manually test**

Run: `./gradlew :editor:run`

Manual test checklist:
1. Click "+ Import" → select `assets/ui/uiskin.atlas`
2. Verify the source panel shows a collapsible "(ATLAS)" entry
3. Expand it — verify all regions from uiskin.atlas are listed by name
4. Click a region → verify the preview panel shows the full atlas texture
5. Right-click a region → "Add as Frame" → verify a sprite frame appears
6. Select the sprite frame → verify the preview shows the cropped region
7. Create an animation, add the frame → verify it renders correctly in the viewport

- [ ] **Step 3: Final commit if any adjustments needed**
