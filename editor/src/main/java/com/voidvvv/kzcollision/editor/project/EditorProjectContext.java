package com.voidvvv.kzcollision.editor.project;

import java.io.File;
import java.util.Objects;

public class EditorProjectContext {
    private final File projectRoot;
    private final File assetsRoot;
    private final File collisionFile;
    private final boolean openedFromProjectRoot;

    public EditorProjectContext(File projectRoot, File assetsRoot, File collisionFile,
                                boolean openedFromProjectRoot) {
        this.projectRoot = projectRoot;               // nullable by design
        this.assetsRoot = Objects.requireNonNull(assetsRoot, "assetsRoot must not be null");
        this.collisionFile = Objects.requireNonNull(collisionFile, "collisionFile must not be null");
        this.openedFromProjectRoot = openedFromProjectRoot;
    }

    public File getProjectRoot() { return projectRoot; }
    public File getAssetsRoot() { return assetsRoot; }
    public File getCollisionFile() { return collisionFile; }
    public boolean isOpenedFromProjectRoot() { return openedFromProjectRoot; }
}
