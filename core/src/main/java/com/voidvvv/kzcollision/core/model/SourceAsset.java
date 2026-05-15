package com.voidvvv.kzcollision.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SourceAsset {
    private String id;
    private AssetType type;
    private String filePath;
    private String internalPath;
    private String atlasFilePath;
    private List<SourceRegion> regions;

    public SourceAsset() {
        this.id = UUID.randomUUID().toString();
        this.regions = new ArrayList<>();
    }

    public SourceAsset(String filePath) {
        this();
        this.type = AssetType.SINGLE;
        this.filePath = filePath;
        this.regions.add(new SourceRegion(filePath, this.id, null));
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public AssetType getType() { return type; }
    public void setType(AssetType type) { this.type = type; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getInternalPath() { return internalPath; }
    public void setInternalPath(String internalPath) { this.internalPath = internalPath; }
    public String getAtlasFilePath() { return atlasFilePath; }
    public void setAtlasFilePath(String atlasFilePath) { this.atlasFilePath = atlasFilePath; }
    public List<SourceRegion> getRegions() { return regions; }
    public void setRegions(List<SourceRegion> regions) { this.regions = regions; }
}
