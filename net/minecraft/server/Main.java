package net.minecraft.server;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import joptsimple.util.PathProperties;
import net.minecraft.CrashReport;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.jfr.Environment;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.neoforged.neoforge.gametest.BlockPosValueConverter;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.server.loading.ServerModLoader;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;

public class Main {
   private static final Logger LOGGER = LogUtils.getLogger();

   @DontObfuscate
   public static void main(String[] p_129699_) throws Exception {
      SharedConstants.tryDetectVersion();
      OptionParser optionparser = new OptionParser();
      OptionSpec<Void> optionspec = optionparser.accepts("nogui");
      OptionSpec<Void> optionspec1 = optionparser.accepts("initSettings", "Initializes 'server.properties' and 'eula.txt', then quits");
      OptionSpec<Void> optionspec2 = optionparser.accepts("demo");
      OptionSpec<Void> optionspec3 = optionparser.accepts("bonusChest");
      OptionSpec<Void> optionspec4 = optionparser.accepts("forceUpgrade");
      OptionSpec<Void> optionspec5 = optionparser.accepts("eraseCache");
      OptionSpec<Void> optionspec6 = optionparser.accepts("recreateRegionFiles");
      OptionSpec<Void> optionspec7 = optionparser.accepts("safeMode", "Loads level with vanilla datapack only");
      OptionSpec<Void> optionspec8 = optionparser.accepts("help").forHelp();
      OptionSpec<String> optionspec9 = optionparser.accepts("universe").withRequiredArg().defaultsTo(".", new String[0]);
      OptionSpec<String> optionspec10 = optionparser.accepts("world").withRequiredArg();
      OptionSpec<Integer> optionspec11 = optionparser.accepts("port").withRequiredArg().ofType(Integer.class).defaultsTo(-1, new Integer[0]);
      OptionSpec<String> optionspec12 = optionparser.accepts("serverId").withRequiredArg();
      OptionSpec<Void> optionspec13 = optionparser.accepts("jfrProfile");
      OptionSpec<Path> optionspec14 = optionparser.accepts("pidFile").withRequiredArg().withValuesConvertedBy(new PathConverter(new PathProperties[0]));
      OptionSpec<String> optionspec15 = optionparser.nonOptions();
      optionparser.accepts("allowUpdates").withRequiredArg().ofType(Boolean.class).defaultsTo(Boolean.TRUE, new Boolean[0]);
      optionparser.accepts("gameDir").withRequiredArg().ofType(File.class).defaultsTo(new File("."), new File[0]);
      boolean gametestEnabled = Boolean.getBoolean("neoforge.gameTestServer");
      OptionSpec<BlockPos> spawnPosOpt;
      if (gametestEnabled) {
         spawnPosOpt = optionparser.accepts("spawnPos")
            .withRequiredArg()
            .withValuesConvertedBy(new BlockPosValueConverter())
            .defaultsTo(new BlockPos(0, 60, 0), new BlockPos[0]);
      } else {
         spawnPosOpt = null;
      }

      try {
         OptionSet optionset = optionparser.parse(p_129699_);
         if (optionset.has(optionspec8)) {
            optionparser.printHelpOn(System.err);
            return;
         }

         Path path2 = Paths.get("eula.txt");
         Eula eula = new Eula(path2);
         if (!eula.hasAgreedToEULA()) {
            LOGGER.info("You need to agree to the EULA in order to run the server. Go to eula.txt for more info.");
            return;
         }

         Path path = (Path)optionset.valueOf(optionspec14);
         if (path != null) {
            writePidFile(path);
         }

         CrashReport.preload();
         if (optionset.has(optionspec13)) {
            JvmProfiler.INSTANCE.start(Environment.SERVER);
         }

         Bootstrap.bootStrap();
         Bootstrap.validate();
         Util.startTimerHackThread();
         Path path1 = Paths.get("server.properties");
         if (!optionset.has(optionspec1)) {
            ServerModLoader.load();
         }

         DedicatedServerSettings dedicatedserversettings = new DedicatedServerSettings(path1);
         dedicatedserversettings.forceSave();
         RegionFileVersion.configure(dedicatedserversettings.getProperties().regionFileComression);
         if (optionset.has(optionspec1)) {
            LOGGER.info("Initialized '{}' and '{}'", path1.toAbsolutePath(), path2.toAbsolutePath());
            return;
         }

         File file1 = new File((String)optionset.valueOf(optionspec9));
         Services services = Services.create(new YggdrasilAuthenticationService(Proxy.NO_PROXY), file1);
         String s = (String)Optional.ofNullable((String)optionset.valueOf(optionspec10)).orElse(dedicatedserversettings.getProperties().levelName);
         if (s == null || s.isEmpty() || new File(file1, s).getAbsolutePath().equals(new File(s).getAbsolutePath())) {
            LOGGER.error("Invalid world directory specified, must not be null, empty or the same directory as your universe! " + s);
            return;
         }

         LevelStorageSource levelstoragesource = LevelStorageSource.createDefault(file1.toPath());
         LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = levelstoragesource.validateAndCreateAccess(s);
         Dynamic<?> dynamic;
         if (levelstoragesource$levelstorageaccess.hasWorldData()) {
            LevelSummary levelsummary;
            try {
               dynamic = levelstoragesource$levelstorageaccess.getDataTag();
               levelsummary = levelstoragesource$levelstorageaccess.getSummary(dynamic);
               levelstoragesource$levelstorageaccess.readAdditionalLevelSaveData(false);
            } catch (ReportedNbtException | IOException | NbtException var43) {
               LevelStorageSource.LevelDirectory levelstoragesource$leveldirectory = levelstoragesource$levelstorageaccess.getLevelDirectory();
               LOGGER.warn("Failed to load world data from {}", levelstoragesource$leveldirectory.dataFile(), var43);
               LOGGER.info("Attempting to use fallback");

               try {
                  dynamic = levelstoragesource$levelstorageaccess.getDataTagFallback();
                  levelsummary = levelstoragesource$levelstorageaccess.getSummary(dynamic);
                  levelstoragesource$levelstorageaccess.readAdditionalLevelSaveData(true);
               } catch (ReportedNbtException | IOException | NbtException var42) {
                  LOGGER.error("Failed to load world data from {}", levelstoragesource$leveldirectory.oldDataFile(), var42);
                  LOGGER.error(
                     "Failed to load world data from {} and {}. World files may be corrupted. Shutting down.",
                     levelstoragesource$leveldirectory.dataFile(),
                     levelstoragesource$leveldirectory.oldDataFile()
                  );
                  return;
               }

               levelstoragesource$levelstorageaccess.restoreLevelDataFromOld();
            }

            if (levelsummary.requiresManualConversion()) {
               LOGGER.info("This world must be opened in an older version (like 1.6.4) to be safely converted");
               return;
            }

            if (!levelsummary.isCompatible()) {
               LOGGER.info("This world was created by an incompatible version.");
               return;
            }
         } else {
            dynamic = null;
         }

         Dynamic<?> dynamic1 = dynamic;
         boolean flag = optionset.has(optionspec7);
         if (flag) {
            LOGGER.warn("Safe mode active, only vanilla datapack will be loaded");
         }

         PackRepository packrepository = ServerPacksSource.createPackRepository(levelstoragesource$levelstorageaccess);
         if (gametestEnabled) {
            GameTestHooks.registerGametests();
            BlockPos spawnPos = (BlockPos)optionset.valueOf(spawnPosOpt);
            MinecraftServer.spin(
               threadx -> GameTestServer.create(
                  threadx, levelstoragesource$levelstorageaccess, packrepository, GameTestRegistry.getAllTestFunctions(), spawnPos
               )
            );
            return;
         }

         WorldStem worldstem;
         try {
            WorldLoader.InitConfig worldloader$initconfig = loadOrCreateConfig(dedicatedserversettings.getProperties(), dynamic1, flag, packrepository);
            worldstem = Util.<WorldStem>blockUntilDone(
                  p_248086_ -> WorldLoader.load(
                     worldloader$initconfig,
                     p_307161_ -> {
                        Registry<LevelStem> registry = p_307161_.datapackDimensions().registryOrThrow(Registries.LEVEL_STEM);
                        if (dynamic1 != null) {
                           LevelDataAndDimensions leveldataanddimensions = LevelStorageSource.getLevelDataAndDimensions(
                              dynamic1, p_307161_.dataConfiguration(), registry, p_307161_.datapackWorldgen()
                           );
                           return new WorldLoader.DataLoadOutput<>(
                              leveldataanddimensions.worldData(), leveldataanddimensions.dimensions().dimensionsRegistryAccess()
                           );
                        } else {
                           LOGGER.info("No existing world data, creating new world");
                           LevelSettings levelsettings;
                           WorldOptions worldoptions;
                           WorldDimensions worlddimensions;
                           if (optionset.has(optionspec2)) {
                              levelsettings = MinecraftServer.DEMO_SETTINGS;
                              worldoptions = WorldOptions.DEMO_OPTIONS;
                              worlddimensions = WorldPresets.createNormalWorldDimensions(p_307161_.datapackWorldgen());
                           } else {
                              DedicatedServerProperties dedicatedserverproperties = dedicatedserversettings.getProperties();
                              levelsettings = new LevelSettings(
                                 dedicatedserverproperties.levelName,
                                 dedicatedserverproperties.gamemode,
                                 dedicatedserverproperties.hardcore,
                                 dedicatedserverproperties.difficulty,
                                 false,
                                 new GameRules(),
                                 p_307161_.dataConfiguration()
                              );
                              worldoptions = optionset.has(optionspec3)
                                 ? dedicatedserverproperties.worldOptions.withBonusChest(true)
                                 : dedicatedserverproperties.worldOptions;
                              worlddimensions = dedicatedserverproperties.createDimensions(p_307161_.datapackWorldgen());
                           }

                           RegistryOps<Tag> registryOps = RegistryOps.create(NbtOps.INSTANCE, p_307161_.datapackWorldgen());
                           worlddimensions = WorldDimensions.CODEC
                              .encoder()
                              .encodeStart(registryOps, worlddimensions)
                              .flatMap(
                                 writtenPayloadWithModdedDimensions -> WorldDimensions.CODEC.decoder().parse(registryOps, writtenPayloadWithModdedDimensions)
                              )
                              .resultOrPartial(LOGGER::error)
                              .orElse(worlddimensions);
                           WorldDimensions.Complete worlddimensions$complete = worlddimensions.bake(registry);
                           Lifecycle lifecycle = worlddimensions$complete.lifecycle().add(p_307161_.datapackWorldgen().allRegistriesLifecycle());
                           return new WorldLoader.DataLoadOutput<>(
                              new PrimaryLevelData(levelsettings, worldoptions, worlddimensions$complete.specialWorldProperty(), lifecycle),
                              worlddimensions$complete.dimensionsRegistryAccess()
                           );
                        }
                     },
                     WorldStem::new,
                     Util.backgroundExecutor(),
                     p_248086_
                  )
               )
               .get();
         } catch (Exception var41) {
            LOGGER.warn(
               "Failed to load datapacks, can't proceed with server load. You can either fix your datapacks or reset to vanilla with --safeMode", var41
            );
            return;
         }

         RegistryAccess.Frozen registryaccess$frozen = worldstem.registries().compositeAccess();
         boolean flag1 = optionset.has(optionspec6);
         if (optionset.has(optionspec4) || flag1) {
            forceUpgrade(levelstoragesource$levelstorageaccess, DataFixers.getDataFixer(), optionset.has(optionspec5), () -> true, registryaccess$frozen, flag1);
         }

         WorldData worlddata = worldstem.worldData();
         levelstoragesource$levelstorageaccess.saveDataTag(registryaccess$frozen, worlddata);
         final DedicatedServer dedicatedserver = MinecraftServer.spin(
            p_293760_ -> {
               DedicatedServer dedicatedserver1 = new DedicatedServer(
                  p_293760_,
                  levelstoragesource$levelstorageaccess,
                  packrepository,
                  worldstem,
                  dedicatedserversettings,
                  DataFixers.getDataFixer(),
                  services,
                  LoggerChunkProgressListener::createFromGameruleRadius
               );
               dedicatedserver1.setPort((Integer)optionset.valueOf(optionspec11));
               dedicatedserver1.setDemo(optionset.has(optionspec2));
               dedicatedserver1.setId((String)optionset.valueOf(optionspec12));
               boolean flag2 = !optionset.has(optionspec) && !optionset.valuesOf(optionspec15).contains("nogui");
               if (flag2 && !GraphicsEnvironment.isHeadless()) {
                  dedicatedserver1.showGui();
               }

               return dedicatedserver1;
            }
         );
         Thread thread = new Thread("Server Shutdown Thread") {
            @Override
            public void run() {
               dedicatedserver.halt(true);
               LogManager.shutdown();
            }
         };
         thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
         Runtime.getRuntime().addShutdownHook(thread);
      } catch (Exception var44) {
         LOGGER.error(LogUtils.FATAL_MARKER, "Failed to start the minecraft server", var44);
      }
   }

   private static void writePidFile(Path p_270192_) {
      try {
         long i = ProcessHandle.current().pid();
         Files.writeString(p_270192_, Long.toString(i));
      } catch (IOException var3) {
         throw new UncheckedIOException(var3);
      }
   }

   private static WorldLoader.InitConfig loadOrCreateConfig(
      DedicatedServerProperties p_248563_, @Nullable Dynamic<?> p_307444_, boolean p_249093_, PackRepository p_251069_
   ) {
      boolean flag;
      WorldDataConfiguration worlddataconfiguration;
      if (p_307444_ != null) {
         WorldDataConfiguration worlddataconfiguration1 = LevelStorageSource.readDataConfig(p_307444_);
         flag = false;
         worlddataconfiguration = worlddataconfiguration1;
      } else {
         flag = true;
         worlddataconfiguration = new WorldDataConfiguration(p_248563_.initialDataPackConfiguration, FeatureFlags.DEFAULT_FLAGS);
      }

      WorldLoader.PackConfig worldloader$packconfig = new WorldLoader.PackConfig(p_251069_, worlddataconfiguration, p_249093_, flag);
      return new WorldLoader.InitConfig(worldloader$packconfig, Commands.CommandSelection.DEDICATED, p_248563_.functionPermissionLevel);
   }

   private static void forceUpgrade(
      LevelStorageSource.LevelStorageAccess p_195489_,
      DataFixer p_195490_,
      boolean p_195491_,
      BooleanSupplier p_195492_,
      RegistryAccess p_323503_,
      boolean p_321704_
   ) {
      LOGGER.info("Forcing world upgrade!");
      WorldUpgrader worldupgrader = new WorldUpgrader(p_195489_, p_195490_, p_323503_, p_195491_, p_321704_);
      Component component = null;

      while (!worldupgrader.isFinished()) {
         Component component1 = worldupgrader.getStatus();
         if (component != component1) {
            component = component1;
            LOGGER.info(worldupgrader.getStatus().getString());
         }

         int i = worldupgrader.getTotalChunks();
         if (i > 0) {
            int j = worldupgrader.getConverted() + worldupgrader.getSkipped();
            LOGGER.info("{}% completed ({} / {} chunks)...", new Object[]{Mth.floor((float)j / i * 100.0F), j, i});
         }

         if (!p_195492_.getAsBoolean()) {
            worldupgrader.cancel();
         } else {
            try {
               Thread.sleep(1000L);
            } catch (InterruptedException var11) {
            }
         }
      }
   }
}
