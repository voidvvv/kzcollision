package com.voidvvv.kzcollision.editor.project;

import com.voidvvv.kzcollision.core.model.AssetType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class AssetIndexBuilderTest {
    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void build_shouldIndexImagesAtlasRegionsAndSkipCollision() throws Exception {
        File assets = temp.newFolder();
        File sprites = new File(assets, "sprites");
        File collision = new File(assets, "collision");
        Assert.assertTrue(sprites.mkdirs());
        Assert.assertTrue(collision.mkdirs());
        Assert.assertTrue(new File(sprites, "hero.png").createNewFile());
        Assert.assertTrue(new File(collision, "collision.json").createNewFile());

        File atlas = new File(sprites, "enemies.atlas");
        Files.write(atlas.toPath(), atlasText().getBytes(StandardCharsets.UTF_8));

        AssetIndex index = new AssetIndexBuilder().build(assets);

        Assert.assertNotNull(index.findByInternalPath("sprites/hero.png"));
        Assert.assertNull(index.findByInternalPath("collision/collision.json"));

        AssetRecord atlasRecord = index.findByInternalPath("sprites/enemies.atlas");
        Assert.assertNotNull(atlasRecord);
        Assert.assertEquals(AssetType.ATLAS, atlasRecord.getType());
        Assert.assertNotNull(atlasRecord.findRegion("slime_0"));
        Assert.assertEquals(16f, atlasRecord.findRegion("slime_0").getBounds().width, 0.001f);

        List<AssetRecord> byName = index.findByFileName("hero.png");
        Assert.assertEquals(1, byName.size());
        Assert.assertEquals("sprites/hero.png", byName.get(0).getInternalPath());
    }

    private static String atlasText() {
        return "enemies.png\n"
                + "size: 64,64\n"
                + "format: RGBA8888\n"
                + "filter: Nearest,Nearest\n"
                + "repeat: none\n"
                + "slime\n"
                + "  rotate: false\n"
                + "  xy: 4, 8\n"
                + "  size: 16, 12\n"
                + "  orig: 16, 12\n"
                + "  offset: 0, 0\n"
                + "  index: 0\n";
    }
}
