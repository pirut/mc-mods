package io.github.pirut.bloodfeud.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** What the server told this player about the feud, read from the snapshot tag. */
final class FeudView {
    record Event(long day, String text) {
    }

    record Rival(String id, String how) {
    }

    record TraitV(String kind, boolean known, String key, String title, String desc) {
    }

    static final class PackV {
        String id, name, family, familyTitle, place, dim, boss, liege;
        int color, x, z, grunts, vassals, unknown, unmetRivals, theyKilled, youKilled;
        long formed;
        double strength;
        final List<String> members = new ArrayList<>();
        final List<Rival> rivals = new ArrayList<>();
        final List<Event> story = new ArrayList<>();
    }

    static final class BruteV {
        String id, name, full, epithet, type, family, rank, rankTitle, standing, pack, dim;
        int level, returned, color, x, z, killedYou, youKilled;
        boolean alive, dead4good, nemesis, hunting, wantsBoss;
        final List<String> scars = new ArrayList<>();
        final List<TraitV> traits = new ArrayList<>();
        final List<String> carries = new ArrayList<>();
        final List<CompoundTag> carryItems = new ArrayList<>();
        final List<Event> memories = new ArrayList<>();

        boolean after() {
            return alive && (nemesis || hunting);
        }
    }

    String meName, meDim, open, focus;
    int meX, meZ;
    float meYaw;
    long day;
    final Map<String, PackV> packs = new LinkedHashMap<>();
    final Map<String, BruteV> brutes = new LinkedHashMap<>();
    int tx0, tz0, cell, n;
    byte[] terrain = new byte[0];

    static FeudView read(CompoundTag t) {
        FeudView v = new FeudView();
        CompoundTag me = t.getCompound("me");
        v.meName = me.getString("name");
        v.meX = me.getInt("x");
        v.meZ = me.getInt("z");
        v.meYaw = me.getFloat("yaw");
        v.meDim = me.getString("dim");
        v.day = me.getLong("day");
        v.open = t.getString("open");
        v.focus = t.getString("focus");
        for (Tag x : t.getList("packs", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            PackV p = new PackV();
            p.id = c.getString("id");
            p.name = c.getString("name");
            p.family = c.getString("family");
            p.familyTitle = c.getString("familyTitle");
            p.place = c.getString("place");
            p.dim = c.getString("dim");
            p.boss = c.getString("boss");
            p.liege = c.getString("liege");
            p.color = c.getInt("color");
            p.x = c.getInt("x");
            p.z = c.getInt("z");
            p.grunts = c.getInt("grunts");
            p.vassals = c.getInt("vassals");
            p.unknown = c.getInt("unknown");
            p.unmetRivals = c.getInt("unmetRivals");
            p.theyKilled = c.getInt("theyKilled");
            p.youKilled = c.getInt("youKilled");
            p.formed = c.getLong("formed");
            p.strength = c.getDouble("strength");
            for (Tag m : c.getList("members", Tag.TAG_STRING)) p.members.add(m.getAsString());
            for (Tag r : c.getList("rivals", Tag.TAG_COMPOUND)) {
                CompoundTag rc = (CompoundTag) r;
                p.rivals.add(new Rival(rc.getString("id"), rc.getString("how")));
            }
            p.story.addAll(events(c.getList("story", Tag.TAG_COMPOUND)));
            v.packs.put(p.id, p);
        }
        for (Tag x : t.getList("brutes", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            BruteV b = new BruteV();
            b.id = c.getString("id");
            b.name = c.getString("name");
            b.full = c.getString("full");
            b.epithet = c.getString("epithet");
            b.type = c.getString("type");
            b.family = c.getString("family");
            b.rank = c.getString("rank");
            b.rankTitle = c.getString("rankTitle");
            b.standing = c.getString("standing");
            b.pack = c.getString("pack");
            b.dim = c.getString("dim");
            b.level = c.getInt("level");
            b.returned = c.getInt("returned");
            b.color = c.getInt("color");
            b.x = c.getInt("x");
            b.z = c.getInt("z");
            b.killedYou = c.getInt("killedYou");
            b.youKilled = c.getInt("youKilled");
            b.alive = c.getBoolean("alive");
            b.dead4good = c.getBoolean("final");
            b.nemesis = c.getBoolean("nemesis");
            b.hunting = c.getBoolean("hunting");
            b.wantsBoss = c.getBoolean("wantsBoss");
            for (Tag s : c.getList("scars", Tag.TAG_STRING)) b.scars.add(s.getAsString());
            for (Tag s : c.getList("carries", Tag.TAG_STRING)) b.carries.add(s.getAsString());
            for (Tag s : c.getList("carryItems", Tag.TAG_COMPOUND)) b.carryItems.add((CompoundTag) s);
            for (Tag tr : c.getList("traits", Tag.TAG_COMPOUND)) {
                CompoundTag tc = (CompoundTag) tr;
                b.traits.add(new TraitV(tc.getString("kind"), tc.getBoolean("known"), tc.getString("key"),
                        tc.getString("title"), tc.getString("desc")));
            }
            b.memories.addAll(events(c.getList("memories", Tag.TAG_COMPOUND)));
            v.brutes.put(b.id, b);
        }
        CompoundTag tr = t.getCompound("terrain");
        v.tx0 = tr.getInt("x0");
        v.tz0 = tr.getInt("z0");
        v.cell = Math.max(1, tr.getInt("cell"));
        v.n = tr.getInt("n");
        v.terrain = tr.getByteArray("data");
        return v;
    }

    private static List<Event> events(ListTag list) {
        List<Event> out = new ArrayList<>();
        for (Tag e : list) {
            CompoundTag c = (CompoundTag) e;
            out.add(new Event(c.getLong("day"), c.getString("text")));
        }
        return out;
    }

    PackV packOf(BruteV b) {
        return b.pack.isEmpty() ? null : packs.get(b.pack);
    }

    /** "312 blocks northwest" from the player. */
    String where(int x, int z) {
        double dx = x - meX, dz = z - meZ;
        int d = (int) Math.round(Math.hypot(dx, dz));
        if (d < 24) return "right here";
        String[] dirs = {"south", "southwest", "west", "northwest", "north", "northeast", "east", "southeast"};
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        int i = (int) Math.round(((angle % 360) + 360) % 360 / 45) % 8;
        return d + " blocks " + dirs[i];
    }
}
