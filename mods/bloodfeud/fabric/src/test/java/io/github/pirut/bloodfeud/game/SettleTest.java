package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.BloodFeud;
import io.github.pirut.bloodfeud.feud.Family;
import io.github.pirut.bloodfeud.feud.Feud;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.game.EarthWorldFixture.FakeLevel;
import io.github.pirut.bloodfeud.game.camp.CampBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static io.github.pirut.bloodfeud.game.EarthWorldFixture.X;
import static io.github.pirut.bloodfeud.game.EarthWorldFixture.Z;
import static org.junit.jupiter.api.Assertions.*;

/** A pack that withdrew from a fight builds nothing until it has chosen its new home, and that choice is saved. */
class SettleTest {
    @BeforeAll
    static void bootstrap() {
        EarthWorldFixture.bootstrap();
    }

    @AfterEach
    void defaults() {
        BloodFeud.config().buildCamps = true;
        Earth.reset();
    }

    private static FeudData reload(FeudData data) throws Exception {
        return reload(data.save(new CompoundTag(), null));
    }

    private static FeudData reload(CompoundTag saved) throws Exception {
        Method load = FeudData.class.getDeclaredMethod("load", CompoundTag.class, HolderLookup.Provider.class);
        load.setAccessible(true);
        return (FeudData) load.invoke(null, saved, null);
    }

    @Test
    void aSettlingPackBuildsNothingUntilItHasAHome() {
        FakeLevel level = EarthWorldFixture.level(70, -64, 320);
        level.key = Level.OVERWORLD;
        level.cubes = null;
        level.difficulty = Difficulty.PEACEFUL;
        FeudData data = new FeudData();
        Pack pack = new Pack(new UUID(5, 7), Family.ROTTEN, "minecraft:overworld", "Test", 3, X, Z, 0);
        pack.grunts = 6;
        pack.settling = true;
        data.feud.packs.put(pack.id, pack);
        CampBuilder builder = new CampBuilder();
        builder.setSight((server, lvl, origin, radius) -> false);
        long clock = 1000;
        for (int i = 0; i < 400; i++, clock++) {
            builder.tick(level.getServer(), data, clock);
            if (clock % 40 == 17) builder.checkAutoBuilds(level.getServer(), data, clock, true);
        }
        assertFalse(pack.siteSet, "no site while it is on the move");
        assertNull(pack.works);
        assertFalse(builder.isBuilding(pack.id));

        data.feud.settle(pack, X, Z);
        for (int i = 0; i < 2000 && pack.builtTier == 0; i++, clock++) {
            builder.tick(level.getServer(), data, clock);
            if (clock % 40 == 17) builder.checkAutoBuilds(level.getServer(), data, clock, true);
        }
        assertTrue(pack.siteSet, "once settled, it picks a site at its new home");
        assertTrue(Math.abs(pack.siteX - X) <= 40 && Math.abs(pack.siteZ - Z) <= 40, "near the home");
        assertEquals(1, pack.builtTier, "and builds its camp there");
    }

    @Test
    void aMoveInProgressIsSavedAndOldSavesKeepTheirHomes() throws Exception {
        FeudData data = new FeudData();
        Pack moving = new Pack(new UUID(1, 2), Family.BONES, "minecraft:overworld", "Moving", 4, 300, -40, 0);
        moving.settling = true;
        moving.settleFromX = 10;
        moving.settleFromZ = 20;
        Pack settled = new Pack(new UUID(3, 4), Family.ROTTEN, "minecraft:overworld", "Settled", 5, 77, 88, 0);
        data.feud.packs.put(moving.id, moving);
        data.feud.packs.put(settled.id, settled);
        FeudData back = reload(data);
        Pack m = back.feud.pack(moving.id);
        assertTrue(m.settling);
        assertEquals(300, m.homeX);
        assertEquals(-40, m.homeZ);
        assertEquals(10, m.settleFromX);
        assertEquals(20, m.settleFromZ);
        assertFalse(back.feud.pack(settled.id).settling);

        // A beta.2 save has no "settling" at all: its packs stay where they are.
        CompoundTag saved = data.save(new CompoundTag(), null);
        for (Tag t : saved.getList("packs", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            c.remove("settling");
            c.remove("settleFromX");
            c.remove("settleFromZ");
        }
        FeudData old = reload(saved);
        for (Pack p : old.feud.packs.values()) assertFalse(p.settling, p.place + " keeps its home");
        assertEquals(300, old.feud.pack(moving.id).homeX);
        assertEquals(77, old.feud.pack(settled.id).homeX);
        assertEquals(88, old.feud.pack(settled.id).homeZ);
    }

    @Test
    void theNewHomeIsDryLevelGroundNoOtherPackHolds() {
        Feud f = new Feud(new Random(1));
        Pack p = new Pack(new UUID(6, 6), Family.ROTTEN, "minecraft:overworld", "New", 1, 0, 0, 0);
        f.packs.put(p.id, p);
        List<int[]> candidates = List.of(new int[]{200, 0}, new int[]{0, 200}, new int[]{-200, 0}, new int[]{0, -200});
        int n = Settler.SAMPLES.length;
        int[][] ground = new int[candidates.size() * n][];
        for (int i = 0; i < ground.length; i++) ground[i] = new int[]{70, 0};
        ground[0] = new int[]{62, Settler.WET};       // the first is under water
        ground[n + 3] = new int[]{90, 0};              // the second is a cliff
        assertArrayEquals(new int[]{-200, 0}, Settler.choose(f, p, candidates, ground, (x, z) -> 1, (x, z) -> 0).pick());

        Pack other = new Pack(new UUID(7, 7), Family.BONES, "minecraft:overworld", "Other", 2, -200, 0, 0);
        other.siteSet = true;
        other.siteX = -210;
        other.siteZ = 5;
        f.packs.put(other.id, other);
        assertArrayEquals(new int[]{0, -200}, Settler.choose(f, p, candidates, ground, (x, z) -> 1, (x, z) -> 0).pick(), "the third is another pack's");

        for (int i = 0; i < ground.length; i++) ground[i] = new int[]{60, 1};
        assertNull(Settler.choose(f, p, candidates, ground, (x, z) -> 1, (x, z) -> 0).pick(), "all sea");
        assertTrue(Settler.dryAndLevel(new int[][]{{70, 0}, {71, 0}, {72, 0}, {73, 0}, {74, 0}, {75, 0}, {76, 0}, {77, 0}, {78, 0}}, 0));
        assertFalse(Settler.dryAndLevel(new int[][]{{70, 0}, {71, 0}, {72, 0}, {73, 0}, {74, 0}, {75, 0}, {76, 0}, {77, 0}, {79, 0}}, 0));
    }
}
