# libGDX Project Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a libGDX project/assets-folder workflow that scans target assets, loads or creates `assets/collision/collision.json`, restores image/atlas resources, and saves rectangle collision data back to the target project.

**Architecture:** Keep the existing ImGui layout and add a small project-aware service layer under `com.voidvvv.edit2d.editor.project`. The service layer owns path resolution, asset indexing, collision JSON loading/saving, and resource recovery; panels consume status from `EditorState` and stay mostly UI-focused.

**Tech Stack:** Java 11, libGDX `FileHandle`, Gson via existing `ProjectSerializer`, ImGui Java, JUnit 4, Gradle `:editor:test`.

---

## File Structure

- Create `editor/src/main/java/com/voidvvv/kzcollision/editor/project/EditorProjectContext.java`
  - Immutable-ish value object for project root, assets root, current collision file, and open mode.
- Create `editor/src/main/java/com/voidvvv/kzcollision/editor/project/AssetRecord.java`
  - One indexed source resource: image or atlas, with internal path, file, atlas regions, and type.
- Create `editor/src/main/java/com/voidvvv/kzcollision/editor/project/AssetRegionRecord.java`
  - One indexed atlas or single-image region with display name and bounds.
- Create `editor/src/main/java/com/voidvvv/kzcollision/editor/project/AssetIndex.java`
  - Lookup structure built from `AssetRecord` entries.
- Create `editor/src/main/java/com/voidvvv/kzcollision/editor/project/AssetIndexBuilder.java`
  - Scans `assetsRoot`, parses atlas files with existing `AtlasParser`, builds `AssetIndex`.
- Create `editor/src/main/java/com/voidvvv/kzcollision/editor/project/ResourceStatus.java`
  - Enum: `MATCHED`, `REPAIRED`, `MISSING`, `CONFLICT`.
- Create `editor/src/main/java/com/voidvvv/kzcollision/editor/project/ResourceRecoveryReport.java`
  - Counts and per-asset status/candidate paths.
- Create `editor/src/main/java/com/voidvvv/kzcollision/editor/project/ResourceResolver.java`
  - Repairs loaded `Project.sourceAssets` against `AssetIndex`.
- Create `editor/src/main/java/com/voidvvv/kzcollision/editor/project/CollisionProjectService.java`
  - Resolves project/assets roots and loads/saves collision JSON.
- Create `editor/src/main/java/com/voidvvv/kzcollision/editor/project/EditorTextureLoader.java`
  - Loads textures using absolute files under `assetsRoot`, but keeps model paths assets-relative.
- Modify `editor/src/main/java/com/voidvvv/kzcollision/editor/EditorState.java`
  - Store current `EditorProjectContext`, `AssetIndex`, and `ResourceRecoveryReport`.
- Modify `editor/src/main/java/com/voidvvv/kzcollision/editor/panels/PanelManager.java`
  - Replace old project-file menu workflow with libGDX project/assets/collision JSON workflow.
- Modify `editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagesPanel.java`
  - Rename displayed title to `Assets`, show context/status, and import assets from `AssetIndex`.
- Modify preview/viewport files in Task 7 to show missing-resource messages and make viewport texture lookup cache-only:
  - `SourceImagePreviewPanel.java`
  - `SpriteFramePreviewPanel.java`
  - `ViewportRenderer.java`
- Add tests under `editor/src/test/java/com/voidvvv/kzcollision/editor/project/`.

---

### Task 1: Project Context and Collision File Service

**Files:**
- Create: `editor/src/main/java/com/voidvvv/kzcollision/editor/project/EditorProjectContext.java`
- Create: `editor/src/main/java/com/voidvvv/kzcollision/editor/project/CollisionProjectService.java`
- Test: `editor/src/test/java/com/voidvvv/kzcollision/editor/project/CollisionProjectServiceTest.java`

- [ ] **Step 1: Write failing tests for context resolution and default save path**

Create `CollisionProjectServiceTest.java`:

```java
package com.voidvvv.edit2d.editor.project;

import com.voidvvv.edit2d.core.model.Project;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

public class CollisionProjectServiceTest {
    @Test
    public void openProjectRoot_shouldResolveAssetsAndDefaultCollisionFile() throws Exception {
        File root = Files.createTempDirectory("edit2d-project").toFile();
        File assets = new File(root, "assets");
        Assert.assertTrue(assets.mkdirs());

        CollisionProjectService service = new CollisionProjectService();
        EditorProjectContext context = service.openProjectRoot(root);

        Assert.assertEquals(root.getCanonicalFile(), context.getProjectRoot().getCanonicalFile());
        Assert.assertEquals(assets.getCanonicalFile(), context.getAssetsRoot().getCanonicalFile());
        Assert.assertEquals(new File(assets, "collision/collision.json").getCanonicalFile(),
                context.getCollisionFile().getCanonicalFile());
        Assert.assertTrue(context.isOpenedFromProjectRoot());
    }

    @Test
    public void openAssetsFolder_shouldUseFolderAsAssetsRoot() throws Exception {
        File assets = Files.createTempDirectory("edit2d-assets").toFile();

        CollisionProjectService service = new CollisionProjectService();
        EditorProjectContext context = service.openAssetsFolder(assets);

        Assert.assertNull(context.getProjectRoot());
        Assert.assertEquals(assets.getCanonicalFile(), context.getAssetsRoot().getCanonicalFile());
        Assert.assertEquals(new File(assets, "collision/collision.json").getCanonicalFile(),
                context.getCollisionFile().getCanonicalFile());
        Assert.assertFalse(context.isOpenedFromProjectRoot());
    }

    @Test
    public void save_shouldCreateCollisionDirectoryAndWriteJson() throws Exception {
        File assets = Files.createTempDirectory("edit2d-assets").toFile();
        CollisionProjectService service = new CollisionProjectService();
        EditorProjectContext context = service.openAssetsFolder(assets);

        Project project = new Project("Saved");
        service.save(project, context.getCollisionFile());

        Assert.assertTrue(context.getCollisionFile().isFile());
        Project loaded = service.loadOrCreate(context.getCollisionFile(), "Fallback");
        Assert.assertEquals("Saved", loaded.getName());
    }

    @Test
    public void loadOrCreate_shouldCreateEmptyProjectWhenFileIsMissing() throws Exception {
        File assets = Files.createTempDirectory("edit2d-assets").toFile();
        CollisionProjectService service = new CollisionProjectService();
        EditorProjectContext context = service.openAssetsFolder(assets);

        Project project = service.loadOrCreate(context.getCollisionFile(), "Untitled");

        Assert.assertEquals("Untitled", project.getName());
        Assert.assertTrue(project.getSourceAssets().isEmpty());
        Assert.assertFalse(context.getCollisionFile().exists());
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `.\gradlew.bat :editor:test --tests com.voidvvv.edit2d.editor.project.CollisionProjectServiceTest`

Expected: FAIL because `CollisionProjectService` and `EditorProjectContext` do not exist.

- [ ] **Step 3: Implement `EditorProjectContext`**

Create `EditorProjectContext.java`:

```java
package com.voidvvv.edit2d.editor.project;

import java.io.File;

public class EditorProjectContext {
    private final File projectRoot;
    private final File assetsRoot;
    private final File collisionFile;
    private final boolean openedFromProjectRoot;

    public EditorProjectContext(File projectRoot, File assetsRoot, File collisionFile,
                                boolean openedFromProjectRoot) {
        this.projectRoot = projectRoot;
        this.assetsRoot = assetsRoot;
        this.collisionFile = collisionFile;
        this.openedFromProjectRoot = openedFromProjectRoot;
    }

    public File getProjectRoot() {
        return projectRoot;
    }

    public File getAssetsRoot() {
        return assetsRoot;
    }

    public File getCollisionFile() {
        return collisionFile;
    }

    public boolean isOpenedFromProjectRoot() {
        return openedFromProjectRoot;
    }
}
```

- [ ] **Step 4: Implement `CollisionProjectService`**

Create `CollisionProjectService.java`:

```java
package com.voidvvv.edit2d.editor.project;

import com.voidvvv.edit2d.core.model.Project;
import com.voidvvv.edit2d.core.serialization.ProjectSerializer;

import java.io.File;
import java.io.IOException;

public class CollisionProjectService {
    public static final String DEFAULT_COLLISION_PATH = "collision/collision.json";

    private final ProjectSerializer serializer;

    public CollisionProjectService() {
        this(new ProjectSerializer());
    }

    public CollisionProjectService(ProjectSerializer serializer) {
        this.serializer = serializer;
    }

    public EditorProjectContext openProjectRoot(File projectRoot) {
        File assetsRoot = new File(projectRoot, "assets");
        if (!assetsRoot.isDirectory()) {
            throw new IllegalArgumentException("Project root does not contain an assets directory: "
                    + projectRoot.getAbsolutePath());
        }
        return new EditorProjectContext(projectRoot, assetsRoot, defaultCollisionFile(assetsRoot), true);
    }

    public EditorProjectContext openAssetsFolder(File assetsRoot) {
        if (!assetsRoot.isDirectory()) {
            throw new IllegalArgumentException("Assets folder does not exist: " + assetsRoot.getAbsolutePath());
        }
        return new EditorProjectContext(null, assetsRoot, defaultCollisionFile(assetsRoot), false);
    }

    public EditorProjectContext withCollisionFile(EditorProjectContext context, File collisionFile) {
        return new EditorProjectContext(context.getProjectRoot(), context.getAssetsRoot(),
                collisionFile, context.isOpenedFromProjectRoot());
    }

    public Project loadOrCreate(File collisionFile, String fallbackName) throws IOException {
        if (!collisionFile.isFile()) {
            return new Project(fallbackName);
        }
        return serializer.load(collisionFile);
    }

    public void save(Project project, File collisionFile) throws IOException {
        File parent = collisionFile.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Failed to create collision directory: " + parent.getAbsolutePath());
        }
        serializer.save(project, collisionFile);
    }

    private File defaultCollisionFile(File assetsRoot) {
        return new File(assetsRoot, DEFAULT_COLLISION_PATH);
    }
}
```

- [ ] **Step 5: Run the service tests**

Run: `.\gradlew.bat :editor:test --tests com.voidvvv.edit2d.editor.project.CollisionProjectServiceTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/project/EditorProjectContext.java \
        editor/src/main/java/com/voidvvv/kzcollision/editor/project/CollisionProjectService.java \
        editor/src/test/java/com/voidvvv/kzcollision/editor/project/CollisionProjectServiceTest.java
git commit -m "feat(editor): add collision project context service"
```

---

### Task 2: Asset Index and Atlas-Aware Scanning

**Files:**
- Create: `editor/src/main/java/com/voidvvv/kzcollision/editor/project/AssetRegionRecord.java`
- Create: `editor/src/main/java/com/voidvvv/kzcollision/editor/project/AssetRecord.java`
- Create: `editor/src/main/java/com/voidvvv/kzcollision/editor/project/AssetIndex.java`
- Create: `editor/src/main/java/com/voidvvv/kzcollision/editor/project/AssetIndexBuilder.java`
- Test: `editor/src/test/java/com/voidvvv/kzcollision/editor/project/AssetIndexBuilderTest.java`

- [ ] **Step 1: Write failing tests for asset index lookups**

Create `AssetIndexBuilderTest.java`:

```java
package com.voidvvv.edit2d.editor.project;

import com.voidvvv.edit2d.core.model.AssetType;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class AssetIndexBuilderTest {
    @Test
    public void build_shouldIndexImagesAtlasRegionsAndSkipCollision() throws Exception {
        File assets = Files.createTempDirectory("edit2d-assets").toFile();
        File sprites = new File(assets, "sprites");
        File collision = new File(assets, "collision");
        Assert.assertTrue(sprites.mkdirs());
        Assert.assertTrue(collision.mkdirs());
        Assert.assertTrue(new File(sprites, "hero.png").createNewFile());
        Assert.assertTrue(new File(collision, "collision.json").createNewFile());

        File atlas = new File(sprites, "enemies.atlas");
        Files.write(atlas.toPath(), atlasText().getBytes(StandardCharsets.UTF_8));

        AssetIndex index = new AssetIndexBuilder().build(assets);

        Assert.assertNotNull(index.findByInternalPath("sprites/hero.png"));
        Assert.assertNull(index.findByInternalPath("collision/collision.json"));

        AssetRecord atlasRecord = index.findByInternalPath("sprites/enemies.atlas");
        Assert.assertNotNull(atlasRecord);
        Assert.assertEquals(AssetType.ATLAS, atlasRecord.getType());
        Assert.assertNotNull(atlasRecord.findRegion("slime_0"));
        Assert.assertEquals(16f, atlasRecord.findRegion("slime_0").getBounds().width, 0.001f);

        List<AssetRecord> byName = index.findByFileName("hero.png");
        Assert.assertEquals(1, byName.size());
        Assert.assertEquals("sprites/hero.png", byName.get(0).getInternalPath());
    }

    private static String atlasText() {
        return "enemies.png\n"
                + "size: 64,64\n"
                + "format: RGBA8888\n"
                + "filter: Nearest,Nearest\n"
                + "repeat: none\n"
                + "slime\n"
                + "  rotate: false\n"
                + "  xy: 4, 8\n"
                + "  size: 16, 12\n"
                + "  orig: 16, 12\n"
                + "  offset: 0, 0\n"
                + "  index: 0\n";
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `.\gradlew.bat :editor:test --tests com.voidvvv.edit2d.editor.project.AssetIndexBuilderTest`

Expected: FAIL because the asset index classes do not exist.

- [ ] **Step 3: Implement asset record classes**

Create `AssetRegionRecord.java`:

```java
package com.voidvvv.edit2d.editor.project;

import com.voidvvv.edit2d.core.model.Rect;

public class AssetRegionRecord {
    private final String name;
    private final Rect bounds;

    public AssetRegionRecord(String name, Rect bounds) {
        this.name = name;
        this.bounds = bounds;
    }

    public String getName() {
        return name;
    }

    public Rect getBounds() {
        return bounds;
    }
}
```

Create `AssetRecord.java`:

```java
package com.voidvvv.edit2d.editor.project;

import com.voidvvv.edit2d.core.model.AssetType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AssetRecord {
    private final AssetType type;
    private final String internalPath;
    private final File file;
    private final List<AssetRegionRecord> regions = new ArrayList<>();

    public AssetRecord(AssetType type, String internalPath, File file) {
        this.type = type;
        this.internalPath = internalPath;
        this.file = file;
    }

    public AssetType getType() {
        return type;
    }

    public String getInternalPath() {
        return internalPath;
    }

    public File getFile() {
        return file;
    }

    public List<AssetRegionRecord> getRegions() {
        return Collections.unmodifiableList(regions);
    }

    public void addRegion(AssetRegionRecord region) {
        regions.add(region);
    }

    public AssetRegionRecord findRegion(String name) {
        for (AssetRegionRecord region : regions) {
            if (region.getName().equals(name)) {
                return region;
            }
        }
        return null;
    }
}
```

- [ ] **Step 4: Implement `AssetIndex`**

Create `AssetIndex.java`:

```java
package com.voidvvv.edit2d.editor.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AssetIndex {
    private final File assetsRoot;
    private final List<AssetRecord> records = new ArrayList<>();
    private final Map<String, AssetRecord> byInternalPath = new HashMap<>();
    private final Map<String, List<AssetRecord>> byFileName = new HashMap<>();

    public AssetIndex(File assetsRoot) {
        this.assetsRoot = assetsRoot;
    }

    public File getAssetsRoot() {
        return assetsRoot;
    }

    public List<AssetRecord> getRecords() {
        return Collections.unmodifiableList(records);
    }

    public void add(AssetRecord record) {
        records.add(record);
        byInternalPath.put(normalize(record.getInternalPath()), record);
        String fileName = new File(record.getInternalPath()).getName().toLowerCase(Locale.ROOT);
        byFileName.computeIfAbsent(fileName, key -> new ArrayList<>()).add(record);
    }

    public AssetRecord findByInternalPath(String internalPath) {
        if (internalPath == null) {
            return null;
        }
        return byInternalPath.get(normalize(internalPath));
    }

    public List<AssetRecord> findByFileName(String fileName) {
        if (fileName == null) {
            return Collections.emptyList();
        }
        List<AssetRecord> found = byFileName.get(fileName.toLowerCase(Locale.ROOT));
        return found == null ? Collections.emptyList() : Collections.unmodifiableList(found);
    }

    public static String normalize(String path) {
        return path.replace('\\', '/');
    }
}
```

- [ ] **Step 5: Implement `AssetIndexBuilder`**

Create `AssetIndexBuilder.java`:

```java
package com.voidvvv.edit2d.editor.project;

import com.voidvvv.edit2d.core.AtlasParser;
import com.voidvvv.edit2d.core.model.AssetType;
import com.voidvvv.edit2d.core.model.AtlasRegionDescriptor;
import com.voidvvv.edit2d.core.model.Rect;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class AssetIndexBuilder {
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList("png", "jpg", "jpeg"));

    public AssetIndex build(File assetsRoot) throws IOException {
        AssetIndex index = new AssetIndex(assetsRoot);
        scan(assetsRoot, assetsRoot, index);
        return index;
    }

    private void scan(File assetsRoot, File current, AssetIndex index) throws IOException {
        File[] files = current.listFiles();
        if (files == null) {
            return;
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            if (file.isDirectory()) {
                if ("collision".equalsIgnoreCase(file.getName())) {
                    continue;
                }
                scan(assetsRoot, file, index);
                continue;
            }

            String ext = extension(file.getName());
            String internalPath = toInternalPath(assetsRoot, file);
            if (IMAGE_EXTENSIONS.contains(ext)) {
                AssetRecord record = new AssetRecord(AssetType.SINGLE, internalPath, file);
                record.addRegion(new AssetRegionRecord(file.getName(), null));
                index.add(record);
            } else if ("atlas".equals(ext)) {
                AssetRecord record = new AssetRecord(AssetType.ATLAS, internalPath, file);
                for (AtlasRegionDescriptor descriptor : AtlasParser.parse(file)) {
                    String name = descriptor.getIndex() >= 0
                            ? descriptor.getName() + "_" + descriptor.getIndex()
                            : descriptor.getName();
                    record.addRegion(new AssetRegionRecord(name, new Rect(descriptor.getX(), descriptor.getY(),
                            descriptor.getWidth(), descriptor.getHeight())));
                }
                index.add(record);
            }
        }
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String toInternalPath(File assetsRoot, File file) throws IOException {
        return AssetIndex.normalize(assetsRoot.getCanonicalFile().toPath()
                .relativize(file.getCanonicalFile().toPath()).toString());
    }
}
```

- [ ] **Step 6: Run the asset index tests**

Run: `.\gradlew.bat :editor:test --tests com.voidvvv.edit2d.editor.project.AssetIndexBuilderTest`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/project/AssetRegionRecord.java \
        editor/src/main/java/com/voidvvv/kzcollision/editor/project/AssetRecord.java \
        editor/src/main/java/com/voidvvv/kzcollision/editor/project/AssetIndex.java \
        editor/src/main/java/com/voidvvv/kzcollision/editor/project/AssetIndexBuilder.java \
        editor/src/test/java/com/voidvvv/kzcollision/editor/project/AssetIndexBuilderTest.java
git commit -m "feat(editor): index project image assets"
```

---

### Task 3: Resource Recovery Report and Resolver

**Files:**
- Create: `editor/src/main/java/com/voidvvv/kzcollision/editor/project/ResourceStatus.java`
- Create: `editor/src/main/java/com/voidvvv/kzcollision/editor/project/ResourceRecoveryReport.java`
- Create: `editor/src/main/java/com/voidvvv/kzcollision/editor/project/ResourceResolver.java`
- Test: `editor/src/test/java/com/voidvvv/kzcollision/editor/project/ResourceResolverTest.java`

- [ ] **Step 1: Write failing tests for exact match, repair, conflict, and missing preservation**

Create `ResourceResolverTest.java`:

```java
package com.voidvvv.edit2d.editor.project;

import com.voidvvv.edit2d.core.model.AssetType;
import com.voidvvv.edit2d.core.model.Project;
import com.voidvvv.edit2d.core.model.Rect;
import com.voidvvv.edit2d.core.model.SourceAsset;
import com.voidvvv.edit2d.core.model.SourceRegion;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

public class ResourceResolverTest {
    @Test
    public void resolve_shouldMatchExistingInternalPath() throws Exception {
        File assets = Files.createTempDirectory("edit2d-assets").toFile();
        File hero = new File(assets, "hero.png");
        Assert.assertTrue(hero.createNewFile());
        AssetIndex index = new AssetIndexBuilder().build(assets);
        Project project = projectWithSingleAsset("hero.png");

        ResourceRecoveryReport report = new ResourceResolver().resolve(project, index);

        Assert.assertEquals(ResourceStatus.MATCHED, report.getStatus(project.getSourceAssets().get(0).getId()));
        Assert.assertEquals(1, report.getMatchedCount());
        Assert.assertEquals("hero.png", project.getSourceAssets().get(0).getInternalPath());
    }

    @Test
    public void resolve_shouldRepairUniqueMovedFileByName() throws Exception {
        File assets = Files.createTempDirectory("edit2d-assets").toFile();
        File sprites = new File(assets, "sprites");
        Assert.assertTrue(sprites.mkdirs());
        Assert.assertTrue(new File(sprites, "hero.png").createNewFile());
        AssetIndex index = new AssetIndexBuilder().build(assets);
        Project project = projectWithSingleAsset("old/hero.png");

        ResourceRecoveryReport report = new ResourceResolver().resolve(project, index);

        Assert.assertEquals(ResourceStatus.REPAIRED, report.getStatus(project.getSourceAssets().get(0).getId()));
        Assert.assertEquals(1, report.getRepairedCount());
        Assert.assertEquals("sprites/hero.png", project.getSourceAssets().get(0).getInternalPath());
    }

    @Test
    public void resolve_shouldLeaveConflictWhenMultipleCandidatesExist() throws Exception {
        File assets = Files.createTempDirectory("edit2d-assets").toFile();
        File a = new File(assets, "a");
        File b = new File(assets, "b");
        Assert.assertTrue(a.mkdirs());
        Assert.assertTrue(b.mkdirs());
        Assert.assertTrue(new File(a, "hero.png").createNewFile());
        Assert.assertTrue(new File(b, "hero.png").createNewFile());
        AssetIndex index = new AssetIndexBuilder().build(assets);
        Project project = projectWithSingleAsset("old/hero.png");

        ResourceRecoveryReport report = new ResourceResolver().resolve(project, index);

        String assetId = project.getSourceAssets().get(0).getId();
        Assert.assertEquals(ResourceStatus.CONFLICT, report.getStatus(assetId));
        Assert.assertEquals(1, report.getConflictCount());
        Assert.assertEquals(2, report.getCandidates(assetId).size());
        Assert.assertEquals("old/hero.png", project.getSourceAssets().get(0).getInternalPath());
    }

    @Test
    public void resolve_shouldPreserveMissingResource() throws Exception {
        File assets = Files.createTempDirectory("edit2d-assets").toFile();
        AssetIndex index = new AssetIndexBuilder().build(assets);
        Project project = projectWithSingleAsset("missing/hero.png");

        ResourceRecoveryReport report = new ResourceResolver().resolve(project, index);

        Assert.assertEquals(ResourceStatus.MISSING, report.getStatus(project.getSourceAssets().get(0).getId()));
        Assert.assertEquals(1, report.getMissingCount());
        Assert.assertEquals("missing/hero.png", project.getSourceAssets().get(0).getInternalPath());
        Assert.assertEquals(1, project.getSourceAssets().size());
    }

    private static Project projectWithSingleAsset(String internalPath) {
        Project project = new Project("Test");
        SourceAsset asset = new SourceAsset();
        asset.setType(AssetType.SINGLE);
        asset.setInternalPath(internalPath);
        asset.getRegions().add(new SourceRegion("hero.png", asset.getId(), new Rect(0, 0, 16, 16)));
        project.getSourceAssets().add(asset);
        return project;
    }
}
```

- [ ] **Step 2: Run the resolver test and verify it fails**

Run: `.\gradlew.bat :editor:test --tests com.voidvvv.edit2d.editor.project.ResourceResolverTest`

Expected: FAIL because resolver/report classes do not exist.

- [ ] **Step 3: Implement status and report classes**

Create `ResourceStatus.java`:

```java
package com.voidvvv.edit2d.editor.project;

public enum ResourceStatus {
    MATCHED,
    REPAIRED,
    MISSING,
    CONFLICT
}
```

Create `ResourceRecoveryReport.java`:

```java
package com.voidvvv.edit2d.editor.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceRecoveryReport {
    private final Map<String, ResourceStatus> statuses = new HashMap<>();
    private final Map<String, List<String>> candidates = new HashMap<>();

    public void setStatus(String assetId, ResourceStatus status) {
        statuses.put(assetId, status);
    }

    public ResourceStatus getStatus(String assetId) {
        return statuses.get(assetId);
    }

    public void setCandidates(String assetId, List<String> paths) {
        candidates.put(assetId, new ArrayList<>(paths));
    }

    public List<String> getCandidates(String assetId) {
        List<String> found = candidates.get(assetId);
        return found == null ? Collections.emptyList() : Collections.unmodifiableList(found);
    }

    public int getMatchedCount() {
        return count(ResourceStatus.MATCHED);
    }

    public int getRepairedCount() {
        return count(ResourceStatus.REPAIRED);
    }

    public int getMissingCount() {
        return count(ResourceStatus.MISSING);
    }

    public int getConflictCount() {
        return count(ResourceStatus.CONFLICT);
    }

    private int count(ResourceStatus status) {
        int total = 0;
        for (ResourceStatus value : statuses.values()) {
            if (value == status) {
                total++;
            }
        }
        return total;
    }
}
```

- [ ] **Step 4: Implement `ResourceResolver`**

Create `ResourceResolver.java`:

```java
package com.voidvvv.edit2d.editor.project;

import com.voidvvv.edit2d.core.model.AssetType;
import com.voidvvv.edit2d.core.model.Project;
import com.voidvvv.edit2d.core.model.SourceAsset;
import com.voidvvv.edit2d.core.model.SourceRegion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ResourceResolver {
    public ResourceRecoveryReport resolve(Project project, AssetIndex index) {
        ResourceRecoveryReport report = new ResourceRecoveryReport();
        for (SourceAsset asset : project.getSourceAssets()) {
            resolveAsset(asset, index, report);
        }
        return report;
    }

    private void resolveAsset(SourceAsset asset, AssetIndex index, ResourceRecoveryReport report) {
        String normalizedPath = asset.getInternalPath() == null ? null : AssetIndex.normalize(asset.getInternalPath());
        AssetRecord exact = index.findByInternalPath(normalizedPath);
        if (exact != null && isCompatible(asset, exact)) {
            applyRecord(asset, exact);
            report.setStatus(asset.getId(), ResourceStatus.MATCHED);
            return;
        }

        String fileName = normalizedPath == null ? "" : new File(normalizedPath).getName();
        List<AssetRecord> candidates = compatibleCandidates(asset, index.findByFileName(fileName));
        if (candidates.size() == 1) {
            applyRecord(asset, candidates.get(0));
            report.setStatus(asset.getId(), ResourceStatus.REPAIRED);
            return;
        }
        if (candidates.size() > 1) {
            List<String> paths = new ArrayList<>();
            for (AssetRecord candidate : candidates) {
                paths.add(candidate.getInternalPath());
            }
            report.setCandidates(asset.getId(), paths);
            report.setStatus(asset.getId(), ResourceStatus.CONFLICT);
            return;
        }
        report.setStatus(asset.getId(), ResourceStatus.MISSING);
    }

    private List<AssetRecord> compatibleCandidates(SourceAsset asset, List<AssetRecord> records) {
        List<AssetRecord> out = new ArrayList<>();
        for (AssetRecord record : records) {
            if (isCompatible(asset, record)) {
                out.add(record);
            }
        }
        return out;
    }

    private boolean isCompatible(SourceAsset asset, AssetRecord record) {
        if (asset.getType() != record.getType()) {
            return false;
        }
        if (asset.getType() == AssetType.ATLAS) {
            for (SourceRegion region : asset.getRegions()) {
                if (record.findRegion(region.getName()) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private void applyRecord(SourceAsset asset, AssetRecord record) {
        asset.setInternalPath(record.getInternalPath());
        if (asset.getType() == AssetType.ATLAS) {
            for (SourceRegion region : asset.getRegions()) {
                AssetRegionRecord indexedRegion = record.findRegion(region.getName());
                if (indexedRegion != null) {
                    region.setBounds(indexedRegion.getBounds());
                }
            }
        }
    }
}
```

- [ ] **Step 5: Run resolver tests**

Run: `.\gradlew.bat :editor:test --tests com.voidvvv.edit2d.editor.project.ResourceResolverTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/project/ResourceStatus.java \
        editor/src/main/java/com/voidvvv/kzcollision/editor/project/ResourceRecoveryReport.java \
        editor/src/main/java/com/voidvvv/kzcollision/editor/project/ResourceResolver.java \
        editor/src/test/java/com/voidvvv/kzcollision/editor/project/ResourceResolverTest.java
git commit -m "feat(editor): resolve project resources"
```

---

### Task 4: Editor State and Texture Loader Integration

**Files:**
- Create: `editor/src/main/java/com/voidvvv/kzcollision/editor/project/EditorTextureLoader.java`
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/EditorState.java`
- Test: `editor/src/test/java/com/voidvvv/kzcollision/editor/EditorStateProjectContextTest.java`

- [ ] **Step 1: Write failing tests for state project context reset**

Create `EditorStateProjectContextTest.java`:

```java
package com.voidvvv.edit2d.editor;

import com.voidvvv.edit2d.editor.project.AssetIndex;
import com.voidvvv.edit2d.editor.project.EditorProjectContext;
import com.voidvvv.edit2d.editor.project.ResourceRecoveryReport;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

public class EditorStateProjectContextTest {
    @Test
    public void reset_shouldClearProjectContextIndexAndReport() throws Exception {
        File assets = Files.createTempDirectory("edit2d-assets").toFile();
        EditorState state = new EditorState();
        state.setProjectContext(new EditorProjectContext(null, assets,
                new File(assets, "collision/collision.json"), false));
        state.setAssetIndex(new AssetIndex(assets));
        state.setRecoveryReport(new ResourceRecoveryReport());

        state.reset();

        Assert.assertNull(state.getProjectContext());
        Assert.assertNull(state.getAssetIndex());
        Assert.assertNull(state.getRecoveryReport());
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `.\gradlew.bat :editor:test --tests com.voidvvv.edit2d.editor.EditorStateProjectContextTest`

Expected: FAIL because `EditorState` lacks context/index/report accessors.

- [ ] **Step 3: Modify `EditorState`**

Add imports:

```java
import com.voidvvv.edit2d.editor.project.AssetIndex;
import com.voidvvv.edit2d.editor.project.EditorProjectContext;
import com.voidvvv.edit2d.editor.project.ResourceRecoveryReport;
```

Add fields:

```java
private EditorProjectContext projectContext;
private AssetIndex assetIndex;
private ResourceRecoveryReport recoveryReport;
```

Add accessors:

```java
public EditorProjectContext getProjectContext() { return projectContext; }
public void setProjectContext(EditorProjectContext projectContext) { this.projectContext = projectContext; }
public AssetIndex getAssetIndex() { return assetIndex; }
public void setAssetIndex(AssetIndex assetIndex) { this.assetIndex = assetIndex; }
public ResourceRecoveryReport getRecoveryReport() { return recoveryReport; }
public void setRecoveryReport(ResourceRecoveryReport recoveryReport) { this.recoveryReport = recoveryReport; }
```

Add to `reset()` after selection fields are cleared:

```java
this.projectContext = null;
this.assetIndex = null;
this.recoveryReport = null;
```

- [ ] **Step 4: Add texture loader**

Create `EditorTextureLoader.java`:

```java
package com.voidvvv.edit2d.editor.project;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.ObjectSet;
import com.voidvvv.edit2d.core.model.AssetType;
import com.voidvvv.edit2d.core.model.Project;
import com.voidvvv.edit2d.core.model.Rect;
import com.voidvvv.edit2d.core.model.SourceAsset;
import com.voidvvv.edit2d.core.model.SourceRegion;
import com.voidvvv.edit2d.editor.EditorState;

import java.io.File;

public class EditorTextureLoader {
    public void reload(EditorState state) {
        clearTextureCache(state);
        EditorProjectContext context = state.getProjectContext();
        Project project = state.getProject();
        if (context == null || project == null) {
            return;
        }

        for (SourceAsset asset : project.getSourceAssets()) {
            if (asset.getInternalPath() == null) {
                continue;
            }
            try {
                File file = new File(context.getAssetsRoot(), AssetIndex.normalize(asset.getInternalPath()));
                if (asset.getType() == AssetType.SINGLE) {
                    Texture texture = new Texture(Gdx.files.absolute(file.getAbsolutePath()));
                    state.getTextureCache().put(asset.getInternalPath(), texture);
                    ensureSingleRegionBounds(asset, texture);
                } else if (asset.getType() == AssetType.ATLAS) {
                    TextureAtlas atlas = new TextureAtlas(Gdx.files.absolute(file.getAbsolutePath()));
                    ObjectSet<Texture> textures = atlas.getTextures();
                    if (textures.size > 0) {
                        state.getTextureCache().put(asset.getInternalPath(), textures.first());
                    }
                }
            } catch (Exception e) {
                Gdx.app.log("EditorTextureLoader", "Failed to load texture: " + asset.getInternalPath(), e);
            }
        }
    }

    public void clearTextureCache(EditorState state) {
        for (Texture texture : state.getTextureCache().values()) {
            texture.dispose();
        }
        state.getTextureCache().clear();
    }

    private void ensureSingleRegionBounds(SourceAsset asset, Texture texture) {
        if (asset.getRegions().isEmpty()) {
            asset.getRegions().add(new SourceRegion(asset.getInternalPath(), asset.getId(),
                    new Rect(0, 0, texture.getWidth(), texture.getHeight())));
            return;
        }
        SourceRegion region = asset.getRegions().get(0);
        if (region.getBounds() == null) {
            region.setBounds(new Rect(0, 0, texture.getWidth(), texture.getHeight()));
        }
    }
}
```

- [ ] **Step 5: Run state tests**

Run: `.\gradlew.bat :editor:test --tests com.voidvvv.edit2d.editor.EditorStateProjectContextTest`

Expected: PASS.

- [ ] **Step 6: Run all editor tests**

Run: `.\gradlew.bat :editor:test`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/EditorState.java \
        editor/src/main/java/com/voidvvv/kzcollision/editor/project/EditorTextureLoader.java \
        editor/src/test/java/com/voidvvv/kzcollision/editor/EditorStateProjectContextTest.java
git commit -m "feat(editor): track opened asset project state"
```

---

### Task 5: Project Open/Save Workflow in `PanelManager`

**Files:**
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/panels/PanelManager.java`

- [ ] **Step 1: Replace service fields in `PanelManager`**

Add imports:

```java
import com.voidvvv.edit2d.editor.project.AssetIndex;
import com.voidvvv.edit2d.editor.project.AssetIndexBuilder;
import com.voidvvv.edit2d.editor.project.CollisionProjectService;
import com.voidvvv.edit2d.editor.project.EditorProjectContext;
import com.voidvvv.edit2d.editor.project.EditorTextureLoader;
import com.voidvvv.edit2d.editor.project.ResourceRecoveryReport;
import com.voidvvv.edit2d.editor.project.ResourceResolver;
```

Replace the serializer/current path fields with:

```java
private final CollisionProjectService projectService = new CollisionProjectService();
private final AssetIndexBuilder assetIndexBuilder = new AssetIndexBuilder();
private final ResourceResolver resourceResolver = new ResourceResolver();
private final EditorTextureLoader textureLoader = new EditorTextureLoader();
```

Remove unused imports for `Texture`, `TextureAtlas`, `ObjectSet`, `SourceAsset`, `SourceRegion`, `AssetType`, `Rect`, and `ProjectSerializer` if the compiler flags them.

- [ ] **Step 2: Replace File menu items**

Replace the `File` menu body in `renderMenuBar()` with:

```java
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
```

- [ ] **Step 3: Add directory chooser methods**

Add methods:

```java
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
```

- [ ] **Step 4: Add open workflow methods**

Add:

```java
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
```

- [ ] **Step 5: Add open/save collision JSON methods**

Add:

```java
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
                    EditorProjectContext contextBeforeSave = stateProvider.getState().getProjectContext();
                    if (contextBeforeSave != null) {
                        stateProvider.getState().setProjectContext(
                                projectService.withCollisionFile(contextBeforeSave, saveFile));
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
```

- [ ] **Step 6: Remove old project file methods**

Remove methods that are replaced by the new workflow:

- `openProjectDialog()`
- `loadProject(File file)`
- `saveAsDialog()`
- `saveProject(String path)`
- `exportCollisionDialog()`

Also remove the `currentFilePath` field.

- [ ] **Step 7: Compile**

Run: `.\gradlew.bat :editor:compileJava`

Expected: PASS. Fix only compiler errors introduced in this task, such as stale imports.

- [ ] **Step 8: Run editor tests**

Run: `.\gradlew.bat :editor:test`

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/panels/PanelManager.java
git commit -m "feat(editor): open libgdx asset projects"
```

---

### Task 6: Assets Panel Uses Scanned Project Assets

**Files:**
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagesPanel.java`

- [ ] **Step 1: Add project status rendering**

Add imports:

```java
import com.voidvvv.edit2d.editor.project.AssetIndex;
import com.voidvvv.edit2d.editor.project.AssetRecord;
import com.voidvvv.edit2d.editor.project.AssetRegionRecord;
import com.voidvvv.edit2d.editor.project.EditorProjectContext;
import com.voidvvv.edit2d.editor.project.ResourceRecoveryReport;
import com.voidvvv.edit2d.editor.project.ResourceStatus;
```

At the top of `render()`, change the window title:

```java
if (ImGui.begin("Assets")) {
    renderProjectStatus();
    ImGui.separator();
    renderAssetList();
}
```

Add:

```java
private void renderProjectStatus() {
    EditorState state = stateProvider.getState();
    EditorProjectContext context = state.getProjectContext();
    if (context == null) {
        ImGui.textDisabled("Open a libGDX project or assets folder to scan resources.");
        return;
    }
    ImGui.text("Assets: " + context.getAssetsRoot().getAbsolutePath());
    ImGui.text("Collision: " + context.getCollisionFile().getAbsolutePath());
    ResourceRecoveryReport report = state.getRecoveryReport();
    if (report != null) {
        ImGui.textDisabled("matched " + report.getMatchedCount()
                + " / repaired " + report.getRepairedCount()
                + " / missing " + report.getMissingCount()
                + " / conflicts " + report.getConflictCount());
    }
}
```

- [ ] **Step 2: Populate project source assets from index when no JSON exists**

Add helper:

```java
private void ensureProjectHasScannedAssets() {
    EditorState state = stateProvider.getState();
    AssetIndex index = state.getAssetIndex();
    if (index == null || !state.getProject().getSourceAssets().isEmpty()) {
        return;
    }
    for (AssetRecord record : index.getRecords()) {
        SourceAsset asset = new SourceAsset();
        asset.setType(record.getType());
        asset.setInternalPath(record.getInternalPath());
        for (AssetRegionRecord region : record.getRegions()) {
            asset.getRegions().add(new SourceRegion(region.getName(), asset.getId(), region.getBounds()));
        }
        state.getProject().getSourceAssets().add(asset);
    }
}
```

Call this immediately before `renderAssetList()`:

```java
ensureProjectHasScannedAssets();
renderAssetList();
```

- [ ] **Step 3: Show per-asset recovery labels**

Add:

```java
private String statusSuffix(SourceAsset asset) {
    ResourceRecoveryReport report = stateProvider.getState().getRecoveryReport();
    if (report == null) {
        return "";
    }
    ResourceStatus status = report.getStatus(asset.getId());
    if (status == ResourceStatus.REPAIRED) {
        return " [repaired]";
    }
    if (status == ResourceStatus.MISSING) {
        return " [missing]";
    }
    if (status == ResourceStatus.CONFLICT) {
        return " [needs binding]";
    }
    return "";
}
```

Update labels:

```java
if (ImGui.selectable(region.getName() + statusSuffix(asset), selected)) {
```

and:

```java
if (ImGui.selectable(getDisplayName(asset) + statusSuffix(asset), selected)) {
```

- [ ] **Step 4: Convert manual import to assets-relative paths when possible**

In `openFileChooser()`, after selecting each file, replace:

```java
String absolutePath = file.getAbsolutePath();
```

with:

```java
String internalPath = toInternalPath(file);
```

Use `internalPath` when calling `asset.setInternalPath(...)`.

Add helper:

```java
private String toInternalPath(File file) {
    EditorProjectContext context = stateProvider.getState().getProjectContext();
    if (context == null) {
        return file.getAbsolutePath();
    }
    try {
        return context.getAssetsRoot().getCanonicalFile().toPath()
                .relativize(file.getCanonicalFile().toPath()).toString().replace('\\', '/');
    } catch (Exception e) {
        return file.getAbsolutePath();
    }
}
```

- [ ] **Step 5: Compile and run tests**

Run: `.\gradlew.bat :editor:compileJava`

Expected: PASS.

Run: `.\gradlew.bat :editor:test`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagesPanel.java
git commit -m "feat(editor): show scanned project assets"
```

---

### Task 7: Missing Resource Preview Safety

**Files:**
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagePreviewPanel.java`
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SpriteFramePreviewPanel.java`
- Modify: `editor/src/main/java/com/voidvvv/kzcollision/editor/viewport/ViewportRenderer.java`

- [ ] **Step 1: Improve source preview missing message**

In `SourceImagePreviewPanel.render()`, replace:

```java
ImGui.textDisabled("Texture not loaded");
```

with:

```java
ImGui.textDisabled("Texture not loaded: " + asset.getInternalPath());
```

- [ ] **Step 2: Improve sprite frame preview missing message**

In `SpriteFramePreviewPanel.render()`, when `resolveTexture(...)` returns null, show:

```java
ImGui.textDisabled("Texture not loaded for selected sprite frame");
```

If the file already has an equivalent text, update it to include the frame source path by resolving `SourceAsset asset = state.getProject().findSourceAsset(frame.getSourceAssetId())`.

- [ ] **Step 3: Make viewport texture lookup cache-only**

In `ViewportRenderer`, replace the `getTexture(String sourceAssetId)` method with a cache-only lookup. This prevents the viewport from falling back to `Gdx.files.internal(key)`, which would incorrectly read from the editor working directory instead of the opened target `assetsRoot`.

```java
private Texture getTexture(String sourceAssetId) {
    Map<String, Texture> cache = state.getTextureCache();
    SourceAsset asset = state.getProject().findSourceAsset(sourceAssetId);
    if (asset == null || asset.getInternalPath() == null) {
        return null;
    }
    return cache.get(asset.getInternalPath());
}
```

The existing render method already skips sprite drawing when `getTexture(...)` returns null and still draws collision boxes, resize handles, and origin markers for the current animation frame. Keep that behavior.

- [ ] **Step 4: Compile and run tests**

Run: `.\gradlew.bat :editor:compileJava`

Expected: PASS.

Run: `.\gradlew.bat :editor:test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagePreviewPanel.java \
        editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SpriteFramePreviewPanel.java \
        editor/src/main/java/com/voidvvv/kzcollision/editor/viewport/ViewportRenderer.java
git commit -m "fix(editor): tolerate unresolved project resources"
```

---

### Task 8: End-to-End Verification

**Files:**
- No new files unless bugs are found during verification.

- [ ] **Step 1: Run all tests**

Run: `.\gradlew.bat test`

Expected: PASS for all included modules.

- [ ] **Step 2: Run editor compile**

Run: `.\gradlew.bat :editor:compileJava`

Expected: PASS.

- [ ] **Step 3: Manual smoke test with a temporary libGDX-like assets folder**

Create this folder structure outside git or under a temp directory:

```text
temp-project/
  assets/
    sprites/
      hero.png
    collision/
```

Run editor:

```bash
.\gradlew.bat :editor:run
```

Manual expected behavior:

- `File > Open libGDX Project...` accepts `temp-project`.
- `Assets` panel shows `hero.png`.
- `Save Collision JSON` creates `temp-project/assets/collision/collision.json`.

- [ ] **Step 4: Manual smoke test with moved resource repair**

Edit `collision.json` so a source asset points to `old/hero.png`, then keep only `sprites/hero.png` in assets.

Manual expected behavior:

- Reopening the project marks the resource as repaired.
- Saved JSON writes `sprites/hero.png` as `internalPath`.

- [ ] **Step 5: Manual smoke test with duplicate candidate conflict**

Create:

```text
assets/a/hero.png
assets/b/hero.png
```

Use JSON with `old/hero.png`.

Manual expected behavior:

- Reopening the project shows `needs binding`.
- The JSON data is not deleted.
- `internalPath` remains `old/hero.png` until a manual binding UI exists.

- [ ] **Step 6: Final status check**

Run: `git status --short`

Expected: only intentional files are modified.

Do not stage `.omc` files or `build/` outputs unless the user explicitly requests them.
