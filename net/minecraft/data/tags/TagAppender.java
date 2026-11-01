package net.minecraft.data.tags;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.extensions.ITagAppenderExtension;

public interface TagAppender<E, T> extends ITagAppenderExtension<E, T> {
   TagAppender<E, T> add(E var1);

   default TagAppender<E, T> add(E... p_422401_) {
      return this.addAll(Arrays.stream(p_422401_));
   }

   default TagAppender<E, T> addAll(Collection<E> p_421875_) {
      p_421875_.forEach(this::add);
      return this;
   }

   default TagAppender<E, T> addAll(Stream<E> p_421681_) {
      p_421681_.forEach(this::add);
      return this;
   }

   TagAppender<E, T> addOptional(E var1);

   TagAppender<E, T> addTag(TagKey<T> var1);

   TagAppender<E, T> addOptionalTag(TagKey<T> var1);

   static <T> TagAppender<ResourceKey<T>, T> forBuilder(final TagBuilder p_422107_) {
      return new TagAppender<ResourceKey<T>, T>() {
         public TagAppender<ResourceKey<T>, T> add(ResourceKey<T> p_421714_) {
            p_422107_.addElement(p_421714_.identifier());
            return this;
         }

         public TagAppender<ResourceKey<T>, T> addOptional(ResourceKey<T> p_422270_) {
            p_422107_.addOptionalElement(p_422270_.identifier());
            return this;
         }

         @Override
         public TagAppender<ResourceKey<T>, T> addTag(TagKey<T> p_422380_) {
            p_422107_.addTag(p_422380_.location());
            return this;
         }

         @Override
         public TagAppender<ResourceKey<T>, T> addOptionalTag(TagKey<T> p_422597_) {
            p_422107_.addOptionalTag(p_422597_.location());
            return this;
         }

         @Override
         public TagAppender<ResourceKey<T>, T> add(TagEntry entry) {
            p_422107_.add(entry);
            return this;
         }

         @Override
         public TagAppender<ResourceKey<T>, T> replace(boolean value) {
            p_422107_.replace(value);
            return this;
         }

         public TagAppender<ResourceKey<T>, T> remove(ResourceKey<T> resourceKey) {
            p_422107_.removeElement(resourceKey.identifier());
            return this;
         }

         @Override
         public TagAppender<ResourceKey<T>, T> remove(TagKey<T> tag) {
            p_422107_.removeTag(tag.location());
            return this;
         }
      };
   }

   default <U> TagAppender<U, T> map(final Function<U, E> p_421966_) {
      final TagAppender<E, T> tagappender = this;
      return new TagAppender<U, T>() {
         @Override
         public TagAppender<U, T> add(U p_422138_) {
            tagappender.add(p_421966_.apply(p_422138_));
            return this;
         }

         @Override
         public TagAppender<U, T> addOptional(U p_422325_) {
            tagappender.addOptional(p_421966_.apply(p_422325_));
            return this;
         }

         @Override
         public TagAppender<U, T> addTag(TagKey<T> p_421614_) {
            tagappender.addTag(p_421614_);
            return this;
         }

         @Override
         public TagAppender<U, T> addOptionalTag(TagKey<T> p_421878_) {
            tagappender.addOptionalTag(p_421878_);
            return this;
         }

         @Override
         public TagAppender<U, T> add(TagEntry entry) {
            tagappender.add(entry);
            return this;
         }

         @Override
         public TagAppender<U, T> replace(boolean value) {
            tagappender.replace(value);
            return this;
         }

         @Override
         public TagAppender<U, T> remove(U u) {
            tagappender.remove(p_421966_.apply(u));
            return this;
         }

         @Override
         public TagAppender<U, T> remove(TagKey<T> tag) {
            tagappender.remove(tag);
            return this;
         }
      };
   }
}
