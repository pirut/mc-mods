package net.minecraft.client.gui.screens.reporting;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Optionull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.report.ReportReason;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonLinks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ReportReasonSelectionScreen extends Screen {
   private static final Component REASON_TITLE = Component.translatable("gui.abuseReport.reason.title");
   private static final Component REASON_DESCRIPTION = Component.translatable("gui.abuseReport.reason.description");
   private static final Component READ_INFO_LABEL = Component.translatable("gui.abuseReport.read_info");
   private static final int DESCRIPTION_BOX_WIDTH = 320;
   private static final int DESCRIPTION_BOX_HEIGHT = 62;
   private static final int PADDING = 4;
   @Nullable
   private final Screen lastScreen;
   @Nullable
   private ReportReasonSelectionScreen.ReasonSelectionList reasonSelectionList;
   @Nullable
   ReportReason currentlySelectedReason;
   private final Consumer<ReportReason> onSelectedReason;
   final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

   public ReportReasonSelectionScreen(@Nullable Screen p_239438_, @Nullable ReportReason p_239439_, Consumer<ReportReason> p_239440_) {
      super(REASON_TITLE);
      this.lastScreen = p_239438_;
      this.currentlySelectedReason = p_239439_;
      this.onSelectedReason = p_239440_;
   }

   @Override
   protected void init() {
      this.layout.addTitleHeader(REASON_TITLE, this.font);
      LinearLayout $$0 = this.layout.addToContents(LinearLayout.vertical().spacing(4));
      this.reasonSelectionList = $$0.addChild(new ReportReasonSelectionScreen.ReasonSelectionList(this.minecraft));
      ReportReasonSelectionScreen.ReasonSelectionList.Entry $$1 = Optionull.map(this.currentlySelectedReason, this.reasonSelectionList::findEntry);
      this.reasonSelectionList.setSelected($$1);
      $$0.addChild(SpacerElement.height(this.descriptionHeight()));
      LinearLayout $$2 = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      $$2.addChild(Button.builder(READ_INFO_LABEL, ConfirmLinkScreen.confirmLink(this, CommonLinks.REPORTING_HELP)).build());
      $$2.addChild(Button.builder(CommonComponents.GUI_DONE, p_329741_ -> {
         ReportReasonSelectionScreen.ReasonSelectionList.Entry $$1x = this.reasonSelectionList.getSelected();
         if ($$1x != null) {
            this.onSelectedReason.accept($$1x.getReason());
         }

         this.minecraft.setScreen(this.lastScreen);
      }).build());
      this.layout.visitWidgets(p_329740_ -> {
         AbstractWidget var10000 = this.addRenderableWidget(p_329740_);
      });
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      if (this.reasonSelectionList != null) {
         this.reasonSelectionList.updateSizeAndPosition(this.width, this.listHeight(), this.layout.getHeaderHeight());
      }
   }

   @Override
   public void render(GuiGraphics p_282815_, int p_283039_, int p_283620_, float p_281336_) {
      super.render(p_282815_, p_283039_, p_283620_, p_281336_);
      p_282815_.fill(this.descriptionLeft(), this.descriptionTop(), this.descriptionRight(), this.descriptionBottom(), -16777216);
      p_282815_.renderOutline(this.descriptionLeft(), this.descriptionTop(), this.descriptionWidth(), this.descriptionHeight(), -1);
      p_282815_.drawString(this.font, REASON_DESCRIPTION, this.descriptionLeft() + 4, this.descriptionTop() + 4, -1);
      ReportReasonSelectionScreen.ReasonSelectionList.Entry $$4 = this.reasonSelectionList.getSelected();
      if ($$4 != null) {
         int $$5 = this.descriptionLeft() + 4 + 16;
         int $$6 = this.descriptionRight() - 4;
         int $$7 = this.descriptionTop() + 4 + 9 + 2;
         int $$8 = this.descriptionBottom() - 4;
         int $$9 = $$6 - $$5;
         int $$10 = $$8 - $$7;
         int $$11 = this.font.wordWrapHeight($$4.reason.description(), $$9);
         p_282815_.drawWordWrap(this.font, $$4.reason.description(), $$5, $$7 + ($$10 - $$11) / 2, $$9, -1);
      }
   }

   private int descriptionLeft() {
      return (this.width - 320) / 2;
   }

   private int descriptionRight() {
      return (this.width + 320) / 2;
   }

   private int descriptionTop() {
      return this.descriptionBottom() - this.descriptionHeight();
   }

   private int descriptionBottom() {
      return this.height - this.layout.getFooterHeight() - 4;
   }

   private int descriptionWidth() {
      return 320;
   }

   private int descriptionHeight() {
      return 62;
   }

   int listHeight() {
      return this.layout.getContentHeight() - this.descriptionHeight() - 8;
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   @OnlyIn(Dist.CLIENT)
   public class ReasonSelectionList extends ObjectSelectionList<ReportReasonSelectionScreen.ReasonSelectionList.Entry> {
      public ReasonSelectionList(Minecraft p_239715_) {
         super(
            p_239715_,
            ReportReasonSelectionScreen.this.width,
            ReportReasonSelectionScreen.this.listHeight(),
            ReportReasonSelectionScreen.this.layout.getHeaderHeight(),
            18
         );

         for (ReportReason $$2 : ReportReason.values()) {
            this.addEntry(new ReportReasonSelectionScreen.ReasonSelectionList.Entry($$2));
         }
      }

      @Nullable
      public ReportReasonSelectionScreen.ReasonSelectionList.Entry findEntry(ReportReason p_239168_) {
         return this.children().stream().filter(p_239293_ -> p_239293_.reason == p_239168_).findFirst().orElse(null);
      }

      @Override
      public int getRowWidth() {
         return 320;
      }

      public void setSelected(@Nullable ReportReasonSelectionScreen.ReasonSelectionList.Entry p_240601_) {
         super.setSelected(p_240601_);
         ReportReasonSelectionScreen.this.currentlySelectedReason = p_240601_ != null ? p_240601_.getReason() : null;
      }

      @OnlyIn(Dist.CLIENT)
      public class Entry extends ObjectSelectionList.Entry<ReportReasonSelectionScreen.ReasonSelectionList.Entry> {
         final ReportReason reason;

         public Entry(ReportReason p_239267_) {
            this.reason = p_239267_;
         }

         @Override
         public void render(
            GuiGraphics p_281941_,
            int p_281450_,
            int p_281781_,
            int p_283334_,
            int p_283073_,
            int p_282523_,
            int p_282667_,
            int p_281567_,
            boolean p_282095_,
            float p_283305_
         ) {
            int $$10 = p_283334_ + 1;
            int $$11 = p_281781_ + (p_282523_ - 9) / 2 + 1;
            p_281941_.drawString(ReportReasonSelectionScreen.this.font, this.reason.title(), $$10, $$11, -1);
         }

         @Override
         public Component getNarration() {
            return Component.translatable("gui.abuseReport.reason.narration", this.reason.title(), this.reason.description());
         }

         @Override
         public boolean mouseClicked(double p_240021_, double p_240022_, int p_240023_) {
            ReasonSelectionList.this.setSelected(this);
            return super.mouseClicked(p_240021_, p_240022_, p_240023_);
         }

         public ReportReason getReason() {
            return this.reason;
         }
      }
   }
}
