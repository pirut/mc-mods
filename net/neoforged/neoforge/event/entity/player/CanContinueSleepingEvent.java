package net.neoforged.neoforge.event.entity.player;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import org.jspecify.annotations.Nullable;

public class CanContinueSleepingEvent extends LivingEvent {
   @Nullable
   protected final Player.BedSleepingProblem problem;
   protected boolean mayContinueSleeping;

   public CanContinueSleepingEvent(LivingEntity entity, @Nullable Player.BedSleepingProblem problem) {
      super(entity);
      this.problem = problem;
      this.mayContinueSleeping = problem == null;
   }

   Optional<BlockPos> getSleepingPos() {
      return this.getEntity().getSleepingPos();
   }

   @Nullable
   public Player.BedSleepingProblem getProblem() {
      return this.problem;
   }

   public boolean mayContinueSleeping() {
      return this.mayContinueSleeping;
   }

   public void setContinueSleeping(boolean sleeping) {
      this.mayContinueSleeping = sleeping;
   }
}
