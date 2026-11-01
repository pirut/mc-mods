package net.neoforged.neoforge.common.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public abstract class BlockTagCopyingItemTagProvider extends IntrinsicHolderTagsProvider<Item> {
   private final CompletableFuture<TagsProvider.TagLookup<Block>> blockTags;
   private final Map<TagKey<Block>, TagKey<Item>> tagsToCopy = new HashMap<>();

   public BlockTagCopyingItemTagProvider(
      PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagsProvider.TagLookup<Block>> blockTags, String modId
   ) {
      super(output, Registries.ITEM, lookupProvider, item -> item.builtInRegistryHolder().key(), modId);
      this.blockTags = blockTags;
   }

   protected void copy(TagKey<Block> blockTag, TagKey<Item> itemTag) {
      this.tagsToCopy.put(blockTag, itemTag);
   }

   @Override
   protected CompletableFuture<HolderLookup.Provider> createContentsProvider() {
      return super.createContentsProvider()
         .thenCombine(
            this.blockTags,
            (provider, blockTags) -> {
               this.tagsToCopy
                  .forEach(
                     (fromBlockTag, toItemTag) -> {
                        TagBuilder toBuilder = this.getOrCreateRawBuilder((TagKey<Item>)toItemTag);
                        TagBuilder fromBuilder = blockTags.apply((TagKey<? super TagKey<Block>>)fromBlockTag)
                           .orElseThrow(() -> new IllegalStateException("Missing block tag " + toItemTag.location()));
                        List<TagEntry> fromTags = fromBuilder.build();
                        fromTags.forEach(toBuilder::add);
                        fromBuilder.getRemoveEntries().forEach(toBuilder::remove);
                     }
                  );
               return (HolderLookup.Provider)provider;
            }
         );
   }
}
