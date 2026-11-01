package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractSelectionList<E extends AbstractSelectionList.Entry<E>> extends AbstractContainerWidget {
   protected static final int SCROLLBAR_WIDTH = 6;
   private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("widget/scroller");
   private static final ResourceLocation SCROLLER_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("widget/scroller_background");
   private static final ResourceLocation MENU_LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/menu_list_background.png");
   private static final ResourceLocation INWORLD_MENU_LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");
   protected final Minecraft minecraft;
   protected final int itemHeight;
   private final List<E> children = new AbstractSelectionList.TrackedList();
   protected boolean centerListVertically = true;
   private double scrollAmount;
   private boolean renderHeader;
   protected int headerHeight;
   private boolean scrolling;
   @Nullable
   private E selected;
   @Nullable
   private E hovered;

   public AbstractSelectionList(Minecraft p_93404_, int p_93405_, int p_93406_, int p_93407_, int p_93408_) {
      super(0, p_93407_, p_93405_, p_93406_, CommonComponents.EMPTY);
      this.minecraft = p_93404_;
      this.itemHeight = p_93408_;
   }

   protected void setRenderHeader(boolean p_93474_, int p_93475_) {
      this.renderHeader = p_93474_;
      this.headerHeight = p_93475_;
      if (!p_93474_) {
         this.headerHeight = 0;
      }
   }

   public int getRowWidth() {
      return 220;
   }

   @Nullable
   public E getSelected() {
      return this.selected;
   }

   public void setSelected(@Nullable E p_93462_) {
      this.selected = p_93462_;
   }

   public E getFirstElement() {
      return this.children.get(0);
   }

   @Nullable
   public E getFocused() {
      return (E)super.getFocused();
   }

   @Override
   public final List<E> children() {
      return this.children;
   }

   protected void clearEntries() {
      this.children.clear();
      this.selected = null;
   }

   protected void replaceEntries(Collection<E> p_93470_) {
      this.clearEntries();
      this.children.addAll(p_93470_);
   }

   protected E getEntry(int p_93501_) {
      return this.children().get(p_93501_);
   }

   protected int addEntry(E p_93487_) {
      this.children.add(p_93487_);
      return this.children.size() - 1;
   }

   protected void addEntryToTop(E p_239858_) {
      double $$1 = this.getMaxScroll() - this.getScrollAmount();
      this.children.add(0, p_239858_);
      this.setScrollAmount(this.getMaxScroll() - $$1);
   }

   protected boolean removeEntryFromTop(E p_239046_) {
      double $$1 = this.getMaxScroll() - this.getScrollAmount();
      boolean $$2 = this.removeEntry(p_239046_);
      this.setScrollAmount(this.getMaxScroll() - $$1);
      return $$2;
   }

   protected int getItemCount() {
      return this.children().size();
   }

   protected boolean isSelectedItem(int p_93504_) {
      return Objects.equals(this.getSelected(), this.children().get(p_93504_));
   }

   @Nullable
   protected final E getEntryAtPosition(double p_93413_, double p_93414_) {
      int $$2 = this.getRowWidth() / 2;
      int $$3 = this.getX() + this.width / 2;
      int $$4 = $$3 - $$2;
      int $$5 = $$3 + $$2;
      int $$6 = Mth.floor(p_93414_ - this.getY()) - this.headerHeight + (int)this.getScrollAmount() - 4;
      int $$7 = $$6 / this.itemHeight;
      return p_93413_ >= $$4 && p_93413_ <= $$5 && $$7 >= 0 && $$6 >= 0 && $$7 < this.getItemCount() ? this.children().get($$7) : null;
   }

   public void updateSize(int p_331980_, HeaderAndFooterLayout p_331821_) {
      this.updateSizeAndPosition(p_331980_, p_331821_.getContentHeight(), p_331821_.getHeaderHeight());
   }

   public void updateSizeAndPosition(int p_332028_, int p_330750_, int p_331681_) {
      this.setSize(p_332028_, p_330750_);
      this.setPosition(0, p_331681_);
      this.clampScrollAmount();
   }

   protected int getMaxPosition() {
      return this.getItemCount() * this.itemHeight + this.headerHeight;
   }

   protected boolean clickedHeader(int p_93431_, int p_93432_) {
      return false;
   }

   protected void renderHeader(GuiGraphics p_282337_, int p_93444_, int p_93445_) {
   }

   protected void renderDecorations(GuiGraphics p_281477_, int p_93459_, int p_93460_) {
   }

   @Override
   public void renderWidget(GuiGraphics p_282708_, int p_283242_, int p_282891_, float p_283683_) {
      this.hovered = this.isMouseOver(p_283242_, p_282891_) ? this.getEntryAtPosition(p_283242_, p_282891_) : null;
      this.renderListBackground(p_282708_);
      this.enableScissor(p_282708_);
      if (this.renderHeader) {
         int $$4 = this.getRowLeft();
         int $$5 = this.getY() + 4 - (int)this.getScrollAmount();
         this.renderHeader(p_282708_, $$4, $$5);
      }

      this.renderListItems(p_282708_, p_283242_, p_282891_, p_283683_);
      p_282708_.disableScissor();
      this.renderListSeparators(p_282708_);
      if (this.scrollbarVisible()) {
         int $$6 = this.getScrollbarPosition();
         int $$7 = (int)((float)(this.height * this.height) / this.getMaxPosition());
         $$7 = Mth.clamp($$7, 32, this.height - 8);
         int $$8 = (int)this.getScrollAmount() * (this.height - $$7) / this.getMaxScroll() + this.getY();
         if ($$8 < this.getY()) {
            $$8 = this.getY();
         }

         RenderSystem.enableBlend();
         p_282708_.blitSprite(SCROLLER_BACKGROUND_SPRITE, $$6, this.getY(), 6, this.getHeight());
         p_282708_.blitSprite(SCROLLER_SPRITE, $$6, $$8, 6, $$7);
         RenderSystem.disableBlend();
      }

      this.renderDecorations(p_282708_, p_283242_, p_282891_);
      RenderSystem.disableBlend();
   }

   protected boolean scrollbarVisible() {
      return this.getMaxScroll() > 0;
   }

   protected void renderListSeparators(GuiGraphics p_331248_) {
      RenderSystem.enableBlend();
      ResourceLocation $$1 = this.minecraft.level == null ? Screen.HEADER_SEPARATOR : Screen.INWORLD_HEADER_SEPARATOR;
      ResourceLocation $$2 = this.minecraft.level == null ? Screen.FOOTER_SEPARATOR : Screen.INWORLD_FOOTER_SEPARATOR;
      p_331248_.blit($$1, this.getX(), this.getY() - 2, 0.0F, 0.0F, this.getWidth(), 2, 32, 2);
      p_331248_.blit($$2, this.getX(), this.getBottom(), 0.0F, 0.0F, this.getWidth(), 2, 32, 2);
      RenderSystem.disableBlend();
   }

   protected void renderListBackground(GuiGraphics p_331297_) {
      RenderSystem.enableBlend();
      ResourceLocation $$1 = this.minecraft.level == null ? MENU_LIST_BACKGROUND : INWORLD_MENU_LIST_BACKGROUND;
      p_331297_.blit($$1, this.getX(), this.getY(), this.getRight(), this.getBottom() + (int)this.getScrollAmount(), this.getWidth(), this.getHeight(), 32, 32);
      RenderSystem.disableBlend();
   }

   protected void enableScissor(GuiGraphics p_282811_) {
      p_282811_.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
   }

   protected void centerScrollOn(E p_93495_) {
      this.setScrollAmount(this.children().indexOf(p_93495_) * this.itemHeight + this.itemHeight / 2 - this.height / 2);
   }

   protected void ensureVisible(E p_93499_) {
      int $$1 = this.getRowTop(this.children().indexOf(p_93499_));
      int $$2 = $$1 - this.getY() - 4 - this.itemHeight;
      if ($$2 < 0) {
         this.scroll($$2);
      }

      int $$3 = this.getBottom() - $$1 - this.itemHeight - this.itemHeight;
      if ($$3 < 0) {
         this.scroll(-$$3);
      }
   }

   private void scroll(int p_93430_) {
      this.setScrollAmount(this.getScrollAmount() + p_93430_);
   }

   public double getScrollAmount() {
      return this.scrollAmount;
   }

   public void setClampedScrollAmount(double p_345538_) {
      this.scrollAmount = Mth.clamp(p_345538_, 0.0, (double)this.getMaxScroll());
   }

   public void setScrollAmount(double p_93411_) {
      this.setClampedScrollAmount(p_93411_);
   }

   public void clampScrollAmount() {
      this.setClampedScrollAmount(this.getScrollAmount());
   }

   public int getMaxScroll() {
      return Math.max(0, this.getMaxPosition() - (this.height - 4));
   }

   protected void updateScrollingState(double p_93482_, double p_93483_, int p_93484_) {
      this.scrolling = p_93484_ == 0 && p_93482_ >= this.getScrollbarPosition() && p_93482_ < this.getScrollbarPosition() + 6;
   }

   protected int getScrollbarPosition() {
      return this.getDefaultScrollbarPosition();
   }

   protected int getDefaultScrollbarPosition() {
      return this.getRealRowRight() + this.getListOutlinePadding();
   }

   private int getListOutlinePadding() {
      return 10;
   }

   protected boolean isValidMouseClick(int p_301202_) {
      return p_301202_ == 0;
   }

   @Override
   public boolean mouseClicked(double p_93420_, double p_93421_, int p_93422_) {
      if (!this.isValidMouseClick(p_93422_)) {
         return false;
      } else {
         this.updateScrollingState(p_93420_, p_93421_, p_93422_);
         if (!this.isMouseOver(p_93420_, p_93421_)) {
            return false;
         } else {
            E $$3 = this.getEntryAtPosition(p_93420_, p_93421_);
            if ($$3 != null) {
               if ($$3.mouseClicked(p_93420_, p_93421_, p_93422_)) {
                  E $$4 = this.getFocused();
                  if ($$4 != $$3 && $$4 instanceof ContainerEventHandler $$5) {
                     $$5.setFocused(null);
                  }

                  this.setFocused($$3);
                  this.setDragging(true);
                  return true;
               }
            } else if (this.clickedHeader(
               (int)(p_93420_ - (this.getX() + this.width / 2 - this.getRowWidth() / 2)), (int)(p_93421_ - this.getY()) + (int)this.getScrollAmount() - 4
            )) {
               return true;
            }

            return this.scrolling;
         }
      }
   }

   @Override
   public boolean mouseReleased(double p_93491_, double p_93492_, int p_93493_) {
      return this.getFocused() != null ? this.getFocused().mouseReleased(p_93491_, p_93492_, p_93493_) : false;
   }

   @Override
   public boolean mouseDragged(double p_93424_, double p_93425_, int p_93426_, double p_93427_, double p_93428_) {
      if (super.mouseDragged(p_93424_, p_93425_, p_93426_, p_93427_, p_93428_)) {
         return true;
      } else if (p_93426_ == 0 && this.scrolling) {
         if (p_93425_ < this.getY()) {
            this.setScrollAmount(0.0);
         } else if (p_93425_ > this.getBottom()) {
            this.setScrollAmount(this.getMaxScroll());
         } else {
            double $$5 = Math.max(1, this.getMaxScroll());
            int $$6 = this.height;
            int $$7 = Mth.clamp((int)((float)($$6 * $$6) / this.getMaxPosition()), 32, $$6 - 8);
            double $$8 = Math.max(1.0, $$5 / ($$6 - $$7));
            this.setScrollAmount(this.getScrollAmount() + p_93428_ * $$8);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseScrolled(double p_93416_, double p_93417_, double p_93418_, double p_294369_) {
      this.setScrollAmount(this.getScrollAmount() - p_294369_ * this.itemHeight / 2.0);
      return true;
   }

   @Override
   public void setFocused(@Nullable GuiEventListener p_265738_) {
      super.setFocused(p_265738_);
      int $$1 = this.children.indexOf(p_265738_);
      if ($$1 >= 0) {
         E $$2 = this.children.get($$1);
         this.setSelected($$2);
         if (this.minecraft.getLastInputType().isKeyboard()) {
            this.ensureVisible($$2);
         }
      }
   }

   @Nullable
   protected E nextEntry(ScreenDirection p_265160_) {
      return this.nextEntry(p_265160_, p_93510_ -> true);
   }

   @Nullable
   protected E nextEntry(ScreenDirection p_265210_, Predicate<E> p_265604_) {
      return this.nextEntry(p_265210_, p_265604_, this.getSelected());
   }

   @Nullable
   protected E nextEntry(ScreenDirection p_265159_, Predicate<E> p_265109_, @Nullable E p_265379_) {
      int $$3 = switch (p_265159_) {
         case RIGHT, LEFT -> 0;
         case UP -> -1;
         case DOWN -> 1;
      };
      if (!this.children().isEmpty() && $$3 != 0) {
         int $$4;
         if (p_265379_ == null) {
            $$4 = $$3 > 0 ? 0 : this.children().size() - 1;
         } else {
            $$4 = this.children().indexOf(p_265379_) + $$3;
         }

         for (int $$6 = $$4; $$6 >= 0 && $$6 < this.children.size(); $$6 += $$3) {
            E $$7 = this.children().get($$6);
            if (p_265109_.test($$7)) {
               return $$7;
            }
         }
      }

      return null;
   }

   @Override
   public boolean isMouseOver(double p_93479_, double p_93480_) {
      return p_93480_ >= this.getY() && p_93480_ <= this.getBottom() && p_93479_ >= this.getX() && p_93479_ <= this.getRight();
   }

   protected void renderListItems(GuiGraphics p_282079_, int p_239229_, int p_239230_, float p_239231_) {
      int $$4 = this.getRowLeft();
      int $$5 = this.getRowWidth();
      int $$6 = this.itemHeight - 4;
      int $$7 = this.getItemCount();

      for (int $$8 = 0; $$8 < $$7; $$8++) {
         int $$9 = this.getRowTop($$8);
         int $$10 = this.getRowBottom($$8);
         if ($$10 >= this.getY() && $$9 <= this.getBottom()) {
            this.renderItem(p_282079_, p_239229_, p_239230_, p_239231_, $$8, $$4, $$9, $$5, $$6);
         }
      }
   }

   protected void renderItem(
      GuiGraphics p_282205_, int p_238966_, int p_238967_, float p_238968_, int p_238969_, int p_238970_, int p_238971_, int p_238972_, int p_238973_
   ) {
      E $$9 = this.getEntry(p_238969_);
      $$9.renderBack(p_282205_, p_238969_, p_238971_, p_238970_, p_238972_, p_238973_, p_238966_, p_238967_, Objects.equals(this.hovered, $$9), p_238968_);
      if (this.isSelectedItem(p_238969_)) {
         int $$10 = this.isFocused() ? -1 : -8355712;
         this.renderSelection(p_282205_, p_238971_, p_238972_, p_238973_, $$10, -16777216);
      }

      $$9.render(p_282205_, p_238969_, p_238971_, p_238970_, p_238972_, p_238973_, p_238966_, p_238967_, Objects.equals(this.hovered, $$9), p_238968_);
   }

   protected void renderSelection(GuiGraphics p_283589_, int p_240142_, int p_240143_, int p_240144_, int p_240145_, int p_240146_) {
      int $$6 = this.getX() + (this.width - p_240143_) / 2;
      int $$7 = this.getX() + (this.width + p_240143_) / 2;
      p_283589_.fill($$6, p_240142_ - 2, $$7, p_240142_ + p_240144_ + 2, p_240145_);
      p_283589_.fill($$6 + 1, p_240142_ - 1, $$7 - 1, p_240142_ + p_240144_ + 1, p_240146_);
   }

   public int getRowLeft() {
      return this.getX() + this.width / 2 - this.getRowWidth() / 2 + 2;
   }

   private int getRealRowLeft() {
      return this.getX() + this.width / 2 - this.getRowWidth() / 2;
   }

   public int getRowRight() {
      return this.getRowLeft() + this.getRowWidth();
   }

   private int getRealRowRight() {
      return this.getRealRowLeft() + this.getRowWidth();
   }

   protected int getRowTop(int p_93512_) {
      return this.getY() + 4 - (int)this.getScrollAmount() + p_93512_ * this.itemHeight + this.headerHeight;
   }

   protected int getRowBottom(int p_93486_) {
      return this.getRowTop(p_93486_) + this.itemHeight;
   }

   @Override
   public NarratableEntry.NarrationPriority narrationPriority() {
      if (this.isFocused()) {
         return NarratableEntry.NarrationPriority.FOCUSED;
      } else {
         return this.hovered != null ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
      }
   }

   @Nullable
   protected E remove(int p_93515_) {
      E $$1 = this.children.get(p_93515_);
      return this.removeEntry(this.children.get(p_93515_)) ? $$1 : null;
   }

   protected boolean removeEntry(E p_93503_) {
      boolean $$1 = this.children.remove(p_93503_);
      if ($$1 && p_93503_ == this.getSelected()) {
         this.setSelected(null);
      }

      return $$1;
   }

   @Nullable
   protected E getHovered() {
      return this.hovered;
   }

   void bindEntryToSelf(AbstractSelectionList.Entry<E> p_93506_) {
      p_93506_.list = this;
   }

   protected void narrateListElementPosition(NarrationElementOutput p_168791_, E p_168792_) {
      List<E> $$2 = this.children();
      if ($$2.size() > 1) {
         int $$3 = $$2.indexOf(p_168792_);
         if ($$3 != -1) {
            p_168791_.add(NarratedElementType.POSITION, Component.translatable("narrator.position.list", $$3 + 1, $$2.size()));
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   protected abstract static class Entry<E extends AbstractSelectionList.Entry<E>> implements GuiEventListener {
      @Deprecated
      protected AbstractSelectionList<E> list;

      @Override
      public void setFocused(boolean p_265302_) {
      }

      @Override
      public boolean isFocused() {
         return this.list.getFocused() == this;
      }

      public abstract void render(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10);

      public void renderBack(
         GuiGraphics p_282673_,
         int p_275556_,
         int p_275667_,
         int p_275713_,
         int p_275408_,
         int p_275330_,
         int p_275603_,
         int p_275450_,
         boolean p_275434_,
         float p_275384_
      ) {
      }

      @Override
      public boolean isMouseOver(double p_93537_, double p_93538_) {
         return Objects.equals(this.list.getEntryAtPosition(p_93537_, p_93538_), this);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class TrackedList extends AbstractList<E> {
      private final List<E> delegate = Lists.newArrayList();

      public E get(int p_93557_) {
         return this.delegate.get(p_93557_);
      }

      @Override
      public int size() {
         return this.delegate.size();
      }

      public E set(int p_93559_, E p_93560_) {
         E $$2 = this.delegate.set(p_93559_, p_93560_);
         AbstractSelectionList.this.bindEntryToSelf(p_93560_);
         return $$2;
      }

      public void add(int p_93567_, E p_93568_) {
         this.delegate.add(p_93567_, p_93568_);
         AbstractSelectionList.this.bindEntryToSelf(p_93568_);
      }

      public E remove(int p_93565_) {
         return this.delegate.remove(p_93565_);
      }
   }
}
