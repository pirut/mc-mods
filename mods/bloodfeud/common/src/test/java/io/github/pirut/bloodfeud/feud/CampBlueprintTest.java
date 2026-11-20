package io.github.pirut.bloodfeud.feud;

import io.github.pirut.bloodfeud.game.camp.CampBlueprint;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CampBlueprintTest {

    @Test
    void allFourTiersAndSixFamiliesPlanCleanlyWithPassOrdering() {
        UUID packId = new UUID(0x123456789ABCDEFL, 0xFEDCBA9876543210L);
        List<Integer> vassals = List.of(1, 11, 14);

        for (Family family : Family.values()) {
            for (SettlementTier tier : new SettlementTier[]{
                    SettlementTier.CAMP, SettlementTier.OUTPOST, SettlementTier.STRONGHOLD, SettlementTier.TOWN}) {
                int facing = (family.ordinal() + tier.level) % 4;
                CampBlueprint.Plan plan = CampBlueprint.plan(packId, family, 14, vassals, facing, 0, tier.level);

                assertFalse(plan.placements().isEmpty(), family + " " + tier + " must have placements");
                assertFalse(plan.zones().isEmpty(), family + " " + tier + " must define zones");
                assertNotNull(plan.bossSpot(), "bossSpot required");
                assertFalse(plan.guardSpots().isEmpty(), "guardSpots required");
                assertFalse(plan.patrolSpots().isEmpty(), "patrolSpots required");

                // Verify pass ordering (0 -> 1 -> 2 -> 3) and bottom-up y ordering within each pass
                int prevPass = -1;
                int prevY = Integer.MIN_VALUE;
                boolean hasChest = false;
                boolean hasBanner = false;
                boolean hasVassalBanner = false;
                boolean hasFoundation = false;
                Set<String> nonFoundationCoords = new HashSet<>();

                int maxBound = tier.radius + 3;
                for (CampBlueprint.Placement p : plan.placements()) {
                    assertTrue(p.pass() >= prevPass, "Pass regressed from " + prevPass + " to " + p.pass());
                    if (p.pass() > prevPass) {
                        prevPass = p.pass();
                        prevY = Integer.MIN_VALUE;
                    }
                    assertTrue(p.ry() >= prevY, "ry regressed inside pass " + p.pass() + ": " + p.ry() + " < " + prevY);
                    prevY = p.ry();

                    assertTrue(Math.abs(p.rx()) <= maxBound && Math.abs(p.rz()) <= maxBound,
                            "Placement out of bounds for " + tier + ": (" + p.rx() + "," + p.rz() + ")");

                    if (p.material() == CampBlueprint.Material.FOUNDATION_COLUMN) {
                        hasFoundation = true;
                    } else {
                        String coord = p.rx() + "," + p.ry() + "," + p.rz();
                        assertTrue(nonFoundationCoords.add(coord), "Duplicate placement at " + coord);
                    }
                    if (p.material().isChest()) hasChest = true;
                    if (p.material() == CampBlueprint.Material.BANNER_STANDING || p.material() == CampBlueprint.Material.BANNER_WALL) {
                        hasBanner = true;
                    }
                    if (p.material() == CampBlueprint.Material.VASSAL_BANNER) hasVassalBanner = true;
                }

                assertTrue(hasFoundation, family + " " + tier + " needs terrain foundations");
                assertTrue(hasChest, family + " " + tier + " needs at least one loot chest");
                assertTrue(hasBanner, family + " " + tier + " needs pack banners");
                if (tier == SettlementTier.TOWN) {
                    assertTrue(hasVassalBanner, "Town with vassals should fly vassal banners in forecourt");
                    assertTrue(plan.zones().containsKey("town_tavern"));
                    assertTrue(plan.zones().containsKey("town_jail"));
                    assertTrue(plan.zones().containsKey("town_forge"));
                    assertTrue(plan.zones().containsKey("town_market"));
                    assertTrue(plan.zones().containsKey("town_house_1"));
                    assertTrue(plan.zones().containsKey("town_house_4"));
                }

                // Spawn anchors sit inside/near settlement footprint
                assertTrue(Math.abs(plan.bossSpot().rx()) <= maxBound && Math.abs(plan.bossSpot().rz()) <= maxBound);
                for (CampBlueprint.Spot s : plan.guardSpots()) {
                    assertTrue(Math.abs(s.rx()) <= maxBound && Math.abs(s.rz()) <= maxBound);
                }
            }
        }
    }

    @Test
    void additiveExpansionAndDeterminism() {
        UUID packId = new UUID(7, 13);
        CampBlueprint.Plan step1 = CampBlueprint.plan(packId, Family.ILLAGERS, 4, List.of(), 1, 0, 1);
        CampBlueprint.Plan step2 = CampBlueprint.plan(packId, Family.ILLAGERS, 4, List.of(), 1, 1, 2);
        CampBlueprint.Plan step3 = CampBlueprint.plan(packId, Family.ILLAGERS, 4, List.of(), 1, 2, 3);
        CampBlueprint.Plan step4 = CampBlueprint.plan(packId, Family.ILLAGERS, 4, List.of(2, 10), 1, 3, 4);

        assertFalse(step1.placements().isEmpty());
        assertFalse(step2.placements().isEmpty());
        assertFalse(step3.placements().isEmpty());
        assertFalse(step4.placements().isEmpty());

        assertTrue(step1.placements().stream().anyMatch(p -> p.material() == CampBlueprint.Material.CHEST_CAMP));
        assertTrue(step2.placements().stream().anyMatch(p -> p.material() == CampBlueprint.Material.CHEST_OUTPOST));
        assertTrue(step3.placements().stream().anyMatch(p -> p.material() == CampBlueprint.Material.CHEST_STRONGHOLD));
        assertTrue(step4.placements().stream().anyMatch(p -> p.material() == CampBlueprint.Material.CHEST_TOWN));

        // Deterministic across repeated invocations
        CampBlueprint.Plan repeat = CampBlueprint.plan(packId, Family.ILLAGERS, 4, List.of(2, 10), 1, 3, 4);
        assertEquals(step4.placements(), repeat.placements());
        assertEquals(step4.zones(), repeat.zones());
    }

    @Test
    void ruinedPlansWorkAcrossAllTiersAndFamiliesWithExtinguishedFiresAndNoFloatingBlocks() {
        UUID packId = new UUID(0xCAFEBABE12345678L, 0x0102030405060708L);
        List<Integer> vassals = List.of(1, 11, 14);

        for (Family family : Family.values()) {
            for (SettlementTier tier : new SettlementTier[]{
                    SettlementTier.CAMP, SettlementTier.OUTPOST, SettlementTier.STRONGHOLD, SettlementTier.TOWN}) {
                int facing = (family.ordinal() + tier.level) % 4;
                CampBlueprint.Plan intact = CampBlueprint.plan(packId, family, 14, vassals, facing, 0, tier.level);
                CampBlueprint.Plan ruined = CampBlueprint.ruinPlan(packId, family, 14, vassals, facing, tier.level);

                assertFalse(ruined.placements().isEmpty());
                // Deterministic
                CampBlueprint.Plan repeat = CampBlueprint.ruinPlan(packId, family, 14, vassals, facing, tier.level);
                assertEquals(ruined.placements(), repeat.placements());

                // No lit campfires remain; extinguished campfires and weathered ruin blocks are present
                assertTrue(ruined.placements().stream().noneMatch(p -> p.material() == CampBlueprint.Material.CAMPFIRE),
                        family + " " + tier + " ruins must extinguish campfires");
                assertTrue(ruined.placements().stream().anyMatch(p -> p.material() == CampBlueprint.Material.CAMPFIRE_UNLIT),
                        family + " " + tier + " ruins should include an unlit campfire");
                assertTrue(ruined.placements().stream().anyMatch(p -> p.material().isRuinMaterial()),
                        family + " " + tier + " ruins should include weathered ruin materials");

                // Fewer structural blocks than intact (visible collapse/crumbling)
                long intactStructure = intact.placements().stream()
                        .filter(p -> p.material().pass == CampBlueprint.PASS_STRUCTURE && p.material() != CampBlueprint.Material.AIR).count();
                long ruinedStructure = ruined.placements().stream()
                        .filter(p -> p.material().pass == CampBlueprint.PASS_STRUCTURE && p.material() != CampBlueprint.Material.AIR).count();
                assertTrue(ruinedStructure < intactStructure,
                        family + " " + tier + " ruins should collapse part of structure (" + ruinedStructure + " vs " + intactStructure + ")");

                // Pass and bottom-up y ordering, within tier bounds, and no isolated floating structure blocks
                int prevPass = -1;
                int prevY = Integer.MIN_VALUE;
                int maxBound = tier.radius + 3;
                Set<String> solidCoords = new HashSet<>();
                for (CampBlueprint.Placement p : ruined.placements()) {
                    assertTrue(p.pass() >= prevPass);
                    if (p.pass() > prevPass) {
                        prevPass = p.pass();
                        prevY = Integer.MIN_VALUE;
                    }
                    assertTrue(p.ry() >= prevY);
                    prevY = p.ry();
                    assertTrue(Math.abs(p.rx()) <= maxBound && Math.abs(p.rz()) <= maxBound);
                    if (p.material() != CampBlueprint.Material.AIR
                            && p.material() != CampBlueprint.Material.FOUNDATION_COLUMN
                            && p.material().pass != CampBlueprint.PASS_DECOR
                            && p.material() != CampBlueprint.Material.CAMPFIRE_UNLIT) {
                        solidCoords.add(p.rx() + "," + p.ry() + "," + p.rz());
                    }
                }
                for (CampBlueprint.Placement p : ruined.placements()) {
                    if (p.ry() >= 1 && p.material().pass == CampBlueprint.PASS_STRUCTURE && p.material() != CampBlueprint.Material.AIR) {
                        boolean below = solidCoords.contains(p.rx() + "," + (p.ry() - 1) + "," + p.rz());
                        int horiz = 0;
                        if (solidCoords.contains((p.rx() + 1) + "," + p.ry() + "," + p.rz())) horiz++;
                        if (solidCoords.contains((p.rx() - 1) + "," + p.ry() + "," + p.rz())) horiz++;
                        if (solidCoords.contains(p.rx() + "," + p.ry() + "," + (p.rz() + 1))) horiz++;
                        if (solidCoords.contains(p.rx() + "," + p.ry() + "," + (p.rz() - 1))) horiz++;
                        assertTrue(below || horiz >= 2, "Floating isolated ruin block at " + p);
                    }
                }
            }
        }
    }

    @Test
    void ruinDiffOrdersTeardownTopDownPreservesContainersAndRepairRestoresCleanly() {
        UUID packId = new UUID(99, 77);
        for (SettlementTier tier : new SettlementTier[]{
                SettlementTier.CAMP, SettlementTier.OUTPOST, SettlementTier.STRONGHOLD, SettlementTier.TOWN}) {
            CampBlueprint.Plan diff = CampBlueprint.ruinDiffPlan(packId, Family.ROTTEN, 14, List.of(1), 2, tier.level);
            assertFalse(diff.placements().isEmpty(), tier + " ruinDiff should not be empty");

            // Never overwrites or removes chests/barrels in-place
            assertTrue(diff.placements().stream().noneMatch(p -> p.material().isChest() || p.material() == CampBlueprint.Material.BARREL));
            // Extinguishes campfire in-place
            assertTrue(diff.placements().stream().anyMatch(p -> p.material() == CampBlueprint.Material.CAMPFIRE_UNLIT));
            // Pass order strictly monotonic 0 -> 1 -> 2 -> 3, and passes 0..2 are top-down (descending ry)
            int prevPass = -1;
            int prevY = Integer.MAX_VALUE;
            for (CampBlueprint.Placement p : diff.placements()) {
                assertTrue(p.pass() >= prevPass, "Pass regressed in ruinDiff");
                if (p.pass() > prevPass) {
                    prevPass = p.pass();
                    prevY = Integer.MAX_VALUE;
                }
                if (p.pass() <= 2) {
                    assertTrue(p.ry() <= prevY, "Teardown pass " + p.pass() + " must run top-down");
                    prevY = p.ry();
                }
            }

            // Repair plan restores lit campfires and includes all intact placements
            CampBlueprint.Plan repair = CampBlueprint.repairPlan(packId, Family.ROTTEN, 14, List.of(1), 2, tier.level, tier.level);
            assertTrue(repair.placements().stream().anyMatch(p -> p.material() == CampBlueprint.Material.CAMPFIRE));
            assertTrue(repair.placements().stream().noneMatch(p -> p.material().isRuinMaterial()));
        }
    }
}
