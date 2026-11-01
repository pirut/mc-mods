package net.neoforged.neoforge.event.entity.player;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

public class FluidTooltipEvent extends PlayerEvent {
   private final TooltipFlag flags;
   private final FluidStack fluidStack;
   private final List<Component> toolTip;
   private final Item.TooltipContext context;

   public FluidTooltipEvent(FluidStack fluidStack, @Nullable Player player, List<Component> list, TooltipFlag flags, Item.TooltipContext context) {
      super(player);
      this.fluidStack = fluidStack;
      this.toolTip = list;
      this.flags = flags;
      this.context = context;
   }

   public TooltipFlag getFlags() {
      return this.flags;
   }

   public FluidStack getFluidStack() {
      return this.fluidStack;
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
