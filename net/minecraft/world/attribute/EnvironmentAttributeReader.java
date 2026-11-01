package net.minecraft.world.attribute;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface EnvironmentAttributeReader {
   EnvironmentAttributeReader EMPTY = new EnvironmentAttributeReader() {
      @Override
      public <Value> Value getDimensionValue(EnvironmentAttribute<Value> p_457691_) {
         return p_457691_.defaultValue();
      }

      @Override
      public <Value> Value getValue(EnvironmentAttribute<Value> p_457526_, Vec3 p_457965_, @Nullable SpatialAttributeInterpolator p_457801_) {
         return p_457526_.defaultValue();
      }
   };

   <Value> Value getDimensionValue(EnvironmentAttribute<Value> var1);

   default <Value> Value getValue(EnvironmentAttribute<Value> p_458118_, BlockPos p_458198_) {
      return this.getValue(p_458118_, Vec3.atCenterOf(p_458198_));
   }

   default <Value> Value getValue(EnvironmentAttribute<Value> p_458044_, Vec3 p_457821_) {
      return this.getValue(p_458044_, p_457821_, null);
   }

   <Value> Value getValue(EnvironmentAttribute<Value> var1, Vec3 var2, @Nullable SpatialAttributeInterpolator var3);
}
