package io.github.pirut.ccagent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The last few hundred tool calls agents made, for the in-game agentlog program. */
public final class ActivityLog {
    public record Entry(int id, String time, String tool, String computer, String text, boolean ok) {
    }

    private static final int MAX = 500;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final ArrayDeque<Entry> ENTRIES = new ArrayDeque<>();
    private static int nextId = 1;

    private ActivityLog() {
    }

    public static synchronized void add(String tool, String computer, String text, boolean ok) {
        ENTRIES.addLast(new Entry(nextId++, LocalTime.now().format(TIME), tool, computer, text, ok));
        while (ENTRIES.size() > MAX) ENTRIES.removeFirst();
    }

    /** Entries after the given id, as Lua tables. */
    public static synchronized List<Map<String, Object>> after(int id) {
        var out = new ArrayList<Map<String, Object>>();
        for (var e : ENTRIES) {
            if (e.id() <= id) continue;
            Map<String, Object> row = new HashMap<>();
            row.put("id", e.id());
            row.put("time", e.time());
            row.put("tool", e.tool());
            if (e.computer() != null) row.put("computer", LuaStrings.toLua(e.computer()));
            row.put("text", LuaStrings.toLua(e.text()));
            row.put("ok", e.ok());
            out.add(row);
        }
        return out;
    }
}
