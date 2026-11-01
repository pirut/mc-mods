package net.neoforged.neoforge.client.model.ao;

import net.minecraft.core.Direction;

enum AoFace {
   DOWN,
   UP,
   NORTH,
   SOUTH,
   WEST,
   EAST;

   public static AoFace fromDirection(Direction direction) {
      return switch (direction) {
         case DOWN -> DOWN;
         case UP -> UP;
         case NORTH -> NORTH;
         case SOUTH -> SOUTH;
         case WEST -> WEST;
         case EAST -> EAST;
      };
   }

   float computeDepth(float x, float y, float z) {
      return switch (this) {
         case DOWN -> y;
         case UP -> 1.0F - y;
         case NORTH -> z;
         case SOUTH -> 1.0F - z;
         case WEST -> x;
         case EAST -> 1.0F - x;
      };
   }

   void computeCornerWeights(float[] out, float x, float y, float z) {
      x = Math.clamp(x, 0.0F, 1.0F);
      y = Math.clamp(y, 0.0F, 1.0F);
      z = Math.clamp(z, 0.0F, 1.0F);
      switch (this) {
         case DOWN:
            out[0] = (1.0F - x) * z;
            out[1] = (1.0F - x) * (1.0F - z);
            out[2] = x * (1.0F - z);
            out[3] = x * z;
            break;
         case UP:
            out[0] = x * z;
            out[1] = x * (1.0F - z);
            out[2] = (1.0F - x) * (1.0F - z);
            out[3] = (1.0F - x) * z;
            break;
         case NORTH:
            out[0] = y * (1.0F - x);
            out[1] = y * x;
            out[2] = (1.0F - y) * x;
            out[3] = (1.0F - y) * (1.0F - x);
            break;
         case SOUTH:
            out[0] = y * (1.0F - x);
            out[1] = (1.0F - y) * (1.0F - x);
            out[2] = (1.0F - y) * x;
            out[3] = y * x;
            break;
         case WEST:
            out[0] = y * z;
            out[1] = y * (1.0F - z);
            out[2] = (1.0F - y) * (1.0F - z);
            out[3] = (1.0F - y) * z;
            break;
         case EAST:
            out[0] = (1.0F - y) * z;
            out[1] = (1.0F - y) * (1.0F - z);
            out[2] = y * (1.0F - z);
            out[3] = y * z;
      }
   }
}
