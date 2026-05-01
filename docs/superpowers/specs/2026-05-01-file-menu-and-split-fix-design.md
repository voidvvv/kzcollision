# File Menu and Split Popup Fix Design

Date: 2026-05-01

## Problem

1. **File menu**: All 5 menu items (New Project, Open Project, Save Project, Save As, Export Collision JSON) render but have no click handlers. `ImGui.menuItem()` return values are ignored.
2. **Split popup**: Clicking "Split m x n..." in the source images context menu does nothing. `ImGui.openPopup()` is called inside the context popup (one ID stack context) but `ImGui.beginPopupModal()` is called outside it (different ID stack context), so the popup IDs never match.

## Design

### Fix 1: Split Popup (SourceImagesPanel.java)

Remove the `ImGui.openPopup()` call from inside the context menu handler. Instead, defer the open call to `renderSplitPopup()` where `beginPopupModal()` is also called, ensuring both use the same ID stack context.

**Changes to `renderRegionContextMenu`:**
- Delete `ImGui.openPopup("Split##" + region.getId())` line
- Add `splitNeedsOpen = true` flag when menu item is clicked

**Changes to `renderSplitPopup`:**
- Check `splitNeedsOpen` at the top; if true, call `ImGui.openPopup(popupId)` then set flag to false
- This ensures `openPopup` and `beginPopupModal` share the same ID stack context

**New field:** `boolean splitNeedsOpen` (default false)

### Fix 2: File Menu (PanelManager.java, EditorState.java)

Wire all menu items to real behavior using `ProjectSerializer` and `JFileChooser`.

**New state in PanelManager:**
- `String currentFilePath` — path of the currently opened project file (null = unsaved)
- `ProjectSerializer serializer` — reuse existing serializer from core module
- `List<Runnable> pendingFileActions` — thread-safe handoff from Swing EDT to render thread

**Menu item behaviors:**

| Menu Item | Behavior |
|-----------|----------|
| New Project | Create new `Project("Untitled")`, clear EditorState (texture cache, selection, playback), reset `currentFilePath` to null |
| Open Project... | Open JFileChooser for `.json` files, `serializer.load()` on selection, replace project in EditorState, reload textures for source assets, reset `currentFilePath` |
| Save Project | If `currentFilePath != null`, serialize directly; otherwise fall through to Save As behavior |
| Save As... | Open JFileChooser to pick save location, `serializer.save()`, update `currentFilePath` |
| Export Collision JSON | Open JFileChooser to pick save location, export a simplified JSON containing only animations/frames/collision boxes (no source asset file paths) |

**Texture reload on Open Project:**
- Iterate new project's `sourceAssets`
- For each SINGLE asset, attempt `new Texture(Gdx.files.absolute(path))`
- Log warning and skip on failure (source image may have moved)

**Thread safety:**
- Use same `pendingXxx` pattern as `SourceImagesPanel`: Swing EDT produces actions, render thread consumes
- File chooser runs on EDT via `SwingUtilities.invokeLater`
- Result data (loaded Project, save path) passed through synchronized list

**EditorState changes:**
- Add `reset()` method: sets new empty project, clears selection/playback state, clears texture cache
- No new fields needed (already has `setProject()`)
