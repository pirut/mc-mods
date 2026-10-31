package io.github.pirut.ccagent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LuaStringsTest {
    @Test
    void roundTripsUtf8ThroughLuaBytes() {
        var text = "héllo → wörld";
        assertEquals(text, LuaStrings.fromLua(LuaStrings.toLua(text)));
    }

    @Test
    void quotesEscapesAndNonAscii() {
        assertEquals("\"a\\\"b\\\\c\\n\"", LuaStrings.quote("a\"b\\c\n"));
        assertEquals("\"\\195\\169\"", LuaStrings.quote("é"));
        // A control character followed by a digit must not merge into one escape.
        assertEquals("\"\\0012\"", LuaStrings.quote("\u00012"));
    }
}
