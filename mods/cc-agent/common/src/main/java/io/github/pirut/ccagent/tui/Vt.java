package io.github.pirut.ccagent.tui;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * A small VT100/xterm emulator: enough of it for Claude Code's and Codex's TUIs (cursor movement,
 * erase, scroll regions, the alternate screen, SGR colors, a few queries they wait on). Cells keep
 * full colors; {@link Palette} fits them into CC's 16 palette slots as rows are sent.
 */
public final class Vt {
    public static final int DEFAULT_FG = 7;
    public static final int DEFAULT_BG = 0;

    private int cols;
    private int rows;
    private Screen main;
    private Screen alt;
    private Screen screen;

    private int cx, cy;
    private boolean wrapPending;
    private int savedX, savedY;
    private int top, bottom;
    private int fg = DEFAULT_FG, bg = DEFAULT_BG;
    private boolean bold, reverse;
    private boolean autowrap = true;
    private boolean cursorVisible = true;
    private boolean appCursorKeys;
    private boolean bracketedPaste;
    private boolean mouseTracking;
    private boolean sgrMouse;

    private final Consumer<byte[]> reply;
    private final Palette palette = new Palette(this::colorsInUse);

    // Parser state
    private enum State { GROUND, ESC, CSI, OSC, STRING, CHARSET }

    private State state = State.GROUND;
    private final StringBuilder params = new StringBuilder();
    private final StringBuilder osc = new StringBuilder();
    private boolean stringEsc;
    private int utfNeed;
    private int utfCode;

    private static final class Screen {
        int[][] chars;
        int[][] fgs;
        int[][] bgs;
        boolean[] dirty;

        Screen(int cols, int rows) {
            chars = new int[rows][cols];
            fgs = new int[rows][cols];
            bgs = new int[rows][cols];
            dirty = new boolean[rows];
            for (int y = 0; y < rows; y++) clearRow(y, 0, cols, DEFAULT_FG, DEFAULT_BG);
        }

        void clearRow(int y, int from, int to, int fg, int bg) {
            for (int x = from; x < to; x++) {
                chars[y][x] = ' ';
                fgs[y][x] = fg;
                bgs[y][x] = bg;
            }
            dirty[y] = true;
        }
    }

    public Vt(int cols, int rows, Consumer<byte[]> reply) {
        this.cols = Math.max(1, cols);
        this.rows = Math.max(1, rows);
        this.reply = reply;
        main = new Screen(this.cols, this.rows);
        alt = new Screen(this.cols, this.rows);
        screen = main;
        top = 0;
        bottom = this.rows - 1;
    }

    public synchronized int cols() {
        return cols;
    }

    public synchronized int rows() {
        return rows;
    }

    public synchronized int cursorX() {
        return Math.min(cx, cols - 1);
    }

    public synchronized int cursorY() {
        return cy;
    }

    public synchronized boolean cursorVisible() {
        return cursorVisible && view == 0;
    }

    // ---- Scrollback ----

    private static final int MAX_HISTORY = 2000;
    private final java.util.ArrayList<int[][]> history = new java.util.ArrayList<>();
    private int view; // lines scrolled back; 0 shows the live screen
    private boolean viewDirty;

    private void keep(int[] chars, int[] fgs, int[] bgs) {
        history.add(new int[][]{chars.clone(), fgs.clone(), bgs.clone()});
        if (history.size() > MAX_HISTORY) history.remove(0);
        else if (view > 0) view++; // keep the lines being read where they are
        if (view > 0) view = Math.min(view, history.size());
    }

    private void setView(int lines) {
        int next = Math.max(0, Math.min(lines, history.size()));
        if (next == view) return;
        view = next;
        viewDirty = true;
        if (view == 0) java.util.Arrays.fill(screen.dirty, true);
    }

    /** Scroll back (positive) or forward through the scrollback. */
    public synchronized void scrollView(int lines) {
        if (screen == main) setView(view + lines);
    }

    public synchronized boolean scrolledBack() {
        return view > 0;
    }

    public synchronized void scrollToBottom() {
        setView(0);
    }

    public synchronized boolean appCursorKeys() {
        return appCursorKeys;
    }

    public synchronized boolean bracketedPaste() {
        return bracketedPaste;
    }

    public synchronized void resize(int newCols, int newRows) {
        newCols = Math.max(1, newCols);
        newRows = Math.max(1, newRows);
        main = resized(main, newCols, newRows);
        alt = resized(alt, newCols, newRows);
        screen = screen == alt ? alt : main;
        cols = newCols;
        rows = newRows;
        top = 0;
        bottom = rows - 1;
        cx = Math.min(cx, cols - 1);
        cy = Math.min(cy, rows - 1);
        wrapPending = false;
    }

    private Screen resized(Screen old, int newCols, int newRows) {
        var next = new Screen(newCols, newRows);
        // Keep the bottom rows, like a terminal does when it shrinks.
        int shift = Math.max(0, rows - newRows);
        for (int y = 0; y < Math.min(rows, newRows); y++) {
            int from = y + shift;
            for (int x = 0; x < Math.min(cols, newCols); x++) {
                next.chars[y][x] = old.chars[from][x];
                next.fgs[y][x] = old.fgs[from][x];
                next.bgs[y][x] = old.bgs[from][x];
            }
        }
        if (shift > 0) cy = Math.max(0, cy - shift);
        return next;
    }

    /**
     * Rows changed since the last call, as CC blit strings: text bytes, then text and background
     * colors as palette slots. Colors new to the palette go in {@code paletteChanges} (slot to RGB).
     */
    public synchronized java.util.Map<Integer, String[]> takeDirty(java.util.Map<Integer, Integer> paletteChanges) {
        var out = new java.util.LinkedHashMap<Integer, String[]>();
        if (view > 0) {
            // Scrolled back: show scrollback then the screen, and only redraw when the view moves.
            if (!viewDirty) return out;
            viewDirty = false;
            int start = history.size() - view;
            for (int y = 0; y < rows; y++) {
                int line = start + y;
                int[][] row = line < history.size() ? history.get(line)
                    : new int[][]{screen.chars[line - history.size()], screen.fgs[line - history.size()], screen.bgs[line - history.size()]};
                out.put(y, blit(row[0], row[1], row[2], paletteChanges));
            }
            return out;
        }
        viewDirty = false;
        for (int y = 0; y < rows; y++) {
            if (!screen.dirty[y]) continue;
            screen.dirty[y] = false;
            out.put(y, blit(screen.chars[y], screen.fgs[y], screen.bgs[y], paletteChanges));
        }
        return out;
    }

    private String[] blit(int[] chars, int[] fgs, int[] bgs, java.util.Map<Integer, Integer> paletteChanges) {
        {
            var text = new StringBuilder(cols);
            var f = new StringBuilder(cols);
            var b = new StringBuilder(cols);
            for (int x = 0; x < cols; x++) {
                if (x >= chars.length) {
                    text.append(' ');
                    f.append(Character.forDigit(palette.slot(DEFAULT_FG, paletteChanges), 16));
                    b.append(Character.forDigit(palette.slot(DEFAULT_BG, paletteChanges), 16));
                    continue;
                }
                int glyph = Glyphs.cc(chars[x]);
                int fc = palette.slot(fgs[x], paletteChanges);
                int bc = palette.slot(bgs[x], paletteChanges);
                if ((glyph & Glyphs.INVERT) != 0) {
                    int t = fc;
                    fc = bc;
                    bc = t;
                }
                text.append((char) (glyph & 0xFF));
                f.append(Character.forDigit(fc, 16));
                b.append(Character.forDigit(bc, 16));
            }
            return new String[]{text.toString(), f.toString(), b.toString()};
        }
    }

    /** Every palette slot in use, for a screen that was just (re)connected. */
    public synchronized java.util.Map<Integer, Integer> palette() {
        return palette.all();
    }

    /** Colors still on screen, for the palette to know which slots it may reuse. */
    private java.util.Set<Integer> colorsInUse() {
        var used = new java.util.HashSet<Integer>();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                used.add(screen.fgs[y][x]);
                used.add(screen.bgs[y][x]);
            }
        }
        return used;
    }

    public synchronized void markAllDirty() {
        java.util.Arrays.fill(screen.dirty, true);
    }

    public synchronized void feed(byte[] data, int length) {
        for (int i = 0; i < length; i++) feedByte(data[i] & 0xFF);
    }

    private void feedByte(int b) {
        // UTF-8 only matters in the ground state; everything else is ASCII.
        if (state == State.GROUND && b >= 0x80) {
            if (utfNeed > 0 && (b & 0xC0) == 0x80) {
                utfCode = (utfCode << 6) | (b & 0x3F);
                if (--utfNeed == 0) print(utfCode);
                return;
            }
            if ((b & 0xE0) == 0xC0) {
                utfNeed = 1;
                utfCode = b & 0x1F;
            } else if ((b & 0xF0) == 0xE0) {
                utfNeed = 2;
                utfCode = b & 0x0F;
            } else if ((b & 0xF8) == 0xF0) {
                utfNeed = 3;
                utfCode = b & 0x07;
            } else {
                utfNeed = 0;
                print('?');
            }
            return;
        }
        utfNeed = 0;
        switch (state) {
            case GROUND -> ground(b);
            case ESC -> escape(b);
            case CSI -> csiByte(b);
            case OSC -> oscByte(b);
            case STRING -> stringByte(b);
            case CHARSET -> state = State.GROUND;
        }
    }

    private void ground(int b) {
        switch (b) {
            case 0x1B -> state = State.ESC;
            case '\r' -> {
                cx = 0;
                wrapPending = false;
            }
            case '\n', 0x0B, 0x0C -> lineFeed();
            case '\b' -> {
                if (cx > 0) cx--;
                wrapPending = false;
            }
            case '\t' -> {
                cx = Math.min(cols - 1, (cx / 8 + 1) * 8);
                wrapPending = false;
            }
            case 0x07, 0x0E, 0x0F, 0x00 -> {
            }
            default -> {
                if (b >= 0x20) print(b);
            }
        }
    }

    private void print(int code) {
        int width = Glyphs.width(code);
        if (width == 0) return;
        if (wrapPending && autowrap) {
            cx = 0;
            lineFeed();
        }
        wrapPending = false;
        if (cx + width > cols) {
            if (!autowrap) return;
            cx = 0;
            lineFeed();
        }
        put(cx, cy, code);
        if (width == 2 && cx + 1 < cols) put(cx + 1, cy, ' ');
        cx += width;
        if (cx >= cols) {
            cx = cols - 1;
            wrapPending = true;
        }
    }

    private void put(int x, int y, int code) {
        int f = fg, b = bg;
        if (bold && f < 8) f += 8;
        if (reverse) {
            int t = f;
            f = b;
            b = t;
        }
        screen.chars[y][x] = code;
        screen.fgs[y][x] = f;
        screen.bgs[y][x] = b;
        screen.dirty[y] = true;
    }

    private void lineFeed() {
        if (cy == bottom) scrollUp(1);
        else if (cy < rows - 1) cy++;
    }

    private void scrollUp(int n) {
        n = Math.min(n, bottom - top + 1);
        for (int i = 0; i < n; i++) {
            var c = screen.chars[top];
            var f = screen.fgs[top];
            var b = screen.bgs[top];
            // Lines leaving the top of the main screen go to the scrollback, like a terminal.
            if (screen == main && top == 0) keep(c, f, b);
            for (int y = top; y < bottom; y++) {
                screen.chars[y] = screen.chars[y + 1];
                screen.fgs[y] = screen.fgs[y + 1];
                screen.bgs[y] = screen.bgs[y + 1];
            }
            screen.chars[bottom] = c;
            screen.fgs[bottom] = f;
            screen.bgs[bottom] = b;
            screen.clearRow(bottom, 0, cols, DEFAULT_FG, bg);
        }
        for (int y = top; y <= bottom; y++) screen.dirty[y] = true;
    }

    private void scrollDown(int n) {
        n = Math.min(n, bottom - top + 1);
        for (int i = 0; i < n; i++) {
            var c = screen.chars[bottom];
            var f = screen.fgs[bottom];
            var b = screen.bgs[bottom];
            for (int y = bottom; y > top; y--) {
                screen.chars[y] = screen.chars[y - 1];
                screen.fgs[y] = screen.fgs[y - 1];
                screen.bgs[y] = screen.bgs[y - 1];
            }
            screen.chars[top] = c;
            screen.fgs[top] = f;
            screen.bgs[top] = b;
            screen.clearRow(top, 0, cols, DEFAULT_FG, bg);
        }
        for (int y = top; y <= bottom; y++) screen.dirty[y] = true;
    }

    private void escape(int b) {
        state = State.GROUND;
        switch (b) {
            case '[' -> {
                params.setLength(0);
                state = State.CSI;
            }
            case ']' -> {
                osc.setLength(0);
                state = State.OSC;
            }
            case 'P', '_', '^', 'X' -> {
                stringEsc = false;
                state = State.STRING;
            }
            case '(', ')', '*', '+' -> state = State.CHARSET;
            case '7' -> {
                savedX = cx;
                savedY = cy;
            }
            case '8' -> {
                cx = savedX;
                cy = savedY;
                wrapPending = false;
            }
            case 'D' -> lineFeed();
            case 'E' -> {
                cx = 0;
                lineFeed();
            }
            case 'M' -> {
                if (cy == top) scrollDown(1);
                else if (cy > 0) cy--;
            }
            case 'c' -> reset();
            default -> {
            }
        }
    }

    private void reset() {
        main = new Screen(cols, rows);
        alt = new Screen(cols, rows);
        screen = main;
        cx = cy = 0;
        top = 0;
        bottom = rows - 1;
        fg = DEFAULT_FG;
        bg = DEFAULT_BG;
        bold = reverse = false;
        autowrap = true;
        cursorVisible = true;
    }

    private void oscByte(int b) {
        if (b == 0x07) {
            endOsc();
        } else if (b == 0x1B) {
            stringEsc = true;
        } else if (stringEsc && b == '\\') {
            stringEsc = false;
            endOsc();
        } else {
            stringEsc = false;
            if (osc.length() < 4096) osc.append((char) b);
        }
    }

    private void endOsc() {
        state = State.GROUND;
        var text = osc.toString();
        // Color queries: answer black background, light text, so TUIs pick their dark themes.
        if (text.equals("11;?")) send("\u001b]11;rgb:0000/0000/0000\u001b\\");
        else if (text.equals("10;?")) send("\u001b]10;rgb:cccc/cccc/cccc\u001b\\");
    }

    private void stringByte(int b) {
        if (b == 0x1B) stringEsc = true;
        else if (stringEsc && b == '\\') state = State.GROUND;
        else if (b == 0x07) state = State.GROUND;
        else stringEsc = false;
    }

    private void csiByte(int b) {
        if (b >= 0x40 && b <= 0x7E) {
            state = State.GROUND;
            csi((char) b, params.toString());
        } else if (params.length() < 256) {
            params.append((char) b);
        }
    }

    private static int[] numbers(String text) {
        if (text.isEmpty()) return new int[0];
        var parts = text.split("[;:]", -1);
        var out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = parts[i].isEmpty() ? 0 : Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                out[i] = 0;
            }
        }
        return out;
    }

    private static int arg(int[] p, int i, int fallback) {
        return i < p.length && p[i] != 0 ? p[i] : fallback;
    }

    private void csi(char fin, String raw) {
        char prefix = !raw.isEmpty() && "?>=<".indexOf(raw.charAt(0)) >= 0 ? raw.charAt(0) : 0;
        var body = prefix != 0 ? raw.substring(1) : raw;
        String intermediate = "";
        while (!body.isEmpty() && body.charAt(body.length() - 1) >= 0x20 && body.charAt(body.length() - 1) <= 0x2F) {
            intermediate = body.charAt(body.length() - 1) + intermediate;
            body = body.substring(0, body.length() - 1);
        }
        if (!intermediate.isEmpty()) return; // DECSCUSR (cursor shape) and friends
        var p = numbers(body);
        if (prefix == '?') {
            if (fin == 'h' || fin == 'l') privateMode(p, fin == 'h');
            return;
        }
        if (prefix != 0) {
            if (prefix == '>' && fin == 'c') send("\u001b[>0;10;1c");
            return;
        }
        int n = arg(p, 0, 1);
        switch (fin) {
            case 'A' -> moveTo(cx, Math.max(cy - n, cy >= top ? top : 0));
            case 'B', 'e' -> moveTo(cx, Math.min(cy + n, cy <= bottom ? bottom : rows - 1));
            case 'C', 'a' -> moveTo(cx + n, cy);
            case 'D' -> moveTo(cx - n, cy);
            case 'E' -> moveTo(0, cy + n);
            case 'F' -> moveTo(0, cy - n);
            case 'G', '`' -> moveTo(n - 1, cy);
            case 'd' -> moveTo(cx, n - 1);
            case 'H', 'f' -> moveTo(arg(p, 1, 1) - 1, arg(p, 0, 1) - 1);
            case 'J' -> eraseDisplay(arg(p, 0, 0));
            case 'K' -> eraseLine(arg(p, 0, 0));
            case 'X' -> screen.clearRow(cy, cx, Math.min(cols, cx + n), DEFAULT_FG, bg);
            case '@' -> insertChars(n);
            case 'P' -> deleteChars(n);
            case 'L' -> insertLines(n);
            case 'M' -> deleteLines(n);
            case 'S' -> scrollUp(n);
            case 'T' -> scrollDown(n);
            case 'r' -> {
                int t = arg(p, 0, 1) - 1, b = arg(p, 1, rows) - 1;
                if (t < b && b < rows) {
                    top = t;
                    bottom = b;
                    moveTo(0, 0);
                }
            }
            case 'm' -> sgr(p);
            case 's' -> {
                savedX = cx;
                savedY = cy;
            }
            case 'u' -> {
                cx = savedX;
                cy = savedY;
            }
            case 'n' -> {
                if (arg(p, 0, 0) == 6) send("\u001b[" + (cy + 1) + ";" + (Math.min(cx, cols - 1) + 1) + "R");
                else if (arg(p, 0, 0) == 5) send("\u001b[0n");
            }
            case 'c' -> send("\u001b[?62;22c");
            case 't' -> {
                if (arg(p, 0, 0) == 18) send("\u001b[8;" + rows + ";" + cols + "t");
            }
            default -> {
            }
        }
    }

    private void privateMode(int[] p, boolean on) {
        for (int mode : p) {
            switch (mode) {
                case 1 -> appCursorKeys = on;
                case 7 -> autowrap = on;
                case 25 -> cursorVisible = on;
                case 47, 1047, 1049 -> {
                    if (mode == 1049 && on) {
                        savedX = cx;
                        savedY = cy;
                    }
                    var target = on ? alt : main;
                    if (on && screen != alt) {
                        alt = new Screen(cols, rows);
                        target = alt;
                    }
                    screen = target;
                    markAllDirty();
                    if (mode == 1049 && !on) {
                        cx = savedX;
                        cy = savedY;
                    }
                }
                case 2004 -> bracketedPaste = on;
                case 1000, 1002, 1003 -> mouseTracking = on;
                case 1006 -> sgrMouse = on;
                default -> {
                }
            }
        }
    }

    private void moveTo(int x, int y) {
        cx = Math.max(0, Math.min(cols - 1, x));
        cy = Math.max(0, Math.min(rows - 1, y));
        wrapPending = false;
    }

    private void eraseDisplay(int mode) {
        switch (mode) {
            case 0 -> {
                screen.clearRow(cy, Math.min(cx, cols), cols, DEFAULT_FG, bg);
                for (int y = cy + 1; y < rows; y++) screen.clearRow(y, 0, cols, DEFAULT_FG, bg);
            }
            case 1 -> {
                for (int y = 0; y < cy; y++) screen.clearRow(y, 0, cols, DEFAULT_FG, bg);
                screen.clearRow(cy, 0, Math.min(cx + 1, cols), DEFAULT_FG, bg);
            }
            default -> {
                for (int y = 0; y < rows; y++) screen.clearRow(y, 0, cols, DEFAULT_FG, bg);
                if (mode == 3) {
                    history.clear();
                    setView(0);
                }
            }
        }
    }

    private void eraseLine(int mode) {
        switch (mode) {
            case 0 -> screen.clearRow(cy, Math.min(cx, cols), cols, DEFAULT_FG, bg);
            case 1 -> screen.clearRow(cy, 0, Math.min(cx + 1, cols), DEFAULT_FG, bg);
            default -> screen.clearRow(cy, 0, cols, DEFAULT_FG, bg);
        }
    }

    private void insertChars(int n) {
        var row = screen.chars[cy];
        var f = screen.fgs[cy];
        var b = screen.bgs[cy];
        for (int x = cols - 1; x >= cx + n; x--) {
            row[x] = row[x - n];
            f[x] = f[x - n];
            b[x] = b[x - n];
        }
        screen.clearRow(cy, cx, Math.min(cols, cx + n), DEFAULT_FG, bg);
    }

    private void deleteChars(int n) {
        var row = screen.chars[cy];
        var f = screen.fgs[cy];
        var b = screen.bgs[cy];
        for (int x = cx; x < cols; x++) {
            int from = x + n;
            row[x] = from < cols ? row[from] : ' ';
            f[x] = from < cols ? f[from] : DEFAULT_FG;
            b[x] = from < cols ? b[from] : bg;
        }
        screen.dirty[cy] = true;
    }

    private void insertLines(int n) {
        if (cy < top || cy > bottom) return;
        int savedTop = top;
        top = cy;
        scrollDown(n);
        top = savedTop;
    }

    private void deleteLines(int n) {
        if (cy < top || cy > bottom) return;
        int savedTop = top;
        top = cy;
        scrollUp(n);
        top = savedTop;
    }

    private void sgr(int[] p) {
        if (p.length == 0) p = new int[]{0};
        for (int i = 0; i < p.length; i++) {
            int c = p[i];
            switch (c) {
                case 0 -> {
                    fg = DEFAULT_FG;
                    bg = DEFAULT_BG;
                    bold = reverse = false;
                }
                case 1 -> bold = true;
                case 22 -> bold = false;
                case 7 -> reverse = true;
                case 27 -> reverse = false;
                case 39 -> fg = DEFAULT_FG;
                case 49 -> bg = DEFAULT_BG;
                case 38, 48 -> {
                    int color = -1;
                    if (i + 1 < p.length && p[i + 1] == 5 && i + 2 < p.length) {
                        color = Glyphs.xterm256(p[i + 2]);
                        i += 2;
                    } else if (i + 1 < p.length && p[i + 1] == 2 && i + 4 < p.length) {
                        color = Glyphs.RGB | (p[i + 2] & 0xFF) << 16 | (p[i + 3] & 0xFF) << 8 | p[i + 4] & 0xFF;
                        i += 4;
                    }
                    if (color >= 0) {
                        if (c == 38) fg = color;
                        else bg = color;
                    }
                }
                default -> {
                    if (c >= 30 && c <= 37) fg = c - 30;
                    else if (c >= 40 && c <= 47) bg = c - 40;
                    else if (c >= 90 && c <= 97) fg = c - 90 + 8;
                    else if (c >= 100 && c <= 107) bg = c - 100 + 8;
                }
            }
        }
    }

    private void send(String text) {
        reply.accept(text.getBytes(StandardCharsets.UTF_8));
    }

    /** Bytes a terminal sends for a key, given CC's GLFW key code and which modifiers are held. */
    public byte[] key(int code, boolean ctrl, boolean alt, boolean shift) {
        String seq = switch (code) {
            case 257, 335 -> "\r";
            case 259 -> "\u007f";
            case 258 -> shift ? "\u001b[Z" : "\t";
            case 256 -> "\u001b";
            case 265 -> cursor('A', ctrl, alt, shift);
            case 264 -> cursor('B', ctrl, alt, shift);
            case 262 -> cursor('C', ctrl, alt, shift);
            case 263 -> cursor('D', ctrl, alt, shift);
            case 268 -> modified("H", ctrl, alt, shift);
            case 269 -> modified("F", ctrl, alt, shift);
            case 266 -> tilde(5, ctrl, alt, shift);
            case 267 -> tilde(6, ctrl, alt, shift);
            case 261 -> tilde(3, ctrl, alt, shift);
            case 260 -> "\u001b[2~";
            case 290 -> "\u001bOP";
            case 291 -> "\u001bOQ";
            case 292 -> "\u001bOR";
            case 293 -> "\u001bOS";
            default -> null;
        };
        if (seq == null && (ctrl || alt) && code >= 32 && code < 127) {
            char ch = (char) code; // GLFW letters are upper case ASCII
            if (ctrl) {
                if (ch >= 'A' && ch <= 'Z') seq = String.valueOf((char) (ch - 'A' + 1));
                else if (ch == ' ' || ch == '2') seq = "\u0000";
                else if (ch == '[') seq = "\u001b";
                else if (ch == '\\') seq = "\u001c";
                else if (ch == ']') seq = "\u001d";
            } else {
                seq = String.valueOf(Character.toLowerCase(ch));
            }
            if (seq != null && alt) seq = "\u001b" + seq;
        }
        if (seq == null && alt && code == 257) seq = "\u001b\r";
        return seq == null ? null : seq.getBytes(StandardCharsets.UTF_8);
    }

    private static int modifiers(boolean ctrl, boolean alt, boolean shift) {
        return 1 + (shift ? 1 : 0) + (alt ? 2 : 0) + (ctrl ? 4 : 0);
    }

    /** Home and End: ESC [ H, or ESC [ 1 ; mod H with modifiers (Ctrl+End jumps to the bottom in Claude Code). */
    private static String modified(String fin, boolean ctrl, boolean alt, boolean shift) {
        int mod = modifiers(ctrl, alt, shift);
        return mod > 1 ? "\u001b[1;" + mod + fin : "\u001b[" + fin;
    }

    private static String tilde(int n, boolean ctrl, boolean alt, boolean shift) {
        int mod = modifiers(ctrl, alt, shift);
        return mod > 1 ? "\u001b[" + n + ";" + mod + "~" : "\u001b[" + n + "~";
    }

    private String cursor(char dir, boolean ctrl, boolean alt, boolean shift) {
        int mod = 1 + (shift ? 1 : 0) + (alt ? 2 : 0) + (ctrl ? 4 : 0);
        if (mod > 1) return "\u001b[1;" + mod + dir;
        return (appCursorKeys() ? "\u001bO" : "\u001b[") + dir;
    }

    /** Whether the program asked for mouse events (Claude Code's full screen scrolls and clicks with them). */
    public synchronized boolean mouseTracking() {
        return mouseTracking;
    }

    /**
     * A mouse event as the program asked for it. button 0 = left, 64/65 = wheel up/down; x, y from 0.
     * Null when the program doesn't track the mouse.
     */
    public synchronized byte[] mouse(int button, int x, int y, boolean press) {
        if (!mouseTracking) return null;
        x = Math.max(0, Math.min(x, cols - 1)) + 1;
        y = Math.max(0, Math.min(y, rows - 1)) + 1;
        if (sgrMouse) return ("\u001b[<" + button + ";" + x + ";" + y + (press ? "M" : "m")).getBytes(StandardCharsets.UTF_8);
        // X10 encoding: release is button 3; coordinates offset by 32.
        int b = (press ? button : 3) + 32;
        return new byte[]{0x1b, '[', 'M', (byte) b, (byte) Math.min(255, x + 32), (byte) Math.min(255, y + 32)};
    }

    public byte[] paste(String text) {
        var out = new ByteArrayOutputStream();
        if (bracketedPaste()) out.writeBytes("\u001b[200~".getBytes(StandardCharsets.UTF_8));
        out.writeBytes(text.replace("\r\n", "\r").replace('\n', '\r').getBytes(StandardCharsets.UTF_8));
        if (bracketedPaste()) out.writeBytes("\u001b[201~".getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }
}
