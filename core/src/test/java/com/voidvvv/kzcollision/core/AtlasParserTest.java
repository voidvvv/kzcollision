package com.voidvvv.kzcollision.core;

import com.voidvvv.kzcollision.core.model.AtlasRegionDescriptor;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static org.junit.Assert.*;

public class AtlasParserTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File createAtlasFile(String content) throws IOException {
        File f = tmp.newFile("test.atlas");
        try (PrintWriter w = new PrintWriter(f, "UTF-8")) {
            w.print(content);
        }
        return f;
    }

    @Test
    public void parsesSingleRegion() throws IOException {
        String atlas = "\n" +
            "test.png\n" +
            "size: 256,256\n" +
            "format: RGBA8888\n" +
            "filter: Nearest,Nearest\n" +
            "repeat: none\n" +
            "hero-idle\n" +
            "  rotate: false\n" +
            "  xy: 10, 20\n" +
            "  size: 32, 48\n" +
            "  orig: 32, 48\n" +
            "  offset: 0, 0\n" +
            "  index: -1\n";

        File f = createAtlasFile(atlas);
        List<AtlasRegionDescriptor> regions = AtlasParser.parse(f);

        assertEquals(1, regions.size());
        AtlasRegionDescriptor r = regions.get(0);
        assertEquals("hero-idle", r.getName());
        assertEquals(-1, r.getIndex());
        assertEquals(10, r.getX());
        assertEquals(20, r.getY());
        assertEquals(32, r.getWidth());
        assertEquals(48, r.getHeight());
        assertEquals(32, r.getOrigWidth());
        assertEquals(48, r.getOrigHeight());
        assertEquals(0, r.getOffsetX());
        assertEquals(0, r.getOffsetY());
        assertFalse(r.isRotate());
    }

    @Test
    public void parsesMultipleRegions() throws IOException {
        String atlas = "\n" +
            "sprites.png\n" +
            "size: 512,512\n" +
            "format: RGBA8888\n" +
            "filter: Linear,Linear\n" +
            "repeat: none\n" +
            "walk-0\n" +
            "  rotate: false\n" +
            "  xy: 0, 0\n" +
            "  size: 64, 64\n" +
            "  orig: 64, 64\n" +
            "  offset: 0, 0\n" +
            "  index: -1\n" +
            "walk-1\n" +
            "  rotate: false\n" +
            "  xy: 64, 0\n" +
            "  size: 64, 64\n" +
            "  orig: 64, 64\n" +
            "  offset: 0, 0\n" +
            "  index: -1\n";

        File f = createAtlasFile(atlas);
        List<AtlasRegionDescriptor> regions = AtlasParser.parse(f);

        assertEquals(2, regions.size());
        assertEquals("walk-0", regions.get(0).getName());
        assertEquals(0, regions.get(0).getX());
        assertEquals("walk-1", regions.get(1).getName());
        assertEquals(64, regions.get(1).getX());
    }

    @Test
    public void parsesIndexedRegion() throws IOException {
        String atlas = "\n" +
            "sheet.png\n" +
            "size: 256,256\n" +
            "format: RGBA8888\n" +
            "filter: Nearest,Nearest\n" +
            "repeat: none\n" +
            "explosion\n" +
            "  rotate: false\n" +
            "  xy: 0, 0\n" +
            "  size: 32, 32\n" +
            "  orig: 32, 32\n" +
            "  offset: 0, 0\n" +
            "  index: 3\n";

        File f = createAtlasFile(atlas);
        List<AtlasRegionDescriptor> regions = AtlasParser.parse(f);

        assertEquals(1, regions.size());
        assertEquals("explosion", regions.get(0).getName());
        assertEquals(3, regions.get(0).getIndex());
    }

    @Test
    public void parsesRotatedRegion() throws IOException {
        String atlas = "\n" +
            "sheet.png\n" +
            "size: 256,256\n" +
            "format: RGBA8888\n" +
            "filter: Nearest,Nearest\n" +
            "repeat: none\n" +
            "corner\n" +
            "  rotate: true\n" +
            "  xy: 100, 50\n" +
            "  size: 16, 32\n" +
            "  orig: 32, 16\n" +
            "  offset: 8, 0\n" +
            "  index: -1\n";

        File f = createAtlasFile(atlas);
        List<AtlasRegionDescriptor> regions = AtlasParser.parse(f);

        assertEquals(1, regions.size());
        assertTrue(regions.get(0).isRotate());
        assertEquals(100, regions.get(0).getX());
        assertEquals(50, regions.get(0).getY());
        assertEquals(16, regions.get(0).getWidth());
        assertEquals(32, regions.get(0).getHeight());
        assertEquals(8, regions.get(0).getOffsetX());
    }

    @Test(expected = IOException.class)
    public void throwsOnMissingFile() throws IOException {
        File missing = new File("/nonexistent/path.atlas");
        AtlasParser.parse(missing);
    }

    @Test
    public void parsesRealUiskinAtlas() throws IOException {
        File atlas = new File("assets/ui/uiskin.atlas");
        if (!atlas.exists()) {
            return; // skip if not running from project root
        }
        List<AtlasRegionDescriptor> regions = AtlasParser.parse(atlas);
        assertFalse("Should parse at least one region", regions.isEmpty());
        AtlasRegionDescriptor first = regions.get(0);
        assertNotNull(first.getName());
        assertTrue(first.getWidth() > 0);
        assertTrue(first.getHeight() > 0);
    }
}
