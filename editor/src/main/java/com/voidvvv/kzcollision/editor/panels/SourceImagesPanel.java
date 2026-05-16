package com.voidvvv.kzcollision.editor.panels;

import com.voidvvv.kzcollision.core.model.AssetType;
import com.voidvvv.kzcollision.core.model.Project;
import com.voidvvv.kzcollision.core.model.Rect;
import com.voidvvv.kzcollision.core.model.SourceAsset;
import com.voidvvv.kzcollision.core.model.SourceRegion;
import com.voidvvv.kzcollision.core.model.SpriteFrame;
import com.voidvvv.kzcollision.editor.EditorState;
import com.voidvvv.kzcollision.editor.project.AssetIndex;
import com.voidvvv.kzcollision.editor.project.AssetRecord;
import com.voidvvv.kzcollision.editor.project.AssetRegionRecord;
import com.voidvvv.kzcollision.editor.project.EditorProjectContext;
import com.voidvvv.kzcollision.editor.project.ResourceRecoveryReport;
import com.voidvvv.kzcollision.editor.project.ResourceStatus;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.ObjectSet;
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
    private boolean splitNeedsOpen;

    // Buffer for pending file imports (produced on Swing EDT, consumed on render thread)
    private final List<SourceAsset> pendingImports = new ArrayList<>();

    public SourceImagesPanel(EditorStateProvider stateProvider) {
        this.stateProvider = stateProvider;
    }

    public void render() {
        // Process any assets imported from the Swing file chooser
        processPendingImports();

        if (ImGui.begin("Assets")) {
            renderProjectStatus();
            ImGui.separator();
            renderImportButton();
            ImGui.separator();
            ensureProjectHasScannedAssets();
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

    private void renderAssetList() {
        EditorState state = stateProvider.getState();
        Project project = state.getProject();

        for (SourceAsset asset : project.getSourceAssets()) {
            if (asset.getType() == AssetType.ATLAS) {
                renderAtlasRegions(asset);
            } else {
                renderSingleAsset(asset);
            }
        }
    }

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

    private String getDisplayName(SourceAsset asset) {
        String path = asset.getInternalPath() != null ? asset.getInternalPath() : asset.getFilePath();
        if (path == null) return "unknown";
        int lastSep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSep >= 0 ? path.substring(lastSep + 1) : path;
    }

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

    private void renderAtlasRegions(SourceAsset asset) {
        for (SourceRegion region : asset.getRegions()) {
            boolean selected = region.getId().equals(stateProvider.getState().getSelectedSourceRegionId());
            if (ImGui.selectable(region.getName() + statusSuffix(asset), selected)) {
                stateProvider.getState().setSelectedSourceAssetId(asset.getId());
                stateProvider.getState().setSelectedSourceRegionId(region.getId());
            }
            renderRegionContextMenu(region);
        }
    }

    private void renderSingleAsset(SourceAsset asset) {
        boolean selected = asset.getId().equals(stateProvider.getState().getSelectedSourceAssetId());
        if (ImGui.selectable(getDisplayName(asset) + statusSuffix(asset), selected)) {
            stateProvider.getState().setSelectedSourceAssetId(asset.getId());
            if (!asset.getRegions().isEmpty()) {
                stateProvider.getState().setSelectedSourceRegionId(asset.getRegions().get(0).getId());
            }
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
        frame.setSubRegion(region.getBounds());
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
                        asset = new SourceAsset();
                        asset.setFilePath(absolutePath);
                        asset.setAtlasFilePath(absolutePath);
                        asset.setType(AssetType.ATLAS);
                        asset.setInternalPath(toInternalPath(file));
                    } else {
                        // SINGLE image — store absolute path for loading, name for display
                        asset = new SourceAsset();
                        asset.setType(AssetType.SINGLE);
                        asset.setFilePath(absolutePath);
                        asset.setInternalPath(toInternalPath(file));
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
                        String cacheKey = asset.getInternalPath() != null ? asset.getInternalPath() : asset.getFilePath();
                        state.getTextureCache().put(cacheKey, tex);
                        if (!asset.getRegions().isEmpty()) {
                            SourceRegion region = asset.getRegions().get(0);
                            region.setBounds(new Rect(0, 0, tex.getWidth(), tex.getHeight()));
                        }
                    } catch (Exception e) {
                        Gdx.app.log("SourceImagesPanel", "Failed to load texture: " + asset.getFilePath(), e);
                    }
                } else if (asset.getType() == AssetType.ATLAS) {
                    String atlasPath = asset.getAtlasFilePath();

                    TextureAtlas atlas;
                    try {
                        atlas = new TextureAtlas(Gdx.files.absolute(atlasPath));
                    } catch (Exception e) {
                        Gdx.app.log("SourceImagesPanel", "Failed to load atlas: " + atlasPath, e);
                        project.getSourceAssets().remove(asset);
                        continue;
                    }

                    ObjectSet<Texture> atlasTextures = atlas.getTextures();
                    if (atlasTextures.size > 0) {
                        String cacheKey = asset.getInternalPath() != null ? asset.getInternalPath() : asset.getFilePath();
                        state.getTextureCache().put(cacheKey, atlasTextures.first());
                    }

                    for (TextureAtlas.AtlasRegion region : atlas.getRegions()) {
                        String regionName = region.index >= 0
                                ? region.name + "_" + region.index
                                : region.name;
                        Rect bounds = new Rect(region.getRegionX(), region.getRegionY(),
                                region.getRegionWidth(), region.getRegionHeight());
                        SourceRegion srcRegion = new SourceRegion(regionName, asset.getId(), bounds);
                        asset.getRegions().add(srcRegion);
                    }
                }
            }
        }
    }
}
