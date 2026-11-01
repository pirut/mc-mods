package net.minecraft.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public record MaterialMapper(Identifier sheet, String prefix) {
   public Material apply(Identifier name) {
      return new Material(this.sheet, name.withPrefix(this.prefix + "/"));
   }

   public Material defaultNamespaceApply(String name) {
      return this.apply(Identifier.withDefaultNamespace(name));
   }
}
