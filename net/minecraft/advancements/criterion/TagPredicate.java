package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public record TagPredicate<T>(TagKey<T> tag, boolean expected) {
   public static <T> Codec<TagPredicate<T>> codec(ResourceKey<? extends Registry<T>> p_468825_) {
      return RecordCodecBuilder.create(
         p_468249_ -> p_468249_.group(
               TagKey.codec(p_468825_).fieldOf("id").forGetter(TagPredicate::tag), Codec.BOOL.fieldOf("expected").forGetter(TagPredicate::expected)
            )
            .apply(p_468249_, TagPredicate::new)
      );
   }

   public static <T> TagPredicate<T> is(TagKey<T> p_467808_) {
      return new TagPredicate<>(p_467808_, true);
   }

   public static <T> TagPredicate<T> isNot(TagKey<T> p_467477_) {
      return new TagPredicate<>(p_467477_, false);
   }

   public boolean matches(Holder<T> p_466990_) {
      return p_466990_.is(this.tag) == this.expected;
   }
}
