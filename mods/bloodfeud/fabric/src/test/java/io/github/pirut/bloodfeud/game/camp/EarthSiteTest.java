package io.github.pirut.bloodfeud.game.camp;

import io.github.pirut.bloodfeud.feud.Family;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.SettlementTier;
import io.github.pirut.bloodfeud.game.Earth;
import io.github.pirut.bloodfeud.game.EarthWorldFixture;
import io.github.pirut.bloodfeud.game.EarthWorldFixture.FakeLevel;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.github.pirut.bloodfeud.game.EarthWorldFixture.X;
import static io.github.pirut.bloodfeud.game.EarthWorldFixture.Z;
import static org.junit.jupiter.api.Assertions.*;

/** The off-screen side: what is asked of the Earth, and from which thread. */
class EarthSiteTest {
    @BeforeAll
    static void bootstrap() {
        EarthWorldFixture.bootstrap();
    }

    @Test
    void theReadyBoxHoldsEverythingAnyPlanTouches() {
        UUID id = new UUID(5, 6);
        List<Integer> vassals = List.of(1, 2, 3, 4, 5, 6);
        for (Family family : Family.values()) {
            for (int tier = 1; tier <= SettlementTier.TOWN.level; tier++) {
                for (int facing = 0; facing < 4; facing++) {
                    List<CampBlueprint.Plan> plans = new ArrayList<>(List.of(
                            CampBlueprint.plan(id, family, 3, vassals, facing, 0, tier),
                            CampBlueprint.ruinPlan(id, family, 3, vassals, facing, tier),
                            CampBlueprint.ruinDiffPlan(id, family, 3, vassals, facing, tier),
                            CampBlueprint.repairPlan(id, family, 3, vassals, facing, tier, tier)));
                    int side = SettlementTier.byLevel(tier).radius + SiteFinder.SITE_SIDE_MARGIN;
                    for (CampBlueprint.Plan plan : plans) {
                        for (CampBlueprint.Placement p : plan.placements()) {
                            int down = p.material() == CampBlueprint.Material.FOUNDATION_COLUMN ? CampBuilder.MAX_FOUNDATION_DEPTH : 0;
                            String what = family + " tier " + tier + " " + p;
                            assertTrue(Math.abs(p.rx()) + 1 <= side && Math.abs(p.rz()) + 1 <= side, what);
                            assertTrue(p.ry() - down - 1 >= -SiteFinder.SITE_DOWN, what);
                            assertTrue(p.ry() + 2 <= SiteFinder.SITE_UP, what);
                        }
                        for (CampBlueprint.Box box : plan.zones().values()) {
                            assertTrue(Math.min(-2, box.minY()) >= -SiteFinder.SITE_DOWN, box.toString());
                            assertTrue(Math.max(6, box.maxY()) <= SiteFinder.SITE_UP, box.toString());
                        }
                    }
                }
            }
        }
    }

    @Test
    void requestsCoverTheBoxAndNeverReadMapDataOnTheServerThread() throws Exception {
        for (int width = 1; width <= 8; width++) {
            FakeLevel level = EarthWorldFixture.level(1530, 0, 16);
            int minX = X & ~15, minY = 1520 & ~15, minZ = Z & ~15, size = width * 16 - 1;
            long until = System.nanoTime() + 10_000_000_000L;
            while (level.cubes.requested.isEmpty() && System.nanoTime() < until) {
                assertFalse(Earth.ready(level, minX, minY, minZ, minX + size, minY + size, minZ + size));
                Thread.sleep(2);
            }
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < width; y++) {
                    for (int z = 0; z < width; z++) {
                        assertTrue(level.cubes.requested.contains(new BlockPos((minX >> 4) + x, (minY >> 4) + y, (minZ >> 4) + z)),
                                "width " + width + " misses " + x + "," + y + "," + z);
                    }
                }
            }
            if (width >= 3) assertEquals(width * width * width, level.cubes.requested.size(), "no overhang");
            level.cubes.deliver();
            assertTrue(Earth.ready(level, minX, minY, minZ, minX + size, minY + size, minZ + size));
            assertEquals(0, level.serverThreadMapReads.get());
        }
    }

    @Test
    void findsAnOffscreenSiteWithCubesThatVanishBetweenTicks() throws Exception {
        FakeLevel level = EarthWorldFixture.level(1530, 0, 16);
        Pack pack = new Pack(new UUID(1, 2), Family.ROTTEN, "earthplatform:cubic", "Test", 14, X, Z, 0);
        EarthSiteSearch search = new EarthSiteSearch(level, pack, SettlementTier.CAMP.radius);
        SiteFinder.Site site = null;
        long until = System.nanoTime() + 10_000_000_000L;
        for (int tick = 0; site == null && System.nanoTime() < until; tick++) {
            if (tick % 8 == 7) level.cubes.loaded.clear(); // the Earth's unload
            else level.cubes.deliver();
            EarthSiteSearch.Step step = search.step(level, pack);
            site = step.site();
            if (site == null) assertFalse(step.done(), "gave up");
            Thread.sleep(2);
        }
        assertNotNull(site);
        assertEquals(new BlockPos(X, 1531, Z), site.origin());
        assertEquals(0, level.serverThreadMapReads.get());
        assertEquals(0, level.unknownHeightReads.get());
    }

    @Test
    void aLevelWhoseInternalsCannotBeReadIsNeverBuiltIn() {
        FakeLevel level = EarthWorldFixture.level(70, 0, 16);
        level.cubes = null;
        BlockPos origin = new BlockPos(X, 71, Z);
        assertFalse(SiteFinder.isSiteReady(level, origin, 7));
        assertNull(SiteFinder.findSurfaceY(level, X, Z));
        assertTrue(SiteFinder.isPlayerBlockAt(level, origin, origin, java.util.Set.of(), java.util.Set.of()));
    }
}
