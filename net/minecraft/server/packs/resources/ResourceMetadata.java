package net.minecraft.server.packs.resources;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;

public interface ResourceMetadata {
   ResourceMetadata EMPTY = new ResourceMetadata() {
      @Override
      public <T> Optional<T> getSection(MetadataSectionSerializer<T> p_215584_) {
         return Optional.empty();
      }
   };
   IoSupplier<ResourceMetadata> EMPTY_SUPPLIER = () -> EMPTY;

   static ResourceMetadata fromJsonStream(InputStream p_215581_) throws IOException {
      ResourceMetadata var3;
      try (BufferedReader $$1 = new BufferedReader(new InputStreamReader(p_215581_, StandardCharsets.UTF_8))) {
         final JsonObject $$2 = GsonHelper.parse($$1);
         var3 = new ResourceMetadata() {
            @Override
            public <T> Optional<T> getSection(MetadataSectionSerializer<T> p_215589_) {
               String $$1x = p_215589_.getMetadataSectionName();
               return $$2.has($$1x) ? Optional.of(p_215589_.fromJson(GsonHelper.getAsJsonObject($$2, $$1x))) : Optional.empty();
            }
         };
      }

      return var3;
   }

   <T> Optional<T> getSection(MetadataSectionSerializer<T> var1);

   default ResourceMetadata copySections(Collection<MetadataSectionSerializer<?>> p_295308_) {
      ResourceMetadata.Builder $$1 = new ResourceMetadata.Builder();

      for (MetadataSectionSerializer<?> $$2 : p_295308_) {
         this.copySection($$1, $$2);
      }

      return $$1.build();
   }

   private <T> void copySection(ResourceMetadata.Builder p_295962_, MetadataSectionSerializer<T> p_294347_) {
      this.getSection(p_294347_).ifPresent(p_293816_ -> p_295962_.put(p_294347_, (T)p_293816_));
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableMap.Builder<MetadataSectionSerializer<?>, Object> map = ImmutableMap.builder();

      public <T> ResourceMetadata.Builder put(MetadataSectionSerializer<T> p_294292_, T p_294133_) {
         this.map.put(p_294292_, p_294133_);
         return this;
      }

      public ResourceMetadata build() {
         final ImmutableMap<MetadataSectionSerializer<?>, Object> $$0 = this.map.build();
         return $$0.isEmpty() ? ResourceMetadata.EMPTY : new ResourceMetadata() {
            @Override
            public <T> Optional<T> getSection(MetadataSectionSerializer<T> p_295383_) {
               return Optional.ofNullable((T)$$0.get(p_295383_));
            }
         };
      }
   }
}
