package net.minecraft.server.packs.metadata;

import com.mojang.serialization.Codec;
import java.util.Optional;

public record MetadataSectionType<T>(String name, Codec<T> codec) {
   public MetadataSectionType.WithValue<T> withValue(T p_434503_) {
      return new MetadataSectionType.WithValue<>(this, p_434503_);
   }

   public record WithValue<T>(MetadataSectionType<T> type, T value) {
      public <U> Optional<U> unwrapToType(MetadataSectionType<U> p_432815_) {
         return p_432815_ == this.type ? Optional.of(this.value) : Optional.empty();
      }
   }
}
