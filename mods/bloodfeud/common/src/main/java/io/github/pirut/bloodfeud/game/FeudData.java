package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.BloodFeud;
import io.github.pirut.bloodfeud.feud.Brute;
import io.github.pirut.bloodfeud.feud.DeathCause;
import io.github.pirut.bloodfeud.feud.Family;
import io.github.pirut.bloodfeud.feud.Feud;
import io.github.pirut.bloodfeud.feud.Memory;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.Relation;
import io.github.pirut.bloodfeud.feud.Scar;
import io.github.pirut.bloodfeud.feud.SettlementTier;
import io.github.pirut.bloodfeud.feud.Trait;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import io.github.pirut.lib.Stores;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Every Brute and pack, saved with the world (data/bloodfeud.dat in the overworld's folder). */
public final class FeudData extends SavedData {
    private static final String NAME = "bloodfeud";
    private static final int VERSION = 1;

    public final Feud feud = new Feud(new Random());
    /** Items Brutes have taken from players, dropped when the Brute dies. */
    public final Map<UUID, List<ItemStack>> stolen = new HashMap<>();
    /** Players who have been handed a Feud Ledger, so they get one only once. */
    public final java.util.Set<UUID> ledgerGiven = new java.util.HashSet<>();

    public static FeudData get(MinecraftServer server) {
        FeudData d = Stores.get(server, NAME, FeudData::new, FeudData::load);
        d.feud.maxPacks = BloodFeud.config().maxPacks;
        return d;
    }

    public void changed() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("version", VERSION);
        tag.putLong("lastNight", feud.lastNight);
        ListTag brutes = new ListTag();
        for (Brute b : feud.brutes.values()) brutes.add(saveBrute(b, registries));
        tag.put("brutes", brutes);
        ListTag packs = new ListTag();
        for (Pack p : feud.packs.values()) packs.add(savePack(p));
        tag.put("packs", packs);
        ListTag abandoned = new ListTag();
        for (Feud.AbandonedSite s : feud.abandonedSites) {
            CompoundTag st = new CompoundTag();
            st.putUUID("id", s.packId());
            st.putString("family", s.family().key());
            st.putInt("color", s.color());
            st.putString("dimension", s.dimension());
            st.putInt("siteX", s.siteX());
            st.putInt("siteY", s.siteY());
            st.putInt("siteZ", s.siteZ());
            st.putInt("siteFacing", s.siteFacing());
            st.putInt("builtTier", s.builtTier());
            abandoned.add(st);
        }
        tag.put("abandonedSites", abandoned);
        ListTag given = new ListTag();
        for (UUID u : ledgerGiven) given.add(StringTag.valueOf(u.toString()));
        tag.put("ledgerGiven", given);
        return tag;
    }

    private CompoundTag saveBrute(Brute b, HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", b.id);
        t.putString("type", b.entityType);
        t.putString("family", b.family.key());
        t.putString("dimension", b.dimension);
        t.putString("name", b.name);
        t.putString("epithet", b.epithet == null ? "" : b.epithet);
        t.putInt("level", b.level);
        t.putFloat("size", b.size);
        t.putInt("look", b.look);
        t.put("traits", strings(b.traits.stream().map(Trait::key).toList()));
        t.put("scars", strings(b.scars.stream().map(Scar::key).toList()));
        if (b.pack != null) t.putUUID("pack", b.pack);
        t.putInt("x", b.x);
        t.putInt("z", b.z);
        t.putBoolean("alive", b.alive);
        t.putBoolean("finalDeath", b.finalDeath);
        t.putLong("diedAt", b.diedAt);
        if (b.lastCause != null) t.putString("lastCause", b.lastCause.key());
        if (b.killedBy != null) t.putUUID("killedBy", b.killedBy);
        t.putInt("timesReturned", b.timesReturned);
        t.putLong("risenAt", b.risenAt);
        if (b.hunting != null) t.putUUID("hunting", b.hunting);
        ListTag rels = new ListTag();
        for (Map.Entry<UUID, Relation> e : b.relations.entrySet()) {
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
        List<ItemStack> items = stolen.get(b.id);
        if (items != null && !items.isEmpty()) {
            ListTag it = new ListTag();
            for (ItemStack s : items) if (!s.isEmpty()) it.add(s.save(registries));
            t.put("stolen", it);
        }
        return t;
    }

    private static CompoundTag savePack(Pack p) {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", p.id);
        t.putString("family", p.family.key());
        t.putString("dimension", p.dimension);
        t.putString("place", p.place);
        t.putInt("color", p.color);
        t.putInt("homeX", p.homeX);
        t.putInt("homeZ", p.homeZ);
        if (p.boss != null) t.putUUID("boss", p.boss);
        if (p.liege != null) t.putUUID("liege", p.liege);
        t.putInt("grunts", p.grunts);
        t.putLong("formedAt", p.formedAt);
        t.putLong("lastAwake", p.lastAwake);
        t.putInt("playerKills", p.playerKills);
        t.putString("tier", (p.tier == null ? SettlementTier.CAMP : p.tier).key);
        t.putInt("builtTier", p.builtTier);
        t.putBoolean("ruined", p.ruined);
        t.putBoolean("builtRuined", p.builtRuined);
        t.putBoolean("siteSet", p.siteSet);
        if (p.siteSet) {
            t.putInt("siteX", p.siteX);
            t.putInt("siteY", p.siteY);
            t.putInt("siteZ", p.siteZ);
            t.putInt("siteFacing", p.siteFacing);
        }
        if (p.bannersDirty) t.putBoolean("bannersDirty", true);
        ListTag met = new ListTag();
        for (UUID u : p.met) met.add(StringTag.valueOf(u.toString()));
        t.put("met", met);
        ListTag chronicle = new ListTag();
        for (Pack.Event e : p.chronicle) {
            CompoundTag c = new CompoundTag();
            c.putLong("t", e.time());
            c.putString("s", e.text());
            chronicle.add(c);
        }
        t.put("chronicle", chronicle);
        return t;
    }

    private static FeudData load(CompoundTag tag, HolderLookup.Provider registries) {
        FeudData d = new FeudData();
        Feud f = d.feud;
        f.lastNight = tag.getLong("lastNight");
        for (Tag u : tag.getList("ledgerGiven", Tag.TAG_STRING)) d.ledgerGiven.add(UUID.fromString(u.getAsString()));
        for (Tag raw : tag.getList("brutes", Tag.TAG_COMPOUND)) {
            try {
                CompoundTag t = (CompoundTag) raw;
                Family family = Family.byKey(t.getString("family"));
                if (family == null) continue;
                Brute b = new Brute(t.getUUID("id"), t.getString("type"), family, t.getString("dimension"),
                        t.getString("name"), t.getLong("risenAt"));
                b.epithet = t.getString("epithet");
                b.level = t.getInt("level");
                b.size = t.contains("size") ? t.getFloat("size") : 0.86f + (b.id.hashCode() & 0xFF) / 255f * 0.3f;
                b.look = t.contains("look") ? t.getInt("look") : b.id.hashCode() & 0xFFFF;
                for (Tag s : t.getList("traits", Tag.TAG_STRING)) {
                    Trait tr = Trait.byKey(s.getAsString());
                    if (tr != null) b.traits.add(tr);
                }
                for (Tag s : t.getList("scars", Tag.TAG_STRING)) {
                    Scar sc = Scar.byKey(s.getAsString());
                    if (sc != null) b.scars.add(sc);
                }
                if (t.hasUUID("pack")) b.pack = t.getUUID("pack");
                b.x = t.getInt("x");
                b.z = t.getInt("z");
                b.alive = t.getBoolean("alive");
                b.finalDeath = t.getBoolean("finalDeath");
                b.diedAt = t.getLong("diedAt");
                if (t.contains("lastCause")) b.lastCause = DeathCause.byKey(t.getString("lastCause"));
                if (t.hasUUID("killedBy")) b.killedBy = t.getUUID("killedBy");
                b.timesReturned = t.getInt("timesReturned");
                if (t.hasUUID("hunting")) b.hunting = t.getUUID("hunting");
                for (Tag rr : t.getList("relations", Tag.TAG_COMPOUND)) {
                    CompoundTag rt = (CompoundTag) rr;
                    Relation r = b.relation(rt.getUUID("player"));
                    r.nemesis = rt.getBoolean("nemesis");
                    r.met = rt.getBoolean("met");
                    r.killedPlayer = rt.getInt("killedPlayer");
                    r.killedByPlayer = rt.getInt("killedByPlayer");
                    for (Tag s : rt.getList("known", Tag.TAG_STRING)) {
                        Trait tr = Trait.byKey(s.getAsString());
                        if (tr != null) r.known.add(tr);
                    }
                    for (Tag mm : rt.getList("memories", Tag.TAG_COMPOUND)) {
                        CompoundTag mt = (CompoundTag) mm;
                        Memory.Kind k = Memory.Kind.byKey(mt.getString("kind"));
                        if (k != null) r.remember(new Memory(k, mt.getLong("time"), mt.getString("detail")));
                    }
                }
                if (t.contains("stolen")) {
                    List<ItemStack> items = new ArrayList<>();
                    for (Tag it : t.getList("stolen", Tag.TAG_COMPOUND)) {
                        ItemStack s = ItemStack.parseOptional(registries, (CompoundTag) it);
                        if (!s.isEmpty()) items.add(s);
                    }
                    d.stolen.put(b.id, items);
                }
                f.brutes.put(b.id, b);
            } catch (RuntimeException e) {
                BloodFeud.LOG.warn("Skipped a Brute that couldn't be read: {}", e.toString());
            }
        }
        f.campsEnabled = BloodFeud.config().buildCamps;
        List<Pack> legacyPacks = new ArrayList<>();
        for (Tag raw : tag.getList("packs", Tag.TAG_COMPOUND)) {
            try {
                CompoundTag t = (CompoundTag) raw;
                Family family = Family.byKey(t.getString("family"));
                if (family == null) continue;
                Pack p = new Pack(t.getUUID("id"), family, t.getString("dimension"), t.getString("place"),
                        t.getInt("color"), t.getInt("homeX"), t.getInt("homeZ"), t.getLong("formedAt"));
                if (t.hasUUID("boss")) p.boss = t.getUUID("boss");
                if (t.hasUUID("liege")) p.liege = t.getUUID("liege");
                p.grunts = t.getInt("grunts");
                p.lastAwake = t.getLong("lastAwake");
                p.playerKills = t.getInt("playerKills");
                if (t.contains("tier")) {
                    SettlementTier parsed = SettlementTier.byKey(t.getString("tier"));
                    p.tier = parsed != null ? parsed : SettlementTier.CAMP;
                    p.builtTier = Math.max(0, Math.min(SettlementTier.TOWN.level, t.getInt("builtTier")));
                    p.ruined = t.getBoolean("ruined");
                    p.builtRuined = t.getBoolean("builtRuined");
                    p.siteSet = t.getBoolean("siteSet");
                    if (p.siteSet) {
                        p.siteX = t.getInt("siteX");
                        p.siteY = t.getInt("siteY");
                        p.siteZ = t.getInt("siteZ");
                        p.siteFacing = Math.floorMod(t.getInt("siteFacing"), 4);
                    }
                    p.bannersDirty = t.getBoolean("bannersDirty");
                } else {
                    p.builtTier = 0;
                    p.siteSet = false;
                    legacyPacks.add(p);
                }
                for (Tag s : t.getList("met", Tag.TAG_STRING)) p.met.add(UUID.fromString(s.getAsString()));
                for (Tag c : t.getList("chronicle", Tag.TAG_COMPOUND)) {
                    p.remember(((CompoundTag) c).getLong("t"), ((CompoundTag) c).getString("s"));
                }
                f.packs.put(p.id, p);
            } catch (RuntimeException e) {
                BloodFeud.LOG.warn("Skipped a pack that couldn't be read: {}", e.toString());
            }
        }
        for (Pack p : legacyPacks) {
            SettlementTier sustained = SettlementTier.forPack(f.members(p).size(), p.grunts, f.strength(p), !f.vassals(p).isEmpty());
            p.tier = sustained == SettlementTier.NONE ? SettlementTier.CAMP : sustained;
            p.ruined = sustained == SettlementTier.NONE;
        }
        for (Tag raw : tag.getList("abandonedSites", Tag.TAG_COMPOUND)) {
            try {
                CompoundTag st = (CompoundTag) raw;
                Family fam = Family.byKey(st.getString("family"));
                if (fam == null || !st.hasUUID("id")) continue;
                int bt = Math.max(1, Math.min(SettlementTier.TOWN.level, st.getInt("builtTier")));
                f.abandonedSites.add(new Feud.AbandonedSite(
                        st.getUUID("id"), fam, st.getInt("color"), st.getString("dimension"),
                        st.getInt("siteX"), st.getInt("siteY"), st.getInt("siteZ"),
                        Math.floorMod(st.getInt("siteFacing"), 4), bt));
            } catch (RuntimeException e) {
                BloodFeud.LOG.warn("Skipped an abandoned site that couldn't be read: {}", e.toString());
            }
        }
        return d;
    }

    private static ListTag strings(List<String> values) {
        ListTag l = new ListTag();
        for (String v : values) l.add(StringTag.valueOf(v));
        return l;
    }
}
