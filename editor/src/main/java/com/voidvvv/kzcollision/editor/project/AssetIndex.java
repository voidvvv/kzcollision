package com.voidvvv.kzcollision.editor.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AssetIndex {
    private final File assetsRoot;
    private final List<AssetRecord> records = new ArrayList<>();
    private final Map<String, AssetRecord> byInternalPath = new HashMap<>();
    private final Map<String, List<AssetRecord>> byFileName = new HashMap<>();

    public AssetIndex(File assetsRoot) {
        this.assetsRoot = assetsRoot;
    }

    public File getAssetsRoot() { return assetsRoot; }
    public List<AssetRecord> getRecords() { return Collections.unmodifiableList(records); }

    void add(AssetRecord record) {
        records.add(record);
        String key = normalize(record.getInternalPath());
        if (byInternalPath.containsKey(key)) {
            throw new IllegalStateException("Duplicate internal path: " + key);
        }
        byInternalPath.put(key, record);
        String fileName = new File(record.getInternalPath()).getName().toLowerCase(Locale.ROOT);
        byFileName.computeIfAbsent(fileName, k -> new ArrayList<>()).add(record);
    }

    public AssetRecord findByInternalPath(String internalPath) {
        if (internalPath == null) return null;
        return byInternalPath.get(normalize(internalPath));
    }

    public List<AssetRecord> findByFileName(String fileName) {
        if (fileName == null) return Collections.emptyList();
        List<AssetRecord> found = byFileName.get(fileName.toLowerCase(Locale.ROOT));
        return found == null ? Collections.emptyList() : Collections.unmodifiableList(found);
    }

    public static String normalize(String path) {
        return path.replace('\\', '/');
    }
}
