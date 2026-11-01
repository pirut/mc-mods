package net.minecraft.server.packs;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class AbstractPackResources implements PackResources {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PackLocationInfo location;

   protected AbstractPackResources(PackLocationInfo p_326071_) {
      this.location = p_326071_;
   }

   @Nullable
   @Override
   public <T> T getMetadataSection(MetadataSectionType<T> p_389404_) throws IOException {
      IoSupplier<InputStream> iosupplier = this.getRootResource(new String[]{"pack.mcmeta"});
      if (iosupplier == null) {
         return null;
      } else {
         Object object;
         try (InputStream inputstream = iosupplier.get()) {
            object = getMetadataFromStream(p_389404_, inputstream, this.location);
         }

         return (T)object;
      }
   }

   @Nullable
   public static <T> T getMetadataFromStream(MetadataSectionType<T> p_389726_, InputStream p_10216_, PackLocationInfo p_434958_) {
      JsonObject jsonobject;
      try (BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(p_10216_, StandardCharsets.UTF_8))) {
         jsonobject = GsonHelper.parse(bufferedreader);
      } catch (Exception var9) {
         LOGGER.error("Couldn't load {} {} metadata: {}", new Object[]{p_434958_.id(), p_389726_.name(), var9.getMessage()});
         return null;
      }

      return (T)(!jsonobject.has(p_389726_.name())
         ? null
         : p_389726_.codec()
            .parse(JsonOps.INSTANCE, jsonobject.get(p_389726_.name()))
            .ifError(p_432469_ -> LOGGER.error("Couldn't load {} {} metadata: {}", new Object[]{p_434958_.id(), p_389726_.name(), p_432469_.message()}))
            .result()
            .orElse(null));
   }

   @Override
   public PackLocationInfo location() {
      return this.location;
   }

   @Override
   public String toString() {
      return String.format(Locale.ROOT, "%s: %s", this.getClass().getName(), this.location.id());
   }
}
