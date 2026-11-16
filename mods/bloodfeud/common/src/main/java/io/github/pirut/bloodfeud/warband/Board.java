package io.github.pirut.bloodfeud.warband;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * The Warband: who holds which place, who is alive, and what struggles are coming. Pure logic with no Minecraft
 * classes, so it can be tested on its own; {@code WarbandData} saves it and the game code acts on it.
 */
public final class Board {
    public static final int WARLORDS = 5;
    public static final int GUARDS_PER_WARLORD = 2;
    public static final int MAX_CAPTAINS = 20;
    /** How many Captains (Sworn Guards included) the Warband tops itself up to at each power shift. */
    public static final int TARGET_CAPTAINS = 14;
    public static final int PENDING_STRUGGLES = 4;
    public static final long DAY = 24000L;

    /** A mob that saw a player die, and may be promoted to fill a place at the next power shift. */
    public record Candidate(String entityType, String dimension) {
    }

    public final Map<UUID, Captain> captains = new LinkedHashMap<>();
    public final UUID[] warlords = new UUID[WARLORDS];
    public final List<Territory> territories = new ArrayList<>();
    public final List<PowerStruggle> struggles = new ArrayList<>();
    public final List<Candidate> candidates = new ArrayList<>();
    public int nextStruggleId = 1;
    public boolean generated;
    /** Mob types used for new Captains when no candidate is waiting. Set by the game from the entity tag. */
    public List<String> defaultTypes = List.of("minecraft:zombie", "minecraft:skeleton");
    public String homeDimension = "minecraft:overworld";

    // ---- queries ----

    public Captain get(UUID id) {
        return id == null ? null : captains.get(id);
    }

    public Captain warlord(int slot) {
        Captain c = get(warlords[slot]);
        return c != null && c.alive ? c : null;
    }

    public List<Captain> living() {
        List<Captain> out = new ArrayList<>();
        for (Captain c : captains.values()) if (c.alive) out.add(c);
        return out;
    }

    /** Living Captains that are not Warlords (Sworn Guards included). */
    public List<Captain> livingCaptains() {
        List<Captain> out = new ArrayList<>();
        for (Captain c : captains.values()) if (c.alive && !c.isWarlord()) out.add(c);
        return out;
    }

    public List<Captain> guardsOf(int slot) {
        List<Captain> out = new ArrayList<>();
        for (Captain c : captains.values()) if (c.alive && c.guarding == slot) out.add(c);
        return out;
    }

    /** A Warlord shows itself only once no Sworn Guard stands in the way. */
    public boolean warlordExposed(int slot) {
        return warlord(slot) != null && guardsOf(slot).isEmpty();
    }

    public Set<String> takenNames() {
        Set<String> names = new HashSet<>();
        for (Captain c : captains.values()) if (c.alive) names.add(c.name);
        return names;
    }

    public Territory nearestTerritory(String dimension, double x, double z) {
        Territory best = null;
        double bestD = Double.MAX_VALUE;
        for (Territory t : territories) {
            double d = t.distanceSq(x, z);
            if (d < bestD) {
                bestD = d;
                best = t;
            }
        }
        return best;
    }

    public Captain findByName(String text) {
        String q = text.toLowerCase(java.util.Locale.ROOT).trim();
        Captain loose = null;
        for (Captain c : captains.values()) {
            if (!c.alive) continue;
            if (c.fullName().toLowerCase(java.util.Locale.ROOT).equals(q) || c.name.toLowerCase(java.util.Locale.ROOT).equals(q)) return c;
            if (loose == null && c.fullName().toLowerCase(java.util.Locale.ROOT).contains(q)) loose = c;
        }
        return loose;
    }

    // ---- creation ----

    /** Builds the first Warband: five Warlords with territories around spawn, their Sworn Guards, and a few Captains. */
    public void generate(Random random, long now, int spawnX, int spawnZ) {
        captains.clear();
        struggles.clear();
        candidates.clear();
        territories.clear();
        java.util.Arrays.fill(warlords, null);
        double turn = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < WARLORDS; i++) {
            double angle = turn + i * Math.PI * 2 / WARLORDS + (random.nextDouble() - 0.5) * 0.5;
            int dist = 900 + random.nextInt(700);
            territories.add(new Territory(i, homeDimension, spawnX + (int) (Math.cos(angle) * dist),
                    spawnZ + (int) (Math.sin(angle) * dist), 600));
        }
        for (int i = 0; i < WARLORDS; i++) {
            Captain w = create(random, pickType(random), homeDimension, 11 + random.nextInt(5), null, now);
            w.warlordOf = i;
            w.territory = i;
            warlords[i] = w.id;
            for (int g = 0; g < GUARDS_PER_WARLORD; g++) {
                Captain guard = create(random, pickType(random), homeDimension, 5 + random.nextInt(6), null, now);
                guard.guarding = i;
                guard.territory = i;
            }
        }
        while (livingCaptains().size() < TARGET_CAPTAINS) {
            Captain c = create(random, pickType(random), homeDimension, 1 + random.nextInt(6), null, now);
            c.territory = random.nextInt(WARLORDS);
        }
        queueStruggles(random, now);
        generated = true;
    }

    /** A new Captain with a name and rolled traits, added to the board. */
    public Captain create(Random random, String entityType, String dimension, int level, DeathCause cause, long now) {
        Captain c = new Captain(new UUID(random.nextLong(), random.nextLong()));
        c.entityType = entityType;
        c.dimension = dimension;
        c.level = Math.max(1, Math.min(Captain.MAX_LEVEL, level));
        c.name = Names.given(random, takenNames());
        c.epithet = Names.epithet(random, entityType, cause);
        c.risenAt = now;
        rollTraits(random, c);
        captains.put(c.id, c);
        return c;
    }

    static void rollTraits(Random random, Captain c) {
        List<Trait> immunities = of(Trait.Kind.IMMUNITY), strengths = of(Trait.Kind.STRENGTH),
                weaknesses = of(Trait.Kind.WEAKNESS), fears = of(Trait.Kind.FEAR);
        if (random.nextInt(100) < 35 + c.level * 2) addRandom(random, c, immunities);
        int s = 1 + (c.level >= 8 ? 1 : 0) + (c.level >= 15 ? 1 : 0);
        for (int i = 0; i < s; i++) addRandom(random, c, strengths);
        int w = 1 + (random.nextInt(100) < 50 ? 1 : 0);
        for (int i = 0; i < w; i++) addRandom(random, c, weaknesses);
        if (random.nextInt(100) < 55) addRandom(random, c, fears);
    }

    private static List<Trait> of(Trait.Kind kind) {
        List<Trait> out = new ArrayList<>();
        for (Trait t : Trait.values()) if (t.kind == kind) out.add(t);
        return out;
    }

    private static boolean addRandom(Random random, Captain c, List<Trait> from) {
        for (int i = 0; i < 8; i++) if (c.addTrait(from.get(random.nextInt(from.size())))) return true;
        return false;
    }

    private String pickType(Random random) {
        return defaultTypes.get(random.nextInt(defaultTypes.size()));
    }

    // ---- what players do ----

    /**
     * A plain mob killed a player: it rises to Captain on the spot. If every place is taken, the weakest Captain
     * is pushed out (dies for good) to make room.
     */
    public Captain rise(Random random, String entityType, String dimension, UUID player, DeathCause cause, long now,
                        double x, double z, List<String> lines) {
        List<Captain> current = livingCaptains();
        if (current.size() >= MAX_CAPTAINS) {
            Captain weakest = current.stream().filter(c -> !c.isGuard()).min(Comparator.comparingInt(c -> c.level))
                    .orElse(current.stream().min(Comparator.comparingInt(c -> c.level)).orElseThrow());
            kill(weakest, DeathCause.MELEE, true, now);
            lines.add(weakest.fullName() + " is cast out of the Warband to make room.");
        }
        Captain c = create(random, entityType, dimension, 1 + random.nextInt(3), cause, now);
        Territory t = nearestTerritory(dimension, x, z);
        c.territory = t == null ? 0 : t.index();
        Relation r = c.relation(player);
        r.met = true;
        r.nemesis = true;
        r.killedPlayer++;
        r.remember(new Memory(Memory.Kind.ROSE, now, cause.key()));
        r.remember(new Memory(Memory.Kind.KILLED_PLAYER, now, cause.key()));
        lines.add(c.fullName() + " rises to Captain.");
        return c;
    }

    /** A Captain killed a player: it grows stronger and becomes their Nemesis. */
    public void killedPlayer(Random random, Captain c, UUID player, DeathCause cause, long now, List<String> lines) {
        int gain = 1 + random.nextInt(3);
        c.levelUp(gain);
        Relation r = c.relation(player);
        boolean wasNemesis = r.nemesis;
        r.met = true;
        r.nemesis = true;
        r.killedPlayer++;
        r.remember(new Memory(Memory.Kind.KILLED_PLAYER, now, cause.key()));
        lines.add(c.fullName() + " grows stronger (level " + c.level + ")" + (wasNemesis ? "." : " and becomes your Nemesis."));
        int roll = random.nextInt(100);
        if (roll < 30) {
            Trait before = firstOf(c, Trait.Kind.STRENGTH);
            if (addRandom(random, c, of(Trait.Kind.STRENGTH)) || before == null) lines.add(c.name + " has learned something new.");
        } else if (roll < 55) {
            Trait weak = firstOf(c, Trait.Kind.WEAKNESS);
            if (weak != null) {
                c.traits.remove(weak);
                r.known.remove(weak);
                lines.add(c.name + " has overcome a weakness.");
            }
        }
    }

    private static Trait firstOf(Captain c, Trait.Kind kind) {
        for (Trait t : c.traits) if (t.kind == kind) return t;
        return null;
    }

    /** A Captain died. A final kill (or a pushed-out Captain) never comes back. */
    public void kill(Captain c, DeathCause cause, boolean finalKill, long now) {
        c.alive = false;
        c.finalDeath = finalKill && !c.has(Trait.DEATHLESS);
        c.diedAt = now;
        c.lastCause = cause;
        if (c.isWarlord()) {
            warlords[c.warlordOf] = null;
            c.warlordOf = -1;
        }
        c.guarding = -1;
        struggles.removeIf(s -> c.id.equals(s.first()) || c.id.equals(s.second()));
    }

    public void killedByPlayer(Captain c, UUID player, DeathCause cause, boolean finalKill, long now) {
        Relation r = c.relation(player);
        r.met = true;
        r.killedByPlayer++;
        r.remember(new Memory(Memory.Kind.WAS_KILLED, now, cause.key()));
        kill(c, cause, finalKill, now);
    }

    /** The player ran: the Captain gets a little stronger for driving them off. */
    public void playerFled(Captain c, UUID player, long now) {
        c.levelUp(1);
        Relation r = c.relation(player);
        r.remember(new Memory(Memory.Kind.PLAYER_FLED, now, ""));
    }

    /** Someone saw a player die. It may be promoted at the next power shift. */
    public void witness(String entityType, String dimension) {
        if (candidates.size() >= 10) candidates.remove(0);
        candidates.add(new Candidate(entityType, dimension));
    }

    // ---- the Warband's own time ----

    /**
     * Moves the Warband forward to {@code now + horizon}: resolves due struggles, brings back Captains that cheated
     * death, fills empty places and queues new struggles. A player dying or sleeping calls this with a day's horizon.
     *
     * @return what happened, one line each, for players to read
     */
    public List<String> advance(Random random, long now, long horizon) {
        List<String> lines = new ArrayList<>();
        long until = now + horizon;
        List<PowerStruggle> due = new ArrayList<>();
        for (PowerStruggle s : struggles) if (s.due() <= until) due.add(s);
        due.sort(Comparator.comparingLong(PowerStruggle::due));
        for (PowerStruggle s : due) {
            if (!struggles.contains(s)) continue; // a participant died in an earlier one
            struggles.remove(s);
            resolve(random, s, Math.max(now, s.due()), lines);
        }
        if (horizon > 0 || !due.isEmpty()) {
            returnFromDeath(random, until, horizon > 0, lines);
            fillPlaces(random, now, lines);
        }
        queueStruggles(random, now);
        return lines;
    }

    void resolve(Random random, PowerStruggle s, long when, List<String> lines) {
        Captain a = get(s.first()), b = get(s.second());
        if (a == null || !a.alive) return;
        switch (s.type()) {
            case FEAST -> {
                a.levelUp(1 + random.nextInt(2));
                lines.add(a.fullName() + " held a feast and grew stronger (level " + a.level + ").");
            }
            case HUNT -> {
                a.levelUp(1);
                boolean learned = addRandom(random, a, of(Trait.Kind.STRENGTH));
                lines.add(a.fullName() + " came back from a hunt" + (learned ? " with a new strength." : "."));
            }
            case RECRUIT -> {
                a.levelUp(1);
                a.addTrait(Trait.PACKCALLER);
                lines.add(a.fullName() + " raised a pack of followers.");
            }
            case DUEL, AMBUSH -> {
                if (b == null || !b.alive) return;
                Captain win = random.nextDouble() < odds(a, b) ? a : b;
                Captain lose = win == a ? b : a;
                win.levelUp(2);
                kill(lose, DeathCause.MELEE, random.nextBoolean(), when);
                lines.add((s.type() == PowerStruggle.Type.DUEL ? "In a duel, " : "In an ambush, ") + win.fullName()
                        + " killed " + lose.fullName() + ".");
            }
            case CHALLENGE -> {
                if (b == null || !b.alive || !b.isWarlord()) return;
                int slot = b.warlordOf;
                if (random.nextDouble() < odds(a, b)) {
                    kill(b, DeathCause.MELEE, random.nextBoolean(), when);
                    a.guarding = -1;
                    a.warlordOf = slot;
                    a.territory = slot;
                    warlords[slot] = a.id;
                    a.levelUp(1);
                    lines.add(a.fullName() + " killed Warlord " + b.fullName() + " and took their place.");
                } else {
                    kill(a, DeathCause.MELEE, random.nextBoolean(), when);
                    b.levelUp(1);
                    lines.add("Warlord " + b.fullName() + " put down a challenge from " + a.fullName() + ".");
                }
            }
        }
    }

    /** The chance that {@code a} beats {@code b} in a fight between Captains. */
    public static double odds(Captain a, Captain b) {
        double diff = (a.level - b.level) + 0.5 * (strengths(a) - strengths(b));
        return 1.0 / (1.0 + Math.exp(-diff / 3.0));
    }

    private static int strengths(Captain c) {
        int n = 0;
        for (Trait t : c.traits) {
            if (t.kind == Trait.Kind.STRENGTH || t.kind == Trait.Kind.IMMUNITY) n++;
            else if (t.kind == Trait.Kind.WEAKNESS) n--;
        }
        return n;
    }

    /** Dead Captains whose kill wasn't final may come back, scarred, once a day has passed. */
    void returnFromDeath(Random random, long now, boolean shift, List<String> lines) {
        for (Captain c : new ArrayList<>(captains.values())) {
            if (c.alive || c.finalDeath || now - c.diedAt < DAY) continue;
            if (livingCaptains().size() >= MAX_CAPTAINS) break;
            double chance = c.has(Trait.DEATHLESS) ? 0.6 : Math.max(0.1, 0.3 + 0.02 * c.level - 0.1 * c.timesReturned);
            if (!shift) chance *= 0.25;
            if (random.nextDouble() >= chance) {
                if (now - c.diedAt > DAY * 10) c.finalDeath = true; // gone for good after ten days
                continue;
            }
            comeBack(random, c, now);
            lines.add(c.fullName() + " has come back from the dead.");
        }
    }

    public void comeBack(Random random, Captain c, long now) {
        c.alive = true;
        c.timesReturned++;
        Scar scar = c.lastCause.scar();
        if (!c.scars.contains(scar)) c.scars.add(scar);
        Trait learned = random.nextBoolean() ? scar.hardened : scar.haunted;
        if (learned == null) learned = scar.hardened != null ? scar.hardened : scar.haunted;
        if (learned != null) c.addTrait(learned);
        if (random.nextBoolean()) c.epithet = scar.epithet;
        c.levelUp(1);
        Territory t = territories.isEmpty() ? null : territories.get(Math.floorMod(c.territory, territories.size()));
        c.territory = t == null ? 0 : t.index();
    }

    /** Empty Warlord places go to the strongest Captain of that territory; guards and Captains are topped up. */
    void fillPlaces(Random random, long now, List<String> lines) {
        for (int slot = 0; slot < WARLORDS; slot++) {
            if (warlord(slot) != null) continue;
            final int s = slot;
            Captain heir = livingCaptains().stream()
                    .max(Comparator.comparingInt((Captain c) -> (c.guarding == s ? 100 : 0) + (c.territory == s ? 50 : 0) + c.level))
                    .orElse(null);
            if (heir == null) heir = create(random, pickType(random), homeDimension, 10 + random.nextInt(4), null, now);
            heir.guarding = -1;
            heir.warlordOf = slot;
            heir.territory = slot;
            warlords[slot] = heir.id;
            lines.add(heir.fullName() + " is now a Warlord.");
        }
        while (livingCaptains().size() < TARGET_CAPTAINS) {
            Candidate cand = candidates.isEmpty() ? null : candidates.remove(0);
            Captain c = cand == null ? create(random, pickType(random), homeDimension, 1 + random.nextInt(4), null, now)
                    : create(random, cand.entityType(), cand.dimension(), 1 + random.nextInt(3), null, now);
            c.territory = random.nextInt(WARLORDS);
            lines.add(c.fullName() + " has been promoted to Captain.");
        }
        for (int slot = 0; slot < WARLORDS; slot++) {
            if (warlord(slot) == null) continue;
            while (guardsOf(slot).size() < GUARDS_PER_WARLORD) {
                final int s = slot;
                Captain pick = livingCaptains().stream().filter(c -> !c.isGuard() && homeDimension.equals(c.dimension))
                        .max(Comparator.comparingInt((Captain c) -> (c.territory == s ? 100 : 0) + c.level)).orElse(null);
                if (pick == null) break;
                pick.guarding = slot;
                pick.territory = slot;
                lines.add(pick.fullName() + " swears to guard Warlord " + warlord(slot).fullName() + ".");
            }
        }
    }

    /** Keeps a few struggles queued, each due one to four days out. */
    void queueStruggles(Random random, long now) {
        int guard = 0;
        while (struggles.size() < PENDING_STRUGGLES && guard++ < 20) {
            List<Captain> caps = livingCaptains();
            if (caps.isEmpty()) return;
            Captain a = caps.get(random.nextInt(caps.size()));
            if (busy(a.id)) continue;
            long due = now + DAY * (1 + random.nextInt(3)) + random.nextInt((int) (DAY / 2));
            int roll = random.nextInt(100);
            PowerStruggle s = null;
            if (roll < 15 && a.isGuard()) {
                Captain w = warlord(a.guarding);
                if (w != null && a.level >= w.level - 3 && !busy(w.id)) s = new PowerStruggle(nextStruggleId++, PowerStruggle.Type.CHALLENGE, a.id, w.id, due);
            } else if (roll < 40) {
                List<Captain> others = caps.stream().filter(c -> c != a && !busy(c.id)).toList();
                if (!others.isEmpty()) {
                    Captain b = others.get(random.nextInt(others.size()));
                    s = new PowerStruggle(nextStruggleId++, random.nextBoolean() ? PowerStruggle.Type.DUEL : PowerStruggle.Type.AMBUSH, a.id, b.id, due);
                }
            } else if (roll < 60) {
                s = new PowerStruggle(nextStruggleId++, PowerStruggle.Type.FEAST, a.id, null, due);
            } else if (roll < 80) {
                s = new PowerStruggle(nextStruggleId++, PowerStruggle.Type.HUNT, a.id, null, due);
            } else {
                s = new PowerStruggle(nextStruggleId++, PowerStruggle.Type.RECRUIT, a.id, null, due);
            }
            if (s != null) struggles.add(s);
        }
    }

    private boolean busy(UUID id) {
        for (PowerStruggle s : struggles) if (id.equals(s.first()) || id.equals(s.second())) return true;
        return false;
    }

    public Collection<Captain> all() {
        return captains.values();
    }
}
