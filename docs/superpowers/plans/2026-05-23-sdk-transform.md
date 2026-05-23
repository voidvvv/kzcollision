# SDK Animation Transform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `KZAnimationPlayer` officially support explicit flips, negative scale, and regular scale with matching draw parameters and world collision boxes.

**Architecture:** Keep the existing public API and centralize transform math inside `KZAnimationPlayer`. `computeDrawInfo()` and `computeWorldCollisionBoxes()` must both use the same effective scale calculation, and `KZDrawInfo` must expose both origin-world position and SpriteBatch bottom-left draw position.

**Tech Stack:** Java 11, Gradle, JUnit 4, libGDX SDK types.

---

### Task 1: Add Transform Behavior Tests

**Files:**
- Modify: `sdk/src/test/java/com/voidvvv/kzcollision/sdk/KZAnimationPlayerTest.java`
- Test: `sdk/src/test/java/com/voidvvv/kzcollision/sdk/KZAnimationPlayerTest.java`

- [ ] **Step 1: Add test setup with a collision box that exposes origin-based mirroring**

Use the existing `setUp()` fixture. It already creates frame 0 with:

```java
frame1.setOriginX(10);
frame1.setOriginY(20);
frame1.getCollisionBoxes().add(
        new com.voidvvv.kzcollision.core.model.CollisionBox(0, 0, 16, 16, "body1"));
```

This is enough to verify mirroring because the local box spans `x=0..16` and `y=0..16` around origin `(10,20)`.

- [ ] **Step 2: Write failing draw info tests**

Add these tests to `KZAnimationPlayerTest`:

```java
@Test
public void drawInfoUsesEffectiveScaleForExplicitFlips() {
    KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
    player.setScale(2f, 3f);
    player.setFlipX(true);
    player.setFlipY(true);

    KZDrawInfo info = player.computeDrawInfo();

    assertEquals(-2f, info.scaleX, 0.001f);
    assertEquals(-3f, info.scaleY, 0.001f);
}

@Test
public void drawInfoSupportsNegativeScaleAndFlipCancellation() {
    KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
    player.setScale(-2f, 3f);
    player.setFlipX(true);

    KZDrawInfo info = player.computeDrawInfo();

    assertEquals(2f, info.scaleX, 0.001f);
    assertEquals(3f, info.scaleY, 0.001f);
}
```

- [ ] **Step 3: Write failing collision transform tests**

Add these tests to `KZAnimationPlayerTest`:

```java
@Test
public void worldCollisionBoxesApplyPositionAndScaleAroundOrigin() {
    KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
    player.setPosition(100f, 200f);
    player.setScale(2f, 3f);

    CollisionBox box = player.computeWorldCollisionBoxes().get(0);

    assertEquals(80f, box.getX(), 0.001f);
    assertEquals(140f, box.getY(), 0.001f);
    assertEquals(32f, box.getWidth(), 0.001f);
    assertEquals(48f, box.getHeight(), 0.001f);
}

@Test
public void worldCollisionBoxesMirrorXAroundOrigin() {
    KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
    player.setPosition(100f, 200f);
    player.setFlipX(true);

    CollisionBox box = player.computeWorldCollisionBoxes().get(0);

    assertEquals(94f, box.getX(), 0.001f);
    assertEquals(180f, box.getY(), 0.001f);
    assertEquals(16f, box.getWidth(), 0.001f);
    assertEquals(16f, box.getHeight(), 0.001f);
}

@Test
public void worldCollisionBoxesMirrorYAroundOrigin() {
    KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
    player.setPosition(100f, 200f);
    player.setFlipY(true);

    CollisionBox box = player.computeWorldCollisionBoxes().get(0);

    assertEquals(90f, box.getX(), 0.001f);
    assertEquals(204f, box.getY(), 0.001f);
    assertEquals(16f, box.getWidth(), 0.001f);
    assertEquals(16f, box.getHeight(), 0.001f);
}

@Test
public void worldCollisionBoxesTreatNegativeScaleLikeFlip() {
    KZAnimationPlayer flipped = new KZAnimationPlayer(testAnimation);
    flipped.setPosition(100f, 200f);
    flipped.setFlipX(true);

    KZAnimationPlayer negativeScaled = new KZAnimationPlayer(testAnimation);
    negativeScaled.setPosition(100f, 200f);
    negativeScaled.setScale(-1f, 1f);

    CollisionBox flipBox = flipped.computeWorldCollisionBoxes().get(0);
    CollisionBox negativeScaleBox = negativeScaled.computeWorldCollisionBoxes().get(0);

    assertEquals(flipBox.getX(), negativeScaleBox.getX(), 0.001f);
    assertEquals(flipBox.getY(), negativeScaleBox.getY(), 0.001f);
    assertEquals(flipBox.getWidth(), negativeScaleBox.getWidth(), 0.001f);
    assertEquals(flipBox.getHeight(), negativeScaleBox.getHeight(), 0.001f);
}

@Test
public void worldCollisionBoxesKeepPositiveSizeWhenFlipAndNegativeScaleCancel() {
    KZAnimationPlayer player = new KZAnimationPlayer(testAnimation);
    player.setPosition(100f, 200f);
    player.setScale(-1f, -1f);
    player.setFlipX(true);
    player.setFlipY(true);

    CollisionBox box = player.computeWorldCollisionBoxes().get(0);

    assertEquals(90f, box.getX(), 0.001f);
    assertEquals(180f, box.getY(), 0.001f);
    assertEquals(16f, box.getWidth(), 0.001f);
    assertEquals(16f, box.getHeight(), 0.001f);
}
```

- [ ] **Step 4: Run tests and verify red**

Run:

```bash
./gradlew.bat :sdk:test --tests com.voidvvv.kzcollision.sdk.KZAnimationPlayerTest
```

Expected: at least one new test fails, especially collision tests that expose the existing non-rotation branch behavior.

### Task 2: Centralize Transform Math

**Files:**
- Modify: `sdk/src/main/java/com/voidvvv/kzcollision/sdk/KZAnimationPlayer.java`
- Modify: `sdk/src/main/java/com/voidvvv/kzcollision/sdk/KZDrawInfo.java`
- Test: `sdk/src/test/java/com/voidvvv/kzcollision/sdk/KZAnimationPlayerTest.java`
- Test: `sdk/src/test/java/com/voidvvv/kzcollision/sdk/KZDrawInfoTest.java`

- [ ] **Step 1: Add effective scale helpers**

Add private methods inside `KZAnimationPlayer`:

```java
private float effectiveScaleX() {
    return scaleX * (flipX ? -1f : 1f);
}

private float effectiveScaleY() {
    return scaleY * (flipY ? -1f : 1f);
}
```

- [ ] **Step 2: Update draw info to use helpers**

Change `computeDrawInfo()` to compute:

```java
float effectiveSX = effectiveScaleX();
float effectiveSY = effectiveScaleY();
```

Keep the returned `KZDrawInfo` shape unchanged.

- [ ] **Step 3: Replace collision branch math with four-corner transform**

Add a small local helper method or inline loop that transforms all four corners using:

```java
float sx = effectiveScaleX();
float sy = effectiveScaleY();
float ox = animation.getOriginX(currentFrame);
float oy = animation.getOriginY(currentFrame);
float rad = (float) Math.toRadians(rotation);
float cos = (float) Math.cos(rad);
float sin = (float) Math.sin(rad);

float localX = (cornerX - ox) * sx;
float localY = (cornerY - oy) * sy;
float worldX = localX * cos - localY * sin + x;
float worldY = localX * sin + localY * cos + y;
```

Build the output AABB from min and max transformed corner coordinates.

- [ ] **Step 4: Run focused tests and verify green**

Update `KZDrawInfo` so `x/y` remain the player origin position and new `drawX/drawY` fields expose the bottom-left position expected by `SpriteBatch`:

```java
this.drawX = x - originX;
this.drawY = y - originY;
```

`KZDrawInfo.draw(batch)` must call:

```java
batch.draw(region, drawX, drawY, originX, originY, width, height, scaleX, scaleY, rotation);
```

- [ ] **Step 5: Run focused tests and verify green**

Run:

```bash
./gradlew.bat :sdk:test --tests com.voidvvv.kzcollision.sdk.KZAnimationPlayerTest --tests com.voidvvv.kzcollision.sdk.KZDrawInfoTest
```

Expected: all focused transform and draw-info tests pass.

### Task 3: Run SDK Regression Tests

**Files:**
- No source changes expected.
- Test: `sdk/src/test/java/com/voidvvv/kzcollision/sdk/*`

- [ ] **Step 1: Run all SDK tests**

Run:

```bash
./gradlew.bat :sdk:test
```

Expected: all SDK tests pass.

- [ ] **Step 2: Inspect git diff**

Run:

```bash
git diff -- sdk/src/main/java/com/voidvvv/kzcollision/sdk/KZAnimationPlayer.java sdk/src/test/java/com/voidvvv/kzcollision/sdk/KZAnimationPlayerTest.java
```

Expected: diff only contains transform math and tests for the new behavior.

- [ ] **Step 3: Commit implementation**

Run:

```bash
git add sdk/src/main/java/com/voidvvv/kzcollision/sdk/KZAnimationPlayer.java sdk/src/test/java/com/voidvvv/kzcollision/sdk/KZAnimationPlayerTest.java docs/superpowers/plans/2026-05-23-sdk-transform.md
git commit -m "feat: support animation flip and scale transforms"
```
