package net.neoforged.neoforge.event.entity.living;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.ICancellableEvent;

public class LivingDestroyBlockEvent extends LivingEvent implements ICancellableEvent {
   private final BlockPos pos;
   private final BlockState state;

   public LivingDestroyBlockEvent(LivingEntity entity, BlockPos pos, BlockState state) {
      super(entity);
      this.pos = pos;
      this.state = state;
   }

   public BlockState getState() {
      return this.state;
   }

   public BlockPos getPos() {
      return this.pos;
   }
}
