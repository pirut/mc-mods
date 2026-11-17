package io.github.pirut.bloodfeud.client;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.pirut.bloodfeud.BloodFeud;
import io.github.pirut.bloodfeud.client.FeudView.BruteV;
import io.github.pirut.bloodfeud.client.FeudView.Event;
import io.github.pirut.bloodfeud.client.FeudView.PackV;
import io.github.pirut.bloodfeud.client.FeudView.Rival;
import io.github.pirut.bloodfeud.client.FeudView.TraitV;
import io.github.pirut.bloodfeud.game.Screens;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Feud Ledger, as a real screen: an overview, every pack and Brute you know with their pages, and a map you can
 * drag and zoom. Names are links; hover a trait for what it does; click a banner on the map to open its pack.
 */
final class LedgerScreen extends Screen {
    // Ink on parchment
    private static final int LEATHER = 0xFF3A2216, LEATHER_HI = 0xFF5A3624, PAPER = 0xFFEDE2C8, PAPER_DARK = 0xFFDCCDAA,
            PAPER_LINE = 0xFFC9B68F, INK = 0xFF2B1A12, FADED = 0xFF7A6652, BLOOD = 0xFF9A1222, BRUTE = 0xFF9A5A00,
            KING = 0xFF6A1E9A, GOOD = 0xFF1E6B1E, LINK = 0xFF1B3F8E, HOVER = 0x30FFFFFF;
    private static final String[] TABS = {"Overview", "Packs", "Brutes", "Map"};
    private static final ResourceLocation TERRAIN = BloodFeud.id("ledger_terrain");

    private FeudView view;
    private int tab;
    private String selPack, selBrute;
    private double listScroll, docScroll;
    private Doc doc;
    private int docWidth = -1;
    private final Map<String, LivingEntity> portraits = new HashMap<>();
    private DynamicTexture terrain;
    // Map view: block at the center of the map area, and pixels per block
    private double mapX, mapZ, zoom = -1;
    private boolean dragging;

    // Layout, worked out in init()
    private int px, py, pw, ph, bodyY, listW;

    LedgerScreen(FeudView view) {
        super(Component.literal("Feud Ledger"));
        update(view);
    }

    void update(FeudView v) {
        this.view = v;
        tab = switch (v.open) {
            case "map" -> 3;
            default -> tab;
        };
        if (v.focus.startsWith("pack:")) {
            selPack = v.focus.substring(5);
            tab = 1;
        } else if (v.focus.startsWith("brute:")) {
            selBrute = v.focus.substring(6);
            tab = 2;
        }
        if (selPack == null || !v.packs.containsKey(selPack)) selPack = packs().isEmpty() ? null : packs().get(0).id;
        if (selBrute == null || !v.brutes.containsKey(selBrute)) selBrute = brutes().isEmpty() ? null : brutes().get(0).id;
        if (terrain != null) {
            terrain.close();
            terrain = null;
        }
        zoom = -1;
        doc = null;
    }

    @Override
    protected void init() {
        pw = Math.min(width - 12, 470);
        ph = Math.min(height - 12, 290);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        bodyY = py + 26;
        listW = Math.max(110, Math.min(150, pw / 3));
        doc = null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        if (terrain != null) {
            minecraft.getTextureManager().release(TERRAIN);
            terrain = null;
        }
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        // Leather cover and paper
        g.fill(px - 4, py - 4, px + pw + 4, py + ph + 4, LEATHER);
        g.fill(px - 3, py - 3, px + pw + 3, py - 2, LEATHER_HI);
        g.fill(px, py, px + pw, py + ph, PAPER);
        g.fill(px, py, px + pw, py + 22, PAPER_DARK);
        g.fill(px, py + 22, px + pw, py + 23, PAPER_LINE);
        int tw = g.drawString(font, Component.literal("☠ Feud Ledger").withStyle(s -> s.withBold(true)), px + 8, py + 7, BLOOD, false);
        String sub = "Day " + view.day + " · " + view.meName;
        if (pw > 360) g.drawString(font, sub, tw + 10, py + 7, FADED, false);
        // Tabs, right-aligned
        int tx = px + pw - 6;
        for (int i = TABS.length - 1; i >= 0; i--) {
            int w = font.width(TABS[i]) + 12;
            tx -= w;
            boolean hot = in(mx, my, tx, py + 3, w, 18);
            if (i == tab) g.fill(tx, py + 3, tx + w, py + 23, PAPER);
            else if (hot) g.fill(tx, py + 3, tx + w, py + 22, 0x20000000);
            g.drawString(font, TABS[i], tx + 6, py + 9, i == tab ? BLOOD : hot ? INK : FADED, false);
            tx -= 2;
        }
        switch (tab) {
            case 0 -> renderDoc(g, mx, my, px + 10, bodyY, pw - 20, py + ph - bodyY - 6);
            case 1, 2 -> {
                renderList(g, mx, my);
                g.fill(px + listW, bodyY, px + listW + 1, py + ph - 6, PAPER_LINE);
                renderDoc(g, mx, my, px + listW + 10, bodyY, pw - listW - 18, py + ph - bodyY - 6);
            }
            case 3 -> renderMap(g, mx, my);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
    }

    // ------------------------------------------------------------------ lists

    private List<PackV> packs() {
        List<PackV> out = new ArrayList<>(view.packs.values());
        out.sort(Comparator.comparingDouble(p -> p.dim.equals(view.meDim) ? Math.hypot(p.x - view.meX, p.z - view.meZ) : 1e12));
        return out;
    }

    private List<BruteV> brutes() {
        List<BruteV> out = new ArrayList<>(view.brutes.values());
        out.sort(Comparator.comparingInt((BruteV b) -> b.after() ? 0 : b.alive ? 1 : 2)
                .thenComparingInt(b -> -b.level).thenComparing(b -> b.name));
        return out;
    }

    private record Entry(String id, ItemStack icon, String title, int color, String sub, int subColor) {
    }

    private List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        if (tab == 1) {
            for (PackV p : packs()) {
                BruteV boss = view.brutes.get(p.boss);
                String sub = (boss != null ? boss.name + " · " : "") + where(p.dim, p.x, p.z);
                out.add(new Entry(p.id, banner(p.color), p.name, p.liege.isEmpty() && p.vassals > 0 ? KING : INK, sub, FADED));
            }
        } else {
            for (BruteV b : brutes()) {
                String sub = !b.alive ? (b.dead4good ? "Dead for good" : "Dead, for now")
                        : b.nemesis ? "Your Nemesis · Lv " + b.level : b.hunting ? "Hunting you · Lv " + b.level
                        : "Lv " + b.level + " " + b.rankTitle;
                out.add(new Entry(b.id, egg(b.type), b.full, b.alive ? rankInk(b.rank) : FADED, sub,
                        b.after() ? BLOOD : FADED));
            }
        }
        return out;
    }

    private void renderList(GuiGraphics g, int mx, int my) {
        List<Entry> es = entries();
        int x = px + 4, y0 = bodyY, h = py + ph - 6 - bodyY, rowH = 24;
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, es.size() * rowH - h));
        g.enableScissor(x, y0, px + listW, y0 + h);
        String sel = tab == 1 ? selPack : selBrute;
        if (es.isEmpty()) {
            g.drawWordWrap(font, FormattedText.of(tab == 1 ? "No packs yet. Packs form where you fight."
                    : "No Brutes yet. Kill a few mobs while their friends watch."), x + 4, y0 + 4, listW - 12, FADED);
        }
        for (int i = 0; i < es.size(); i++) {
            Entry e = es.get(i);
            int y = y0 + i * rowH - (int) listScroll;
            if (y + rowH < y0 || y > y0 + h) continue;
            boolean selected = e.id.equals(sel);
            if (selected) g.fill(x, y, px + listW - 2, y + rowH - 1, PAPER_DARK);
            else if (in(mx, my, x, y, listW - 6, rowH - 1) && my >= y0 && my < y0 + h) g.fill(x, y, px + listW - 2, y + rowH - 1, 0x18000000);
            g.renderItem(e.icon, x + 2, y + 4);
            int tw = listW - 30;
            g.drawString(font, clip(e.title, tw), x + 22, y + 3, e.color, false);
            g.drawString(font, clip(e.sub, tw), x + 22, y + 13, e.subColor, false);
        }
        g.disableScissor();
    }

    // ------------------------------------------------------------------ the page on the right

    /** A laid-out page: lines of text with optional link, icon, tooltip and bar. */
    private static final class Doc {
        final List<Line> lines = new ArrayList<>();
        int height;
        int reserveH, reserveW; // room kept at the top right for a picture
    }

    private record Line(int y, int h, int indent, FormattedCharSequence text, int color, String link, ItemStack icon,
                        List<Component> tip, float bar, int barColor) {
    }

    private final class DocBuilder {
        final Doc d = new Doc();
        final int width;

        DocBuilder(int width, int reserveW, int reserveH) {
            this.width = width;
            d.reserveW = reserveW;
            d.reserveH = reserveH;
        }

        private int widthAt(int indent) {
            return (d.height < d.reserveH ? width - d.reserveW : width) - indent;
        }

        DocBuilder text(Component c, int color, String link, List<Component> tip, int indent) {
            for (FormattedCharSequence s : font.split(c, Math.max(40, widthAt(indent)))) {
                d.lines.add(new Line(d.height, 10, indent, s, color, link, null, tip, -1, 0));
                d.height += 10;
            }
            return this;
        }

        DocBuilder text(String s, int color) {
            return text(Component.literal(s), color, null, null, 0);
        }

        DocBuilder bold(String s, int color) {
            return text(Component.literal(s).withStyle(st -> st.withBold(true)), color, null, null, 0);
        }

        DocBuilder heading(String s) {
            gap(4);
            text(Component.literal(s.toUpperCase()).withStyle(st -> st.withBold(true)), BLOOD, null, null, 0);
            d.height += 1;
            return this;
        }

        DocBuilder link(String s, int color, String link) {
            return text(Component.literal(s).withStyle(Style.EMPTY.withUnderlined(true)), color, link, null, 0);
        }

        /** A row with an item icon: a title (maybe a link) and a short note. */
        DocBuilder iconRow(ItemStack icon, String title, int color, String link, String note, int noteColor) {
            d.lines.add(new Line(d.height, 20, 20, Component.literal(title)
                    .withStyle(link == null ? Style.EMPTY : Style.EMPTY.withUnderlined(true)).getVisualOrderText(),
                    color, link, icon, null, -1, 0));
            if (note != null && !note.isEmpty()) {
                d.lines.add(new Line(d.height + 10, 10, 20, Component.literal(note).getVisualOrderText(), noteColor,
                        null, null, null, -1, 0));
            }
            d.height += 21;
            return this;
        }

        DocBuilder bar(String label, float value, int color) {
            return bar(label, value, color, 0);
        }

        DocBuilder bar(String label, float value, int color, int indent) {
            d.lines.add(new Line(d.height, 12, indent, Component.literal(label).getVisualOrderText(), FADED, null, null,
                    null, Mth.clamp(value, 0, 1), color));
            d.height += 13;
            return this;
        }

        DocBuilder gap(int h) {
            if (d.height > 0) d.height += h;
            return this;
        }

        Doc done() {
            d.height = Math.max(d.height, d.reserveH);
            return d;
        }
    }

    private Doc buildDoc(int width) {
        return switch (tab) {
            case 0 -> overview(width);
            case 1 -> selPack == null ? empty(width) : packPage(width, view.packs.get(selPack));
            case 2 -> selBrute == null ? empty(width) : brutePage(width, view.brutes.get(selBrute));
            default -> empty(width);
        };
    }

    private Doc empty(int width) {
        return new DocBuilder(width, 0, 0).text("Nothing here yet.", FADED).done();
    }

    private Doc overview(int width) {
        DocBuilder b = new DocBuilder(width, 0, 0);
        long kings = view.packs.values().stream().filter(p -> p.liege.isEmpty() && p.vassals > 0).count();
        long alive = view.brutes.values().stream().filter(x -> x.alive).count();
        b.bold("The feud so far", INK);
        b.text(view.packs.size() + (view.packs.size() == 1 ? " pack" : " packs") + " known" + (kings > 0 ? ", "
                + kings + (kings == 1 ? " King" : " Kings") + " among them" : "") + ". " + view.brutes.size()
                + " Brutes met, " + alive + " still alive.", FADED);

        List<BruteV> after = brutes().stream().filter(BruteV::after).toList();
        b.heading("Who's after you");
        if (after.isEmpty()) b.text("No one, for now.", FADED);
        for (BruteV x : after) {
            b.iconRow(egg(x.type), x.full, BLOOD, "brute:" + x.id, (x.nemesis ? "Your Nemesis" : "Hunting you") + " · Lv "
                    + x.level + (x.killedYou == 0 ? " · hasn't killed you yet" : " · killed you " + times(x.killedYou)) + " · " + where(x.dim, x.x, x.z), FADED);
        }

        List<PackV> strong = new ArrayList<>(view.packs.values());
        strong.sort(Comparator.comparingDouble((PackV p) -> -p.strength));
        if (!strong.isEmpty()) {
            b.heading("Strongest packs");
            double top = strong.get(0).strength;
            for (PackV p : strong.subList(0, Math.min(4, strong.size()))) {
                BruteV boss = view.brutes.get(p.boss);
                b.iconRow(banner(p.color), p.name, p.liege.isEmpty() && p.vassals > 0 ? KING : LINK, "pack:" + p.id,
                        (boss != null ? (p.vassals > 0 ? "King " : "Boss ") + boss.name + " · " : "")
                                + plural(p.members.size(), "Brute") + ", " + plural(p.grunts, "grunt") + " · " + where(p.dim, p.x, p.z), FADED);
                b.bar("strength", (float) (p.strength / Math.max(0.01, top)), 0xFFB2121F, 20);
                b.gap(3);
            }
        }

        List<String[]> news = new ArrayList<>();
        List<Long> days = new ArrayList<>();
        for (PackV p : view.packs.values()) {
            for (Event e : p.story) {
                boolean dup = false;
                for (String[] n : news) if (n[0].equals(e.text())) dup = true;
                if (dup) continue;
                news.add(new String[]{e.text(), p.id});
                days.add(e.day());
            }
        }
        if (!news.isEmpty()) {
            b.heading("Latest news");
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < news.size(); i++) order.add(i);
            order.sort(Comparator.comparingLong((Integer i) -> -days.get(i)).thenComparingInt(i -> -i));
            for (int i : order.subList(0, Math.min(10, order.size()))) {
                b.text(Component.literal("Day " + days.get(i) + "  ").withStyle(s -> s.withColor(BLOOD & 0xFFFFFF))
                        .append(Component.literal(news.get(i)[0]).withStyle(s -> s.withColor(INK & 0xFFFFFF))), INK,
                        "pack:" + news.get(i)[1], null, 0);
                b.gap(2);
            }
        }
        b.heading("How it works");
        b.text("Mobs that watch you kill their kind, or that kill you, rise as Brutes. Brutes band into packs, grow "
                + "every night and fight each other. Fight a Brute to learn its strengths and weaknesses. Press "
                + FeudClient.LEDGER.getTranslatedKeyMessage().getString() + " to open this Ledger anywhere.", FADED);
        return b.done();
    }

    private Doc packPage(int width, PackV p) {
        if (p == null) return empty(width);
        DocBuilder b = new DocBuilder(width, 44, 40);
        boolean king = p.liege.isEmpty() && p.vassals > 0;
        b.bold(p.name, king ? KING : INK);
        b.text(p.familyTitle + " · formed day " + p.formed, FADED);
        b.text("Home: " + p.place + ", " + where(p.dim, p.x, p.z) + " (" + p.x + ", " + p.z + ")", INK);
        if (!p.liege.isEmpty()) {
            PackV top = view.packs.get(p.liege);
            BruteV k = top == null ? null : view.brutes.get(top.boss);
            if (k != null) b.text(Component.literal("Kneels to King ").append(Component.literal(k.full)
                    .withStyle(Style.EMPTY.withUnderlined(true))), KING, "brute:" + k.id, null, 0);
            else b.text("Kneels to " + (top == null ? "a King you haven't met" : top.name), KING);
        }
        if (p.vassals > 0) b.text("Rules " + p.vassals + (p.vassals == 1 ? " other pack" : " other packs") + " as King", KING);
        b.link("Show on the map", LINK, "map:" + p.id);
        double max = view.packs.values().stream().mapToDouble(q -> q.strength).max().orElse(1);
        b.gap(3);
        b.bar("Strength", (float) (p.strength / Math.max(0.01, max)), 0xFFB2121F);

        b.heading(king ? "King" : "Boss");
        BruteV boss = view.brutes.get(p.boss);
        if (boss == null) b.text(p.boss.isEmpty() ? "No one" : "??? You haven't met it", FADED);
        else b.iconRow(egg(boss.type), boss.full, rankInk(boss.rank), "brute:" + boss.id, "Lv " + boss.level
                + (boss.alive ? "" : ", dead for now") + (boss.nemesis ? " · YOUR NEMESIS" : ""), boss.nemesis ? BLOOD : FADED);

        b.heading("Brutes");
        int shown = 0;
        for (String id : p.members) {
            BruteV m = view.brutes.get(id);
            if (m == null || id.equals(p.boss)) continue;
            shown++;
            b.iconRow(egg(m.type), m.full, m.alive ? BRUTE : FADED, "brute:" + m.id, "Lv " + m.level
                    + (m.wantsBoss ? " · wants to be Boss" : "") + (m.nemesis ? " · your Nemesis" : ""), m.wantsBoss ? BLOOD : FADED);
        }
        if (shown == 0 && p.unknown == 0) b.text("None besides the Boss", FADED);
        if (p.unknown > 0) b.text(p.unknown + (p.unknown == 1 ? " more you haven't met" : " more you haven't met"), FADED);

        b.heading("Grunts");
        b.text(plural(p.grunts, "grunt") + ", in " + DyeColor.byId(p.color).getName().replace('_', ' ') + " caps", INK);

        if (!p.rivals.isEmpty() || p.unmetRivals > 0) {
            b.heading("Rivals");
            for (Rival r : p.rivals) {
                PackV q = view.packs.get(r.id());
                if (q == null) continue;
                b.iconRow(banner(q.color), q.name, LINK, "pack:" + q.id, r.how(), FADED);
            }
            if (p.unmetRivals > 0) b.text(p.unmetRivals + " more you don't know", FADED);
        }

        b.heading("Your grudge");
        b.text(p.youKilled == 0 && p.theyKilled == 0 ? "No blood between you yet."
                : (p.youKilled == 0 ? "You haven't killed one of its Brutes yet." : "You killed its Brutes " + times(p.youKilled) + ".")
                + (p.theyKilled == 0 ? " They never killed you." : " They killed you " + times(p.theyKilled) + "."), INK);

        if (!p.story.isEmpty()) {
            b.heading("Its story");
            for (int i = p.story.size() - 1; i >= 0; i--) {
                Event e = p.story.get(i);
                b.text(Component.literal("Day " + e.day() + "  ").withStyle(s -> s.withColor(BLOOD & 0xFFFFFF))
                        .append(Component.literal(e.text()).withStyle(s -> s.withColor(INK & 0xFFFFFF))), INK, null, null, 0);
                b.gap(2);
            }
        }
        return b.done();
    }

    private Doc brutePage(int width, BruteV x) {
        if (x == null) return empty(width);
        DocBuilder b = new DocBuilder(width, 80, 96);
        b.bold(x.name, rankInk(x.rank));
        if (!x.epithet.isEmpty()) b.text(x.epithet, rankInk(x.rank));
        b.text("Level " + x.level + " " + x.rankTitle, INK);
        b.bar("Level", x.level / 20f, rankInk(x.rank));
        PackV p = view.packOf(x);
        if (p != null) b.text(Component.literal("of ").append(Component.literal(p.name).withStyle(Style.EMPTY.withUnderlined(true))),
                LINK, "pack:" + p.id, null, 0);
        b.text(cap(x.family) + " · " + (x.alive ? "alive" : x.dead4good ? "dead for good" : "dead, for now")
                + (x.returned > 0 ? " · came back " + times(x.returned) : ""), FADED);
        if (x.nemesis) b.bold("YOUR NEMESIS", BLOOD);
        else if (x.hunting) b.bold("HUNTING YOU", BLOOD);
        b.text(x.killedYou == 0 && x.youKilled == 0 ? "No blood between you yet."
                : (x.killedYou == 0 ? "It never killed you." : "It killed you " + times(x.killedYou) + ".")
                + (x.youKilled == 0 ? " You never killed it." : " You killed it " + times(x.youKilled) + "."), INK);
        if (x.after()) {
            b.text("Last seen " + where(x.dim, x.x, x.z) + ".", FADED);
            b.link("Show on the map", LINK, "map:" + x.id);
        }

        if (!x.scars.isEmpty()) {
            b.heading("Scars");
            for (String s : x.scars) b.text("• " + s, INK);
        }
        traits(b, x, "Strengths", GOOD, "IMMUNITY", "STRENGTH");
        traits(b, x, "Weaknesses", BLOOD, "WEAKNESS", null);
        traits(b, x, "Fears", KING, "FEAR", null);
        if (!x.carries.isEmpty()) {
            b.heading("Carries");
            List<ItemStack> items = items(x);
            for (int i = 0; i < x.carries.size(); i++) {
                ItemStack it = i < items.size() ? items.get(i) : ItemStack.EMPTY;
                if (it.isEmpty()) b.text("Your " + x.carries.get(i), INK);
                else b.iconRow(it, "Your " + x.carries.get(i), INK, null, gearNote(it), FADED);
            }
            b.text("Kill it to get them back.", BLOOD);
        }
        if (!x.memories.isEmpty()) {
            b.heading("What it remembers");
            for (int i = x.memories.size() - 1; i >= 0; i--) {
                Event e = x.memories.get(i);
                b.text(Component.literal("Day " + e.day() + "  ").withStyle(s -> s.withColor(BLOOD & 0xFFFFFF))
                        .append(Component.literal(e.text()).withStyle(s -> s.withColor(INK & 0xFFFFFF))), INK, null, null, 0);
            }
        }
        return b.done();
    }

    private void traits(DocBuilder b, BruteV x, String title, int color, String kind, String also) {
        List<TraitV> mine = x.traits.stream().filter(t -> t.kind().equals(kind) || t.kind().equals(also)).toList();
        if (mine.isEmpty()) return;
        b.heading(title);
        for (TraitV t : mine) {
            if (t.known()) {
                b.text(Component.literal("◆ " + t.title()), color, null,
                        List.of(Component.literal(t.title()).withStyle(s -> s.withBold(true)), Component.literal(t.desc())), 0);
            } else {
                b.text(Component.literal("◇ ??? fight it to find out"), FADED, null,
                        List.of(Component.literal("You haven't found this out yet. Watch how it fights.")), 0);
            }
        }
    }

    private void renderDoc(GuiGraphics g, int mx, int my, int x, int y, int w, int h) {
        if (doc == null || docWidth != w) {
            doc = buildDoc(w);
            docWidth = w;
        }
        docScroll = Mth.clamp(docScroll, 0, Math.max(0, doc.height - h + 6));
        int top = y - (int) docScroll;
        g.enableScissor(x - 4, y, x + w + 4, y + h);
        // The picture at the top right
        if (doc.reserveW > 0) {
            int rx = x + w - doc.reserveW + 4, ry = top;
            if (tab == 2 && view.brutes.get(selBrute) != null) {
                BruteV bv = view.brutes.get(selBrute);
                g.fill(rx, ry, rx + doc.reserveW - 4, ry + doc.reserveH - 4, PAPER_DARK);
                LivingEntity e = portrait(bv);
                if (e != null && ry + doc.reserveH > y) {
                    float size = Math.max(e.getBbHeight(), e.getBbWidth() * 0.9f);
                    int scale = (int) Math.min(40, 66 / Math.max(0.5f, size));
                    InventoryScreen.renderEntityInInventoryFollowsMouse(g, rx, Math.max(y, ry), rx + doc.reserveW - 4,
                            ry + doc.reserveH - 4, scale, 0.0625f, mx, my, e);
                }
                if (!bv.alive) g.drawString(font, "DEAD", rx + 4, ry + 4, FADED, false);
            } else if (tab == 1 && view.packs.get(selPack) != null) {
                PackV pv = view.packs.get(selPack);
                g.pose().pushPose();
                g.pose().translate(rx + 4, ry, 0);
                g.pose().scale(2.25f, 2.25f, 1);
                g.renderItem(banner(pv.color), 0, 0);
                g.pose().popPose();
            }
        }
        Line hot = null;
        for (Line l : doc.lines) {
            int ly = top + l.y();
            if (ly + l.h() < y || ly > y + h) continue;
            boolean over = my >= y && my < y + h && mx >= x - 2 && mx < x + w && my >= ly && my < ly + l.h();
            if (over && (l.link() != null || l.tip() != null)) {
                hot = l;
                if (l.link() != null) g.fill(x - 2, ly - 1, x + w, ly + l.h() - 1, 0x14000000);
            }
            if (l.icon() != null) g.renderItem(l.icon(), x, ly + 1);
            if (l.bar() >= 0) {
                int lw = l.text() == null ? 0 : font.width(l.text());
                int bx = x + l.indent() + (lw > 0 ? lw + 6 : 0), bw = Math.min(100, w - (bx - x) - 4);
                g.drawString(font, l.text(), x + l.indent(), ly + 2, l.color(), false);
                g.fill(bx, ly + 3, bx + bw, ly + 9, 0xFFC8B590);
                g.fill(bx, ly + 3, bx + Math.max(1, (int) (bw * l.bar())), ly + 9, l.barColor());
                continue;
            }
            int color = l.link() != null && hot == l ? BLOOD : l.color();
            g.drawString(font, l.text(), x + l.indent(), ly + 1, color, false);
        }
        g.disableScissor();
        // Scrollbar
        if (doc.height > h) {
            int sh = Math.max(12, h * h / doc.height);
            int sy = y + (int) ((h - sh) * (docScroll / (doc.height - h + 6)));
            g.fill(x + w + 2, sy, x + w + 4, sy + sh, PAPER_LINE);
        }
        if (hot != null && hot.tip() != null) g.renderComponentTooltip(font, hot.tip(), mx, my);
        docHot = hot;
    }

    private Line docHot;

    private List<ItemStack> items(BruteV b) {
        List<ItemStack> out = new ArrayList<>();
        if (minecraft == null || minecraft.level == null) return out;
        for (net.minecraft.nbt.CompoundTag t : b.carryItems) {
            out.add(ItemStack.parseOptional(minecraft.level.registryAccess(), t));
        }
        return out;
    }

    private static String gearNote(ItemStack it) {
        if (it.getItem() instanceof net.minecraft.world.item.ArmorItem) return "It wears it into fights";
        if (it.has(DataComponents.ATTRIBUTE_MODIFIERS) || it.getItem() instanceof net.minecraft.world.item.BowItem
                || it.getItem() instanceof net.minecraft.world.item.TieredItem) return "It fights with it";
        return "It keeps it";
    }

    private LivingEntity portrait(BruteV b) {
        if (minecraft == null || minecraft.level == null) return null;
        return portraits.computeIfAbsent(b.id, k -> {
            Optional<EntityType<?>> type = EntityType.byString(b.type);
            if (type.isEmpty()) return null;
            if (!(type.get().create(minecraft.level) instanceof LivingEntity e)) return null;
            if (b.color >= 0 && (e instanceof Zombie || e instanceof AbstractSkeleton
                    || e instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin)) {
                ItemStack cap = new ItemStack(Items.LEATHER_HELMET);
                cap.set(DataComponents.DYED_COLOR, new DyedItemColor(DyeColor.byId(b.color).getTextureDiffuseColor() & 0xFFFFFF, false));
                e.setItemSlot(EquipmentSlot.HEAD, cap);
            }
            // Dressed in what it took from you, as it is in a fight
            for (ItemStack it : items(b)) {
                if (it.getItem() instanceof net.minecraft.world.item.ArmorItem armor) {
                    if (armor.getEquipmentSlot() != EquipmentSlot.HEAD) e.setItemSlot(armor.getEquipmentSlot(), it);
                } else if (e.getMainHandItem().isEmpty() || it.getItem() instanceof net.minecraft.world.item.TieredItem) {
                    e.setItemSlot(EquipmentSlot.MAINHAND, it);
                }
            }
            return e;
        });
    }

    // ------------------------------------------------------------------ the map

    private static final class Marker {
        String link, title;
        List<Component> tip;
        int sx, sy;
    }

    private void renderMap(GuiGraphics g, int mx, int my) {
        int x = px + 6, y = bodyY, w = pw - 12, h = py + ph - 6 - bodyY;
        if (terrain == null) bakeTerrain();
        double span = view.n * (double) view.cell;
        if (zoom < 0) {
            zoom = Math.min(w, h) / Math.max(64.0, span);
            mapX = view.tx0 + span / 2;
            mapZ = view.tz0 + span / 2;
            if (view.focus.startsWith("map:")) focusMap(view.focus.substring(4));
        }
        g.fill(x, y, x + w, y + h, 0xFFE4D5B0);
        g.enableScissor(x, y, x + w, y + h);
        int cx = x + w / 2, cy = y + h / 2;
        // Terrain, from the server's biome sample
        int tx = (int) Math.round(cx + (view.tx0 - mapX) * zoom), ty = (int) Math.round(cy + (view.tz0 - mapZ) * zoom);
        int ts = (int) Math.round(span * zoom);
        if (view.n > 0) g.blit(TERRAIN, tx, ty, ts, ts, 0, 0, view.n, view.n, view.n, view.n);
        // Grid every 256 blocks (or wider when zoomed out)
        int step = 256;
        while (step * zoom < 40) step *= 2;
        long gx0 = (long) Math.floor((mapX - w / 2.0 / zoom) / step) * step;
        long gz0 = (long) Math.floor((mapZ - h / 2.0 / zoom) / step) * step;
        for (long gx = gx0; gx < mapX + w / 2.0 / zoom + step; gx += step) {
            int sx = (int) (cx + (gx - mapX) * zoom);
            g.fill(sx, y, sx + 1, y + h, 0x18000000);
        }
        for (long gz = gz0; gz < mapZ + h / 2.0 / zoom + step; gz += step) {
            int sy = (int) (cy + (gz - mapZ) * zoom);
            g.fill(x, sy, x + w, sy + 1, 0x18000000);
        }

        List<Marker> markers = new ArrayList<>();
        // Ties between vassals and their King
        for (PackV p : view.packs.values()) {
            if (p.liege.isEmpty() || !p.dim.equals(view.meDim)) continue;
            PackV k = view.packs.get(p.liege);
            if (k == null || !k.dim.equals(view.meDim)) continue;
            dotted(g, sx(p.x, cx), sy(p.z, cy), sx(k.x, cx), sy(k.z, cy), 0xA06A1E9A);
        }
        // Territories and banners; names only where they don't crowd each other
        List<PackV> order = new ArrayList<>(view.packs.values());
        order.sort(Comparator.comparingDouble((PackV p) -> -p.strength));
        List<int[]> labels = new ArrayList<>();
        for (PackV p : order) {
            if (!p.dim.equals(view.meDim)) continue;
            int sx = sx(p.x, cx), sy = sy(p.z, cy);
            int r = (int) Math.max(6, (40 + p.grunts * 4 + p.members.size() * 12) * zoom);
            disc(g, sx, sy, r, (DyeColor.byId(p.color).getTextureDiffuseColor() & 0xFFFFFF) | 0x38000000);
        }
        for (PackV p : order) {
            if (!p.dim.equals(view.meDim)) continue;
            int sx = sx(p.x, cx), sy = sy(p.z, cy);
            sprite(g, new MapBanner(BlockPos.ZERO, DyeColor.byId(p.color), Optional.empty()).getDecoration(), sx, sy, 0, 14);
            int nw = font.width(p.name);
            boolean crowded = false;
            for (int[] l : labels) {
                if (Math.abs(l[0] - sx) < (l[2] + nw) / 2 + 4 && Math.abs(l[1] - sy) < 14) crowded = true;
            }
            if (!crowded) {
                labels.add(new int[]{sx, sy, nw});
                g.fill(sx - nw / 2 - 2, sy + 8, sx + nw / 2 + 2, sy + 18, 0xB0EDE2C8);
                g.drawString(font, p.name, sx - nw / 2, sy + 9, p.liege.isEmpty() && p.vassals > 0 ? KING : INK, false);
            }
            BruteV boss = view.brutes.get(p.boss);
            Marker m = new Marker();
            m.link = "pack:" + p.id;
            m.sx = sx;
            m.sy = sy;
            m.tip = new ArrayList<>();
            m.tip.add(Component.literal(p.name).withStyle(s -> s.withBold(true).withColor(DyeColor.byId(p.color).getTextColor())));
            m.tip.add(Component.literal(p.familyTitle + ", home at " + p.place).withStyle(s -> s.withColor(0xAAAAAA)));
            if (boss != null) m.tip.add(Component.literal((p.vassals > 0 ? "King " : "Boss ") + boss.full + ", Lv " + boss.level));
            m.tip.add(Component.literal(plural(p.members.size(), "Brute") + ", " + plural(p.grunts, "grunt")));
            if (!p.liege.isEmpty() && view.packs.containsKey(p.liege)) {
                m.tip.add(Component.literal("Kneels to " + view.packs.get(p.liege).name).withStyle(s -> s.withColor(0xC080FF)));
            }
            m.tip.add(Component.literal(where(p.dim, p.x, p.z)).withStyle(s -> s.withColor(0xAAAAAA)));
            m.tip.add(Component.literal("Click to open its page").withStyle(s -> s.withColor(0x777777)));
            markers.add(m);
        }
        // Brutes after you
        for (BruteV b : view.brutes.values()) {
            if (!b.after() || !b.dim.equals(view.meDim)) continue;
            int sx = sx(b.x, cx), sy = sy(b.z, cy);
            sprite(g, MapDecorationTypes.RED_X, sx, sy, 0, 12);
            Marker m = new Marker();
            m.link = "brute:" + b.id;
            m.sx = sx;
            m.sy = sy;
            m.tip = List.of(Component.literal(b.full).withStyle(s -> s.withBold(true).withColor(0xFF5555)),
                    Component.literal((b.nemesis ? "Your Nemesis" : "Hunting you") + ", Lv " + b.level),
                    Component.literal("Last seen " + where(b.dim, b.x, b.z)).withStyle(s -> s.withColor(0xAAAAAA)),
                    Component.literal("Click to open its page").withStyle(s -> s.withColor(0x777777)));
            markers.add(m);
        }
        // You
        sprite(g, MapDecorationTypes.PLAYER, sx(view.meX, cx), sy(view.meZ, cy), view.meYaw, 12);
        g.disableScissor();

        // Frame, compass, scale and help
        g.fill(x, y, x + w, y + 1, PAPER_LINE);
        g.fill(x, y + h - 1, x + w, y + h, PAPER_LINE);
        g.fill(x, y, x + 1, y + h, PAPER_LINE);
        g.fill(x + w - 1, y, x + w, y + h, PAPER_LINE);
        g.drawString(font, "N", x + w - 14, y + 6, BLOOD, false);
        g.fill(x + w - 12, y + 16, x + w - 11, y + 26, BLOOD);
        int bar = step;
        int bw = (int) (bar * zoom);
        g.fill(x + 8, y + h - 12, x + 8 + bw, y + h - 10, INK);
        g.drawString(font, bar + " blocks", x + 12 + bw, y + h - 15, INK, false);
        String help = "Drag to move · Scroll to zoom · Click a banner";
        int hx = x + w - font.width(help) - 8;
        g.fill(hx - 3, y + h - 16, x + w - 5, y + h - 4, 0xC0EDE2C8);
        g.drawString(font, help, hx, y + h - 14, FADED, false);
        if (view.packs.isEmpty()) {
            String s = "No packs yet. Packs form where you fight.";
            g.drawString(font, s, x + (w - font.width(s)) / 2, y + h / 2 - 20, INK, false);
        }

        Marker hot = null;
        double best = 81;
        if (in(mx, my, x, y, w, h)) {
            for (Marker m : markers) {
                double d = (m.sx - mx) * (m.sx - mx) + (m.sy - my) * (m.sy - my);
                if (d < best) {
                    best = d;
                    hot = m;
                }
            }
        }
        mapHot = hot;
        if (hot != null) g.renderComponentTooltip(font, hot.tip, mx, my);
        else if (in(mx, my, x, y, w, h)) {
            int wx = (int) Math.floor(mapX + (mx - cx) / zoom), wz = (int) Math.floor(mapZ + (my - cy) / zoom);
            String c = wx + ", " + wz;
            g.drawString(font, c, x + 8, y + 6, FADED, false);
        }
    }

    private Marker mapHot;

    private int sx(int wx, int cx) {
        return (int) Math.round(cx + (wx - mapX) * zoom);
    }

    private int sy(int wz, int cy) {
        return (int) Math.round(cy + (wz - mapZ) * zoom);
    }

    private void focusMap(String id) {
        PackV p = view.packs.get(id);
        if (p != null) {
            mapX = p.x;
            mapZ = p.z;
            zoom = Math.max(zoom, 0.5);
            return;
        }
        BruteV b = view.brutes.get(id);
        if (b != null) {
            mapX = b.x;
            mapZ = b.z;
            zoom = Math.max(zoom, 0.5);
        }
    }

    private void sprite(GuiGraphics g, Holder<MapDecorationType> type, int x, int y, float rot, int size) {
        TextureAtlasSprite s = minecraft.getMapDecorationTextures().get(new MapDecoration(type, (byte) 0, (byte) 0, (byte) 0, Optional.empty()));
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        if (rot != 0) g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rot + 180));
        g.blit(-size / 2, -size / 2, 0, size, size, s);
        g.pose().popPose();
    }

    private static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt((double) r * r - dy * dy);
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    private static void dotted(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        double len = Math.hypot(x1 - x0, y1 - y0);
        for (double t = 0; t < len; t += 6) {
            int x = (int) (x0 + (x1 - x0) * t / len), y = (int) (y0 + (y1 - y0) * t / len);
            g.fill(x - 1, y - 1, x + 2, y + 2, color);
        }
    }

    /** Paints the server's biome sample into a texture, in an old map's colors. */
    private void bakeTerrain() {
        int n = Math.max(1, view.n);
        NativeImage img = new NativeImage(n, n, false);
        java.util.Random noise = new java.util.Random(n * 31L + view.tx0);
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int k = j * n + i;
                int kind = k < view.terrain.length ? view.terrain[k] : 0;
                int rgb = switch (kind) {
                    case Screens.OCEAN -> 0x7FA3B5;
                    case Screens.DEEP -> 0x6A8EA3;
                    case Screens.RIVER -> 0x86AABB;
                    case Screens.BEACH -> 0xE6D3A0;
                    case Screens.DESERT -> 0xE3CB8E;
                    case Screens.BADLANDS -> 0xD09A6A;
                    case Screens.FOREST -> 0xA9B98A;
                    case Screens.JUNGLE -> 0x93AD78;
                    case Screens.TAIGA -> 0x9DAE90;
                    case Screens.SNOW -> 0xEEEDE6;
                    case Screens.MOUNTAIN -> 0xB9B2A2;
                    case Screens.SWAMP -> 0x9BA585;
                    case Screens.SAVANNA -> 0xCFC48E;
                    case Screens.NETHER -> 0xA0554A;
                    case Screens.END -> 0xD8D6B0;
                    case Screens.MUSHROOM -> 0xB9A0B0;
                    default -> 0xC9C99A;
                };
                // Darker edges where the ground changes, like inked coastlines
                boolean edge = (i > 0 && view.terrain.length > k - 1 && water(view.terrain[k - 1]) != water((byte) kind))
                        || (j > 0 && water(view.terrain[k - n]) != water((byte) kind));
                if (edge) rgb = water((byte) kind) ? 0x5F7F92 : 0x8A7552;
                int v = noise.nextInt(9) - 4;
                int r = Mth.clamp((rgb >> 16 & 255) + v, 0, 255), gg = Mth.clamp((rgb >> 8 & 255) + v, 0, 255),
                        b = Mth.clamp((rgb & 255) + v, 0, 255);
                img.setPixelRGBA(i, j, 0xFF000000 | b << 16 | gg << 8 | r); // NativeImage wants ABGR
            }
        }
        terrain = new DynamicTexture(img);
        minecraft.getTextureManager().register(TERRAIN, terrain);
    }

    private static boolean water(byte kind) {
        return kind == Screens.OCEAN || kind == Screens.DEEP || kind == Screens.RIVER;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // Tabs
        int tx = px + pw - 6;
        for (int i = TABS.length - 1; i >= 0; i--) {
            int w = font.width(TABS[i]) + 12;
            tx -= w;
            if (in(mx, my, tx, py + 3, w, 18)) {
                setTab(i);
                return true;
            }
            tx -= 2;
        }
        if (tab == 1 || tab == 2) {
            int h = py + ph - 6 - bodyY;
            if (in(mx, my, px + 4, bodyY, listW - 4, h)) {
                int i = (int) ((my - bodyY + listScroll) / 24);
                List<Entry> es = entries();
                if (i >= 0 && i < es.size()) {
                    if (tab == 1) selPack = es.get(i).id;
                    else selBrute = es.get(i).id;
                    doc = null;
                    docScroll = 0;
                    click();
                }
                return true;
            }
        }
        if ((tab != 3) && docHot != null && docHot.link() != null) {
            follow(docHot.link());
            return true;
        }
        if (tab == 3 && button == 0) {
            if (mapHot != null) {
                follow(mapHot.link);
                return true;
            }
            dragging = true;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (tab == 3 && dragging) {
            mapX -= dx / zoom;
            mapZ -= dy / zoom;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (tab == 3) {
            int cx = px + 6 + (pw - 12) / 2, cy = bodyY + (py + ph - 6 - bodyY) / 2;
            double wx = mapX + (mx - cx) / zoom, wz = mapZ + (my - cy) / zoom;
            zoom = Mth.clamp(zoom * (sy > 0 ? 1.25 : 0.8), 0.01, 8);
            mapX = wx - (mx - cx) / zoom;
            mapZ = wz - (my - cy) / zoom;
            return true;
        }
        if ((tab == 1 || tab == 2) && mx < px + listW) listScroll -= sy * 24;
        else docScroll -= sy * 20;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (FeudClient.LEDGER.matches(key, scan)) {
            onClose();
            return true;
        }
        if (key >= org.lwjgl.glfw.GLFW.GLFW_KEY_1 && key <= org.lwjgl.glfw.GLFW.GLFW_KEY_4) {
            setTab(key - org.lwjgl.glfw.GLFW.GLFW_KEY_1);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void setTab(int i) {
        if (tab != i) click();
        tab = i;
        doc = null;
        docScroll = 0;
    }

    private void follow(String link) {
        click();
        if (link.startsWith("pack:")) {
            selPack = link.substring(5);
            setTab(1);
        } else if (link.startsWith("brute:")) {
            selBrute = link.substring(6);
            setTab(2);
        } else if (link.startsWith("map:")) {
            setTab(3);
            if (zoom < 0) {
                view.focus = link;
            } else {
                focusMap(link.substring(4));
            }
        }
    }

    private void click() {
        minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.0f));
    }

    // ------------------------------------------------------------------ helpers

    private String where(String dim, int x, int z) {
        return dim.equals(view.meDim) ? view.where(x, z) : "in another dimension";
    }

    private static ItemStack banner(int color) {
        return new ItemStack(BannerBlock.byColor(DyeColor.byId(color)).asItem());
    }

    private static ItemStack egg(String type) {
        Optional<EntityType<?>> t = EntityType.byString(type);
        SpawnEggItem e = t.map(SpawnEggItem::byId).orElse(null);
        return e == null ? new ItemStack(Items.SKELETON_SKULL) : new ItemStack(e);
    }

    private static int rankInk(String rank) {
        return switch (rank) {
            case "KING" -> KING;
            case "BOSS" -> BLOOD;
            default -> BRUTE;
        };
    }

    private String clip(String s, int w) {
        if (font.width(s) <= w) return s;
        return font.plainSubstrByWidth(s, w - font.width("…")) + "…";
    }

    private static String plural(int n, String what) {
        return n + " " + what + (n == 1 ? "" : "s");
    }

    private static String times(int n) {
        return n == 0 ? "never" : n == 1 ? "once" : n == 2 ? "twice" : n + " times";
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
