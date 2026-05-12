package com.voidvvv.kzcollision.core;

import com.voidvvv.kzcollision.core.model.AtlasRegionDescriptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AtlasParser {

    public static List<AtlasRegionDescriptor> parse(File atlasFile) throws IOException {
        List<AtlasRegionDescriptor> regions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(atlasFile), StandardCharsets.UTF_8))) {

            String line = skipBlankLines(reader);

            while (line != null) {
                // line is the page image filename
                line = reader.readLine(); // size: W,H
                line = reader.readLine(); // format: ...
                line = reader.readLine(); // filter: ...
                line = reader.readLine(); // repeat: ...

                line = reader.readLine();
                while (line != null && !line.trim().isEmpty()) {
                    String regionName = line.trim();
                    line = readRegion(reader, regionName, regions);
                }

                line = skipBlankLines(reader);
            }
        }

        return regions;
    }

    private static String skipBlankLines(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                return line;
            }
        }
        return null;
    }

    private static String readRegion(BufferedReader reader, String name,
                                      List<AtlasRegionDescriptor> regions) throws IOException {
        boolean rotate = false;
        int x = 0, y = 0, width = 0, height = 0;
        int origWidth = 0, origHeight = 0;
        int offsetX = 0, offsetY = 0;
        int index = -1;

        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || (!trimmed.startsWith("rotate") && !trimmed.startsWith("xy")
                    && !trimmed.startsWith("size") && !trimmed.startsWith("orig")
                    && !trimmed.startsWith("offset") && !trimmed.startsWith("index"))) {
                break;
            }

            String[] parts = trimmed.split(": ", 2);
            if (parts.length < 2) continue;
            String key = parts[0];
            String value = parts[1];

            switch (key) {
                case "rotate":
                    rotate = Boolean.parseBoolean(value);
                    break;
                case "xy":
                    int[] xy = parseIntPair(value);
                    x = xy[0];
                    y = xy[1];
                    break;
                case "size":
                    int[] sz = parseIntPair(value);
                    width = sz[0];
                    height = sz[1];
                    break;
                case "orig":
                    int[] orig = parseIntPair(value);
                    origWidth = orig[0];
                    origHeight = orig[1];
                    break;
                case "offset":
                    int[] off = parseIntPair(value);
                    offsetX = off[0];
                    offsetY = off[1];
                    break;
                case "index":
                    index = Integer.parseInt(value.trim());
                    break;
            }
        }

        regions.add(new AtlasRegionDescriptor(name, index, x, y, width, height,
                origWidth, origHeight, offsetX, offsetY, rotate));

        return line;
    }

    private static int[] parseIntPair(String value) {
        String[] parts = value.split(",");
        return new int[]{
                Integer.parseInt(parts[0].trim()),
                Integer.parseInt(parts[1].trim())
        };
    }
}
