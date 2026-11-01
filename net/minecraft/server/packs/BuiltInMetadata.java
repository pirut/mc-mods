package net.minecraft.server.packs;

import java.util.Map;
import net.minecraft.server.packs.metadata.MetadataSectionType;

public class BuiltInMetadata {
   private static final BuiltInMetadata EMPTY = new BuiltInMetadata(Map.of());
   private final Map<MetadataSectionType<?>, ?> values;

   private BuiltInMetadata(Map<MetadataSectionType<?>, ?> p_251588_) {
      this.values = p_251588_;
   }

   public <T> T get(MetadataSectionType<T> p_389627_) {
      return (T)this.values.get(p_389627_);
   }

   public static BuiltInMetadata of() {
      return EMPTY;
   }

   public static <T> BuiltInMetadata of(MetadataSectionType<T> p_389597_, T p_249997_) {
      return new BuiltInMetadata(Map.of(p_389597_, p_249997_));
   }

   public static <T1, T2> BuiltInMetadata of(MetadataSectionType<T1> p_389690_, T1 p_252174_, MetadataSectionType<T2> p_389633_, T2 p_250020_) {
      return new BuiltInMetadata(Map.of(p_389690_, p_252174_, p_389633_, (T1)p_250020_));
   }
}
