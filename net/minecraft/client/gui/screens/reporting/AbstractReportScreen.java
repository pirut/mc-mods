package net.minecraft.client.gui.screens.reporting;

import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.logging.LogUtils;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.GenericWaitingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.WarningScreen;
import net.minecraft.client.multiplayer.chat.report.Report;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ThrowingComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractReportScreen<B extends Report.Builder<?>> extends Screen {
   private static final Component REPORT_SENT_MESSAGE = Component.translatable("gui.abuseReport.report_sent_msg");
   private static final Component REPORT_SENDING_TITLE = Component.translatable("gui.abuseReport.sending.title").withStyle(ChatFormatting.BOLD);
   private static final Component REPORT_SENT_TITLE = Component.translatable("gui.abuseReport.sent.title").withStyle(ChatFormatting.BOLD);
   private static final Component REPORT_ERROR_TITLE = Component.translatable("gui.abuseReport.error.title").withStyle(ChatFormatting.BOLD);
   private static final Component REPORT_SEND_GENERIC_ERROR = Component.translatable("gui.abuseReport.send.generic_error");
   protected static final Component SEND_REPORT = Component.translatable("gui.abuseReport.send");
   protected static final Component OBSERVED_WHAT_LABEL = Component.translatable("gui.abuseReport.observed_what");
   protected static final Component SELECT_REASON = Component.translatable("gui.abuseReport.select_reason");
   private static final Component DESCRIBE_PLACEHOLDER = Component.translatable("gui.abuseReport.describe");
   protected static final Component MORE_COMMENTS_LABEL = Component.translatable("gui.abuseReport.more_comments");
   private static final Component MORE_COMMENTS_NARRATION = Component.translatable("gui.abuseReport.comments");
   private static final Component ATTESTATION_CHECKBOX = Component.translatable("gui.abuseReport.attestation");
   protected static final int BUTTON_WIDTH = 120;
   protected static final int MARGIN = 20;
   protected static final int SCREEN_WIDTH = 280;
   protected static final int SPACING = 8;
   private static final Logger LOGGER = LogUtils.getLogger();
   protected final Screen lastScreen;
   protected final ReportingContext reportingContext;
   protected final LinearLayout layout = LinearLayout.vertical().spacing(8);
   protected B reportBuilder;
   private Checkbox attestation;
   protected Button sendButton;

   protected AbstractReportScreen(Component p_299999_, Screen p_300006_, ReportingContext p_299904_, B p_299834_) {
      super(p_299999_);
      this.lastScreen = p_300006_;
      this.reportingContext = p_299904_;
      this.reportBuilder = p_299834_;
   }

   protected MultiLineEditBox createCommentBox(int p_300027_, int p_300007_, Consumer<String> p_299915_) {
      AbuseReportLimits $$3 = this.reportingContext.sender().reportLimits();
      MultiLineEditBox $$4 = new MultiLineEditBox(this.font, 0, 0, p_300027_, p_300007_, DESCRIBE_PLACEHOLDER, MORE_COMMENTS_NARRATION);
      $$4.setValue(this.reportBuilder.comments());
      $$4.setCharacterLimit($$3.maxOpinionCommentsLength());
      $$4.setValueListener(p_299915_);
      return $$4;
   }

   @Override
   protected void init() {
      this.layout.defaultCellSetting().alignHorizontallyCenter();
      this.createHeader();
      this.addContent();
      this.createFooter();
      this.onReportChanged();
      this.layout.visitWidgets(p_352666_ -> {
         AbstractWidget var10000 = this.addRenderableWidget(p_352666_);
      });
      this.repositionElements();
   }

   protected void createHeader() {
      this.layout.addChild(new StringWidget(this.title, this.font));
   }

   protected abstract void addContent();

   protected void createFooter() {
      this.attestation = this.layout
         .addChild(
            Checkbox.builder(ATTESTATION_CHECKBOX, this.font).selected(this.reportBuilder.attested()).maxWidth(280).onValueChange((p_352662_, p_352663_) -> {
               this.reportBuilder.setAttested(p_352663_);
               this.onReportChanged();
            }).build()
         );
      LinearLayout $$0 = this.layout.addChild(LinearLayout.horizontal().spacing(8));
      $$0.addChild(Button.builder(CommonComponents.GUI_BACK, p_352664_ -> this.onClose()).width(120).build());
      this.sendButton = $$0.addChild(Button.builder(SEND_REPORT, p_352661_ -> this.sendReport()).width(120).build());
   }

   protected void onReportChanged() {
      Report.CannotBuildReason $$0 = this.reportBuilder.checkBuildable();
      this.sendButton.active = $$0 == null && this.attestation.selected();
      this.sendButton.setTooltip(Optionull.map($$0, Report.CannotBuildReason::tooltip));
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      FrameLayout.centerInRectangle(this.layout, this.getRectangle());
   }

   protected void sendReport() {
      this.reportBuilder.build(this.reportingContext).ifLeft(p_299972_ -> {
         CompletableFuture<?> $$1 = this.reportingContext.sender().send(p_299972_.id(), p_299972_.reportType(), p_299972_.report());
         this.minecraft.setScreen(GenericWaitingScreen.createWaiting(REPORT_SENDING_TITLE, CommonComponents.GUI_CANCEL, () -> {
            this.minecraft.setScreen(this);
            $$1.cancel(true);
         }));
         $$1.handleAsync((p_299984_, p_299884_) -> {
            if (p_299884_ == null) {
               this.onReportSendSuccess();
            } else {
               if (p_299884_ instanceof CancellationException) {
                  return null;
               }

               this.onReportSendError(p_299884_);
            }

            return null;
         }, this.minecraft);
      }).ifRight(p_300030_ -> this.displayReportSendError(p_300030_.message()));
   }

   private void onReportSendSuccess() {
      this.clearDraft();
      this.minecraft
         .setScreen(
            GenericWaitingScreen.createCompleted(REPORT_SENT_TITLE, REPORT_SENT_MESSAGE, CommonComponents.GUI_DONE, () -> this.minecraft.setScreen(null))
         );
   }

   private void onReportSendError(Throwable p_299933_) {
      LOGGER.error("Encountered error while sending abuse report", p_299933_);
      Component $$2;
      if (p_299933_.getCause() instanceof ThrowingComponent $$1) {
         $$2 = $$1.getComponent();
      } else {
         $$2 = REPORT_SEND_GENERIC_ERROR;
      }

      this.displayReportSendError($$2);
   }

   private void displayReportSendError(Component p_299983_) {
      Component $$1 = p_299983_.copy().withStyle(ChatFormatting.RED);
      this.minecraft.setScreen(GenericWaitingScreen.createCompleted(REPORT_ERROR_TITLE, $$1, CommonComponents.GUI_BACK, () -> this.minecraft.setScreen(this)));
   }

   void saveDraft() {
      if (this.reportBuilder.hasContent()) {
         this.reportingContext.setReportDraft(this.reportBuilder.report().copy());
      }
   }

   void clearDraft() {
      this.reportingContext.setReportDraft(null);
   }

   @Override
   public void onClose() {
      if (this.reportBuilder.hasContent()) {
         this.minecraft.setScreen(new AbstractReportScreen.DiscardReportWarningScreen());
      } else {
         this.minecraft.setScreen(this.lastScreen);
      }
   }

   @Override
   public void removed() {
      this.saveDraft();
      super.removed();
   }

   @OnlyIn(Dist.CLIENT)
   class DiscardReportWarningScreen extends WarningScreen {
      private static final Component TITLE = Component.translatable("gui.abuseReport.discard.title").withStyle(ChatFormatting.BOLD);
      private static final Component MESSAGE = Component.translatable("gui.abuseReport.discard.content");
      private static final Component RETURN = Component.translatable("gui.abuseReport.discard.return");
      private static final Component DRAFT = Component.translatable("gui.abuseReport.discard.draft");
      private static final Component DISCARD = Component.translatable("gui.abuseReport.discard.discard");

      protected DiscardReportWarningScreen() {
         super(TITLE, MESSAGE, MESSAGE);
      }

      @Override
      protected Layout addFooterButtons() {
         LinearLayout $$0 = LinearLayout.vertical().spacing(8);
         $$0.defaultCellSetting().alignHorizontallyCenter();
         LinearLayout $$1 = $$0.addChild(LinearLayout.horizontal().spacing(8));
         $$1.addChild(Button.builder(RETURN, p_299917_ -> this.onClose()).build());
         $$1.addChild(Button.builder(DRAFT, p_299913_ -> {
            AbstractReportScreen.this.saveDraft();
            this.minecraft.setScreen(AbstractReportScreen.this.lastScreen);
         }).build());
         $$0.addChild(Button.builder(DISCARD, p_299901_ -> {
            AbstractReportScreen.this.clearDraft();
            this.minecraft.setScreen(AbstractReportScreen.this.lastScreen);
         }).build());
         return $$0;
      }

      @Override
      public void onClose() {
         this.minecraft.setScreen(AbstractReportScreen.this);
      }

      @Override
      public boolean shouldCloseOnEsc() {
         return false;
      }
   }
}
