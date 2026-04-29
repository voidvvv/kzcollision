package com.voidvvv.kzcollision.sdk;

import com.badlogic.gdx.files.FileHandle;
import com.voidvvv.kzcollision.core.model.Project;
import com.voidvvv.kzcollision.core.serialization.ProjectSerializer;

import java.io.File;
import java.io.IOException;

public class KZCollisionLoader {

    public static KZCollisionData load(FileHandle file) {
        return loadFromFile(file.file());
    }

    public static KZCollisionData loadFromFile(File file) {
        ProjectSerializer serializer = new ProjectSerializer();
        try {
            Project project = serializer.load(file);
            return new KZCollisionData(project);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load collision data: " + file.getPath(), e);
        }
    }
}
