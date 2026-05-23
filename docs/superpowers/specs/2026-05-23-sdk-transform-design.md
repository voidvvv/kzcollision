# SDK Animation Transform Design

## Overview

The SDK should let third-party users flip animations on the X axis, flip animations on the Y axis, and scale animations while keeping rendering and collision boxes in sync.

The current SDK already has most of the public surface in `KZAnimationPlayer`: position, scale, rotation, `flipX`, `flipY`, `computeDrawInfo()`, and `computeWorldCollisionBoxes()`. This change formalizes the transform semantics and makes the implementation and tests treat rendering and collision as two outputs of the same transform.

## Goals

- Support explicit X and Y flips through `setFlipX(boolean)` and `setFlipY(boolean)`.
- Support positive and negative scale values through `setScale(float)` and `setScale(float, float)`.
- Make negative scale usable as a flip mechanism.
- Ensure rendered sprites and returned collision boxes use the same effective transform.
- Ensure returned collision box `width` and `height` are always non-negative.
- Preserve existing SDK entry points and keep the feature easy for game code to adopt.

## Non-Goals

- Do not introduce a new public transform class unless implementation proves it necessary.
- Do not change editor data format or core JSON serialization.
- Do not add oriented collision boxes. Rotated collision boxes will continue to be returned as conservative axis-aligned bounding boxes.
- Do not change how animation frames, origins, or texture regions are loaded.

## Recommended Approach

Use the existing `KZAnimationPlayer` API and unify its internal transform math.

The player will continue exposing:

```java
player.setPosition(x, y);
player.setScale(scale);
player.setScale(scaleX, scaleY);
player.setFlipX(true);
player.setFlipY(true);
player.setRotation(degrees);

KZDrawInfo drawInfo = player.computeDrawInfo();
List<CollisionBox> boxes = player.computeWorldCollisionBoxes();
```

Internally, rendering and collision will both use effective scale values:

```java
effectiveScaleX = scaleX * (flipX ? -1f : 1f);
effectiveScaleY = scaleY * (flipY ? -1f : 1f);
```

This makes explicit flip and negative scale composable:

- `setScale(-1f, 1f)` flips X.
- `setFlipX(true)` flips X.
- `setScale(-1f, 1f)` plus `setFlipX(true)` cancels out and renders unflipped on X.
- The same rule applies to Y.

## Coordinate Semantics

`KZAnimationPlayer` position represents the world-space location of the current frame origin.

For each frame:

- `originX` and `originY` are read from the animation data.
- Sprite drawing uses that origin as the scale and rotation pivot.
- Collision boxes are defined in frame-local coordinates.
- World collision boxes are computed by transforming each local box around the same origin.

The transform order is:

1. Start from a local point on the collision box.
2. Subtract the frame origin.
3. Apply effective X/Y scale.
4. Apply rotation around the origin.
5. Add player world position.

For rendering, `KZDrawInfo` will expose the same effective scale values so `KZDrawInfo.draw(batch)` and `computeWorldCollisionBoxes()` agree.

## Collision Box Calculation

`computeWorldCollisionBoxes()` should transform all four corners of each local collision box:

```java
local corners:
  (box.x, box.y)
  (box.x + box.width, box.y)
  (box.x, box.y + box.height)
  (box.x + box.width, box.y + box.height)
```

Each corner is transformed using the player transform. The returned SDK `CollisionBox` is the axis-aligned bounding box enclosing those transformed corners:

```java
x = min(transformedCornerX)
y = min(transformedCornerY)
width = max(transformedCornerX) - min(transformedCornerX)
height = max(transformedCornerY) - min(transformedCornerY)
```

This algorithm handles positive scale, negative scale, explicit flips, and rotation with one consistent code path.

## Rendering Behavior

`computeDrawInfo()` should return:

- `region`: current frame texture region.
- `x`, `y`: player position.
- `originX`, `originY`: current frame origin.
- `width`, `height`: current region dimensions.
- `scaleX`, `scaleY`: effective scale values.
- `rotation`: current player rotation.

`KZDrawInfo.draw(SpriteBatch batch)` can continue using libGDX's full draw overload:

```java
batch.draw(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
```

The design assumes libGDX negative scale behavior is acceptable and intentional for mirroring around the origin.

## API Compatibility

This is intended to be backward compatible:

- Existing users of `setScale(positive)` keep the same behavior.
- Existing users of `setFlipX` or `setFlipY` keep the same behavior.
- Existing users of `computeDrawInfo()` continue to receive a `KZDrawInfo`.
- Existing users of `computeWorldCollisionBoxes()` continue to receive SDK `CollisionBox` instances.

The main behavioral tightening is that negative scale becomes officially supported and tested.

## Implementation Notes

The implementation should keep the math small and centralized.

Recommended private helpers in `KZAnimationPlayer`:

```java
private float effectiveScaleX()
private float effectiveScaleY()
private void transformPoint(...)
```

`computeDrawInfo()` should call the effective scale helpers.

`computeWorldCollisionBoxes()` should call the same helpers and transform all four corners for every box, even when rotation is zero. The current split between the rotation and non-rotation branches can be simplified or retained only if both branches delegate to shared point transform logic.

## Testing Plan

Add or update SDK unit tests around `KZAnimationPlayer`.

Rendering info tests:

- Positive scale returns positive draw scale.
- `flipX` returns negative effective X draw scale.
- `flipY` returns negative effective Y draw scale.
- Negative `scaleX` acts as X flip.
- Negative `scaleY` acts as Y flip.
- Negative scale combined with explicit flip cancels on that axis.

World collision tests:

- Position offset moves collision boxes correctly.
- Positive scale changes world collision box position and size correctly.
- X flip mirrors the box around the frame origin.
- Y flip mirrors the box around the frame origin.
- X and Y flip together mirror both axes.
- Negative scale gives the same collision result as the corresponding explicit flip.
- Negative scale plus explicit flip cancels out.
- Returned `width` and `height` remain positive.
- Existing rotation behavior remains valid through corner-based AABB calculation.

## Acceptance Criteria

- A third-party user can flip X, flip Y, and scale through `KZAnimationPlayer`.
- The same transform settings produce visually matching sprite output and collision output.
- `computeDrawInfo()` exposes effective scale values that correctly reflect flip and negative scale.
- `computeWorldCollisionBoxes()` returns correct world-space AABBs after flip, scale, and rotation.
- Existing public API remains source-compatible.
- SDK tests cover explicit flip, negative scale, flip cancellation, collision coordinates, and collision sizes.

