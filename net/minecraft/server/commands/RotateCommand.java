package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;

public class RotateCommand {
   public static void register(CommandDispatcher<CommandSourceStack> p_379668_) {
      p_379668_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("rotate").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("target", EntityArgument.entity())
                     .then(
                        Commands.argument("rotation", RotationArgument.rotation())
                           .executes(
                              p_379342_ -> rotate(
                                 (CommandSourceStack)p_379342_.getSource(),
                                 EntityArgument.getEntity(p_379342_, "target"),
                                 RotationArgument.getRotation(p_379342_, "rotation")
                              )
                           )
                     ))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("facing")
                           .then(
                              Commands.literal("entity")
                                 .then(
                                    ((RequiredArgumentBuilder)Commands.argument("facingEntity", EntityArgument.entity())
                                          .executes(
                                             p_379526_ -> rotate(
                                                (CommandSourceStack)p_379526_.getSource(),
                                                EntityArgument.getEntity(p_379526_, "target"),
                                                new LookAt.LookAtEntity(EntityArgument.getEntity(p_379526_, "facingEntity"), EntityAnchorArgument.Anchor.FEET)
                                             )
                                          ))
                                       .then(
                                          Commands.argument("facingAnchor", EntityAnchorArgument.anchor())
                                             .executes(
                                                p_380018_ -> rotate(
                                                   (CommandSourceStack)p_380018_.getSource(),
                                                   EntityArgument.getEntity(p_380018_, "target"),
                                                   new LookAt.LookAtEntity(
                                                      EntityArgument.getEntity(p_380018_, "facingEntity"),
                                                      EntityAnchorArgument.getAnchor(p_380018_, "facingAnchor")
                                                   )
                                                )
                                             )
                                       )
                                 )
                           ))
                        .then(
                           Commands.argument("facingLocation", Vec3Argument.vec3())
                              .executes(
                                 p_379579_ -> rotate(
                                    (CommandSourceStack)p_379579_.getSource(),
                                    EntityArgument.getEntity(p_379579_, "target"),
                                    new LookAt.LookAtPosition(Vec3Argument.getVec3(p_379579_, "facingLocation"))
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int rotate(CommandSourceStack p_379593_, Entity p_380003_, Coordinates p_380100_) {
      Vec2 vec2 = p_380100_.getRotation(p_379593_);
      float f = p_380100_.isYRelative() ? vec2.y - p_380003_.getYRot() : vec2.y;
      float f1 = p_380100_.isXRelative() ? vec2.x - p_380003_.getXRot() : vec2.x;
      p_380003_.forceSetRotation(f, p_380100_.isYRelative(), f1, p_380100_.isXRelative());
      p_379593_.sendSuccess(() -> Component.translatable("commands.rotate.success", p_380003_.getDisplayName()), true);
      return 1;
   }

   private static int rotate(CommandSourceStack p_379896_, Entity p_380037_, LookAt p_379419_) {
      p_379419_.perform(p_379896_, p_380037_);
      p_379896_.sendSuccess(() -> Component.translatable("commands.rotate.success", p_380037_.getDisplayName()), true);
      return 1;
   }
}
