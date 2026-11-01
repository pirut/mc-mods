package net.minecraft.server.commands;

import com.google.common.net.InetAddresses;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;

public class BanIpCommands {
   private static final SimpleCommandExceptionType ERROR_INVALID_IP = new SimpleCommandExceptionType(Component.translatable("commands.banip.invalid"));
   private static final SimpleCommandExceptionType ERROR_ALREADY_BANNED = new SimpleCommandExceptionType(Component.translatable("commands.banip.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> p_136528_) {
      p_136528_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("ban-ip").requires(p_136532_ -> p_136532_.hasPermission(3)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("target", StringArgumentType.word())
                     .executes(p_136538_ -> banIpOrName((CommandSourceStack)p_136538_.getSource(), StringArgumentType.getString(p_136538_, "target"), null)))
                  .then(
                     Commands.argument("reason", MessageArgument.message())
                        .executes(
                           p_136530_ -> banIpOrName(
                              (CommandSourceStack)p_136530_.getSource(),
                              StringArgumentType.getString(p_136530_, "target"),
                              MessageArgument.getMessage(p_136530_, "reason")
                           )
                        )
                  )
            )
      );
   }

   private static int banIpOrName(CommandSourceStack p_136534_, String p_136535_, @Nullable Component p_136536_) throws CommandSyntaxException {
      if (InetAddresses.isInetAddress(p_136535_)) {
         return banIp(p_136534_, p_136535_, p_136536_);
      } else {
         ServerPlayer $$3 = p_136534_.getServer().getPlayerList().getPlayerByName(p_136535_);
         if ($$3 != null) {
            return banIp(p_136534_, $$3.getIpAddress(), p_136536_);
         } else {
            throw ERROR_INVALID_IP.create();
         }
      }
   }

   private static int banIp(CommandSourceStack p_136540_, String p_136541_, @Nullable Component p_136542_) throws CommandSyntaxException {
      IpBanList $$3 = p_136540_.getServer().getPlayerList().getIpBans();
      if ($$3.isBanned(p_136541_)) {
         throw ERROR_ALREADY_BANNED.create();
      } else {
         List<ServerPlayer> $$4 = p_136540_.getServer().getPlayerList().getPlayersWithAddress(p_136541_);
         IpBanListEntry $$5 = new IpBanListEntry(p_136541_, null, p_136540_.getTextName(), null, p_136542_ == null ? null : p_136542_.getString());
         $$3.add($$5);
         p_136540_.sendSuccess(() -> Component.translatable("commands.banip.success", p_136541_, $$5.getReason()), true);
         if (!$$4.isEmpty()) {
            p_136540_.sendSuccess(() -> Component.translatable("commands.banip.info", $$4.size(), EntitySelector.joinNames($$4)), true);
         }

         for (ServerPlayer $$6 : $$4) {
            $$6.connection.disconnect(Component.translatable("multiplayer.disconnect.ip_banned"));
         }

         return $$4.size();
      }
   }
}
