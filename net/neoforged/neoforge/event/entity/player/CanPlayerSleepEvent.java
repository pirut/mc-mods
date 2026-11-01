package net.neoforged.neoforge.event.entity.player;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class CanPlayerSleepEvent extends PlayerEvent {
   private final BlockPos pos;
   private final BlockState state;
   @Nullable
   private final Player.BedSleepingProblem vanillaProblem;
   @Nullable
   private Player.BedSleepingProblem problem;

   public CanPlayerSleepEvent(ServerPlayer player, BlockPos pos, @Nullable Player.BedSleepingProblem problem) {
      super(player);
      this.pos = pos;
      this.state = player.level().getBlockState(pos);
      this.vanillaProblem = this.problem = problem;
   }

   public ServerPlayer getEntity() {
      return (ServerPlayer)super.getEntity();
   }

   public Level getLevel() {
      return this.getEntity().level();
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public BlockState getState() {
      return this.state;
   }

   @Nullable
   public Player.BedSleepingProblem getProblem() {
      return this.problem;
   }

   public void setProblem(@Nullable Player.BedSleepingProblem problem) {
      this.problem = problem;
   }

   @Nullable
   public Player.BedSleepingProblem getVanillaProblem() {
      return this.vanillaProblem;
   }
}
