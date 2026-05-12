package com.voidvvv.kzcollision.core.serialization;

import com.voidvvv.kzcollision.core.model.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class ProjectSerializerRoundTripTest {

    private final ProjectSerializer serializer = new ProjectSerializer();
    private File tempFile;

    @Before
    public void setUp() throws IOException {
        tempFile = File.createTempFile("test_project", ".json");
        tempFile.deleteOnExit();
    }

    @After
    public void tearDown() {
        if (tempFile != null) tempFile.delete();
    }

    @Test
    public void roundTripEmptyProject() throws IOException {
        Project original = new Project("Empty");
        serializer.save(original, tempFile);
        Project loaded = serializer.load(tempFile);

        assertEquals("Empty", loaded.getName());
        assertTrue(loaded.getSourceAssets().isEmpty());
        assertTrue(loaded.getSpriteFrames().isEmpty());
        assertTrue(loaded.getAnimations().isEmpty());
    }

    @Test
    public void roundTripFullProject() throws IOException {
        Project original = new Project("FullTest");

        // Source asset with region
        SourceAsset asset = new SourceAsset();
        asset.setType(AssetType.SINGLE);
        asset.setFilePath("C:/images/sprite.png");
        SourceRegion region = new SourceRegion("sprite.png", asset.getId(), new Rect(0, 0, 128, 64));
        asset.getRegions().add(region);
        original.getSourceAssets().add(asset);

        // Sprite frame with sub-region
        SpriteFrame frame = new SpriteFrame();
        frame.setSourceAssetId(asset.getId());
        frame.setSourceRegionName("sprite.png");
        frame.setSubRegion(new Rect(0, 0, 32, 32));
        frame.setOffsetX(5f);
        frame.setOffsetY(10f);
        original.getSpriteFrames().add(frame);

        // Animation with frame + collision box
        Animation anim = new Animation("walk");
        AnimationFrame animFrame = new AnimationFrame();
        animFrame.setSpriteFrameId(frame.getId());
        animFrame.setDuration(0.15f);
        animFrame.setOriginX(16f);
        animFrame.setOriginY(8f);
        CollisionBox box = new CollisionBox(-8, -8, 16, 16, "body");
        animFrame.getCollisionBoxes().add(box);
        anim.getFrames().add(animFrame);
        original.getAnimations().add(anim);

        serializer.save(original, tempFile);
        Project loaded = serializer.load(tempFile);

        // Project metadata
        assertEquals("FullTest", loaded.getName());

        // Source assets
        assertEquals(1, loaded.getSourceAssets().size());
        SourceAsset loadedAsset = loaded.getSourceAssets().get(0);
        assertEquals("C:/images/sprite.png", loadedAsset.getFilePath());
        assertEquals(AssetType.SINGLE, loadedAsset.getType());
        assertEquals(1, loadedAsset.getRegions().size());
        SourceRegion loadedRegion = loadedAsset.getRegions().get(0);
        assertEquals("sprite.png", loadedRegion.getName());
        assertEquals(0f, loadedRegion.getBounds().x, 0.001f);
        assertEquals(0f, loadedRegion.getBounds().y, 0.001f);
        assertEquals(128f, loadedRegion.getBounds().width, 0.001f);
        assertEquals(64f, loadedRegion.getBounds().height, 0.001f);

        // Sprite frames
        assertEquals(1, loaded.getSpriteFrames().size());
        SpriteFrame loadedFrame = loaded.getSpriteFrames().get(0);
        assertEquals(loadedAsset.getId(), loadedFrame.getSourceAssetId());
        assertEquals("sprite.png", loadedFrame.getSourceRegionName());
        assertEquals(0f, loadedFrame.getSubRegion().x, 0.001f);
        assertEquals(32f, loadedFrame.getSubRegion().width, 0.001f);
        assertEquals(5f, loadedFrame.getOffsetX(), 0.001f);
        assertEquals(10f, loadedFrame.getOffsetY(), 0.001f);

        // Animations
        assertEquals(1, loaded.getAnimations().size());
        Animation loadedAnim = loaded.getAnimations().get(0);
        assertEquals("walk", loadedAnim.getName());
        assertEquals(1, loadedAnim.getFrames().size());

        AnimationFrame loadedAF = loadedAnim.getFrames().get(0);
        assertEquals(0.15f, loadedAF.getDuration(), 0.001f);
        assertEquals(16f, loadedAF.getOriginX(), 0.001f);
        assertEquals(8f, loadedAF.getOriginY(), 0.001f);

        assertEquals(1, loadedAF.getCollisionBoxes().size());
        CollisionBox loadedBox = loadedAF.getCollisionBoxes().get(0);
        assertEquals("body", loadedBox.getLabel());
        assertEquals(-8f, loadedBox.getX(), 0.001f);
        assertEquals(-8f, loadedBox.getY(), 0.001f);
        assertEquals(16f, loadedBox.getWidth(), 0.001f);
        assertEquals(16f, loadedBox.getHeight(), 0.001f);
    }

    @Test
    public void toJsonAndFromJson() throws IOException {
        Project original = new Project("JsonTest");
        String json = serializer.toJson(original);
        Project loaded = serializer.fromJson(json);
        assertEquals("JsonTest", loaded.getName());
    }
}
