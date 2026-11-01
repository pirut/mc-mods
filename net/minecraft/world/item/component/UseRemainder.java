package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record UseRemainder(ItemStack convertInto) {
   public static final Codec<UseRemainder> CODEC = ItemStack.CODEC.xmap(UseRemainder::new, UseRemainder::convertInto);
   public static final StreamCodec<RegistryFriendlyByteBuf, UseRemainder> STREAM_CODEC = StreamCodec.composite(
      ItemStack.STREAM_CODEC, UseRemainder::convertInto, UseRemainder::new
   );

   public ItemStack convertIntoRemainder(ItemStack p_366873_, int p_366665_, boolean p_376140_, UseRemainder.OnExtraCreatedRemainder p_376106_) {
      if (p_376140_) {
         return p_366873_;
      } else if (p_366873_.getCount() >= p_366665_) {
         return p_366873_;
      } else {
         ItemStack itemstack = this.convertInto.copy();
         if (p_366873_.isEmpty()) {
            return itemstack;
         } else {
            p_376106_.apply(itemstack);
            return p_366873_;
         }
      }
   }

   @Override
   public boolean equals(Object p_366515_) {
      if (this == p_366515_) {
         return true;
      } else if (p_366515_ != null && this.getClass() == p_366515_.getClass()) {
         UseRemainder useremainder = (UseRemainder)p_366515_;
         return ItemStack.matches(this.convertInto, useremainder.convertInto);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return ItemStack.hashItemAndComponents(this.convertInto);
   }

   @FunctionalInterface
   public interface OnExtraCreatedRemainder {
      void apply(ItemStack var1);
   }
}
