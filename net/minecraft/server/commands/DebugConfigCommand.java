package net.minecraft.server.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;

public class DebugConfigCommand {
   public static void register(CommandDispatcher<CommandSourceStack> p_294998_) {
      p_294998_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("debugconfig")
                  .requires(p_295607_ -> p_295607_.hasPermission(3)))
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
            )
      );
   }

   private static Iterable<String> getUuidsInConfig(MinecraftServer p_294210_) {
      Set<String> $$1 = new HashSet<>();

      for (Connection $$2 : p_294210_.getConnection().getConnections()) {
         if ($$2.getPacketListener() instanceof ServerConfigurationPacketListenerImpl $$3) {
            $$1.add($$3.getOwner().getId().toString());
         }
      }

      return $$1;
   }

   private static int config(CommandSourceStack p_295666_, ServerPlayer p_294477_) {
      GameProfile $$2 = p_294477_.getGameProfile();
      p_294477_.connection.switchToConfig();
      p_295666_.sendSuccess(() -> Component.literal("Switched player " + $$2.getName() + "(" + $$2.getId() + ") to config mode"), false);
      return 1;
   }

   private static int unconfig(CommandSourceStack p_294216_, UUID p_296468_) {
      for (Connection $$2 : p_294216_.getServer().getConnection().getConnections()) {
         if ($$2.getPacketListener() instanceof ServerConfigurationPacketListenerImpl $$3 && $$3.getOwner().getId().equals(p_296468_)) {
            $$3.returnToWorld();
         }
      }

      p_294216_.sendFailure(Component.literal("Can't find player to unconfig"));
      return 0;
   }
}
