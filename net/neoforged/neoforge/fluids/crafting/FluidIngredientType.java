package net.neoforged.neoforge.fluids.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record FluidIngredientType<T extends FluidIngredient>(MapCodec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
   public FluidIngredientType(MapCodec<T> mapCodec) {
      this(mapCodec, ByteBufCodecs.fromCodecWithRegistries(mapCodec.codec()));
   }
}
