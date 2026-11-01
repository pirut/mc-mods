package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.GlUtil;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GpuWarnlistManager extends SimplePreparableReloadListener<GpuWarnlistManager.Preparations> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation GPU_WARNLIST_LOCATION = ResourceLocation.withDefaultNamespace("gpu_warnlist.json");
   private ImmutableMap<String, String> warnings = ImmutableMap.of();
   private boolean showWarning;
   private boolean warningDismissed;
   private boolean skipFabulous;

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

   public void dismissWarningAndSkipFabulous() {
      this.warningDismissed = true;
      this.skipFabulous = true;
   }

   public boolean isShowingWarning() {
      return this.showWarning && !this.warningDismissed;
   }

   public boolean isSkippingFabulous() {
      return this.skipFabulous;
   }

   public void resetWarnings() {
      this.showWarning = false;
      this.warningDismissed = false;
      this.skipFabulous = false;
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
      StringBuilder $$0 = new StringBuilder();
      this.warnings.forEach((p_109235_, p_109236_) -> $$0.append(p_109235_).append(": ").append(p_109236_));
      return $$0.length() == 0 ? null : $$0.toString();
   }

   protected GpuWarnlistManager.Preparations prepare(ResourceManager p_109220_, ProfilerFiller p_109221_) {
      List<Pattern> $$2 = Lists.newArrayList();
      List<Pattern> $$3 = Lists.newArrayList();
      List<Pattern> $$4 = Lists.newArrayList();
      p_109221_.startTick();
      JsonObject $$5 = parseJson(p_109220_, p_109221_);
      if ($$5 != null) {
         p_109221_.push("compile_regex");
         compilePatterns($$5.getAsJsonArray("renderer"), $$2);
         compilePatterns($$5.getAsJsonArray("version"), $$3);
         compilePatterns($$5.getAsJsonArray("vendor"), $$4);
         p_109221_.pop();
      }

      p_109221_.endTick();
      return new GpuWarnlistManager.Preparations($$2, $$3, $$4);
   }

   protected void apply(GpuWarnlistManager.Preparations p_109226_, ResourceManager p_109227_, ProfilerFiller p_109228_) {
      this.warnings = p_109226_.apply();
   }

   private static void compilePatterns(JsonArray p_109223_, List<Pattern> p_109224_) {
      p_109223_.forEach(p_109239_ -> p_109224_.add(Pattern.compile(p_109239_.getAsString(), 2)));
   }

   @Nullable
   private static JsonObject parseJson(ResourceManager p_109245_, ProfilerFiller p_109246_) {
      p_109246_.push("parse_json");
      JsonObject $$2 = null;

      try (Reader $$3 = p_109245_.openAsReader(GPU_WARNLIST_LOCATION)) {
         $$2 = JsonParser.parseReader($$3).getAsJsonObject();
      } catch (JsonSyntaxException | IOException var8) {
         LOGGER.warn("Failed to load GPU warnlist");
      }

      p_109246_.pop();
      return $$2;
   }

   @OnlyIn(Dist.CLIENT)
   protected static final class Preparations {
      private final List<Pattern> rendererPatterns;
      private final List<Pattern> versionPatterns;
      private final List<Pattern> vendorPatterns;

      Preparations(List<Pattern> p_109261_, List<Pattern> p_109262_, List<Pattern> p_109263_) {
         this.rendererPatterns = p_109261_;
         this.versionPatterns = p_109262_;
         this.vendorPatterns = p_109263_;
      }

      private static String matchAny(List<Pattern> p_109273_, String p_109274_) {
         List<String> $$2 = Lists.newArrayList();

         for (Pattern $$3 : p_109273_) {
            Matcher $$4 = $$3.matcher(p_109274_);

            while ($$4.find()) {
               $$2.add($$4.group());
            }
         }

         return String.join(", ", $$2);
      }

      ImmutableMap<String, String> apply() {
         Builder<String, String> $$0 = new Builder();
         String $$1 = matchAny(this.rendererPatterns, GlUtil.getRenderer());
         if (!$$1.isEmpty()) {
            $$0.put("renderer", $$1);
         }

         String $$2 = matchAny(this.versionPatterns, GlUtil.getOpenGLVersion());
         if (!$$2.isEmpty()) {
            $$0.put("version", $$2);
         }

         String $$3 = matchAny(this.vendorPatterns, GlUtil.getVendor());
         if (!$$3.isEmpty()) {
            $$0.put("vendor", $$3);
         }

         return $$0.build();
      }
   }
}
