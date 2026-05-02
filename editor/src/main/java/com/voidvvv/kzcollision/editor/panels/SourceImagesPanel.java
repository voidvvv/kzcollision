package com.voidvvv.kzcollision.editor.panels;

import com.voidvvv.kzcollision.core.AtlasParser;
import com.voidvvv.kzcollision.core.model.AtlasRegionDescriptor;
import com.voidvvv.kzcollision.core.model.AssetType;
import com.voidvvv.kzcollision.core.model.Project;
import com.voidvvv.kzcollision.core.model.Rect;
import com.voidvvv.kzcollision.core.model.SourceAsset;
import com.voidvvv.kzcollision.core.model.SourceRegion;
import com.voidvvv.kzcollision.core.model.SpriteFrame;
import com.voidvvv.kzcollision.editor.EditorState;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import imgui.ImGui;
import imgui.type.ImInt;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SourceImagesPanel {

    private final EditorStateProvider stateProvider;

    // Split popup state
    private boolean showSplitPopup;
    private String splitRegionId;
    private final ImInt splitRows = new ImInt(2);
    private final ImInt splitCols = new ImInt(2);
    private boolean splitNeedsOpen;

    // Buffer for pending file imports (produced on Swing EDT, consumed on render thread)
    private final List<SourceAsset> pendingImports = new ArrayList<>();

    public SourceImagesPanel(EditorStateProvider stateProvider) {
        this.stateProvider = stateProvider;
    }

    public void render() {
        // Process any assets imported from the Swing file chooser
        processPendingImports();

        if (ImGui.begin("Source Images")) {
            renderImportButton();
            ImGui.separator();
            renderAssetList();
        }
        ImGui.end();

        if (showSplitPopup) {
            renderSplitPopup();
        }
    }

    private void renderImportButton() {
        if (ImGui.button("+ Import")) {
            openFileChooser();
        }
    }

    private void renderAssetList() {
        EditorState state = stateProvider.getState();
        Project project = state.getProject();

        for (SourceAsset asset : project.getSourceAssets()) {
            if (asset.getType() == AssetType.ATLAS) {
                renderAtlasAsset(asset);
            } else {
                renderSingleAsset(asset);
            }
        }
    }

    private String getDisplayName(SourceAsset asset) {
        String path = asset.getFilePath();
        int lastSep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSep >= 0 ? path.substring(lastSep + 1) : path;
    }

    private void renderAtlasAsset(SourceAsset asset) {
        if (ImGui.collapsingHeader(getDisplayName(asset) + " (ATLAS)")) {
            for (SourceRegion region : asset.getRegions()) {
                if (ImGui.selectable("  " + region.getName())) {
                    stateProvider.getState().setSelectedSourceAssetId(asset.getId());
                }
                renderRegionContextMenu(region);
            }
        }
    }

    private void renderSingleAsset(SourceAsset asset) {
        if (ImGui.selectable(getDisplayName(asset))) {
            stateProvider.getState().setSelectedSourceAssetId(asset.getId());
        }
        if (!asset.getRegions().isEmpty()) {
            renderRegionContextMenu(asset.getRegions().get(0));
        }
    }

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
                splitNeedsOpen = true;
            }
            ImGui.endPopup();
        }
    }

    private void addAsFrame(SourceRegion region) {
        SpriteFrame frame = new SpriteFrame();
        frame.setSourceAssetId(region.getAssetId());
        frame.setSourceRegionName(region.getName());
        stateProvider.getState().getProject().getSpriteFrames().add(frame);
    }

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

    private SourceRegion findRegionById(String regionId) {
        if (regionId == null) {
            return null;
        }
        Project project = stateProvider.getState().getProject();
        for (SourceAsset asset : project.getSourceAssets()) {
            for (SourceRegion region : asset.getRegions()) {
                if (region.getId().equals(regionId)) {
                    return region;
                }
            }
        }
        return null;
    }

    private void openFileChooser() {
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter(
                "Image Files (*.png, *.jpg) & Atlas (*.atlas)", "png", "jpg", "jpeg", "atlas"));
            chooser.setMultiSelectionEnabled(true);

            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                List<SourceAsset> imported = new ArrayList<>();
                for (File file : chooser.getSelectedFiles()) {
                    String absolutePath = file.getAbsolutePath();
                    String fileName = file.getName();
                    SourceAsset asset;
                    if (fileName.endsWith(".atlas")) {
                        // Resolve the PNG by convention: same directory, same basename
                        String pngPath = absolutePath.replaceAll("\\.atlas$", ".png");
                        asset = new SourceAsset();
                        asset.setFilePath(pngPath);
                        asset.setAtlasFilePath(absolutePath);
                        asset.setType(AssetType.ATLAS);
                    } else {
                        // SINGLE image — store absolute path for loading, name for display
                        asset = new SourceAsset();
                        asset.setType(AssetType.SINGLE);
                        asset.setFilePath(absolutePath);
                        SourceRegion region = new SourceRegion(fileName, asset.getId(), null);
                        asset.getRegions().add(region);
                    }
                    imported.add(asset);
                }

                // Thread-safe handoff to render thread
                synchronized (pendingImports) {
                    pendingImports.addAll(imported);
                }
            }
        });
    }

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
            }
        }
    }
}
