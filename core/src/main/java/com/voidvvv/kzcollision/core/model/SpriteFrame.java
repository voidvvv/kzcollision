package com.voidvvv.kzcollision.core.model;

import java.util.UUID;

public class SpriteFrame {
    private String id;
    private String sourceAssetId;
    private String sourceRegionName;
    private Rect subRegion;
    private float offsetX;
    private float offsetY;

    public SpriteFrame() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSourceAssetId() { return sourceAssetId; }
    public void setSourceAssetId(String sourceAssetId) { this.sourceAssetId = sourceAssetId; }
    public String getSourceRegionName() { return sourceRegionName; }
    public void setSourceRegionName(String sourceRegionName) { this.sourceRegionName = sourceRegionName; }
    public Rect getSubRegion() { return subRegion; }
    public void setSubRegion(Rect subRegion) { this.subRegion = subRegion; }
    public float getOffsetX() { return offsetX; }
    public void setOffsetX(float offsetX) { this.offsetX = offsetX; }
    public float getOffsetY() { return offsetY; }
    public void setOffsetY(float offsetY) { this.offsetY = offsetY; }
}
