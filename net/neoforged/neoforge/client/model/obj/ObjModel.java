package net.neoforged.neoforge.client.model.obj;

import net.minecraft.client.resources.model.UnbakedGeometry;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.StandardModelParameters;

public class ObjModel extends AbstractUnbakedModel {
   private final ObjGeometry geometry;

   public ObjModel(StandardModelParameters parameters, ObjGeometry geometry) {
      super(parameters);
      this.geometry = geometry;
   }

   @Override
   public UnbakedGeometry geometry() {
      return this.geometry;
   }
}
