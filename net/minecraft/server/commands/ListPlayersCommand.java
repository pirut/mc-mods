package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;

public class ListPlayersCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("list")
               .executes(commandContext -> listPlayers((CommandSourceStack)commandContext.getSource())))
            .then(Commands.literal("uuids").executes(commandContext -> listPlayersWithUuids((CommandSourceStack)commandContext.getSource())))
      );
   }

   private static int listPlayers(CommandSourceStack source) {
      return format(source, Player::getDisplayName);
   }

   private static int listPlayersWithUuids(CommandSourceStack source) {
      return format(
         source,
         serverPlayer -> Component.translatable("commands.list.nameAndId", serverPlayer.getName(), Component.translationArg(serverPlayer.getGameProfile().id()))
      );
   }

   private static int format(CommandSourceStack source, Function<ServerPlayer, Component> nameExtractor) {
      PlayerList playerList = source.getServer().getPlayerList();
      List<ServerPlayer> list = playerList.getPlayers();
      Component component = ComponentUtils.formatList(list, nameExtractor);
      source.sendSuccess(() -> Component.translatable("commands.list.players", list.size(), playerList.getMaxPlayers(), component), false);
      return list.size();
   }
}
