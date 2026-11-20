package io.github.pirut.bloodfeud.feud;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SettlementTest {
    static final String OW = "minecraft:overworld";
    static final UUID PLAYER = new UUID(42, 42);
    static final Feud.Places PLACES = (r, d, x, z) -> "Ashwood";

    static List<Feud.PlayerSpot> at(int x, int z) {
        return List.of(new Feud.PlayerSpot(PLAYER, OW, x, z));
    }

    @Test
    void settlementTierThresholdsAndHelpers() {
        assertEquals(SettlementTier.CAMP, SettlementTier.forPack(1, 3, 5, false));
        assertEquals(SettlementTier.OUTPOST, SettlementTier.forPack(2, 6, 10, false));
        assertEquals(SettlementTier.OUTPOST, SettlementTier.forPack(1, 2, 16, false));
        assertEquals(SettlementTier.STRONGHOLD, SettlementTier.forPack(4, 12, 20, false));
        assertEquals(SettlementTier.STRONGHOLD, SettlementTier.forPack(2, 4, 30, false));
        assertEquals(SettlementTier.TOWN, SettlementTier.forPack(5, 18, 20, false));
        assertEquals(SettlementTier.TOWN, SettlementTier.forPack(1, 2, 46, false));
        assertEquals(SettlementTier.TOWN, SettlementTier.forPack(1, 1, 2, true));

        assertEquals(SettlementTier.OUTPOST, SettlementTier.byKey("outpost"));
        assertEquals(SettlementTier.STRONGHOLD, SettlementTier.byKey("STRONGHOLD"));
        assertNull(SettlementTier.byKey("citadel"));
        assertEquals(SettlementTier.CAMP, SettlementTier.NONE.next());
        assertEquals(SettlementTier.OUTPOST, SettlementTier.CAMP.next());
        assertEquals(SettlementTier.STRONGHOLD, SettlementTier.OUTPOST.next());
        assertEquals(SettlementTier.TOWN, SettlementTier.STRONGHOLD.next());
        assertEquals(SettlementTier.TOWN, SettlementTier.TOWN.next());
    }

    @Test
    void nightGrowthAdvancesSettlementAndNeverShrinks() {
        Feud f = new Feud(new Random(101));
        Brute boss = f.rise("minecraft:zombie", Family.ROTTEN, OW, 0, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        Pack p = f.form(boss, PLACES, 0, 2);
        assertEquals(SettlementTier.CAMP, p.tier);
        f.drainNews();

        // Grow pack to Outpost threshold and run a night
        Brute b2 = f.rise("minecraft:zombie", Family.ROTTEN, OW, 0, 0, PLAYER, Memory.Kind.SAW_KILLS, null, p.id, 0);
        b2.level = 5;
        p.grunts = 7;
        f.drainNews();
        f.night(Feud.DAY, at(0, 0), PLACES);

        assertTrue(p.tier.level >= SettlementTier.OUTPOST.level, "Expected Outpost+, got " + p.tier);
        assertTrue(f.drainNews().stream().anyMatch(n -> n.kind() == News.Kind.SETTLED));

        // Even if the pack loses grunts later, its tier never shrinks automatically
        SettlementTier peak = p.tier;
        p.grunts = 0;
        f.night(2 * Feud.DAY, at(0, 0), PLACES);
        assertEquals(peak, p.tier);
    }

    @Test
    void crowningAKingImmediatelyUnlocksTown() {
        Feud f = new Feud(new Random(202));
        Brute kingBoss = f.rise("minecraft:vindicator", Family.ILLAGERS, OW, 0, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        kingBoss.level = 8;
        Pack winner = f.form(kingBoss, PLACES, 0, 4);
        assertEquals(SettlementTier.CAMP, winner.tier);

        Brute smallBoss = f.rise("minecraft:vindicator", Family.ILLAGERS, OW, 300, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        Pack loser = f.form(smallBoss, PLACES, 0, 2);
        f.drainNews();

        f.swallow(winner, loser);
        assertEquals(SettlementTier.TOWN, winner.tier);
        List<News> news = f.drainNews();
        assertTrue(news.stream().anyMatch(n -> n.kind() == News.Kind.CROWNED));
        assertTrue(news.stream().anyMatch(n -> n.kind() == News.Kind.SETTLED && n.text().contains("full town")));
    }

    @Test
    void upgradeSettlementPopulatesFullGangRoster() {
        Feud f = new Feud(new Random(303));
        Brute boss = f.rise("minecraft:skeleton", Family.BONES, OW, 100, -50, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        Pack p = f.form(boss, PLACES, 0, 1);
        f.drainNews();

        SettlementTier result = f.upgradeSettlement(p, SettlementTier.STRONGHOLD, 500);
        assertEquals(SettlementTier.STRONGHOLD, result);
        assertEquals(SettlementTier.STRONGHOLD, p.tier);
        assertTrue(f.members(p).size() >= SettlementTier.STRONGHOLD.minBrutes);
        assertTrue(p.grunts >= SettlementTier.STRONGHOLD.minGrunts);
        assertTrue(f.drainNews().stream().anyMatch(n -> n.kind() == News.Kind.SETTLED));

        f.upgradeSettlement(p, SettlementTier.TOWN, 1000);
        assertEquals(SettlementTier.TOWN, p.tier);
        assertTrue(f.members(p).size() >= SettlementTier.TOWN.minBrutes);
        assertTrue(p.grunts >= SettlementTier.TOWN.minGrunts);
    }

    @Test
    void buildCampsDisabledPreventsAutomaticAdvancementAndNews() {
        Feud f = new Feud(new Random(404));
        f.campsEnabled = false;

        Brute boss = f.rise("minecraft:zombie", Family.ROTTEN, OW, 0, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        boss.level = 18;
        Pack p = f.form(boss, PLACES, 0, 20);
        assertEquals(SettlementTier.CAMP, p.tier);
        assertEquals(SettlementTier.NONE, p.displayTier(false), "Unbuilt pack omits settlement tier when disabled");
        f.drainNews();

        // Nightly tick should not advance settlement tier or spam SETTLED news
        f.night(Feud.DAY, at(0, 0), PLACES);
        assertEquals(SettlementTier.CAMP, p.tier);
        assertFalse(f.drainNews().stream().anyMatch(n -> n.kind() == News.Kind.SETTLED));

        // Swallowing a rival should crown a King without advancing settlement tier while disabled
        Brute small = f.rise("minecraft:zombie", Family.ROTTEN, OW, 200, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        Pack loser = f.form(small, PLACES, 0, 1);
        f.drainNews();
        f.swallow(p, loser);
        assertEquals(SettlementTier.CAMP, p.tier);
        assertFalse(f.drainNews().stream().anyMatch(n -> n.kind() == News.Kind.SETTLED));

        // Manual operator upgradeSettlement still works on demand even when auto-camps are off
        f.upgradeSettlement(p, SettlementTier.OUTPOST, 2000);
        assertEquals(SettlementTier.OUTPOST, p.tier);
        p.builtTier = SettlementTier.OUTPOST.level;
        assertEquals(SettlementTier.OUTPOST, p.displayTier(false), "Already-built pack still displays gracefully");
    }
}
