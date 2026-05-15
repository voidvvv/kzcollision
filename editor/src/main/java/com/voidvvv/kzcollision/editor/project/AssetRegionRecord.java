package com.voidvvv.kzcollision.editor.project;

import com.voidvvv.kzcollision.core.model.Rect;

import java.util.Objects;

public class AssetRegionRecord {
    private final String name;
    private final Rect bounds;

    public AssetRegionRecord(String name, Rect bounds) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.bounds = bounds; // may be null for single images
    }

    public String getName() { return name; }
    public Rect getBounds() { return bounds; }
}
