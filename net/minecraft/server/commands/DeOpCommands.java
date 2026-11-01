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

public class DeOpCommands {
   private static final SimpleCommandExceptionType ERROR_NOT_OP = new SimpleCommandExceptionType(Component.translatable("commands.deop.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> p_136889_) {
      p_136889_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("deop").requires(p_136896_ -> p_136896_.hasPermission(3)))
            .then(
               Commands.argument("targets", GameProfileArgument.gameProfile())
                  .suggests(
                     (p_136893_, p_136894_) -> SharedSuggestionProvider.suggest(
                        ((CommandSourceStack)p_136893_.getSource()).getServer().getPlayerList().getOpNames(), p_136894_
                     )
                  )
                  .executes(p_136891_ -> deopPlayers((CommandSourceStack)p_136891_.getSource(), GameProfileArgument.getGameProfiles(p_136891_, "targets")))
            )
      );
   }

   private static int deopPlayers(CommandSourceStack p_136898_, Collection<GameProfile> p_136899_) throws CommandSyntaxException {
      PlayerList $$2 = p_136898_.getServer().getPlayerList();
      int $$3 = 0;

      for (GameProfile $$4 : p_136899_) {
         if ($$2.isOp($$4)) {
            $$2.deop($$4);
            $$3++;
            p_136898_.sendSuccess(() -> Component.translatable("commands.deop.success", p_136899_.iterator().next().getName()), true);
         }
      }

      if ($$3 == 0) {
         throw ERROR_NOT_OP.create();
      } else {
         p_136898_.getServer().kickUnlistedPlayers(p_136898_);
         return $$3;
      }
   }
}
