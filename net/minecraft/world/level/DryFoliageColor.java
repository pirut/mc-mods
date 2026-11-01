package net.minecraft.world.level;

public class DryFoliageColor {
   public static final int FOLIAGE_DRY_DEFAULT = -10732494;
   private static int[] pixels = new int[65536];

   public static void init(int[] p_406233_) {
      pixels = p_406233_;
   }

   public static int get(double p_406310_, double p_406267_) {
      return ColorMapColorUtil.get(p_406310_, p_406267_, pixels, -10732494);
   }
}
