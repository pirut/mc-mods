package net.minecraft.world.level;

public class GrassColor {
   private static int[] pixels = new int[65536];

   public static void init(int[] p_46419_) {
      pixels = p_46419_;
   }

   public static int get(double p_46416_, double p_46417_) {
      p_46417_ *= p_46416_;
      int $$2 = (int)((1.0 - p_46416_) * 255.0);
      int $$3 = (int)((1.0 - p_46417_) * 255.0);
      int $$4 = $$3 << 8 | $$2;
      return $$4 >= pixels.length ? -65281 : pixels[$$4];
   }

   public static int getDefaultColor() {
      return get(0.5, 1.0);
   }
}
