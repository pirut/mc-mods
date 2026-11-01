package net.minecraft.world.phys.shapes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.jspecify.annotations.Nullable;

public class MinecartCollisionContext extends EntityCollisionContext {
   @Nullable
   private BlockPos ingoreBelow;
   @Nullable
   private BlockPos slopeIgnore;

   protected MinecartCollisionContext(AbstractMinecart minecart, boolean alwaysCollideWithFluid) {
      super(minecart, alwaysCollideWithFluid, false);
      this.setupContext(minecart);
   }

   private void setupContext(AbstractMinecart minecart) {
      BlockPos blockPos = minecart.getCurrentBlockPosOrRailBelow();
      BlockState blockState = minecart.level().getBlockState(blockPos);
      boolean bl = BaseRailBlock.isRail(blockState);
      if (bl) {
         this.ingoreBelow = blockPos.below();
         RailShape railShape = blockState.getValue(((BaseRailBlock)blockState.getBlock()).getShapeProperty());
         if (railShape.isSlope()) {
            this.slopeIgnore = switch (railShape) {
               case ASCENDING_EAST -> blockPos.east();
               case ASCENDING_WEST -> blockPos.west();
               case ASCENDING_NORTH -> blockPos.north();
               case ASCENDING_SOUTH -> blockPos.south();
               default -> null;
            };
         }
      }
   }

   @Override
   public VoxelShape getCollisionShape(BlockState state, CollisionGetter collisionGetter, BlockPos pos) {
      return !pos.equals(this.ingoreBelow) && !pos.equals(this.slopeIgnore) ? super.getCollisionShape(state, collisionGetter, pos) : Shapes.empty();
   }
}
