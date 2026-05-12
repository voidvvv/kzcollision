# SDK 模块设计分析报告

## 项目模块概览

```
core (数据模型 + 序列化)
  ↑
sdk  (运行时碰撞查询门面)
  ↑
editor (桌面编辑器, libGDX + ImGui)
```

- **core**: 纯数据模型（POJO + Gson 序列化），框架无关，仅依赖 Gson
- **sdk**: 对 core 的封装，提供不可变、简化的碰撞数据查询 API
- **editor**: 桌面 GUI 工具，直接操作 core 模型，保存 JSON 项目文件

---

## 发现的问题

### 问题 1 [严重]：`api` 依赖泄漏了 core 全部内部模型

**文件**: `sdk/build.gradle`

```groovy
dependencies {
  api project(':core')        // <-- 问题
  api "com.badlogicgames.gdx:gdx:$gdxVersion"  // <-- 问题
}
```

**现状**: SDK 使用 `api` 声明对 core 和 libGDX 的依赖。这意味着所有 SDK 的消费者会 **传递性地** 看到以下包的全部公开类：

- `com.voidvvv.kzcollision.core.model.*` — 全部 11 个类（Project, Animation, SpriteFrame, SourceAsset, Placement, Rect 等编辑模型）
- `com.voidvvv.kzcollision.core.serialization.*` — ProjectSerializer, ProjectGsonFactory
- `com.badlogicgames.gdx.*` — 整个 libGDX 框架

**为什么有问题**: SDK 的设计目标是作为简化的、只读的碰撞数据门面。消费者应该只需要知道 `com.voidvvv.kzcollision.sdk.*` 下的 5 个类。但 `api` 依赖把 core 的编辑时内部模型（SourceAsset, SpriteFrame, Placement, Rect, AssetType 等）全部暴露给了运行时消费者，造成：

1. **封装性被破坏** — SDK 消费者可以直接 import 并操作 `core.model.Project`、`core.model.CollisionBox` 等可变类型，绕过 SDK 的不可变保护
2. **无法独立演进** — core 模型的任何改动（重命名、删除字段）都会直接破坏 SDK 消费者的编译
3. **libGDX 被强制引入** — 不使用 libGDX 的项目（服务端、headless 工具）也被迫引入整个 libGDX 框架，只因为 `KZCollisionLoader.load(FileHandle)` 一个方法

**修复建议**:

```groovy
dependencies {
  implementation project(':core')
  compileOnly "com.badlogicgames.gdx:gdx:$gdxVersion"  // 只在编译时需要
  testImplementation 'junit:junit:4.13.2'
}
```

但仅改 Gradle 配置不够——还需要同步修改 SDK 的 Java 代码（见问题 2）。

---

### 问题 2 [严重]：SDK 公开构造函数暴露了 core 内部类型

**涉及文件**:
- `sdk/KZCollisionData.java` — `public KZCollisionData(Project project)`
- `sdk/AnimationCollisionData.java` — `public AnimationCollisionData(Animation animation)`

**现状**: 这两个类的构造函数是 `public` 的，参数类型直接引用了 core 模型类：

```java
// KZCollisionData.java
public KZCollisionData(Project project) { ... }

// AnimationCollisionData.java
public AnimationCollisionData(Animation animation) { ... }
```

**为什么有问题**: 这使得 SDK 消费者可以直接 `new KZCollisionData(project)` 来构造 SDK 对象，从而必须知道 `core.model.Project` 和 `core.model.Animation`。这是问题 1 中无法将 `api` 改为 `implementation` 的根本原因——改了之后消费者就编译不过了。

**修复建议**: 将构造函数改为包私有，对外只通过 `KZCollisionLoader` 工厂方法创建：

```java
// KZCollisionData.java
KZCollisionData(Project project) { ... }  // 去掉 public

// AnimationCollisionData.java
AnimationCollisionData(Animation animation) { ... }  // 去掉 public
```

---

### 问题 3 [严重]：SDK 缺失运行时渲染所需的关键数据

**现状**: SDK 的 `AnimationCollisionData` 从 `AnimationFrame` 中只提取了以下字段：

| 已提取 | 未提取 |
|--------|--------|
| duration | **spriteFrameId** |
| originX, originY | **placements** |

**缺失的 `spriteFrameId`**: 每个 `AnimationFrame` 通过 `spriteFrameId` 引用一个 `SpriteFrame`，而 `SpriteFrame` 记录了纹理来源（sourceAssetId + subRegion）。SDK 完全没有暴露这个引用，导致：

- 游戏运行时无法知道某个动画帧该渲染哪张图片的哪个区域
- SDK 消费者必须自行另外加载项目文件来获取渲染信息，违背了 SDK 简化接入的初衷

**缺失的 `placements`**: `AnimationFrame` 支持复合精灵（一个帧上叠加多个 SpriteFrame），但 SDK 完全忽略了 placement 数据。使用复合精灵的动画在运行时无法正确渲染。

**影响**: SDK 目前只能做纯碰撞查询，无法支撑完整的游戏运行时动画播放。对于一个"运行时 SDK"来说，这是一个重大功能缺失。

**修复建议**: 在 `AnimationCollisionData` 中增加：

```java
private final String spriteFrameId;           // 每帧引用的 SpriteFrame
private final List<PlacementData> placements;  // 复合精灵放置信息
```

或者更好的方案：提供一个独立的 `SpriteFrameData` 类来承载渲染所需信息。

---

### 问题 4 [中等]：Editor 导出的 JSON 与 SDK 读取范围不匹配

**文件**: `editor/panels/PanelManager.java:212-238`

**现状**: Editor 的 "Export Collision JSON" 功能导出的内容是：

```java
Project export = new Project(src.getName());
export.setAnimations(src.getAnimations());
export.setSpriteFrames(src.getSpriteFrames());  // 包含了 SpriteFrame 数据
serializer.save(export, exportFile);
```

导出的 JSON 同时包含 `animations` 和 `spriteFrames`，但 SDK 的 `KZCollisionData` 在构造时只遍历 `project.getAnimations()`，完全忽略 `project.getSpriteFrames()`。

**结果**:
- 导出的 JSON 包含了 SDK 不读取的冗余数据（spriteFrames）
- 或者反过来说，SDK 缺失了对 spriteFrames 的读取能力（与问题 3 一致）

**修复建议**: 两个方向任选其一：
- 如果 SDK 只做碰撞 → Editor 导出时应排除 spriteFrames
- 如果 SDK 也需支持渲染 → SDK 应读取 spriteFrames 数据

---

### 问题 5 [中等]：`CollisionBox` 和 `CollisionQuery` 缺少基本碰撞检测方法

**现状**:
- `core.model.CollisionBox` 有 `containsPoint(float px, float py)` 方法
- `sdk.CollisionBox` 没有 `containsPoint` 方法
- `sdk.CollisionQuery` 只提供了 `overlaps`，没有 `contains` 方法

**影响**: 运行时最常用的碰撞检测操作之一——判断一个点是否在碰撞盒内（例如判断子弹是否命中、鼠标是否点击到实体）——SDK 无法直接完成。消费者需要自行实现这个简单但基础的计算。

**修复建议**:

在 `sdk.CollisionBox` 中添加：

```java
public boolean contains(float px, float py) {
    return px >= x && px <= x + width && py >= y && py <= y + height;
}
```

在 `CollisionQuery` 中添加：

```java
public static boolean contains(CollisionBox box, float px, float py) {
    return box.contains(px, py);
}
```

---

### 问题 6 [轻微]：Editor 声明了 sdk 依赖但未使用

**文件**: `editor/build.gradle:27`

```groovy
implementation project(':sdk')  // 声明了但没有任何 editor 源文件 import sdk 包
```

**现状**: 搜索 editor 全部 16 个 Java 源文件，没有任何 `import com.voidvvv.kzcollision.sdk.*` 语句。SDK 依赖是完全未使用的死依赖。

**影响**: 增加了编译时间和编辑器 JAR 的体积（虽然 sdk 本身很小，但 `api` 声明会传递引入 libGDX，而 editor 已经单独声明了 libGDX 依赖）。

**修复建议**: 从 `editor/build.gradle` 中移除 `implementation project(':sdk')`。如果将来 editor 需要使用 SDK（例如验证导出文件的 SDK 可读性），再添加回来。

---

### 问题 7 [轻微]：`KZCollisionLoader` 每次加载都创建新的 `ProjectSerializer` 和 `Gson` 实例

**文件**: `sdk/KZCollisionLoader.java:16-24`

```java
public static KZCollisionData loadFromFile(File file) {
    ProjectSerializer serializer = new ProjectSerializer();  // 每次创建新实例
    try {
        Project project = serializer.load(file);
        return new KZCollisionData(project);
    } catch (IOException e) {
        throw new RuntimeException("Failed to load collision data: " + file.getPath(), e);
    }
}
```

**现状**: 每次调用 `loadFromFile` 或 `load` 都会创建一个新的 `ProjectSerializer`，其内部通过 `ProjectGsonFactory.create()` 创建新的 `Gson` 实例（含自定义 TypeAdapter 注册）。

**影响**: `Gson` 实例是线程安全的，通常应被复用。虽然游戏通常只在启动时加载一次，性能影响极小，但这不符合 Gson 的惯用模式。如果将来需要批量加载（如热更新资源），浪费会更明显。

**修复建议**: 使用 static final 复用 serializer：

```java
private static final ProjectSerializer SERIALIZER = new ProjectSerializer();
```

---

### 问题 8 [轻微]：SDK 不校验项目版本号

**现状**: `Project` 模型有 `version` 字段（默认 `"1.0"`），但 `KZCollisionLoader` 加载时不检查版本号。

**影响**: 如果将来 core 的 JSON 格式发生变化（比如字段重命名、结构重组），SDK 会尝试用旧逻辑解析新格式，可能静默产生错误数据而非抛出有意义的异常。

**修复建议**: 在 `KZCollisionLoader` 加载后检查版本：

```java
Project project = serializer.load(file);
if (!"1.0".equals(project.getVersion())) {
    throw new RuntimeException("Unsupported project version: " + project.getVersion());
}
```

---

## 问题汇总

| # | 严重度 | 问题 | 影响范围 |
|---|--------|------|----------|
| 1 | 严重 | `api` 依赖泄漏 core 和 libGDX | 所有 SDK 消费者 |
| 2 | 严重 | 公开构造函数暴露 core 类型 | API 封装性 |
| 3 | 严重 | 缺失 spriteFrameId 和 placements | 游戏运行时渲染 |
| 4 | 中等 | Editor 导出与 SDK 读取范围不匹配 | 数据一致性 |
| 5 | 中等 | 碰撞检测缺少 containsPoint | 运行时实用性 |
| 6 | 轻微 | Editor 未使用的 sdk 依赖 | 构建清洁度 |
| 7 | 轻微 | 每次加载创建新 Gson | 性能模式 |
| 8 | 轻微 | 不校验项目版本号 | 向前兼容性 |

---

## 核心矛盾总结

SDK 的设计定位存在一个根本矛盾：**它声称是 core 的简化门面，但实际上只是 core 的部分数据提取器**。

一方面，SDK 用了自己的 `CollisionBox`（不可变、无 id）替代 core 的版本，体现了"简化"的设计意图。另一方面，它的 `api` 依赖和公开构造函数又让 core 的完整编辑模型完全暴露给了消费者，使得"简化"变成了一个空壳。

建议的方向是二选一：

**方案 A — 纯碰撞 SDK**: 如果 SDK 只做碰撞查询
- 将 `api` 改为 `implementation`
- 构造函数改为包私有
- Editor 导出时排除 spriteFrames
- 补充 containsPoint 方法

**方案 B — 完整运行时 SDK**: 如果 SDK 要支撑游戏运行时
- 在方案 A 基础上
- 增加渲染信息（spriteFrame → texture region 映射）
- 增加 placement 支持
- 考虑增加动画播放状态管理
