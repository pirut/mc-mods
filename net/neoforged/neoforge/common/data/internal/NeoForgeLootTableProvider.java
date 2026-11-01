package net.neoforged.neoforge.common.data.internal;

import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.WritableRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.loot.packs.VanillaLootTableProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.DynamicLoot;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.EntryGroup;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.entries.SequentialEntry;
import net.minecraft.world.level.storage.loot.entries.TagEntry;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.AllOfCondition;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.CompositeLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.InvertedLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.loot.CanItemPerformAbility;
import org.jspecify.annotations.Nullable;

public final class NeoForgeLootTableProvider extends LootTableProvider {
   private final List<Function<LootItemCondition, LootItemCondition.Builder>> conditionReplacers = new ArrayList<>();

   public NeoForgeLootTableProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> provider) {
      super(packOutput, Set.of(), VanillaLootTableProvider.create(packOutput, provider).getTables(), provider);
   }

   @Override
   protected void validate(
      WritableRegistry<LootTable> writableregistry, ValidationContext validationcontext, ProblemReporter.Collector problemreporter$collector
   ) {
   }

   @Override
   public List<LootTableProvider.SubProviderEntry> getTables() {
      this.replaceLootItemCondition(
         condition -> condition instanceof MatchTool matchTool && this.checkMatchTool(matchTool, Items.SHEARS)
            ? CanItemPerformAbility.canItemPerformAbility(ItemAbilities.SHEARS_DIG)
            : null
      );
      return super.getTables()
         .stream()
         .map(entry -> new LootTableProvider.SubProviderEntry(provider -> this.replaceAndFilterChangesOnly(entry.provider().apply(provider)), entry.paramSet()))
         .collect(Collectors.toList());
   }

   private LootTableSubProvider replaceAndFilterChangesOnly(LootTableSubProvider subProvider) {
      return newConsumer -> subProvider.generate((resourceLocation, builder) -> {
         LootTable.Builder newBuilder = this.findAndReplaceInLootTableBuilder(builder);
         if (newBuilder != null) {
            newConsumer.accept(resourceLocation, newBuilder);
         }
      });
   }

   private void replaceLootItemCondition(Function<LootItemCondition, LootItemCondition.Builder> replacer) {
      this.conditionReplacers.add(replacer);
   }

   @Nullable
   private LootTable.Builder findAndReplaceInLootTableBuilder(LootTable.Builder builder) {
      LootTable lootTable = builder.build();
      Optional<Identifier> randomSequence = this.getPrivateValue(LootTable.class, lootTable, "randomSequence");
      List<LootPool> lootPools = this.getPrivateValue(LootTable.class, lootTable, "pools");
      List<LootItemFunction> lootItemFunctions = this.getPrivateValue(LootTable.class, lootTable, "functions");
      boolean found = false;
      LootTable.Builder newBuilder = new LootTable.Builder();
      newBuilder.setParamSet(lootTable.getParamSet());
      randomSequence.ifPresent(newBuilder::setRandomSequence);

      for (LootItemFunction lootItemFunction : lootItemFunctions) {
         newBuilder.apply(() -> lootItemFunction);
      }

      for (LootPool lootPool : lootPools) {
         found |= this.findAndReplaceInLootPool(lootPool, newBuilder);
      }

      return found ? newBuilder : null;
   }

   private boolean findAndReplaceInLootPool(LootPool lootPool, LootTable.Builder newBuilder) {
      List<LootPoolEntryContainer> lootEntries = this.getPrivateValue(LootPool.class, lootPool, "entries");
      List<LootItemCondition> lootConditions = this.getPrivateValue(LootPool.class, lootPool, "conditions");
      List<LootItemFunction> lootFunctions = this.getPrivateValue(LootPool.class, lootPool, "functions");
      LootPool.Builder poolBuilder = new LootPool.Builder();
      poolBuilder.setRolls(lootPool.getRolls());
      poolBuilder.setBonusRolls(lootPool.getBonusRolls());
      poolBuilder.name(lootPool.getName());
      boolean found = false;

      for (LootPoolEntryContainer lootEntry : lootEntries) {
         found |= this.findAndReplaceInLootEntry(lootEntry, poolBuilder::add);
      }

      for (LootItemCondition lootCondition : lootConditions) {
         if (lootCondition instanceof InvertedLootItemCondition invertedLootItemCondition) {
            LootItemCondition invLootCondition = invertedLootItemCondition.term();
            Consumer<LootItemCondition.Builder> consumer = cond -> poolBuilder.when(InvertedLootItemCondition.invert(cond));
            if (invLootCondition instanceof CompositeLootItemCondition compositeLootItemCondition
               && this.findAndReplaceInComposite(compositeLootItemCondition, consumer)) {
               found = true;
            } else {
               found |= this.replaceCondition(invLootCondition, consumer);
            }
         } else if (lootCondition instanceof CompositeLootItemCondition composite) {
            found |= this.findAndReplaceInComposite(composite, poolBuilder::when);
         } else {
            found |= this.replaceCondition(lootCondition, poolBuilder::when);
         }
      }

      for (LootItemFunction lootFunction : lootFunctions) {
         poolBuilder.apply(() -> lootFunction);
      }

      newBuilder.withPool(poolBuilder);
      return found;
   }

   private boolean findAndReplaceInParentedLootEntry(CompositeEntryBase entry, Consumer<LootPoolEntryContainer.Builder<?>> newBuilder) {
      List<LootPoolEntryContainer> lootEntries = this.getPrivateValue(CompositeEntryBase.class, entry, "children");
      boolean found = false;

      for (LootPoolEntryContainer lootEntry : lootEntries) {
         found |= this.findAndReplaceInLootEntry(lootEntry, newBuilder);
      }

      return found;
   }

   private boolean findAndReplaceInLootEntry(LootPoolEntryContainer entry, Consumer<LootPoolEntryContainer.Builder<?>> newBuilder) {
      List<LootItemCondition> lootConditions = this.getPrivateValue(LootPoolEntryContainer.class, entry, "conditions");
      boolean found = false;
      LootPoolEntryContainer.Builder<?> builder;
      if (entry instanceof CompositeEntryBase compositeEntryBase) {
         Consumer<LootPoolEntryContainer.Builder<?>> consumer;
         if (compositeEntryBase instanceof AlternativesEntry) {
            builder = new AlternativesEntry.Builder();
            consumer = builder::otherwise;
         } else if (compositeEntryBase instanceof SequentialEntry) {
            builder = new SequentialEntry.Builder();
            consumer = builder::then;
         } else {
            if (!(compositeEntryBase instanceof EntryGroup)) {
               throw new IllegalStateException("Unknown CompositeEntryBase type: " + compositeEntryBase.getClass().getName());
            }

            builder = new EntryGroup.Builder();
            consumer = builder::append;
         }

         found |= this.findAndReplaceInParentedLootEntry(compositeEntryBase, consumer);
      } else {
         if (!(entry instanceof LootPoolSingletonContainer singleton)) {
            throw new IllegalStateException("Unknown LootPoolEntryContainer type: " + entry.getClass().getName());
         }

         if (singleton instanceof DynamicLoot dynamicLoot) {
            Identifier name = this.getPrivateValue(DynamicLoot.class, dynamicLoot, "name");
            builder = DynamicLoot.dynamicEntry(name);
         } else if (singleton instanceof EmptyLootItem) {
            builder = EmptyLootItem.emptyItem();
         } else if (singleton instanceof LootItem lootItem) {
            Holder<Item> item = this.getPrivateValue(LootItem.class, lootItem, "item");
            builder = LootItem.lootTableItem(item.value());
         } else if (singleton instanceof TagEntry tagEntry) {
            TagKey<Item> tag = this.getPrivateValue(TagEntry.class, tagEntry, "tag");
            boolean expand = this.<Boolean, TagEntry>getPrivateValue(TagEntry.class, tagEntry, "expand");
            builder = expand ? TagEntry.expandTag(tag) : TagEntry.tagContents(tag);
         } else {
            if (!(singleton instanceof NestedLootTable reference)) {
               throw new IllegalStateException("Unknown LootPoolSingletonContainer type: " + singleton.getClass().getName());
            }

            Either<ResourceKey<LootTable>, LootTable> contents = this.getPrivateValue(NestedLootTable.class, reference, "contents");
            builder = (LootPoolEntryContainer.Builder<?>)contents.map(NestedLootTable::lootTableReference, NestedLootTable::inlineLootTable);
         }

         int weight = this.<Integer, LootPoolSingletonContainer>getPrivateValue(LootPoolSingletonContainer.class, singleton, "weight");
         int quality = this.<Integer, LootPoolSingletonContainer>getPrivateValue(LootPoolSingletonContainer.class, singleton, "quality");
         List<LootItemFunction> functions = this.getPrivateValue(LootPoolSingletonContainer.class, singleton, "functions");
         ((LootPoolSingletonContainer.Builder)builder).setWeight(weight);
         ((LootPoolSingletonContainer.Builder)builder).setQuality(quality);

         for (LootItemFunction function : functions) {
            ((LootPoolSingletonContainer.Builder)builder).apply(() -> function);
         }
      }

      for (LootItemCondition lootCondition : lootConditions) {
         if (lootCondition instanceof CompositeLootItemCondition composite && this.findAndReplaceInComposite(composite, builder::when)) {
            found = true;
         } else {
            found |= this.replaceCondition(lootCondition, builder::when);
         }
      }

      newBuilder.accept(builder);
      return found;
   }

   private boolean findAndReplaceInComposite(CompositeLootItemCondition alternative, Consumer<LootItemCondition.Builder> poolBuilder) {
      List<LootItemCondition> lootConditions = this.getPrivateValue(CompositeLootItemCondition.class, alternative, "terms");
      CompositeLootItemCondition.Builder builder = (CompositeLootItemCondition.Builder)(alternative instanceof AllOfCondition
         ? new AllOfCondition.Builder()
         : (alternative instanceof AnyOfCondition ? new AnyOfCondition.Builder() : null));
      boolean found = false;

      for (LootItemCondition lootCondition : lootConditions) {
         found |= this.replaceCondition(lootCondition, builder::addTerm);
      }

      poolBuilder.accept(builder);
      return found;
   }

   private boolean checkMatchTool(MatchTool lootCondition, Item expected) {
      return lootCondition.predicate().flatMap(ItemPredicate::items).filter(holders -> holders.contains(expected.builtInRegistryHolder())).isPresent();
   }

   private boolean replaceCondition(LootItemCondition lootCondition, Consumer<LootItemCondition.Builder> poolBuilder) {
      for (Function<LootItemCondition, LootItemCondition.Builder> conditionReplacer : this.conditionReplacers) {
         LootItemCondition.Builder newCondition = conditionReplacer.apply(lootCondition);
         if (newCondition != null) {
            poolBuilder.accept(newCondition);
            return true;
         }
      }

      poolBuilder.accept(() -> lootCondition);
      return false;
   }

   private <T, C> T getPrivateValue(Class<C> clazz, C inst, String name) {
      T value = (T)ObfuscationReflectionHelper.getPrivateValue(clazz, inst, name);
      if (value == null) {
         throw new IllegalStateException(clazz.getName() + " is missing field " + name);
      } else {
         return value;
      }
   }
}
