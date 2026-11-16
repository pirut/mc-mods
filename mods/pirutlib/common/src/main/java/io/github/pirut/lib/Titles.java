package io.github.pirut.lib;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

/** Big text on a player's screen. All vanilla, so players without the mod see it too. */
public final class Titles {
    private Titles() {
    }

    /** A title card: {@code title} big, {@code subtitle} under it (or null). Times are in ticks. */
    public static void show(ServerPlayer player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        if (subtitle != null) player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** A title card with the usual timing: half a second in, three and a half on screen, one second out. */
    public static void show(ServerPlayer player, Component title, Component subtitle) {
        show(player, title, subtitle, 10, 70, 20);
    }

    /** A line just above the hotbar. */
    public static void actionBar(ServerPlayer player, Component text) {
        player.displayClientMessage(text, true);
    }
}
