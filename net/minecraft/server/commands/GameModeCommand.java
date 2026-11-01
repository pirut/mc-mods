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
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;

public class GameModeCommand {
   public static final int PERMISSION_LEVEL = 2;

   public static void register(CommandDispatcher<CommandSourceStack> p_137730_) {
      p_137730_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("gamemode").requires(p_137736_ -> p_137736_.hasPermission(2)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("gamemode", GameModeArgument.gameMode())
                     .executes(
                        p_258228_ -> setMode(
                           p_258228_,
                           Collections.singleton(((CommandSourceStack)p_258228_.getSource()).getPlayerOrException()),
                           GameModeArgument.getGameMode(p_258228_, "gamemode")
                        )
                     ))
                  .then(
                     Commands.argument("target", EntityArgument.players())
                        .executes(
                           p_258229_ -> setMode(p_258229_, EntityArgument.getPlayers(p_258229_, "target"), GameModeArgument.getGameMode(p_258229_, "gamemode"))
                        )
                  )
            )
      );
   }

   private static void logGamemodeChange(CommandSourceStack p_137738_, ServerPlayer p_137739_, GameType p_137740_) {
      Component $$3 = Component.translatable("gameMode." + p_137740_.getName());
      if (p_137738_.getEntity() == p_137739_) {
         p_137738_.sendSuccess(() -> Component.translatable("commands.gamemode.success.self", $$3), true);
      } else {
         if (p_137738_.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
            p_137739_.sendSystemMessage(Component.translatable("gameMode.changed", $$3));
         }

         p_137738_.sendSuccess(() -> Component.translatable("commands.gamemode.success.other", p_137739_.getDisplayName(), $$3), true);
      }
   }

   private static int setMode(CommandContext<CommandSourceStack> p_137732_, Collection<ServerPlayer> p_137733_, GameType p_137734_) {
      int $$3 = 0;

      for (ServerPlayer $$4 : p_137733_) {
         if ($$4.setGameMode(p_137734_)) {
            logGamemodeChange((CommandSourceStack)p_137732_.getSource(), $$4, p_137734_);
            $$3++;
         }
      }

      return $$3;
   }
}
