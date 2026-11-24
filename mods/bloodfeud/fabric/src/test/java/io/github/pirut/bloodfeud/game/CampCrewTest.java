package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.BloodFeud;
import io.github.pirut.bloodfeud.feud.Family;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.SettlementTier;
import io.github.pirut.bloodfeud.game.EarthWorldFixture.FakeLevel;
import io.github.pirut.bloodfeud.game.camp.CampBlueprint;
import io.github.pirut.bloodfeud.game.camp.CampBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static io.github.pirut.bloodfeud.game.EarthWorldFixture.X;
import static io.github.pirut.bloodfeud.game.EarthWorldFixture.Z;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A pack's settlement is built by its crew while a player can see the site, and off screen while nobody can; one saved
 * build either way, from the same step, and never over player blocks. Up to 0.1.0-beta.1 automatic camps were only
 * built once every player was past a standoff distance, so a player who stayed near never got one.
 */
class CampCrewTest {
    @BeforeAll
    static void bootstrap() {
        EarthWorldFixture.bootstrap();
    }

    @AfterEach
    void defaults() {
        BloodFeud.config().campBlocksPerMinute = 60;
        BloodFeud.config().buildCamps = true;
    }

    private static final class Site {
        final FakeLevel level;
        FeudData data;
        Pack pack;
        CampBuilder builder;
        /** Whether a player can see the site. */
        boolean seen = true;
        long clock = 1000;

        Site(FakeLevel level, FeudData data, Pack pack) {
            this.level = level;
            this.data = data;
            this.pack = pack;
            this.builder = newBuilder();
        }

        CampBuilder newBuilder() {
            CampBuilder b = new CampBuilder();
            b.setSight((server, lvl, origin, radius) -> seen);
            return b;
        }

        void tick(int ticks) {
            for (int i = 0; i < ticks; i++) {
                builder.tick(level.getServer(), data, clock);
                if (clock % 40 == 17 || builder.isWaitingForCubes()) builder.checkAutoBuilds(level.getServer(), data, clock, clock % 40 == 17);
                clock++;
            }
        }

        int done() {
            return pack.works == null ? -1 : pack.works.done;
        }

        BlockPos at(CampBlueprint.Placement op) {
            return new BlockPos(pack.siteX + op.rx(), pack.siteY + op.ry(), pack.siteZ + op.rz());
        }

        BlockPos find(CampBlueprint.Material material) {
            BlockPos found = null;
            for (CampBlueprint.Placement op : CampBlueprint.plan(pack.id, pack.family, pack.color, List.of(), pack.siteFacing, 0, 1).placements()) {
                if (op.material() == material) found = at(op);
            }
            assertNotNull(found, "no " + material);
            return found;
        }

        void assertCampStands() {
            assertEquals(1, pack.builtTier);
            assertNull(pack.works);
            BlockState fire = level.getBlockState(find(CampBlueprint.Material.CAMPFIRE));
            assertTrue(fire.getBlock() instanceof CampfireBlock, "campfire built, got " + fire);
            assertTrue(level.getBlockState(find(CampBlueprint.Material.WOOL_PRIMARY)).getBlock().toString().contains("wool"));
            Container stash = (Container) level.getBlockEntity(find(CampBlueprint.Material.CHEST_CAMP));
            assertFalse(stash.isEmpty(), "the stash was filled as it was placed");
        }

        /** Saves the feud and reads it back, as a restart does, with a new builder. */
        void restart() throws Exception {
            CompoundTag saved = data.save(new CompoundTag(), null);
            Method load = FeudData.class.getDeclaredMethod("load", CompoundTag.class, HolderLookup.Provider.class);
            load.setAccessible(true);
            data = (FeudData) load.invoke(null, saved, null);
            pack = data.feud.pack(pack.id);
            builder = newBuilder();
        }
    }

    /** A pack whose camp site is chosen, on flat ground in a normal dimension. Peaceful, so its crew goes unseen. */
    private static Site site() {
        FakeLevel level = EarthWorldFixture.level(70, -64, 320);
        level.key = Level.OVERWORLD;
        level.cubes = null;
        level.difficulty = Difficulty.PEACEFUL;
        FeudData data = new FeudData();
        Pack pack = new Pack(new UUID(5, 6), Family.ROTTEN, "minecraft:overworld", "Test", 3, X, Z, 0);
        pack.grunts = 3;
        pack.siteSet = true;
        pack.siteX = X;
        pack.siteY = 71;
        pack.siteZ = Z;
        data.feud.packs.put(pack.id, pack);
        return new Site(level, data, pack);
    }

    @Test
    void inViewTheCrewBuildsAtItsPaceAndFinishesTheCamp() {
        Site s = site();
        s.tick(60);
        assertNotNull(s.pack.works, "a crew started on the camp while a player could see it");
        assertEquals(0, s.pack.builtTier);
        int total = s.pack.works.total;
        s.tick(200); // ten more seconds: three builders place about 30 blocks
        int placed = s.level.placeSounds;
        assertTrue(placed >= 20 && placed <= 45, "about three blocks a second, placed " + placed);
        assertTrue(s.done() > 0 && s.done() < total, "part built: " + s.done() + "/" + total);
        assertTrue(s.level.chunks.tickets.isEmpty(), "nothing is held loaded for a build someone can see");
        s.tick(20 * 400);
        s.assertCampStands();
    }

    @Test
    void aFasterSettingBuildsFaster() {
        Site s = site();
        BloodFeud.config().campBlocksPerMinute = 600;
        s.tick(60 + 200);
        assertTrue(s.level.placeSounds > 150, "ten times the pace, placed " + s.level.placeSounds);
    }

    @Test
    void outOfSightItIsBuiltOffScreenOnGroundLoadedJustForTheBuild() {
        Site s = site();
        s.seen = false;
        s.level.chunks.loaded = false; // no player near: only a ticket loads the site
        s.tick(80);
        s.assertCampStands();
        assertEquals(0, s.level.placeSounds, "built off screen, not block by block");
        assertTrue(s.level.chunks.tickets.isEmpty(), "and the ground was let go again");
    }

    @Test
    void itNeverWritesWhileTheSiteCantBeLoaded() {
        Site s = site();
        s.seen = false;
        s.level.chunks.loaded = false;
        s.level.chunks.ticketsLoad = false; // the ticket's chunks never arrive
        s.tick(20 * 120);
        assertEquals(0, s.pack.builtTier);
        assertTrue(s.level.blocks.isEmpty(), "nothing written to unloaded ground");
        s.level.chunks.ticketsLoad = true;
        s.tick(20 * 150); // it gave up for a while when the ground didn't come, then tried again
        s.assertCampStands();
    }

    @Test
    void aBuildInViewFinishesOffScreenWhenEveryoneLeavesFromTheSameStep() {
        Site s = site();
        s.tick(260);
        int done = s.done();
        int sounds = s.level.placeSounds;
        assertTrue(done > 0 && s.pack.builtTier == 0);
        s.seen = false; // the player walked off, past what they can see
        s.level.chunks.loaded = false;
        s.tick(1);
        assertTrue(s.pack.works == null || s.done() >= done, "never back to an earlier step");
        s.tick(80);
        s.assertCampStands();
        assertEquals(sounds, s.level.placeSounds, "the rest went up off screen");
    }

    @Test
    void anOffScreenBuildCarriesOnInViewWhenSomeoneComesNear() {
        Site s = site();
        s.tick(100);
        int done = s.done();
        s.seen = false;
        s.level.chunks.loaded = false;
        s.level.chunks.ticketsLoad = false; // off screen, but its ground can't load: it waits where it was
        s.tick(200);
        assertEquals(done, s.done());
        s.seen = true; // someone walks into range: their view loads the site
        s.level.chunks.loaded = true;
        s.level.chunks.ticketsLoad = true;
        int sounds = s.level.placeSounds;
        s.tick(200);
        assertTrue(s.done() > done, "the crew carried on from step " + done + ", now " + s.done());
        assertTrue(s.level.placeSounds > sounds, "in view, block by block");
        assertTrue(s.level.chunks.tickets.isEmpty(), "with nothing held loaded for it");
        s.tick(20 * 400);
        s.assertCampStands();
    }

    @Test
    void aSavedBuildPicksUpAfterARestartFromTheSameStep() throws Exception {
        Site s = site();
        s.tick(260);
        int done = s.done();
        assertTrue(done > 0);
        s.restart();
        assertNotNull(s.pack.works, "the build was saved with the pack");
        assertEquals(done, s.done(), "with how far it got");
        s.seen = false;
        s.level.chunks.loaded = false;
        s.tick(80);
        s.assertCampStands();
    }

    @Test
    void killingTheCrewStallsTheBuildInViewAndOffScreen() {
        Site s = site();
        s.tick(100);
        int done = s.done();
        s.pack.works.stalledUntil = s.clock + CampBuilder.BUILDERS_RETURN_TICKS;
        s.tick(20 * 60);
        assertEquals(done, s.done(), "no builders, no building");
        s.seen = false; // the player who killed them walks off
        s.level.chunks.loaded = false;
        s.tick(20 * 60);
        assertEquals(done, s.done(), "not off screen either");
        assertTrue(s.level.chunks.tickets.isEmpty(), "and nothing is held loaded while it waits");
        s.tick((int) CampBuilder.BUILDERS_RETURN_TICKS);
        s.assertCampStands();
    }

    @Test
    void aPlayersBlockInTheSiteIsLeftAloneInViewAndOffScreen() {
        for (boolean inView : new boolean[]{true, false}) {
            Site s = site();
            BlockPos fire = s.find(CampBlueprint.Material.CAMPFIRE);
            BlockPos wool = s.find(CampBlueprint.Material.WOOL_PRIMARY);
            s.tick(60);
            assertNotNull(s.pack.works);
            s.level.setBlock(fire, Blocks.BRICKS.defaultBlockState(), 3); // a player builds while the crew works
            s.level.setBlock(wool, Blocks.GLASS.defaultBlockState(), 3);
            s.seen = inView;
            s.tick(20 * 400);
            assertEquals(1, s.pack.builtTier);
            assertTrue(s.level.getBlockState(fire).is(Blocks.BRICKS), "the player's bricks stand, in view " + inView);
            assertTrue(s.level.getBlockState(wool).is(Blocks.GLASS), "the player's glass stands, in view " + inView);
            Container stash = (Container) s.level.getBlockEntity(s.find(CampBlueprint.Material.CHEST_CAMP));
            assertFalse(stash.isEmpty());
        }
    }

    @Test
    void aPackThatFallsToRuinMidBuildLetsItsBuildGo() {
        Site s = site();
        s.tick(100);
        assertNotNull(s.pack.works);
        s.pack.ruined = true;
        for (int i = 0; i < 80; i++) s.builder.tick(s.level.getServer(), s.data, s.clock++); // ruins are an off-screen job
        assertNull(s.pack.works, "a broken pack stops building");
        assertFalse(s.builder.isBuilding(s.pack.id));
        assertEquals(0, s.pack.builtTier);
        assertTrue(s.pack.buildStarted, "what its crew put up stays, for the ruin to take over");
    }

    private static Site earth() {
        FakeLevel level = EarthWorldFixture.level(1530, 0, 16);
        level.difficulty = Difficulty.PEACEFUL;
        FeudData data = new FeudData();
        Pack pack = new Pack(new UUID(9, 9), Family.BONES, "earthplatform:cubic", "High", 5, X, Z, 0);
        pack.grunts = 4;
        pack.siteSet = true;
        pack.siteX = X;
        pack.siteY = 1531;
        pack.siteZ = Z;
        data.feud.packs.put(pack.id, pack);
        return new Site(level, data, pack);
    }

    @Test
    void onTheEarthAPlayerInViewLoadsTheCubesAndTheCrewNeverAsksForThem() {
        Site s = earth();
        s.tick(400); // a player is near, but the site's cubes haven't reached them yet
        assertNull(s.pack.works, "nothing starts while its cubes are unloaded");
        assertTrue(s.level.blocks.isEmpty(), "nothing was written into void air");
        assertTrue(s.level.cubes.requested.isEmpty(), "and the crew asked for no cubes: the player's view loads them");
        s.level.load(new BlockPos(X, 1531, Z), 16, 24, 24);
        s.tick(20 * 400);
        s.assertCampStands();
    }

    @Test
    void onTheEarthOutOfSightItLoadsAndKeepsTheCubesForTheBuildThenLetsThemGo() throws InterruptedException {
        Site s = earth();
        s.seen = false;
        long until = System.nanoTime() + 20_000_000_000L;
        while (s.level.cubes.requested.isEmpty() && System.nanoTime() < until) { // first it reads the map data, aside
            s.tick(1);
            Thread.sleep(2);
        }
        assertTrue(s.level.blocks.isEmpty(), "nothing written while the cubes are unloaded");
        assertFalse(s.level.cubes.requested.isEmpty(), "it asked for the site's cubes");
        assertFalse(s.level.cubes.kept.isEmpty(), "and kept them for the build, where Earth Platform can");
        while (s.pack.builtTier == 0 && System.nanoTime() < until) {
            s.level.cubes.deliver(); // the workers deliver what was asked for
            s.tick(1);
            Thread.sleep(2);
        }
        s.assertCampStands();
        assertTrue(s.level.cubes.kept.isEmpty(), "every kept cube was let go");
        assertTrue(s.level.chunks.tickets.isEmpty());
    }

    @Test
    void aBiggerPackSendsABiggerCrew() {
        assertEquals(3, CampBuilder.crewSize(packWith(10), 1));
        assertEquals(6, CampBuilder.crewSize(packWith(10), 4));
        assertEquals(1, CampBuilder.crewSize(packWith(0), 3), "a pack with no grunts still sends one");
        assertEquals(3.0, CampBuilder.crewRate(3, 60), 1e-9, "three a second");
    }

    private static Pack packWith(int grunts) {
        Pack p = new Pack(new UUID(1, 1), Family.ROTTEN, "minecraft:overworld", "T", 0, 0, 0, 0);
        p.grunts = grunts;
        return p;
    }

    @Test
    void theLedgerSaysHowFarAlongABuildIs() {
        Site s = site();
        assertEquals("Camp (not built yet)", s.pack.settlementLabel(true));
        s.tick(260);
        assertTrue(s.pack.settlementLabel(true).startsWith("Camp, being built ("), s.pack.settlementLabel(true));
        s.pack.tier = SettlementTier.CAMP;
        s.tick(20 * 400);
        assertEquals("Camp", s.pack.settlementLabel(true));
    }
}
