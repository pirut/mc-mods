package io.github.pirut.bloodfeud.feud;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The book of lines, read from {@code /bloodfeud/lines.txt}: pools of lines by section, with {word} blanks filled
 * from what happened. Each speaker skips lines it said lately, so a pool only repeats once it has run through.
 */
public final class Talk {
    private static final Pattern BLANK = Pattern.compile("\\{([a-z]+)}");
    private static final int MEMORY = 48;
    private static Talk book;

    private final Map<String, List<String>> pools;
    private final Map<UUID, Deque<String>> said = new HashMap<>();

    Talk(Map<String, List<String>> pools) {
        this.pools = pools;
    }

    /** The lines bundled with the mod. */
    public static synchronized Talk book() {
        if (book == null) {
            try (InputStream in = Talk.class.getResourceAsStream("/bloodfeud/lines.txt")) {
                book = new Talk(in == null ? Map.of() : parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
            } catch (IOException e) {
                book = new Talk(Map.of());
            }
        }
        return book;
    }

    static Map<String, List<String>> parse(String text) {
        Map<String, List<String>> pools = new LinkedHashMap<>();
        List<String> current = null;
        try (BufferedReader r = new BufferedReader(new java.io.StringReader(text))) {
            for (String line; (line = r.readLine()) != null; ) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("[") && line.endsWith("]")) {
                    current = pools.computeIfAbsent(line.substring(1, line.length() - 1), k -> new ArrayList<>());
                } else if (current != null) {
                    current.add(line);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return pools;
    }

    public List<String> pool(String section) {
        return pools.getOrDefault(section, List.of());
    }

    public int size() {
        int n = 0;
        for (List<String> p : pools.values()) n += p.size();
        return n;
    }

    public Map<String, List<String>> pools() {
        return pools;
    }

    /**
     * A line from the first sections (more than one mixes them), with its blanks filled from {@code words}. Lines
     * with a blank that has no word are skipped; lines this speaker said lately are skipped while others remain.
     * Returns null if no section has a usable line.
     */
    public String line(Random random, UUID speaker, Map<String, String> words, String... sections) {
        List<String> usable = new ArrayList<>();
        for (String s : sections) for (String l : pool(s)) if (fillable(l, words)) usable.add(l);
        if (usable.isEmpty()) return null;
        Deque<String> recent = speaker == null ? new ArrayDeque<>() : said.computeIfAbsent(speaker, k -> new ArrayDeque<>());
        List<String> fresh = new ArrayList<>(usable);
        fresh.removeAll(recent);
        String pick = (fresh.isEmpty() ? usable : fresh).get(random.nextInt(fresh.isEmpty() ? usable.size() : fresh.size()));
        if (speaker != null) {
            recent.remove(pick);
            recent.addLast(pick);
            while (recent.size() > MEMORY) recent.removeFirst();
        }
        return fill(pick, words);
    }

    /** Forgets what a speaker said (when it dies for good). */
    public void forget(UUID speaker) {
        said.remove(speaker);
    }

    static boolean fillable(String line, Map<String, String> words) {
        Matcher m = BLANK.matcher(line);
        while (m.find()) {
            String w = words.get(m.group(1));
            if (w == null || w.isEmpty()) return false;
        }
        return true;
    }

    static String fill(String line, Map<String, String> words) {
        Matcher m = BLANK.matcher(line);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String w = words.get(m.group(1));
            // A blank that starts the sentence gets a capital: "{days} ago" -> "Three days ago".
            if (m.start() == 0 || line.charAt(m.start() - 1) == ' ' && m.start() >= 2 && ".!?".indexOf(line.charAt(m.start() - 2)) >= 0) {
                w = Character.toUpperCase(w.charAt(0)) + w.substring(1);
            }
            m.appendReplacement(out, Matcher.quoteReplacement(w));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static final String[] SMALL = {"no", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
            "ten", "eleven", "twelve"};

    /** "three", or "14" past twelve. */
    public static String count(int n) {
        return n >= 0 && n < SMALL.length ? SMALL[n] : Integer.toString(n);
    }

    /** "a day", "three days", "a long time" for game days. */
    public static String days(long days) {
        if (days <= 0) return null;
        if (days == 1) return "a day";
        if (days > 60) return "a long time";
        return count((int) days) + " days";
    }
}
