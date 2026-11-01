package net.neoforged.neoforge.common.extensions;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;

public interface ITagBuilderExtension {
   default TagBuilder getRawBuilder() {
      return (TagBuilder)this;
   }

   @Deprecated(
      forRemoval = true,
      since = "1.21.1"
   )
   default TagBuilder remove(TagEntry tagEntry, String source) {
      return this.getRawBuilder().remove(tagEntry);
   }

   @Deprecated(
      forRemoval = true,
      since = "1.21.1"
   )
   default TagBuilder removeElement(ResourceLocation elementID, String source) {
      return this.remove(TagEntry.element(elementID), source);
   }

   default TagBuilder removeElement(ResourceLocation elementID) {
      return this.getRawBuilder().remove(TagEntry.element(elementID));
   }

   @Deprecated(
      forRemoval = true,
      since = "1.21.1"
   )
   default TagBuilder removeTag(ResourceLocation tagID, String source) {
      return this.remove(TagEntry.tag(tagID), source);
   }

   default TagBuilder removeTag(ResourceLocation tagID) {
      return this.getRawBuilder().remove(TagEntry.tag(tagID));
   }
}
