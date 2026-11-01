package net.neoforged.neoforge.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.ApiStatus.Internal;

public record ClientTooltipFlag(boolean advanced, boolean creative, boolean shiftDown, boolean controlDown, boolean altDown) implements TooltipFlag {
   @Internal
   public ClientTooltipFlag(boolean advanced, boolean creative, boolean shiftDown, boolean controlDown, boolean altDown) {
      this.advanced = advanced;
      this.creative = creative;
      this.shiftDown = shiftDown;
      this.controlDown = controlDown;
      this.altDown = altDown;
   }

   @Override
   public boolean isAdvanced() {
      return this.advanced;
   }

   @Override
   public boolean isCreative() {
      return this.creative;
   }

   @Override
   public boolean hasControlDown() {
      return this.controlDown;
   }

   @Override
   public boolean hasShiftDown() {
      return this.shiftDown;
   }

   @Override
   public boolean hasAltDown() {
      return this.altDown;
   }

   public static TooltipFlag of(TooltipFlag other) {
      return new ClientTooltipFlag(other.isAdvanced(), other.isCreative(), Screen.hasShiftDown(), Screen.hasControlDown(), Screen.hasAltDown());
   }
}
