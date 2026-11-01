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
   public static void register(CommandDispatcher<CommandSourceStack> p_138795_) {
      RequiredArgumentBuilder<CommandSourceStack, EntitySelector> requiredargumentbuilder = (RequiredArgumentBuilder<CommandSourceStack, EntitySelector>)((RequiredArgumentBuilder)Commands.argument(
               "targets", EntityArgument.players()
            )
            .executes(p_466298_ -> stopSound((CommandSourceStack)p_466298_.getSource(), EntityArgument.getPlayers(p_466298_, "targets"), null, null)))
         .then(
            Commands.literal("*")
               .then(
                  Commands.argument("sound", IdentifierArgument.id())
                     .suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS))
                     .executes(
                        p_466297_ -> stopSound(
                           (CommandSourceStack)p_466297_.getSource(),
                           EntityArgument.getPlayers(p_466297_, "targets"),
                           null,
                           IdentifierArgument.getId(p_466297_, "sound")
                        )
                     )
               )
         );

      for (SoundSource soundsource : SoundSource.values()) {
         requiredargumentbuilder.then(
            ((LiteralArgumentBuilder)Commands.literal(soundsource.getName())
                  .executes(
                     p_466305_ -> stopSound((CommandSourceStack)p_466305_.getSource(), EntityArgument.getPlayers(p_466305_, "targets"), soundsource, null)
                  ))
               .then(
                  Commands.argument("sound", IdentifierArgument.id())
                     .suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS))
                     .executes(
                        p_466303_ -> stopSound(
                           (CommandSourceStack)p_466303_.getSource(),
                           EntityArgument.getPlayers(p_466303_, "targets"),
                           soundsource,
                           IdentifierArgument.getId(p_466303_, "sound")
                        )
                     )
               )
         );
      }

      p_138795_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("stopsound").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(requiredargumentbuilder)
      );
   }

   private static int stopSound(
      CommandSourceStack p_138801_, Collection<ServerPlayer> p_138802_, @Nullable SoundSource p_138803_, @Nullable Identifier p_468676_
   ) {
      ClientboundStopSoundPacket clientboundstopsoundpacket = new ClientboundStopSoundPacket(p_468676_, p_138803_);

      for (ServerPlayer serverplayer : p_138802_) {
         serverplayer.connection.send(clientboundstopsoundpacket);
      }

      if (p_138803_ != null) {
         if (p_468676_ != null) {
            p_138801_.sendSuccess(
               () -> Component.translatable("commands.stopsound.success.source.sound", Component.translationArg(p_468676_), p_138803_.getName()), true
            );
         } else {
            p_138801_.sendSuccess(() -> Component.translatable("commands.stopsound.success.source.any", p_138803_.getName()), true);
         }
      } else if (p_468676_ != null) {
         p_138801_.sendSuccess(() -> Component.translatable("commands.stopsound.success.sourceless.sound", Component.translationArg(p_468676_)), true);
      } else {
         p_138801_.sendSuccess(() -> Component.translatable("commands.stopsound.success.sourceless.any"), true);
      }

      return p_138802_.size();
   }
}
