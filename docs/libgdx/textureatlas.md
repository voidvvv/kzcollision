在 libGDX 中，TextureAtlas 是用于高效管理和加载多个纹理（如游戏精灵、UI 图片等）的核心工具。它通过将多个小图打包进一张大图（纹理图集），减少 GPU 绘制调用次数，从而提升性能并节省内存。

---

核心概念与使用方式

- TextureAtlas 类位于 `com.badlogic.gdx.graphics.g2d.TextureAtlas`。
- 它依赖于两个文件：
  - `.png`（或 `.jpg` 等）：包含所有子图的大图。
  - `.atlas`：文本文件，记录每个子图在大图中的位置、大小、偏移等信息。
- 使用时需将这两个文件放入项目 `assets/` 目录下。

---

基本使用步骤

1. 打包图片为 TextureAtlas  
   可使用以下任一方式：
   - TexturePacker 工具（推荐）：[libgdx-texturepacker-gui](https://code.google.com/archive/p/libgdx-texturepacker-gui/)（需手动下载）或命令行工具。
   - 命令行示例（Windows）：
     ```bash
     java -cp gdx.jar;extensions/gdx-tools/gdx-tools.jar com.badlogic.gdx.tools.texturepacker.TexturePacker "input_dir" "output_dir" "pack_name"
     ```

2. 在代码中加载与使用
   ```java
   // 加载 TextureAtlas
   TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("images.atlas"));

   // 获取单个区域（按名称）
   TextureRegion region = atlas.findRegion("sprite_name");

   // 获取多个同名区域（需按特定命名规则）
   Array<AtlasRegion> regions = atlas.findRegions("group_name");

   // 创建 Sprite 或 Animation
   Sprite sprite = new Sprite(region);
   Animation animation = new Animation(0.1f, atlas.getRegions(), Animation.PlayMode.LOOP);
   ```

---

关键注意事项

- `findRegions()` 的匹配机制  
  该方法不是前缀匹配，而是精确字符串匹配。若要批量获取一组图（如 `A_0.png`, `A_1.png`），必须在 `.atlas` 文件中将它们的 `name` 字段设为相同（如 `A`），索引（`index`）从 0 开始递增。否则无法通过 `findRegions("A")` 获取全部 。

- 命名规范  
  若希望自动分组，图片应命名为 `baseName_index.png`（如 `walk_0.png`, `walk_1.png`），TexturePacker 会自动将 `baseName` 作为统一名称，并分配 `index` 。

- 性能优化建议
  - 尽量在启动时一次性加载所有所需 `TextureAtlas`。
  - 使用 `SpriteBatch` 批量绘制共享同一 Atlas 的精灵。
  - 调用 `atlas.dispose()` 释放资源 。

- 内存管理  
  `TextureAtlas` 实现了 `Disposable` 接口，使用完毕后务必调用 `dispose()` 避免内存泄漏 。

---

常见问题

- Q：为什么 `findRegions("A")` 返回空？  
  A：因为 `.atlas` 文件中没有 `name` 为 `"A"` 的条目。检查生成的 `.atlas` 文件，确认是否按 `name_index` 格式命名并正确打包 。

- Q：能否重复绘制同一 Atlas 中的纹理？  
  A：可以，但需通过多个 `Sprite` 或多次 `batch.draw()` 实现，TextureAtlas 本身不提供“重复”功能 。

- Q：是否必须使用 2 的幂尺寸图片？  
  A：不需要。现代 libGDX + TexturePacker 已支持任意尺寸，无需手动填充为 2^n 。

---

参考资料
- [libGDX 官方文档 - TextureAtlas](https://libgdx.com/wiki/graphics/2d/textures/textureatlas)
- [TexturePacker GUI 下载（CSDN）](http://download.csdn.net/detail/xietansheng/9235265) 
- [高效纹理 Atlas 实践（2024）](https://blog.csdn.net/weixin_34547628/article/details/143102328) 