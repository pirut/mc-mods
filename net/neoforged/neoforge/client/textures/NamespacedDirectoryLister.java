package net.neoforged.neoforge.client.textures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public record NamespacedDirectoryLister(String namespace, String sourcePath, String idPrefix) implements SpriteSource {
   public static final MapCodec<NamespacedDirectoryLister> CODEC = RecordCodecBuilder.mapCodec(
      inst -> inst.group(
            Codec.STRING.fieldOf("namespace").forGetter(lister -> lister.namespace),
            Codec.STRING.fieldOf("source").forGetter(lister -> lister.sourcePath),
            Codec.STRING.fieldOf("prefix").forGetter(lister -> lister.idPrefix)
         )
         .apply(inst, NamespacedDirectoryLister::new)
   );
   public static final Identifier ID = Identifier.fromNamespaceAndPath("neoforge", "namespaced_directory");

   @Override
   public void run(ResourceManager resourceManager, SpriteSource.Output output) {
      FileToIdConverter converter = new FileToIdConverter("textures/" + this.sourcePath, ".png");
      converter.listMatchingResourcesFromNamespace(resourceManager, this.namespace).forEach((path, resource) -> {
         Identifier id = converter.fileToId(path).withPrefix(this.idPrefix);
         output.add(id, resource);
      });
   }

   @Override
   public MapCodec<? extends SpriteSource> codec() {
      return CODEC;
   }
}
