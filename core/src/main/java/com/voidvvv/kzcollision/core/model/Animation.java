package com.voidvvv.kzcollision.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Animation {
    private String id;
    private String name;
    private List<AnimationFrame> frames;

    public Animation() {
        this.id = UUID.randomUUID().toString();
        this.frames = new ArrayList<>();
    }

    public Animation(String name) {
        this();
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<AnimationFrame> getFrames() { return frames; }
    public void setFrames(List<AnimationFrame> frames) { this.frames = frames; }
}
