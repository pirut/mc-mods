package io.github.pirut.seaworthy.sable;

import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionContainer;
import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionRegion;
import dev.ryanhcode.sable.util.BoundedBitVolume3i;
import io.github.pirut.seaworthy.Seaworthy;
import io.github.pirut.seaworthy.SeaworthyConfig;
import io.github.pirut.seaworthy.hull.Breach;
import io.github.pirut.seaworthy.hull.Compartment;
import io.github.pirut.seaworthy.hull.Flooding;
import io.github.pirut.seaworthy.hull.HullAnalysis;
import io.github.pirut.seaworthy.hull.HullGrid;
import io.github.pirut.seaworthy.hull.Lumps;
import io.github.pirut.seaworthy.net.FloodSyncPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One ship's hull: its compartments, how much water is in each, the dry spaces registered with Sable's water
 * occlusion, and (on the server) the lift its dry air gives.
 */
final class ShipHull {
    private static final AtomicInteger THREADS = new AtomicInteger();
    private static final ExecutorService ANALYSIS = Executors.newFixedThreadPool(
            Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() / 2)), r -> {
                Thread t = new Thread(r, "Seaworthy hull " + THREADS.incrementAndGet());
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            });

    /** Flood levels are worked out this often (ticks); water moves slowly enough. */
    private static final int FLOOD_INTERVAL = 4;
    /** Wait this long after a block changes before looking at the hull again, so building isn't rescanned per block. */
    private static final int SCAN_DEBOUNCE = 10;
    private static final double GRAVITY = 9.81;
    private static final double DISCHARGE = 0.6;
    /** The water is written to the level's saved data this often (ticks), and when the ship unloads. */
    private static final int SAVE_INTERVAL = 100;
    /** How far the world's up can move (about 2 degrees) before the water is laid flat again. */
    private static final double RELEVEL = 0.035;
    /** Compartments bigger than this stay in level layers, to keep regathering them cheap. */
    private static final int MAX_TILTED_CELLS = 200_000;
    private static final int LEAK_INTERVAL = 5;
    private static final int LEAK_SOUND_INTERVAL = 40;
    /** Holes looked at, and holes shown, per compartment per interval, so a shattered hull can't flood the screen. */
    private static final int LEAK_CHECKS = 256;
    private static final int LEAK_SHOWN = 3;
    private static final double LEAK_RANGE = 32;
    private static final double LEAK_NEAR_SHIP = 128;
    /** The speed at which a pump moves {@code pumpRate} blocks of water a second. */
    private static final double PUMP_FULL_RPM = 256;

    private final SubLevel subLevel;
    private final Level level;
    private final boolean client;
    private final WaterProbe probe;
    private final int phase;

    private boolean dirty = true;
    private long lastScan = Long.MIN_VALUE / 2;
    private int age;
    private @Nullable CompletableFuture<Scan> pending;
    private @Nullable Scan scan;
    private boolean failed;

    private final List<WaterOcclusionRegion> regions = new ArrayList<>();
    private int[] regionWet = new int[0];
    private long lastRegionBuild = Long.MIN_VALUE / 2;
    private boolean regionsStale;

    /** On the player's game: where to draw the top of the water in each compartment. */
    private Flooding.Surface[] surfaces = new Flooding.Surface[0];

    private double[] lumpVolume = new double[0];
    private long lastLumpBuild = Long.MIN_VALUE / 2;
    private boolean lumpsStale;
    private final Vector3d lumpUp = new Vector3d(0, 1, 0);
    private int[] pumpsRunning = new int[0];

    private float[] sent = new float[0];
    private long lastSync;

    /** Read by the physics tick. */
    private volatile Buoyancy.Sample sample = Buoyancy.Sample.NONE;
    private Lumps lumps = Lumps.NONE;
    private @Nullable SeaWake wake;

    private record Scan(HullSnapshot snapshot, HullAnalysis.Result result, double[] volume, double[][] centres) {
        HullGrid grid() {
            return snapshot.grid();
        }

        BlockPos origin() {
            return snapshot.origin();
        }
    }

    ShipHull(SubLevel subLevel) {
        this.subLevel = subLevel;
        this.level = subLevel.getLevel();
        this.client = level.isClientSide();
        this.probe = new WaterProbe(level);
        this.phase = Math.floorMod(subLevel.getUniqueId().hashCode(), FLOOD_INTERVAL);
    }

    SubLevel subLevel() {
        return subLevel;
    }

    Buoyancy.Sample sample() {
        return sample;
    }

    void markDirty() {
        dirty = true;
        failed = false;
    }

    void fail(RuntimeException e) {
        if (!failed) Seaworthy.LOG.error("Seaworthy stopped working on ship {} after an error", subLevel.getUniqueId(), e);
        failed = true;
        dispose();
    }

    void tick(long now) {
        if (failed) return;
        age++;
        SeaworthyConfig config = Seaworthy.config();

        if (pending != null && pending.isDone()) {
            Scan next = null;
            try {
                next = pending.join();
            } catch (RuntimeException e) {
                Seaworthy.LOG.error("Couldn't work out the hull of ship {}", subLevel.getUniqueId(), e);
            }
            pending = null;
            if (next != null) adopt(next);
        }

        // A player's game may get the ship's blocks a little after the ship itself, without a block change.
        if (client && age <= 600 && age % 100 == 0) dirty = true;
        if (scan != null && now % 20 == 0 && boundsChanged(scan.origin(), scan.grid())) dirty = true;

        if (dirty && pending == null && now - lastScan >= SCAN_DEBOUNCE) {
            dirty = false;
            lastScan = now;
            HullSnapshot snapshot = HullSnapshot.take(subLevel, config.maxHullCells());
            if (snapshot == null || snapshot.blocks() == 0) {
                clear();
            } else {
                int holeWidth = config.maxHoleWidth();
                pending = CompletableFuture.supplyAsync(() -> {
                    HullAnalysis.Result result = HullAnalysis.analyze(snapshot.grid(), holeWidth);
                    double[][] centres = new double[result.compartments().size()][];
                    for (int k = 0; k < centres.length; k++) centres[k] = Flooding.centre(snapshot.grid(), result.compartments().get(k));
                    return new Scan(snapshot, result, new double[centres.length], centres);
                }, ANALYSIS);
            }
        }

        Scan s = scan;
        if (s == null) return;

        if (config.flooding() && now % FLOOD_INTERVAL == phase) {
            flood(s, config, FLOOD_INTERVAL / 20.0);
            if (!client) pumpOut(s, config, FLOOD_INTERVAL / 20.0);
        }

        if (client) leakEffects(s, now);
        long cooldown = s.grid().volume() > 2_000_000 ? 40 : 10;
        if (regionsStale || wetChanged(s)) {
            if (regionsStale || now - lastRegionBuild >= cooldown) {
                rebuild(s, config);
                lastRegionBuild = now;
                regionsStale = false;
            }
        }

        if (!client && (lumpsStale || (lumpsChanged(s) && now - lastLumpBuild >= cooldown))) {
            rebuildLumps(s, config);
            lastLumpBuild = now;
            lumpsStale = false;
        }

        if (!client) {
            if (wake != null && wake.run((ServerLevel) level)) wake = null;
            sample = Buoyancy.sample(subLevel, s.origin(), lumps, probe, config);
            if (now - lastSync >= 10) sync(s, now);
            if (now % SAVE_INTERVAL == phase % SAVE_INTERVAL) saveWater(s);
        }
    }

    private boolean boundsChanged(BlockPos origin, HullGrid grid) {
        BoundingBox3ic b = subLevel.getPlot().getBoundingBox();
        return b.minX() - 1 != origin.getX() || b.minY() - 1 != origin.getY() || b.minZ() - 1 != origin.getZ()
                || b.maxX() - b.minX() + 3 != grid.sx || b.maxY() - b.minY() + 3 != grid.sy || b.maxZ() - b.minZ() + 3 != grid.sz;
    }

    /** Takes over a fresh analysis, carrying the water over: each new compartment keeps the flooded cells it got. */
    private void adopt(Scan next) {
        Scan old = scan;
        if (old == null && level instanceof ServerLevel server) {
            FloodStore store = FloodStore.of(server);
            if (store.firstSight(subLevel.getUniqueId())) wake = SeaWake.under(subLevel);
            if (Seaworthy.config().drainShipWater() && store.firstDrain(subLevel.getUniqueId())) drainCarriedWater(server, next);
            restoreWater(server, next);
        }
        if (old != null) {
            HullGrid og = old.grid(), ng = next.grid();
            BlockPos shift = old.origin().subtract(next.origin());
            float[] wet = new float[og.volume()];
            for (int k = 0; k < old.result().compartments().size(); k++) {
                Compartment c = old.result().compartments().get(k);
                Flooding.wetness(c, old.volume()[k], wet);
                for (int i : c.cells()) {
                    if (wet[i] <= 0) continue;
                    int x = og.x(i) + shift.getX(), y = og.y(i) + shift.getY(), z = og.z(i) + shift.getZ();
                    if (!ng.inBounds(x, y, z)) continue;
                    int comp = next.result().compartmentOf()[ng.index(x, y, z)];
                    if (comp >= 0) next.volume()[comp] += wet[i];
                }
            }
        }
        scan = next;
        regionsStale = true;
        lumpsStale = true;
    }

    /**
     * Removes the water blocks inside the hull that the ship was assembled with, such as the sea it was built in. They
     * are part of the ship, so occlusion can't hide them, and they'd sit in the hull for good. Waterlogged blocks are
     * left alone. The block changes make the hull be worked out again.
     */
    private void drainCarriedWater(ServerLevel server, Scan next) {
        HullGrid g = next.grid();
        BlockPos o = next.origin();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int removed = 0;
        for (Compartment c : next.result().compartments()) {
            for (int i : c.cells()) {
                if ((g.flags(i) & HullGrid.FLUID) == 0) continue;
                pos.set(o.getX() + g.x(i), o.getY() + g.y(i), o.getZ() + g.z(i));
                BlockState state = server.getBlockState(pos);
                if (!(state.getBlock() instanceof LiquidBlock) || !state.getFluidState().is(FluidTags.WATER)) continue;
                server.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                removed++;
            }
        }
        if (removed > 0) Seaworthy.LOG.info("Removed {} water blocks ship {} was assembled with", removed, subLevel.getUniqueId());
    }

    /** Each compartment's anchor cell, relative to the ship's bounds: what its saved water is filed under. */
    private static long[] saveKeys(Scan s) {
        List<Compartment> compartments = s.result().compartments();
        HullGrid g = s.grid();
        long[] keys = new long[compartments.size()];
        for (int k = 0; k < keys.length; k++) {
            int a = compartments.get(k).anchor();
            keys[k] = BlockPos.asLong(g.x(a), g.y(a), g.z(a));
        }
        return keys;
    }

    /** Puts back the water a ship had when it was last saved. */
    private void restoreWater(ServerLevel server, Scan next) {
        Map<Long, Float> saved = FloodStore.of(server).get(subLevel.getUniqueId());
        if (saved.isEmpty()) return;
        long[] keys = saveKeys(next);
        List<Compartment> compartments = next.result().compartments();
        for (int k = 0; k < keys.length; k++) {
            Float volume = saved.get(keys[k]);
            if (volume != null) next.volume()[k] = Math.min(compartments.get(k).size(), volume);
        }
    }

    private void saveWater(Scan s) {
        if (!(level instanceof ServerLevel server)) return;
        float[] volumes = new float[s.volume().length];
        for (int k = 0; k < volumes.length; k++) volumes[k] = (float) s.volume()[k];
        FloodStore.of(server).put(subLevel.getUniqueId(), saveKeys(s), volumes);
    }

    /** Create pumps running on the ship empty the compartment they're in, or the wettest one they touch. */
    private void pumpOut(Scan s, SeaworthyConfig config, double seconds) {
        if (pumpsRunning.length != s.volume().length) pumpsRunning = new int[s.volume().length];
        java.util.Arrays.fill(pumpsRunning, 0);
        if (s.snapshot().pumps().length == 0) return;
        HullGrid g = s.grid();
        BlockPos o = s.origin();
        int[] compartmentOf = s.result().compartmentOf();
        for (long packed : s.snapshot().pumps()) {
            BlockPos pos = BlockPos.of(packed);
            float speed = CreateCompat.speed(level.getBlockEntity(pos));
            if (speed == 0) continue;
            int x = pos.getX() - o.getX(), y = pos.getY() - o.getY(), z = pos.getZ() - o.getZ();
            int k = compartmentOf[g.index(x, y, z)];
            if (k < 0) {
                double wettest = -1;
                for (Direction side : Direction.values()) {
                    int nx = x + side.getStepX(), ny = y + side.getStepY(), nz = z + side.getStepZ();
                    if (!g.inBounds(nx, ny, nz)) continue;
                    int near = compartmentOf[g.index(nx, ny, nz)];
                    if (near >= 0 && s.volume()[near] > wettest) {
                        wettest = s.volume()[near];
                        k = near;
                    }
                }
            }
            if (k < 0) continue;
            pumpsRunning[k]++;
            s.volume()[k] = Flooding.pump(s.volume()[k], config.pumpRate() * Math.abs(speed) / PUMP_FULL_RPM, seconds);
        }
    }

    private void flood(Scan s, SeaworthyConfig config, double seconds) {
        Pose3dc pose = subLevel.logicalPose();
        HullGrid g = s.grid();
        BlockPos o = s.origin();
        Vector3d p = new Vector3d();
        Vector3d up = worldUp();
        List<Compartment> compartments = s.result().compartments();
        for (int k = 0; k < compartments.size(); k++) {
            Compartment c = compartments.get(k);
            if (c.sealed()) continue;
            List<Breach> breaches = c.breaches();
            double[] outside = new double[breaches.size()];
            for (int b = 0; b < breaches.size(); b++) {
                int cell = breaches.get(b).outside();
                p.set(o.getX() + g.x(cell) + 0.5, o.getY() + g.y(cell) + 0.5, o.getZ() + g.z(cell) + 0.5);
                p = pose.transformPosition(p);
                double surface = probe.surfaceAbove(p.x, p.y, p.z);
                if (Double.isNaN(surface)) {
                    outside[b] = Double.NaN;
                } else {
                    p.y = surface;
                    p = pose.transformPositionInverse(p);
                    outside[b] = p.y - o.getY();
                }
            }
            double[] lean = Flooding.lean(g, c, s.centres()[k], up.x, up.y, up.z);
            s.volume()[k] = Flooding.step(g, c, s.volume()[k], outside, lean, GRAVITY, DISCHARGE * config.floodSpeed(), seconds);
        }
    }

    private boolean wetChanged(Scan s) {
        List<Compartment> compartments = s.result().compartments();
        if (regionWet.length != compartments.size()) return true;
        for (int k = 0; k < compartments.size(); k++) {
            if (Flooding.wetCells(compartments.get(k), s.volume()[k]) != regionWet[k]) return true;
            // The surface appears when water first comes in, and goes when the last drains.
            if (client && k < surfaces.length && (surfaces[k] == Flooding.Surface.NONE) != (s.volume()[k] <= 0)) return true;
        }
        return false;
    }

    /** Whether the water has moved by a block's worth in any compartment since the air was last gathered. */
    private boolean lumpsChanged(Scan s) {
        if (lumpVolume.length != s.volume().length) return true;
        for (int k = 0; k < lumpVolume.length; k++) {
            if (Math.abs(s.volume()[k] - lumpVolume[k]) >= 1) return true;
        }
        for (double volume : lumpVolume) {
            if (volume > 0) return worldUp().distanceSquared(lumpUp) > RELEVEL * RELEVEL;
        }
        return false;
    }

    /** Which way is up in the world, in the ship's frame. */
    private Vector3d worldUp() {
        return subLevel.logicalPose().orientation().transformInverse(new Vector3d(0, 1, 0));
    }

    /** Regathers the hull's dry air and floodwater for buoyancy, with the water lying flat in the world. */
    private void rebuildLumps(Scan s, SeaworthyConfig config) {
        float[] wet = new float[s.grid().volume()];
        List<Compartment> compartments = s.result().compartments();
        Vector3d up = worldUp();
        lumpUp.set(up);
        for (int k = 0; k < compartments.size(); k++) {
            Compartment c = compartments.get(k);
            if (c.size() > MAX_TILTED_CELLS) Flooding.wetness(c, s.volume()[k], wet);
            else Flooding.wetness(s.grid(), c, s.volume()[k], up.x, up.y, up.z, wet);
        }
        lumpVolume = s.volume().clone();
        lumps = Lumps.build(s.result(), wet, Lumps.sizeFor(s.result().interiorCells(), config.maxLumps()));
    }

    /** Re-registers the dry spaces with Sable. */
    private void rebuild(Scan s, SeaworthyConfig config) {
        HullGrid g = s.grid();
        BlockPos o = s.origin();
        List<Compartment> compartments = s.result().compartments();
        regionWet = new int[compartments.size()];
        for (int k = 0; k < compartments.size(); k++) {
            Compartment c = compartments.get(k);
            int n = Flooding.wetCells(c, s.volume()[k]);
            regionWet[k] = n;
        }
        if (client) {
            Flooding.Surface[] next = new Flooding.Surface[compartments.size()];
            for (int k = 0; k < compartments.size(); k++) next[k] = Flooding.surface(g, compartments.get(k), s.volume()[k]);
            surfaces = next;
        }

        WaterOcclusionContainer<?> occlusion = WaterOcclusionContainer.getContainer(level);
        if (occlusion != null) {
            removeRegions(occlusion);
            for (int k = 0; k < compartments.size(); k++) {
                Compartment c = compartments.get(k);
                int[] cells = c.cells();
                int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
                int dry = 0;
                for (int w = regionWet[k]; w < cells.length; w++) {
                    int i = cells[w];
                    if ((g.flags(i) & HullGrid.FLUID) != 0) continue;
                    minX = Math.min(minX, g.x(i));
                    minY = Math.min(minY, g.y(i));
                    minZ = Math.min(minZ, g.z(i));
                    maxX = Math.max(maxX, g.x(i));
                    maxY = Math.max(maxY, g.y(i));
                    maxZ = Math.max(maxZ, g.z(i));
                    dry++;
                }
                if (dry == 0) continue;
                BoundedBitVolume3i volume = new BoundedBitVolume3i(o.getX() + minX, o.getY() + minY, o.getZ() + minZ,
                        o.getX() + maxX, o.getY() + maxY, o.getZ() + maxZ);
                for (int w = regionWet[k]; w < cells.length; w++) {
                    int i = cells[w];
                    if ((g.flags(i) & HullGrid.FLUID) != 0) continue;
                    volume.setOccupied(o.getX() + g.x(i), o.getY() + g.y(i), o.getZ() + g.z(i), true);
                }
                regions.add(occlusion.addRegion(volume));
            }
        }
    }

    /** One compartment, for telling a player about it. */
    record CompartmentStatus(int size, double volume, int leaks, int pumps) {
    }

    /** The ship's compartments, or none if it hasn't been looked at yet. */
    List<CompartmentStatus> status() {
        Scan s = scan;
        if (s == null || failed) return List.of();
        List<Compartment> compartments = s.result().compartments();
        List<CompartmentStatus> status = new ArrayList<>(compartments.size());
        for (int k = 0; k < compartments.size(); k++) {
            Compartment c = compartments.get(k);
            status.add(new CompartmentStatus(c.size(), s.volume()[k], leaks(s, c), k < pumpsRunning.length ? pumpsRunning[k] : 0));
        }
        return status;
    }

    /** The water's height outside a breach, in the ship's frame relative to its grid, or NaN if it's dry there. */
    private double outsideSurface(Scan s, Breach breach) {
        HullGrid g = s.grid();
        BlockPos o = s.origin();
        Pose3dc pose = subLevel.logicalPose();
        int cell = breach.outside();
        Vector3d p = new Vector3d(o.getX() + g.x(cell) + 0.5, o.getY() + g.y(cell) + 0.5, o.getZ() + g.z(cell) + 0.5);
        p = pose.transformPosition(p);
        double surface = probe.surfaceAbove(p.x, p.y, p.z);
        if (Double.isNaN(surface)) return Double.NaN;
        p.y = surface;
        p = pose.transformPositionInverse(p);
        return p.y - o.getY();
    }

    /** Spray or bubbles at the holes where water is coming in, and the sound of it, on the player's game. */
    private void leakEffects(Scan s, long now) {
        if (now % LEAK_INTERVAL != phase % LEAK_INTERVAL) return;
        var position = subLevel.logicalPose().position();
        if (level.getNearestPlayer(position.x(), position.y(), position.z(), LEAK_NEAR_SHIP, entity -> true) == null) return;
        HullGrid g = s.grid();
        BlockPos o = s.origin();
        Pose3dc pose = subLevel.logicalPose();
        List<Compartment> compartments = s.result().compartments();
        for (int k = 0; k < compartments.size(); k++) {
            Compartment c = compartments.get(k);
            List<Breach> breaches = c.breaches();
            if (breaches.isEmpty()) continue;
            double inside = c.surface(s.volume()[k]);
            int start = level.random.nextInt(breaches.size());
            int shown = 0;
            for (int j = 0; j < Math.min(breaches.size(), LEAK_CHECKS) && shown < LEAK_SHOWN; j++) {
                Breach breach = breaches.get((start + j) % breaches.size());
                double outside = outsideSurface(s, breach);
                double hole = breach.height(g);
                if (Double.isNaN(outside) || outside <= hole || outside <= inside) continue;
                int in = breach.inside(), out = breach.outside();
                Vector3d p = pose.transformPosition(new Vector3d(o.getX() + (g.x(in) + g.x(out)) / 2.0 + 0.5,
                        o.getY() + (g.y(in) + g.y(out)) / 2.0 + 0.5, o.getZ() + (g.z(in) + g.z(out)) / 2.0 + 0.5));
                if (level.getNearestPlayer(p.x, p.y, p.z, LEAK_RANGE, entity -> true) == null) return;
                boolean underWater = inside > hole + 0.1;
                for (int n = 0; n < 3; n++) {
                    double dx = (level.random.nextDouble() - 0.5) * 0.5, dz = (level.random.nextDouble() - 0.5) * 0.5;
                    level.addParticle(underWater ? ParticleTypes.BUBBLE : ParticleTypes.SPLASH, p.x + dx, p.y, p.z + dz, dx * 0.2, 0.15, dz * 0.2);
                }
                if (shown == 0 && now % LEAK_SOUND_INTERVAL < LEAK_INTERVAL) {
                    level.playLocalSound(p.x, p.y, p.z, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 0.4f, 1.2f, false);
                }
                shown++;
            }
        }
    }

    /** How many of the compartment's breaches have water outside them, above the hole. */
    private int leaks(Scan s, Compartment c) {
        HullGrid g = s.grid();
        int leaks = 0;
        for (Breach breach : c.breaches()) {
            double outside = outsideSurface(s, breach);
            if (!Double.isNaN(outside) && outside > breach.height(g)) leaks++;
        }
        return leaks;
    }

    /**
     * Called with each cell whose top is the water's surface, in plot coordinates, and how far up it the water is. The
     * middle of the compartment's surface ({@code cx}, {@code cz}) and the top of the compartment ({@code ceiling})
     * are for laying the water flat in the world when the ship leans.
     */
    interface SurfaceSink {
        void cell(int x, int y, int z, float height, double cx, double cz, int ceiling);
    }

    /** The water's surface in every compartment that has some, for drawing it (on the player's game). */
    void surface(SurfaceSink sink) {
        Scan s = scan;
        Flooding.Surface[] all = surfaces;
        if (s == null || failed) return;
        List<Compartment> compartments = s.result().compartments();
        if (all.length != compartments.size()) return;
        HullGrid g = s.grid();
        BlockPos o = s.origin();
        for (int k = 0; k < all.length; k++) {
            Flooding.Surface top = all[k];
            if (top.fixed().length == 0 && top.live().length == 0) continue;
            Compartment c = compartments.get(k);
            double sumX = 0, sumZ = 0;
            for (int i : top.fixed()) {
                sumX += g.x(i) + 0.5;
                sumZ += g.z(i) + 0.5;
            }
            for (int i : top.live()) {
                sumX += g.x(i) + 0.5;
                sumZ += g.z(i) + 0.5;
            }
            int n = top.fixed().length + top.live().length;
            double cx = o.getX() + sumX / n, cz = o.getZ() + sumZ / n;
            int ceiling = o.getY() + c.groupY()[c.groupY().length - 1] + 1;
            for (int i : top.fixed()) sink.cell(o.getX() + g.x(i), o.getY() + g.y(i), o.getZ() + g.z(i), 1f, cx, cz, ceiling);
            if (top.live().length == 0) continue;
            float h = (float) Math.max(0.02, top.liveHeight(c, s.volume()[k]));
            for (int i : top.live()) sink.cell(o.getX() + g.x(i), o.getY() + g.y(i), o.getZ() + g.z(i), h, cx, cz, ceiling);
        }
    }

    /** Tells the players who can see the ship how much water is in it. */
    private void sync(Scan s, long now) {
        if (!(subLevel instanceof ServerSubLevel server) || !(level instanceof ServerLevel serverLevel)) return;
        List<Compartment> compartments = s.result().compartments();
        boolean changed = sent.length != compartments.size();
        boolean any = false;
        float[] volumes = new float[compartments.size()];
        long[] anchors = new long[compartments.size()];
        HullGrid g = s.grid();
        BlockPos o = s.origin();
        for (int k = 0; k < compartments.size(); k++) {
            volumes[k] = (float) s.volume()[k];
            int a = compartments.get(k).anchor();
            anchors[k] = o.offset(g.x(a), g.y(a), g.z(a)).asLong();
            if (volumes[k] > 0) any = true;
            if (!changed && Math.abs(volumes[k] - sent[k]) > 0.2f) changed = true;
        }
        // Resend now and then while there's water, for players who just came into range.
        if (!changed && !(any && now - lastSync >= 100)) return;
        lastSync = now;
        sent = volumes;
        if (!any && !changed) return;
        FloodSyncPayload payload = new FloodSyncPayload(subLevel.getUniqueId(), anchors, volumes);
        for (UUID id : server.getTrackingPlayers()) {
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(id);
            if (player != null) Seaworthy.platform().sendFlood(player, payload);
        }
    }

    /** Takes the server's flood levels. False if we haven't worked out the hull yet. */
    boolean receive(FloodSyncPayload payload) {
        Scan s = scan;
        if (s == null) return false;
        HullGrid g = s.grid();
        BlockPos o = s.origin();
        for (int i = 0; i < payload.anchors().length; i++) {
            BlockPos a = BlockPos.of(payload.anchors()[i]);
            int x = a.getX() - o.getX(), y = a.getY() - o.getY(), z = a.getZ() - o.getZ();
            if (!g.inBounds(x, y, z)) continue;
            int k = s.result().compartmentOf()[g.index(x, y, z)];
            if (k >= 0) s.volume()[k] = Math.max(0, Math.min(s.result().compartments().get(k).size(), payload.volumes()[i]));
        }
        return true;
    }

    private void removeRegions(WaterOcclusionContainer<?> occlusion) {
        for (WaterOcclusionRegion region : regions) occlusion.removeRegion(region);
        regions.clear();
    }

    private void clear() {
        WaterOcclusionContainer<?> occlusion = WaterOcclusionContainer.getContainer(level);
        if (occlusion != null) removeRegions(occlusion);
        scan = null;
        regionWet = new int[0];
        surfaces = new Flooding.Surface[0];
        lumpVolume = new double[0];
        lumps = Lumps.NONE;
        sample = Buoyancy.Sample.NONE;
    }

    void dispose() {
        if (pending != null) pending.cancel(false);
        pending = null;
        if (scan != null) saveWater(scan);
        clear();
    }
}
