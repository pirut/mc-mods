package net.minecraft.client.renderer.state;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;

@Environment(EnvType.CLIENT)
public class WorldBorderRenderState {
   public double minX;
   public double maxX;
   public double minZ;
   public double maxZ;
   public int tint;
   public double alpha;

   public List<WorldBorderRenderState.DistancePerDirection> closestBorder(double x, double z) {
      WorldBorderRenderState.DistancePerDirection[] distancePerDirections = new WorldBorderRenderState.DistancePerDirection[]{
         new WorldBorderRenderState.DistancePerDirection(Direction.NORTH, z - this.minZ),
         new WorldBorderRenderState.DistancePerDirection(Direction.SOUTH, this.maxZ - z),
         new WorldBorderRenderState.DistancePerDirection(Direction.WEST, x - this.minX),
         new WorldBorderRenderState.DistancePerDirection(Direction.EAST, this.maxX - x)
      };
      return Arrays.stream(distancePerDirections).sorted(Comparator.comparingDouble(distancePerDirection -> distancePerDirection.distance)).toList();
   }

   public void reset() {
      this.alpha = 0.0;
   }

   @Environment(EnvType.CLIENT)
   public record DistancePerDirection(Direction direction, double distance) {
   }
}
