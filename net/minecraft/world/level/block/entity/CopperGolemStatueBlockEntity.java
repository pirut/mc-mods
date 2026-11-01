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
   public CopperGolemStatueBlockEntity(BlockPos p_433337_, BlockState p_435540_) {
      super(BlockEntityType.COPPER_GOLEM_STATUE, p_433337_, p_435540_);
   }

   public void createStatue(CopperGolem p_480648_) {
      this.setComponents(DataComponentMap.builder().addAll(this.components()).set(DataComponents.CUSTOM_NAME, p_480648_.getCustomName()).build());
      super.setChanged();
   }

   @Nullable
   public CopperGolem removeStatue(BlockState p_435095_) {
      CopperGolem coppergolem = EntityType.COPPER_GOLEM.create(this.level, EntitySpawnReason.TRIGGERED);
      if (coppergolem != null) {
         coppergolem.setCustomName(this.components().get(DataComponents.CUSTOM_NAME));
         return this.initCopperGolem(p_435095_, coppergolem);
      } else {
         return null;
      }
   }

   private CopperGolem initCopperGolem(BlockState p_434526_, CopperGolem p_481299_) {
      BlockPos blockpos = this.getBlockPos();
      p_481299_.snapTo(blockpos.getCenter().x, blockpos.getY(), blockpos.getCenter().z, p_434526_.getValue(CopperGolemStatueBlock.FACING).toYRot(), 0.0F);
      p_481299_.yHeadRot = p_481299_.getYRot();
      p_481299_.yBodyRot = p_481299_.getYRot();
      p_481299_.playSpawnSound();
      return p_481299_;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public ItemStack getItem(ItemStack p_446299_, CopperGolemStatueBlock.Pose p_446035_) {
      p_446299_.applyComponents(this.collectComponents());
      p_446299_.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(CopperGolemStatueBlock.POSE, p_446035_));
      return p_446299_;
   }
}
