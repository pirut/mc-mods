package io.github.pirut.bloodfeud.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Everything a pack (or a lone Brute) sends out into the world, as one model. An outing is a patrol, an encounter
 * or a garrison: the Brutes that lead it, and its members, every other mob that came with it (escorts, Packcaller
 * grunts, garrison grunts). Rules:
 * <ul>
 * <li>A member carries the saved tag {@value #TAG}; while it does it drops no loot and gives no experience, after an
 * unload and reload too ({@link #onLoad}).</li>
 * <li>An outing with leaders ends when none of them is out any more. One with none (a garrison while its Brutes
 * rest) ends once no player has been near any member for {@link #GRUNTS_LINGER} ticks, or every member is gone.</li>
 * <li>When an outing ends its members go back into the pack: they leave the world, and the pack's count, which
 * sending them out never touched, is unchanged. Nothing it sent out stays behind.</li>
 * <li>A member that rises to a Brute leaves its outing first ({@link #promote}), so the end of the outing never
 * takes the new Brute.</li>
 * </ul>
 */
final class Outings {
    static final String TAG = "bloodfeud.member";
    static final long GRUNTS_LINGER = 400;

    static final class Outing {
        final UUID id = UUID.randomUUID();
        /** The pack it came from, or null for a lone Brute's. */
        final UUID pack;
        final boolean patrol;
        final Set<UUID> leaders = new HashSet<>();
        final Set<UUID> members = new HashSet<>();
        /** For a wave that came out without a Brute (they all rested): its pack rests at least until this. */
        long restWindow;
        long farSince = -1;

        Outing(UUID pack, boolean patrol) {
            this.pack = pack;
            this.patrol = patrol;
        }
    }

    private final Map<UUID, Outing> outings = new HashMap<>();
    private final Map<UUID, Outing> byMember = new HashMap<>();
    private final Map<UUID, Outing> byLeader = new HashMap<>();

    Outing start(UUID pack, boolean patrol) {
        Outing o = new Outing(pack, patrol);
        outings.put(o.id, o);
        return o;
    }

    void addLeader(Outing o, UUID brute) {
        o.leaders.add(brute);
        byLeader.put(brute, o);
    }

    void addMember(Outing o, UUID mob) {
        o.members.add(mob);
        byMember.put(mob, o);
    }

    Outing ofMember(UUID mob) {
        return byMember.get(mob);
    }

    Outing ofLeader(UUID brute) {
        return byLeader.get(brute);
    }

    boolean isMember(UUID mob) {
        return byMember.containsKey(mob);
    }

    /** Whether the pack has anyone out: a leader or a member. */
    boolean packOut(UUID pack) {
        for (Outing o : outings.values()) if (pack.equals(o.pack)) return true;
        return false;
    }

    /** The member died or rose: it is no longer part of its outing. */
    void leave(UUID mob) {
        Outing o = byMember.remove(mob);
        if (o != null) o.members.remove(mob);
    }

    Collection<Outing> all() {
        return new ArrayList<>(outings.values());
    }

    /** It ended: forgets it, and hands back its members for removal. */
    List<UUID> end(Outing o) {
        outings.remove(o.id);
        for (UUID b : o.leaders) if (byLeader.get(b) == o) byLeader.remove(b);
        List<UUID> members = new ArrayList<>(o.members);
        for (UUID m : members) byMember.remove(m);
        o.members.clear();
        return members;
    }

    void clear() {
        outings.clear();
        byMember.clear();
        byLeader.clear();
    }

    /**
     * Whether an outing is over: its leaders are all back, or, with none, its members are all gone or no player has
     * been near one for {@link #GRUNTS_LINGER} ticks ({@code farFor}).
     */
    static boolean over(boolean hadLeaders, boolean anyLeaderOut, int membersLeft, long farFor) {
        if (hadLeaders) return !anyLeaderOut;
        return membersLeft == 0 || farFor >= GRUNTS_LINGER;
    }

    /**
     * How long its pack rests once an outing is over: a wave that came out without its Brutes rests until their rest
     * is over (one wave per rest window), and never less than a minute. -1 for an outing that had Brutes, whose
     * withdrawal rests the pack.
     */
    static long restAfter(Outing o, long gameTime) {
        if (!o.leaders.isEmpty()) return -1;
        return Math.max(gameTime + 1200, o.restWindow);
    }

    /** It joins an outing: tagged (saved with it), and no farm. */
    static void outfit(Mob mob) {
        mob.addTag(TAG);
        CompoundTag tag = new CompoundTag();
        mob.saveWithoutId(tag);
        tag.putString("DeathLootTable", BuiltInLootTables.EMPTY.location().toString());
        mob.load(tag);
        for (EquipmentSlot slot : EquipmentSlot.values()) mob.setDropChance(slot, 0f);
        mob.setCanPickUpLoot(false);
        mob.skipDropExperience();
    }

    /**
     * A member read back from disk: its tag, loot table, drop chances and pickup were saved with it, but skipping
     * experience isn't saved, so it is put back here.
     */
    static void onLoad(Mob mob) {
        if (mob.getTags().contains(TAG)) mob.skipDropExperience();
    }

    /** It rose to a Brute: it is a member no more, and its own loot table is back. */
    static void promote(Mob mob) {
        if (!mob.removeTag(TAG)) return;
        CompoundTag tag = new CompoundTag();
        mob.saveWithoutId(tag);
        tag.putString("DeathLootTable", mob.getType().getDefaultLootTable().location().toString());
        mob.load(tag);
    }
}
