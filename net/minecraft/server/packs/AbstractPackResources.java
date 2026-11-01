package net.minecraft.server.packs;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class AbstractPackResources implements PackResources {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PackLocationInfo location;

   protected AbstractPackResources(PackLocationInfo location) {
      this.location = location;
   }

   @Nullable
   @Override
   public <T> T getMetadataSection(MetadataSectionType<T> type) throws IOException {
      IoSupplier<InputStream> ioSupplier = this.getRootResource(new String[]{"pack.mcmeta"});
      if (ioSupplier == null) {
         return null;
      } else {
         Object var4;
         try (InputStream inputStream = ioSupplier.get()) {
            var4 = getMetadataFromStream(type, inputStream, this.location);
         }

         return (T)var4;
      }
   }

   @Nullable
   public static <T> T getMetadataFromStream(MetadataSectionType<T> type, InputStream stream, PackLocationInfo location) {
      JsonObject jsonObject;
      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
         jsonObject = GsonHelper.parse(bufferedReader);
      } catch (Exception var9) {
         LOGGER.error("Couldn't load {} {} metadata: {}", new Object[]{location.id(), type.name(), var9.getMessage()});
         return null;
      }

      return (T)(!jsonObject.has(type.name())
         ? null
         : type.codec()
            .parse(JsonOps.INSTANCE, jsonObject.get(type.name()))
            .ifError(error -> LOGGER.error("Couldn't load {} {} metadata: {}", new Object[]{location.id(), type.name(), error.message()}))
            .result()
            .orElse(null));
   }

   @Override
   public PackLocationInfo location() {
      return this.location;
   }
}
