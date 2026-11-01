package net.minecraft.client.gui.screens.reporting;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Optionull;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.ChatTrustLevel;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.client.multiplayer.chat.report.ChatReport;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ChatSelectionScreen extends Screen {
   static final Identifier CHECKMARK_SPRITE = Identifier.withDefaultNamespace("icon/checkmark");
   private static final Component TITLE = Component.translatable("gui.chatSelection.title");
   private static final Component CONTEXT_INFO = Component.translatable("gui.chatSelection.context");
   @Nullable
   private final Screen lastScreen;
   private final ReportingContext reportingContext;
   private Button confirmSelectedButton;
   private MultiLineLabel contextInfoLabel;
   private ChatSelectionScreen.ChatSelectionList chatSelectionList;
   final ChatReport.Builder report;
   private final Consumer<ChatReport.Builder> onSelected;
   private ChatSelectionLogFiller chatLogFiller;

   public ChatSelectionScreen(
      @Nullable Screen lastScreen, ReportingContext reportingContext, ChatReport.Builder report, Consumer<ChatReport.Builder> onSelected
   ) {
      super(TITLE);
      this.lastScreen = lastScreen;
      this.reportingContext = reportingContext;
      this.report = report.copy();
      this.onSelected = onSelected;
   }

   @Override
   protected void init() {
      this.chatLogFiller = new ChatSelectionLogFiller(this.reportingContext, this::canReport);
      this.contextInfoLabel = MultiLineLabel.create(this.font, CONTEXT_INFO, this.width - 16);
      this.chatSelectionList = this.addRenderableWidget(
         new ChatSelectionScreen.ChatSelectionList(this.minecraft, (this.contextInfoLabel.getLineCount() + 1) * 9)
      );
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).bounds(this.width / 2 - 155, this.height - 32, 150, 20).build()
      );
      this.confirmSelectedButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
         this.onSelected.accept(this.report);
         this.onClose();
      }).bounds(this.width / 2 - 155 + 160, this.height - 32, 150, 20).build());
      this.updateConfirmSelectedButton();
      this.extendLog();
      this.chatSelectionList.setScrollAmount(this.chatSelectionList.maxScrollAmount());
   }

   private boolean canReport(LoggedChatMessage message) {
      return message.canReport(this.report.reportedProfileId());
   }

   private void extendLog() {
      int i = this.chatSelectionList.getMaxVisibleEntries();
      this.chatLogFiller.fillNextPage(i, this.chatSelectionList);
   }

   void onReachedScrollTop() {
      this.extendLog();
   }

   void updateConfirmSelectedButton() {
      this.confirmSelectedButton.active = !this.report.reportedMessages().isEmpty();
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      ActiveTextCollector activeTextCollector = guiGraphics.textRenderer();
      guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, -1);
      AbuseReportLimits abuseReportLimits = this.reportingContext.sender().reportLimits();
      int i = this.report.reportedMessages().size();
      int j = abuseReportLimits.maxReportedMessageCount();
      Component component = Component.translatable("gui.chatSelection.selected", i, j);
      guiGraphics.drawCenteredString(this.font, component, this.width / 2, 26, -1);
      int k = this.chatSelectionList.getFooterTop();
      this.contextInfoLabel.visitLines(TextAlignment.CENTER, this.width / 2, k, 9, activeTextCollector);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), CONTEXT_INFO);
   }

   @Environment(EnvType.CLIENT)
   public class ChatSelectionList extends ObjectSelectionList<ChatSelectionScreen.ChatSelectionList.Entry> implements ChatSelectionLogFiller.Output {
      public static final int ITEM_HEIGHT = 16;
      @Nullable
      private ChatSelectionScreen.ChatSelectionList.Heading previousHeading;

      public ChatSelectionList(final Minecraft minecraft, final int height) {
         super(minecraft, ChatSelectionScreen.this.width, ChatSelectionScreen.this.height - height - 80, 40, 16);
      }

      @Override
      public void setScrollAmount(double scrollAmount) {
         double d = this.scrollAmount();
         super.setScrollAmount(scrollAmount);
         if (this.maxScrollAmount() > 1.0E-5F && scrollAmount <= 1.0E-5F && !Mth.equal(scrollAmount, d)) {
            ChatSelectionScreen.this.onReachedScrollTop();
         }
      }

      @Override
      public void acceptMessage(int chatId, LoggedChatMessage.Player playerMessage) {
         boolean bl = playerMessage.canReport(ChatSelectionScreen.this.report.reportedProfileId());
         ChatTrustLevel chatTrustLevel = playerMessage.trustLevel();
         GuiMessageTag guiMessageTag = chatTrustLevel.createTag(playerMessage.message());
         ChatSelectionScreen.ChatSelectionList.Entry entry = new ChatSelectionScreen.ChatSelectionList.MessageEntry(
            chatId, playerMessage.toContentComponent(), playerMessage.toNarrationComponent(), guiMessageTag, bl, true
         );
         this.addEntryToTop(entry);
         this.updateHeading(playerMessage, bl);
      }

      private void updateHeading(LoggedChatMessage.Player loggedPlayerChatMessage, boolean canReport) {
         ChatSelectionScreen.ChatSelectionList.Entry entry = new ChatSelectionScreen.ChatSelectionList.MessageHeadingEntry(
            loggedPlayerChatMessage.profile(), loggedPlayerChatMessage.toHeadingComponent(), canReport
         );
         this.addEntryToTop(entry);
         ChatSelectionScreen.ChatSelectionList.Heading heading = new ChatSelectionScreen.ChatSelectionList.Heading(loggedPlayerChatMessage.profileId(), entry);
         if (this.previousHeading != null && this.previousHeading.canCombine(heading)) {
            this.removeEntryFromTop(this.previousHeading.entry());
         }

         this.previousHeading = heading;
      }

      @Override
      public void acceptDivider(Component text) {
         this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.PaddingEntry());
         this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.DividerEntry(text));
         this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.PaddingEntry());
         this.previousHeading = null;
      }

      @Override
      public int getRowWidth() {
         return Math.min(350, this.width - 50);
      }

      public int getMaxVisibleEntries() {
         return Mth.positiveCeilDiv(this.height, 16);
      }

      protected void renderItem(GuiGraphics guiGraphics, int i, int j, float f, ChatSelectionScreen.ChatSelectionList.Entry entry) {
         if (this.shouldHighlightEntry(entry)) {
            boolean bl = this.getSelected() == entry;
            int k = this.isFocused() && bl ? -1 : -8355712;
            this.renderSelection(guiGraphics, entry, k);
         }

         entry.renderContent(guiGraphics, i, j, this.getHovered() == entry, f);
      }

      private boolean shouldHighlightEntry(ChatSelectionScreen.ChatSelectionList.Entry entry) {
         if (entry.canSelect()) {
            boolean bl = this.getSelected() == entry;
            boolean bl2 = this.getSelected() == null;
            boolean bl3 = this.getHovered() == entry;
            return bl || bl2 && bl3 && entry.canReport();
         } else {
            return false;
         }
      }

      @Nullable
      protected ChatSelectionScreen.ChatSelectionList.Entry nextEntry(ScreenDirection screenDirection) {
         return this.nextEntry(screenDirection, ChatSelectionScreen.ChatSelectionList.Entry::canSelect);
      }

      public void setSelected(@Nullable ChatSelectionScreen.ChatSelectionList.Entry entry) {
         super.setSelected(entry);
         ChatSelectionScreen.ChatSelectionList.Entry entry2 = this.nextEntry(ScreenDirection.UP);
         if (entry2 == null) {
            ChatSelectionScreen.this.onReachedScrollTop();
         }
      }

      @Override
      public boolean keyPressed(KeyEvent event) {
         ChatSelectionScreen.ChatSelectionList.Entry entry = this.getSelected();
         return entry != null && entry.keyPressed(event) ? true : super.keyPressed(event);
      }

      public int getFooterTop() {
         return this.getBottom() + 9;
      }

      @Environment(EnvType.CLIENT)
      public class DividerEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         private final Component text;

         public DividerEntry(final Component text) {
            this.text = text;
         }

         @Override
         public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
            int i = this.getContentYMiddle();
            int j = this.getContentRight() - 8;
            int k = ChatSelectionScreen.this.font.width(this.text);
            int l = (this.getContentX() + j - k) / 2;
            int m = i - 9 / 2;
            guiGraphics.drawString(ChatSelectionScreen.this.font, this.text, l, m, -6250336);
         }

         @Override
         public Component getNarration() {
            return this.text;
         }
      }

      @Environment(EnvType.CLIENT)
      public abstract static class Entry extends ObjectSelectionList.Entry<ChatSelectionScreen.ChatSelectionList.Entry> {
         @Override
         public Component getNarration() {
            return CommonComponents.EMPTY;
         }

         public boolean isSelected() {
            return false;
         }

         public boolean canSelect() {
            return false;
         }

         public boolean canReport() {
            return this.canSelect();
         }

         @Override
         public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            return this.canSelect();
         }
      }

      @Environment(EnvType.CLIENT)
      record Heading(UUID sender, ChatSelectionScreen.ChatSelectionList.Entry entry) {
         public boolean canCombine(ChatSelectionScreen.ChatSelectionList.Heading other) {
            return other.sender.equals(this.sender);
         }
      }

      @Environment(EnvType.CLIENT)
      public class MessageEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         private static final int CHECKMARK_WIDTH = 9;
         private static final int CHECKMARK_HEIGHT = 8;
         private static final int INDENT_AMOUNT = 11;
         private static final int TAG_MARGIN_LEFT = 4;
         private final int chatId;
         private final FormattedText text;
         private final Component narration;
         @Nullable
         private final List<FormattedCharSequence> hoverText;
         @Nullable
         private final GuiMessageTag.Icon tagIcon;
         @Nullable
         private final List<FormattedCharSequence> tagHoverText;
         private final boolean canReport;
         private final boolean playerMessage;

         public MessageEntry(
            final int chatId,
            final Component text,
            final Component narration,
            @Nullable final GuiMessageTag tagIcon,
            final boolean canReport,
            final boolean playerMessage
         ) {
            this.chatId = chatId;
            this.tagIcon = Optionull.map(tagIcon, GuiMessageTag::icon);
            this.tagHoverText = tagIcon != null && tagIcon.text() != null
               ? ChatSelectionScreen.this.font.split(tagIcon.text(), ChatSelectionList.this.getRowWidth())
               : null;
            this.canReport = canReport;
            this.playerMessage = playerMessage;
            FormattedText formattedText = ChatSelectionScreen.this.font
               .substrByWidth(text, this.getMaximumTextWidth() - ChatSelectionScreen.this.font.width(CommonComponents.ELLIPSIS));
            if (text != formattedText) {
               this.text = FormattedText.composite(formattedText, CommonComponents.ELLIPSIS);
               this.hoverText = ChatSelectionScreen.this.font.split(text, ChatSelectionList.this.getRowWidth());
            } else {
               this.text = text;
               this.hoverText = null;
            }

            this.narration = narration;
         }

         @Override
         public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
            if (this.isSelected() && this.canReport) {
               this.renderSelectedCheckmark(guiGraphics, this.getContentY(), this.getContentX(), this.getContentHeight());
            }

            int i = this.getContentX() + this.getTextIndent();
            int j = this.getContentY() + 1 + (this.getContentHeight() - 9) / 2;
            guiGraphics.drawString(ChatSelectionScreen.this.font, Language.getInstance().getVisualOrder(this.text), i, j, this.canReport ? -1 : -1593835521);
            if (this.hoverText != null && isHovering) {
               guiGraphics.setTooltipForNextFrame(this.hoverText, mouseX, mouseY);
            }

            int k = ChatSelectionScreen.this.font.width(this.text);
            this.renderTag(guiGraphics, i + k + 4, this.getContentY(), this.getContentHeight(), mouseX, mouseY);
         }

         private void renderTag(GuiGraphics guiGraphics, int x, int y, int height, int mouseX, int mouseY) {
            if (this.tagIcon != null) {
               int i = y + (height - this.tagIcon.height) / 2;
               this.tagIcon.draw(guiGraphics, x, i);
               if (this.tagHoverText != null && mouseX >= x && mouseX <= x + this.tagIcon.width && mouseY >= i && mouseY <= i + this.tagIcon.height) {
                  guiGraphics.setTooltipForNextFrame(this.tagHoverText, mouseX, mouseY);
               }
            }
         }

         private void renderSelectedCheckmark(GuiGraphics guiGraphics, int top, int left, int height) {
            int j = top + (height - 8) / 2;
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, ChatSelectionScreen.CHECKMARK_SPRITE, left, j, 9, 8);
         }

         private int getMaximumTextWidth() {
            int i = this.tagIcon != null ? this.tagIcon.width + 4 : 0;
            return ChatSelectionList.this.getRowWidth() - this.getTextIndent() - 4 - i;
         }

         private int getTextIndent() {
            return this.playerMessage ? 11 : 0;
         }

         @Override
         public Component getNarration() {
            return (Component)(this.isSelected() ? Component.translatable("narrator.select", this.narration) : this.narration);
         }

         @Override
         public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            ChatSelectionList.this.setSelected(null);
            return this.toggleReport();
         }

         @Override
         public boolean keyPressed(KeyEvent event) {
            return event.isSelection() ? this.toggleReport() : false;
         }

         @Override
         public boolean isSelected() {
            return ChatSelectionScreen.this.report.isReported(this.chatId);
         }

         @Override
         public boolean canSelect() {
            return true;
         }

         @Override
         public boolean canReport() {
            return this.canReport;
         }

         private boolean toggleReport() {
            if (this.canReport) {
               ChatSelectionScreen.this.report.toggleReported(this.chatId);
               ChatSelectionScreen.this.updateConfirmSelectedButton();
               return true;
            } else {
               return false;
            }
         }
      }

      @Environment(EnvType.CLIENT)
      public class MessageHeadingEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         private static final int FACE_SIZE = 12;
         private static final int PADDING = 4;
         private final Component heading;
         private final Supplier<PlayerSkin> skin;
         private final boolean canReport;

         public MessageHeadingEntry(final GameProfile profile, final Component heading, final boolean canReport) {
            this.heading = heading;
            this.canReport = canReport;
            this.skin = ChatSelectionList.this.minecraft.getSkinManager().createLookup(profile, true);
         }

         @Override
         public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
            int i = this.getContentX() - 12 + 4;
            int j = this.getContentY() + (this.getContentHeight() - 12) / 2;
            PlayerFaceRenderer.draw(guiGraphics, this.skin.get(), i, j, 12);
            int k = this.getContentY() + 1 + (this.getContentHeight() - 9) / 2;
            guiGraphics.drawString(ChatSelectionScreen.this.font, this.heading, i + 12 + 4, k, this.canReport ? -1 : -1593835521);
         }
      }

      @Environment(EnvType.CLIENT)
      public static class PaddingEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         @Override
         public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
         }
      }
   }
}
