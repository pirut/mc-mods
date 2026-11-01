package net.minecraft.server.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;

public class BanPlayerCommands {
   private static final SimpleCommandExceptionType ERROR_ALREADY_BANNED = new SimpleCommandExceptionType(Component.translatable("commands.ban.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> p_136559_) {
      p_136559_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("ban").requires(p_136563_ -> p_136563_.hasPermission(3)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("targets", GameProfileArgument.gameProfile())
                     .executes(
                        p_136569_ -> banPlayers((CommandSourceStack)p_136569_.getSource(), GameProfileArgument.getGameProfiles(p_136569_, "targets"), null)
                     ))
                  .then(
                     Commands.argument("reason", MessageArgument.message())
                        .executes(
                           p_136561_ -> banPlayers(
                              (CommandSourceStack)p_136561_.getSource(),
                              GameProfileArgument.getGameProfiles(p_136561_, "targets"),
                              MessageArgument.getMessage(p_136561_, "reason")
                           )
                        )
                  )
            )
      );
   }

   private static int banPlayers(CommandSourceStack p_136565_, Collection<GameProfile> p_136566_, @Nullable Component p_136567_) throws CommandSyntaxException {
      UserBanList $$3 = p_136565_.getServer().getPlayerList().getBans();
      int $$4 = 0;

      for (GameProfile $$5 : p_136566_) {
         if (!$$3.isBanned($$5)) {
            UserBanListEntry $$6 = new UserBanListEntry($$5, null, p_136565_.getTextName(), null, p_136567_ == null ? null : p_136567_.getString());
            $$3.add($$6);
            $$4++;
            p_136565_.sendSuccess(() -> Component.translatable("commands.ban.success", Component.literal($$5.getName()), $$6.getReason()), true);
            ServerPlayer $$7 = p_136565_.getServer().getPlayerList().getPlayer($$5.getId());
            if ($$7 != null) {
               $$7.connection.disconnect(Component.translatable("multiplayer.disconnect.banned"));
            }
         }
      }

      if ($$4 == 0) {
         throw ERROR_ALREADY_BANNED.create();
      } else {
         return $$4;
      }
   }
}
