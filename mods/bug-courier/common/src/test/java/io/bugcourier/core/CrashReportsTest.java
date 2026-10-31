package io.bugcourier.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CrashReportsTest {
    private static final String CRASH = """
            ---- Minecraft Crash Report ----
            Description: Rendering screen

            java.lang.NullPointerException: Cannot invoke "Object.hashCode()"
            \tat TRANSFORMER/earthplatform@0.1.0-alpha.65/io.earthplatform.far.FarRenderer.draw(FarRenderer.java:812)
            \tat TRANSFORMER/minecraft@1.21.1/net.minecraft.client.renderer.LevelRenderer.renderLevel(LevelRenderer.java:1200)
            \tat TRANSFORMER/sodium@0.8.13/net.caffeinemc.Foo.bar(Foo.java:10)

            -- Head --
            Thread: Render thread
            Suspected Mods:\s
            \tEarth Platform (earthplatform), Version: 0.1.0-alpha.65
            \t\tIssue tracker URL: https://github.com/pirut/earth-platform/issues
            Stacktrace:
            \tat TRANSFORMER/earthplatform@0.1.0-alpha.65/io.earthplatform.far.FarRenderer.draw(FarRenderer.java:812)
            """;

    @Test
    void blamesSuspectedModFirst() {
        assertEquals(List.of("earthplatform", "sodium"), CrashReports.suspectedMods(CRASH));
    }

    @Test
    void signatureIgnoresLineNumbers() {
        String moved = CRASH.replace("FarRenderer.java:812", "FarRenderer.java:830");
        assertEquals(CrashReports.signature(CRASH), CrashReports.signature(moved));
        assertEquals(6, CrashReports.signature(CRASH).length());
        assertNotEquals(CrashReports.signature(CRASH), CrashReports.signature(CRASH.replace("draw(", "drawWater(")));
    }
}
