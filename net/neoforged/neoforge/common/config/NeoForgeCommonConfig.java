package net.neoforged.neoforge.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.TagConventionLogWarning;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class NeoForgeCommonConfig {
   @Internal
   public static final ModConfigSpec SPEC;
   public static final NeoForgeCommonConfig INSTANCE;
   public final ModConfigSpec.EnumValue<TagConventionLogWarning.LogWarningMode> logUntranslatedItemTagWarnings;
   public final ModConfigSpec.EnumValue<TagConventionLogWarning.LogWarningMode> logLegacyTagWarnings;
   public final ModConfigSpec.BooleanValue attributeAdvancedTooltipDebugInfo;
   public final ModConfigSpec.IntValue splitPacketMaxSizeLimitMb;
   public final ModConfigSpec.IntValue maxAdhocChannelLimit;

   private NeoForgeCommonConfig(ModConfigSpec.Builder builder) {
      this.logUntranslatedItemTagWarnings = builder.comment(
            "A config option mainly for developers. Logs out modded item tags that do not have translations when running on integrated server. Format desired is tag.item.<namespace>.<path> for the translation key. Defaults to SILENCED."
         )
         .translation("neoforge.configgui.logUntranslatedItemTagWarnings")
         .defineEnum("logUntranslatedItemTagWarnings", TagConventionLogWarning.LogWarningMode.SILENCED);
      this.logLegacyTagWarnings = builder.comment(
            "A config option mainly for developers. Logs out modded tags that are using the 'forge' namespace when running on integrated server. Defaults to DEV_SHORT."
         )
         .translation("neoforge.configgui.logLegacyTagWarnings")
         .defineEnum("logLegacyTagWarnings", TagConventionLogWarning.LogWarningMode.DEV_SHORT);
      this.attributeAdvancedTooltipDebugInfo = builder.comment(
            "Set this to true to enable showing debug information about attributes on an item when advanced tooltips is on."
         )
         .translation("neoforge.configgui.attributeAdvancedTooltipDebugInfo")
         .define("attributeAdvancedTooltipDebugInfo", true);
      this.splitPacketMaxSizeLimitMb = builder.comment("The maximum allowed size of a reassembled split packet; expressed in MBs.")
         .translation("neoforge.configgui.splitPacketMaxSizeLimitMb")
         .defineInRange("splitPacketMaxSizeLimitMb", 128, 1, 2048);
      this.maxAdhocChannelLimit = builder.comment("The maximum number of adhoc channels allowed to be registered.")
         .translation("neoforge.configgui.maxAdhocChannelLimit")
         .defineInRange("maxAdhocChannelLimit", 2048, 1024, 16384);
   }

   static {
      Pair<NeoForgeCommonConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(NeoForgeCommonConfig::new);
      SPEC = (ModConfigSpec)specPair.getRight();
      INSTANCE = (NeoForgeCommonConfig)specPair.getLeft();
   }
}
