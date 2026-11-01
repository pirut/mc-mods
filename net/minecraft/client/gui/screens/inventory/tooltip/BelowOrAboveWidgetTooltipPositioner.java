package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2i;
import org.joml.Vector2ic;

@OnlyIn(Dist.CLIENT)
public class BelowOrAboveWidgetTooltipPositioner implements ClientTooltipPositioner {
   private final ScreenRectangle screenRectangle;

   public BelowOrAboveWidgetTooltipPositioner(ScreenRectangle p_305899_) {
      this.screenRectangle = p_305899_;
   }

   @Override
   public Vector2ic positionTooltip(int p_282513_, int p_281649_, int p_283308_, int p_282740_, int p_281398_, int p_283404_) {
      Vector2i $$6 = new Vector2i();
      $$6.x = this.screenRectangle.left() + 3;
      $$6.y = this.screenRectangle.bottom() + 3 + 1;
      if ($$6.y + p_283404_ + 3 > p_281649_) {
         $$6.y = this.screenRectangle.top() - p_283404_ - 3 - 1;
      }

      if ($$6.x + p_281398_ > p_282513_) {
         $$6.x = Math.max(this.screenRectangle.right() - p_281398_ - 3, 4);
      }

      return $$6;
   }
}
