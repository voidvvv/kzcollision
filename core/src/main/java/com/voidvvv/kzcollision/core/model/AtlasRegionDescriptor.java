package com.voidvvv.kzcollision.core.model;

public class AtlasRegionDescriptor {
    private final String name;
    private final int index;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int origWidth;
    private final int origHeight;
    private final int offsetX;
    private final int offsetY;
    private final boolean rotate;

    public AtlasRegionDescriptor(String name, int index, int x, int y,
                                 int width, int height, int origWidth, int origHeight,
                                 int offsetX, int offsetY, boolean rotate) {
        this.name = name;
        this.index = index;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.origWidth = origWidth;
        this.origHeight = origHeight;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.rotate = rotate;
    }

    public String getName() { return name; }
    public int getIndex() { return index; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getOrigWidth() { return origWidth; }
    public int getOrigHeight() { return origHeight; }
    public int getOffsetX() { return offsetX; }
    public int getOffsetY() { return offsetY; }
    public boolean isRotate() { return rotate; }
}
