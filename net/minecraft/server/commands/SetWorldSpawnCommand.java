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
   public static void register(CommandDispatcher<CommandSourceStack> p_138661_) {
      p_138661_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("setworldspawn")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .executes(
                  p_450800_ -> setSpawn(
                     (CommandSourceStack)p_450800_.getSource(),
                     BlockPos.containing(((CommandSourceStack)p_450800_.getSource()).getPosition()),
                     WorldCoordinates.ZERO_ROTATION
                  )
               ))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("pos", BlockPosArgument.blockPos())
                     .executes(
                        p_450801_ -> setSpawn(
                           (CommandSourceStack)p_450801_.getSource(), BlockPosArgument.getSpawnablePos(p_450801_, "pos"), WorldCoordinates.ZERO_ROTATION
                        )
                     ))
                  .then(
                     Commands.argument("rotation", RotationArgument.rotation())
                        .executes(
                           p_450799_ -> setSpawn(
                              (CommandSourceStack)p_450799_.getSource(),
                              BlockPosArgument.getSpawnablePos(p_450799_, "pos"),
                              RotationArgument.getRotation(p_450799_, "rotation")
                           )
                        )
                  )
            )
      );
   }

   private static int setSpawn(CommandSourceStack p_138667_, BlockPos p_138668_, Coordinates p_450977_) {
      ServerLevel serverlevel = p_138667_.getLevel();
      Vec2 vec2 = p_450977_.getRotation(p_138667_);
      float f = vec2.y;
      float f1 = vec2.x;
      LevelData.RespawnData leveldata$respawndata = LevelData.RespawnData.of(serverlevel.dimension(), p_138668_, f, f1);
      serverlevel.setRespawnData(leveldata$respawndata);
      p_138667_.sendSuccess(
         () -> Component.translatable(
            "commands.setworldspawn.success",
            p_138668_.getX(),
            p_138668_.getY(),
            p_138668_.getZ(),
            leveldata$respawndata.yaw(),
            leveldata$respawndata.pitch(),
            serverlevel.dimension().identifier().toString()
         ),
         true
      );
      return 1;
   }
}
