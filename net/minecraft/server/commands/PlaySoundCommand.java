package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PlaySoundCommand {
   private static final SimpleCommandExceptionType ERROR_TOO_FAR = new SimpleCommandExceptionType(Component.translatable("commands.playsound.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      RequiredArgumentBuilder<CommandSourceStack, Identifier> requiredArgumentBuilder = (RequiredArgumentBuilder<CommandSourceStack, Identifier>)Commands.argument(
            "sound", IdentifierArgument.id()
         )
         .suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS))
         .executes(
            commandContext -> playSound(
               (CommandSourceStack)commandContext.getSource(),
               getCallingPlayerAsCollection(((CommandSourceStack)commandContext.getSource()).getPlayer()),
               IdentifierArgument.getId(commandContext, "sound"),
               SoundSource.MASTER,
               ((CommandSourceStack)commandContext.getSource()).getPosition(),
               1.0F,
               1.0F,
               0.0F
            )
         );

      for (SoundSource soundSource : SoundSource.values()) {
         requiredArgumentBuilder.then(source(soundSource));
      }

      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("playsound").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(requiredArgumentBuilder)
      );
   }

   private static LiteralArgumentBuilder<CommandSourceStack> source(SoundSource category) {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Commands.literal(category.getName())
            .executes(
               commandContext -> playSound(
                  (CommandSourceStack)commandContext.getSource(),
                  getCallingPlayerAsCollection(((CommandSourceStack)commandContext.getSource()).getPlayer()),
                  IdentifierArgument.getId(commandContext, "sound"),
                  category,
                  ((CommandSourceStack)commandContext.getSource()).getPosition(),
                  1.0F,
                  1.0F,
                  0.0F
               )
            ))
         .then(
            ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players())
                  .executes(
                     commandContext -> playSound(
                        (CommandSourceStack)commandContext.getSource(),
                        EntityArgument.getPlayers(commandContext, "targets"),
                        IdentifierArgument.getId(commandContext, "sound"),
                        category,
                        ((CommandSourceStack)commandContext.getSource()).getPosition(),
                        1.0F,
                        1.0F,
                        0.0F
                     )
                  ))
               .then(
                  ((RequiredArgumentBuilder)Commands.argument("pos", Vec3Argument.vec3())
                        .executes(
                           commandContext -> playSound(
                              (CommandSourceStack)commandContext.getSource(),
                              EntityArgument.getPlayers(commandContext, "targets"),
                              IdentifierArgument.getId(commandContext, "sound"),
                              category,
                              Vec3Argument.getVec3(commandContext, "pos"),
                              1.0F,
                              1.0F,
                              0.0F
                           )
                        ))
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("volume", FloatArgumentType.floatArg(0.0F))
                              .executes(
                                 commandContext -> playSound(
                                    (CommandSourceStack)commandContext.getSource(),
                                    EntityArgument.getPlayers(commandContext, "targets"),
                                    IdentifierArgument.getId(commandContext, "sound"),
                                    category,
                                    Vec3Argument.getVec3(commandContext, "pos"),
                                    (Float)commandContext.getArgument("volume", Float.class),
                                    1.0F,
                                    0.0F
                                 )
                              ))
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("pitch", FloatArgumentType.floatArg(0.0F, 2.0F))
                                    .executes(
                                       commandContext -> playSound(
                                          (CommandSourceStack)commandContext.getSource(),
                                          EntityArgument.getPlayers(commandContext, "targets"),
                                          IdentifierArgument.getId(commandContext, "sound"),
                                          category,
                                          Vec3Argument.getVec3(commandContext, "pos"),
                                          (Float)commandContext.getArgument("volume", Float.class),
                                          (Float)commandContext.getArgument("pitch", Float.class),
                                          0.0F
                                       )
                                    ))
                                 .then(
                                    Commands.argument("minVolume", FloatArgumentType.floatArg(0.0F, 1.0F))
                                       .executes(
                                          commandContext -> playSound(
                                             (CommandSourceStack)commandContext.getSource(),
                                             EntityArgument.getPlayers(commandContext, "targets"),
                                             IdentifierArgument.getId(commandContext, "sound"),
                                             category,
                                             Vec3Argument.getVec3(commandContext, "pos"),
                                             (Float)commandContext.getArgument("volume", Float.class),
                                             (Float)commandContext.getArgument("pitch", Float.class),
                                             (Float)commandContext.getArgument("minVolume", Float.class)
                                          )
                                       )
                                 )
                           )
                     )
               )
         );
   }

   private static Collection<ServerPlayer> getCallingPlayerAsCollection(@Nullable ServerPlayer player) {
      return player != null ? List.of(player) : List.of();
   }

   private static int playSound(
      CommandSourceStack source, Collection<ServerPlayer> targets, Identifier sound, SoundSource category, Vec3 pos, float volume, float pitch, float minVolume
   ) throws CommandSyntaxException {
      Holder<SoundEvent> holder = Holder.direct(SoundEvent.createVariableRangeEvent(sound));
      double d = Mth.square(holder.value().getRange(volume));
      ServerLevel serverLevel = source.getLevel();
      long l = serverLevel.getRandom().nextLong();
      List<ServerPlayer> list = new ArrayList<>();

      for (ServerPlayer serverPlayer : targets) {
         if (serverPlayer.level() == serverLevel) {
            double e = pos.x - serverPlayer.getX();
            double f = pos.y - serverPlayer.getY();
            double g = pos.z - serverPlayer.getZ();
            double h = e * e + f * f + g * g;
            Vec3 vec3 = pos;
            float i = volume;
            if (h > d) {
               if (minVolume <= 0.0F) {
                  continue;
               }

               double j = Math.sqrt(h);
               vec3 = new Vec3(serverPlayer.getX() + e / j * 2.0, serverPlayer.getY() + f / j * 2.0, serverPlayer.getZ() + g / j * 2.0);
               i = minVolume;
            }

            serverPlayer.connection.send(new ClientboundSoundPacket(holder, category, vec3.x(), vec3.y(), vec3.z(), i, pitch, l));
            list.add(serverPlayer);
         }
      }

      int k = list.size();
      if (k == 0) {
         throw ERROR_TOO_FAR.create();
      } else {
         if (k == 1) {
            source.sendSuccess(
               () -> Component.translatable("commands.playsound.success.single", Component.translationArg(sound), list.getFirst().getDisplayName()), true
            );
         } else {
            source.sendSuccess(() -> Component.translatable("commands.playsound.success.multiple", Component.translationArg(sound), k), true);
         }

         return k;
      }
   }
}
