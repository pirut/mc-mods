package net.minecraft.world.entity;

import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

public record PositionMoveRotation(Vec3 position, Vec3 deltaMovement, float yRot, float xRot) {
   public static final StreamCodec<FriendlyByteBuf, PositionMoveRotation> STREAM_CODEC = StreamCodec.composite(
      Vec3.STREAM_CODEC,
      PositionMoveRotation::position,
      Vec3.STREAM_CODEC,
      PositionMoveRotation::deltaMovement,
      ByteBufCodecs.FLOAT,
      PositionMoveRotation::yRot,
      ByteBufCodecs.FLOAT,
      PositionMoveRotation::xRot,
      PositionMoveRotation::new
   );

   public static PositionMoveRotation of(Entity p_371737_) {
      return p_371737_.isInterpolating()
         ? new PositionMoveRotation(
            p_371737_.getInterpolation().position(), p_371737_.getKnownMovement(), p_371737_.getInterpolation().yRot(), p_371737_.getInterpolation().xRot()
         )
         : new PositionMoveRotation(p_371737_.position(), p_371737_.getKnownMovement(), p_371737_.getYRot(), p_371737_.getXRot());
   }

   public PositionMoveRotation withRotation(float p_436804_, float p_436575_) {
      return new PositionMoveRotation(this.position(), this.deltaMovement(), p_436804_, p_436575_);
   }

   public static PositionMoveRotation of(TeleportTransition p_379436_) {
      return new PositionMoveRotation(p_379436_.position(), p_379436_.deltaMovement(), p_379436_.yRot(), p_379436_.xRot());
   }

   public static PositionMoveRotation calculateAbsolute(PositionMoveRotation p_371265_, PositionMoveRotation p_371276_, Set<Relative> p_371779_) {
      double d0 = p_371779_.contains(Relative.X) ? p_371265_.position.x : 0.0;
      double d1 = p_371779_.contains(Relative.Y) ? p_371265_.position.y : 0.0;
      double d2 = p_371779_.contains(Relative.Z) ? p_371265_.position.z : 0.0;
      float f = p_371779_.contains(Relative.Y_ROT) ? p_371265_.yRot : 0.0F;
      float f1 = p_371779_.contains(Relative.X_ROT) ? p_371265_.xRot : 0.0F;
      Vec3 vec3 = new Vec3(d0 + p_371276_.position.x, d1 + p_371276_.position.y, d2 + p_371276_.position.z);
      float f2 = f + p_371276_.yRot;
      float f3 = Mth.clamp(f1 + p_371276_.xRot, -90.0F, 90.0F);
      Vec3 vec31 = p_371265_.deltaMovement;
      if (p_371779_.contains(Relative.ROTATE_DELTA)) {
         float f4 = p_371265_.yRot - f2;
         float f5 = p_371265_.xRot - f3;
         vec31 = vec31.xRot((float)Math.toRadians(f5));
         vec31 = vec31.yRot((float)Math.toRadians(f4));
      }

      Vec3 vec32 = new Vec3(
         calculateDelta(vec31.x, p_371276_.deltaMovement.x, p_371779_, Relative.DELTA_X),
         calculateDelta(vec31.y, p_371276_.deltaMovement.y, p_371779_, Relative.DELTA_Y),
         calculateDelta(vec31.z, p_371276_.deltaMovement.z, p_371779_, Relative.DELTA_Z)
      );
      return new PositionMoveRotation(vec3, vec32, f2, f3);
   }

   private static double calculateDelta(double p_371633_, double p_371363_, Set<Relative> p_371682_, Relative p_371585_) {
      return p_371682_.contains(p_371585_) ? p_371633_ + p_371363_ : p_371363_;
   }
}
