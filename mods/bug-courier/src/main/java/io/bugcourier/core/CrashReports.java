package io.bugcourier.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads crash reports: which mods they blame, and a short tag that is the same for the same crash. */
public final class CrashReports {
    private static final Pattern FRAME = Pattern.compile("^\\s*at\\s+(?:[^\\s/]+/)*([\\w$.]+)\\.([\\w$<>]+)\\(", Pattern.MULTILINE);
    private static final Pattern SUSPECT = Pattern.compile("\\(([a-z][a-z0-9_]{1,63})\\),\\s*Version:");
    private static final Pattern FRAME_MOD = Pattern.compile("^\\s*at\\s+[A-Z]+/([a-z][a-z0-9_]{1,63})@", Pattern.MULTILINE);
    private static final Set<String> PLATFORM = Set.of("minecraft", "neoforge", "fml", "forge", "java", "mixinextras");

    private CrashReports() {
    }

    /** Six hex digits from the top frames, ignoring line numbers so a rebuild keeps the same tag. */
    public static String signature(String crashText) {
        Matcher m = FRAME.matcher(crashText);
        StringBuilder key = new StringBuilder();
        int frames = 0;
        while (m.find() && frames < 6) {
            key.append(m.group(1)).append('.').append(m.group(2)).append('\n');
            frames++;
        }
        if (frames == 0) return null;
        String exception = firstException(crashText);
        if (exception != null) key.insert(0, exception + '\n');
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 3);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Mod ids the crash points at, most likely first: NeoForge's "Suspected Mods", then mods in the stack. */
    public static List<String> suspectedMods(String crashText) {
        Set<String> ids = new LinkedHashSet<>();
        int at = crashText.indexOf("Suspected Mod");
        if (at >= 0) {
            int end = crashText.indexOf("\nStacktrace:", at);
            if (end < 0) end = Math.min(crashText.length(), at + 2000);
            Matcher s = SUSPECT.matcher(crashText.substring(at, end));
            while (s.find()) ids.add(s.group(1));
        }
        Matcher f = FRAME_MOD.matcher(crashText);
        while (f.find()) ids.add(f.group(1));
        ids.removeAll(PLATFORM);
        return new ArrayList<>(ids);
    }

    private static String firstException(String crashText) {
        for (String line : crashText.split("\\R")) {
            String t = line.strip();
            int colon = t.indexOf(':');
            String head = colon > 0 ? t.substring(0, colon) : t;
            if (head.matches("[\\w$.]+(?:Exception|Error|Throwable)")) return head;
        }
        return null;
    }
}
