package com.voidvvv.kzcollision.core.model;

import java.util.ArrayList;
import java.util.List;

public class Project {
    private String version = "1.0";
    private String name;
    private List<SourceAsset> sourceAssets;
    private List<SpriteFrame> spriteFrames;
    private List<Animation> animations;

    public Project() {
        this.sourceAssets = new ArrayList<>();
        this.spriteFrames = new ArrayList<>();
        this.animations = new ArrayList<>();
    }

    public Project(String name) {
        this();
        this.name = name;
    }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<SourceAsset> getSourceAssets() { return sourceAssets; }
    public void setSourceAssets(List<SourceAsset> sourceAssets) { this.sourceAssets = sourceAssets; }
    public List<SpriteFrame> getSpriteFrames() { return spriteFrames; }
    public void setSpriteFrames(List<SpriteFrame> spriteFrames) { this.spriteFrames = spriteFrames; }
    public List<Animation> getAnimations() { return animations; }
    public void setAnimations(List<Animation> animations) { this.animations = animations; }

    public SourceAsset findSourceAsset(String id) {
        return sourceAssets.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
    }
    public SpriteFrame findSpriteFrame(String id) {
        return spriteFrames.stream().filter(f -> f.getId().equals(id)).findFirst().orElse(null);
    }
    public Animation findAnimation(String id) {
        return animations.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
    }
}
