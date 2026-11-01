package net.minecraft.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

public class Cloner<T> {
   private final Codec<T> directCodec;

   Cloner(Codec<T> directCodec) {
      this.directCodec = directCodec;
   }

   public T clone(T object, HolderLookup.Provider lookupProvider1, HolderLookup.Provider lookupProvider2) {
      DynamicOps<Object> dynamicOps = lookupProvider1.createSerializationContext(JavaOps.INSTANCE);
      DynamicOps<Object> dynamicOps2 = lookupProvider2.createSerializationContext(JavaOps.INSTANCE);
      Object object2 = this.directCodec.encodeStart(dynamicOps, object).getOrThrow(string -> new IllegalStateException("Failed to encode: " + string));
      return (T)this.directCodec.parse(dynamicOps2, object2).getOrThrow(string -> new IllegalStateException("Failed to decode: " + string));
   }

   public static class Factory {
      private final Map<ResourceKey<? extends Registry<?>>, Cloner<?>> codecs = new HashMap<>();

      public <T> Cloner.Factory addCodec(ResourceKey<? extends Registry<? extends T>> registryKey, Codec<T> codec) {
         this.codecs.put(registryKey, new Cloner(codec));
         return this;
      }

      @Nullable
      public <T> Cloner<T> cloner(ResourceKey<? extends Registry<? extends T>> registryKey) {
         return (Cloner<T>)this.codecs.get(registryKey);
      }
   }
}
