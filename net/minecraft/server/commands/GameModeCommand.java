package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;

public class GameModeCommand {
   public static final PermissionCheck PERMISSION_CHECK = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("gamemode").requires(Commands.hasPermission(PERMISSION_CHECK)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("gamemode", GameModeArgument.gameMode())
                     .executes(
                        commandContext -> setMode(
                           commandContext,
                           Collections.singleton(((CommandSourceStack)commandContext.getSource()).getPlayerOrException()),
                           GameModeArgument.getGameMode(commandContext, "gamemode")
                        )
                     ))
                  .then(
                     Commands.argument("target", EntityArgument.players())
                        .executes(
                           commandContext -> setMode(
                              commandContext, EntityArgument.getPlayers(commandContext, "target"), GameModeArgument.getGameMode(commandContext, "gamemode")
                           )
                        )
                  )
            )
      );
   }

   private static void logGamemodeChange(CommandSourceStack source, ServerPlayer player, GameType gameType) {
      Component component = Component.translatable("gameMode." + gameType.getName());
      if (source.getEntity() == player) {
         source.sendSuccess(() -> Component.translatable("commands.gamemode.success.self", component), true);
      } else {
         if (source.getLevel().getGameRules().get(GameRules.SEND_COMMAND_FEEDBACK)) {
            player.sendSystemMessage(Component.translatable("gameMode.changed", component));
         }

         source.sendSuccess(() -> Component.translatable("commands.gamemode.success.other", player.getDisplayName(), component), true);
      }
   }

   private static int setMode(CommandContext<CommandSourceStack> source, Collection<ServerPlayer> players, GameType gameType) {
      int i = 0;

      for (ServerPlayer serverPlayer : players) {
         if (setGameMode((CommandSourceStack)source.getSource(), serverPlayer, gameType)) {
            i++;
         }
      }

      return i;
   }

   public static void setGameMode(ServerPlayer player, GameType gameMode) {
      setGameMode(player.createCommandSourceStack(), player, gameMode);
   }

   private static boolean setGameMode(CommandSourceStack source, ServerPlayer player, GameType gameMode) {
      if (player.setGameMode(gameMode)) {
         logGamemodeChange(source, player, gameMode);
         return true;
      } else {
         return false;
      }
   }
}
