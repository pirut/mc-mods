package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class EffectCommands {
   private static final SimpleCommandExceptionType ERROR_GIVE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.effect.give.failed"));
   private static final SimpleCommandExceptionType ERROR_CLEAR_EVERYTHING_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.effect.clear.everything.failed")
   );
   private static final SimpleCommandExceptionType ERROR_CLEAR_SPECIFIC_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.effect.clear.specific.failed")
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_136954_, CommandBuildContext p_251610_) {
      p_136954_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("effect").requires(p_136958_ -> p_136958_.hasPermission(2)))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("clear")
                        .executes(
                           p_136984_ -> clearEffects(
                              (CommandSourceStack)p_136984_.getSource(), ImmutableList.of(((CommandSourceStack)p_136984_.getSource()).getEntityOrException())
                           )
                        ))
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.entities())
                              .executes(p_136982_ -> clearEffects((CommandSourceStack)p_136982_.getSource(), EntityArgument.getEntities(p_136982_, "targets"))))
                           .then(
                              Commands.argument("effect", ResourceArgument.resource(p_251610_, Registries.MOB_EFFECT))
                                 .executes(
                                    p_248126_ -> clearEffect(
                                       (CommandSourceStack)p_248126_.getSource(),
                                       EntityArgument.getEntities(p_248126_, "targets"),
                                       ResourceArgument.getMobEffect(p_248126_, "effect")
                                    )
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("give")
                  .then(
                     Commands.argument("targets", EntityArgument.entities())
                        .then(
                           ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                       "effect", ResourceArgument.resource(p_251610_, Registries.MOB_EFFECT)
                                    )
                                    .executes(
                                       p_248127_ -> giveEffect(
                                          (CommandSourceStack)p_248127_.getSource(),
                                          EntityArgument.getEntities(p_248127_, "targets"),
                                          ResourceArgument.getMobEffect(p_248127_, "effect"),
                                          null,
                                          0,
                                          true
                                       )
                                    ))
                                 .then(
                                    ((RequiredArgumentBuilder)Commands.argument("seconds", IntegerArgumentType.integer(1, 1000000))
                                          .executes(
                                             p_248124_ -> giveEffect(
                                                (CommandSourceStack)p_248124_.getSource(),
                                                EntityArgument.getEntities(p_248124_, "targets"),
                                                ResourceArgument.getMobEffect(p_248124_, "effect"),
                                                IntegerArgumentType.getInteger(p_248124_, "seconds"),
                                                0,
                                                true
                                             )
                                          ))
                                       .then(
                                          ((RequiredArgumentBuilder)Commands.argument("amplifier", IntegerArgumentType.integer(0, 255))
                                                .executes(
                                                   p_248123_ -> giveEffect(
                                                      (CommandSourceStack)p_248123_.getSource(),
                                                      EntityArgument.getEntities(p_248123_, "targets"),
                                                      ResourceArgument.getMobEffect(p_248123_, "effect"),
                                                      IntegerArgumentType.getInteger(p_248123_, "seconds"),
                                                      IntegerArgumentType.getInteger(p_248123_, "amplifier"),
                                                      true
                                                   )
                                                ))
                                             .then(
                                                Commands.argument("hideParticles", BoolArgumentType.bool())
                                                   .executes(
                                                      p_248125_ -> giveEffect(
                                                         (CommandSourceStack)p_248125_.getSource(),
                                                         EntityArgument.getEntities(p_248125_, "targets"),
                                                         ResourceArgument.getMobEffect(p_248125_, "effect"),
                                                         IntegerArgumentType.getInteger(p_248125_, "seconds"),
                                                         IntegerArgumentType.getInteger(p_248125_, "amplifier"),
                                                         !BoolArgumentType.getBool(p_248125_, "hideParticles")
                                                      )
                                                   )
                                             )
                                       )
                                 ))
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("infinite")
                                       .executes(
                                          p_267907_ -> giveEffect(
                                             (CommandSourceStack)p_267907_.getSource(),
                                             EntityArgument.getEntities(p_267907_, "targets"),
                                             ResourceArgument.getMobEffect(p_267907_, "effect"),
                                             -1,
                                             0,
                                             true
                                          )
                                       ))
                                    .then(
                                       ((RequiredArgumentBuilder)Commands.argument("amplifier", IntegerArgumentType.integer(0, 255))
                                             .executes(
                                                p_267908_ -> giveEffect(
                                                   (CommandSourceStack)p_267908_.getSource(),
                                                   EntityArgument.getEntities(p_267908_, "targets"),
                                                   ResourceArgument.getMobEffect(p_267908_, "effect"),
                                                   -1,
                                                   IntegerArgumentType.getInteger(p_267908_, "amplifier"),
                                                   true
                                                )
                                             ))
                                          .then(
                                             Commands.argument("hideParticles", BoolArgumentType.bool())
                                                .executes(
                                                   p_267909_ -> giveEffect(
                                                      (CommandSourceStack)p_267909_.getSource(),
                                                      EntityArgument.getEntities(p_267909_, "targets"),
                                                      ResourceArgument.getMobEffect(p_267909_, "effect"),
                                                      -1,
                                                      IntegerArgumentType.getInteger(p_267909_, "amplifier"),
                                                      !BoolArgumentType.getBool(p_267909_, "hideParticles")
                                                   )
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int giveEffect(
      CommandSourceStack p_250553_,
      Collection<? extends Entity> p_250411_,
      Holder<MobEffect> p_249495_,
      @Nullable Integer p_249652_,
      int p_251498_,
      boolean p_249944_
   ) throws CommandSyntaxException {
      MobEffect $$6 = p_249495_.value();
      int $$7 = 0;
      int $$8;
      if (p_249652_ != null) {
         if ($$6.isInstantenous()) {
            $$8 = p_249652_;
         } else if (p_249652_ == -1) {
            $$8 = -1;
         } else {
            $$8 = p_249652_ * 20;
         }
      } else if ($$6.isInstantenous()) {
         $$8 = 1;
      } else {
         $$8 = 600;
      }

      for (Entity $$13 : p_250411_) {
         if ($$13 instanceof LivingEntity) {
            MobEffectInstance $$14 = new MobEffectInstance(p_249495_, $$8, p_251498_, false, p_249944_);
            if (((LivingEntity)$$13).addEffect($$14, p_250553_.getEntity())) {
               $$7++;
            }
         }
      }

      if ($$7 == 0) {
         throw ERROR_GIVE_FAILED.create();
      } else {
         if (p_250411_.size() == 1) {
            p_250553_.sendSuccess(
               () -> Component.translatable("commands.effect.give.success.single", $$6.getDisplayName(), p_250411_.iterator().next().getDisplayName(), $$8 / 20),
               true
            );
         } else {
            p_250553_.sendSuccess(() -> Component.translatable("commands.effect.give.success.multiple", $$6.getDisplayName(), p_250411_.size(), $$8 / 20), true);
         }

         return $$7;
      }
   }

   private static int clearEffects(CommandSourceStack p_136960_, Collection<? extends Entity> p_136961_) throws CommandSyntaxException {
      int $$2 = 0;

      for (Entity $$3 : p_136961_) {
         if ($$3 instanceof LivingEntity && ((LivingEntity)$$3).removeAllEffects()) {
            $$2++;
         }
      }

      if ($$2 == 0) {
         throw ERROR_CLEAR_EVERYTHING_FAILED.create();
      } else {
         if (p_136961_.size() == 1) {
            p_136960_.sendSuccess(
               () -> Component.translatable("commands.effect.clear.everything.success.single", p_136961_.iterator().next().getDisplayName()), true
            );
         } else {
            p_136960_.sendSuccess(() -> Component.translatable("commands.effect.clear.everything.success.multiple", p_136961_.size()), true);
         }

         return $$2;
      }
   }

   private static int clearEffect(CommandSourceStack p_250069_, Collection<? extends Entity> p_248561_, Holder<MobEffect> p_249198_) throws CommandSyntaxException {
      MobEffect $$3 = p_249198_.value();
      int $$4 = 0;

      for (Entity $$5 : p_248561_) {
         if ($$5 instanceof LivingEntity && ((LivingEntity)$$5).removeEffect(p_249198_)) {
            $$4++;
         }
      }

      if ($$4 == 0) {
         throw ERROR_CLEAR_SPECIFIC_FAILED.create();
      } else {
         if (p_248561_.size() == 1) {
            p_250069_.sendSuccess(
               () -> Component.translatable("commands.effect.clear.specific.success.single", $$3.getDisplayName(), p_248561_.iterator().next().getDisplayName()),
               true
            );
         } else {
            p_250069_.sendSuccess(() -> Component.translatable("commands.effect.clear.specific.success.multiple", $$3.getDisplayName(), p_248561_.size()), true);
         }

         return $$4;
      }
   }
}
