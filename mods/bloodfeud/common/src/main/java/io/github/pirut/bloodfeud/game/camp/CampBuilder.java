package io.github.pirut.bloodfeud.game.camp;

import io.github.pirut.bloodfeud.BloodFeud;
import io.github.pirut.bloodfeud.feud.Family;
import io.github.pirut.bloodfeud.feud.Feud;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.SettlementTier;
import io.github.pirut.bloodfeud.game.Earth;
import io.github.pirut.bloodfeud.game.FeudData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Chunk-safe incremental builder that places, ruins, repairs, and re-banners gang camps, outposts, strongholds,
 * and towns over multiple ticks without lag or overwriting player builds and containers.
 * <p>
 * A pack builds (and rebuilds) its own settlement. While a player can see the site (its chunks are in someone's view;
 * on the Earth, its cubes are in someone's watch range), a crew of the pack's mobs walks to each spot and puts the
 * block there, a few blocks a second. While nobody can see it, it is built off screen, quickly, with its ground loaded
 * for it just for the build: a chunk ticket and, on the Earth, its cubes asked for (and kept, where Earth Platform can
 * keep them). Both are the same saved build ({@link Pack.Works}), so it carries on from the same step whichever way it
 * goes, after a restart too. Nothing is ever written to ground that isn't loaded, or to the Earth's void air. Killing
 * the crew stalls the build until the pack sends another.
 * <p>
 * Other automatic world mutations (ruin, abandoned-site crumbling, re-bannering) only run off-screen while all
 * non-spectator players in the same dimension are outside
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
    /** How long a pack takes to send new builders after a player killed the last of them. */
    public static final long BUILDERS_RETURN_TICKS = 6000L;
    private static final long HIRE_INTERVAL_TICKS = 40L;
    /** Builders saved with the site come back as it loads, so hiring waits a moment. */
    private static final long SETTLE_TICKS = 40L;
    /** A builder that can't get this close to its spot within {@link #PLACE_ANYWAY_TICKS} places it anyway. */
    private static final double REACH = 5.0;
    private static final long PLACE_ANYWAY_TICKS = 100L;
    /** A builder not seen for this long (killed out of sight, despawned) is struck from the crew. */
    private static final long LOST_BUILDER_TICKS = 200L;
    /** Steps that need no block placed (already right, or protected) are passed over for free, up to this many a tick. */
    private static final int FREE_SKIPS_PER_TICK = 512;
    /** After this many failed hires in a row (nowhere to stand, peaceful), the build goes on without visible builders. */
    private static final int MAX_FAILED_HIRES = 5;
    /** Longest a build keeps a slot while others wait for one, before it hands it on. */
    private static final long HOLD_TURN_TICKS = 1200L;

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
        /** The saved progress of a build its crew carries out, or null for operator and off-screen jobs. */
        Pack.Works works;
        Set<String> blockedZones = Set.of();
        Set<Long> blockedCols = Set.of();
        /** Where the blocks it puts down are noted as the pack's (null for an old settlement with no record). */
        Map<Long, String> placedSink;
        /** Since when the ground it holds hasn't all loaded, or -1. */
        long terrainWaitSince = -1;

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
        /** Held for a crewed build's site search or first load: kept while players are near but can't see it. */
        boolean crewed;
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

    /** Spawns one builder for a pack near its site: a grunt of its kind. Live supplies it. */
    public interface Hiring {
        Mob hire(ServerLevel level, Pack pack, BlockPos near, int radius);
    }

    /** A pack's builders and how its build is pacing, while the server runs. */
    private static final class Crew {
        final Map<UUID, Long> mobs = new LinkedHashMap<>(); // builder -> game time last seen
        long loadedSince = -1;
        long nextHire;
        double budget;
        long waitingSince = -1;
        /** Where a builder can reach the current step from, worked out once for it (null: from nowhere). */
        BlockPos vantage;
        int vantageFor = -1;
        /** Building off screen right now (nobody can see the site). */
        boolean offscreen;
        int failedHires;
        boolean logged;
        long lastReport;
        int reportedAt = -1;
    }

    /** Whether a player can see a settlement site. Tests replace it. */
    public interface Sight {
        boolean seen(MinecraftServer server, ServerLevel level, BlockPos origin, int radius);
    }

    private Hiring hiring;
    private Sight sight = CampBuilder::seenByPlayer;
    /** Steps every job together may still take this tick. */
    private int stepBudget;
    /** Builds backing off from taking a slot, until this game time. */
    private final Map<UUID, Long> holdRetryAt = new HashMap<>();
    /** The last game time a build was refused a slot because all were taken. */
    private long slotWantedAt = Long.MIN_VALUE / 2;
    /** Packs whose site cubes are kept loaded on the Earth for an off-screen build, and where. */
    private final Map<UUID, ResourceKey<Level>> cubeHolds = new HashMap<>();
    private final Map<UUID, Crew> crews = new HashMap<>();
    private final Map<UUID, UUID> builderPack = new HashMap<>();
    /** Site searches on the Earth, which take a few ticks each. */
    private final Map<UUID, EarthSiteSearch> searches = new HashMap<>();

    public void setHiring(Hiring hiring) {
        this.hiring = hiring;
    }

    public void setSight(Sight sight) {
        this.sight = sight;
    }

    /**
     * Whether any player in the level can see a site: one of its chunks is in a player's view. On the Earth the site's
     * cubes must also be within the range the Earth sends cubes round a player (fewer cubes across than the view, and
     * {@code Earth.watchVertical} cubes up and down).
     */
    public static boolean seenByPlayer(MinecraftServer server, ServerLevel level, BlockPos origin, int radius) {
        if (server == null || server.getPlayerList() == null || level == null) return false;
        int viewDistance = server.getPlayerList().getViewDistance();
        boolean earth = Earth.is(level);
        int[] bounds = CampBlueprint.footprintChunkBounds(origin.getX(), origin.getZ(), radius);
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            ChunkPos at = p.chunkPosition();
            if (earth) {
                int v = Earth.watchVertical(level), cy = p.getBlockY() >> 4;
                if (cy < ((origin.getY() - SiteFinder.SITE_DOWN) >> 4) - v || cy > ((origin.getY() + SiteFinder.SITE_UP) >> 4) + v) continue;
                int h = Earth.watchRadius(level, viewDistance);
                long dx = Math.max(0, Math.max(bounds[0] - at.x, at.x - bounds[2]));
                long dz = Math.max(0, Math.max(bounds[1] - at.z, at.z - bounds[3]));
                if (dx * dx + dz * dz <= (long) h * h + h) return true;
                continue;
            }
            if (CampBlueprint.isFootprintTrackedByChunkView(origin.getX(), origin.getZ(), radius, at.x, at.z, viewDistance)) return true;
            ChunkTrackingView view = p.getChunkTrackingView();
            if (view != null && isChunkBoundsTracked(bounds, view)) return true;
        }
        return false;
    }

    /** Keeps a site's cubes loaded on the Earth until its build is done, where Earth Platform can. */
    private void keepCubes(ServerLevel level, UUID packId, BlockPos origin, int radius) {
        if (cubeHolds.containsKey(packId) || !Earth.canKeep(level)) return;
        int reach = radius + SiteFinder.SITE_SIDE_MARGIN;
        cubeHolds.put(packId, level.dimension()); // tracked before it is taken, so every way out lets it go
        if (Earth.keep(level, packId, origin.getX() - reach, origin.getY() - SiteFinder.SITE_DOWN, origin.getZ() - reach,
                origin.getX() + reach, origin.getY() + SiteFinder.SITE_UP, origin.getZ() + reach)) {
            BloodFeud.LOG.info("Blood Feud: keeping the Earth's cubes round {} {} {} loaded for a build",
                    origin.getX(), origin.getY(), origin.getZ());
        } else {
            cubeHolds.remove(packId); // Earth.keep let go of what it took
        }
    }

    /** Lets go of the Earth cubes a pack's build kept, whatever state the build, the pack or the Earth is in. */
    private void releaseCubes(MinecraftServer server, UUID packId) {
        if (cubeHolds.remove(packId) == null) return;
        Earth.release(packId);
        BloodFeud.LOG.info("Blood Feud: let go of the Earth's cubes kept for a build");
    }

    /** Lets go of holds nothing needs any more: no job or ticket wants them, or their dimension has gone. */
    private void sweepHolds(MinecraftServer server) {
        if (cubeHolds.isEmpty()) return;
        for (UUID id : new ArrayList<>(cubeHolds.keySet())) {
            ResourceKey<Level> dimension = cubeHolds.get(id);
            boolean gone = server == null || server.getLevel(dimension) == null;
            if (gone || (!jobs.containsKey(id) && !heldTickets.containsKey(id))) releaseCubes(server, id);
        }
    }

    /** Whether a site search on the Earth is under way for this pack. For tests and checks. */
    public boolean searching(UUID packId) {
        return searches.containsKey(packId);
    }

    /** How many packs have Earth cubes kept for their builds. For tests and checks. */
    public int cubeHolds() {
        return cubeHolds.size();
    }

    /** Whether this mob is building a settlement right now. */
    public boolean isBuilder(UUID mob) {
        return builderPack.containsKey(mob);
    }

    /** How many builders a pack sends for a settlement of this tier: up to its grunts, at least one. */
    public static int crewSize(Pack pack, int toTier) {
        int most = switch (SettlementTier.byLevel(toTier)) {
            case TOWN -> 6;
            case STRONGHOLD -> 5;
            case OUTPOST -> 4;
            default -> 3;
        };
        return Math.max(1, Math.min(most, pack == null ? 1 : pack.grunts));
    }

    /** Blocks a crew of {@code builders} places a second at {@code blocksPerMinute} each. */
    public static double crewRate(int builders, int blocksPerMinute) {
        return Math.max(0, builders) * Math.max(1, blocksPerMinute) / 60.0;
    }

    /**
     * A builder died. When a player killed the last one, the build stalls until the pack sends more and this returns
     * true, so the pack can remember who did it.
     */
    public boolean builderDied(Pack pack, UUID mob, UUID playerKiller, long gameTime) {
        UUID packId = builderPack.remove(mob);
        if (packId == null || pack == null || !packId.equals(pack.id)) return false;
        Crew crew = crews.get(packId);
        if (crew != null) crew.mobs.remove(mob);
        Pack.Works works = pack.works;
        if (works == null || playerKiller == null || (crew != null && !crew.mobs.isEmpty())) return false;
        works.stalledUntil = gameTime + BUILDERS_RETURN_TICKS;
        works.wreckedBy = playerKiller;
        BloodFeud.LOG.info("Blood Feud: {}'s builders were killed; its {} stands {}% built until it sends more",
                pack.title(), SettlementTier.byLevel(works.toTier).key, works.percent());
        return true;
    }

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
        if (server.getPlayerList() == null) return true; // no players at all (a server still starting, or a test)
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
        if (!searches.isEmpty()) return true;
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
                dismissCrew(server, job.packId);
            }
        }
        searches.clear();
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
        crews.clear();
        builderPack.clear();
        searches.clear();
        holdRetryAt.clear();
    }

    /** Sends a pack's builders off: they stay as plain grunts of the pack. */
    private void dismissCrew(MinecraftServer server, UUID packId) {
        Crew crew = crews.remove(packId);
        if (crew == null) return;
        for (UUID mob : crew.mobs.keySet()) {
            builderPack.remove(mob);
            if (server == null) continue;
            for (ServerLevel level : server.getAllLevels()) {
                if (level.getEntity(mob) instanceof Mob m) {
                    releaseCrew(m);
                    m.clearRestriction();
                    break;
                }
            }
        }
    }

    /** Builders carry this tag while they build. */
    public static final String TAG_BUILDER = "bloodfeud.builder";
    private static final String TAG_CREW_LOOT = "bloodfeud.crew_could_pick_up";
    private static final String TAG_CREW_FIREPROOF = "bloodfeud.crew_fireproof";

    /**
     * Readies a mob hired as crew: it drops nothing it wears or carries and picks nothing up while it builds, and one
     * the sun would burn gets fire resistance so it can work through the day. {@link #releaseCrew} undoes what it
     * gave; the drop chances stay at nothing.
     */
    public static void outfitCrew(Mob mob) {
        for (EquipmentSlot slot : EquipmentSlot.values()) mob.setDropChance(slot, 0f);
        if (mob.canPickUpLoot()) mob.addTag(TAG_CREW_LOOT);
        mob.setCanPickUpLoot(false);
        if (sunSensitive(mob) && !mob.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE)) {
            mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE,
                    net.minecraft.world.effect.MobEffectInstance.INFINITE_DURATION, 0, false, false));
            mob.addTag(TAG_CREW_FIREPROOF);
        }
    }

    /** A mob leaves the crew (its build is done, or it rose to a Brute): it picks things up again and can burn again. */
    public static void releaseCrew(Mob mob) {
        mob.removeTag(TAG_BUILDER);
        if (mob.removeTag(TAG_CREW_LOOT)) mob.setCanPickUpLoot(true);
        if (mob.removeTag(TAG_CREW_FIREPROOF)) mob.removeEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE);
        if (mob.removeTag(TAG_BUILDER_HOLDS)) mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    /** Whether the sun sets this mob alight: zombies (not husks or zombified piglins) and skeletons that aren't fireproof. */
    public static boolean sunSensitive(Mob mob) {
        if (mob.fireImmune()) return false;
        if (mob instanceof net.minecraft.world.entity.monster.Zombie) {
            return !(mob instanceof net.minecraft.world.entity.monster.Husk)
                    && !(mob instanceof net.minecraft.world.entity.monster.ZombifiedPiglin);
        }
        return mob instanceof net.minecraft.world.entity.monster.AbstractSkeleton;
    }

    /** A builder is leaving its crew for good (it rose to a Brute). */
    public void leaveCrew(Mob mob) {
        UUID packId = builderPack.remove(mob.getUUID());
        if (packId != null) {
            Crew crew = crews.get(packId);
            if (crew != null) crew.mobs.remove(mob.getUUID());
        }
        releaseCrew(mob);
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
        releaseCubes(server, id);
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
        for (UUID id : new ArrayList<>(cubeHolds.keySet())) releaseCubes(server, id);
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
                    releaseCubes(server, id);
                }
                continue;
            }
            if (ticket.purpose == TicketPurpose.PREP && !BloodFeud.config().buildCamps) {
                it.remove();
                removeTicketFromServerIfUnused(server, ticket);
                releaseCubes(server, id);
                continue;
            }
            Pack pack = data.feud.pack(id);
            boolean hasAbandoned = pack == null && hasAbandonedSite(data.feud, id);
            if (pack == null && !hasAbandoned) {
                it.remove();
                removeTicketFromServerIfUnused(server, ticket);
                releaseCubes(server, id);
                continue;
            }
            if (ticket.purpose == TicketPurpose.REBANNER && (pack == null || !pack.bannersDirty || jobs.containsKey(id))) {
                it.remove();
                removeTicketFromServerIfUnused(server, ticket);
                releaseCubes(server, id);
                continue;
            }
            if (!ticket.crewed && !isAreaClearOfPlayers(server, ticket.dimension.location().toString(),
                    ticket.targetX, ticket.targetZ, ticket.standoffRadius)) {
                it.remove();
                removeTicketFromServerIfUnused(server, ticket);
                releaseCubes(server, id);
                continue;
            }
            long timeout = Earth.is(ticket.dimension) ? EARTH_PREP_TIMEOUT_TICKS : PREP_TIMEOUT_TICKS;
            if (gameTime - ticket.acquiredAtTick >= timeout) {
                it.remove();
                removeTicketFromServerIfUnused(server, ticket);
                releaseCubes(server, id);
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
                    releaseCubes(server, id);
                    continue;
                }
                level.getChunkSource().addRegionTicket(OFFSCREEN_TICKET, ticket.center, ticket.ticketRadius, ticket.center);
                ticket.renewedAtTick = gameTime;
            }
        }
    }

    /**
     * Starts the crewed builds, upgrades and repairs of packs whose sites are loaded ({@link #startCrewedWork}), and
     * schedules off-screen re-bannering, in-place ruinations and abandoned-site crumbling for areas completely clear
     * of players ({@link #isAreaClearOfPlayers}) using brief, bounded off-screen chunk tickets.
     *
     * @param scheduled false on the ticks between scheduled checks, which only look again at work waiting for cubes
     */
    public void checkAutoBuilds(MinecraftServer server, FeudData data, long gameTime, boolean scheduled) {
        Feud feud = data.feud;
        searches.keySet().removeIf(id -> feud.pack(id) == null); // a dead pack's site search stops

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

        // 2. New builds, upgrades and repairs, by the pack's crew in the open; in-place ruinations off-screen
        for (Pack pack : feud.packs.values()) {
            if (jobs.containsKey(pack.id)) continue;
            // A pack still choosing ground for its home (it just withdrew from a fight) builds nothing yet.
            if (pack.settling && !pack.siteSet && pack.works == null) continue;
            HeldTicket existingTicket = heldTickets.get(pack.id);
            if (existingTicket != null && existingTicket.purpose == TicketPurpose.REBANNER) continue;
            if (!scheduled && !isWaitingForCubes(existingTicket) && !searches.containsKey(pack.id)) continue;

            Pack.AutoBuildAction action = autoActionFor(pack);
            if (action != Pack.AutoBuildAction.BUILD && action != Pack.AutoBuildAction.REPAIR) {
                searches.remove(pack.id);
                if (pack.works != null) {
                    abandonWorks(pack); // it fell to ruin (or its build stopped mattering) before its crew finished
                    data.changed();
                }
            }
            if (action == Pack.AutoBuildAction.NONE) {
                if (existingTicket != null) releaseTicket(server, pack.id);
                continue;
            }
            if (action == Pack.AutoBuildAction.BUILD || action == Pack.AutoBuildAction.REPAIR) {
                if (!scheduled && !searches.containsKey(pack.id) && !isWaitingForCubes(existingTicket)) continue;
                if (pack.siteRetryAt > gameTime && pack.works == null) continue;
                startCrewedWork(server, data, pack, action, gameTime);
                continue;
            }
            if (!scheduled && !isWaitingForCubes(existingTicket)) continue;
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
                    EarthSiteSearch.Step step = existingTicket.earthSearch.step(level, pack, feud);
                    if (!step.done()) continue;
                    site = step.site();
                } else {
                    site = SiteFinder.findAutoSite(level, pack, target, feud);
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
            Map<Long, String> sitePlaced = site.placed().isEmpty() ? null : new HashMap<>(site.placed());
            Set<CampBlueprint.Spot> ownedPositions = sitePlaced != null ? new SiteFinder.Owned(sitePlaced)
                    : new SiteFinder.Owned(CampBlueprint.ownedMaterials(site.packId(), site.family(), site.color(),
                            site.vassalColors(), site.siteFacing(), site.builtTier(), site.skippedZones(),
                            site.skippedColumns()), site.family());
            Set<CampBlueprint.Spot> ownedContainers = sitePlaced != null ? ownedPositions
                    : CampBlueprint.containerPositions(site.packId(), site.family(), site.color(), site.vassalColors(),
                            site.siteFacing(), site.builtTier(), site.skippedZones(), site.skippedColumns());
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
            job.placedSink = sitePlaced;
            jobs.put(site.packId(), job);
            acquireOrRenewTicket(server, site.packId(), level, site.siteX(), site.siteZ(),
                    built.radius, built.radius, gameTime, TicketPurpose.BUILD);
        }
    }

    /**
     * Starts (or, after a restart, picks up) a pack's crewed build. Its site is searched for once the ground around the
     * pack's home is loaded: by a player near it, or by a short ticket when none is. A chosen site is checked for player
     * builds once it is loaded: as it is while a player can see it, or loaded for it while nobody can.
     */
    private void startCrewedWork(MinecraftServer server, FeudData data, Pack pack, Pack.AutoBuildAction action,
                                 long gameTime) {
        Feud feud = data.feud;
        ServerLevel level = resolveBuildLevel(server, pack.dimension);
        if (level == null) return;
        SettlementTier target = pack.tier == null ? SettlementTier.CAMP : pack.tier;
        int toTier = Math.max(pack.builtTier, target.level);
        int radius = SettlementTier.byLevel(toTier).radius;

        if (!pack.siteSet) {
            pack.works = null;
            int searchRadius = searchFootprintRadius(target);
            if (!SiteFinder.isFootprintLoaded(level, pack.homeX, pack.homeZ, searchRadius)) {
                // No player is near enough to load it: load it for the search, off screen.
                searches.remove(pack.id);
                boolean held = holdSite(server, level, pack.id, new BlockPos(pack.homeX, 0, pack.homeZ), searchRadius, gameTime,
                        TicketPurpose.PREP, false);
                BloodFeud.LOG.debug("Blood Feud: {}'s home isn't loaded for its site search; {}", pack.title(),
                        held ? "holding it" : "waiting for a slot");
                return;
            }
            SiteFinder.Site site;
            if (Earth.is(level)) {
                EarthSiteSearch search = searches.computeIfAbsent(pack.id,
                        k -> new EarthSiteSearch(level, pack, Math.max(SettlementTier.CAMP.radius, target.radius)));
                EarthSiteSearch.Step step = search.step(level, pack, feud);
                if (!step.done()) return;
                searches.remove(pack.id);
                site = step.site();
            } else {
                site = SiteFinder.findAutoSite(level, pack, target, feud);
            }
            if (site == null) {
                BloodFeud.LOG.debug("Blood Feud: {} found no site near its home this time (next from candidate {})",
                        pack.title(), pack.siteSearchOffset);
                releaseTicket(server, pack.id);
                pack.siteRetryAt = gameTime + RETRY_COOLDOWN_TICKS;
                return;
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
            BloodFeud.LOG.info("Blood Feud: {} chose a site for its {} at {} {} {} in {}", pack.title(),
                    SettlementTier.byLevel(toTier).key, pack.siteX, pack.siteY, pack.siteZ, pack.dimension);
        }

        BlockPos origin = new BlockPos(pack.siteX, pack.siteY, pack.siteZ);
        if (pack.works != null) {
            Pack.Works w = pack.works;
            boolean fits = w.repair == (action == Pack.AutoBuildAction.REPAIR)
                    && w.fromTier == (w.repair ? Math.max(1, pack.builtTier) : pack.builtTier);
            if (fits) {
                resumeWorks(level, feud, pack, gameTime); // its ticks load the site if they need to
                return;
            }
            abandonWorks(pack);
            data.changed();
        }

        // In view or not, what of the site isn't loaded is held loaded for it, and asked for; nothing is written yet.
        if (!SiteFinder.isSiteLoaded(level, origin, radius)
                && !(holdSite(server, level, pack.id, origin, radius, gameTime, TicketPurpose.PREP, true)
                        && SiteFinder.isSiteReady(level, origin, radius))) {
            return;
        }

        if (pack.builtTier == 0 && !pack.buildStarted
                && SiteFinder.hasPlayerBuild(level, origin, radius, Set.of(), Set.of())) {
            releaseTicket(server, pack.id);
            pack.siteRetryAt = gameTime + RETRY_COOLDOWN_TICKS;
            pack.siteSet = false;
            pack.skippedZones.clear();
            pack.skippedColumns.clear();
            data.changed();
            return;
        }
        QueueResult res = action == Pack.AutoBuildAction.REPAIR
                ? queueRepair(level, feud, pack, toTier, true, gameTime, null)
                : queueBuild(level, feud, pack, pack.builtTier, target.level, true, gameTime, null);
        if (res.queued()) {
            data.changed();
            return;
        }
        releaseTicket(server, pack.id);
        pack.siteRetryAt = gameTime + RETRY_COOLDOWN_TICKS;
        if (pack.builtTier == 0 && !pack.buildStarted) {
            pack.siteSet = false;
            pack.skippedZones.clear();
            pack.skippedColumns.clear();
            data.changed();
        }
    }

    /**
     * Lets a pack's unfinished build go. One rule: an upgrade or repair that put blocks down leaves an intact settlement
     * of its target tier on record (built to it, not ruined), so the next ruin or repair covers everything it put up. A
     * first camp keeps {@code buildStarted} instead, so its ruin covers the whole footprint, as before.
     */
    public static void abandonWorks(Pack pack) {
        Pack.Works w = pack.works;
        if (w == null) return;
        pack.works = null;
        if (w.placedCount > 0 && (w.fromTier > 0 || w.repair)) {
            pack.builtTier = Math.max(pack.builtTier, w.toTier);
            pack.builtRuined = false;
        }
    }

    /** What of a pack's site is its own, which containers there are its stashes, and where its new blocks are noted. */
    private record Ownership(Set<CampBlueprint.Spot> positions, Set<CampBlueprint.Spot> containers, Map<Long, String> sink) {
    }

    /**
     * Exact (see {@link SiteFinder.Owned}) for a settlement with a record of what it placed, or none built yet: a
     * position is the pack's only while the block it put there still stands. A settlement built before there was a
     * record goes by its blueprint, and goes on without one.
     */
    private static Ownership ownership(Pack pack, java.util.function.Supplier<Map<CampBlueprint.Spot, Set<CampBlueprint.Material>>> blueprint,
                                       java.util.function.Supplier<Set<CampBlueprint.Spot>> blueprintContainers) {
        if (!pack.placed.isEmpty() || pack.builtTier == 0) {
            SiteFinder.Owned exact = new SiteFinder.Owned(pack.placed);
            return new Ownership(exact, exact, pack.placed);
        }
        return new Ownership(new SiteFinder.Owned(blueprint.get(), pack.family), blueprintContainers.get(), null);
    }

    /** Saves a new automatic job as the pack's works, for its crew to carry out. */
    private static void crewUp(BuildJob job, Pack pack, long gameTime) {
        Pack.Works w = new Pack.Works(job.mode == Mode.REPAIR, job.fromTier, job.toTier, job.packColor,
                job.vassalColors, job.blockedZones, job.blockedCols);
        w.total = job.ops.size();
        pack.works = w;
        job.works = w;
        BloodFeud.LOG.info("Blood Feud: {} began {} its {} at {} {} {} ({} steps)", pack.title(),
                w.repair ? "rebuilding" : "building", SettlementTier.byLevel(w.toTier).key,
                job.origin.getX(), job.origin.getY(), job.origin.getZ(), w.total);
    }

    /** The steps of a pack's saved works: the same ones, in the same order, as when its build began. */
    static List<CampBlueprint.Placement> worksSteps(Pack pack, Pack.Works w) {
        CampBlueprint.Plan plan = w.repair
                ? CampBlueprint.repairPlan(pack.id, pack.family, w.color, w.vassalColors, pack.siteFacing, w.fromTier, w.toTier)
                : CampBlueprint.plan(pack.id, pack.family, w.color, w.vassalColors, pack.siteFacing, w.fromTier, w.toTier);
        plan = CampBlueprint.withoutSkipped(plan, pack.skippedZones, pack.skippedColumns);
        plan = CampBlueprint.withoutSkipped(plan, w.skipZones, w.skipColumns);
        return crewOrder(plan.placements());
    }

    /**
     * The order a crew builds in: first it clears the site, top down, so every block after is one a builder can see
     * and reach; then the blueprint's own order (ground, then walls and roofs bottom up, the stash and banners last).
     * The same steps in the same order every time, so a saved build resumes at the right one.
     */
    public static List<CampBlueprint.Placement> crewOrder(List<CampBlueprint.Placement> ops) {
        List<CampBlueprint.Placement> clear = new ArrayList<>();
        List<CampBlueprint.Placement> rest = new ArrayList<>(ops.size());
        for (CampBlueprint.Placement op : ops) (op.material() == CampBlueprint.Material.AIR ? clear : rest).add(op);
        clear.sort(java.util.Comparator.comparingInt(CampBlueprint.Placement::ry).reversed()
                .thenComparingInt(CampBlueprint.Placement::rx).thenComparingInt(CampBlueprint.Placement::rz));
        clear.addAll(rest);
        return List.copyOf(clear);
    }

    /** Picks up a pack's saved works (after a restart): the same steps, from where its crew stopped. */
    private void resumeWorks(ServerLevel level, Feud feud, Pack pack, long gameTime) {
        Pack.Works w = pack.works;
        List<CampBlueprint.Placement> ops = worksSteps(pack, w);
        Ownership own = ownership(pack,
                () -> w.repair
                        ? CampBlueprint.ownedRuinedMaterials(pack.id, pack.family, w.color, w.vassalColors, pack.siteFacing,
                                w.fromTier, pack.skippedZones, pack.skippedColumns)
                        : CampBlueprint.ownedMaterials(pack.id, pack.family, w.color, w.vassalColors, pack.siteFacing,
                                w.fromTier, pack.skippedZones, pack.skippedColumns),
                () -> CampBlueprint.containerPositions(pack.id, pack.family, w.color, w.vassalColors, pack.siteFacing,
                        w.fromTier, pack.skippedZones, pack.skippedColumns));
        Set<CampBlueprint.Spot> ownedPositions = own.positions();
        Set<CampBlueprint.Spot> ownedContainers = own.containers();
        BuildJob job = new BuildJob(pack.id, level.dimension(), new BlockPos(pack.siteX, pack.siteY, pack.siteZ),
                pack.siteFacing, pack.family, w.color, pack.title(), w.fromTier, w.toTier,
                w.repair ? Mode.REPAIR : Mode.BUILD, true, true, ops, ownedPositions, ownedContainers,
                w.vassalColors, pack.skippedZones, pack.skippedColumns, gameTime, null);
        job.blockedZones = w.skipZones;
        job.blockedCols = w.skipColumns;
        job.works = w;
        job.placedSink = own.sink();
        job.index = Math.min(w.done, ops.size());
        w.total = ops.size();
        jobs.put(pack.id, job);
        BloodFeud.LOG.info("Blood Feud: {} picked up {} its {} at {}% ({} of {} steps)", pack.title(),
                w.repair ? "rebuilding" : "building", SettlementTier.byLevel(w.toTier).key, w.percent(), job.index, w.total);
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
        Ownership own = ownership(pack,
                () -> CampBlueprint.ownedMaterials(pack.id, pack.family, displayColor, vassalColors, pack.siteFacing,
                        ownedTier, pack.skippedZones, pack.skippedColumns),
                () -> CampBlueprint.containerPositions(pack.id, pack.family, displayColor, vassalColors, pack.siteFacing,
                        ownedTier, pack.skippedZones, pack.skippedColumns));
        Set<CampBlueprint.Spot> ownedPositions = own.positions();
        Set<CampBlueprint.Spot> ownedContainers = own.containers();

        boolean shouldProtect = automatic || from > 0 || pack.buildStarted;
        if (shouldProtect && SiteFinder.isWaitingForGround(level, origin, SettlementTier.byLevel(to).radius)) {
            return new QueueResult(false, List.of(), 0);
        }
        FilterResult filtered = shouldProtect
                ? filterPlan(level, origin, plan, from > 0 ? Set.of() : null, ownedPositions, ownedContainers,
                        from > 0 ? SiteFinder.heldByOthers(feud, pack) : List.of(), null) // an upgrade keeps off neighbours
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
                pack.title(), from, to, Mode.BUILD, automatic, shouldProtect,
                automatic ? crewOrder(filtered.ops()) : filtered.ops(),
                ownedPositions, ownedContainers, vassalColors, pack.skippedZones, pack.skippedColumns,
                gameTime, onComplete);
        job.blockedZones = filtered.blockedZones();
        job.blockedCols = filtered.blockedRingCols();
        job.placedSink = own.sink();
        if (!automatic) releaseTicket(level.getServer(), pack.id);
        if (automatic) crewUp(job, pack, gameTime);
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

        int ruinTier = inPlace ? pack.builtTier : to;
        boolean blueprint = inPlace || pack.buildStarted;
        List<Integer> vassals = vassalColors;
        Ownership own = ownership(pack,
                () -> !blueprint ? Map.of() : inPlace
                        ? CampBlueprint.ownedMaterials(pack.id, pack.family, displayColor, vassals, pack.siteFacing,
                                ruinTier, pack.skippedZones, pack.skippedColumns)
                        : CampBlueprint.ownedRuinedMaterials(pack.id, pack.family, displayColor, vassals,
                                pack.siteFacing, ruinTier, pack.skippedZones, pack.skippedColumns),
                () -> !blueprint ? Set.of() : CampBlueprint.containerPositions(pack.id, pack.family, displayColor,
                        vassals, pack.siteFacing, ruinTier, pack.skippedZones, pack.skippedColumns));
        Set<CampBlueprint.Spot> ownedPositions = own.positions();
        Set<CampBlueprint.Spot> ownedContainers = own.containers();

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
        job.placedSink = own.sink();
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

        Ownership own = ownership(pack,
                () -> CampBlueprint.ownedRuinedMaterials(pack.id, pack.family, displayColor, vassalColors,
                        pack.siteFacing, built, pack.skippedZones, pack.skippedColumns),
                () -> CampBlueprint.containerPositions(pack.id, pack.family, displayColor, vassalColors,
                        pack.siteFacing, built, pack.skippedZones, pack.skippedColumns));
        Set<CampBlueprint.Spot> ownedPositions = own.positions();
        Set<CampBlueprint.Spot> ownedContainers = own.containers();

        Set<String> existingZones = (to > built)
                ? CampBlueprint.plan(pack.id, pack.family, displayColor, vassalColors, pack.siteFacing, 0, built).zones().keySet()
                : null;
        FilterResult filtered = filterPlan(level, origin, plan, existingZones, ownedPositions, ownedContainers,
                to > built ? SiteFinder.heldByOthers(feud, pack) : List.of(), existingZones);
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
                pack.title(), built, to, Mode.REPAIR, automatic, true,
                automatic ? crewOrder(filtered.ops()) : filtered.ops(),
                ownedPositions, ownedContainers, vassalColors, pack.skippedZones, pack.skippedColumns,
                gameTime, onComplete);
        job.blockedZones = filtered.blockedZones();
        job.blockedCols = filtered.blockedRingCols();
        job.placedSink = own.sink();
        if (!automatic) releaseTicket(level.getServer(), pack.id);
        if (automatic) crewUp(job, pack, gameTime);
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
        return filterPlan(level, origin, plan, abyssExcludeZones, ownedPositions, ownedContainers, List.of(), null);
    }

    /**
     * As above, also leaving out every zone and ring column that would reach into {@code held} ground (another pack's
     * reservation), except the zones in {@code builtZones}, which already stand.
     */
    private static FilterResult filterPlan(ServerLevel level, BlockPos origin, CampBlueprint.Plan plan,
                                           Set<String> abyssExcludeZones,
                                           Set<CampBlueprint.Spot> ownedPositions,
                                           Set<CampBlueprint.Spot> ownedContainers,
                                           List<int[]> held, Set<String> builtZones) {
        List<CampBlueprint.Placement> ops = plan.placements();
        Set<String> blockedZones = new HashSet<>();
        List<String> skippedZones = new ArrayList<>();
        int skippedRingColumns = 0;

        for (Map.Entry<String, CampBlueprint.Box> entry : plan.zones().entrySet()) {
            String zone = entry.getKey();
            if (CampBlueprint.isRingZone(zone)) continue;
            CampBlueprint.Box box = entry.getValue();
            if (!held.isEmpty() && (builtZones == null || !builtZones.contains(zone))
                    && SiteFinder.overlapsAny(held, origin.getX() + box.minX(), origin.getZ() + box.minZ(),
                            origin.getX() + box.maxX(), origin.getZ() + box.maxZ())) {
                blockedZones.add(zone);
                skippedZones.add(zone + " (a neighbour's ground)");
                continue;
            }
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
                boolean blocked = !held.isEmpty() && SiteFinder.overlapsAny(held, origin.getX() + rx - 1,
                        origin.getZ() + rz - 1, origin.getX() + rx + 1, origin.getZ() + rz + 1);
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
        stepBudget = OFFSCREEN_OPS_PER_TICK;
        rotateSlots(server, gameTime);
        sweepHolds(server);
        if (jobs.isEmpty()) return;

        int manualRemaining = OPS_PER_TICK;
        List<Runnable> completedCallbacks = null;

        for (Iterator<Map.Entry<UUID, BuildJob>> it = jobs.entrySet().iterator(); it.hasNext(); ) {
            BuildJob job = it.next().getValue();
            if (job.automatic && !BloodFeud.config().buildCamps) {
                it.remove();
                releaseTicket(server, job.packId);
                dismissCrew(server, job.packId);
                continue;
            }
            Pack pack = null;
            if (job.mode != Mode.ABANDONED_RUIN) {
                pack = data.feud.pack(job.packId);
                if (pack == null) {
                    it.remove();
                    releaseTicket(server, job.packId);
                    dismissCrew(server, job.packId);
                    // Something to crumble only if the camp really put blocks down here.
                    boolean placedSomething = job.placedSink != null ? !job.placedSink.isEmpty() : job.index > 0;
                    if (placedSomething && job.mode != Mode.RUIN) {
                        int coverTier = Math.max(job.fromTier, job.toTier);
                        if (coverTier > 0) {
                            data.feud.recordAbandonedSite(new Feud.AbandonedSite(
                                    job.packId, job.family, job.packColor, job.dimension.location().toString(),
                                    job.origin.getX(), job.origin.getY(), job.origin.getZ(),
                                    job.siteFacing, coverTier,
                                    job.vassalColors, job.skippedZones, job.skippedColumns,
                                    job.placedSink == null ? Map.of() : job.placedSink));
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
                dismissCrew(server, job.packId);
                continue;
            }

            if (job.works != null && pack != null) {
                Pack.AutoBuildAction wanted = pack.autoBuildAction();
                boolean stillWanted = job.mode == Mode.REPAIR ? wanted == Pack.AutoBuildAction.REPAIR
                        : wanted == Pack.AutoBuildAction.BUILD;
                if (pack.works != job.works || !stillWanted) { // it fell to ruin before its crew finished, or moved on
                    it.remove();
                    releaseTicket(server, job.packId);
                    dismissCrew(server, job.packId);
                    if (pack.works == job.works) {
                        abandonWorks(pack);
                        data.changed();
                    }
                    continue;
                }
                boolean finished;
                try {
                    finished = tickCrew(server, level, data, job, pack, gameTime);
                } catch (RuntimeException e) {
                    // Whatever went wrong, the build lets go of everything it held; its saved progress stays.
                    BloodFeud.LOG.error("Blood Feud: {}'s build failed; it lets go of its site and tries again later", pack.title(), e);
                    it.remove();
                    releaseTicket(server, job.packId);
                    dismissCrew(server, job.packId);
                    holdRetryAt.put(job.packId, gameTime + RETRY_COOLDOWN_TICKS);
                    continue;
                }
                if (finished) {
                    it.remove();
                    releaseTicket(server, job.packId);
                    dismissCrew(server, job.packId);
                    pack.works = null;
                    pack.builtTier = Math.max(pack.builtTier, job.toTier);
                    pack.builtRuined = false;
                    pack.buildStarted = false;
                    data.changed();
                    BloodFeud.LOG.info("Blood Feud: {} finished {} its {} at {} {} {}", pack.title(),
                            job.mode == Mode.REPAIR ? "rebuilding" : "building", SettlementTier.byLevel(job.toTier).key,
                            job.origin.getX(), job.origin.getY(), job.origin.getZ());
                }
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

            int jobBudget = job.automatic ? stepBudget : Math.min(manualRemaining, stepBudget);
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
            stepBudget -= opsDone;
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

    /**
     * One tick of a crewed build. Returns true once every step is done.
     * <p>
     * Holds: while a build is active it holds its site every tick ({@link #holdSite}), whatever is loaded already, so
     * no place a player stands can freeze it. Only ground that won't load times out (see {@link #rotateSlots}).
     * <p>
     * While a player can see the site ({@link Sight}), its crew places each step a builder can reach and see; a step no
     * builder can place safely is left out if nobody could ever see its spot, and otherwise deferred. While nobody can
     * see it, it is built off screen ({@link #buildOffscreen}): deferred steps first, in plan order, then the rest.
     * Once only deferred steps are left and someone can see the site, it waits for them to look away, holding nothing.
     * After a player killed the last builders, nothing is built either way.
     */
    private boolean tickCrew(MinecraftServer server, ServerLevel level, FeudData data, BuildJob job, Pack pack, long gameTime) {
        Pack.Works w = job.works;
        Crew crew = crews.computeIfAbsent(pack.id, k -> new Crew());
        int radius = SettlementTier.byLevel(Math.max(job.fromTier, job.toTier)).radius;
        FamilyPalette palette = FamilyPalette.of(job.family);
        boolean mainDone = job.index >= job.ops.size();
        if (mainDone && w.deferred.isEmpty()) return true;
        boolean seen = sight.seen(server, level, job.origin, radius);
        if (gameTime < w.stalledUntil || (mainDone && seen)) {
            // Waiting: for the pack to send builders, or for everyone to look away from what's left. It holds nothing.
            if (holding(pack.id)) releaseTicket(server, pack.id);
            job.terrainWaitSince = -1;
            crew.budget = 0;
            return false;
        }
        if (!seen && !crew.offscreen) {
            crew.offscreen = true;
            BloodFeud.LOG.info("Blood Feud: nobody can see {}'s site; building its {} off screen from step {} of {} ({} deferred)",
                    pack.title(), SettlementTier.byLevel(w.toTier).key, job.index, job.ops.size(), w.deferred.size());
        } else if (seen && crew.offscreen) {
            crew.offscreen = false;
            BloodFeud.LOG.info("Blood Feud: {}'s site came into view; its crew carries on from step {} of {}",
                    pack.title(), job.index, job.ops.size());
        }
        boolean held = holdSite(server, level, pack.id, job.origin, radius, gameTime, TicketPurpose.BUILD);
        boolean ready = held ? SiteFinder.isSiteReady(level, job.origin, radius) // asks for what is missing; never writes
                : SiteFinder.isSiteLoaded(level, job.origin, radius);
        // "The ground won't load" is only ever counted while the build holds a slot for it.
        if (!held || ready) job.terrainWaitSince = -1;
        else if (job.terrainWaitSince < 0) job.terrainWaitSince = gameTime;
        if (!seen) {
            crew.loadedSince = -1;
            crew.budget = 0;
            crew.mobs.replaceAll((mob, at) -> gameTime); // unloaded with the site, not lost: they come back with it
            return ready && buildOffscreen(level, data, job, pack, palette, gameTime);
        }
        return buildInView(level, data, job, pack, crew, radius, palette, gameTime);
    }

    /** The crew's half of {@link #tickCrew}: one step at a time, each placed by a builder that can reach it. */
    private boolean buildInView(ServerLevel level, FeudData data, BuildJob job, Pack pack, Crew crew, int radius,
                                FamilyPalette palette, long gameTime) {
        Pack.Works w = job.works;
        int perMinute = BloodFeud.config().campBlocksPerMinute;
        boolean originLoaded = SiteFinder.isChunkReady(level, job.origin.getX() >> 4, job.origin.getZ() >> 4)
                && Earth.isLoaded(level, job.origin);
        if (!originLoaded) crew.loadedSince = -1;
        else if (crew.loadedSince < 0) crew.loadedSince = gameTime;
        boolean mobsMove = originLoaded && level.isPositionEntityTicking(job.origin);
        int before = job.index;

        // Pass over steps that need nothing placed, out of the shared step budget.
        while (stepBudget > 0 && job.index < job.ops.size()) {
            CampBlueprint.Placement op = job.ops.get(job.index);
            BlockPos pos = job.origin.offset(op.rx(), op.ry(), op.rz());
            if (!SiteFinder.isChunkReady(level, pos.getX() >> 4, pos.getZ() >> 4) || !Earth.isLoaded(level, pos)) break;
            if (!isNoOp(level, pos, op, palette, job)) break;
            job.index++;
            stepBudget--;
        }
        if (job.index >= job.ops.size()) {
            progressed(data, job, pack, w, gameTime, before);
            return w.deferred.isEmpty();
        }

        List<Mob> builders = builders(level, crew, gameTime);
        int want = crewSize(pack, w.toTier);
        // Builders can't walk where mobs don't tick (past the simulation distance), or exist in peaceful: the build
        // still goes on at the crew's pace, without them.
        boolean unseen = hiring == null || !mobsMove || crew.failedHires >= MAX_FAILED_HIRES
                || level.getDifficulty() == Difficulty.PEACEFUL;
        if (!unseen && builders.size() < want && gameTime - crew.loadedSince >= SETTLE_TICKS && gameTime >= crew.nextHire) {
            crew.nextHire = gameTime + HIRE_INTERVAL_TICKS;
            Mob hired = hiring.hire(level, pack, job.origin, radius);
            if (hired != null && hired.isAlive()) {
                hired.addTag(TAG_BUILDER);
                hired.restrictTo(job.origin, radius + 12);
                crew.mobs.put(hired.getUUID(), gameTime);
                builderPack.put(hired.getUUID(), pack.id);
                builders.add(hired);
                crew.failedHires = 0;
            } else {
                crew.failedHires++;
            }
        }
        if (!crew.logged && !builders.isEmpty()) {
            crew.logged = true;
            BloodFeud.LOG.info("Blood Feud: {} has {} builders at its {} ({}%)", pack.title(), builders.size(),
                    SettlementTier.byLevel(w.toTier).key, w.percent());
        }
        List<Mob> idle = new ArrayList<>();
        for (Mob m : builders) {
            keepWorking(m);
            if (!fighting(m)) idle.add(m);
        }
        int workers = unseen ? want : idle.size();
        if (gameTime - crew.lastReport >= 600) {
            if (crew.reportedAt == job.index) {
                CampBlueprint.Placement next = job.ops.get(job.index);
                BloodFeud.LOG.debug("Blood Feud: {}'s build is waiting at step {} of {} ({} at {}): {} builders, {} fighting, {} unseen",
                        pack.title(), job.index, job.ops.size(), next.material(),
                        job.origin.offset(next.rx(), next.ry(), next.rz()).toShortString(),
                        builders.size(), builders.size() - idle.size(), unseen);
            }
            crew.lastReport = gameTime;
            crew.reportedAt = job.index;
        }
        if (workers == 0) {
            progressed(data, job, pack, w, gameTime, before);
            return false;
        }

        crew.budget = Math.min(crew.budget + crewRate(workers, perMinute) / 20.0, 3.0);
        if (!unseen && gameTime % 10 == 0) steer(idle, job);
        while (crew.budget >= 1.0 && stepBudget > 0 && job.index < job.ops.size()) {
            CampBlueprint.Placement op = job.ops.get(job.index);
            BlockPos pos = job.origin.offset(op.rx(), op.ry(), op.rz());
            if (!SiteFinder.isChunkReady(level, pos.getX() >> 4, pos.getZ() >> 4) || !Earth.isLoaded(level, pos)) break;
            if (isNoOp(level, pos, op, palette, job)) {
                job.index++;
                stepBudget--;
                continue;
            }
            if (op.material().pass == CampBlueprint.PASS_DECOR
                    && !palette.resolve(op, job.packColor).canSurvive(level, pos)) {
                // What it hangs on or stands on isn't there yet (deferred, or taken): it goes up off screen, after it.
                w.deferred.add(job.index);
                job.index++;
                stepBudget--;
                continue;
            }
            Mob worker = unseen ? null : nearest(idle, pos);
            if (worker != null && !canReach(level, worker.getEyePosition(), pos, worker)) {
                if (crew.vantageFor != job.index) {
                    crew.vantageFor = job.index;
                    crew.vantage = vantage(level, pos, worker.position(), worker.getEyeHeight());
                    crew.waitingSince = gameTime;
                    BloodFeud.LOG.debug("Blood Feud: {}'s nearest builder is {} blocks from step {} ({} at {}); {}",
                            pack.title(), String.format("%.1f", Math.sqrt(worker.distanceToSqr(Vec3.atCenterOf(pos)))),
                            job.index, op.material(), pos.toShortString(),
                            crew.vantage == null ? "nowhere to reach it from" : "it walks to " + crew.vantage.toShortString());
                }
                if (crew.vantage == null) {
                    leaveOut(level, job, w, op, pos, pack, "no builder can reach it from anywhere");
                    continue;
                }
                if (gameTime - crew.waitingSince < PLACE_ANYWAY_TICKS) {
                    if (gameTime % 10 == 0) {
                        worker.getNavigation().moveTo(crew.vantage.getX() + 0.5, crew.vantage.getY(), crew.vantage.getZ() + 0.5, 1.0);
                    }
                    break;
                }
                // It couldn't walk there. It is moved there only if that is safe right now; never into a wall or the void.
                crew.vantageFor = -1;
                if (!safeToStand(level, crew.vantage, worker)) {
                    leaveOut(level, job, w, op, pos, pack, "the spot to reach it from isn't safe to stand on");
                    continue;
                }
                worker.getNavigation().stop();
                worker.teleportTo(crew.vantage.getX() + 0.5, crew.vantage.getY(), crew.vantage.getZ() + 0.5);
                if (!canReach(level, worker.getEyePosition(), pos, worker)) {
                    leaveOut(level, job, w, op, pos, pack, "something is in the way");
                    continue;
                }
            }
            crew.waitingSince = -1;
            if (placeInView(level, pos, op, palette, job, worker)) w.deferred.add(job.index); // fill too deep to reach
            job.index++;
            stepBudget--;
            crew.budget -= 1.0;
        }
        progressed(data, job, pack, w, gameTime, before);
        return job.index >= job.ops.size() && w.deferred.isEmpty();
    }

    /**
     * Builds off screen on a tick where the whole site is loaded, as many steps as the shared per-tick budget has left
     * ({@value #OFFSCREEN_OPS_PER_TICK} a tick for every build together): first the steps its crew deferred, in plan
     * order, then the rest. The same player-block checks hold as anywhere.
     */
    private boolean buildOffscreen(ServerLevel level, FeudData data, BuildJob job, Pack pack, FamilyPalette palette,
                                   long gameTime) {
        int before = job.index;
        Pack.Works w = job.works;
        for (Iterator<Integer> it = w.deferred.iterator(); it.hasNext() && stepBudget > 0; ) {
            int step = it.next();
            if (step < 0 || step >= job.ops.size()) {
                it.remove();
                continue;
            }
            CampBlueprint.Placement op = job.ops.get(step);
            BlockPos pos = job.origin.offset(op.rx(), op.ry(), op.rz());
            if (!SiteFinder.isChunkReady(level, pos.getX() >> 4, pos.getZ() >> 4) || !Earth.isLoaded(level, pos)) break;
            applyOp(level, pos, op, palette, job);
            it.remove();
            stepBudget--;
            data.changed();
        }
        while (stepBudget > 0 && job.index < job.ops.size()) {
            CampBlueprint.Placement op = job.ops.get(job.index);
            BlockPos pos = job.origin.offset(op.rx(), op.ry(), op.rz());
            if (!SiteFinder.isChunkReady(level, pos.getX() >> 4, pos.getZ() >> 4) || !Earth.isLoaded(level, pos)) break;
            applyOp(level, pos, op, palette, job);
            job.index++;
            stepBudget--;
        }
        progressed(data, job, pack, w, gameTime, before);
        return job.index >= job.ops.size() && w.deferred.isEmpty();
    }

    /**
     * Holds a build's site loaded for it: a chunk ticket, and on the Earth its cubes (kept where Earth Platform can).
     * False while it has to wait its turn: every slot is taken, or it is backing off because its ground didn't come.
     */
    private boolean holdSite(MinecraftServer server, ServerLevel level, UUID id, BlockPos origin, int radius,
                             long gameTime, TicketPurpose purpose) {
        return holdSite(server, level, id, origin, radius, gameTime, purpose, true);
    }

    private boolean holdSite(MinecraftServer server, ServerLevel level, UUID id, BlockPos origin, int radius,
                             long gameTime, TicketPurpose purpose, boolean cubes) {
        if (gameTime < holdRetryAt.getOrDefault(id, Long.MIN_VALUE)) return false;
        if (!heldTickets.containsKey(id) && heldTickets.size() >= MAX_CONCURRENT_AUTO) {
            slotWantedAt = gameTime;
            return false;
        }
        acquireOrRenewTicket(server, id, level, origin.getX(), origin.getZ(), radius, radius, gameTime, purpose).crewed = true;
        if (cubes) keepCubes(level, id, origin, radius);
        return true;
    }

    private boolean holding(UUID id) {
        return heldTickets.containsKey(id) || cubeHolds.containsKey(id);
    }

    /**
     * Frees held slots fairly. A build whose held ground hasn't all loaded for {@link #PREP_TIMEOUT_TICKS} (longer on
     * the Earth) lets go and backs off for {@link #RETRY_COOLDOWN_TICKS}. Any build that has held a slot for
     * {@link #HOLD_TURN_TICKS} while others wait hands it on. Either goes to the back of the queue.
     */
    private void rotateSlots(MinecraftServer server, long gameTime) {
        List<UUID> requeue = new ArrayList<>();
        for (Map.Entry<UUID, HeldTicket> e : new ArrayList<>(heldTickets.entrySet())) {
            HeldTicket ticket = e.getValue();
            BuildJob job = jobs.get(e.getKey());
            if (!ticket.crewed || job == null || job.works == null) continue;
            long limit = Earth.is(ticket.dimension) ? EARTH_PREP_TIMEOUT_TICKS : PREP_TIMEOUT_TICKS;
            // Only ground that won't load gives the slot up and backs off; a slow or fighting crew just waits its turn.
            if (job.terrainWaitSince >= 0 && gameTime - job.terrainWaitSince >= limit) {
                job.terrainWaitSince = -1;
                BloodFeud.LOG.info("Blood Feud: a build's ground didn't load in {} s; it lets go of its slot and tries again later",
                        limit / 20);
                releaseTicket(server, e.getKey());
                holdRetryAt.put(e.getKey(), gameTime + RETRY_COOLDOWN_TICKS);
                requeue.add(e.getKey());
            } else if (gameTime - slotWantedAt <= 40 && gameTime - ticket.acquiredAtTick >= HOLD_TURN_TICKS) {
                releaseTicket(server, e.getKey());
                holdRetryAt.put(e.getKey(), gameTime + 100);
                requeue.add(e.getKey());
            }
        }
        for (UUID id : requeue) {
            BuildJob job = jobs.remove(id);
            if (job != null) jobs.put(id, job);
        }
    }

    /**
     * A step the crew can't place safely. Left out for good only if nobody could ever see its spot (it is shut in by
     * solid blocks: buried ground, stone behind a tent); otherwise deferred, to be placed off screen.
     */
    private void leaveOut(ServerLevel level, BuildJob job, Pack.Works w, CampBlueprint.Placement op, BlockPos pos,
                                 Pack pack, String why) {
        boolean omit = enclosed(level, pos);
        if (!omit) w.deferred.add(job.index);
        BloodFeud.LOG.debug("Blood Feud: {} {} step {} ({} at {}): {}", pack.title(), omit ? "leaves out" : "defers",
                job.index, op.material(), pos.toShortString(), why);
        job.index++;
        stepBudget--;
    }

    /** Whether every side of {@code pos} is a loaded, opaque full block: nobody can see it from anywhere. */
    public static boolean enclosed(ServerLevel level, BlockPos pos) {
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            BlockPos n = pos.relative(d);
            if (!SiteFinder.isChunkReady(level, n.getX() >> 4, n.getZ() >> 4) || !Earth.isLoaded(level, n)) return false;
            if (!level.getBlockState(n).isSolidRender(level, n)) return false; // glass or a fence lets it be seen
        }
        return true;
    }

    /**
     * Whether a mob {@code width} wide and {@code height} tall can stand at {@code at} right now: the block under it is
     * loaded and solid on top, and nothing loaded or solid is where its body would be.
     */
    public static boolean safeToStand(ServerLevel level, BlockPos at, float width, float height) {
        BlockPos below = at.below();
        if (!SiteFinder.isChunkReady(level, at.getX() >> 4, at.getZ() >> 4) || !Earth.isLoaded(level, below)) return false;
        if (!level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) return false;
        net.minecraft.world.phys.AABB body = new net.minecraft.world.phys.AABB(at.getX() + 0.5 - width / 2, at.getY(),
                at.getZ() + 0.5 - width / 2, at.getX() + 0.5 + width / 2, at.getY() + height, at.getZ() + 0.5 + width / 2);
        // Every block whose collision shape could reach into the body: its own cells, their neighbours, and the row
        // under its feet (a fence or wall stands 1.5 tall).
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(body.minX - 1, body.minY - 1, body.minZ - 1),
                BlockPos.containing(body.maxX + 1, body.maxY, body.maxZ + 1))) {
            if (!SiteFinder.isChunkReady(level, p.getX() >> 4, p.getZ() >> 4) || !Earth.isLoaded(level, p)) {
                if (p.getX() >= Math.floor(body.minX) && p.getX() < body.maxX && p.getZ() >= Math.floor(body.minZ)
                        && p.getZ() < body.maxZ && p.getY() >= at.getY() - 1) {
                    return false; // part of where it would stand isn't loaded
                }
                continue;
            }
            for (net.minecraft.world.phys.AABB part : level.getBlockState(p).getCollisionShape(level, p).toAabbs()) {
                if (part.move(p).intersects(body)) return false;
            }
        }
        return true;
    }

    /** As above with the mob's own box, also asking the level (world border, anything else solid in the way). */
    public static boolean safeToStand(ServerLevel level, BlockPos at, Mob mob) {
        if (!safeToStand(level, at, mob.getBbWidth(), mob.getBbHeight())) return false;
        return level.noCollision(mob, mob.getDimensions(mob.getPose()).makeBoundingBox(Vec3.atBottomCenterOf(at)));
    }

    /**
     * Whether a builder with its eyes at {@code eye} can put a block at {@code pos}, as a player would: within reach, it
     * sees the spot itself, or a face of a neighbour the new block would sit against, with nothing in between.
     */
    public static boolean canReach(net.minecraft.world.level.BlockGetter level, Vec3 eye, BlockPos pos, Entity who) {
        Vec3 centre = Vec3.atCenterOf(pos);
        if (eye.distanceToSqr(centre) > REACH * REACH) return false;
        CollisionContext context = who == null ? CollisionContext.empty() : CollisionContext.of(who);
        if (sees(level, eye, centre, pos, context)) return true;
        for (net.minecraft.core.Direction face : net.minecraft.core.Direction.values()) {
            // Just outside each face of the spot: the side a block placed there would be put against.
            Vec3 aim = centre.add(face.getStepX() * 0.49, face.getStepY() * 0.49, face.getStepZ() * 0.49);
            if (eye.distanceToSqr(aim) <= REACH * REACH && sees(level, eye, aim, pos, context)) return true;
        }
        return false;
    }

    /** Whether the line from the eye to {@code aim} reaches the spot: nothing in the way, or what it meets touches it. */
    private static boolean sees(net.minecraft.world.level.BlockGetter level, Vec3 eye, Vec3 aim, BlockPos pos,
                                CollisionContext context) {
        BlockHitResult hit = level.clip(new ClipContext(eye, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context));
        if (hit.getType() == HitResult.Type.MISS) return true;
        BlockPos at = hit.getBlockPos();
        return at.equals(pos) || at.relative(hit.getDirection()).equals(pos);
    }

    /**
     * Somewhere near {@code pos} a builder can stand (solid below, room for it) and reach the block from, nearest to
     * where it is; or null.
     */
    public static BlockPos vantage(ServerLevel level, BlockPos pos, Vec3 from, float eyeHeight) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                for (int dy = -4; dy <= 3; dy++) {
                    at.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (at.equals(pos) || at.above().equals(pos)) continue;
                    if (!Earth.isLoaded(level, at.below()) || !Earth.isLoaded(level, at.above())) continue;
                    if (!SiteFinder.isChunkReady(level, at.getX() >> 4, at.getZ() >> 4)) continue;
                    if (!level.getBlockState(at.below()).blocksMotion()) continue;
                    if (level.getBlockState(at).blocksMotion() || level.getBlockState(at.above()).blocksMotion()) continue;
                    Vec3 eye = new Vec3(at.getX() + 0.5, at.getY() + eyeHeight, at.getZ() + 0.5);
                    if (!canReach(level, eye, pos, null)) continue;
                    double d = from.distanceToSqr(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                    if (d < bestD) {
                        bestD = d;
                        best = at.immutable();
                    }
                }
            }
        }
        return best;
    }

    private static void progressed(FeudData data, BuildJob job, Pack pack, Pack.Works w, long gameTime, int before) {
        if (job.index != before) job.lastProgressTick = gameTime;
        if (w.done != job.index) {
            w.done = job.index;
            if (pack.builtTier == 0 && !pack.buildStarted && job.index > 0) pack.buildStarted = true;
            data.changed();
        }
    }

    /** The crew's builders that are loaded and alive; ones gone a while are struck off. */
    private List<Mob> builders(ServerLevel level, Crew crew, long gameTime) {
        List<Mob> out = new ArrayList<>();
        for (Iterator<Map.Entry<UUID, Long>> it = crew.mobs.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> e = it.next();
            Entity entity = level.getEntity(e.getKey());
            if (entity instanceof Mob m && m.isAlive() && !m.isRemoved()) {
                e.setValue(gameTime);
                m.skipDropExperience(); // not saved, so set again on a builder that reloaded with its chunk
                out.add(m);
            } else if (gameTime - e.getValue() > LOST_BUILDER_TICKS || (entity != null && !entity.isAlive())) {
                it.remove();
                builderPack.remove(e.getKey());
            }
        }
        return out;
    }

    /**
     * Builders mind their work while a player watches from a distance: they only turn on one who comes within
     * {@value #BUILDER_CALM_RANGE} blocks or hits them.
     */
    private static void keepWorking(Mob m) {
        LivingEntity target = m.getTarget();
        if (!(target instanceof Player p) || !p.isAlive()) return;
        if (m.getLastHurtByMob() == p) return;
        if (m.distanceToSqr(p) <= BUILDER_CALM_RANGE * BUILDER_CALM_RANGE) return;
        m.setTarget(null);
        // Brain mobs (piglin brutes) keep their target in memory, not in the goal selector.
        m.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        m.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        m.getNavigation().stop();
    }

    /** How close a player may come before builders drop their work and fight. */
    private static final double BUILDER_CALM_RANGE = 8.0;

    private static boolean fighting(Mob m) {
        LivingEntity target = m.getTarget();
        return target != null && target.isAlive() && (!(target instanceof Player p) || !p.isSpectator());
    }

    private static Mob nearest(List<Mob> mobs, BlockPos pos) {
        Mob best = null;
        double bestD = Double.MAX_VALUE;
        for (Mob m : mobs) {
            double d = m.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (d < bestD) {
                bestD = d;
                best = m;
            }
        }
        return best;
    }

    /** Spreads idle builders over the next few spots, so the crew works the site together. */
    private static void steer(List<Mob> idle, BuildJob job) {
        for (int i = 0; i < idle.size(); i++) {
            int at = Math.min(job.ops.size() - 1, job.index + i * 6);
            CampBlueprint.Placement op = job.ops.get(at);
            BlockPos pos = job.origin.offset(op.rx(), op.ry(), op.rz());
            Mob m = idle.get(i);
            if (m.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > 9.0) {
                m.getNavigation().moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 1.0);
            }
        }
    }

    /** Places a step's block where everyone can see it: the builder swings, the block makes its sound and dust. */
    private static boolean placeInView(ServerLevel level, BlockPos pos, CampBlueprint.Placement op, FamilyPalette palette,
                                       BuildJob job, Mob worker) {
        BlockState before = level.getBlockState(pos);
        // A builder fills a foundation only as deep as it can reach from where it stands.
        boolean cut = applyOp(level, pos, op, palette, job, worker == null ? Integer.MIN_VALUE : worker.getBlockY() - (int) REACH);
        BlockState after = level.getBlockState(pos);
        BlockPos shown = pos;
        if (after == before && op.material() == CampBlueprint.Material.FOUNDATION_COLUMN) {
            shown = pos.below();
            after = level.getBlockState(shown);
        }
        if (worker != null) {
            worker.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (worker.getMainHandItem().isEmpty() || worker.getTags().contains(TAG_BUILDER_HOLDS)) {
                ItemStack held = after.isAir() ? ItemStack.EMPTY : new ItemStack(after.getBlock().asItem());
                worker.setItemSlot(EquipmentSlot.MAINHAND, held);
                worker.setDropChance(EquipmentSlot.MAINHAND, 0f);
                worker.addTag(TAG_BUILDER_HOLDS);
            }
            worker.swing(InteractionHand.MAIN_HAND);
        }
        if (after.isAir()) {
            if (!before.isAir()) level.levelEvent(2001, pos, Block.getId(before)); // break sound and dust
            return cut;
        }
        SoundType sound = after.getSoundType();
        level.playSound(null, shown, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0f) / 2.0f, sound.getPitch() * 0.8f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, after),
                shown.getX() + 0.5, shown.getY() + 0.5, shown.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.05);
        return cut;
    }

    /** Builders that hold the block they place carry this tag (their hands were empty when hired). */
    private static final String TAG_BUILDER_HOLDS = "bloodfeud.builder_holds";

    /** Whether a step would leave the world as it is: the block is already right, or the spot isn't the camp's to change. */
    static boolean isNoOp(ServerLevel level, BlockPos pos, CampBlueprint.Placement op, FamilyPalette palette, BuildJob job) {
        if (pos.getY() <= Earth.bottom(level) || pos.getY() >= Earth.top(level) - 1) return true;
        BlockState existing = level.getBlockState(pos);
        if (op.material() == CampBlueprint.Material.FOUNDATION_COLUMN) {
            if (existing.blocksMotion() && existing.getFluidState().isEmpty() && !existing.canBeReplaced()) return true;
            return SiteFinder.isIndestructibleBlock(existing) || SiteFinder.isNonEmptyContainer(level, pos, existing)
                    || (job.protectPlayerBlocks && SiteFinder.isPlayerBlockAt(level, job.origin, pos, job.ownedPositions, job.ownedContainers));
        }
        if (SiteFinder.isIndestructibleBlock(existing) || SiteFinder.isNonEmptyContainer(level, pos, existing)) return true;
        if (job.protectPlayerBlocks && SiteFinder.isPlayerBlockAt(level, job.origin, pos, job.ownedPositions, job.ownedContainers)) {
            return true;
        }
        if (op.material().isChest() && existing.getBlock() instanceof AbstractChestBlock<?>) return true;
        if (op.material() == CampBlueprint.Material.BARREL && existing.getBlock() instanceof BarrelBlock) return true;
        BlockState target = palette.resolve(op, job.packColor);
        if (op.material() == CampBlueprint.Material.AIR) {
            return existing.isAir() || existing.getBlock() instanceof AbstractChestBlock<?>
                    || existing.getBlock() instanceof BarrelBlock || SiteFinder.isContainerBlock(level, pos, existing);
        }
        return existing == target;
    }

    /** A crewed build put {@code state} at {@code pos}: from now on that block there is the pack's, after a restart too. */
    private static void placed(BuildJob job, BlockPos pos, BlockState state) {
        if (job.works != null) job.works.placedCount++;
        if (job.placedSink == null) return;
        long at = BlockPos.asLong(pos.getX() - job.origin.getX(), pos.getY() - job.origin.getY(), pos.getZ() - job.origin.getZ());
        if (state.isAir()) job.placedSink.remove(at); // cleared: nothing of the pack's there any more
        else job.placedSink.put(at, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }

    private static void applyOp(ServerLevel level, BlockPos pos, CampBlueprint.Placement op,
                                FamilyPalette palette, BuildJob job) {
        applyOp(level, pos, op, palette, job, Integer.MIN_VALUE);
    }

    /**
     * Applies one step. A foundation column stops above {@code minY} (how deep the builder placing it can reach) and
     * returns true when it had more to fill below that, for the off-screen pass to finish; a column it filled before
     * is passed through, not mistaken for the ground.
     */
    static boolean applyOp(ServerLevel level, BlockPos pos, CampBlueprint.Placement op,
                           FamilyPalette palette, BuildJob job, int minY) {
        if (pos.getY() <= Earth.bottom(level) || pos.getY() >= Earth.top(level) - 1) return false;

        if (op.material() == CampBlueprint.Material.FOUNDATION_COLUMN) {
            BlockState fill = palette.foundation.defaultBlockState();
            String fillId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(palette.foundation).toString();
            BlockPos.MutableBlockPos cur = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
            for (int depth = 0; depth < MAX_FOUNDATION_DEPTH; depth++) {
                if (cur.getY() <= Earth.bottom(level) + 1 || !Earth.isLoaded(level, cur)) break;
                BlockState existing = level.getBlockState(cur);
                if (existing.is(palette.foundation) && job.placedSink != null && fillId.equals(job.placedSink.get(
                        BlockPos.asLong(cur.getX() - job.origin.getX(), cur.getY() - job.origin.getY(), cur.getZ() - job.origin.getZ())))) {
                    cur.move(0, -1, 0); // its own fill from before: go on below it
                    continue;
                }
                if (existing.blocksMotion() && existing.getFluidState().isEmpty() && !existing.canBeReplaced()) {
                    break;
                }
                if (SiteFinder.isIndestructibleBlock(existing) || SiteFinder.isNonEmptyContainer(level, cur, existing)) {
                    break;
                }
                if (job.protectPlayerBlocks && SiteFinder.isPlayerBlockAt(level, job.origin, cur, job.ownedPositions, job.ownedContainers)) {
                    break;
                }
                if (cur.getY() < minY) return true; // deeper than the builder can reach
                level.setBlock(cur, fill, Block.UPDATE_CLIENTS);
                placed(job, cur, fill);
                cur.move(0, -1, 0);
            }
            return false;
        }

        BlockState existing = level.getBlockState(pos);
        if (SiteFinder.isIndestructibleBlock(existing)) return false;
        // NEVER overwrite any non-empty container (even on manual operator builds)
        if (SiteFinder.isNonEmptyContainer(level, pos, existing)) return false;
        if (job.protectPlayerBlocks && SiteFinder.isPlayerBlockAt(level, job.origin, pos, job.ownedPositions, job.ownedContainers)) {
            return false;
        }

        boolean wasChestOrBarrel = existing.getBlock() instanceof AbstractChestBlock<?>
                || existing.getBlock() instanceof BarrelBlock;

        // Preserve existing containers on repair/rebuild so loot is never destroyed or infinitely refilled
        if (op.material().isChest() && existing.getBlock() instanceof AbstractChestBlock<?>) return false;
        if (op.material() == CampBlueprint.Material.BARREL && existing.getBlock() instanceof BarrelBlock) return false;

        BlockState target = palette.resolve(op, job.packColor);
        if (op.material() == CampBlueprint.Material.AIR) {
            // Never destroy containers during teardown/clearing
            if (wasChestOrBarrel || SiteFinder.isContainerBlock(level, pos, existing)) return false;
            if (!existing.isAir() && level.setBlock(pos, target, Block.UPDATE_CLIENTS)) placed(job, pos, target);
            return false;
        }

        if (op.material().pass == CampBlueprint.PASS_DECOR && !target.canSurvive(level, pos)) return false;
        if (level.setBlock(pos, target, Block.UPDATE_ALL)) placed(job, pos, target);

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
        return false;
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

        boolean recorded = !pack.placed.isEmpty(); // an older settlement with no record has no banners it can prove are its own
        for (CampBlueprint.Placement op : banners) {
            if (!recorded) break;
            if (CampBlueprint.isSkipped(op.zone(), op.rx(), op.rz(), pack.skippedZones, pack.skippedColumns)) {
                continue;
            }
            BlockPos pos = origin.offset(op.rx(), op.ry(), op.rz());
            if (!SiteFinder.isChunkReady(level, pos.getX() >> 4, pos.getZ() >> 4) || !Earth.isLoaded(level, pos)) continue;
            BlockState existing = level.getBlockState(pos);
            boolean newVassalBanner = !pack.builtRuined && op.material() == CampBlueprint.Material.VASSAL_BANNER && existing.isAir();
            if (!placedHere(pack, origin, pos, existing) && !newVassalBanner) continue; // only its own banner, as it put it
            if (newVassalBanner) {
                BlockPos below = pos.below();
                if (level.getBlockState(below).isAir()) setNoted(level, pack, origin, below, palette.fence.defaultBlockState());
                BlockPos base = below.below();
                if (level.getBlockState(base).isAir()) setNoted(level, pack, origin, base, palette.pillar.defaultBlockState());
            }
            setNoted(level, pack, origin, pos, palette.resolve(op, displayColor));
        }

        // Take down vassal banners (and their posts) beyond the current vassals: only what the camp put there itself.
        if (built == SettlementTier.TOWN && recorded) {
            List<CampBlueprint.Spot> allVassalSpots = CampBlueprint.vassalBannerSpots(pack.siteFacing);
            for (int i = vassals.size(); i < allVassalSpots.size(); i++) {
                CampBlueprint.Spot s = allVassalSpots.get(i);
                if (CampBlueprint.isSkipped("town_kings_hall", s.rx(), s.rz(), pack.skippedZones, pack.skippedColumns)) {
                    continue;
                }
                BlockPos pos = origin.offset(s.rx(), s.ry(), s.rz());
                if (!SiteFinder.isChunkReady(level, pos.getX() >> 4, pos.getZ() >> 4) || !Earth.isLoaded(level, pos)) continue;
                for (BlockPos part : List.of(pos, pos.below(), pos.below(2))) {
                    if (!Earth.isLoaded(level, part)) continue;
                    BlockState here = level.getBlockState(part);
                    if (!here.isAir() && placedHere(pack, origin, part, here)) {
                        setNoted(level, pack, origin, part, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        return true;
    }

    /** Whether {@code state} at {@code pos} is exactly what the pack's camp put there. */
    private static boolean placedHere(Pack pack, BlockPos origin, BlockPos pos, BlockState state) {
        String mine = pack.placed.get(BlockPos.asLong(pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ()));
        return mine != null && mine.equals(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }

    /** Sets a block for the pack's camp and notes it in its record (cleared to air: no longer the pack's). */
    private static void setNoted(ServerLevel level, Pack pack, BlockPos origin, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_ALL);
        long at = BlockPos.asLong(pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ());
        if (state.isAir()) pack.placed.remove(at);
        else pack.placed.put(at, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
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
