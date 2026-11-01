package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.client.RealmsError;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.PreferredRegionsDto;
import com.mojang.realmsclient.dto.RealmsRegion;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.RegionDataDto;
import com.mojang.realmsclient.dto.RegionSelectionPreference;
import com.mojang.realmsclient.dto.RegionSelectionPreferenceDto;
import com.mojang.realmsclient.dto.ServiceQuality;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.CloseServerTask;
import com.mojang.realmsclient.util.task.OpenServerTask;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.LoadingTab;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.StringUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsConfigureWorldScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component PLAY_TEXT = Component.translatable("mco.selectServer.play");
   private final RealmsMainScreen lastScreen;
   @Nullable
   private RealmsServer serverData;
   @Nullable
   private PreferredRegionsDto regions;
   private final Map<RealmsRegion, ServiceQuality> regionServiceQuality = new LinkedHashMap<>();
   private final long serverId;
   private boolean stateChanged;
   private final TabManager tabManager = new TabManager(p_419553_ -> {
      AbstractWidget abstractwidget = this.addRenderableWidget(p_419553_);
   }, p_419490_ -> this.removeWidget(p_419490_), this::onTabSelected, this::onTabDeselected);
   @Nullable
   private Button playButton;
   @Nullable
   private TabNavigationBar tabNavigationBar;
   final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

   public RealmsConfigureWorldScreen(RealmsMainScreen p_427298_, long p_427446_, @Nullable RealmsServer p_427282_, @Nullable PreferredRegionsDto p_427395_) {
      super(Component.empty());
      this.lastScreen = p_427298_;
      this.serverId = p_427446_;
      this.serverData = p_427282_;
      this.regions = p_427395_;
   }

   public RealmsConfigureWorldScreen(RealmsMainScreen p_420068_, long p_419727_) {
      this(p_420068_, p_419727_, null, null);
   }

   @Override
   public void init() {
      if (this.serverData == null) {
         this.fetchServerData(this.serverId);
      }

      if (this.regions == null) {
         this.fetchRegionData();
      }

      Component component = Component.translatable("mco.configure.world.loading");
      this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width)
         .addTabs(
            new LoadingTab(this.getFont(), RealmsWorldsTab.TITLE, component),
            new LoadingTab(this.getFont(), RealmsPlayersTab.TITLE, component),
            new LoadingTab(this.getFont(), RealmsSubscriptionTab.TITLE, component),
            new LoadingTab(this.getFont(), RealmsSettingsTab.TITLE, component)
         )
         .build();
      this.tabNavigationBar.setTabActiveState(3, false);
      this.addRenderableWidget(this.tabNavigationBar);
      LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      this.playButton = linearlayout.addChild(Button.builder(PLAY_TEXT, p_419811_ -> {
         this.onClose();
         RealmsMainScreen.play(this.serverData, this);
      }).width(150).build());
      this.playButton.active = false;
      linearlayout.addChild(Button.builder(CommonComponents.GUI_BACK, p_419967_ -> this.onClose()).build());
      this.layout.visitWidgets(p_419870_ -> {
         p_419870_.setTabOrderGroup(1);
         this.addRenderableWidget(p_419870_);
      });
      this.tabNavigationBar.selectTab(0, false);
      this.repositionElements();
      if (this.serverData != null && this.regions != null) {
         this.onRealmsDataFetched();
      }
   }

   private void onTabSelected(Tab p_419780_) {
      if (this.serverData != null && p_419780_ instanceof RealmsConfigurationTab realmsconfigurationtab) {
         realmsconfigurationtab.onSelected(this.serverData);
      }
   }

   private void onTabDeselected(Tab p_419627_) {
      if (this.serverData != null && p_419627_ instanceof RealmsConfigurationTab realmsconfigurationtab) {
         realmsconfigurationtab.onDeselected(this.serverData);
      }
   }

   public int getContentHeight() {
      return this.layout.getContentHeight();
   }

   public int getHeaderHeight() {
      return this.layout.getHeaderHeight();
   }

   public Screen getLastScreen() {
      return this.lastScreen;
   }

   public Screen createErrorScreen(RealmsServiceException p_428711_) {
      return new RealmsGenericErrorScreen(p_428711_, this.lastScreen);
   }

   @Override
   public void repositionElements() {
      if (this.tabNavigationBar != null) {
         this.tabNavigationBar.setWidth(this.width);
         this.tabNavigationBar.arrangeElements();
         int i = this.tabNavigationBar.getRectangle().bottom();
         ScreenRectangle screenrectangle = new ScreenRectangle(0, i, this.width, this.height - this.layout.getFooterHeight() - i);
         this.tabManager.setTabArea(screenrectangle);
         this.layout.setHeaderHeight(i);
         this.layout.arrangeElements();
      }
   }

   private void updateButtonStates() {
      if (this.serverData != null && this.playButton != null) {
         this.playButton.active = this.serverData.shouldPlayButtonBeActive();
         if (!this.playButton.active && this.serverData.state == RealmsServer.State.CLOSED) {
            this.playButton.setTooltip(Tooltip.create(RealmsServer.WORLD_CLOSED_COMPONENT));
         }
      }
   }

   @Override
   public void render(GuiGraphics p_419682_, int p_419919_, int p_419524_, float p_419765_) {
      super.render(p_419682_, p_419919_, p_419524_, p_419765_);
      p_419682_.blit(
         RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight() - 2, 0.0F, 0.0F, this.width, 2, 32, 2
      );
   }

   @Override
   public boolean keyPressed(KeyEvent p_445575_) {
      return this.tabNavigationBar.keyPressed(p_445575_) ? true : super.keyPressed(p_445575_);
   }

   @Override
   protected void renderMenuBackground(GuiGraphics p_420036_) {
      p_420036_.blit(RenderPipelines.GUI_TEXTURED, CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.layout.getHeaderHeight(), 16, 16);
      this.renderMenuBackground(p_420036_, 0, this.layout.getHeaderHeight(), this.width, this.height);
   }

   @Override
   public void onClose() {
      if (this.serverData != null && this.tabManager.getCurrentTab() instanceof RealmsConfigurationTab realmsconfigurationtab) {
         realmsconfigurationtab.onDeselected(this.serverData);
      }

      this.minecraft.setScreen(this.lastScreen);
      if (this.stateChanged) {
         this.lastScreen.resetScreen();
      }
   }

   public void fetchRegionData() {
      RealmsUtil.supplyAsync(
            RealmsClient::getPreferredRegionSelections, RealmsUtil.openScreenAndLogOnFailure(this::createErrorScreen, "Couldn't get realms region data")
         )
         .thenAcceptAsync(p_426844_ -> {
            this.regions = p_426844_;
            this.onRealmsDataFetched();
         }, this.minecraft);
   }

   public void fetchServerData(long p_419600_) {
      RealmsUtil.<RealmsServer>supplyAsync(
            p_428649_ -> p_428649_.getOwnRealm(p_419600_), RealmsUtil.openScreenAndLogOnFailure(this::createErrorScreen, "Couldn't get own world")
         )
         .thenAcceptAsync(p_419461_ -> {
            this.serverData = p_419461_;
            this.onRealmsDataFetched();
         }, this.minecraft);
   }

   private void onRealmsDataFetched() {
      if (this.serverData != null && this.regions != null) {
         this.regionServiceQuality.clear();

         for (RegionDataDto regiondatadto : this.regions.regionData()) {
            if (regiondatadto.region() != RealmsRegion.INVALID_REGION) {
               this.regionServiceQuality.put(regiondatadto.region(), regiondatadto.serviceQuality());
            }
         }

         int i = -1;
         if (this.tabNavigationBar != null) {
            i = this.tabNavigationBar.getTabs().indexOf(this.tabManager.getCurrentTab());
         }

         if (this.tabNavigationBar != null) {
            this.removeWidget(this.tabNavigationBar);
         }

         this.tabNavigationBar = this.addRenderableWidget(
            TabNavigationBar.builder(this.tabManager, this.width)
               .addTabs(
                  new RealmsWorldsTab(this, Objects.requireNonNull(this.minecraft), this.serverData),
                  new RealmsPlayersTab(this, this.minecraft, this.serverData),
                  new RealmsSubscriptionTab(this, this.minecraft, this.serverData),
                  new RealmsSettingsTab(this, this.minecraft, this.serverData, this.regionServiceQuality)
               )
               .build()
         );
         this.setFocused(this.tabNavigationBar);
         if (i != -1) {
            this.tabNavigationBar.selectTab(i, false);
         }

         this.tabNavigationBar.setTabActiveState(3, !this.serverData.expired);
         if (this.serverData.expired) {
            this.tabNavigationBar.setTabTooltip(3, Tooltip.create(Component.translatable("mco.configure.world.settings.expired")));
         } else {
            this.tabNavigationBar.setTabTooltip(3, null);
         }

         this.updateButtonStates();
         this.repositionElements();
      }
   }

   public void saveSlotSettings(RealmsSlot p_419771_) {
      RealmsSlot realmsslot = this.serverData.slots.get(this.serverData.activeSlot);
      p_419771_.options.templateId = realmsslot.options.templateId;
      p_419771_.options.templateImage = realmsslot.options.templateImage;
      RealmsClient realmsclient = RealmsClient.getOrCreate();

      try {
         if (this.serverData.activeSlot != p_419771_.slotId) {
            throw new RealmsServiceException(RealmsError.CustomError.configurationError());
         }

         realmsclient.updateSlot(this.serverData.id, p_419771_.slotId, p_419771_.options, p_419771_.settings);
         this.serverData.slots.put(this.serverData.activeSlot, p_419771_);
         if (p_419771_.options.gameMode != realmsslot.options.gameMode || p_419771_.isHardcore() != realmsslot.isHardcore()) {
            RealmsMainScreen.refreshServerList();
         }

         this.stateChanged();
      } catch (RealmsServiceException var5) {
         LOGGER.error("Couldn't save slot settings", var5);
         this.minecraft.setScreen(new RealmsGenericErrorScreen(var5, this));
         return;
      }

      this.minecraft.setScreen(this);
   }

   public void saveSettings(String p_420072_, String p_419823_, RegionSelectionPreference p_426282_, @Nullable RealmsRegion p_419580_) {
      String s = StringUtil.isBlank(p_419823_) ? "" : p_419823_;
      String s1 = StringUtil.isBlank(p_420072_) ? "" : p_420072_;
      RealmsClient realmsclient = RealmsClient.getOrCreate();

      try {
         RealmsSlot realmsslot = this.serverData.slots.get(this.serverData.activeSlot);
         RealmsRegion realmsregion = p_426282_ == RegionSelectionPreference.MANUAL ? p_419580_ : null;
         RegionSelectionPreferenceDto regionselectionpreferencedto = new RegionSelectionPreferenceDto(p_426282_, realmsregion);
         realmsclient.updateConfiguration(this.serverData.id, s1, s, regionselectionpreferencedto, realmsslot.slotId, realmsslot.options, realmsslot.settings);
         this.serverData.regionSelectionPreference = regionselectionpreferencedto;
         this.serverData.name = p_420072_;
         this.serverData.motd = s;
         this.stateChanged();
      } catch (RealmsServiceException var11) {
         LOGGER.error("Couldn't save settings", var11);
         this.minecraft.setScreen(new RealmsGenericErrorScreen(var11, this));
         return;
      }

      this.minecraft.setScreen(this);
   }

   public void openTheWorld(boolean p_419983_) {
      RealmsConfigureWorldScreen realmsconfigureworldscreen = this.getNewScreenWithKnownData(this.serverData);
      this.minecraft
         .setScreen(
            new RealmsLongRunningMcoTaskScreen(this.getNewScreen(), new OpenServerTask(this.serverData, realmsconfigureworldscreen, p_419983_, this.minecraft))
         );
   }

   public void closeTheWorld() {
      RealmsConfigureWorldScreen realmsconfigureworldscreen = this.getNewScreenWithKnownData(this.serverData);
      this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.getNewScreen(), new CloseServerTask(this.serverData, realmsconfigureworldscreen)));
   }

   public void stateChanged() {
      this.stateChanged = true;
      if (this.tabNavigationBar != null) {
         for (Tab tab : this.tabNavigationBar.getTabs()) {
            if (tab instanceof RealmsConfigurationTab realmsconfigurationtab) {
               realmsconfigurationtab.updateData(this.serverData);
            }
         }
      }
   }

   public boolean invitePlayer(long p_419576_, String p_419508_) {
      RealmsClient realmsclient = RealmsClient.getOrCreate();

      try {
         List<PlayerInfo> list = realmsclient.invite(p_419576_, p_419508_);
         if (this.serverData != null) {
            this.serverData.players = list;
         } else {
            this.serverData = realmsclient.getOwnRealm(p_419576_);
         }

         this.stateChanged();
         return true;
      } catch (RealmsServiceException var6) {
         LOGGER.error("Couldn't invite user", var6);
         return false;
      }
   }

   public RealmsConfigureWorldScreen getNewScreen() {
      RealmsConfigureWorldScreen realmsconfigureworldscreen = new RealmsConfigureWorldScreen(this.lastScreen, this.serverId);
      realmsconfigureworldscreen.stateChanged = this.stateChanged;
      return realmsconfigureworldscreen;
   }

   public RealmsConfigureWorldScreen getNewScreenWithKnownData(RealmsServer p_427317_) {
      RealmsConfigureWorldScreen realmsconfigureworldscreen = new RealmsConfigureWorldScreen(this.lastScreen, this.serverId, p_427317_, this.regions);
      realmsconfigureworldscreen.stateChanged = this.stateChanged;
      return realmsconfigureworldscreen;
   }
}
