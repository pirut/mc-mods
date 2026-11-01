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

   public static PositionMoveRotation of(Entity entity) {
      return entity.isInterpolating()
         ? new PositionMoveRotation(
            entity.getInterpolation().position(), entity.getKnownMovement(), entity.getInterpolation().yRot(), entity.getInterpolation().xRot()
         )
         : new PositionMoveRotation(entity.position(), entity.getKnownMovement(), entity.getYRot(), entity.getXRot());
   }

   public PositionMoveRotation withRotation(float yRot, float xRot) {
      return new PositionMoveRotation(this.position(), this.deltaMovement(), yRot, xRot);
   }

   public static PositionMoveRotation of(TeleportTransition teleportTransition) {
      return new PositionMoveRotation(teleportTransition.position(), teleportTransition.deltaMovement(), teleportTransition.yRot(), teleportTransition.xRot());
   }

   public static PositionMoveRotation calculateAbsolute(PositionMoveRotation current, PositionMoveRotation after, Set<Relative> relatives) {
      double d = relatives.contains(Relative.X) ? current.position.x : 0.0;
      double e = relatives.contains(Relative.Y) ? current.position.y : 0.0;
      double f = relatives.contains(Relative.Z) ? current.position.z : 0.0;
      float g = relatives.contains(Relative.Y_ROT) ? current.yRot : 0.0F;
      float h = relatives.contains(Relative.X_ROT) ? current.xRot : 0.0F;
      Vec3 vec3 = new Vec3(d + after.position.x, e + after.position.y, f + after.position.z);
      float i = g + after.yRot;
      float j = Mth.clamp(h + after.xRot, -90.0F, 90.0F);
      Vec3 vec32 = current.deltaMovement;
      if (relatives.contains(Relative.ROTATE_DELTA)) {
         float k = current.yRot - i;
         float l = current.xRot - j;
         vec32 = vec32.xRot((float)Math.toRadians(l));
         vec32 = vec32.yRot((float)Math.toRadians(k));
      }

      Vec3 vec33 = new Vec3(
         calculateDelta(vec32.x, after.deltaMovement.x, relatives, Relative.DELTA_X),
         calculateDelta(vec32.y, after.deltaMovement.y, relatives, Relative.DELTA_Y),
         calculateDelta(vec32.z, after.deltaMovement.z, relatives, Relative.DELTA_Z)
      );
      return new PositionMoveRotation(vec3, vec33, i, j);
   }

   private static double calculateDelta(double position, double deltaMovement, Set<Relative> relatives, Relative deltaRelative) {
      return relatives.contains(deltaRelative) ? position + deltaMovement : deltaMovement;
   }
}
