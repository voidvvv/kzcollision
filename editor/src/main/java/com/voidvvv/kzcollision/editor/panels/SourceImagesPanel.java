package com.voidvvv.kzcollision.editor.panels;

import com.voidvvv.kzcollision.core.model.AssetType;
import com.voidvvv.kzcollision.core.model.Project;
import com.voidvvv.kzcollision.core.model.Rect;
import com.voidvvv.kzcollision.core.model.SourceAsset;
import com.voidvvv.kzcollision.core.model.SourceRegion;
import com.voidvvv.kzcollision.core.model.SpriteFrame;
import com.voidvvv.kzcollision.editor.EditorState;
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

    private void renderAtlasAsset(SourceAsset asset) {
        if (ImGui.collapsingHeader(asset.getFilePath() + " (ATLAS)")) {
            for (SourceRegion region : asset.getRegions()) {
                if (ImGui.selectable("  " + region.getName())) {
                    // Selection handled via context menu
                }
                renderRegionContextMenu(region);
            }
        }
    }

    private void renderSingleAsset(SourceAsset asset) {
        if (ImGui.selectable(asset.getFilePath())) {
            // Selection handled via context menu
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
                ImGui.openPopup("Split##" + region.getId());
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
        if (ImGui.beginPopupModal(popupId)) {
            ImGui.text("Split region into grid");
            ImGui.inputInt("Rows", splitRows);
            ImGui.inputInt("Cols", splitCols);

            // Clamp to valid range
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
        // If no bounds are set, we cannot compute sub-regions
        if (bounds == null) {
            return;
        }

        float tileWidth = bounds.width / cols;
        float tileHeight = bounds.height / rows;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float subX = bounds.x + c * tileWidth;
                float subY = bounds.y + r * tileHeight;
                Rect subRegion = new Rect(subX, subY, tileWidth, tileHeight);

                SpriteFrame frame = new SpriteFrame();
                frame.setSourceAssetId(region.getAssetId());
                frame.setSourceRegionName(region.getName());
                frame.setSubRegion(subRegion);
                stateProvider.getState().getProject().getSpriteFrames().add(frame);
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
                    String fileName = file.getName();
                    SourceAsset asset;
                    if (fileName.endsWith(".atlas")) {
                        asset = new SourceAsset();
                        asset.setFilePath(fileName);
                        asset.setType(AssetType.ATLAS);
                        // For now, create one region covering the whole image.
                        // Full atlas parsing comes in Task 17.
                        SourceRegion region = new SourceRegion(fileName, asset.getId(), null);
                        asset.getRegions().add(region);
                    } else {
                        // SINGLE image
                        asset = new SourceAsset(fileName);
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
            Project project = stateProvider.getState().getProject();
            project.getSourceAssets().addAll(toAdd);
        }
    }
}
