package com.voidvvv.kzcollision.core.serialization;

import com.google.gson.Gson;
import com.voidvvv.kzcollision.core.model.Project;

import java.io.*;

public class ProjectSerializer {

    private final Gson gson;

    public ProjectSerializer() {
        this.gson = ProjectGsonFactory.create();
    }

    public void save(Project project, File file) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(project, writer);
        }
    }

    public Project load(File file) throws IOException {
        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, Project.class);
        }
    }

    public String toJson(Project project) {
        return gson.toJson(project);
    }

    public Project fromJson(String json) {
        return gson.fromJson(json, Project.class);
    }
}
