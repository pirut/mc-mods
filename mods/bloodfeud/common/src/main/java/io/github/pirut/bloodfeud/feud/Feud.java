package io.github.pirut.bloodfeud.feud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Every Brute and pack in a world, and the rules for how they rise, band together, fight each other and fall.
 * Pure logic, no game classes, so it can be tested on its own. Things that happen are queued as {@link News}
 * for the game to tell players about ({@link #drainNews}).
 */
public final class Feud {
    public static final long DAY = 24000;
    /** A pack's members stay within this many blocks of its home. */
    public static final int PACK_RANGE = 500;
    /** Packs with homes this close are rivals. */
    public static final int RIVAL_RANGE = 1500;
    /** A King only rules packs this close to its own home. */
    public static final int KING_RANGE = 3000;
    /** Packs and Brutes with no player this close are asleep: they don't grow or fight. */
    public static final int AWAKE_RANGE = 2000;
    /** How far a Brute that is following a player covers in a day, on paper. */
    public static final int TRAVEL_PER_DAY = 2000;
    /** Two lone Brutes this close may meet and form a pack. */
    public static final int MEET_RANGE = 500;
    /** A pack that wakes up after sleeping plays out at most this many of the nights it missed. */
    public static final int CATCH_UP_NIGHTS = 5;
    public static final int MAX_GRUNTS = 24;
    public static final int CLASSIC_MAX_GRUNTS = 16;
    public static final int MAX_MEMBERS = 5;
    /** A Brute that has come back this many times stays dead next time. */
    public static final int MAX_RETURNS = 3;

    public final Map<UUID, Brute> brutes = new LinkedHashMap<>();
    public final Map<UUID, Pack> packs = new LinkedHashMap<>();
    /** The time of the call being played out, for pack chronicles. */
    private long clock;
    public long lastNight;
    /** When false, packs do not automatically advance settlement tiers or emit camp news. */
    public boolean campsEnabled = true;

    /** Maximum grunts per pack: 24 with settlements enabled, classic 16 when disabled. */
    public int maxGrunts() {
        return campsEnabled ? MAX_GRUNTS : CLASSIC_MAX_GRUNTS;
    }
    public int maxPacks = 64;
    public int maxBrutes = 256;

    private final Random random;
    /** Built settlement left behind by a disbanded pack, waiting to be crumbled into ruins in the world. */
    public record AbandonedSite(UUID packId, Family family, int color, String dimension,
                                int siteX, int siteY, int siteZ, int siteFacing, int builtTier,
                                List<Integer> vassalColors, Set<String> skippedZones, Set<Long> skippedColumns) {
        public AbandonedSite {
            vassalColors = vassalColors == null ? List.of() : List.copyOf(vassalColors);
            skippedZones = skippedZones == null ? Set.of() : Set.copyOf(skippedZones);
            skippedColumns = skippedColumns == null ? Set.of() : Set.copyOf(skippedColumns);
        }
    }

    public static final int MAX_ABANDONED_SITES = 64;
    public final List<AbandonedSite> abandonedSites = new ArrayList<>();

    /**
     * Records or updates an abandoned built settlement waiting for automatic ruination.
     * If an entry for the same pack already exists (e.g. from disbanding mid-upgrade), merges the higher tier,
     * vassal banner colors, and skipped-zone sets.
     */
    public void recordAbandonedSite(AbandonedSite site) {
        if (site == null || site.builtTier() <= 0) return;
        for (int i = 0; i < abandonedSites.size(); i++) {
            AbandonedSite cur = abandonedSites.get(i);
            if (cur.packId().equals(site.packId())) {
                int mergedTier = Math.max(cur.builtTier(), site.builtTier());
                List<Integer> mergedVassals = !site.vassalColors().isEmpty() ? site.vassalColors() : cur.vassalColors();
                Set<String> mergedZones = new HashSet<>(cur.skippedZones());
                mergedZones.addAll(site.skippedZones());
                Set<Long> mergedCols = new HashSet<>(cur.skippedColumns());
                mergedCols.addAll(site.skippedColumns());
                abandonedSites.set(i, new AbandonedSite(
                        cur.packId(), cur.family(), cur.color(), cur.dimension(),
                        cur.siteX(), cur.siteY(), cur.siteZ(), cur.siteFacing(), mergedTier,
                        mergedVassals, mergedZones, mergedCols));
                return;
            }
        }
        abandonedSites.add(site);
        while (abandonedSites.size() > MAX_ABANDONED_SITES) abandonedSites.remove(0);
    }

    private final List<News> news = new ArrayList<>();

    /** Where a player is right now. */
    public record PlayerSpot(UUID id, String dimension, int x, int z) {
    }

    /** Names the place a pack forms at. */
    public interface Places {
        String name(Random random, String dimension, int x, int z);
    }

    public Feud(Random random) {
        this.random = random;
    }

    public Random random() {
        return random;
    }

    public List<News> drainNews() {
        List<News> out = new ArrayList<>(news);
        news.clear();
        return out;
    }

    // ---------------------------------------------------------------- looking things up

    public Brute brute(UUID id) {
        return id == null ? null : brutes.get(id);
    }

    public Pack pack(UUID id) {
        return id == null ? null : packs.get(id);
    }

    public Pack packOf(Brute b) {
        return b == null ? null : pack(b.pack);
    }

    /** Living Brutes in a pack, its Boss first. */
    public List<Brute> members(Pack p) {
        List<Brute> out = new ArrayList<>();
        Brute boss = brute(p.boss);
        if (boss != null && boss.alive) out.add(boss);
        for (Brute b : brutes.values()) if (b.alive && p.id.equals(b.pack) && !b.id.equals(p.boss)) out.add(b);
        return out;
    }

    public List<Pack> vassals(Pack p) {
        List<Pack> out = new ArrayList<>();
        for (Pack q : packs.values()) if (p.id.equals(q.liege)) out.add(q);
        return out;
    }

    /** The King's pack at the top of the chain this pack is in (itself if it kneels to no one). */
    public Pack top(Pack p) {
        Pack at = p;
        for (int i = 0; i < 8 && at.liege != null && packs.containsKey(at.liege); i++) at = packs.get(at.liege);
        return at;
    }

    public Rank rank(Brute b) {
        Pack p = packOf(b);
        if (p == null || !b.id.equals(p.boss)) return Rank.BRUTE;
        return vassals(p).isEmpty() ? Rank.BOSS : Rank.KING;
    }

    /** "Brute of the Mossy Ravine Pack", "King of the Rotten of the Bog Pack", "Brute". */
    public String standing(Brute b) {
        Pack p = packOf(b);
        Rank r = rank(b);
        if (p == null) return r.title;
        return r.title + " of " + p.title();
    }

    public List<Brute> alive(String dimension, double x, double z, double radius) {
        List<Brute> out = new ArrayList<>();
        for (Brute b : brutes.values()) {
            if (b.alive && b.dimension.equals(dimension) && Math.hypot(b.x - x, b.z - z) <= radius) out.add(b);
        }
        return out;
    }

    /** The epithets living Brutes wear, so no two share one. */
    public Set<String> takenEpithets() {
        Set<String> out = new java.util.HashSet<>();
        for (Brute b : brutes.values()) if (b.alive && b.epithet != null) out.add(b.epithet);
        return out;
    }

    public Set<String> takenNames() {
        Set<String> out = new HashSet<>();
        for (Brute b : brutes.values()) if (b.alive) out.add(b.name);
        return out;
    }

    public Brute byName(String name) {
        for (Brute b : brutes.values()) if (b.name.equalsIgnoreCase(name) || b.fullName().equalsIgnoreCase(name)) return b;
        return null;
    }

    /** Odds that {@code a} beats {@code b} in a fight between them. */
    public static double odds(Brute a, Brute b) {
        return 1 / (1 + Math.exp(-(a.power() - b.power()) / 3));
    }

    public double strength(Pack p) {
        double s = p.grunts * 0.5;
        for (Brute b : members(p)) s += b.id.equals(p.boss) ? b.power() * 2 : b.power();
        for (Pack v : vassals(p)) {
            s += v.grunts * 0.25;
            Brute vb = brute(v.boss);
            if (vb != null && vb.alive) s += vb.power();
        }
        return s;
    }

    // ---------------------------------------------------------------- things players do

    /**
     * A plain mob becomes a Brute.
     *
     * @param why      {@link Memory.Kind#ROSE} if it killed the player, {@link Memory.Kind#SAW_KILLS} if it watched
     *                 the player kill its kind, or null (a pack grew one, or a command made it)
     * @param packHint the pack it was a grunt of, or null
     */
    public Brute rise(String entityType, Family family, String dimension, int x, int z, UUID player, Memory.Kind why,
                      DeathCause cause, UUID packHint, long now) {
        return rise(entityType, family, dimension, x, z, player, why, cause, packHint, now, true);
    }

    private Brute rise(String entityType, Family family, String dimension, int x, int z, UUID player, Memory.Kind why,
                       DeathCause cause, UUID packHint, long now, boolean announce) {
        clock = now;
        Brute b = new Brute(new UUID(random.nextLong(), random.nextLong()), entityType, family, dimension,
                Names.given(random, takenNames()), now);
        b.epithet = Names.epithet(random, entityType, why == Memory.Kind.ROSE ? cause : null, takenEpithets());
        Random looks = new Random(b.id.getMostSignificantBits() ^ b.name.hashCode()); // kept apart from the feud's dice
        b.size = 0.86f + looks.nextFloat() * 0.3f;
        b.look = looks.nextInt(1 << 16);
        b.level = why == Memory.Kind.ROSE ? 2 + random.nextInt(2) : 1 + random.nextInt(2);
        rollTraits(b);
        b.x = x;
        b.z = z;
        brutes.put(b.id, b);
        if (player != null && why != null) {
            Relation r = b.relation(player);
            r.met = true;
            r.remember(new Memory(why, now, cause == null ? "" : cause.key()));
            if (why == Memory.Kind.ROSE) {
                r.nemesis = true;
                r.killedPlayer++;
                b.hunting = player;
            }
        }
        Pack p = pack(packHint);
        if (p != null && p.family == family && members(p).size() < MAX_MEMBERS) {
            b.pack = p.id;
            p.grunts = Math.max(0, p.grunts - 1);
            if (why == Memory.Kind.ROSE) p.playerKills++;
        }
        if (announce) {
            tell(News.Kind.ROSE, b.fullName() + " has risen" + (p != null && p.id.equals(b.pack) ? " in " + p.title() : "") + ".", b);
        }
        trim(now, List.of());
        return b;
    }

    /** A Brute killed a player. It levels up and the player is now its Nemesis. */
    public void killedPlayer(UUID bruteId, UUID player, DeathCause cause, long now) {
        clock = now;
        Brute b = brute(bruteId);
        if (b == null) return;
        b.levelUp(1);
        Relation r = b.relation(player);
        r.met = true;
        r.nemesis = true;
        r.killedPlayer++;
        r.remember(new Memory(Memory.Kind.KILLED_PLAYER, now, cause == null ? "" : cause.key()));
        b.hunting = player;
        Pack p = packOf(b);
        if (p != null) {
            p.playerKills++;
            p.grunts = Math.min(maxGrunts(), p.grunts + 2);
        }
    }

    /**
     * A player killed a Brute.
     *
     * @param finalBlow the kind of kill nothing comes back from (a critical hit with a blade, or lava)
     * @return whether it is dead for good
     */
    public boolean killedByPlayer(UUID bruteId, UUID player, DeathCause cause, boolean finalBlow, long now) {
        clock = now;
        Brute b = brute(bruteId);
        if (b == null || !b.alive) return true;
        if (player != null) {
            Relation r = b.relation(player);
            r.met = true;
            r.killedByPlayer++;
        }
        boolean deathless = b.traits.contains(Trait.DEATHLESS);
        boolean fin = !deathless && (finalBlow || b.timesReturned >= MAX_RETURNS);
        die(b, cause, player, fin, now);
        return fin;
    }

    /** Records that a pack lost a grunt in the world and re-checks whether its settlement falls into ruin. */
    /**
     * A player killed the last of a pack's builders, leaving its settlement half built. Every Brute of the pack
     * remembers who did it, and the pack's story says so.
     */
    public void wreckedBuild(Pack p, UUID player, String playerName, String what, long now) {
        if (p == null || player == null) return;
        clock = now;
        for (Brute b : members(p)) {
            Relation r = b.relation(player);
            r.met = true;
            r.remember(new Memory(Memory.Kind.WRECKED_CAMP, now, ""));
        }
        p.met.add(player);
        tellAt(p, News.Kind.RAID, playerName + " killed the builders of " + p.title() + "'s " + what + ".");
    }

    public void gruntKilled(Pack p, long now) {
        if (p == null) return;
        clock = now;
        p.grunts = Math.max(0, p.grunts - 1);
        updateSettlement(p);
    }

    /** The player ran from a fight with this Brute. It gets a little stronger for driving them off. */
    public void playerFled(UUID bruteId, UUID player, long now) {
        clock = now;
        Brute b = brute(bruteId);
        if (b == null) return;
        b.levelUp(1);
        Relation r = b.relation(player);
        r.met = true;
        r.remember(new Memory(Memory.Kind.PLAYER_FLED, now, ""));
    }

    public void stole(UUID bruteId, UUID player, String item, long now) {
        clock = now;
        Brute b = brute(bruteId);
        if (b == null) return;
        b.relation(player).remember(new Memory(Memory.Kind.STOLE, now, item));
    }

    // ---------------------------------------------------------------- the night

    /**
     * Plays out a night: the dead may come back, lone Brutes band together, packs grow, Brutes challenge their
     * Bosses, rival packs raid each other and Brutes that swore revenge move toward their player. Only what is
     * near a player happens; the rest sleeps and catches up when someone comes back.
     */
    public void night(long now, List<PlayerSpot> players, Places places) {
        clock = now;
        lastNight = now;
        returns(now, players);
        travel(now, players);
        gather(now, players, places);
        for (Pack p : new ArrayList<>(packs.values())) {
            if (!packs.containsKey(p.id) || !awake(p, players)) continue;
            int nights = (int) Math.max(1, Math.min(CATCH_UP_NIGHTS, (now - p.lastAwake) / DAY));
            for (int i = 0; i < nights; i++) grow(p, now);
            updateSettlement(p);
            p.lastAwake = now;
            challenge(p, now);
        }
        raids(now, players);
        for (Pack p : new ArrayList<>(packs.values())) {
            if (packs.containsKey(p.id) && awake(p, players)) updateSettlement(p);
        }
        trim(now, players);
    }

    public boolean awake(Pack p, List<PlayerSpot> players) {
        for (PlayerSpot s : players) if (p.distanceTo(s.dimension(), s.x(), s.z()) <= AWAKE_RANGE) return true;
        return false;
    }

    public boolean awake(Brute b, List<PlayerSpot> players) {
        for (PlayerSpot s : players) {
            if (b.dimension.equals(s.dimension()) && Math.hypot(b.x - s.x(), b.z - s.z()) <= AWAKE_RANGE) return true;
        }
        return false;
    }

    private void returns(long now, List<PlayerSpot> players) {
        for (Brute b : new ArrayList<>(brutes.values())) {
            if (b.alive || b.finalDeath || now - b.diedAt < DAY / 2) continue;
            if (now - b.diedAt > 6 * DAY) {
                b.finalDeath = true;
                continue;
            }
            double chance = (b.traits.contains(Trait.DEATHLESS) ? 0.9 : 0.6) - 0.15 * b.timesReturned + 0.01 * b.level;
            if (random.nextDouble() < chance) comeBack(b, now);
        }
    }

    /** A dead Brute comes back, scarred by how it died, angrier, and looking for whoever did it. */
    public void comeBack(Brute b, long now) {
        clock = now;
        b.alive = true;
        b.timesReturned++;
        Scar scar = (b.lastCause == null ? DeathCause.MELEE : b.lastCause).scar();
        if (!b.scars.contains(scar)) b.scars.add(scar);
        if (scar.hardened != null && random.nextBoolean()) b.addTrait(scar.hardened);
        else if (scar.haunted != null && random.nextBoolean()) b.addTrait(scar.haunted);
        if (random.nextInt(3) < 2) {
            String e = Names.scarEpithet(random, scar, takenEpithets());
            if (e != null) b.epithet = e;
        }
        b.levelUp(1 + random.nextInt(2));
        if (b.killedBy != null) {
            Relation r = b.relation(b.killedBy);
            r.remember(new Memory(Memory.Kind.WAS_KILLED, b.diedAt, b.lastCause == null ? "" : b.lastCause.key()));
            r.remember(new Memory(Memory.Kind.RETURNED, now, ""));
            b.hunting = b.killedBy;
        }
        Pack p = packOf(b);
        if (p == null || members(p).size() >= MAX_MEMBERS) b.pack = null;
        else if (brute(p.boss) == null || !brute(p.boss).alive) p.boss = b.id;
        tell(News.Kind.RETURNED, b.fullName() + " is back from the dead.", b);
    }

    private void travel(long now, List<PlayerSpot> players) {
        for (Brute b : new ArrayList<>(brutes.values())) {
            if (!b.alive || b.hunting == null) continue;
            PlayerSpot target = null;
            for (PlayerSpot s : players) if (s.id().equals(b.hunting)) target = s;
            if (target == null || !target.dimension().equals(b.dimension)) continue;
            double d = Math.hypot(target.x() - b.x, target.z() - b.z);
            if (d <= 600) continue;
            Pack p = packOf(b);
            if (p != null && b.id.equals(p.boss) && random.nextInt(3) != 0) continue; // a Boss mostly stays home
            double step = Math.min(d - 150, TRAVEL_PER_DAY);
            b.x += (int) Math.round((target.x() - b.x) / d * step);
            b.z += (int) Math.round((target.z() - b.z) / d * step);
            if (p != null && p.distanceTo(b.dimension, b.x, b.z) > PACK_RANGE) {
                leavePack(b, now);
                tell(News.Kind.HUNTING, b.fullName() + " has left " + p.title() + " to hunt someone down.", b);
            }
        }
    }

    private void gather(long now, List<PlayerSpot> players, Places places) {
        for (Brute b : new ArrayList<>(brutes.values())) {
            if (!b.alive || b.pack != null || !awake(b, players)) continue;
            if (b.hunting != null && huntingFar(b, players)) continue;
            Pack near = null;
            double best = PACK_RANGE;
            for (Pack p : packs.values()) {
                double d = p.distanceTo(b.dimension, b.x, b.z);
                if (p.family == b.family && d <= best && members(p).size() < MAX_MEMBERS) {
                    near = p;
                    best = d;
                }
            }
            if (near != null && random.nextDouble() < 0.4) {
                b.pack = near.id;
                b.x = near.homeX;
                b.z = near.homeZ;
                tell(News.Kind.JOINED, b.fullName() + " has joined " + near.title() + ".", b);
                continue;
            }
            Brute other = null;
            for (Brute o : brutes.values()) {
                if (o != b && o.alive && o.pack == null && o.family == b.family && o.dimension.equals(b.dimension)
                        && Math.hypot(o.x - b.x, o.z - b.z) <= MEET_RANGE) {
                    other = o;
                    break;
                }
            }
            if (other != null) {
                Brute boss = b.power() >= other.power() ? b : other;
                Brute second = boss == b ? other : b;
                Pack p = form(boss, places, now, 3 + random.nextInt(3));
                second.pack = p.id;
                second.x = p.homeX;
                second.z = p.homeZ;
                tell(News.Kind.FORMED, boss.fullName() + " and " + second.fullName() + " have formed " + p.title()
                        + ", with " + boss.name + " as its Boss.", boss, second);
            } else if (random.nextDouble() < 0.3) {
                Pack p = form(b, places, now, 2 + random.nextInt(3));
                tell(News.Kind.FORMED, b.fullName() + " has gathered a following: " + p.title() + ".", b);
            }
        }
    }

    private boolean huntingFar(Brute b, List<PlayerSpot> players) {
        for (PlayerSpot s : players) {
            if (s.id().equals(b.hunting)) {
                return !s.dimension().equals(b.dimension) || Math.hypot(s.x() - b.x, s.z() - b.z) > 600;
            }
        }
        return false;
    }

    /** Starts a pack around a Brute, at the place it stands. */
    public Pack form(Brute boss, Places places, long now, int grunts) {
        clock = now;
        Set<Integer> usedColors = new HashSet<>();
        for (Pack p : packs.values()) if (p.distanceTo(boss.dimension, boss.x, boss.z) <= RIVAL_RANGE) usedColors.add(p.color);
        int color = random.nextInt(16);
        for (int i = 0; i < 16 && usedColors.contains(color); i++) color = (color + 5) % 16;
        String place = places.name(random, boss.dimension, boss.x, boss.z);
        Pack p = new Pack(new UUID(random.nextLong(), random.nextLong()), boss.family, boss.dimension, place, color,
                boss.x, boss.z, now);
        p.boss = boss.id;
        p.grunts = Math.min(maxGrunts(), grunts);
        p.tier = SettlementTier.CAMP;
        boss.pack = p.id;
        for (Map.Entry<UUID, Relation> e : boss.relations.entrySet()) if (e.getValue().met) p.met.add(e.getKey());
        packs.put(p.id, p);
        return p;
    }

    private void grow(Pack p, long now) {
        p.grunts = Math.min(maxGrunts(), p.grunts + 1 + random.nextInt(2));
        if (p.grunts >= 4 && members(p).size() < MAX_MEMBERS && random.nextDouble() < 0.15) {
            Brute boss = brute(p.boss);
            String type = boss != null ? boss.entityType : p.family.defaultType;
            Brute b = rise(type, p.family, p.dimension, p.homeX, p.homeZ, null, null, null, p.id, now, false);
            tell(News.Kind.GREW, "A new Brute has risen in " + p.title() + ": " + b.fullName() + ".", b);
        }
    }

    /** Compatibility alias for {@link #updateSettlement(Pack)}. */
    public boolean advanceSettlement(Pack p) {
        return updateSettlement(p);
    }

    /**
     * Re-evaluates a pack's settlement against its current living roster, grunts, strength, and King rank:
     * advances {@code p.tier} upward when it grows past a threshold, marks {@code p.ruined = true} when its
     * numbers fall below its current tier's threshold, and restores {@code p.ruined = false} once it recovers.
     * Does nothing while {@link #campsEnabled} is false.
     */
    public boolean updateSettlement(Pack p) {
        if (!campsEnabled || p == null) return false;
        if (p.tier == null || p.tier == SettlementTier.NONE) p.tier = SettlementTier.CAMP;
        int living = members(p).size();
        double str = strength(p);
        boolean king = !vassals(p).isEmpty();
        SettlementTier sustained = SettlementTier.forPack(living, p.grunts, str, king);
        if (sustained.level > p.tier.level) {
            p.tier = sustained;
            p.ruined = false;
            tellAt(p, News.Kind.SETTLED, settlementNews(p, sustained));
            return true;
        }
        if (!p.ruined) {
            if (!p.tier.sustains(living, p.grunts, str, king)) {
                p.ruined = true;
                tellAt(p, News.Kind.SETTLED, ruinedNews(p, p.tier));
                return true;
            }
            return false;
        }
        if (sustained.level >= p.tier.level) {
            p.ruined = false;
            tellAt(p, News.Kind.SETTLED, rebuiltNews(p, p.tier));
            return true;
        }
        return false;
    }

    /**
     * Operator helper: ratchets a pack to at least {@code target} tier, clears any ruined state, and ensures
     * its living roster of Brutes and grunts fits that tier so command-spawned settlements have a gang running them.
     */
    public SettlementTier upgradeSettlement(Pack p, SettlementTier target, long now) {
        clock = now;
        if (target == null || target == SettlementTier.NONE) target = SettlementTier.CAMP;
        if (p.tier == null || p.tier == SettlementTier.NONE) p.tier = SettlementTier.CAMP;
        boolean raised = target.level > p.tier.level;
        boolean restored = p.ruined;
        if (raised) p.tier = target;
        p.ruined = false;
        int needBrutes = Math.min(MAX_MEMBERS, Math.max(1, p.tier.minBrutes));
        int needGrunts = Math.min(maxGrunts(), p.tier.minGrunts + 4);
        Brute boss = brute(p.boss);
        String type = boss != null ? boss.entityType : p.family.defaultType;
        while (members(p).size() < needBrutes) {
            Brute b = rise(type, p.family, p.dimension, p.campX(), p.campZ(), null, null, null, p.id, now, false);
            if (p.boss == null) p.boss = b.id;
        }
        p.grunts = Math.max(p.grunts, needGrunts);
        boss = brute(p.boss);
        if (boss != null && boss.alive) {
            int minBossLevel = switch (p.tier) {
                case TOWN -> 10;
                case STRONGHOLD -> 7;
                case OUTPOST -> 4;
                default -> 2;
            };
            if (boss.level < minBossLevel) boss.level = minBossLevel;
        }
        if (raised) tellAt(p, News.Kind.SETTLED, settlementNews(p, p.tier));
        else if (restored) tellAt(p, News.Kind.SETTLED, rebuiltNews(p, p.tier));
        return p.tier;
    }

    /**
     * Operator helper: puts a pack's settlement (at {@code target} tier) into ruins and trims its numbers
     * below that tier's threshold so the ruins persist until the pack recovers.
     */
    public SettlementTier ruinSettlement(Pack p, SettlementTier target, long now) {
        clock = now;
        if (target == null || target == SettlementTier.NONE) {
            target = (p.tier == null || p.tier == SettlementTier.NONE) ? SettlementTier.CAMP : p.tier;
        }
        if (p.tier == null || target.level > p.tier.level) p.tier = target;
        List<Pack> detached = vassals(p);
        if (!detached.isEmpty()) {
            for (Pack v : detached) {
                v.liege = null;
                v.bannersDirty = true;
            }
            p.bannersDirty = true;
        }
        p.grunts = p.tier == SettlementTier.CAMP ? 0 : Math.min(p.grunts, Math.max(1, p.tier.minGrunts / 4));
        // If high-level Brutes would still sustain the tier via raw strength, zero grunts so it stays ruined
        int living = members(p).size();
        double str = strength(p);
        if (p.tier.sustains(living, p.grunts, str, false)
                || SettlementTier.forPack(living, p.grunts, str, false).level >= p.tier.level) {
            p.grunts = 0;
        }
        boolean newlyRuined = !p.ruined;
        p.ruined = true;
        if (newlyRuined) tellAt(p, News.Kind.SETTLED, ruinedNews(p, p.tier));
        return p.tier;
    }

    public static String settlementNews(Pack p, SettlementTier tier) {
        return switch (tier) {
            case OUTPOST -> cap(p.title()) + " has fortified its camp into an outpost.";
            case STRONGHOLD -> cap(p.title()) + " has raised a stronghold.";
            case TOWN -> cap(p.title()) + " now runs a full town.";
            default -> cap(p.title()) + " has pitched camp.";
        };
    }

    public static String ruinedNews(Pack p, SettlementTier tier) {
        SettlementTier t = tier == null || tier == SettlementTier.NONE ? SettlementTier.CAMP : tier;
        return "With its numbers broken, " + p.title() + "'s " + t.key + " has fallen into ruin.";
    }

    public static String rebuiltNews(Pack p, SettlementTier tier) {
        SettlementTier t = tier == null || tier == SettlementTier.NONE ? SettlementTier.CAMP : tier;
        return cap(p.title()) + " has rebuilt its ruined " + t.key + ".";
    }

    private void challenge(Pack p, long now) {
        Brute boss = brute(p.boss);
        if (boss == null || !boss.alive) {
            succession(p, now);
            return;
        }
        Brute challenger = null;
        for (Brute b : members(p)) {
            if (b != boss && b.level >= boss.level + 2 && (challenger == null || b.power() > challenger.power())) challenger = b;
        }
        if (challenger == null || random.nextDouble() < 0.5) return;
        boolean won = random.nextDouble() < odds(challenger, boss);
        Brute winner = won ? challenger : boss;
        Brute loser = won ? boss : challenger;
        winner.levelUp(1);
        if (won) p.boss = winner.id;
        if (random.nextDouble() < 0.6) {
            die(loser, DeathCause.MELEE, null, false, now);
            tell(News.Kind.CHALLENGE, challenger.fullName() + " challenged " + boss.fullName() + " for " + p.title()
                    + ". " + loser.name + " was cut down.", winner, loser);
        } else {
            loser.levelUp(-1);
            tell(News.Kind.CHALLENGE, challenger.fullName() + " challenged " + boss.fullName() + " for " + p.title()
                    + ". " + loser.name + " lost and kneels to " + winner.name + ".", winner, loser);
        }
    }

    private void raids(long now, List<PlayerSpot> players) {
        List<Pack> awake = new ArrayList<>();
        for (Pack p : packs.values()) if (awake(p, players)) awake.add(p);
        java.util.Collections.shuffle(awake, random);
        Set<UUID> used = new HashSet<>();
        for (Pack a : awake) {
            if (used.contains(a.id) || !packs.containsKey(a.id)) continue;
            for (Pack b : awake) {
                if (a == b || used.contains(b.id) || !packs.containsKey(b.id)) continue;
                if (a.distanceTo(b) > RIVAL_RANGE || top(a) == top(b)) continue;
                if (random.nextDouble() >= 0.25) continue;
                used.add(a.id);
                used.add(b.id);
                raid(a, b, now);
                break;
            }
        }
    }

    /** One pack raids another. The loser bleeds, and if they are the same family the winner may swallow it. */
    public void raid(Pack attacker, Pack defender, long now) {
        clock = now;
        double sa = strength(attacker) * (0.75 + random.nextDouble() * 0.5);
        double sd = strength(defender) * (0.75 + random.nextDouble() * 0.5) * 1.1; // fighting at home
        Pack winner = sa >= sd ? attacker : defender;
        Pack loser = winner == attacker ? defender : attacker;
        double margin = Math.max(sa, sd) / Math.max(0.1, Math.min(sa, sd));
        loser.grunts = Math.max(0, loser.grunts - Math.max(1, loser.grunts / 2));
        winner.grunts = Math.max(0, winner.grunts - random.nextInt(2));
        StringBuilder text = new StringBuilder(cap(attacker.title()) + " raided " + defender.title() + ". "
                + cap(winner.title()) + " won.");
        List<Brute> losers = members(loser);
        losers.removeIf(x -> x.id.equals(loser.boss));
        if (!losers.isEmpty() && random.nextDouble() < 0.3) {
            Brute fallen = losers.get(random.nextInt(losers.size()));
            die(fallen, DeathCause.MELEE, null, false, now);
            text.append(" ").append(fallen.fullName()).append(" fell.");
        }
        Brute wb = brute(winner.boss);
        if (wb != null) wb.levelUp(1);
        boolean decisive = margin > 1.3 || loser.grunts == 0;
        if (decisive && winner.family == loser.family && winner.distanceTo(loser) <= KING_RANGE) {
            loser.remember(clock, text.toString());
            tell(News.Kind.RAID, text.toString(), wb);
            swallow(winner, loser);
            return;
        }
        if (decisive && winner.family != loser.family && random.nextDouble() < 0.3) {
            Brute lb = brute(loser.boss);
            if (lb != null && lb.alive) {
                die(lb, DeathCause.MELEE, null, false, now);
                text.append(" Its Boss ").append(lb.fullName()).append(" was slain.");
            }
        }
        loser.remember(clock, text.toString());
        tell(News.Kind.RAID, text.toString(), wb);
    }

    /** The winner's King (or the winner, who becomes one) takes the loser and everything that kneels to it. */
    public void swallow(Pack winner, Pack loser) {
        Pack king = top(winner);
        if (king == top(loser)) return;
        boolean crowned = vassals(king).isEmpty();
        Pack oldLiege = loser.liege == null ? null : pack(loser.liege);
        if (oldLiege != null && oldLiege != king) oldLiege.bannersDirty = true;
        for (Pack v : vassals(loser)) {
            v.liege = king.id;
            v.bannersDirty = true;
        }
        loser.liege = king.id;
        loser.bannersDirty = true;
        king.bannersDirty = true;
        Brute kb = brute(king.boss);
        String kingName = kb != null ? kb.fullName() : cap(king.title());
        tell(News.Kind.SWALLOWED, cap(loser.title()) + " now kneels to " + kingName + ".", kb);
        if (crowned && kb != null) tell(News.Kind.CROWNED, kb.fullName() + " is now King of " + king.family.title
                + " around " + king.place + ".", kb);
        advanceSettlement(king);
    }

    // ---------------------------------------------------------------- deaths, leaving and keeping it small

    private void die(Brute b, DeathCause cause, UUID killer, boolean fin, long now) {
        b.alive = false;
        b.finalDeath = fin;
        b.diedAt = now;
        b.lastCause = cause;
        b.killedBy = killer;
        Pack p = packOf(b);
        if (p != null && b.id.equals(p.boss)) succession(p, now);
        if (p != null && packs.containsKey(p.id)) updateSettlement(p);
    }

    private void leavePack(Brute b, long now) {
        Pack p = packOf(b);
        b.pack = null;
        if (p != null && b.id.equals(p.boss)) succession(p, now);
        if (p != null && packs.containsKey(p.id)) updateSettlement(p);
    }

    /** The Boss is gone: the strongest Brute takes over, or the pack breaks up. A King's packs go their own way. */
    private void succession(Pack p, long now) {
        List<Pack> vassals = vassals(p);
        List<Integer> prevVassalColors = new ArrayList<>(vassals.size());
        if (!vassals.isEmpty()) {
            for (Pack v : vassals) {
                prevVassalColors.add(v.color);
                v.liege = null;
                v.bannersDirty = true;
            }
            p.bannersDirty = true;
            tellAt(p, News.Kind.SPLIT, "With its King gone, " + p.title() + " has lost its hold: " + vassals.size()
                    + (vassals.size() == 1 ? " pack goes" : " packs go") + " its own way.");
        }
        Brute next = null;
        for (Brute b : members(p)) {
            if (b.id.equals(p.boss)) continue;
            if (next == null || b.power() > next.power()) next = b;
        }
        if (next != null) {
            p.boss = next.id;
            tell(News.Kind.CHALLENGE, next.fullName() + " now leads " + p.title() + ".", next);
            return;
        }
        Brute old = brute(p.boss);
        if (old != null && !old.alive && !old.finalDeath) {
            return; // the pack waits a while for its Boss to come back
        }
        disband(p, true, prevVassalColors);
    }

    private void disband(Pack p, boolean announce) {
        List<Integer> vassalColors = new ArrayList<>();
        for (Pack v : vassals(p)) vassalColors.add(v.color);
        disband(p, announce, vassalColors);
    }

    private void disband(Pack p, boolean announce, List<Integer> vassalColors) {
        if (campsEnabled && p.siteSet && p.builtTier > 0 && !p.builtRuined) {
            recordAbandonedSite(new AbandonedSite(p.id, p.family, p.color, p.dimension,
                    p.siteX, p.siteY, p.siteZ, p.siteFacing, p.builtTier,
                    vassalColors, p.skippedZones, p.skippedColumns));
        }
        packs.remove(p.id);
        if (p.liege != null) {
            Pack liege = pack(p.liege);
            if (liege != null) liege.bannersDirty = true;
        }
        for (Pack v : packs.values()) {
            if (p.id.equals(v.liege)) {
                v.liege = null;
                v.bannersDirty = true;
            }
        }
        for (Brute b : brutes.values()) if (p.id.equals(b.pack)) b.pack = null;
        if (announce) tellAt(p, News.Kind.SPLIT, cap(p.title()) + " has scattered.");
    }

    private void trim(long now, List<PlayerSpot> players) {
        for (Pack p : new ArrayList<>(packs.values())) {
            Brute boss = brute(p.boss);
            boolean bossGone = boss == null || boss.finalDeath || (!boss.alive && members(p).isEmpty() && now - boss.diedAt > 6 * DAY);
            if (bossGone && members(p).isEmpty()) disband(p, true);
        }
        while (packs.size() > maxPacks) {
            Pack fade = packs.values().stream().filter(p -> !awake(p, players))
                    .min(Comparator.comparing((Pack p) -> !p.met.isEmpty()).thenComparingLong(p -> p.formedAt))
                    .orElse(null);
            if (fade == null) break;
            disband(fade, false);
            tellAt(fade, News.Kind.FADED, cap(fade.title()) + " has faded away.");
        }
        brutes.values().removeIf(b -> b.finalDeath && now - b.diedAt > 10 * DAY);
        while (brutes.size() > maxBrutes) {
            Brute drop = brutes.values().stream().filter(b -> !b.alive)
                    .min(Comparator.comparingLong(b -> b.diedAt)).orElse(null);
            if (drop == null) {
                drop = brutes.values().stream().filter(b -> b.pack == null && !awake(b, players))
                        .min(Comparator.comparingLong(b -> b.risenAt)).orElse(null);
            }
            if (drop == null) break;
            brutes.remove(drop.id);
        }
    }

    // ---------------------------------------------------------------- traits

    void rollTraits(Brute b) {
        List<Trait> good = new ArrayList<>(), bad = new ArrayList<>(), fears = new ArrayList<>();
        for (Trait t : Trait.values()) {
            if (t == Trait.DEATHLESS) continue;
            switch (t.kind) {
                case IMMUNITY, STRENGTH -> good.add(t);
                case WEAKNESS -> bad.add(t);
                case FEAR -> fears.add(t);
            }
        }
        if (random.nextInt(12) == 0) b.addTrait(Trait.DEATHLESS);
        int strengths = 1 + random.nextInt(2);
        for (int i = 0; i < 10 && strengths > 0; i++) if (b.addTrait(good.get(random.nextInt(good.size())))) strengths--;
        for (int i = 0; i < 10; i++) if (b.addTrait(bad.get(random.nextInt(bad.size())))) break;
        if (random.nextDouble() < 0.6) for (int i = 0; i < 10; i++) if (b.addTrait(fears.get(random.nextInt(fears.size())))) break;
    }

    // ---------------------------------------------------------------- news

    private void tell(News.Kind kind, String text, Brute... about) {
        Brute first = null;
        List<UUID> ids = new ArrayList<>();
        java.util.Set<Pack> chronicled = new java.util.HashSet<>();
        for (Brute b : about) {
            if (b == null) continue;
            if (first == null) first = b;
            ids.add(b.id);
            Pack p = packOf(b);
            if (p != null && chronicled.add(p)) p.remember(clock, text);
        }
        news.add(new News(kind, text, first == null ? "" : first.dimension, first == null ? 0 : first.x,
                first == null ? 0 : first.z, ids));
    }

    private void tellAt(Pack p, News.Kind kind, String text) {
        p.remember(clock, text);
        news.add(new News(kind, text, p.dimension, p.campX(), p.campZ(), List.of()));
    }

    public static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
