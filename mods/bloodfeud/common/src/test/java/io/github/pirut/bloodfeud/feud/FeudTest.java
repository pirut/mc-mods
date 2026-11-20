package io.github.pirut.bloodfeud.feud;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeudTest {
    static final String OW = "minecraft:overworld";
    static final UUID PLAYER = new UUID(1, 1);
    static final Feud.Places PLACES = (r, d, x, z) -> PlaceNames.name(r, "minecraft:swamp", false);

    static List<Feud.PlayerSpot> at(int x, int z) {
        return List.of(new Feud.PlayerSpot(PLAYER, OW, x, z));
    }

    static Brute zombie(Feud f, int x, int z) {
        return f.rise("minecraft:zombie", Family.ROTTEN, OW, x, z, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
    }

    @Test
    void theMobThatKillsYouBecomesYourNemesis() {
        Feud f = new Feud(new Random(1));
        Brute b = f.rise("minecraft:husk", Family.ROTTEN, OW, 10, 10, PLAYER, Memory.Kind.ROSE, DeathCause.MELEE, null, 0);
        assertTrue(b.isNemesisOf(PLAYER));
        assertEquals(PLAYER, b.hunting);
        assertTrue(b.level >= 2);
        assertEquals(Rank.BRUTE, f.rank(b));
        int before = b.level;
        f.killedPlayer(b.id, PLAYER, DeathCause.MELEE, 100);
        assertEquals(before + 1, b.level);
        assertEquals(2, b.relation(PLAYER).killedPlayer);
    }

    @Test
    void twoLoneBrutesMeetAndFormAPack() {
        Feud f = new Feud(new Random(2));
        Brute a = zombie(f, 0, 0);
        Brute b = zombie(f, 100, 0);
        f.drainNews();
        f.night(Feud.DAY, at(0, 0), PLACES);
        assertEquals(1, f.packs.size());
        Pack p = f.packs.values().iterator().next();
        assertEquals(a.pack, p.id);
        assertEquals(b.pack, p.id);
        Brute boss = f.brute(p.boss);
        assertEquals(Rank.BOSS, f.rank(boss));
        assertTrue(boss.power() >= (boss == a ? b : a).power());
        assertTrue(p.title().startsWith("the ") && p.title().endsWith(" Pack"));
        assertTrue(f.drainNews().stream().anyMatch(n -> n.kind() == News.Kind.FORMED));
    }

    @Test
    void differentFamiliesNeverBand() {
        Feud f = new Feud(new Random(3));
        zombie(f, 0, 0);
        f.rise("minecraft:skeleton", Family.BONES, OW, 50, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        for (int day = 1; day <= 30; day++) f.night(day * Feud.DAY, at(0, 0), PLACES);
        for (Pack p : f.packs.values()) for (Brute m : f.members(p)) assertEquals(p.family, m.family);
    }

    @Test
    void farAwayPacksSleepAndCatchUpWhenSomeoneComesBack() {
        Feud f = new Feud(new Random(4));
        Brute a = zombie(f, 0, 0);
        Pack p = f.form(a, PLACES, 0, 2);
        p.lastAwake = 0;
        for (int day = 1; day <= 20; day++) f.night(day * Feud.DAY, at(50_000, 0), PLACES);
        assertEquals(2, p.grunts, "nobody near it, so it didn't grow");
        f.night(21 * Feud.DAY, at(100, 0), PLACES);
        assertTrue(p.grunts >= 2 + Feud.CATCH_UP_NIGHTS && p.grunts <= 2 + 2 * Feud.CATCH_UP_NIGHTS, "grunts " + p.grunts);
        assertEquals(21 * Feud.DAY, p.lastAwake);
    }

    @Test
    void killingTheBossHandsThePackToTheStrongestBrute() {
        Feud f = new Feud(new Random(5));
        Brute boss = zombie(f, 0, 0);
        Pack p = f.form(boss, PLACES, 0, 4);
        Brute weak = zombie(f, 0, 0);
        Brute strong = zombie(f, 0, 0);
        weak.pack = p.id;
        strong.pack = p.id;
        weak.level = 1;
        strong.level = 9;
        f.killedByPlayer(boss.id, PLAYER, DeathCause.MELEE, true, 10);
        assertFalse(boss.alive);
        assertTrue(boss.finalDeath);
        assertEquals(strong.id, p.boss);
        f.killedByPlayer(strong.id, PLAYER, DeathCause.MELEE, true, 20);
        assertEquals(weak.id, p.boss);
        f.killedByPlayer(weak.id, PLAYER, DeathCause.MELEE, true, 30);
        assertNull(f.pack(p.id), "nobody left to lead it");
    }

    @Test
    void sameFamilyRivalsGetSwallowedAndTheWinnerIsCrowned() {
        Feud f = new Feud(new Random(6));
        Brute big = zombie(f, 0, 0);
        big.level = 15;
        Pack strong = f.form(big, PLACES, 0, 16);
        Brute small = zombie(f, 800, 0);
        small.level = 1;
        Pack weak = f.form(small, PLACES, 0, 1);
        f.raid(strong, weak, 10);
        assertEquals(strong.id, weak.liege);
        assertEquals(Rank.KING, f.rank(big));
        assertEquals(Rank.BOSS, f.rank(small));
        assertTrue(f.drainNews().stream().anyMatch(n -> n.kind() == News.Kind.CROWNED));
        f.killedByPlayer(big.id, PLAYER, DeathCause.MELEE, true, 20);
        assertNull(weak.liege, "the King's packs split apart when it dies");
    }

    @Test
    void noTwoLivingBrutesShareAnEpithet() {
        Feud f = new Feud(new Random(13));
        Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < 40; i++) {
            Brute b = zombie(f, i * 50, 0);
            assertTrue(seen.add(b.epithet), "repeated: " + b.epithet);
            assertTrue(b.size >= 0.86f && b.size <= 1.16f);
        }
    }

    @Test
    void packsKeepAShortStoryOfWhatHappened() {
        Feud f = new Feud(new Random(12));
        Brute big = zombie(f, 0, 0);
        big.level = 15;
        Pack strong = f.form(big, PLACES, 0, 16);
        Brute small = zombie(f, 800, 0);
        small.level = 1;
        Pack weak = f.form(small, PLACES, 3, 1);
        f.raid(strong, weak, 10);
        assertFalse(strong.chronicle.isEmpty());
        assertFalse(weak.chronicle.isEmpty(), "the loser remembers the raid too");
        assertEquals(10, weak.chronicle.peekLast().time());
        for (int i = 0; i < 40; i++) strong.remember(i, "event " + i);
        assertEquals(Pack.CHRONICLE, strong.chronicle.size());
        assertEquals("event 39", strong.chronicle.peekLast().text());
    }

    @Test
    void theDeadComeBackScarredUnlessTheKillWasFinal() {
        Feud f = new Feud(new Random(7));
        Brute back = zombie(f, 0, 0);
        Brute gone = zombie(f, 0, 0);
        assertFalse(f.killedByPlayer(back.id, PLAYER, DeathCause.FIRE, false, 0));
        assertTrue(f.killedByPlayer(gone.id, PLAYER, DeathCause.MELEE, true, 0) || gone.traits.contains(Trait.DEATHLESS));
        gone.traits.remove(Trait.DEATHLESS);
        gone.finalDeath = true;
        for (int day = 1; day <= 5 && !back.alive; day++) f.night(day * Feud.DAY, at(0, 0), PLACES);
        assertTrue(back.alive);
        assertTrue(back.scars.contains(Scar.BURNED));
        assertEquals(PLAYER, back.hunting);
        assertEquals(Memory.Kind.RETURNED, back.relation(PLAYER).last().kind());
        assertFalse(gone.alive);
    }

    @Test
    void comingBackTooOftenEndsIt() {
        Feud f = new Feud(new Random(8));
        Brute b = zombie(f, 0, 0);
        b.traits.remove(Trait.DEATHLESS);
        b.timesReturned = Feud.MAX_RETURNS;
        assertTrue(f.killedByPlayer(b.id, PLAYER, DeathCause.ARROW, false, 0));
    }

    @Test
    void aGrudgeFollowsYouAcrossTheWorld() {
        Feud f = new Feud(new Random(9));
        Brute b = f.rise("minecraft:zombie", Family.ROTTEN, OW, 0, 0, PLAYER, Memory.Kind.ROSE, DeathCause.MELEE, null, 0);
        f.night(Feud.DAY, at(10_000, 0), PLACES);
        assertEquals(Feud.TRAVEL_PER_DAY, b.x);
        for (int day = 2; day <= 10; day++) f.night(day * Feud.DAY, at(10_000, 0), PLACES);
        assertTrue(Math.abs(10_000 - b.x) <= 600, "caught up: " + b.x);
    }

    @Test
    void theOldestSleepingUnmetPackFadesAtTheCap() {
        Feud f = new Feud(new Random(10));
        f.maxPacks = 2;
        Pack first = f.form(zombie(f, 0, 0), PLACES, 0, 1);
        first.met.clear();
        Pack second = f.form(zombie(f, 10_000, 0), PLACES, 5, 1);
        second.met.clear();
        Pack third = f.form(zombie(f, 20_000, 0), PLACES, 10, 1);
        third.met.add(PLAYER);
        f.night(Feud.DAY, at(20_000, 0), PLACES);
        assertNull(f.pack(first.id));
        assertNotNull(f.pack(second.id));
        assertNotNull(f.pack(third.id));
    }

    @Test
    void traitsNeverClash() {
        Feud f = new Feud(new Random(11));
        for (int i = 0; i < 500; i++) {
            Brute b = new Brute(UUID.randomUUID(), "minecraft:zombie", Family.ROTTEN, OW, "X", 0);
            f.rollTraits(b);
            for (Trait t : b.traits) for (Trait c : t.conflicts()) assertFalse(b.traits.contains(c), t + " with " + c);
            assertTrue(b.traits.stream().anyMatch(t -> t.kind == Trait.Kind.WEAKNESS), "always a way to beat it");
        }
    }

    @Test
    void rivalPacksFightOverTime() {
        Feud f = new Feud(new Random(12));
        f.form(zombie(f, 0, 0), PLACES, 0, 6);
        f.form(f.rise("minecraft:skeleton", Family.BONES, OW, 900, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0),
                PLACES, 0, 6);
        f.drainNews();
        boolean raided = false;
        for (int day = 1; day <= 30 && !raided; day++) {
            f.night(day * Feud.DAY, at(400, 0), PLACES);
            raided = f.drainNews().stream().anyMatch(n -> n.kind() == News.Kind.RAID);
        }
        assertTrue(raided);
    }

    @Test
    void aHundredNightsOfFeudingKeepsTheWorldSane() {
        Feud f = new Feud(new Random(13));
        Family[] fams = Family.values();
        Random r = new Random(14);
        for (int i = 0; i < 24; i++) {
            Family fam = fams[i % fams.length];
            f.rise(fam.defaultType, fam, OW, r.nextInt(3000) - 1500, r.nextInt(3000) - 1500, PLAYER,
                    Memory.Kind.SAW_KILLS, null, null, 0);
        }
        boolean king = false;
        for (int day = 1; day <= 100; day++) {
            f.night(day * Feud.DAY, at(0, 0), PLACES);
            f.drainNews();
            for (Pack p : f.packs.values()) {
                assertTrue(f.members(p).size() <= Feud.MAX_MEMBERS);
                assertTrue(p.grunts >= 0 && p.grunts <= Feud.MAX_GRUNTS);
                for (Brute m : f.members(p)) assertEquals(p.family, m.family);
                Pack top = f.top(p);
                assertTrue(top.liege == null, "chains end at a King");
                if (p.liege != null) assertEquals(p.family, f.pack(p.liege).family);
                Brute boss = f.brute(p.boss);
                if (boss != null && boss.alive) king |= f.rank(boss) == Rank.KING;
            }
            assertTrue(f.packs.size() <= f.maxPacks && f.brutes.size() <= f.maxBrutes);
        }
        assertTrue(king, "some pack swallowed another in 100 nights");
    }
}
