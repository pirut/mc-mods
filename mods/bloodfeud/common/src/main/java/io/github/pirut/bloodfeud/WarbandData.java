package io.github.pirut.bloodfeud;

import io.github.pirut.bloodfeud.warband.Board;
import io.github.pirut.bloodfeud.warband.Captain;
import io.github.pirut.bloodfeud.warband.DeathCause;
import io.github.pirut.bloodfeud.warband.Memory;
import io.github.pirut.bloodfeud.warband.PowerStruggle;
import io.github.pirut.bloodfeud.warband.Relation;
import io.github.pirut.bloodfeud.warband.Scar;
import io.github.pirut.bloodfeud.warband.Territory;
import io.github.pirut.bloodfeud.warband.Trait;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The Warband, saved with the world (data/bloodfeud_warband.dat in the overworld's folder). */
public final class WarbandData extends SavedData {
    private static final String NAME = "bloodfeud_warband";
    private static final int VERSION = 1;

    public final Board board = new Board();
    /** Items Captains have taken from players, dropped when the Captain dies. */
    public final Map<UUID, List<ItemStack>> stolen = new HashMap<>();

    public static WarbandData get(MinecraftServer server) {
        // The data fix type only matters for files from older game versions; ours are always current.
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WarbandData::new, WarbandData::load, DataFixTypes.LEVEL), NAME);
    }

    public void changed() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("version", VERSION);
        tag.putBoolean("generated", board.generated);
        tag.putInt("nextStruggle", board.nextStruggleId);
        ListTag warlords = new ListTag();
        for (UUID w : board.warlords) warlords.add(StringTag.valueOf(w == null ? "" : w.toString()));
        tag.put("warlords", warlords);
        ListTag territories = new ListTag();
        for (Territory t : board.territories) {
            CompoundTag tt = new CompoundTag();
            tt.putInt("index", t.index());
            tt.putString("dimension", t.dimension());
            tt.putInt("x", t.x());
            tt.putInt("z", t.z());
            tt.putInt("radius", t.radius());
            territories.add(tt);
        }
        tag.put("territories", territories);
        ListTag captains = new ListTag();
        for (Captain c : board.all()) captains.add(saveCaptain(c, registries));
        tag.put("captains", captains);
        ListTag struggles = new ListTag();
        for (PowerStruggle s : board.struggles) {
            CompoundTag st = new CompoundTag();
            st.putInt("id", s.id());
            st.putString("type", s.type().key());
            st.putUUID("first", s.first());
            if (s.second() != null) st.putUUID("second", s.second());
            st.putLong("due", s.due());
            struggles.add(st);
        }
        tag.put("struggles", struggles);
        ListTag candidates = new ListTag();
        for (Board.Candidate c : board.candidates) {
            CompoundTag ct = new CompoundTag();
            ct.putString("type", c.entityType());
            ct.putString("dimension", c.dimension());
            candidates.add(ct);
        }
        tag.put("candidates", candidates);
        return tag;
    }

    private CompoundTag saveCaptain(Captain c, HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", c.id);
        t.putString("type", c.entityType);
        t.putString("dimension", c.dimension);
        t.putString("name", c.name);
        t.putString("epithet", c.epithet == null ? "" : c.epithet);
        t.putInt("level", c.level);
        t.put("traits", strings(c.traits.stream().map(Trait::key).toList()));
        t.put("scars", strings(c.scars.stream().map(Scar::key).toList()));
        t.putInt("territory", c.territory);
        t.putInt("guarding", c.guarding);
        t.putInt("warlordOf", c.warlordOf);
        t.putBoolean("alive", c.alive);
        t.putBoolean("finalDeath", c.finalDeath);
        t.putLong("diedAt", c.diedAt);
        t.putString("lastCause", c.lastCause.key());
        t.putInt("timesReturned", c.timesReturned);
        t.putLong("risenAt", c.risenAt);
        ListTag rels = new ListTag();
        for (Map.Entry<UUID, Relation> e : c.relations.entrySet()) {
            Relation r = e.getValue();
            CompoundTag rt = new CompoundTag();
            rt.putUUID("player", e.getKey());
            rt.putBoolean("nemesis", r.nemesis);
            rt.putBoolean("met", r.met);
            rt.putInt("killedPlayer", r.killedPlayer);
            rt.putInt("killedByPlayer", r.killedByPlayer);
            rt.put("known", strings(r.known.stream().map(Trait::key).toList()));
            ListTag mems = new ListTag();
            for (Memory m : r.memories) {
                CompoundTag mt = new CompoundTag();
                mt.putString("kind", m.kind().key());
                mt.putLong("time", m.time());
                mt.putString("detail", m.detail());
                mems.add(mt);
            }
            rt.put("memories", mems);
            rels.add(rt);
        }
        t.put("relations", rels);
        List<ItemStack> items = stolen.get(c.id);
        if (items != null && !items.isEmpty()) {
            ListTag it = new ListTag();
            for (ItemStack s : items) if (!s.isEmpty()) it.add(s.save(registries));
            t.put("stolen", it);
        }
        return t;
    }

    private static ListTag strings(List<String> values) {
        ListTag list = new ListTag();
        for (String v : values) list.add(StringTag.valueOf(v));
        return list;
    }

    private static WarbandData load(CompoundTag tag, HolderLookup.Provider registries) {
        WarbandData d = new WarbandData();
        Board b = d.board;
        b.generated = tag.getBoolean("generated");
        b.nextStruggleId = Math.max(1, tag.getInt("nextStruggle"));
        ListTag warlords = tag.getList("warlords", Tag.TAG_STRING);
        for (int i = 0; i < Math.min(Board.WARLORDS, warlords.size()); i++) {
            String s = warlords.getString(i);
            b.warlords[i] = s.isEmpty() ? null : UUID.fromString(s);
        }
        for (Tag x : tag.getList("territories", Tag.TAG_COMPOUND)) {
            CompoundTag tt = (CompoundTag) x;
            b.territories.add(new Territory(tt.getInt("index"), tt.getString("dimension"), tt.getInt("x"),
                    tt.getInt("z"), tt.getInt("radius")));
        }
        for (Tag x : tag.getList("captains", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) x;
            Captain c = new Captain(t.getUUID("id"));
            c.entityType = t.getString("type");
            c.dimension = t.getString("dimension");
            c.name = t.getString("name");
            c.epithet = t.getString("epithet");
            c.level = t.getInt("level");
            for (Tag s : t.getList("traits", Tag.TAG_STRING)) {
                Trait tr = Trait.byKey(s.getAsString());
                if (tr != null) c.traits.add(tr);
            }
            for (Tag s : t.getList("scars", Tag.TAG_STRING)) {
                Scar sc = Scar.byKey(s.getAsString());
                if (sc != null) c.scars.add(sc);
            }
            c.territory = t.getInt("territory");
            c.guarding = t.getInt("guarding");
            c.warlordOf = t.getInt("warlordOf");
            c.alive = t.getBoolean("alive");
            c.finalDeath = t.getBoolean("finalDeath");
            c.diedAt = t.getLong("diedAt");
            c.lastCause = DeathCause.byKey(t.getString("lastCause"));
            c.timesReturned = t.getInt("timesReturned");
            c.risenAt = t.getLong("risenAt");
            for (Tag y : t.getList("relations", Tag.TAG_COMPOUND)) {
                CompoundTag rt = (CompoundTag) y;
                Relation r = c.relation(rt.getUUID("player"));
                r.nemesis = rt.getBoolean("nemesis");
                r.met = rt.getBoolean("met");
                r.killedPlayer = rt.getInt("killedPlayer");
                r.killedByPlayer = rt.getInt("killedByPlayer");
                for (Tag s : rt.getList("known", Tag.TAG_STRING)) {
                    Trait tr = Trait.byKey(s.getAsString());
                    if (tr != null) r.known.add(tr);
                }
                for (Tag z : rt.getList("memories", Tag.TAG_COMPOUND)) {
                    CompoundTag mt = (CompoundTag) z;
                    Memory.Kind kind = Memory.Kind.byKey(mt.getString("kind"));
                    if (kind != null) r.remember(new Memory(kind, mt.getLong("time"), mt.getString("detail")));
                }
            }
            if (t.contains("stolen")) {
                List<ItemStack> items = new ArrayList<>();
                for (Tag s : t.getList("stolen", Tag.TAG_COMPOUND)) {
                    ItemStack st = ItemStack.parseOptional(registries, (CompoundTag) s);
                    if (!st.isEmpty()) items.add(st);
                }
                if (!items.isEmpty()) d.stolen.put(c.id, items);
            }
            b.captains.put(c.id, c);
        }
        for (Tag x : tag.getList("struggles", Tag.TAG_COMPOUND)) {
            CompoundTag st = (CompoundTag) x;
            PowerStruggle.Type type = PowerStruggle.Type.byKey(st.getString("type"));
            if (type == null) continue;
            b.struggles.add(new PowerStruggle(st.getInt("id"), type, st.getUUID("first"),
                    st.hasUUID("second") ? st.getUUID("second") : null, st.getLong("due")));
        }
        for (Tag x : tag.getList("candidates", Tag.TAG_COMPOUND)) {
            CompoundTag ct = (CompoundTag) x;
            b.candidates.add(new Board.Candidate(ct.getString("type"), ct.getString("dimension")));
        }
        return d;
    }
}
