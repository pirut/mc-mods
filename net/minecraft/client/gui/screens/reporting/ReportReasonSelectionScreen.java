package net.minecraft.client.gui.screens.reporting;

import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.report.ReportReason;
import net.minecraft.client.multiplayer.chat.report.ReportType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonLinks;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ReportReasonSelectionScreen extends Screen {
   private static final Component REASON_TITLE = Component.translatable("gui.abuseReport.reason.title");
   private static final Component REASON_DESCRIPTION = Component.translatable("gui.abuseReport.reason.description");
   private static final Component READ_INFO_LABEL = Component.translatable("gui.abuseReport.read_info");
   private static final int DESCRIPTION_BOX_WIDTH = 320;
   private static final int DESCRIPTION_BOX_HEIGHT = 62;
   private static final int PADDING = 4;
   @Nullable
   private final Screen lastScreen;
   private ReportReasonSelectionScreen.ReasonSelectionList reasonSelectionList;
   @Nullable
   ReportReason currentlySelectedReason;
   private final Consumer<ReportReason> onSelectedReason;
   final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   final ReportType reportType;

   public ReportReasonSelectionScreen(
      @Nullable Screen lastScreen, @Nullable ReportReason currentlySelectedReason, ReportType reportType, Consumer<ReportReason> onSelectedReason
   ) {
      super(REASON_TITLE);
      this.lastScreen = lastScreen;
      this.currentlySelectedReason = currentlySelectedReason;
      this.onSelectedReason = onSelectedReason;
      this.reportType = reportType;
   }

   @Override
   protected void init() {
      this.layout.addTitleHeader(REASON_TITLE, this.font);
      LinearLayout linearLayout = this.layout.addToContents(LinearLayout.vertical().spacing(4));
      this.reasonSelectionList = linearLayout.addChild(new ReportReasonSelectionScreen.ReasonSelectionList(this.minecraft));
      ReportReasonSelectionScreen.ReasonSelectionList.Entry entry = Optionull.map(this.currentlySelectedReason, this.reasonSelectionList::findEntry);
      this.reasonSelectionList.setSelected(entry);
      linearLayout.addChild(SpacerElement.height(this.descriptionHeight()));
      LinearLayout linearLayout2 = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      linearLayout2.addChild(Button.builder(READ_INFO_LABEL, ConfirmLinkScreen.confirmLink(this, CommonLinks.REPORTING_HELP)).build());
      linearLayout2.addChild(Button.builder(CommonComponents.GUI_DONE, button -> {
         ReportReasonSelectionScreen.ReasonSelectionList.Entry entryx = this.reasonSelectionList.getSelected();
         if (entryx != null) {
            this.onSelectedReason.accept(entryx.getReason());
         }

         this.minecraft.setScreen(this.lastScreen);
      }).build());
      this.layout.visitWidgets(guiEventListener -> {
         AbstractWidget var10000 = this.addRenderableWidget(guiEventListener);
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
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      guiGraphics.fill(this.descriptionLeft(), this.descriptionTop(), this.descriptionRight(), this.descriptionBottom(), -16777216);
      guiGraphics.renderOutline(this.descriptionLeft(), this.descriptionTop(), this.descriptionWidth(), this.descriptionHeight(), -1);
      guiGraphics.drawString(this.font, REASON_DESCRIPTION, this.descriptionLeft() + 4, this.descriptionTop() + 4, -1);
      ReportReasonSelectionScreen.ReasonSelectionList.Entry entry = this.reasonSelectionList.getSelected();
      if (entry != null) {
         int i = this.descriptionLeft() + 4 + 16;
         int j = this.descriptionRight() - 4;
         int k = this.descriptionTop() + 4 + 9 + 2;
         int l = this.descriptionBottom() - 4;
         int m = j - i;
         int n = l - k;
         int o = this.font.wordWrapHeight(entry.reason.description(), m);
         guiGraphics.drawWordWrap(this.font, entry.reason.description(), i, k + (n - o) / 2, m, -1);
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

   @Environment(EnvType.CLIENT)
   public class ReasonSelectionList extends ObjectSelectionList<ReportReasonSelectionScreen.ReasonSelectionList.Entry> {
      public ReasonSelectionList(final Minecraft minecraft) {
         super(
            minecraft,
            ReportReasonSelectionScreen.this.width,
            ReportReasonSelectionScreen.this.listHeight(),
            ReportReasonSelectionScreen.this.layout.getHeaderHeight(),
            18
         );

         for (ReportReason reportReason : ReportReason.values()) {
            if (!ReportReason.getIncompatibleCategories(ReportReasonSelectionScreen.this.reportType).contains(reportReason)) {
               this.addEntry(new ReportReasonSelectionScreen.ReasonSelectionList.Entry(reportReason));
            }
         }
      }

      public ReportReasonSelectionScreen.ReasonSelectionList.Entry findEntry(ReportReason reason) {
         return this.children().stream().filter(entry -> entry.reason == reason).findFirst().orElse(null);
      }

      @Override
      public int getRowWidth() {
         return 320;
      }

      public void setSelected(ReportReasonSelectionScreen.ReasonSelectionList.Entry entry) {
         super.setSelected(entry);
         ReportReasonSelectionScreen.this.currentlySelectedReason = entry != null ? entry.getReason() : null;
      }

      @Environment(EnvType.CLIENT)
      public class Entry extends ObjectSelectionList.Entry<ReportReasonSelectionScreen.ReasonSelectionList.Entry> {
         final ReportReason reason;

         public Entry(final ReportReason reason) {
            this.reason = reason;
         }

         @Override
         public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
            int i = this.getContentX() + 1;
            int j = this.getContentY() + (this.getContentHeight() - 9) / 2 + 1;
            guiGraphics.drawString(ReportReasonSelectionScreen.this.font, this.reason.title(), i, j, -1);
         }

         @Override
         public Component getNarration() {
            return Component.translatable("gui.abuseReport.reason.narration", this.reason.title(), this.reason.description());
         }

         @Override
         public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            ReasonSelectionList.this.setSelected(this);
            return super.mouseClicked(event, isDoubleClick);
         }

         public ReportReason getReason() {
            return this.reason;
         }
      }
   }
}
