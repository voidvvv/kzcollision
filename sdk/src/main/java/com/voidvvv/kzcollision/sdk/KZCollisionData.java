package com.voidvvv.kzcollision.sdk;

import com.voidvvv.kzcollision.core.model.Animation;
import com.voidvvv.kzcollision.core.model.Project;

import java.util.*;

public class KZCollisionData {
    private final Map<String, AnimationCollisionData> animations;

    public KZCollisionData(Project project) {
        this.animations = new LinkedHashMap<>();
        for (Animation anim : project.getAnimations()) {
            animations.put(anim.getName(), new AnimationCollisionData(anim));
        }
    }

    public AnimationCollisionData getAnimation(String name) {
        return animations.get(name);
    }

    public List<CollisionBox> getCollisionBoxes(String animationName, int frameIndex) {
        AnimationCollisionData animData = animations.get(animationName);
        if (animData == null) return Collections.emptyList();
        if (frameIndex < 0 || frameIndex >= animData.getFrameCount()) return Collections.emptyList();
        return animData.getCollisionBoxes(frameIndex);
    }

    public Set<String> getAnimationNames() {
        return Collections.unmodifiableSet(animations.keySet());
    }
}
