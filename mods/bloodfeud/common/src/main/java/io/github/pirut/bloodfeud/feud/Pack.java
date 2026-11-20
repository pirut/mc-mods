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
    /** Players who have seen it. */
    public final Set<UUID> met = new HashSet<>();

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
