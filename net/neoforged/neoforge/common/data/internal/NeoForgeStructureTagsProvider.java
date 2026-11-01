package net.neoforged.neoforge.common.data.internal;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.StructureTagsProvider;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class NeoForgeStructureTagsProvider extends StructureTagsProvider {
   public NeoForgeStructureTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
      super(output, lookupProvider, "neoforge", existingFileHelper);
   }

   @Override
   public void addTags(HolderLookup.Provider lookupProvider) {
      this.tag(Tags.Structures.HIDDEN_FROM_DISPLAYERS);
      this.tag(Tags.Structures.HIDDEN_FROM_LOCATOR_SELECTION);
   }
}
