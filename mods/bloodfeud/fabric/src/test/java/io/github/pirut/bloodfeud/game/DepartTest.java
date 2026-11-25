package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.feud.Brute;
import io.github.pirut.bloodfeud.feud.Family;
import io.github.pirut.bloodfeud.feud.Feud;
import io.github.pirut.bloodfeud.feud.Memory;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.PlaceNames;
import io.github.pirut.bloodfeud.game.EarthWorldFixture.FakeLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.pirut.bloodfeud.game.EarthWorldFixture.X;
import static io.github.pirut.bloodfeud.game.EarthWorldFixture.Z;
import static org.junit.jupiter.api.Assertions.*;

/** Sol's round 1 on #34: one test per rule. */
class DepartTest {
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

    // 1. ------------------------------------------------------------------ the rest is saved

    @Test
    void aRisenBrutesRestIsSavedAndOnlyEverGrows() throws Exception {
        FeudData data = new FeudData();
        Brute b = data.feud.rise("minecraft:zombie", Family.ROTTEN, OW, 0, 0, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        b.restUntil(5000);
        b.restUntil(3000); // a shorter rest never cuts a longer one short
        assertEquals(5000, b.restUntil);
        assertTrue(b.resting(4999));
        assertFalse(b.resting(5000));
        Method load = FeudData.class.getDeclaredMethod("load", CompoundTag.class, HolderLookup.Provider.class);
        load.setAccessible(true);
        FeudData back = (FeudData) load.invoke(null, data.save(new CompoundTag(), null), null);
        assertEquals(5000, back.feud.brute(b.id).restUntil, "a restart doesn't cut it short");
        CompoundTag old = data.save(new CompoundTag(), null);
        old.getList("brutes", 10).getCompound(0).remove("restUntil");
        FeudData beta2 = (FeudData) load.invoke(null, old, null);
        assertFalse(beta2.feud.brute(b.id).resting(0), "older saves have no rest");
    }

    // 2 and 9. ------------------------------------------------------------ a mob rises once, and a mark waits

    @Test
    void aMobRisesOnceAndItsMarkWaitsUntilItCan() {
        FakeLevel level = overworld();
        Zombie z = new Zombie(EntityType.ZOMBIE, level);
        Map<UUID, UUID> bruteOfEntity = new HashMap<>();
        assertTrue(Rises.canRise(z, bruteOfEntity));
        bruteOfEntity.put(z.getUUID(), new UUID(9, 9));
        assertFalse(Rises.canRise(z, bruteOfEntity), "already a Brute's body");
        bruteOfEntity.clear();
        z.addTag(Live.TAG_BRUTE);
        assertFalse(Rises.canRise(z, bruteOfEntity), "tagged as a Brute");

        Rises rises = new Rises();
        UUID mob = new UUID(3, 3);
        rises.mark(mob, new Rises.Mark(Level.OVERWORLD, PLAYER, 100, null, 5, 5));
        assertTrue(rises.hasMarkFor(PLAYER));
        AtomicInteger asked = new AtomicInteger();
        rises.tick(100 + Rises.DELAY - 1, (id, m) -> {
            asked.incrementAndGet();
            return Rises.Outcome.ROSE;
        });
        assertEquals(0, asked.get(), "too soon");
        rises.tick(100 + Rises.DELAY, (id, m) -> Rises.Outcome.NOT_YET); // not loaded, or the cap is full
        assertTrue(rises.isMarked(mob), "the mark waits");
        rises.tick(100 + Rises.DELAY + 20, (id, m) -> Rises.Outcome.ROSE);
        assertFalse(rises.isMarked(mob), "used up when it rises");
        rises.tick(100 + Rises.DELAY + 40, (id, m) -> {
            asked.incrementAndGet();
            return Rises.Outcome.ROSE;
        });
        assertEquals(0, asked.get(), "and never asked again");

        rises.mark(mob, new Rises.Mark(Level.OVERWORLD, PLAYER, 0, null, 0, 0));
        rises.forget(mob); // it rose another way (it killed the player)
        assertFalse(rises.isMarked(mob));
        rises.mark(mob, new Rises.Mark(Level.OVERWORLD, PLAYER, 0, null, 0, 0));
        rises.tick(Rises.EXPIRE + 1, (id, m) -> Rises.Outcome.NOT_YET);
        assertFalse(rises.isMarked(mob), "a mark that can never be used expires");
    }

    // 3. ------------------------------------------------------------------ a departing Brute can't fight

    @SuppressWarnings("unchecked")
    private static EnumSet<Goal.Flag> disabled(GoalSelector g) throws Exception {
        Field f = GoalSelector.class.getDeclaredField("disabledFlags");
        f.setAccessible(true);
        return (EnumSet<Goal.Flag>) f.get(g);
    }

    @Test
    void aDepartingBruteHasNoFightingGoalsAndItsHitsDoNothing() throws Exception {
        FakeLevel level = overworld();
        Zombie z = new Zombie(EntityType.ZOMBIE, level);
        List<GoalSelector> selectors = Departure.selectors(z);
        assertEquals(2, selectors.size(), "goals and targets");
        DamageSource hit = new DamageSource(Holder.direct(new DamageType("mob", 0.1f)), z);
        Zombie victim = new Zombie(EntityType.ZOMBIE, level);
        assertFalse(Effects.immune(victim, hit));

        Departure.hold(z);
        assertTrue(Departure.departing(z));
        for (GoalSelector g : selectors) {
            assertTrue(disabled(g).containsAll(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.TARGET)));
            assertFalse(disabled(g).contains(Goal.Flag.JUMP), "it still swims");
        }
        assertNull(z.getTarget());
        assertTrue(Effects.immune(victim, hit), "its hits do nothing");

        Departure.release(z);
        assertFalse(Departure.departing(z));
        for (GoalSelector g : selectors) assertTrue(disabled(g).isEmpty(), "it can fight again");
        assertFalse(Effects.immune(victim, hit));
    }

    // 4. ------------------------------------------------------------------ escorts are no farm

    @Test
    void anEscortDropsNothingAndPicksNothingUp() throws Exception {
        Method chance = net.minecraft.world.entity.Mob.class.getDeclaredMethod("getEquipmentDropChance", EquipmentSlot.class);
        chance.setAccessible(true);
        Zombie z = new Zombie(EntityType.ZOMBIE, overworld());
        z.setCanPickUpLoot(true);
        Live.noSpoils(z);
        assertEquals(BuiltInLootTables.EMPTY, z.getLootTable());
        assertFalse(z.canPickUpLoot());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            assertEquals(0f, (float) chance.invoke(z, slot), slot.toString());
        }
    }

    // 5. ------------------------------------------------------------------ patrol spots

    @Test
    void aPatrolComesOutOnlyOnDarkKnownGroundAwayFromPlayerBlocks() {
        FakeLevel level = EarthWorldFixture.level(70, -16384, 16383); // the Earth
        level.blockLight = new HashMap<>();
        BlockPos spot = new BlockPos(X, 71, Z);
        assertNull(PatrolSpots.inColumn(level, X, 71, Z), "nothing known there yet");
        assertEquals(0, level.unknownHeightReads.get(), "and its height was never read");
        assertNull(PatrolSpots.find(level, spot, 0, 6, net.minecraft.util.RandomSource.create(1)));
        assertEquals(0, level.unknownHeightReads.get(), "nor any other unknown column's");

        level.load(spot, 24, 16, 16);
        assertEquals(spot, PatrolSpots.inColumn(level, X, 71, Z));
        level.blockLight.put(spot, 7);
        assertNull(PatrolSpots.inColumn(level, X, 71, Z), "lit by a block");
        level.blockLight.clear();
        level.setBlock(spot.offset(3, 0, 2), Blocks.OAK_PLANKS.defaultBlockState(), 3);
        assertNull(PatrolSpots.inColumn(level, X, 71, Z), "a player's block nearby");
    }

    // 6. ------------------------------------------------------------------ retreat sites

    private static int[][] dry(int candidates) {
        int[][] g = new int[candidates * Settler.SAMPLES.length][];
        for (int i = 0; i < g.length; i++) g[i] = new int[]{70, Settler.DRY};
        return g;
    }

    @Test
    void aHomeMustBeRealGroundInsideTheBorderAndHeldByNoOne() {
        Feud f = new Feud(new Random(1));
        Pack p = new Pack(new UUID(6, 6), Family.ROTTEN, OW, "New", 1, 0, 0, 0);
        f.packs.put(p.id, p);
        List<int[]> candidates = List.of(new int[]{300, 0}, new int[]{0, 300}, new int[]{-300, 0});
        int[][] ground = dry(3);
        assertEquals(Settler.VOID, Settler.kind(-16384, -16384, -16384), "the world's floor is void");
        assertEquals(Settler.WET, Settler.kind(60, 62, -64));
        for (int i = 0; i < Settler.SAMPLES.length; i++) ground[i] = new int[]{-64, Settler.VOID};
        assertArrayEquals(new int[]{0, 300}, Settler.choose(f, p, candidates, ground, (x, z) -> 1, (x, z) -> 0), "not over the void");
        assertArrayEquals(new int[]{-300, 0}, Settler.choose(f, p, candidates, ground, (x, z) -> z > 100 ? 0 : 1, (x, z) -> 0),
                "not past the border");

        // Another pack still settling, its pick at (-280, 30): that ground is held from the moment it was picked.
        Pack other = new Pack(new UUID(7, 7), Family.BONES, OW, "Other", 2, -280, 30, 0);
        other.settling = true;
        f.packs.put(other.id, other);
        assertTrue(Settler.heldByAnother(f, p, -300, 0));
        assertFalse(Settler.heldByAnother(f, p, 0, 300));
        assertNull(Settler.choose(f, p, candidates, ground, (x, z) -> z > 100 ? 0 : 1, (x, z) -> 0));
    }

    @Test
    void withNoCheckedHomeAPackSettlesWhereItFormed() {
        Feud f = new Feud(new Random(2));
        Pack p = new Pack(new UUID(8, 8), Family.ROTTEN, OW, "New", 1, 40, 50, 0);
        f.packs.put(p.id, p);
        f.settleAway(p);
        assertTrue(p.settling);
        assertNotEquals(40, p.homeX, "its unchecked pick");
        Settler settler = new Settler();
        assertTrue(settler.tick(dim -> null, f, 100), "the dimension can't be read");
        assertFalse(p.settling);
        assertEquals(40, p.homeX, "back where it formed, never the unchecked pick");
        assertEquals(50, p.homeZ);
    }

    @Test
    void aSurveyStopsWhenItsPackIsGone() throws Exception {
        FakeLevel level = EarthWorldFixture.level(70, -16384, 16383);
        Feud f = new Feud(new Random(3));
        Pack p = new Pack(new UUID(9, 9), Family.ROTTEN, "earthplatform:cubic", "New", 1, X, Z, 0);
        f.packs.put(p.id, p);
        f.settleAway(p);
        Settler settler = new Settler();
        settler.tick(dim -> level, f, 100);
        assertEquals(1, settler.running(), "its ground is being read");
        f.packs.remove(p.id);
        settler.tick(dim -> level, f, 120);
        assertEquals(0, settler.running(), "and the reading stopped with it");
    }

    // 7. ------------------------------------------------------------------ the hunter's own pack

    @Test
    void aHunterComesWithItsOwnPacksPatrolWhicheverPackIsNearest() {
        Feud f = new Feud(new Random(4));
        Feud.Places places = (r, d, x, z) -> PlaceNames.name(r, "minecraft:swamp", false);
        Brute boss = f.rise("minecraft:zombie", Family.ROTTEN, OW, 0, 0, null, null, null, null, 0);
        Pack own = f.form(boss, places, 0, 4);
        Brute hunter = f.rise("minecraft:zombie", Family.ROTTEN, OW, 0, 0, PLAYER, Memory.Kind.ROSE, null, own.id, 0);
        // A nearer pack of another family, right where the player is.
        Pack nearer = f.form(f.rise("minecraft:skeleton", Family.BONES, OW, 380, 0, null, null, null, null, 0), places, 0, 4);
        assertTrue(nearer.distanceTo(OW, 390, 0) < own.distanceTo(OW, 390, 0));
        assertEquals(own, f.patrolFor(hunter, OW, 390, 0));
        assertNull(f.patrolFor(hunter, OW, 900, 0), "out of its ground it comes on its own");
        own.settling = true;
        assertNull(f.patrolFor(hunter, OW, 390, 0), "its pack is on the move");
    }

    // 8. ------------------------------------------------------------------ where a departed Brute is on paper

    @Test
    void aDepartedBruteIsWhereItRoseOrAtItsCampNeverWhereAPlayerWent() {
        Feud f = new Feud(new Random(5));
        Brute lone = f.rise("minecraft:zombie", Family.ROTTEN, OW, 1000, 1000, PLAYER, Memory.Kind.SAW_KILLS, null, null, 0);
        lone.x = 9999; // as if it had been dragged off with a player who teleported
        Live.recordGone(lone, null, 12.7, -30.2);
        assertEquals(12, lone.x);
        assertEquals(-31, lone.z);
        Pack pk = f.form(lone, (r, d, x, z) -> "Here", 0, 3);
        pk.siteSet = true;
        pk.siteX = 500;
        pk.siteZ = 600;
        Live.recordGone(lone, pk, 12.7, -30.2);
        assertEquals(500, lone.x);
        assertEquals(600, lone.z);
    }
}
