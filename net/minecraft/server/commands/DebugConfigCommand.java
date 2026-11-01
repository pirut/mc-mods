package net.minecraft.server.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.jspecify.annotations.Nullable;

public class DebugConfigCommand {
   public static void register(CommandDispatcher<CommandSourceStack> p_294998_, CommandBuildContext p_427272_) {
      p_294998_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("debugconfig")
                     .requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
                  .then(
                     Commands.literal("config")
                        .then(
                           Commands.argument("target", EntityArgument.player())
                              .executes(p_294114_ -> config((CommandSourceStack)p_294114_.getSource(), EntityArgument.getPlayer(p_294114_, "target")))
                        )
                  ))
               .then(
                  Commands.literal("unconfig")
                     .then(
                        Commands.argument("target", UuidArgument.uuid())
                           .suggests(
                              (p_295936_, p_294731_) -> SharedSuggestionProvider.suggest(
                                 getUuidsInConfig(((CommandSourceStack)p_295936_.getSource()).getServer()), p_294731_
                              )
                           )
                           .executes(p_294910_ -> unconfig((CommandSourceStack)p_294910_.getSource(), UuidArgument.getUuid(p_294910_, "target")))
                     )
               ))
            .then(
               Commands.literal("dialog")
                  .then(
                     Commands.argument("target", UuidArgument.uuid())
                        .suggests(
                           (p_426945_, p_426946_) -> SharedSuggestionProvider.suggest(
                              getUuidsInConfig(((CommandSourceStack)p_426945_.getSource()).getServer()), p_426946_
                           )
                        )
                        .then(
                           Commands.argument("dialog", ResourceOrIdArgument.dialog(p_427272_))
                              .executes(
                                 p_426944_ -> showDialog(
                                    (CommandSourceStack)p_426944_.getSource(),
                                    UuidArgument.getUuid(p_426944_, "target"),
                                    ResourceOrIdArgument.getDialog(p_426944_, "dialog")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static Iterable<String> getUuidsInConfig(MinecraftServer p_294210_) {
      Set<String> set = new HashSet<>();

      for (Connection connection : p_294210_.getConnection().getConnections()) {
         if (connection.getPacketListener() instanceof ServerConfigurationPacketListenerImpl serverconfigurationpacketlistenerimpl) {
            set.add(serverconfigurationpacketlistenerimpl.getOwner().id().toString());
         }
      }

      return set;
   }

   private static int config(CommandSourceStack p_295666_, ServerPlayer p_294477_) {
      GameProfile gameprofile = p_294477_.getGameProfile();
      p_294477_.connection.switchToConfig();
      p_295666_.sendSuccess(() -> Component.literal("Switched player " + gameprofile.name() + "(" + gameprofile.id() + ") to config mode"), false);
      return 1;
   }

   @Nullable
   private static ServerConfigurationPacketListenerImpl findConfigPlayer(MinecraftServer p_427394_, UUID p_427324_) {
      for (Connection connection : p_427394_.getConnection().getConnections()) {
         if (connection.getPacketListener() instanceof ServerConfigurationPacketListenerImpl serverconfigurationpacketlistenerimpl
            && serverconfigurationpacketlistenerimpl.getOwner().id().equals(p_427324_)) {
            return serverconfigurationpacketlistenerimpl;
         }
      }

      return null;
   }

   private static int unconfig(CommandSourceStack p_294216_, UUID p_296468_) {
      ServerConfigurationPacketListenerImpl serverconfigurationpacketlistenerimpl = findConfigPlayer(p_294216_.getServer(), p_296468_);
      if (serverconfigurationpacketlistenerimpl != null) {
         serverconfigurationpacketlistenerimpl.returnToWorld();
         return 1;
      } else {
         p_294216_.sendFailure(Component.literal("Can't find player to unconfig"));
         return 0;
      }
   }

   private static int showDialog(CommandSourceStack p_427433_, UUID p_427293_, Holder<Dialog> p_427499_) {
      ServerConfigurationPacketListenerImpl serverconfigurationpacketlistenerimpl = findConfigPlayer(p_427433_.getServer(), p_427293_);
      if (serverconfigurationpacketlistenerimpl != null) {
         serverconfigurationpacketlistenerimpl.send(new ClientboundShowDialogPacket(p_427499_));
         return 1;
      } else {
         p_427433_.sendFailure(Component.literal("Can't find player to talk to"));
         return 0;
      }
   }
}
