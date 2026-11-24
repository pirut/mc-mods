package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.feud.Brute;
import io.github.pirut.bloodfeud.feud.DeathCause;
import io.github.pirut.bloodfeud.feud.Feud;
import io.github.pirut.bloodfeud.feud.Memory;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.Rank;
import io.github.pirut.bloodfeud.feud.Relation;
import io.github.pirut.bloodfeud.feud.Scar;
import io.github.pirut.bloodfeud.feud.Talk;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What Brutes say: lines from {@code lines.txt}, picked by what the Brute remembers about this player and filled in
 * with the details (the place, the item, the days, the scar), never the same line twice in a row of 48.
 */
public final class Lines {
    private Lines() {
    }

    /** The first thing it says when it finds a player. */
    public static String greet(Live live, ServerPlayer p, Brute b, Relation rel, Rank rank, boolean hunted) {
        Memory last = rel == null ? null : rel.last();
        List<String> sections = new ArrayList<>();
        if (hunted && rel != null && rel.nemesis) {
            sections.add("greet.hunted");
        } else if (last != null) {
            switch (last.kind()) {
                case RETURNED, WAS_KILLED -> {
                    sections.add("returned." + DeathCause.byKey(lastCause(rel)).key());
                    sections.add("returned.any");
                }
                case KILLED_PLAYER, ROSE -> sections.add(rel.killedPlayer >= 3 ? "greet.killed_you_often" : "greet.killed_you");
                case PLAYER_FLED -> sections.add("greet.fled");
                case IT_FLED -> sections.add("greet.it_fled");
                case STOLE -> sections.add("greet.stole");
                case SAW_KILLS -> sections.add("greet.saw_kills");
                case WRECKED_CAMP -> sections.add("greet.wrecked_camp");
            }
        }
        if (sections.isEmpty()) {
            sections.add("greet." + rank.name().toLowerCase(Locale.ROOT));
            sections.add("greet." + b.family.name().toLowerCase(Locale.ROOT));
        } else if (live.random.nextInt(4) == 0) {
            // Now and then it talks like what it is instead of about what happened.
            sections.add(live.random.nextBoolean() ? "greet." + b.family.name().toLowerCase(Locale.ROOT)
                    : "greet." + rank.name().toLowerCase(Locale.ROOT));
        }
        return say(live, p, b, sections.toArray(String[]::new));
    }

    public static String killedPlayer(Live live, ServerPlayer p, Brute b, int times) {
        return times >= 3 && live.random.nextInt(3) > 0 ? say(live, p, b, "killed_player.often")
                : say(live, p, b, "killed_player");
    }

    public static String falling(Live live, ServerPlayer p, Brute b, boolean forGood) {
        return say(live, p, b, forGood ? "falling.final" : "falling");
    }

    public static String rose(Live live, ServerPlayer p, Brute b, boolean killedYou) {
        return say(live, p, b, killedYou ? "rose.killed_you" : "rose.saw");
    }

    public static String flee(Live live, ServerPlayer p, Brute b) {
        return say(live, p, b, "flee");
    }

    public static String driveOff(Live live, ServerPlayer p, Brute b) {
        return say(live, p, b, "drive_off");
    }

    public static String stole(Live live, ServerPlayer p, Brute b, String item) {
        Map<String, String> w = words(live, p, b);
        w.put("item", item);
        return or(Talk.book().line(live.random, b.id, w, "stole"), "Mine now.");
    }

    /** When it gets badly hurt. */
    public static String hurt(Live live, ServerPlayer p, Brute b) {
        return say(live, p, b, "hurt");
    }

    /** When the player it's fighting is nearly dead. */
    public static String winning(Live live, ServerPlayer p, Brute b) {
        return say(live, p, b, "winning");
    }

    private static String say(Live live, ServerPlayer p, Brute b, String... sections) {
        return or(Talk.book().line(live.random, b.id, words(live, p, b), sections), "...");
    }

    private static String or(String line, String fallback) {
        return line == null ? fallback : line;
    }

    /** Everything a line might bring up, for this Brute and this player. Blanks with nothing to say are left out. */
    static Map<String, String> words(Live live, ServerPlayer p, Brute b) {
        Map<String, String> w = new HashMap<>();
        w.put("name", b.name);
        w.put("kin", switch (b.family) {
            case ROTTEN -> "brothers";
            case BONES -> "kin";
            case ILLAGERS -> "kinsmen";
            case NETHER -> "kin";
            case SPIDERS -> "brood";
            case WITCHES -> "sisters";
        });
        Feud f = live.data().feud;
        Pack pack = f.packOf(b);
        if (pack != null) {
            w.put("pack", pack.title());
            w.put("home", pack.place);
            Pack top = f.top(pack);
            Brute king = top == pack ? null : f.brute(top.boss);
            if (king != null && king.alive) w.put("king", king.name);
        }
        if (!b.scars.isEmpty()) w.put("scar", scar(b.scars.get(b.scars.size() - 1)));
        if (p == null) return w;
        w.put("player", p.getName().getString());
        ItemStack held = p.getMainHandItem();
        if (!held.isEmpty()) {
            String name = held.getHoverName().getString();
            w.put("weapon", held.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME) ? name : name.toLowerCase(Locale.ROOT));
        }
        Relation rel = b.relations.get(p.getUUID());
        if (rel != null) {
            if (rel.killedPlayer > 0) w.put("times", Talk.count(rel.killedPlayer));
            Memory last = rel.last();
            if (last != null) {
                String days = Talk.days((live.now() - last.time()) / Feud.DAY);
                if (days != null) w.put("days", days);
            }
            for (Iterator<Memory> it = rel.memories.descendingIterator(); it.hasNext(); ) {
                Memory m = it.next();
                if (m.kind() == Memory.Kind.STOLE && !m.detail().isEmpty()) {
                    w.put("item", m.detail());
                    break;
                }
            }
        }
        return w;
    }

    private static String scar(Scar s) {
        return switch (s) {
            case BURNED -> "burned";
            case BROKEN -> "with a broken leg";
            case BLASTED -> "half blown apart";
            case PIERCED -> "full of holes";
            case DROWNED -> "waterlogged";
            case CUT -> "with a face full of stitches";
            case MAULED -> "chewed up";
        };
    }

    private static String lastCause(Relation rel) {
        for (Iterator<Memory> it = rel.memories.descendingIterator(); it.hasNext(); ) {
            Memory m = it.next();
            if (m.kind() == Memory.Kind.WAS_KILLED) return m.detail();
        }
        return "";
    }
}
