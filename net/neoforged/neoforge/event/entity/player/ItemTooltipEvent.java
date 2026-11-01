package net.neoforged.neoforge.event.entity.player;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;

public class ItemTooltipEvent extends PlayerEvent {
   private final TooltipFlag flags;
   private final ItemStack itemStack;
   private final List<Component> toolTip;
   private final Item.TooltipContext context;

   public ItemTooltipEvent(ItemStack itemStack, @Nullable Player player, List<Component> list, TooltipFlag flags, Item.TooltipContext context) {
      super(player);
      this.itemStack = itemStack;
      this.toolTip = list;
      this.flags = flags;
      this.context = context;
   }

   public TooltipFlag getFlags() {
      return this.flags;
   }

   public ItemStack getItemStack() {
      return this.itemStack;
   }

   public List<Component> getToolTip() {
      return this.toolTip;
   }

   @Nullable
   @Override
   public Player getEntity() {
      return super.getEntity();
   }

   public Item.TooltipContext getContext() {
      return this.context;
   }
}
