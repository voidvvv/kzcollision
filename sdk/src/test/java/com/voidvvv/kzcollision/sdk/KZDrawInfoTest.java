package com.voidvvv.kzcollision.sdk;

import com.voidvvv.kzcollision.core.model.Animation;
import com.voidvvv.kzcollision.core.model.AnimationFrame;
import com.voidvvv.kzcollision.core.model.Project;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class KZDrawInfoTest {

    private KZAnimation testAnimation;

    @Before
    public void setUp() {
        Project project = new Project("Test");
        Animation anim = new Animation("walk");

        AnimationFrame frame = new AnimationFrame();
        frame.setDuration(0.1f);
        frame.setOriginX(32);
        frame.setOriginY(16);
        anim.getFrames().add(frame);

        project.getAnimations().add(anim);
        testAnimation = new KZAnimation(anim, project, new HashMap<>(), new HashMap<>());
    }

    @Test
    public void drawInfo_noTransform() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        KZDrawInfo info = player.computeDrawInfo();

        assertEquals(0f, info.x, 0.001f);
        assertEquals(0f, info.y, 0.001f);
        assertEquals(1f, info.scaleX, 0.001f);
        assertEquals(1f, info.scaleY, 0.001f);
        assertEquals(0f, info.rotation, 0.001f);
        assertEquals(32f, info.originX, 0.001f);
        assertEquals(16f, info.originY, 0.001f);
    }

    @Test
    public void drawInfo_withPosition() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        player.setPosition(100, 200);
        KZDrawInfo info = player.computeDrawInfo();

        assertEquals(100f, info.x, 0.001f);
        assertEquals(200f, info.y, 0.001f);
        assertEquals(68f, info.drawX, 0.001f);
        assertEquals(184f, info.drawY, 0.001f);
    }

    @Test
    public void drawInfo_withScale() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        player.setScale(2, 3);
        KZDrawInfo info = player.computeDrawInfo();

        assertEquals(2f, info.scaleX, 0.001f);
        assertEquals(3f, info.scaleY, 0.001f);
    }

    @Test
    public void drawInfo_withFlipX() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        player.setFlipX(true);
        KZDrawInfo info = player.computeDrawInfo();

        assertEquals(-1f, info.scaleX, 0.001f);
        assertEquals(1f, info.scaleY, 0.001f);
    }

    @Test
    public void drawInfo_withFlipY() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        player.setFlipY(true);
        KZDrawInfo info = player.computeDrawInfo();

        assertEquals(1f, info.scaleX, 0.001f);
        assertEquals(-1f, info.scaleY, 0.001f);
    }

    @Test
    public void drawInfo_withFlipAndScale() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        player.setScale(2, 2);
        player.setFlipX(true);
        KZDrawInfo info = player.computeDrawInfo();

        assertEquals(-2f, info.scaleX, 0.001f);
        assertEquals(2f, info.scaleY, 0.001f);
    }

    @Test
    public void drawInfo_withRotation() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        player.setRotation(45f);
        KZDrawInfo info = player.computeDrawInfo();

        assertEquals(45f, info.rotation, 0.001f);
    }

    @Test
    public void drawInfo_allTransformsCombined() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        player.setPosition(50, 75);
        player.setScale(2, 3);
        player.setRotation(90f);
        player.setFlipX(true);
        KZDrawInfo info = player.computeDrawInfo();

        assertEquals(50f, info.x, 0.001f);
        assertEquals(75f, info.y, 0.001f);
        assertEquals(18f, info.drawX, 0.001f);
        assertEquals(59f, info.drawY, 0.001f);
        assertEquals(-2f, info.scaleX, 0.001f);
        assertEquals(3f, info.scaleY, 0.001f);
        assertEquals(90f, info.rotation, 0.001f);
        assertEquals(32f, info.originX, 0.001f);
        assertEquals(16f, info.originY, 0.001f);
    }
}
