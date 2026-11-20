package io.github.pirut.bloodfeud.feud;

import io.github.pirut.bloodfeud.game.camp.CampBlueprint;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
                        if (p.material() == CampBlueprint.Material.CHAIN) {
                            boolean above = solidCoords.contains(p.rx() + "," + (p.ry() + 1) + "," + p.rz());
                            assertTrue(above, "Hanging ruin chain without support above at " + p);
                            continue;
                        }
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

    @Test
    void verticalLogsStayVerticalAndLogBeamsRotateAcrossAllFacings() {
        for (CampBlueprint.Facing f : CampBlueprint.Facing.values()) {
            assertEquals(CampBlueprint.Axis.Y, CampBlueprint.logAxis(CampBlueprint.Material.LOG, f),
                    "Vertical LOG must always stay Axis.Y regardless of rotated facing " + f);
        }
        assertEquals(CampBlueprint.Axis.Z, CampBlueprint.logAxis(CampBlueprint.Material.LOG_BEAM, CampBlueprint.Facing.NORTH));
        assertEquals(CampBlueprint.Axis.Z, CampBlueprint.logAxis(CampBlueprint.Material.LOG_BEAM, CampBlueprint.Facing.SOUTH));
        assertEquals(CampBlueprint.Axis.X, CampBlueprint.logAxis(CampBlueprint.Material.LOG_BEAM, CampBlueprint.Facing.EAST));
        assertEquals(CampBlueprint.Axis.X, CampBlueprint.logAxis(CampBlueprint.Material.LOG_BEAM, CampBlueprint.Facing.WEST));

        UUID packId = new UUID(99, 101);
        for (int facing = 0; facing < 4; facing++) {
            CampBlueprint.Plan plan = CampBlueprint.plan(packId, Family.ROTTEN, 14, List.of(), facing, 0, SettlementTier.OUTPOST.level);
            long verticalLogs = plan.placements().stream()
                    .filter(p -> p.material() == CampBlueprint.Material.LOG).count();
            long beams = plan.placements().stream()
                    .filter(p -> p.material() == CampBlueprint.Material.LOG_BEAM).count();
            assertTrue(verticalLogs > 0, "Expected vertical LOG placements");
            assertTrue(beams > 0, "Expected horizontal LOG_BEAM placements");

            // Check that rotated campfire seat beam at unrotated (0, 0, 2) alternates X/Z across 90-degree rotations
            int expectedX = switch (facing) { case 1 -> -2; case 2 -> 0; case 3 -> 2; default -> 0; };
            int expectedZ = switch (facing) { case 1 -> 0; case 2 -> -2; case 3 -> 0; default -> 2; };
            CampBlueprint.Placement seat = plan.placements().stream()
                    .filter(p -> p.rx() == expectedX && p.ry() == 0 && p.rz() == expectedZ)
                    .findFirst().orElseThrow();
            assertEquals(CampBlueprint.Material.LOG_BEAM, seat.material());
            CampBlueprint.Axis expectedAxis = (facing % 2 == 0) ? CampBlueprint.Axis.X : CampBlueprint.Axis.Z;
            assertEquals(expectedAxis, CampBlueprint.logAxis(seat.material(), seat.facing()),
                    "Beam seat axis mismatch for siteFacing=" + facing);
        }
    }

    @Test
    void hangingLanternsHaveSolidOrChainSupportInEarlierPass() {
        assertTrue(CampBlueprint.Material.CHAIN.pass < CampBlueprint.Material.HANGING_LANTERN.pass,
                "CHAIN must be placed in an earlier pass than HANGING_LANTERN");
        UUID packId = new UUID(11, 22);
        for (Family family : Family.values()) {
            for (SettlementTier tier : new SettlementTier[]{
                    SettlementTier.CAMP, SettlementTier.OUTPOST, SettlementTier.STRONGHOLD, SettlementTier.TOWN}) {
                for (int facing = 0; facing < 4; facing++) {
                    CampBlueprint.Plan plan = CampBlueprint.plan(packId, family, 3, List.of(1, 2), facing, 0, tier.level);
                    Map<CampBlueprint.Spot, Integer> indexOf = new HashMap<>();
                    Map<CampBlueprint.Spot, CampBlueprint.Placement> bySpot = new HashMap<>();
                    List<CampBlueprint.Placement> ops = plan.placements();
                    for (int i = 0; i < ops.size(); i++) {
                        CampBlueprint.Placement p = ops.get(i);
                        if (p.material() == CampBlueprint.Material.FOUNDATION_COLUMN) continue;
                        CampBlueprint.Spot s = new CampBlueprint.Spot(p.rx(), p.ry(), p.rz());
                        indexOf.put(s, i);
                        bySpot.put(s, p);
                    }
                    for (int i = 0; i < ops.size(); i++) {
                        CampBlueprint.Placement p = ops.get(i);
                        if (p.material() != CampBlueprint.Material.HANGING_LANTERN) continue;
                        CampBlueprint.Spot above = new CampBlueprint.Spot(p.rx(), p.ry() + 1, p.rz());
                        CampBlueprint.Placement support = bySpot.get(above);
                        assertNotNull(support, family + " " + tier + " f=" + facing + " HANGING_LANTERN at "
                                + p.rx() + "," + p.ry() + "," + p.rz() + " missing support above");
                        assertNotEquals(CampBlueprint.Material.AIR, support.material(),
                                "HANGING_LANTERN has AIR above it at " + above);
                        assertTrue(support.pass() < p.pass() && indexOf.get(above) < i,
                                "Support " + support.material() + " above HANGING_LANTERN at " + above
                                        + " must be placed in an earlier pass");
                    }
                }
            }
        }
    }

    @Test
    void laddersHaveSolidBackingWallsAndStandingBannersHaveSolidSupport() {
        UUID packId = new UUID(33, 44);
        for (Family family : Family.values()) {
            for (SettlementTier tier : new SettlementTier[]{
                    SettlementTier.CAMP, SettlementTier.OUTPOST, SettlementTier.STRONGHOLD, SettlementTier.TOWN}) {
                for (int facing = 0; facing < 4; facing++) {
                    CampBlueprint.Plan plan = CampBlueprint.plan(packId, family, 5, List.of(1, 2, 3), facing, 0, tier.level);
                    Map<CampBlueprint.Spot, CampBlueprint.Placement> bySpot = new HashMap<>();
                    for (CampBlueprint.Placement p : plan.placements()) {
                        if (p.material() == CampBlueprint.Material.FOUNDATION_COLUMN) continue;
                        bySpot.put(new CampBlueprint.Spot(p.rx(), p.ry(), p.rz()), p);
                    }
                    for (CampBlueprint.Placement p : plan.placements()) {
                        if (p.material() == CampBlueprint.Material.LADDER
                                || p.material() == CampBlueprint.Material.BANNER_WALL
                                || p.material() == CampBlueprint.Material.WALL_TORCH) {
                            CampBlueprint.Facing back = p.facing().opposite();
                            CampBlueprint.Spot behind = new CampBlueprint.Spot(p.rx() + back.dx, p.ry(), p.rz() + back.dz);
                            CampBlueprint.Placement wall = bySpot.get(behind);
                            assertNotNull(wall, family + " " + tier + " f=" + facing + " " + p.material()
                                    + " at " + p.rx() + "," + p.ry() + "," + p.rz() + " missing backing block at " + behind);
                            assertNotEquals(CampBlueprint.Material.AIR, wall.material(),
                                    p.material() + " backing block is AIR at " + behind);
                            assertTrue(wall.pass() < p.pass(),
                                    p.material() + " backing block must be in an earlier pass at " + behind);
                        } else if (p.material() == CampBlueprint.Material.BANNER_STANDING
                                || p.material() == CampBlueprint.Material.VASSAL_BANNER) {
                            CampBlueprint.Spot below = new CampBlueprint.Spot(p.rx(), p.ry() - 1, p.rz());
                            CampBlueprint.Placement support = bySpot.get(below);
                            assertNotNull(support, family + " " + tier + " f=" + facing + " " + p.material()
                                    + " at " + p.rx() + "," + p.ry() + "," + p.rz() + " missing support below");
                            assertNotEquals(CampBlueprint.Material.AIR, support.material(),
                                    p.material() + " sits over AIR at " + below);
                            assertTrue(support.pass() < p.pass(),
                                    p.material() + " support " + support.material() + " must be in an earlier pass");
                        }
                    }
                }
            }
        }
    }

    @Test
    void townOuterDistrictsAreClearOfStrongholdAndCornerTowersAndVassalsClearOfTents() {
        UUID packId = new UUID(55, 66);
        CampBlueprint.Plan town = CampBlueprint.plan(packId, Family.ILLAGERS, 14,
                List.of(1, 2, 3, 4, 5, 6), 0, 0, SettlementTier.TOWN.level);

        String[] outerZones = {
                "town_tavern", "town_jail", "town_forge", "town_market",
                "town_house_1", "town_house_2", "town_house_3", "town_house_4"
        };
        int[][] townCornerTowers = {
                {-29, -25, -29, -25}, {25, 29, -29, -25},
                {-29, -25, 25, 29}, {25, 29, 25, 29}
        };

        for (int i = 0; i < outerZones.length; i++) {
            String z = outerZones[i];
            CampBlueprint.Box box = town.zones().get(z);
            assertNotNull(box, "Missing Town outer zone " + z);

            // Must lie cleanly outside Stronghold [-20..20] x [-20..20] (and its [+-16..+-20] corner towers)
            assertFalse(boxesOverlapXZ(box.minX(), box.maxX(), box.minZ(), box.maxZ(), -20, 20, -20, 20),
                    z + " overlaps Stronghold [-20..20]x[-20..20]: " + box);

            // Must not overlap any Town corner guard tower [+-25..+-29] x [+-25..+-29]
            for (int[] tower : townCornerTowers) {
                assertFalse(boxesOverlapXZ(box.minX(), box.maxX(), box.minZ(), box.maxZ(),
                                tower[0], tower[1], tower[2], tower[3]),
                        z + " overlaps Town corner tower: " + box);
            }

            // Must not overlap any other outer district zone
            for (int j = i + 1; j < outerZones.length; j++) {
                CampBlueprint.Box other = town.zones().get(outerZones[j]);
                assertFalse(boxesOverlapXZ(box.minX(), box.maxX(), box.minZ(), box.maxZ(),
                                other.minX(), other.maxX(), other.minZ(), other.maxZ()),
                        z + " overlaps " + outerZones[j]);
            }
        }

        // Vassal banner spots (y = 0..3) must be completely clear of Tier 1-3 structures/tents
        Set<CampBlueprint.Spot> tier1To3Owned = CampBlueprint.ownedPositions(
                packId, Family.ILLAGERS, 14, List.of(), 0, SettlementTier.STRONGHOLD.level);
        for (CampBlueprint.Spot bannerSpot : CampBlueprint.vassalBannerSpots(0)) {
            for (int y = 0; y <= 3; y++) {
                assertFalse(tier1To3Owned.contains(new CampBlueprint.Spot(bannerSpot.rx(), y, bannerSpot.rz())),
                        "Vassal banner column (" + bannerSpot.rx() + "," + bannerSpot.rz()
                                + ") collides with Tier 1-3 block at y=" + y);
            }
        }

        // Tier 4 streets/lamp posts must not place non-air blocks inside Tier 3 Barracks [-17..-13, -5..5] or Forge [13..17, -5..5]
        CampBlueprint.Plan tier4Only = CampBlueprint.plan(packId, Family.ILLAGERS, 14,
                List.of(1, 2), 0, SettlementTier.STRONGHOLD.level, SettlementTier.TOWN.level);
        for (CampBlueprint.Placement p : tier4Only.placements()) {
            if (!"town_streets".equals(p.zone()) || p.material() == CampBlueprint.Material.AIR) continue;
            if (p.ry() < 0) continue;
            boolean inBarracks = p.rx() >= -17 && p.rx() <= -13 && p.rz() >= -5 && p.rz() <= 5;
            boolean inForge = p.rx() >= 13 && p.rx() <= 17 && p.rz() >= -5 && p.rz() <= 5;
            assertFalse(inBarracks || inForge,
                    "town_streets placement " + p.material() + " at (" + p.rx() + "," + p.ry() + "," + p.rz()
                            + ") clips Tier 3 Barracks or Forge");
        }
    }

    @Test
    void exactChunkBoundsAndOwnedContainerTracking() {
        // Footprint near chunk boundary must include all touched edge chunks
        int[] b1 = CampBlueprint.footprintChunkBounds(15, 15, 7); // r = 10 -> [5..25] => chunks [0..1]
        assertArrayEquals(new int[]{0, 0, 1, 1}, b1);

        int[] b2 = CampBlueprint.footprintChunkBounds(0, 0, 29); // r = 32 -> [-32..32] => chunks [-2..2]
        assertArrayEquals(new int[]{-2, -2, 2, 2}, b2);

        UUID packId = new UUID(77, 88);
        assertTrue(CampBlueprint.ownedPositions(packId, Family.ROTTEN, 1, List.of(), 0, 0).isEmpty());
        assertTrue(CampBlueprint.containerPositions(packId, Family.ROTTEN, 1, List.of(), 0, 0).isEmpty());

        Set<CampBlueprint.Spot> ownedCamp = CampBlueprint.ownedPositions(packId, Family.WITCHES, 1, List.of(), 0, 1);
        Set<CampBlueprint.Spot> campContainers = CampBlueprint.containerPositions(packId, Family.WITCHES, 1, List.of(), 0, 1);
        assertFalse(ownedCamp.isEmpty());
        assertFalse(campContainers.isEmpty());
        assertTrue(ownedCamp.containsAll(campContainers), "Every owned container must also be an owned position");

        assertTrue(CampBlueprint.isRingZone("outpost_palisade"));
        assertTrue(CampBlueprint.isRingZone("stronghold_wall"));
        assertTrue(CampBlueprint.isRingZone("town_streets"));
        assertFalse(CampBlueprint.isRingZone("town_tavern"));
        assertFalse(CampBlueprint.isRingZone("camp_core"));
    }

    private static boolean boxesOverlapXZ(int ax0, int ax1, int az0, int az1, int bx0, int bx1, int bz0, int bz1) {
        return ax0 <= bx1 && ax1 >= bx0 && az0 <= bz1 && az1 >= bz0;
    }
}
