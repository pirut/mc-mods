package io.github.pirut.ccagent;

import java.nio.charset.StandardCharsets;

/**
 * CC: Tweaked maps Lua's byte strings to Java strings one byte per char (ISO-8859-1). These turn
 * UTF-8 text from an agent into the same bytes on the computer, and back.
 */
public final class LuaStrings {
    private LuaStrings() {
    }

    public static String toLua(String text) {
        return new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }

    public static String fromLua(String bytes) {
        return bytes == null ? null : new String(bytes.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }

    /** A Lua string literal holding exactly this UTF-8 text. */
    public static String quote(String text) {
        var bytes = text.getBytes(StandardCharsets.UTF_8);
        var out = new StringBuilder(bytes.length + 2).append('"');
        for (var b : bytes) {
            int c = b & 0xFF;
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 32 || c >= 127) out.append('\\').append(c);
                    else out.append((char) c);
                }
            }
        }
        return out.append('"').toString();
    }
}
