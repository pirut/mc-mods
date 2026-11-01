package net.neoforged.neoforge.client.color.item;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jspecify.annotations.Nullable;

public final class FluidContentsTint implements ItemTintSource {
   public static final FluidContentsTint INSTANCE = new FluidContentsTint();
   public static final MapCodec<FluidContentsTint> MAP_CODEC = MapCodec.unit(INSTANCE);

   private FluidContentsTint() {
   }

   @Override
   public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
      FluidStack fluid = FluidUtil.getFirstStackContained(stack);
      return IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid);
   }

   @Override
   public MapCodec<? extends ItemTintSource> type() {
      return MAP_CODEC;
   }
}
