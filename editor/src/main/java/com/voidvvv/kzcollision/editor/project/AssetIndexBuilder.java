package com.voidvvv.kzcollision.editor.project;

import com.voidvvv.kzcollision.core.AtlasParser;
import com.voidvvv.kzcollision.core.model.AssetType;
import com.voidvvv.kzcollision.core.model.AtlasRegionDescriptor;
import com.voidvvv.kzcollision.core.model.Rect;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class AssetIndexBuilder {
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList("png", "jpg", "jpeg"));

    public AssetIndex build(File assetsRoot) throws IOException {
        if (assetsRoot == null) {
            throw new IllegalArgumentException("assetsRoot must not be null");
        }
        if (!assetsRoot.isDirectory()) {
            throw new IllegalArgumentException("assetsRoot is not a directory: " + assetsRoot.getAbsolutePath());
        }
        File canonicalRoot = assetsRoot.getCanonicalFile();
        AssetIndex index = new AssetIndex(assetsRoot);
        scan(canonicalRoot, canonicalRoot, index);
        return index;
    }

    private void scan(File canonicalRoot, File current, AssetIndex index) throws IOException {
        File[] files = current.listFiles();
        if (files == null) return;
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            if (file.isDirectory()) {
                if ("collision".equalsIgnoreCase(file.getName())) continue;
                scan(canonicalRoot, file, index);
                continue;
            }
            String ext = extension(file.getName());
            String internalPath = toInternalPath(canonicalRoot, file);
            if (IMAGE_EXTENSIONS.contains(ext)) {
                AssetRecord record = new AssetRecord(AssetType.SINGLE, internalPath, file);
                record.addRegion(new AssetRegionRecord(file.getName(), null));
                index.add(record);
            } else if ("atlas".equals(ext)) {
                AssetRecord record = new AssetRecord(AssetType.ATLAS, internalPath, file);
                for (AtlasRegionDescriptor descriptor : AtlasParser.parse(file)) {
                    String name = descriptor.getIndex() >= 0
                            ? descriptor.getName() + "_" + descriptor.getIndex()
                            : descriptor.getName();
                    record.addRegion(new AssetRegionRecord(name, new Rect(descriptor.getX(), descriptor.getY(),
                            descriptor.getWidth(), descriptor.getHeight())));
                }
                index.add(record);
            }
        }
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String toInternalPath(File canonicalRoot, File file) throws IOException {
        return AssetIndex.normalize(canonicalRoot.toPath()
                .relativize(file.getCanonicalFile().toPath()).toString());
    }
}
