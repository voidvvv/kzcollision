package com.voidvvv.kzcollision.editor.project;

import com.voidvvv.kzcollision.core.model.AssetType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AssetRecord {
    private final AssetType type;
    private final String internalPath;
    private final File file;
    private final List<AssetRegionRecord> regions = new ArrayList<>();

    public AssetRecord(AssetType type, String internalPath, File file) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.internalPath = Objects.requireNonNull(internalPath, "internalPath must not be null");
        this.file = Objects.requireNonNull(file, "file must not be null");
    }

    public AssetType getType() { return type; }
    public String getInternalPath() { return internalPath; }
    public File getFile() { return file; }

    public List<AssetRegionRecord> getRegions() {
        return Collections.unmodifiableList(regions);
    }

    void addRegion(AssetRegionRecord region) {
        regions.add(region);
    }

    AssetRegionRecord findRegion(String name) {
        if (name == null) return null;
        for (AssetRegionRecord region : regions) {
            if (name.equals(region.getName())) {
                return region;
            }
        }
        return null;
    }
}
