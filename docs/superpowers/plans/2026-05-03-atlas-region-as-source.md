# Atlas 区域作为独立图片素材 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 导入 atlas 文件后，每个解析出的 atlas 区域作为独立图片素材显示在素材列表中（平铺展示，不使用折叠分组），预览面板只展示该区域对应的纹理部分，而非整张 atlas 拼接大图。

**架构方案：** 保持现有数据模型不变（一个 `SourceAsset(type=ATLAS)` 包含多个 `SourceRegion`）。修改素材列表 UI，将 atlas 区域平铺为顶层可选项。在 `EditorState` 中新增 `selectedSourceRegionId` 字段，让预览面板能定位到具体的区域。修改 `SourceImagePreviewPanel`，根据选中区域的 bounds 计算 UV 坐标，只渲染该区域的纹理部分（复用 `SpriteFramePreviewPanel` 的 UV 计算模式）。修复项目加载时 ATLAS 纹理不恢复的问题。

**技术栈：** Java 11、libGDX、ImGui（imgui-java 绑定），无新增依赖。

---

## 问题分析

**当前行为：**
1. 导入 atlas 文件后，创建一个 `SourceAsset(type=ATLAS)`，包含多个 `SourceRegion` 子项
2. `SourceImagesPanel.renderAtlasAsset()` 使用 `ImGui.collapsingHeader()` 折叠展示 atlas 区域
3. `SourceImagePreviewPanel` 无论选中哪个区域，始终渲染**整张 atlas 纹理**
4. `PanelManager.loadProject()` 只恢复 SINGLE 资产的纹理，ATLAS 资产在项目重新加载后丢失纹理

**期望行为：**
1. 每个 atlas 区域作为**顶层、独立的可选项**显示在素材列表中（无折叠分组）
2. 选中某个区域后，预览面板只显示该区域的纹理部分
3. 每个区域可右键"Add as Frame"（已支持）
4. 项目保存/加载能正确恢复 ATLAS 资产

---

## 文件结构

| 操作 | 文件 | 职责 |
|------|------|------|
| 修改 | `editor/.../EditorState.java` | 新增 `selectedSourceRegionId` 字段及其 getter/setter/reset |
| 修改 | `editor/.../panels/SourceImagesPanel.java` | 平铺展示 atlas 区域；选中时设置 `selectedSourceRegionId` |
| 修改 | `editor/.../panels/SourceImagePreviewPanel.java` | 基于区域 bounds 的 UV 裁剪预览 |
| 修改 | `editor/.../panels/PanelManager.java` | 项目加载时恢复 ATLAS 资产纹理 |

无需修改：`SourceAsset`、`SourceRegion`、`AtlasParser`、`SpriteFrame`、`ViewportRenderer`、`SpriteFramePreviewPanel`。

---

### 任务 1：在 EditorState 中新增 `selectedSourceRegionId`

**文件：**
- 修改：`editor/src/main/java/com/voidvvv/kzcollision/editor/EditorState.java`

当前只有 `selectedSourceAssetId` 被追踪。对于 atlas 资产，多个区域共享同一个 assetId，因此需要区域级别的选中状态，预览面板才能知道要展示哪个区域。

- [ ] **步骤 1：添加字段、getter/setter、reset 逻辑**

在 `EditorState.java` 的第 21 行 `private String selectedSourceAssetId;` 之后新增：

```java
private String selectedSourceRegionId;
```

在第 42 行 `public void setSelectedSourceAssetId(String id) { this.selectedSourceAssetId = id; }` 之后新增：

```java
public String getSelectedSourceRegionId() { return selectedSourceRegionId; }
public void setSelectedSourceRegionId(String id) { this.selectedSourceRegionId = id; }
```

在 `reset()` 方法中（第 67 行），在 `this.selectedSourceAssetId = null;` 之后新增：

```java
this.selectedSourceRegionId = null;
```

- [ ] **步骤 2：编译验证**

运行：`./gradlew :editor:compileJava 2>&1 | tail -10`
预期：BUILD SUCCESSFUL

- [ ] **步骤 3：提交**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/EditorState.java
git commit -m "feat(editor): add selectedSourceRegionId to EditorState"
```

---

### 任务 2：平铺展示 atlas 区域并追踪区域选中状态

**文件：**
- 修改：`editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagesPanel.java`
  - 第 64-75 行：`renderAssetList()` — 移除 `renderAtlasAsset` 分支
  - 第 83-92 行：`renderAtlasAsset()` — 改为平铺展示各区域
  - 第 94-101 行：`renderSingleAsset()` — 设置 `selectedSourceRegionId`

**当前行为：** `renderAtlasAsset()` 使用 `ImGui.collapsingHeader()` 将区域折叠在 atlas 文件名下方。`renderSingleAsset()` 展示为顶层可选项。

**新行为：** atlas 区域和单张图片都作为顶层可选项平铺展示。atlas 区域使用区域名称作为显示标签。选中任一项时同时设置 `selectedSourceAssetId` 和 `selectedSourceRegionId`。

- [ ] **步骤 1：替换 `renderAssetList()` 方法**

替换 `renderAssetList()` 方法（第 64-75 行）：

```java
private void renderAssetList() {
    EditorState state = stateProvider.getState();
    Project project = state.getProject();

    for (SourceAsset asset : project.getSourceAssets()) {
        if (asset.getType() == AssetType.ATLAS) {
            renderAtlasRegions(asset);
        } else {
            renderSingleAsset(asset);
        }
    }
}
```

- [ ] **步骤 2：将 `renderAtlasAsset()` 替换为 `renderAtlasRegions()`**

替换 `renderAtlasAsset()` 方法（第 83-92 行）：

```java
private void renderAtlasRegions(SourceAsset asset) {
    for (SourceRegion region : asset.getRegions()) {
        boolean selected = region.getId().equals(stateProvider.getState().getSelectedSourceRegionId());
        if (ImGui.selectable(region.getName(), selected)) {
            stateProvider.getState().setSelectedSourceAssetId(asset.getId());
            stateProvider.getState().setSelectedSourceRegionId(region.getId());
        }
        renderRegionContextMenu(region);
    }
}
```

与旧版 `renderAtlasAsset()` 的关键区别：
- 不使用 `collapsingHeader`，区域直接平铺展示
- 区域名称直接作为 selectable 标签（无 "  " 前缀缩进）
- 设置 `selectedSourceRegionId`，使预览面板知道选中了哪个区域
- 使用 `selected` 标志高亮当前选中的区域

- [ ] **步骤 3：更新 `renderSingleAsset()` 以设置区域 ID**

替换 `renderSingleAsset()` 方法（第 94-101 行）：

```java
private void renderSingleAsset(SourceAsset asset) {
    boolean selected = asset.getId().equals(stateProvider.getState().getSelectedSourceAssetId());
    if (ImGui.selectable(getDisplayName(asset), selected)) {
        stateProvider.getState().setSelectedSourceAssetId(asset.getId());
        if (!asset.getRegions().isEmpty()) {
            stateProvider.getState().setSelectedSourceRegionId(asset.getRegions().get(0).getId());
        }
    }
    if (!asset.getRegions().isEmpty()) {
        renderRegionContextMenu(asset.getRegions().get(0));
    }
}
```

新增内容：
- 同步设置 `selectedSourceRegionId`（保持行为一致性）
- 添加 `selected` 标志实现视觉高亮

- [ ] **步骤 4：编译验证**

运行：`./gradlew :editor:compileJava 2>&1 | tail -10`
预期：BUILD SUCCESSFUL

- [ ] **步骤 5：提交**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagesPanel.java
git commit -m "feat(editor): flatten atlas regions as top-level source items"
```

---

### 任务 3：更新 SourceImagePreviewPanel 实现区域级预览

**文件：**
- 修改：`editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagePreviewPanel.java`

**当前行为：** `renderPreview(Texture tex)` 使用 UV `(0, 1, 1, 0)` 渲染完整纹理——始终显示整张 atlas 图片。

**新行为：** 如果选中的 `SourceRegion` 带有 bounds，则根据 `region.bounds` 相对于纹理尺寸计算 UV 坐标，只渲染该区域部分。若无区域或无 bounds，则回退到显示完整纹理。

- [ ] **步骤 1：重写 `render()` 方法，解析选中的区域**

替换 `render()` 方法（第 15-38 行）：

```java
public void render() {
    if (ImGui.begin("Source Image Preview")) {
        EditorState state = stateProvider.getState();
        String assetId = state.getSelectedSourceAssetId();

        if (assetId == null) {
            ImGui.textDisabled("Select a source image to preview");
        } else {
            SourceAsset asset = state.getProject().findSourceAsset(assetId);
            if (asset == null) {
                ImGui.textDisabled("Source image not found");
            } else {
                Texture tex = state.getTextureCache().get(asset.getFilePath());
                if (tex == null) {
                    ImGui.textDisabled("Texture not loaded");
                } else {
                    SourceRegion region = findSelectedRegion(asset, state.getSelectedSourceRegionId());
                    renderPreview(tex, region);
                    renderInfo(asset, tex, region);
                }
            }
        }
    }
    ImGui.end();
}
```

- [ ] **步骤 2：添加 `findSelectedRegion()` 辅助方法**

在 `render()` 方法之后添加：

```java
private SourceRegion findSelectedRegion(SourceAsset asset, String regionId) {
    if (regionId == null || asset.getRegions().isEmpty()) {
        return null;
    }
    for (SourceRegion region : asset.getRegions()) {
        if (region.getId().equals(regionId)) {
            return region;
        }
    }
    return null;
}
```

- [ ] **步骤 3：重写 `renderPreview()` 使用区域 bounds**

替换 `renderPreview(Texture tex)` 方法（第 40-57 行）：

```java
private void renderPreview(Texture tex, SourceRegion region) {
    float availWidth = ImGui.getContentRegionAvailX();
    float imgWidth, imgHeight;
    float uv0x, uv0y, uv1x, uv1y;

    if (region != null && region.getBounds() != null) {
        Rect bounds = region.getBounds();
        imgWidth = bounds.width;
        imgHeight = bounds.height;
        float texW = tex.getWidth();
        float texH = tex.getHeight();
        uv0x = bounds.x / texW;
        uv0y = 1f - (bounds.y + bounds.height) / texH;
        uv1x = (bounds.x + bounds.width) / texW;
        uv1y = 1f - bounds.y / texH;
    } else {
        imgWidth = tex.getWidth();
        imgHeight = tex.getHeight();
        uv0x = 0;
        uv0y = 1;
        uv1x = 1;
        uv1y = 0;
    }

    float scale = availWidth / imgWidth;
    float displayWidth = imgWidth * scale;
    float displayHeight = imgHeight * scale;

    float maxHeight = 160f;
    if (displayHeight > maxHeight) {
        float heightScale = maxHeight / displayHeight;
        displayWidth *= heightScale;
        displayHeight *= heightScale;
    }

    int texId = tex.getTextureObjectHandle();
    ImGui.image(texId, displayWidth, displayHeight, uv0x, uv0y, uv1x, uv1y);
}
```

UV 计算逻辑与 `SpriteFramePreviewPanel` 一致：atlas 坐标以左上角为原点，libGDX 纹理以左下角为原点，因此 `uv0y = 1 - (y + h) / texH`、`uv1y = 1 - y / texH` 实现 Y 轴翻转。

- [ ] **步骤 4：重写 `renderInfo()` 显示区域详情**

替换 `renderInfo(SourceAsset asset, Texture tex)` 方法（第 59-68 行）：

```java
private void renderInfo(SourceAsset asset, Texture tex, SourceRegion region) {
    if (region != null && region.getName() != null) {
        ImGui.text(region.getName());
    } else {
        String displayName = asset.getFilePath();
        int lastSep = Math.max(displayName.lastIndexOf('/'), displayName.lastIndexOf('\\'));
        if (lastSep >= 0) {
            displayName = displayName.substring(lastSep + 1);
        }
        ImGui.text(displayName);
    }
    ImGui.sameLine();
    if (region != null && region.getBounds() != null) {
        Rect b = region.getBounds();
        ImGui.textDisabled((int) b.width + " x " + (int) b.height);
    } else {
        ImGui.textDisabled(tex.getWidth() + " x " + tex.getHeight());
    }
}
```

- [ ] **步骤 5：添加缺失的 import**

在文件顶部的 import 区域添加：

```java
import com.voidvvv.kzcollision.core.model.SourceRegion;
import com.voidvvv.kzcollision.core.model.Rect;
```

- [ ] **步骤 6：编译验证**

运行：`./gradlew :editor:compileJava 2>&1 | tail -10`
预期：BUILD SUCCESSFUL

- [ ] **步骤 7：提交**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/panels/SourceImagePreviewPanel.java
git commit -m "feat(editor): preview individual atlas regions in SourceImagePreviewPanel"
```

---

### 任务 4：修复项目加载时 ATLAS 资产纹理不恢复的问题

**文件：**
- 修改：`editor/src/main/java/com/voidvvv/kzcollision/editor/panels/PanelManager.java`
  - 第 152-164 行：`loadProject()` 方法

**当前行为：** 只有 SINGLE 资产的纹理被重新加载。ATLAS 资产被跳过，导致项目加载后 atlas 纹理丢失。

**新行为：** 同时恢复 ATLAS 资产的纹理。区域数据已序列化在项目 JSON 中，无需重新解析——只需加载 PNG 纹理即可。

- [ ] **步骤 1：修改资产加载循环以支持所有资产类型**

替换 `loadProject()` 中的资产加载循环（第 152-164 行）：

```java
for (SourceAsset asset : project.getSourceAssets()) {
    if (asset.getFilePath() != null) {
        try {
            Texture tex = new Texture(Gdx.files.absolute(asset.getFilePath()));
            state.getTextureCache().put(asset.getFilePath(), tex);
            if (asset.getType() == AssetType.SINGLE && !asset.getRegions().isEmpty()) {
                SourceRegion region = asset.getRegions().get(0);
                if (region.getBounds() == null) {
                    region.setBounds(new Rect(0, 0, tex.getWidth(), tex.getHeight()));
                }
            }
        } catch (Exception e) {
            Gdx.app.log("PanelManager", "Failed to load texture: " + asset.getFilePath(), e);
        }
    }
}
```

关键变化：
- 移除了 `AssetType.SINGLE` 条件守卫——现在为所有资产类型（SINGLE 和 ATLAS）加载纹理
- 对 SINGLE 资产，仍在 bounds 为空时设置默认区域（保持向后兼容）
- 对 ATLAS 资产，区域 bounds 已在序列化数据中保存，无需更新
- 纹理缓存键始终为 `asset.getFilePath()`（PNG 路径），两种类型统一

- [ ] **步骤 2：编译验证**

运行：`./gradlew :editor:compileJava 2>&1 | tail -10`
预期：BUILD SUCCESSFUL

- [ ] **步骤 3：提交**

```bash
git add editor/src/main/java/com/voidvvv/kzcollision/editor/panels/PanelManager.java
git commit -m "fix(editor): reload ATLAS asset textures on project load"
```

---

### 任务 5：完整构建与手动验证

- [ ] **步骤 1：运行完整构建（含测试）**

运行：`./gradlew build 2>&1 | tail -20`
预期：BUILD SUCCESSFUL，所有测试通过。

- [ ] **步骤 2：运行编辑器并手动测试**

运行：`./gradlew :editor:run`

手动测试清单：
1. 点击 "+ Import" → 选择一个 `.atlas` 文件（如 `assets/ui/uiskin.atlas`）
2. 验证每个 atlas 区域在 Source Images 面板中作为**顶层独立项**平铺展示（无折叠分组）
3. 点击某个区域 → 验证 **Source Image Preview** 只显示该区域的裁剪部分，而非整张 atlas 纹理
4. 点击另一个区域 → 验证预览更新为新区域
5. 右键某个区域 → "Add as Frame" → 验证生成了 sprite frame
6. 选中该 sprite frame → 验证 **SpriteFrame Preview** 显示正确的裁剪区域
7. 创建动画、添加帧 → 验证视口中渲染正确
8. 保存项目、关闭、重新打开 → 验证 ATLAS 资产仍然正常显示和预览

- [ ] **步骤 3：如有调整则最终提交**

---

## 自审检查

**1. 需求覆盖：**
- 每个 atlas 区域作为独立图片素材：任务 2（平铺展示）
- 预览只显示区域部分：任务 3（基于 UV 的裁剪渲染）
- 项目加载恢复 ATLAS 资产：任务 4

**2. 占位符扫描：**
- 所有代码块包含完整实现代码——未发现 TBD/TODO/占位符。

**3. 类型一致性：**
- `selectedSourceRegionId` 在 `EditorState` 中为 `String`——在 `SourceImagesPanel` 和 `SourceImagePreviewPanel` 中均作为 `String` 使用
- `findSelectedRegion()` 返回 `SourceRegion`——被 `renderPreview(tex, region)` 和 `renderInfo(asset, tex, region)` 消费，参数类型匹配
- `region.getBounds()` 返回 `Rect`——用于计算 UV 坐标（`float`），与 `SpriteFramePreviewPanel` 的现有模式一致
- `ImGui.selectable(String, boolean)` 匹配 ImGui Java 绑定的选中状态签名
