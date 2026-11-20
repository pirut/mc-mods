package io.github.pirut.bloodfeud.game.camp;

import io.github.pirut.bloodfeud.BloodFeud;
import io.github.pirut.bloodfeud.feud.Family;
import io.github.pirut.bloodfeud.feud.Feud;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.SettlementTier;
import io.github.pirut.bloodfeud.game.FeudData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Chunk-safe incremental builder that places, ruins, and repairs gang camps, outposts, strongholds and towns
 * over multiple ticks (~250 block ops/tick) without lag, chunk force-loading, or overwriting player builds
 * and containers.
 */
public final class CampBuilder {
    private static final int OPS_PER_TICK = 250;
    private static final int AUTO_TRIGGER_RANGE = 144;
    private static final int MAX_CONCURRENT_AUTO = 3;
    private static final long RETRY_COOLDOWN_TICKS = 2400L;
    private static final long STALL_TIMEOUT_TICKS = 600L;
    public static final int MAX_FOUNDATION_DEPTH = 14;

    public record QueueResult(boolean queued, List<String> skippedZones, int skippedRingColumns) {
    }

    /** Delegates to {@link Pack#autoBuildAction()} for automatic build/ruin/repair selection. */
    static Pack.AutoBuildAction autoActionFor(Pack pack) {
        return pack == null ? Pack.AutoBuildAction.NONE : pack.autoBuildAction();
    }

    private enum Mode { BUILD, RUIN, REPAIR, ABANDONED_RUIN }

    private record FilterResult(List<CampBlueprint.Placement> ops, Set<String> blockedZones,
                                Set<Long> blockedRingCols, List<String> skippedZones, int skippedRingColumns) {
    }

    private static final class BuildJob {
        final UUID packId;
        final ResourceKey<Level> dimension;
        final BlockPos origin;
        final int siteFacing;
        final Family family;
        final int packColor;
        final String packTitle;
        final int fromTier;
        final int toTier;
        final Mode mode;
        final boolean automatic;
        final boolean protectPlayerBlocks;
        final List<CampBlueprint.Placement> ops;
        final Set<CampBlueprint.Spot> ownedPositions;
        final Set<CampBlueprint.Spot> ownedContainers;
        final List<Integer> vassalColors;
        final Set<String> skippedZones;
        final Set<Long> skippedColumns;
        final long seed;
        final Runnable onComplete;
        int index;
        long lastProgressTick;
        boolean stalled;

        BuildJob(UUID packId, ResourceKey<Level> dimension, BlockPos origin, int siteFacing, Family family, int packColor,
                 String packTitle, int fromTier, int toTier, Mode mode, boolean automatic, boolean protectPlayerBlocks,
                 List<CampBlueprint.Placement> ops, Set<CampBlueprint.Spot> ownedPositions,
                 Set<CampBlueprint.Spot> ownedContainers, List<Integer> vassalColors,
                 Set<String> skippedZones, Set<Long> skippedColumns, long startTick, Runnable onComplete) {
            this.packId = packId;
            this.dimension = dimension;
            this.origin = origin;
            this.siteFacing = siteFacing;
            this.family = family;
            this.packColor = packColor;
            this.packTitle = packTitle;
            this.fromTier = fromTier;
            this.toTier = toTier;
            this.mode = mode;
            this.automatic = automatic;
            this.protectPlayerBlocks = protectPlayerBlocks;
            this.ops = ops;
            this.ownedPositions = ownedPositions;
            this.ownedContainers = ownedContainers;
            this.vassalColors = vassalColors == null ? List.of() : List.copyOf(vassalColors);
            this.skippedZones = skippedZones == null ? Set.of() : Set.copyOf(skippedZones);
            this.skippedColumns = skippedColumns == null ? Set.of() : Set.copyOf(skippedColumns);
            this.seed = packId.getMostSignificantBits() ^ packId.getLeastSignificantBits();
            this.lastProgressTick = startTick;
            this.onComplete = onComplete;
        }
    }

    private final Map<UUID, BuildJob> jobs = new LinkedHashMap<>();

    public boolean isBuilding(UUID packId) {
        return jobs.containsKey(packId);
    }

    /** Cancels all in-flight automatic build/ruin/repair jobs (called when {@code /bloodfeud camps off} disables auto-camps). */
    public void cancelAutomatic() {
        jobs.values().removeIf(job -> job.automatic);
    }

    /** Clears every queued build job (called on server stop or {@code /bloodfeud forget everything}). */
    public void clearAll() {
        jobs.clear();
    }

    /**
     * Flushes pending re-bannering on loaded settlements, then checks nearby packs and abandoned sites and schedules
     * automatic builds, upgrades, in-place ruinations, and restorations when {@code Config.buildCamps} is enabled
     * and a player comes within 144 blocks.
     */
    public void checkAutoBuilds(MinecraftServer server, FeudData data, long gameTime) {
        Feud feud = data.feud;

        // Always flush pending faction re-bannering once a built settlement's footprint loads
        boolean dirtiedData = false;
        for (Pack pack : feud.packs.values()) {
            if (!pack.bannersDirty || !pack.siteSet || pack.builtTier <= 0 || jobs.containsKey(pack.id)) continue;
            ServerLevel level = resolveLevel(server, pack.dimension);
            if (level == null) continue;
            if (rebanner(level, feud, pack)) {
                pack.bannersDirty = false;
                dirtiedData = true;
            }
        }
        if (dirtiedData) data.changed();

        if (!BloodFeud.config().buildCamps) return;

        int autoCount = 0;
        for (BuildJob j : jobs.values()) {
            if (j.automatic && !j.stalled) autoCount++;
        }
        if (autoCount >= MAX_CONCURRENT_AUTO) return;

        for (Pack pack : feud.packs.values()) {
            if (autoCount >= MAX_CONCURRENT_AUTO) break;
            if (jobs.containsKey(pack.id)) continue;
            Pack.AutoBuildAction action = autoActionFor(pack);
            if (action == Pack.AutoBuildAction.NONE) continue;
            if (pack.siteRetryAt > gameTime) continue;

            SettlementTier target = pack.tier == null ? SettlementTier.CAMP : pack.tier;
            if (!playerNearby(server, pack.dimension, pack.campX(), pack.campZ())) continue;
            ServerLevel level = resolveLevel(server, pack.dimension);
            if (level == null) continue;

            int checkTier = (action == Pack.AutoBuildAction.RUIN && pack.builtTier > 0)
                    ? pack.builtTier
                    : Math.max(pack.builtTier, target.level);
            int radius = SettlementTier.byLevel(checkTier).radius;
            if (!pack.siteSet) {
                SiteFinder.Site site = SiteFinder.findAutoSite(level, pack, target);
                if (site == null) {
                    pack.siteRetryAt = gameTime + RETRY_COOLDOWN_TICKS;
                    continue;
                }
                pack.siteSet = true;
                pack.siteX = site.origin().getX();
                pack.siteY = site.origin().getY();
                pack.siteZ = site.origin().getZ();
                pack.siteFacing = site.facing();
                pack.buildStarted = false;
                pack.skippedZones.clear();
                pack.skippedColumns.clear();
                data.changed();
            } else {
                if (!SiteFinder.isFootprintLoaded(level, pack.siteX, pack.siteZ, radius)) {
                    continue;
                }
                // Re-validate resumed initial builds (siteSet == true && builtTier == 0) before placing blocks
                if (pack.builtTier == 0) {
                    BlockPos origin = new BlockPos(pack.siteX, pack.siteY, pack.siteZ);
                    Set<CampBlueprint.Spot> resumeOwned = Set.of();
                    Set<CampBlueprint.Spot> resumeContainers = Set.of();
                    if (pack.buildStarted) {
                        int displayColor = feud.top(pack).color;
                        List<Integer> vassals = vassalColors(feud, pack);
                        resumeOwned = pack.ruined
                                ? CampBlueprint.ownedRuinedPositions(pack.id, pack.family, displayColor, vassals,
                                        pack.siteFacing, target.level, pack.skippedZones, pack.skippedColumns)
                                : CampBlueprint.ownedPositions(pack.id, pack.family, displayColor, vassals,
                                        pack.siteFacing, target.level, pack.skippedZones, pack.skippedColumns);
                        resumeContainers = CampBlueprint.containerPositions(pack.id, pack.family, displayColor, vassals,
                                pack.siteFacing, target.level, pack.skippedZones, pack.skippedColumns);
                    }
                    if (SiteFinder.hasPlayerBuild(level, origin, radius, resumeOwned, resumeContainers)) {
                        pack.siteRetryAt = gameTime + RETRY_COOLDOWN_TICKS;
                        if (!pack.buildStarted) {
                            pack.siteSet = false;
                            pack.skippedZones.clear();
                            pack.skippedColumns.clear();
                            data.changed();
                        }
                        continue;
                    }
                }
            }

            QueueResult res = switch (action) {
                case RUIN -> queueRuin(level, feud, pack, pack.builtTier > 0 ? pack.builtTier : target.level, true, gameTime, null);
                case REPAIR -> queueRepair(level, feud, pack, Math.max(pack.builtTier, target.level), true, gameTime, null);
                case BUILD -> queueBuild(level, feud, pack, pack.builtTier, target.level, true, gameTime, null);
                case NONE -> new QueueResult(false, List.of(), 0);
            };
            if (res.queued()) {
                autoCount++;
                if (!res.skippedZones().isEmpty() || res.skippedRingColumns() > 0) {
                    data.changed();
                }
            } else {
                pack.siteRetryAt = gameTime + RETRY_COOLDOWN_TICKS;
                if (pack.builtTier == 0 && !pack.buildStarted) {
                    pack.siteSet = false;
                    pack.skippedZones.clear();
                    pack.skippedColumns.clear();
                    data.changed();
                }
            }
        }

        // Crumble intact settlements left behind by disbanded packs once a player approaches
        for (Feud.AbandonedSite site : new ArrayList<>(feud.abandonedSites)) {
            if (autoCount >= MAX_CONCURRENT_AUTO) break;
            if (jobs.containsKey(site.packId())) continue;
            if (!playerNearby(server, site.dimension(), site.siteX(), site.siteZ())) continue;
            ServerLevel level = resolveLevel(server, site.dimension());
            if (level == null) continue;
            SettlementTier built = SettlementTier.byLevel(site.builtTier());
            if (!SiteFinder.isFootprintLoaded(level, site.siteX(), site.siteZ(), built.radius)) continue;

            CampBlueprint.Plan diff = CampBlueprint.ruinDiffPlan(site.packId(), site.family(), site.color(),
                    site.vassalColors(), site.siteFacing(), site.builtTier());
            diff = CampBlueprint.withoutSkipped(diff, site.skippedZones(), site.skippedColumns());
            Set<CampBlueprint.Spot> ownedPositions = CampBlueprint.ownedPositions(
                    site.packId(), site.family(), site.color(), site.vassalColors(), site.siteFacing(),
                    site.builtTier(), site.skippedZones(), site.skippedColumns());
            Set<CampBlueprint.Spot> ownedContainers = CampBlueprint.containerPositions(
                    site.packId(), site.family(), site.color(), site.vassalColors(), site.siteFacing(),
                    site.builtTier(), site.skippedZones(), site.skippedColumns());
            BlockPos origin = new BlockPos(site.siteX(), site.siteY(), site.siteZ());
            FilterResult filtered = filterPlan(level, origin, diff, null, ownedPositions, ownedContainers);
            if (filtered.ops().isEmpty()) {
                if (feud.abandonedSites.removeIf(s -> s.packId().equals(site.packId()))) {
                    data.changed();
                }
                continue;
            }
            BuildJob job = new BuildJob(site.packId(), level.dimension(), origin, site.siteFacing(),
                    site.family(), site.color(), "", site.builtTier(), site.builtTier(),
                    Mode.ABANDONED_RUIN, true, true, filtered.ops(), ownedPositions, ownedContainers,
                    site.vassalColors(), site.skippedZones(), site.skippedColumns(), gameTime, null);
            jobs.put(site.packId(), job);
            autoCount++;
        }
    }

    /**
     * Queues an incremental intact build/upgrade for a pack from {@code fromTier} to {@code toTier}.
     * If the existing built settlement is currently ruined, routes through {@link #queueRepair} so ruins are
     * restored cleanly as part of the build.
     */
    public QueueResult queueBuild(ServerLevel level, Feud feud, Pack pack, int fromTier, int toTier,
                                  boolean automatic, long gameTime, Runnable onComplete) {
        if (!pack.siteSet) {
            return new QueueResult(false, List.of(), 0);
        }
        int from = Math.max(0, Math.min(SettlementTier.TOWN.level, fromTier));
        int to = Math.max(from, Math.min(SettlementTier.TOWN.level, toTier));
        if (pack.builtRuined && pack.builtTier > 0) {
            return queueRepair(level, feud, pack, Math.max(pack.builtTier, to), automatic, gameTime, onComplete);
        }
        if (to <= from) {
            return new QueueResult(false, List.of(), 0);
        }
        BlockPos origin = new BlockPos(pack.siteX, pack.siteY, pack.siteZ);
        int displayColor = feud.top(pack).color;
        List<Integer> vassalColors = vassalColors(feud, pack);
        CampBlueprint.Plan plan = CampBlueprint.plan(pack.id, pack.family, displayColor, vassalColors,
                pack.siteFacing, from, to);
        plan = CampBlueprint.withoutSkipped(plan, pack.skippedZones, pack.skippedColumns);

        int ownedTier = (from == 0 && pack.buildStarted) ? to : from;
        Set<CampBlueprint.Spot> ownedPositions = CampBlueprint.ownedPositions(
                pack.id, pack.family, displayColor, vassalColors, pack.siteFacing, ownedTier,
                pack.skippedZones, pack.skippedColumns);
        Set<CampBlueprint.Spot> ownedContainers = CampBlueprint.containerPositions(
                pack.id, pack.family, displayColor, vassalColors, pack.siteFacing, ownedTier,
                pack.skippedZones, pack.skippedColumns);

        boolean shouldProtect = automatic || from > 0 || pack.buildStarted;
        FilterResult filtered = shouldProtect
                ? filterPlan(level, origin, plan, from > 0 ? Set.of() : null, ownedPositions, ownedContainers)
                : new FilterResult(plan.placements(), Set.of(), Set.of(), List.of(), 0);

        if (automatic && from == 0 && filtered.blockedZones().contains("camp_core")) {
            return new QueueResult(false, filtered.skippedZones(), filtered.skippedRingColumns());
        }
        if (filtered.ops().isEmpty()) {
            return new QueueResult(false, filtered.skippedZones(), filtered.skippedRingColumns());
        }

        pack.skippedZones.addAll(filtered.blockedZones());
        pack.skippedColumns.addAll(filtered.blockedRingCols());

        BuildJob job = new BuildJob(pack.id, level.dimension(), origin, pack.siteFacing, pack.family, displayColor,
                pack.title(), from, to, Mode.BUILD, automatic, shouldProtect, filtered.ops(),
                ownedPositions, ownedContainers, vassalColors, pack.skippedZones, pack.skippedColumns,
                gameTime, onComplete);
        jobs.put(pack.id, job);
        return new QueueResult(true, filtered.skippedZones(), filtered.skippedRingColumns());
    }

    /**
     * Queues either an in-place ruination diff (if the settlement is already built at {@code pack.builtTier})
     * or a fresh ruined settlement build from scratch up to {@code targetTier}.
     */
    public QueueResult queueRuin(ServerLevel level, Feud feud, Pack pack, int targetTier,
                                 boolean automatic, long gameTime, Runnable onComplete) {
        if (!pack.siteSet) {
            return new QueueResult(false, List.of(), 0);
        }
        int to = Math.max(1, Math.min(SettlementTier.TOWN.level, targetTier));
        BlockPos origin = new BlockPos(pack.siteX, pack.siteY, pack.siteZ);
        int displayColor = feud.top(pack).color;
        List<Integer> vassalColors = vassalColors(feud, pack);
        boolean inPlace = pack.builtTier > 0 && to <= pack.builtTier;
        CampBlueprint.Plan plan = inPlace
                ? CampBlueprint.ruinDiffPlan(pack.id, pack.family, displayColor, vassalColors, pack.siteFacing, pack.builtTier)
                : CampBlueprint.ruinPlan(pack.id, pack.family, displayColor, vassalColors, pack.siteFacing, to);
        if (inPlace || pack.buildStarted) {
            plan = CampBlueprint.withoutSkipped(plan, pack.skippedZones, pack.skippedColumns);
        }
        int finalTier = inPlace ? pack.builtTier : to;

        Set<CampBlueprint.Spot> ownedPositions = inPlace
                ? CampBlueprint.ownedPositions(pack.id, pack.family, displayColor, vassalColors,
                        pack.siteFacing, pack.builtTier, pack.skippedZones, pack.skippedColumns)
                : (pack.buildStarted
                        ? CampBlueprint.ownedRuinedPositions(pack.id, pack.family, displayColor, vassalColors,
                                pack.siteFacing, to, pack.skippedZones, pack.skippedColumns)
                        : Set.of());
        Set<CampBlueprint.Spot> ownedContainers = inPlace
                ? CampBlueprint.containerPositions(pack.id, pack.family, displayColor, vassalColors,
                        pack.siteFacing, pack.builtTier, pack.skippedZones, pack.skippedColumns)
                : (pack.buildStarted
                        ? CampBlueprint.containerPositions(pack.id, pack.family, displayColor, vassalColors,
                                pack.siteFacing, to, pack.skippedZones, pack.skippedColumns)
                        : Set.of());

        List<CampBlueprint.Placement> ops = plan.placements();
        List<String> skippedZones = List.of();
        int skippedRingColumns = 0;
        boolean shouldProtect = inPlace || automatic || pack.buildStarted;
        if (shouldProtect) {
            FilterResult filtered = filterPlan(level, origin, plan, null, ownedPositions, ownedContainers);
            if ((!inPlace && automatic && filtered.blockedZones().contains("camp_core")) || filtered.ops().isEmpty()) {
                return new QueueResult(false, filtered.skippedZones(), filtered.skippedRingColumns());
            }
            ops = filtered.ops();
            skippedZones = filtered.skippedZones();
            skippedRingColumns = filtered.skippedRingColumns();
            if (!inPlace) {
                pack.skippedZones.addAll(filtered.blockedZones());
                pack.skippedColumns.addAll(filtered.blockedRingCols());
            }
        }
        if (ops.isEmpty()) {
            return new QueueResult(false, skippedZones, skippedRingColumns);
        }

        BuildJob job = new BuildJob(pack.id, level.dimension(), origin, pack.siteFacing, pack.family, displayColor,
                pack.title(), pack.builtTier, finalTier, Mode.RUIN, automatic, shouldProtect, ops,
                ownedPositions, ownedContainers, vassalColors, pack.skippedZones, pack.skippedColumns,
                gameTime, onComplete);
        jobs.put(pack.id, job);
        return new QueueResult(true, skippedZones, skippedRingColumns);
    }

    /** Queues restoration of a ruined built settlement back to intact {@code toTier} (clearing ruin debris). */
    public QueueResult queueRepair(ServerLevel level, Feud feud, Pack pack, int toTier,
                                   boolean automatic, long gameTime, Runnable onComplete) {
        if (!pack.siteSet) {
            return new QueueResult(false, List.of(), 0);
        }
        int built = Math.max(1, Math.min(SettlementTier.TOWN.level, pack.builtTier));
        int to = Math.max(built, Math.min(SettlementTier.TOWN.level, toTier));
        BlockPos origin = new BlockPos(pack.siteX, pack.siteY, pack.siteZ);
        int displayColor = feud.top(pack).color;
        List<Integer> vassalColors = vassalColors(feud, pack);
        CampBlueprint.Plan plan = CampBlueprint.repairPlan(pack.id, pack.family, displayColor, vassalColors,
                pack.siteFacing, built, to);
        plan = CampBlueprint.withoutSkipped(plan, pack.skippedZones, pack.skippedColumns);

        Set<CampBlueprint.Spot> ownedPositions = CampBlueprint.ownedRuinedPositions(
                pack.id, pack.family, displayColor, vassalColors, pack.siteFacing, built,
                pack.skippedZones, pack.skippedColumns);
        Set<CampBlueprint.Spot> ownedContainers = CampBlueprint.containerPositions(
                pack.id, pack.family, displayColor, vassalColors, pack.siteFacing, built,
                pack.skippedZones, pack.skippedColumns);

        Set<String> existingZones = (to > built)
                ? CampBlueprint.plan(pack.id, pack.family, displayColor, vassalColors, pack.siteFacing, 0, built).zones().keySet()
                : null;
        FilterResult filtered = filterPlan(level, origin, plan, existingZones, ownedPositions, ownedContainers);
        if (filtered.ops().isEmpty()) {
            return new QueueResult(false, filtered.skippedZones(), filtered.skippedRingColumns());
        }

        if (to > built && existingZones != null) {
            for (String z : filtered.blockedZones()) {
                if (!existingZones.contains(z)) pack.skippedZones.add(z);
            }
            if (!filtered.blockedRingCols().isEmpty()) {
                Set<Long> newRingCols = new HashSet<>();
                for (CampBlueprint.Placement p : plan.placements()) {
                    if (CampBlueprint.isRingZone(p.zone()) && !existingZones.contains(p.zone())) {
                        newRingCols.add(colKey(p.rx(), p.rz()));
                    }
                }
                for (Long col : filtered.blockedRingCols()) {
                    if (newRingCols.contains(col)) pack.skippedColumns.add(col);
                }
            }
        }

        BuildJob job = new BuildJob(pack.id, level.dimension(), origin, pack.siteFacing, pack.family, displayColor,
                pack.title(), built, to, Mode.REPAIR, automatic, true, filtered.ops(),
                ownedPositions, ownedContainers, vassalColors, pack.skippedZones, pack.skippedColumns,
                gameTime, onComplete);
        jobs.put(pack.id, job);
        return new QueueResult(true, filtered.skippedZones(), filtered.skippedRingColumns());
    }

    /**
     * Filters player-blocked zones/ring columns (and optionally abyss-hanging new upgrade zones) out of {@code plan}.
     * When {@code abyssExcludeZones} is non-null, any non-ring zone not in {@code abyssExcludeZones} is also
     * rejected if it sits over lava or lacks solid ground within {@link #MAX_FOUNDATION_DEPTH}.
     */
    private static FilterResult filterPlan(ServerLevel level, BlockPos origin, CampBlueprint.Plan plan,
                                           Set<String> abyssExcludeZones,
                                           Set<CampBlueprint.Spot> ownedPositions,
                                           Set<CampBlueprint.Spot> ownedContainers) {
        List<CampBlueprint.Placement> ops = plan.placements();
        Set<String> blockedZones = new HashSet<>();
        List<String> skippedZones = new ArrayList<>();
        int skippedRingColumns = 0;

        for (Map.Entry<String, CampBlueprint.Box> entry : plan.zones().entrySet()) {
            String zone = entry.getKey();
            if (CampBlueprint.isRingZone(zone)) continue;
            CampBlueprint.Box box = entry.getValue();
            if (SiteFinder.isZoneBlockedByPlayer(level, origin, box, ownedPositions, ownedContainers)) {
                blockedZones.add(zone);
                skippedZones.add(zone);
            } else if (abyssExcludeZones != null && !abyssExcludeZones.contains(zone)
                    && SiteFinder.isZoneOverAbyss(level, origin, box, MAX_FOUNDATION_DEPTH)) {
                blockedZones.add(zone);
                skippedZones.add(zone + " (ravine/fluid)");
            }
        }

        // Per-column filtering (1-block margin) for large ring zones so one player block never drops a whole wall/street ring
        Map<Long, int[]> ringColBounds = new HashMap<>();
        for (CampBlueprint.Placement p : ops) {
            if (!CampBlueprint.isRingZone(p.zone())) continue;
            long ck = colKey(p.rx(), p.rz());
            int[] yr = ringColBounds.get(ck);
            if (yr == null) {
                ringColBounds.put(ck, new int[]{p.rx(), p.rz(), p.ry(), p.ry()});
            } else {
                yr[2] = Math.min(yr[2], p.ry());
                yr[3] = Math.max(yr[3], p.ry());
            }
        }

        Set<Long> blockedRingCols = new HashSet<>();
        if (!ringColBounds.isEmpty()) {
            Map<Long, Boolean> colPlayerCache = new HashMap<>();
            for (Map.Entry<Long, int[]> e : ringColBounds.entrySet()) {
                int[] info = e.getValue();
                int rx = info[0], rz = info[1], minY = info[2], maxY = info[3];
                boolean blocked = false;
                for (int dx = -1; dx <= 1 && !blocked; dx++) {
                    for (int dz = -1; dz <= 1 && !blocked; dz++) {
                        int nx = rx + dx, nz = rz + dz;
                        long nk = colKey(nx, nz);
                        Boolean hit = colPlayerCache.get(nk);
                        if (hit == null) {
                            hit = SiteFinder.hasPlayerBlockInColumn(level, origin, nx, nz, minY, maxY,
                                    ownedPositions, ownedContainers);
                            colPlayerCache.put(nk, hit);
                        }
                        if (hit) blocked = true;
                    }
                }
                if (blocked) blockedRingCols.add(e.getKey());
            }
            skippedRingColumns = blockedRingCols.size();
        }

        if (!blockedZones.isEmpty() || !blockedRingCols.isEmpty()) {
            List<CampBlueprint.Placement> filtered = new ArrayList<>(ops.size());
            for (CampBlueprint.Placement p : ops) {
                if (blockedZones.contains(p.zone())) continue;
                if (CampBlueprint.isRingZone(p.zone()) && blockedRingCols.contains(colKey(p.rx(), p.rz()))) continue;
                filtered.add(p);
            }
            ops = filtered;
        }
        return new FilterResult(ops, blockedZones, blockedRingCols, List.copyOf(skippedZones), skippedRingColumns);
    }

    /** Executes up to ~250 block operations per tick across queued build/ruin/repair jobs. */
    public void tick(MinecraftServer server, FeudData data, long gameTime) {
        if (jobs.isEmpty()) return;
        int budget = OPS_PER_TICK;
        List<Runnable> completedCallbacks = null;

        for (Iterator<Map.Entry<UUID, BuildJob>> it = jobs.entrySet().iterator(); it.hasNext() && budget > 0; ) {
            BuildJob job = it.next().getValue();
            if (job.automatic && !BloodFeud.config().buildCamps) {
                it.remove();
                continue;
            }
            Pack pack = null;
            if (job.mode != Mode.ABANDONED_RUIN) {
                pack = data.feud.pack(job.packId);
                if (pack == null) {
                    it.remove();
                    if (job.index > 0 && job.mode != Mode.RUIN) {
                        int coverTier = Math.max(job.fromTier, job.toTier);
                        if (coverTier > 0) {
                            data.feud.recordAbandonedSite(new Feud.AbandonedSite(
                                    job.packId, job.family, job.packColor, job.dimension.location().toString(),
                                    job.origin.getX(), job.origin.getY(), job.origin.getZ(),
                                    job.siteFacing, coverTier,
                                    job.vassalColors, job.skippedZones, job.skippedColumns));
                            data.changed();
                        }
                    }
                    continue;
                }
            }
            ServerLevel level = server.getLevel(job.dimension);
            if (level == null) {
                it.remove();
                continue;
            }
            FamilyPalette palette = FamilyPalette.of(job.family);
            boolean stalled = false;
            int opsDone = 0;
            while (budget > 0 && job.index < job.ops.size()) {
                CampBlueprint.Placement op = job.ops.get(job.index);
                BlockPos pos = job.origin.offset(op.rx(), op.ry(), op.rz());
                if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                    stalled = true;
                    break;
                }
                applyOp(level, pos, op, palette, job);
                job.index++;
                budget--;
                opsDone++;
            }
            if (opsDone > 0) {
                job.lastProgressTick = gameTime;
                if (pack != null && pack.builtTier == 0 && !pack.buildStarted) {
                    pack.buildStarted = true;
                    data.changed();
                }
            }
            job.stalled = stalled;
            if (stalled) {
                if (job.automatic && gameTime - job.lastProgressTick >= STALL_TIMEOUT_TICKS) {
                    it.remove();
                    if (pack != null && pack.builtTier == 0) {
                        pack.siteRetryAt = gameTime + RETRY_COOLDOWN_TICKS;
                    }
                }
                continue;
            }

            if (job.index >= job.ops.size()) {
                it.remove();
                if (job.mode == Mode.ABANDONED_RUIN) {
                    if (data.feud.abandonedSites.removeIf(s -> s.packId().equals(job.packId))) {
                        data.changed();
                    }
                } else if (pack != null) {
                    pack.builtTier = Math.max(pack.builtTier, job.toTier);
                    pack.builtRuined = (job.mode == Mode.RUIN);
                    if (pack.builtTier > 0) {
                        pack.buildStarted = false;
                    }
                    data.changed();
                    if (job.onComplete != null) {
                        if (completedCallbacks == null) completedCallbacks = new ArrayList<>();
                        completedCallbacks.add(job.onComplete);
                    }
                }
            }
        }

        if (completedCallbacks != null) {
            for (Runnable cb : completedCallbacks) cb.run();
        }
    }

    private static void applyOp(ServerLevel level, BlockPos pos, CampBlueprint.Placement op,
                                FamilyPalette palette, BuildJob job) {
        if (pos.getY() <= level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight() - 1) return;

        if (op.material() == CampBlueprint.Material.FOUNDATION_COLUMN) {
            BlockState fill = palette.foundation.defaultBlockState();
            BlockPos.MutableBlockPos cur = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
            for (int depth = 0; depth < MAX_FOUNDATION_DEPTH; depth++) {
                if (cur.getY() <= level.getMinBuildHeight() + 1) break;
                BlockState existing = level.getBlockState(cur);
                if (existing.blocksMotion() && existing.getFluidState().isEmpty() && !existing.canBeReplaced()) {
                    break;
                }
                if (SiteFinder.isIndestructibleBlock(existing) || SiteFinder.isNonEmptyContainer(level, cur, existing)) {
                    break;
                }
                if (job.protectPlayerBlocks && SiteFinder.isPlayerBlockAt(level, job.origin, cur, job.ownedPositions, job.ownedContainers)) {
                    break;
                }
                level.setBlock(cur, fill, Block.UPDATE_CLIENTS);
                cur.move(0, -1, 0);
            }
            return;
        }

        BlockState existing = level.getBlockState(pos);
        if (SiteFinder.isIndestructibleBlock(existing)) return;
        // NEVER overwrite any non-empty container (even on manual operator builds)
        if (SiteFinder.isNonEmptyContainer(level, pos, existing)) return;
        if (job.protectPlayerBlocks && SiteFinder.isPlayerBlockAt(level, job.origin, pos, job.ownedPositions, job.ownedContainers)) {
            return;
        }

        boolean wasChestOrBarrel = existing.getBlock() instanceof AbstractChestBlock<?>
                || existing.getBlock() instanceof BarrelBlock;

        // Preserve existing containers on repair/rebuild so loot is never destroyed or infinitely refilled
        if (op.material().isChest() && existing.getBlock() instanceof AbstractChestBlock<?>) return;
        if (op.material() == CampBlueprint.Material.BARREL && existing.getBlock() instanceof BarrelBlock) return;

        BlockState target = palette.resolve(op, job.packColor);
        if (op.material() == CampBlueprint.Material.AIR) {
            // Never destroy containers during teardown/clearing
            if (wasChestOrBarrel || SiteFinder.isContainerBlock(level, pos, existing)) return;
            if (!existing.isAir()) level.setBlock(pos, target, Block.UPDATE_CLIENTS);
            return;
        }

        if (op.material().pass == CampBlueprint.PASS_DECOR && !target.canSurvive(level, pos)) return;
        level.setBlock(pos, target, Block.UPDATE_ALL);

        if (op.material().isChest() && !wasChestOrBarrel) {
            SettlementTier lootTier = CampLoot.tierForChest(op.material());
            boolean shouldFill = job.mode != Mode.REPAIR || lootTier.level > job.fromTier;
            if (shouldFill) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof Container container && container.isEmpty()) {
                    long posSeed = job.seed ^ (((long) pos.getX() * 341873128712L) ^ ((long) pos.getZ() * 132897987541L));
                    CampLoot.fill(container, job.family, lootTier, posSeed);
                    be.applyComponents(be.components(), DataComponentPatch.builder()
                            .set(DataComponents.CUSTOM_NAME, CampLoot.chestTitle(job.packTitle, lootTier))
                            .build());
                    be.setChanged();
                }
            }
        } else if (op.material() == CampBlueprint.Material.BARREL && !wasChestOrBarrel) {
            if (job.mode != Mode.REPAIR) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof Container container && container.isEmpty()) {
                    long posSeed = job.seed ^ (((long) pos.getX() * 9187L) ^ ((long) pos.getZ() * 7919L));
                    CampLoot.fillBarrel(container, job.family, posSeed);
                }
            }
        }
    }

    /**
     * Re-banners an already built settlement (when a pack is swallowed by a King, freed on King death,
     * or a King gains/loses vassals). Returns {@code true} if the footprint was loaded and banners were updated.
     */
    public boolean rebanner(ServerLevel level, Feud feud, Pack pack) {
        if (level == null || pack == null || !pack.siteSet || pack.builtTier <= 0) return false;
        SettlementTier built = SettlementTier.byLevel(pack.builtTier);
        if (!SiteFinder.isFootprintLoaded(level, pack.siteX, pack.siteZ, built.radius)) return false;

        Pack top = feud.top(pack);
        int displayColor = top.color;
        List<Integer> vassals = vassalColors(feud, pack);
        List<CampBlueprint.Placement> banners = CampBlueprint.bannerPlacements(
                pack.id, pack.family, displayColor, vassals, pack.siteFacing, built);
        FamilyPalette palette = FamilyPalette.of(pack.family);
        BlockPos origin = new BlockPos(pack.siteX, pack.siteY, pack.siteZ);

        for (CampBlueprint.Placement op : banners) {
            if (CampBlueprint.isSkipped(op.zone(), op.rx(), op.rz(), pack.skippedZones, pack.skippedColumns)) {
                continue;
            }
            BlockPos pos = origin.offset(op.rx(), op.ry(), op.rz());
            if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
            BlockState existing = level.getBlockState(pos);
            Block block = existing.getBlock();
            boolean replaceable = block instanceof BannerBlock || block instanceof WallBannerBlock
                    || (!pack.builtRuined && op.material() == CampBlueprint.Material.VASSAL_BANNER && existing.isAir());
            if (!replaceable) continue;
            if (!pack.builtRuined && op.material() == CampBlueprint.Material.VASSAL_BANNER && existing.isAir()) {
                BlockPos below = pos.below();
                if (level.getBlockState(below).isAir()) {
                    level.setBlock(below, palette.fence.defaultBlockState(), Block.UPDATE_ALL);
                }
                BlockPos base = below.below();
                if (level.getBlockState(base).isAir()) {
                    level.setBlock(base, palette.pillar.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            level.setBlock(pos, palette.resolve(op, displayColor), Block.UPDATE_ALL);
        }

        // Clear stale vassal banners (and matching FENCE/PILLAR posts) beyond current vassals.size()
        if (built == SettlementTier.TOWN) {
            List<CampBlueprint.Spot> allVassalSpots = CampBlueprint.vassalBannerSpots(pack.siteFacing);
            for (int i = vassals.size(); i < allVassalSpots.size(); i++) {
                CampBlueprint.Spot s = allVassalSpots.get(i);
                if (CampBlueprint.isSkipped("town_kings_hall", s.rx(), s.rz(), pack.skippedZones, pack.skippedColumns)) {
                    continue;
                }
                BlockPos pos = origin.offset(s.rx(), s.ry(), s.rz());
                if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
                boolean clearedBanner = false;
                if (level.getBlockState(pos).getBlock() instanceof BannerBlock) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    clearedBanner = true;
                }
                BlockPos below = pos.below();
                BlockPos base = below.below();
                boolean fenceMatches = level.getBlockState(below).is(palette.fence);
                boolean pillarMatches = level.getBlockState(base).is(palette.pillar);
                if (clearedBanner || (level.getBlockState(pos).isAir() && (fenceMatches || pillarMatches))) {
                    if (fenceMatches) {
                        level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                    if (pillarMatches && (fenceMatches || level.getBlockState(below).isAir())) {
                        level.setBlock(base, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
        return true;
    }

    private static long colKey(int rx, int rz) {
        return CampBlueprint.colKey(rx, rz);
    }

    private static List<Integer> vassalColors(Feud feud, Pack pack) {
        List<Integer> colors = new ArrayList<>();
        for (Pack v : feud.vassals(pack)) colors.add(v.color);
        return colors;
    }

    private static boolean playerNearby(MinecraftServer server, String dimension, int x, int z) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator()) continue;
            if (!p.level().dimension().location().toString().equals(dimension)) continue;
            if (Math.hypot(p.getX() - x, p.getZ() - z) <= AUTO_TRIGGER_RANGE) {
                return true;
            }
        }
        return false;
    }

    private static ServerLevel resolveLevel(MinecraftServer server, String dimension) {
        try {
            return server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension)));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
