package net.minecraft.commands.arguments.blocks;

import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockInput implements Predicate<BlockInWorld> {
   private final BlockState state;
   private final Set<Property<?>> properties;
   @Nullable
   private final CompoundTag tag;

   public BlockInput(BlockState p_114666_, Set<Property<?>> p_114667_, @Nullable CompoundTag p_114668_) {
      this.state = p_114666_;
      this.properties = p_114667_;
      this.tag = p_114668_;
   }

   public BlockState getState() {
      return this.state;
   }

   public Set<Property<?>> getDefinedProperties() {
      return this.properties;
   }

   public boolean test(BlockInWorld p_114675_) {
      BlockState $$1 = p_114675_.getState();
      if (!$$1.is(this.state.getBlock())) {
         return false;
      } else {
         for (Property<?> $$2 : this.properties) {
            if ($$1.getValue($$2) != this.state.getValue($$2)) {
               return false;
            }
         }

         if (this.tag == null) {
            return true;
         } else {
            BlockEntity $$3 = p_114675_.getEntity();
            return $$3 != null && NbtUtils.compareNbt(this.tag, $$3.saveWithFullMetadata(p_114675_.getLevel().registryAccess()), true);
         }
      }
   }

   public boolean test(ServerLevel p_173524_, BlockPos p_173525_) {
      return this.test(new BlockInWorld(p_173524_, p_173525_, false));
   }

   public boolean place(ServerLevel p_114671_, BlockPos p_114672_, int p_114673_) {
      BlockState $$3 = Block.updateFromNeighbourShapes(this.state, p_114671_, p_114672_);
      if ($$3.isAir()) {
         $$3 = this.state;
      }

      if (!p_114671_.setBlock(p_114672_, $$3, p_114673_)) {
         return false;
      } else {
         if (this.tag != null) {
            BlockEntity $$4 = p_114671_.getBlockEntity(p_114672_);
            if ($$4 != null) {
               $$4.loadWithComponents(this.tag, p_114671_.registryAccess());
            }
         }

         return true;
      }
   }
}
