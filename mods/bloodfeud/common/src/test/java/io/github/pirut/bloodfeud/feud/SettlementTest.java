package io.github.pirut.bloodfeud.feud;

import org.junit.jupiter.api.Test;

import io.github.pirut.bloodfeud.game.camp.CampBlueprint;

import java.util.List;
import java.util.Random;
import java.util.Set;
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

        // Hysteresis: losing 1 grunt leaves 1 grunt, which still sustains an intact Camp
        f.gruntKilled(p, 100);
        assertEquals(1, p.grunts);
        assertFalse(p.ruined, "Intact Camp with 1 grunt should not collapse on a single kill");

        // Losing the last grunt drops below the Camp sustains floor -> Ruined Camp
        f.gruntKilled(p, 120);
        assertEquals(0, p.grunts);
        assertTrue(p.ruined, "Camp with 0 grunts should fall into ruin");
        assertEquals("Ruined Camp", p.displayTierTitle(true));
        assertTrue(f.drainNews().stream().anyMatch(n -> n.kind() == News.Kind.SETTLED && n.text().contains("camp has fallen into ruin")));

        // Regrowing to 1 grunt stays ruined (no flapping below full Camp threshold of 2)
        p.grunts = 1;
        assertFalse(f.updateSettlement(p));
        assertTrue(p.ruined);

        // Recruiting back up to full threshold (2 grunts) restores the camp
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

    @Test
    void ruinedPackWithHigherPeakTierDoesNotRetriggerRuin() {
        Pack p = new Pack(UUID.randomUUID(), Family.ILLAGERS, OW, "Ironford", 14, 0, 0, 0L);
        p.tier = SettlementTier.TOWN;
        p.builtTier = SettlementTier.CAMP.level;
        p.ruined = true;
        p.builtRuined = true;

        assertEquals(Pack.AutoBuildAction.NONE, p.autoBuildAction(),
                "Already-ruined built camp with higher peak tier must not loop re-queueing ruin jobs");

        // Still queues ruin if not yet built as ruined
        p.builtRuined = false;
        assertEquals(Pack.AutoBuildAction.RUIN, p.autoBuildAction());

        // Unbuilt ruined pack still queues initial ruined build
        p.builtTier = 0;
        p.builtRuined = true;
        assertEquals(Pack.AutoBuildAction.RUIN, p.autoBuildAction());

        // Once recovered, queues repair first, then tier expansion once repaired
        p.builtTier = SettlementTier.CAMP.level;
        p.ruined = false;
        p.builtRuined = true;
        assertEquals(Pack.AutoBuildAction.REPAIR, p.autoBuildAction());

        p.builtRuined = false;
        assertEquals(Pack.AutoBuildAction.BUILD, p.autoBuildAction());

        p.builtTier = SettlementTier.TOWN.level;
        assertEquals(Pack.AutoBuildAction.NONE, p.autoBuildAction());
    }

    @Test
    void ruinSettlementMarksBannersDirtyOnKingAndFreedVassals() {
        Feud f = new Feud(new Random(707));
        Brute kingBoss = f.rise("minecraft:vindicator", Family.ILLAGERS, OW, 0, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        kingBoss.level = 10;
        Pack king = f.form(kingBoss, PLACES, 0, 8);

        Brute v1Boss = f.rise("minecraft:vindicator", Family.ILLAGERS, OW, 150, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        Pack v1 = f.form(v1Boss, PLACES, 0, 3);
        Brute v2Boss = f.rise("minecraft:vindicator", Family.ILLAGERS, OW, -150, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        Pack v2 = f.form(v2Boss, PLACES, 0, 3);

        f.swallow(king, v1);
        f.swallow(king, v2);
        king.bannersDirty = false;
        v1.bannersDirty = false;
        v2.bannersDirty = false;

        f.ruinSettlement(king, SettlementTier.TOWN, 1000);
        assertTrue(king.ruined);
        assertNull(v1.liege);
        assertNull(v2.liege);
        assertTrue(king.bannersDirty, "ruinSettlement must mark former King bannersDirty");
        assertTrue(v1.bannersDirty, "ruinSettlement must mark freed vassal 1 bannersDirty");
        assertTrue(v2.bannersDirty, "ruinSettlement must mark freed vassal 2 bannersDirty");
    }

    @Test
    void disbandingKingCarriesVassalsAndSkippedAreasIntoAbandonedSiteAndMergesMidBuild() {
        Feud f = new Feud(new Random(808));
        Brute kingBoss = f.rise("minecraft:vindicator", Family.ILLAGERS, OW, 0, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        kingBoss.level = 10;
        Pack king = f.form(kingBoss, PLACES, 0, 8);
        Brute vBoss = f.rise("minecraft:vindicator", Family.ILLAGERS, OW, 100, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        Pack vassal = f.form(vBoss, PLACES, 0, 3);
        f.swallow(king, vassal);

        king.siteSet = true;
        king.siteX = 10;
        king.siteY = 64;
        king.siteZ = -20;
        king.siteFacing = 1;
        king.builtTier = SettlementTier.STRONGHOLD.level;
        king.builtRuined = false;
        long col1 = CampBlueprint.colKey(12, -5);
        king.skippedZones.add("stronghold_forge");
        king.skippedColumns.add(col1);

        f.killedByPlayer(kingBoss.id, PLAYER, DeathCause.MELEE, true, 500);
        assertNull(f.pack(king.id));
        assertEquals(1, f.abandonedSites.size());
        Feud.AbandonedSite site = f.abandonedSites.get(0);
        assertEquals(SettlementTier.STRONGHOLD.level, site.builtTier());
        assertEquals(List.of(vassal.color), site.vassalColors());
        assertTrue(site.skippedZones().contains("stronghold_forge"));
        assertTrue(site.skippedColumns().contains(col1));

        // Mid-upgrade disband records the higher target tier and merges newly skipped areas cleanly
        long col2 = CampBlueprint.colKey(20, 4);
        f.recordAbandonedSite(new Feud.AbandonedSite(
                king.id, king.family, king.color, king.dimension,
                10, 64, -20, 1, SettlementTier.TOWN.level,
                List.of(vassal.color), Set.of("town_forge"), Set.of(col2)));
        assertEquals(1, f.abandonedSites.size());
        Feud.AbandonedSite merged = f.abandonedSites.get(0);
        assertEquals(SettlementTier.TOWN.level, merged.builtTier());
        assertEquals(Set.of("stronghold_forge", "town_forge"), merged.skippedZones());
        assertEquals(Set.of(col1, col2), merged.skippedColumns());
    }

    @Test
    void commandSpawnedTownSurvivesSingleGruntKillAndHysteresisAppliesToAllTiers() {
        // (a) Command-spawned Town gets +4 grunt buffer and survives killing 1 grunt
        Feud f = new Feud(new Random(909));
        Brute boss = f.rise("minecraft:zombie", Family.ROTTEN, OW, 0, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        Pack town = f.form(boss, PLACES, 0, 2);
        f.upgradeSettlement(town, SettlementTier.TOWN, 100);
        assertTrue(town.grunts >= SettlementTier.TOWN.minGrunts + 4,
                "upgradeSettlement should grant +4 grunt buffer above minGrunts");
        f.gruntKilled(town, 150);
        assertFalse(town.ruined, "Killing 1 grunt in a command-spawned Town must not ruin it");

        // (b) Beating a pack down below its sustains floor DOES ruin it across all 4 tiers,
        // and (c) regrowing inside the hysteresis band stays ruined until the full threshold restores it
        assertTrue(SettlementTier.CAMP.sustains(1, 1, 2, false));
        assertFalse(SettlementTier.CAMP.sustains(1, 0, 10, false));

        assertTrue(SettlementTier.OUTPOST.sustains(2, 4, 8, false));
        assertFalse(SettlementTier.OUTPOST.sustains(2, 3, 12, false));

        assertTrue(SettlementTier.STRONGHOLD.sustains(3, 9, 18, false));
        assertFalse(SettlementTier.STRONGHOLD.sustains(3, 8, 24, false));

        assertTrue(SettlementTier.TOWN.sustains(4, 14, 30, false));
        assertFalse(SettlementTier.TOWN.sustains(4, 13, 37, false));
        assertTrue(SettlementTier.TOWN.sustains(1, 1, 2, true));

        // Verify full ruin -> hysteresis hold -> recovery cycle on Stronghold & Town
        Feud f2 = new Feud(new Random(910));
        Brute sBoss = f2.rise("minecraft:skeleton", Family.BONES, OW, 0, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        Pack strong = f2.form(sBoss, PLACES, 0, 2);
        f2.upgradeSettlement(strong, SettlementTier.STRONGHOLD, 100);
        // Trim levels so strength alone does not hold Stronghold
        for (Brute m : f2.members(strong)) m.level = 1;
        strong.grunts = 9; // above sustains floor (9), below full threshold (12)
        assertFalse(f2.updateSettlement(strong));
        assertFalse(strong.ruined, "Stronghold at sustains floor (9 grunts) stays intact");

        f2.gruntKilled(strong, 200); // drops to 8 grunts -> below sustains floor
        assertTrue(strong.ruined, "Stronghold below sustains floor must fall into ruin");

        strong.grunts = 10; // back above sustains floor, still below full threshold (12)
        assertFalse(f2.updateSettlement(strong));
        assertTrue(strong.ruined, "Ruined Stronghold must not flap back to intact before full threshold");

        strong.grunts = 12; // full threshold reached
        assertTrue(f2.updateSettlement(strong));
        assertFalse(strong.ruined, "Regrowing to full threshold restores ruined Stronghold");

        // Town collapse below sustains floor and recovery at full threshold
        Feud f3 = new Feud(new Random(911));
        Brute tBoss = f3.rise("minecraft:zombie", Family.ROTTEN, OW, 0, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        Pack tPack = f3.form(tBoss, PLACES, 0, 2);
        f3.upgradeSettlement(tPack, SettlementTier.TOWN, 100);
        for (Brute m : f3.members(tPack)) m.level = 1;
        tPack.grunts = 14;
        assertFalse(f3.updateSettlement(tPack));
        assertFalse(tPack.ruined, "Town at sustains floor (14 grunts) stays intact");

        f3.gruntKilled(tPack, 200); // drops to 13 -> below sustains floor
        assertTrue(tPack.ruined, "Town below sustains floor must fall into ruin");

        tPack.grunts = 16; // inside hysteresis band (14..17)
        assertFalse(f3.updateSettlement(tPack));
        assertTrue(tPack.ruined, "Ruined Town must not flap back to intact below full threshold");

        tPack.grunts = 18; // full Town threshold (5 living, 18 grunts)
        assertTrue(f3.updateSettlement(tPack));
        assertFalse(tPack.ruined, "Regrowing to full threshold restores ruined Town");
    }

    @Test
    void standoffDistanceScalesWithViewDistanceAndTierRadius() {
        SettlementTier[] tiers = {
                SettlementTier.CAMP, SettlementTier.OUTPOST, SettlementTier.STRONGHOLD, SettlementTier.TOWN
        };
        int[] viewDistances = {0, 2, 4, 6, 8, 10, 12, 16, 32};

        for (int vd : viewDistances) {
            int prev = -1;
            for (SettlementTier tier : tiers) {
                int expected = Math.max(tier.radius + 96, Math.max(2, vd) * 16 + tier.radius + 16);
                int actual = CampBlueprint.standoffDistance(vd, tier.radius);
                assertEquals(expected, actual, "standoffDistance(" + vd + ", " + tier.key + ")");
                assertTrue(actual >= tier.radius + 96, "must respect minimum radius+96 floor");
                assertTrue(actual > prev, "standoff must grow strictly with larger settlement tiers");
                prev = actual;
            }
        }

        // Low view distances clamp to radius + 96 floor
        assertEquals(SettlementTier.CAMP.radius + 96, CampBlueprint.standoffDistance(2, SettlementTier.CAMP.radius));
        assertEquals(SettlementTier.TOWN.radius + 96, CampBlueprint.standoffDistance(4, SettlementTier.TOWN.radius));
        // Standard 10-chunk view distance: 10 * 16 + radius + 16 = 176 + radius
        assertEquals(183, CampBlueprint.standoffDistance(10, SettlementTier.CAMP.radius));
        assertEquals(205, CampBlueprint.standoffDistance(10, SettlementTier.TOWN.radius));
        // High 16-chunk view distance: 16 * 16 + radius + 16 = 272 + radius
        assertEquals(279, CampBlueprint.standoffDistance(16, SettlementTier.CAMP.radius));
        assertEquals(301, CampBlueprint.standoffDistance(16, SettlementTier.TOWN.radius));
    }

    @Test
    void isOutsideStandoffBoundaryCases() {
        int standoff = 100;
        // Same point & inside
        assertFalse(CampBlueprint.isOutsideStandoff(10, -20, 10, -20, standoff));
        assertFalse(CampBlueprint.isOutsideStandoff(10, -20, 109.99, -20, standoff));
        assertFalse(CampBlueprint.isOutsideStandoff(10, -20, 10 + 59.9, -20 + 79.9, standoff));

        // Exactly on boundary (axis-aligned and 3-4-5 diagonal) must NOT be considered clear
        assertFalse(CampBlueprint.isOutsideStandoff(10, -20, 110.0, -20, standoff));
        assertFalse(CampBlueprint.isOutsideStandoff(10, -20, 10, -120.0, standoff));
        assertFalse(CampBlueprint.isOutsideStandoff(10, -20, 70.0, 60.0, standoff)); // hypot(60, 80) == 100

        // Strictly outside boundary
        assertTrue(CampBlueprint.isOutsideStandoff(10, -20, 110.01, -20, standoff));
        assertTrue(CampBlueprint.isOutsideStandoff(10, -20, 10, -120.01, standoff));
        assertTrue(CampBlueprint.isOutsideStandoff(10, -20, 70.01, 60.0, standoff));
    }

    @Test
    void ticketCoverageCoversEveryFootprintChunkWithFullStatusMargin() {
        SettlementTier[] tiers = {
                SettlementTier.CAMP, SettlementTier.OUTPOST, SettlementTier.STRONGHOLD, SettlementTier.TOWN
        };
        int[][] coords = {
                {0, 0}, {7, 7}, {15, 15}, {16, 16},
                {-1, -1}, {-16, -16}, {-17, 31}, {1234, -5678}
        };

        for (SettlementTier tier : tiers) {
            int[] radii = {tier.radius, CampBlueprint.searchFootprintRadius(tier)};
            for (int radius : radii) {
                for (int[] c : coords) {
                    int cx = c[0], cz = c[1];
                    int[] bounds = CampBlueprint.footprintChunkBounds(cx, cz, radius);
                    int[] cov = CampBlueprint.ticketCoverage(cx, cz, radius);
                    int centerCx = cov[0], centerCz = cov[1], ticketRadius = cov[2];

                    assertTrue(ticketRadius >= 1, "ticketRadius must be at least 1");
                    int maxDistSeen = 0;
                    for (int chunkX = bounds[0]; chunkX <= bounds[2]; chunkX++) {
                        for (int chunkZ = bounds[1]; chunkZ <= bounds[3]; chunkZ++) {
                            int chebyshev = Math.max(Math.abs(chunkX - centerCx), Math.abs(chunkZ - centerCz));
                            assertTrue(chebyshev < ticketRadius,
                                    "chunk (" + chunkX + "," + chunkZ + ") must be within Chebyshev < "
                                            + ticketRadius + " of (" + centerCx + "," + centerCz + ") for r=" + radius);
                            maxDistSeen = Math.max(maxDistSeen, chebyshev);
                        }
                    }
                    assertEquals(Math.max(1, maxDistSeen + 1), ticketRadius,
                            "ticketRadius must tightly equal maxChebyshevDist + 1");
                    assertTrue(ticketRadius <= 5,
                            "off-screen ticket radius should stay modest (<= 5) for " + tier.key + " r=" + radius);
                }
            }
        }
    }
}
