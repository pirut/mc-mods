package net.neoforged.neoforge.common.data.internal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.DamagePredicate;
import net.minecraft.advancements.criterion.DamageSourcePredicate;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.advancements.criterion.EntityEquipmentPredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.EntitySubPredicate;
import net.minecraft.advancements.criterion.EntityTypePredicate;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.ItemUsedOnLocationTrigger;
import net.minecraft.advancements.criterion.PlayerHurtEntityTrigger;
import net.minecraft.advancements.criterion.PlayerInteractTrigger;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.data.advancements.packs.VanillaAdvancementProvider;
import net.minecraft.data.advancements.packs.VanillaHusbandryAdvancements;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.predicates.InvertedLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.advancements.critereon.ItemAbilityPredicate;
import net.neoforged.neoforge.common.advancements.critereon.PiglinCurrencyItemPredicate;
import net.neoforged.neoforge.common.advancements.critereon.PiglinNeutralArmorEntityPredicate;
import net.neoforged.neoforge.common.advancements.critereon.SnowBootsEntityPredicate;
import net.neoforged.neoforge.common.advancements.critereon.TridentEntityPredicate;
import org.jspecify.annotations.Nullable;

public class NeoForgeAdvancementProvider extends AdvancementProvider {
   public NeoForgeAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
      super(output, registries, getVanillaAdvancementProviders(output, registries));
   }

   private static List<AdvancementSubProvider> getVanillaAdvancementProviders(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
      List<BiFunction<Criterion<?>, HolderLookup.Provider, Criterion<?>>> criteriaReplacers = new ArrayList<>();
      criteriaReplacers.add(
         replaceMatchToolCriteria(ItemAbilities.AXE_WAX_OFF, getPrivateValue(VanillaHusbandryAdvancements.class, null, "WAX_SCRAPING_TOOLS"))
      );
      criteriaReplacers.add(
         replaceInteractCriteria(
            ItemPredicate.Builder.item()
               .withComponents(
                  DataComponentMatchers.Builder.components()
                     .partial(ItemAbilityPredicate.TYPE, new ItemAbilityPredicate(ItemAbilities.SHEARS_REMOVE_ARMOR))
                     .build()
               )
               .build(),
            Items.SHEARS
         )
      );
      criteriaReplacers.add(
         replaceInteractCriteria(
            ItemPredicate.Builder.item()
               .withComponents(
                  DataComponentMatchers.Builder.components().partial(PiglinCurrencyItemPredicate.TYPE, PiglinCurrencyItemPredicate.INSTANCE).build()
               )
               .build(),
            PiglinAi.BARTERING_ITEM
         )
      );
      criteriaReplacers.add(
         replaceLootEntityPredicate(
            helper -> {
               if (helper.clearEquipmentIfMatches(
                  predicate -> {
                     if (predicate.head().filter(item -> predicateMatches(item, ItemTags.PIGLIN_SAFE_ARMOR)).isPresent()) {
                        return true;
                     } else if (predicate.chest().filter(item -> predicateMatches(item, ItemTags.PIGLIN_SAFE_ARMOR)).isPresent()) {
                        return true;
                     } else {
                        return predicate.legs().filter(item -> predicateMatches(item, ItemTags.PIGLIN_SAFE_ARMOR)).isPresent()
                           ? true
                           : predicate.feet().filter(item -> predicateMatches(item, ItemTags.PIGLIN_SAFE_ARMOR)).isPresent();
                     }
                  }
               )) {
                  helper.replaceSubPredicate(PiglinNeutralArmorEntityPredicate.INSTANCE);
                  return true;
               } else {
                  return false;
               }
            }
         )
      );
      criteriaReplacers.add(replacePlayerHurtEntityCriteria(helper -> {
         if (helper.clearTypeIfMatches(EntityType.TRIDENT)) {
            helper.replaceSubPredicate(TridentEntityPredicate.INSTANCE);
            return true;
         } else {
            return false;
         }
      }));
      criteriaReplacers.add(replaceLootEntityPredicate(helper -> {
         if (helper.clearEquipmentIfMatches(predicate -> predicate.feet().filter(item -> predicateMatches(item, Items.LEATHER_BOOTS)).isPresent())) {
            helper.replaceSubPredicate(SnowBootsEntityPredicate.INSTANCE);
            return true;
         } else {
            return false;
         }
      }));
      List<AdvancementSubProvider> subProviders = getPrivateValue(
         AdvancementProvider.class, VanillaAdvancementProvider.create(output, registries), "subProviders"
      );
      return subProviders.stream()
         .map(vanillaProvider -> new NeoForgeAdvancementProvider.NeoForgeAdvancementGenerator(vanillaProvider, criteriaReplacers))
         .toList();
   }

   private static BiFunction<Criterion<?>, HolderLookup.Provider, Criterion<?>> replaceMatchToolCriteria(ItemAbility itemAbility, ItemLike... targetItem) {
      UnaryOperator<LootItemCondition> replacer = condition -> condition instanceof MatchTool toolMatch
            && toolMatch.predicate().filter(predicate -> predicateMatches(predicate, targetItem)).isPresent()
         ? new MatchTool(
            Optional.of(
               ItemPredicate.Builder.item()
                  .withComponents(DataComponentMatchers.Builder.components().partial(ItemAbilityPredicate.TYPE, new ItemAbilityPredicate(itemAbility)).build())
                  .build()
            )
         )
         : null;
      return (criterion, registries) -> {
         if (criterion.trigger() instanceof ItemUsedOnLocationTrigger trigger
            && criterion.triggerInstance() instanceof ItemUsedOnLocationTrigger.TriggerInstance instance) {
            ContextAwarePredicate newLocation = replaceConditions(instance.location().orElse(null), replacer, condition -> false);
            if (newLocation != null) {
               return new Criterion<>(trigger, new ItemUsedOnLocationTrigger.TriggerInstance(instance.player(), Optional.of(newLocation)));
            }
         }

         return null;
      };
   }

   private static BiFunction<Criterion<?>, HolderLookup.Provider, Criterion<?>> replaceInteractCriteria(ItemPredicate replacement, ItemLike... targetItem) {
      return (criterion, registries) -> criterion.trigger() instanceof PlayerInteractTrigger trigger
            && criterion.triggerInstance() instanceof PlayerInteractTrigger.TriggerInstance instance
            && instance.item().filter(predicate -> predicateMatches(predicate, targetItem)).isPresent()
         ? new Criterion<>(trigger, new PlayerInteractTrigger.TriggerInstance(instance.player(), Optional.of(replacement), instance.entity()))
         : null;
   }

   private static BiFunction<Criterion<?>, HolderLookup.Provider, Criterion<?>> replacePlayerHurtEntityCriteria(
      Predicate<NeoForgeAdvancementProvider.EntityPredicateReplacementHelper> predicateHelper
   ) {
      return (criterion, registries) -> {
         if (criterion.trigger() instanceof PlayerHurtEntityTrigger trigger
            && criterion.triggerInstance() instanceof PlayerHurtEntityTrigger.TriggerInstance instance
            && instance.damage().isPresent()) {
            DamagePredicate damagePredicate = instance.damage().get();
            if (damagePredicate.type().isPresent()) {
               DamageSourcePredicate sourcePredicate = damagePredicate.type().get();
               if (sourcePredicate.directEntity().isPresent()) {
                  NeoForgeAdvancementProvider.EntityPredicateReplacementHelper helper = new NeoForgeAdvancementProvider.EntityPredicateReplacementHelper(
                     sourcePredicate.directEntity().get()
                  );
                  if (predicateHelper.test(helper)) {
                     DamageSourcePredicate replacementSourcePredicate = new DamageSourcePredicate(
                        sourcePredicate.tags(), Optional.of(helper.create()), sourcePredicate.sourceEntity(), sourcePredicate.isDirect()
                     );
                     DamagePredicate replacement = new DamagePredicate(
                        damagePredicate.dealtDamage(),
                        damagePredicate.takenDamage(),
                        damagePredicate.sourceEntity(),
                        damagePredicate.blocked(),
                        Optional.of(replacementSourcePredicate)
                     );
                     return new Criterion<>(
                        trigger, new PlayerHurtEntityTrigger.TriggerInstance(instance.player(), Optional.of(replacement), instance.entity())
                     );
                  }
               }
            }
         }

         return null;
      };
   }

   private static boolean predicateMatches(ItemPredicate predicate, ItemLike... targets) {
      Optional<HolderSet<Item>> items = predicate.items();
      if (items.isEmpty()) {
         return false;
      } else {
         HolderSet<Item> holders = items.get();

         for (ItemLike target : targets) {
            if (!holders.contains(target.asItem().builtInRegistryHolder())) {
               return false;
            }
         }

         return true;
      }
   }

   private static boolean predicateMatches(ItemPredicate predicate, TagKey<Item> tagKey) {
      return predicate.items().orElse(HolderSet.empty()).unwrapKey().map(k -> k == tagKey).orElse(false);
   }

   private static BiFunction<Criterion<?>, HolderLookup.Provider, Criterion<?>> replaceLootEntityPredicate(
      Predicate<NeoForgeAdvancementProvider.EntityPredicateReplacementHelper> predicateHelper
   ) {
      return replacePlayerPredicate(
         condition -> {
            boolean invert = false;
            if (condition instanceof InvertedLootItemCondition inverted) {
               condition = inverted.term();
               invert = true;
            }

            if (condition instanceof LootItemEntityPropertyCondition entityPropertyCondition) {
               Optional<EntityPredicate> predicate = entityPropertyCondition.predicate();
               if (predicate.isPresent()) {
                  NeoForgeAdvancementProvider.EntityPredicateReplacementHelper helper = new NeoForgeAdvancementProvider.EntityPredicateReplacementHelper(
                     predicate.get()
                  );
                  if (predicateHelper.test(helper)) {
                     LootItemCondition.Builder conditionBuilder = LootItemEntityPropertyCondition.hasProperties(
                        entityPropertyCondition.entityTarget(), helper.create()
                     );
                     if (invert) {
                        return conditionBuilder.invert().build();
                     }

                     return conditionBuilder.build();
                  }
               }
            }

            return null;
         },
         condition -> true
      );
   }

   private static BiFunction<Criterion<?>, HolderLookup.Provider, Criterion<?>> replacePlayerPredicate(
      UnaryOperator<LootItemCondition> replacer, Predicate<LootItemCondition> shouldSkipReplacement
   ) {
      return (criterion, registries) -> {
         if (criterion.triggerInstance() instanceof SimpleCriterionTrigger.SimpleInstance simpleInstance) {
            ContextAwarePredicate newPlayer = replaceConditions(simpleInstance.player().orElse(null), replacer, shouldSkipReplacement);
            if (newPlayer != null) {
               return replacePlayerPredicate(criterion, newPlayer, registries);
            }
         }

         return null;
      };
   }

   private static <T extends SimpleCriterionTrigger.SimpleInstance> Criterion<T> replacePlayerPredicate(
      Criterion<T> old, ContextAwarePredicate newPlayer, HolderLookup.Provider registries
   ) {
      Codec<T> codec = old.trigger().codec();
      RegistryOps<JsonElement> registryops = registries.createSerializationContext(JsonOps.INSTANCE);
      return (Criterion<T>)codec.encodeStart(registryops, old.triggerInstance())
         .flatMap(
            element -> {
               if (element instanceof JsonObject object && object.has("player")) {
                  object.add(
                     "player",
                     (JsonElement)ContextAwarePredicate.CODEC
                        .encodeStart(registryops, newPlayer)
                        .getOrThrow(error -> new IllegalStateException("Unable to serialize new player predicate"))
                  );
                  return codec.parse(registryops, object);
               } else {
                  return DataResult.error(() -> "Serialized instance does not contain a 'player' element");
               }
            }
         )
         .map(old.trigger()::createCriterion)
         .getOrThrow(error -> new IllegalStateException("Unable to convert criterion serialization and replacement: " + error));
   }

   @Nullable
   private static ContextAwarePredicate replaceConditions(
      @Nullable ContextAwarePredicate basePredicate, UnaryOperator<LootItemCondition> replacer, Predicate<LootItemCondition> shouldSkipReplacement
   ) {
      if (basePredicate == null) {
         return null;
      } else {
         List<LootItemCondition> conditions = getPrivateValue(ContextAwarePredicate.class, basePredicate, "conditions");
         if (!conditions.isEmpty()) {
            boolean shouldReplace = false;
            List<LootItemCondition> clonedConditions = new ArrayList<>(conditions.size());

            for (LootItemCondition condition : conditions) {
               LootItemCondition replacement = replacer.apply(condition);
               if (replacement != null) {
                  if (shouldReplace && shouldSkipReplacement.test(replacement)) {
                     continue;
                  }

                  shouldReplace = true;
                  condition = replacement;
               }

               clonedConditions.add(condition);
            }

            if (shouldReplace) {
               return ContextAwarePredicate.create(clonedConditions.toArray(LootItemCondition[]::new));
            }
         }

         return null;
      }
   }

   private static <T, C> T getPrivateValue(Class<C> clazz, @Nullable C inst, String name) {
      T value = (T)ObfuscationReflectionHelper.getPrivateValue(clazz, inst, name);
      if (value == null) {
         throw new IllegalStateException(clazz.getName() + " is missing field " + name);
      } else {
         return value;
      }
   }

   private static class EntityPredicateReplacementHelper {
      private final EntityPredicate source;
      private Optional<EntityTypePredicate> entityType;
      private Optional<EntityEquipmentPredicate> equipment;
      private Optional<EntitySubPredicate> subPredicate;

      public EntityPredicateReplacementHelper(EntityPredicate source) {
         this.source = source;
         this.entityType = this.source.entityType();
         this.equipment = this.source.equipment();
         this.subPredicate = this.source.subPredicate();
      }

      public boolean clearTypeIfMatches(EntityType<?> type) {
         if (this.entityType.isPresent() && this.entityType.get().matches(type)) {
            this.entityType = Optional.empty();
            return true;
         } else {
            return false;
         }
      }

      public boolean clearEquipmentIfMatches(Predicate<EntityEquipmentPredicate> shouldReplace) {
         if (this.equipment.isPresent() && shouldReplace.test(this.equipment.get())) {
            this.equipment = Optional.empty();
            return true;
         } else {
            return false;
         }
      }

      public void replaceSubPredicate(EntitySubPredicate predicate) {
         if (this.subPredicate.isPresent()) {
            throw new IllegalStateException("Attempting to replace an entity predicate that already has a sub predicate");
         } else {
            this.subPredicate = Optional.of(predicate);
         }
      }

      public EntityPredicate create() {
         return new EntityPredicate(
            this.entityType,
            this.source.distanceToPlayer(),
            this.source.movement(),
            this.source.location(),
            this.source.effects(),
            this.source.nbt(),
            this.source.flags(),
            this.equipment,
            this.subPredicate,
            this.source.periodicTick(),
            this.source.vehicle(),
            this.source.passenger(),
            this.source.targetedEntity(),
            this.source.team(),
            this.source.slots(),
            this.source.components()
         );
      }
   }

   private record NeoForgeAdvancementGenerator(
      AdvancementSubProvider vanillaProvider, List<BiFunction<Criterion<?>, HolderLookup.Provider, Criterion<?>>> criteriaReplacers
   ) implements AdvancementSubProvider {
      @Override
      public void generate(final HolderLookup.Provider registries, Consumer<AdvancementHolder> saver) {
         var registriesWithAnyTag = new HolderLookup.Provider() {
            @Override
            public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
               return registries.listRegistryKeys();
            }

            @Override
            public <T> Optional<? extends HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> p_256285_) {
               return registries.lookup(p_256285_);
            }

            @Override
            public <V> RegistryOps<V> createSerializationContext(DynamicOps<V> p_326817_) {
               return RegistryOps.create(
                  p_326817_,
                  new RegistryOps.RegistryInfoLookup() {
                     @Override
                     public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> registry) {
                        Registry<T> builtInRegistry = (Registry<T>)BuiltInRegistries.REGISTRY.getValue(registry.identifier());
                        return registries.lookup(registry)
                           .map(lookup -> new RegistryOps.RegistryInfo<>(builtInRegistry, new HolderLookup.RegistryLookup.Delegate<T>() {
                              @Override
                              public HolderLookup.RegistryLookup<T> parent() {
                                 return lookup;
                              }

                              @Override
                              public boolean canSerializeIn(HolderOwner<T> p_255875_) {
                                 return this.parent().canSerializeIn(p_255875_);
                              }

                              @Override
                              public Optional<HolderSet.Named<T>> get(TagKey<T> tagKey) {
                                 Optional<HolderSet.Named<T>> ret = HolderLookup.RegistryLookup.Delegate.super.get(tagKey);
                                 if (ret.isEmpty()) {
                                    ret = Optional.of(HolderSet.emptyNamed(lookup, tagKey));
                                 }

                                 return ret;
                              }
                           }, lookup.registryLifecycle()));
                     }
                  }
               );
            }
         };
         this.vanillaProvider.generate(registriesWithAnyTag, advancementHolder -> {
            Advancement.Builder newBuilder = this.findAndReplaceInHolder(advancementHolder, registriesWithAnyTag);
            if (newBuilder != null) {
               newBuilder.save(saver, advancementHolder.id());
            }
         });
      }

      @Nullable
      private Advancement.Builder findAndReplaceInHolder(AdvancementHolder advancementHolder, HolderLookup.Provider registries) {
         Advancement advancement = advancementHolder.value();
         Advancement.Builder builder = Advancement.Builder.advancement();
         boolean hasReplaced = false;

         for (Entry<String, Criterion<?>> entry : advancement.criteria().entrySet()) {
            Criterion<?> criterion = entry.getValue();

            for (BiFunction<Criterion<?>, HolderLookup.Provider, Criterion<?>> criteriaReplacer : this.criteriaReplacers) {
               Criterion<?> replacedCriterion = criteriaReplacer.apply(criterion, registries);
               if (replacedCriterion != null) {
                  hasReplaced = true;
                  criterion = replacedCriterion;
               }
            }

            builder.addCriterion(entry.getKey(), criterion);
         }

         if (!hasReplaced) {
            return null;
         } else {
            advancement.parent().ifPresent(builder::parent);
            advancement.display().ifPresent(builder::display);
            builder.rewards(advancement.rewards());
            builder.requirements(advancement.requirements());
            if (advancement.sendsTelemetryEvent()) {
               builder.sendsTelemetryEvent();
            }

            return builder;
         }
      }
   }
}
