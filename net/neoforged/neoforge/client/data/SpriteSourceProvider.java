package net.neoforged.neoforge.client.data;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.WithConditions;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

public abstract class SpriteSourceProvider extends JsonCodecProvider<List<Optional<WithConditions<SpriteSource>>>> {
   private static final Codec<List<Optional<WithConditions<SpriteSource>>>> CODEC = ConditionalOps.createConditionalCodecWithConditions(SpriteSources.CODEC)
      .listOf()
      .fieldOf("sources")
      .codec();
   private final Map<Identifier, SpriteSourceProvider.SourceList> atlases = new HashMap<>();

   public SpriteSourceProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId) {
      super(output, PackOutput.Target.RESOURCE_PACK, "atlases", CODEC, lookupProvider, modId);
   }

   protected final SpriteSourceProvider.SourceList atlas(Identifier id) {
      return this.atlases.computeIfAbsent(id, i -> {
         SpriteSourceProvider.SourceList newAtlas = new SpriteSourceProvider.SourceList();
         this.unconditional(i, newAtlas.sources);
         return newAtlas;
      });
   }

   protected static final class SourceList {
      private final List<Optional<WithConditions<SpriteSource>>> sources = new ArrayList<>();

      private SourceList() {
      }

      public SpriteSourceProvider.SourceList addSource(SpriteSource source) {
         this.sources.add(Optional.of(new WithConditions<>(source)));
         return this;
      }

      public SpriteSourceProvider.SourceList addSource(SpriteSource source, ICondition... conditions) {
         this.sources.add(Optional.of(new WithConditions<>(source, conditions)));
         return this;
      }
   }
}
