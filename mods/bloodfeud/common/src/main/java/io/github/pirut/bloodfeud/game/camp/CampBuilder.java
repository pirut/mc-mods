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
import net.minecraft.world.level.block.Block;
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
 * Chunk-safe incremental builder that places gang camps, outposts, strongholds and towns over multiple ticks
 * (~250 block ops/tick) without lag or chunk force-loading.
 */
public final class CampBuilder {
    private static final int OPS_PER_TICK = 250;
    private static final int AUTO_TRIGGER_RANGE = 144;
    private static final int MAX_CONCURRENT_AUTO = 3;
    private static final long RETRY_COOLDOWN_TICKS = 2400L;
    private static final int MAX_FOUNDATION_DEPTH = 10;

    private static final class BuildJob {
        final UUID packId;
        final ResourceKey<Level> dimension;
        final BlockPos origin;
        final Family family;
        final int packColor;
        final String packTitle;
        final int fromTier;
        final int toTier;
        final boolean automatic;
        final List<CampBlueprint.Placement> ops;
        final long seed;
        final Runnable onComplete;
        int index;

        BuildJob(UUID packId, ResourceKey<Level> dimension, BlockPos origin, Family family, int packColor,
                 String packTitle, int fromTier, int toTier, boolean automatic,
                 List<CampBlueprint.Placement> ops, Runnable onComplete) {
            this.packId = packId;
            this.dimension = dimension;
            this.origin = origin;
            this.family = family;
            this.packColor = packColor;
            this.packTitle = packTitle;
            this.fromTier = fromTier;
            this.toTier = toTier;
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

    /** Cancels all in-flight automatic build jobs (called when {@code /bloodfeud camps off} disables auto-camps). */
    public void cancelAutomatic() {
        jobs.values().removeIf(job -> job.automatic);
    }

    /** Clears every queued build job (called on server stop or {@code /bloodfeud forget everything}). */
    public void clearAll() {
        jobs.clear();
    }

    /**
     * Checks nearby packs and schedules automatic builds/upgrades when {@code Config.buildCamps} is enabled
     * and a player comes within 144 blocks of a pack whose {@code builtTier < tier.level}.
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
            if (target == SettlementTier.NONE || pack.builtTier >= target.level) continue;
            if (pack.siteRetryAt > gameTime) continue;

            if (!playerNearby(server, pack)) continue;
            ServerLevel level = resolveLevel(server, pack.dimension);
            if (level == null) continue;

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
            } else if (!SiteFinder.isFootprintLoaded(level, pack.siteX, pack.siteZ, target.radius)) {
                continue;
            }

            queueBuild(level, feud, pack, pack.builtTier, target.level, true, null);
            autoCount++;
        }
    }

    /** Queues an incremental build/upgrade for a pack from {@code fromTier} to {@code toTier}. */
    public void queueBuild(ServerLevel level, Feud feud, Pack pack, int fromTier, int toTier,
                           boolean automatic, Runnable onComplete) {
        int from = Math.max(0, Math.min(SettlementTier.TOWN.level, fromTier));
        int to = Math.max(from, Math.min(SettlementTier.TOWN.level, toTier));
        if (to <= from || !pack.siteSet) {
            if (onComplete != null) onComplete.run();
            return;
        }
        BlockPos origin = new BlockPos(pack.siteX, pack.siteY, pack.siteZ);
        List<Integer> vassalColors = vassalColors(feud, pack);
        CampBlueprint.Plan plan = CampBlueprint.plan(pack.id, pack.family, pack.color, vassalColors,
                pack.siteFacing, from, to);

        List<CampBlueprint.Placement> ops = plan.placements();
        if (automatic && from > 0) {
            int prevRadius = SettlementTier.byLevel(from).radius;
            Set<String> blockedZones = new HashSet<>();
            for (Map.Entry<String, CampBlueprint.Box> entry : plan.zones().entrySet()) {
                if (SiteFinder.isZoneBlockedByPlayer(level, origin, entry.getValue(), prevRadius)) {
                    blockedZones.add(entry.getKey());
                }
            }
            if (!blockedZones.isEmpty()) {
                List<CampBlueprint.Placement> filtered = new ArrayList<>(ops.size());
                for (CampBlueprint.Placement p : ops) {
                    if (!blockedZones.contains(p.zone())) filtered.add(p);
                }
                ops = filtered;
            }
        }

        BuildJob job = new BuildJob(pack.id, level.dimension(), origin, pack.family, pack.color,
                pack.title(), from, to, automatic, ops, onComplete);
        jobs.put(pack.id, job);
    }

    /** Executes up to ~250 block operations per tick across queued build jobs. */
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
                Pack pack = data.feud.pack(job.packId);
                if (pack != null) {
                    pack.builtTier = Math.max(pack.builtTier, job.toTier);
                    data.changed();
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

        BlockState target = palette.resolve(op, job.packColor);
        if (op.material() == CampBlueprint.Material.AIR) {
            if (!existing.isAir()) level.setBlock(pos, target, Block.UPDATE_CLIENTS);
            return;
        }

        if (op.pass() == CampBlueprint.PASS_DECOR && !target.canSurvive(level, pos)) return;
        level.setBlock(pos, target, Block.UPDATE_ALL);

        if (op.material().isChest()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Container container) {
                SettlementTier lootTier = CampLoot.tierForChest(op.material());
                long posSeed = job.seed ^ (((long) pos.getX() * 341873128712L) ^ ((long) pos.getZ() * 132897987541L));
                CampLoot.fill(container, job.family, lootTier, posSeed);
            }
        } else if (op.material() == CampBlueprint.Material.BARREL) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Container container) {
                long posSeed = job.seed ^ (((long) pos.getX() * 9187L) ^ ((long) pos.getZ() * 7919L));
                CampLoot.fillBarrel(container, job.family, posSeed);
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
                    || (op.material() == CampBlueprint.Material.VASSAL_BANNER && existing.isAir());
            if (!replaceable) continue;
            if (op.material() == CampBlueprint.Material.VASSAL_BANNER && existing.isAir()) {
                // Ensure pole exists beneath a newly added vassal banner in the King's forecourt
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

    private static boolean playerNearby(MinecraftServer server, Pack pack) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator()) continue;
            if (!p.level().dimension().location().toString().equals(pack.dimension)) continue;
            if (Math.hypot(p.getX() - pack.campX(), p.getZ() - pack.campZ()) <= AUTO_TRIGGER_RANGE) {
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
