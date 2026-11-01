package net.neoforged.neoforge.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.StructureModifier;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.mixins.MappedRegistryAccessor;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jspecify.annotations.Nullable;

public class ServerLifecycleHooks {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Marker SERVERHOOKS = MarkerManager.getMarker("SERVERHOOKS");
   private static final LevelResource SERVERCONFIG = new LevelResource("serverconfig");
   @Nullable
   private static volatile CountDownLatch exitLatch = null;
   @Nullable
   private static MinecraftServer currentServer;

   private static Path getServerConfigPath(MinecraftServer server) {
      Path serverConfig = server.getWorldPath(SERVERCONFIG);
      if (!Files.isDirectory(serverConfig)) {
         try {
            Files.createDirectories(serverConfig);
         } catch (IOException var5) {
            throw new RuntimeException(var5);
         }
      }

      Path explanation = serverConfig.resolve("readme.txt");
      if (!Files.exists(explanation)) {
         try {
            Files.writeString(
               explanation,
               "Any server configs put in this folder will override the corresponding server config from <instance path>/config/<config path>.\nIf the config being transferred is in a subfolder of the base config folder make sure to include that folder here in the path to the file you are overwriting.\nFor example if you are overwriting a config with the path <instance path>/config/ExampleMod/config-server.toml, you would need to put it in serverconfig/ExampleMod/config-server.toml\n",
               StandardCharsets.UTF_8
            );
         } catch (IOException var4) {
            throw new RuntimeException(var4);
         }
      }

      return serverConfig;
   }

   public static void handleServerAboutToStart(MinecraftServer server) {
      currentServer = server;
      ConfigTracker.INSTANCE.loadConfigs(Type.SERVER, FMLPaths.CONFIGDIR.get(), getServerConfigPath(server));
      runModifiers(server);
      NeoForge.EVENT_BUS.post(new ServerAboutToStartEvent(server));
   }

   public static void handleServerStarting(MinecraftServer server) {
      if (FMLEnvironment.getDist().isDedicatedServer()) {
         LanguageHook.loadModLanguages(server);
      }

      PermissionAPI.initializePermissionAPI();
      NeoForge.EVENT_BUS.post(new ServerStartingEvent(server));
   }

   public static void handleServerStarted(MinecraftServer server) {
      NeoForge.EVENT_BUS.post(new ServerStartedEvent(server));
   }

   public static void handleServerStopping(MinecraftServer server) {
      NeoForge.EVENT_BUS.post(new ServerStoppingEvent(server));
   }

   public static void expectServerStopped() {
      exitLatch = new CountDownLatch(1);
   }

   public static void handleServerStopped(MinecraftServer server) {
      NeoForge.EVENT_BUS.post(new ServerStoppedEvent(server));
      currentServer = null;
      CountDownLatch latch = exitLatch;
      if (latch != null) {
         latch.countDown();
         exitLatch = null;
      }

      ConfigTracker.INSTANCE.unloadConfigs(Type.SERVER);
   }

   @Nullable
   public static MinecraftServer getCurrentServer() {
      return currentServer;
   }

   public static void handleExit(int retVal) {
      System.exit(retVal);
   }

   private static <T> void ensureProperSync(boolean modified, Holder.Reference<T> holder, Registry<T> registry) {
      if (modified) {
         Optional<RegistrationInfo> originalInfo = registry.registrationInfo(holder.key());
         originalInfo.ifPresent(info -> {
            RegistrationInfo newInfo = new RegistrationInfo(Optional.empty(), info.lifecycle());
            ((MappedRegistryAccessor)registry).neoforge$getRegistrationInfos().put(holder.key(), newInfo);
         });
      }
   }

   private static void runModifiers(MinecraftServer server) {
      RegistryAccess registries = server.registryAccess();
      List<BiomeModifier> biomeModifiers = registries.lookupOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS).listElements().map(Holder::value).toList();
      List<StructureModifier> structureModifiers = registries.lookupOrThrow(NeoForgeRegistries.Keys.STRUCTURE_MODIFIERS)
         .listElements()
         .map(Holder::value)
         .toList();
      Set<EntityType<?>> entitiesWithoutPlacements = new HashSet<>();
      Registry<Biome> biomeRegistry = registries.lookupOrThrow(Registries.BIOME);
      biomeRegistry.listElements()
         .forEach(
            biomeHolder -> {
               Biome biome = biomeHolder.value();
               ensureProperSync(
                  biome.modifiableBiomeInfo().applyBiomeModifiers(biomeHolder, biomeModifiers, registries), (Holder.Reference<Biome>)biomeHolder, biomeRegistry
               );
               MobSpawnSettings mobSettings = biome.getMobSettings();
               mobSettings.getSpawnerTypes().forEach(category -> mobSettings.getMobs(category).unwrap().forEach(data -> {
                  if (!SpawnPlacements.hasPlacement(((MobSpawnSettings.SpawnerData)data.value()).type())) {
                     entitiesWithoutPlacements.add(data.value().type());
                  }
               }));

               for (MobCategory mobCategory : mobSettings.getSpawnerTypes()) {
                  for (Weighted<MobSpawnSettings.SpawnerData> spawnerData : mobSettings.getMobs(mobCategory).unwrap()) {
                     if (spawnerData.value().type().getCategory() != mobCategory) {
                        boolean isVanillaBug = spawnerData.value().type() == EntityType.OCELOT
                           && (biomeHolder.is(Biomes.JUNGLE) || biomeHolder.is(Biomes.BAMBOO_JUNGLE));
                        if (!isVanillaBug) {
                           LOGGER.warn(
                              "Detected {} that was registered with {} mob category but was added under {} mob category for {} biome! Mobs should be added to biomes under the same mob category that the mob was registered as to prevent mob cap spawning issues.",
                              BuiltInRegistries.ENTITY_TYPE.getKey(spawnerData.value().type()),
                              spawnerData.value().type().getCategory(),
                              mobCategory,
                              biomeHolder.getKey().identifier()
                           );
                        }
                     }
                  }
               }
            }
         );
      registries.lookupOrThrow(Registries.LEVEL_STEM).forEach(levelStem -> levelStem.generator().refreshFeaturesPerStep());
      registries.lookupOrThrow(Registries.STRUCTURE)
         .listElements()
         .forEach(structureHolder -> structureHolder.value().modifiableStructureInfo().applyStructureModifiers(structureHolder, structureModifiers));
      if (!entitiesWithoutPlacements.isEmpty() && !FMLEnvironment.isProduction()) {
         LOGGER.error(
            "The following entities have not registered to the RegisterSpawnPlacementsEvent, but a spawn entry was found. This will mean that the entity doesn't have restrictions on its spawn location, please register a spawn placement for the entity, you can register with NO_RESTRICTIONS if you don't want any restrictions."
               + entitiesWithoutPlacements.stream().map(EntityType::getKey).map(Identifier::toString).collect(Collectors.joining("\n\t - ", "\n\t - ", ""))
         );
      }
   }
}
