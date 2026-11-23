package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.feud.Family;
import io.github.pirut.bloodfeud.feud.Feud;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.SettlementTier;
import io.github.pirut.bloodfeud.game.EarthWorldFixture.FakeLevel;
import io.github.pirut.bloodfeud.game.camp.CampBlueprint;
import io.github.pirut.bloodfeud.game.camp.CampBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static io.github.pirut.bloodfeud.game.EarthWorldFixture.X;
import static io.github.pirut.bloodfeud.game.EarthWorldFixture.Z;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A finished camp's positions are the pack's only while the pack's own blocks stand there; and its stash holds loot.
 * Both were wrong in every dimension up to 0.1.0-alpha.5.
 */
class CampOwnershipTest {
    @BeforeAll
    static void bootstrap() {
        EarthWorldFixture.bootstrap();
    }

    private record Camp(FakeLevel level, FeudData data, Pack pack, CampBuilder builder, long[] clock) {
        void run(CampBuilder.QueueResult queued) {
            assertTrue(queued.queued(), "queued");
            for (int i = 0; i < 400 && builder.isBuilding(pack.id); i++) builder.tick(level.getServer(), data, clock[0]++);
            assertFalse(builder.isBuilding(pack.id), "finished");
        }

        BlockPos at(CampBlueprint.Placement op) {
            return new BlockPos(pack.siteX + op.rx(), pack.siteY + op.ry(), pack.siteZ + op.rz());
        }

        /** The last thing the camp put at each position, so what should stand there. */
        Map<BlockPos, CampBlueprint.Placement> built(int tier) {
            Map<BlockPos, CampBlueprint.Placement> last = new HashMap<>();
            for (CampBlueprint.Placement op : CampBlueprint.plan(pack.id, pack.family, pack.color, List.of(), pack.siteFacing, 0, tier).placements()) {
                last.put(at(op), op);
            }
            return last;
        }

        BlockPos find(int tier, CampBlueprint.Material material) {
            for (Map.Entry<BlockPos, CampBlueprint.Placement> e : built(tier).entrySet()) {
                if (e.getValue().material() == material) return e.getKey();
            }
            throw new AssertionError("no " + material);
        }
    }

    /** A camp built for real in a normal dimension, by the operator command's path. */
    private static Camp camp() {
        return camp(SettlementTier.CAMP);
    }

    private static Camp camp(SettlementTier tier) {
        FakeLevel level = EarthWorldFixture.level(70, -64, 320);
        level.key = Level.OVERWORLD;
        level.cubes = null;
        FeudData data = new FeudData();
        Pack pack = new Pack(new UUID(7, 8), Family.ROTTEN, "minecraft:overworld", "Test", 14, X, Z, 0);
        data.feud.packs.put(pack.id, pack);
        pack.siteSet = true;
        pack.siteX = X;
        pack.siteY = 71;
        pack.siteZ = Z;
        pack.tier = tier;
        Camp camp = new Camp(level, data, pack, new CampBuilder(), new long[]{0});
        camp.run(camp.builder.queueBuild(level, data.feud, pack, 0, tier.level, false, 0, null));
        assertEquals(tier.level, pack.builtTier);
        return camp;
    }

    private static boolean is(FakeLevel level, BlockPos pos, Block block) {
        return level.getBlockState(pos).is(block);
    }

    @Test
    void blocksPlayersPutInACampsPlaceSurviveRuinRepairAndUpgrade() {
        Camp camp = camp();
        FakeLevel level = camp.level;
        BlockPos fire = camp.find(1, CampBlueprint.Material.CAMPFIRE);
        BlockPos wool = camp.find(1, CampBlueprint.Material.WOOL_PRIMARY);
        BlockPos banner = camp.find(1, CampBlueprint.Material.BANNER_STANDING);
        assertTrue(level.getBlockState(fire).getBlock() instanceof CampfireBlock, "the camp was built");
        level.setBlock(fire, Blocks.BRICKS.defaultBlockState(), 3);
        level.setBlock(wool, Blocks.GLASS.defaultBlockState(), 3);
        level.setBlock(banner, Blocks.CHEST.defaultBlockState(), 3);
        ((Container) level.getBlockEntity(banner)).setItem(0, new ItemStack(Items.DIAMOND, 37));

        Feud feud = camp.data.feud;
        int woolBefore = count(level, camp.built(1), "_wool");
        camp.pack.ruined = true;
        camp.run(camp.builder.queueRuin(level, feud, camp.pack, 1, false, camp.clock[0], null));
        assertTrue(camp.pack.builtRuined);
        assertPlayerBlocksStand(level, fire, wool, banner, "ruin");
        assertTrue(count(level, camp.built(1), "_wool") < woolBefore - 1, "the rest of the camp still fell to ruin");
        camp.pack.ruined = false;
        camp.run(camp.builder.queueRepair(level, feud, camp.pack, 1, false, camp.clock[0], null));
        assertFalse(camp.pack.builtRuined);
        assertPlayerBlocksStand(level, fire, wool, banner, "repair");
        camp.pack.tier = SettlementTier.OUTPOST;
        camp.run(camp.builder.queueBuild(level, feud, camp.pack, 1, 2, false, camp.clock[0], null));
        assertEquals(2, camp.pack.builtTier);
        assertPlayerBlocksStand(level, fire, wool, banner, "upgrade");
    }

    @Test
    void anUpgradeThatRebuildsAnEditedSpotLeavesThePlayersBlockAndFillsTheNewStash() {
        Camp camp = camp();
        FakeLevel level = camp.level;
        Feud feud = camp.data.feud;
        camp.pack.tier = SettlementTier.OUTPOST;
        camp.run(camp.builder.queueBuild(level, feud, camp.pack, 1, 2, false, camp.clock[0], null));
        Set<CampBlueprint.Spot> outpost = CampBlueprint.ownedPositions(camp.pack.id, camp.pack.family, camp.pack.color,
                List.of(), camp.pack.siteFacing, 2);
        BlockPos edited = null;
        for (CampBlueprint.Placement op : CampBlueprint.plan(camp.pack.id, camp.pack.family, camp.pack.color, List.of(),
                camp.pack.siteFacing, 2, 3).placements()) {
            CampBlueprint.Material m = op.material();
            if (m == CampBlueprint.Material.AIR || m == CampBlueprint.Material.FOUNDATION_COLUMN) continue;
            BlockPos at = camp.at(op);
            if (outpost.contains(new CampBlueprint.Spot(op.rx(), op.ry(), op.rz())) && !level.getBlockState(at).isAir()) {
                edited = at;
                break;
            }
        }
        assertNotNull(edited, "the stronghold rebuilds a spot the outpost already holds");
        level.setBlock(edited, Blocks.BRICKS.defaultBlockState(), 3);
        camp.pack.tier = SettlementTier.STRONGHOLD;
        camp.run(camp.builder.queueBuild(level, feud, camp.pack, 2, 3, false, camp.clock[0], null));
        assertEquals(3, camp.pack.builtTier);
        assertTrue(is(level, edited, Blocks.BRICKS), "the stronghold left the player's bricks at " + edited);
        BlockPos stash = camp.find(3, CampBlueprint.Material.CHEST_STRONGHOLD);
        assertFalse(((Container) level.getBlockEntity(stash)).isEmpty(), "the new stronghold stash holds loot");
    }

    @Test
    void aPlayersFurnaceOnTheStashSpotSurvivesRepair() {
        Camp camp = camp();
        FakeLevel level = camp.level;
        BlockPos stash = camp.find(1, CampBlueprint.Material.CHEST_CAMP);
        level.setBlock(stash, Blocks.FURNACE.defaultBlockState(), 3);
        camp.pack.ruined = true;
        camp.run(camp.builder.queueRuin(level, camp.data.feud, camp.pack, 1, false, camp.clock[0], null));
        camp.pack.ruined = false;
        camp.run(camp.builder.queueRepair(level, camp.data.feud, camp.pack, 1, false, camp.clock[0], null));
        assertTrue(is(level, stash, Blocks.FURNACE), "the furnace is still there, not a camp chest");
    }

    @Test
    void aFenceAtAVassalPillarsHeightSurvivesRepair() {
        Camp camp = camp(SettlementTier.TOWN);
        FakeLevel level = camp.level;
        Feud feud = camp.data.feud;
        assertTrue(camp.pack.skippedZones.isEmpty(), "the whole town stands, vassal posts included");
        CampBlueprint.Spot slot = CampBlueprint.vassalBannerSpots(camp.pack.siteFacing).get(0);
        // No vassal flies here, so the camp keeps this spot clear; only a vassal's pillar would ever stand at y 0.
        BlockPos pillarHeight = new BlockPos(camp.pack.siteX + slot.rx(), camp.pack.siteY, camp.pack.siteZ + slot.rz());
        Block fence = io.github.pirut.bloodfeud.game.camp.FamilyPalette.of(camp.pack.family).fence;
        level.setBlock(pillarHeight, fence.defaultBlockState(), 3);
        camp.pack.ruined = true;
        camp.run(camp.builder.queueRuin(level, feud, camp.pack, 4, false, camp.clock[0], null));
        camp.pack.ruined = false;
        camp.run(camp.builder.queueRepair(level, feud, camp.pack, 4, false, camp.clock[0], null));
        assertTrue(is(level, pillarHeight, fence), "the player's fence where only a pillar belongs is still there");
    }

    private static void assertPlayerBlocksStand(FakeLevel level, BlockPos fire, BlockPos wool, BlockPos banner, String after) {
        assertTrue(is(level, fire, Blocks.BRICKS), "bricks where the campfire was, after " + after);
        assertTrue(is(level, wool, Blocks.GLASS), "glass where tent wool was, after " + after);
        assertTrue(is(level, banner, Blocks.CHEST), "chest where the banner was, after " + after);
        ItemStack kept = ((Container) level.getBlockEntity(banner)).getItem(0);
        assertTrue(kept.is(Items.DIAMOND) && kept.getCount() == 37, "its diamonds, after " + after);
    }

    @Test
    void anUntouchedCampStillRuinsAndRepairsFully() {
        Camp camp = camp();
        FakeLevel level = camp.level;
        Map<BlockPos, CampBlueprint.Placement> intact = camp.built(1);
        BlockPos fire = camp.find(1, CampBlueprint.Material.CAMPFIRE);
        BlockPos banner = camp.find(1, CampBlueprint.Material.BANNER_STANDING);
        int woolBefore = count(level, intact, "_wool");

        camp.pack.ruined = true;
        camp.run(camp.builder.queueRuin(level, camp.data.feud, camp.pack, 1, false, camp.clock[0], null));
        assertTrue(camp.pack.skippedZones.isEmpty(), "nothing of its own counted as a player's: " + camp.pack.skippedZones);
        assertTrue(camp.pack.skippedColumns.isEmpty());
        assertFalse(level.getBlockState(fire).getValue(CampfireBlock.LIT), "the fire is out");
        assertFalse(level.getBlockState(banner).getBlock() instanceof BannerBlock && count(level, intact, "_wool") == woolBefore,
                "the camp really fell to ruin");
        assertTrue(count(level, intact, "_wool") < woolBefore, "tents came down");

        camp.pack.ruined = false;
        camp.run(camp.builder.queueRepair(level, camp.data.feud, camp.pack, 1, false, camp.clock[0], null));
        assertTrue(level.getBlockState(fire).getValue(CampfireBlock.LIT), "the fire burns again");
        assertEquals(woolBefore, count(level, intact, "_wool"), "tents are back");
        int right = 0, all = 0;
        for (Map.Entry<BlockPos, CampBlueprint.Placement> e : intact.entrySet()) {
            CampBlueprint.Material m = e.getValue().material();
            if (m == CampBlueprint.Material.AIR || m == CampBlueprint.Material.FOUNDATION_COLUMN) continue;
            all++;
            BlockState want = io.github.pirut.bloodfeud.game.camp.FamilyPalette.of(camp.pack.family).resolve(e.getValue(), camp.pack.color);
            if (level.getBlockState(e.getKey()).is(want.getBlock())) right++;
        }
        assertTrue(right >= all - 2, "repaired " + right + " of " + all);
    }

    private static int count(FakeLevel level, Map<BlockPos, CampBlueprint.Placement> spots, String suffix) {
        int n = 0;
        for (BlockPos p : spots.keySet()) {
            if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(p).getBlock()).getPath().endsWith(suffix.substring(1))) n++;
        }
        return n;
    }

    @Test
    void aCampStashHoldsLootAndARepairNeverRefillsIt() {
        Camp camp = camp();
        FakeLevel level = camp.level;
        BlockPos chest = camp.find(1, CampBlueprint.Material.CHEST_CAMP);
        Container stash = (Container) level.getBlockEntity(chest);
        assertNotNull(stash, "a chest stands there");
        assertFalse(stash.isEmpty(), "the camp stash holds loot");
        ItemStack first = null;
        int stacks = 0;
        for (int i = 0; i < stash.getContainerSize(); i++) {
            if (!stash.getItem(i).isEmpty()) {
                stacks++;
                if (first == null) first = stash.getItem(i).copy();
            }
        }

        camp.pack.ruined = true;
        camp.run(camp.builder.queueRuin(level, camp.data.feud, camp.pack, 1, false, camp.clock[0], null));
        camp.pack.ruined = false;
        camp.run(camp.builder.queueRepair(level, camp.data.feud, camp.pack, 1, false, camp.clock[0], null));
        Container after = (Container) level.getBlockEntity(chest);
        int kept = 0;
        for (int i = 0; i < after.getContainerSize(); i++) if (!after.getItem(i).isEmpty()) kept++;
        assertEquals(stacks, kept, "the loot is still there after ruin and repair");

        for (int i = 0; i < after.getContainerSize(); i++) after.setItem(i, ItemStack.EMPTY); // a player took it all
        camp.pack.ruined = true;
        camp.run(camp.builder.queueRuin(level, camp.data.feud, camp.pack, 1, false, camp.clock[0], null));
        camp.pack.ruined = false;
        camp.run(camp.builder.queueRepair(level, camp.data.feud, camp.pack, 1, false, camp.clock[0], null));
        assertTrue(((Container) level.getBlockEntity(chest)).isEmpty(), "a repair doesn't refill the stash");
        assertNotNull(first);
    }
}
