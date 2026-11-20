package io.github.pirut.bloodfeud.feud;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

/** Names and epithets for Brutes. All of it written for this mod. */
public final class Names {
    private Names() {
    }

    private static final String[] FIRST = {
            "Grak", "Mol", "Durz", "Skeg", "Bram", "Vosk", "Thul", "Kreb", "Ozz", "Rusk", "Gorm", "Nib", "Hask",
            "Ulg", "Zab", "Prug", "Feld", "Krot", "Mard", "Shug", "Brisk", "Dolt", "Fen", "Grub", "Hob", "Jask",
            "Klem", "Lurk", "Murt", "Nog", "Pell", "Quill", "Rattle", "Snag", "Tusk", "Vell", "Wort", "Yorl",
            "Clod", "Dreg", "Flint", "Grist", "Husk", "Knob", "Lump", "Moss", "Pock", "Scab", "Slag", "Thorn"};
    private static final String[] LAST = {"", "", "", "", "ok", "ug", "ash", "rik", "bo", "nar", "gul", "et", "ard",
            "wick", "en", "o", "le", "y", "um", "ith"};

    // Grunty syllables for made-up names: onset + vowel + coda, sometimes a second syllable.
    private static final String[] ONSET = {"B", "Br", "D", "Dr", "G", "Gr", "Gl", "H", "K", "Kr", "Kl", "M", "N", "Sk",
            "Sn", "Sp", "Str", "Th", "Thr", "V", "Z", "R", "P", "Pr", "Tr", "J", "Y", "W", "Wr", "Gh", "Bl", "Fl", "Sc",
            "Ch", "Sl"};
    private static final String[] VOWEL = {"a", "o", "u", "i", "e", "a", "o", "u", "oo"};
    private static final String[] CODA = {"k", "g", "sk", "rk", "rg", "zz", "b", "m", "n", "rn", "mp", "nd", "st",
            "th", "x", "lg", "rt", "sh", "d", "gg", "tch", "lk", "rb", "z"};
    private static final String[] TAIL = {"", "", "", "", "ok", "ug", "ash", "rik", "bo", "nar", "gul", "et", "ard",
            "wick", "en", "o", "le", "y", "um", "ith", "ax", "ur", "isk", "og", "ma", "da", "rot", "mor", "zug"};
    /** Pieces a made-up name must never contain. */
    private static final Pattern RUDE = Pattern.compile("(?i)shit|fuck|cunt|dick|cock|nig|fag|slut|whor|rape|twat|"
            + "tit|cum|jizz|porn|nazi|kkk|piss|wank|bitch|spic|kike|coon|homo|gay|anal|anus|sex|poo|butt|ass");

    public static String given(Random random, Set<String> taken) {
        for (int i = 0; i < 80; i++) {
            String n = random.nextInt(3) == 0 ? FIRST[random.nextInt(FIRST.length)] + LAST[random.nextInt(LAST.length)]
                    : made(random);
            if (n.length() >= 3 && n.length() <= 10 && !taken.contains(n) && !RUDE.matcher(n).find()) return n;
        }
        return FIRST[random.nextInt(FIRST.length)] + (taken.size() + 1);
    }

    static String made(Random random) {
        String n = ONSET[random.nextInt(ONSET.length)] + VOWEL[random.nextInt(VOWEL.length)] + CODA[random.nextInt(CODA.length)];
        n += TAIL[random.nextInt(TAIL.length)];
        return n.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + n.substring(1).toLowerCase(java.util.Locale.ROOT);
    }

    /** An epithet, preferring one about how it won (if it killed someone) or what it is. */
    public static String epithet(Random random, String entityType, DeathCause cause) {
        return epithet(random, entityType, cause, java.util.Set.of());
    }

    /** The same, never one in {@code taken} (the epithets living Brutes already wear) while any other is left. */
    public static String epithet(Random random, String entityType, DeathCause cause, java.util.Set<String> taken) {
        Talk t = Talk.book();
        List<String> byCause = free(cause == null ? List.of() : t.pool("epithets." + cause.key()), taken);
        if (!byCause.isEmpty() && random.nextInt(3) < 2) return pick(random, byCause);
        List<String> typed = free(t.pool("epithets." + entityType), taken);
        if (!typed.isEmpty() && random.nextInt(3) == 0) return pick(random, typed);
        List<String> general = free(t.pool("epithets.general"), taken);
        if (!general.isEmpty() && random.nextInt(4) > 0) return pick(random, general);
        String made = made(random, taken);
        if (made != null) return made;
        if (!general.isEmpty()) return pick(random, general);
        for (String pool : List.of("epithets." + entityType, cause == null ? "" : "epithets." + cause.key())) {
            List<String> rest = free(t.pool(pool), taken);
            if (!rest.isEmpty()) return pick(random, rest);
        }
        List<String> all = t.pool("epithets.general");
        return all.isEmpty() ? "the Nameless" : pick(random, all);
    }

    /** An epithet for the scar it came back with, or null if every one of them is taken. */
    public static String scarEpithet(Random random, Scar scar, java.util.Set<String> taken) {
        List<String> pool = free(Talk.book().pool("epithets.scar." + scar.key()), taken);
        if (pool.isEmpty()) return taken.contains(scar.epithet) ? null : scar.epithet;
        return pick(random, pool);
    }

    /** "Gravel-Tooth": a made-up epithet from two halves, for when the written ones run out. */
    private static String made(Random random, java.util.Set<String> taken) {
        List<String> first = Talk.book().pool("epithets.first"), second = Talk.book().pool("epithets.second");
        if (first.isEmpty() || second.isEmpty()) return null;
        for (int i = 0; i < 40; i++) {
            String e = pick(random, first) + "-" + pick(random, second);
            if (!taken.contains(e)) return e;
        }
        return null;
    }

    private static List<String> free(List<String> pool, java.util.Set<String> taken) {
        if (taken.isEmpty()) return pool;
        List<String> out = new java.util.ArrayList<>(pool.size());
        for (String e : pool) if (!taken.contains(e)) out.add(e);
        return out;
    }

    private static String pick(Random random, List<String> from) {
        return from.get(random.nextInt(from.size()));
    }
}
