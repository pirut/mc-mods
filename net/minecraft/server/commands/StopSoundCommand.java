package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

public class StopSoundCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      RequiredArgumentBuilder<CommandSourceStack, EntitySelector> requiredArgumentBuilder = (RequiredArgumentBuilder<CommandSourceStack, EntitySelector>)((RequiredArgumentBuilder)Commands.argument(
               "targets", EntityArgument.players()
            )
            .executes(
               commandContext -> stopSound((CommandSourceStack)commandContext.getSource(), EntityArgument.getPlayers(commandContext, "targets"), null, null)
            ))
         .then(
            Commands.literal("*")
               .then(
                  Commands.argument("sound", IdentifierArgument.id())
                     .suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS))
                     .executes(
                        commandContext -> stopSound(
                           (CommandSourceStack)commandContext.getSource(),
                           EntityArgument.getPlayers(commandContext, "targets"),
                           null,
                           IdentifierArgument.getId(commandContext, "sound")
                        )
                     )
               )
         );

      for (SoundSource soundSource : SoundSource.values()) {
         requiredArgumentBuilder.then(
            ((LiteralArgumentBuilder)Commands.literal(soundSource.getName())
                  .executes(
                     commandContext -> stopSound(
                        (CommandSourceStack)commandContext.getSource(), EntityArgument.getPlayers(commandContext, "targets"), soundSource, null
                     )
                  ))
               .then(
                  Commands.argument("sound", IdentifierArgument.id())
                     .suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS))
                     .executes(
                        commandContext -> stopSound(
                           (CommandSourceStack)commandContext.getSource(),
                           EntityArgument.getPlayers(commandContext, "targets"),
                           soundSource,
                           IdentifierArgument.getId(commandContext, "sound")
                        )
                     )
               )
         );
      }

      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("stopsound").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(requiredArgumentBuilder)
      );
   }

   private static int stopSound(CommandSourceStack source, Collection<ServerPlayer> targets, @Nullable SoundSource category, @Nullable Identifier sound) {
      ClientboundStopSoundPacket clientboundStopSoundPacket = new ClientboundStopSoundPacket(sound, category);

      for (ServerPlayer serverPlayer : targets) {
         serverPlayer.connection.send(clientboundStopSoundPacket);
      }

      if (category != null) {
         if (sound != null) {
            source.sendSuccess(
               () -> Component.translatable("commands.stopsound.success.source.sound", Component.translationArg(sound), category.getName()), true
            );
         } else {
            source.sendSuccess(() -> Component.translatable("commands.stopsound.success.source.any", category.getName()), true);
         }
      } else if (sound != null) {
         source.sendSuccess(() -> Component.translatable("commands.stopsound.success.sourceless.sound", Component.translationArg(sound)), true);
      } else {
         source.sendSuccess(() -> Component.translatable("commands.stopsound.success.sourceless.any"), true);
      }

      return targets.size();
   }
}
