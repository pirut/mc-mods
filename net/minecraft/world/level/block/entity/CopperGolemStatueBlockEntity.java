package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class CopperGolemStatueBlockEntity extends BlockEntity {
   public CopperGolemStatueBlockEntity(BlockPos pos, BlockState state) {
      super(BlockEntityType.COPPER_GOLEM_STATUE, pos, state);
   }

   public void createStatue(CopperGolem golem) {
      this.setComponents(DataComponentMap.builder().addAll(this.components()).set(DataComponents.CUSTOM_NAME, golem.getCustomName()).build());
      super.setChanged();
   }

   @Nullable
   public CopperGolem removeStatue(BlockState state) {
      CopperGolem copperGolem = EntityType.COPPER_GOLEM.create(this.level, EntitySpawnReason.TRIGGERED);
      if (copperGolem != null) {
         copperGolem.setCustomName(this.components().get(DataComponents.CUSTOM_NAME));
         return this.initCopperGolem(state, copperGolem);
      } else {
         return null;
      }
   }

   private CopperGolem initCopperGolem(BlockState state, CopperGolem golem) {
      BlockPos blockPos = this.getBlockPos();
      golem.snapTo(blockPos.getCenter().x, blockPos.getY(), blockPos.getCenter().z, state.getValue(CopperGolemStatueBlock.FACING).toYRot(), 0.0F);
      golem.yHeadRot = golem.getYRot();
      golem.yBodyRot = golem.getYRot();
      golem.playSpawnSound();
      return golem;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public ItemStack getItem(ItemStack stack, CopperGolemStatueBlock.Pose pose) {
      stack.applyComponents(this.collectComponents());
      stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(CopperGolemStatueBlock.POSE, pose));
      return stack;
   }
}
