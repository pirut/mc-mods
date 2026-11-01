package net.minecraft.world.item.component;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class ChargedProjectiles implements TooltipProvider {
   public static final ChargedProjectiles EMPTY = new ChargedProjectiles(List.of());
   public static final Codec<ChargedProjectiles> CODEC = ItemStack.CODEC.listOf().xmap(ChargedProjectiles::new, p_331186_ -> p_331186_.items);
   public static final StreamCodec<RegistryFriendlyByteBuf, ChargedProjectiles> STREAM_CODEC = ItemStack.STREAM_CODEC
      .apply(ByteBufCodecs.list())
      .map(ChargedProjectiles::new, p_330917_ -> p_330917_.items);
   private final List<ItemStack> items;

   private ChargedProjectiles(List<ItemStack> p_331523_) {
      this.items = p_331523_;
   }

   public static ChargedProjectiles of(ItemStack p_331405_) {
      return new ChargedProjectiles(List.of(p_331405_.copy()));
   }

   public static ChargedProjectiles of(List<ItemStack> p_331284_) {
      return new ChargedProjectiles(List.copyOf(Lists.transform(p_331284_, ItemStack::copy)));
   }

   public boolean contains(Item p_331165_) {
      for (ItemStack itemstack : this.items) {
         if (itemstack.is(p_331165_)) {
            return true;
         }
      }

      return false;
   }

   public List<ItemStack> getItems() {
      return Lists.transform(this.items, ItemStack::copy);
   }

   public boolean isEmpty() {
      return this.items.isEmpty();
   }

   @Override
   public boolean equals(Object p_331828_) {
      return this == p_331828_
         ? true
         : p_331828_ instanceof ChargedProjectiles chargedprojectiles && ItemStack.listMatches(this.items, chargedprojectiles.items);
   }

   @Override
   public int hashCode() {
      return ItemStack.hashStackList(this.items);
   }

   @Override
   public String toString() {
      return "ChargedProjectiles[items=" + this.items + "]";
   }

   @Override
   public void addToTooltip(Item.TooltipContext p_399623_, Consumer<Component> p_400053_, TooltipFlag p_399978_, DataComponentGetter p_399993_) {
      ItemStack itemstack = null;
      int i = 0;

      for (ItemStack itemstack1 : this.items) {
         if (itemstack == null) {
            itemstack = itemstack1;
            i = 1;
         } else if (ItemStack.matches(itemstack, itemstack1)) {
            i++;
         } else {
            addProjectileTooltip(p_399623_, p_400053_, itemstack, i);
            itemstack = itemstack1;
            i = 1;
         }
      }

      if (itemstack != null) {
         addProjectileTooltip(p_399623_, p_400053_, itemstack, i);
      }
   }

   private static void addProjectileTooltip(Item.TooltipContext p_401796_, Consumer<Component> p_401811_, ItemStack p_401841_, int p_401911_) {
      if (p_401911_ == 1) {
         p_401811_.accept(Component.translatable("item.minecraft.crossbow.projectile.single", p_401841_.getDisplayName()));
      } else {
         p_401811_.accept(Component.translatable("item.minecraft.crossbow.projectile.multiple", p_401911_, p_401841_.getDisplayName()));
      }

      TooltipDisplay tooltipdisplay = p_401841_.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
      p_401841_.addDetailsToTooltip(
         p_401796_,
         tooltipdisplay,
         null,
         TooltipFlag.NORMAL,
         p_399415_ -> p_401811_.accept(Component.literal("  ").append(p_399415_).withStyle(ChatFormatting.GRAY))
      );
   }
}
