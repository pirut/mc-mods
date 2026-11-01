package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec2;

public class SetSpawnCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("spawnpoint")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .executes(
                  commandContext -> setSpawn(
                     (CommandSourceStack)commandContext.getSource(),
                     Collections.singleton(((CommandSourceStack)commandContext.getSource()).getPlayerOrException()),
                     BlockPos.containing(((CommandSourceStack)commandContext.getSource()).getPosition()),
                     WorldCoordinates.ZERO_ROTATION
                  )
               ))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players())
                     .executes(
                        commandContext -> setSpawn(
                           (CommandSourceStack)commandContext.getSource(),
                           EntityArgument.getPlayers(commandContext, "targets"),
                           BlockPos.containing(((CommandSourceStack)commandContext.getSource()).getPosition()),
                           WorldCoordinates.ZERO_ROTATION
                        )
                     ))
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("pos", BlockPosArgument.blockPos())
                           .executes(
                              commandContext -> setSpawn(
                                 (CommandSourceStack)commandContext.getSource(),
                                 EntityArgument.getPlayers(commandContext, "targets"),
                                 BlockPosArgument.getSpawnablePos(commandContext, "pos"),
                                 WorldCoordinates.ZERO_ROTATION
                              )
                           ))
                        .then(
                           Commands.argument("rotation", RotationArgument.rotation())
                              .executes(
                                 commandContext -> setSpawn(
                                    (CommandSourceStack)commandContext.getSource(),
                                    EntityArgument.getPlayers(commandContext, "targets"),
                                    BlockPosArgument.getSpawnablePos(commandContext, "pos"),
                                    RotationArgument.getRotation(commandContext, "rotation")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int setSpawn(CommandSourceStack source, Collection<ServerPlayer> targets, BlockPos pos, Coordinates rotation) {
      ResourceKey<Level> resourceKey = source.getLevel().dimension();
      Vec2 vec2 = rotation.getRotation(source);
      float f = Mth.wrapDegrees(vec2.y);
      float g = Mth.clamp(vec2.x, -90.0F, 90.0F);

      for (ServerPlayer serverPlayer : targets) {
         serverPlayer.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(resourceKey, pos, f, g), true), false);
      }

      String string = resourceKey.identifier().toString();
      if (targets.size() == 1) {
         source.sendSuccess(
            () -> Component.translatable(
               "commands.spawnpoint.success.single", pos.getX(), pos.getY(), pos.getZ(), f, g, string, targets.iterator().next().getDisplayName()
            ),
            true
         );
      } else {
         source.sendSuccess(
            () -> Component.translatable("commands.spawnpoint.success.multiple", pos.getX(), pos.getY(), pos.getZ(), f, g, string, targets.size()), true
         );
      }

      return targets.size();
   }
}
