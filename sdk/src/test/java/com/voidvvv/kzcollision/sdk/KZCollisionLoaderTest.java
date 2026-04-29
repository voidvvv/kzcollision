package com.voidvvv.kzcollision.sdk;

import com.voidvvv.kzcollision.core.model.Animation;
import com.voidvvv.kzcollision.core.model.AnimationFrame;
import com.voidvvv.kzcollision.core.model.Project;
import com.voidvvv.kzcollision.core.serialization.ProjectSerializer;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class KZCollisionLoaderTest {

    private File tempFile;

    @Before
    public void setUp() throws IOException {
        tempFile = File.createTempFile("test_collision", ".json");
        tempFile.deleteOnExit();
    }

    @After
    public void tearDown() {
        if (tempFile != null) tempFile.delete();
    }

    @Test
    public void loadAndQueryCollisionBoxes() throws IOException {
        // Build a project with known data
        Project project = new Project("TestProject");

        com.voidvvv.kzcollision.core.model.CollisionBox box =
                new com.voidvvv.kzcollision.core.model.CollisionBox(-16, 0, 32, 24, "body");
        AnimationFrame frame = new AnimationFrame();
        frame.setDuration(0.15f);
        frame.setOriginX(16);
        frame.setOriginY(0);
        frame.getCollisionBoxes().add(box);

        Animation anim = new Animation("walk");
        anim.getFrames().add(frame);
        project.getAnimations().add(anim);

        // Save
        new ProjectSerializer().save(project, tempFile);

        // Load via SDK
        KZCollisionData data = KZCollisionLoader.loadFromFile(tempFile);

        // Verify animation data
        AnimationCollisionData animData = data.getAnimation("walk");
        assertNotNull(animData);
        assertEquals("walk", animData.getName());
        assertEquals(1, animData.getFrameCount());
        assertEquals(0.15f, animData.getFrameDuration(0), 0.001f);
        assertEquals(16f, animData.getOriginX(0), 0.001f);
        assertEquals(0f, animData.getOriginY(0), 0.001f);

        // Verify collision boxes
        List<CollisionBox> boxes = data.getCollisionBoxes("walk", 0);
        assertEquals(1, boxes.size());
        assertEquals("body", boxes.get(0).getLabel());
        assertEquals(-16f, boxes.get(0).getX(), 0.001f);
        assertEquals(0f, boxes.get(0).getY(), 0.001f);
        assertEquals(32f, boxes.get(0).getWidth(), 0.001f);
        assertEquals(24f, boxes.get(0).getHeight(), 0.001f);
    }

    @Test
    public void queryNonexistentAnimationReturnsEmpty() throws IOException {
        Project project = new Project("Empty");
        new ProjectSerializer().save(project, tempFile);

        KZCollisionData data = KZCollisionLoader.loadFromFile(tempFile);
        assertNull(data.getAnimation("nonexistent"));
        assertTrue(data.getCollisionBoxes("nonexistent", 0).isEmpty());
    }

    @Test
    public void multipleAnimationsAndFrames() throws IOException {
        Project project = new Project("Multi");

        // Walk animation with 2 frames
        Animation walk = new Animation("walk");
        AnimationFrame walkFrame1 = new AnimationFrame();
        walkFrame1.setDuration(0.1f);
        walkFrame1.setOriginX(16);
        walkFrame1.getCollisionBoxes().add(
                new com.voidvvv.kzcollision.core.model.CollisionBox(0, 0, 16, 16, "f1box"));
        AnimationFrame walkFrame2 = new AnimationFrame();
        walkFrame2.setDuration(0.2f);
        walkFrame2.setOriginX(16);
        walkFrame2.getCollisionBoxes().add(
                new com.voidvvv.kzcollision.core.model.CollisionBox(-8, 0, 32, 32, "f2box"));
        walk.getFrames().add(walkFrame1);
        walk.getFrames().add(walkFrame2);

        // Jump animation with 1 frame
        Animation jump = new Animation("jump");
        AnimationFrame jumpFrame = new AnimationFrame();
        jumpFrame.setDuration(0.3f);
        jumpFrame.setOriginX(20);
        jumpFrame.setOriginY(10);
        jump.getFrames().add(jumpFrame);

        project.getAnimations().add(walk);
        project.getAnimations().add(jump);

        new ProjectSerializer().save(project, tempFile);
        KZCollisionData data = KZCollisionLoader.loadFromFile(tempFile);

        // Verify walk
        assertEquals(2, data.getAnimation("walk").getFrameCount());
        assertEquals("f1box", data.getCollisionBoxes("walk", 0).get(0).getLabel());
        assertEquals("f2box", data.getCollisionBoxes("walk", 1).get(0).getLabel());

        // Verify jump
        assertEquals(1, data.getAnimation("jump").getFrameCount());
        assertEquals(10f, data.getAnimation("jump").getOriginY(0), 0.001f);
        assertTrue(data.getCollisionBoxes("jump", 0).isEmpty());

        // Verify animation names
        assertTrue(data.getAnimationNames().contains("walk"));
        assertTrue(data.getAnimationNames().contains("jump"));
    }
}
