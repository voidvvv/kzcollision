package com.voidvvv.kzcollision.sdk;

import com.voidvvv.kzcollision.core.model.Animation;
import com.voidvvv.kzcollision.core.model.AnimationFrame;
import com.voidvvv.kzcollision.core.model.Project;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class KZTransformTest {

    private KZAnimation testAnimation;

    @Before
    public void setUp() {
        Project project = new Project("Test");
        Animation anim = new Animation("walk");

        // Frame with origin at (32, 16), collision box at local (10, 5, 44, 54)
        AnimationFrame frame = new AnimationFrame();
        frame.setDuration(0.1f);
        frame.setOriginX(32);
        frame.setOriginY(16);
        frame.getCollisionBoxes().add(
                new com.voidvvv.kzcollision.core.model.CollisionBox(10, 5, 44, 54, "body"));
        anim.getFrames().add(frame);

        project.getAnimations().add(anim);
        testAnimation = new KZAnimation(anim, project, new HashMap<>(), new HashMap<>());
    }

    @Test
    public void worldBoxes_noTransform_positionOnly() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        player.setPosition(100, 200);

        List<CollisionBox> boxes = player.computeWorldCollisionBoxes();
        assertEquals(1, boxes.size());

        CollisionBox box = boxes.get(0);
        // local box (10,5) relative to origin (32,16) → offset = (-22, -11)
        // world = position + offset = (100-22, 200-11) = (78, 189)
        assertEquals(78f, box.getX(), 0.001f);
        assertEquals(189f, box.getY(), 0.001f);
        assertEquals(44f, box.getWidth(), 0.001f);
        assertEquals(54f, box.getHeight(), 0.001f);
        assertEquals("body", box.getLabel());
    }

    @Test
    public void worldBoxes_noTransform_zeroPosition() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        // position = (0,0)
        List<CollisionBox> boxes = player.computeWorldCollisionBoxes();
        CollisionBox box = boxes.get(0);

        // offset = (10-32, 5-16) = (-22, -11)
        assertEquals(-22f, box.getX(), 0.001f);
        assertEquals(-11f, box.getY(), 0.001f);
        assertEquals(44f, box.getWidth(), 0.001f);
        assertEquals(54f, box.getHeight(), 0.001f);
    }

    @Test
    public void worldBoxes_withScale() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        player.setPosition(100, 200);
        player.setScale(2, 2);

        List<CollisionBox> boxes = player.computeWorldCollisionBoxes();
        CollisionBox box = boxes.get(0);

        // offset = (10-32, 5-16) * 2 = (-44, -22)
        // world = (100-44, 200-22) = (56, 178)
        // size = (44*2, 54*2) = (88, 108)
        assertEquals(56f, box.getX(), 0.001f);
        assertEquals(178f, box.getY(), 0.001f);
        assertEquals(88f, box.getWidth(), 0.001f);
        assertEquals(108f, box.getHeight(), 0.001f);
    }

    @Test
    public void worldBoxes_withFlipX() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        player.setPosition(100, 200);
        player.setFlipX(true);

        List<CollisionBox> boxes = player.computeWorldCollisionBoxes();
        CollisionBox box = boxes.get(0);

        // effectiveScaleX = -1
        // left corner: (10-32)*(-1) = 22, world = 100+22 = 122
        // right corner: (10+44-32)*(-1) = -22, world = 100-22 = 78
        // AABB: x = min(122,78) = 78, width = |122-78| = 44
        assertEquals(78f, box.getX(), 0.001f);
        // Y unchanged: 200 + (5-16)*1 = 189
        assertEquals(189f, box.getY(), 0.001f);
        assertEquals(44f, box.getWidth(), 0.001f);
        assertEquals(54f, box.getHeight(), 0.001f);
    }

    @Test
    public void worldBoxes_withFlipXAndScale() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        player.setPosition(100, 200);
        player.setScale(2, 2);
        player.setFlipX(true);

        List<CollisionBox> boxes = player.computeWorldCollisionBoxes();
        CollisionBox box = boxes.get(0);

        // effectiveScaleX = -2, effectiveScaleY = 2
        // left corner: (10-32)*(-2) = 44, world = 100+44 = 144
        // right corner: (54-32)*(-2) = -44, world = 100-44 = 56
        // AABB: x = 56, width = 144-56 = 88
        assertEquals(56f, box.getX(), 0.001f);
        // Y: (5-16)*2 = -22, world = 200-22 = 178, height = 54*2 = 108
        assertEquals(178f, box.getY(), 0.001f);
        assertEquals(88f, box.getWidth(), 0.001f);
        assertEquals(108f, box.getHeight(), 0.001f);
    }

    @Test
    public void worldBoxes_withRotation90() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        player.setPosition(100, 200);
        player.setRotation(90f);

        List<CollisionBox> boxes = player.computeWorldCollisionBoxes();
        CollisionBox box = boxes.get(0);

        // Corners relative to origin (32,16):
        // (10-32, 5-16) = (-22, -11)
        // (10+44-32, 5-16) = (22, -11)
        // (10-32, 5+54-16) = (-22, 43)
        // (10+44-32, 5+54-16) = (22, 43)
        // Rotate 90° CCW: (x,y) → (-y, x)
        // (-22,-11) → (11, -22) + (100,200) = (111, 178)
        // (22,-11) → (11, 22) + (100,200) = (111, 222)
        // (-22,43) → (-43, -22) + (100,200) = (57, 178)
        // (22,43) → (-43, 22) + (100,200) = (57, 222)
        // AABB: x=57, y=178, width=111-57=54, height=222-178=44
        assertEquals(57f, box.getX(), 0.1f);
        assertEquals(178f, box.getY(), 0.1f);
        assertEquals(54f, box.getWidth(), 0.1f);
        assertEquals(44f, box.getHeight(), 0.1f);
    }

    @Test
    public void worldBoxes_withRotation180() {
        KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
        player.setPosition(100, 200);
        player.setRotation(180f);

        List<CollisionBox> boxes = player.computeWorldCollisionBoxes();
        CollisionBox box = boxes.get(0);

        // Rotate 180°: (x,y) → (-x, -y)
        // (-22,-11) → (22, 11) + (100,200) = (122, 211)
        // (22,-11) → (-22, 11) + (100,200) = (78, 211)
        // (-22,43) → (22, -43) + (100,200) = (122, 157)
        // (22,43) → (-22, -43) + (100,200) = (78, 157)
        // AABB: x=78, y=157, width=44, height=54
        assertEquals(78f, box.getX(), 0.1f);
        assertEquals(157f, box.getY(), 0.1f);
        assertEquals(44f, box.getWidth(), 0.1f);
        assertEquals(54f, box.getHeight(), 0.1f);
    }

    @Test
    public void worldBoxes_multipleBoxes() {
        Project project = new Project("Test");
        Animation anim = new Animation("walk");

        AnimationFrame frame = new AnimationFrame();
        frame.setDuration(0.1f);
        frame.setOriginX(0);
        frame.setOriginY(0);
        frame.getCollisionBoxes().add(
                new com.voidvvv.kzcollision.core.model.CollisionBox(0, 0, 10, 10, "a"));
        frame.getCollisionBoxes().add(
                new com.voidvvv.kzcollision.core.model.CollisionBox(20, 0, 10, 10, "b"));
        anim.getFrames().add(frame);

        project.getAnimations().add(anim);
        KZAnimation multiAnim = new KZAnimation(anim, project, new HashMap<>(), new HashMap<>());

        KZAnimationPlayer player = new KZAnimationPlayer(multiAnim);
        player.setPosition(50, 50);

        List<CollisionBox> boxes = player.computeWorldCollisionBoxes();
        assertEquals(2, boxes.size());

        assertEquals(50f, boxes.get(0).getX(), 0.001f);
        assertEquals("a", boxes.get(0).getLabel());
        assertEquals(70f, boxes.get(1).getX(), 0.001f);
        assertEquals("b", boxes.get(1).getLabel());
    }
}
