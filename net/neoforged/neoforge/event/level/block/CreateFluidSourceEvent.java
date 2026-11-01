package net.neoforged.neoforge.event.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.event.level.BlockEvent;

public class CreateFluidSourceEvent extends BlockEvent {
   private final boolean vanillaResult;
   private boolean canConvert;

   public CreateFluidSourceEvent(ServerLevel level, BlockPos pos, BlockState state) {
      super(level, pos, state);
      this.vanillaResult = state.getFluidState().canConvertToSource(level, pos);
      this.canConvert = this.vanillaResult;
   }

   public ServerLevel getLevel() {
      return (ServerLevel)super.getLevel();
   }

   public FluidState getFluidState() {
      return this.getState().getFluidState();
   }

   public boolean getVanillaResult() {
      return this.vanillaResult;
   }

   public boolean canConvert() {
      return this.canConvert;
   }

   public void setCanConvert(boolean convert) {
      this.canConvert = convert;
   }
}
