package net.neoforged.neoforge.client.data.internal;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.AtlasIds;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.data.SpriteSourceProvider;

public class NeoForgeSpriteSourceProvider extends SpriteSourceProvider {
   public NeoForgeSpriteSourceProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
      super(output, lookupProvider, "neoforge");
   }

   @Override
   protected void gather() {
      this.atlas(AtlasIds.BLOCKS).addSource(new SingleFile(Identifier.fromNamespaceAndPath("neoforge", "white")));
   }
}
