package com.voidvvv.kzcollision.sdk;

import java.util.List;

public final class CollisionQuery {

    public static boolean overlaps(CollisionBox a, CollisionBox b) {
        return a.getX() < b.getX() + b.getWidth() &&
               a.getX() + a.getWidth() > b.getX() &&
               a.getY() < b.getY() + b.getHeight() &&
               a.getY() + a.getHeight() > b.getY();
    }

    public static boolean overlaps(List<CollisionBox> boxesA, List<CollisionBox> boxesB) {
        for (CollisionBox a : boxesA) {
            for (CollisionBox b : boxesB) {
                if (overlaps(a, b)) return true;
            }
        }
        return false;
    }
}
