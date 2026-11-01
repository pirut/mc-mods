package net.minecraft.client.gui.screens.inventory.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

@Environment(EnvType.CLIENT)
public interface ClientTooltipComponent {
   static ClientTooltipComponent create(FormattedCharSequence text) {
      return new ClientTextTooltip(text);
   }

   static ClientTooltipComponent create(TooltipComponent visualTooltipComponent) {
      return (ClientTooltipComponent)(switch (visualTooltipComponent) {
         case BundleTooltip bundleTooltip -> new ClientBundleTooltip(bundleTooltip.contents());
         case ClientActivePlayersTooltip.ActivePlayersTooltip activePlayersTooltip -> new ClientActivePlayersTooltip(activePlayersTooltip);
         default -> throw new IllegalArgumentException("Unknown TooltipComponent");
      });
   }

   int getHeight(Font font);

   int getWidth(Font font);

   default boolean showTooltipWithItemInHand() {
      return false;
   }

   default void renderText(GuiGraphics guiGraphics, Font font, int x, int y) {
   }

   default void renderImage(Font font, int x, int y, int width, int height, GuiGraphics guiGraphics) {
   }
}
