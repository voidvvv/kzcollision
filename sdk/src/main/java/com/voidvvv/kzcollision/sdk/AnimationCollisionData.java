package com.voidvvv.kzcollision.sdk;

import com.voidvvv.kzcollision.core.model.Animation;
import com.voidvvv.kzcollision.core.model.AnimationFrame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnimationCollisionData {
    private final String name;
    private final int frameCount;
    private final List<Float> frameDurations;
    private final List<Float> originXs;
    private final List<Float> originYs;
    private final List<List<CollisionBox>> frameCollisionBoxes;

    public AnimationCollisionData(Animation animation) {
        this.name = animation.getName();
        this.frameCount = animation.getFrames().size();
        this.frameDurations = new ArrayList<>();
        this.originXs = new ArrayList<>();
        this.originYs = new ArrayList<>();
        this.frameCollisionBoxes = new ArrayList<>();

        for (AnimationFrame frame : animation.getFrames()) {
            frameDurations.add(frame.getDuration());
            originXs.add(frame.getOriginX());
            originYs.add(frame.getOriginY());
            List<CollisionBox> boxes = new ArrayList<>();
            for (com.voidvvv.kzcollision.core.model.CollisionBox box : frame.getCollisionBoxes()) {
                boxes.add(new CollisionBox(box.getX(), box.getY(), box.getWidth(), box.getHeight(), box.getLabel()));
            }
            frameCollisionBoxes.add(Collections.unmodifiableList(boxes));
        }
    }

    public String getName() { return name; }
    public int getFrameCount() { return frameCount; }
    public float getFrameDuration(int frameIndex) { return frameDurations.get(frameIndex); }
    public float getOriginX(int frameIndex) { return originXs.get(frameIndex); }
    public float getOriginY(int frameIndex) { return originYs.get(frameIndex); }
    public List<CollisionBox> getCollisionBoxes(int frameIndex) { return frameCollisionBoxes.get(frameIndex); }
}
