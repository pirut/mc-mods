package net.minecraft.server.packs.resources;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

@FunctionalInterface
public interface ResourceProvider {
   ResourceProvider EMPTY = p_467319_ -> Optional.empty();

   Optional<Resource> getResource(Identifier var1);

   default Resource getResourceOrThrow(Identifier p_467396_) throws FileNotFoundException {
      return this.getResource(p_467396_).orElseThrow(() -> new FileNotFoundException(p_467396_.toString()));
   }

   default InputStream open(Identifier p_468703_) throws IOException {
      return this.getResourceOrThrow(p_468703_).open();
   }

   default BufferedReader openAsReader(Identifier p_468916_) throws IOException {
      return this.getResourceOrThrow(p_468916_).openAsReader();
   }

   static ResourceProvider fromMap(Map<Identifier, Resource> p_251819_) {
      return p_467898_ -> Optional.ofNullable(p_251819_.get(p_467898_));
   }
}
