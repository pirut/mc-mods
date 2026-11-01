package net.minecraft.client.model.geom.builders;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;

@Environment(EnvType.CLIENT)
public class LayerDefinition {
   private final MeshDefinition mesh;
   private final MaterialDefinition material;

   private LayerDefinition(MeshDefinition mesh, MaterialDefinition material) {
      this.mesh = mesh;
      this.material = material;
   }

   public LayerDefinition apply(MeshTransformer transformer) {
      return new LayerDefinition(transformer.apply(this.mesh), this.material);
   }

   public ModelPart bakeRoot() {
      return this.mesh.getRoot().bake(this.material.xTexSize, this.material.yTexSize);
   }

   public static LayerDefinition create(MeshDefinition mesh, int texWidth, int texHeight) {
      return new LayerDefinition(mesh, new MaterialDefinition(texWidth, texHeight));
   }
}
