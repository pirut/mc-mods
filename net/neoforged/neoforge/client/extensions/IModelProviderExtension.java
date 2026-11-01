package net.neoforged.neoforge.client.extensions;

import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.resources.Identifier;

public interface IModelProviderExtension {
   default Identifier modLocation(String modelPath) {
      return Identifier.fromNamespaceAndPath(this.self().modId, modelPath);
   }

   default Identifier mcLocation(String modelPath) {
      return Identifier.withDefaultNamespace(modelPath);
   }

   private ModelProvider self() {
      return (ModelProvider)this;
   }
}
