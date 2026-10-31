package io.github.pirut.ccagent.tui;

/**
 * Maps Unicode to CC: Tweaked's font: printable ASCII and Latin-1 as they are, CP437 symbols in 1-31,
 * and the teletext sextants in 128-159 for block and line drawing. {@link #PALETTE} is the RGB of the 16
 * ANSI colors.
 */
public final class Glyphs {
    /** Set on a {@link #cc} result when the cell's text and background colors must be swapped. */
    public static final int INVERT = 0x100;

    /** The ANSI colors 0-15. */
    public static final int[] PALETTE = {
        0x0c0c0c, 0xcd3131, 0x0dbc79, 0xe5e510, 0x2472c8, 0xbc3fbc, 0x11a8cd, 0xcccccc,
        0x666666, 0xf14c4c, 0x23d18b, 0xf5f543, 0x3b8eea, 0xd670d6, 0x29b8db, 0xf2f2f2,
    };

    private Glyphs() {
    }

    /** Cells a code point takes: 0 for combining marks, 2 for wide CJK and emoji, else 1. */
    public static int width(int cp) {
        if (cp == 0x200B || cp == 0x200C || cp == 0x200D || cp == 0x2060 || cp == 0xFEFF) return 0;
        if ((cp >= 0x0300 && cp <= 0x036F) || (cp >= 0x1AB0 && cp <= 0x1AFF) || (cp >= 0x20D0 && cp <= 0x20FF)
            || (cp >= 0xFE00 && cp <= 0xFE0F) || (cp >= 0xFE20 && cp <= 0xFE2F) || (cp >= 0xE0100 && cp <= 0xE01EF)
            || (cp >= 0x1F3FB && cp <= 0x1F3FF)) {
            return 0;
        }
        if ((cp >= 0x1100 && cp <= 0x115F) || (cp >= 0x2E80 && cp <= 0x303E) || (cp >= 0x3041 && cp <= 0x33FF)
            || (cp >= 0x3400 && cp <= 0x4DBF) || (cp >= 0x4E00 && cp <= 0x9FFF) || (cp >= 0xA000 && cp <= 0xA4CF)
            || (cp >= 0xAC00 && cp <= 0xD7A3) || (cp >= 0xF900 && cp <= 0xFAFF) || (cp >= 0xFE30 && cp <= 0xFE4F)
            || (cp >= 0xFF00 && cp <= 0xFF60) || (cp >= 0xFFE0 && cp <= 0xFFE6)
            || (cp >= 0x1F300 && cp <= 0x1F64F) || (cp >= 0x1F900 && cp <= 0x1F9FF)
            || (cp >= 0x1F680 && cp <= 0x1F6FF) || (cp >= 0x1FA70 && cp <= 0x1FAFF)
            || (cp >= 0x20000 && cp <= 0x3FFFD)) {
            return 2;
        }
        return 1;
    }

    // Sextant pixels: top-left, top-right, middle-left, middle-right, bottom-left, bottom-right.
    private static int sextant(int bits) {
        if ((bits & 32) != 0) return INVERT | (128 + (~bits & 31));
        return 128 + bits;
    }

    // Quadrants drawn as sextants: the top half covers the top and middle rows, the bottom half the
    // middle and bottom rows, so the halves look the same size.
    private static int quadrant(boolean tl, boolean tr, boolean bl, boolean br) {
        int bits = 0;
        if (tl) bits |= 1 | 4;
        if (tr) bits |= 2 | 8;
        if (bl) bits |= 4 | 16;
        if (br) bits |= 8 | 32;
        return sextant(bits);
    }

    /** CC's character for a code point (0-255), possibly with {@link #INVERT}. */
    public static int cc(int cp) {
        if (cp >= 0x20 && cp < 0x7F) return cp;
        if (cp >= 0xA0 && cp <= 0xFF) return cp;
        return switch (cp) {
            // Lines: horizontals through the middle row, verticals down the left column.
            case '─', '━', '═', '╌', '┄', '┈', '╴', '╶' -> sextant(4 | 8);
            case '│', '┃', '║', '╎', '┆', '┊', '┤', '┫', '╡', '╢', '╣' -> sextant(1 | 4 | 16);
            case '┌', '╭', '┏', '╔', '┬', '┳', '╦' -> sextant(4 | 8 | 16);
            case '┐', '╮', '┓', '╗' -> sextant(4 | 16);
            case '└', '╰', '┗', '╚', '┴', '┻', '╩', '⎿' -> sextant(1 | 4 | 8);
            case '┘', '╯', '┛', '╝' -> sextant(1 | 4);
            case '├', '┣', '╠', '┼', '╋', '╬' -> sextant(1 | 4 | 8 | 16);
            // Blocks
            case '█' -> sextant(63);
            case '▀', '▔' -> quadrant(true, true, false, false);
            case '▄', '▁', '▂', '▃' -> quadrant(false, false, true, true);
            case '▌', '▍', '▎', '▏' -> quadrant(true, false, true, false);
            case '▐', '▕' -> quadrant(false, true, false, true);
            case '▘' -> quadrant(true, false, false, false);
            case '▝' -> quadrant(false, true, false, false);
            case '▖' -> quadrant(false, false, true, false);
            case '▗' -> quadrant(false, false, false, true);
            case '▛' -> quadrant(true, true, true, false);
            case '▜' -> quadrant(true, true, false, true);
            case '▙' -> quadrant(true, false, true, true);
            case '▟' -> quadrant(false, true, true, true);
            case '▚' -> quadrant(true, false, false, true);
            case '▞' -> quadrant(false, true, true, false);
            case '▅', '▆', '▇' -> sextant(4 | 8 | 16 | 32);
            case '░', '▒', '▓' -> 127;
            // Symbols in CC's CP437 range
            case '●', '•', '⏺', '∙', '◉', '⦿', '▪', '◆', '♦' -> 7;
            case '○', '◯', '◦', '□', '☐' -> 9;
            case '◙', '■', '◼', '▣', '☑', '☒' -> 8;
            case '▶', '►', '▸', '⏵', '⯈' -> 16;
            case '◀', '◄', '◂', '⏴' -> 17;
            case '↕', '⇕' -> 18;
            case '↑', '⇡', '⬆' -> 24;
            case '↓', '⇣', '⬇' -> 25;
            case '→', '⇒', '⟶', '➜', '➔' -> 26;
            case '←', '⇐', '⟵', '↵', '⏎' -> 27;
            case '↔', '⇔' -> 29;
            case '▲', '△', '⏶' -> 30;
            case '▼', '▽', '⏷' -> 31;
            case '♪', '♫' -> 13;
            case '☼', '✺' -> 15;
            // Close enough in ASCII
            case '✻', '✳', '✶', '✢', '✽', '✱', '✷', '✸', '✹', '∗', '⁎' -> '*';
            case '·', '⋅' -> 0xB7;
            case '›', '❯', '❱', '〉', '⟩', '»' -> '>';
            case '‹', '❮', '❰', '〈', '⟨' -> '<';
            case '‘', '’', '‚', '′' -> '\'';
            case '“', '”', '„', '″' -> '"';
            case '–', '—', '―', '−', '‐' -> '-';
            case '…', '⋯' -> '.';
            case '✓', '✔' -> 'v';
            case '✗', '✘', '✕', '✖', '×' -> 'x';
            case '⚠', '‼' -> '!';
            case '⧉', '⎘' -> '#';
            case '≈' -> '~';
            case '≤' -> '<';
            case '≥' -> '>';
            case '≠' -> '#';
            case '⌘' -> '#';
            case '⏸' -> '"';
            case ' ' -> ' ';
            default -> cp < 0x20 ? ' ' : '?';
        };
    }

    /** Marks a cell color as 24-bit RGB; colors without it are ANSI indexes 0-15. */
    public static final int RGB = 1 << 24;

    private static final int[] CUBE = {0, 95, 135, 175, 215, 255};

    /** An xterm 256-color index as a cell color. */
    public static int xterm256(int index) {
        if (index < 0 || index > 255) return Vt.DEFAULT_FG;
        if (index < 16) return index;
        if (index < 232) {
            int i = index - 16;
            return RGB | CUBE[i / 36 % 6] << 16 | CUBE[i / 6 % 6] << 8 | CUBE[i % 6];
        }
        int level = 8 + 10 * (index - 232);
        return RGB | level << 16 | level << 8 | level;
    }

    /** A cell color's RGB. */
    public static int rgb(int color) {
        return (color & RGB) != 0 ? color & 0xFFFFFF : PALETTE[color & 15];
    }

    /** Perceptual ("redmean") distance squared between two colors. */
    public static long distance(int a, int b) {
        int ar = a >> 16 & 0xFF, ag = a >> 8 & 0xFF, ab = a & 0xFF;
        int br = b >> 16 & 0xFF, bg = b >> 8 & 0xFF, bb = b & 0xFF;
        long mean = (ar + br) / 2;
        long dr = ar - br, dg = ag - bg, db = ab - bb;
        return ((512 + mean) * dr * dr >> 8) + 4 * dg * dg + ((767 - mean) * db * db >> 8);
    }
}
