package net.neoforged.neoforge.event.enchanting;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.Event;
import org.jspecify.annotations.Nullable;

public class GetEnchantmentLevelEvent extends Event {
   protected final ItemStack stack;
   protected final ItemEnchantments.Mutable enchantments;
   @Nullable
   protected final Holder<Enchantment> targetEnchant;
   protected final HolderLookup.RegistryLookup<Enchantment> lookup;

   public GetEnchantmentLevelEvent(
      ItemStack stack, ItemEnchantments.Mutable enchantments, @Nullable Holder<Enchantment> targetEnchant, HolderLookup.RegistryLookup<Enchantment> lookup
   ) {
      this.stack = stack;
      this.enchantments = enchantments;
      this.targetEnchant = targetEnchant;
      this.lookup = lookup;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public ItemEnchantments.Mutable getEnchantments() {
      return this.enchantments;
   }

   @Nullable
   public Holder<Enchantment> getTargetEnchant() {
      return this.targetEnchant;
   }

   public boolean isTargetting(Holder<Enchantment> ench) {
      return this.targetEnchant == null || this.targetEnchant.is(ench);
   }

   public boolean isTargetting(ResourceKey<Enchantment> ench) {
      return this.targetEnchant == null || this.targetEnchant.is(ench);
   }

   public Optional<Holder.Reference<Enchantment>> getHolder(ResourceKey<Enchantment> key) {
      return this.lookup.get(key);
   }

   public HolderLookup.RegistryLookup<Enchantment> getLookup() {
      return this.lookup;
   }
}
