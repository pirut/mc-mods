package net.minecraft.client.gui.components;

import java.util.function.Consumer;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractStringWidget extends AbstractWidget {
   @Nullable
   private Consumer<Style> componentClickHandler = null;
   private final Font font;

   public AbstractStringWidget(int p_270910_, int p_270297_, int p_270088_, int p_270842_, Component p_270063_, Font p_270327_) {
      super(p_270910_, p_270297_, p_270088_, p_270842_, p_270063_);
      this.font = p_270327_;
   }

   public abstract void visitLines(ActiveTextCollector var1);

   @Override
   public void renderWidget(GuiGraphics p_458179_, int p_457646_, int p_457574_, float p_458204_) {
      GuiGraphics.HoveredTextEffects guigraphics$hoveredtexteffects;
      if (this.isHovered()) {
         if (this.componentClickHandler != null) {
            guigraphics$hoveredtexteffects = GuiGraphics.HoveredTextEffects.TOOLTIP_AND_CURSOR;
         } else {
            guigraphics$hoveredtexteffects = GuiGraphics.HoveredTextEffects.TOOLTIP_ONLY;
         }
      } else {
         guigraphics$hoveredtexteffects = GuiGraphics.HoveredTextEffects.NONE;
      }

      this.visitLines(p_458179_.textRendererForWidget(this, guigraphics$hoveredtexteffects));
   }

   @Override
   public void onClick(MouseButtonEvent p_457557_, boolean p_457596_) {
      if (this.componentClickHandler != null) {
         ActiveTextCollector.ClickableStyleFinder activetextcollector$clickablestylefinder = new ActiveTextCollector.ClickableStyleFinder(
            this.getFont(), (int)p_457557_.x(), (int)p_457557_.y()
         );
         this.visitLines(activetextcollector$clickablestylefinder);
         Style style = activetextcollector$clickablestylefinder.result();
         if (style != null) {
            this.componentClickHandler.accept(style);
            return;
         }
      }

      super.onClick(p_457557_, p_457596_);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput p_270859_) {
   }

   protected final Font getFont() {
      return this.font;
   }

   @Override
   public void setMessage(Component p_439219_) {
      super.setMessage(p_439219_);
      this.setWidth(this.getFont().width(p_439219_.getVisualOrderText()));
   }

   public AbstractStringWidget setComponentClickHandler(@Nullable Consumer<Style> p_458157_) {
      this.componentClickHandler = p_458157_;
      return this;
   }
}
