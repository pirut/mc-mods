package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.feud.Family;
import io.github.pirut.bloodfeud.feud.Feud;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.SettlementTier;
import io.github.pirut.bloodfeud.game.EarthWorldFixture.FakeLevel;
import io.github.pirut.bloodfeud.game.camp.CampBlueprint;
import io.github.pirut.bloodfeud.game.camp.CampBuilder;
import io.github.pirut.bloodfeud.game.camp.SiteFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static io.github.pirut.bloodfeud.game.EarthWorldFixture.X;
import static io.github.pirut.bloodfeud.game.EarthWorldFixture.Z;
import static org.junit.jupiter.api.Assertions.*;

/** The ways Blood Feud 0.1.0-alpha.5 broke in an Earth Platform world. */
class EarthCompatTest {
    private static final int HIGH = 1530, BELOW_SEA = -280;

    @BeforeAll
    static void bootstrap() {
        EarthWorldFixture.bootstrap();
    }

    private static FakeLevel earth(int ground) {
        return EarthWorldFixture.level(ground, 0, 16);
    }

    private static Pack pack(FakeLevel level) {
        return new Pack(new UUID(1, 2), Family.ROTTEN, level.key.location().toString(), "Test", 14, X, Z, 0);
    }

    @Test
    void unloadedCubesAreNeverReadAsEmpty() {
        // With Sable (-64..320) the range lets the scan run, and every unloaded block reads as replaceable void air.
        for (int[] range : new int[][]{{0, 16}, {-64, 320}}) {
            FakeLevel level = EarthWorldFixture.level(70, range[0], range[1]);
            BlockPos origin = new BlockPos(X, 71, Z);
            assertTrue(SiteFinder.isPlayerBlockAt(level, origin, origin, Set.of(), Set.of()), "unloaded block");
            assertTrue(SiteFinder.hasPlayerBuild(level, origin, 7, Set.of(), Set.of()), "unloaded footprint");
            assertTrue(SiteFinder.isZoneBlockedByPlayer(level, origin, new CampBlueprint.Box(-2, 0, -2, 2, 4, 2),
                    Set.of(), Set.of()), "unloaded zone");
            assertTrue(SiteFinder.hasPlayerBlockInColumn(level, origin, 1, 1, 0, 4, Set.of(), Set.of()), "unloaded column");
        }
    }

    @Test
    void oneUnloadedCubeInAFootprintBlocksIt() {
        FakeLevel level = earth(HIGH);
        BlockPos origin = new BlockPos(X, HIGH + 1, Z);
        level.load(origin, 16, 24, 24);
        assertFalse(SiteFinder.hasPlayerBuild(level, origin, 7, Set.of(), Set.of()), "open ground");
        level.cubes.loaded.remove(new BlockPos(X >> 4, (HIGH + 6) >> 4, Z >> 4));
        assertTrue(SiteFinder.hasPlayerBuild(level, origin, 7, Set.of(), Set.of()));
    }

    @Test
    void playerBuildsAreProtectedAtAnyHeight() {
        for (int ground : new int[]{BELOW_SEA, 70, HIGH, 8800}) {
            FakeLevel level = earth(ground);
            BlockPos origin = new BlockPos(X, ground + 1, Z);
            level.load(origin, 16, 24, 24);
            level.blocks.put(origin.offset(2, 1, 2), Blocks.CHEST.defaultBlockState());
            assertTrue(SiteFinder.hasPlayerBuild(level, origin, 7, Set.of(), Set.of()), "footprint at " + ground);
            assertTrue(SiteFinder.isZoneBlockedByPlayer(level, origin, new CampBlueprint.Box(0, 0, 0, 3, 3, 3),
                    Set.of(), Set.of()), "zone at " + ground);
            assertTrue(SiteFinder.hasPlayerBlockInColumn(level, origin, 2, 2, 0, 3, Set.of(), Set.of()), "column at " + ground);
        }
    }

    @Test
    void solidGroundBelowSeaLevelOrHighUpIsNotAnAbyss() {
        for (int ground : new int[]{BELOW_SEA, HIGH}) {
            FakeLevel level = earth(ground);
            BlockPos origin = new BlockPos(X, ground + 1, Z);
            level.load(origin, 16, 24, 24);
            CampBlueprint.Box box = new CampBlueprint.Box(-3, 0, -3, 3, 4, 3);
            assertFalse(SiteFinder.isZoneOverAbyss(level, origin, box, CampBuilder.MAX_FOUNDATION_DEPTH), "ground at " + ground);
        }
    }

    @Test
    void findsSitesFromBelowSeaLevelToTheHimalayas() {
        for (int ground : new int[]{BELOW_SEA, 70, HIGH, 8800}) {
            FakeLevel level = earth(ground);
            level.load(new BlockPos(X, ground + 1, Z), 48, 32, 40);
            assertEquals(ground + 1, SiteFinder.findSurfaceY(level, X, Z), "surface");
            SiteFinder.Site site = SiteFinder.findAutoSite(level, pack(level), SettlementTier.CAMP);
            assertNotNull(site, "site at " + ground);
            assertEquals(new BlockPos(X, ground + 1, Z), site.origin());
        }
    }

    @Test
    void neverAsksTheHeightOfAColumnThatWouldHaveToDownload() {
        FakeLevel level = earth(HIGH);
        assertNull(SiteFinder.findSurfaceY(level, X, Z));
        assertNull(SiteFinder.findAutoSite(level, pack(level), SettlementTier.CAMP));
        assertEquals(0, level.unknownHeightReads.get(), "getHeight on an unknown column hung the server for a minute");
        assertEquals(0, level.serverThreadMapReads.get());
    }

    @Test
    void automaticBuildsDoNotQueueOverCubesTheyCannotSee() {
        FakeLevel level = earth(HIGH);
        Pack pack = pack(level);
        pack.siteSet = true;
        pack.siteX = X;
        pack.siteY = HIGH + 1;
        pack.siteZ = Z;
        CampBuilder.QueueResult result = new CampBuilder().queueBuild(level, new Feud(new Random(1)), pack, 0, 1, true, 0, null);
        assertFalse(result.queued());
        assertTrue(pack.skippedZones.isEmpty(), "waiting for cubes must not write zones off");
        assertTrue(pack.skippedColumns.isEmpty());
    }

    @Test
    void aCampOnlyCountsAsBuiltOnceItsBlocksAreInTheWorld() {
        FakeLevel level = earth(HIGH);
        FeudData data = new FeudData();
        Pack pack = pack(level);
        data.feud.packs.put(pack.id, pack);
        pack.siteSet = true;
        pack.siteX = X;
        pack.siteY = HIGH + 1;
        pack.siteZ = Z;
        CampBuilder builder = new CampBuilder();
        assertTrue(builder.queueBuild(level, data.feud, pack, 0, 1, false, 0, null).queued());
        for (int t = 0; t < 20; t++) builder.tick(level.getServer(), data, t);
        assertEquals(0, pack.builtTier, "a phantom camp");
        assertTrue(level.blocks.isEmpty());

        level.load(new BlockPos(X, HIGH + 1, Z), 20, 28, 32);
        for (int t = 20; t < 60; t++) builder.tick(level.getServer(), data, t);
        assertEquals(1, pack.builtTier);
        assertTrue(level.blocks.size() > 50, "the camp's blocks are really there");
        assertTrue(level.blocks.values().stream().anyMatch(s -> s.is(Blocks.CHEST)), "with its stash");
    }

    @Test
    void ringingBellsAreFoundInCubes() throws Exception {
        FakeLevel level = earth(HIGH);
        BlockPos at = new BlockPos(X + 4, HIGH + 1, Z);
        BellBlockEntity bell = new BellBlockEntity(at, Blocks.BELL.defaultBlockState());
        bell.shaking = true;
        level.cubes.load(at.getX(), at.getY(), at.getZ()).blockEntities().put(at, bell);
        Method ringing = Effects.class.getDeclaredMethod("ringingBell", net.minecraft.server.level.ServerLevel.class, BlockPos.class);
        ringing.setAccessible(true);
        Object found;
        try {
            found = ringing.invoke(null, level, new BlockPos(X, HIGH + 1, Z));
        } catch (java.lang.reflect.InvocationTargetException carrierOnly) {
            found = null; // it went to the carrier chunk, which holds no block entities
        }
        assertEquals(at, found);
    }

    @Test
    void settlementsRecordedByOlderVersionsOnTheEarthAreNotOwned() throws Exception {
        Pack earth = load(1, "earthplatform:cubic");
        assertFalse(earth.siteSet);
        assertEquals(0, earth.builtTier);
        assertTrue(earth.skippedZones.isEmpty());
    }

    @Test
    void otherSettlementsAreKept() throws Exception {
        for (Pack kept : new Pack[]{load(1, "minecraft:overworld"), load(2, "earthplatform:cubic")}) {
            assertTrue(kept.siteSet);
            assertEquals(1, kept.builtTier);
            assertEquals(1531, kept.siteY);
        }
    }

    @Test
    void vanillaDimensionsAreUntouched() {
        FakeLevel level = EarthWorldFixture.level(70, -64, 320);
        level.key = Level.OVERWORLD;
        level.cubes = null;
        BlockPos origin = new BlockPos(X, 71, Z);
        assertEquals(71, SiteFinder.findSurfaceY(level, X, Z));
        assertFalse(SiteFinder.hasPlayerBuild(level, origin, 7, Set.of(), Set.of()));
        level.blocks.put(origin, Blocks.CHEST.defaultBlockState());
        assertTrue(SiteFinder.hasPlayerBuild(level, origin, 7, Set.of(), Set.of()));
    }

    private static Pack load(int version, String dimension) throws Exception {
        CompoundTag pack = new CompoundTag();
        pack.putUUID("id", new UUID(3, 4));
        pack.putString("family", Family.ROTTEN.key());
        pack.putString("dimension", dimension);
        pack.putString("tier", SettlementTier.CAMP.key);
        pack.putInt("builtTier", 1);
        pack.putBoolean("siteSet", true);
        pack.putInt("siteY", 1531);
        ListTag packs = new ListTag();
        packs.add(pack);
        CompoundTag tag = new CompoundTag();
        tag.putInt("version", version);
        tag.put("packs", packs);
        Method load = FeudData.class.getDeclaredMethod("load", CompoundTag.class, HolderLookup.Provider.class);
        load.setAccessible(true);
        return ((FeudData) load.invoke(null, tag, null)).feud.packs.values().iterator().next();
    }
}
