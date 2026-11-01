package net.neoforged.neoforge.registries.datamaps.builtin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

public class NeoForgeDataMaps {
   public static final DataMapType<Item, Compostable> COMPOSTABLES = DataMapType.<Compostable, Item>builder(
         id("compostables"), Registries.ITEM, Compostable.CODEC
      )
      .synced(Compostable.CHANCE_CODEC, false)
      .build();
   public static final DataMapType<Item, FurnaceFuel> FURNACE_FUELS = DataMapType.<FurnaceFuel, Item>builder(
         id("furnace_fuels"), Registries.ITEM, FurnaceFuel.CODEC
      )
      .synced(FurnaceFuel.BURN_TIME_CODEC, false)
      .build();
   public static final DataMapType<EntityType<?>, MonsterRoomMob> MONSTER_ROOM_MOBS = DataMapType.<MonsterRoomMob, EntityType<?>>builder(
         id("monster_room_mobs"), Registries.ENTITY_TYPE, MonsterRoomMob.CODEC
      )
      .synced(MonsterRoomMob.WEIGHT_CODEC, false)
      .build();
   public static final DataMapType<Block, Oxidizable> OXIDIZABLES = DataMapType.<Oxidizable, Block>builder(
         id("oxidizables"), Registries.BLOCK, Oxidizable.CODEC
      )
      .synced(Oxidizable.OXIDIZABLE_CODEC, false)
      .build();
   public static final DataMapType<EntityType<?>, ParrotImitation> PARROT_IMITATIONS = DataMapType.<ParrotImitation, EntityType<?>>builder(
         id("parrot_imitations"), Registries.ENTITY_TYPE, ParrotImitation.CODEC
      )
      .synced(ParrotImitation.SOUND_CODEC, false)
      .build();
   public static final DataMapType<VillagerProfession, RaidHeroGift> RAID_HERO_GIFTS = DataMapType.<RaidHeroGift, VillagerProfession>builder(
         id("raid_hero_gifts"), Registries.VILLAGER_PROFESSION, RaidHeroGift.CODEC
      )
      .synced(RaidHeroGift.LOOT_TABLE_CODEC, false)
      .build();
   public static final DataMapType<Block, Strippable> STRIPPABLES = DataMapType.<Strippable, Block>builder(
         id("strippables"), Registries.BLOCK, Strippable.CODEC
      )
      .synced(Strippable.STRIPPED_BLOCK_CODEC, false)
      .build();
   public static final DataMapType<GameEvent, VibrationFrequency> VIBRATION_FREQUENCIES = DataMapType.<VibrationFrequency, GameEvent>builder(
         id("vibration_frequencies"), Registries.GAME_EVENT, VibrationFrequency.CODEC
      )
      .synced(VibrationFrequency.FREQUENCY_CODEC, false)
      .build();
   public static final DataMapType<Biome, BiomeVillagerType> VILLAGER_TYPES = DataMapType.<BiomeVillagerType, Biome>builder(
         id("villager_types"), Registries.BIOME, BiomeVillagerType.CODEC
      )
      .synced(BiomeVillagerType.TYPE_CODEC, false)
      .build();
   public static final DataMapType<Block, Waxable> WAXABLES = DataMapType.<Waxable, Block>builder(id("waxables"), Registries.BLOCK, Waxable.CODEC)
      .synced(Waxable.WAXABLE_CODEC, false)
      .build();

   private static ResourceLocation id(String name) {
      return ResourceLocation.fromNamespaceAndPath("neoforge", name);
   }

   @SubscribeEvent
   private static void register(RegisterDataMapTypesEvent event) {
      event.register(COMPOSTABLES);
      event.register(FURNACE_FUELS);
      event.register(MONSTER_ROOM_MOBS);
      event.register(OXIDIZABLES);
      event.register(PARROT_IMITATIONS);
      event.register(RAID_HERO_GIFTS);
      event.register(STRIPPABLES);
      event.register(VIBRATION_FREQUENCIES);
      event.register(VILLAGER_TYPES);
      event.register(WAXABLES);
   }
}
