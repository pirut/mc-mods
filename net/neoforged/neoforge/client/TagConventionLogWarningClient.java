package net.neoforged.neoforge.client;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeConfig;
import net.neoforged.neoforge.common.TagConventionLogWarning;
import net.neoforged.neoforge.common.Tags;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class TagConventionLogWarningClient {
   private static final Logger LOGGER = LogManager.getLogger();

   private TagConventionLogWarningClient() {
   }

   static void init() {
      IEventBus forgeBus = NeoForge.EVENT_BUS;
      setupUntranslatedItemTagWarning(forgeBus);
   }

   static void setupUntranslatedItemTagWarning(IEventBus forgeBus) {
      forgeBus.addListener(
         serverStartingEvent -> {
            TagConventionLogWarning.LogWarningMode untranslatedTagWarningMode = NeoForgeConfig.COMMON.logUntranslatedItemTagWarnings.get();
            if (FMLEnvironment.dist == Dist.CLIENT && untranslatedTagWarningMode != TagConventionLogWarning.LogWarningMode.SILENCED) {
               boolean isConfigSetToDev = untranslatedTagWarningMode == TagConventionLogWarning.LogWarningMode.DEV_SHORT
                  || untranslatedTagWarningMode == TagConventionLogWarning.LogWarningMode.DEV_VERBOSE;
               if (!FMLLoader.isProduction() == isConfigSetToDev) {
                  List<TagKey<?>> untranslatedTags = new ObjectArrayList();
                  RegistryAccess.Frozen registryAccess = serverStartingEvent.getServer().registryAccess();
                  extractUnregisteredModdedTags(registryAccess.registryOrThrow(Registries.ITEM), untranslatedTags);
                  if (!untranslatedTags.isEmpty()) {
                     StringBuilder stringBuilder = new StringBuilder();
                     stringBuilder.append(
                        "\n\tDev warning - Untranslated Item Tags detected. Please translate your item tags so other mods such as recipe viewers can properly display your tag's name.\n    The format desired is tag.item.<namespace>.<path> for the translation key with slashes in path turned into periods.\n    You can see a shortened version of this message by setting logUntranslatedItemTagWarnings in NeoForge's common config to \"DEV_SHORT\".\n    You can see individual untranslated item tags by setting logUntranslatedItemTagWarnings in NeoForge's common config to \"DEV_VERBOSE\".\n    Default is \"SILENCED\" to hide this message.\n"
                     );
                     boolean isConfigSetToVerbose = untranslatedTagWarningMode == TagConventionLogWarning.LogWarningMode.DEV_VERBOSE
                        || untranslatedTagWarningMode == TagConventionLogWarning.LogWarningMode.PROD_VERBOSE;
                     if (isConfigSetToVerbose) {
                        stringBuilder.append("\nUntranslated item tags:");

                        for (TagKey<?> tagKey : untranslatedTags) {
                           stringBuilder.append("\n     ").append(tagKey.location());
                        }
                     }

                     LOGGER.warn(stringBuilder);
                  }
               }
            }
         }
      );
   }

   private static void extractUnregisteredModdedTags(Registry<?> registry, List<TagKey<?>> untranslatedTags) {
      registry.getTagNames().forEach(itemTagKey -> {
         if (!itemTagKey.location().getNamespace().equals("minecraft")) {
            String translationKey = Tags.getTagTranslationKey((TagKey<?>)itemTagKey);
            if (!I18n.exists(translationKey)) {
               untranslatedTags.add((TagKey<?>)itemTagKey);
            }
         }
      });
   }
}
