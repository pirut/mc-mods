package com.mojang.realmsclient;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.Ping;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.PingResult;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerPlayerLists;
import com.mojang.realmsclient.dto.RegionPingResult;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.RealmsServerList;
import com.mojang.realmsclient.gui.screens.AddRealmPopupScreen;
import com.mojang.realmsclient.gui.screens.RealmsCreateRealmScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPendingInvitesScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.task.DataFetcher;
import com.mojang.realmsclient.util.RealmsPersistence;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientActivePlayersTooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsMainScreen extends RealmsScreen {
   static final Identifier INFO_SPRITE = Identifier.withDefaultNamespace("icon/info");
   static final Identifier NEW_REALM_SPRITE = Identifier.withDefaultNamespace("icon/new_realm");
   static final Identifier EXPIRED_SPRITE = Identifier.withDefaultNamespace("realm_status/expired");
   static final Identifier EXPIRES_SOON_SPRITE = Identifier.withDefaultNamespace("realm_status/expires_soon");
   static final Identifier OPEN_SPRITE = Identifier.withDefaultNamespace("realm_status/open");
   static final Identifier CLOSED_SPRITE = Identifier.withDefaultNamespace("realm_status/closed");
   private static final Identifier INVITE_SPRITE = Identifier.withDefaultNamespace("icon/invite");
   private static final Identifier NEWS_SPRITE = Identifier.withDefaultNamespace("icon/news");
   public static final Identifier HARDCORE_MODE_SPRITE = Identifier.withDefaultNamespace("hud/heart/hardcore_full");
   static final Logger LOGGER = LogUtils.getLogger();
   private static final Identifier NO_REALMS_LOCATION = Identifier.withDefaultNamespace("textures/gui/realms/no_realms.png");
   private static final Component TITLE = Component.translatable("menu.online");
   private static final Component LOADING_TEXT = Component.translatable("mco.selectServer.loading");
   static final Component SERVER_UNITIALIZED_TEXT = Component.translatable("mco.selectServer.uninitialized");
   static final Component SUBSCRIPTION_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredList");
   private static final Component SUBSCRIPTION_RENEW_TEXT = Component.translatable("mco.selectServer.expiredRenew");
   static final Component TRIAL_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredTrial");
   private static final Component PLAY_TEXT = Component.translatable("mco.selectServer.play");
   private static final Component LEAVE_SERVER_TEXT = Component.translatable("mco.selectServer.leave");
   private static final Component CONFIGURE_SERVER_TEXT = Component.translatable("mco.selectServer.configure");
   static final Component SERVER_EXPIRED_TOOLTIP = Component.translatable("mco.selectServer.expired");
   static final Component SERVER_EXPIRES_SOON_TOOLTIP = Component.translatable("mco.selectServer.expires.soon");
   static final Component SERVER_EXPIRES_IN_DAY_TOOLTIP = Component.translatable("mco.selectServer.expires.day");
   static final Component SERVER_OPEN_TOOLTIP = Component.translatable("mco.selectServer.open");
   static final Component SERVER_CLOSED_TOOLTIP = Component.translatable("mco.selectServer.closed");
   static final Component UNITIALIZED_WORLD_NARRATION = Component.translatable("gui.narrate.button", SERVER_UNITIALIZED_TEXT);
   private static final Component NO_REALMS_TEXT = Component.translatable("mco.selectServer.noRealms");
   private static final Component NO_PENDING_INVITES = Component.translatable("mco.invites.nopending");
   private static final Component PENDING_INVITES = Component.translatable("mco.invites.pending");
   private static final Component INCOMPATIBLE_POPUP_TITLE = Component.translatable("mco.compatibility.incompatible.popup.title");
   private static final Component INCOMPATIBLE_RELEASE_TYPE_POPUP_MESSAGE = Component.translatable("mco.compatibility.incompatible.releaseType.popup.message");
   private static final int BUTTON_WIDTH = 100;
   private static final int BUTTON_COLUMNS = 3;
   private static final int BUTTON_SPACING = 4;
   private static final int CONTENT_WIDTH = 308;
   private static final int LOGO_PADDING = 5;
   private static final int HEADER_HEIGHT = 44;
   private static final int FOOTER_PADDING = 11;
   private static final int NEW_REALM_SPRITE_WIDTH = 40;
   private static final int NEW_REALM_SPRITE_HEIGHT = 20;
   private static final boolean SNAPSHOT = !SharedConstants.getCurrentVersion().stable();
   private static boolean snapshotToggle = SNAPSHOT;
   private final CompletableFuture<RealmsAvailability.Result> availability = RealmsAvailability.get();
   private DataFetcher.Subscription dataSubscription;
   private final Set<UUID> handledSeenNotifications = new HashSet<>();
   private static boolean regionsPinged;
   private final RateLimiter inviteNarrationLimiter;
   private final Screen lastScreen;
   private Button playButton;
   private Button backButton;
   private Button renewButton;
   private Button configureButton;
   private Button leaveButton;
   RealmsMainScreen.RealmSelectionList realmSelectionList;
   RealmsServerList serverList;
   List<RealmsServer> availableSnapshotServers = List.of();
   RealmsServerPlayerLists onlinePlayersPerRealm = new RealmsServerPlayerLists(Map.of());
   private volatile boolean trialsAvailable;
   @Nullable
   private volatile String newsLink;
   final List<RealmsNotification> notifications = new ArrayList<>();
   private Button addRealmButton;
   private RealmsMainScreen.NotificationButton pendingInvitesButton;
   private RealmsMainScreen.NotificationButton newsButton;
   private RealmsMainScreen.LayoutState activeLayoutState;
   @Nullable
   private HeaderAndFooterLayout layout;

   public RealmsMainScreen(Screen p_86315_) {
      super(TITLE);
      this.lastScreen = p_86315_;
      this.inviteNarrationLimiter = RateLimiter.create(0.016666668F);
   }

   @Override
   public void init() {
      this.serverList = new RealmsServerList(this.minecraft);
      this.realmSelectionList = new RealmsMainScreen.RealmSelectionList();
      Component component = Component.translatable("mco.invites.title");
      this.pendingInvitesButton = new RealmsMainScreen.NotificationButton(
         component, INVITE_SPRITE, p_293547_ -> this.minecraft.setScreen(new RealmsPendingInvitesScreen(this, component)), null
      );
      Component component1 = Component.translatable("mco.news");
      this.newsButton = new RealmsMainScreen.NotificationButton(component1, NEWS_SPRITE, p_307015_ -> {
         String s = this.newsLink;
         if (s != null) {
            ConfirmLinkScreen.confirmLinkNow(this, s);
            if (this.newsButton.notificationCount() != 0) {
               RealmsPersistence.RealmsPersistenceData realmspersistence$realmspersistencedata = RealmsPersistence.readFile();
               realmspersistence$realmspersistencedata.hasUnreadNews = false;
               RealmsPersistence.writeFile(realmspersistence$realmspersistencedata);
               this.newsButton.setNotificationCount(0);
            }
         }
      }, component1);
      this.playButton = Button.builder(PLAY_TEXT, p_302303_ -> play(this.getSelectedServer(), this)).width(100).build();
      this.configureButton = Button.builder(CONFIGURE_SERVER_TEXT, p_86672_ -> this.configureClicked(this.getSelectedServer())).width(100).build();
      this.renewButton = Button.builder(SUBSCRIPTION_RENEW_TEXT, p_86622_ -> this.onRenew(this.getSelectedServer())).width(100).build();
      this.leaveButton = Button.builder(LEAVE_SERVER_TEXT, p_86679_ -> this.leaveClicked(this.getSelectedServer())).width(100).build();
      this.addRealmButton = Button.builder(Component.translatable("mco.selectServer.purchase"), p_300620_ -> this.openTrialAvailablePopup())
         .size(100, 20)
         .build();
      this.backButton = Button.builder(CommonComponents.GUI_BACK, p_315807_ -> this.onClose()).width(100).build();
      if (RealmsClient.ENVIRONMENT == RealmsClient.Environment.STAGE) {
         this.addRenderableWidget(
            CycleButton.booleanBuilder(Component.literal("Snapshot"), Component.literal("Release"), snapshotToggle)
               .create(5, 5, 100, 20, Component.literal("Realm"), (p_305606_, p_305607_) -> {
                  snapshotToggle = p_305607_;
                  this.availableSnapshotServers = List.of();
                  this.debugRefreshDataFetchers();
               })
         );
      }

      this.updateLayout(RealmsMainScreen.LayoutState.LOADING);
      this.updateButtonStates();
      this.availability.thenAcceptAsync(p_293549_ -> {
         Screen screen = p_293549_.createErrorScreen(this.lastScreen);
         if (screen == null) {
            this.dataSubscription = this.initDataFetcher(this.minecraft.realmsDataFetcher());
         } else {
            this.minecraft.setScreen(screen);
         }
      }, this.screenExecutor);
   }

   public static boolean isSnapshot() {
      return SNAPSHOT && snapshotToggle;
   }

   @Override
   protected void repositionElements() {
      if (this.layout != null) {
         this.realmSelectionList.updateSize(this.width, this.layout);
         this.layout.arrangeElements();
      }
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   private void updateLayout() {
      if (this.serverList.isEmpty() && this.availableSnapshotServers.isEmpty() && this.notifications.isEmpty()) {
         this.updateLayout(RealmsMainScreen.LayoutState.NO_REALMS);
      } else {
         this.updateLayout(RealmsMainScreen.LayoutState.LIST);
      }
   }

   private void updateLayout(RealmsMainScreen.LayoutState p_294474_) {
      if (this.activeLayoutState != p_294474_) {
         if (this.layout != null) {
            this.layout.visitWidgets(p_321332_ -> this.removeWidget(p_321332_));
         }

         this.layout = this.createLayout(p_294474_);
         this.activeLayoutState = p_294474_;
         this.layout.visitWidgets(p_321334_ -> {
            AbstractWidget abstractwidget = this.addRenderableWidget(p_321334_);
         });
         this.repositionElements();
      }
   }

   private HeaderAndFooterLayout createLayout(RealmsMainScreen.LayoutState p_295052_) {
      HeaderAndFooterLayout headerandfooterlayout = new HeaderAndFooterLayout(this);
      headerandfooterlayout.setHeaderHeight(44);
      headerandfooterlayout.addToHeader(this.createHeader());
      Layout layout = this.createFooter(p_295052_);
      layout.arrangeElements();
      headerandfooterlayout.setFooterHeight(layout.getHeight() + 22);
      headerandfooterlayout.addToFooter(layout);
      switch (p_295052_) {
         case LOADING:
            headerandfooterlayout.addToContents(new LoadingDotsWidget(this.font, LOADING_TEXT));
            break;
         case NO_REALMS:
            headerandfooterlayout.addToContents(this.createNoRealmsContent());
            break;
         case LIST:
            headerandfooterlayout.addToContents(this.realmSelectionList);
      }

      return headerandfooterlayout;
   }

   private Layout createHeader() {
      int i = 90;
      LinearLayout linearlayout = LinearLayout.horizontal().spacing(4);
      linearlayout.defaultCellSetting().alignVerticallyMiddle();
      linearlayout.addChild(this.pendingInvitesButton);
      linearlayout.addChild(this.newsButton);
      LinearLayout linearlayout1 = LinearLayout.horizontal();
      linearlayout1.defaultCellSetting().alignVerticallyMiddle();
      linearlayout1.addChild(SpacerElement.width(90));
      linearlayout1.addChild(realmsLogo(), LayoutSettings::alignHorizontallyCenter);
      linearlayout1.addChild(new FrameLayout(90, 44)).addChild(linearlayout, LayoutSettings::alignHorizontallyRight);
      return linearlayout1;
   }

   private Layout createFooter(RealmsMainScreen.LayoutState p_294561_) {
      GridLayout gridlayout = new GridLayout().spacing(4);
      GridLayout.RowHelper gridlayout$rowhelper = gridlayout.createRowHelper(3);
      if (p_294561_ == RealmsMainScreen.LayoutState.LIST) {
         gridlayout$rowhelper.addChild(this.playButton);
         gridlayout$rowhelper.addChild(this.configureButton);
         gridlayout$rowhelper.addChild(this.renewButton);
         gridlayout$rowhelper.addChild(this.leaveButton);
      }

      gridlayout$rowhelper.addChild(this.addRealmButton);
      gridlayout$rowhelper.addChild(this.backButton);
      return gridlayout;
   }

   private LinearLayout createNoRealmsContent() {
      LinearLayout linearlayout = LinearLayout.vertical().spacing(8);
      linearlayout.defaultCellSetting().alignHorizontallyCenter();
      linearlayout.addChild(ImageWidget.texture(130, 64, NO_REALMS_LOCATION, 130, 64));
      linearlayout.addChild(
         FocusableTextWidget.builder(NO_REALMS_TEXT, this.font)
            .maxWidth(308)
            .alwaysShowBorder(false)
            .backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS)
            .build()
      );
      return linearlayout;
   }

   void updateButtonStates() {
      RealmsServer realmsserver = this.getSelectedServer();
      boolean flag = realmsserver != null;
      this.addRealmButton.active = this.activeLayoutState != RealmsMainScreen.LayoutState.LOADING;
      this.playButton.active = flag && realmsserver.shouldPlayButtonBeActive();
      if (!this.playButton.active && flag && realmsserver.state == RealmsServer.State.CLOSED) {
         this.playButton.setTooltip(Tooltip.create(RealmsServer.WORLD_CLOSED_COMPONENT));
      }

      this.renewButton.active = flag && this.shouldRenewButtonBeActive(realmsserver);
      this.leaveButton.active = flag && this.shouldLeaveButtonBeActive(realmsserver);
      this.configureButton.active = flag && this.shouldConfigureButtonBeActive(realmsserver);
   }

   private boolean shouldRenewButtonBeActive(RealmsServer p_86595_) {
      return p_86595_.expired && isSelfOwnedServer(p_86595_);
   }

   private boolean shouldConfigureButtonBeActive(RealmsServer p_86620_) {
      return isSelfOwnedServer(p_86620_) && p_86620_.state != RealmsServer.State.UNINITIALIZED;
   }

   private boolean shouldLeaveButtonBeActive(RealmsServer p_86645_) {
      return !isSelfOwnedServer(p_86645_);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.dataSubscription != null) {
         this.dataSubscription.tick();
      }
   }

   public static void refreshPendingInvites() {
      Minecraft.getInstance().realmsDataFetcher().pendingInvitesTask.reset();
   }

   public static void refreshServerList() {
      Minecraft.getInstance().realmsDataFetcher().serverListUpdateTask.reset();
   }

   private void debugRefreshDataFetchers() {
      for (DataFetcher.Task<?> task : this.minecraft.realmsDataFetcher().getTasks()) {
         task.reset();
      }
   }

   private DataFetcher.Subscription initDataFetcher(RealmsDataFetcher p_238836_) {
      DataFetcher.Subscription datafetcher$subscription = p_238836_.dataFetcher.createSubscription();
      datafetcher$subscription.subscribe(p_238836_.serverListUpdateTask, p_305616_ -> {
         this.serverList.updateServersList(p_305616_.serverList());
         this.availableSnapshotServers = p_305616_.availableSnapshotServers();
         this.refreshListAndLayout();
         boolean flag = false;

         for (RealmsServer realmsserver : this.serverList) {
            if (this.isSelfOwnedNonExpiredServer(realmsserver)) {
               flag = true;
            }
         }

         if (!regionsPinged && flag) {
            regionsPinged = true;
            this.pingRegions();
         }
      });
      callRealmsClient(RealmsClient::getNotifications, p_304053_ -> {
         this.notifications.clear();
         this.notifications.addAll(p_304053_);

         for (RealmsNotification realmsnotification : p_304053_) {
            if (realmsnotification instanceof RealmsNotification.InfoPopup realmsnotification$infopopup) {
               PopupScreen popupscreen = realmsnotification$infopopup.buildScreen(this, this::dismissNotification);
               if (popupscreen != null) {
                  this.minecraft.setScreen(popupscreen);
                  this.markNotificationsAsSeen(List.of(realmsnotification));
                  break;
               }
            }
         }

         if (!this.notifications.isEmpty() && this.activeLayoutState != RealmsMainScreen.LayoutState.LOADING) {
            this.refreshListAndLayout();
         }
      });
      datafetcher$subscription.subscribe(p_238836_.pendingInvitesTask, p_300619_ -> {
         this.pendingInvitesButton.setNotificationCount(p_300619_);
         this.pendingInvitesButton.setTooltip(p_300619_ == 0 ? Tooltip.create(NO_PENDING_INVITES) : Tooltip.create(PENDING_INVITES));
         if (p_300619_ > 0 && this.inviteNarrationLimiter.tryAcquire(1)) {
            this.minecraft.getNarrator().saySystemNow(Component.translatable("mco.configure.world.invite.narration", p_300619_));
         }
      });
      datafetcher$subscription.subscribe(p_238836_.trialAvailabilityTask, p_293548_ -> this.trialsAvailable = p_293548_);
      datafetcher$subscription.subscribe(p_238836_.onlinePlayersTask, p_349772_ -> this.onlinePlayersPerRealm = p_349772_);
      datafetcher$subscription.subscribe(p_238836_.newsTask, p_300622_ -> {
         p_238836_.newsManager.updateUnreadNews(p_300622_);
         this.newsLink = p_238836_.newsManager.newsLink();
         this.newsButton.setNotificationCount(p_238836_.newsManager.hasUnreadNews() ? Integer.MAX_VALUE : 0);
      });
      return datafetcher$subscription;
   }

   void markNotificationsAsSeen(Collection<RealmsNotification> p_304698_) {
      List<UUID> list = new ArrayList<>(p_304698_.size());

      for (RealmsNotification realmsnotification : p_304698_) {
         if (!realmsnotification.seen() && !this.handledSeenNotifications.contains(realmsnotification.uuid())) {
            list.add(realmsnotification.uuid());
         }
      }

      if (!list.isEmpty()) {
         callRealmsClient(p_274625_ -> {
            p_274625_.notificationsSeen(list);
            return null;
         }, p_274630_ -> this.handledSeenNotifications.addAll(list));
      }
   }

   private static <T> void callRealmsClient(RealmsMainScreen.RealmsCall<T> p_275561_, Consumer<T> p_275686_) {
      Minecraft minecraft = Minecraft.getInstance();
      CompletableFuture.<T>supplyAsync(() -> {
         try {
            return p_275561_.request(RealmsClient.getOrCreate(minecraft));
         } catch (RealmsServiceException var3) {
            throw new RuntimeException(var3);
         }
      }).thenAcceptAsync(p_275686_, minecraft).exceptionally(p_274626_ -> {
         LOGGER.error("Failed to execute call to Realms Service", p_274626_);
         return null;
      });
   }

   private void refreshListAndLayout() {
      this.realmSelectionList.refreshEntries(this);
      this.updateLayout();
      this.updateButtonStates();
   }

   private void pingRegions() {
      new Thread(() -> {
         List<RegionPingResult> list = Ping.pingAllRegions();
         RealmsClient realmsclient = RealmsClient.getOrCreate();
         PingResult pingresult = new PingResult(list, this.getOwnedNonExpiredRealmIds());

         try {
            realmsclient.sendPingResults(pingresult);
         } catch (Throwable var5) {
            LOGGER.warn("Could not send ping result to Realms: ", var5);
         }
      }).start();
   }

   private List<Long> getOwnedNonExpiredRealmIds() {
      List<Long> list = Lists.newArrayList();

      for (RealmsServer realmsserver : this.serverList) {
         if (this.isSelfOwnedNonExpiredServer(realmsserver)) {
            list.add(realmsserver.id);
         }
      }

      return list;
   }

   private void onRenew(@Nullable RealmsServer p_193500_) {
      if (p_193500_ != null) {
         String s = CommonLinks.extendRealms(p_193500_.remoteSubscriptionId, this.minecraft.getUser().getProfileId(), p_193500_.expiredTrial);
         this.minecraft.setScreen(new ConfirmLinkScreen(p_465311_ -> {
            if (p_465311_) {
               Util.getPlatform().openUri(s);
            } else {
               this.minecraft.setScreen(this);
            }
         }, s, true));
      }
   }

   private void configureClicked(@Nullable RealmsServer p_86657_) {
      if (p_86657_ != null && this.minecraft.isLocalPlayer(p_86657_.ownerUUID)) {
         this.minecraft.setScreen(new RealmsConfigureWorldScreen(this, p_86657_.id));
      }
   }

   private void leaveClicked(@Nullable RealmsServer p_86670_) {
      if (p_86670_ != null && !this.minecraft.isLocalPlayer(p_86670_.ownerUUID)) {
         Component component = Component.translatable("mco.configure.world.leave.question.line1");
         this.minecraft.setScreen(RealmsPopups.infoPopupScreen(this, component, p_344113_ -> this.leaveServer(p_86670_)));
      }
   }

   @Nullable
   private RealmsServer getSelectedServer() {
      return this.realmSelectionList.getSelected() instanceof RealmsMainScreen.ServerEntry realmsmainscreen$serverentry
         ? realmsmainscreen$serverentry.getServer()
         : null;
   }

   private void leaveServer(final RealmsServer p_193495_) {
      (new Thread("Realms-leave-server") {
            @Override
            public void run() {
               try {
                  RealmsClient realmsclient = RealmsClient.getOrCreate();
                  realmsclient.uninviteMyselfFrom(p_193495_.id);
                  RealmsMainScreen.this.minecraft.execute(RealmsMainScreen::refreshServerList);
               } catch (RealmsServiceException var2) {
                  RealmsMainScreen.LOGGER.error("Couldn't configure world", var2);
                  RealmsMainScreen.this.minecraft
                     .execute(() -> RealmsMainScreen.this.minecraft.setScreen(new RealmsGenericErrorScreen(var2, RealmsMainScreen.this)));
               }
            }
         })
         .start();
      this.minecraft.setScreen(this);
   }

   void dismissNotification(UUID p_275349_) {
      callRealmsClient(p_274628_ -> {
         p_274628_.notificationsDismiss(List.of(p_275349_));
         return null;
      }, p_305610_ -> {
         this.notifications.removeIf(p_274621_ -> p_274621_.dismissable() && p_275349_.equals(p_274621_.uuid()));
         this.refreshListAndLayout();
      });
   }

   public void resetScreen() {
      this.realmSelectionList.setSelected(null);
      refreshServerList();
   }

   @Override
   public Component getNarrationMessage() {
      return (Component)(switch (this.activeLayoutState) {
         case LOADING -> CommonComponents.joinForNarration(super.getNarrationMessage(), LOADING_TEXT);
         case NO_REALMS -> CommonComponents.joinForNarration(super.getNarrationMessage(), NO_REALMS_TEXT);
         case LIST -> super.getNarrationMessage();
      });
   }

   @Override
   public void render(GuiGraphics p_282736_, int p_283347_, int p_282480_, float p_283485_) {
      super.render(p_282736_, p_283347_, p_282480_, p_283485_);
      if (isSnapshot()) {
         p_282736_.drawString(this.font, "Minecraft " + SharedConstants.getCurrentVersion().name(), 2, this.height - 10, -1);
      }

      if (this.trialsAvailable && this.addRealmButton.active) {
         AddRealmPopupScreen.renderDiamond(p_282736_, this.addRealmButton);
      }

      switch (RealmsClient.ENVIRONMENT) {
         case STAGE:
            this.renderEnvironment(p_282736_, "STAGE!", -256);
            break;
         case LOCAL:
            this.renderEnvironment(p_282736_, "LOCAL!", -8388737);
      }
   }

   private void openTrialAvailablePopup() {
      this.minecraft.setScreen(new AddRealmPopupScreen(this, this.trialsAvailable));
   }

   public static void play(@Nullable RealmsServer p_86516_, Screen p_86517_) {
      play(p_86516_, p_86517_, false);
   }

   public static void play(@Nullable RealmsServer p_305964_, Screen p_305959_, boolean p_306296_) {
      if (p_305964_ != null) {
         if (!isSnapshot() || p_306296_ || p_305964_.isMinigameActive()) {
            Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(p_305959_, new GetServerDetailsTask(p_305959_, p_305964_)));
            return;
         }

         switch (p_305964_.compatibility) {
            case COMPATIBLE:
               Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(p_305959_, new GetServerDetailsTask(p_305959_, p_305964_)));
               break;
            case UNVERIFIABLE:
               confirmToPlay(
                  p_305964_,
                  p_305959_,
                  Component.translatable("mco.compatibility.unverifiable.title").withColor(-171),
                  Component.translatable("mco.compatibility.unverifiable.message"),
                  CommonComponents.GUI_CONTINUE
               );
               break;
            case NEEDS_DOWNGRADE:
               confirmToPlay(
                  p_305964_,
                  p_305959_,
                  Component.translatable("selectWorld.backupQuestion.downgrade").withColor(-2142128),
                  Component.translatable(
                     "mco.compatibility.downgrade.description",
                     Component.literal(p_305964_.activeVersion).withColor(-171),
                     Component.literal(SharedConstants.getCurrentVersion().name()).withColor(-171)
                  ),
                  Component.translatable("mco.compatibility.downgrade")
               );
               break;
            case NEEDS_UPGRADE:
               upgradeRealmAndPlay(p_305964_, p_305959_);
               break;
            case INCOMPATIBLE:
               Minecraft.getInstance()
                  .setScreen(
                     new PopupScreen.Builder(p_305959_, INCOMPATIBLE_POPUP_TITLE)
                        .setMessage(
                           Component.translatable(
                              "mco.compatibility.incompatible.series.popup.message",
                              Component.literal(p_305964_.activeVersion).withColor(-171),
                              Component.literal(SharedConstants.getCurrentVersion().name()).withColor(-171)
                           )
                        )
                        .addButton(CommonComponents.GUI_BACK, PopupScreen::onClose)
                        .build()
                  );
               break;
            case RELEASE_TYPE_INCOMPATIBLE:
               Minecraft.getInstance()
                  .setScreen(
                     new PopupScreen.Builder(p_305959_, INCOMPATIBLE_POPUP_TITLE)
                        .setMessage(INCOMPATIBLE_RELEASE_TYPE_POPUP_MESSAGE)
                        .addButton(CommonComponents.GUI_BACK, PopupScreen::onClose)
                        .build()
                  );
         }
      }
   }

   private static void confirmToPlay(RealmsServer p_305792_, Screen p_306263_, Component p_305782_, Component p_306311_, Component p_306238_) {
      Minecraft.getInstance().setScreen(new PopupScreen.Builder(p_306263_, p_305782_).setMessage(p_306311_).addButton(p_306238_, p_349775_ -> {
         Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(p_306263_, new GetServerDetailsTask(p_306263_, p_305792_)));
         refreshServerList();
      }).addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose).build());
   }

   private static void upgradeRealmAndPlay(RealmsServer p_350306_, Screen p_351001_) {
      Component component = Component.translatable("mco.compatibility.upgrade.title").withColor(-171);
      Component component1 = Component.translatable("mco.compatibility.upgrade");
      Component component2 = Component.literal(p_350306_.activeVersion).withColor(-171);
      Component component3 = Component.literal(SharedConstants.getCurrentVersion().name()).withColor(-171);
      Component component4 = isSelfOwnedServer(p_350306_)
         ? Component.translatable("mco.compatibility.upgrade.description", component2, component3)
         : Component.translatable("mco.compatibility.upgrade.friend.description", component2, component3);
      confirmToPlay(p_350306_, p_351001_, component, component4, component1);
   }

   public static Component getVersionComponent(String p_307541_, boolean p_307256_) {
      return getVersionComponent(p_307541_, p_307256_ ? -8355712 : -2142128);
   }

   public static Component getVersionComponent(String p_307429_, int p_307536_) {
      return (Component)(StringUtils.isBlank(p_307429_) ? CommonComponents.EMPTY : Component.literal(p_307429_).withColor(p_307536_));
   }

   public static Component getGameModeComponent(int p_380242_, boolean p_379740_) {
      return (Component)(p_379740_ ? Component.translatable("gameMode.hardcore").withColor(-65536) : GameType.byId(p_380242_).getLongDisplayName());
   }

   static boolean isSelfOwnedServer(RealmsServer p_86684_) {
      return Minecraft.getInstance().isLocalPlayer(p_86684_.ownerUUID);
   }

   private boolean isSelfOwnedNonExpiredServer(RealmsServer p_86689_) {
      return isSelfOwnedServer(p_86689_) && !p_86689_.expired;
   }

   private void renderEnvironment(GuiGraphics p_294591_, String p_295050_, int p_294351_) {
      p_294591_.pose().pushMatrix();
      p_294591_.pose().translate(this.width / 2 - 25, 20.0F);
      p_294591_.pose().rotate((float) (-Math.PI / 9));
      p_294591_.pose().scale(1.5F, 1.5F);
      p_294591_.drawString(this.font, p_295050_, 0, 0, p_294351_);
      p_294591_.pose().popMatrix();
   }

   @OnlyIn(Dist.CLIENT)
   class AvailableSnapshotEntry extends RealmsMainScreen.Entry {
      private static final Component START_SNAPSHOT_REALM = Component.translatable("mco.snapshot.start");
      private static final int TEXT_PADDING = 5;
      private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();
      private final RealmsServer parent;

      public AvailableSnapshotEntry(RealmsServer p_306154_) {
         this.parent = p_306154_;
         this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.tooltip")));
      }

      @Override
      public void renderContent(GuiGraphics p_439566_, int p_440470_, int p_438889_, boolean p_439430_, float p_439385_) {
         p_439566_.blitSprite(RenderPipelines.GUI_TEXTURED, RealmsMainScreen.NEW_REALM_SPRITE, this.getContentX() - 5, this.getContentYMiddle() - 10, 40, 20);
         int i = this.getContentYMiddle() - 4;
         p_439566_.drawString(RealmsMainScreen.this.font, START_SNAPSHOT_REALM, this.getContentX() + 40 - 2, i - 5, -8388737);
         p_439566_.drawString(
            RealmsMainScreen.this.font,
            Component.translatable("mco.snapshot.description", Objects.requireNonNullElse(this.parent.name, "unknown server")),
            this.getContentX() + 40 - 2,
            i + 5,
            -8355712
         );
         this.tooltip
            .refreshTooltipForNextRenderPass(
               p_439566_,
               p_440470_,
               p_438889_,
               p_439430_,
               this.isFocused(),
               new ScreenRectangle(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight())
            );
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent p_445652_, boolean p_435256_) {
         this.addSnapshotRealm();
         return true;
      }

      @Override
      public boolean keyPressed(KeyEvent p_446206_) {
         if (p_446206_.isSelection()) {
            this.addSnapshotRealm();
            return false;
         } else {
            return super.keyPressed(p_446206_);
         }
      }

      private void addSnapshotRealm() {
         RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         RealmsMainScreen.this.minecraft
            .setScreen(
               new PopupScreen.Builder(RealmsMainScreen.this, Component.translatable("mco.snapshot.createSnapshotPopup.title"))
                  .setMessage(Component.translatable("mco.snapshot.createSnapshotPopup.text"))
                  .addButton(
                     Component.translatable("mco.selectServer.create"),
                     p_438677_ -> RealmsMainScreen.this.minecraft.setScreen(new RealmsCreateRealmScreen(RealmsMainScreen.this, this.parent, true))
                  )
                  .addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose)
                  .build()
            );
      }

      @Override
      public Component getNarration() {
         return Component.translatable(
            "gui.narrate.button",
            CommonComponents.joinForNarration(
               START_SNAPSHOT_REALM, Component.translatable("mco.snapshot.description", Objects.requireNonNullElse(this.parent.name, "unknown server"))
            )
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class CrossButton extends ImageButton {
      private static final WidgetSprites SPRITES = new WidgetSprites(
         Identifier.withDefaultNamespace("widget/cross_button"), Identifier.withDefaultNamespace("widget/cross_button_highlighted")
      );

      protected CrossButton(Button.OnPress p_275420_, Component p_275193_) {
         super(0, 0, 14, 14, SPRITES, p_275420_);
         this.setTooltip(Tooltip.create(p_275193_));
      }
   }

   @OnlyIn(Dist.CLIENT)
   abstract class Entry extends ObjectSelectionList.Entry<RealmsMainScreen.Entry> {
      protected static final int STATUS_LIGHT_WIDTH = 10;
      private static final int STATUS_LIGHT_HEIGHT = 28;
      protected static final int PADDING_X = 7;
      protected static final int PADDING_Y = 2;

      protected void renderStatusLights(RealmsServer p_305936_, GuiGraphics p_306216_, int p_306276_, int p_305985_, int p_306182_, int p_306221_) {
         int i = p_306276_ - 10 - 7;
         int j = p_305985_ + 2;
         if (p_305936_.expired) {
            this.drawRealmStatus(p_306216_, i, j, p_306182_, p_306221_, RealmsMainScreen.EXPIRED_SPRITE, () -> RealmsMainScreen.SERVER_EXPIRED_TOOLTIP);
         } else if (p_305936_.state == RealmsServer.State.CLOSED) {
            this.drawRealmStatus(p_306216_, i, j, p_306182_, p_306221_, RealmsMainScreen.CLOSED_SPRITE, () -> RealmsMainScreen.SERVER_CLOSED_TOOLTIP);
         } else if (RealmsMainScreen.isSelfOwnedServer(p_305936_) && p_305936_.daysLeft < 7) {
            this.drawRealmStatus(
               p_306216_,
               i,
               j,
               p_306182_,
               p_306221_,
               RealmsMainScreen.EXPIRES_SOON_SPRITE,
               () -> {
                  if (p_305936_.daysLeft <= 0) {
                     return RealmsMainScreen.SERVER_EXPIRES_SOON_TOOLTIP;
                  } else {
                     return (Component)(p_305936_.daysLeft == 1
                        ? RealmsMainScreen.SERVER_EXPIRES_IN_DAY_TOOLTIP
                        : Component.translatable("mco.selectServer.expires.days", p_305936_.daysLeft));
                  }
               }
            );
         } else if (p_305936_.state == RealmsServer.State.OPEN) {
            this.drawRealmStatus(p_306216_, i, j, p_306182_, p_306221_, RealmsMainScreen.OPEN_SPRITE, () -> RealmsMainScreen.SERVER_OPEN_TOOLTIP);
         }
      }

      private void drawRealmStatus(
         GuiGraphics p_306047_, int p_306257_, int p_306190_, int p_306269_, int p_306067_, Identifier p_467107_, Supplier<Component> p_306129_
      ) {
         p_306047_.blitSprite(RenderPipelines.GUI_TEXTURED, p_467107_, p_306257_, p_306190_, 10, 28);
         if (RealmsMainScreen.this.realmSelectionList.isMouseOver(p_306269_, p_306067_)
            && p_306269_ >= p_306257_
            && p_306269_ <= p_306257_ + 10
            && p_306067_ >= p_306190_
            && p_306067_ <= p_306190_ + 28) {
            p_306047_.setTooltipForNextFrame(p_306129_.get(), p_306269_, p_306067_);
         }
      }

      protected void renderFirstLine(GuiGraphics p_418291_, int p_418452_, int p_418248_, int p_418035_, int p_418097_, RealmsServer p_418213_) {
         int i = this.textX(p_418248_);
         int j = this.firstLineY(p_418452_);
         Component component = RealmsMainScreen.getVersionComponent(p_418213_.activeVersion, p_418213_.isCompatible());
         int k = this.versionTextX(p_418248_, p_418035_, component);
         this.renderClampedString(p_418291_, p_418213_.getName(), i, j, k, p_418097_);
         if (component != CommonComponents.EMPTY && !p_418213_.isMinigameActive()) {
            p_418291_.drawString(RealmsMainScreen.this.font, component, k, j, -8355712);
         }
      }

      protected void renderSecondLine(GuiGraphics p_418401_, int p_418132_, int p_418292_, int p_418366_, RealmsServer p_418199_) {
         int i = this.textX(p_418292_);
         int j = this.firstLineY(p_418132_);
         int k = this.secondLineY(j);
         String s = p_418199_.getMinigameName();
         boolean flag = p_418199_.isMinigameActive();
         if (flag && s != null) {
            Component component = Component.literal(s).withStyle(ChatFormatting.GRAY);
            p_418401_.drawString(RealmsMainScreen.this.font, Component.translatable("mco.selectServer.minigameName", component).withColor(-171), i, k, -1);
         } else {
            int l = this.renderGameMode(p_418199_, p_418401_, p_418292_, p_418366_, j);
            this.renderClampedString(p_418401_, p_418199_.getDescription(), i, this.secondLineY(j), l, -8355712);
         }
      }

      protected void renderThirdLine(GuiGraphics p_307382_, int p_307598_, int p_307521_, RealmsServer p_307617_) {
         int i = this.textX(p_307521_);
         int j = this.firstLineY(p_307598_);
         int k = this.thirdLineY(j);
         if (!RealmsMainScreen.isSelfOwnedServer(p_307617_)) {
            p_307382_.drawString(RealmsMainScreen.this.font, p_307617_.owner, i, this.thirdLineY(j), -8355712);
         } else if (p_307617_.expired) {
            Component component = p_307617_.expiredTrial ? RealmsMainScreen.TRIAL_EXPIRED_TEXT : RealmsMainScreen.SUBSCRIPTION_EXPIRED_TEXT;
            p_307382_.drawString(RealmsMainScreen.this.font, component, i, k, -2142128);
         }
      }

      protected void renderClampedString(GuiGraphics p_306102_, @Nullable String p_305897_, int p_305934_, int p_306080_, int p_305845_, int p_306320_) {
         if (p_305897_ != null) {
            int i = p_305845_ - p_305934_;
            if (RealmsMainScreen.this.font.width(p_305897_) > i) {
               String s = RealmsMainScreen.this.font.plainSubstrByWidth(p_305897_, i - RealmsMainScreen.this.font.width("... "));
               p_306102_.drawString(RealmsMainScreen.this.font, s + "...", p_305934_, p_306080_, p_306320_);
            } else {
               p_306102_.drawString(RealmsMainScreen.this.font, p_305897_, p_305934_, p_306080_, p_306320_);
            }
         }
      }

      protected int versionTextX(int p_306042_, int p_306287_, Component p_305839_) {
         return p_306042_ + p_306287_ - RealmsMainScreen.this.font.width(p_305839_) - 20;
      }

      protected int gameModeTextX(int p_379545_, int p_379584_, Component p_379755_) {
         return p_379545_ + p_379584_ - RealmsMainScreen.this.font.width(p_379755_) - 20;
      }

      protected int renderGameMode(RealmsServer p_379292_, GuiGraphics p_379418_, int p_379744_, int p_379359_, int p_380276_) {
         boolean flag = p_379292_.isHardcore;
         int i = p_379292_.gameMode;
         int j = p_379744_;
         if (GameType.isValidId(i)) {
            Component component = RealmsMainScreen.getGameModeComponent(i, flag);
            j = this.gameModeTextX(p_379744_, p_379359_, component);
            p_379418_.drawString(RealmsMainScreen.this.font, component, j, this.secondLineY(p_380276_), -8355712);
         }

         if (flag) {
            j -= 10;
            p_379418_.blitSprite(RenderPipelines.GUI_TEXTURED, RealmsMainScreen.HARDCORE_MODE_SPRITE, j, this.secondLineY(p_380276_), 8, 8);
         }

         return j;
      }

      protected int firstLineY(int p_306168_) {
         return p_306168_ + 1;
      }

      protected int lineHeight() {
         return 11;
      }

      protected int textX(int p_305801_) {
         return p_305801_ + 36 + 2;
      }

      protected int secondLineY(int p_306251_) {
         return p_306251_ + this.lineHeight();
      }

      protected int thirdLineY(int p_306115_) {
         return p_306115_ + this.lineHeight() * 2;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static enum LayoutState {
      LOADING,
      NO_REALMS,
      LIST;
   }

   @OnlyIn(Dist.CLIENT)
   static class NotificationButton extends SpriteIconButton.CenteredIcon {
      private static final Identifier[] NOTIFICATION_ICONS = new Identifier[]{
         Identifier.withDefaultNamespace("notification/1"),
         Identifier.withDefaultNamespace("notification/2"),
         Identifier.withDefaultNamespace("notification/3"),
         Identifier.withDefaultNamespace("notification/4"),
         Identifier.withDefaultNamespace("notification/5"),
         Identifier.withDefaultNamespace("notification/more")
      };
      private static final int UNKNOWN_COUNT = Integer.MAX_VALUE;
      private static final int SIZE = 20;
      private static final int SPRITE_SIZE = 14;
      private int notificationCount;

      public NotificationButton(Component p_296209_, Identifier p_467534_, Button.OnPress p_294154_, @Nullable Component p_439277_) {
         super(20, 20, p_296209_, 14, 14, new WidgetSprites(p_467534_), p_294154_, p_439277_, null);
      }

      int notificationCount() {
         return this.notificationCount;
      }

      public void setNotificationCount(int p_295599_) {
         this.notificationCount = p_295599_;
      }

      @Override
      public void renderContents(GuiGraphics p_458073_, int p_458104_, int p_457964_, float p_457870_) {
         super.renderContents(p_458073_, p_458104_, p_457964_, p_457870_);
         if (this.active && this.notificationCount != 0) {
            this.drawNotificationCounter(p_458073_);
         }
      }

      private void drawNotificationCounter(GuiGraphics p_295995_) {
         p_295995_.blitSprite(
            RenderPipelines.GUI_TEXTURED, NOTIFICATION_ICONS[Math.min(this.notificationCount, 6) - 1], this.getX() + this.getWidth() - 5, this.getY() - 3, 8, 8
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   class NotificationMessageEntry extends RealmsMainScreen.Entry {
      private static final int SIDE_MARGINS = 40;
      public static final int PADDING = 7;
      public static final int HEIGHT_WITHOUT_TEXT = 38;
      private final Component text;
      private final List<AbstractWidget> children = new ArrayList<>();
      @Nullable
      private final RealmsMainScreen.CrossButton dismissButton;
      private final MultiLineTextWidget textWidget;
      private final GridLayout gridLayout;
      private final FrameLayout textFrame;
      private final Button button;
      private int lastEntryWidth = -1;

      public NotificationMessageEntry(RealmsMainScreen p_440155_, int p_302495_, Component p_275215_, RealmsNotification.VisitUrl p_439108_) {
         this.text = p_275215_;
         this.gridLayout = new GridLayout();
         this.gridLayout.addChild(ImageWidget.sprite(20, 20, RealmsMainScreen.INFO_SPRITE), 0, 0, this.gridLayout.newCellSettings().padding(7, 7, 0, 0));
         this.gridLayout.addChild(SpacerElement.width(40), 0, 0);
         this.textFrame = this.gridLayout.addChild(new FrameLayout(0, p_302495_), 0, 1, this.gridLayout.newCellSettings().paddingTop(7));
         this.textWidget = this.textFrame
            .addChild(
               new MultiLineTextWidget(p_275215_, RealmsMainScreen.this.font).setCentered(true),
               this.textFrame.newChildLayoutSettings().alignHorizontallyCenter().alignVerticallyTop()
            );
         this.gridLayout.addChild(SpacerElement.width(40), 0, 2);
         if (p_439108_.dismissable()) {
            this.dismissButton = this.gridLayout
               .addChild(
                  new RealmsMainScreen.CrossButton(
                     p_438679_ -> RealmsMainScreen.this.dismissNotification(p_439108_.uuid()), Component.translatable("mco.notification.dismiss")
                  ),
                  0,
                  2,
                  this.gridLayout.newCellSettings().alignHorizontallyRight().padding(0, 7, 7, 0)
               );
         } else {
            this.dismissButton = null;
         }

         this.button = this.gridLayout
            .addChild(p_439108_.buildOpenLinkButton(p_440155_), 1, 1, this.gridLayout.newCellSettings().alignHorizontallyCenter().padding(4));
         this.button.setOverrideRenderHighlightedSprite(() -> this.isFocused());
         this.gridLayout.visitWidgets(this.children::add);
      }

      @Override
      public boolean keyPressed(KeyEvent p_445623_) {
         if (this.button.keyPressed(p_445623_)) {
            return true;
         } else {
            return this.dismissButton != null && this.dismissButton.keyPressed(p_445623_) ? true : super.keyPressed(p_445623_);
         }
      }

      private void updateEntryWidth() {
         int i = this.getWidth();
         if (this.lastEntryWidth != i) {
            this.refreshLayout(i);
            this.lastEntryWidth = i;
         }
      }

      private void refreshLayout(int p_275267_) {
         int i = textWidth(p_275267_);
         this.textFrame.setMinWidth(i);
         this.textWidget.setMaxWidth(i);
         this.gridLayout.arrangeElements();
      }

      public static int textWidth(int p_445924_) {
         return p_445924_ - 80;
      }

      @Override
      public void renderContent(GuiGraphics p_440677_, int p_439917_, int p_439616_, boolean p_440038_, float p_439405_) {
         this.gridLayout.setPosition(this.getContentX(), this.getContentY());
         this.updateEntryWidth();
         this.children.forEach(p_470468_ -> p_470468_.render(p_440677_, p_439917_, p_439616_, p_439405_));
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent p_447212_, boolean p_435417_) {
         if (this.dismissButton != null && this.dismissButton.mouseClicked(p_447212_, p_435417_)) {
            return true;
         } else {
            return this.button.mouseClicked(p_447212_, p_435417_) ? true : super.mouseClicked(p_447212_, p_435417_);
         }
      }

      public Component getText() {
         return this.text;
      }

      @Override
      public Component getNarration() {
         return this.getText();
      }
   }

   @OnlyIn(Dist.CLIENT)
   class ParentEntry extends RealmsMainScreen.Entry {
      private final RealmsServer server;
      private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

      public ParentEntry(RealmsServer p_306253_) {
         this.server = p_306253_;
         if (!p_306253_.expired) {
            this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.parent.tooltip")));
         }
      }

      @Override
      public void renderContent(GuiGraphics p_439160_, int p_439315_, int p_440617_, boolean p_439247_, float p_440453_) {
         this.renderStatusLights(this.server, p_439160_, this.getContentRight(), this.getContentY(), p_439315_, p_440617_);
         RealmsUtil.renderPlayerFace(p_439160_, this.getContentX(), this.getContentY(), 32, this.server.ownerUUID);
         this.renderFirstLine(p_439160_, this.getContentY(), this.getContentX(), this.getContentWidth(), -8355712, this.server);
         this.renderSecondLine(p_439160_, this.getContentY(), this.getContentX(), this.getContentWidth(), this.server);
         this.renderThirdLine(p_439160_, this.getContentY(), this.getContentX(), this.server);
         this.tooltip
            .refreshTooltipForNextRenderPass(
               p_439160_,
               p_439315_,
               p_440617_,
               p_439247_,
               this.isFocused(),
               new ScreenRectangle(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight())
            );
      }

      @Override
      public Component getNarration() {
         return Component.literal(Objects.requireNonNullElse(this.server.name, "unknown server"));
      }
   }

   @OnlyIn(Dist.CLIENT)
   class RealmSelectionList extends ObjectSelectionList<RealmsMainScreen.Entry> {
      public RealmSelectionList() {
         super(Minecraft.getInstance(), RealmsMainScreen.this.width, RealmsMainScreen.this.height, 0, 36);
      }

      public void setSelected(RealmsMainScreen.Entry p_86849_) {
         super.setSelected(p_86849_);
         RealmsMainScreen.this.updateButtonStates();
      }

      @Override
      public int getRowWidth() {
         return 300;
      }

      void refreshEntries(RealmsMainScreen p_379863_) {
         RealmsMainScreen.Entry realmsmainscreen$entry = this.getSelected();
         this.clearEntries();

         for (RealmsNotification realmsnotification : RealmsMainScreen.this.notifications) {
            if (realmsnotification instanceof RealmsNotification.VisitUrl realmsnotification$visiturl) {
               this.addEntriesForNotification(realmsnotification$visiturl, p_379863_, realmsmainscreen$entry);
               RealmsMainScreen.this.markNotificationsAsSeen(List.of(realmsnotification));
               break;
            }
         }

         this.refreshServerEntries(realmsmainscreen$entry);
      }

      private void addEntriesForNotification(RealmsNotification.VisitUrl p_379382_, RealmsMainScreen p_379543_, RealmsMainScreen.Entry p_446788_) {
         Component component = p_379382_.getMessage();
         int i = RealmsMainScreen.this.font.wordWrapHeight(component, RealmsMainScreen.NotificationMessageEntry.textWidth(this.getRowWidth()));
         RealmsMainScreen.NotificationMessageEntry realmsmainscreen$notificationmessageentry = RealmsMainScreen.this.new NotificationMessageEntry(
            p_379543_, i, component, p_379382_
         );
         this.addEntry(realmsmainscreen$notificationmessageentry, 38 + i);
         if (p_446788_ instanceof RealmsMainScreen.NotificationMessageEntry realmsmainscreen$notificationmessageentry1
            && realmsmainscreen$notificationmessageentry1.getText().equals(component)) {
            this.setSelected((RealmsMainScreen.Entry)realmsmainscreen$notificationmessageentry);
         }
      }

      private void refreshServerEntries(RealmsMainScreen.Entry p_446580_) {
         for (RealmsServer realmsserver : RealmsMainScreen.this.availableSnapshotServers) {
            this.addEntry(RealmsMainScreen.this.new AvailableSnapshotEntry(realmsserver));
         }

         for (RealmsServer realmsserver1 : RealmsMainScreen.this.serverList) {
            RealmsMainScreen.Entry realmsmainscreen$entry;
            if (RealmsMainScreen.isSnapshot() && !realmsserver1.isSnapshotRealm()) {
               if (realmsserver1.state == RealmsServer.State.UNINITIALIZED) {
                  continue;
               }

               realmsmainscreen$entry = RealmsMainScreen.this.new ParentEntry(realmsserver1);
            } else {
               realmsmainscreen$entry = RealmsMainScreen.this.new ServerEntry(realmsserver1);
            }

            this.addEntry(realmsmainscreen$entry);
            if (p_446580_ instanceof RealmsMainScreen.ServerEntry realmsmainscreen$serverentry
               && realmsmainscreen$serverentry.serverData.id == realmsserver1.id) {
               this.setSelected(realmsmainscreen$entry);
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   interface RealmsCall<T> {
      T request(RealmsClient var1) throws RealmsServiceException;
   }

   @OnlyIn(Dist.CLIENT)
   class ServerEntry extends RealmsMainScreen.Entry {
      private static final Component ONLINE_PLAYERS_TOOLTIP_HEADER = Component.translatable("mco.onlinePlayers");
      private static final int PLAYERS_ONLINE_SPRITE_SIZE = 9;
      private static final int PLAYERS_ONLINE_SPRITE_SEPARATION = 3;
      private static final int SKIN_HEAD_LARGE_WIDTH = 36;
      final RealmsServer serverData;
      private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

      public ServerEntry(RealmsServer p_86856_) {
         this.serverData = p_86856_;
         boolean flag = RealmsMainScreen.isSelfOwnedServer(p_86856_);
         if (RealmsMainScreen.isSnapshot() && flag && p_86856_.isSnapshotRealm()) {
            this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.paired", p_86856_.parentWorldName)));
         } else if (!flag && p_86856_.needsDowngrade()) {
            this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.friendsRealm.downgrade", p_86856_.activeVersion)));
         }
      }

      @Override
      public void renderContent(GuiGraphics p_438955_, int p_440520_, int p_439923_, boolean p_440111_, float p_440380_) {
         if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
            p_438955_.blitSprite(RenderPipelines.GUI_TEXTURED, RealmsMainScreen.NEW_REALM_SPRITE, this.getContentX() - 5, this.getContentYMiddle() - 10, 40, 20);
            int i = this.getContentYMiddle() - 4;
            p_438955_.drawString(RealmsMainScreen.this.font, RealmsMainScreen.SERVER_UNITIALIZED_TEXT, this.getContentX() + 40 - 2, i, -8388737);
         } else {
            RealmsUtil.renderPlayerFace(p_438955_, this.getContentX(), this.getContentY(), 32, this.serverData.ownerUUID);
            this.renderFirstLine(p_438955_, this.getContentY(), this.getContentX(), this.getContentWidth(), -1, this.serverData);
            this.renderSecondLine(p_438955_, this.getContentY(), this.getContentX(), this.getContentWidth(), this.serverData);
            this.renderThirdLine(p_438955_, this.getContentY(), this.getContentX(), this.serverData);
            this.renderStatusLights(this.serverData, p_438955_, this.getContentRight(), this.getContentY(), p_440520_, p_439923_);
            boolean flag = this.renderOnlinePlayers(
               p_438955_, this.getContentY(), this.getContentX(), this.getContentWidth(), this.getContentHeight(), p_440520_, p_439923_, p_440380_
            );
            if (!flag) {
               this.tooltip
                  .refreshTooltipForNextRenderPass(
                     p_438955_,
                     p_440520_,
                     p_439923_,
                     p_440111_,
                     this.isFocused(),
                     new ScreenRectangle(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight())
                  );
            }
         }
      }

      private boolean renderOnlinePlayers(
         GuiGraphics p_350774_, int p_350924_, int p_350405_, int p_350844_, int p_351009_, int p_350267_, int p_351054_, float p_419837_
      ) {
         List<ResolvableProfile> list = RealmsMainScreen.this.onlinePlayersPerRealm.getProfileResultsFor(this.serverData.id);
         int i = list.size();
         if (i > 0) {
            int j = p_350405_ + p_350844_ - 21;
            int k = p_350924_ + p_351009_ - 9 - 2;
            int l = 9 * i + 3 * (i - 1);
            int i1 = j - l;
            List<PlayerSkinRenderCache.RenderInfo> list1;
            if (p_350267_ >= i1 && p_350267_ <= j && p_351054_ >= k && p_351054_ <= k + 9) {
               list1 = new ArrayList<>(i);
            } else {
               list1 = null;
            }

            PlayerSkinRenderCache playerskinrendercache = RealmsMainScreen.this.minecraft.playerSkinRenderCache();

            for (int j1 = 0; j1 < list.size(); j1++) {
               ResolvableProfile resolvableprofile = list.get(j1);
               PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo = playerskinrendercache.getOrDefault(resolvableprofile);
               int k1 = i1 + 12 * j1;
               PlayerFaceRenderer.draw(p_350774_, playerskinrendercache$renderinfo.playerSkin(), k1, k, 9);
               if (list1 != null) {
                  list1.add(playerskinrendercache$renderinfo);
               }
            }

            if (list1 != null) {
               p_350774_.setTooltipForNextFrame(
                  RealmsMainScreen.this.font,
                  List.of(ONLINE_PLAYERS_TOOLTIP_HEADER),
                  Optional.of(new ClientActivePlayersTooltip.ActivePlayersTooltip(list1)),
                  p_350267_,
                  p_351054_
               );
               return true;
            }
         }

         return false;
      }

      private void playRealm() {
         RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         RealmsMainScreen.play(this.serverData, RealmsMainScreen.this);
      }

      private void createUnitializedRealm() {
         RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         RealmsCreateRealmScreen realmscreaterealmscreen = new RealmsCreateRealmScreen(
            RealmsMainScreen.this, this.serverData, this.serverData.isSnapshotRealm()
         );
         RealmsMainScreen.this.minecraft.setScreen(realmscreaterealmscreen);
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent p_446191_, boolean p_433725_) {
         if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
            this.createUnitializedRealm();
         } else if (this.serverData.shouldPlayButtonBeActive() && p_433725_ && this.isFocused()) {
            this.playRealm();
         }

         return true;
      }

      @Override
      public boolean keyPressed(KeyEvent p_445839_) {
         if (p_445839_.isSelection()) {
            if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
               this.createUnitializedRealm();
               return true;
            }

            if (this.serverData.shouldPlayButtonBeActive()) {
               this.playRealm();
               return true;
            }
         }

         return super.keyPressed(p_445839_);
      }

      @Override
      public Component getNarration() {
         return (Component)(this.serverData.state == RealmsServer.State.UNINITIALIZED
            ? RealmsMainScreen.UNITIALIZED_WORLD_NARRATION
            : Component.translatable("narrator.select", Objects.requireNonNullElse(this.serverData.name, "unknown server")));
      }

      public RealmsServer getServer() {
         return this.serverData;
      }
   }
}
