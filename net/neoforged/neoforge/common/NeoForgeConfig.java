package net.neoforged.neoforge.common;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.Logging;
import net.neoforged.fml.event.config.ModConfigEvent.Loading;
import net.neoforged.fml.event.config.ModConfigEvent.Reloading;
import net.neoforged.neoforge.client.ClientHooks;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;

public class NeoForgeConfig {
   static final ModConfigSpec clientSpec;
   public static final NeoForgeConfig.Client CLIENT;
   static final ModConfigSpec commonSpec;
   public static final NeoForgeConfig.Common COMMON;
   static final ModConfigSpec serverSpec;
   public static final NeoForgeConfig.Server SERVER;

   @SubscribeEvent
   public static void onLoad(Loading configEvent) {
      LogManager.getLogger().debug(Logging.FORGEMOD, "Loaded NeoForge config file {}", configEvent.getConfig().getFileName());
      if (configEvent.getConfig().getSpec() == clientSpec) {
         CLIENT.experimentalPipelineActive = CLIENT.experimentalForgeLightPipelineEnabled.getAsBoolean();
      }
   }

   @SubscribeEvent
   public static void onFileChange(Reloading configEvent) {
      LogManager.getLogger().debug(Logging.FORGEMOD, "NeoForge config just got changed on the file system!");
      if (configEvent.getConfig().getSpec() == clientSpec) {
         boolean experimentalPipelineActive = CLIENT.experimentalForgeLightPipelineEnabled.getAsBoolean();
         if (experimentalPipelineActive != CLIENT.experimentalPipelineActive) {
            CLIENT.experimentalPipelineActive = experimentalPipelineActive;
            ClientHooks.reloadRenderer();
         }
      }
   }

   static {
      Pair<NeoForgeConfig.Client, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(NeoForgeConfig.Client::new);
      clientSpec = (ModConfigSpec)specPair.getRight();
      CLIENT = (NeoForgeConfig.Client)specPair.getLeft();
      specPair = new ModConfigSpec.Builder().configure(NeoForgeConfig.Common::new);
      commonSpec = (ModConfigSpec)specPair.getRight();
      COMMON = (NeoForgeConfig.Common)specPair.getLeft();
      specPair = new ModConfigSpec.Builder().configure(NeoForgeConfig.Server::new);
      serverSpec = (ModConfigSpec)specPair.getRight();
      SERVER = (NeoForgeConfig.Server)specPair.getLeft();
   }

   public static class Client {
      public final ModConfigSpec.BooleanValue experimentalForgeLightPipelineEnabled;
      boolean experimentalPipelineActive;
      public final ModConfigSpec.BooleanValue showLoadWarnings;
      public final ModConfigSpec.BooleanValue useCombinedDepthStencilAttachment;
      public final ModConfigSpec.BooleanValue logUntranslatedConfigurationWarnings;

      Client(ModConfigSpec.Builder builder) {
         this.experimentalForgeLightPipelineEnabled = builder.comment(
               "EXPERIMENTAL: Enable the NeoForge block rendering pipeline - fixes the lighting of custom models."
            )
            .translation("neoforge.configgui.forgeLightPipelineEnabled")
            .define("experimentalForgeLightPipelineEnabled", false);
         this.showLoadWarnings = builder.comment("When enabled, NeoForge will show any warnings that occurred during loading.")
            .translation("neoforge.configgui.showLoadWarnings")
            .define("showLoadWarnings", true);
         this.useCombinedDepthStencilAttachment = builder.comment("Set to true to use a combined DEPTH_STENCIL attachment instead of two separate ones.")
            .translation("neoforge.configgui.useCombinedDepthStencilAttachment")
            .define("useCombinedDepthStencilAttachment", false);
         this.logUntranslatedConfigurationWarnings = builder.comment(
               "A config option mainly for developers. Logs out configuration values that do not have translations when running a client in a development environment."
            )
            .translation("neoforge.configgui.logUntranslatedConfigurationWarnings")
            .define("logUntranslatedConfigurationWarnings", true);
      }
   }

   public static class Common {
      public final ModConfigSpec.EnumValue<TagConventionLogWarning.LogWarningMode> logUntranslatedItemTagWarnings;
      public final ModConfigSpec.EnumValue<TagConventionLogWarning.LogWarningMode> logLegacyTagWarnings;
      public final ModConfigSpec.BooleanValue attributeAdvancedTooltipDebugInfo;

      Common(ModConfigSpec.Builder builder) {
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
      }
   }

   public static class Server {
      public final ModConfigSpec.BooleanValue removeErroringBlockEntities;
      public final ModConfigSpec.BooleanValue removeErroringEntities;
      public final ModConfigSpec.BooleanValue fullBoundingBoxLadders;
      public final ModConfigSpec.ConfigValue<String> permissionHandler;
      public final ModConfigSpec.BooleanValue advertiseDedicatedServerToLan;

      Server(ModConfigSpec.Builder builder) {
         this.removeErroringBlockEntities = builder.comment(
               "Set this to true to remove any BlockEntity that throws an error in its update method instead of closing the server and reporting a crash log. BE WARNED THIS COULD SCREW UP EVERYTHING USE SPARINGLY WE ARE NOT RESPONSIBLE FOR DAMAGES."
            )
            .translation("neoforge.configgui.removeErroringBlockEntities")
            .worldRestart()
            .define("removeErroringBlockEntities", false);
         this.removeErroringEntities = builder.comment(
               "Set this to true to remove any Entity (Note: Does not include BlockEntities) that throws an error in its tick method instead of closing the server and reporting a crash log. BE WARNED THIS COULD SCREW UP EVERYTHING USE SPARINGLY WE ARE NOT RESPONSIBLE FOR DAMAGES."
            )
            .translation("neoforge.configgui.removeErroringEntities")
            .worldRestart()
            .define("removeErroringEntities", false);
         this.fullBoundingBoxLadders = builder.comment(
               "Set this to true to check the entire entity's collision bounding box for ladders instead of just the block they are in. Causes noticeable differences in mechanics so default is vanilla behavior. Default: false."
            )
            .translation("neoforge.configgui.fullBoundingBoxLadders")
            .worldRestart()
            .define("fullBoundingBoxLadders", false);
         this.permissionHandler = builder.comment(
               "The permission handler used by the server. Defaults to neoforge:default_handler if no such handler with that name is registered."
            )
            .translation("neoforge.configgui.permissionHandler")
            .define("permissionHandler", "neoforge:default_handler");
         this.advertiseDedicatedServerToLan = builder.comment(
               "Set this to true to enable advertising the dedicated server to local LAN clients so that it shows up in the Multiplayer screen automatically."
            )
            .translation("neoforge.configgui.advertiseDedicatedServerToLan")
            .define("advertiseDedicatedServerToLan", true);
      }
   }
}
