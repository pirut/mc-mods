package net.minecraft.client.resources.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public interface ModelBaker {
   ResolvedModel getModel(Identifier modelLocation);

   BlockModelPart missingBlockModelPart();

   SpriteGetter sprites();

   ModelBaker.PartCache parts();

   <T> T compute(ModelBaker.SharedOperationKey<T> key);

   @Environment(EnvType.CLIENT)
   public interface PartCache {
      default Vector3fc vector(float x, float y, float z) {
         return this.vector(new Vector3f(x, y, z));
      }

      Vector3fc vector(Vector3fc vector);
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface SharedOperationKey<T> {
      T compute(ModelBaker modelBaker);
   }
}
