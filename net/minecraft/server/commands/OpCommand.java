package net.minecraft.server.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;

public class OpCommand {
   private static final SimpleCommandExceptionType ERROR_ALREADY_OP = new SimpleCommandExceptionType(Component.translatable("commands.op.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> p_138080_) {
      p_138080_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("op").requires(p_138087_ -> p_138087_.hasPermission(3)))
            .then(
               Commands.argument("targets", GameProfileArgument.gameProfile())
                  .suggests(
                     (p_138084_, p_138085_) -> {
                        PlayerList $$2 = ((CommandSourceStack)p_138084_.getSource()).getServer().getPlayerList();
                        return SharedSuggestionProvider.suggest(
                           $$2.getPlayers()
                              .stream()
                              .filter(p_352693_ -> !$$2.isOp(p_352693_.getGameProfile()))
                              .map(p_352691_ -> p_352691_.getGameProfile().getName()),
                           p_138085_
                        );
                     }
                  )
                  .executes(p_138082_ -> opPlayers((CommandSourceStack)p_138082_.getSource(), GameProfileArgument.getGameProfiles(p_138082_, "targets")))
            )
      );
   }

   private static int opPlayers(CommandSourceStack p_138089_, Collection<GameProfile> p_138090_) throws CommandSyntaxException {
      PlayerList $$2 = p_138089_.getServer().getPlayerList();
      int $$3 = 0;

      for (GameProfile $$4 : p_138090_) {
         if (!$$2.isOp($$4)) {
            $$2.op($$4);
            $$3++;
            p_138089_.sendSuccess(() -> Component.translatable("commands.op.success", p_138090_.iterator().next().getName()), true);
         }
      }

      if ($$3 == 0) {
         throw ERROR_ALREADY_OP.create();
      } else {
         return $$3;
      }
   }
}
