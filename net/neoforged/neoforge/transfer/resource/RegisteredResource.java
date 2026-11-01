package net.neoforged.neoforge.transfer.resource;

import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

public interface RegisteredResource<T> extends Resource {
   T value();

   Holder<T> getHolder();

   @NonExtendable
   default boolean is(TagKey<T> tag) {
      return this.getHolder().is(tag);
   }

   @NonExtendable
   default boolean is(T instance) {
      return instance == this.value();
   }

   @NonExtendable
   default boolean is(Predicate<Holder<T>> predicate) {
      return predicate.test(this.getHolder());
   }

   @NonExtendable
   default boolean is(Holder<T> holder) {
      return this.is(holder.value());
   }

   @NonExtendable
   default boolean is(HolderSet<T> holders) {
      return holders.contains(this.getHolder());
   }
}
