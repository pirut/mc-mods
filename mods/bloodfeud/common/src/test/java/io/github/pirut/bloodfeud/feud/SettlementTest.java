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
        assertEquals(SettlementTier.NONE, SettlementTier.forPack(0, 10, 5, false));
        assertEquals(SettlementTier.NONE, SettlementTier.forPack(1, 0, 5, false));
        assertEquals(SettlementTier.NONE, SettlementTier.forPack(1, 1, 3, false));
        assertEquals(SettlementTier.CAMP, SettlementTier.forPack(1, 2, 4, false));
        assertEquals(SettlementTier.CAMP, SettlementTier.forPack(1, 3, 5, false));
        assertEquals(SettlementTier.OUTPOST, SettlementTier.forPack(2, 6, 10, false));
        assertEquals(SettlementTier.OUTPOST, SettlementTier.forPack(1, 2, 16, false));
        assertEquals(SettlementTier.STRONGHOLD, SettlementTier.forPack(4, 12, 20, false));
        assertEquals(SettlementTier.STRONGHOLD, SettlementTier.forPack(2, 4, 30, false));
        assertEquals(SettlementTier.TOWN, SettlementTier.forPack(5, 18, 20, false));
        assertEquals(SettlementTier.TOWN, SettlementTier.forPack(1, 2, 46, false));
        assertEquals(SettlementTier.TOWN, SettlementTier.forPack(1, 1, 2, true));

        assertEquals("Town", SettlementTier.TOWN.title(false));
        assertEquals("Ruined Town", SettlementTier.TOWN.title(true));
        assertEquals("Ruined Camp", SettlementTier.CAMP.title(true));

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
    void droppingBelowThresholdRuinsSettlementAndRecoveryRebuildsIt() {
        Feud f = new Feud(new Random(101));
        Brute boss = f.rise("minecraft:zombie", Family.ROTTEN, OW, 0, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        Pack p = f.form(boss, PLACES, 0, 2);
        assertEquals(SettlementTier.CAMP, p.tier);
        assertFalse(p.ruined);
        f.drainNews();

        // Grow pack to Outpost threshold and run a night (keep boss stronger than b2 so no fatal challenge occurs)
        boss.level = 4;
        Brute b2 = f.rise("minecraft:zombie", Family.ROTTEN, OW, 0, 0, PLAYER, Memory.Kind.SAW_KILLS, null, p.id, 0);
        b2.level = 3;
        p.grunts = 7;
        f.drainNews();
        f.night(Feud.DAY, at(0, 0), PLACES);

        assertEquals(SettlementTier.OUTPOST, p.tier);
        assertFalse(p.ruined);
        assertTrue(f.drainNews().stream().anyMatch(n -> n.kind() == News.Kind.SETTLED));

        // Dropping grunts and killing a Brute below the tier threshold turns the settlement into ruins
        // while keeping its peak tier footprint intact
        SettlementTier peak = p.tier;
        f.drainNews();
        f.killedByPlayer(b2.id, PLAYER, DeathCause.MELEE, true, Feud.DAY + 10);
        p.grunts = 2;
        f.gruntKilled(p, Feud.DAY + 20);
        assertNotNull(f.pack(p.id), "Pack still exists with its Boss");
        assertEquals(peak, p.tier, "Peak structural tier stays intact so ruins keep their footprint");
        assertTrue(p.ruined, "Dropping below threshold should mark settlement ruined");
        assertEquals("Ruined " + peak.title, p.displayTierTitle(true));
        assertTrue(f.drainNews().stream().anyMatch(n -> n.kind() == News.Kind.SETTLED && n.text().contains("ruin")));

        // Recovering numbers back up to the peak tier restores the ruined settlement
        Brute b3 = f.rise("minecraft:zombie", Family.ROTTEN, OW, 0, 0, PLAYER, Memory.Kind.SAW_KILLS, null, p.id, Feud.DAY + 30);
        b3.level = 4;
        p.grunts = 7;
        f.drainNews();
        assertTrue(f.updateSettlement(p));
        assertFalse(p.ruined, "Recovering above threshold should rebuild the ruined settlement");
        assertEquals(peak.title, p.displayTierTitle(true));
        assertTrue(f.drainNews().stream().anyMatch(n -> n.kind() == News.Kind.SETTLED && n.text().contains("rebuilt")));
    }

    @Test
    void campFallsIntoRuinWhenGruntsOrBrutesDropBelowCampThreshold() {
        Feud f = new Feud(new Random(151));
        Brute boss = f.rise("minecraft:zombie", Family.ROTTEN, OW, 0, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        Pack p = f.form(boss, PLACES, 0, 2);
        assertEquals(SettlementTier.CAMP, p.tier);
        assertFalse(p.ruined);
        f.drainNews();

        // Losing a grunt drops count to 1 (< 2 camp threshold) -> Ruined Camp
        f.gruntKilled(p, 100);
        assertEquals(1, p.grunts);
        assertTrue(p.ruined, "Camp with < 2 grunts should fall into ruin");
        assertEquals("Ruined Camp", p.displayTierTitle(true));
        assertTrue(f.drainNews().stream().anyMatch(n -> n.kind() == News.Kind.SETTLED && n.text().contains("camp has fallen into ruin")));

        // Recruiting back up to 2 grunts restores the camp
        p.grunts = 2;
        assertTrue(f.updateSettlement(p));
        assertFalse(p.ruined);
        assertEquals("Camp", p.displayTierTitle(true));
    }

    @Test
    void killingKingRuinsTownWhenVassalsBreakAway() {
        Feud f = new Feud(new Random(202));
        Brute kingBoss = f.rise("minecraft:vindicator", Family.ILLAGERS, OW, 0, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        kingBoss.level = 6;
        Pack winner = f.form(kingBoss, PLACES, 0, 4);
        Brute lieutenant = f.rise("minecraft:vindicator", Family.ILLAGERS, OW, 0, 0, PLAYER, Memory.Kind.SAW_KILLS, null, winner.id, 0);
        lieutenant.level = 3;
        assertEquals(SettlementTier.CAMP, winner.tier);

        Brute smallBoss = f.rise("minecraft:vindicator", Family.ILLAGERS, OW, 300, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        Pack loser = f.form(smallBoss, PLACES, 0, 2);
        f.drainNews();

        f.swallow(winner, loser);
        assertEquals(SettlementTier.TOWN, winner.tier);
        assertFalse(winner.ruined);
        List<News> news = f.drainNews();
        assertTrue(news.stream().anyMatch(n -> n.kind() == News.Kind.CROWNED));
        assertTrue(news.stream().anyMatch(n -> n.kind() == News.Kind.SETTLED && n.text().contains("full town")));

        // Slaying the King strips vassals; without King status or Town numbers, the Town falls into ruin
        f.killedByPlayer(kingBoss.id, PLAYER, DeathCause.MELEE, true, 500);
        assertTrue(f.vassals(winner).isEmpty());
        assertEquals(SettlementTier.TOWN, winner.tier);
        assertTrue(winner.ruined, "Former King's Town should fall into ruin when vassals scatter");
        assertEquals("Ruined Town", winner.displayTierTitle(true));
        assertTrue(f.drainNews().stream().anyMatch(n -> n.kind() == News.Kind.SETTLED && n.text().contains("town has fallen into ruin")));
    }

    @Test
    void disbandingBuiltPackQueuesAbandonedSiteForRuination() {
        Feud f = new Feud(new Random(252));
        Brute boss = f.rise("minecraft:skeleton", Family.BONES, OW, 50, 50, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        Pack p = f.form(boss, PLACES, 0, 3);
        p.siteSet = true;
        p.siteX = 50;
        p.siteY = 64;
        p.siteZ = 50;
        p.builtTier = SettlementTier.OUTPOST.level;
        p.builtRuined = false;

        f.killedByPlayer(boss.id, PLAYER, DeathCause.MELEE, true, 500);
        assertNull(f.pack(p.id), "Pack should disband after last Brute's final death");
        assertEquals(1, f.abandonedSites.size(), "Intact built site should be queued for ruination");
        assertEquals(p.id, f.abandonedSites.get(0).packId());
        assertEquals(SettlementTier.OUTPOST.level, f.abandonedSites.get(0).builtTier());
    }

    @Test
    void upgradeAndRuinOperatorHelpersWorkOnDemand() {
        Feud f = new Feud(new Random(303));
        Brute boss = f.rise("minecraft:skeleton", Family.BONES, OW, 100, -50, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        Pack p = f.form(boss, PLACES, 0, 1);
        f.drainNews();

        SettlementTier result = f.upgradeSettlement(p, SettlementTier.STRONGHOLD, 500);
        assertEquals(SettlementTier.STRONGHOLD, result);
        assertEquals(SettlementTier.STRONGHOLD, p.tier);
        assertFalse(p.ruined);
        assertTrue(f.members(p).size() >= SettlementTier.STRONGHOLD.minBrutes);
        assertTrue(p.grunts >= SettlementTier.STRONGHOLD.minGrunts);
        assertTrue(f.drainNews().stream().anyMatch(n -> n.kind() == News.Kind.SETTLED));

        f.ruinSettlement(p, SettlementTier.STRONGHOLD, 700);
        assertTrue(p.ruined);
        assertEquals("Ruined Stronghold", p.displayTierTitle(true));
        assertTrue(SettlementTier.forPack(f.members(p).size(), p.grunts, f.strength(p), false).level < SettlementTier.STRONGHOLD.level);

        f.upgradeSettlement(p, SettlementTier.TOWN, 1000);
        assertEquals(SettlementTier.TOWN, p.tier);
        assertFalse(p.ruined);
        assertTrue(f.members(p).size() >= SettlementTier.TOWN.minBrutes);
        assertTrue(p.grunts >= SettlementTier.TOWN.minGrunts);
    }

    @Test
    void buildCampsDisabledPreventsAutomaticAdvancementRuinAndNews() {
        Feud f = new Feud(new Random(404));
        f.campsEnabled = false;

        Brute boss = f.rise("minecraft:zombie", Family.ROTTEN, OW, 0, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        boss.level = 18;
        Pack p = f.form(boss, PLACES, 0, 20);
        assertEquals(SettlementTier.CAMP, p.tier);
        assertEquals(SettlementTier.NONE, p.displayTier(false), "Unbuilt pack omits settlement tier when disabled");
        assertEquals("", p.displayTierTitle(false));
        f.drainNews();

        // Nightly tick should not advance settlement tier or spam SETTLED news
        f.night(Feud.DAY, at(0, 0), PLACES);
        assertEquals(SettlementTier.CAMP, p.tier);
        assertFalse(p.ruined);
        assertFalse(f.drainNews().stream().anyMatch(n -> n.kind() == News.Kind.SETTLED));

        // Dropping grunts while disabled should not auto-ruin or emit news
        p.grunts = 1;
        f.gruntKilled(p, Feud.DAY + 50);
        assertFalse(p.ruined);
        assertFalse(f.drainNews().stream().anyMatch(n -> n.kind() == News.Kind.SETTLED));

        // Swallowing a rival should crown a King without advancing settlement tier while disabled
        Brute small = f.rise("minecraft:zombie", Family.ROTTEN, OW, 200, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        Pack loser = f.form(small, PLACES, 0, 1);
        f.drainNews();
        f.swallow(p, loser);
        assertEquals(SettlementTier.CAMP, p.tier);
        assertFalse(f.drainNews().stream().anyMatch(n -> n.kind() == News.Kind.SETTLED));

        // Manual operator helpers still work on demand even when auto-camps are off
        f.upgradeSettlement(p, SettlementTier.OUTPOST, 2000);
        assertEquals(SettlementTier.OUTPOST, p.tier);
        p.builtTier = SettlementTier.OUTPOST.level;
        assertEquals(SettlementTier.OUTPOST, p.displayTier(false), "Already-built pack still displays gracefully");
        assertEquals("Outpost", p.displayTierTitle(false));

        f.ruinSettlement(p, SettlementTier.OUTPOST, 2100);
        p.builtRuined = true;
        assertEquals("Ruined Outpost", p.displayTierTitle(false));
    }

    @Test
    void bannersDirtyMarkedOnSwallowAndKingDeathSplit() {
        Feud f = new Feud(new Random(505));
        Brute kingBoss = f.rise("minecraft:vindicator", Family.ILLAGERS, OW, 0, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        kingBoss.level = 10;
        Pack king = f.form(kingBoss, PLACES, 0, 5);

        Brute vassalBoss = f.rise("minecraft:vindicator", Family.ILLAGERS, OW, 200, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        Pack vassal = f.form(vassalBoss, PLACES, 0, 2);

        assertFalse(king.bannersDirty);
        assertFalse(vassal.bannersDirty);

        f.swallow(king, vassal);
        assertTrue(king.bannersDirty, "King pack must be marked bannersDirty after swallowing a vassal");
        assertTrue(vassal.bannersDirty, "Swallowed pack must be marked bannersDirty to switch to King's color");

        // Simulate rebannering completed, then kill the King -> SPLIT must mark both dirty again
        king.bannersDirty = false;
        vassal.bannersDirty = false;
        f.drainNews();

        f.killedByPlayer(kingBoss.id, PLAYER, DeathCause.MELEE, true, 1000);
        assertNull(vassal.liege, "Vassal must be freed when its King dies");
        assertTrue(king.bannersDirty, "King pack must be marked bannersDirty on SPLIT to clear stale vassal banners");
        assertTrue(vassal.bannersDirty, "Freed vassal must be marked bannersDirty on SPLIT to restore its own banner color");
        assertTrue(f.drainNews().stream().anyMatch(n -> n.kind() == News.Kind.SPLIT));
    }

    @Test
    void maxGruntsRestoresClassicCapWhenCampsDisabled() {
        Feud f = new Feud(new Random(606));
        assertEquals(Feud.MAX_GRUNTS, f.maxGrunts());
        assertEquals(24, f.maxGrunts());

        f.campsEnabled = false;
        assertEquals(16, f.maxGrunts(), "Disabling camps must restore classic 16-grunt cap");

        Brute boss = f.rise("minecraft:zombie", Family.ROTTEN, OW, 0, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        Pack p = f.form(boss, PLACES, 0, 22);
        assertEquals(16, p.grunts, "Pack grunt cap must respect maxGrunts() when campsEnabled=false");
    }
}
