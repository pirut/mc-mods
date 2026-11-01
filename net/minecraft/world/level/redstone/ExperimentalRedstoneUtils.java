package net.minecraft.world.level.redstone;

import net.minecraft.core.Direction;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ExperimentalRedstoneUtils {
   @Nullable
   public static Orientation initialOrientation(Level p_366754_, @Nullable Direction p_366872_, @Nullable Direction p_366796_) {
      if (p_366754_.enabledFeatures().contains(FeatureFlags.REDSTONE_EXPERIMENTS)) {
         Orientation orientation = Orientation.random(p_366754_.random).withSideBias(Orientation.SideBias.LEFT);
         if (p_366796_ != null) {
            orientation = orientation.withUp(p_366796_);
         }

         if (p_366872_ != null) {
            orientation = orientation.withFront(p_366872_);
         }

         return orientation;
      } else {
         return null;
      }
   }

   @Nullable
   public static Orientation withFront(@Nullable Orientation p_361215_, Direction p_362021_) {
      return p_361215_ == null ? null : p_361215_.withFront(p_362021_);
   }
}
