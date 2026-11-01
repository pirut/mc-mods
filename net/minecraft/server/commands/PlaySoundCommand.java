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

   public static void register(CommandDispatcher<CommandSourceStack> p_138157_) {
      RequiredArgumentBuilder<CommandSourceStack, Identifier> requiredargumentbuilder = (RequiredArgumentBuilder<CommandSourceStack, Identifier>)Commands.argument(
            "sound", IdentifierArgument.id()
         )
         .suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS))
         .executes(
            p_466272_ -> playSound(
               (CommandSourceStack)p_466272_.getSource(),
               getCallingPlayerAsCollection(((CommandSourceStack)p_466272_.getSource()).getPlayer()),
               IdentifierArgument.getId(p_466272_, "sound"),
               SoundSource.MASTER,
               ((CommandSourceStack)p_466272_.getSource()).getPosition(),
               1.0F,
               1.0F,
               0.0F
            )
         );

      for (SoundSource soundsource : SoundSource.values()) {
         requiredargumentbuilder.then(source(soundsource));
      }

      p_138157_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("playsound").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(requiredargumentbuilder)
      );
   }

   private static LiteralArgumentBuilder<CommandSourceStack> source(SoundSource p_138152_) {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Commands.literal(p_138152_.getName())
            .executes(
               p_466261_ -> playSound(
                  (CommandSourceStack)p_466261_.getSource(),
                  getCallingPlayerAsCollection(((CommandSourceStack)p_466261_.getSource()).getPlayer()),
                  IdentifierArgument.getId(p_466261_, "sound"),
                  p_138152_,
                  ((CommandSourceStack)p_466261_.getSource()).getPosition(),
                  1.0F,
                  1.0F,
                  0.0F
               )
            ))
         .then(
            ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players())
                  .executes(
                     p_466263_ -> playSound(
                        (CommandSourceStack)p_466263_.getSource(),
                        EntityArgument.getPlayers(p_466263_, "targets"),
                        IdentifierArgument.getId(p_466263_, "sound"),
                        p_138152_,
                        ((CommandSourceStack)p_466263_.getSource()).getPosition(),
                        1.0F,
                        1.0F,
                        0.0F
                     )
                  ))
               .then(
                  ((RequiredArgumentBuilder)Commands.argument("pos", Vec3Argument.vec3())
                        .executes(
                           p_466265_ -> playSound(
                              (CommandSourceStack)p_466265_.getSource(),
                              EntityArgument.getPlayers(p_466265_, "targets"),
                              IdentifierArgument.getId(p_466265_, "sound"),
                              p_138152_,
                              Vec3Argument.getVec3(p_466265_, "pos"),
                              1.0F,
                              1.0F,
                              0.0F
                           )
                        ))
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("volume", FloatArgumentType.floatArg(0.0F))
                              .executes(
                                 p_466274_ -> playSound(
                                    (CommandSourceStack)p_466274_.getSource(),
                                    EntityArgument.getPlayers(p_466274_, "targets"),
                                    IdentifierArgument.getId(p_466274_, "sound"),
                                    p_138152_,
                                    Vec3Argument.getVec3(p_466274_, "pos"),
                                    (Float)p_466274_.getArgument("volume", Float.class),
                                    1.0F,
                                    0.0F
                                 )
                              ))
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("pitch", FloatArgumentType.floatArg(0.0F, 2.0F))
                                    .executes(
                                       p_466259_ -> playSound(
                                          (CommandSourceStack)p_466259_.getSource(),
                                          EntityArgument.getPlayers(p_466259_, "targets"),
                                          IdentifierArgument.getId(p_466259_, "sound"),
                                          p_138152_,
                                          Vec3Argument.getVec3(p_466259_, "pos"),
                                          (Float)p_466259_.getArgument("volume", Float.class),
                                          (Float)p_466259_.getArgument("pitch", Float.class),
                                          0.0F
                                       )
                                    ))
                                 .then(
                                    Commands.argument("minVolume", FloatArgumentType.floatArg(0.0F, 1.0F))
                                       .executes(
                                          p_466269_ -> playSound(
                                             (CommandSourceStack)p_466269_.getSource(),
                                             EntityArgument.getPlayers(p_466269_, "targets"),
                                             IdentifierArgument.getId(p_466269_, "sound"),
                                             p_138152_,
                                             Vec3Argument.getVec3(p_466269_, "pos"),
                                             (Float)p_466269_.getArgument("volume", Float.class),
                                             (Float)p_466269_.getArgument("pitch", Float.class),
                                             (Float)p_466269_.getArgument("minVolume", Float.class)
                                          )
                                       )
                                 )
                           )
                     )
               )
         );
   }

   private static Collection<ServerPlayer> getCallingPlayerAsCollection(@Nullable ServerPlayer p_330601_) {
      return p_330601_ != null ? List.of(p_330601_) : List.of();
   }

   private static int playSound(
      CommandSourceStack p_138161_,
      Collection<ServerPlayer> p_138162_,
      Identifier p_469672_,
      SoundSource p_138164_,
      Vec3 p_138165_,
      float p_138166_,
      float p_138167_,
      float p_138168_
   ) throws CommandSyntaxException {
      Holder<SoundEvent> holder = Holder.direct(SoundEvent.createVariableRangeEvent(p_469672_));
      double d0 = Mth.square(holder.value().getRange(p_138166_));
      ServerLevel serverlevel = p_138161_.getLevel();
      long i = serverlevel.getRandom().nextLong();
      List<ServerPlayer> list = new ArrayList<>();

      for (ServerPlayer serverplayer : p_138162_) {
         if (serverplayer.level() == serverlevel) {
            double d1 = p_138165_.x - serverplayer.getX();
            double d2 = p_138165_.y - serverplayer.getY();
            double d3 = p_138165_.z - serverplayer.getZ();
            double d4 = d1 * d1 + d2 * d2 + d3 * d3;
            Vec3 vec3 = p_138165_;
            float f = p_138166_;
            if (d4 > d0) {
               if (p_138168_ <= 0.0F) {
                  continue;
               }

               double d5 = Math.sqrt(d4);
               vec3 = new Vec3(serverplayer.getX() + d1 / d5 * 2.0, serverplayer.getY() + d2 / d5 * 2.0, serverplayer.getZ() + d3 / d5 * 2.0);
               f = p_138168_;
            }

            serverplayer.connection.send(new ClientboundSoundPacket(holder, p_138164_, vec3.x(), vec3.y(), vec3.z(), f, p_138167_, i));
            list.add(serverplayer);
         }
      }

      int j = list.size();
      if (j == 0) {
         throw ERROR_TOO_FAR.create();
      } else {
         if (j == 1) {
            p_138161_.sendSuccess(
               () -> Component.translatable("commands.playsound.success.single", Component.translationArg(p_469672_), list.getFirst().getDisplayName()), true
            );
         } else {
            p_138161_.sendSuccess(() -> Component.translatable("commands.playsound.success.multiple", Component.translationArg(p_469672_), j), true);
         }

         return j;
      }
   }
}
