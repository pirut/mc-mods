package net.neoforged.neoforge.common.util.flag;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.flag.FeatureFlagRegistry;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.jarcontents.JarResource;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class FeatureFlagLoader {
   private static final Gson GSON = new Gson();

   private FeatureFlagLoader() {
   }

   public static void loadModdedFlags(FeatureFlagRegistry.Builder builder) {
      Map<IModInfo, JarResource> pathPerMod = new HashMap<>();
      FMLLoader.getCurrent()
         .getLoadingModList()
         .getModFiles()
         .stream()
         .map(ModFileInfo::getMods)
         .flatMap(Collection::stream)
         .forEach(mod -> mod.getConfig().getConfigElement(new String[]{"featureFlags"}).ifPresent(file -> {
            JarResource resource = mod.getOwningFile().getFile().getContents().get(file);
            if (resource == null) {
               ModLoader.addLoadingIssue(ModLoadingIssue.error("fml.modloadingissue.feature_flags.file_not_found", new Object[]{file}).withAffectedMod(mod));
            } else {
               pathPerMod.put(mod, resource);
            }
         }));
      pathPerMod.forEach(
         (mod, resource) -> {
            try (BufferedReader reader = resource.bufferedReader()) {
               JsonObject obj = (JsonObject)GSON.fromJson(reader, JsonObject.class);

               for (JsonElement elem : GsonHelper.getAsJsonArray(obj, "flags")) {
                  String flagName = GsonHelper.convertToString(elem, "flag");
                  Identifier flagLocation = Identifier.parse(flagName);
                  Preconditions.checkArgument(
                     flagLocation.getNamespace().equals(mod.getModId()), "Cannot add new flags to foreign namespaces: %s", flagLocation
                  );
                  builder.create(flagLocation, true);
               }
            } catch (Throwable var12) {
               ModLoader.addLoadingIssue(
                  ModLoadingIssue.error("fml.modloadingissue.feature_flags.loading_error", new Object[]{resource}).withAffectedMod(mod).withCause(var12)
               );
            }
         }
      );
   }
}
