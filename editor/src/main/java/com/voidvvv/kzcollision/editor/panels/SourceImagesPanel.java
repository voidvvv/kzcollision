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

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
        String baseName = region.getName();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float subX = bounds.x + c * tileWidth;
                float subY = bounds.y + r * tileHeight;
                Rect subRegion = new Rect(subX, subY, tileWidth, tileHeight);

                SpriteFrame frame = new SpriteFrame();
                frame.setSourceAssetId(region.getAssetId());
                frame.setSourceRegionName(baseName + "_r" + r + "_c" + c);
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
                        asset = importAtlasFile(file);
                    } else {
                        asset = importSingleImage(file);
                    }
                    if (asset != null) {
                        imported.add(asset);
                    }
                }

                // Thread-safe handoff to render thread
                synchronized (pendingImports) {
                    pendingImports.addAll(imported);
                }
            }
        });
    }

    /**
     * Import a single image file. Reads the image dimensions and stores them
     * as the SourceRegion bounds so that grid split can work correctly.
     */
    private SourceAsset importSingleImage(File imageFile) {
        String fileName = imageFile.getName();
        SourceAsset asset = new SourceAsset(fileName);
        // Try to read image dimensions for the region bounds
        Rect bounds = readImageDimensions(imageFile);
        if (bounds != null) {
            SourceRegion region = asset.getRegions().get(0);
            region.setBounds(bounds);
        }
        return asset;
    }

    /**
     * Read image dimensions using ImageIO. Returns a Rect with (0, 0, width, height)
     * or null if the image cannot be read.
     */
    private Rect readImageDimensions(File imageFile) {
        try {
            BufferedImage img = ImageIO.read(imageFile);
            if (img != null) {
                return new Rect(0, 0, img.getWidth(), img.getHeight());
            }
        } catch (IOException e) {
            System.err.println("Failed to read image dimensions: " + imageFile.getAbsolutePath());
        }
        return null;
    }

    /**
     * Import a .atlas file by parsing its text format and extracting regions.
     * The .atlas format structure:
     * - First section: image filename, then atlas metadata (size, format, filter, repeat)
     * - Subsequent sections: region name, then region metadata (rotate, xy, size, orig, offset, index)
     */
    private SourceAsset importAtlasFile(File atlasFile) {
        String atlasFileName = atlasFile.getName();
        String imageFileName = null;
        List<SourceRegion> regions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(atlasFile))) {
            String line;
            boolean inHeader = true;
            String currentRegionName = null;
            int regionX = 0;
            int regionY = 0;
            int regionW = 0;
            int regionH = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    // End of a section — flush the current region if any
                    if (currentRegionName != null) {
                        // We'll set assetId below after creating the asset
                        regions.add(createTempRegion(currentRegionName, regionX, regionY, regionW, regionH));
                        currentRegionName = null;
                    }
                    inHeader = false;
                    continue;
                }

                if (inHeader) {
                    if (imageFileName == null) {
                        // First non-empty line is the image file name
                        imageFileName = line;
                    }
                    // Skip other header fields (size, format, filter, repeat)
                } else {
                    // Region section
                    if (!line.contains(":")) {
                        // This is a region name (no colon separator)
                        currentRegionName = line;
                    } else if (line.startsWith("xy:")) {
                        String[] parts = line.substring(3).trim().split(",\\s*");
                        if (parts.length >= 2) {
                            regionX = Integer.parseInt(parts[0].trim());
                            regionY = Integer.parseInt(parts[1].trim());
                        }
                    } else if (line.startsWith("size:")) {
                        String[] parts = line.substring(5).trim().split(",\\s*");
                        if (parts.length >= 2) {
                            regionW = Integer.parseInt(parts[0].trim());
                            regionH = Integer.parseInt(parts[1].trim());
                        }
                    }
                    // Skip rotate, orig, offset, index — not needed for bounds
                }
            }

            // Flush the last region if the file doesn't end with an empty line
            if (currentRegionName != null) {
                regions.add(createTempRegion(currentRegionName, regionX, regionY, regionW, regionH));
            }
        } catch (IOException e) {
            System.err.println("Failed to parse atlas file: " + atlasFile.getAbsolutePath());
            return null;
        }

        // Build the SourceAsset
        SourceAsset asset = new SourceAsset();
        asset.setType(AssetType.ATLAS);
        asset.setFilePath(imageFileName != null ? imageFileName : atlasFileName);
        asset.setAtlasFilePath(atlasFile.getAbsolutePath());

        // Assign assetId to all parsed regions
        for (SourceRegion region : regions) {
            region.setAssetId(asset.getId());
            asset.getRegions().add(region);
        }

        if (asset.getRegions().isEmpty()) {
            System.err.println("Atlas file contained no regions: " + atlasFile.getAbsolutePath());
        }

        return asset;
    }

    /**
     * Temporary region holder used during atlas parsing before the asset ID is known.
     * The assetId will be set after the SourceAsset is created.
     */
    private SourceRegion createTempRegion(String name, int x, int y, int w, int h) {
        return new SourceRegion(name, null, new Rect(x, y, w, h));
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
