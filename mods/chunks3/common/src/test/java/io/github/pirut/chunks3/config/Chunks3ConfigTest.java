package io.github.pirut.chunks3.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.pirut.chunks3.far.Far;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Chunks3's settings file: what is saved is read back, and the defaults leave the engine as it was. */
class Chunks3ConfigTest {
    @Test void aNewFileHoldsTheDefaultsAndSetsNoSwitch(@TempDir Path dir) {
        Chunks3Config.load(dir);
        assertTrue(Files.isRegularFile(dir.resolve("chunks3-client.properties")));
        assertEquals(Far.DEFAULT_DETAIL, Chunks3Config.FAR_DETAIL.get());
        assertEquals(Chunks3Config.Sharpness.FAST, Chunks3Config.FAR_SHARPNESS.get());
        assertEquals(Far.MAX_LEVEL, Chunks3Config.FAR_TEXTURED_LEVELS.get());
        assertEquals(Chunks3Config.FarSmooth.CUBIC_WORLDS, Chunks3Config.FAR_SMOOTH.get());
        for (EngineToggles.Toggle toggle : EngineToggles.ALL) {
            assertEquals(toggle.defaultValue(), EngineToggles.get(toggle), toggle.key());
            assertNull(System.getProperty(toggle.systemProperty()), toggle.key());
        }
    }

    @Test void whatIsSavedIsReadBackAndApplied(@TempDir Path dir) {
        Chunks3Config.load(dir);
        EngineToggles.Toggle smooth = EngineToggles.ALL.getFirst();
        Chunks3Config.setSmooth(Chunks3Config.FarSmooth.OFF);
        try {
            Chunks3Config.setDetail(20);
            Chunks3Config.setSharpness(Chunks3Config.Sharpness.SHARP);
            Chunks3Config.setToggle(smooth, true);
            Chunks3Config.FAR_DETAIL.set(4);
            Chunks3Config.load(dir);
            assertEquals(20, Chunks3Config.FAR_DETAIL.get());
            assertEquals(20, Far.detail(), "in effect at once");
            assertEquals(Chunks3Config.Sharpness.SHARP, Chunks3Config.FAR_SHARPNESS.get());
            assertEquals(Far.hole(Chunks3Config.Sharpness.SHARP.scale, 20), Far.hole());
            assertEquals("true", System.getProperty(smooth.systemProperty()));
            assertEquals(Chunks3Config.FarSmooth.OFF, Chunks3Config.FAR_SMOOTH.get());
            assertEquals("false", System.getProperty("chunks3.far.smooth"));
        } finally {
            Chunks3Config.setDetail(Far.DEFAULT_DETAIL);
            Chunks3Config.setSharpness(Chunks3Config.Sharpness.FAST);
            Chunks3Config.setToggle(smooth, false);
            Chunks3Config.setSmooth(Chunks3Config.FarSmooth.CUBIC_WORLDS);
            Chunks3Config.load(dir);
        }
        assertNull(System.getProperty(smooth.systemProperty()));
        assertNull(System.getProperty("chunks3.far.smooth"), "the default leaves it unset: Cubic worlds alone");
        assertEquals(Far.DEFAULT_DETAIL, Far.detail());
    }
}
