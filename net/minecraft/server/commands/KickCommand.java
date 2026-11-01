package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class KickCommand {
   private static final SimpleCommandExceptionType ERROR_KICKING_OWNER = new SimpleCommandExceptionType(Component.translatable("commands.kick.owner.failed"));
   private static final SimpleCommandExceptionType ERROR_SINGLEPLAYER = new SimpleCommandExceptionType(
      Component.translatable("commands.kick.singleplayer.failed")
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_137796_) {
      p_137796_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("kick").requires(p_137800_ -> p_137800_.hasPermission(3)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players())
                     .executes(
                        p_137806_ -> kickPlayers(
                           (CommandSourceStack)p_137806_.getSource(),
                           EntityArgument.getPlayers(p_137806_, "targets"),
                           Component.translatable("multiplayer.disconnect.kicked")
                        )
                     ))
                  .then(
                     Commands.argument("reason", MessageArgument.message())
                        .executes(
                           p_137798_ -> kickPlayers(
                              (CommandSourceStack)p_137798_.getSource(),
                              EntityArgument.getPlayers(p_137798_, "targets"),
                              MessageArgument.getMessage(p_137798_, "reason")
                           )
                        )
                  )
            )
      );
   }

   private static int kickPlayers(CommandSourceStack p_137802_, Collection<ServerPlayer> p_137803_, Component p_137804_) throws CommandSyntaxException {
      if (!p_137802_.getServer().isPublished()) {
         throw ERROR_SINGLEPLAYER.create();
      } else {
         int $$3 = 0;

         for (ServerPlayer $$4 : p_137803_) {
            if (!p_137802_.getServer().isSingleplayerOwner($$4.getGameProfile())) {
               $$4.connection.disconnect(p_137804_);
               p_137802_.sendSuccess(() -> Component.translatable("commands.kick.success", $$4.getDisplayName(), p_137804_), true);
               $$3++;
            }
         }

         if ($$3 == 0) {
            throw ERROR_KICKING_OWNER.create();
         } else {
            return $$3;
         }
      }
   }
}
