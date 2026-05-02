# Atlas File Import Design

## Problem

The editor recognizes `.atlas` files in the file chooser but does not parse them. Importing an atlas creates a single empty `SourceRegion` with no bounds and no loaded texture. The expected behavior is: import an atlas file, display all parsed image regions in the source panel, and allow each region to be independently added as a sprite frame.

## Approach

Dedicated `AtlasParser` utility class in the `core` module. Clean separation between parsing logic and UI import flow. Each atlas region becomes a `SourceRegion` under the `SourceAsset`, fully consistent with the existing single-image workflow.

## Design

### 1. AtlasRegionDescriptor (core module)

New data class in `com.voidvvv.kzcollision.core.model`:

```
AtlasRegionDescriptor {
    name: String
    index: int (-1 if not indexed)
    x, y: int          // position in atlas texture
    width, height: int  // size in atlas texture
    origWidth, origHeight: int  // original size before cropping
    offsetX, offsetY: int       // offset from original
    rotate: boolean     // stored but not used in v1
}
```

### 2. AtlasParser (core module)

New utility class in `com.voidvvv.kzcollision.core`:

- Input: `File` or `Path` pointing to a `.atlas` text file
- Output: `List<AtlasRegionDescriptor>`
- Parses the standard libGDX TextureAtlas text format:
  - First line: PNG path (ignored for lookup; we use convention-based resolution)
  - Region entries: name line followed by `rotate: true/false`, `xy: x, y`, `size: width, height`, `orig: width, height`, `offset: x, y`, `index: N`
- No libGDX dependency — pure Java file I/O and text parsing
- Throws `IOException` on file errors, `IllegalArgumentException` on malformed input

### 3. Import Flow Changes (editor module, SourceImagesPanel)

In `processPendingImports()`, add an `ATLAS` branch parallel to the existing `SINGLE` branch:

**Texture loading:**
- Resolve the associated PNG by convention: same directory, same basename as the atlas file (e.g., `hero.atlas` -> `hero.png`)
- Load via `new Texture(Gdx.files.absolute(resolvedPngPath))`
- Cache in `textureCache` keyed by the atlas file path (the `SourceAsset.filePath`)

**Region creation:**
- Call `AtlasParser.parse(atlasFile)` to get region descriptors
- For each descriptor, create a `SourceRegion` with:
  - `name`: descriptor name (with index suffix if indexed, e.g., `"region_2"`)
  - `assetId`: the SourceAsset's ID
  - `bounds`: `Rect(descriptor.x, descriptor.y, descriptor.width, descriptor.height)`
- Add all regions to the `SourceAsset.regions` list

**Error handling:**
- If the PNG is not found, log an error and skip the asset (do not add to project)
- If the atlas file is malformed, log an error and skip the asset
- In both cases, show an error message via ImGui toast or similar notification

### 4. Display (no changes needed)

The existing `SourceImagesPanel` already handles atlas display:
- Atlas assets render as a collapsible header with "(ATLAS)" suffix
- When expanded, iterates `asset.getRegions()` and renders each as a selectable item
- Right-click context menu ("Add as Frame", "Split m x n") works on any SourceRegion regardless of parent asset type

### 5. Rendering and Preview (minimal changes)

**SourceImagePreviewPanel:**
- When a SourceRegion from an atlas is selected, compute UV coordinates from `region.bounds` relative to the atlas texture dimensions
- Render the cropped region in the preview panel
- This follows the same UV-based rendering pattern already used by `SpriteFramePreviewPanel`

**SpriteFramePreviewPanel and ViewportRenderer:**
- Already support sub-region rendering via UV coordinates
- When a sprite frame is created from an atlas region (via "Add as Frame"), `sourceRegionName` matches the atlas region name
- `ViewportRenderer.findSourceRegion()` searches by name — works without changes
- No changes needed for rendering

### 6. Rotation (deferred)

Atlas regions with `rotate: true` are parsed and stored in `AtlasRegionDescriptor.rotate` but ignored during rendering in v1. Rotated regions will render incorrectly. Full rotation support can be added later by adjusting UV coordinates when `rotate` is true.

## Scope

- In scope: parse libGDX atlas format, create SourceRegions, load texture, display regions, enable "Add as Frame"
- Out of scope: multi-page atlases, rotated region rendering, atlas region editing, drag-and-drop import
