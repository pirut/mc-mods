package net.neoforged.neoforge.registries.datamaps.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.npc.VillagerType;

public record BiomeVillagerType(VillagerType type) {
   public static final Codec<BiomeVillagerType> TYPE_CODEC = BuiltInRegistries.VILLAGER_TYPE
      .byNameCodec()
      .xmap(BiomeVillagerType::new, BiomeVillagerType::type);
   public static final Codec<BiomeVillagerType> CODEC = Codec.withAlternative(
      RecordCodecBuilder.create(
         in -> in.group(BuiltInRegistries.VILLAGER_TYPE.byNameCodec().fieldOf("villager_type").forGetter(BiomeVillagerType::type))
            .apply(in, BiomeVillagerType::new)
      ),
      TYPE_CODEC
   );
}
