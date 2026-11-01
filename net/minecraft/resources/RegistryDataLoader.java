package net.minecraft.resources;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ChatType;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.tags.TagLoader;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.minecraft.world.entity.animal.cow.CowVariant;
import net.minecraft.world.entity.animal.feline.CatVariant;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilusVariant;
import net.minecraft.world.entity.animal.pig.PigVariant;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.providers.EnchantmentProvider;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerConfig;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.timeline.Timeline;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.DataPackRegistriesHooks;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.slf4j.Logger;

public class RegistryDataLoader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Comparator<ResourceKey<?>> ERROR_KEY_COMPARATOR = Comparator.comparing(ResourceKey::registry).thenComparing(ResourceKey::identifier);
   private static final RegistrationInfo NETWORK_REGISTRATION_INFO = new RegistrationInfo(Optional.empty(), Lifecycle.experimental());
   private static final Function<Optional<KnownPack>, RegistrationInfo> REGISTRATION_INFO_CACHE = Util.memoize(p_325559_ -> {
      Lifecycle lifecycle = p_325559_.map(KnownPack::isVanilla).map(p_325560_ -> Lifecycle.stable()).orElse(Lifecycle.experimental());
      return new RegistrationInfo(p_325559_, lifecycle);
   });
   public static final List<RegistryDataLoader.RegistryData<?>> WORLDGEN_REGISTRIES = List.of(
      new RegistryDataLoader.RegistryData<>(Registries.DIMENSION_TYPE, DimensionType.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.BIOME, Biome.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.CHAT_TYPE, ChatType.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.CONFIGURED_CARVER, ConfiguredWorldCarver.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.CONFIGURED_FEATURE, ConfiguredFeature.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.PLACED_FEATURE, PlacedFeature.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.STRUCTURE, Structure.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.STRUCTURE_SET, StructureSet.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.PROCESSOR_LIST, StructureProcessorType.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.TEMPLATE_POOL, StructureTemplatePool.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.NOISE_SETTINGS, NoiseGeneratorSettings.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.NOISE, NormalNoise.NoiseParameters.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.DENSITY_FUNCTION, DensityFunction.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.WORLD_PRESET, WorldPreset.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.FLAT_LEVEL_GENERATOR_PRESET, FlatLevelGeneratorPreset.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.TRIM_PATTERN, TrimPattern.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.TRIM_MATERIAL, TrimMaterial.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.TRIAL_SPAWNER_CONFIG, TrialSpawnerConfig.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.WOLF_VARIANT, WolfVariant.DIRECT_CODEC, true),
      new RegistryDataLoader.RegistryData<>(Registries.WOLF_SOUND_VARIANT, WolfSoundVariant.DIRECT_CODEC, true),
      new RegistryDataLoader.RegistryData<>(Registries.PIG_VARIANT, PigVariant.DIRECT_CODEC, true),
      new RegistryDataLoader.RegistryData<>(Registries.FROG_VARIANT, FrogVariant.DIRECT_CODEC, true),
      new RegistryDataLoader.RegistryData<>(Registries.CAT_VARIANT, CatVariant.DIRECT_CODEC, true),
      new RegistryDataLoader.RegistryData<>(Registries.COW_VARIANT, CowVariant.DIRECT_CODEC, true),
      new RegistryDataLoader.RegistryData<>(Registries.CHICKEN_VARIANT, ChickenVariant.DIRECT_CODEC, true),
      new RegistryDataLoader.RegistryData<>(Registries.ZOMBIE_NAUTILUS_VARIANT, ZombieNautilusVariant.DIRECT_CODEC, true),
      new RegistryDataLoader.RegistryData<>(Registries.PAINTING_VARIANT, PaintingVariant.DIRECT_CODEC, true),
      new RegistryDataLoader.RegistryData<>(Registries.DAMAGE_TYPE, DamageType.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, MultiNoiseBiomeSourceParameterList.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.BANNER_PATTERN, BannerPattern.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.ENCHANTMENT, Enchantment.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.ENCHANTMENT_PROVIDER, EnchantmentProvider.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.JUKEBOX_SONG, JukeboxSong.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.INSTRUMENT, Instrument.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.TEST_ENVIRONMENT, TestEnvironmentDefinition.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.TEST_INSTANCE, GameTestInstance.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.DIALOG, Dialog.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.TIMELINE, Timeline.DIRECT_CODEC)
   );
   public static final List<RegistryDataLoader.RegistryData<?>> DIMENSION_REGISTRIES = List.of(
      new RegistryDataLoader.RegistryData<>(Registries.LEVEL_STEM, LevelStem.CODEC)
   );
   public static final List<RegistryDataLoader.RegistryData<?>> SYNCHRONIZED_REGISTRIES = DataPackRegistriesHooks.grabNetworkableRegistries(
      List.of(
         new RegistryDataLoader.RegistryData<>(Registries.BIOME, Biome.NETWORK_CODEC),
         new RegistryDataLoader.RegistryData<>(Registries.CHAT_TYPE, ChatType.DIRECT_CODEC),
         new RegistryDataLoader.RegistryData<>(Registries.TRIM_PATTERN, TrimPattern.DIRECT_CODEC),
         new RegistryDataLoader.RegistryData<>(Registries.TRIM_MATERIAL, TrimMaterial.DIRECT_CODEC),
         new RegistryDataLoader.RegistryData<>(Registries.WOLF_VARIANT, WolfVariant.NETWORK_CODEC, true),
         new RegistryDataLoader.RegistryData<>(Registries.WOLF_SOUND_VARIANT, WolfSoundVariant.NETWORK_CODEC, true),
         new RegistryDataLoader.RegistryData<>(Registries.PIG_VARIANT, PigVariant.NETWORK_CODEC, true),
         new RegistryDataLoader.RegistryData<>(Registries.FROG_VARIANT, FrogVariant.NETWORK_CODEC, true),
         new RegistryDataLoader.RegistryData<>(Registries.CAT_VARIANT, CatVariant.NETWORK_CODEC, true),
         new RegistryDataLoader.RegistryData<>(Registries.COW_VARIANT, CowVariant.NETWORK_CODEC, true),
         new RegistryDataLoader.RegistryData<>(Registries.CHICKEN_VARIANT, ChickenVariant.NETWORK_CODEC, true),
         new RegistryDataLoader.RegistryData<>(Registries.ZOMBIE_NAUTILUS_VARIANT, ZombieNautilusVariant.NETWORK_CODEC, true),
         new RegistryDataLoader.RegistryData<>(Registries.PAINTING_VARIANT, PaintingVariant.DIRECT_CODEC, true),
         new RegistryDataLoader.RegistryData<>(Registries.DIMENSION_TYPE, DimensionType.NETWORK_CODEC),
         new RegistryDataLoader.RegistryData<>(Registries.DAMAGE_TYPE, DamageType.DIRECT_CODEC),
         new RegistryDataLoader.RegistryData<>(Registries.BANNER_PATTERN, BannerPattern.DIRECT_CODEC),
         new RegistryDataLoader.RegistryData<>(Registries.ENCHANTMENT, Enchantment.DIRECT_CODEC),
         new RegistryDataLoader.RegistryData<>(Registries.JUKEBOX_SONG, JukeboxSong.DIRECT_CODEC),
         new RegistryDataLoader.RegistryData<>(Registries.INSTRUMENT, Instrument.DIRECT_CODEC),
         new RegistryDataLoader.RegistryData<>(Registries.TEST_ENVIRONMENT, TestEnvironmentDefinition.DIRECT_CODEC),
         new RegistryDataLoader.RegistryData<>(Registries.TEST_INSTANCE, GameTestInstance.DIRECT_CODEC),
         new RegistryDataLoader.RegistryData<>(Registries.DIALOG, Dialog.DIRECT_CODEC),
         new RegistryDataLoader.RegistryData<>(Registries.TIMELINE, Timeline.NETWORK_CODEC)
      )
   );

   public static RegistryAccess.Frozen load(
      ResourceManager p_364116_, List<HolderLookup.RegistryLookup<?>> p_321716_, List<RegistryDataLoader.RegistryData<?>> p_365382_
   ) {
      return load((p_321412_, p_321413_) -> p_321412_.loadFromResources(p_364116_, p_321413_), p_321716_, p_365382_, true);
   }

   public static RegistryAccess.Frozen load(
      Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> p_364449_,
      ResourceProvider p_360464_,
      List<HolderLookup.RegistryLookup<?>> p_250344_,
      List<RegistryDataLoader.RegistryData<?>> p_363651_
   ) {
      return load((p_325557_, p_325558_) -> p_325557_.loadFromNetwork(p_364449_, p_360464_, p_325558_), p_250344_, p_363651_, false);
   }

   @Deprecated
   private static RegistryAccess.Frozen load(
      RegistryDataLoader.LoadingFunction p_321713_, List<HolderLookup.RegistryLookup<?>> p_321856_, List<RegistryDataLoader.RegistryData<?>> p_364355_
   ) {
      return load(p_321713_, p_321856_, p_364355_, false);
   }

   private static RegistryAccess.Frozen load(
      RegistryDataLoader.LoadingFunction p_321713_,
      List<HolderLookup.RegistryLookup<?>> p_321856_,
      List<RegistryDataLoader.RegistryData<?>> p_364355_,
      boolean fromResources
   ) {
      Map<ResourceKey<?>, Exception> map = new HashMap<>();
      List<RegistryDataLoader.Loader<?>> list = p_364355_.stream()
         .map(p_321410_ -> p_321410_.create(Lifecycle.stable(), map))
         .collect(Collectors.toUnmodifiableList());
      RegistryOps.RegistryInfoLookup registryops$registryinfolookup = createContext(p_321856_, list);
      list.forEach(p_321416_ -> p_321713_.apply((RegistryDataLoader.Loader<?>)p_321416_, registryops$registryinfolookup));
      if (fromResources && GameTestHooks.isGametestEnabled()) {
         Map<? extends ResourceKey<? extends Registry<?>>, ? extends WritableRegistry<?>> byKeyMap = list.stream()
            .collect(Collectors.toMap(d -> d.registry().key(), d -> d.registry));
         WritableRegistry envRegistry = byKeyMap.get(Registries.TEST_ENVIRONMENT);
         WritableRegistry testsRegistry = byKeyMap.get(Registries.TEST_INSTANCE);
         if (envRegistry != null && testsRegistry != null) {
            ModLoader.postEvent(new RegisterGameTestsEvent(envRegistry, testsRegistry));
         }
      }

      list.forEach(p_466125_ -> {
         Registry<?> registry = p_466125_.registry();

         try {
            registry.freeze();
         } catch (Exception var4x) {
            map.put(registry.key(), var4x);
         }

         if (p_466125_.data.requiredNonEmpty && registry.size() == 0) {
            map.put(registry.key(), new IllegalStateException("Registry must be non-empty: " + registry.key().identifier()));
         }
      });
      if (!map.isEmpty()) {
         throw logErrors(map);
      } else {
         return new RegistryAccess.ImmutableRegistryAccess(list.stream().map(RegistryDataLoader.Loader::registry).toList()).freeze();
      }
   }

   private static RegistryOps.RegistryInfoLookup createContext(List<HolderLookup.RegistryLookup<?>> p_255821_, List<RegistryDataLoader.Loader<?>> p_360285_) {
      final Map<ResourceKey<? extends Registry<?>>, RegistryOps.RegistryInfo<?>> map = new HashMap<>();
      p_255821_.forEach(p_359481_ -> map.put(p_359481_.key(), createInfoForContextRegistry((HolderLookup.RegistryLookup<?>)p_359481_)));
      p_360285_.forEach(p_367838_ -> map.put(p_367838_.registry.key(), createInfoForNewRegistry(p_367838_.registry)));
      return new RegistryOps.RegistryInfoLookup() {
         @Override
         public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> p_256014_) {
            return Optional.ofNullable((RegistryOps.RegistryInfo<T>)map.get(p_256014_));
         }
      };
   }

   private static <T> RegistryOps.RegistryInfo<T> createInfoForNewRegistry(WritableRegistry<T> p_256020_) {
      return new RegistryOps.RegistryInfo<>(p_256020_, p_256020_.createRegistrationLookup(), p_256020_.registryLifecycle());
   }

   private static <T> RegistryOps.RegistryInfo<T> createInfoForContextRegistry(HolderLookup.RegistryLookup<T> p_364499_) {
      return new RegistryOps.RegistryInfo<>(p_364499_, p_364499_, p_364499_.registryLifecycle());
   }

   private static ReportedException logErrors(Map<ResourceKey<?>, Exception> p_366903_) {
      printFullDetailsToLog(p_366903_);
      return createReportWithBriefInfo(p_366903_);
   }

   private static void printFullDetailsToLog(Map<ResourceKey<?>, Exception> p_252325_) {
      StringWriter stringwriter = new StringWriter();
      PrintWriter printwriter = new PrintWriter(stringwriter);
      Map<Identifier, Map<Identifier, Exception>> map = p_252325_.entrySet()
         .stream()
         .collect(
            Collectors.groupingBy(p_466123_ -> p_466123_.getKey().registry(), Collectors.toMap(p_466126_ -> p_466126_.getKey().identifier(), Entry::getValue))
         );
      map.entrySet().stream().sorted(Entry.comparingByKey()).forEach(p_458830_ -> {
         printwriter.printf(Locale.ROOT, "> Errors in registry %s:%n", p_458830_.getKey());
         p_458830_.getValue().entrySet().stream().sorted(Entry.comparingByKey()).forEach(p_458828_ -> {
            printwriter.printf(Locale.ROOT, ">> Errors in element %s:%n", p_458828_.getKey());
            p_458828_.getValue().printStackTrace(printwriter);
         });
      });
      printwriter.flush();
      LOGGER.error("Registry loading errors:\n{}", stringwriter);
   }

   private static ReportedException createReportWithBriefInfo(Map<ResourceKey<?>, Exception> p_366880_) {
      CrashReport crashreport = CrashReport.forThrowable(new IllegalStateException("Failed to load registries due to errors"), "Registry Loading");
      CrashReportCategory crashreportcategory = crashreport.addCategory("Loading info");
      crashreportcategory.setDetail(
         "Errors",
         () -> {
            StringBuilder stringbuilder = new StringBuilder();
            p_366880_.entrySet()
               .stream()
               .sorted(Entry.comparingByKey(ERROR_KEY_COMPARATOR))
               .forEach(
                  p_466122_ -> stringbuilder.append("\n\t\t")
                     .append(p_466122_.getKey().registry())
                     .append("/")
                     .append(p_466122_.getKey().identifier())
                     .append(": ")
                     .append(p_466122_.getValue().getMessage())
               );
            return stringbuilder.toString();
         }
      );
      return new ReportedException(crashreport);
   }

   private static <E> void loadElementFromResource(
      WritableRegistry<E> p_326195_,
      Decoder<E> p_326476_,
      RegistryOps<JsonElement> p_325932_,
      ResourceKey<E> p_326054_,
      Resource p_326141_,
      RegistrationInfo p_326033_
   ) throws IOException {
      Decoder<Optional<E>> decoder = ConditionalOps.<E>createConditionalCodec(NeoForgeExtraCodecs.decodeOnly(p_326476_));

      try (Reader reader = p_326141_.openAsReader()) {
         JsonElement jsonelement = StrictJsonParser.parse(reader);
         DataResult<Optional<E>> dataresult = decoder.parse(p_325932_, jsonelement);
         Optional<E> candidate = (Optional<E>)dataresult.getOrThrow();
         candidate.ifPresentOrElse(
            e -> p_326195_.register(p_326054_, (E)e, p_326033_),
            () -> LOGGER.debug("Skipping loading registry entry {} as its conditions were not met", p_326054_)
         );
      }
   }

   static <E> void loadContentsFromManager(
      ResourceManager p_321535_,
      RegistryOps.RegistryInfoLookup p_321612_,
      WritableRegistry<E> p_321557_,
      Decoder<E> p_321820_,
      Map<ResourceKey<?>, Exception> p_321649_
   ) {
      FileToIdConverter filetoidconverter = FileToIdConverter.registry(p_321557_.key());
      RegistryOps<JsonElement> registryops = new ConditionalOps<>(RegistryOps.create(JsonOps.INSTANCE, p_321612_), ICondition.IContext.TAGS_INVALID);

      for (Entry<Identifier, Resource> entry : filetoidconverter.listMatchingResources(p_321535_).entrySet()) {
         Identifier identifier = entry.getKey();
         ResourceKey<E> resourcekey = ResourceKey.create(p_321557_.key(), filetoidconverter.fileToId(identifier));
         Resource resource = entry.getValue();
         RegistrationInfo registrationinfo = REGISTRATION_INFO_CACHE.apply(resource.knownPackInfo());

         try {
            loadElementFromResource(p_321557_, p_321820_, registryops, resourcekey, resource, registrationinfo);
         } catch (Exception var14) {
            p_321649_.put(
               resourcekey,
               new IllegalStateException(String.format(Locale.ROOT, "Failed to parse %s from pack %s", identifier, resource.sourcePackId()), var14)
            );
         }
      }

      TagLoader.loadTagsForRegistry(p_321535_, p_321557_);
   }

   static <E> void loadContentsFromNetwork(
      Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> p_321633_,
      ResourceProvider p_326020_,
      RegistryOps.RegistryInfoLookup p_321801_,
      WritableRegistry<E> p_321671_,
      Decoder<E> p_321718_,
      Map<ResourceKey<?>, Exception> p_321625_
   ) {
      RegistryDataLoader.NetworkedRegistryData registrydataloader$networkedregistrydata = p_321633_.get(p_321671_.key());
      if (registrydataloader$networkedregistrydata != null) {
         RegistryOps<Tag> registryops = RegistryOps.create(NbtOps.INSTANCE, p_321801_);
         RegistryOps<JsonElement> registryops1 = RegistryOps.create(JsonOps.INSTANCE, p_321801_);
         FileToIdConverter filetoidconverter = FileToIdConverter.registry(p_321671_.key());

         for (RegistrySynchronization.PackedRegistryEntry registrysynchronization$packedregistryentry : registrydataloader$networkedregistrydata.elements) {
            ResourceKey<E> resourcekey = ResourceKey.create(p_321671_.key(), registrysynchronization$packedregistryentry.id());
            Optional<Tag> optional = registrysynchronization$packedregistryentry.data();
            if (optional.isPresent()) {
               try {
                  DataResult<E> dataresult = p_321718_.parse(registryops, optional.get());
                  E e = (E)dataresult.getOrThrow();
                  p_321671_.register(resourcekey, e, NETWORK_REGISTRATION_INFO);
               } catch (Exception var17) {
                  p_321625_.put(
                     resourcekey, new IllegalStateException(String.format(Locale.ROOT, "Failed to parse value %s from server", optional.get()), var17)
                  );
               }
            } else {
               Identifier identifier = filetoidconverter.idToFile(registrysynchronization$packedregistryentry.id());

               try {
                  Resource resource = p_326020_.getResourceOrThrow(identifier);
                  loadElementFromResource(p_321671_, p_321718_, registryops1, resourcekey, resource, NETWORK_REGISTRATION_INFO);
               } catch (Exception var16) {
                  p_321625_.put(resourcekey, new IllegalStateException("Failed to parse local data", var16));
               }
            }
         }

         TagLoader.loadTagsFromNetwork(registrydataloader$networkedregistrydata.tags, p_321671_);
      }
   }

   record Loader<T>(RegistryDataLoader.RegistryData<T> data, WritableRegistry<T> registry, Map<ResourceKey<?>, Exception> loadingErrors) {
      public void loadFromResources(ResourceManager p_321702_, RegistryOps.RegistryInfoLookup p_321840_) {
         RegistryDataLoader.loadContentsFromManager(p_321702_, p_321840_, this.registry, this.data.elementCodec, this.loadingErrors);
      }

      public void loadFromNetwork(
         Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> p_321562_,
         ResourceProvider p_326419_,
         RegistryOps.RegistryInfoLookup p_321617_
      ) {
         RegistryDataLoader.loadContentsFromNetwork(p_321562_, p_326419_, p_321617_, this.registry, this.data.elementCodec, this.loadingErrors);
      }
   }

   @FunctionalInterface
   interface LoadingFunction {
      void apply(RegistryDataLoader.Loader<?> var1, RegistryOps.RegistryInfoLookup var2);
   }

   public record NetworkedRegistryData(List<RegistrySynchronization.PackedRegistryEntry> elements, TagNetworkSerialization.NetworkPayload tags) {
   }

   public record RegistryData<T>(
      ResourceKey<? extends Registry<T>> key, Codec<T> elementCodec, boolean requiredNonEmpty, Consumer<RegistryBuilder<T>> registryBuilderConsumer
   ) {
      public RegistryData(ResourceKey<? extends Registry<T>> key, Codec<T> elementCodec, boolean requiredNonEmpty) {
         this(key, elementCodec, requiredNonEmpty, registryBuilder -> {});
      }

      RegistryData(ResourceKey<? extends Registry<T>> p_251360_, Codec<T> p_248976_) {
         this(p_251360_, p_248976_, false);
      }

      RegistryDataLoader.Loader<T> create(Lifecycle p_251662_, Map<ResourceKey<?>, Exception> p_251565_) {
         RegistryBuilder<T> registryBuilder = new RegistryBuilder<>(this.key);
         this.registryBuilderConsumer.accept(registryBuilder);
         WritableRegistry<T> writableregistry = (WritableRegistry<T>)registryBuilder.disableRegistrationCheck().create();
         return new RegistryDataLoader.Loader<>(this, writableregistry, p_251565_);
      }

      public void runWithArguments(BiConsumer<ResourceKey<? extends Registry<T>>, Codec<T>> p_312899_) {
         p_312899_.accept(this.key, this.elementCodec);
      }
   }
}
