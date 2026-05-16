package com.voidvvv.kzcollision.sdk;

import com.voidvvv.kzcollision.core.model.Animation;
import com.voidvvv.kzcollision.core.model.AnimationFrame;
import com.voidvvv.kzcollision.core.model.Project;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class KZAnimationPlayerTest {

    private KZAnimation testAnimation;

    @Before
    public void setUp() {
        Project project = new Project("Test");
        Animation anim = new Animation("walk");

        AnimationFrame frame1 = new AnimationFrame();
        frame1.setDuration(0.1f);
        frame1.setOriginX(10);
        frame1.setOriginY(20);
        frame1.getCollisionBoxes().add(
                new com.voidvvv.kzcollision.core.model.CollisionBox(0, 0, 16, 16, "body1"));

        AnimationFrame frame2 = new AnimationFrame();
        frame2.setDuration(0.2f);
        frame2.setOriginX(15);
        frame2.setOriginY(25);
        frame2.getCollisionBoxes().add(
                new com.voidvvv.kzcollision.core.model.CollisionBox(-5, -5, 20, 20, "body2"));

        AnimationFrame frame3 = new AnimationFrame();
        frame3.setDuration(0.15f);
        frame3.setOriginX(12);
        frame3.setOriginY(22);

        anim.getFrames().add(frame1);
        anim.getFrames().add(frame2);
        anim.getFrames().add(frame3);
        project.getAnimations().add(anim);

        testAnimation = new KZAnimation(anim, project, new HashMap<>(), new HashMap<>());
    }

    @Test
    public void initialState() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        assertEquals(0, player.getCurrentFrameIndex());
        assertTrue(player.isPlaying());
        assertTrue(player.isLooping());
    }

    @Test
    public void advanceToNextFrame() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        // Frame 0 duration is 0.1f
        player.update(0.1f);
        assertEquals(1, player.getCurrentFrameIndex());
    }

    @Test
    public void loopBackToFirstFrame() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        // Total duration: 0.1 + 0.2 + 0.15 = 0.45 (with float rounding)
        // Use a delta slightly above total to guarantee all frames complete and loop
        player.update(0.5f);
        assertEquals(0, player.getCurrentFrameIndex());
    }

    @Test
    public void noLoopStopsAtLastFrame() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        player.setLooping(false);
        player.update(0.5f);
        assertEquals(2, player.getCurrentFrameIndex());
        assertFalse(player.isPlaying());
    }

    @Test
    public void getCollisionBoxes() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        List<CollisionBox> boxes = player.getCurrentCollisionBoxes();
        assertEquals(1, boxes.size());
        assertEquals("body1", boxes.get(0).getLabel());
    }

    @Test
    public void getOriginValues() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        assertEquals(10f, player.getCurrentOriginX(), 0.001f);
        assertEquals(20f, player.getCurrentOriginY(), 0.001f);
    }

    @Test
    public void setCurrentFrameIndex() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        player.setCurrentFrameIndex(2);
        assertEquals(2, player.getCurrentFrameIndex());
    }

    @Test
    public void reset() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        player.update(0.3f);
        player.setPlaying(false);
        player.reset();
        assertEquals(0, player.getCurrentFrameIndex());
        assertTrue(player.isPlaying());
    }

    @Test
    public void pauseDoesNotAdvance() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        player.setPlaying(false);
        player.update(1.0f);
        assertEquals(0, player.getCurrentFrameIndex());
    }
}
