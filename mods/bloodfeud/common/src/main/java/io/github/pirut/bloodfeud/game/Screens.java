package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.BloodFeud;
import io.github.pirut.bloodfeud.feud.Brute;
import io.github.pirut.bloodfeud.feud.Feud;
import io.github.pirut.bloodfeud.feud.Memory;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.Relation;
import io.github.pirut.bloodfeud.feud.Scar;
import io.github.pirut.bloodfeud.feud.SettlementTier;
import io.github.pirut.bloodfeud.feud.Trait;
import io.github.pirut.lib.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The Ledger and Map screens for players who have Blood Feud on their client: the server sends everything this
 * player knows as one tag, and the client draws it. Players without the mod get the book and map items instead.
 */
public final class Screens {
    /** Server to client: open the screens with this snapshot. */
    public static final ResourceLocation SHOW = BloodFeud.id("show");
    /** Client to server: please send a snapshot (the Ledger key). */
    public static final ResourceLocation ASK = BloodFeud.id("ask");
    /** Server to client: which Brutes near the player bear which scars, so a client with the mod can paint them. */
    public static final ResourceLocation SCARS = BloodFeud.id("scars");

    /** Map terrain: cells per side, and the kinds of ground each cell can be. */
    static final int GRID = 128;
    public static final byte PLAINS = 0, OCEAN = 1, DEEP = 2, RIVER = 3, BEACH = 4, DESERT = 5, BADLANDS = 6,
            FOREST = 7, JUNGLE = 8, TAIGA = 9, SNOW = 10, MOUNTAIN = 11, SWAMP = 12, SAVANNA = 13, NETHER = 14,
            END = 15, MUSHROOM = 16;

    private Screens() {
    }

    /** Opens the screens on the player's client, if it has the mod. False if it doesn't. */
    public static boolean open(Live live, ServerPlayer p, String tab, String focus) {
        if (!Net.canSend(p, SHOW)) return false;
        CompoundTag t = snapshot(live, p);
        t.putString("open", tab == null || tab.isEmpty() ? "ledger" : tab);
        t.putString("focus", focus == null ? "" : focus);
        Net.send(p, SHOW, t);
        return true;
    }

    static CompoundTag snapshot(Live live, ServerPlayer p) {
        Feud f = live.feud();
        UUID me = p.getUUID();
        String dim = Live.dim(p.level());
        CompoundTag t = new CompoundTag();
        CompoundTag self = new CompoundTag();
        self.putString("name", p.getName().getString());
        self.putInt("x", p.getBlockX());
        self.putInt("z", p.getBlockZ());
        self.putFloat("yaw", p.getYRot());
        self.putString("dim", dim);
        self.putLong("day", day(live.now()));
        t.put("me", self);

        ListTag packs = new ListTag();
        int minX = p.getBlockX(), maxX = minX, minZ = p.getBlockZ(), maxZ = minZ;
        for (Pack pk : f.packs.values()) {
            if (!pk.met.contains(me)) continue;
            packs.add(pack(f, pk, me));
            if (pk.dimension.equals(dim)) {
                minX = Math.min(minX, pk.campX());
                maxX = Math.max(maxX, pk.campX());
                minZ = Math.min(minZ, pk.campZ());
                maxZ = Math.max(maxZ, pk.campZ());
            }
        }
        t.put("packs", packs);

        ListTag brutes = new ListTag();
        for (Brute br : f.brutes.values()) {
            Relation r = br.relations.get(me);
            if (r == null || !r.met) continue;
            brutes.add(brute(live, f, br, r, me, p.registryAccess()));
            if (br.alive && br.dimension.equals(dim) && (r.nemesis || me.equals(br.hunting))) {
                minX = Math.min(minX, br.x);
                maxX = Math.max(maxX, br.x);
                minZ = Math.min(minZ, br.z);
                maxZ = Math.max(maxZ, br.z);
            }
        }
        t.put("brutes", brutes);
        t.put("terrain", terrain(p.serverLevel(), minX, maxX, minZ, maxZ));
        return t;
    }

    private static CompoundTag pack(Feud f, Pack pk, UUID me) {
        CompoundTag t = new CompoundTag();
        t.putString("id", pk.id.toString());
        t.putString("name", cap(pk.title().substring(4)));
        t.putString("family", pk.family.key());
        t.putString("familyTitle", cap(pk.family.title));
        t.putString("place", pk.place);
        t.putInt("color", pk.color);
        t.putInt("x", pk.campX());
        t.putInt("z", pk.campZ());
        SettlementTier displayTier = pk.displayTier(BloodFeud.config().buildCamps);
        t.putString("tier", displayTier.key);
        t.putString("tierTitle", displayTier == SettlementTier.NONE ? "" : displayTier.title);
        t.putInt("builtTier", pk.builtTier);
        t.putString("dim", pk.dimension);
        t.putLong("formed", day(pk.formedAt));
        t.putInt("grunts", pk.grunts);
        t.putString("boss", pk.boss == null ? "" : pk.boss.toString());
        Pack top = f.top(pk);
        t.putString("liege", top == pk ? "" : top.id.toString());
        t.putInt("vassals", f.vassals(pk).size());
        t.putDouble("strength", f.strength(pk));
        ListTag members = new ListTag();
        int unknown = 0;
        for (Brute m : f.members(pk)) {
            Relation r = m.relations.get(me);
            if (r != null && r.met) members.add(StringTag.valueOf(m.id.toString()));
            else unknown++;
        }
        t.put("members", members);
        t.putInt("unknown", unknown);

        ListTag rivals = new ListTag();
        int unmet = 0;
        double mine = f.strength(pk);
        for (Pack q : f.packs.values()) {
            if (q == pk || f.top(q) == top || q.distanceTo(pk) > Feud.RIVAL_RANGE) continue;
            if (!q.met.contains(me)) {
                unmet++;
                continue;
            }
            CompoundTag r = new CompoundTag();
            r.putString("id", q.id.toString());
            r.putString("how", q.family == pk.family
                    ? (mine >= f.strength(q) ? "could swallow it" : "could be swallowed by it")
                    : (mine >= f.strength(q) ? "stronger" : "weaker"));
            rivals.add(r);
        }
        t.put("rivals", rivals);
        t.putInt("unmetRivals", unmet);

        int theyKilled = 0, youKilled = 0;
        for (Brute m : f.brutes.values()) {
            if (!pk.id.equals(m.pack)) continue;
            Relation r = m.relations.get(me);
            if (r == null) continue;
            theyKilled += r.killedPlayer;
            youKilled += r.killedByPlayer;
        }
        t.putInt("theyKilled", theyKilled);
        t.putInt("youKilled", youKilled);
        ListTag story = new ListTag();
        for (Pack.Event e : pk.chronicle) story.add(event(day(e.time()), e.text()));
        t.put("story", story);
        return t;
    }

    private static CompoundTag brute(Live live, Feud f, Brute br, Relation r, UUID me, net.minecraft.core.HolderLookup.Provider reg) {
        CompoundTag t = new CompoundTag();
        t.putString("id", br.id.toString());
        t.putString("name", br.name);
        t.putString("full", br.fullName());
        t.putString("epithet", br.epithet == null ? "" : br.epithet);
        t.putString("type", br.entityType);
        t.putString("family", br.family.key());
        t.putInt("level", br.level);
        t.putString("rank", f.rank(br).name());
        t.putString("rankTitle", f.rank(br).title);
        t.putString("standing", f.standing(br));
        t.putBoolean("alive", br.alive);
        t.putBoolean("final", br.finalDeath);
        t.putInt("returned", br.timesReturned);
        Pack pk = f.packOf(br);
        t.putString("pack", pk == null ? "" : pk.id.toString());
        t.putInt("color", pk == null ? -1 : pk.color);
        t.putInt("x", br.x);
        t.putInt("z", br.z);
        t.putString("dim", br.dimension == null ? "" : br.dimension);
        t.putBoolean("nemesis", r.nemesis);
        t.putBoolean("hunting", me.equals(br.hunting));
        t.putInt("killedYou", r.killedPlayer);
        t.putInt("youKilled", r.killedByPlayer);
        Brute boss = pk == null ? null : f.brute(pk.boss);
        t.putBoolean("wantsBoss", boss != null && boss != br && boss.alive && br.level >= boss.level + 2);

        ListTag scars = new ListTag();
        for (Scar s : br.scars) scars.add(StringTag.valueOf(scarName(s)));
        t.put("scars", scars);
        ListTag traits = new ListTag();
        for (Trait tr : br.traits) {
            CompoundTag c = new CompoundTag();
            boolean known = r.known.contains(tr);
            c.putString("kind", tr.kind.name());
            c.putBoolean("known", known);
            if (known) {
                c.putString("key", tr.key());
                c.putString("title", tr.title);
                c.putString("desc", tr.description);
            }
            traits.add(c);
        }
        t.put("traits", traits);
        ListTag carries = new ListTag();
        List<ItemStack> stolen = live.data().stolen.get(br.id);
        ListTag carryItems = new ListTag();
        if (stolen != null) {
            for (ItemStack s : stolen) {
                carries.add(StringTag.valueOf(s.getHoverName().getString()));
                carryItems.add(s.saveOptional(reg));
            }
        }
        t.put("carries", carries);
        t.put("carryItems", carryItems);
        ListTag memories = new ListTag();
        for (Memory m : r.memories) memories.add(event(day(m.time()), FeudCommand.memory(m)));
        t.put("memories", memories);
        return t;
    }

    /** What the ground is like over the area the map needs, sampled from the world's biomes (no chunks loaded). */
    private static CompoundTag terrain(ServerLevel level, int minX, int maxX, int minZ, int maxZ) {
        int span = Math.max(Math.max(maxX - minX, maxZ - minZ) * 5 / 2, 768);
        int cell = Math.max(4, (span + GRID - 1) / GRID);
        int cx = (minX + maxX) / 2, cz = (minZ + maxZ) / 2;
        int x0 = cx - cell * GRID / 2, z0 = cz - cell * GRID / 2;
        byte[] data = new byte[GRID * GRID];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int j = 0; j < GRID; j++) {
            for (int i = 0; i < GRID; i++) {
                int x = x0 + i * cell + cell / 2, z = z0 + j * cell + cell / 2;
                Holder<Biome> b = level.getUncachedNoiseBiome(x >> 2, 63 >> 2, z >> 2);
                data[j * GRID + i] = kind(b, pos.set(x, 64, z));
            }
        }
        CompoundTag t = new CompoundTag();
        t.putInt("x0", x0);
        t.putInt("z0", z0);
        t.putInt("cell", cell);
        t.putInt("n", GRID);
        t.putByteArray("data", data);
        return t;
    }

    private static byte kind(Holder<Biome> b, BlockPos pos) {
        if (b.is(BiomeTags.IS_NETHER)) return NETHER;
        if (b.is(BiomeTags.IS_END)) return END;
        if (b.is(BiomeTags.IS_DEEP_OCEAN)) return DEEP;
        if (b.is(BiomeTags.IS_OCEAN)) return OCEAN;
        if (b.is(BiomeTags.IS_RIVER)) return RIVER;
        if (b.is(BiomeTags.IS_BEACH)) return BEACH;
        if (b.is(Biomes.MUSHROOM_FIELDS)) return MUSHROOM;
        if (b.is(Biomes.DESERT)) return DESERT;
        if (b.is(BiomeTags.IS_BADLANDS)) return BADLANDS;
        if (b.is(Biomes.SWAMP) || b.is(Biomes.MANGROVE_SWAMP)) return SWAMP;
        if (b.is(BiomeTags.IS_MOUNTAIN)) return b.value().coldEnoughToSnow(pos) ? SNOW : MOUNTAIN;
        if (b.value().coldEnoughToSnow(pos)) return SNOW;
        if (b.is(BiomeTags.IS_JUNGLE)) return JUNGLE;
        if (b.is(BiomeTags.IS_SAVANNA)) return SAVANNA;
        if (b.is(BiomeTags.IS_TAIGA)) return TAIGA;
        if (b.is(BiomeTags.IS_FOREST)) return FOREST;
        return PLAINS;
    }

    private static CompoundTag event(long day, String text) {
        CompoundTag c = new CompoundTag();
        c.putLong("day", day);
        c.putString("text", text);
        return c;
    }

    private static String scarName(Scar s) {
        return switch (s) {
            case BURNED -> "Burned";
            case BROKEN -> "A broken leg";
            case BLASTED -> "Half blown apart";
            case PIERCED -> "Full of arrow holes";
            case DROWNED -> "Drowned once";
            case CUT -> "A face full of stitches";
            case MAULED -> "Chewed by wolves";
        };
    }

    private static long day(long time) {
        return time / Feud.DAY + 1;
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
