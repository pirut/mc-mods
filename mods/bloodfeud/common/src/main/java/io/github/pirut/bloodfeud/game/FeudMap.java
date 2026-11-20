package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.BloodFeud;
import io.github.pirut.bloodfeud.feud.Brute;
import io.github.pirut.bloodfeud.feud.Feud;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.Relation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A vanilla map of the feud around the player: every pack home they know, marked with a banner in the pack's color,
 * and a red X where each Brute that's after them was last seen. The map is zoomed out just far enough to hold them.
 */
public final class FeudMap {
    private static final int EDGE = 6;

    private FeudMap() {
    }

    record Mark(String key, int x, int z, String label, int color, boolean enemy) {
    }

    static ItemStack make(Live live, ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        String dim = Live.dim(level);
        Feud f = live.feud();
        UUID me = p.getUUID();
        List<Mark> marks = new ArrayList<>();
        List<Pack> packs = new ArrayList<>();
        for (Pack pk : f.packs.values()) if (pk.met.contains(me) && pk.dimension.equals(dim)) packs.add(pk);
        packs.sort(Comparator.comparingDouble(pk -> pk.distanceTo(dim, p.getX(), p.getZ())));
        boolean campsOn = BloodFeud.config().buildCamps;
        for (Pack pk : packs) {
            Brute boss = f.brute(pk.boss);
            String tierTitle = pk.displayTierTitle(campsOn);
            String tierTag = tierTitle.isEmpty() ? "" : " (" + tierTitle + ")";
            String who = boss == null ? "" : ", " + boss.name + (f.top(pk) == pk && !f.vassals(pk).isEmpty() ? " the King" : "");
            marks.add(new Mark("pack:" + pk.id, pk.campX(), pk.campZ(), cap(pk.title()) + tierTag + who, pk.color, false));
        }
        for (Brute br : f.brutes.values()) {
            Relation r = br.relations.get(me);
            if (!br.alive || r == null || !r.met || !br.dimension.equals(dim)) continue;
            if (r.nemesis || me.equals(br.hunting)) {
                marks.add(new Mark("brute:" + br.id, br.x, br.z, br.name + (r.nemesis ? ", your Nemesis" : ", hunting you"), -1, true));
            }
        }

        // The smallest zoom that holds every mark, else the widest and the most marks.
        int px = p.getBlockX(), pz = p.getBlockZ();
        View view = view(marks, px, pz);
        List<Mark> shown = view.shown;

        ItemStack map = blank(level, view.x, view.z, view.scale);
        MapItem.renderBiomePreviewMap(level, map);
        Map<String, MapDecorations.Entry> deco = new HashMap<>();
        for (Mark m : shown) {
            var type = m.enemy ? MapDecorationTypes.RED_X
                    : new MapBanner(BlockPos.ZERO, DyeColor.byId(m.color), Optional.empty()).getDecoration();
            deco.put(m.key, new MapDecorations.Entry(type, m.x, m.z, 180f));
        }
        map.set(DataComponents.MAP_DECORATIONS, new MapDecorations(deco));
        map.set(DataComponents.CUSTOM_NAME, Component.literal("Map of the Feuds")
                .withStyle(Style.EMPTY.withItalic(false).withColor(0xB2121F).withBold(true)));

        List<Component> lore = new ArrayList<>();
        if (marks.isEmpty()) lore.add(grey("No packs you know of here yet."));
        for (Mark m : shown) {
            int color = m.enemy ? 0xFF5555 : DyeColor.byId(m.color).getTextColor();
            lore.add(Component.literal(m.enemy ? "✖ " : "⚑ ").withStyle(Style.EMPTY.withItalic(false).withColor(color))
                    .append(Component.literal(m.label + " (" + FeudCommand.where(px, pz, m.x, m.z) + ")")
                            .withStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY))));
        }
        if (shown.size() < marks.size()) lore.add(grey((marks.size() - shown.size()) + " more too far for this map."));
        map.set(DataComponents.LORE, new ItemLore(lore.size() > 12 ? lore.subList(0, 12) : lore));
        return map;
    }

    /** Hands the player a fresh map. */
    static void give(Live live, ServerPlayer p) {
        ItemStack map = make(live, p);
        if (!p.getInventory().add(map)) p.drop(map, false);
    }

    record View(int scale, int x, int z, List<Mark> shown) {
    }

    /**
     * The smallest zoom whose map holds every mark (and the player, if it can), centered on them. Past the widest
     * zoom it centers on the player and keeps what fits.
     */
    static View view(List<Mark> marks, int px, int pz) {
        for (int scale = 0; scale <= 4; scale++) {
            int room = (128 - 2 * EDGE) << scale;
            for (boolean withPlayer : new boolean[]{true, false}) {
                int minX = withPlayer ? px : Integer.MAX_VALUE, maxX = withPlayer ? px : Integer.MIN_VALUE;
                int minZ = withPlayer ? pz : Integer.MAX_VALUE, maxZ = withPlayer ? pz : Integer.MIN_VALUE;
                for (Mark m : marks) {
                    minX = Math.min(minX, m.x);
                    maxX = Math.max(maxX, m.x);
                    minZ = Math.min(minZ, m.z);
                    maxZ = Math.max(maxZ, m.z);
                }
                if (minX > maxX) return new View(0, px, pz, List.of());
                if (maxX - minX <= room && maxZ - minZ <= room) {
                    return new View(scale, Math.floorDiv(minX + maxX, 2), Math.floorDiv(minZ + maxZ, 2), marks);
                }
            }
        }
        int half = (64 - EDGE) << 4;
        List<Mark> in = new ArrayList<>();
        for (Mark m : marks) if (Math.abs(m.x - px) <= half && Math.abs(m.z - pz) <= half) in.add(m);
        return new View(4, px, pz, in);
    }

    /** An empty map centered exactly here (vanilla's own maps snap to a grid, which can split a feud in two). */
    private static ItemStack blank(ServerLevel level, int x, int z, int scale) {
        CompoundTag t = new CompoundTag();
        t.putString("dimension", level.dimension().location().toString());
        t.putInt("xCenter", x);
        t.putInt("zCenter", z);
        t.putByte("scale", (byte) scale);
        t.putBoolean("trackingPosition", true);
        t.putBoolean("unlimitedTracking", true);
        t.putBoolean("locked", false);
        MapId id = level.getFreeMapId();
        level.setMapData(id, MapItemSavedData.load(t, level.registryAccess()));
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        map.set(DataComponents.MAP_ID, id);
        return map;
    }

    private static Component grey(String s) {
        return Component.literal(s).withStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.DARK_GRAY));
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
