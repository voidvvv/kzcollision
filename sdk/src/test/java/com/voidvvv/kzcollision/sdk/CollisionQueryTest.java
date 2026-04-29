package com.voidvvv.kzcollision.sdk;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.Collections;

public class CollisionQueryTest {

    @Test
    public void overlappingBoxes() {
        CollisionBox a = new CollisionBox(0, 0, 10, 10, "a");
        CollisionBox b = new CollisionBox(5, 5, 10, 10, "b");
        assertTrue(CollisionQuery.overlaps(a, b));
    }

    @Test
    public void nonOverlappingBoxes() {
        CollisionBox a = new CollisionBox(0, 0, 10, 10, "a");
        CollisionBox b = new CollisionBox(20, 20, 10, 10, "b");
        assertFalse(CollisionQuery.overlaps(a, b));
    }

    @Test
    public void touchingBoxesDoNotOverlap() {
        CollisionBox a = new CollisionBox(0, 0, 10, 10, "a");
        CollisionBox b = new CollisionBox(10, 0, 10, 10, "b");
        assertFalse(CollisionQuery.overlaps(a, b));
    }

    @Test
    public void fullyContainedBox() {
        CollisionBox outer = new CollisionBox(0, 0, 20, 20, "outer");
        CollisionBox inner = new CollisionBox(5, 5, 5, 5, "inner");
        assertTrue(CollisionQuery.overlaps(outer, inner));
    }

    @Test
    public void overlappingLists() {
        CollisionBox a = new CollisionBox(0, 0, 10, 10, "a");
        CollisionBox b = new CollisionBox(5, 5, 10, 10, "b");
        assertTrue(CollisionQuery.overlaps(Arrays.asList(a), Arrays.asList(b)));
    }

    @Test
    public void nonOverlappingLists() {
        CollisionBox a = new CollisionBox(0, 0, 10, 10, "a");
        CollisionBox b = new CollisionBox(20, 20, 10, 10, "b");
        assertFalse(CollisionQuery.overlaps(Arrays.asList(a), Arrays.asList(b)));
    }

    @Test
    public void emptyListsDoNotOverlap() {
        CollisionBox a = new CollisionBox(0, 0, 10, 10, "a");
        assertFalse(CollisionQuery.overlaps(Arrays.asList(a), Collections.emptyList()));
        assertFalse(CollisionQuery.overlaps(Collections.emptyList(), Arrays.asList(a)));
    }
}
