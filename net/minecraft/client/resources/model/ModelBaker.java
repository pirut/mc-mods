package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.ModelBakerExtension;
import net.neoforged.neoforge.client.extensions.ModelBakerPartCacheExtension;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public interface ModelBaker extends ModelBakerExtension {
   ResolvedModel getModel(Identifier var1);

   BlockModelPart missingBlockModelPart();

   SpriteGetter sprites();

   ModelBaker.PartCache parts();

   <T> T compute(ModelBaker.SharedOperationKey<T> var1);

   @OnlyIn(Dist.CLIENT)
   public interface PartCache extends ModelBakerPartCacheExtension {
      default Vector3fc vector(float p_470811_, float p_470686_, float p_470742_) {
         return this.vector(new Vector3f(p_470811_, p_470686_, p_470742_));
      }

      Vector3fc vector(Vector3fc var1);
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface SharedOperationKey<T> {
      T compute(ModelBaker var1);
   }
}
