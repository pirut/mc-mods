package io.github.pirut.bloodfeud.game;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.pirut.bloodfeud.BloodFeud;
import io.github.pirut.bloodfeud.feud.Brute;
import io.github.pirut.bloodfeud.feud.SettlementTier;
import io.github.pirut.bloodfeud.feud.Family;
import io.github.pirut.bloodfeud.feud.Feud;
import io.github.pirut.bloodfeud.feud.Memory;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.Rank;
import io.github.pirut.bloodfeud.feud.Relation;
import io.github.pirut.bloodfeud.feud.Scar;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * {@code /bloodfeud}: the packs around you, a Brute's page, your Nemeses. Operators also get commands to make Brutes,
 * play out a night and wipe the slate.
 */
public final class FeudCommand {
    private static final int LIST_RANGE = 3000;

    private FeudCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bloodfeud")
                .executes(c -> packs(c.getSource()))
                .then(Commands.literal("packs").executes(c -> packs(c.getSource())))
                .then(Commands.literal("nemesis").executes(c -> nemesis(c.getSource())))
                .then(Commands.literal("ledger").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (Screens.open(Live.get(c.getSource().getServer()), p, "ledger", "")) return 1;
                    boolean fresh = Ledger.give(Live.get(c.getSource().getServer()), p);
                    c.getSource().sendSuccess(() -> Component.literal(fresh ? "Here is your Feud Ledger."
                            : "Your Feud Ledger is up to date.").withStyle(ChatFormatting.GRAY), false);
                    return 1;
                }))
                .then(Commands.literal("book").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Ledger.give(Live.get(c.getSource().getServer()), p);
                    return 1;
                }))
                .then(Commands.literal("mapitem").executes(c -> {
                    FeudMap.give(Live.get(c.getSource().getServer()), c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("map").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (Screens.open(Live.get(c.getSource().getServer()), p, "map", "")) return 1;
                    FeudMap.give(Live.get(c.getSource().getServer()), p);
                    c.getSource().sendSuccess(() -> Component.literal("You sketch a Map of the Feuds.").withStyle(ChatFormatting.GRAY), false);
                    return 1;
                }))
                .then(Commands.literal("brute").then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(knownNames(c), b))
                        .executes(c -> page(c.getSource(), StringArgumentType.getString(c, "name")))))
                .then(Commands.literal("rise").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("family", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        java.util.Arrays.stream(Family.values()).map(Family::key), b))
                                .executes(FeudCommand::rise)))
                .then(Commands.literal("scar").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("scar", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        java.util.Arrays.stream(io.github.pirut.bloodfeud.feud.Scar.values()).map(io.github.pirut.bloodfeud.feud.Scar::key), b))
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(knownNames(c), b))
                                        .executes(FeudCommand::scar))))
                .then(Commands.literal("night").requires(s -> s.hasPermission(2)).executes(c -> {
                    Live.get(c.getSource().getServer()).runNight();
                    c.getSource().sendSuccess(() -> Component.literal("A night has played out."), true);
                    return 1;
                }))
                .then(Commands.literal("camp").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("upgrade").executes(FeudCommand::campUpgrade))
                        .then(Commands.literal("ruin").executes(FeudCommand::campRuin))
                        .then(Commands.argument("family", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        java.util.Arrays.stream(Family.values()).map(Family::key), b))
                                .executes(c -> campSpawn(c, SettlementTier.CAMP, false))
                                .then(Commands.argument("tier", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                List.of("camp", "outpost", "stronghold", "town"), b))
                                        .executes(c -> campSpawn(c, null, false))
                                        .then(Commands.literal("ruined")
                                                .executes(c -> campSpawn(c, null, true))))))
                .then(Commands.literal("camps").requires(s -> s.hasPermission(2))
                        .executes(c -> campsStatus(c.getSource()))
                        .then(Commands.literal("on").executes(c -> campsToggle(c.getSource(), true)))
                        .then(Commands.literal("off").executes(c -> campsToggle(c.getSource(), false))))
                .then(Commands.literal("all").requires(s -> s.hasPermission(2)).executes(c -> all(c.getSource())))
                .then(Commands.literal("forget").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("everything").executes(c -> {
                            Live.get(c.getSource().getServer()).forgetAll();
                            c.getSource().sendSuccess(() -> Component.literal("Every Brute and pack is gone."), true);
                            return 1;
                        }))));
    }

    private static List<String> knownNames(CommandContext<CommandSourceStack> c) {
        List<String> out = new ArrayList<>();
        Live live = Live.get(c.getSource().getServer());
        ServerPlayer p = c.getSource().getPlayer();
        for (Brute b : live.feud().brutes.values()) {
            if (p == null || c.getSource().hasPermission(2) || met(b, p.getUUID())) out.add(b.name);
        }
        return out;
    }

    private static boolean met(Brute b, UUID player) {
        Relation r = b.relations.get(player);
        return r != null && r.met;
    }

    private static int packs(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Live live = Live.get(source.getServer());
        Feud f = live.feud();
        String dim = Live.dim(p.level());
        List<Pack> near = new ArrayList<>();
        int unmet = 0;
        for (Pack pk : f.packs.values()) {
            if (pk.distanceTo(dim, p.getX(), p.getZ()) > LIST_RANGE) continue;
            if (pk.met.contains(p.getUUID()) || source.hasPermission(2)) near.add(pk);
            else unmet++;
        }
        near.sort(Comparator.comparingDouble(pk -> pk.distanceTo(dim, p.getX(), p.getZ())));
        p.sendSystemMessage(Component.literal("☠ Packs around you").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        if (near.isEmpty()) {
            p.sendSystemMessage(Component.literal(unmet == 0
                    ? "None yet. Packs form where you fight: kill a few mobs while their friends watch."
                    : "You haven't met any of the packs around here yet.").withStyle(ChatFormatting.GRAY));
        }
        for (Pack pk : near) {
            Brute boss = f.brute(pk.boss);
            Pack top = f.top(pk);
            String tierTitle = pk.displayTierTitle(BloodFeud.config().buildCamps);
            String tierNote = tierTitle.isEmpty() ? "" : " — " + tierTitle;
            MutableComponent line = Component.literal("■ ").withStyle(s -> s.withColor(DyeColor.byId(pk.color).getTextColor()))
                    .append(Component.literal(cap(pk.title())).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                    .append(Component.literal(" (" + pk.family.title + tierNote + ")").withStyle(ChatFormatting.GRAY));
            p.sendSystemMessage(line);
            String leader = boss == null ? "no one" : boss.alive ? boss.fullName() + ", level " + boss.level
                    : boss.fullName() + " (dead, for now)";
            int brutes = f.members(pk).size() - (boss != null && boss.alive ? 1 : 0);
            String kneel = top != pk ? ". Kneels to " + top.title() : f.vassals(pk).isEmpty() ? ""
                    : ". Rules " + f.vassals(pk).size() + (f.vassals(pk).size() == 1 ? " other pack" : " other packs");
            p.sendSystemMessage(Component.literal("   " + (f.vassals(pk).isEmpty() ? "Boss" : "King") + ": " + leader
                    + ". " + brutes + (brutes == 1 ? " Brute, " : " Brutes, ") + pk.grunts + " grunts" + kneel + ".")
                    .withStyle(ChatFormatting.GRAY));
            p.sendSystemMessage(Component.literal("   " + where(p.getX(), p.getZ(), pk.campX(), pk.campZ()) + ", home at "
                    + pk.campX() + " " + pk.campZ()).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (unmet > 0 && !near.isEmpty()) {
            p.sendSystemMessage(Component.literal("...and " + unmet + (unmet == 1 ? " pack" : " packs") + " nearby you haven't met.")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        long lone = f.brutes.values().stream().filter(b -> b.alive && b.pack == null && met(b, p.getUUID())
                && b.dimension.equals(dim) && Math.hypot(b.x - p.getX(), b.z - p.getZ()) <= LIST_RANGE).count();
        if (lone > 0) p.sendSystemMessage(Component.literal(lone + (lone == 1 ? " Brute you know roams" : " Brutes you know roam")
                + " on its own nearby. /bloodfeud nemesis lists the ones after you.").withStyle(ChatFormatting.DARK_GRAY));
        return near.size();
    }

    private static int nemesis(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Feud f = Live.get(source.getServer()).feud();
        List<Brute> list = new ArrayList<>();
        for (Brute b : f.brutes.values()) {
            Relation r = b.relations.get(p.getUUID());
            if (b.alive && r != null && (r.nemesis || p.getUUID().equals(b.hunting))) list.add(b);
        }
        list.sort(Comparator.comparingInt((Brute b) -> -b.level));
        p.sendSystemMessage(Component.literal("☠ Who is after you").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        if (list.isEmpty()) p.sendSystemMessage(Component.literal("No one, for now.").withStyle(ChatFormatting.GRAY));
        for (Brute b : list) {
            Relation r = b.relation(p.getUUID());
            String dist = b.dimension.equals(Live.dim(p.level())) ? where(p.getX(), p.getZ(), b.x, b.z) : "in another dimension";
            p.sendSystemMessage(Component.literal(b.fullName()).withStyle(Live.rankColor(f.rank(b)))
                    .append(Component.literal(", level " + b.level + " " + f.standing(b) + ". Killed you " + r.killedPlayer
                            + (r.killedPlayer == 1 ? " time" : " times") + ". Last seen " + dist + ".").withStyle(ChatFormatting.GRAY)));
        }
        return list.size();
    }

    private static int page(CommandSourceStack source, String name) {
        Live live = Live.get(source.getServer());
        Feud f = live.feud();
        Brute b = f.byName(name);
        ServerPlayer p = source.getPlayer();
        if (b == null || (p != null && !source.hasPermission(2) && !met(b, p.getUUID()))) {
            source.sendFailure(Component.literal("You don't know anyone called " + name + "."));
            return 0;
        }
        Relation r = p == null ? null : b.relations.get(p.getUUID());
        Rank rank = f.rank(b);
        source.sendSystemMessage(Component.literal(b.fullName()).withStyle(Live.rankColor(rank), ChatFormatting.BOLD));
        source.sendSystemMessage(Component.literal("Level " + b.level + " " + f.standing(b) + ". " + cap(b.family.title)
                + ", " + typeName(b.entityType) + ". " + (b.alive ? "Alive" : b.finalDeath ? "Dead for good" : "Dead, for now")
                + (b.timesReturned > 0 ? ", came back " + b.timesReturned + (b.timesReturned == 1 ? " time" : " times") : "") + ".")
                .withStyle(ChatFormatting.GRAY));
        source.sendSystemMessage(Live.knownLine(b, source.hasPermission(2) && r == null ? allKnown(b) : r));
        if (r != null) {
            StringBuilder s = new StringBuilder();
            if (r.nemesis) s.append("Your Nemesis. ");
            s.append("It killed you ").append(r.killedPlayer).append(r.killedPlayer == 1 ? " time" : " times")
                    .append(", you killed it ").append(r.killedByPlayer).append(r.killedByPlayer == 1 ? " time." : " times.");
            source.sendSystemMessage(Component.literal(s.toString()).withStyle(r.nemesis ? ChatFormatting.RED : ChatFormatting.GRAY));
            int shown = 0;
            for (Iterator<Memory> it = r.memories.descendingIterator(); it.hasNext() && shown < 4; shown++) {
                source.sendSystemMessage(Component.literal("  - " + memory(it.next())).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        return 1;
    }

    private static Relation allKnown(Brute b) {
        Relation r = new Relation();
        r.known.addAll(b.traits);
        return r;
    }

    static String memory(Memory m) {
        String d = m.detail();
        return switch (m.kind()) {
            case KILLED_PLAYER -> "It killed you" + (d.isEmpty() ? "." : " (" + d + ").");
            case ROSE -> "It rose by killing you.";
            case SAW_KILLS -> "It watched you kill its kin, and rose.";
            case WAS_KILLED -> "You killed it" + (d.isEmpty() ? "." : " (" + d + ").");
            case RETURNED -> "It came back from the dead.";
            case PLAYER_FLED -> "You ran from it.";
            case IT_FLED -> "It ran from you.";
            case STOLE -> "It took your " + d + ".";
        };
    }

    private static int rise(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Family family = Family.byKey(StringArgumentType.getString(c, "family"));
        if (family == null) {
            c.getSource().sendFailure(Component.literal("Families: " + String.join(", ", java.util.Arrays.stream(Family.values()).map(Family::key).toList()) + "."));
            return 0;
        }
        Brute b = Live.get(c.getSource().getServer()).makeBrute(p, family);
        c.getSource().sendSuccess(() -> Component.literal(b.fullName() + " has risen."), true);
        return 1;
    }

    /** Ops only: gives a Brute a scar, as if it had come back from that death. */
    private static int scar(CommandContext<CommandSourceStack> c) {
        io.github.pirut.bloodfeud.feud.Scar scar = io.github.pirut.bloodfeud.feud.Scar.byKey(StringArgumentType.getString(c, "scar"));
        Live live = Live.get(c.getSource().getServer());
        Brute b = live.feud().byName(StringArgumentType.getString(c, "name"));
        if (scar == null || b == null) {
            c.getSource().sendFailure(Component.literal(scar == null ? "Scars: " + String.join(", ",
                    java.util.Arrays.stream(io.github.pirut.bloodfeud.feud.Scar.values()).map(io.github.pirut.bloodfeud.feud.Scar::key).toList()) + "."
                    : "No Brute by that name."));
            return 0;
        }
        live.scar(b, scar);
        c.getSource().sendSuccess(() -> Component.literal(b.fullName() + " now bears the " + scar.key() + " scar."), true);
        return 1;
    }

    private static int campSpawn(CommandContext<CommandSourceStack> c, SettlementTier fixedTier, boolean ruined) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Family family = Family.byKey(StringArgumentType.getString(c, "family"));
        if (family == null) {
            c.getSource().sendFailure(Component.literal("Families: "
                    + String.join(", ", java.util.Arrays.stream(Family.values()).map(Family::key).toList()) + "."));
            return 0;
        }
        SettlementTier tier = fixedTier;
        if (tier == null) {
            tier = SettlementTier.byKey(StringArgumentType.getString(c, "tier"));
            if (tier == null || tier == SettlementTier.NONE) {
                c.getSource().sendFailure(Component.literal("Tiers: camp, outpost, stronghold, town."));
                return 0;
            }
        }
        SettlementTier chosen = tier;
        String label = chosen.title(ruined);
        UUID playerId = p.getUUID();
        net.minecraft.server.MinecraftServer srv = c.getSource().getServer();
        Pack pack = Live.get(srv).spawnCommandCamp(p, family, chosen, ruined, built -> {
            ServerPlayer online = srv.getPlayerList().getPlayer(playerId);
            if (online != null) {
                online.sendSystemMessage(Component.literal(cap(built.title()) + " (" + label + ") is ready at "
                        + built.campX() + " " + built.siteY + " " + built.campZ() + ".").withStyle(ChatFormatting.GOLD));
            }
        });
        if (pack == null) {
            c.getSource().sendFailure(Component.literal("No dry, solid ground ahead of you to build "
                    + withArticle(label) + "."));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.literal("Building " + cap(pack.title()) + " (" + label
                + ", " + family.title + ") at " + pack.campX() + " " + pack.siteY + " " + pack.campZ() + "..."), true);
        if (!BloodFeud.config().buildCamps) {
            c.getSource().sendSystemMessage(Component.literal("Note: automatic pack camps are off (/bloodfeud camps on to re-enable).")
                    .withStyle(ChatFormatting.GRAY));
        }
        return 1;
    }

    private static int campUpgrade(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        UUID playerId = p.getUUID();
        net.minecraft.server.MinecraftServer srv = c.getSource().getServer();
        Live.UpgradeResult res = Live.get(srv).upgradeNearestCamp(p, built -> {
            ServerPlayer online = srv.getPlayerList().getPlayer(playerId);
            if (online != null) {
                online.sendSystemMessage(Component.literal(cap(built.title()) + " now stands as "
                        + withArticle(built.tier.title) + ".").withStyle(ChatFormatting.GOLD));
            }
        });
        if (!res.ok()) {
            c.getSource().sendFailure(Component.literal(res.error()));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.literal("Building " + cap(res.pack().title()) + " as "
                + withArticle(res.target().title) + " at " + res.pack().campX() + " " + res.pack().siteY + " " + res.pack().campZ() + "..."), true);
        if (!res.skippedZones().isEmpty() || res.skippedRingColumns() > 0) {
            StringBuilder note = new StringBuilder("Protected nearby builds/terrain — skipped");
            if (!res.skippedZones().isEmpty()) {
                note.append(" zones: ").append(String.join(", ", res.skippedZones()));
            }
            if (res.skippedRingColumns() > 0) {
                if (!res.skippedZones().isEmpty()) note.append("; and");
                note.append(" ").append(res.skippedRingColumns()).append(" wall/street columns");
            }
            note.append(".");
            c.getSource().sendSystemMessage(Component.literal(note.toString()).withStyle(ChatFormatting.YELLOW));
        }
        if (!BloodFeud.config().buildCamps) {
            c.getSource().sendSystemMessage(Component.literal("Note: automatic pack camps are off (/bloodfeud camps on to re-enable).")
                    .withStyle(ChatFormatting.GRAY));
        }
        return 1;
    }

    private static int campRuin(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        UUID playerId = p.getUUID();
        net.minecraft.server.MinecraftServer srv = c.getSource().getServer();
        Live.UpgradeResult res = Live.get(srv).ruinNearestCamp(p, built -> {
            ServerPlayer online = srv.getPlayerList().getPlayer(playerId);
            if (online != null) {
                online.sendSystemMessage(Component.literal(cap(built.title()) + "'s "
                        + resLabel(built) + " now lies in ruins.").withStyle(ChatFormatting.GRAY));
            }
        });
        if (!res.ok()) {
            c.getSource().sendFailure(Component.literal(res.error()));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.literal("Ruining " + cap(res.pack().title()) + " ("
                + res.target().title(true) + ") at " + res.pack().campX() + " " + res.pack().siteY + " " + res.pack().campZ() + "..."), true);
        if (!BloodFeud.config().buildCamps) {
            c.getSource().sendSystemMessage(Component.literal("Note: automatic pack camps are off (/bloodfeud camps on to re-enable).")
                    .withStyle(ChatFormatting.GRAY));
        }
        return 1;
    }

    private static String resLabel(Pack pack) {
        SettlementTier t = pack.builtTier > 0 ? SettlementTier.byLevel(pack.builtTier)
                : (pack.tier == null ? SettlementTier.CAMP : pack.tier);
        return t.key;
    }

    private static int campsStatus(CommandSourceStack source) {
        boolean on = BloodFeud.config().buildCamps;
        source.sendSuccess(() -> Component.literal("Automatic pack camps & towns are currently "
                + (on ? "ENABLED" : "DISABLED") + " (buildCamps=" + on + "). Use /bloodfeud camps "
                + (on ? "off" : "on") + " to toggle."), false);
        return on ? 1 : 0;
    }

    private static int campsToggle(CommandSourceStack source, boolean enabled) {
        BloodFeud.setBuildCamps(enabled);
        source.sendSuccess(() -> Component.literal("Automatic pack camps & towns are now "
                + (enabled ? "ENABLED" : "DISABLED")
                + " (saved to config/bloodfeud.properties)."
                + (enabled ? "" : " Any in-flight automatic builds were cancelled.")), true);
        return 1;
    }

    private static int all(CommandSourceStack source) {
        Feud f = Live.get(source.getServer()).feud();
        boolean campsOn = BloodFeud.config().buildCamps;
        source.sendSystemMessage(Component.literal(f.packs.size() + " packs, " + f.brutes.size() + " Brutes ("
                + f.brutes.values().stream().filter(b -> b.alive).count() + " alive).").withStyle(ChatFormatting.GOLD));
        for (Pack pk : f.packs.values()) {
            Brute boss = f.brute(pk.boss);
            String tierTitle = pk.displayTierTitle(campsOn);
            String tierPart = tierTitle.isEmpty() ? "" : " [" + tierTitle + (pk.builtTier > 0 ? " built" : "") + "]";
            source.sendSystemMessage(Component.literal(cap(pk.title()) + " [" + pk.family.key() + "]" + tierPart + " " + pk.dimension + " "
                    + pk.campX() + " " + pk.campZ() + ": " + (boss == null ? "?" : boss.fullName() + " L" + boss.level) + ", "
                    + f.members(pk).size() + " Brutes, " + pk.grunts + " grunts" + (pk.liege != null ? ", vassal" : ""))
                    .withStyle(ChatFormatting.GRAY));
        }
        return f.packs.size();
    }

    static String where(double fromX, double fromZ, int x, int z) {
        double dx = x - fromX, dz = z - fromZ;
        int d = (int) Math.round(Math.hypot(dx, dz));
        if (d < 24) return "right here";
        String[] dirs = {"south", "southwest", "west", "northwest", "north", "northeast", "east", "southeast"};
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        int i = (int) Math.round(((angle % 360) + 360) % 360 / 45) % 8;
        return d + " blocks " + dirs[i];
    }

    private static String typeName(String type) {
        String t = type.contains(":") ? type.substring(type.indexOf(':') + 1) : type;
        return t.replace('_', ' ');
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    private static String withArticle(String word) {
        if (word == null || word.isEmpty()) return "";
        char c = Character.toLowerCase(word.charAt(0));
        return ("aeiou".indexOf(c) >= 0 ? "an " : "a ") + word;
    }
}
