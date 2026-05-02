# Preview Panels & Split Frame Naming Design

Date: 2026-05-02

## Overview

Three related enhancements to the KZCollision Editor:

1. **SpriteFrame Preview Panel** — a new ImGui panel that shows a preview of the currently selected SpriteFrame
2. **Source Image Preview Panel** — a new ImGui panel that shows a preview of the currently selected imported source image
3. **Split Frame Naming** — add sequential numbering to SpriteFrames created by the split operation, ordered from top-left to bottom-right

## 1. Split Frame Naming

### Current Behavior

`SourceImagesPanel.performSplit()` creates `rows * cols` SpriteFrames, all with identical `sourceRegionName` (the parent SourceRegion's name). In the Sprite Frames panel, every split frame displays the same label (e.g., "naruduo_anim.png [split]").

### New Behavior

- Each SpriteFrame created by split receives a numbered name: `{region name}_{NNN}`
- Sequence number format: three-digit zero-padded, starting from `001`
- Ordering: left-to-right, top-to-bottom by row (row 0 col 0 = 001, row 0 col 1 = 002, ..., last cell = `rows*cols`)
- Example: 6x6 split of "naruduo_anim.png" produces "naruduo_anim.png_001" through "naruduo_anim.png_036"

### Files Modified

- `SourceImagesPanel.java` — update `performSplit()` to generate numbered names
- `SpriteFramesPanel.java` — update `buildLabel()` to drop the `[split]` suffix since the name now provides sufficient differentiation

### Name Generation Logic

```
for each row r from 0 to rows-1:
    for each col c from 0 to cols-1:
        index = r * cols + c + 1
        name = regionName + "_" + String.format("%03d", index)
        create SpriteFrame with sourceRegionName = name
```

## 2. SpriteFrame Preview Panel

### New Class

`SpriteFramePreviewPanel` in `editor/src/.../panels/`

### Behavior

- Monitors `EditorState.selectedSpriteFrameId`
- When a SpriteFrame is selected and has a loadable Texture:
  - Renders the frame image in the ImGui window using `ImGui.image()` with the OpenGL texture ID
  - If the frame has a `subRegion`, uses UV coordinates to render only the cropped area
  - Auto-scales to fit the panel size while maintaining aspect ratio
- Below the image, displays frame info: name and dimensions (width x height)
- When no frame is selected, shows placeholder text

### Selection Mechanism

- `SpriteFramesPanel` sets `EditorState.selectedSpriteFrameId` on list item click
- The preview panel reads this value each frame

### Rendering Details

- Convert libGDX Texture to ImGui-renderable format via texture ID
- For subRegion frames, compute UV0 and UV1 from the sub-region Rect relative to the full texture dimensions
- For full frames, use UV (0,0) to (1,1)

## 3. Source Image Preview Panel

### New Class

`SourceImagePreviewPanel` in `editor/src/.../panels/`

### Behavior

- Monitors `EditorState.selectedSourceAssetId`
- When a SourceAsset is selected and has a cached Texture:
  - Renders the full source image in the ImGui window
  - Auto-scales to fit the panel size while maintaining aspect ratio
- Below the image, displays asset info: file name and original image dimensions
- When no asset is selected, shows placeholder text

### Selection Mechanism

- `SourceImagesPanel` sets `EditorState.selectedSourceAssetId` on list item click
- The preview panel reads this value each frame

## 4. Layout & Integration

### EditorState Changes

Add two new nullable String fields:
- `selectedSpriteFrameId` — ID of the currently selected SpriteFrame
- `selectedSourceAssetId` — ID of the currently selected SourceAsset

### PanelManager Layout

Current bottom area: only AnimationControlsPanel (820x40).

New bottom area (left to right):
1. Source Image Preview Panel (~300x200)
2. SpriteFrame Preview Panel (~300x200)
3. Animation Controls Panel (fills remaining width, ~40px height)

All panels remain draggable and resizable (inherent ImGui behavior).

### Files Modified

| File | Change |
|------|--------|
| `EditorState.java` | Add `selectedSpriteFrameId`, `selectedSourceAssetId` fields with getters/setters |
| `PanelManager.java` | Instantiate and register two new preview panels, adjust bottom layout |
| `SourceImagesPanel.java` | Add click selection logic for source assets |
| `SpriteFramesPanel.java` | Add click selection logic for sprite frames, update `buildLabel()` |
| New: `SpriteFramePreviewPanel.java` | SpriteFrame preview rendering |
| New: `SourceImagePreviewPanel.java` | Source image preview rendering |

## Scope

This design covers only the three features described. No changes to:
- Viewport rendering
- Collision box editing
- Animation playback
- Project save/load format (SpriteFrame names are already serialized)
