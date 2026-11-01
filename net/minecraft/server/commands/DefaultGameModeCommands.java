package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public class DefaultGameModeCommands {
   public static void register(CommandDispatcher<CommandSourceStack> p_136927_) {
      p_136927_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("defaultgamemode").requires(p_136929_ -> p_136929_.hasPermission(2)))
            .then(
               Commands.argument("gamemode", GameModeArgument.gameMode())
                  .executes(p_258227_ -> setMode((CommandSourceStack)p_258227_.getSource(), GameModeArgument.getGameMode(p_258227_, "gamemode")))
            )
      );
   }

   private static int setMode(CommandSourceStack p_136931_, GameType p_136932_) {
      int $$2 = 0;
      MinecraftServer $$3 = p_136931_.getServer();
      $$3.setDefaultGameType(p_136932_);
      GameType $$4 = $$3.getForcedGameType();
      if ($$4 != null) {
         for (ServerPlayer $$5 : $$3.getPlayerList().getPlayers()) {
            if ($$5.setGameMode($$4)) {
               $$2++;
            }
         }
      }

      p_136931_.sendSuccess(() -> Component.translatable("commands.defaultgamemode.success", p_136932_.getLongDisplayName()), true);
      return $$2;
   }
}
