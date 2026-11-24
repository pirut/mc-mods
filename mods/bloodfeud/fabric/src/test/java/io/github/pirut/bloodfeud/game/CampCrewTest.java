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
import java.util.Set;
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
        Earth.reset();
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
        assertFalse(s.level.chunks.tickets.isEmpty(), "an active build holds its site, seen or not");
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
        s.tick(20 * 400);
        s.assertCampStands();
        assertTrue(s.level.chunks.tickets.isEmpty(), "and what it held went with the build");
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
    void onTheEarthAPlayerWhoSeesOnlyAnEdgeOfTheSiteDoesntFreezeIt() throws InterruptedException {
        Site s = earth();
        // The player's view reaches one edge of the site; the rest of its cubes aren't sent to them.
        s.level.load(new BlockPos(X - 12, 1531, Z), 3, 24, 24);
        long until = System.nanoTime() + 20_000_000_000L;
        while (s.pack.builtTier == 0 && System.nanoTime() < until) {
            s.level.cubes.deliver(); // what the build asked for arrives
            s.tick(1);
            Thread.sleep(1);
        }
        s.assertCampStands();
        assertTrue(s.level.cubes.kept.isEmpty(), "and its holds were let go");
        assertTrue(s.level.chunks.tickets.isEmpty());
    }

    @Test
    void aPlayerWhoSeesOnlyAnEdgeOfAVanillaSiteDoesntFreezeIt() {
        Site s = site();
        s.level.chunks.loaded = false; // in view, but the player's view loads only an edge: a ticket loads the rest
        s.tick(20 * 400);
        s.assertCampStands();
        assertTrue(s.level.placeSounds > 0, "built in view, block by block");
        assertTrue(s.level.chunks.tickets.isEmpty(), "and the ticket went with the build");
    }

    @Test
    void buildsWhoseGroundNeverComesFreeTheirSlotsForOthers() {
        Site s = site();
        s.seen = false;
        s.level.chunks.loaded = false;
        Pack a = s.pack;
        Pack b = extraPack(s, new UUID(5, 7), X + 400);
        for (Pack p : List.of(a, b)) {
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
                s.level.chunks.neverLoad.add(net.minecraft.world.level.ChunkPos.asLong((p.siteX >> 4) + dx, (p.siteZ >> 4) + dz));
            }
        }
        s.tick(100);
        assertEquals(2, s.level.chunks.tickets.size(), "both took a slot");
        Pack c = extraPack(s, new UUID(5, 8), X + 800); // a third pack, whose ground is fine, comes after them
        s.tick(20 * 40);
        assertEquals(1, c.builtTier, "it got a slot once theirs timed out, and built");
        assertEquals(0, a.builtTier);
        assertEquals(0, b.builtTier);
        assertTrue(s.level.chunks.tickets.size() <= 2);
    }

    @Test
    void stalledJobsGiveUpTheirSlotsToo() throws Exception {
        Site s = site();
        Pack a = s.pack;
        Pack b = extraPack(s, new UUID(5, 7), X + 400);
        Pack c = extraPack(s, new UUID(5, 8), X + 800);
        s.tick(100); // all three crews start while a player sees them
        assertNotNull(a.works);
        assertNotNull(b.works);
        assertNotNull(c.works);
        s.restart(); // jobs that resume hold no ground yet
        a = s.data.feud.pack(a.id);
        b = s.data.feud.pack(b.id);
        c = s.data.feud.pack(c.id);
        s.seen = false;
        s.level.chunks.loaded = false;
        for (Pack p : List.of(a, b)) {
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
                s.level.chunks.neverLoad.add(net.minecraft.world.level.ChunkPos.asLong((p.siteX >> 4) + dx, (p.siteZ >> 4) + dz));
            }
        }
        s.tick(20 * 40);
        assertEquals(1, c.builtTier, "the third got a slot once the stuck two let theirs go");
        assertNotNull(a.works, "the stuck ones keep their saved builds, to try again later");
        assertNotNull(b.works);
    }

    @Test
    void everyBuildDrawsFromOneStepBudgetATick() {
        Site s = site();
        Pack b = extraPack(s, new UUID(5, 7), X + 400);
        s.tick(60);
        int before = s.done() + b.works.done;
        s.seen = false;
        s.tick(1); // both build off screen now
        int after = (s.pack.works == null ? s.pack.builtTier > 0 ? 1593 : 0 : s.done()) + (b.works == null ? 1593 : b.works.done);
        assertTrue(after - before <= 400, "at most 400 steps a tick for all builds together, took " + (after - before));
    }

    private static Pack extraPack(Site s, UUID id, int x) {
        Pack p = new Pack(id, Family.ROTTEN, "minecraft:overworld", "Other", 9, x, Z, 0);
        p.grunts = 3;
        p.siteSet = true;
        p.siteX = x;
        p.siteY = 71;
        p.siteZ = Z;
        s.data.feud.packs.put(p.id, p);
        return p;
    }

    @Test
    void onTheEarthHoldsGoWhenPrepTimesOut() {
        Site s = earth();
        s.seen = false;
        int owners = Earth.keptOwners();
        s.tick(100);
        assertFalse(s.level.cubes.kept.isEmpty(), "prep holds the site's cubes");
        assertEquals(1, s.builder.cubeHolds());
        s.tick(20 * 70); // the cubes never arrive: past the Earth's prep timeout
        assertTrue(s.level.cubes.kept.isEmpty(), "every hold went with the timeout");
        assertEquals(0, s.builder.cubeHolds());
        assertEquals(owners, Earth.keptOwners());
        assertTrue(s.level.chunks.tickets.isEmpty());
    }

    @Test
    void onTheEarthHoldsGoWhenThePackIsGone() {
        Site s = earth();
        s.seen = false;
        s.tick(100);
        assertFalse(s.level.cubes.kept.isEmpty());
        s.data.feud.packs.remove(s.pack.id); // disbanded
        s.tick(2);
        assertTrue(s.level.cubes.kept.isEmpty(), "the disbanded pack's holds went");
        assertEquals(0, s.builder.cubeHolds());
        assertTrue(s.level.chunks.tickets.isEmpty());
    }

    @Test
    void onTheEarthHoldsGoWhenTheServerStops() {
        Site s = earth();
        s.seen = false;
        s.tick(100);
        assertFalse(s.level.cubes.kept.isEmpty());
        s.builder.clearAll(s.level.getServer());
        assertTrue(s.level.cubes.kept.isEmpty());
        assertEquals(0, s.builder.cubeHolds());
    }

    @Test
    void aKeepThatFailsPartWayLetsGoOfWhatItTook() {
        Site s = earth();
        s.pack.tier = SettlementTier.STRONGHOLD; // a site big enough to need several holders
        s.seen = false;
        s.level.cubes.failKeepAfter = 1; // Earth Platform throws on the second holder
        s.tick(100);
        assertTrue(s.level.cubes.kept.isEmpty(), "the holder it did take was let go, though the Earth failed");
        assertEquals(0, s.builder.cubeHolds());
        assertEquals(0, Earth.keptOwners());
        assertTrue(s.level.blocks.isEmpty(), "and nothing was built on an Earth it can't work with");
    }

    @Test
    void aPlayersWoolAtASpotTheCampHasntReachedSurvivesARestart() throws Exception {
        Site s = site();
        BlockPos wool = s.find(CampBlueprint.Material.WOOL_PRIMARY);
        s.tick(60);
        assertNotNull(s.pack.works);
        assertTrue(s.level.getBlockState(wool).isAir(), "the camp hasn't reached its wool yet");
        s.level.setBlock(wool, Blocks.RED_WOOL.defaultBlockState(), 3); // a player's wool, not the pack's color
        s.restart();
        s.tick(20 * 400);
        assertEquals(1, s.pack.builtTier);
        assertTrue(s.level.getBlockState(wool).is(Blocks.RED_WOOL), "still the player's red wool, got " + s.level.getBlockState(wool));
    }

    @Test
    void aPlayersWoolInAnotherColorOnACampsWoolIsTheirs() {
        Site s = site();
        s.tick(20 * 400);
        BlockPos wool = s.find(CampBlueprint.Material.WOOL_PRIMARY);
        s.level.setBlock(wool, Blocks.RED_WOOL.defaultBlockState(), 3);
        s.pack.ruined = true;
        s.data.feud.packs.get(s.pack.id).ruined = true;
        CampBuilder manual = s.newBuilder();
        assertTrue(manual.queueRuin(s.level, s.data.feud, s.pack, 1, false, s.clock, null).queued());
        for (int i = 0; i < 400 && manual.isBuilding(s.pack.id); i++) manual.tick(s.level.getServer(), s.data, s.clock++);
        assertTrue(s.level.getBlockState(wool).is(Blocks.RED_WOOL), "a ruin leaves the player's red wool alone");
    }

    @Test
    void aPackRuinedMidUpgradeCoversWhatItBuiltOfTheUpgrade() {
        Site s = site();
        s.tick(20 * 400);
        assertEquals(1, s.pack.builtTier);
        s.pack.tier = SettlementTier.OUTPOST;
        s.pack.grunts = 6;
        java.util.Set<Long> campPlaced = new java.util.HashSet<>(s.pack.placed.keySet());
        java.util.Set<BlockPos> campSpots = new java.util.HashSet<>();
        for (CampBlueprint.Placement op : CampBlueprint.plan(s.pack.id, s.pack.family, s.pack.color, List.of(), s.pack.siteFacing, 0, 1).placements()) {
            campSpots.add(s.at(op));
        }
        java.util.Map<Long, CampBlueprint.Placement> upgradeOps = new java.util.HashMap<>();
        for (CampBlueprint.Placement op : CampBlueprint.plan(s.pack.id, s.pack.family, s.pack.color, List.of(), s.pack.siteFacing, 1, 2).placements()) {
            upgradeOps.put(BlockPos.asLong(op.rx(), op.ry(), op.rz()), op);
        }
        // Until the upgrade has put up some of the outpost's structure, not just its ground.
        for (int i = 0; i < 20 * 600; i++) {
            s.tick(1);
            if (s.pack.builtTier >= 2) break;
            if (s.pack.works == null) continue;
            long walls = s.pack.placed.keySet().stream().filter(k -> !campPlaced.contains(k) && upgradeOps.containsKey(k)
                    && upgradeOps.get(k).pass() == CampBlueprint.PASS_STRUCTURE).count();
            if (walls >= 5) break;
        }
        assertNotNull(s.pack.works, "the upgrade is under way");
        java.util.Set<Long> placed = new java.util.HashSet<>(s.pack.placed.keySet());
        placed.removeAll(campPlaced);
        assertFalse(placed.isEmpty());
        s.pack.ruined = true; // beaten down before the outpost was done
        for (int i = 0; i < 5; i++) s.builder.tick(s.level.getServer(), s.data, s.clock++);
        assertNull(s.pack.works);
        assertEquals(2, s.pack.builtTier, "the part-built outpost counts as built, for ruin and repair");
        CampBuilder manual = s.newBuilder();
        assertTrue(manual.queueRuin(s.level, s.data.feud, s.pack, 2, false, s.clock, null).queued());
        for (int i = 0; i < 400 && manual.isBuilding(s.pack.id); i++) manual.tick(s.level.getServer(), s.data, s.clock++);
        s.pack.ruined = false;
        assertTrue(manual.queueRepair(s.level, s.data.feud, s.pack, 2, false, s.clock, null).queued());
        for (int i = 0; i < 400 && manual.isBuilding(s.pack.id); i++) manual.tick(s.level.getServer(), s.data, s.clock++);
        assertTrue(s.pack.skippedZones.isEmpty(), "no part of the outpost was taken for a player's build: " + s.pack.skippedZones);
        java.util.Map<BlockPos, CampBlueprint.Placement> outpost = new java.util.HashMap<>();
        for (CampBlueprint.Placement op : CampBlueprint.plan(s.pack.id, s.pack.family, s.pack.color, List.of(), s.pack.siteFacing, 0, 2).placements()) {
            outpost.put(s.at(op), op);
        }
        java.util.Set<BlockPos> camp = new java.util.HashSet<>();
        for (CampBlueprint.Placement op : CampBlueprint.plan(s.pack.id, s.pack.family, s.pack.color, List.of(), s.pack.siteFacing, 0, 1).placements()) {
            camp.add(s.at(op));
        }
        io.github.pirut.bloodfeud.game.camp.FamilyPalette palette = io.github.pirut.bloodfeud.game.camp.FamilyPalette.of(s.pack.family);
        int checked = 0, restored = 0;
        for (long k : placed) {
            BlockPos at = new BlockPos(s.pack.siteX, s.pack.siteY, s.pack.siteZ).offset(BlockPos.of(k));
            CampBlueprint.Placement op = outpost.get(at);
            if (op == null || camp.contains(at) || op.pass() != CampBlueprint.PASS_STRUCTURE) continue;
            checked++;
            if (s.level.getBlockState(at).is(palette.resolve(op, s.pack.color).getBlock())) restored++;
        }
        assertTrue(checked > 0, "the upgrade had put up some of the outpost");
        assertEquals(checked, restored, "every outpost block it put up stands again after the ruin and repair");
    }

    @Test
    void twoPacksWithOneHomeDontShareASite() {
        Site s = site();
        s.pack.siteSet = false;
        Pack b = new Pack(new UUID(6, 6), Family.ROTTEN, "minecraft:overworld", "Twin", 9, X, Z, 0);
        b.grunts = 3;
        s.data.feud.packs.put(b.id, b);
        s.tick(20); // one scheduled pass
        assertTrue(s.pack.siteSet, "the first chose a site");
        if (!b.siteSet) return; // nothing near its home is clear of the first's reservation: it waits, not overlaps
        int apart = Math.max(Math.abs(s.pack.siteX - b.siteX), Math.abs(s.pack.siteZ - b.siteZ));
        // Each site reserves a Stronghold's ground; the two reservations don't overlap.
        assertTrue(apart > 2 * (SettlementTier.STRONGHOLD.radius + CampBlueprint.FOOTPRINT_MARGIN), "apart by " + apart);
    }

    @Test
    void aCrewClearsTheSiteTopDownBeforeItBuilds() {
        List<CampBlueprint.Placement> plan = CampBlueprint.plan(new UUID(5, 6), Family.ROTTEN, 3, List.of(), 0, 0, 1).placements();
        List<CampBlueprint.Placement> order = CampBuilder.crewOrder(plan);
        assertEquals(plan.size(), order.size());
        int clears = (int) plan.stream().filter(op -> op.material() == CampBlueprint.Material.AIR).count();
        assertTrue(clears > 0);
        for (int i = 0; i < order.size(); i++) {
            assertEquals(i < clears, order.get(i).material() == CampBlueprint.Material.AIR, "clearing comes first, at " + i);
            if (i > 0 && i < clears) assertTrue(order.get(i).ry() <= order.get(i - 1).ry(), "top down");
        }
        assertEquals(order, CampBuilder.crewOrder(plan), "the same order every time");
    }

    @Test
    void aBuilderOnlyReachesABlockItCanSee() {
        Site s = site();
        BlockPos target = new BlockPos(X + 3, 72, Z);
        net.minecraft.world.phys.Vec3 eye = new net.minecraft.world.phys.Vec3(X + 0.5, 72.6, Z + 0.5);
        assertTrue(CampBuilder.canReach(s.level, eye, target, null), "open ground, in reach");
        assertFalse(CampBuilder.canReach(s.level, eye, new BlockPos(X + 9, 72, Z), null), "too far");
        for (int y = 71; y <= 74; y++) for (int z = -2; z <= 2; z++) s.level.setBlock(new BlockPos(X + 2, y, Z + z), Blocks.STONE.defaultBlockState(), 3);
        assertFalse(CampBuilder.canReach(s.level, eye, target, null), "not through a wall");
        BlockPos spot = CampBuilder.vantage(s.level, target, eye, 1.62f);
        assertNotNull(spot, "a place to stand where it can reach it");
        net.minecraft.world.phys.Vec3 there = new net.minecraft.world.phys.Vec3(spot.getX() + 0.5, spot.getY() + 1.62, spot.getZ() + 0.5);
        assertTrue(CampBuilder.canReach(s.level, there, target, null));
        assertTrue(spot.getX() >= X + 2, "on the wall or past it, not behind it: " + spot.toShortString());
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

    // ------------------------------------------------------------------ round 2 of review: one invariant each

    @Test
    void anActiveBuildRenewsItsHoldsEvenWhenItsSiteIsLoaded() {
        Site s = site();
        BloodFeud.config().campBlocksPerMinute = 10; // a build far longer than a ticket lasts
        s.tick(60);
        assertNotNull(s.pack.works);
        int adds = s.level.chunks.ticketAdds;
        for (int i = 0; i < 5; i++) {
            s.tick(200);
            assertFalse(s.level.chunks.tickets.isEmpty(), "still held after " + (i + 1) * 10 + " s");
        }
        assertTrue(s.level.chunks.ticketAdds >= adds + 5, "renewed as it went: " + adds + " -> " + s.level.chunks.ticketAdds);
    }

    @Test
    void aCrewThatIsntGettingAnywhereKeepsItsSlot() {
        Site s = site();
        BloodFeud.config().campBlocksPerMinute = 1; // a crew busy fighting, or idle: next to no progress
        s.tick(60);
        assertNotNull(s.pack.works);
        for (int i = 0; i < 6; i++) {
            s.tick(200);
            assertEquals(1, s.level.chunks.tickets.size(), "its ground is loaded, so it keeps its slot (" + i + ")");
        }
    }

    @Test
    void aBuilderIsOnlyMovedWhereItCanStand() {
        Site s = site();
        BlockPos at = new BlockPos(X + 3, 71, Z);
        assertTrue(CampBuilder.safeToStand(s.level, at, 0.6f, 1.95f), "open ground");
        s.level.setBlock(at.above(), Blocks.STONE.defaultBlockState(), 3);
        assertFalse(CampBuilder.safeToStand(s.level, at, 0.6f, 1.95f), "walled in");
        s.level.setBlock(at.above(), Blocks.AIR.defaultBlockState(), 3);
        s.level.setBlock(at.below(), Blocks.AIR.defaultBlockState(), 3);
        assertFalse(CampBuilder.safeToStand(s.level, at, 0.6f, 1.95f), "its support is gone");
        s.level.setBlock(at.below(), Blocks.GRAVEL.defaultBlockState(), 3);
        s.level.chunks.loaded = false;
        assertFalse(CampBuilder.safeToStand(s.level, at, 0.6f, 1.95f), "not loaded");

        Site e = earth();
        BlockPos high = new BlockPos(X, 1531, Z);
        assertFalse(CampBuilder.safeToStand(e.level, high, 0.6f, 1.95f), "the Earth's void");
        e.level.load(high, 4, 4, 4);
        assertTrue(CampBuilder.safeToStand(e.level, high, 0.6f, 1.95f));
    }

    @Test
    void aStepNobodyCouldSeeIsLeftOutAndAnyOtherIsNot() {
        Site s = site();
        BlockPos buried = new BlockPos(X + 3, 65, Z);
        assertTrue(CampBuilder.enclosed(s.level, buried), "under the ground on every side");
        assertFalse(CampBuilder.enclosed(s.level, new BlockPos(X + 3, 70, Z)), "the top of the ground can be seen");
    }

    private static int stepOf(List<CampBlueprint.Placement> steps, CampBlueprint.Material material) {
        for (int i = 0; i < steps.size(); i++) if (steps.get(i).material() == material) return i;
        throw new AssertionError("no " + material);
    }

    @Test
    void deferredStepsWaitForEveryoneToLookAwayThenGoUpOffScreen() {
        Site s = site();
        List<CampBlueprint.Placement> steps = CampBuilder.crewOrder(CampBlueprint.plan(s.pack.id, s.pack.family,
                s.pack.color, List.of(), s.pack.siteFacing, 0, 1).placements());
        int fire = stepOf(steps, CampBlueprint.Material.CAMPFIRE);
        int stash = stepOf(steps, CampBlueprint.Material.CHEST_CAMP);
        Pack.Works w = new Pack.Works(false, 0, 1, s.pack.color, List.of(), Set.of(), Set.of());
        w.total = steps.size();
        w.done = steps.size(); // its crew got through everything but two steps it couldn't place
        w.deferred.add(fire);
        w.deferred.add(stash);
        s.pack.works = w;
        s.pack.buildStarted = true;
        s.tick(200);
        assertTrue(s.level.getBlockState(s.at(steps.get(fire))).isAir(), "deferred steps wait while someone can see the site");
        assertTrue(s.level.chunks.tickets.isEmpty(), "holding nothing while it waits");
        assertEquals(0, s.pack.builtTier);
        s.seen = false;
        s.level.chunks.loaded = false;
        s.tick(80);
        assertTrue(s.level.getBlockState(s.at(steps.get(fire))).getBlock() instanceof CampfireBlock, "then go up off screen");
        assertFalse(((Container) s.level.getBlockEntity(s.at(steps.get(stash)))).isEmpty(), "the stash filled as placed");
        assertEquals(1, s.pack.builtTier);
        assertTrue(s.level.chunks.tickets.isEmpty());
    }

    @Test
    void aFoundationTooDeepForItsBuilderIsFinishedOffScreen() {
        FakeLevel level = EarthWorldFixture.level(64, -64, 320); // the ground is six blocks under the site
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
        Site s = new Site(level, data, pack);
        List<CampBlueprint.Placement> steps = CampBuilder.crewOrder(CampBlueprint.plan(pack.id, pack.family,
                pack.color, List.of(), pack.siteFacing, 0, 1).placements());
        int column = stepOf(steps, CampBlueprint.Material.FOUNDATION_COLUMN);
        CampBlueprint.Placement op = steps.get(column);
        BlockPos top = s.at(op);
        net.minecraft.world.level.block.Block fill = io.github.pirut.bloodfeud.game.camp.FamilyPalette.of(pack.family).foundation;
        // Its builder filled the top two blocks it could reach, and deferred the rest.
        for (int d = 0; d < 2; d++) {
            level.setBlock(top.below(d), fill.defaultBlockState(), 3);
            pack.placed.put(BlockPos.asLong(op.rx(), op.ry() - d, op.rz()),
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(fill).toString());
        }
        Pack.Works w = new Pack.Works(false, 0, 1, pack.color, List.of(), Set.of(), Set.of());
        w.total = steps.size();
        w.done = steps.size();
        w.placedCount = 2;
        w.deferred.add(column);
        pack.works = w;
        pack.buildStarted = true;
        s.seen = false;
        level.chunks.loaded = false;
        s.tick(80);
        for (int y = top.getY(); y > 64; y--) {
            assertTrue(level.getBlockState(new BlockPos(top.getX(), y, top.getZ())).is(fill), "filled down to the ground at " + y);
        }
    }

    private static int wool(Site s, net.minecraft.world.level.block.Block wool) {
        int n = 0;
        for (CampBlueprint.Placement op : CampBlueprint.plan(s.pack.id, s.pack.family, s.pack.color, List.of(), s.pack.siteFacing, 0, 1).placements()) {
            if (s.level.getBlockState(s.at(op)).is(wool)) n++;
        }
        return n;
    }

    @Test
    void aVassalThatGoesFreeStillRuinsTheWoolItPutUpInItsLiegesColor() {
        Site s = site();
        Pack king = extraPack(s, new UUID(8, 8), X + 300); // color 9, cyan
        s.pack.liege = king.id; // built as the King's vassal: in the King's color
        s.tick(20 * 400);
        assertEquals(1, s.pack.builtTier);
        int before = wool(s, Blocks.CYAN_WOOL);
        assertTrue(before > 20, "built in its liege's cyan: " + before);
        s.pack.liege = null; // it goes free
        s.pack.ruined = true;
        CampBuilder manual = s.newBuilder();
        assertTrue(manual.queueRuin(s.level, s.data.feud, s.pack, 1, false, s.clock, null).queued());
        for (int i = 0; i < 400 && manual.isBuilding(s.pack.id); i++) manual.tick(s.level.getServer(), s.data, s.clock++);
        int after = wool(s, Blocks.CYAN_WOOL) + wool(s, Blocks.LIGHT_BLUE_WOOL);
        assertTrue(after < before / 2, "its own wool fell to ruin, whatever color: " + before + " -> " + after);
    }

    @Test
    void anOldSettlementWithNoRecordStillRuinsByItsBlueprint() {
        Site s = site();
        s.tick(20 * 400);
        assertEquals(1, s.pack.builtTier);
        s.pack.placed.clear(); // as built by 0.1.0-beta.1
        int before = wool(s, Blocks.LIGHT_BLUE_WOOL);
        s.pack.ruined = true;
        CampBuilder manual = s.newBuilder();
        assertTrue(manual.queueRuin(s.level, s.data.feud, s.pack, 1, false, s.clock, null).queued());
        for (int i = 0; i < 400 && manual.isBuilding(s.pack.id); i++) manual.tick(s.level.getServer(), s.data, s.clock++);
        BlockState fire = s.level.getBlockState(s.find(CampBlueprint.Material.CAMPFIRE));
        assertTrue(fire.getBlock() instanceof CampfireBlock && !fire.getValue(CampfireBlock.LIT), "the rest falls to ruin: " + fire);
        assertEquals(before, wool(s, Blocks.LIGHT_BLUE_WOOL), "its wool it can't tell from a player's, so it is left");
        assertTrue(s.pack.placed.isEmpty(), "and it goes on without a record");
    }

    @Test
    void anUpgradeBesideANeighbourKeepsOutOfItsGround() {
        Site s = site();
        Pack neighbour = extraPack(s, new UUID(5, 9), X + 24); // two camps 24 blocks apart, from an older version
        s.tick(20 * 400);
        assertEquals(1, s.pack.builtTier);
        assertEquals(1, neighbour.builtTier);
        java.util.Set<Long> camp = new java.util.HashSet<>(s.pack.placed.keySet());
        s.pack.tier = SettlementTier.STRONGHOLD;
        s.pack.grunts = 20;
        s.seen = false; // built off screen, quickly
        for (int i = 0; i < 20 * 600 && s.pack.builtTier < 3; i++) s.tick(1);
        assertEquals(3, s.pack.builtTier, "the stronghold went up");
        int r = io.github.pirut.bloodfeud.game.camp.SiteFinder.reservedRadius(neighbour);
        int inside = 0;
        for (long k : s.pack.placed.keySet()) {
            if (camp.contains(k)) continue; // its own camp, from before
            int x = s.pack.siteX + BlockPos.getX(k), z = s.pack.siteZ + BlockPos.getZ(k);
            if (Math.abs(x - neighbour.siteX) <= r && Math.abs(z - neighbour.siteZ) <= r) inside++;
        }
        assertEquals(0, inside, "nothing of the stronghold on the neighbour's ground");
        assertFalse(s.pack.skippedZones.isEmpty(), "the parts that would have reached in were left out");
        BlockPos fire = null;
        for (CampBlueprint.Placement op : CampBlueprint.plan(neighbour.id, neighbour.family, neighbour.color, List.of(), neighbour.siteFacing, 0, 1).placements()) {
            if (op.material() == CampBlueprint.Material.CAMPFIRE) fire = new BlockPos(neighbour.siteX + op.rx(), neighbour.siteY + op.ry(), neighbour.siteZ + op.rz());
        }
        assertTrue(s.level.getBlockState(fire).getBlock() instanceof CampfireBlock, "the neighbour's camp stands");
    }

    @Test
    void aPackThatDiesBeforeItPlacesAnythingLeavesNoRuins() {
        Site s = site();
        Pack.Works w = new Pack.Works(false, 0, 1, s.pack.color, List.of(), Set.of(), Set.of());
        s.pack.works = w;
        s.level.chunks.loaded = false;
        s.level.chunks.ticketsLoad = false; // its ground never comes, so nothing is placed
        s.tick(100);
        assertTrue(s.builder.isBuilding(s.pack.id));
        assertTrue(s.pack.placed.isEmpty());
        s.data.feud.packs.remove(s.pack.id);
        s.tick(2);
        assertTrue(s.data.feud.abandonedSites.isEmpty(), "no abandoned site where nothing was built");
        assertTrue(s.level.chunks.tickets.isEmpty(), "its reservation and holds went with it");
    }

    @Test
    void aDeadPacksSiteSearchStops() throws InterruptedException {
        Site s = earth();
        s.pack.siteSet = false;
        s.seen = false;
        s.tick(40);
        long until = System.nanoTime() + 5_000_000_000L;
        while (!s.builder.searching(s.pack.id) && System.nanoTime() < until) {
            s.tick(1);
            Thread.sleep(2);
        }
        assertTrue(s.builder.searching(s.pack.id), "an off-screen search on the Earth");
        s.data.feud.packs.remove(s.pack.id);
        s.tick(41);
        assertFalse(s.builder.searching(s.pack.id), "stops when the pack dies");
    }

    @Test
    void aSavedBuildFromBeforeThereWasARecordStartsOver() throws Exception {
        Site s = site();
        s.tick(260);
        assertTrue(s.done() > 0);
        CompoundTag saved = s.data.save(new CompoundTag(), null);
        CompoundTag works = saved.getList("packs", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0).getCompound("works");
        works.remove("v"); // as saved by an earlier build of this change
        Method load = FeudData.class.getDeclaredMethod("load", CompoundTag.class, HolderLookup.Provider.class);
        load.setAccessible(true);
        FeudData data = (FeudData) load.invoke(null, saved, null);
        assertEquals(0, data.feud.pack(s.pack.id).works.done, "it starts again from its first step");
    }

    @Test
    void aRepairCutShortLeavesSomethingTheNextRuinCovers() {
        Pack p = packWith(5);
        p.builtTier = 1;
        p.builtRuined = true;
        p.works = new Pack.Works(true, 1, 2, 0, List.of(), Set.of(), Set.of());
        p.works.placedCount = 12;
        CampBuilder.abandonWorks(p);
        assertNull(p.works);
        assertEquals(2, p.builtTier, "counted as built to the tier it was repairing to");
        assertFalse(p.builtRuined, "and intact, so the next ruin covers what it put back");
        Pack fresh = packWith(5);
        fresh.works = new Pack.Works(false, 0, 1, 0, List.of(), Set.of(), Set.of());
        fresh.works.placedCount = 12;
        CampBuilder.abandonWorks(fresh);
        assertEquals(0, fresh.builtTier, "a first camp keeps buildStarted instead");
    }

    // ------------------------------------------------------------------ final check

    @Test
    void twoNewCampsAFewDozenBlocksApartDontClaimOverlappingGround() {
        Site s = site();
        s.pack.siteSet = false;
        Pack b = new Pack(new UUID(6, 7), Family.ROTTEN, "minecraft:overworld", "Other", 9, X + 40, Z, 0);
        b.grunts = 3;
        s.data.feud.packs.put(b.id, b);
        s.tick(20);
        assertTrue(s.pack.siteSet && b.siteSet, "both found a site");
        int apart = Math.max(Math.abs(s.pack.siteX - b.siteX), Math.abs(s.pack.siteZ - b.siteZ));
        assertTrue(apart > 2 * (SettlementTier.STRONGHOLD.radius + CampBlueprint.FOOTPRINT_MARGIN), "apart by " + apart);
    }

    @Test
    void aBuilderIsntMovedWhereSomethingNextDoorReachesIntoItsBody() {
        Site s = site();
        BlockPos at = new BlockPos(X + 3, 71, Z);
        assertTrue(CampBuilder.safeToStand(s.level, at, 1.95f, 2.2f), "open ground, for a ravager-sized mob");
        s.level.setBlock(at.east(), Blocks.OAK_FENCE.defaultBlockState(), 3);
        assertFalse(CampBuilder.safeToStand(s.level, at, 1.95f, 2.2f), "a fence in the next column reaches into a wide body");
        assertTrue(CampBuilder.safeToStand(s.level, at, 0.6f, 1.95f), "but not a narrow one");
        s.level.setBlock(at.east(), Blocks.AIR.defaultBlockState(), 3);
        s.level.setBlock(at.east().below(), Blocks.COBBLESTONE_WALL.defaultBlockState(), 3);
        assertFalse(CampBuilder.safeToStand(s.level, at, 1.95f, 2.2f), "a wall below, 1.5 tall, pokes up into it");
    }

    @Test
    void glassDoesntShutASpotIn() {
        Site s = site();
        BlockPos spot = new BlockPos(X + 3, 65, Z);
        assertTrue(CampBuilder.enclosed(s.level, spot));
        s.level.setBlock(spot.above(), Blocks.GLASS.defaultBlockState(), 3);
        assertFalse(CampBuilder.enclosed(s.level, spot), "seen through glass");
    }

    @Test
    void aBlockWhoseSupportIsntThereYetIsDeferredNotDropped() {
        Site s = site();
        List<CampBlueprint.Placement> steps = CampBuilder.crewOrder(CampBlueprint.plan(s.pack.id, s.pack.family,
                s.pack.color, List.of(), s.pack.siteFacing, 0, 1).placements());
        int banner = stepOf(steps, CampBlueprint.Material.BANNER_STANDING);
        BlockPos bannerAt = s.at(steps.get(banner));
        s.tick(60);
        assertNotNull(s.pack.works);
        // Take its post away just before the crew gets to the banner.
        for (int i = 0; i < 20 * 600 && s.done() >= 0 && s.done() < banner; i++) {
            s.tick(1);
            if (s.done() >= banner - 1 && !s.level.getBlockState(bannerAt.below()).isAir()) {
                s.level.setBlock(bannerAt.below(), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        s.level.setBlock(bannerAt.below(), Blocks.AIR.defaultBlockState(), 3);
        for (int i = 0; i < 20 * 400 && s.pack.works != null && s.done() < s.pack.works.total; i++) s.tick(1);
        assertNotNull(s.pack.works, "not done: the banner waits");
        assertTrue(s.pack.works.deferred.contains(banner), "deferred, not passed over");
        assertTrue(s.level.getBlockState(bannerAt).isAir());
        s.level.setBlock(bannerAt.below(), Blocks.OAK_FENCE.defaultBlockState(), 3); // its post is back
        s.seen = false;
        s.level.chunks.loaded = false;
        s.tick(80);
        assertTrue(s.level.getBlockState(bannerAt).getBlock() instanceof net.minecraft.world.level.block.BannerBlock,
                "it went up off screen once it could stand");
        assertEquals(1, s.pack.builtTier);
    }

    @Test
    void reBanneringOnlyTouchesTheCampsOwnBanners() {
        Site s = site();
        s.tick(20 * 400);
        BlockPos banner = s.find(CampBlueprint.Material.BANNER_STANDING);
        s.level.setBlock(banner, Blocks.RED_BANNER.defaultBlockState(), 3); // a player's banner in its place
        s.pack.bannersDirty = true;
        assertTrue(s.builder.rebanner(s.level, s.data.feud, s.pack));
        assertTrue(s.level.getBlockState(banner).is(Blocks.RED_BANNER), "the player's banner is left");

        s.pack.placed.clear(); // a settlement from before there was a record
        s.level.setBlock(banner, Blocks.LIGHT_BLUE_BANNER.defaultBlockState(), 3);
        s.builder.rebanner(s.level, s.data.feud, s.pack);
        assertTrue(s.level.getBlockState(banner).is(Blocks.LIGHT_BLUE_BANNER), "nothing it can prove is its own: left");
    }

    @Test
    void aTownTakesDownOnlyTheVassalBannersAndPostsItPutUp() {
        Site s = site();
        Pack vassal = extraPack(s, new UUID(5, 10), X + 500);
        vassal.liege = s.pack.id;
        s.pack.tier = SettlementTier.TOWN;
        CampBuilder manual = s.newBuilder();
        assertTrue(manual.queueBuild(s.level, s.data.feud, s.pack, 0, 4, false, s.clock, null).queued());
        for (int i = 0; i < 200 && manual.isBuilding(s.pack.id); i++) manual.tick(s.level.getServer(), s.data, s.clock++);
        assertEquals(4, s.pack.builtTier);
        List<CampBlueprint.Spot> spots = CampBlueprint.vassalBannerSpots(s.pack.siteFacing);
        BlockPos ours = new BlockPos(s.pack.siteX, s.pack.siteY, s.pack.siteZ).offset(spots.get(0).rx(), spots.get(0).ry(), spots.get(0).rz());
        BlockPos theirs = new BlockPos(s.pack.siteX, s.pack.siteY, s.pack.siteZ).offset(spots.get(1).rx(), spots.get(1).ry(), spots.get(1).rz());
        assertTrue(s.level.getBlockState(ours).getBlock() instanceof net.minecraft.world.level.block.BannerBlock, "its vassal's banner");
        net.minecraft.world.level.block.Block fence = io.github.pirut.bloodfeud.game.camp.FamilyPalette.of(s.pack.family).fence;
        s.level.setBlock(theirs.below(), fence.defaultBlockState(), 3); // a player's fence, the same kind, at an empty post
        s.level.setBlock(theirs, Blocks.RED_BANNER.defaultBlockState(), 3);
        vassal.liege = null; // the vassal goes free
        manual.rebanner(s.level, s.data.feud, s.pack);
        assertTrue(s.level.getBlockState(ours).isAir(), "its own vassal banner came down");
        assertFalse(s.level.getBlockState(ours.below()).is(fence), "with the post it put up");
        assertTrue(s.level.getBlockState(theirs).is(Blocks.RED_BANNER), "a player's banner stays");
        assertTrue(s.level.getBlockState(theirs.below()).is(fence), "and so does a player's post");
    }

    /** Every equipment drop chance, as the mob saves them. */
    private static void assertDropsNothing(net.minecraft.world.entity.Mob mob) {
        CompoundTag tag = new CompoundTag();
        mob.addAdditionalSaveData(tag);
        for (String list : List.of("ArmorDropChances", "HandDropChances", "body_armor_drop_chance")) {
            if (tag.contains(list, net.minecraft.nbt.Tag.TAG_LIST)) {
                net.minecraft.nbt.ListTag chances = tag.getList(list, net.minecraft.nbt.Tag.TAG_FLOAT);
                for (int i = 0; i < chances.size(); i++) assertEquals(0f, chances.getFloat(i), list + " " + i);
            } else if (tag.contains(list)) {
                assertEquals(0f, tag.getFloat(list), list);
            }
        }
        assertTrue(tag.contains("ArmorDropChances") && tag.contains("HandDropChances"), "saved its drop chances");
    }

    @Test
    void aCrewHireDropsAndPicksUpNothingAndOnlyTheSunSensitiveGetFireResistanceWhileHired() {
        Site s = site();
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(net.minecraft.world.entity.EntityType.ZOMBIE, s.level);
        zombie.setCanPickUpLoot(true);
        CampBuilder.outfitCrew(zombie);
        assertDropsNothing(zombie);
        assertFalse(zombie.canPickUpLoot(), "picks nothing up while hired");
        assertTrue(zombie.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE), "a zombie would burn in the sun");
        CampBuilder.releaseCrew(zombie);
        assertTrue(zombie.canPickUpLoot(), "picks up again once dismissed");
        assertFalse(zombie.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE), "and can burn again, in lava too");
        assertDropsNothing(zombie);

        for (net.minecraft.world.entity.Mob m : List.<net.minecraft.world.entity.Mob>of(
                new net.minecraft.world.entity.monster.Husk(net.minecraft.world.entity.EntityType.HUSK, s.level),
                new net.minecraft.world.entity.monster.piglin.PiglinBrute(net.minecraft.world.entity.EntityType.PIGLIN_BRUTE, s.level),
                new net.minecraft.world.entity.monster.WitherSkeleton(net.minecraft.world.entity.EntityType.WITHER_SKELETON, s.level),
                new net.minecraft.world.entity.monster.Vindicator(net.minecraft.world.entity.EntityType.VINDICATOR, s.level))) {
            CampBuilder.outfitCrew(m);
            assertFalse(m.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE), m.getType() + " doesn't burn in the sun");
            assertDropsNothing(m);
        }
        net.minecraft.world.entity.Mob skeleton =
                new net.minecraft.world.entity.monster.Skeleton(net.minecraft.world.entity.EntityType.SKELETON, s.level);
        CampBuilder.outfitCrew(skeleton);
        assertTrue(skeleton.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE));
    }
}
