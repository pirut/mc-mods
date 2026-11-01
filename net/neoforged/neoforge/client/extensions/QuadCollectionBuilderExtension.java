package net.neoforged.neoforge.client.extensions;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.Direction;

public interface QuadCollectionBuilderExtension {
   private QuadCollection.Builder self() {
      return (QuadCollection.Builder)this;
   }

   default QuadCollection.Builder addAll(QuadCollection collection) {
      QuadCollection.Builder self = this.self();

      for (Direction direction : Direction.values()) {
         for (BakedQuad quad : collection.getQuads(direction)) {
            self.addCulledFace(direction, quad);
         }
      }

      for (BakedQuad quad : collection.getQuads(null)) {
         self.addUnculledFace(quad);
      }

      return self;
   }
}
