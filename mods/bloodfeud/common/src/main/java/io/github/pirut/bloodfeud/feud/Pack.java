package io.github.pirut.bloodfeud.feud;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** A group of mobs of one family that formed around a Boss, with a home it stays near. */
public final class Pack {
    public final UUID id;
    public final Family family;
    public final String dimension;
    /** The place it formed, for its name: "Mossy Ravine" makes "the Mossy Ravine Pack". */
    public final String place;
    /** Dye color index (0 to 15), worn by every member. */
    public final int color;
    public final int homeX, homeZ;
    public UUID boss;
    /** The King's pack this one kneels to, or null. */
    public UUID liege;
    /** Plain mobs that run with it, on paper. */
    public int grunts;
    public final long formedAt;
    /** The last time it had a night with a player nearby. */
    public long lastAwake;
    public int playerKills;
    /** Peak settlement tier this pack has built/unlocked (remains intact footprint even when ruined). */
    public SettlementTier tier = SettlementTier.CAMP;
    /** Highest tier actually built in the world (0 = not yet built). */
    public int builtTier = 0;
    /** True when the pack's numbers have dropped below its current tier's threshold and the settlement is in ruins. */
    public boolean ruined;
    /** True when the blocks currently built in the world at this site are in the ruined state. */
    public boolean builtRuined;
    /** True once a concrete build site has been chosen in the world. */
    public boolean siteSet;
    public int siteX, siteY, siteZ;
    /** Cardinal facing (0..3) of the settlement's main gate. */
    public int siteFacing;
    /** Server game-time tick before which automatic site search will not retry. */
    public long siteRetryAt;
    /** Cursor into the spiral site-search candidate list so capped attempts make progress across retries. */
    public int siteSearchOffset;
    /** True when the pack's faction banners changed (swallowed, freed, king death) and need world re-bannering. */
    public boolean bannersDirty;
    /** True once a tier-0 build/ruin job has placed at least one block at the site; cleared when builtTier > 0 or site resets. */
    public boolean buildStarted;
    /** Blueprint zones skipped during build/upgrade (player-blocked or abyss), excluded from ownership and future ruin/repair. */
    public final Set<String> skippedZones = new HashSet<>();
    /** Ring-zone relative columns (CampBlueprint.colKey(rx, rz)) skipped during build/upgrade, excluded from ownership and future ruin/repair. */
    public final Set<Long> skippedColumns = new HashSet<>();
    /** Players who have seen it. */
    public final Set<UUID> met = new HashSet<>();
    /** The settlement its builders are putting up right now, or null. Saved, so a build picks up where it stopped. */
    public Works works;
    /**
     * Every block the pack's settlement has put down and is still answerable for: the offset from the site
     * ({@code BlockPos.asLong}) to the block's id. A position is the pack's only while this block still stands there.
     * Empty for settlements built before 0.1.0-beta.2, which fall back to their blueprint.
     */
    public final java.util.Map<Long, String> placed = new java.util.HashMap<>();

    /**
     * A settlement under construction: which plan, and how far the builders got. The plan's steps come from the
     * pack and these fields alone, so the same steps come back after a restart.
     */
    public static final class Works {
        /** True for rebuilding a ruined settlement, false for building a new one or the next tier. */
        public final boolean repair;
        public final int fromTier, toTier;
        /** The color its banners and wool are in, fixed for the whole build. */
        public final int color;
        public final java.util.List<Integer> vassalColors;
        /** Parts left out when the build started (a player's block stood in them), on top of the pack's own. */
        public final Set<String> skipZones;
        public final Set<Long> skipColumns;
        /** Steps done (placed, or passed over because nothing needed doing there). */
        public int done;
        /** Steps in the whole plan, for "40% built". */
        public int total;
        /** Blocks this build has put down, so a cut-short build can tell whether it changed anything. */
        public int placedCount;
        /**
         * Steps its crew couldn't place safely (no builder could reach or see them, or a move there wasn't safe),
         * in plan order. They are placed off screen once nobody can see the site.
         */
        public final java.util.TreeSet<Integer> deferred = new java.util.TreeSet<>();
        /** No builders come before this game time: a player killed the last of them. */
        public long stalledUntil;
        /** The player who killed the last builders, or null. */
        public UUID wreckedBy;

        public Works(boolean repair, int fromTier, int toTier, int color, java.util.List<Integer> vassalColors,
                     Set<String> skipZones, Set<Long> skipColumns) {
            this.repair = repair;
            this.fromTier = fromTier;
            this.toTier = toTier;
            this.color = color;
            this.vassalColors = vassalColors == null ? java.util.List.of() : java.util.List.copyOf(vassalColors);
            this.skipZones = skipZones == null ? new HashSet<>() : new HashSet<>(skipZones);
            this.skipColumns = skipColumns == null ? new HashSet<>() : new HashSet<>(skipColumns);
        }

        /** How far along, 0 to 100. */
        public int percent() {
            return total <= 0 ? 0 : Math.min(100, (int) (100L * done / total));
        }
    }

    /** "Camp", "Stronghold, being built (40%)", "Ruined Camp": the settlement as a player would find it. */
    public String settlementLabel(boolean campsEnabled) {
        String title = displayTierTitle(campsEnabled);
        if (title.isEmpty() || !campsEnabled) return title;
        if (works != null) {
            String what = SettlementTier.byLevel(works.toTier).title(false);
            return what + (works.repair ? ", being rebuilt (" : ", being built (") + works.percent() + "%)";
        }
        if (!ruined && builtTier < (tier == null ? SettlementTier.CAMP : tier).level) {
            return title + " (not built yet)";
        }
        return title;
    }

    /** What happened to the pack, oldest first, for the Feud Ledger. */
    public final java.util.Deque<Event> chronicle = new java.util.ArrayDeque<>();
    public static final int CHRONICLE = 16;

    public record Event(long time, String text) {
    }

    public void remember(long time, String text) {
        chronicle.addLast(new Event(time, text));
        while (chronicle.size() > CHRONICLE) chronicle.removeFirst();
    }

    public Pack(UUID id, Family family, String dimension, String place, int color, int homeX, int homeZ, long formedAt) {
        this.id = id;
        this.family = family;
        this.dimension = dimension;
        this.place = place;
        this.color = color;
        this.homeX = homeX;
        this.homeZ = homeZ;
        this.formedAt = formedAt;
        this.lastAwake = formedAt;
    }

    public int campX() {
        return siteSet ? siteX : homeX;
    }

    public int campZ() {
        return siteSet ? siteZ : homeZ;
    }

    /**
     * Settlement tier to display in UI/commands. When automatic camps are disabled, packs that never built
     * a camp return {@link SettlementTier#NONE} so unbuilt labels are omitted; packs with an existing camp
     * still show their built tier.
     */
    public SettlementTier displayTier(boolean campsEnabled) {
        if (campsEnabled) return tier == null ? SettlementTier.CAMP : tier;
        return builtTier > 0 ? SettlementTier.byLevel(builtTier) : SettlementTier.NONE;
    }

    /** Automatic world-build action needed for this pack based on logical tier/ruin state vs built state. */
    public enum AutoBuildAction { NONE, BUILD, RUIN, REPAIR }

    /**
     * Pure decision helper used by {@code CampBuilder.checkAutoBuilds} to pick the next automatic job.
     * Never requests tier expansion while ruined, and never re-queues ruination once the built site is already ruined.
     */
    public AutoBuildAction autoBuildAction() {
        SettlementTier target = tier == null ? SettlementTier.CAMP : tier;
        if (target == SettlementTier.NONE) return AutoBuildAction.NONE;
        boolean needsTierBuild = !ruined && builtTier < target.level;
        boolean needsRuin = ruined && (builtTier == 0 || !builtRuined);
        boolean needsRepair = !ruined && builtRuined && builtTier > 0;
        if (needsRuin) return AutoBuildAction.RUIN;
        if (needsRepair) return AutoBuildAction.REPAIR;
        if (needsTierBuild) return AutoBuildAction.BUILD;
        return AutoBuildAction.NONE;
    }

    /** Human-readable settlement label (e.g. "Town", "Ruined Town", "Ruined Camp"), or empty when omitted. */
    public String displayTierTitle(boolean campsEnabled) {
        SettlementTier t = displayTier(campsEnabled);
        if (t == SettlementTier.NONE) return "";
        boolean showRuined = campsEnabled ? ruined : (builtTier > 0 && (builtRuined || ruined));
        return t.title(showRuined);
    }

    public String title() {
        return "the " + place + " " + switch (family) {
            case SPIDERS -> "Nest";
            case WITCHES -> "Coven";
            default -> "Pack";
        };
    }

    public double distanceTo(String dim, double x, double z) {
        if (!dimension.equals(dim)) return Double.POSITIVE_INFINITY;
        return Math.hypot(campX() - x, campZ() - z);
    }

    public double distanceTo(Pack other) {
        return distanceTo(other.dimension, other.campX(), other.campZ());
    }
}
