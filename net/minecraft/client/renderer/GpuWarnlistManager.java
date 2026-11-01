package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class GpuWarnlistManager extends SimplePreparableReloadListener<GpuWarnlistManager.Preparations> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Identifier GPU_WARNLIST_LOCATION = Identifier.withDefaultNamespace("gpu_warnlist.json");
   private ImmutableMap<String, String> warnings = ImmutableMap.of();
   private boolean showWarning;
   private boolean warningDismissed;

   public boolean hasWarnings() {
      return !this.warnings.isEmpty();
   }

   public boolean willShowWarning() {
      return this.hasWarnings() && !this.warningDismissed;
   }

   public void showWarning() {
      this.showWarning = true;
   }

   public void dismissWarning() {
      this.warningDismissed = true;
   }

   public boolean isShowingWarning() {
      return this.showWarning && !this.warningDismissed;
   }

   public void resetWarnings() {
      this.showWarning = false;
      this.warningDismissed = false;
   }

   @Nullable
   public String getRendererWarnings() {
      return (String)this.warnings.get("renderer");
   }

   @Nullable
   public String getVersionWarnings() {
      return (String)this.warnings.get("version");
   }

   @Nullable
   public String getVendorWarnings() {
      return (String)this.warnings.get("vendor");
   }

   @Nullable
   public String getAllWarnings() {
      StringBuilder stringBuilder = new StringBuilder();
      this.warnings.forEach((string, string2) -> stringBuilder.append(string).append(": ").append(string2));
      return stringBuilder.isEmpty() ? null : stringBuilder.toString();
   }

   protected GpuWarnlistManager.Preparations prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
      List<Pattern> list = Lists.newArrayList();
      List<Pattern> list2 = Lists.newArrayList();
      List<Pattern> list3 = Lists.newArrayList();
      JsonObject jsonObject = parseJson(resourceManager, profiler);
      if (jsonObject != null) {
         try (Zone zone = profiler.zone("compile_regex")) {
            compilePatterns(jsonObject.getAsJsonArray("renderer"), list);
            compilePatterns(jsonObject.getAsJsonArray("version"), list2);
            compilePatterns(jsonObject.getAsJsonArray("vendor"), list3);
         }
      }

      return new GpuWarnlistManager.Preparations(list, list2, list3);
   }

   protected void apply(GpuWarnlistManager.Preparations object, ResourceManager resourceManager, ProfilerFiller profiler) {
      this.warnings = object.apply();
   }

   private static void compilePatterns(JsonArray jsonArray, List<Pattern> patterns) {
      jsonArray.forEach(jsonElement -> patterns.add(Pattern.compile(jsonElement.getAsString(), 2)));
   }

   @Nullable
   private static JsonObject parseJson(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
      try {
         JsonObject var4;
         try (
            Zone zone = profilerFiller.zone("parse_json");
            Reader reader = resourceManager.openAsReader(GPU_WARNLIST_LOCATION);
         ) {
            var4 = StrictJsonParser.parse(reader).getAsJsonObject();
         }

         return var4;
      } catch (JsonSyntaxException | IOException var10) {
         LOGGER.warn("Failed to load GPU warnlist", var10);
         return null;
      }
   }

   @Environment(EnvType.CLIENT)
   protected static final class Preparations {
      private final List<Pattern> rendererPatterns;
      private final List<Pattern> versionPatterns;
      private final List<Pattern> vendorPatterns;

      Preparations(List<Pattern> rendererPatterns, List<Pattern> versionPatterns, List<Pattern> vendorPatterns) {
         this.rendererPatterns = rendererPatterns;
         this.versionPatterns = versionPatterns;
         this.vendorPatterns = vendorPatterns;
      }

      private static String matchAny(List<Pattern> patterns, String string) {
         List<String> list = Lists.newArrayList();

         for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(string);

            while (matcher.find()) {
               list.add(matcher.group());
            }
         }

         return String.join(", ", list);
      }

      ImmutableMap<String, String> apply() {
         Builder<String, String> builder = new Builder();
         GpuDevice gpuDevice = RenderSystem.getDevice();
         if (gpuDevice.getBackendName().equals("OpenGL")) {
            String string = matchAny(this.rendererPatterns, gpuDevice.getRenderer());
            if (!string.isEmpty()) {
               builder.put("renderer", string);
            }

            String string2 = matchAny(this.versionPatterns, gpuDevice.getVersion());
            if (!string2.isEmpty()) {
               builder.put("version", string2);
            }

            String string3 = matchAny(this.vendorPatterns, gpuDevice.getVendor());
            if (!string3.isEmpty()) {
               builder.put("vendor", string3);
            }
         }

         return builder.build();
      }
   }
}
