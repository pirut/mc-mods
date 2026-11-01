package net.neoforged.neoforge.common.extensions;

import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public interface IIntrinsicHolderTagAppenderExtension<T> extends ITagAppenderExtension<T> {
   private IntrinsicHolderTagsProvider.IntrinsicTagAppender<T> self() {
      return (IntrinsicHolderTagsProvider.IntrinsicTagAppender<T>)this;
   }

   ResourceKey<T> getKey(T var1);

   default IntrinsicHolderTagsProvider.IntrinsicTagAppender<T> remove(T entry) {
      return this.remove(this.getKey(entry));
   }

   default IntrinsicHolderTagsProvider.IntrinsicTagAppender<T> remove(T first, T... entries) {
      this.remove(first);

      for (T entry : entries) {
         this.remove(entry);
      }

      return this.self();
   }

   default IntrinsicHolderTagsProvider.IntrinsicTagAppender<T> addTags(TagKey<T>... values) {
      ITagAppenderExtension.super.addTags(values);
      return this.self();
   }

   default IntrinsicHolderTagsProvider.IntrinsicTagAppender<T> replace() {
      ITagAppenderExtension.super.replace();
      return this.self();
   }

   default IntrinsicHolderTagsProvider.IntrinsicTagAppender<T> replace(boolean value) {
      ITagAppenderExtension.super.replace(value);
      return this.self();
   }

   default IntrinsicHolderTagsProvider.IntrinsicTagAppender<T> remove(ResourceLocation location) {
      ITagAppenderExtension.super.remove(location);
      return this.self();
   }

   default IntrinsicHolderTagsProvider.IntrinsicTagAppender<T> remove(ResourceLocation first, ResourceLocation... locations) {
      ITagAppenderExtension.super.remove(first, locations);
      return this.self();
   }

   default IntrinsicHolderTagsProvider.IntrinsicTagAppender<T> remove(ResourceKey<T> resourceKey) {
      ITagAppenderExtension.super.remove(resourceKey);
      return this.self();
   }

   default IntrinsicHolderTagsProvider.IntrinsicTagAppender<T> remove(ResourceKey<T> firstResourceKey, ResourceKey<T>... resourceKeys) {
      ITagAppenderExtension.super.remove(firstResourceKey, resourceKeys);
      return this.self();
   }

   default IntrinsicHolderTagsProvider.IntrinsicTagAppender<T> remove(TagKey<T> tag) {
      ITagAppenderExtension.super.remove(tag);
      return this.self();
   }

   default IntrinsicHolderTagsProvider.IntrinsicTagAppender<T> remove(TagKey<T> first, TagKey<T>... tags) {
      ITagAppenderExtension.super.remove(first, tags);
      return this.self();
   }
}
