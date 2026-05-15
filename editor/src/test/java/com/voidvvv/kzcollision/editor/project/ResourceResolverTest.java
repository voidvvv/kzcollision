package com.voidvvv.kzcollision.editor.project;

import com.voidvvv.kzcollision.core.model.AssetType;
import com.voidvvv.kzcollision.core.model.Project;
import com.voidvvv.kzcollision.core.model.Rect;
import com.voidvvv.kzcollision.core.model.SourceAsset;
import com.voidvvv.kzcollision.core.model.SourceRegion;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class ResourceResolverTest {
    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void resolve_shouldMatchExistingInternalPath() throws Exception {
        File assets = temp.newFolder();
        File hero = new File(assets, "hero.png");
        Assert.assertTrue(hero.createNewFile());
        AssetIndex index = new AssetIndexBuilder().build(assets);
        Project project = projectWithSingleAsset("hero.png");

        ResourceRecoveryReport report = new ResourceResolver().resolve(project, index);

        Assert.assertEquals(ResourceStatus.MATCHED, report.getStatus(project.getSourceAssets().get(0).getId()));
        Assert.assertEquals(1, report.getMatchedCount());
        Assert.assertEquals("hero.png", project.getSourceAssets().get(0).getInternalPath());
    }

    @Test
    public void resolve_shouldRepairUniqueMovedFileByName() throws Exception {
        File assets = temp.newFolder();
        File sprites = new File(assets, "sprites");
        Assert.assertTrue(sprites.mkdirs());
        Assert.assertTrue(new File(sprites, "hero.png").createNewFile());
        AssetIndex index = new AssetIndexBuilder().build(assets);
        Project project = projectWithSingleAsset("old/hero.png");

        ResourceRecoveryReport report = new ResourceResolver().resolve(project, index);

        Assert.assertEquals(ResourceStatus.REPAIRED, report.getStatus(project.getSourceAssets().get(0).getId()));
        Assert.assertEquals(1, report.getRepairedCount());
        Assert.assertEquals("sprites/hero.png", project.getSourceAssets().get(0).getInternalPath());
    }

    @Test
    public void resolve_shouldLeaveConflictWhenMultipleCandidatesExist() throws Exception {
        File assets = temp.newFolder();
        File a = new File(assets, "a");
        File b = new File(assets, "b");
        Assert.assertTrue(a.mkdirs());
        Assert.assertTrue(b.mkdirs());
        Assert.assertTrue(new File(a, "hero.png").createNewFile());
        Assert.assertTrue(new File(b, "hero.png").createNewFile());
        AssetIndex index = new AssetIndexBuilder().build(assets);
        Project project = projectWithSingleAsset("old/hero.png");

        ResourceRecoveryReport report = new ResourceResolver().resolve(project, index);

        String assetId = project.getSourceAssets().get(0).getId();
        Assert.assertEquals(ResourceStatus.CONFLICT, report.getStatus(assetId));
        Assert.assertEquals(1, report.getConflictCount());
        Assert.assertEquals(2, report.getCandidates(assetId).size());
        Assert.assertEquals("old/hero.png", project.getSourceAssets().get(0).getInternalPath());
    }

    @Test
    public void resolve_shouldPreserveMissingResource() throws Exception {
        File assets = temp.newFolder();
        AssetIndex index = new AssetIndexBuilder().build(assets);
        Project project = projectWithSingleAsset("missing/hero.png");

        ResourceRecoveryReport report = new ResourceResolver().resolve(project, index);

        Assert.assertEquals(ResourceStatus.MISSING, report.getStatus(project.getSourceAssets().get(0).getId()));
        Assert.assertEquals(1, report.getMissingCount());
        Assert.assertEquals("missing/hero.png", project.getSourceAssets().get(0).getInternalPath());
        Assert.assertEquals(1, project.getSourceAssets().size());
    }

    @Test
    public void resolve_shouldReturnEmptyReportForEmptyProject() throws Exception {
        File assets = temp.newFolder();
        AssetIndex index = new AssetIndexBuilder().build(assets);
        Project project = new Project("Empty");

        ResourceRecoveryReport report = new ResourceResolver().resolve(project, index);

        Assert.assertEquals(0, report.getMatchedCount());
        Assert.assertEquals(0, report.getRepairedCount());
        Assert.assertEquals(0, report.getMissingCount());
        Assert.assertEquals(0, report.getConflictCount());
    }

    private static Project projectWithSingleAsset(String internalPath) {
        Project project = new Project("Test");
        SourceAsset asset = new SourceAsset();
        asset.setType(AssetType.SINGLE);
        asset.setInternalPath(internalPath);
        asset.getRegions().add(new SourceRegion("hero.png", asset.getId(), new Rect(0, 0, 16, 16)));
        project.getSourceAssets().add(asset);
        return project;
    }
}
