package net.neoforged.neoforge.client.event;

import java.util.HashSet;
import java.util.SequencedMap;
import java.util.Set;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterTextureAtlasesEvent extends Event implements IModBusEvent {
   private final SequencedMap<Identifier, AtlasManager.AtlasConfig> atlases;

   @Internal
   public RegisterTextureAtlasesEvent(SequencedMap<Identifier, AtlasManager.AtlasConfig> atlases) {
      this.atlases = atlases;
   }

   public void register(AtlasManager.AtlasConfig atlasConfig) {
      AtlasManager.AtlasConfig oldConfig = this.atlases.putIfAbsent(atlasConfig.definitionLocation(), atlasConfig);
      if (oldConfig != null) {
         throw new IllegalStateException(
            String.format("Duplicate registration of atlas: %s (old config: %s, new config: %s)", atlasConfig.definitionLocation(), oldConfig, atlasConfig)
         );
      }
   }

   public void addAdditionalMetadata(Identifier atlasId, MetadataSectionType<?> metaSectionType) {
      if (metaSectionType == AnimationMetadataSection.TYPE) {
         throw new IllegalArgumentException("Animation metadata is always loaded, it may not be added as additional metadata");
      } else {
         AtlasManager.AtlasConfig atlas = this.atlases.get(atlasId);
         if (atlas == null) {
            throw new IllegalArgumentException("Unknown texture atlas: " + atlasId);
         } else {
            Set<MetadataSectionType<?>> additionalMetadata = new HashSet<>(atlas.additionalMetadata());
            additionalMetadata.add(metaSectionType);
            this.atlases
               .put(atlasId, new AtlasManager.AtlasConfig(atlas.textureId(), atlas.definitionLocation(), atlas.createMipmaps(), Set.copyOf(additionalMetadata)));
         }
      }
   }
}
