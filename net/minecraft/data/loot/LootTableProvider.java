package net.minecraft.data.loot;

import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.RandomSequence;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.WithConditions;
import org.slf4j.Logger;

public class LootTableProvider implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PackOutput.PathProvider pathProvider;
   private final Set<ResourceKey<LootTable>> requiredTables;
   private final List<LootTableProvider.SubProviderEntry> subProviders;
   private final CompletableFuture<HolderLookup.Provider> registries;

   public LootTableProvider(
      PackOutput p_254123_,
      Set<ResourceKey<LootTable>> p_254481_,
      List<LootTableProvider.SubProviderEntry> p_253798_,
      CompletableFuture<HolderLookup.Provider> p_323798_
   ) {
      this.pathProvider = p_254123_.createRegistryElementsPathProvider(Registries.LOOT_TABLE);
      this.subProviders = p_253798_;
      this.requiredTables = p_254481_;
      this.registries = p_323798_;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput p_254060_) {
      return this.registries.thenCompose(p_323117_ -> this.run(p_254060_, p_323117_));
   }

   private CompletableFuture<?> run(CachedOutput p_324447_, HolderLookup.Provider p_323978_) {
      WritableRegistry<LootTable> writableregistry = new MappedRegistry<>(Registries.LOOT_TABLE, Lifecycle.experimental());
      Map<RandomSupport.Seed128bit, Identifier> map = new Object2ObjectOpenHashMap();
      Map<LootTable, List<ICondition>> conditionsPerTable = new IdentityHashMap<>();
      this.getTables().forEach(p_344197_ -> p_344197_.provider().apply(p_323978_).generate((p_466036_, p_466037_) -> {
         Identifier identifier = sequenceIdForLootTable(p_466036_);
         Identifier identifier1 = map.put(RandomSequence.seedForKey(identifier), identifier);
         if (identifier1 != null) {
            Util.logAndPauseIfInIde("Loot table random sequence seed collision on " + identifier1 + " and " + p_466036_.identifier());
         }

         p_466037_.setRandomSequence(identifier);
         LootTable loottable = p_466037_.setParamSet(p_344197_.paramSet).build();
         writableregistry.register(p_466036_, loottable, RegistrationInfo.BUILT_IN);
         List<ICondition> conditions = p_466037_.buildConditions();
         if (!conditions.isEmpty()) {
            conditionsPerTable.put(loottable, conditions);
         }
      }));
      writableregistry.freeze();
      ProblemReporter.Collector problemreporter$collector = new ProblemReporter.Collector();
      HolderGetter.Provider holdergetter$provider = new RegistryAccess.ImmutableRegistryAccess(List.of(writableregistry)).freeze();
      ValidationContext validationcontext = new ValidationContext(problemreporter$collector, LootContextParamSets.ALL_PARAMS, holdergetter$provider);
      this.validate(writableregistry, validationcontext, problemreporter$collector);
      if (!problemreporter$collector.isEmpty()) {
         problemreporter$collector.forEach((p_421299_, p_421300_) -> LOGGER.warn("Found validation problem in {}: {}", p_421299_, p_421300_.description()));
         throw new IllegalStateException("Failed to validate loot tables, see logs");
      } else {
         return CompletableFuture.allOf(writableregistry.entrySet().stream().map(p_466040_ -> {
            ResourceKey<LootTable> resourcekey1 = p_466040_.getKey();
            LootTable loottable = p_466040_.getValue();
            Path path = this.pathProvider.json(resourcekey1.identifier());
            WithConditions<LootTable> conditional = new WithConditions<>(conditionsPerTable.getOrDefault(loottable, List.of()), loottable);
            return DataProvider.saveStable(p_324447_, p_323978_, LootTable.CONDITIONAL_DIRECT_CODEC, Optional.of(conditional), path);
         }).toArray(CompletableFuture[]::new));
      }
   }

   public List<LootTableProvider.SubProviderEntry> getTables() {
      return this.subProviders;
   }

   protected void validate(
      WritableRegistry<LootTable> writableregistry, ValidationContext validationcontext, ProblemReporter.Collector problemreporter$collector
   ) {
      UnmodifiableIterator var4 = Sets.difference(this.requiredTables, writableregistry.registryKeySet()).iterator();

      while (var4.hasNext()) {
         ResourceKey<LootTable> resourcekey = (ResourceKey<LootTable>)var4.next();
         problemreporter$collector.report(new LootTableProvider.MissingTableProblem(resourcekey));
      }

      writableregistry.listElements()
         .forEach(
            p_380823_ -> p_380823_.value()
               .validate(
                  validationcontext.setContextKeySet(p_380823_.value().getParamSet())
                     .enterElement(new ProblemReporter.RootElementPathElement(p_380823_.key()), p_380823_.key())
               )
         );
   }

   private static Identifier sequenceIdForLootTable(ResourceKey<LootTable> p_336172_) {
      return p_336172_.identifier();
   }

   @Override
   public final String getName() {
      return "Loot Tables";
   }

   public record MissingTableProblem(ResourceKey<LootTable> id) implements ProblemReporter.Problem {
      @Override
      public String description() {
         return "Missing built-in table: " + this.id.identifier();
      }
   }

   public record SubProviderEntry(Function<HolderLookup.Provider, LootTableSubProvider> provider, ContextKeySet paramSet) {
   }
}
