package io.github.pirut.bloodfeud.feud;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalkTest {
    static final Set<String> WORDS = Set.of("player", "name", "times", "pack", "home", "king", "item", "weapon", "days",
            "scar", "kin");

    @Test
    void everyPoolTheGameAsksForHasLines() {
        Talk t = Talk.book();
        for (String s : List.of("greet.saw_kills", "greet.killed_you", "greet.killed_you_often", "greet.fled",
                "greet.stole", "greet.it_fled", "greet.hunted", "returned.any", "greet.king", "greet.boss",
                "greet.brute", "greet.rotten", "greet.bones", "greet.illagers", "greet.nether", "killed_player",
                "killed_player.often", "hurt", "winning", "falling", "falling.final", "rose.killed_you", "rose.saw",
                "flee", "drive_off", "stole", "epithets.general")) {
            assertTrue(t.pool(s).size() >= 3, s + " has " + t.pool(s).size());
        }
        for (Family f : Family.values()) assertTrue(t.pool("greet." + f.name().toLowerCase()).size() >= 6, f.name());
        for (DeathCause c : DeathCause.values()) {
            if (c != DeathCause.OTHER) assertFalse(t.pool("epithets." + c.key()).isEmpty(), c.key());
        }
        for (Scar sc : Scar.values()) assertTrue(t.pool("epithets.scar." + sc.key()).size() >= 6, sc.key());
        assertTrue(t.size() > 400, "lines: " + t.size());
    }

    @Test
    void everyBlankIsAWordTheGameFillsIn() {
        Pattern blank = Pattern.compile("\\{([^}]*)}");
        for (Map.Entry<String, List<String>> e : Talk.book().pools().entrySet()) {
            for (String l : e.getValue()) {
                Matcher m = blank.matcher(l);
                while (m.find()) assertTrue(WORDS.contains(m.group(1)), e.getKey() + ": " + l);
            }
        }
    }

    @Test
    void aBruteDoesntRepeatItselfUntilItRunsOut() {
        Talk t = Talk.book();
        Map<String, String> w = new HashMap<>(Map.of("player", "pirutship", "kin", "kin"));
        UUID me = UUID.randomUUID();
        int usable = (int) t.pool("greet.saw_kills").stream().filter(l -> Talk.fillable(l, w)).count();
        Set<String> heard = new HashSet<>();
        Random r = new Random(1);
        for (int i = 0; i < usable; i++) assertTrue(heard.add(t.line(r, me, w, "greet.saw_kills")), "repeated after " + i);
    }

    @Test
    void blanksFillAndCapitalize() {
        assertEquals("Three days ago. Then you.", Talk.fill("{days} ago. Then you.", Map.of("days", "three days")));
        assertEquals("Hi, Vosk.", Talk.fill("Hi, {name}.", Map.of("name", "Vosk")));
        assertEquals(null, Talk.book().line(new Random(), null, Map.of(), "no.such.pool"));
    }

    @Test
    void namesAreManyAndClean() {
        Random r = new Random(2);
        Set<String> names = new HashSet<>();
        for (int i = 0; i < 20_000; i++) {
            String n = Names.given(r, Set.of());
            assertNotNull(n);
            assertTrue(n.length() >= 3 && n.length() <= 10, n);
            names.add(n);
        }
        assertTrue(names.size() > 8_000, "distinct names: " + names.size());
    }
}
