package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.feud.Brute;
import io.github.pirut.bloodfeud.feud.DeathCause;
import io.github.pirut.bloodfeud.feud.Memory;
import io.github.pirut.bloodfeud.feud.Rank;
import io.github.pirut.bloodfeud.feud.Relation;

import java.util.Random;

/** What Brutes say. All of it written for this mod. */
public final class Lines {
    private Lines() {
    }

    /** The first thing it says when it finds a player. */
    public static String greet(Random r, Brute b, Relation rel, Rank rank, boolean hunted) {
        Memory last = rel == null ? null : rel.last();
        if (hunted && rel != null && rel.nemesis) {
            return pick(r, "You thought you could walk far enough? I walk too.",
                    "Every step you took, I took two.", "You left. I followed. That's how this works now.");
        }
        if (last != null) {
            switch (last.kind()) {
                case RETURNED, WAS_KILLED -> {
                    return returned(r, DeathCause.byKey(lastCause(rel)));
                }
                case KILLED_PLAYER, ROSE -> {
                    if (rel.killedPlayer >= 3) return pick(r, "That's " + rel.killedPlayer + " times now. I keep count.",
                            "Again? You never learn, and I never get tired of it.");
                    return pick(r, "Back for another beating?", "Remember me? I'm the one who put you in the dirt.",
                            "Your bed's a long way off. That's how I like it.");
                }
                case PLAYER_FLED -> {
                    return pick(r, "Ran off last time. Your legs tired yet?", "No running today. I brought friends.");
                }
                case STOLE -> {
                    return pick(r, "Still got your " + last.detail() + ". Want it back? Come and take it.",
                            "Nice " + last.detail() + ". Mine now.");
                }
                case SAW_KILLS -> {
                    return pick(r, "You cut down my kin like wheat. I counted every one.",
                            "I watched you. I learned. Now it's my turn.", "You killed my brothers. I'm still here.");
                }
                default -> {
                }
            }
        }
        if (rank == Rank.KING) return pick(r, "Every pack here kneels to me. You'll kneel too.",
                "Kings don't run. Kings don't die. Ask the last one who tried.");
        if (rank == Rank.BOSS) return pick(r, "This is our place. You're not leaving it.",
                "You're on our ground. Get them!", "The pack is hungry tonight.");
        return pick(r, "Fresh meat!", "Oh, I've heard about you.", "Let's see what you've got.");
    }

    private static String lastCause(Relation rel) {
        for (java.util.Iterator<Memory> it = rel.memories.descendingIterator(); it.hasNext(); ) {
            Memory m = it.next();
            if (m.kind() == Memory.Kind.WAS_KILLED) return m.detail();
        }
        return "";
    }

    private static String returned(Random r, DeathCause cause) {
        return switch (cause) {
            case FIRE -> pick(r, "You burned me. Look at me now. Look!", "I still smell the smoke. Do you?");
            case ARROW -> pick(r, "I've still got your arrow in me. Came to give it back.", "One arrow? That's all you had?");
            case FALL -> pick(r, "You knocked me off a cliff. I climbed back up.", "I've learned to land. Have you?");
            case DROWNING -> pick(r, "You left me at the bottom of the water. I walked out.", "The water didn't want me either.");
            case EXPLOSION -> pick(r, "You blew me to bits. Most of them came back.", "I'm missing a few pieces. You'll pay for each one.");
            case BEAST -> pick(r, "Your dog chewed me up. Where is it now?", "Bring your animals. I'm ready for them this time.");
            default -> pick(r, "You cut me down. It didn't take.", "Remember me? You killed me. Bad job.",
                    "Dead? Me? Not even close.");
        };
    }

    public static String killedPlayer(Random r, int times) {
        if (times >= 3) return pick(r, "That's " + times + ". Keep coming.", "Again. Always again.");
        return pick(r, "Stay down.", "Tell your bed I said hello.", "That's how it goes.", "Next!");
    }

    public static String falling(Random r, boolean forGood) {
        if (forGood) return pick(r, "No... not like this...", "Tell the pack... I...", "Huh. So that's what it's like.");
        return pick(r, "This isn't over...", "I'll be back. You'll see.", "You'll wish you'd finished it.",
                "Remember my name...");
    }

    public static String rose(Random r, boolean killedYou) {
        if (killedYou) return pick(r, "Look at that. I'm somebody now.", "I'll remember you. Every time.");
        return pick(r, "You killed my kin. I won't forget it.", "I saw what you did. I'll find you.",
                "Run along. I'll come to you.");
    }

    public static String flee(Random r) {
        return pick(r, "Get it away from me!", "No, no, no!", "Not that! Anything but that!");
    }

    public static String driveOff(Random r) {
        return pick(r, "Run, then! Run and keep running!", "That's right. Go.", "Coward! I'll find you again.");
    }

    public static String stole(Random r, String item) {
        return pick(r, "Ooh, a " + item + ". Mine now.", "Thanks for the " + item + "!", "Finders keepers.");
    }

    private static String pick(Random r, String... options) {
        return options[r.nextInt(options.length)];
    }
}
