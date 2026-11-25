package io.github.pirut.bloodfeud.game;

import io.earthplatform.minecraft.cubic.ServerCubeWorld;
import io.earthplatform.minecraft.cubic.gen.CubeGenerator;
import io.github.pirut.bloodfeud.feud.Brute;
import io.github.pirut.bloodfeud.feud.Family;
import io.github.pirut.bloodfeud.feud.Feud;
import io.github.pirut.bloodfeud.feud.Memory;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.SettlementTier;
import io.github.pirut.bloodfeud.game.EarthWorldFixture.FakeLevel;
import io.github.pirut.bloodfeud.game.camp.SiteFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.pirut.bloodfeud.game.EarthWorldFixture.X;
import static io.github.pirut.bloodfeud.game.EarthWorldFixture.Z;
import static org.junit.jupiter.api.Assertions.*;

/** Sol's round 2 on #34: one test (or more) per rule. */
class OutingTest {
    static final String OW = "minecraft:overworld";
    static final UUID PLAYER = new UUID(1, 1);

    @BeforeAll
    static void bootstrap() {
        EarthWorldFixture.bootstrap();
    }

    @AfterEach
    void defaults() {
        Earth.reset();
    }

    private static FakeLevel overworld() {
        FakeLevel level = EarthWorldFixture.level(70, -64, 320);
        level.key = Level.OVERWORLD;
        level.cubes = null;
        return level;
    }

    private static boolean skipsExperience(Zombie z) throws Exception {
        Field f = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("skipDropExperience");
        f.setAccessible(true);
        return f.getBoolean(z);
    }

    // 1. ------------------------------------------------------------------ one patrol-member model

    @Test
    void aMembersNoSpoilsIsSavedWithItAndComesBackOnLoad() throws Exception {
        FakeLevel level = overworld();
        Zombie z = new Zombie(EntityType.ZOMBIE, level);
        Outings.outfit(z);
        assertTrue(z.getTags().contains(Outings.TAG));
        assertTrue(skipsExperience(z));
        CompoundTag saved = new CompoundTag();
        z.saveWithoutId(saved);
        Zombie back = new Zombie(EntityType.ZOMBIE, level);
        back.load(saved); // unloaded, then read back
        assertTrue(back.getTags().contains(Outings.TAG), "the tag is saved");
        assertEquals(BuiltInLootTables.EMPTY, back.getLootTable(), "so is the empty loot table");
        assertFalse(back.canPickUpLoot());
        assertFalse(skipsExperience(back), "skipping experience isn't saved...");
        Outings.onLoad(back);
        assertTrue(skipsExperience(back), "...so loading puts it back");

        Outings.promote(back);
        assertFalse(back.getTags().contains(Outings.TAG), "a member that rises is a member no more");
        assertEquals(EntityType.ZOMBIE.getDefaultLootTable(), back.getLootTable(), "and its own loot table is back");
    }

    @Test
    void anOutingEndsTogetherAndAPromotedMemberIsNotPartOfIt() {
        Outings outings = new Outings();
        UUID pack = new UUID(5, 5), boss = new UUID(6, 6), escort = new UUID(7, 7), risen = new UUID(8, 8);
        Outings.Outing o = outings.start(pack, true);
        outings.addLeader(o, boss);
        outings.addMember(o, escort);
        outings.addMember(o, risen);
        assertTrue(outings.packOut(pack));
        outings.leave(risen); // it rose mid-patrol
        assertFalse(outings.isMember(risen));
        assertFalse(Outings.over(true, true, 1, 0), "its leader is still out");
        assertTrue(Outings.over(true, false, 1, 0), "its leader is back");
        assertEquals(List.of(escort), outings.end(o), "only the escort goes back into the pack");
        assertFalse(outings.packOut(pack));
        assertNull(outings.ofLeader(boss));

        // A wave without its Brutes (they rest): over when no player has been near for a while, or it's all gone.
        assertFalse(Outings.over(false, false, 3, Outings.GRUNTS_LINGER - 1));
        assertTrue(Outings.over(false, false, 3, Outings.GRUNTS_LINGER));
        assertTrue(Outings.over(false, false, 0, 0));
    }

    @Test
    void aWaveWithoutItsBrutesKeepsThePackRestingUntilTheirRestIsOver() {
        Outings.Outing led = new Outings.Outing(new UUID(1, 2), false);
        led.leaders.add(new UUID(3, 4));
        assertEquals(-1, Outings.restAfter(led, 1000), "a led outing rests by its Brutes' withdrawal");
        Outings.Outing wave = new Outings.Outing(new UUID(1, 2), false);
        wave.restWindow = 9000;
        assertEquals(9000, Outings.restAfter(wave, 1000), "the Boss's rest window: one wave per window");
        wave.restWindow = 0;
        assertEquals(2200, Outings.restAfter(wave, 1000), "and never straight back");
    }

    @Test
    void membersComeOutOnlyOnCheckedGroundButAPackMayStandInItsOwnLitCamp() {
        FakeLevel level = overworld();
        level.blockLight = new HashMap<>();
        Pack pack = new Pack(new UUID(9, 1), Family.ROTTEN, OW, "Home", 3, X, Z, 0);
        pack.siteSet = true;
        pack.siteX = X;
        pack.siteY = 71;
        pack.siteZ = Z;
        pack.builtTier = 1;
        BlockPos spot = new BlockPos(X + 2, 71, Z);
        BlockPos tent = new BlockPos(X + 4, 71, Z + 1);
        level.setBlock(tent, Blocks.RED_WOOL.defaultBlockState(), 3);
        level.blockLight.put(spot, 12); // its campfire
        assertFalse(PatrolSpots.allowed(level, spot, null), "lit, and a block that isn't natural nearby");
        pack.placed.put(BlockPos.asLong(0, 0, 0), BuiltInRegistries.BLOCK.getKey(Blocks.CAMPFIRE).toString());
        assertFalse(PatrolSpots.allowed(level, spot, pack), "the wool isn't the camp's by its record");
        pack.placed.put(BlockPos.asLong(4, 0, 1), BuiltInRegistries.BLOCK.getKey(Blocks.RED_WOOL).toString());
        assertTrue(PatrolSpots.allowed(level, spot, pack), "in its own camp, its own light and its own wool");
        BlockPos outside = new BlockPos(X + 40, 71, Z);
        level.blockLight.put(outside, 3);
        assertFalse(PatrolSpots.allowed(level, outside, pack), "out of its camp, no light at all");
    }

    // 2. ------------------------------------------------------------------ departure

    @SuppressWarnings("unchecked")
    private static java.util.EnumSet<Goal.Flag> off(GoalSelector g) {
        return Departure.disabled(g);
    }

    @Test
    void aDepartureHoldsEveryTickAndGivesBackOnlyWhatItTook() {
        FakeLevel level = overworld();
        Zombie z = new Zombie(EntityType.ZOMBIE, level);
        GoalSelector goals = Departure.selectors(z).get(0);
        goals.disableControlFlag(Goal.Flag.LOOK); // switched off by something else before
        Departure.Held held = Departure.hold(z);
        goals.setControlFlag(Goal.Flag.MOVE, true); // what vanilla does every 5 ticks
        goals.setControlFlag(Goal.Flag.LOOK, true);
        Departure.suppress(z, held);
        assertTrue(off(goals).contains(Goal.Flag.MOVE), "back off on the next tick");
        assertTrue(off(goals).contains(Goal.Flag.LOOK));
        goals.disableControlFlag(Goal.Flag.LOOK);
        Departure.release(z, held);
        assertFalse(off(goals).contains(Goal.Flag.MOVE), "what it switched off comes back on");
        assertTrue(off(goals).contains(Goal.Flag.LOOK), "what was off before stays off");
    }

    @Test
    void aDepartingPiglinBruteForgetsWhoItWasAngryAtEveryTick() {
        FakeLevel level = overworld();
        PiglinBrute brute = new PiglinBrute(EntityType.PIGLIN_BRUTE, level);
        Zombie victim = new Zombie(EntityType.ZOMBIE, level);
        Departure.Held held = Departure.hold(brute);
        brute.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, victim);
        brute.getBrain().setMemory(MemoryModuleType.ANGRY_AT, victim.getUUID());
        Departure.suppress(brute, held);
        assertFalse(brute.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
        assertFalse(brute.getBrain().hasMemoryValue(MemoryModuleType.ANGRY_AT));
    }

    // 3. ------------------------------------------------------------------ the retreat home's reservation

    @Test
    void aRetreatHomeIsHeldInTheReservationsCampSitesCheck() throws Exception {
        Feud f = new Feud(new Random(1));
        Pack settler = new Pack(new UUID(2, 1), Family.ROTTEN, OW, "Settler", 1, 0, 0, 0);
        f.packs.put(settler.id, settler);
        f.settleAway(settler);
        assertTrue(settler.retreatHome, "held from the moment it is picked");
        Pack neighbour = new Pack(new UUID(2, 2), Family.BONES, OW, "Neighbour", 2, settler.homeX + 60, settler.homeZ, 0);
        f.packs.put(neighbour.id, neighbour);
        assertTrue(SiteFinder.claimedByAnother(f, neighbour, settler.homeX + 60, settler.homeZ, SettlementTier.CAMP.radius),
                "a camp search can't take ground round the picked home");
        assertFalse(SiteFinder.claimedByAnother(f, neighbour, settler.homeX + 200, settler.homeZ, SettlementTier.CAMP.radius));
        settler.retreatHome = false; // a home from before 0.1.0-beta.3
        assertFalse(SiteFinder.claimedByAnother(f, neighbour, settler.homeX + 60, settler.homeZ, SettlementTier.CAMP.radius));

        settler.retreatHome = true;
        FeudData data = new FeudData();
        data.feud.packs.put(settler.id, settler);
        Method load = FeudData.class.getDeclaredMethod("load", CompoundTag.class, HolderLookup.Provider.class);
        load.setAccessible(true);
        FeudData back = (FeudData) load.invoke(null, data.save(new CompoundTag(), null), null);
        assertTrue(back.feud.pack(settler.id).retreatHome, "saved");
    }

    @Test
    void aCancelledEarthSurveyStopsReadingMapData() throws Exception {
        FakeLevel level = EarthWorldFixture.level(70, -16384, 16383);
        AtomicInteger reads = new AtomicInteger();
        level.cubes = new ServerCubeWorld(new CubeGenerator() {
            @Override
            public int surfaceY(int x, int z) {
                reads.incrementAndGet();
                return 70;
            }
        });
        List<int[]> columns = new ArrayList<>();
        for (int i = 0; i < 500; i++) columns.add(new int[]{X + i, Z});
        CompletableFuture<int[][]> cancelled = Earth.survey(level, columns, () -> true);
        assertThrows(Exception.class, () -> cancelled.get(10, TimeUnit.SECONDS));
        assertEquals(0, reads.get(), "not one column was read");
        int[][] all = Earth.survey(level, columns, () -> false).get(10, TimeUnit.SECONDS);
        assertEquals(500, all.length);
        assertTrue(reads.get() >= 500, "every column read when it isn't: " + reads.get());
    }

    // 4. ------------------------------------------------------------------ the hunter guarantee

    @Test
    void aHuntingBossLeadsItsPacksPatrolAnywhereInItsGround() {
        Feud f = new Feud(new Random(3));
        Brute boss = f.rise("minecraft:zombie", Family.ROTTEN, OW, 0, 0, PLAYER, Memory.Kind.ROSE, null, null, 0);
        Pack pk = f.form(boss, (r, d, x, z) -> "Here", 0, 4);
        Brute strong = f.rise("minecraft:zombie", Family.ROTTEN, OW, 0, 0, null, null, null, pk.id, 0);
        strong.level = 19;
        assertEquals(pk, f.patrolFor(boss, OW, 480, 0), "480 blocks off is still its ground (500)");
        assertNull(f.patrolFor(boss, OW, 520, 0));
        assertEquals(boss, f.patrolLeader(pk, PLAYER, b -> true), "the Boss hunting you leads, over a stronger Brute");
        assertEquals(strong, f.patrolLeader(pk, new UUID(4, 4), b -> true), "for anyone else the Boss stays home");
    }

    // 5. ------------------------------------------------------------------ mark expiry

    @Test
    void anExpiredMarkNeverMakesABrute() {
        Rises rises = new Rises();
        UUID mob = new UUID(3, 3);
        rises.mark(mob, new Rises.Mark(Level.OVERWORLD, PLAYER, 0, null, 0, 0));
        AtomicInteger asked = new AtomicInteger();
        rises.tick(Rises.EXPIRE + 1, (id, m) -> {
            asked.incrementAndGet();
            return Rises.Outcome.ROSE;
        });
        assertEquals(0, asked.get(), "it was never asked to rise");
        assertFalse(rises.isMarked(mob));
    }

    // 6. ------------------------------------------------------------------ the fallback's reason

    @Test
    void theFallbackSaysWhyNoPlaceWouldDo() {
        Feud f = new Feud(new Random(4));
        Pack p = new Pack(new UUID(6, 1), Family.ROTTEN, OW, "New", 1, 0, 0, 0);
        f.packs.put(p.id, p);
        List<int[]> candidates = List.of(new int[]{300, 0}, new int[]{0, 300});
        int[][] ground = new int[candidates.size() * Settler.SAMPLES.length][];
        for (int i = 0; i < ground.length; i++) ground[i] = new int[]{70, Settler.DRY};
        Settler.Choice c = Settler.choose(f, p, candidates, ground, (x, z) -> 0, (x, z) -> 0);
        assertNull(c.pick());
        assertTrue(c.why().contains("2 outside the world border"), c.why());
        assertFalse(c.why().contains("dry"), c.why());
        for (int i = 0; i < Settler.SAMPLES.length; i++) ground[i] = new int[]{60, Settler.WET};
        Settler.Choice d = Settler.choose(f, p, candidates, ground, (x, z) -> 0, (x, z) -> 0);
        assertTrue(d.why().contains("1 under water") && d.why().contains("1 outside the world border"), d.why());
    }
}
