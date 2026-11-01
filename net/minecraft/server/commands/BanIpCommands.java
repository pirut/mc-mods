package net.minecraft.server.commands;

import com.google.common.net.InetAddresses;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;
import org.jspecify.annotations.Nullable;

public class BanIpCommands {
   private static final SimpleCommandExceptionType ERROR_INVALID_IP = new SimpleCommandExceptionType(Component.translatable("commands.banip.invalid"));
   private static final SimpleCommandExceptionType ERROR_ALREADY_BANNED = new SimpleCommandExceptionType(Component.translatable("commands.banip.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("ban-ip").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("target", StringArgumentType.word())
                     .executes(
                        commandContext -> banIpOrName(
                           (CommandSourceStack)commandContext.getSource(), StringArgumentType.getString(commandContext, "target"), null
                        )
                     ))
                  .then(
                     Commands.argument("reason", MessageArgument.message())
                        .executes(
                           commandContext -> banIpOrName(
                              (CommandSourceStack)commandContext.getSource(),
                              StringArgumentType.getString(commandContext, "target"),
                              MessageArgument.getMessage(commandContext, "reason")
                           )
                        )
                  )
            )
      );
   }

   private static int banIpOrName(CommandSourceStack source, String username, @Nullable Component reason) throws CommandSyntaxException {
      if (InetAddresses.isInetAddress(username)) {
         return banIp(source, username, reason);
      } else {
         ServerPlayer serverPlayer = source.getServer().getPlayerList().getPlayerByName(username);
         if (serverPlayer != null) {
            return banIp(source, serverPlayer.getIpAddress(), reason);
         } else {
            throw ERROR_INVALID_IP.create();
         }
      }
   }

   private static int banIp(CommandSourceStack source, String ip, @Nullable Component reason) throws CommandSyntaxException {
      IpBanList ipBanList = source.getServer().getPlayerList().getIpBans();
      if (ipBanList.isBanned(ip)) {
         throw ERROR_ALREADY_BANNED.create();
      } else {
         List<ServerPlayer> list = source.getServer().getPlayerList().getPlayersWithAddress(ip);
         IpBanListEntry ipBanListEntry = new IpBanListEntry(ip, null, source.getTextName(), null, reason == null ? null : reason.getString());
         ipBanList.add(ipBanListEntry);
         source.sendSuccess(() -> Component.translatable("commands.banip.success", ip, ipBanListEntry.getReasonMessage()), true);
         if (!list.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.banip.info", list.size(), EntitySelector.joinNames(list)), true);
         }

         for (ServerPlayer serverPlayer : list) {
            serverPlayer.connection.disconnect(Component.translatable("multiplayer.disconnect.ip_banned"));
         }

         return list.size();
      }
   }
}
