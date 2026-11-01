package net.minecraft.client.model.geom;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ModelLayerLocation {
   private final ResourceLocation model;
   private final String layer;

   public ModelLayerLocation(ResourceLocation p_171121_, String p_171122_) {
      this.model = p_171121_;
      this.layer = p_171122_;
   }

   public ResourceLocation getModel() {
      return this.model;
   }

   public String getLayer() {
      return this.layer;
   }

   @Override
   public boolean equals(Object p_171126_) {
      if (this == p_171126_) {
         return true;
      } else {
         return !(p_171126_ instanceof ModelLayerLocation $$1) ? false : this.model.equals($$1.model) && this.layer.equals($$1.layer);
      }
   }

   @Override
   public int hashCode() {
      int $$0 = this.model.hashCode();
      return 31 * $$0 + this.layer.hashCode();
   }

   @Override
   public String toString() {
      return this.model + "#" + this.layer;
   }
}
