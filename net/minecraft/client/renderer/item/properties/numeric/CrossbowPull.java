package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class CrossbowPull implements RangeSelectItemModelProperty {
   public static final MapCodec<CrossbowPull> MAP_CODEC = MapCodec.unit(new CrossbowPull());

   @Override
   public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
      LivingEntity livingEntity = owner == null ? null : owner.asLivingEntity();
      if (livingEntity == null) {
         return 0.0F;
      } else if (CrossbowItem.isCharged(stack)) {
         return 0.0F;
      } else {
         int i = CrossbowItem.getChargeDuration(stack, livingEntity);
         return (float)UseDuration.useDuration(stack, livingEntity) / i;
      }
   }

   @Override
   public MapCodec<CrossbowPull> type() {
      return MAP_CODEC;
   }
}
