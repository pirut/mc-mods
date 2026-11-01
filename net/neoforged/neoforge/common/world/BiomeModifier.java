package net.neoforged.neoforge.common.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public interface BiomeModifier {
   Codec<BiomeModifier> DIRECT_CODEC = NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS.byNameCodec().dispatch(BiomeModifier::codec, Function.identity());
   Codec<Holder<BiomeModifier>> REFERENCE_CODEC = RegistryFileCodec.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, DIRECT_CODEC);
   Codec<HolderSet<BiomeModifier>> LIST_CODEC = RegistryCodecs.homogeneousList(NeoForgeRegistries.Keys.BIOME_MODIFIERS, DIRECT_CODEC);

   void modify(Holder<Biome> var1, BiomeModifier.Phase var2, ModifiableBiomeInfo.BiomeInfo.Builder var3);

   MapCodec<? extends BiomeModifier> codec();

   public static enum Phase {
      BEFORE_EVERYTHING,
      ADD,
      REMOVE,
      MODIFY,
      AFTER_EVERYTHING;
   }
}
