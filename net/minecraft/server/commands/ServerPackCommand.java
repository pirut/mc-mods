package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;

public class ServerPackCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("serverpack")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(
                  Commands.literal("push")
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("url", StringArgumentType.string())
                              .then(
                                 ((RequiredArgumentBuilder)Commands.argument("uuid", UuidArgument.uuid())
                                       .then(
                                          Commands.argument("hash", StringArgumentType.word())
                                             .executes(
                                                commandContext -> pushPack(
                                                   (CommandSourceStack)commandContext.getSource(),
                                                   StringArgumentType.getString(commandContext, "url"),
                                                   Optional.of(UuidArgument.getUuid(commandContext, "uuid")),
                                                   Optional.of(StringArgumentType.getString(commandContext, "hash"))
                                                )
                                             )
                                       ))
                                    .executes(
                                       commandContext -> pushPack(
                                          (CommandSourceStack)commandContext.getSource(),
                                          StringArgumentType.getString(commandContext, "url"),
                                          Optional.of(UuidArgument.getUuid(commandContext, "uuid")),
                                          Optional.empty()
                                       )
                                    )
                              ))
                           .executes(
                              commandContext -> pushPack(
                                 (CommandSourceStack)commandContext.getSource(),
                                 StringArgumentType.getString(commandContext, "url"),
                                 Optional.empty(),
                                 Optional.empty()
                              )
                           )
                     )
               ))
            .then(
               Commands.literal("pop")
                  .then(
                     Commands.argument("uuid", UuidArgument.uuid())
                        .executes(commandContext -> popPack((CommandSourceStack)commandContext.getSource(), UuidArgument.getUuid(commandContext, "uuid")))
                  )
            )
      );
   }

   private static void sendToAllConnections(CommandSourceStack source, Packet<?> packet) {
      source.getServer().getConnection().getConnections().forEach(connection -> connection.send(packet));
   }

   private static int pushPack(CommandSourceStack source, String url, Optional<UUID> uuid, Optional<String> hash) {
      UUID uUID = uuid.orElseGet(() -> UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)));
      String string = hash.orElse("");
      ClientboundResourcePackPushPacket clientboundResourcePackPushPacket = new ClientboundResourcePackPushPacket(uUID, url, string, false, null);
      sendToAllConnections(source, clientboundResourcePackPushPacket);
      return 0;
   }

   private static int popPack(CommandSourceStack source, UUID uuid) {
      ClientboundResourcePackPopPacket clientboundResourcePackPopPacket = new ClientboundResourcePackPopPacket(Optional.of(uuid));
      sendToAllConnections(source, clientboundResourcePackPopPacket);
      return 0;
   }
}
