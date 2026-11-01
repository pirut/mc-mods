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
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
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

    private float[] sent = new float[0];
    private long lastSync;

    /** Read by the physics tick. */
    private volatile Buoyancy.Sample sample = Buoyancy.Sample.NONE;
    private Lumps lumps = Lumps.NONE;

    private record Scan(HullSnapshot snapshot, HullAnalysis.Result result, double[] volume) {
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
                    return new Scan(snapshot, result, new double[result.compartments().size()]);
                }, ANALYSIS);
            }
        }

        Scan s = scan;
        if (s == null) return;

        if (config.flooding() && now % FLOOD_INTERVAL == phase) flood(s, config, FLOOD_INTERVAL / 20.0);

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
            sample = Buoyancy.sample(subLevel, s.origin(), lumps, probe, config);
            if (now - lastSync >= 10) sync(s, now);
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
        if (old == null && level instanceof ServerLevel server) wakeSea(server);
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

    /** Most blocks of the sea under a ship (positions) checked at once, so a giant ship can't stall the server. */
    private static final int WAKE_LIMIT = 4_000_000;

    /**
     * When a ship is put together on the water, its blocks leave the world, and the hole they leave in the sea can
     * stay dry: water only fills a gap when something nudges it. A ship floating over a dry hole has nothing to
     * displace, and sits blocks too low. So the first time we see a ship, every bit of sea beside an air gap under
     * it is told to flow, and the hole fills as it would if you dug it out by hand.
     */
    private void wakeSea(ServerLevel server) {
        BoundingBox3ic b = subLevel.getPlot().getBoundingBox();
        Pose3dc pose = subLevel.logicalPose();
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        Vector3d p = new Vector3d();
        for (int c = 0; c < 8; c++) {
            p.set((c & 1) == 0 ? b.minX() : b.maxX() + 1, (c & 2) == 0 ? b.minY() : b.maxY() + 1, (c & 4) == 0 ? b.minZ() : b.maxZ() + 1);
            p = pose.transformPosition(p);
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            minZ = Math.min(minZ, p.z);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
            maxZ = Math.max(maxZ, p.z);
        }
        // Not clamped to the level's build height: some dimensions (Earth Platform's) report -64..320 and reach far beyond.
        int x0 = (int) Math.floor(minX) - 1, y0 = (int) Math.floor(minY) - 1, z0 = (int) Math.floor(minZ) - 1;
        int x1 = (int) Math.ceil(maxX) + 1, y1 = (int) Math.ceil(maxY) + 1, z1 = (int) Math.ceil(maxZ) + 1;
        if ((long) (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1) > WAKE_LIMIT) return;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (!server.hasChunk(x >> 4, z >> 4)) continue;
                for (int y = y0; y <= y1; y++) {
                    at.set(x, y, z);
                    FluidState fluid = server.getFluidState(at);
                    if (!fluid.is(FluidTags.WATER)) continue;
                    for (Direction d : WAKE_DIRECTIONS) {
                        if (server.getBlockState(next.setWithOffset(at, d)).isAir()) {
                            server.scheduleTick(at.immutable(), fluid.getType(), fluid.getType().getTickDelay(server));
                            break;
                        }
                    }
                }
            }
        }
    }

    private static final Direction[] WAKE_DIRECTIONS = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private void flood(Scan s, SeaworthyConfig config, double seconds) {
        Pose3dc pose = subLevel.logicalPose();
        HullGrid g = s.grid();
        BlockPos o = s.origin();
        Vector3d p = new Vector3d();
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
            s.volume()[k] = Flooding.step(g, c, s.volume()[k], outside, GRAVITY, DISCHARGE * config.floodSpeed(), seconds);
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
        return false;
    }

    /** Regathers the hull's dry air and floodwater for buoyancy, with the filling layer spread evenly. */
    private void rebuildLumps(Scan s, SeaworthyConfig config) {
        float[] wet = new float[s.grid().volume()];
        List<Compartment> compartments = s.result().compartments();
        for (int k = 0; k < compartments.size(); k++) Flooding.wetness(compartments.get(k), s.volume()[k], wet);
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

    /** Called with each cell whose top is the water's surface, in plot coordinates, and how far up it the water is. */
    interface SurfaceSink {
        void cell(int x, int y, int z, float height);
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
            for (int i : top.fixed()) sink.cell(o.getX() + g.x(i), o.getY() + g.y(i), o.getZ() + g.z(i), 1f);
            if (top.live().length == 0) continue;
            float h = (float) Math.max(0.02, top.liveHeight(compartments.get(k), s.volume()[k]));
            for (int i : top.live()) sink.cell(o.getX() + g.x(i), o.getY() + g.y(i), o.getZ() + g.z(i), h);
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
        clear();
    }
}
