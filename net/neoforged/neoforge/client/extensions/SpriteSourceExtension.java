package net.neoforged.neoforge.client.extensions;

import java.util.Set;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.ResourceManager;

public interface SpriteSourceExtension {
   default void run(ResourceManager resourceManager, SpriteSource.Output output, Set<MetadataSectionType<?>> additionalMetadata) {
      this.self().run(resourceManager, output);
   }

   private SpriteSource self() {
      return (SpriteSource)this;
   }
}
