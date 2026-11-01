package net.minecraft.world.level.block;

import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public interface SelectableSlotContainer {
   int getRows();

   int getColumns();

   default OptionalInt getHitSlot(BlockHitResult p_435931_, Direction p_434653_) {
      return getRelativeHitCoordinatesForBlockFace(p_435931_, p_434653_).map(p_434037_ -> {
         int i = getSection(1.0F - p_434037_.y, this.getRows());
         int j = getSection(p_434037_.x, this.getColumns());
         return OptionalInt.of(j + i * this.getColumns());
      }).orElseGet(OptionalInt::empty);
   }

   private static Optional<Vec2> getRelativeHitCoordinatesForBlockFace(BlockHitResult p_435943_, Direction p_434847_) {
      Direction direction = p_435943_.getDirection();
      if (p_434847_ != direction) {
         return Optional.empty();
      } else {
         BlockPos blockpos = p_435943_.getBlockPos().relative(direction);
         Vec3 vec3 = p_435943_.getLocation().subtract(blockpos.getX(), blockpos.getY(), blockpos.getZ());
         double d0 = vec3.x();
         double d1 = vec3.y();
         double d2 = vec3.z();

         return switch (direction) {
            case NORTH -> Optional.of(new Vec2((float)(1.0 - d0), (float)d1));
            case SOUTH -> Optional.of(new Vec2((float)d0, (float)d1));
            case WEST -> Optional.of(new Vec2((float)d2, (float)d1));
            case EAST -> Optional.of(new Vec2((float)(1.0 - d2), (float)d1));
            case DOWN, UP -> Optional.empty();
         };
      }
   }

   private static int getSection(float p_434560_, int p_433136_) {
      float f = p_434560_ * 16.0F;
      float f1 = 16.0F / p_433136_;
      return Mth.clamp(Mth.floor(f / f1), 0, p_433136_ - 1);
   }
}
