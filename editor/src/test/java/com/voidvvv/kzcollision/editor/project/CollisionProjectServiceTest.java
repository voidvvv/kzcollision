package com.voidvvv.kzcollision.editor.project;

import com.voidvvv.kzcollision.core.model.Project;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class CollisionProjectServiceTest {
    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void openProjectRoot_shouldResolveAssetsAndDefaultCollisionFile() throws Exception {
        File root = temp.newFolder();
        File assets = new File(root, "assets");
        Assert.assertTrue(assets.mkdirs());

        CollisionProjectService service = new CollisionProjectService();
        EditorProjectContext context = service.openProjectRoot(root);

        Assert.assertEquals(root.getCanonicalFile(), context.getProjectRoot().getCanonicalFile());
        Assert.assertEquals(assets.getCanonicalFile(), context.getAssetsRoot().getCanonicalFile());
        Assert.assertEquals(new File(assets, "collision/collision.json").getCanonicalFile(),
                context.getCollisionFile().getCanonicalFile());
        Assert.assertTrue(context.isOpenedFromProjectRoot());
    }

    @Test
    public void openAssetsFolder_shouldUseFolderAsAssetsRoot() throws Exception {
        File assets = temp.newFolder();

        CollisionProjectService service = new CollisionProjectService();
        EditorProjectContext context = service.openAssetsFolder(assets);

        Assert.assertNull(context.getProjectRoot());
        Assert.assertEquals(assets.getCanonicalFile(), context.getAssetsRoot().getCanonicalFile());
        Assert.assertEquals(new File(assets, "collision/collision.json").getCanonicalFile(),
                context.getCollisionFile().getCanonicalFile());
        Assert.assertFalse(context.isOpenedFromProjectRoot());
    }

    @Test
    public void save_shouldCreateCollisionDirectoryAndWriteJson() throws Exception {
        File assets = temp.newFolder();
        CollisionProjectService service = new CollisionProjectService();
        EditorProjectContext context = service.openAssetsFolder(assets);

        Project project = new Project("Saved");
        service.save(project, context.getCollisionFile());

        Assert.assertTrue(context.getCollisionFile().isFile());
        Project loaded = service.loadOrCreate(context.getCollisionFile(), "Fallback");
        Assert.assertEquals("Saved", loaded.getName());
    }

    @Test
    public void loadOrCreate_shouldCreateEmptyProjectWhenFileIsMissing() throws Exception {
        File assets = temp.newFolder();
        CollisionProjectService service = new CollisionProjectService();
        EditorProjectContext context = service.openAssetsFolder(assets);

        Project project = service.loadOrCreate(context.getCollisionFile(), "Untitled");

        Assert.assertEquals("Untitled", project.getName());
        Assert.assertTrue(project.getSourceAssets().isEmpty());
        Assert.assertFalse(context.getCollisionFile().exists());
    }
}
