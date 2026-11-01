package net.neoforged.neoforge.client.model.quad;

public sealed interface BakedColors permits BakedColors.PerQuad, BakedColors.PerVertex {
   BakedColors DEFAULT = new BakedColors.PerQuad(-1);

   int color(int var1);

   static BakedColors of(int color0, int color1, int color2, int color3) {
      return (BakedColors)(color0 == color1 && color0 == color2 && color0 == color3 ? of(color0) : new BakedColors.PerVertex(color0, color1, color2, color3));
   }

   static BakedColors of(int color) {
      return (BakedColors)(color == -1 ? DEFAULT : new BakedColors.PerQuad(color));
   }

   public record PerQuad(int color) implements BakedColors {
   }

   public record PerVertex(int color0, int color1, int color2, int color3) implements BakedColors {
      @Override
      public int color(int vertex) {
         return switch (vertex) {
            case 0 -> this.color0;
            case 1 -> this.color1;
            case 2 -> this.color2;
            case 3 -> this.color3;
            default -> throw new IndexOutOfBoundsException(vertex);
         };
      }
   }
}
