package net.minecraft.world.level.chunk;

public interface PaletteResize<T> {
   int onResize(int var1, T var2);

   static <T> PaletteResize<T> noResizeExpected() {
      return (p_446326_, p_446950_) -> {
         throw new IllegalArgumentException("Unexpected palette resize, bits = " + p_446326_ + ", added value = " + p_446950_);
      };
   }
}
