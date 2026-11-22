package io.github.pirut.bloodfeud.game.camp;

import io.github.pirut.bloodfeud.BloodFeud;
import io.github.pirut.bloodfeud.feud.Family;
import io.github.pirut.bloodfeud.feud.Feud;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.SettlementTier;
import io.github.pirut.bloodfeud.game.Earth;
import io.github.pirut.bloodfeud.game.FeudData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Chunk-safe incremental builder that places, ruins, repairs, and re-banners gang camps, outposts, strongholds,
 * and towns over multiple ticks without lag or overwriting player builds and containers.
 * <p>
 * Automatic world mutations only run off-screen while all non-spectator players in the same dimension are outside
 * {@link #standoffDistance(int, int)} and do not track any footprint chunk, using brief, auto-expiring in-memory
 * chunk tickets that are released on
 * every completion, pause, abort, cancellation, or shutdown path.
 * <p>
 * On the Earth a ticket only loads the empty carrier chunks. The cubes behind them are asked for every tick a ticket
 * is held, and nothing is scanned, queued or written except on a tick where the whole site's cubes are loaded.
 */
public final class CampBuilder {
    private static final int OPS_PER_TICK = 250;
    private static final int OFFSCREEN_OPS_PER_TICK = 400;
    private static final int MAX_CONCURRENT_AUTO = 2;
    private static final int OFFSCREEN_TICKET_TTL_TICKS = 200;
    private static final long TICKET_RENEW_INTERVAL_TICKS = 80L;
    private static final long PREP_TIMEOUT_TICKS = 400L;
    /** Longer on the Earth, where a first visit waits for map data to download. */
    private static final long EARTH_PREP_TIMEOUT_TICKS = 1200L;
    private static final long RETRY_COOLDOWN_TICKS = 2400L;
    private static final long STALL_TIMEOUT_TICKS = 600L;
    private static final int MAX_SEARCH_ATTEMPTS = 5;
    public static final int MAX_FOUNDATION_DEPTH = 14;

    private static final TicketType<ChunkPos> OFFSCREEN_TICKET = TicketType.create(
            "bloodfeud_camp", Comparator.comparingLong(ChunkPos::toLong), OFFSCREEN_TICKET_TTL_TICKS);

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

    private enum TicketPurpose { PREP, BUILD, REBANNER }

    private static final class HeldTicket {
        final ResourceKey<Level> dimension;
        final ChunkPos center;
        final int ticketRadius;
        final int targetX;
        final int targetZ;
        final int standoffRadius;
        final long acquiredAtTick;
        long renewedAtTick;
        TicketPurpose purpose;
        int searchAttempts;
        EarthSiteSearch earthSearch;

        HeldTicket(ResourceKey<Level> dimension, ChunkPos center, int ticketRadius,
                   int targetX, int targetZ, int standoffRadius,
                   long gameTime, TicketPurpose purpose) {
            this.dimension = dimension;
            this.center = center;
            this.ticketRadius = ticketRadius;
            this.targetX = targetX;
            this.targetZ = targetZ;
            this.standoffRadius = standoffRadius;
            this.acquiredAtTick = gameTime;
            this.renewedAtTick = gameTime;
            this.purpose = purpose;
        }
    }

    private final Map<UUID, BuildJob> jobs = new LinkedHashMap<>();
    private final Map<UUID, HeldTicket> heldTickets = new LinkedHashMap<>();

    /**
     * Minimum horizontal distance (blocks) every non-spectator player in the same dimension must be beyond
     * before automatic settlement changes may occur, guaranteeing every block of the settlement is outside
     * every player's loaded/rendered view distance (with a minimum floor of {@code settlementRadius + 96}).
     */
    public static int standoffDistance(int serverViewDistanceChunks, int settlementRadius) {
        return CampBlueprint.standoffDistance(serverViewDistanceChunks, settlementRadius);
    }

    /** Returns {@code true} iff {@code (playerX, playerZ)} is strictly beyond {@code standoff} blocks of {@code (targetX, targetZ)}. */
    public static boolean isOutsideStandoff(double targetX, double targetZ, double playerX, double playerZ, int standoff) {
        return CampBlueprint.isOutsideStandoff(targetX, targetZ, playerX, playerZ, standoff);
    }

    /**
     * Returns {@code true} iff no non-spectator player in {@code dimension} is within
     * {@link #standoffDistance(int, int)} of {@code (x, z)} AND no non-spectator player's
     * {@link ChunkTrackingView} (or current chunk view at server view distance) tracks any chunk
     * in {@link CampBlueprint#footprintChunkBounds(int, int, int)}.
     */
    public static boolean isAreaClearOfPlayers(MinecraftServer server, String dimension, int x, int z, int radius) {
        if (server == null || dimension == null) return false;
        int viewDistance = server.getPlayerList().getViewDistance();
        int standoff = standoffDistance(viewDistance, radius);
        int[] bounds = CampBlueprint.footprintChunkBounds(x, z, radius);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator()) continue;
            if (!p.level().dimension().location().toString().equals(dimension)) continue;
            if (!isOutsideStandoff(x, z, p.getX(), p.getZ(), standoff)) {
                return false;
            }
            ChunkPos playerChunk = p.chunkPosition();
            if (CampBlueprint.isFootprintTrackedByChunkView(
                    x, z, radius, playerChunk.x, playerChunk.z, viewDistance)) {
                return false;
            }
            ChunkTrackingView view = p.getChunkTrackingView();
            if (view != null && isChunkBoundsTracked(bounds, view)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isChunkBoundsTracked(int[] bounds, ChunkTrackingView view) {
        for (int cx = bounds[0]; cx <= bounds[2]; cx++) {
            for (int cz = bounds[1]; cz <= bounds[3]; cz++) {
                if (view.contains(cx, cz)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Computes {@code {centerChunkX, centerChunkZ, ticketRadius}} for an off-screen region ticket that covers
     * every chunk in {@link CampBlueprint#footprintChunkBounds(int, int, int)} at FULL status (plus 1-chunk margin).
     * <p>
     * In Minecraft 1.21.1 {@code DistanceManager.addRegionTicket(type, center, ticketRadius, value)}, the ticket level
     * at {@code center} is {@code 33 - ticketRadius}, so every chunk at Chebyshev distance {@code d <= ticketRadius}
     * reaches {@code FULL} status ({@code level <= 33}); setting {@code ticketRadius = maxChebyshevDist + 1} (min 1)
     * ensures every footprint chunk has {@code d < ticketRadius} ({@code level <= 32}) and all 1-chunk neighbors are
     * also {@code FULL}.
     */
    public static int[] ticketCoverage(int cx, int cz, int footprintRadius) {
        return CampBlueprint.ticketCoverage(cx, cz, footprintRadius);
    }

    /** Off-screen search footprint radius covering every spiral candidate around {@code (homeX, homeZ)}. */
    public static int searchFootprintRadius(int settlementRadius) {
        return CampBlueprint.searchFootprintRadius(settlementRadius);
    }

    /** Off-screen search footprint radius for finding an initial site around {@code (homeX, homeZ)}. */
    public static int searchFootprintRadius(SettlementTier target) {
        return CampBlueprint.searchFootprintRadius(target);
    }

    public boolean isBuilding(UUID packId) {
        return jobs.containsKey(packId);
    }

    /** Whether a held ticket is waiting on cubes, which only last until the Earth's next unload and so can't wait 40 ticks. */
    public boolean isWaitingForCubes() {
        for (HeldTicket ticket : heldTickets.values()) {
            if (isWaitingForCubes(ticket)) return true;
        }
        return false;
    }

    private static boolean isWaitingForCubes(HeldTicket ticket) {
        return ticket != null && ticket.purpose != TicketPurpose.BUILD && Earth.is(ticket.dimension);
    }

    /**
     * Cancels all in-flight automatic build/ruin/repair jobs and releases their off-screen chunk tickets
     * (called when {@code /bloodfeud camps off} disables auto-camps).
     */
    public void cancelAutomatic(MinecraftServer server) {
        for (Iterator<Map.Entry<UUID, BuildJob>> it = jobs.entrySet().iterator(); it.hasNext(); ) {
            BuildJob job = it.next().getValue();
            if (job.automatic) {
                it.remove();
                releaseTicket(server, job.packId);
            }
        }
        List<UUID> toRelease = new ArrayList<>();
        for (Map.Entry<UUID, HeldTicket> entry : heldTickets.entrySet()) {
            if (entry.getValue().purpose != TicketPurpose.REBANNER) {
                toRelease.add(entry.getKey());
            }
        }
        for (UUID id : toRelease) {
            releaseTicket(server, id);
        }
    }

    /** Clears every queued build job and releases all off-screen chunk tickets (server stop or forget everything). */
    public void clearAll(MinecraftServer server) {
        jobs.clear();
        releaseAllTickets(server);
    }

    private HeldTicket acquireOrRenewTicket(MinecraftServer server, UUID id, ServerLevel level,
                                            int targetX, int targetZ, int footprintRadius, int standoffRadius,
                                            long gameTime, TicketPurpose purpose) {
        int[] cov = ticketCoverage(targetX, targetZ, footprintRadius);
        ChunkPos center = new ChunkPos(cov[0], cov[1]);
        int ticketRadius = cov[2];
        ResourceKey<Level> dim = level.dimension();

        HeldTicket existing = heldTickets.get(id);
        if (existing != null) {
            if (existing.dimension.equals(dim)
                    && existing.center.equals(center)
                    && existing.ticketRadius == ticketRadius) {
                existing.purpose = purpose;
                if (gameTime - existing.renewedAtTick >= TICKET_RENEW_INTERVAL_TICKS) {
                    level.getChunkSource().addRegionTicket(OFFSCREEN_TICKET, center, ticketRadius, center);
                    existing.renewedAtTick = gameTime;
                }
                return existing;
            }
            // Add the updated ticket first so overlapping chunks stay loaded continuously before releasing the old one
            level.getChunkSource().addRegionTicket(OFFSCREEN_TICKET, center, ticketRadius, center);
            HeldTicket updated = new HeldTicket(dim, center, ticketRadius, targetX, targetZ, standoffRadius, gameTime, purpose);
            heldTickets.put(id, updated);
            removeTicketFromServerIfUnused(server, existing);
            return updated;
        }

        level.getChunkSource().addRegionTicket(OFFSCREEN_TICKET, center, ticketRadius, center);
        HeldTicket ticket = new HeldTicket(dim, center, ticketRadius, targetX, targetZ, standoffRadius, gameTime, purpose);
        heldTickets.put(id, ticket);
        return ticket;
    }

    private void releaseTicket(MinecraftServer server, UUID id) {
        HeldTicket removed = heldTickets.remove(id);
        if (removed != null) {
            removeTicketFromServerIfUnused(server, removed);
        }
    }

    private void removeTicketFromServerIfUnused(MinecraftServer server, HeldTicket target) {
        if (server == null || target == null) return;
        for (HeldTicket other : heldTickets.values()) {
            if (other.dimension.equals(target.dimension)
                    && other.center.equals(target.center)
                    && other.ticketRadius == target.ticketRadius) {
                return;
            }
        }
        ServerLevel level = server.getLevel(target.dimension);
        if (level != null) {
            level.getChunkSource().removeRegionTicket(OFFSCREEN_TICKET, target.center, target.ticketRadius, target.center);
        }
    }

    private void releaseAllTickets(MinecraftServer server) {
        if (heldTickets.isEmpty()) return;
        List<HeldTicket> toRelease = new ArrayList<>(heldTickets.values());
        heldTickets.clear();
        if (server == null) return;
        for (HeldTicket t : toRelease) {
            ServerLevel level = server.getLevel(t.dimension);
            if (level != null) {
                level.getChunkSource().removeRegionTicket(OFFSCREEN_TICKET, t.center, t.ticketRadius, t.center);
            }
        }
    }

    private static boolean hasAbandonedSite(Feud feud, UUID packId) {
        for (Feud.AbandonedSite site : feud.abandonedSites) {
            if (site.packId().equals(packId)) return true;
        }
        return false;
    }

    /**
     * Guards pending off-screen prep/rebanner tickets on every tick: immediately releases tickets if a player
     * enters standoff distance, if the pack disappears, if auto-camps are turned off, or if prep times out,
     * and renews still-valid tickets before TTL expiry.
     */
    private void maintainOffscreenTickets(MinecraftServer server, FeudData data, long gameTime) {
        if (heldTickets.isEmpty()) return;
        for (Iterator<Map.Entry<UUID, HeldTicket>> it = heldTickets.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, HeldTicket> entry = it.next();
            UUID id = entry.getKey();
            HeldTicket ticket = entry.getValue();
            if (ticket.purpose == TicketPurpose.BUILD) {
                if (!jobs.containsKey(id)) {
                    it.remove();
                    removeTicketFromServerIfUnused(server, ticket);
                }
                continue;
            }
            if (ticket.purpose == TicketPurpose.PREP && !BloodFeud.config().buildCamps) {
                it.remove();
                removeTicketFromServerIfUnused(server, ticket);
                continue;
            }
            Pack pack = data.feud.pack(id);
            boolean hasAbandoned = pack == null && hasAbandonedSite(data.feud, id);
            if (pack == null && !hasAbandoned) {
                it.remove();
                removeTicketFromServerIfUnused(server, ticket);
                continue;
            }
            if (ticket.purpose == TicketPurpose.REBANNER && (pack == null || !pack.bannersDirty || jobs.containsKey(id))) {
                it.remove();
                removeTicketFromServerIfUnused(server, ticket);
                continue;
            }
            if (!isAreaClearOfPlayers(server, ticket.dimension.location().toString(),
                    ticket.targetX, ticket.targetZ, ticket.standoffRadius)) {
                it.remove();
                removeTicketFromServerIfUnused(server, ticket);
                continue;
            }
            long timeout = Earth.is(ticket.dimension) ? EARTH_PREP_TIMEOUT_TICKS : PREP_TIMEOUT_TICKS;
            if (gameTime - ticket.acquiredAtTick >= timeout) {
                it.remove();
                removeTicketFromServerIfUnused(server, ticket);
                if (ticket.purpose == TicketPurpose.PREP && pack != null) {
                    pack.siteRetryAt = gameTime + RETRY_COOLDOWN_TICKS;
                }
                continue;
            }
            if (gameTime - ticket.renewedAtTick >= TICKET_RENEW_INTERVAL_TICKS) {
                ServerLevel level = server.getLevel(ticket.dimension);
                if (level == null) {
                    it.remove();
                    removeTicketFromServerIfUnused(server, ticket);
                    continue;
                }
                level.getChunkSource().addRegionTicket(OFFSCREEN_TICKET, ticket.center, ticket.ticketRadius, ticket.center);
                ticket.renewedAtTick = gameTime;
            }
        }
    }

    /**
     * Checks packs and abandoned sites whose areas are completely clear of players ({@link #isAreaClearOfPlayers})
     * and schedules off-screen re-bannering, initial builds, upgrades, in-place ruinations, restorations, and
     * abandoned-site crumbling using brief, bounded off-screen chunk tickets.
     *
     * @param scheduled false on the ticks between scheduled checks, which only look again at work waiting for cubes
     */
    public void checkAutoBuilds(MinecraftServer server, FeudData data, long gameTime, boolean scheduled) {
        Feud feud = data.feud;

        // 1. Flush pending faction re-bannering off-screen only
        boolean dirtiedData = false;
        for (Pack pack : feud.packs.values()) {
            if (!pack.bannersDirty || !pack.siteSet || pack.builtTier <= 0 || jobs.containsKey(pack.id)) continue;
            if (!scheduled && !isWaitingForCubes(heldTickets.get(pack.id))) continue;
            SettlementTier built = SettlementTier.byLevel(pack.builtTier);
            if (!isAreaClearOfPlayers(server, pack.dimension, pack.siteX, pack.siteZ, built.radius)) {
                releaseTicket(server, pack.id);
                continue;
            }
            ServerLevel level = resolveBuildLevel(server, pack.dimension);
            if (level == null) {
                releaseTicket(server, pack.id);
                continue;
            }
            if (SiteFinder.isSiteReady(level, new BlockPos(pack.siteX, pack.siteY, pack.siteZ), built.radius)) {
                if (rebanner(level, feud, pack)) {
                    pack.bannersDirty = false;
                    dirtiedData = true;
                }
                releaseTicket(server, pack.id);
            } else if (!heldTickets.containsKey(pack.id) && heldTickets.size() < MAX_CONCURRENT_AUTO) {
                acquireOrRenewTicket(server, pack.id, level, pack.siteX, pack.siteZ, built.radius, built.radius,
                        gameTime, TicketPurpose.REBANNER);
            }
        }
        if (dirtiedData) data.changed();

        if (!BloodFeud.config().buildCamps) return;

        // 2. Automatic initial builds, upgrades, in-place ruinations, and repairs off-screen
        for (Pack pack : feud.packs.values()) {
            if (jobs.containsKey(pack.id)) continue;
            HeldTicket existingTicket = heldTickets.get(pack.id);
            if (existingTicket != null && existingTicket.purpose == TicketPurpose.REBANNER) continue;
            if (!scheduled && !isWaitingForCubes(existingTicket)) continue;

            Pack.AutoBuildAction action = autoActionFor(pack);
            if (action == Pack.AutoBuildAction.NONE) {
                if (existingTicket != null) releaseTicket(server, pack.id);
                continue;
            }
            if (pack.siteRetryAt > gameTime) {
                if (existingTicket != null) releaseTicket(server, pack.id);
                continue;
            }

            SettlementTier target = pack.tier == null ? SettlementTier.CAMP : pack.tier;
            int checkTier = (action == Pack.AutoBuildAction.RUIN && pack.builtTier > 0)
                    ? pack.builtTier
                    : Math.max(pack.builtTier, target.level);
            SettlementTier checkTierEnum = SettlementTier.byLevel(checkTier);
            int radius = checkTierEnum.radius;
            int gateX = pack.campX();
            int gateZ = pack.campZ();
            int gateRadius = pack.siteSet ? radius : searchFootprintRadius(checkTierEnum);

            if (!isAreaClearOfPlayers(server, pack.dimension, gateX, gateZ, gateRadius)) {
                releaseTicket(server, pack.id);
                continue;
            }
            ServerLevel level = resolveBuildLevel(server, pack.dimension);
            if (level == null) {
                releaseTicket(server, pack.id);
                continue;
            }

            if (!pack.siteSet) {
                if (!SiteFinder.isFootprintLoaded(level, pack.homeX, pack.homeZ, gateRadius)) {
                    if (existingTicket == null && heldTickets.size() < MAX_CONCURRENT_AUTO) {
                        acquireOrRenewTicket(server, pack.id, level, pack.homeX, pack.homeZ,
                                gateRadius, gateRadius, gameTime, TicketPurpose.PREP);
                    }
                    continue;
                }
                if (existingTicket == null && heldTickets.size() >= MAX_CONCURRENT_AUTO) continue;

                SiteFinder.Site site;
                boolean earth = Earth.is(level);
                if (earth) {
                    if (existingTicket == null) {
                        existingTicket = acquireOrRenewTicket(server, pack.id, level, pack.homeX, pack.homeZ,
                                gateRadius, gateRadius, gameTime, TicketPurpose.PREP);
                    }
                    if (existingTicket.earthSearch == null) {
                        existingTicket.earthSearch = new EarthSiteSearch(level, pack,
                                Math.max(SettlementTier.CAMP.radius, target.radius));
                    }
                    EarthSiteSearch.Step step = existingTicket.earthSearch.step(level, pack);
                    if (!step.done()) continue;
                    site = step.site();
                } else {
                    site = SiteFinder.findAutoSite(level, pack, target);
                }
                if (site == null) {
                    if (!earth && existingTicket != null && existingTicket.searchAttempts + 1 < MAX_SEARCH_ATTEMPTS) {
                        existingTicket.searchAttempts++;
                    } else {
                        releaseTicket(server, pack.id);
                        pack.siteRetryAt = gameTime + RETRY_COOLDOWN_TICKS;
                    }
                    continue;
                }
                if (!isAreaClearOfPlayers(server, pack.dimension, site.origin().getX(), site.origin().getZ(), radius)) {
                    releaseTicket(server, pack.id);
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
                if (SiteFinder.isWaitingForGround(level, site.origin(), radius)) continue;
            } else {
                if (!SiteFinder.isSiteReady(level, new BlockPos(pack.siteX, pack.siteY, pack.siteZ), radius)) {
                    if (existingTicket == null && heldTickets.size() < MAX_CONCURRENT_AUTO) {
                        acquireOrRenewTicket(server, pack.id, level, pack.siteX, pack.siteZ,
                                radius, radius, gameTime, TicketPurpose.PREP);
                    }
                    continue;
                }
                if (existingTicket == null && heldTickets.size() >= MAX_CONCURRENT_AUTO) continue;

                // Re-validate reserved initial sites (siteSet && !buildStarted && builtTier == 0) before placing blocks;
                // once buildStarted is true, skip the coarse full-radius scan (which would flag skipped-zone player blocks
                // or downward foundation fills) and let queueBuild/queueRuin run per-zone filterPlan with ownership directly.
                if (pack.builtTier == 0 && !pack.buildStarted) {
                    BlockPos origin = new BlockPos(pack.siteX, pack.siteY, pack.siteZ);
                    if (SiteFinder.hasPlayerBuild(level, origin, radius, Set.of(), Set.of())) {
                        releaseTicket(server, pack.id);
                        pack.siteRetryAt = gameTime + RETRY_COOLDOWN_TICKS;
                        pack.siteSet = false;
                        pack.skippedZones.clear();
                        pack.skippedColumns.clear();
                        data.changed();
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
                acquireOrRenewTicket(server, pack.id, level, pack.siteX, pack.siteZ,
                        radius, radius, gameTime, TicketPurpose.BUILD);
                if (!res.skippedZones().isEmpty() || res.skippedRingColumns() > 0) {
                    data.changed();
                }
            } else {
                releaseTicket(server, pack.id);
                pack.siteRetryAt = gameTime + RETRY_COOLDOWN_TICKS;
                if (pack.builtTier == 0 && !pack.buildStarted) {
                    pack.siteSet = false;
                    pack.skippedZones.clear();
                    pack.skippedColumns.clear();
                    data.changed();
                }
            }
        }

        // 3. Crumble intact settlements left behind by disbanded packs while players are away
        for (Feud.AbandonedSite site : new ArrayList<>(feud.abandonedSites)) {
            if (jobs.containsKey(site.packId())) continue;
            if (!scheduled && !isWaitingForCubes(heldTickets.get(site.packId()))) continue;
            SettlementTier built = SettlementTier.byLevel(site.builtTier());
            HeldTicket existingTicket = heldTickets.get(site.packId());
            if (!isAreaClearOfPlayers(server, site.dimension(), site.siteX(), site.siteZ(), built.radius)) {
                releaseTicket(server, site.packId());
                continue;
            }
            ServerLevel level = resolveBuildLevel(server, site.dimension());
            if (level == null) {
                releaseTicket(server, site.packId());
                continue;
            }
            if (!SiteFinder.isSiteReady(level, new BlockPos(site.siteX(), site.siteY(), site.siteZ()), built.radius)) {
                if (existingTicket == null && heldTickets.size() < MAX_CONCURRENT_AUTO) {
                    acquireOrRenewTicket(server, site.packId(), level, site.siteX(), site.siteZ(),
                            built.radius, built.radius, gameTime, TicketPurpose.PREP);
                }
                continue;
            }
            if (existingTicket == null && heldTickets.size() >= MAX_CONCURRENT_AUTO) continue;

            CampBlueprint.Plan diff = CampBlueprint.ruinDiffPlan(site.packId(), site.family(), site.color(),
                    site.vassalColors(), site.siteFacing(), site.builtTier());
            diff = CampBlueprint.withoutSkipped(diff, site.skippedZones(), site.skippedColumns());
            Set<CampBlueprint.Spot> ownedPositions = new SiteFinder.Owned(CampBlueprint.ownedMaterials(
                    site.packId(), site.family(), site.color(), site.vassalColors(), site.siteFacing(),
                    site.builtTier(), site.skippedZones(), site.skippedColumns()), site.family());
            Set<CampBlueprint.Spot> ownedContainers = CampBlueprint.containerPositions(
                    site.packId(), site.family(), site.color(), site.vassalColors(), site.siteFacing(),
                    site.builtTier(), site.skippedZones(), site.skippedColumns());
            BlockPos origin = new BlockPos(site.siteX(), site.siteY(), site.siteZ());
            FilterResult filtered = filterPlan(level, origin, diff, null, ownedPositions, ownedContainers);
            if (filtered.ops().isEmpty()) {
                releaseTicket(server, site.packId());
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
            acquireOrRenewTicket(server, site.packId(), level, site.siteX(), site.siteZ(),
                    built.radius, built.radius, gameTime, TicketPurpose.BUILD);
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
        Set<CampBlueprint.Spot> ownedPositions = new SiteFinder.Owned(CampBlueprint.ownedMaterials(
                pack.id, pack.family, displayColor, vassalColors, pack.siteFacing, ownedTier,
                pack.skippedZones, pack.skippedColumns), pack.family);
        Set<CampBlueprint.Spot> ownedContainers = CampBlueprint.containerPositions(
                pack.id, pack.family, displayColor, vassalColors, pack.siteFacing, ownedTier,
                pack.skippedZones, pack.skippedColumns);

        boolean shouldProtect = automatic || from > 0 || pack.buildStarted;
        if (shouldProtect && SiteFinder.isWaitingForGround(level, origin, SettlementTier.byLevel(to).radius)) {
            return new QueueResult(false, List.of(), 0);
        }
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
        if (!automatic) releaseTicket(level.getServer(), pack.id);
        jobs.put(pack.id, job);
        return new QueueResult(true, filtered.skippedZones(), filtered.skippedRingColumns());
    }

    /**
     * Queues either an in-place ruination diff (if the settlement is already built at {@code pack.builtTier})
     * or a fresh ruined settlement build from scratch up to {@code targetTier}.
     */
    public QueueResult queueRuin(ServerLevel level, Feud feud, Pack pack, int targetTier,
                                 boolean automatic, long gameTime, Runnable onComplete) {
        return queueRuin(level, feud, pack, targetTier, vassalColors(feud, pack), automatic, gameTime, onComplete);
    }

    /**
     * Queues either an in-place ruination diff or a fresh ruined settlement build using an explicit
     * {@code vassalColors} list (e.g. {@code List.of()} when pre-validating before detaching vassals).
     */
    public QueueResult queueRuin(ServerLevel level, Feud feud, Pack pack, int targetTier,
                                 List<Integer> vassalColors, boolean automatic, long gameTime, Runnable onComplete) {
        if (!pack.siteSet) {
            return new QueueResult(false, List.of(), 0);
        }
        int to = Math.max(1, Math.min(SettlementTier.TOWN.level, targetTier));
        BlockPos origin = new BlockPos(pack.siteX, pack.siteY, pack.siteZ);
        int displayColor = feud.top(pack).color;
        vassalColors = vassalColors == null ? List.of() : List.copyOf(vassalColors);
        boolean inPlace = pack.builtTier > 0 && to <= pack.builtTier;
        CampBlueprint.Plan plan = inPlace
                ? CampBlueprint.ruinDiffPlan(pack.id, pack.family, displayColor, vassalColors, pack.siteFacing, pack.builtTier)
                : CampBlueprint.ruinPlan(pack.id, pack.family, displayColor, vassalColors, pack.siteFacing, to);
        if (inPlace || pack.buildStarted) {
            plan = CampBlueprint.withoutSkipped(plan, pack.skippedZones, pack.skippedColumns);
        }
        int finalTier = inPlace ? pack.builtTier : to;

        Set<CampBlueprint.Spot> ownedPositions = new SiteFinder.Owned(inPlace
                ? CampBlueprint.ownedMaterials(pack.id, pack.family, displayColor, vassalColors,
                        pack.siteFacing, pack.builtTier, pack.skippedZones, pack.skippedColumns)
                : (pack.buildStarted
                        ? CampBlueprint.ownedRuinedMaterials(pack.id, pack.family, displayColor, vassalColors,
                                pack.siteFacing, to, pack.skippedZones, pack.skippedColumns)
                        : Map.of()), pack.family);
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
            if (SiteFinder.isWaitingForGround(level, origin, SettlementTier.byLevel(finalTier).radius)) {
                return new QueueResult(false, List.of(), 0);
            }
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
        if (!automatic) releaseTicket(level.getServer(), pack.id);
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
        if (SiteFinder.isWaitingForGround(level, origin, SettlementTier.byLevel(to).radius)) {
            return new QueueResult(false, List.of(), 0);
        }
        int displayColor = feud.top(pack).color;
        List<Integer> vassalColors = vassalColors(feud, pack);
        CampBlueprint.Plan plan = CampBlueprint.repairPlan(pack.id, pack.family, displayColor, vassalColors,
                pack.siteFacing, built, to);
        plan = CampBlueprint.withoutSkipped(plan, pack.skippedZones, pack.skippedColumns);

        Set<CampBlueprint.Spot> ownedPositions = new SiteFinder.Owned(CampBlueprint.ownedRuinedMaterials(
                pack.id, pack.family, displayColor, vassalColors, pack.siteFacing, built,
                pack.skippedZones, pack.skippedColumns), pack.family);
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
        if (!automatic) releaseTicket(level.getServer(), pack.id);
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

    /**
     * Executes queued build/ruin/repair jobs (up to {@value #OPS_PER_TICK} ops/tick for manual builds and
     * {@value #OFFSCREEN_OPS_PER_TICK} ops/tick for off-screen automatic jobs) while maintaining off-screen
     * chunk tickets and immediately pausing any automatic job if a player enters standoff distance.
     */
    public void tick(MinecraftServer server, FeudData data, long gameTime) {
        maintainOffscreenTickets(server, data, gameTime);
        if (jobs.isEmpty()) return;

        int manualRemaining = OPS_PER_TICK;
        int totalRemaining = OFFSCREEN_OPS_PER_TICK;
        List<Runnable> completedCallbacks = null;

        for (Iterator<Map.Entry<UUID, BuildJob>> it = jobs.entrySet().iterator(); it.hasNext(); ) {
            BuildJob job = it.next().getValue();
            if (job.automatic && !BloodFeud.config().buildCamps) {
                it.remove();
                releaseTicket(server, job.packId);
                continue;
            }
            Pack pack = null;
            if (job.mode != Mode.ABANDONED_RUIN) {
                pack = data.feud.pack(job.packId);
                if (pack == null) {
                    it.remove();
                    releaseTicket(server, job.packId);
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
                releaseTicket(server, job.packId);
                continue;
            }

            // Mid-flight player-away standoff protection for automatic jobs
            if (job.automatic) {
                int jobRadius = SettlementTier.byLevel(Math.max(job.fromTier, job.toTier)).radius;
                if (!isAreaClearOfPlayers(server, job.dimension.location().toString(),
                        job.origin.getX(), job.origin.getZ(), jobRadius)) {
                    job.lastProgressTick = gameTime;
                    job.stalled = false;
                    releaseTicket(server, job.packId);
                    continue;
                }
                if (!heldTickets.containsKey(job.packId) && heldTickets.size() >= MAX_CONCURRENT_AUTO) {
                    job.lastProgressTick = gameTime;
                    continue;
                }
                acquireOrRenewTicket(server, job.packId, level,
                        job.origin.getX(), job.origin.getZ(), jobRadius, jobRadius,
                        gameTime, TicketPurpose.BUILD);
            }

            int jobBudget = job.automatic ? totalRemaining : Math.min(manualRemaining, totalRemaining);
            if (jobBudget <= 0) {
                job.lastProgressTick = gameTime;
                continue;
            }

            FamilyPalette palette = FamilyPalette.of(job.family);
            boolean stalled = SiteFinder.isWaitingForGround(level, job.origin,
                    SettlementTier.byLevel(Math.max(job.fromTier, job.toTier)).radius);
            int opsDone = 0;
            while (!stalled && jobBudget > 0 && job.index < job.ops.size()) {
                CampBlueprint.Placement op = job.ops.get(job.index);
                BlockPos pos = job.origin.offset(op.rx(), op.ry(), op.rz());
                if (!SiteFinder.isChunkReady(level, pos.getX() >> 4, pos.getZ() >> 4) || !Earth.isLoaded(level, pos)) {
                    stalled = true;
                    break;
                }
                applyOp(level, pos, op, palette, job);
                job.index++;
                jobBudget--;
                opsDone++;
            }
            totalRemaining -= opsDone;
            if (!job.automatic) {
                manualRemaining -= opsDone;
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
                    releaseTicket(server, job.packId);
                    if (pack != null) {
                        pack.siteRetryAt = gameTime + RETRY_COOLDOWN_TICKS;
                    }
                }
                continue;
            }

            if (job.index >= job.ops.size()) {
                it.remove();
                releaseTicket(server, job.packId);
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
        if (pos.getY() <= Earth.bottom(level) || pos.getY() >= Earth.top(level) - 1) return;

        if (op.material() == CampBlueprint.Material.FOUNDATION_COLUMN) {
            BlockState fill = palette.foundation.defaultBlockState();
            BlockPos.MutableBlockPos cur = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
            for (int depth = 0; depth < MAX_FOUNDATION_DEPTH; depth++) {
                if (cur.getY() <= Earth.bottom(level) + 1 || !Earth.isLoaded(level, cur)) break;
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
                    // Named first: applying components also applies the chest's (empty) contents, which wiped the loot.
                    be.applyComponents(be.components(), DataComponentPatch.builder()
                            .set(DataComponents.CUSTOM_NAME, CampLoot.chestTitle(job.packTitle, lootTier))
                            .build());
                    CampLoot.fill(container, job.family, lootTier, posSeed);
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
        if (!SiteFinder.isSiteReady(level, new BlockPos(pack.siteX, pack.siteY, pack.siteZ), built.radius)) return false;

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
            if (!SiteFinder.isChunkReady(level, pos.getX() >> 4, pos.getZ() >> 4)) continue;
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
                if (!SiteFinder.isChunkReady(level, pos.getX() >> 4, pos.getZ() >> 4)) continue;
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

    /** The level automatic building may work in, or null. */
    private static ServerLevel resolveBuildLevel(MinecraftServer server, String dimension) {
        ServerLevel level = resolveLevel(server, dimension);
        return level != null && Earth.canBuildIn(level) ? level : null;
    }

    private static ServerLevel resolveLevel(MinecraftServer server, String dimension) {
        try {
            return server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension)));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
