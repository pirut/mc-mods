package io.github.pirut.bloodfeud.warband;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {
    private static Board fresh(long seed) {
        Board b = new Board();
        b.generate(new Random(seed), 0, 0, 0);
        return b;
    }

    @Test
    void generatesFiveWarlordsWithGuards() {
        Board b = fresh(1);
        for (int i = 0; i < Board.WARLORDS; i++) {
            assertNotNull(b.warlord(i));
            assertEquals(Board.GUARDS_PER_WARLORD, b.guardsOf(i).size());
            assertFalse(b.warlordExposed(i));
        }
        assertEquals(Board.TARGET_CAPTAINS, b.livingCaptains().size());
        assertEquals(Board.WARLORDS, b.territories.size());
        assertFalse(b.struggles.isEmpty());
    }

    @Test
    void namesAreUniqueAndTraitsDontConflict() {
        Board b = fresh(2);
        Set<String> names = new HashSet<>();
        for (Captain c : b.living()) {
            assertTrue(names.add(c.name), "duplicate " + c.name);
            for (Trait t : c.traits) for (Trait x : t.conflicts()) assertFalse(c.has(x), c.name + " has " + t + " and " + x);
        }
    }

    @Test
    void mobThatKillsPlayerRisesAndIsNemesis() {
        Board b = fresh(3);
        UUID player = UUID.randomUUID();
        List<String> lines = new ArrayList<>();
        Captain c = b.rise(new Random(4), "minecraft:husk", "minecraft:overworld", player, DeathCause.ARROW, 100, 0, 0, lines);
        assertTrue(c.alive);
        assertTrue(c.isNemesisOf(player));
        assertEquals(Memory.Kind.KILLED_PLAYER, c.relation(player).last().kind());
        assertTrue(lines.get(lines.size() - 1).contains("rises to Captain"));
    }

    @Test
    void fullBoardPushesOutTheWeakest() {
        Board b = fresh(5);
        Random r = new Random(6);
        List<String> lines = new ArrayList<>();
        while (b.livingCaptains().size() < Board.MAX_CAPTAINS)
            b.rise(r, "minecraft:zombie", "minecraft:overworld", UUID.randomUUID(), DeathCause.MELEE, 0, 0, 0, lines);
        b.rise(r, "minecraft:zombie", "minecraft:overworld", UUID.randomUUID(), DeathCause.MELEE, 0, 0, 0, lines);
        assertEquals(Board.MAX_CAPTAINS, b.livingCaptains().size());
        assertTrue(lines.stream().anyMatch(l -> l.contains("cast out")));
    }

    @Test
    void killingTheGuardsExposesTheWarlord() {
        Board b = fresh(7);
        for (Captain g : b.guardsOf(2)) b.killedByPlayer(g, UUID.randomUUID(), DeathCause.MELEE, true, 10);
        assertTrue(b.warlordExposed(2));
    }

    @Test
    void deadWarlordIsReplacedAtTheNextShift() {
        Board b = fresh(8);
        Captain w = b.warlord(0);
        b.killedByPlayer(w, UUID.randomUUID(), DeathCause.MELEE, true, 10);
        assertNull(b.warlord(0));
        b.advance(new Random(9), 20, Board.DAY);
        assertNotNull(b.warlord(0));
        assertNotEquals(w.id, b.warlord(0).id);
    }

    @Test
    void finalKillNeverComesBack() {
        Board b = fresh(10);
        Captain c = b.livingCaptains().get(0);
        c.traits.remove(Trait.DEATHLESS);
        b.killedByPlayer(c, UUID.randomUUID(), DeathCause.FIRE, true, 0);
        Random r = new Random(11);
        for (int day = 1; day < 30; day++) b.advance(r, day * Board.DAY, Board.DAY);
        assertFalse(c.alive);
    }

    @Test
    void someoneComesBackScarred() {
        Board b = fresh(12);
        Random r = new Random(13);
        List<Captain> killed = new ArrayList<>(b.livingCaptains().subList(0, 6));
        for (Captain c : killed) b.killedByPlayer(c, UUID.randomUUID(), DeathCause.FIRE, false, 0);
        for (int day = 1; day < 10; day++) b.advance(r, day * Board.DAY, Board.DAY);
        Captain back = killed.stream().filter(c -> c.alive).findFirst().orElse(null);
        assertNotNull(back, "nobody came back in nine days");
        assertTrue(back.scars.contains(Scar.BURNED));
        assertTrue(back.timesReturned > 0);
    }

    @Test
    void longRunKeepsTheBoardConsistent() {
        Board b = fresh(14);
        Random r = new Random(15);
        for (int day = 1; day < 200; day++) {
            b.advance(r, day * Board.DAY, day % 3 == 0 ? Board.DAY : 0);
            for (int i = 0; i < Board.WARLORDS; i++) {
                Captain w = b.warlord(i);
                if (w != null) {
                    assertEquals(i, w.warlordOf);
                    assertFalse(w.isGuard());
                }
                assertTrue(b.guardsOf(i).size() <= Board.GUARDS_PER_WARLORD);
            }
            assertTrue(b.livingCaptains().size() <= Board.MAX_CAPTAINS);
            for (Captain c : b.living()) assertTrue(c.level >= 1 && c.level <= Captain.MAX_LEVEL);
            for (PowerStruggle s : b.struggles) {
                assertTrue(b.get(s.first()).alive);
                if (s.second() != null) assertTrue(b.get(s.second()).alive);
            }
        }
    }

    @Test
    void oddsFavorTheStronger() {
        Captain weak = new Captain(UUID.randomUUID()), strong = new Captain(UUID.randomUUID());
        weak.level = 2;
        strong.level = 12;
        assertTrue(Board.odds(strong, weak) > 0.9);
        assertEquals(1.0, Board.odds(strong, weak) + Board.odds(weak, strong), 1e-9);
    }
}
