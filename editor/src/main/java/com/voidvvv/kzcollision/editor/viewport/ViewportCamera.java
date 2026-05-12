package com.voidvvv.kzcollision.editor.viewport;

public class ViewportCamera {
    private float offsetX = 0;
    private float offsetY = 0;
    private float zoom = 2.0f;

    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }
    public float getZoom() { return zoom; }

    public void pan(float dx, float dy) {
        offsetX += dx;
        offsetY += dy;
    }

    public void zoom(float amount, float screenX, float screenY, float viewportWidth, float viewportHeight) {
        float oldZoom = zoom;
        zoom *= (amount > 0) ? 0.9f : 1.1f;
        zoom = Math.max(0.1f, Math.min(20f, zoom));
        float zoomRatio = zoom / oldZoom;
        float cx = screenX - viewportWidth / 2f;
        float cy = screenY - viewportHeight / 2f;
        offsetX = cx - (cx - offsetX) * zoomRatio;
        offsetY = cy - (cy - offsetY) * zoomRatio;
    }

    public float screenToWorldX(float screenX, float viewportWidth) {
        return (screenX - viewportWidth / 2f - offsetX) / zoom;
    }

    public float screenToWorldY(float screenY, float viewportHeight) {
        return (screenY - viewportHeight / 2f - offsetY) / zoom;
    }

    public void reset() {
        offsetX = 0;
        offsetY = 0;
        zoom = 2.0f;
    }
}
