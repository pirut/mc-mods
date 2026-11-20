package io.github.pirut.bloodfeud.game.camp;

import io.github.pirut.bloodfeud.BloodFeud;
import io.github.pirut.bloodfeud.feud.Family;
import io.github.pirut.bloodfeud.feud.Feud;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.SettlementTier;
import io.github.pirut.bloodfeud.game.FeudData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Chunk-safe incremental builder that places, ruins, and repairs gang camps, outposts, strongholds and towns
 * over multiple ticks (~250 block ops/tick) without lag or chunk force-loading.
 */
public final class CampBuilder {
    private static final int OPS_PER_TICK = 250;
    private static final int AUTO_TRIGGER_RANGE = 144;
    private static final int MAX_CONCURRENT_AUTO = 3;
    private static final long RETRY_COOLDOWN_TICKS = 2400L;
    private static final int MAX_FOUNDATION_DEPTH = 10;

    private enum Mode { BUILD, RUIN, REPAIR, ABANDONED_RUIN }

    private static final class BuildJob {
        final UUID packId;
        final ResourceKey<Level> dimension;
        final BlockPos origin;
        final Family family;
        final int packColor;
        final int fromTier;
        final int toTier;
        final Mode mode;
        final boolean automatic;
        final List<CampBlueprint.Placement> ops;
        final long seed;
        final Runnable onComplete;
        int index;

        BuildJob(UUID packId, ResourceKey<Level> dimension, BlockPos origin, Family family, int packColor,
                 int fromTier, int toTier, Mode mode, boolean automatic,
                 List<CampBlueprint.Placement> ops, Runnable onComplete) {
            this.packId = packId;
            this.dimension = dimension;
            this.origin = origin;
            this.family = family;
            this.packColor = packColor;
            this.fromTier = fromTier;
            this.toTier = toTier;
            this.mode = mode;
            this.automatic = automatic;
            this.ops = ops;
            this.seed = packId.getMostSignificantBits() ^ packId.getLeastSignificantBits();
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
     * Checks nearby packs and abandoned sites and schedules automatic builds, upgrades, in-place ruinations,
     * and restorations when {@code Config.buildCamps} is enabled and a player comes within 144 blocks.
     */
    public void checkAutoBuilds(MinecraftServer server, FeudData data, long gameTime) {
        if (!BloodFeud.config().buildCamps) return;
        Feud feud = data.feud;
        int autoCount = 0;
        for (BuildJob j : jobs.values()) if (j.automatic) autoCount++;
        if (autoCount >= MAX_CONCURRENT_AUTO) return;

        for (Pack pack : feud.packs.values()) {
            if (autoCount >= MAX_CONCURRENT_AUTO) break;
            if (jobs.containsKey(pack.id)) continue;
            SettlementTier target = pack.tier == null ? SettlementTier.CAMP : pack.tier;
            if (target == SettlementTier.NONE) continue;

            boolean needsTierBuild = pack.builtTier < target.level;
            boolean needsRuin = pack.ruined && !pack.builtRuined;
            boolean needsRepair = !pack.ruined && pack.builtRuined && pack.builtTier > 0;
            if (!needsTierBuild && !needsRuin && !needsRepair) continue;
            if (pack.siteRetryAt > gameTime) continue;

            if (!playerNearby(server, pack.dimension, pack.campX(), pack.campZ())) continue;
            ServerLevel level = resolveLevel(server, pack.dimension);
            if (level == null) continue;

            int radius = SettlementTier.byLevel(Math.max(pack.builtTier, target.level)).radius;
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
                data.changed();
            } else if (!SiteFinder.isFootprintLoaded(level, pack.siteX, pack.siteZ, radius)) {
                continue;
            }

            if (pack.ruined) {
                queueRuin(level, feud, pack, pack.builtTier > 0 ? pack.builtTier : target.level, true, null);
            } else if (pack.builtRuined && pack.builtTier > 0) {
                queueRepair(level, feud, pack, Math.max(pack.builtTier, target.level), true, null);
            } else {
                queueBuild(level, feud, pack, pack.builtTier, target.level, true, null);
            }
            autoCount++;
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
                    List.of(), site.siteFacing(), site.builtTier());
            BlockPos origin = new BlockPos(site.siteX(), site.siteY(), site.siteZ());
            BuildJob job = new BuildJob(site.packId(), level.dimension(), origin, site.family(), site.color(),
                    site.builtTier(), site.builtTier(), Mode.ABANDONED_RUIN, true, diff.placements(), null);
            jobs.put(site.packId(), job);
            autoCount++;
        }
    }

    /**
     * Queues an incremental intact build/upgrade for a pack from {@code fromTier} to {@code toTier}.
     * If the existing built settlement is currently ruined, routes through {@link #queueRepair} so ruins are
     * restored cleanly as part of the build.
     */
    public void queueBuild(ServerLevel level, Feud feud, Pack pack, int fromTier, int toTier,
                           boolean automatic, Runnable onComplete) {
        if (!pack.siteSet) {
            if (onComplete != null) onComplete.run();
            return;
        }
        int from = Math.max(0, Math.min(SettlementTier.TOWN.level, fromTier));
        int to = Math.max(from, Math.min(SettlementTier.TOWN.level, toTier));
        if (pack.builtRuined && pack.builtTier > 0) {
            queueRepair(level, feud, pack, Math.max(pack.builtTier, to), automatic, onComplete);
            return;
        }
        if (to <= from) {
            if (onComplete != null) onComplete.run();
            return;
        }
        BlockPos origin = new BlockPos(pack.siteX, pack.siteY, pack.siteZ);
        List<Integer> vassalColors = vassalColors(feud, pack);
        CampBlueprint.Plan plan = CampBlueprint.plan(pack.id, pack.family, pack.color, vassalColors,
                pack.siteFacing, from, to);

        List<CampBlueprint.Placement> ops = filterBlockedZones(level, origin, plan, from, automatic);
        BuildJob job = new BuildJob(pack.id, level.dimension(), origin, pack.family, pack.color,
                from, to, Mode.BUILD, automatic, ops, onComplete);
        jobs.put(pack.id, job);
    }

    /**
     * Queues either an in-place ruination diff (if the settlement is already built at {@code pack.builtTier})
     * or a fresh ruined settlement build from scratch up to {@code targetTier}.
     */
    public void queueRuin(ServerLevel level, Feud feud, Pack pack, int targetTier,
                          boolean automatic, Runnable onComplete) {
        if (!pack.siteSet) {
            if (onComplete != null) onComplete.run();
            return;
        }
        int to = Math.max(1, Math.min(SettlementTier.TOWN.level, targetTier));
        BlockPos origin = new BlockPos(pack.siteX, pack.siteY, pack.siteZ);
        List<Integer> vassalColors = vassalColors(feud, pack);
        boolean inPlace = pack.builtTier > 0 && to <= pack.builtTier;
        CampBlueprint.Plan plan = inPlace
                ? CampBlueprint.ruinDiffPlan(pack.id, pack.family, pack.color, vassalColors, pack.siteFacing, pack.builtTier)
                : CampBlueprint.ruinPlan(pack.id, pack.family, pack.color, vassalColors, pack.siteFacing, to);
        int finalTier = inPlace ? pack.builtTier : to;
        BuildJob job = new BuildJob(pack.id, level.dimension(), origin, pack.family, pack.color,
                pack.builtTier, finalTier, Mode.RUIN, automatic, plan.placements(), onComplete);
        jobs.put(pack.id, job);
    }

    /** Queues restoration of a ruined built settlement back to intact {@code toTier} (clearing ruin debris). */
    public void queueRepair(ServerLevel level, Feud feud, Pack pack, int toTier,
                            boolean automatic, Runnable onComplete) {
        if (!pack.siteSet) {
            if (onComplete != null) onComplete.run();
            return;
        }
        int built = Math.max(1, Math.min(SettlementTier.TOWN.level, pack.builtTier));
        int to = Math.max(built, Math.min(SettlementTier.TOWN.level, toTier));
        BlockPos origin = new BlockPos(pack.siteX, pack.siteY, pack.siteZ);
        List<Integer> vassalColors = vassalColors(feud, pack);
        CampBlueprint.Plan plan = CampBlueprint.repairPlan(pack.id, pack.family, pack.color, vassalColors,
                pack.siteFacing, built, to);
        List<CampBlueprint.Placement> ops = filterBlockedZones(level, origin, plan, built, automatic);
        BuildJob job = new BuildJob(pack.id, level.dimension(), origin, pack.family, pack.color,
                built, to, Mode.REPAIR, automatic, ops, onComplete);
        jobs.put(pack.id, job);
    }

    private static List<CampBlueprint.Placement> filterBlockedZones(ServerLevel level, BlockPos origin,
                                                                    CampBlueprint.Plan plan, int prevTier, boolean automatic) {
        List<CampBlueprint.Placement> ops = plan.placements();
        if (!automatic || prevTier <= 0) return ops;
        int prevRadius = SettlementTier.byLevel(prevTier).radius;
        Set<String> blockedZones = new HashSet<>();
        for (Map.Entry<String, CampBlueprint.Box> entry : plan.zones().entrySet()) {
            if (SiteFinder.isZoneBlockedByPlayer(level, origin, entry.getValue(), prevRadius)) {
                blockedZones.add(entry.getKey());
            }
        }
        if (blockedZones.isEmpty()) return ops;
        List<CampBlueprint.Placement> filtered = new ArrayList<>(ops.size());
        for (CampBlueprint.Placement p : ops) {
            if (!blockedZones.contains(p.zone())) filtered.add(p);
        }
        return filtered;
    }

    /** Executes up to ~250 block operations per tick across queued build/ruin/repair jobs. */
    public void tick(MinecraftServer server, FeudData data) {
        if (jobs.isEmpty()) return;
        int budget = OPS_PER_TICK;
        List<Runnable> completedCallbacks = null;

        for (Iterator<Map.Entry<UUID, BuildJob>> it = jobs.entrySet().iterator(); it.hasNext() && budget > 0; ) {
            BuildJob job = it.next().getValue();
            if (job.automatic && !BloodFeud.config().buildCamps) {
                it.remove();
                continue;
            }
            ServerLevel level = server.getLevel(job.dimension);
            if (level == null) {
                it.remove();
                continue;
            }
            FamilyPalette palette = FamilyPalette.of(job.family);
            boolean stalled = false;
            while (budget > 0 && job.index < job.ops.size()) {
                CampBlueprint.Placement op = job.ops.get(job.index);
                BlockPos pos = job.origin.offset(op.rx(), op.ry(), op.rz());
                if (!level.isLoaded(pos)) {
                    stalled = true;
                    break;
                }
                applyOp(level, pos, op, palette, job);
                job.index++;
                budget--;
            }
            if (stalled) continue;

            if (job.index >= job.ops.size()) {
                it.remove();
                if (job.mode == Mode.ABANDONED_RUIN) {
                    if (data.feud.abandonedSites.removeIf(s -> s.packId().equals(job.packId))) {
                        data.changed();
                    }
                } else {
                    Pack pack = data.feud.pack(job.packId);
                    if (pack != null) {
                        pack.builtTier = Math.max(pack.builtTier, job.toTier);
                        pack.builtRuined = (job.mode == Mode.RUIN);
                        data.changed();
                    }
                }
                if (job.onComplete != null) {
                    if (completedCallbacks == null) completedCallbacks = new ArrayList<>();
                    completedCallbacks.add(job.onComplete);
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
                if (!SiteFinder.isProtectedBlock(existing)) {
                    level.setBlock(cur, fill, Block.UPDATE_CLIENTS);
                }
                cur.move(0, -1, 0);
            }
            return;
        }

        BlockState existing = level.getBlockState(pos);
        if (SiteFinder.isProtectedBlock(existing)) return;

        // Preserve existing containers on repair/rebuild so loot is never destroyed or infinitely refilled
        if (op.material().isChest() && existing.getBlock() instanceof ChestBlock) return;
        if (op.material() == CampBlueprint.Material.BARREL && existing.getBlock() instanceof BarrelBlock) return;

        BlockState target = palette.resolve(op, job.packColor);
        if (op.material() == CampBlueprint.Material.AIR) {
            // Never destroy containers during teardown/clearing
            if (existing.getBlock() instanceof ChestBlock || existing.getBlock() instanceof BarrelBlock) return;
            if (!existing.isAir()) level.setBlock(pos, target, Block.UPDATE_CLIENTS);
            return;
        }

        if (op.material().pass == CampBlueprint.PASS_DECOR && !target.canSurvive(level, pos)) return;
        level.setBlock(pos, target, Block.UPDATE_ALL);

        if (op.material().isChest()) {
            SettlementTier lootTier = CampLoot.tierForChest(op.material());
            boolean shouldFill = job.mode != Mode.REPAIR || lootTier.level > job.fromTier;
            if (shouldFill) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof Container container) {
                    long posSeed = job.seed ^ (((long) pos.getX() * 341873128712L) ^ ((long) pos.getZ() * 132897987541L));
                    CampLoot.fill(container, job.family, lootTier, posSeed);
                }
            }
        } else if (op.material() == CampBlueprint.Material.BARREL) {
            if (job.mode != Mode.REPAIR) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof Container container) {
                    long posSeed = job.seed ^ (((long) pos.getX() * 9187L) ^ ((long) pos.getZ() * 7919L));
                    CampLoot.fillBarrel(container, job.family, posSeed);
                }
            }
        }
    }

    /**
     * Re-banners an already built settlement (for instance when a pack is swallowed by a King, or a King gains
     * a new vassal whose banner should fly in the Town's forecourt).
     */
    public void rebanner(ServerLevel level, Feud feud, Pack pack) {
        if (level == null || pack == null || !pack.siteSet || pack.builtTier <= 0) return;
        SettlementTier built = SettlementTier.byLevel(pack.builtTier);
        if (!SiteFinder.isFootprintLoaded(level, pack.siteX, pack.siteZ, built.radius)) return;

        Pack top = feud.top(pack);
        int displayColor = top.color;
        List<Integer> vassals = vassalColors(feud, pack);
        List<CampBlueprint.Placement> banners = CampBlueprint.bannerPlacements(
                pack.id, pack.family, displayColor, vassals, pack.siteFacing, built);
        FamilyPalette palette = FamilyPalette.of(pack.family);
        BlockPos origin = new BlockPos(pack.siteX, pack.siteY, pack.siteZ);

        for (CampBlueprint.Placement op : banners) {
            BlockPos pos = origin.offset(op.rx(), op.ry(), op.rz());
            if (!level.isLoaded(pos)) continue;
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
