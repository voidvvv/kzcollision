package com.voidvvv.kzcollision.editor;

import com.voidvvv.kzcollision.core.model.Animation;
import com.voidvvv.kzcollision.core.model.AnimationFrame;
import com.voidvvv.kzcollision.core.model.Project;

import com.badlogic.gdx.graphics.Texture;

import java.util.HashMap;
import java.util.Map;

public class EditorState {
    private Project project;
    private String selectedAnimationId;
    private int currentFrameIndex = 0;
    private String selectedCollisionBoxId;
    private boolean playing = false;
    private float playbackTimer = 0f;
    private transient Map<String, Texture> textureCache = new HashMap<>();

    public EditorState() {
        this.project = new Project("Untitled");
    }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public String getSelectedAnimationId() { return selectedAnimationId; }
    public void setSelectedAnimationId(String id) { this.selectedAnimationId = id; }
    public int getCurrentFrameIndex() { return currentFrameIndex; }
    public void setCurrentFrameIndex(int idx) { this.currentFrameIndex = idx; }
    public String getSelectedCollisionBoxId() { return selectedCollisionBoxId; }
    public void setSelectedCollisionBoxId(String id) { this.selectedCollisionBoxId = id; }
    public boolean isPlaying() { return playing; }
    public void setPlaying(boolean playing) { this.playing = playing; }
    public float getPlaybackTimer() { return playbackTimer; }
    public void setPlaybackTimer(float timer) { this.playbackTimer = timer; }

    public Animation getSelectedAnimation() {
        if (selectedAnimationId == null) return null;
        return project.findAnimation(selectedAnimationId);
    }

    public AnimationFrame getCurrentFrame() {
        Animation anim = getSelectedAnimation();
        if (anim == null || anim.getFrames().isEmpty()) return null;
        int idx = Math.min(currentFrameIndex, anim.getFrames().size() - 1);
        return anim.getFrames().get(idx);
    }

    public Map<String, Texture> getTextureCache() { return textureCache; }

    public void reset() {
        this.project = new Project("Untitled");
        this.selectedAnimationId = null;
        this.currentFrameIndex = 0;
        this.selectedCollisionBoxId = null;
        this.playing = false;
        this.playbackTimer = 0f;
        for (Texture tex : textureCache.values()) {
            tex.dispose();
        }
        textureCache.clear();
    }
}
