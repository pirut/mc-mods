package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec2;

public class SetWorldSpawnCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("setworldspawn")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .executes(
                  commandContext -> setSpawn(
                     (CommandSourceStack)commandContext.getSource(),
                     BlockPos.containing(((CommandSourceStack)commandContext.getSource()).getPosition()),
                     WorldCoordinates.ZERO_ROTATION
                  )
               ))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("pos", BlockPosArgument.blockPos())
                     .executes(
                        commandContext -> setSpawn(
                           (CommandSourceStack)commandContext.getSource(),
                           BlockPosArgument.getSpawnablePos(commandContext, "pos"),
                           WorldCoordinates.ZERO_ROTATION
                        )
                     ))
                  .then(
                     Commands.argument("rotation", RotationArgument.rotation())
                        .executes(
                           commandContext -> setSpawn(
                              (CommandSourceStack)commandContext.getSource(),
                              BlockPosArgument.getSpawnablePos(commandContext, "pos"),
                              RotationArgument.getRotation(commandContext, "rotation")
                           )
                        )
                  )
            )
      );
   }

   private static int setSpawn(CommandSourceStack source, BlockPos pos, Coordinates rotation) {
      ServerLevel serverLevel = source.getLevel();
      Vec2 vec2 = rotation.getRotation(source);
      float f = vec2.y;
      float g = vec2.x;
      LevelData.RespawnData respawnData = LevelData.RespawnData.of(serverLevel.dimension(), pos, f, g);
      serverLevel.setRespawnData(respawnData);
      source.sendSuccess(
         () -> Component.translatable(
            "commands.setworldspawn.success",
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            respawnData.yaw(),
            respawnData.pitch(),
            serverLevel.dimension().identifier().toString()
         ),
         true
      );
      return 1;
   }
}
