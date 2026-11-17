package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.BloodFeud;
import io.github.pirut.bloodfeud.feud.Brute;
import io.github.pirut.bloodfeud.feud.Feud;
import io.github.pirut.bloodfeud.feud.Memory;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.Rank;
import io.github.pirut.bloodfeud.feud.Relation;
import io.github.pirut.bloodfeud.feud.Scar;
import io.github.pirut.bloodfeud.feud.Trait;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The Feud Ledger: a plain written book (so vanilla clients can read it) that rewrites itself every time it's opened,
 * with a page for each pack the player knows (its Boss, Brutes, grunts, rivals, its grudge with the player and its
 * history) and each Brute they've met (traits found out, scars, what it remembers). Names are links between pages.
 */
public final class Ledger {
    private static final String TAG = "bloodfeud_ledger";
    private static final int LINES = 14;
    private static final int WIDTH = 112; // the page is 114 pixels; a little room for error
    private static final int MAX_PAGES = 100;
    // Colors that read on the book's paper.
    private static final int INK = 0x2B1A12, FADED = 0x6B5A4A, BLOOD = 0x8E1020, BRUTE = 0x9A5A00, KING = 0x6A1E9A,
            GOOD = 0x1E6B1E, LINK = 0x1B3F8E;

    private Ledger() {
    }

    public static boolean is(ItemStack stack) {
        if (!stack.is(Items.WRITTEN_BOOK)) return false;
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d != null && d.contains(TAG);
    }

    public static ItemStack make(Live live, ServerPlayer p) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG, true);
        book.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        book.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        refresh(live, p, book);
        return book;
    }

    /** Rewrites the book for this player. Marked unresolved so vanilla sends the new pages when it opens. */
    public static void refresh(Live live, ServerPlayer p, ItemStack book) {
        List<Filterable<Component>> pages = new ArrayList<>();
        for (Component c : write(live, p)) pages.add(Filterable.passThrough(c));
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Feud Ledger"),
                "Blood Feud", 0, pages, false));
    }

    /** Hands the player a ledger the first time a Brute finds them. */
    static void firstMeeting(Live live, ServerPlayer p) {
        if (!BloodFeud.config().giveLedger || !live.data().ledgerGiven.add(p.getUUID())) return;
        give(live, p);
        p.sendSystemMessage(Component.literal("You found a Feud Ledger. It fills in as you learn about the packs.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        live.data().changed();
    }

    /** Gives a ledger unless the player already carries one, which is brought up to date instead. */
    static boolean give(Live live, ServerPlayer p) {
        for (ItemStack s : p.getInventory().items) {
            if (is(s)) {
                refresh(live, p, s);
                return false;
            }
        }
        ItemStack book = make(live, p);
        if (!p.getInventory().add(book)) p.drop(book, false);
        return true;
    }

    // ---------------------------------------------------------------- writing

    static List<Component> write(Live live, ServerPlayer p) {
        // Twice: the first pass finds which page each pack and Brute starts on, the second writes links to them.
        Book first = new Book(Map.of());
        compose(first, live, p);
        Book second = new Book(first.anchors);
        compose(second, live, p);
        return second.pages();
    }

    private static void compose(Book b, Live live, ServerPlayer p) {
        Feud f = live.feud();
        UUID me = p.getUUID();
        String dim = Live.dim(p.level());
        List<Pack> packs = new ArrayList<>();
        for (Pack pk : f.packs.values()) if (pk.met.contains(me)) packs.add(pk);
        packs.sort(Comparator.comparingDouble(pk -> pk.dimension.equals(dim) ? pk.distanceTo(dim, p.getX(), p.getZ()) : 1e12));
        List<Brute> after = new ArrayList<>();
        List<Brute> lone = new ArrayList<>();
        List<Brute> known = new ArrayList<>();
        for (Brute br : f.brutes.values()) {
            Relation r = br.relations.get(me);
            if (r == null || !r.met) continue;
            known.add(br);
            if (br.alive && (r.nemesis || me.equals(br.hunting))) after.add(br);
            if (br.alive && f.packOf(br) == null) lone.add(br);
        }
        after.sort(Comparator.comparingInt((Brute x) -> -x.level));
        known.sort(Comparator.comparing((Brute x) -> x.name));

        // Cover and contents
        b.anchor("contents");
        b.line(text("☠ Feud Ledger", BLOOD).withStyle(ChatFormatting.BOLD));
        b.line(text("Day " + day(live.now()) + ", " + p.getName().getString(), FADED));
        b.gap();
        long kings = packs.stream().filter(pk -> pk.liege == null && !f.vassals(pk).isEmpty()).count();
        b.line(text(packs.size() + (packs.size() == 1 ? " pack" : " packs") + " you know", INK));
        if (kings > 0) b.line(text(kings + (kings == 1 ? " King" : " Kings") + " among them", KING));
        b.line(after.isEmpty() ? text("No one is after you", INK)
                : text(after.size() + (after.size() == 1 ? " Brute is" : " Brutes are") + " after you", BLOOD));
        b.line(text("[Sketch a map]", GOOD).withStyle(Style.EMPTY.withColor(GOOD).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bloodfeud map"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("A map with every pack home you know marked")))));
        b.gap();
        if (packs.isEmpty() && known.isEmpty()) {
            b.para(text("Nothing yet. Packs form where you fight: kill a few mobs while their friends watch.", FADED));
        }
        if (!after.isEmpty()) b.line(link(b, "nemeses", "▸ Who's after you", BLOOD));
        for (Pack pk : packs) b.line(link(b, "pack:" + pk.id, "▸ " + cap(pk.title().substring(4)), LINK));
        if (!lone.isEmpty()) b.line(link(b, "lone", "▸ Lone Brutes", LINK));

        if (!after.isEmpty()) {
            b.page();
            b.anchor("nemeses");
            b.line(text("Who's after you", BLOOD).withStyle(ChatFormatting.BOLD));
            for (Brute br : after) {
                Relation r = br.relation(me);
                b.line(bruteLink(b, f, br));
                b.para(text("Lv " + br.level + ". Killed you " + times(r.killedPlayer) + ". "
                        + (br.dimension.equals(dim) ? cap(FeudCommand.where(p.getX(), p.getZ(), br.x, br.z)) : "In another dimension")
                        + ".", FADED));
            }
            b.line(back(b));
        }

        for (Pack pk : packs) writePack(b, live, p, pk);

        if (!lone.isEmpty()) {
            b.page();
            b.anchor("lone");
            b.line(text("Lone Brutes", INK).withStyle(ChatFormatting.BOLD));
            b.para(text("No pack yet. Two of a kind that meet start one.", FADED));
            for (Brute br : lone) {
                b.line(bruteLink(b, f, br));
                b.line(text("Lv " + br.level + ", " + cap(br.family.title), FADED));
            }
            b.line(back(b));
        }

        for (Brute br : known) writeBrute(b, live, p, br);
    }

    private static void writePack(Book b, Live live, ServerPlayer p, Pack pk) {
        Feud f = live.feud();
        UUID me = p.getUUID();
        b.page();
        b.anchor("pack:" + pk.id);
        DyeColor dye = DyeColor.byId(pk.color);
        b.line(Component.literal("■ ").withStyle(s -> s.withColor(dye.getTextColor()))
                .append(text(cap(pk.title().substring(4)), INK).withStyle(ChatFormatting.BOLD)));
        b.line(text(cap(pk.family.title), FADED));
        String dist = pk.dimension.equals(Live.dim(p.level())) ? FeudCommand.where(p.getX(), p.getZ(), pk.homeX, pk.homeZ)
                : "in another dimension";
        b.para(text("Home: " + pk.place + ", " + dist + " (" + pk.homeX + ", " + pk.homeZ + ")", INK));
        b.line(text("Formed day " + day(pk.formedAt), FADED));
        Pack top = f.top(pk);
        if (top != pk) {
            Brute king = f.brute(top.boss);
            MutableComponent l = text("Kneels to ", KING);
            b.para(king != null && known(king, me) ? l.append(bruteLink(b, f, king)) : l.append(text(cap(top.title()), KING)));
        }
        List<Pack> vassals = f.vassals(pk);
        if (!vassals.isEmpty()) {
            b.para(text("Rules " + vassals.size() + (vassals.size() == 1 ? " other pack" : " other packs") + " as King", KING));
        }

        Brute boss = f.brute(pk.boss);
        b.gap();
        b.line(text(vassals.isEmpty() ? "BOSS" : "KING", BLOOD).withStyle(ChatFormatting.BOLD));
        if (boss == null) b.line(text("No one", FADED));
        else if (known(boss, me)) {
            b.line(bruteLink(b, f, boss));
            Relation r = boss.relations.get(me);
            b.line(text("Lv " + boss.level + (boss.alive ? "" : ", dead for now")
                    + (r != null && r.nemesis ? ". YOUR NEMESIS" : ""), r != null && r.nemesis ? BLOOD : FADED));
        } else b.line(text("??? not met yet", FADED));

        List<Brute> members = f.members(pk);
        members.removeIf(x -> x.id.equals(pk.boss));
        b.gap();
        b.line(text("BRUTES", BLOOD).withStyle(ChatFormatting.BOLD));
        int unknown = 0;
        for (Brute m : members) {
            if (!known(m, me)) {
                unknown++;
                continue;
            }
            b.line(bruteLink(b, f, m));
            String note = boss != null && boss.alive && m.level >= boss.level + 2 ? ", wants to be Boss"
                    : m.relation(me).nemesis ? ", your Nemesis" : "";
            b.line(text("Lv " + m.level + note, FADED));
        }
        if (members.isEmpty()) b.line(text("None", FADED));
        if (unknown > 0) b.line(text(unknown + " you haven't met", FADED));
        b.gap();
        b.para(Component.empty().append(text("GRUNTS ", BLOOD).withStyle(ChatFormatting.BOLD)).append(text(pk.grunts + ", in "
                + dye.getName().replace('_', ' ') + " caps", INK)));

        // Rivals: packs close enough to raid, that don't kneel to the same King
        List<Pack> rivals = new ArrayList<>();
        int unmetRivals = 0;
        for (Pack q : f.packs.values()) {
            if (q == pk || f.top(q) == top || q.distanceTo(pk) > Feud.RIVAL_RANGE) continue;
            if (q.met.contains(me)) rivals.add(q);
            else unmetRivals++;
        }
        if (!rivals.isEmpty() || unmetRivals > 0) {
            b.gap();
            b.line(text("RIVALS", BLOOD).withStyle(ChatFormatting.BOLD));
            double mine = f.strength(pk);
            for (Pack q : rivals) {
                String how = q.family == pk.family
                        ? (mine >= f.strength(q) ? ": could swallow it" : ": could be swallowed by it")
                        : (mine >= f.strength(q) ? ": stronger" : ": weaker");
                b.para(link(b, "pack:" + q.id, cap(q.title().substring(4)), LINK).append(text(how, FADED)));
            }
            if (unmetRivals > 0) b.line(text(unmetRivals + " more you don't know", FADED));
        }

        // The grudge between this pack and the player
        int theyKilled = 0, youKilled = 0;
        for (Brute m : f.brutes.values()) {
            if (!pk.id.equals(m.pack)) continue;
            Relation r = m.relations.get(me);
            if (r == null) continue;
            theyKilled += r.killedPlayer;
            youKilled += r.killedByPlayer;
        }
        b.gap();
        b.line(text("YOUR GRUDGE", BLOOD).withStyle(ChatFormatting.BOLD));
        b.para(text(youKilled == 0 && theyKilled == 0 ? "No blood between you yet."
                : "You killed its Brutes " + times(youKilled) + ". They killed you " + times(theyKilled) + ".", INK));

        if (!pk.chronicle.isEmpty()) {
            b.gap();
            b.line(text("ITS STORY", BLOOD).withStyle(ChatFormatting.BOLD));
            List<Pack.Event> events = new ArrayList<>(pk.chronicle);
            for (int i = Math.max(0, events.size() - 8); i < events.size(); i++) {
                Pack.Event e = events.get(i);
                b.para(text("Day " + day(e.time()) + ": ", BLOOD).append(text(e.text(), INK)));
            }
        }
        b.gap();
        b.line(back(b));
    }

    private static void writeBrute(Book b, Live live, ServerPlayer p, Brute br) {
        Feud f = live.feud();
        UUID me = p.getUUID();
        Relation r = br.relation(me);
        Rank rank = f.rank(br);
        b.page();
        b.anchor("brute:" + br.id);
        b.line(text(br.name, rankInk(rank)).withStyle(ChatFormatting.BOLD));
        if (br.epithet != null && !br.epithet.isEmpty()) b.line(text(br.epithet, rankInk(rank)));
        b.para(text("Level " + br.level + " " + rank.title, INK));
        Pack pk = f.packOf(br);
        if (pk != null) {
            MutableComponent of = text("of ", INK);
            b.para(pk.met.contains(me) ? of.append(link(b, "pack:" + pk.id, cap(pk.title().substring(4)), LINK))
                    : of.append(text(cap(pk.title().substring(4)), INK)));
        }
        b.para(text(cap(br.family.title) + ". " + (br.alive ? "Alive" : br.finalDeath ? "Dead for good" : "Dead, for now")
                + (br.timesReturned > 0 ? ", back " + times(br.timesReturned) : "") + ".", FADED));
        if (r.nemesis) b.line(text("YOUR NEMESIS", BLOOD).withStyle(ChatFormatting.BOLD));

        if (!br.scars.isEmpty()) {
            b.gap();
            b.line(text("SCARS", BLOOD).withStyle(ChatFormatting.BOLD));
            for (Scar s : br.scars) b.line(text("■ " + scarName(s), INK));
        }
        traits(b, br, r, "STRENGTHS", GOOD, Trait.Kind.IMMUNITY, Trait.Kind.STRENGTH);
        traits(b, br, r, "WEAKNESSES", BLOOD, Trait.Kind.WEAKNESS, null);
        traits(b, br, r, "FEARS", KING, Trait.Kind.FEAR, null);

        List<ItemStack> stolen = live.data().stolen.get(br.id);
        if (stolen != null && !stolen.isEmpty()) {
            b.gap();
            b.line(text("CARRIES", BLOOD).withStyle(ChatFormatting.BOLD));
            for (ItemStack s : stolen) b.para(text("Your " + s.getHoverName().getString().toLowerCase(Locale.ROOT), INK));
        }
        if (!r.memories.isEmpty()) {
            b.gap();
            b.line(text("WHAT IT REMEMBERS", BLOOD).withStyle(ChatFormatting.BOLD));
            List<Memory> ms = new ArrayList<>(r.memories);
            for (int i = Math.max(0, ms.size() - 8); i < ms.size(); i++) {
                Memory m = ms.get(i);
                b.para(text("Day " + day(m.time()) + ": ", BLOOD).append(text(FeudCommand.memory(m), INK)));
            }
        }
        b.gap();
        if (pk != null && pk.met.contains(me)) b.line(link(b, "pack:" + pk.id, "◀ " + cap(pk.title().substring(4)), LINK));
        b.line(back(b));
    }

    private static void traits(Book b, Brute br, Relation r, String title, int color, Trait.Kind kind, Trait.Kind also) {
        List<Trait> knownTraits = new ArrayList<>();
        int hidden = 0;
        for (Trait t : br.traits) {
            if (t.kind != kind && t.kind != also) continue;
            if (r.known.contains(t)) knownTraits.add(t);
            else hidden++;
        }
        if (knownTraits.isEmpty() && hidden == 0) return;
        b.gap();
        b.line(text(title, color).withStyle(ChatFormatting.BOLD));
        for (Trait t : knownTraits) {
            b.para(text(t.title, INK).withStyle(Style.EMPTY.withColor(INK)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(t.description))))
                    .append(text(" (?)", FADED)));
        }
        for (int i = 0; i < hidden; i++) b.line(text("??? fight it to find out", FADED));
    }

    // ---------------------------------------------------------------- pieces

    private static MutableComponent bruteLink(Book b, Feud f, Brute br) {
        Rank rank = f.rank(br);
        MutableComponent hover = Component.literal(br.fullName()).withStyle(Live.rankColor(rank))
                .append(Component.literal("\nLevel " + br.level + " " + f.standing(br)).withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\nClick to open its page").withStyle(ChatFormatting.DARK_GRAY));
        return Component.literal(br.fullName()).withStyle(Style.EMPTY.withColor(rankInk(rank)).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, Integer.toString(b.pageOf("brute:" + br.id))))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
    }

    private static MutableComponent link(Book b, String anchor, String label, int color) {
        return Component.literal(label).withStyle(Style.EMPTY.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, Integer.toString(b.pageOf(anchor)))));
    }

    private static MutableComponent back(Book b) {
        return link(b, "contents", "◀ Contents", LINK);
    }

    private static MutableComponent text(String s, int color) {
        return Component.literal(s).withStyle(Style.EMPTY.withColor(color));
    }

    private static int rankInk(Rank rank) {
        return switch (rank) {
            case KING -> KING;
            case BOSS -> BLOOD;
            case BRUTE -> BRUTE;
        };
    }

    private static boolean known(Brute br, UUID me) {
        Relation r = br.relations.get(me);
        return r != null && r.met;
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

    private static String times(int n) {
        return n == 1 ? "once" : n == 2 ? "twice" : n + " times";
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    /** Lays text out in book pages of 14 lines of 114 pixels, and remembers where each section starts. */
    static final class Book {
        final Map<String, Integer> anchors = new HashMap<>();
        private final Map<String, Integer> known;
        private final List<MutableComponent> pages = new ArrayList<>();
        private MutableComponent current;
        private int used;

        Book(Map<String, Integer> known) {
            this.known = known;
        }

        int pageOf(String anchor) {
            return known.getOrDefault(anchor, 1);
        }

        void anchor(String name) {
            if (current == null) page();
            anchors.put(name, pages.size());
        }

        void page() {
            if (pages.size() >= MAX_PAGES) return;
            if (current != null && used == 0) return; // already on a fresh page
            current = Component.empty();
            pages.add(current);
            used = 0;
        }

        /** A line (wrapped as the book wraps it, if long). */
        void line(MutableComponent c) {
            int lines = lines(c);
            if (current == null) page();
            if (used + lines > LINES) {
                if (pages.size() >= MAX_PAGES) return;
                current = Component.empty();
                pages.add(current);
                used = 0;
            }
            if (used > 0) current.append("\n");
            current.append(c);
            used += lines;
        }

        void para(MutableComponent c) {
            line(c);
        }

        /** How many rows the book needs for this text: word wrap over the page's 114 pixels, by the font's widths. */
        static int lines(Component c) {
            List<int[]> chars = new ArrayList<>();
            c.visit((style, str) -> {
                str.codePoints().forEach(cp -> chars.add(new int[]{cp, width(cp) + (style.isBold() ? 1 : 0)}));
                return java.util.Optional.empty();
            }, Style.EMPTY);
            int rows = 1, x = 0, i = 0;
            while (i < chars.size()) {
                // The next word, with the space before it
                int j = i, w = 0;
                if (chars.get(j)[0] == ' ') w += chars.get(j++)[1];
                while (j < chars.size() && chars.get(j)[0] != ' ') w += chars.get(j++)[1];
                if (x + w <= WIDTH) {
                    x += w;
                } else if (x == 0) {
                    x = w % WIDTH; // a word longer than a line breaks mid-word
                    rows += w / WIDTH;
                } else {
                    rows++;
                    x = w - (chars.get(i)[0] == ' ' ? chars.get(i)[1] : 0);
                }
                i = j;
            }
            return rows;
        }

        private static int width(int cp) {
            if (cp > 0x7F) return 9;
            if ("!',.:;i|".indexOf(cp) >= 0) return 2;
            if ("l`".indexOf(cp) >= 0) return 3;
            if (" It[]\"*{}".indexOf(cp) >= 0) return 4;
            if ("fk<>()".indexOf(cp) >= 0) return 5;
            if ("@~".indexOf(cp) >= 0) return 7;
            return 6;
        }

        void gap() {
            if (used > 0 && used < LINES - 1) line(Component.literal(""));
        }

        List<Component> pages() {
            return new ArrayList<>(pages);
        }
    }

    @SuppressWarnings("unused")
    private static Iterator<Brute> none() {
        return List.<Brute>of().iterator();
    }
}
