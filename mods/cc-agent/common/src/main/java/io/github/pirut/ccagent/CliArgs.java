package io.github.pirut.ccagent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Building the Claude Code / Codex command line. Plain Java, so it's tested without Minecraft. */
final class CliArgs {
    /** Model names, effort levels and session ids: letters, digits and . _ - : @ / [ ] only. */
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._:@/\\[\\]-]{1,128}");
    private static final Pattern WORD = Pattern.compile("\"([^\"]*)\"|(\\S+)");

    private CliArgs() {
    }

    /**
     * Refuses a value that could do more than name a model, effort or session. These come from the computer (any
     * Lua program can set them) or, in multiplayer, from the server, and end up on the command line. On Windows the
     * npm shims run through cmd.exe, which would run "&calc".
     */
    static void check(String what, String value) {
        if (value != null && !SAFE.matcher(value).matches()) throw new ToolException("that " + what + " isn't valid: " + value);
    }

    /** The player's own extra arguments from the config, split on spaces; "double quotes" keep spaces. */
    static List<String> split(String args) {
        var out = new ArrayList<String>();
        if (args == null || args.isBlank()) return out;
        var m = WORD.matcher(args);
        while (m.find()) out.add(m.group(1) != null ? m.group(1) : m.group(2));
        return out;
    }
}
