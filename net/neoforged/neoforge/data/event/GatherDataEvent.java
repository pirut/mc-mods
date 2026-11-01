package net.neoforged.neoforge.data.event;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.DetectedVersion;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.resource.ResourcePackLoader;
import net.neoforged.neoforgespi.language.IModFileInfo;
import org.apache.commons.lang3.function.Consumers;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public abstract class GatherDataEvent extends Event implements IModBusEvent {
   private final DataGenerator dataGenerator;
   private final GatherDataEvent.DataGeneratorConfig config;
   private final ModContainer modContainer;
   @Nullable
   private CompletableFuture<HolderLookup.Provider> registriesWithModdedEntries = null;

   public GatherDataEvent(ModContainer mc, DataGenerator dataGenerator, GatherDataEvent.DataGeneratorConfig dataGeneratorConfig) {
      this.modContainer = mc;
      this.dataGenerator = dataGenerator;
      this.config = dataGeneratorConfig;
   }

   public ModContainer getModContainer() {
      return this.modContainer;
   }

   public ResourceManager getResourceManager(PackType packType) {
      return switch (packType) {
         case CLIENT_RESOURCES -> this.config.clientResourceManager;
         case SERVER_DATA -> this.config.serverResourceManager;
      };
   }

   public Collection<Path> getInputs() {
      return this.config.getInputs();
   }

   public DataGenerator getGenerator() {
      return this.dataGenerator;
   }

   public CompletableFuture<HolderLookup.Provider> getLookupProvider() {
      return Objects.requireNonNullElse(this.registriesWithModdedEntries, this.config.lookupProvider);
   }

   public boolean includeDev() {
      return this.config.dev;
   }

   public boolean includeReports() {
      return this.config.reports;
   }

   public boolean validate() {
      return this.config.validate;
   }

   public <T extends DataProvider> T addProvider(T provider) {
      return this.dataGenerator.addProvider(true, provider);
   }

   public <T extends DataProvider> T createProvider(GatherDataEvent.DataProviderFromOutput<T> builder) {
      return this.addProvider(builder.create(this.dataGenerator.getPackOutput()));
   }

   public <T extends DataProvider> T createProvider(GatherDataEvent.DataProviderFromOutputLookup<T> builder) {
      return this.addProvider(builder.create(this.dataGenerator.getPackOutput(), this.getLookupProvider()));
   }

   public void createBlockAndItemTags(
      GatherDataEvent.DataProviderFromOutputLookup<TagsProvider<Block>> blockTagsProvider, GatherDataEvent.ItemTagsProvider itemTagsProvider
   ) {
      TagsProvider<Block> blockTags = this.createProvider(blockTagsProvider);
      this.addProvider(itemTagsProvider.create(this.getGenerator().getPackOutput(), this.getLookupProvider(), blockTags.contentsGetter()));
   }

   public void createDatapackRegistryObjects(RegistrySetBuilder datapackEntriesBuilder) {
      this.createDatapackRegistryObjects(datapackEntriesBuilder, Set.of(this.modContainer.getModId()));
   }

   public void createDatapackRegistryObjects(RegistrySetBuilder datapackEntriesBuilder, Set<String> modIds) {
      this.createDatapackRegistryObjects(datapackEntriesBuilder, Consumers.nop(), modIds);
   }

   public void createDatapackRegistryObjects(RegistrySetBuilder datapackEntriesBuilder, Map<ResourceKey<?>, List<ICondition>> conditions) {
      this.createDatapackRegistryObjects(datapackEntriesBuilder, conditions, Set.of(this.modContainer.getModId()));
   }

   public void createDatapackRegistryObjects(RegistrySetBuilder datapackEntriesBuilder, Map<ResourceKey<?>, List<ICondition>> conditions, Set<String> modIds) {
      DatapackBuiltinEntriesProvider registries = this.createProvider(
         (output, lookupProvider) -> new DatapackBuiltinEntriesProvider(output, lookupProvider, datapackEntriesBuilder, conditions, modIds)
      );
      this.registriesWithModdedEntries = registries.getRegistryProvider();
   }

   public void createDatapackRegistryObjects(RegistrySetBuilder datapackEntriesBuilder, Consumer<BiConsumer<ResourceKey<?>, ICondition>> conditionsBuilder) {
      this.createDatapackRegistryObjects(datapackEntriesBuilder, conditionsBuilder, Set.of(this.modContainer.getModId()));
   }

   public void createDatapackRegistryObjects(
      RegistrySetBuilder datapackEntriesBuilder, Consumer<BiConsumer<ResourceKey<?>, ICondition>> conditionsBuilder, Set<String> modIds
   ) {
      DatapackBuiltinEntriesProvider registries = this.createProvider(
         (output, lookupProvider) -> new DatapackBuiltinEntriesProvider(output, lookupProvider, datapackEntriesBuilder, conditionsBuilder, modIds)
      );
      this.registriesWithModdedEntries = registries.getRegistryProvider();
   }

   public static class Client extends GatherDataEvent {
      public Client(ModContainer mc, DataGenerator dataGenerator, GatherDataEvent.DataGeneratorConfig dataGeneratorConfig) {
         super(mc, dataGenerator, dataGeneratorConfig);
      }
   }

   @Internal
   public static class DataGeneratorConfig {
      private final Set<String> mods;
      private final Path path;
      private final Collection<Path> inputs;
      private final CompletableFuture<HolderLookup.Provider> lookupProvider;
      private final boolean dev;
      private final boolean reports;
      private final boolean validate;
      private final boolean flat;
      private final List<DataGenerator> generators = new ArrayList<>();
      private final ResourceManager clientResourceManager;
      private final ResourceManager serverResourceManager;

      public DataGeneratorConfig(
         Set<String> mods,
         Path path,
         Collection<Path> inputs,
         CompletableFuture<HolderLookup.Provider> lookupProvider,
         boolean dev,
         boolean reports,
         boolean validate,
         boolean flat,
         DataGenerator vanillaGenerator,
         Collection<Path> existingPacks,
         Consumer<Consumer<PackResources>> vanillaClientAssets
      ) {
         this.mods = mods;
         this.path = path;
         this.inputs = inputs;
         this.lookupProvider = lookupProvider;
         this.dev = dev;
         this.reports = reports;
         this.validate = validate;
         this.flat = flat;
         this.clientResourceManager = createResourceManager(PackType.CLIENT_RESOURCES, mods::contains, existingPacks, vanillaClientAssets);
         this.serverResourceManager = createResourceManager(
            PackType.SERVER_DATA, mods::contains, existingPacks, consumer -> consumer.accept(ServerPacksSource.createVanillaPackSource())
         );
         if (mods.contains("minecraft") || mods.isEmpty()) {
            this.generators.add(vanillaGenerator);
         }
      }

      public Collection<Path> getInputs() {
         return this.inputs;
      }

      public Set<String> getMods() {
         return this.mods;
      }

      public boolean isFlat() {
         return this.flat || this.getMods().size() == 1;
      }

      public DataGenerator makeGenerator(Function<Path, Path> pathEnhancer) {
         DataGenerator generator = new DataGenerator(pathEnhancer.apply(this.path), DetectedVersion.tryDetectVersion(), true);
         this.generators.add(generator);
         return generator;
      }

      public void runAll() {
         Map<Path, List<DataGenerator>> paths = this.generators
            .stream()
            .collect(Collectors.groupingBy(gen -> gen.getPackOutput().getOutputFolder(), LinkedHashMap::new, Collectors.toList()));
         paths.values().forEach(lst -> {
            DataGenerator parent = lst.get(0);

            for (int x = 1; x < lst.size(); x++) {
               parent.merge(lst.get(x));
            }

            try {
               parent.run();
            } catch (IOException var3) {
               throw new UncheckedIOException(var3);
            }
         });
      }

      private static ResourceManager createResourceManager(
         PackType packType, Predicate<String> isGeneratedMod, Collection<Path> existingPacks, Consumer<Consumer<PackResources>> consumer
      ) {
         ArrayList<PackResources> packs = Lists.newArrayList();
         consumer.accept(packs::add);
         existingPacks.forEach(path -> {
            PackLocationInfo packInfo = new PackLocationInfo(path.getFileName().toString(), Component.empty(), PackSource.BUILT_IN, Optional.empty());
            packs.add(new PathPackResources(packInfo, path));
         });
         ModList.get()
            .getSortedMods()
            .stream()
            .filter(Predicate.not(mod -> mod.getModId().equals("minecraft")))
            .filter(Predicate.not(mod -> isGeneratedMod.test(mod.getModId())))
            .map(mod -> {
               IModFileInfo owningFile = mod.getModInfo().getOwningFile();
               PackLocationInfo packInfo = new PackLocationInfo("mod/" + mod.getModId(), Component.empty(), PackSource.BUILT_IN, Optional.empty());
               return ResourcePackLoader.createPackForMod(owningFile).openPrimary(packInfo);
            })
            .forEach(packs::add);
         return new MultiPackResourceManager(packType, packs);
      }
   }

   @FunctionalInterface
   public interface DataProviderFromOutput<T extends DataProvider> {
      T create(PackOutput var1);
   }

   @FunctionalInterface
   public interface DataProviderFromOutputLookup<T extends DataProvider> {
      T create(PackOutput var1, CompletableFuture<HolderLookup.Provider> var2);
   }

   @FunctionalInterface
   public interface GatherDataEventGenerator {
      GatherDataEvent create(ModContainer var1, DataGenerator var2, GatherDataEvent.DataGeneratorConfig var3);
   }

   @FunctionalInterface
   public interface ItemTagsProvider {
      TagsProvider<Item> create(PackOutput var1, CompletableFuture<HolderLookup.Provider> var2, CompletableFuture<TagsProvider.TagLookup<Block>> var3);
   }

   public static class Server extends GatherDataEvent {
      public Server(ModContainer mc, DataGenerator dataGenerator, GatherDataEvent.DataGeneratorConfig dataGeneratorConfig) {
         super(mc, dataGenerator, dataGeneratorConfig);
      }
   }
}
