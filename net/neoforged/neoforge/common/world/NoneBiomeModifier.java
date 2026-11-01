package net.neoforged.neoforge.common.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.NeoForgeMod;

public class NoneBiomeModifier implements BiomeModifier {
   public static final NoneBiomeModifier INSTANCE = new NoneBiomeModifier();

   @Override
   public void modify(Holder<Biome> biome, BiomeModifier.Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
   }

   @Override
   public MapCodec<? extends BiomeModifier> codec() {
      return NeoForgeMod.NONE_BIOME_MODIFIER_TYPE.get();
   }
}
