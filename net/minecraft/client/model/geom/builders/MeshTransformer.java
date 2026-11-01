package net.minecraft.client.model.geom.builders;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface MeshTransformer {
   MeshTransformer IDENTITY = p_396294_ -> p_396294_;

   static MeshTransformer scaling(float p_365200_) {
      float f = 24.016F * (1.0F - p_365200_);
      return p_362687_ -> p_362687_.transformed(p_362796_ -> p_362796_.scaled(p_365200_).translated(0.0F, f, 0.0F));
   }

   MeshDefinition apply(MeshDefinition var1);
}
