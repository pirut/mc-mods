package net.minecraft.data.tags;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;

public interface TagAppender<E, T> {
   TagAppender<E, T> add(E block);

   default TagAppender<E, T> add(E... elements) {
      return this.addAll(Arrays.stream(elements));
   }

   default TagAppender<E, T> addAll(Collection<E> elements) {
      elements.forEach(this::add);
      return this;
   }

   default TagAppender<E, T> addAll(Stream<E> stream) {
      stream.forEach(this::add);
      return this;
   }

   TagAppender<E, T> addOptional(E block);

   TagAppender<E, T> addTag(TagKey<T> tag);

   TagAppender<E, T> addOptionalTag(TagKey<T> tag);

   static <T> TagAppender<ResourceKey<T>, T> forBuilder(TagBuilder builder) {
      return new TagAppender<ResourceKey<T>, T>() {
         public TagAppender<ResourceKey<T>, T> add(ResourceKey<T> resourceKey) {
            builder.addElement(resourceKey.identifier());
            return this;
         }

         public TagAppender<ResourceKey<T>, T> addOptional(ResourceKey<T> resourceKey) {
            builder.addOptionalElement(resourceKey.identifier());
            return this;
         }

         @Override
         public TagAppender<ResourceKey<T>, T> addTag(TagKey<T> tag) {
            builder.addTag(tag.location());
            return this;
         }

         @Override
         public TagAppender<ResourceKey<T>, T> addOptionalTag(TagKey<T> tag) {
            builder.addOptionalTag(tag.location());
            return this;
         }
      };
   }

   default <U> TagAppender<U, T> map(Function<U, E> mapper) {
      final TagAppender<E, T> tagAppender = this;
      return new TagAppender<U, T>() {
         @Override
         public TagAppender<U, T> add(U block) {
            tagAppender.add(mapper.apply(block));
            return this;
         }

         @Override
         public TagAppender<U, T> addOptional(U block) {
            tagAppender.add(mapper.apply(block));
            return this;
         }

         @Override
         public TagAppender<U, T> addTag(TagKey<T> tag) {
            tagAppender.addTag(tag);
            return this;
         }

         @Override
         public TagAppender<U, T> addOptionalTag(TagKey<T> tag) {
            tagAppender.addOptionalTag(tag);
            return this;
         }
      };
   }
}
