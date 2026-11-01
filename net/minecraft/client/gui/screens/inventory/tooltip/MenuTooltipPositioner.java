package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2i;
import org.joml.Vector2ic;

@OnlyIn(Dist.CLIENT)
public class MenuTooltipPositioner implements ClientTooltipPositioner {
   private static final int MARGIN = 5;
   private static final int MOUSE_OFFSET_X = 12;
   public static final int MAX_OVERLAP_WITH_WIDGET = 3;
   public static final int MAX_DISTANCE_TO_WIDGET = 5;
   private final ScreenRectangle screenRectangle;

   public MenuTooltipPositioner(ScreenRectangle p_305825_) {
      this.screenRectangle = p_305825_;
   }

   @Override
   public Vector2ic positionTooltip(int p_283490_, int p_282509_, int p_282684_, int p_281703_, int p_281348_, int p_283657_) {
      Vector2i $$6 = new Vector2i(p_282684_ + 12, p_281703_);
      if ($$6.x + p_281348_ > p_283490_ - 5) {
         $$6.x = Math.max(p_282684_ - 12 - p_281348_, 9);
      }

      $$6.y += 3;
      int $$7 = p_283657_ + 3 + 3;
      int $$8 = this.screenRectangle.bottom() + 3 + getOffset(0, 0, this.screenRectangle.height());
      int $$9 = p_282509_ - 5;
      if ($$8 + $$7 <= $$9) {
         $$6.y = $$6.y + getOffset($$6.y, this.screenRectangle.top(), this.screenRectangle.height());
      } else {
         $$6.y = $$6.y - ($$7 + getOffset($$6.y, this.screenRectangle.bottom(), this.screenRectangle.height()));
      }

      return $$6;
   }

   private static int getOffset(int p_268188_, int p_268026_, int p_268015_) {
      int $$3 = Math.min(Math.abs(p_268188_ - p_268026_), p_268015_);
      return Math.round(Mth.lerp((float)$$3 / p_268015_, (float)(p_268015_ - 3), 5.0F));
   }
}
