package net.minecraft.data.tags;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public abstract class ItemTagsProvider extends IntrinsicHolderTagsProvider<Item> {
   private final CompletableFuture<TagsProvider.TagLookup<Block>> blockTags;
   private final Map<TagKey<Block>, TagKey<Item>> tagsToCopy = new HashMap<>();

   public ItemTagsProvider(
      @Deprecated PackOutput p_275343_, CompletableFuture<HolderLookup.Provider> p_275729_, CompletableFuture<TagsProvider.TagLookup<Block>> p_275322_
   ) {
      this(p_275343_, p_275729_, p_275322_, "vanilla", null);
   }

   @Deprecated
   public ItemTagsProvider(
      PackOutput p_275204_,
      CompletableFuture<HolderLookup.Provider> p_275194_,
      CompletableFuture<TagsProvider.TagLookup<Item>> p_275207_,
      CompletableFuture<TagsProvider.TagLookup<Block>> p_275634_
   ) {
      this(p_275204_, p_275194_, p_275207_, p_275634_, "vanilla", null);
   }

   public ItemTagsProvider(
      PackOutput p_275343_,
      CompletableFuture<HolderLookup.Provider> p_275729_,
      CompletableFuture<TagsProvider.TagLookup<Block>> p_275322_,
      String modId,
      @Nullable ExistingFileHelper existingFileHelper
   ) {
      super(p_275343_, Registries.ITEM, p_275729_, p_255790_ -> p_255790_.builtInRegistryHolder().key(), modId, existingFileHelper);
      this.blockTags = p_275322_;
   }

   public ItemTagsProvider(
      PackOutput p_275204_,
      CompletableFuture<HolderLookup.Provider> p_275194_,
      CompletableFuture<TagsProvider.TagLookup<Item>> p_275207_,
      CompletableFuture<TagsProvider.TagLookup<Block>> p_275634_,
      String modId,
      @Nullable ExistingFileHelper existingFileHelper
   ) {
      super(p_275204_, Registries.ITEM, p_275194_, p_275207_, p_274765_ -> p_274765_.builtInRegistryHolder().key(), modId, existingFileHelper);
      this.blockTags = p_275634_;
   }

   protected void copy(TagKey<Block> p_206422_, TagKey<Item> p_206423_) {
      this.tagsToCopy.put(p_206422_, p_206423_);
   }

   @Override
   protected CompletableFuture<HolderLookup.Provider> createContentsProvider() {
      return super.createContentsProvider().thenCombine(this.blockTags, (p_274766_, p_274767_) -> {
         this.tagsToCopy.forEach((p_274763_, p_274764_) -> {
            TagBuilder tagbuilder = this.getOrCreateRawBuilder((TagKey<Item>)p_274764_);
            Optional<TagBuilder> optional = p_274767_.apply((TagKey<? super TagKey<Block>>)p_274763_);
            TagBuilder fromBuilder = optional.orElseThrow(() -> new IllegalStateException("Missing block tag " + p_274764_.location()));
            fromBuilder.build().forEach(tagbuilder::add);
            fromBuilder.getRemoveEntries().forEach(tagbuilder::remove);
         });
         return (HolderLookup.Provider)p_274766_;
      });
   }
}
