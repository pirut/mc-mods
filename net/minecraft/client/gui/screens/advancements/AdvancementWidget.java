package net.minecraft.client.gui.screens.advancements;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AdvancementWidget {
   private static final ResourceLocation TITLE_BOX_SPRITE = ResourceLocation.withDefaultNamespace("advancements/title_box");
   private static final int HEIGHT = 26;
   private static final int BOX_X = 0;
   private static final int BOX_WIDTH = 200;
   private static final int FRAME_WIDTH = 26;
   private static final int ICON_X = 8;
   private static final int ICON_Y = 5;
   private static final int ICON_WIDTH = 26;
   private static final int TITLE_PADDING_LEFT = 3;
   private static final int TITLE_PADDING_RIGHT = 5;
   private static final int TITLE_X = 32;
   private static final int TITLE_Y = 9;
   private static final int TITLE_MAX_WIDTH = 163;
   private static final int[] TEST_SPLIT_OFFSETS = new int[]{0, 10, -10, 25, -25};
   private final AdvancementTab tab;
   private final AdvancementNode advancementNode;
   private final DisplayInfo display;
   private final FormattedCharSequence title;
   private final int width;
   private final List<FormattedCharSequence> description;
   private final Minecraft minecraft;
   @Nullable
   private AdvancementWidget parent;
   private final List<AdvancementWidget> children = Lists.newArrayList();
   @Nullable
   private AdvancementProgress progress;
   private final int x;
   private final int y;

   public AdvancementWidget(AdvancementTab p_97255_, Minecraft p_97256_, AdvancementNode p_300868_, DisplayInfo p_97258_) {
      this.tab = p_97255_;
      this.advancementNode = p_300868_;
      this.display = p_97258_;
      this.minecraft = p_97256_;
      this.title = Language.getInstance().getVisualOrder(p_97256_.font.substrByWidth(p_97258_.getTitle(), 163));
      this.x = Mth.floor(p_97258_.getX() * 28.0F);
      this.y = Mth.floor(p_97258_.getY() * 27.0F);
      int $$4 = this.getMaxProgressWidth();
      int $$5 = 29 + p_97256_.font.width(this.title) + $$4;
      this.description = Language.getInstance()
         .getVisualOrder(
            this.findOptimalLines(ComponentUtils.mergeStyles(p_97258_.getDescription().copy(), Style.EMPTY.withColor(p_97258_.getType().getChatColor())), $$5)
         );

      for (FormattedCharSequence $$6 : this.description) {
         $$5 = Math.max($$5, p_97256_.font.width($$6));
      }

      this.width = $$5 + 3 + 5;
   }

   private int getMaxProgressWidth() {
      int $$0 = this.advancementNode.advancement().requirements().size();
      if ($$0 <= 1) {
         return 0;
      } else {
         int $$1 = 8;
         Component $$2 = Component.translatable("advancements.progress", $$0, $$0);
         return this.minecraft.font.width($$2) + 8;
      }
   }

   private static float getMaxWidth(StringSplitter p_97304_, List<FormattedText> p_97305_) {
      return (float)p_97305_.stream().mapToDouble(p_97304_::stringWidth).max().orElse(0.0);
   }

   private List<FormattedText> findOptimalLines(Component p_97309_, int p_97310_) {
      StringSplitter $$2 = this.minecraft.font.getSplitter();
      List<FormattedText> $$3 = null;
      float $$4 = Float.MAX_VALUE;

      for (int $$5 : TEST_SPLIT_OFFSETS) {
         List<FormattedText> $$6 = $$2.splitLines(p_97309_, p_97310_ - $$5, Style.EMPTY);
         float $$7 = Math.abs(getMaxWidth($$2, $$6) - p_97310_);
         if ($$7 <= 10.0F) {
            return $$6;
         }

         if ($$7 < $$4) {
            $$4 = $$7;
            $$3 = $$6;
         }
      }

      return $$3;
   }

   @Nullable
   private AdvancementWidget getFirstVisibleParent(AdvancementNode p_301093_) {
      do {
         p_301093_ = p_301093_.parent();
      } while (p_301093_ != null && p_301093_.advancement().display().isEmpty());

      return p_301093_ != null && !p_301093_.advancement().display().isEmpty() ? this.tab.getWidget(p_301093_.holder()) : null;
   }

   public void drawConnectivity(GuiGraphics p_281947_, int p_97300_, int p_97301_, boolean p_97302_) {
      if (this.parent != null) {
         int $$4 = p_97300_ + this.parent.x + 13;
         int $$5 = p_97300_ + this.parent.x + 26 + 4;
         int $$6 = p_97301_ + this.parent.y + 13;
         int $$7 = p_97300_ + this.x + 13;
         int $$8 = p_97301_ + this.y + 13;
         int $$9 = p_97302_ ? -16777216 : -1;
         if (p_97302_) {
            p_281947_.hLine($$5, $$4, $$6 - 1, $$9);
            p_281947_.hLine($$5 + 1, $$4, $$6, $$9);
            p_281947_.hLine($$5, $$4, $$6 + 1, $$9);
            p_281947_.hLine($$7, $$5 - 1, $$8 - 1, $$9);
            p_281947_.hLine($$7, $$5 - 1, $$8, $$9);
            p_281947_.hLine($$7, $$5 - 1, $$8 + 1, $$9);
            p_281947_.vLine($$5 - 1, $$8, $$6, $$9);
            p_281947_.vLine($$5 + 1, $$8, $$6, $$9);
         } else {
            p_281947_.hLine($$5, $$4, $$6, $$9);
            p_281947_.hLine($$7, $$5, $$8, $$9);
            p_281947_.vLine($$5, $$8, $$6, $$9);
         }
      }

      for (AdvancementWidget $$10 : this.children) {
         $$10.drawConnectivity(p_281947_, p_97300_, p_97301_, p_97302_);
      }
   }

   public void draw(GuiGraphics p_281958_, int p_281323_, int p_283679_) {
      if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
         float $$3 = this.progress == null ? 0.0F : this.progress.getPercent();
         AdvancementWidgetType $$4;
         if ($$3 >= 1.0F) {
            $$4 = AdvancementWidgetType.OBTAINED;
         } else {
            $$4 = AdvancementWidgetType.UNOBTAINED;
         }

         p_281958_.blitSprite($$4.frameSprite(this.display.getType()), p_281323_ + this.x + 3, p_283679_ + this.y, 26, 26);
         p_281958_.renderFakeItem(this.display.getIcon(), p_281323_ + this.x + 8, p_283679_ + this.y + 5);
      }

      for (AdvancementWidget $$6 : this.children) {
         $$6.draw(p_281958_, p_281323_, p_283679_);
      }
   }

   public int getWidth() {
      return this.width;
   }

   public void setProgress(AdvancementProgress p_97265_) {
      this.progress = p_97265_;
   }

   public void addChild(AdvancementWidget p_97307_) {
      this.children.add(p_97307_);
   }

   public void drawHover(GuiGraphics p_283068_, int p_281304_, int p_281253_, float p_281848_, int p_282097_, int p_281537_) {
      boolean $$6 = p_282097_ + p_281304_ + this.x + this.width + 26 >= this.tab.getScreen().width;
      Component $$7 = this.progress == null ? null : this.progress.getProgressText();
      int $$8 = $$7 == null ? 0 : this.minecraft.font.width($$7);
      boolean $$9 = 113 - p_281253_ - this.y - 26 <= 6 + this.description.size() * 9;
      float $$10 = this.progress == null ? 0.0F : this.progress.getPercent();
      int $$11 = Mth.floor($$10 * this.width);
      AdvancementWidgetType $$12;
      AdvancementWidgetType $$13;
      AdvancementWidgetType $$14;
      if ($$10 >= 1.0F) {
         $$11 = this.width / 2;
         $$12 = AdvancementWidgetType.OBTAINED;
         $$13 = AdvancementWidgetType.OBTAINED;
         $$14 = AdvancementWidgetType.OBTAINED;
      } else if ($$11 < 2) {
         $$11 = this.width / 2;
         $$12 = AdvancementWidgetType.UNOBTAINED;
         $$13 = AdvancementWidgetType.UNOBTAINED;
         $$14 = AdvancementWidgetType.UNOBTAINED;
      } else if ($$11 > this.width - 2) {
         $$11 = this.width / 2;
         $$12 = AdvancementWidgetType.OBTAINED;
         $$13 = AdvancementWidgetType.OBTAINED;
         $$14 = AdvancementWidgetType.UNOBTAINED;
      } else {
         $$12 = AdvancementWidgetType.OBTAINED;
         $$13 = AdvancementWidgetType.UNOBTAINED;
         $$14 = AdvancementWidgetType.UNOBTAINED;
      }

      int $$24 = this.width - $$11;
      RenderSystem.enableBlend();
      int $$25 = p_281253_ + this.y;
      int $$26;
      if ($$6) {
         $$26 = p_281304_ + this.x - this.width + 26 + 6;
      } else {
         $$26 = p_281304_ + this.x;
      }

      int $$28 = 32 + this.description.size() * 9;
      if (!this.description.isEmpty()) {
         if ($$9) {
            p_283068_.blitSprite(TITLE_BOX_SPRITE, $$26, $$25 + 26 - $$28, this.width, $$28);
         } else {
            p_283068_.blitSprite(TITLE_BOX_SPRITE, $$26, $$25, this.width, $$28);
         }
      }

      p_283068_.blitSprite($$12.boxSprite(), 200, 26, 0, 0, $$26, $$25, $$11, 26);
      p_283068_.blitSprite($$13.boxSprite(), 200, 26, 200 - $$24, 0, $$26 + $$11, $$25, $$24, 26);
      p_283068_.blitSprite($$14.frameSprite(this.display.getType()), p_281304_ + this.x + 3, p_281253_ + this.y, 26, 26);
      if ($$6) {
         p_283068_.drawString(this.minecraft.font, this.title, $$26 + 5, p_281253_ + this.y + 9, -1);
         if ($$7 != null) {
            p_283068_.drawString(this.minecraft.font, $$7, p_281304_ + this.x - $$8, p_281253_ + this.y + 9, -1);
         }
      } else {
         p_283068_.drawString(this.minecraft.font, this.title, p_281304_ + this.x + 32, p_281253_ + this.y + 9, -1);
         if ($$7 != null) {
            p_283068_.drawString(this.minecraft.font, $$7, p_281304_ + this.x + this.width - $$8 - 5, p_281253_ + this.y + 9, -1);
         }
      }

      if ($$9) {
         for (int $$29 = 0; $$29 < this.description.size(); $$29++) {
            p_283068_.drawString(this.minecraft.font, this.description.get($$29), $$26 + 5, $$25 + 26 - $$28 + 7 + $$29 * 9, -5592406, false);
         }
      } else {
         for (int $$30 = 0; $$30 < this.description.size(); $$30++) {
            p_283068_.drawString(this.minecraft.font, this.description.get($$30), $$26 + 5, p_281253_ + this.y + 9 + 17 + $$30 * 9, -5592406, false);
         }
      }

      p_283068_.renderFakeItem(this.display.getIcon(), p_281304_ + this.x + 8, p_281253_ + this.y + 5);
   }

   public boolean isMouseOver(int p_97260_, int p_97261_, int p_97262_, int p_97263_) {
      if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
         int $$4 = p_97260_ + this.x;
         int $$5 = $$4 + 26;
         int $$6 = p_97261_ + this.y;
         int $$7 = $$6 + 26;
         return p_97262_ >= $$4 && p_97262_ <= $$5 && p_97263_ >= $$6 && p_97263_ <= $$7;
      } else {
         return false;
      }
   }

   public void attachToParent() {
      if (this.parent == null && this.advancementNode.parent() != null) {
         this.parent = this.getFirstVisibleParent(this.advancementNode);
         if (this.parent != null) {
            this.parent.addChild(this);
         }
      }
   }

   public int getY() {
      return this.y;
   }

   public int getX() {
      return this.x;
   }
}
