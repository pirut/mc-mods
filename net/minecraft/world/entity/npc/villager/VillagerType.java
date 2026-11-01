package net.minecraft.world.entity.npc.villager;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.registries.datamaps.builtin.BiomeVillagerType;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

public final class VillagerType {
   public static final ResourceKey<VillagerType> DESERT = createKey("desert");
   public static final ResourceKey<VillagerType> JUNGLE = createKey("jungle");
   public static final ResourceKey<VillagerType> PLAINS = createKey("plains");
   public static final ResourceKey<VillagerType> SAVANNA = createKey("savanna");
   public static final ResourceKey<VillagerType> SNOW = createKey("snow");
   public static final ResourceKey<VillagerType> SWAMP = createKey("swamp");
   public static final ResourceKey<VillagerType> TAIGA = createKey("taiga");
   public static final Codec<Holder<VillagerType>> CODEC = RegistryFixedCodec.create(Registries.VILLAGER_TYPE);
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<VillagerType>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.VILLAGER_TYPE);
   @Deprecated
   private static final Map<ResourceKey<Biome>, ResourceKey<VillagerType>> BY_BIOME = Util.make(Maps.newHashMap(), p_478038_ -> {
      p_478038_.put(Biomes.BADLANDS, DESERT);
      p_478038_.put(Biomes.DESERT, DESERT);
      p_478038_.put(Biomes.ERODED_BADLANDS, DESERT);
      p_478038_.put(Biomes.WOODED_BADLANDS, DESERT);
      p_478038_.put(Biomes.BAMBOO_JUNGLE, JUNGLE);
      p_478038_.put(Biomes.JUNGLE, JUNGLE);
      p_478038_.put(Biomes.SPARSE_JUNGLE, JUNGLE);
      p_478038_.put(Biomes.SAVANNA_PLATEAU, SAVANNA);
      p_478038_.put(Biomes.SAVANNA, SAVANNA);
      p_478038_.put(Biomes.WINDSWEPT_SAVANNA, SAVANNA);
      p_478038_.put(Biomes.DEEP_FROZEN_OCEAN, SNOW);
      p_478038_.put(Biomes.FROZEN_OCEAN, SNOW);
      p_478038_.put(Biomes.FROZEN_RIVER, SNOW);
      p_478038_.put(Biomes.ICE_SPIKES, SNOW);
      p_478038_.put(Biomes.SNOWY_BEACH, SNOW);
      p_478038_.put(Biomes.SNOWY_TAIGA, SNOW);
      p_478038_.put(Biomes.SNOWY_PLAINS, SNOW);
      p_478038_.put(Biomes.GROVE, SNOW);
      p_478038_.put(Biomes.SNOWY_SLOPES, SNOW);
      p_478038_.put(Biomes.FROZEN_PEAKS, SNOW);
      p_478038_.put(Biomes.JAGGED_PEAKS, SNOW);
      p_478038_.put(Biomes.SWAMP, SWAMP);
      p_478038_.put(Biomes.MANGROVE_SWAMP, SWAMP);
      p_478038_.put(Biomes.OLD_GROWTH_SPRUCE_TAIGA, TAIGA);
      p_478038_.put(Biomes.OLD_GROWTH_PINE_TAIGA, TAIGA);
      p_478038_.put(Biomes.WINDSWEPT_GRAVELLY_HILLS, TAIGA);
      p_478038_.put(Biomes.WINDSWEPT_HILLS, TAIGA);
      p_478038_.put(Biomes.TAIGA, TAIGA);
      p_478038_.put(Biomes.WINDSWEPT_FOREST, TAIGA);
   });

   private static ResourceKey<VillagerType> createKey(String p_480589_) {
      return ResourceKey.create(Registries.VILLAGER_TYPE, Identifier.withDefaultNamespace(p_480589_));
   }

   private static VillagerType register(Registry<VillagerType> p_478449_, ResourceKey<VillagerType> p_480836_) {
      return Registry.register(p_478449_, p_480836_, new VillagerType());
   }

   public static VillagerType bootstrap(Registry<VillagerType> p_482077_) {
      register(p_482077_, DESERT);
      register(p_482077_, JUNGLE);
      register(p_482077_, PLAINS);
      register(p_482077_, SAVANNA);
      register(p_482077_, SNOW);
      register(p_482077_, SWAMP);
      return register(p_482077_, TAIGA);
   }

   public static ResourceKey<VillagerType> byBiome(Holder<Biome> p_480603_) {
      BiomeVillagerType fromDataMap = p_480603_.getData(NeoForgeDataMaps.VILLAGER_TYPES);
      return fromDataMap != null ? fromDataMap.type() : p_480603_.unwrapKey().map(BY_BIOME::get).orElse(PLAINS);
   }
}
