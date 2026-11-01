package net.neoforged.neoforge.event.entity.player;

import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

public class PlayerEnchantItemEvent extends PlayerEvent {
   private final ItemStack enchantedItem;
   private final List<EnchantmentInstance> enchantments;

   public PlayerEnchantItemEvent(Player player, ItemStack enchantedItem, List<EnchantmentInstance> enchantments) {
      super(player);
      this.enchantedItem = enchantedItem;
      this.enchantments = enchantments;
   }

   public ItemStack getEnchantedItem() {
      return this.enchantedItem;
   }

   public List<EnchantmentInstance> getEnchantments() {
      return this.enchantments;
   }
}
