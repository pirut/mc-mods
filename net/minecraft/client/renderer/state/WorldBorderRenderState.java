package net.minecraft.client.renderer.state;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WorldBorderRenderState {
   public double minX;
   public double maxX;
   public double minZ;
   public double maxZ;
   public int tint;
   public double alpha;

   public List<WorldBorderRenderState.DistancePerDirection> closestBorder(double p_451554_, double p_451518_) {
      WorldBorderRenderState.DistancePerDirection[] aworldborderrenderstate$distanceperdirection = new WorldBorderRenderState.DistancePerDirection[]{
         new WorldBorderRenderState.DistancePerDirection(Direction.NORTH, p_451518_ - this.minZ),
         new WorldBorderRenderState.DistancePerDirection(Direction.SOUTH, this.maxZ - p_451518_),
         new WorldBorderRenderState.DistancePerDirection(Direction.WEST, p_451554_ - this.minX),
         new WorldBorderRenderState.DistancePerDirection(Direction.EAST, this.maxX - p_451554_)
      };
      return Arrays.stream(aworldborderrenderstate$distanceperdirection).sorted(Comparator.comparingDouble(p_451523_ -> p_451523_.distance)).toList();
   }

   public void reset() {
      this.alpha = 0.0;
   }

   @OnlyIn(Dist.CLIENT)
   public record DistancePerDirection(Direction direction, double distance) {
   }
}
