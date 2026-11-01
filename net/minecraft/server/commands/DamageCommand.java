package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

public class DamageCommand {
   private static final SimpleCommandExceptionType ERROR_INVULNERABLE = new SimpleCommandExceptionType(Component.translatable("commands.damage.invulnerable"));

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("damage").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("target", EntityArgument.entity())
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("amount", FloatArgumentType.floatArg(0.0F))
                           .executes(
                              commandContext -> damage(
                                 (CommandSourceStack)commandContext.getSource(),
                                 EntityArgument.getEntity(commandContext, "target"),
                                 FloatArgumentType.getFloat(commandContext, "amount"),
                                 ((CommandSourceStack)commandContext.getSource()).getLevel().damageSources().generic()
                              )
                           ))
                        .then(
                           ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                       "damageType", ResourceArgument.resource(context, Registries.DAMAGE_TYPE)
                                    )
                                    .executes(
                                       commandContext -> damage(
                                          (CommandSourceStack)commandContext.getSource(),
                                          EntityArgument.getEntity(commandContext, "target"),
                                          FloatArgumentType.getFloat(commandContext, "amount"),
                                          new DamageSource(ResourceArgument.getResource(commandContext, "damageType", Registries.DAMAGE_TYPE))
                                       )
                                    ))
                                 .then(
                                    Commands.literal("at")
                                       .then(
                                          Commands.argument("location", Vec3Argument.vec3())
                                             .executes(
                                                commandContext -> damage(
                                                   (CommandSourceStack)commandContext.getSource(),
                                                   EntityArgument.getEntity(commandContext, "target"),
                                                   FloatArgumentType.getFloat(commandContext, "amount"),
                                                   new DamageSource(
                                                      ResourceArgument.getResource(commandContext, "damageType", Registries.DAMAGE_TYPE),
                                                      Vec3Argument.getVec3(commandContext, "location")
                                                   )
                                                )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("by")
                                    .then(
                                       ((RequiredArgumentBuilder)Commands.argument("entity", EntityArgument.entity())
                                             .executes(
                                                commandContext -> damage(
                                                   (CommandSourceStack)commandContext.getSource(),
                                                   EntityArgument.getEntity(commandContext, "target"),
                                                   FloatArgumentType.getFloat(commandContext, "amount"),
                                                   new DamageSource(
                                                      ResourceArgument.getResource(commandContext, "damageType", Registries.DAMAGE_TYPE),
                                                      EntityArgument.getEntity(commandContext, "entity")
                                                   )
                                                )
                                             ))
                                          .then(
                                             Commands.literal("from")
                                                .then(
                                                   Commands.argument("cause", EntityArgument.entity())
                                                      .executes(
                                                         commandContext -> damage(
                                                            (CommandSourceStack)commandContext.getSource(),
                                                            EntityArgument.getEntity(commandContext, "target"),
                                                            FloatArgumentType.getFloat(commandContext, "amount"),
                                                            new DamageSource(
                                                               ResourceArgument.getResource(commandContext, "damageType", Registries.DAMAGE_TYPE),
                                                               EntityArgument.getEntity(commandContext, "entity"),
                                                               EntityArgument.getEntity(commandContext, "cause")
                                                            )
                                                         )
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

   private static int damage(CommandSourceStack source, Entity target, float amount, DamageSource damageType) throws CommandSyntaxException {
      if (target.hurtServer(source.getLevel(), damageType, amount)) {
         source.sendSuccess(() -> Component.translatable("commands.damage.success", amount, target.getDisplayName()), true);
         return 1;
      } else {
         throw ERROR_INVULNERABLE.create();
      }
   }
}
