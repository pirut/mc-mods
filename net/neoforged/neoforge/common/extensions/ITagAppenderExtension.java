package net.neoforged.neoforge.common.extensions;

import net.minecraft.data.tags.TagAppender;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;

public interface ITagAppenderExtension<E, T> {
   private TagAppender<E, T> self() {
      return (TagAppender<E, T>)this;
   }

   default TagAppender<E, T> addTags(TagKey<T>... values) {
      TagAppender<E, T> appender = this.self();

      for (TagKey<T> value : values) {
         appender.addTag(value);
      }

      return appender;
   }

   default TagAppender<E, T> addOptionalTags(TagKey<T>... values) {
      TagAppender<E, T> appender = this.self();

      for (TagKey<T> value : values) {
         appender.addOptionalTag(value);
      }

      return appender;
   }

   TagAppender<E, T> add(TagEntry var1);

   default TagAppender<E, T> replace() {
      return this.replace(true);
   }

   TagAppender<E, T> replace(boolean var1);

   TagAppender<E, T> remove(E var1);

   default TagAppender<E, T> remove(E firstE, E... es) {
      this.remove(firstE);

      for (E e : es) {
         this.remove(e);
      }

      return this.self();
   }

   TagAppender<E, T> remove(TagKey<T> var1);

   default TagAppender<E, T> remove(TagKey<T> first, TagKey<T>... tags) {
      this.remove(first);

      for (TagKey<T> tag : tags) {
         this.remove(tag);
      }

      return this.self();
   }
}
