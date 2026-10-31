package io.github.pirut.mcmods.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CommonLibTest {
    @Test
    void ellipsize() {
        assertEquals("short", CommonLib.ellipsize("short", 10));
        assertEquals("abcdefg...", CommonLib.ellipsize("abcdefghijklmnop", 10));
        assertEquals(null, CommonLib.ellipsize(null, 10));
    }
}
