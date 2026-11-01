package net.neoforged.neoforge.network;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.network.payload.ConfigFilePayload;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class ConfigSync {
   private ConfigSync() {
   }

   public static List<ConfigFilePayload> syncConfigs() {
      Map<String, byte[]> configData = ModConfigs.getConfigSet(Type.SERVER).stream().collect(Collectors.toMap(ModConfig::getFileName, mc -> {
         try {
            return Files.readAllBytes(mc.getFullPath());
         } catch (IOException var2) {
            throw new RuntimeException(var2);
         }
      }));
      return configData.entrySet().stream().map(e -> new ConfigFilePayload(e.getKey(), e.getValue())).toList();
   }

   public static void receiveSyncedConfig(byte[] contents, String fileName) {
      if (!Minecraft.getInstance().isLocalServer()) {
         Optional.ofNullable((ModConfig)ModConfigs.getFileMap().get(fileName)).ifPresent(mc -> ConfigTracker.acceptSyncedConfig((ModConfig)mc, contents));
      }
   }
}
