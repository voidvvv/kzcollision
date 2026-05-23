# KZCollision SDK 使用指南

本指南说明如何在 libGDX 游戏项目中集成 KZCollision SDK，加载编辑器生成的碰撞框动画资源，并在游戏中进行渲染和碰撞检测。

---

## 目录

1. [项目集成](#1-项目集成)
2. [资源文件准备](#2-资源文件准备)
3. [快速上手 (Hello World)](#3-快速上手-hello-world)
4. [核心 API 参考](#4-核心-api-参考)
5. [完整使用教程](#5-完整使用教程)
6. [碰撞检测详解](#6-碰撞检测详解)
7. [高级用法](#7-高级用法)
8. [常见问题](#8-常见问题)

---

## 1. 项目集成

### 1.1 添加依赖

在您的 libGDX 项目的 `build.gradle` 中添加 SDK 依赖：

```groovy
// 核心模块（build.gradle 根目录定义了 project 变量）
dependencies {
    // 方式 A：如果 kzcollision 以本地模块形式存在
    implementation project(':kzcollision:sdk')

    // 方式 B：如果已发布为 JAR
    // implementation files('libs/sdk-1.0.0.jar')
}
```

SDK 会自动传递依赖 `core` 模块和 `com.badlogicgames.gdx:gdx`，无需重复添加。

### 1.2 SDK JAR 说明

构建产物位于 `sdk/build/libs/sdk-1.0.0.jar`，包含以下类：

| 类名 | 说明 |
|------|------|
| `KZProject` | 主入口，加载碰撞 JSON 并管理纹理资源 |
| `KZAnimation` | 已解析的动画数据（TextureRegion + 碰撞框） |
| `KZAnimationPlayer` | 动画播放状态机（含坐标变换封装） |
| `KZDrawInfo` | 不可变绘制参数快照 |
| `CollisionBox` | 不可变碰撞框 |
| `CollisionQuery` | 碰撞检测工具类 |
| `KZCollisionLoader` | 旧版加载器（仅加载碰撞数据，不加载纹理） |
| `KZCollisionData` | 旧版碰撞数据容器 |
| `AnimationCollisionData` | 旧版单动画碰撞数据 |

### 1.3 包导入

```java
import com.voidvvv.kzcollision.sdk.KZProject;
import com.voidvvv.kzcollision.sdk.KZAnimation;
import com.voidvvv.kzcollision.sdk.KZAnimationPlayer;
import com.voidvvv.kzcollision.sdk.KZDrawInfo;
import com.voidvvv.kzcollision.sdk.CollisionBox;
import com.voidvvv.kzcollision.sdk.CollisionQuery;
```

---

## 2. 资源文件准备

### 2.1 资源目录结构

使用 KZCollision 编辑器创建项目后，会在 assets 目录下生成如下结构：

```
assets/
├── collision/
│   └── collision.json        ← 碰撞框动画数据文件
├── sprites/
│   ├── hero.png              ← 单张精灵图（SINGLE 类型）
│   └── enemy.png
└── atlas/
    ├── characters.atlas      ← TextureAtlas 描述文件（ATLAS 类型）
    └── characters.png        ← Atlas 对应的纹理图集
```

### 2.2 collision.json 结构概览

JSON 文件包含三部分核心数据：

```json
{
  "version": "1.0",
  "name": "MyGame",
  "sourceAssets": [
    {
      "id": "asset-uuid-1",
      "type": "SINGLE",
      "internalPath": "sprites/hero.png",
      "regions": [...]
    },
    {
      "id": "asset-uuid-2",
      "type": "ATLAS",
      "internalPath": "atlas/characters.atlas",
      "regions": [...]
    }
  ],
  "spriteFrames": [
    {
      "id": "frame-uuid-1",
      "sourceAssetId": "asset-uuid-1",
      "sourceRegionName": null,
      "subRegion": { "x": 0, "y": 0, "width": 64, "height": 64 }
    }
  ],
  "animations": [
    {
      "id": "anim-uuid-1",
      "name": "walk",
      "frames": [
        {
          "spriteFrameId": "frame-uuid-1",
          "duration": 0.15,
          "originX": 32,
          "originY": 0,
          "collisionBoxes": [
            { "x": 10, "y": 5, "width": 44, "height": 54, "label": "body" }
          ]
        }
      ]
    }
  ]
}
```

- **sourceAssets**：源图片资源列表，`internalPath` 是相对于 assets 根目录的路径
- **spriteFrames**：精灵帧定义，关联源资源并指定裁切区域（subRegion）
- **animations**：动画定义，每个动画包含多个帧，每帧有持续时间、原点偏移和碰撞框列表

### 2.3 资源类型

| 类型 | 说明 | 纹理加载方式 |
|------|------|-------------|
| `SINGLE` | 单张图片（PNG/JPG） | `new Texture(fileHandle)` |
| `ATLAS` | libGDX TextureAtlas | `new TextureAtlas(fileHandle)` |

---

## 3. 快速上手 (Hello World)

以下是一个最小可运行的示例，展示如何在 libGDX 游戏中加载并渲染一个带碰撞框的动画。

### 3.1 资源文件

确保 assets 目录下有以下文件：

```
assets/
├── collision/
│   └── collision.json
└── sprites/
    └── hero.png
```

### 3.2 完整代码

```java
package com.mygame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.voidvvv.kzcollision.sdk.*;

import java.util.List;

public class MyGame extends ApplicationAdapter {

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private KZProject project;
    private KZAnimationPlayer player;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // 1. 加载碰撞项目（自动推断 assets 根目录）
        project = KZProject.load(Gdx.files.internal("collision/collision.json"));

        // 2. 获取动画并创建播放器
        KZAnimation walkAnim = project.getAnimation("walk");
        player = new KZAnimationPlayer(walkAnim);

        // 3. 设置实体世界位置
        player.setPosition(100, 100);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // 更新动画帧
        player.update(delta);

        // 清屏
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 绘制精灵 — 一行搞定，无需手动计算原点偏移
        batch.begin();
        player.draw(batch);
        batch.end();

        // 绘制碰撞框（调试用）— 直接获取世界坐标碰撞框
        drawCollisionBoxes();
    }

    private void drawCollisionBoxes() {
        // computeWorldCollisionBoxes() 自动将碰撞框变换到世界坐标
        List<CollisionBox> boxes = player.computeWorldCollisionBoxes();
        if (boxes.isEmpty()) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1, 0, 0, 1);
        for (CollisionBox box : boxes) {
            shapeRenderer.rect(box.getX(), box.getY(), box.getWidth(), box.getHeight());
        }
        shapeRenderer.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        project.dispose();
    }
}
```

### 3.3 运行效果

- 屏幕上显示动画精灵
- 红色矩形线框覆盖在精灵上显示碰撞框
- 动画自动循环播放

---

## 4. 核心 API 参考

### 4.1 KZProject

碰撞项目主入口。负责加载 JSON、预加载纹理、构建动画数据。

```java
// 创建方式（均为静态工厂方法，不可直接构造）
KZProject project = KZProject.load(collisionFile);
KZProject project = KZProject.load(assetsRoot, collisionFile);
KZProject project = KZProject.loadFromFile(assetsRootFile, collisionFile);
```

| 方法 | 说明 |
|------|------|
| `load(FileHandle collisionFile)` | 加载碰撞 JSON。assets 根目录自动推断为 `collisionFile.parent().parent()` |
| `load(FileHandle assetsRoot, FileHandle collisionFile)` | 显式指定 assets 根目录加载 |
| `loadFromFile(File assetsRoot, File collisionFile)` | 使用 `java.io.File` 加载 |
| `getAnimation(String name)` | 按名称获取动画，返回 `KZAnimation`，不存在则返回 `null` |
| `getAnimationNames()` | 获取所有动画名称集合 (`Set<String>`) |
| `dispose()` | 释放所有加载的 `Texture` 和 `TextureAtlas` 资源 |

**资源路径解析规则：**

```
load(Gdx.files.internal("collision/collision.json"))
  → collisionFile = "collision/collision.json"
  → assetsRoot = collisionFile.parent().parent() = ""（即 assets 根目录）

纹理加载时: assetsRoot.child(sourceAsset.internalPath)
  → 例如 "sprites/hero.png" → "sprites/hero.png"（相对于 assets 根）
```

### 4.2 KZAnimation

已解析的单个动画。每个帧包含 `TextureRegion`、持续时间、原点偏移和碰撞框列表。

| 方法 | 说明 |
|------|------|
| `getName()` | 动画名称 |
| `getFrameCount()` | 总帧数 |
| `getRegion(int frameIndex)` | 获取指定帧的 `TextureRegion` |
| `getDuration(int frameIndex)` | 获取指定帧持续时间（秒） |
| `getOriginX(int frameIndex)` | 获取指定帧 X 原点偏移 |
| `getOriginY(int frameIndex)` | 获取指定帧 Y 原点偏移 |
| `getCollisionBoxes(int frameIndex)` | 获取指定帧的碰撞框列表 (`List<CollisionBox>`) |

**纹理解析策略：**

- **SINGLE 资源**：使用 `subRegion` 从 `Texture` 裁切 `TextureRegion`
- **ATLAS 资源**：通过 `sourceRegionName` 在 `TextureAtlas` 中查找区域，若存在 `subRegion` 则进一步裁切

### 4.3 KZAnimationPlayer

动画播放状态机，内置坐标变换（位置、缩放、镜像、旋转）。

```java
KZAnimationPlayer player = new KZAnimationPlayer(animation);
```

**播放控制：**

| 方法 | 说明 |
|------|------|
| `update(float delta)` | 推进动画时间，自动切换帧 |
| `getCurrentRegion()` | 当前帧的 `TextureRegion` |
| `getCurrentOriginX()` | 当前帧 X 原点偏移 |
| `getCurrentOriginY()` | 当前帧 Y 原点偏移 |
| `getCurrentCollisionBoxes()` | 当前帧碰撞框列表（局部坐标） |
| `getCurrentDuration()` | 当前帧持续时间 |
| `getCurrentFrameIndex()` | 当前帧索引 |
| `getAnimation()` | 获取关联的 `KZAnimation` |
| `setPlaying(boolean)` | 设置播放/暂停 |
| `isPlaying()` | 是否正在播放 |
| `setLooping(boolean)` | 设置循环/单次播放（默认循环） |
| `isLooping()` | 是否循环 |
| `setCurrentFrameIndex(int)` | 跳转到指定帧 |
| `reset()` | 重置到第 0 帧，恢复播放状态 |

**坐标变换：**

| 方法 | 说明 |
|------|------|
| `setPosition(float x, float y)` | 设置实体世界位置 |
| `getX()` / `getY()` | 获取实体世界位置 |
| `setScale(float sx, float sy)` | 设置缩放 |
| `setScale(float s)` | 设置统一缩放 |
| `getScaleX()` / `getScaleY()` | 获取缩放值 |
| `setRotation(float degrees)` | 设置旋转角度（度，逆时针） |
| `getRotation()` | 获取旋转角度 |
| `setFlipX(boolean)` | 设置水平镜像 |
| `setFlipY(boolean)` | 设置垂直镜像 |
| `isFlipX()` / `isFlipY()` | 获取镜像状态 |

**绘制与碰撞（封装坐标变换）：**

| 方法 | 说明 |
|------|------|
| `draw(SpriteBatch batch)` | 便捷绘制，一行完成渲染（自动处理原点、缩放、旋转、镜像） |
| `computeDrawInfo()` | 计算并返回 `KZDrawInfo` 绘制参数快照 |
| `computeWorldCollisionBoxes()` | 返回世界坐标碰撞框（自动应用位置、缩放、镜像、旋转） |

**播放行为：**

- 默认循环播放 (`looping = true`)
- 非循环模式播放到最后一帧后停止，`isPlaying()` 返回 `false`
- `setCurrentFrameIndex()` 会重置内部计时器
- `reset()` 等价于 `setCurrentFrameIndex(0)` + `setPlaying(true)`

### 4.4 KZDrawInfo

不可变绘制参数快照，由 `KZAnimationPlayer.computeDrawInfo()` 产生。

| 字段 | 类型 | 说明 |
|------|------|------|
| `region` | `TextureRegion` | 当前帧纹理区域 |
| `x` | `float` | 绘制位置（KZ 原点在此处对齐世界位置） |
| `y` | `float` | 绘制位置 Y |
| `originX` | `float` | 旋转/缩放中心 X（相对于精灵左下角） |
| `originY` | `float` | 旋转/缩放中心 Y |
| `width` | `float` | 绘制宽度（像素） |
| `height` | `float` | 绘制高度（像素） |
| `scaleX` | `float` | X 缩放（负数 = 水平镜像） |
| `scaleY` | `float` | Y 缩放（负数 = 垂直镜像） |
| `rotation` | `float` | 旋转角度（度，逆时针） |

```java
// 便捷绘制
KZDrawInfo info = player.computeDrawInfo();
info.draw(batch);

// 等价于：
batch.draw(info.region, info.x, info.y, info.originX, info.originY,
           info.width, info.height, info.scaleX, info.scaleY, info.rotation);
```

### 4.5 CollisionBox

不可变碰撞框。

| 字段 | 类型 | 说明 |
|------|------|------|
| `x` | `float` | 左下角 X 坐标（相对于精灵原点的局部坐标） |
| `y` | `float` | 左下角 Y 坐标 |
| `width` | `float` | 宽度 |
| `height` | `float` | 高度 |
| `label` | `String` | 碰撞框标签（如 "body"、"head"、"attack"），可为 `null` |

### 4.6 CollisionQuery

碰撞检测工具类，提供静态方法。

```java
// 检测两个碰撞框是否重叠
boolean hit = CollisionQuery.overlaps(boxA, boxB);

// 检测两组碰撞框是否有任一重叠
boolean hit = CollisionQuery.overlaps(boxesA, boxesB);
```

---

## 5. 完整使用教程

### 5.1 多动画角色

一个角色通常有多个动画（站立、行走、攻击等）。以下展示如何管理多动画切换：

```java
public class Player {
    private final KZProject project;
    private KZAnimationPlayer animationPlayer;
    private float x, y;
    private boolean facingLeft = false;

    public Player(KZProject project, float x, float y) {
        this.project = project;
        this.x = x;
        this.y = y;
        switchAnimation("idle");
    }

    public void switchAnimation(String name) {
        KZAnimation anim = project.getAnimation(name);
        if (anim == null) {
            Gdx.app.error("Player", "Animation not found: " + name);
            return;
        }
        boolean wasFlipX = animationPlayer != null && animationPlayer.isFlipX();
        animationPlayer = new KZAnimationPlayer(anim);
        animationPlayer.setFlipX(wasFlipX);
    }

    public void update(float delta) {
        animationPlayer.update(delta);
    }

    public void render(SpriteBatch batch) {
        // 一行绘制，自动处理原点偏移和镜像
        animationPlayer.setPosition(x, y);
        animationPlayer.draw(batch);
    }

    /** 获取世界坐标碰撞框，直接用于碰撞检测 */
    public List<CollisionBox> getWorldCollisionBoxes() {
        return animationPlayer.computeWorldCollisionBoxes();
    }

    public void setFacingLeft(boolean facingLeft) {
        this.facingLeft = facingLeft;
        animationPlayer.setFlipX(facingLeft);
    }

    public void moveLeft(float delta) { x -= 200 * delta; setFacingLeft(true); }
    public void moveRight(float delta) { x += 200 * delta; setFacingLeft(false); }

    public float getX() { return x; }
    public float getY() { return y; }
}
```

使用方式：

```java
// 在 Game 类中
KZProject project = KZProject.load(Gdx.files.internal("collision/collision.json"));
Player player = new Player(project, 100, 100);

// 根据输入切换动画
if (movingLeft) {
    player.moveLeft(delta);
    player.switchAnimation("walk");
} else if (attacking) {
    player.switchAnimation("attack");
} else {
    player.switchAnimation("idle");
}

// 碰撞检测 — 直接使用世界坐标碰撞框
if (CollisionQuery.overlaps(player.getWorldCollisionBoxes(), enemy.getWorldCollisionBoxes())) {
    onCollision();
}
```

### 5.2 使用 TextureAtlas 资源

如果碰撞 JSON 引用了 `.atlas` 文件，SDK 会自动加载 `TextureAtlas` 并通过区域名称定位纹理：

```java
// 无需特殊处理 — SDK 内部自动识别 ATLAS 类型资源
// atlas/characters.atlas 中定义的每个 region 都会被正确映射
KZProject project = KZProject.load(Gdx.files.internal("collision/collision.json"));

// 获取使用 atlas 区域的动画
KZAnimation attackAnim = project.getAnimation("attack");
KZAnimationPlayer player = new KZAnimationPlayer(attackAnim);
```

SDK 内部的 ATLAS 解析流程：

1. 加载 `TextureAtlas`：`new TextureAtlas(assetsRoot.child("atlas/characters.atlas"))`
2. 通过 `spriteFrame.sourceRegionName` 在 atlas 中查找 `AtlasRegion`
3. 如果 `spriteFrame.subRegion` 非空，在 atlas region 基础上进一步裁切

### 5.3 显式指定 Assets 根目录

当 collision.json 不在标准位置时，可以显式指定 assets 根目录：

```java
// 场景 1：collision.json 在自定义路径
KZProject project = KZProject.load(
    Gdx.files.internal("data/"),                    // assets 根目录
    Gdx.files.internal("data/config/collision.json") // collision JSON 路径
);

// 场景 2：使用 java.io.File（适用于桌面端）
KZProject project = KZProject.loadFromFile(
    new File("C:/mygame/assets/"),
    new File("C:/mygame/assets/collision/collision.json")
);
```

### 5.4 单次播放动画（如攻击动画）

```java
KZAnimation attackAnim = project.getAnimation("attack");
KZAnimationPlayer attackPlayer = new KZAnimationPlayer(attackAnim);
attackPlayer.setLooping(false); // 单次播放

// 在 update 中
attackPlayer.update(delta);

// 检查动画是否播放完毕
if (!attackPlayer.isPlaying()) {
    // 攻击结束，切换回 idle
    player.switchAnimation("idle");
}
```

### 5.5 手动控制帧

```java
KZAnimationPlayer player = new KZAnimationPlayer(anim);
player.setPlaying(false); // 暂停自动播放

// 手动跳转到特定帧（例如预览模式）
player.setCurrentFrameIndex(3);

// 继续播放
player.setPlaying(true);
```

---

## 6. 碰撞检测详解

### 6.1 使用 computeWorldCollisionBoxes()

SDK 封装了所有坐标变换逻辑。只需调用 `computeWorldCollisionBoxes()` 即可获得世界坐标碰撞框：

```java
player.setPosition(entityX, entityY);
player.setScale(2.0f, 2.0f);
player.setFlipX(true);
player.setRotation(15f);

// 自动应用位置、缩放、镜像、旋转
List<CollisionBox> worldBoxes = player.computeWorldCollisionBoxes();
```

碰撞框的坐标已经过原点偏移、缩放、镜像和旋转变换，可直接用于碰撞检测。

### 6.2 两个实体之间的碰撞检测

```java
public class GameWorld {
    private Player hero;
    private Player enemy;

    public void checkCollisions() {
        // 直接获取世界坐标碰撞框
        List<CollisionBox> heroBoxes = hero.getWorldCollisionBoxes();
        List<CollisionBox> enemyBoxes = enemy.getWorldCollisionBoxes();

        if (CollisionQuery.overlaps(heroBoxes, enemyBoxes)) {
            onCollision(hero, enemy);
        }
    }
}
```

### 6.3 基于标签的碰撞检测

碰撞框可以有标签（label），用于区分不同类型的碰撞区域：

```java
public void checkAttackHit(Player attacker, Player defender) {
    List<CollisionBox> attackBoxes = new ArrayList<>();
    List<CollisionBox> bodyBoxes = new ArrayList<>();

    // 从世界坐标碰撞框中按标签分类
    for (CollisionBox box : attacker.getWorldCollisionBoxes()) {
        if ("attack".equals(box.getLabel())) {
            attackBoxes.add(box);
        }
    }

    for (CollisionBox box : defender.getWorldCollisionBoxes()) {
        if ("body".equals(box.getLabel())) {
            bodyBoxes.add(box);
        }
    }

    // 只检测攻击框与身体框的碰撞
    if (CollisionQuery.overlaps(attackBoxes, bodyBoxes)) {
        applyDamage(defender);
    }
}
```

### 6.4 手动坐标换算（低级 API）

如果需要直接访问局部坐标碰撞框，可以使用 `getCurrentCollisionBoxes()`：

```java
// 局部坐标碰撞框（相对于精灵左上角）
List<CollisionBox> localBoxes = player.getCurrentCollisionBoxes();

// 手动换算为世界坐标
float worldBoxX = player.getX() - player.getCurrentOriginX() + localBoxes.get(0).getX();
float worldBoxY = player.getY() - player.getCurrentOriginY() + localBoxes.get(0).getY();
```

通常推荐使用 `computeWorldCollisionBoxes()` 代替手动换算。

---

## 7. 高级用法

### 7.1 列出所有可用动画

```java
KZProject project = KZProject.load(Gdx.files.internal("collision/collision.json"));

Set<String> animNames = project.getAnimationNames();
Gdx.app.log("Game", "Available animations: " + animNames);

for (String name : animNames) {
    KZAnimation anim = project.getAnimation(name);
    Gdx.app.log("Game", name + ": " + anim.getFrameCount() + " frames");
}
```

### 7.2 多角色共享项目

`KZProject` 加载纹理资源，多个 `KZAnimationPlayer` 可以共享同一个 `KZProject`：

```java
// 只加载一次
KZProject project = KZProject.load(Gdx.files.internal("collision/collision.json"));

// 多个角色使用同一项目中的不同动画
KZAnimationPlayer heroIdle = new KZAnimationPlayer(project.getAnimation("hero_idle"));
KZAnimationPlayer heroWalk = new KZAnimationPlayer(project.getAnimation("hero_walk"));
KZAnimationPlayer enemyIdle = new KZAnimationPlayer(project.getAnimation("enemy_idle"));

// 各 Player 独立更新
heroWalk.update(delta);
enemyIdle.update(delta);
```

### 7.3 碰撞框可视化调试

使用 `computeWorldCollisionBoxes()` 简化调试渲染：

```java
public class CollisionDebugger {
    private final ShapeRenderer shapes = new ShapeRenderer();

    public void draw(KZAnimationPlayer player) {
        List<CollisionBox> boxes = player.computeWorldCollisionBoxes();

        shapes.setProjectionMatrix(/* your camera matrix */);
        shapes.begin(ShapeRenderer.ShapeType.Line);

        for (CollisionBox box : boxes) {
            if ("attack".equals(box.getLabel())) {
                shapes.setColor(1, 1, 0, 1); // 黄色 = 攻击框
            } else if ("hurt".equals(box.getLabel())) {
                shapes.setColor(1, 0, 0, 1); // 红色 = 受击框
            } else {
                shapes.setColor(0, 1, 0, 1); // 绿色 = 默认
            }
            // box 坐标已经是世界坐标，直接绘制
            shapes.rect(box.getX(), box.getY(), box.getWidth(), box.getHeight());
        }

        // 绘制实体位置标记
        shapes.setColor(1, 1, 1, 1);
        shapes.circle(player.getX(), player.getY(), 3);

        shapes.end();
    }

    public void dispose() {
        shapes.dispose();
    }
}
```

### 7.4 缩放、镜像与旋转

SDK 支持对精灵和碰撞框统一应用变换：

```java
KZAnimationPlayer player = new KZAnimationPlayer(anim);
player.setPosition(200, 100);

// 缩放 2 倍
player.setScale(2.0f);
player.draw(batch);
// 碰撞框也会相应放大
List<CollisionBox> boxes = player.computeWorldCollisionBoxes();

// 水平镜像（角色面向左）
player.setFlipX(true);
player.draw(batch);

// 旋转 45 度
player.setRotation(45f);
player.draw(batch);
// 旋转时碰撞框自动计算 AABB 包围盒

// 组合变换
player.setScale(1.5f, 1.5f);
player.setFlipX(true);
player.setRotation(15f);
player.draw(batch);
List<CollisionBox> worldBoxes = player.computeWorldCollisionBoxes();
// 所有变换已应用到碰撞框
```

### 7.5 旧版 API（仅碰撞数据）

如果只需要碰撞框数据而不需要纹理加载（例如服务端逻辑、自定义渲染），可以使用旧版 API：

```java
// 仅加载碰撞数据，不加载纹理
KZCollisionData data = KZCollisionLoader.load(Gdx.files.internal("collision/collision.json"));

// 获取动画碰撞数据
AnimationCollisionData animData = data.getAnimation("walk");
int frameCount = animData.getFrameCount();
float duration = animData.getFrameDuration(0);
float originX = animData.getOriginX(0);
float originY = animData.getOriginY(0);
List<CollisionBox> boxes = animData.getCollisionBoxes(0);

// 按快捷方式获取碰撞框
List<CollisionBox> boxes2 = data.getCollisionBoxes("walk", 0);

// 列出所有动画
Set<String> names = data.getAnimationNames();
```

---

## 8. 常见问题

### Q: `getAnimation()` 返回 null

**原因**：动画名称不匹配。检查编辑器中设置的动画名称是否与代码中使用的名称完全一致（区分大小写）。

```java
// 调试：列出所有可用动画
for (String name : project.getAnimationNames()) {
    Gdx.app.log("Debug", "Animation: " + name);
}
```

### Q: `getCurrentRegion()` 返回 null

**原因**：纹理加载失败。可能的情况：
- `sourceAsset.internalPath` 对应的文件不存在于 assets 目录中
- 文件路径大小写不匹配（Android 平台区分大小写）

**排查方法**：查看日志中的 `"KZProject"` 标签，寻找 `"Failed to load asset"` 信息。

### Q: 纹理路径问题 — Android 黑屏

Android 的 `Gdx.files.internal()` 路径区分大小写。确保 `internalPath` 的大小写与实际文件完全一致。

```
正确: internalPath = "sprites/Hero.png"  文件: sprites/Hero.png
错误: internalPath = "sprites/hero.png"  文件: sprites/Hero.png  ← Android 上会失败
```

### Q: assets 根目录推断错误

`load(collisionFile)` 使用 `collisionFile.parent().parent()` 推断 assets 根目录：

```
collision/collision.json
  → parent() = collision/
  → parent().parent() = ""  (assets 根)

data/config/collision.json
  → parent() = data/config/
  → parent().parent() = data/  (可能不是您期望的根目录)
```

如果路径不标准，使用显式指定版本：

```java
KZProject.load(Gdx.files.internal("assets/"), Gdx.files.internal("data/config/collision.json"));
```

### Q: 内存泄漏

`KZProject` 会加载并持有所有纹理资源。务必在不需要时调用 `dispose()`：

```java
@Override
public void dispose() {
    project.dispose(); // 释放所有 Texture 和 TextureAtlas
}
```

多个 `KZAnimationPlayer` 共享同一个 `KZProject`，不需要（也不应该）单独释放 player。

### Q: 如何与 Tiled Map 或 Box2D 配合使用？

SDK 的碰撞框是自定义的矩形数据，不依赖 Box2D。您可以手动将 `CollisionBox` 转换为 Box2D 的 `Body`：

```java
for (CollisionBox box : player.getCurrentCollisionBoxes()) {
    PolygonShape shape = new PolygonShape();
    shape.setAsBox(box.getWidth() / 2f, box.getHeight() / 2f);
    // 创建 BodyDef 和 Fixture...
}
```

注意碰撞框是动画帧级别的，每帧可能不同。通常在每帧更新后重新同步到物理引擎。

---

## 完整生命周期示例

```java
public class KZGame extends ApplicationAdapter {
    private SpriteBatch batch;
    private KZProject project;
    private KZAnimationPlayer player;
    private float playerX = 200, playerY = 150;

    @Override
    public void create() {
        batch = new SpriteBatch();

        // 加载
        project = KZProject.load(Gdx.files.internal("collision/collision.json"));

        // 初始化动画
        KZAnimation idle = project.getAnimation("idle");
        player = new KZAnimationPlayer(idle);
        player.setPosition(playerX, playerY);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // 输入处理
        handleInput(delta);

        // 更新
        player.update(delta);

        // 渲染
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        player.draw(batch); // 一行绘制
        batch.end();
    }

    private void handleInput(float delta) {
        boolean moving = false;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            playerX -= 200 * delta;
            moving = true;
            player.setFlipX(true);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            playerX += 200 * delta;
            moving = true;
            player.setFlipX(false);
        }

        // 切换动画
        String animName = moving ? "walk" : "idle";
        if (player.getAnimation() != project.getAnimation(animName)) {
            boolean flipX = player.isFlipX();
            player = new KZAnimationPlayer(project.getAnimation(animName));
            player.setFlipX(flipX);
        }

        // 同步位置
        player.setPosition(playerX, playerY);
    }

    @Override
    public void dispose() {
        batch.dispose();
        project.dispose();
    }
}
```
