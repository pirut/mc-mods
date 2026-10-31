package io.github.pirut.mcmods.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PropertiesFileTest {
    @TempDir
    Path dir;

    private final List<Path> errors = new ArrayList<>();

    @Test
    void missingFileIsEmpty() {
        assertTrue(PropertiesFile.read(dir.resolve("none.properties"), (p, e) -> errors.add(p)).isEmpty());
        assertTrue(errors.isEmpty());
    }

    @Test
    void roundTrip() {
        Path file = dir.resolve("sub/state.properties");
        Properties p = new Properties();
        p.setProperty("name", "Grüße");
        p.setProperty("path", "C:\\Users\\me");
        assertTrue(PropertiesFile.write(file, p, "test", (f, e) -> errors.add(f)));
        Properties back = PropertiesFile.read(file, (f, e) -> errors.add(f));
        assertEquals("Grüße", back.getProperty("name"));
        assertEquals("C:\\Users\\me", back.getProperty("path"));
        assertTrue(errors.isEmpty());
    }

    @Test
    void handWrittenText() throws IOException {
        Path file = dir.resolve("config.properties");
        assertTrue(PropertiesFile.writeText(file, "# hi\npath=" + PropertiesFile.escape("C:\\x") + "\nport= 12 \non=TRUE\n",
                (f, e) -> errors.add(f)));
        Properties p = PropertiesFile.read(file, (f, e) -> errors.add(f));
        assertEquals("C:\\x", p.getProperty("path"));
        assertEquals(12, PropertiesFile.integer(p, "port", 1));
        assertEquals(5, PropertiesFile.integer(p, "missing", 5));
        assertTrue(PropertiesFile.bool(p, "on", false));
        assertFalse(PropertiesFile.bool(p, "missing", false));
        assertEquals("x", PropertiesFile.string(p, "missing", "x"));
        try (var files = Files.list(dir)) {
            assertEquals(1, files.count(), "no temp files left behind");
        }
    }
}
