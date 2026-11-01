package net.minecraft.client.model.geom.builders;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record UVPair(float u, float v) {
   @Override
   public String toString() {
      return "(" + this.u + "," + this.v + ")";
   }

   public static long pack(float u, float v) {
      long l = Float.floatToIntBits(u) & 4294967295L;
      long m = Float.floatToIntBits(v) & 4294967295L;
      return l << 32 | m;
   }

   public static float unpackU(long packed) {
      int i = (int)(packed >> 32);
      return Float.intBitsToFloat(i);
   }

   public static float unpackV(long packed) {
      return Float.intBitsToFloat((int)packed);
   }
}
