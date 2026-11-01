package net.neoforged.neoforge.event.level;

import com.google.common.base.Preconditions;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.ICancellableEvent;
import org.jspecify.annotations.Nullable;

public class BlockDropsEvent extends BlockEvent implements ICancellableEvent {
   @Nullable
   private final BlockEntity blockEntity;
   private final List<ItemEntity> drops;
   @Nullable
   private final Entity breaker;
   private final ItemStack tool;
   private int experience;

   public BlockDropsEvent(
      ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, List<ItemEntity> drops, @Nullable Entity breaker, ItemStack tool
   ) {
      super(level, pos, state);
      this.blockEntity = blockEntity;
      this.drops = drops;
      this.breaker = breaker;
      this.tool = tool;
      this.experience = EnchantmentHelper.processBlockExperience(level, tool, state.getExpDrop(level, pos, blockEntity, breaker, tool));
   }

   public List<ItemEntity> getDrops() {
      return this.drops;
   }

   @Nullable
   public BlockEntity getBlockEntity() {
      return this.blockEntity;
   }

   @Nullable
   public Entity getBreaker() {
      return this.breaker;
   }

   public ItemStack getTool() {
      return this.tool;
   }

   public void setCanceled(boolean canceled) {
      super.setCanceled(canceled);
   }

   public ServerLevel getLevel() {
      return (ServerLevel)super.getLevel();
   }

   public int getDroppedExperience() {
      return this.experience;
   }

   public void setDroppedExperience(int experience) {
      Preconditions.checkArgument(experience >= 0, "May not set a negative experience drop.");
      this.experience = experience;
   }
}
