package io.github.pirut.bloodfeud.feud;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Packs don't settle where they fought: they withdraw, make a home a way off, and patrol the ground round it. */
class RoamTest {
    static final String OW = "minecraft:overworld";
    static final UUID PLAYER = new UUID(1, 1);
    static final Feud.Places PLACES = (r, d, x, z) -> PlaceNames.name(r, "minecraft:swamp", false);

    static List<Feud.PlayerSpot> at(int x, int z) {
        return List.of(new Feud.PlayerSpot(PLAYER, OW, x, z));
    }

    static Brute zombie(Feud f, int x, int z) {
        return f.rise("minecraft:zombie", Family.ROTTEN, OW, x, z, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
    }

    /** Nights until the lone Brutes form a pack. */
    static Pack formed(Feud f, int x, int z) {
        for (int day = 1; day <= 40 && f.packs.isEmpty(); day++) f.night(day * Feud.DAY, at(x, z), PLACES);
        assertEquals(1, f.packs.size(), "a pack formed");
        return f.packs.values().iterator().next();
    }

    @Test
    void aLoneBrutesPackWithdrawsFromTheFightBeforeItSettles() {
        for (long seed = 1; seed <= 30; seed++) {
            Feud f = new Feud(new Random(seed));
            Brute b = zombie(f, 1000, -500);
            Pack p = formed(f, 1000, -500);
            double d = Math.hypot(p.homeX - 1000, p.homeZ + 500);
            assertTrue(d >= 150 - 1 && d <= 400 + 1, "seed " + seed + ": home " + d + " blocks from the fight");
            assertTrue(p.settling, "it still has to check the ground there");
            assertEquals(1000, p.settleFromX);
            assertEquals(-500, p.settleFromZ);
            assertEquals(p.homeX, b.x, "its Boss went with it");
            assertEquals(p.homeZ, b.z);
            assertEquals(p.homeX, p.campX());
        }
    }

    @Test
    void twoBrutesThatMeetBothGoToTheNewHome() {
        Feud f = new Feud(new Random(2));
        Brute a = zombie(f, 0, 0);
        Brute b = zombie(f, 100, 0);
        f.night(Feud.DAY, at(0, 0), PLACES);
        Pack p = f.packs.values().iterator().next();
        Brute boss = f.brute(p.boss);
        double d = Math.hypot(p.homeX - p.settleFromX, p.homeZ - p.settleFromZ);
        assertTrue(d >= 149 && d <= 401, "withdrew " + d);
        assertEquals(p.homeX, boss.x);
        for (Brute m : List.of(a, b)) {
            assertEquals(p.homeX, m.x, m.name + " went home");
            assertEquals(p.homeZ, m.z);
        }
    }

    @Test
    void aPackMadeOnTheSpotKeepsItsHome() {
        // The operator command (and anything else that calls form() itself) puts the pack where it says.
        Feud f = new Feud(new Random(4));
        Pack p = f.form(zombie(f, 30, 40), PLACES, 0, 3);
        assertEquals(30, p.homeX);
        assertEquals(40, p.homeZ);
        assertFalse(p.settling);
    }

    @Test
    void theWithdrawalDistanceIsConfigurable() {
        Feud f = new Feud(new Random(5));
        f.retreatMin = 600;
        f.retreatMax = 700;
        zombie(f, 0, 0);
        Pack p = formed(f, 0, 0);
        double d = Math.hypot(p.homeX, p.homeZ);
        assertTrue(d >= 599 && d <= 701, "home " + d);
        f.retreatMin = 0;
        f.retreatMax = 0;
        Pack q = new Pack(new UUID(9, 9), Family.BONES, OW, "Here", 2, 50, 60, 0);
        f.packs.put(q.id, q);
        f.settleAway(q);
        assertEquals(50, q.homeX, "0 to 0 means it stays");
        assertEquals(60, q.homeZ);
    }

    @Test
    void aNewHomeKeepsClearOfOtherPacksWhereItCan() {
        int crowded = 0;
        for (long seed = 1; seed <= 40; seed++) {
            Feud f = new Feud(new Random(seed));
            // A pack 250 blocks east, right where half the new homes would land.
            Pack other = new Pack(new UUID(seed, 1), Family.BONES, OW, "Other", 1, 250, 0, 0);
            f.packs.put(other.id, other);
            Pack p = new Pack(new UUID(seed, 2), Family.ROTTEN, OW, "New", 2, 0, 0, 0);
            f.packs.put(p.id, p);
            f.settleAway(p);
            if (Math.hypot(p.homeX - 250, p.homeZ) < Feud.HOME_SPACING) crowded++;
        }
        assertEquals(0, crowded, "homes too close to the other pack");
    }

    @Test
    void homeCandidatesStartWithThePickAndStayInRange() {
        Feud f = new Feud(new Random(6));
        Pack p = new Pack(new UUID(3, 4), Family.ROTTEN, OW, "Here", 2, 0, 0, 0);
        f.packs.put(p.id, p);
        f.settleAway(p);
        List<int[]> first = f.homeCandidates(p, 12);
        assertEquals(12, first.size());
        assertEquals(p.homeX, first.get(0)[0]);
        assertEquals(p.homeZ, first.get(0)[1]);
        for (int[] c : first) {
            double d = Math.hypot(c[0] - p.settleFromX, c[1] - p.settleFromZ);
            assertTrue(d >= 149 && d <= 401, "candidate " + d);
        }
        List<int[]> again = f.homeCandidates(p, 12);
        for (int i = 0; i < 12; i++) {
            assertEquals(first.get(i)[0], again.get(i)[0], "the same pack gets the same candidates");
            assertEquals(first.get(i)[1], again.get(i)[1]);
        }
    }

    @Test
    void settlingMovesTheMembersAndEndsTheMove() {
        Feud f = new Feud(new Random(7));
        zombie(f, 0, 0);
        Pack p = formed(f, 0, 0);
        Brute boss = f.brute(p.boss);
        f.settle(p, 321, -123);
        assertFalse(p.settling);
        assertEquals(321, p.homeX);
        assertEquals(-123, p.homeZ);
        assertEquals(321, boss.x);
        assertEquals(-123, boss.z);
        assertEquals("Camp (not built yet)", p.settlementLabel(true));
        p.settling = true;
        assertEquals("Camp (on the move to a new home)", p.settlementLabel(true));
    }

    @Test
    void aRetreatPointHeadsTheWayItIsSent() {
        Random r = new Random(8);
        for (int i = 0; i < 200; i++) {
            int[] at = Feud.retreatPoint(r, 0, 0, 0, 150, 400);
            double d = Math.hypot(at[0], at[1]);
            assertTrue(d >= 149 && d <= 401, "distance " + d);
            assertTrue(Math.abs(Math.atan2(at[1], at[0])) <= Math.PI / 4 + 0.01, "east, give or take 45 degrees");
        }
    }

    @Test
    void thePacksGroundIsWhereItPatrols() {
        Feud f = new Feud(new Random(9));
        Pack p = new Pack(new UUID(5, 5), Family.ROTTEN, OW, "Here", 2, 0, 0, 0);
        assertTrue(f.inTerritory(p, OW, 300, 300));
        assertFalse(f.inTerritory(p, OW, 400, 400));
        assertFalse(f.inTerritory(p, "minecraft:the_nether", 0, 0));
        f.patrolRange = 800;
        assertTrue(f.inTerritory(p, OW, 400, 400));
    }

    @Test
    void aPatrolIsLedByWhoeverIsHuntingYouAndNeverTheBossWhileItHasOthers() {
        Feud f = new Feud(new Random(10));
        Brute boss = zombie(f, 0, 0);
        Pack p = f.form(boss, PLACES, 0, 6);
        Brute strong = f.rise("minecraft:zombie", Family.ROTTEN, OW, 0, 0, null, null, null, p.id, 0);
        Brute weak = f.rise("minecraft:zombie", Family.ROTTEN, OW, 0, 0, null, null, null, p.id, 0);
        strong.level = 9;
        weak.level = 1;
        boss.level = 20;
        assertEquals(strong, f.patrolLeader(p, PLAYER, b -> true), "the strongest, not the Boss");
        weak.hunting = PLAYER;
        assertEquals(weak, f.patrolLeader(p, PLAYER, b -> true), "the one hunting you");
        assertEquals(strong, f.patrolLeader(p, PLAYER, b -> b != weak), "the hunter is out already");
        assertEquals(boss, f.patrolLeader(p, PLAYER, b -> b == boss), "the Boss goes when no one else can");
        assertNull(f.patrolLeader(p, PLAYER, b -> false));
        strong.alive = false;
        weak.alive = false;
        assertEquals(boss, f.patrolLeader(p, PLAYER, b -> true));
    }

    @Test
    void aBruteThatJoinsAPackGoesToItsCamp() {
        Feud f = new Feud(new Random(11));
        Pack p = f.form(zombie(f, 0, 0), PLACES, 0, 3);
        p.siteSet = true;
        p.siteX = 40;
        p.siteZ = 40;
        Brute lone = zombie(f, 100, 0);
        for (int day = 1; day <= 30 && lone.pack == null; day++) f.night(day * Feud.DAY, at(0, 0), PLACES);
        assertNotNull(lone.pack);
        if (lone.pack.equals(p.id)) {
            assertEquals(40, lone.x);
            assertEquals(40, lone.z);
        }
    }
}
