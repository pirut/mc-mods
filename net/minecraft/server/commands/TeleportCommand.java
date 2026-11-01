package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class TeleportCommand {
   private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(
      Component.translatable("commands.teleport.invalidPosition")
   );

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      LiteralCommandNode<CommandSourceStack> literalCommandNode = dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("teleport")
                     .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                  .then(
                     Commands.argument("location", Vec3Argument.vec3())
                        .executes(
                           commandContext -> teleportToPos(
                              (CommandSourceStack)commandContext.getSource(),
                              Collections.singleton(((CommandSourceStack)commandContext.getSource()).getEntityOrException()),
                              ((CommandSourceStack)commandContext.getSource()).getLevel(),
                              Vec3Argument.getCoordinates(commandContext, "location"),
                              null,
                              null
                           )
                        )
                  ))
               .then(
                  Commands.argument("destination", EntityArgument.entity())
                     .executes(
                        commandContext -> teleportToEntity(
                           (CommandSourceStack)commandContext.getSource(),
                           Collections.singleton(((CommandSourceStack)commandContext.getSource()).getEntityOrException()),
                           EntityArgument.getEntity(commandContext, "destination")
                        )
                     )
               ))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.entities())
                     .then(
                        ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("location", Vec3Argument.vec3())
                                 .executes(
                                    commandContext -> teleportToPos(
                                       (CommandSourceStack)commandContext.getSource(),
                                       EntityArgument.getEntities(commandContext, "targets"),
                                       ((CommandSourceStack)commandContext.getSource()).getLevel(),
                                       Vec3Argument.getCoordinates(commandContext, "location"),
                                       null,
                                       null
                                    )
                                 ))
                              .then(
                                 Commands.argument("rotation", RotationArgument.rotation())
                                    .executes(
                                       commandContext -> teleportToPos(
                                          (CommandSourceStack)commandContext.getSource(),
                                          EntityArgument.getEntities(commandContext, "targets"),
                                          ((CommandSourceStack)commandContext.getSource()).getLevel(),
                                          Vec3Argument.getCoordinates(commandContext, "location"),
                                          RotationArgument.getRotation(commandContext, "rotation"),
                                          null
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
                                                      commandContext -> teleportToPos(
                                                         (CommandSourceStack)commandContext.getSource(),
                                                         EntityArgument.getEntities(commandContext, "targets"),
                                                         ((CommandSourceStack)commandContext.getSource()).getLevel(),
                                                         Vec3Argument.getCoordinates(commandContext, "location"),
                                                         null,
                                                         new LookAt.LookAtEntity(
                                                            EntityArgument.getEntity(commandContext, "facingEntity"), EntityAnchorArgument.Anchor.FEET
                                                         )
                                                      )
                                                   ))
                                                .then(
                                                   Commands.argument("facingAnchor", EntityAnchorArgument.anchor())
                                                      .executes(
                                                         commandContext -> teleportToPos(
                                                            (CommandSourceStack)commandContext.getSource(),
                                                            EntityArgument.getEntities(commandContext, "targets"),
                                                            ((CommandSourceStack)commandContext.getSource()).getLevel(),
                                                            Vec3Argument.getCoordinates(commandContext, "location"),
                                                            null,
                                                            new LookAt.LookAtEntity(
                                                               EntityArgument.getEntity(commandContext, "facingEntity"),
                                                               EntityAnchorArgument.getAnchor(commandContext, "facingAnchor")
                                                            )
                                                         )
                                                      )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.argument("facingLocation", Vec3Argument.vec3())
                                       .executes(
                                          commandContext -> teleportToPos(
                                             (CommandSourceStack)commandContext.getSource(),
                                             EntityArgument.getEntities(commandContext, "targets"),
                                             ((CommandSourceStack)commandContext.getSource()).getLevel(),
                                             Vec3Argument.getCoordinates(commandContext, "location"),
                                             null,
                                             new LookAt.LookAtPosition(Vec3Argument.getVec3(commandContext, "facingLocation"))
                                          )
                                       )
                                 )
                           )
                     ))
                  .then(
                     Commands.argument("destination", EntityArgument.entity())
                        .executes(
                           commandContext -> teleportToEntity(
                              (CommandSourceStack)commandContext.getSource(),
                              EntityArgument.getEntities(commandContext, "targets"),
                              EntityArgument.getEntity(commandContext, "destination")
                           )
                        )
                  )
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("tp").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .redirect(literalCommandNode)
      );
   }

   private static int teleportToEntity(CommandSourceStack source, Collection<? extends Entity> targets, Entity destination) throws CommandSyntaxException {
      for (Entity entity : targets) {
         performTeleport(
            source,
            entity,
            (ServerLevel)destination.level(),
            destination.getX(),
            destination.getY(),
            destination.getZ(),
            EnumSet.noneOf(Relative.class),
            destination.getYRot(),
            destination.getXRot(),
            null
         );
      }

      if (targets.size() == 1) {
         source.sendSuccess(
            () -> Component.translatable("commands.teleport.success.entity.single", targets.iterator().next().getDisplayName(), destination.getDisplayName()),
            true
         );
      } else {
         source.sendSuccess(() -> Component.translatable("commands.teleport.success.entity.multiple", targets.size(), destination.getDisplayName()), true);
      }

      return targets.size();
   }

   private static int teleportToPos(
      CommandSourceStack source,
      Collection<? extends Entity> targets,
      ServerLevel level,
      Coordinates position,
      @Nullable Coordinates rotation,
      @Nullable LookAt lookAt
   ) throws CommandSyntaxException {
      Vec3 vec3 = position.getPosition(source);
      Vec2 vec2 = rotation == null ? null : rotation.getRotation(source);

      for (Entity entity : targets) {
         Set<Relative> set = getRelatives(position, rotation, entity.level().dimension() == level.dimension());
         if (vec2 == null) {
            performTeleport(source, entity, level, vec3.x, vec3.y, vec3.z, set, entity.getYRot(), entity.getXRot(), lookAt);
         } else {
            performTeleport(source, entity, level, vec3.x, vec3.y, vec3.z, set, vec2.y, vec2.x, lookAt);
         }
      }

      if (targets.size() == 1) {
         source.sendSuccess(
            () -> Component.translatable(
               "commands.teleport.success.location.single",
               targets.iterator().next().getDisplayName(),
               formatDouble(vec3.x),
               formatDouble(vec3.y),
               formatDouble(vec3.z)
            ),
            true
         );
      } else {
         source.sendSuccess(
            () -> Component.translatable(
               "commands.teleport.success.location.multiple", targets.size(), formatDouble(vec3.x), formatDouble(vec3.y), formatDouble(vec3.z)
            ),
            true
         );
      }

      return targets.size();
   }

   private static Set<Relative> getRelatives(Coordinates position, @Nullable Coordinates rotation, boolean absolute) {
      Set<Relative> set = Relative.direction(position.isXRelative(), position.isYRelative(), position.isZRelative());
      Set<Relative> set2 = absolute ? Relative.position(position.isXRelative(), position.isYRelative(), position.isZRelative()) : Set.of();
      Set<Relative> set3 = rotation == null ? Relative.ROTATION : Relative.rotation(rotation.isYRelative(), rotation.isXRelative());
      return Relative.union(set, set2, set3);
   }

   private static String formatDouble(double value) {
      return String.format(Locale.ROOT, "%f", value);
   }

   private static void performTeleport(
      CommandSourceStack source,
      Entity target,
      ServerLevel level,
      double x,
      double y,
      double z,
      Set<Relative> relatives,
      float yRot,
      float xRot,
      @Nullable LookAt lookAt
   ) throws CommandSyntaxException {
      BlockPos blockPos = BlockPos.containing(x, y, z);
      if (!Level.isInSpawnableBounds(blockPos)) {
         throw INVALID_POSITION.create();
      } else {
         double d = relatives.contains(Relative.X) ? x - target.getX() : x;
         double e = relatives.contains(Relative.Y) ? y - target.getY() : y;
         double f = relatives.contains(Relative.Z) ? z - target.getZ() : z;
         float g = relatives.contains(Relative.Y_ROT) ? yRot - target.getYRot() : yRot;
         float h = relatives.contains(Relative.X_ROT) ? xRot - target.getXRot() : xRot;
         float i = Mth.wrapDegrees(g);
         float j = Mth.wrapDegrees(h);
         if (target.teleportTo(level, d, e, f, relatives, i, j, true)) {
            if (lookAt != null) {
               lookAt.perform(source, target);
            }

            if (!(target instanceof LivingEntity livingEntity && livingEntity.isFallFlying())) {
               target.setDeltaMovement(target.getDeltaMovement().multiply(1.0, 0.0, 1.0));
               target.setOnGround(true);
            }

            if (target instanceof PathfinderMob pathfinderMob) {
               pathfinderMob.getNavigation().stop();
            }
         }
      }
   }
}
