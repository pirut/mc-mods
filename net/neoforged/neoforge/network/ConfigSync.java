package net.neoforged.neoforge.network;

import com.electronwill.nightconfig.core.ConfigFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.fml.config.IConfigSpec.ILoadedConfig;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.config.ModConfigEvent.Reloading;
import net.neoforged.neoforge.network.payload.ConfigFilePayload;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class ConfigSync {
   private static final Object lock = new Object();
   private static final Map<Connection, Map<String, byte[]>> configsToSync = new WeakHashMap<>();

   private ConfigSync() {
   }

   public static void syncAllConfigs(ServerConfigurationPacketListener listener) {
      Connection connection = listener.getConnection();
      if (!connection.isMemoryConnection()) {
         synchronized (lock) {
            configsToSync.put(connection, new LinkedHashMap<>());
         }

         Map<String, byte[]> configData = ModConfigs.getConfigSet(Type.SERVER).stream().collect(Collectors.toMap(ModConfig::getFileName, mc -> {
            try {
               return Files.readAllBytes(mc.getFullPath());
            } catch (IOException var2) {
               throw new RuntimeException(var2);
            }
         }));

         for (Entry<String, byte[]> entry : configData.entrySet()) {
            listener.send(new ConfigFilePayload(entry.getKey(), entry.getValue()));
         }
      }
   }

   public static void registerEventListeners() {
      for (ModContainer modContainer : ModList.get().getSortedMods()) {
         IEventBus modBus = modContainer.getEventBus();
         if (modBus != null) {
            modBus.addListener(Reloading.class, event -> {
               ModConfig config = event.getConfig();
               if (config.getType() == Type.SERVER) {
                  ILoadedConfig loadedConfig = config.getLoadedConfig();
                  if (loadedConfig != null) {
                     ConfigFormat<?> configFormat = loadedConfig.config().configFormat();
                     if (!configFormat.isInMemory()) {
                        byte[] bytes = configFormat.createWriter().writeToString(loadedConfig.config()).getBytes(StandardCharsets.UTF_8);
                        synchronized (lock) {
                           for (Map<String, byte[]> toSync : configsToSync.values()) {
                              toSync.put(config.getFileName(), bytes);
                           }
                        }
                     }
                  }
               }
            });
         }
      }
   }

   public static void syncPendingConfigs(MinecraftServer server) {
      synchronized (lock) {
         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.connection.hasChannel(ConfigFilePayload.TYPE) && !player.connection.getConnection().isMemoryConnection()) {
               Map<String, byte[]> toSync = configsToSync.get(player.connection.getConnection());
               if (toSync == null) {
                  if (player.getClass() == ServerPlayer.class) {
                     throw new IllegalStateException("configsToSync should contain an entry for player " + player.getName());
                  }
               } else {
                  toSync.forEach((fileName, data) -> PacketDistributor.sendToPlayer(player, new ConfigFilePayload(fileName, data)));
                  toSync.clear();
               }
            }
         }
      }
   }

   public static void receiveSyncedConfig(byte[] contents, String fileName) {
      Optional.ofNullable((ModConfig)ModConfigs.getFileMap().get(fileName)).ifPresent(mc -> ConfigTracker.acceptSyncedConfig((ModConfig)mc, contents));
   }
}
