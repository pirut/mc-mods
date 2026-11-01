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
   public static void register(CommandDispatcher<CommandSourceStack> p_138644_) {
      p_138644_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("spawnpoint")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .executes(
                  p_450787_ -> setSpawn(
                     (CommandSourceStack)p_450787_.getSource(),
                     Collections.singleton(((CommandSourceStack)p_450787_.getSource()).getPlayerOrException()),
                     BlockPos.containing(((CommandSourceStack)p_450787_.getSource()).getPosition()),
                     WorldCoordinates.ZERO_ROTATION
                  )
               ))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players())
                     .executes(
                        p_450786_ -> setSpawn(
                           (CommandSourceStack)p_450786_.getSource(),
                           EntityArgument.getPlayers(p_450786_, "targets"),
                           BlockPos.containing(((CommandSourceStack)p_450786_.getSource()).getPosition()),
                           WorldCoordinates.ZERO_ROTATION
                        )
                     ))
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("pos", BlockPosArgument.blockPos())
                           .executes(
                              p_450785_ -> setSpawn(
                                 (CommandSourceStack)p_450785_.getSource(),
                                 EntityArgument.getPlayers(p_450785_, "targets"),
                                 BlockPosArgument.getSpawnablePos(p_450785_, "pos"),
                                 WorldCoordinates.ZERO_ROTATION
                              )
                           ))
                        .then(
                           Commands.argument("rotation", RotationArgument.rotation())
                              .executes(
                                 p_450793_ -> setSpawn(
                                    (CommandSourceStack)p_450793_.getSource(),
                                    EntityArgument.getPlayers(p_450793_, "targets"),
                                    BlockPosArgument.getSpawnablePos(p_450793_, "pos"),
                                    RotationArgument.getRotation(p_450793_, "rotation")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int setSpawn(CommandSourceStack p_138650_, Collection<ServerPlayer> p_138651_, BlockPos p_138652_, Coordinates p_451006_) {
      ResourceKey<Level> resourcekey = p_138650_.getLevel().dimension();
      Vec2 vec2 = p_451006_.getRotation(p_138650_);
      float f = Mth.wrapDegrees(vec2.y);
      float f1 = Mth.clamp(vec2.x, -90.0F, 90.0F);

      for (ServerPlayer serverplayer : p_138651_) {
         serverplayer.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(resourcekey, p_138652_, f, f1), true), false);
      }

      String s = resourcekey.identifier().toString();
      if (p_138651_.size() == 1) {
         p_138650_.sendSuccess(
            () -> Component.translatable(
               "commands.spawnpoint.success.single",
               p_138652_.getX(),
               p_138652_.getY(),
               p_138652_.getZ(),
               f,
               f1,
               p_138650_.getLevel().getDescription(),
               p_138651_.iterator().next().getDisplayName()
            ),
            true
         );
      } else {
         p_138650_.sendSuccess(
            () -> Component.translatable(
               "commands.spawnpoint.success.multiple", p_138652_.getX(), p_138652_.getY(), p_138652_.getZ(), f, f1, s, p_138651_.size()
            ),
            true
         );
      }

      return p_138651_.size();
   }
}
