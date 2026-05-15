package com.voidvvv.kzcollision.editor.project;

import com.voidvvv.kzcollision.core.model.Project;
import com.voidvvv.kzcollision.core.serialization.ProjectSerializer;

import java.io.File;
import java.io.IOException;

public class CollisionProjectService {
    public static final String DEFAULT_COLLISION_PATH = "collision/collision.json";

    private final ProjectSerializer serializer;

    public CollisionProjectService() {
        this(new ProjectSerializer());
    }

    public CollisionProjectService(ProjectSerializer serializer) {
        this.serializer = serializer;
    }

    public EditorProjectContext openProjectRoot(File projectRoot) {
        if (projectRoot == null) {
            throw new IllegalArgumentException("projectRoot must not be null");
        }
        File assetsRoot = new File(projectRoot, "assets");
        if (!assetsRoot.isDirectory()) {
            throw new IllegalArgumentException("Project root does not contain an assets directory: "
                    + projectRoot.getAbsolutePath());
        }
        return new EditorProjectContext(projectRoot, assetsRoot, defaultCollisionFile(assetsRoot), true);
    }

    public EditorProjectContext openAssetsFolder(File assetsRoot) {
        if (assetsRoot == null) {
            throw new IllegalArgumentException("assetsRoot must not be null");
        }
        if (!assetsRoot.isDirectory()) {
            throw new IllegalArgumentException("Assets folder does not exist: " + assetsRoot.getAbsolutePath());
        }
        return new EditorProjectContext(null, assetsRoot, defaultCollisionFile(assetsRoot), false);
    }

    public EditorProjectContext withCollisionFile(EditorProjectContext context, File collisionFile) {
        return new EditorProjectContext(context.getProjectRoot(), context.getAssetsRoot(),
                collisionFile, context.isOpenedFromProjectRoot());
    }

    public Project loadOrCreate(File collisionFile, String fallbackName) throws IOException {
        if (!collisionFile.isFile()) {
            return new Project(fallbackName);
        }
        return serializer.load(collisionFile);
    }

    public void save(Project project, File collisionFile) throws IOException {
        File parent = collisionFile.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Failed to create collision directory: " + parent.getAbsolutePath());
        }
        serializer.save(project, collisionFile);
    }

    private File defaultCollisionFile(File assetsRoot) {
        return new File(assetsRoot, DEFAULT_COLLISION_PATH);
    }
}
