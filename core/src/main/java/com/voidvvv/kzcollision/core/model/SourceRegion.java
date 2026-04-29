package com.voidvvv.kzcollision.core.model;

import java.util.UUID;

public class SourceRegion {
    private String id;
    private String name;
    private String assetId;
    private Rect bounds;

    public SourceRegion() {
        this.id = UUID.randomUUID().toString();
    }

    public SourceRegion(String name, String assetId, Rect bounds) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.assetId = assetId;
        this.bounds = bounds;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public Rect getBounds() { return bounds; }
    public void setBounds(Rect bounds) { this.bounds = bounds; }
}
