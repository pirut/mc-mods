package com.mojang.math;

import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.Mth;

public enum Quadrant {
   R0(0, OctahedralGroup.IDENTITY, OctahedralGroup.IDENTITY, OctahedralGroup.IDENTITY),
   R90(1, OctahedralGroup.BLOCK_ROT_X_90, OctahedralGroup.BLOCK_ROT_Y_90, OctahedralGroup.BLOCK_ROT_Z_90),
   R180(2, OctahedralGroup.BLOCK_ROT_X_180, OctahedralGroup.BLOCK_ROT_Y_180, OctahedralGroup.BLOCK_ROT_Z_180),
   R270(3, OctahedralGroup.BLOCK_ROT_X_270, OctahedralGroup.BLOCK_ROT_Y_270, OctahedralGroup.BLOCK_ROT_Z_270);

   public static final Codec<Quadrant> CODEC = Codec.INT.comapFlatMap(p_405170_ -> {
      return switch (Mth.positiveModulo(p_405170_, 360)) {
         case 0 -> DataResult.success(R0);
         case 90 -> DataResult.success(R90);
         case 180 -> DataResult.success(R180);
         case 270 -> DataResult.success(R270);
         default -> DataResult.error(() -> "Invalid rotation " + p_405170_ + " found, only 0/90/180/270 allowed");
      };
   }, p_405217_ -> {
      return switch (p_405217_) {
         case R0 -> 0;
         case R90 -> 90;
         case R180 -> 180;
         case R270 -> 270;
      };
   });
   public final int shift;
   public final OctahedralGroup rotationX;
   public final OctahedralGroup rotationY;
   public final OctahedralGroup rotationZ;

   private Quadrant(int p_405863_, OctahedralGroup p_470773_, OctahedralGroup p_470606_, OctahedralGroup p_470824_) {
      this.shift = p_405863_;
      this.rotationX = p_470773_;
      this.rotationY = p_470606_;
      this.rotationZ = p_470824_;
   }

   @Deprecated
   public static Quadrant parseJson(int p_404967_) {
      return switch (Mth.positiveModulo(p_404967_, 360)) {
         case 0 -> R0;
         case 90 -> R90;
         case 180 -> R180;
         case 270 -> R270;
         default -> throw new JsonParseException("Invalid rotation " + p_404967_ + " found, only 0/90/180/270 allowed");
      };
   }

   public static OctahedralGroup fromXYAngles(Quadrant p_470810_, Quadrant p_470761_) {
      return p_470761_.rotationY.compose(p_470810_.rotationX);
   }

   public static OctahedralGroup fromXYZAngles(Quadrant p_470812_, Quadrant p_470719_, Quadrant p_470579_) {
      return p_470579_.rotationZ.compose(p_470719_.rotationY.compose(p_470812_.rotationX));
   }

   public int rotateVertexIndex(int p_405719_) {
      return (p_405719_ + this.shift) % 4;
   }
}
