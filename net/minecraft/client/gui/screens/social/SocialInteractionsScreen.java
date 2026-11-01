package net.minecraft.client.gui.screens.social;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonLinks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SocialInteractionsScreen extends Screen {
   private static final Component TITLE = Component.translatable("gui.socialInteractions.title");
   private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("social_interactions/background");
   private static final ResourceLocation SEARCH_SPRITE = ResourceLocation.withDefaultNamespace("icon/search");
   private static final Component TAB_ALL = Component.translatable("gui.socialInteractions.tab_all");
   private static final Component TAB_HIDDEN = Component.translatable("gui.socialInteractions.tab_hidden");
   private static final Component TAB_BLOCKED = Component.translatable("gui.socialInteractions.tab_blocked");
   private static final Component TAB_ALL_SELECTED = TAB_ALL.plainCopy().withStyle(ChatFormatting.UNDERLINE);
   private static final Component TAB_HIDDEN_SELECTED = TAB_HIDDEN.plainCopy().withStyle(ChatFormatting.UNDERLINE);
   private static final Component TAB_BLOCKED_SELECTED = TAB_BLOCKED.plainCopy().withStyle(ChatFormatting.UNDERLINE);
   private static final Component SEARCH_HINT = Component.translatable("gui.socialInteractions.search_hint")
      .withStyle(ChatFormatting.ITALIC)
      .withStyle(ChatFormatting.GRAY);
   static final Component EMPTY_SEARCH = Component.translatable("gui.socialInteractions.search_empty").withStyle(ChatFormatting.GRAY);
   private static final Component EMPTY_HIDDEN = Component.translatable("gui.socialInteractions.empty_hidden").withStyle(ChatFormatting.GRAY);
   private static final Component EMPTY_BLOCKED = Component.translatable("gui.socialInteractions.empty_blocked").withStyle(ChatFormatting.GRAY);
   private static final Component BLOCKING_HINT = Component.translatable("gui.socialInteractions.blocking_hint");
   private static final int BG_BORDER_SIZE = 8;
   private static final int BG_WIDTH = 236;
   private static final int SEARCH_HEIGHT = 16;
   private static final int MARGIN_Y = 64;
   public static final int SEARCH_START = 72;
   public static final int LIST_START = 88;
   private static final int IMAGE_WIDTH = 238;
   private static final int BUTTON_HEIGHT = 20;
   private static final int ITEM_HEIGHT = 36;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   @Nullable
   private final Screen lastScreen;
   SocialInteractionsPlayerList socialInteractionsPlayerList;
   EditBox searchBox;
   private String lastSearch = "";
   private SocialInteractionsScreen.Page page = SocialInteractionsScreen.Page.ALL;
   private Button allButton;
   private Button hiddenButton;
   private Button blockedButton;
   private Button blockingHintButton;
   @Nullable
   private Component serverLabel;
   private int playerCount;

   public SocialInteractionsScreen() {
      this(null);
   }

   public SocialInteractionsScreen(@Nullable Screen p_331950_) {
      super(TITLE);
      this.lastScreen = p_331950_;
      this.updateServerLabel(Minecraft.getInstance());
   }

   private int windowHeight() {
      return Math.max(52, this.height - 128 - 16);
   }

   private int listEnd() {
      return 80 + this.windowHeight() - 8;
   }

   private int marginX() {
      return (this.width - 238) / 2;
   }

   @Override
   public Component getNarrationMessage() {
      return (Component)(this.serverLabel != null
         ? CommonComponents.joinForNarration(super.getNarrationMessage(), this.serverLabel)
         : super.getNarrationMessage());
   }

   @Override
   protected void init() {
      this.layout.addTitleHeader(TITLE, this.font);
      this.socialInteractionsPlayerList = new SocialInteractionsPlayerList(this, this.minecraft, this.width, this.listEnd() - 88, 88, 36);
      int $$0 = this.socialInteractionsPlayerList.getRowWidth() / 3;
      int $$1 = this.socialInteractionsPlayerList.getRowLeft();
      int $$2 = this.socialInteractionsPlayerList.getRowRight();
      this.allButton = this.addRenderableWidget(
         Button.builder(TAB_ALL, p_240243_ -> this.showPage(SocialInteractionsScreen.Page.ALL)).bounds($$1, 45, $$0, 20).build()
      );
      this.hiddenButton = this.addRenderableWidget(
         Button.builder(TAB_HIDDEN, p_100791_ -> this.showPage(SocialInteractionsScreen.Page.HIDDEN)).bounds(($$1 + $$2 - $$0) / 2 + 1, 45, $$0, 20).build()
      );
      this.blockedButton = this.addRenderableWidget(
         Button.builder(TAB_BLOCKED, p_100785_ -> this.showPage(SocialInteractionsScreen.Page.BLOCKED)).bounds($$2 - $$0 + 1, 45, $$0, 20).build()
      );
      String $$3 = this.searchBox != null ? this.searchBox.getValue() : "";
      this.searchBox = new EditBox(this.font, this.marginX() + 28, 74, 200, 15, SEARCH_HINT) {
         @Override
         protected MutableComponent createNarrationMessage() {
            return !SocialInteractionsScreen.this.searchBox.getValue().isEmpty() && SocialInteractionsScreen.this.socialInteractionsPlayerList.isEmpty()
               ? super.createNarrationMessage().append(", ").append(SocialInteractionsScreen.EMPTY_SEARCH)
               : super.createNarrationMessage();
         }
      };
      this.searchBox.setMaxLength(16);
      this.searchBox.setVisible(true);
      this.searchBox.setTextColor(-1);
      this.searchBox.setValue($$3);
      this.searchBox.setHint(SEARCH_HINT);
      this.searchBox.setResponder(this::checkSearchStringUpdate);
      this.addRenderableWidget(this.searchBox);
      this.addWidget(this.socialInteractionsPlayerList);
      this.blockingHintButton = this.addRenderableWidget(
         Button.builder(BLOCKING_HINT, ConfirmLinkScreen.confirmLink(this, CommonLinks.BLOCKING_HELP))
            .bounds(this.width / 2 - 100, 64 + this.windowHeight(), 200, 20)
            .build()
      );
      this.showPage(this.page);
      this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, p_329743_ -> this.onClose()).width(200).build());
      this.layout.visitWidgets(p_329745_ -> {
         AbstractWidget var10000 = this.addRenderableWidget(p_329745_);
      });
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      this.socialInteractionsPlayerList.updateSizeAndPosition(this.width, this.listEnd() - 88, 88);
      this.searchBox.setPosition(this.marginX() + 28, 74);
      int $$0 = this.socialInteractionsPlayerList.getRowLeft();
      int $$1 = this.socialInteractionsPlayerList.getRowRight();
      int $$2 = this.socialInteractionsPlayerList.getRowWidth() / 3;
      this.allButton.setPosition($$0, 45);
      this.hiddenButton.setPosition(($$0 + $$1 - $$2) / 2 + 1, 45);
      this.blockedButton.setPosition($$1 - $$2 + 1, 45);
      this.blockingHintButton.setPosition(this.width / 2 - 100, 64 + this.windowHeight());
   }

   @Override
   protected void setInitialFocus() {
      this.setInitialFocus(this.searchBox);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   private void showPage(SocialInteractionsScreen.Page p_100772_) {
      this.page = p_100772_;
      this.allButton.setMessage(TAB_ALL);
      this.hiddenButton.setMessage(TAB_HIDDEN);
      this.blockedButton.setMessage(TAB_BLOCKED);
      boolean $$1 = false;
      switch (p_100772_) {
         case ALL:
            this.allButton.setMessage(TAB_ALL_SELECTED);
            Collection<UUID> $$2 = this.minecraft.player.connection.getOnlinePlayerIds();
            this.socialInteractionsPlayerList.updatePlayerList($$2, this.socialInteractionsPlayerList.getScrollAmount(), true);
            break;
         case HIDDEN:
            this.hiddenButton.setMessage(TAB_HIDDEN_SELECTED);
            Set<UUID> $$3 = this.minecraft.getPlayerSocialManager().getHiddenPlayers();
            $$1 = $$3.isEmpty();
            this.socialInteractionsPlayerList.updatePlayerList($$3, this.socialInteractionsPlayerList.getScrollAmount(), false);
            break;
         case BLOCKED:
            this.blockedButton.setMessage(TAB_BLOCKED_SELECTED);
            PlayerSocialManager $$4 = this.minecraft.getPlayerSocialManager();
            Set<UUID> $$5 = this.minecraft.player.connection.getOnlinePlayerIds().stream().filter($$4::isBlocked).collect(Collectors.toSet());
            $$1 = $$5.isEmpty();
            this.socialInteractionsPlayerList.updatePlayerList($$5, this.socialInteractionsPlayerList.getScrollAmount(), false);
      }

      GameNarrator $$6 = this.minecraft.getNarrator();
      if (!this.searchBox.getValue().isEmpty() && this.socialInteractionsPlayerList.isEmpty() && !this.searchBox.isFocused()) {
         $$6.sayNow(EMPTY_SEARCH);
      } else if ($$1) {
         if (p_100772_ == SocialInteractionsScreen.Page.HIDDEN) {
            $$6.sayNow(EMPTY_HIDDEN);
         } else if (p_100772_ == SocialInteractionsScreen.Page.BLOCKED) {
            $$6.sayNow(EMPTY_BLOCKED);
         }
      }
   }

   @Override
   public void renderBackground(GuiGraphics p_283202_, int p_296258_, int p_295067_, float p_295331_) {
      super.renderBackground(p_283202_, p_296258_, p_295067_, p_295331_);
      int $$4 = this.marginX() + 3;
      p_283202_.blitSprite(BACKGROUND_SPRITE, $$4, 64, 236, this.windowHeight() + 16);
      p_283202_.blitSprite(SEARCH_SPRITE, $$4 + 10, 76, 12, 12);
   }

   @Override
   public void render(GuiGraphics p_282516_, int p_100764_, int p_100765_, float p_100766_) {
      super.render(p_282516_, p_100764_, p_100765_, p_100766_);
      this.updateServerLabel(this.minecraft);
      if (this.serverLabel != null) {
         p_282516_.drawString(this.minecraft.font, this.serverLabel, this.marginX() + 8, 35, -1);
      }

      if (!this.socialInteractionsPlayerList.isEmpty()) {
         this.socialInteractionsPlayerList.render(p_282516_, p_100764_, p_100765_, p_100766_);
      } else if (!this.searchBox.getValue().isEmpty()) {
         p_282516_.drawCenteredString(this.minecraft.font, EMPTY_SEARCH, this.width / 2, (72 + this.listEnd()) / 2, -1);
      } else if (this.page == SocialInteractionsScreen.Page.HIDDEN) {
         p_282516_.drawCenteredString(this.minecraft.font, EMPTY_HIDDEN, this.width / 2, (72 + this.listEnd()) / 2, -1);
      } else if (this.page == SocialInteractionsScreen.Page.BLOCKED) {
         p_282516_.drawCenteredString(this.minecraft.font, EMPTY_BLOCKED, this.width / 2, (72 + this.listEnd()) / 2, -1);
      }

      this.blockingHintButton.visible = this.page == SocialInteractionsScreen.Page.BLOCKED;
   }

   @Override
   public boolean keyPressed(int p_100757_, int p_100758_, int p_100759_) {
      if (!this.searchBox.isFocused() && this.minecraft.options.keySocialInteractions.matches(p_100757_, p_100758_)) {
         this.onClose();
         return true;
      } else {
         return super.keyPressed(p_100757_, p_100758_, p_100759_);
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private void checkSearchStringUpdate(String p_100789_) {
      p_100789_ = p_100789_.toLowerCase(Locale.ROOT);
      if (!p_100789_.equals(this.lastSearch)) {
         this.socialInteractionsPlayerList.setFilter(p_100789_);
         this.lastSearch = p_100789_;
         this.showPage(this.page);
      }
   }

   private void updateServerLabel(Minecraft p_100768_) {
      int $$1 = p_100768_.getConnection().getOnlinePlayers().size();
      if (this.playerCount != $$1) {
         String $$2 = "";
         ServerData $$3 = p_100768_.getCurrentServer();
         if (p_100768_.isLocalServer()) {
            $$2 = p_100768_.getSingleplayerServer().getMotd();
         } else if ($$3 != null) {
            $$2 = $$3.name;
         }

         if ($$1 > 1) {
            this.serverLabel = Component.translatable("gui.socialInteractions.server_label.multiple", $$2, $$1);
         } else {
            this.serverLabel = Component.translatable("gui.socialInteractions.server_label.single", $$2, $$1);
         }

         this.playerCount = $$1;
      }
   }

   public void onAddPlayer(PlayerInfo p_100776_) {
      this.socialInteractionsPlayerList.addPlayer(p_100776_, this.page);
   }

   public void onRemovePlayer(UUID p_100780_) {
      this.socialInteractionsPlayerList.removePlayer(p_100780_);
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Page {
      ALL,
      HIDDEN,
      BLOCKED;
   }
}
