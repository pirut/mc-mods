package net.neoforged.neoforge.common;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.criterion.EntitySubPredicate;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.attribute.AttributeType;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.CrashReportCallables;
import net.neoforged.fml.FMLVersion;
import net.neoforged.fml.Logging;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.VersionChecker;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.config.ModConfigEvent.Loading;
import net.neoforged.fml.event.config.ModConfigEvent.Reloading;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import net.neoforged.neoforge.capabilities.CapabilityHooks;
import net.neoforged.neoforge.common.advancements.critereon.ItemAbilityPredicate;
import net.neoforged.neoforge.common.advancements.critereon.PiglinCurrencyItemPredicate;
import net.neoforged.neoforge.common.advancements.critereon.PiglinNeutralArmorEntityPredicate;
import net.neoforged.neoforge.common.advancements.critereon.SnowBootsEntityPredicate;
import net.neoforged.neoforge.common.advancements.critereon.TridentEntityPredicate;
import net.neoforged.neoforge.common.conditions.AlwaysCondition;
import net.neoforged.neoforge.common.conditions.AndCondition;
import net.neoforged.neoforge.common.conditions.FeatureFlagsEnabledCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.conditions.NeverCondition;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.OrCondition;
import net.neoforged.neoforge.common.conditions.RegisteredCondition;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;
import net.neoforged.neoforge.common.config.NeoForgeCommonConfig;
import net.neoforged.neoforge.common.config.NeoForgeServerConfig;
import net.neoforged.neoforge.common.crafting.BlockTagIngredient;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.common.crafting.CustomDisplayIngredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.DifferenceIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.common.crafting.IntersectionIngredient;
import net.neoforged.neoforge.common.loot.AddTableLootModifier;
import net.neoforged.neoforge.common.loot.CanItemPerformAbility;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;
import net.neoforged.neoforge.common.util.SelfTest;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.common.world.NeoForgeAttributeTypes;
import net.neoforged.neoforge.common.world.NeoForgeEnvironmentAttributes;
import net.neoforged.neoforge.common.world.NoneBiomeModifier;
import net.neoforged.neoforge.common.world.NoneStructureModifier;
import net.neoforged.neoforge.common.world.StructureModifier;
import net.neoforged.neoforge.common.world.StructureModifiers;
import net.neoforged.neoforge.data.loading.DatagenModLoader;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.crafting.CompoundFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.CustomDisplayFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.DifferenceFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.FluidIngredientCodecs;
import net.neoforged.neoforge.fluids.crafting.FluidIngredientType;
import net.neoforged.neoforge.fluids.crafting.IntersectionFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SimpleFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.display.FluidSlotDisplay;
import net.neoforged.neoforge.fluids.crafting.display.FluidStackSlotDisplay;
import net.neoforged.neoforge.fluids.crafting.display.FluidTagSlotDisplay;
import net.neoforged.neoforge.network.ConfigSync;
import net.neoforged.neoforge.network.DualStackUtils;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.NeoForgeRegistriesSetup;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.registries.holdersets.AndHolderSet;
import net.neoforged.neoforge.registries.holdersets.AnyHolderSet;
import net.neoforged.neoforge.registries.holdersets.HolderSetType;
import net.neoforged.neoforge.registries.holdersets.NotHolderSet;
import net.neoforged.neoforge.registries.holdersets.OrHolderSet;
import net.neoforged.neoforge.server.command.EnumArgument;
import net.neoforged.neoforge.server.command.ModIdArgument;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jspecify.annotations.Nullable;

@Mod("neoforge")
public class NeoForgeMod {
   public static final String MOD_ID = "neoforge";
   public static final String VERSION_CHECK_CAT = "version_checking";
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Marker NEOFORGEMOD = MarkerManager.getMarker("NEOFORGE-MOD");
   private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, "neoforge");
   private static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, "neoforge");
   private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIER_SERIALIZERS = DeferredRegister.create(
      NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "neoforge"
   );
   private static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS = DeferredRegister.create(
      NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, "neoforge"
   );
   private static final DeferredRegister<MapCodec<? extends StructureModifier>> STRUCTURE_MODIFIER_SERIALIZERS = DeferredRegister.create(
      NeoForgeRegistries.Keys.STRUCTURE_MODIFIER_SERIALIZERS, "neoforge"
   );
   private static final DeferredRegister<HolderSetType> HOLDER_SET_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.HOLDER_SET_TYPES, "neoforge");
   private static final DeferredRegister<AttributeType<?>> ATTRIBUTE_TYPES = DeferredRegister.create(Registries.ATTRIBUTE_TYPE, "neoforge");
   private static final DeferredRegister<EnvironmentAttribute<?>> ENVIRONMENT_ATTRIBUTES = DeferredRegister.create(Registries.ENVIRONMENT_ATTRIBUTE, "neoforge");
   private static final Holder<ArgumentTypeInfo<?, ?>> ENUM_COMMAND_ARGUMENT_TYPE = COMMAND_ARGUMENT_TYPES.register(
      "enum", () -> ArgumentTypeInfos.registerByClass(EnumArgument.class, new EnumArgument.Info())
   );
   private static final DeferredHolder<ArgumentTypeInfo<?, ?>, SingletonArgumentInfo<ModIdArgument>> MODID_COMMAND_ARGUMENT_TYPE = COMMAND_ARGUMENT_TYPES.register(
      "modid", () -> ArgumentTypeInfos.registerByClass(ModIdArgument.class, SingletonArgumentInfo.contextFree(ModIdArgument::modIdArgument))
   );
   public static final Holder<Attribute> SWIM_SPEED = ATTRIBUTES.register(
      "swim_speed", () -> new PercentageAttribute("neoforge.swim_speed", 1.0, 0.0, 1024.0).setSyncable(true)
   );
   public static final Holder<Attribute> NAMETAG_DISTANCE = ATTRIBUTES.register(
      "nametag_distance", () -> new RangedAttribute("neoforge.name_tag_distance", 32.0, 0.0, 32.0).setSyncable(true).setSentiment(Attribute.Sentiment.NEUTRAL)
   );
   public static final Holder<Attribute> CREATIVE_FLIGHT = ATTRIBUTES.register(
      "creative_flight", () -> new BooleanAttribute("neoforge.creative_flight", false).setSyncable(true)
   );
   public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<AddTableLootModifier>> ADD_TABLE_LOOT_MODIFIER_TYPE = GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(
      "add_table", () -> AddTableLootModifier.CODEC
   );
   public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<NoneBiomeModifier>> NONE_BIOME_MODIFIER_TYPE = BIOME_MODIFIER_SERIALIZERS.register(
      "none", () -> MapCodec.unit(NoneBiomeModifier.INSTANCE)
   );
   public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<BiomeModifiers.AddFeaturesBiomeModifier>> ADD_FEATURES_BIOME_MODIFIER_TYPE = BIOME_MODIFIER_SERIALIZERS.register(
      "add_features",
      () -> RecordCodecBuilder.mapCodec(
         builder -> builder.group(
               Biome.LIST_CODEC.fieldOf("biomes").forGetter(BiomeModifiers.AddFeaturesBiomeModifier::biomes),
               PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(BiomeModifiers.AddFeaturesBiomeModifier::features),
               GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(BiomeModifiers.AddFeaturesBiomeModifier::step)
            )
            .apply(builder, BiomeModifiers.AddFeaturesBiomeModifier::new)
      )
   );
   public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<BiomeModifiers.RemoveFeaturesBiomeModifier>> REMOVE_FEATURES_BIOME_MODIFIER_TYPE = BIOME_MODIFIER_SERIALIZERS.register(
      "remove_features",
      () -> RecordCodecBuilder.mapCodec(
         builder -> builder.group(
               Biome.LIST_CODEC.fieldOf("biomes").forGetter(BiomeModifiers.RemoveFeaturesBiomeModifier::biomes),
               PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(BiomeModifiers.RemoveFeaturesBiomeModifier::features),
               Codec.either(GenerationStep.Decoration.CODEC.listOf(), GenerationStep.Decoration.CODEC)
                  .xmap(
                     either -> (Set)either.map(Set::copyOf, Set::of),
                     set -> set.size() == 1 ? Either.right(set.toArray(GenerationStep.Decoration[]::new)[0]) : Either.left(List.copyOf(set))
                  )
                  .optionalFieldOf("steps", EnumSet.allOf(GenerationStep.Decoration.class))
                  .forGetter(BiomeModifiers.RemoveFeaturesBiomeModifier::steps)
            )
            .apply(builder, BiomeModifiers.RemoveFeaturesBiomeModifier::new)
      )
   );
   public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<BiomeModifiers.AddSpawnsBiomeModifier>> ADD_SPAWNS_BIOME_MODIFIER_TYPE = BIOME_MODIFIER_SERIALIZERS.register(
      "add_spawns",
      () -> RecordCodecBuilder.mapCodec(
         builder -> builder.group(
               Biome.LIST_CODEC.fieldOf("biomes").forGetter(BiomeModifiers.AddSpawnsBiomeModifier::biomes),
               Codec.either(WeightedList.codec(MobSpawnSettings.SpawnerData.CODEC), Weighted.codec(MobSpawnSettings.SpawnerData.CODEC))
                  .xmap(
                     either -> (WeightedList)either.map(Function.identity(), xva$0 -> WeightedList.of(xva$0)),
                     list -> list.unwrap().size() == 1 ? Either.right((Weighted)list.unwrap().get(0)) : Either.left(list)
                  )
                  .fieldOf("spawners")
                  .forGetter(BiomeModifiers.AddSpawnsBiomeModifier::spawners)
            )
            .apply(builder, BiomeModifiers.AddSpawnsBiomeModifier::new)
      )
   );
   public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<BiomeModifiers.RemoveSpawnsBiomeModifier>> REMOVE_SPAWNS_BIOME_MODIFIER_TYPE = BIOME_MODIFIER_SERIALIZERS.register(
      "remove_spawns",
      () -> RecordCodecBuilder.mapCodec(
         builder -> builder.group(
               Biome.LIST_CODEC.fieldOf("biomes").forGetter(BiomeModifiers.RemoveSpawnsBiomeModifier::biomes),
               RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).fieldOf("entity_types").forGetter(BiomeModifiers.RemoveSpawnsBiomeModifier::entityTypes)
            )
            .apply(builder, BiomeModifiers.RemoveSpawnsBiomeModifier::new)
      )
   );
   public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<BiomeModifiers.AddCarversBiomeModifier>> ADD_CARVERS_BIOME_MODIFIER_TYPE = BIOME_MODIFIER_SERIALIZERS.register(
      "add_carvers",
      () -> RecordCodecBuilder.mapCodec(
         builder -> builder.group(
               Biome.LIST_CODEC.fieldOf("biomes").forGetter(BiomeModifiers.AddCarversBiomeModifier::biomes),
               ConfiguredWorldCarver.LIST_CODEC.fieldOf("carvers").forGetter(BiomeModifiers.AddCarversBiomeModifier::carvers)
            )
            .apply(builder, BiomeModifiers.AddCarversBiomeModifier::new)
      )
   );
   public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<BiomeModifiers.RemoveCarversBiomeModifier>> REMOVE_CARVERS_BIOME_MODIFIER_TYPE = BIOME_MODIFIER_SERIALIZERS.register(
      "remove_carvers",
      () -> RecordCodecBuilder.mapCodec(
         builder -> builder.group(
               Biome.LIST_CODEC.fieldOf("biomes").forGetter(BiomeModifiers.RemoveCarversBiomeModifier::biomes),
               ConfiguredWorldCarver.LIST_CODEC.fieldOf("carvers").forGetter(BiomeModifiers.RemoveCarversBiomeModifier::carvers)
            )
            .apply(builder, BiomeModifiers.RemoveCarversBiomeModifier::new)
      )
   );
   public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<BiomeModifiers.AddSpawnCostsBiomeModifier>> ADD_SPAWN_COSTS_BIOME_MODIFIER_TYPE = BIOME_MODIFIER_SERIALIZERS.register(
      "add_spawn_costs",
      () -> RecordCodecBuilder.mapCodec(
         builder -> builder.group(
               Biome.LIST_CODEC.fieldOf("biomes").forGetter(BiomeModifiers.AddSpawnCostsBiomeModifier::biomes),
               RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).fieldOf("entity_types").forGetter(BiomeModifiers.AddSpawnCostsBiomeModifier::entityTypes),
               MobSpawnSettings.MobSpawnCost.CODEC.fieldOf("spawn_cost").forGetter(BiomeModifiers.AddSpawnCostsBiomeModifier::spawnCost)
            )
            .apply(builder, BiomeModifiers.AddSpawnCostsBiomeModifier::new)
      )
   );
   public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<BiomeModifiers.RemoveSpawnCostsBiomeModifier>> REMOVE_SPAWN_COSTS_BIOME_MODIFIER_TYPE = BIOME_MODIFIER_SERIALIZERS.register(
      "remove_spawn_costs",
      () -> RecordCodecBuilder.mapCodec(
         builder -> builder.group(
               Biome.LIST_CODEC.fieldOf("biomes").forGetter(BiomeModifiers.RemoveSpawnCostsBiomeModifier::biomes),
               RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE)
                  .fieldOf("entity_types")
                  .forGetter(BiomeModifiers.RemoveSpawnCostsBiomeModifier::entityTypes)
            )
            .apply(builder, BiomeModifiers.RemoveSpawnCostsBiomeModifier::new)
      )
   );
   public static final DeferredHolder<MapCodec<? extends StructureModifier>, MapCodec<NoneStructureModifier>> NONE_STRUCTURE_MODIFIER_TYPE = STRUCTURE_MODIFIER_SERIALIZERS.register(
      "none", () -> MapCodec.unit(NoneStructureModifier.INSTANCE)
   );
   public static final DeferredHolder<MapCodec<? extends StructureModifier>, MapCodec<StructureModifiers.AddSpawnsStructureModifier>> ADD_SPAWNS_STRUCTURE_MODIFIER_TYPE = STRUCTURE_MODIFIER_SERIALIZERS.register(
      "add_spawns",
      () -> RecordCodecBuilder.mapCodec(
         builder -> builder.group(
               RegistryCodecs.homogeneousList(Registries.STRUCTURE, Structure.DIRECT_CODEC)
                  .fieldOf("structures")
                  .forGetter(StructureModifiers.AddSpawnsStructureModifier::structures),
               Codec.either(WeightedList.codec(MobSpawnSettings.SpawnerData.CODEC), Weighted.codec(MobSpawnSettings.SpawnerData.CODEC))
                  .xmap(
                     either -> (WeightedList)either.map(Function.identity(), xva$0 -> WeightedList.of(xva$0)),
                     list -> list.unwrap().size() == 1 ? Either.right((Weighted)list.unwrap().get(0)) : Either.left(list)
                  )
                  .fieldOf("spawners")
                  .forGetter(StructureModifiers.AddSpawnsStructureModifier::spawners)
            )
            .apply(builder, StructureModifiers.AddSpawnsStructureModifier::new)
      )
   );
   public static final DeferredHolder<MapCodec<? extends StructureModifier>, MapCodec<StructureModifiers.RemoveSpawnsStructureModifier>> REMOVE_SPAWNS_STRUCTURE_MODIFIER_TYPE = STRUCTURE_MODIFIER_SERIALIZERS.register(
      "remove_spawns",
      () -> RecordCodecBuilder.mapCodec(
         builder -> builder.group(
               RegistryCodecs.homogeneousList(Registries.STRUCTURE, Structure.DIRECT_CODEC)
                  .fieldOf("structures")
                  .forGetter(StructureModifiers.RemoveSpawnsStructureModifier::structures),
               RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE)
                  .fieldOf("entity_types")
                  .forGetter(StructureModifiers.RemoveSpawnsStructureModifier::entityTypes)
            )
            .apply(builder, StructureModifiers.RemoveSpawnsStructureModifier::new)
      )
   );
   public static final DeferredHolder<MapCodec<? extends StructureModifier>, MapCodec<StructureModifiers.ClearSpawnsStructureModifier>> CLEAR_SPAWNS_STRUCTURE_MODIFIER_TYPE = STRUCTURE_MODIFIER_SERIALIZERS.register(
      "clear_spawns",
      () -> RecordCodecBuilder.mapCodec(
         builder -> builder.group(
               RegistryCodecs.homogeneousList(Registries.STRUCTURE, Structure.DIRECT_CODEC)
                  .fieldOf("structures")
                  .forGetter(StructureModifiers.ClearSpawnsStructureModifier::structures),
               Codec.either(MobCategory.CODEC.listOf(), MobCategory.CODEC)
                  .xmap(
                     either -> (Set)either.map(Set::copyOf, Set::of),
                     set -> set.size() == 1 ? Either.right(set.toArray(MobCategory[]::new)[0]) : Either.left(List.copyOf(set))
                  )
                  .optionalFieldOf("categories", EnumSet.allOf(MobCategory.class))
                  .forGetter(StructureModifiers.ClearSpawnsStructureModifier::categories)
            )
            .apply(builder, StructureModifiers.ClearSpawnsStructureModifier::new)
      )
   );
   public static final Holder<HolderSetType> ANY_HOLDER_SET = HOLDER_SET_TYPES.register("any", AnyHolderSet.Type::new);
   public static final Holder<HolderSetType> AND_HOLDER_SET = HOLDER_SET_TYPES.register("and", AndHolderSet.Type::new);
   public static final Holder<HolderSetType> OR_HOLDER_SET = HOLDER_SET_TYPES.register("or", OrHolderSet.Type::new);
   public static final Holder<HolderSetType> NOT_HOLDER_SET = HOLDER_SET_TYPES.register("not", NotHolderSet.Type::new);
   private static final DeferredRegister<SlotDisplay.Type<?>> SLOT_DISPLAY_TYPES = DeferredRegister.create(Registries.SLOT_DISPLAY, "neoforge");
   public static final DeferredHolder<SlotDisplay.Type<?>, SlotDisplay.Type<FluidSlotDisplay>> FLUID_SLOT_DISPLAY = SLOT_DISPLAY_TYPES.register(
      "fluid", () -> new SlotDisplay.Type<>(FluidSlotDisplay.MAP_CODEC, FluidSlotDisplay.STREAM_CODEC)
   );
   public static final DeferredHolder<SlotDisplay.Type<?>, SlotDisplay.Type<FluidStackSlotDisplay>> FLUID_STACK_SLOT_DISPLAY = SLOT_DISPLAY_TYPES.register(
      "fluid_stack", () -> new SlotDisplay.Type<>(FluidStackSlotDisplay.MAP_CODEC, FluidStackSlotDisplay.STREAM_CODEC)
   );
   public static final DeferredHolder<SlotDisplay.Type<?>, SlotDisplay.Type<FluidTagSlotDisplay>> FLUID_TAG_SLOT_DISPLAY = SLOT_DISPLAY_TYPES.register(
      "fluid_tag", () -> new SlotDisplay.Type<>(FluidTagSlotDisplay.MAP_CODEC, FluidTagSlotDisplay.STREAM_CODEC)
   );
   private static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, "neoforge");
   public static final DeferredHolder<IngredientType<?>, IngredientType<CompoundIngredient>> COMPOUND_INGREDIENT_TYPE = INGREDIENT_TYPES.register(
      "compound", () -> new IngredientType<>(CompoundIngredient.CODEC)
   );
   public static final DeferredHolder<IngredientType<?>, IngredientType<DataComponentIngredient>> DATA_COMPONENT_INGREDIENT_TYPE = INGREDIENT_TYPES.register(
      "components", () -> new IngredientType<>(DataComponentIngredient.CODEC)
   );
   public static final DeferredHolder<IngredientType<?>, IngredientType<DifferenceIngredient>> DIFFERENCE_INGREDIENT_TYPE = INGREDIENT_TYPES.register(
      "difference", () -> new IngredientType<>(DifferenceIngredient.CODEC)
   );
   public static final DeferredHolder<IngredientType<?>, IngredientType<IntersectionIngredient>> INTERSECTION_INGREDIENT_TYPE = INGREDIENT_TYPES.register(
      "intersection", () -> new IngredientType<>(IntersectionIngredient.CODEC)
   );
   public static final DeferredHolder<IngredientType<?>, IngredientType<BlockTagIngredient>> BLOCK_TAG_INGREDIENT = INGREDIENT_TYPES.register(
      "block_tag", () -> new IngredientType<>(BlockTagIngredient.CODEC)
   );
   public static final DeferredHolder<IngredientType<?>, IngredientType<CustomDisplayIngredient>> CUSTOM_DISPLAY_INGREDIENT = INGREDIENT_TYPES.register(
      "custom_display", () -> new IngredientType<>(CustomDisplayIngredient.CODEC)
   );
   private static final DeferredRegister<FluidIngredientType<?>> FLUID_INGREDIENT_TYPES = DeferredRegister.create(
      NeoForgeRegistries.Keys.FLUID_INGREDIENT_TYPES, "neoforge"
   );
   public static final DeferredHolder<FluidIngredientType<?>, FluidIngredientType<SimpleFluidIngredient>> SIMPLE_FLUID_INGREDIENT_TYPE = FLUID_INGREDIENT_TYPES.register(
      "simple", FluidIngredientCodecs::simpleType
   );
   public static final DeferredHolder<FluidIngredientType<?>, FluidIngredientType<CompoundFluidIngredient>> COMPOUND_FLUID_INGREDIENT_TYPE = FLUID_INGREDIENT_TYPES.register(
      "compound", () -> new FluidIngredientType<>(CompoundFluidIngredient.CODEC)
   );
   public static final DeferredHolder<FluidIngredientType<?>, FluidIngredientType<DataComponentFluidIngredient>> DATA_COMPONENT_FLUID_INGREDIENT_TYPE = FLUID_INGREDIENT_TYPES.register(
      "components", () -> new FluidIngredientType<>(DataComponentFluidIngredient.CODEC)
   );
   public static final DeferredHolder<FluidIngredientType<?>, FluidIngredientType<DifferenceFluidIngredient>> DIFFERENCE_FLUID_INGREDIENT_TYPE = FLUID_INGREDIENT_TYPES.register(
      "difference", () -> new FluidIngredientType<>(DifferenceFluidIngredient.CODEC)
   );
   public static final DeferredHolder<FluidIngredientType<?>, FluidIngredientType<IntersectionFluidIngredient>> INTERSECTION_FLUID_INGREDIENT_TYPE = FLUID_INGREDIENT_TYPES.register(
      "intersection", () -> new FluidIngredientType<>(IntersectionFluidIngredient.CODEC)
   );
   public static final DeferredHolder<FluidIngredientType<?>, FluidIngredientType<CustomDisplayFluidIngredient>> CUSTOM_DISPLAY_FLUID_INGREDIENT = FLUID_INGREDIENT_TYPES.register(
      "custom_display", () -> new FluidIngredientType<>(CustomDisplayFluidIngredient.CODEC, CustomDisplayFluidIngredient.STREAM_CODEC)
   );
   private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS = DeferredRegister.create(
      NeoForgeRegistries.Keys.CONDITION_CODECS, "neoforge"
   );
   public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<AndCondition>> AND_CONDITION = CONDITION_CODECS.register(
      "and", () -> AndCondition.CODEC
   );
   public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<NeverCondition>> NEVER_CONDITION = CONDITION_CODECS.register(
      "never", () -> NeverCondition.CODEC
   );
   public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<RegisteredCondition<?>>> REGISTERED_CONDITION = CONDITION_CODECS.register(
      "registered", () -> RegisteredCondition.CODEC
   );
   public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<ModLoadedCondition>> MOD_LOADED_CONDITION = CONDITION_CODECS.register(
      "mod_loaded", () -> ModLoadedCondition.CODEC
   );
   public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<NotCondition>> NOT_CONDITION = CONDITION_CODECS.register(
      "not", () -> NotCondition.CODEC
   );
   public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<OrCondition>> OR_CONDITION = CONDITION_CODECS.register(
      "or", () -> OrCondition.CODEC
   );
   public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<TagEmptyCondition<?>>> TAG_EMPTY_CONDITION = CONDITION_CODECS.register(
      "tag_empty", () -> TagEmptyCondition.CODEC
   );
   public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<AlwaysCondition>> ALWAYS_CONDITION = CONDITION_CODECS.register(
      "always", () -> AlwaysCondition.CODEC
   );
   public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<FeatureFlagsEnabledCondition>> FEATURE_FLAGS_ENABLED_CONDITION = CONDITION_CODECS.register(
      "feature_flags_enabled", () -> FeatureFlagsEnabledCondition.CODEC
   );
   private static final DeferredRegister<MapCodec<? extends EntitySubPredicate>> ENTITY_PREDICATE_CODECS = DeferredRegister.create(
      Registries.ENTITY_SUB_PREDICATE_TYPE, "neoforge"
   );
   public static final DeferredHolder<MapCodec<? extends EntitySubPredicate>, MapCodec<PiglinNeutralArmorEntityPredicate>> PIGLIN_NEUTRAL_ARMOR_PREDICATE = ENTITY_PREDICATE_CODECS.register(
      "piglin_neutral_armor", () -> PiglinNeutralArmorEntityPredicate.CODEC
   );
   public static final DeferredHolder<MapCodec<? extends EntitySubPredicate>, MapCodec<SnowBootsEntityPredicate>> SNOW_BOOTS_PREDICATE = ENTITY_PREDICATE_CODECS.register(
      "snow_boots", () -> SnowBootsEntityPredicate.CODEC
   );
   public static final DeferredHolder<MapCodec<? extends EntitySubPredicate>, MapCodec<TridentEntityPredicate>> IS_TRIDENT_PREDICATE = ENTITY_PREDICATE_CODECS.register(
      "is_trident", () -> TridentEntityPredicate.CODEC
   );
   private static final DeferredRegister<DataComponentPredicate.Type<?>> DATA_COMPONENT_PREDICATE_TYPES = DeferredRegister.create(
      Registries.DATA_COMPONENT_PREDICATE_TYPE, "neoforge"
   );
   public static final DeferredHolder<DataComponentPredicate.Type<?>, DataComponentPredicate.Type<ItemAbilityPredicate>> ITEM_ABILITY_PREDICATE = DATA_COMPONENT_PREDICATE_TYPES.register(
      "item_ability", () -> ItemAbilityPredicate.TYPE
   );
   public static final DeferredHolder<DataComponentPredicate.Type<?>, DataComponentPredicate.Type<PiglinCurrencyItemPredicate>> PIGLIN_CURRENCY_PREDICATE = DATA_COMPONENT_PREDICATE_TYPES.register(
      "piglin_currency", () -> PiglinCurrencyItemPredicate.TYPE
   );
   private static final DeferredRegister<TicketType> TICKET_TYPES = DeferredRegister.create(Registries.TICKET_TYPE, "neoforge");
   public static final Holder<TicketType> GENERATE_FORCED_TICKET = TICKET_TYPES.register("generate_forced", () -> new TicketType(0L, 2));
   public static final Holder<TicketType> BLOCK_TICKET = TICKET_TYPES.register("block", () -> new TicketType(0L, 6));
   public static final Holder<TicketType> BLOCK_WITH_NATURAL_SPAWNING_TICKET = TICKET_TYPES.register(
      "block_with_natural_spawning", () -> new TicketType(0L, 6, true)
   );
   public static final Holder<TicketType> ENTITY_TICKET = TICKET_TYPES.register("entity", () -> new TicketType(0L, 6));
   public static final Holder<TicketType> ENTITY_WITH_NATURAL_SPAWNING_TICKET = TICKET_TYPES.register(
      "entity_with_natural_spawning", () -> new TicketType(0L, 6, true)
   );
   private static final DeferredRegister<FluidType> VANILLA_FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, "minecraft");
   public static final Holder<FluidType> EMPTY_TYPE = VANILLA_FLUID_TYPES.register(
      "empty",
      () -> new FluidType(
         FluidType.Properties.create()
            .descriptionId("block.minecraft.air")
            .motionScale(1.0)
            .canPushEntity(false)
            .canSwim(false)
            .canDrown(false)
            .fallDistanceModifier(1.0F)
            .pathType(null)
            .adjacentPathType(null)
            .density(0)
            .temperature(0)
            .viscosity(0)
      ) {
         @Override
         public void setItemMovement(ItemEntity entity) {
            if (!entity.isNoGravity()) {
               entity.setDeltaMovement(entity.getDeltaMovement().add(0.0, -0.04, 0.0));
            }
         }
      }
   );
   public static final Holder<FluidType> WATER_TYPE = VANILLA_FLUID_TYPES.register(
      "water",
      () -> new FluidType(
         FluidType.Properties.create()
            .descriptionId("block.minecraft.water")
            .fallDistanceModifier(0.0F)
            .canExtinguish(true)
            .canConvertToSource(true)
            .supportsBoating(true)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
            .canHydrate(true)
            .isWaterLike(true)
            .addDripstoneDripping(
               0.17578125F, ParticleTypes.DRIPPING_DRIPSTONE_WATER, Blocks.WATER_CAULDRON, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON
            )
      ) {
         @Override
         public boolean canConvertToSource(FluidState state, LevelReader reader, BlockPos pos) {
            return reader instanceof ServerLevel level
               ? level.getGameRules().get(GameRules.WATER_SOURCE_CONVERSION)
               : super.canConvertToSource(state, reader, pos);
         }

         @Nullable
         @Override
         public PathType getBlockPathType(FluidState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, boolean canFluidLog) {
            return canFluidLog ? super.getBlockPathType(state, level, pos, mob, true) : null;
         }
      }
   );
   public static final Holder<FluidType> LAVA_TYPE = VANILLA_FLUID_TYPES.register(
      "lava",
      () -> new FluidType(
         FluidType.Properties.create()
            .descriptionId("block.minecraft.lava")
            .canSwim(false)
            .canDrown(false)
            .pathType(PathType.LAVA)
            .adjacentPathType(null)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
            .lightLevel(15)
            .density(3000)
            .viscosity(6000)
            .temperature(1300)
            .addDripstoneDripping(
               0.05859375F, ParticleTypes.DRIPPING_DRIPSTONE_LAVA, Blocks.LAVA_CAULDRON, SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON
            )
      ) {
         @Override
         public boolean canConvertToSource(FluidState state, LevelReader reader, BlockPos pos) {
            return reader instanceof ServerLevel level
               ? level.getGameRules().get(GameRules.LAVA_SOURCE_CONVERSION)
               : super.canConvertToSource(state, reader, pos);
         }

         @Override
         public double motionScale(Entity entity) {
            return entity.level().environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, entity.blockPosition())
               ? 0.007
               : 0.0023333333333333335;
         }

         @Override
         public void setItemMovement(ItemEntity entity) {
            Vec3 vec3 = entity.getDeltaMovement();
            entity.setDeltaMovement(vec3.x * 0.95F, vec3.y + (vec3.y < 0.06F ? 5.0E-4F : 0.0F), vec3.z * 0.95F);
         }

         @Override
         public boolean move(FluidState state, LivingEntity entity, Vec3 movementVector, double gravity) {
            return true;
         }
      }
   );
   private static boolean enableProperFilenameValidation = false;
   private static boolean enableMilkFluid = false;
   private static boolean enableMergedAttributeTooltips = false;
   public static final DeferredHolder<SoundEvent, SoundEvent> BUCKET_EMPTY_MILK = DeferredHolder.create(
      Registries.SOUND_EVENT, Identifier.withDefaultNamespace("item.bucket.empty_milk")
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> BUCKET_FILL_MILK = DeferredHolder.create(
      Registries.SOUND_EVENT, Identifier.withDefaultNamespace("item.bucket.fill_milk")
   );
   public static final DeferredHolder<FluidType, FluidType> MILK_TYPE = DeferredHolder.create(
      NeoForgeRegistries.Keys.FLUID_TYPES, Identifier.withDefaultNamespace("milk")
   );
   public static final DeferredHolder<Fluid, Fluid> MILK = DeferredHolder.create(Registries.FLUID, Identifier.withDefaultNamespace("milk"));
   public static final DeferredHolder<Fluid, Fluid> FLOWING_MILK = DeferredHolder.create(Registries.FLUID, Identifier.withDefaultNamespace("flowing_milk"));
   public static final ResourceKey<DamageType> POISON_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("neoforge", "poison"));
   public static final PermissionNode<Boolean> USE_SELECTORS_PERMISSION = new PermissionNode<>(
      "neoforge",
      "use_entity_selectors",
      PermissionTypes.BOOLEAN,
      (player, uuid, contexts) -> player != null && Commands.LEVEL_GAMEMASTERS.check(player.permissions())
   );

   public static void enableMilkFluid() {
      enableMilkFluid = true;
   }

   public static void enableMergedAttributeTooltips() {
      enableMergedAttributeTooltips = true;
   }

   public static void enableProperFilenameValidation() {
      enableProperFilenameValidation = true;
   }

   public static boolean getProperFilenameValidation() {
      return enableProperFilenameValidation;
   }

   public static boolean shouldMergeAttributeTooltips() {
      return enableMergedAttributeTooltips;
   }

   public NeoForgeMod(IEventBus modEventBus, Dist dist, ModContainer container) {
      LOGGER.info(NEOFORGEMOD, "NeoForge mod loading, version {}, for MC {}", NeoForgeVersion.getVersion(), SharedConstants.getCurrentVersion().name());
      SelfTest.initCommon();
      CrashReportCallables.registerCrashCallable("Crash Report UUID", () -> {
         UUID uuid = UUID.randomUUID();
         LOGGER.fatal("Preparing crash report with UUID {}", uuid);
         return uuid.toString();
      });
      CrashReportCallables.registerCrashCallable("FML", FMLVersion::getVersion);
      CrashReportCallables.registerCrashCallable("NeoForge", NeoForgeVersion::getVersion);
      ATTRIBUTE_TYPES.register("identifier", () -> NeoForgeAttributeTypes.IDENTIFIER);
      ENVIRONMENT_ATTRIBUTES.register("custom_weather_effects", () -> NeoForgeEnvironmentAttributes.CUSTOM_WEATHER_EFFECTS);
      ENVIRONMENT_ATTRIBUTES.register("custom_clouds", () -> NeoForgeEnvironmentAttributes.CUSTOM_CLOUDS);
      ENVIRONMENT_ATTRIBUTES.register("custom_skybox", () -> NeoForgeEnvironmentAttributes.CUSTOM_SKYBOX);
      modEventBus.addListener(event -> {
         event.dataPackRegistry(NeoForgeRegistries.Keys.BIOME_MODIFIERS, BiomeModifier.DIRECT_CODEC);
         event.dataPackRegistry(NeoForgeRegistries.Keys.STRUCTURE_MODIFIERS, StructureModifier.DIRECT_CODEC);
      });
      modEventBus.addListener(this::preInit);
      modEventBus.addListener(this::loadComplete);
      modEventBus.addListener(this::registerFluids);
      modEventBus.addListener(this::registerLootData);
      modEventBus.addListener(NeoForgeMod::onConfigLoad);
      modEventBus.addListener(NeoForgeMod::onConfigFileChange);
      ATTRIBUTES.register(modEventBus);
      COMMAND_ARGUMENT_TYPES.register(modEventBus);
      BIOME_MODIFIER_SERIALIZERS.register(modEventBus);
      STRUCTURE_MODIFIER_SERIALIZERS.register(modEventBus);
      HOLDER_SET_TYPES.register(modEventBus);
      VANILLA_FLUID_TYPES.register(modEventBus);
      ENTITY_PREDICATE_CODECS.register(modEventBus);
      DATA_COMPONENT_PREDICATE_TYPES.register(modEventBus);
      TICKET_TYPES.register(modEventBus);
      SLOT_DISPLAY_TYPES.register(modEventBus);
      INGREDIENT_TYPES.register(modEventBus);
      FLUID_INGREDIENT_TYPES.register(modEventBus);
      CONDITION_CODECS.register(modEventBus);
      GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
      ATTRIBUTE_TYPES.register(modEventBus);
      ENVIRONMENT_ATTRIBUTES.register(modEventBus);
      NeoForge.EVENT_BUS.addListener(this::serverStopping);
      ConfigSync.registerEventListeners();
      container.registerConfig(Type.SERVER, NeoForgeServerConfig.SPEC);
      container.registerConfig(Type.COMMON, NeoForgeCommonConfig.SPEC);
      NeoForgeRegistriesSetup.setup(modEventBus);
      StartupNotificationManager.addModMessage("NeoForge version " + NeoForgeVersion.getVersion());
      NeoForge.EVENT_BUS.addListener(VillagerTradingManager::loadTrades);
      NeoForge.EVENT_BUS.register(new NeoForgeEventHandler());
      NeoForge.EVENT_BUS.addListener(this::registerPermissionNodes);
      UsernameCache.load();
      DualStackUtils.initialise();
      TagConventionLogWarning.init();
      modEventBus.addListener(EventPriority.HIGH, CapabilityHooks::markProxyableCapabilities);
      modEventBus.addListener(CapabilityHooks::registerVanillaProviders);
      modEventBus.addListener(EventPriority.LOW, CapabilityHooks::registerFallbackVanillaProviders);
      modEventBus.addListener(CauldronFluidContent::registerCapabilities);
      NeoForge.EVENT_BUS.addListener(CapabilityHooks::invalidateCapsOnChunkLoad);
      NeoForge.EVENT_BUS.addListener(CapabilityHooks::invalidateCapsOnChunkUnload);
      NeoForge.EVENT_BUS.addListener(CapabilityHooks::cleanCapabilityListenerReferencesOnTick);
      NeoForge.EVENT_BUS.addListener(DataMapHooks::onDataMapsUpdated);
      modEventBus.register(NeoForgeDataMaps.class);
      modEventBus.register(SpawnEggItem.class);
      if (NeoForgeVersion.getBuildType() == NeoForgeBuildType.PULL_REQUEST) {
         ModLoader.addLoadingIssue(ModLoadingIssue.warning("loadwarning.neoforge.prbuild", new Object[0]).withAffectedMod(container.getModInfo()));
      }
   }

   public void preInit(FMLCommonSetupEvent evt) {
      VersionChecker.startVersionCheck();
   }

   public void loadComplete(FMLLoadCompleteEvent event) {
   }

   public void serverStopping(ServerStoppingEvent evt) {
      ModConfigs.getFileMap().values().forEach(config -> {
         if (config.getSpec() instanceof ModConfigSpec spec) {
            spec.resetCaches(ModConfigSpec.RestartType.WORLD);
         }
      });
   }

   public void registerFluids(RegisterEvent event) {
      if (enableMilkFluid || DatagenModLoader.isRunningDataGen()) {
         event.register(Registries.SOUND_EVENT, helper -> {
            helper.register(BUCKET_EMPTY_MILK.getId(), SoundEvent.createVariableRangeEvent(BUCKET_EMPTY_MILK.getId()));
            helper.register(BUCKET_FILL_MILK.getId(), SoundEvent.createVariableRangeEvent(BUCKET_FILL_MILK.getId()));
         });
         event.register(
            NeoForgeRegistries.Keys.FLUID_TYPES,
            helper -> helper.register(
               MILK_TYPE.unwrapKey().orElseThrow(),
               new FluidType(
                  FluidType.Properties.create()
                     .density(1024)
                     .viscosity(1024)
                     .sound(SoundActions.BUCKET_FILL, BUCKET_FILL_MILK.value())
                     .sound(SoundActions.BUCKET_EMPTY, BUCKET_EMPTY_MILK.value())
               )
            )
         );
         event.register(
            Registries.FLUID,
            helper -> {
               BaseFlowingFluid.Properties properties = new BaseFlowingFluid.Properties(MILK_TYPE::value, MILK::value, FLOWING_MILK::value)
                  .bucket(() -> Items.MILK_BUCKET);
               helper.register(MILK.getId(), new BaseFlowingFluid.Source(properties));
               helper.register(FLOWING_MILK.getId(), new BaseFlowingFluid.Flowing(properties));
            }
         );
      }
   }

   public void registerLootData(RegisterEvent event) {
      if (event.getRegistryKey().equals(Registries.LOOT_CONDITION_TYPE)) {
         event.register(Registries.LOOT_CONDITION_TYPE, Identifier.fromNamespaceAndPath("neoforge", "loot_table_id"), () -> LootTableIdCondition.LOOT_TABLE_ID);
         event.register(
            Registries.LOOT_CONDITION_TYPE,
            Identifier.fromNamespaceAndPath("neoforge", "can_item_perform_ability"),
            () -> CanItemPerformAbility.LOOT_CONDITION_TYPE
         );
      }
   }

   private static void onConfigLoad(Loading configEvent) {
      LogManager.getLogger().debug(Logging.FORGEMOD, "Loaded NeoForge config file {}", configEvent.getConfig().getFileName());
   }

   private static void onConfigFileChange(Reloading configEvent) {
      LogManager.getLogger().debug(Logging.FORGEMOD, "NeoForge config just got changed on the file system!");
   }

   public void registerPermissionNodes(PermissionGatherEvent.Nodes event) {
      event.addNodes(USE_SELECTORS_PERMISSION);
   }
}
