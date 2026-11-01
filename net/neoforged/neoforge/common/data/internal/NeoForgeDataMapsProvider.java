package net.neoforged.neoforge.common.data.internal;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.behavior.GiveGiftToHero;
import net.minecraft.world.entity.ai.behavior.WorkAtComposter;
import net.minecraft.world.entity.ai.sensing.VillagerHostilesSensor;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.levelgen.feature.MonsterRoomFeature;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.AcceptableVillagerDistance;
import net.neoforged.neoforge.registries.datamaps.builtin.BiomeVillagerType;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.MonsterRoomMob;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.registries.datamaps.builtin.Oxidizable;
import net.neoforged.neoforge.registries.datamaps.builtin.ParrotImitation;
import net.neoforged.neoforge.registries.datamaps.builtin.RaidHeroGift;
import net.neoforged.neoforge.registries.datamaps.builtin.Strippable;
import net.neoforged.neoforge.registries.datamaps.builtin.VibrationFrequency;
import net.neoforged.neoforge.registries.datamaps.builtin.Waxable;

public class NeoForgeDataMapsProvider extends DataMapProvider {
   public NeoForgeDataMapsProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
      super(packOutput, lookupProvider);
   }

   @Override
   protected void gather(HolderLookup.Provider provider) {
      DataMapProvider.Builder<BiomeVillagerType, Biome> biomeVillagers = this.builder(NeoForgeDataMaps.VILLAGER_TYPES);
      ((Map)ObfuscationReflectionHelper.getPrivateValue(VillagerType.class, null, "BY_BIOME"))
         .forEach((biome, type) -> biomeVillagers.add((ResourceKey<Biome>)biome, new BiomeVillagerType((ResourceKey<VillagerType>)type), false));
      DataMapProvider.Builder<Compostable, Item> compostables = this.builder(NeoForgeDataMaps.COMPOSTABLES);
      List<Item> villagerCompostables = (List<Item>)ObfuscationReflectionHelper.getPrivateValue(WorkAtComposter.class, null, "COMPOSTABLE_ITEMS");
      ComposterBlock.COMPOSTABLES
         .forEach(
            (item, chance) -> compostables.add(
               item.asItem().builtInRegistryHolder(), new Compostable(chance, villagerCompostables.contains(item.asItem())), false
            )
         );
      DataMapProvider.Builder<AcceptableVillagerDistance, EntityType<?>> acceptableVillagerDistances = this.builder(
         NeoForgeDataMaps.ACCEPTABLE_VILLAGER_DISTANCES
      );
      ((ImmutableMap)ObfuscationReflectionHelper.getPrivateValue(VillagerHostilesSensor.class, null, "ACCEPTABLE_DISTANCE_FROM_HOSTILES"))
         .forEach(
            (entityType, distance) -> acceptableVillagerDistances.add(
               BuiltInRegistries.ENTITY_TYPE.getKey(entityType), new AcceptableVillagerDistance(distance), false
            )
         );
      DataMapProvider.Builder<FurnaceFuel, Item> fuels = this.builder(NeoForgeDataMaps.FURNACE_FUELS);
      FuelValues.vanillaBurnTimes(new NeoForgeDataMapsProvider.FuelValuesDataMapBuilder(provider, fuels), 200);
      DataMapProvider.Builder<VibrationFrequency, GameEvent> vibrationFrequencies = this.builder(NeoForgeDataMaps.VIBRATION_FREQUENCIES);
      ((Reference2IntMap)VibrationSystem.VIBRATION_FREQUENCY_FOR_EVENT)
         .forEach((event, frequency) -> vibrationFrequencies.add(event, new VibrationFrequency(frequency), false));
      DataMapProvider.Builder<ParrotImitation, EntityType<?>> imitations = this.builder(NeoForgeDataMaps.PARROT_IMITATIONS);
      ((Map)ObfuscationReflectionHelper.getPrivateValue(Parrot.class, null, "MOB_SOUND_MAP"))
         .forEach((type, sound) -> imitations.add(type.builtInRegistryHolder(), new ParrotImitation(sound), false));
      DataMapProvider.Builder<RaidHeroGift, VillagerProfession> raidHeroGifts = this.builder(NeoForgeDataMaps.RAID_HERO_GIFTS);
      ((Map)ObfuscationReflectionHelper.getPrivateValue(GiveGiftToHero.class, null, "GIFTS"))
         .forEach(
            (type, lootTable) -> raidHeroGifts.add(
               BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow((ResourceKey<VillagerProfession>)type),
               new RaidHeroGift((ResourceKey<LootTable>)lootTable),
               false
            )
         );
      DataMapProvider.Builder<Strippable, Block> strippables = this.builder(NeoForgeDataMaps.STRIPPABLES);
      NeoForgeDataMapsProvider.StrippablesAccess.getStrippables()
         .forEach((block, stripped) -> strippables.add(block.builtInRegistryHolder(), new Strippable(stripped), false));
      DataMapProvider.Builder<MonsterRoomMob, EntityType<?>> monsterRoomMobs = this.builder(NeoForgeDataMaps.MONSTER_ROOM_MOBS);
      Arrays.stream((EntityType[])ObfuscationReflectionHelper.getPrivateValue(MonsterRoomFeature.class, null, "MOBS"))
         .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
         .forEach(
            (type, weight) -> monsterRoomMobs.add(
               BuiltInRegistries.ENTITY_TYPE.wrapAsHolder((EntityType<?>)type), new MonsterRoomMob((int)(weight * 100L)), false
            )
         );
      DataMapProvider.Builder<Oxidizable, Block> oxidizables = this.builder(NeoForgeDataMaps.OXIDIZABLES);
      WeatheringCopper.NEXT_BY_BLOCK.get().forEach((now, after) -> oxidizables.add(now.builtInRegistryHolder(), new Oxidizable(after), false));
      DataMapProvider.Builder<Waxable, Block> waxables = this.builder(NeoForgeDataMaps.WAXABLES);
      HoneycombItem.WAXABLES.get().forEach((now, after) -> waxables.add(now.builtInRegistryHolder(), new Waxable(after), false));
   }

   private static class FuelValuesDataMapBuilder extends FuelValues.Builder {
      private final DataMapProvider.Builder<FurnaceFuel, Item> builder;

      public FuelValuesDataMapBuilder(HolderLookup.Provider lookupProvider, DataMapProvider.Builder<FurnaceFuel, Item> builder) {
         super(lookupProvider, FeatureFlags.DEFAULT_FLAGS);
         this.builder = builder;
      }

      public NeoForgeDataMapsProvider.FuelValuesDataMapBuilder add(TagKey<Item> tagKey, int burnTime) {
         this.builder.add(tagKey, new FurnaceFuel(burnTime), false);
         return this;
      }

      public NeoForgeDataMapsProvider.FuelValuesDataMapBuilder add(ItemLike item, int burnTime) {
         this.builder.add(item.asItem().builtInRegistryHolder(), new FurnaceFuel(burnTime), false);
         return this;
      }

      public NeoForgeDataMapsProvider.FuelValuesDataMapBuilder remove(TagKey<Item> tagKey) {
         this.builder.remove(tagKey);
         return this;
      }
   }

   private static class StrippablesAccess extends AxeItem {
      private StrippablesAccess(ToolMaterial material, float attackDamage, float attackSpeed, Item.Properties properties) {
         super(material, attackDamage, attackSpeed, properties);
      }

      public static Map<Block, Block> getStrippables() {
         return STRIPPABLES;
      }
   }
}
