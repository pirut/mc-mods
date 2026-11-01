package net.minecraft.client.gui.components;

import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class AbstractStringWidget extends AbstractWidget {
   @Nullable
   private Consumer<Style> componentClickHandler = null;
   private final Font font;

   public AbstractStringWidget(int x, int y, int width, int height, Component message, Font font) {
      super(x, y, width, height, message);
      this.font = font;
   }

   public abstract void visitLines(ActiveTextCollector activeTextCollector);

   @Override
   public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      GuiGraphics.HoveredTextEffects hoveredTextEffects;
      if (this.isHovered()) {
         if (this.componentClickHandler != null) {
            hoveredTextEffects = GuiGraphics.HoveredTextEffects.TOOLTIP_AND_CURSOR;
         } else {
            hoveredTextEffects = GuiGraphics.HoveredTextEffects.TOOLTIP_ONLY;
         }
      } else {
         hoveredTextEffects = GuiGraphics.HoveredTextEffects.NONE;
      }

      this.visitLines(guiGraphics.textRendererForWidget(this, hoveredTextEffects));
   }

   @Override
   public void onClick(MouseButtonEvent event, boolean isDoubleClick) {
      if (this.componentClickHandler != null) {
         ActiveTextCollector.ClickableStyleFinder clickableStyleFinder = new ActiveTextCollector.ClickableStyleFinder(
            this.getFont(), (int)event.x(), (int)event.y()
         );
         this.visitLines(clickableStyleFinder);
         Style style = clickableStyleFinder.result();
         if (style != null) {
            this.componentClickHandler.accept(style);
            return;
         }
      }

      super.onClick(event, isDoubleClick);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
   }

   protected final Font getFont() {
      return this.font;
   }

   @Override
   public void setMessage(Component message) {
      super.setMessage(message);
      this.setWidth(this.getFont().width(message.getVisualOrderText()));
   }

   public AbstractStringWidget setComponentClickHandler(@Nullable Consumer<Style> componentClickHandler) {
      this.componentClickHandler = componentClickHandler;
      return this;
   }
}
