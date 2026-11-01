package net.minecraft.server.packs.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.GsonHelper;

public interface ResourceMetadata {
   ResourceMetadata EMPTY = new ResourceMetadata() {
      @Override
      public <T> Optional<T> getSection(MetadataSectionType<T> p_389466_) {
         return Optional.empty();
      }
   };
   IoSupplier<ResourceMetadata> EMPTY_SUPPLIER = () -> EMPTY;

   static ResourceMetadata fromJsonStream(InputStream p_215581_) throws IOException {
      ResourceMetadata resourcemetadata;
      try (BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(p_215581_, StandardCharsets.UTF_8))) {
         final JsonObject jsonobject = GsonHelper.parse(bufferedreader);
         resourcemetadata = new ResourceMetadata() {
            @Override
            public <T> Optional<T> getSection(MetadataSectionType<T> p_389450_) {
               String s = p_389450_.name();
               if (jsonobject.has(s)) {
                  T t = (T)p_389450_.codec().parse(JsonOps.INSTANCE, jsonobject.get(s)).getOrThrow(JsonParseException::new);
                  return Optional.of(t);
               } else {
                  return Optional.empty();
               }
            }
         };
      }

      return resourcemetadata;
   }

   <T> Optional<T> getSection(MetadataSectionType<T> var1);

   default <T> Optional<MetadataSectionType.WithValue<T>> getTypedSection(MetadataSectionType<T> p_432986_) {
      return this.getSection(p_432986_).map(p_432986_::withValue);
   }

   default List<MetadataSectionType.WithValue<?>> getTypedSections(Collection<MetadataSectionType<?>> p_433914_) {
      return p_433914_.stream().map(this::getTypedSection).flatMap(Optional::stream).collect(Collectors.toUnmodifiableList());
   }
}
