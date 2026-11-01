package net.neoforged.neoforge.event.entity.player;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jspecify.annotations.Nullable;

public class BonemealEvent extends Event implements ICancellableEvent {
   @Nullable
   private final Player player;
   private final Level level;
   private final BlockPos pos;
   private final BlockState state;
   private final ItemStack stack;
   private final boolean isValidBonemealTarget;
   private boolean isSuccess = false;

   public BonemealEvent(@Nullable Player player, Level level, BlockPos pos, BlockState state, ItemStack stack) {
      this.player = player;
      this.level = level;
      this.pos = pos;
      this.state = state;
      this.stack = stack;
      this.isValidBonemealTarget = state.getBlock() instanceof BonemealableBlock bonemealable && bonemealable.isValidBonemealTarget(level, pos, state);
   }

   @Nullable
   public Player getPlayer() {
      return this.player;
   }

   public Level getLevel() {
      return this.level;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public BlockState getState() {
      return this.state;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public boolean isValidBonemealTarget() {
      return this.isValidBonemealTarget;
   }

   public void setSuccessful(boolean success) {
      this.isSuccess = success;
      this.setCanceled(true);
   }

   public boolean isSuccessful() {
      return this.isSuccess;
   }

   public void setCanceled(boolean canceled) {
      super.setCanceled(canceled);
   }
}
