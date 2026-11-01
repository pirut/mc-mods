package com.mojang.realmsclient;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
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
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsCreateRealmScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPendingInvitesScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.gui.task.DataFetcher;
import com.mojang.realmsclient.util.RealmsPersistence;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
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
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientActivePlayersTooltip;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsMainScreen extends RealmsScreen {
   static final ResourceLocation INFO_SPRITE = ResourceLocation.withDefaultNamespace("icon/info");
   static final ResourceLocation NEW_REALM_SPRITE = ResourceLocation.withDefaultNamespace("icon/new_realm");
   static final ResourceLocation EXPIRED_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/expired");
   static final ResourceLocation EXPIRES_SOON_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/expires_soon");
   static final ResourceLocation OPEN_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/open");
   static final ResourceLocation CLOSED_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/closed");
   private static final ResourceLocation INVITE_SPRITE = ResourceLocation.withDefaultNamespace("icon/invite");
   private static final ResourceLocation NEWS_SPRITE = ResourceLocation.withDefaultNamespace("icon/news");
   static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation LOGO_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/title/realms.png");
   private static final ResourceLocation NO_REALMS_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/realms/no_realms.png");
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
   private static final int LOGO_WIDTH = 128;
   private static final int LOGO_HEIGHT = 34;
   private static final int LOGO_TEXTURE_WIDTH = 128;
   private static final int LOGO_TEXTURE_HEIGHT = 64;
   private static final int LOGO_PADDING = 5;
   private static final int HEADER_HEIGHT = 44;
   private static final int FOOTER_PADDING = 11;
   private static final int NEW_REALM_SPRITE_WIDTH = 40;
   private static final int NEW_REALM_SPRITE_HEIGHT = 20;
   private static final int ENTRY_WIDTH = 216;
   private static final int ITEM_HEIGHT = 36;
   private static final boolean SNAPSHOT = !SharedConstants.getCurrentVersion().isStable();
   private static boolean snapshotToggle = SNAPSHOT;
   private final CompletableFuture<RealmsAvailability.Result> availability = RealmsAvailability.get();
   @Nullable
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
   private RealmsServerList serverList;
   private List<RealmsServer> availableSnapshotServers = List.of();
   RealmsServerPlayerLists onlinePlayersPerRealm = new RealmsServerPlayerLists();
   private volatile boolean trialsAvailable;
   @Nullable
   private volatile String newsLink;
   long lastClickTime;
   private final List<RealmsNotification> notifications = new ArrayList<>();
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
      Component $$0 = Component.translatable("mco.invites.title");
      this.pendingInvitesButton = new RealmsMainScreen.NotificationButton(
         $$0, INVITE_SPRITE, p_293547_ -> this.minecraft.setScreen(new RealmsPendingInvitesScreen(this, $$0))
      );
      Component $$1 = Component.translatable("mco.news");
      this.newsButton = new RealmsMainScreen.NotificationButton($$1, NEWS_SPRITE, p_307015_ -> {
         String $$1x = this.newsLink;
         if ($$1x != null) {
            ConfirmLinkScreen.confirmLinkNow(this, $$1x);
            if (this.newsButton.notificationCount() != 0) {
               RealmsPersistence.RealmsPersistenceData $$2 = RealmsPersistence.readFile();
               $$2.hasUnreadNews = false;
               RealmsPersistence.writeFile($$2);
               this.newsButton.setNotificationCount(0);
            }
         }
      });
      this.newsButton.setTooltip(Tooltip.create($$1));
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
            CycleButton.booleanBuilder(Component.literal("Snapshot"), Component.literal("Release"))
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
         Screen $$1x = p_293549_.createErrorScreen(this.lastScreen);
         if ($$1x == null) {
            this.dataSubscription = this.initDataFetcher(this.minecraft.realmsDataFetcher());
         } else {
            this.minecraft.setScreen($$1x);
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
            AbstractWidget var10000 = this.addRenderableWidget(p_321334_);
         });
         this.repositionElements();
      }
   }

   private HeaderAndFooterLayout createLayout(RealmsMainScreen.LayoutState p_295052_) {
      HeaderAndFooterLayout $$1 = new HeaderAndFooterLayout(this);
      $$1.setHeaderHeight(44);
      $$1.addToHeader(this.createHeader());
      Layout $$2 = this.createFooter(p_295052_);
      $$2.arrangeElements();
      $$1.setFooterHeight($$2.getHeight() + 22);
      $$1.addToFooter($$2);
      switch (p_295052_) {
         case LOADING:
            $$1.addToContents(new LoadingDotsWidget(this.font, LOADING_TEXT));
            break;
         case NO_REALMS:
            $$1.addToContents(this.createNoRealmsContent());
            break;
         case LIST:
            $$1.addToContents(this.realmSelectionList);
      }

      return $$1;
   }

   private Layout createHeader() {
      int $$0 = 90;
      LinearLayout $$1 = LinearLayout.horizontal().spacing(4);
      $$1.defaultCellSetting().alignVerticallyMiddle();
      $$1.addChild(this.pendingInvitesButton);
      $$1.addChild(this.newsButton);
      LinearLayout $$2 = LinearLayout.horizontal();
      $$2.defaultCellSetting().alignVerticallyMiddle();
      $$2.addChild(SpacerElement.width(90));
      $$2.addChild(ImageWidget.texture(128, 34, LOGO_LOCATION, 128, 64), LayoutSettings::alignHorizontallyCenter);
      $$2.addChild(new FrameLayout(90, 44)).addChild($$1, LayoutSettings::alignHorizontallyRight);
      return $$2;
   }

   private Layout createFooter(RealmsMainScreen.LayoutState p_294561_) {
      GridLayout $$1 = new GridLayout().spacing(4);
      GridLayout.RowHelper $$2 = $$1.createRowHelper(3);
      if (p_294561_ == RealmsMainScreen.LayoutState.LIST) {
         $$2.addChild(this.playButton);
         $$2.addChild(this.configureButton);
         $$2.addChild(this.renewButton);
         $$2.addChild(this.leaveButton);
      }

      $$2.addChild(this.addRealmButton);
      $$2.addChild(this.backButton);
      return $$1;
   }

   private LinearLayout createNoRealmsContent() {
      LinearLayout $$0 = LinearLayout.vertical().spacing(8);
      $$0.defaultCellSetting().alignHorizontallyCenter();
      $$0.addChild(ImageWidget.texture(130, 64, NO_REALMS_LOCATION, 130, 64));
      FocusableTextWidget $$1 = new FocusableTextWidget(308, NO_REALMS_TEXT, this.font, false, 4);
      $$0.addChild($$1);
      return $$0;
   }

   void updateButtonStates() {
      RealmsServer $$0 = this.getSelectedServer();
      this.addRealmButton.active = this.activeLayoutState != RealmsMainScreen.LayoutState.LOADING;
      this.playButton.active = $$0 != null && this.shouldPlayButtonBeActive($$0);
      this.renewButton.active = $$0 != null && this.shouldRenewButtonBeActive($$0);
      this.leaveButton.active = $$0 != null && this.shouldLeaveButtonBeActive($$0);
      this.configureButton.active = $$0 != null && this.shouldConfigureButtonBeActive($$0);
   }

   boolean shouldPlayButtonBeActive(RealmsServer p_86563_) {
      boolean $$1 = !p_86563_.expired && p_86563_.state == RealmsServer.State.OPEN;
      return $$1 && (p_86563_.isCompatible() || p_86563_.needsUpgrade() || isSelfOwnedServer(p_86563_));
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
      for (DataFetcher.Task<?> $$0 : this.minecraft.realmsDataFetcher().getTasks()) {
         $$0.reset();
      }
   }

   private DataFetcher.Subscription initDataFetcher(RealmsDataFetcher p_238836_) {
      DataFetcher.Subscription $$1 = p_238836_.dataFetcher.createSubscription();
      $$1.subscribe(p_238836_.serverListUpdateTask, p_305616_ -> {
         this.serverList.updateServersList(p_305616_.serverList());
         this.availableSnapshotServers = p_305616_.availableSnapshotServers();
         this.refreshListAndLayout();
         boolean $$1x = false;

         for (RealmsServer $$2 : this.serverList) {
            if (this.isSelfOwnedNonExpiredServer($$2)) {
               $$1x = true;
            }
         }

         if (!regionsPinged && $$1x) {
            regionsPinged = true;
            this.pingRegions();
         }
      });
      callRealmsClient(RealmsClient::getNotifications, p_304053_ -> {
         this.notifications.clear();
         this.notifications.addAll(p_304053_);

         for (RealmsNotification $$1x : p_304053_) {
            if ($$1x instanceof RealmsNotification.InfoPopup $$2) {
               PopupScreen $$3 = $$2.buildScreen(this, this::dismissNotification);
               if ($$3 != null) {
                  this.minecraft.setScreen($$3);
                  this.markNotificationsAsSeen(List.of($$1x));
                  break;
               }
            }
         }

         if (!this.notifications.isEmpty() && this.activeLayoutState != RealmsMainScreen.LayoutState.LOADING) {
            this.refreshListAndLayout();
         }
      });
      $$1.subscribe(p_238836_.pendingInvitesTask, p_300619_ -> {
         this.pendingInvitesButton.setNotificationCount(p_300619_);
         this.pendingInvitesButton.setTooltip(p_300619_ == 0 ? Tooltip.create(NO_PENDING_INVITES) : Tooltip.create(PENDING_INVITES));
         if (p_300619_ > 0 && this.inviteNarrationLimiter.tryAcquire(1)) {
            this.minecraft.getNarrator().sayNow(Component.translatable("mco.configure.world.invite.narration", p_300619_));
         }
      });
      $$1.subscribe(p_238836_.trialAvailabilityTask, p_293548_ -> this.trialsAvailable = p_293548_);
      $$1.subscribe(p_238836_.onlinePlayersTask, p_349772_ -> this.onlinePlayersPerRealm = p_349772_);
      $$1.subscribe(p_238836_.newsTask, p_300622_ -> {
         p_238836_.newsManager.updateUnreadNews(p_300622_);
         this.newsLink = p_238836_.newsManager.newsLink();
         this.newsButton.setNotificationCount(p_238836_.newsManager.hasUnreadNews() ? Integer.MAX_VALUE : 0);
      });
      return $$1;
   }

   private void markNotificationsAsSeen(Collection<RealmsNotification> p_304698_) {
      List<UUID> $$1 = new ArrayList<>(p_304698_.size());

      for (RealmsNotification $$2 : p_304698_) {
         if (!$$2.seen() && !this.handledSeenNotifications.contains($$2.uuid())) {
            $$1.add($$2.uuid());
         }
      }

      if (!$$1.isEmpty()) {
         callRealmsClient(p_274625_ -> {
            p_274625_.notificationsSeen($$1);
            return null;
         }, p_274630_ -> this.handledSeenNotifications.addAll($$1));
      }
   }

   private static <T> void callRealmsClient(RealmsMainScreen.RealmsCall<T> p_275561_, Consumer<T> p_275686_) {
      Minecraft $$2 = Minecraft.getInstance();
      CompletableFuture.<T>supplyAsync(() -> {
         try {
            return p_275561_.request(RealmsClient.create($$2));
         } catch (RealmsServiceException var3) {
            throw new RuntimeException(var3);
         }
      }).thenAcceptAsync(p_275686_, $$2).exceptionally(p_274626_ -> {
         LOGGER.error("Failed to execute call to Realms Service", p_274626_);
         return null;
      });
   }

   private void refreshListAndLayout() {
      RealmsServer $$0 = this.getSelectedServer();
      this.realmSelectionList.clear();

      for (RealmsNotification $$1 : this.notifications) {
         if (this.addListEntriesForNotification($$1)) {
            this.markNotificationsAsSeen(List.of($$1));
            break;
         }
      }

      for (RealmsServer $$2 : this.availableSnapshotServers) {
         this.realmSelectionList.addEntry(new RealmsMainScreen.AvailableSnapshotEntry($$2));
      }

      for (RealmsServer $$3 : this.serverList) {
         RealmsMainScreen.Entry $$4;
         if (isSnapshot() && !$$3.isSnapshotRealm()) {
            if ($$3.state == RealmsServer.State.UNINITIALIZED) {
               continue;
            }

            $$4 = new RealmsMainScreen.ParentEntry($$3);
         } else {
            $$4 = new RealmsMainScreen.ServerEntry($$3);
         }

         this.realmSelectionList.addEntry($$4);
         if ($$0 != null && $$0.id == $$3.id) {
            this.realmSelectionList.setSelected($$4);
         }
      }

      this.updateLayout();
      this.updateButtonStates();
   }

   private boolean addListEntriesForNotification(RealmsNotification p_304511_) {
      if (!(p_304511_ instanceof RealmsNotification.VisitUrl $$1)) {
         return false;
      } else {
         Component $$2 = $$1.getMessage();
         int $$3 = this.font.wordWrapHeight($$2, 216);
         int $$4 = Mth.positiveCeilDiv($$3 + 7, 36) - 1;
         this.realmSelectionList.addEntry(new RealmsMainScreen.NotificationMessageEntry($$2, $$4 + 2, $$1));

         for (int $$5 = 0; $$5 < $$4; $$5++) {
            this.realmSelectionList.addEntry(new RealmsMainScreen.EmptyEntry());
         }

         this.realmSelectionList.addEntry(new RealmsMainScreen.ButtonEntry($$1.buildOpenLinkButton(this)));
         return true;
      }
   }

   private void pingRegions() {
      new Thread(() -> {
         List<RegionPingResult> $$0 = Ping.pingAllRegions();
         RealmsClient $$1 = RealmsClient.create();
         PingResult $$2 = new PingResult();
         $$2.pingResults = $$0;
         $$2.realmIds = this.getOwnedNonExpiredRealmIds();

         try {
            $$1.sendPingResults($$2);
         } catch (Throwable var5) {
            LOGGER.warn("Could not send ping result to Realms: ", var5);
         }
      }).start();
   }

   private List<Long> getOwnedNonExpiredRealmIds() {
      List<Long> $$0 = Lists.newArrayList();

      for (RealmsServer $$1 : this.serverList) {
         if (this.isSelfOwnedNonExpiredServer($$1)) {
            $$0.add($$1.id);
         }
      }

      return $$0;
   }

   private void onRenew(@Nullable RealmsServer p_193500_) {
      if (p_193500_ != null) {
         String $$1 = CommonLinks.extendRealms(p_193500_.remoteSubscriptionId, this.minecraft.getUser().getProfileId(), p_193500_.expiredTrial);
         this.minecraft.keyboardHandler.setClipboard($$1);
         Util.getPlatform().openUri($$1);
      }
   }

   private void configureClicked(@Nullable RealmsServer p_86657_) {
      if (p_86657_ != null && this.minecraft.isLocalPlayer(p_86657_.ownerUUID)) {
         this.minecraft.setScreen(new RealmsConfigureWorldScreen(this, p_86657_.id));
      }
   }

   private void leaveClicked(@Nullable RealmsServer p_86670_) {
      if (p_86670_ != null && !this.minecraft.isLocalPlayer(p_86670_.ownerUUID)) {
         Component $$1 = Component.translatable("mco.configure.world.leave.question.line1");
         this.minecraft.setScreen(RealmsPopups.infoPopupScreen(this, $$1, p_344113_ -> this.leaveServer(p_86670_)));
      }
   }

   @Nullable
   private RealmsServer getSelectedServer() {
      return this.realmSelectionList.getSelected() instanceof RealmsMainScreen.ServerEntry $$0 ? $$0.getServer() : null;
   }

   private void leaveServer(final RealmsServer p_193495_) {
      (new Thread("Realms-leave-server") {
            @Override
            public void run() {
               try {
                  RealmsClient $$0 = RealmsClient.create();
                  $$0.uninviteMyselfFrom(p_193495_.id);
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
         p_282736_.drawString(this.font, "Minecraft " + SharedConstants.getCurrentVersion().getName(), 2, this.height - 10, -1);
      }

      if (this.trialsAvailable && this.addRealmButton.active) {
         AddRealmPopupScreen.renderDiamond(p_282736_, this.addRealmButton);
      }

      switch (RealmsClient.ENVIRONMENT) {
         case STAGE:
            this.renderEnvironment(p_282736_, "STAGE!", -256);
            break;
         case LOCAL:
            this.renderEnvironment(p_282736_, "LOCAL!", 8388479);
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
                     Component.literal(SharedConstants.getCurrentVersion().getName()).withColor(-171)
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
                              Component.literal(SharedConstants.getCurrentVersion().getName()).withColor(-171)
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
      Component $$2 = Component.translatable("mco.compatibility.upgrade.title").withColor(-171);
      Component $$3 = Component.translatable("mco.compatibility.upgrade");
      Component $$4 = Component.literal(p_350306_.activeVersion).withColor(-171);
      Component $$5 = Component.literal(SharedConstants.getCurrentVersion().getName()).withColor(-171);
      Component $$6 = isSelfOwnedServer(p_350306_)
         ? Component.translatable("mco.compatibility.upgrade.description", $$4, $$5)
         : Component.translatable("mco.compatibility.upgrade.friend.description", $$4, $$5);
      confirmToPlay(p_350306_, p_351001_, $$2, $$6, $$3);
   }

   public static Component getVersionComponent(String p_307541_, boolean p_307256_) {
      return getVersionComponent(p_307541_, p_307256_ ? -8355712 : -2142128);
   }

   public static Component getVersionComponent(String p_307429_, int p_307536_) {
      return (Component)(StringUtils.isBlank(p_307429_)
         ? CommonComponents.EMPTY
         : Component.translatable("mco.version", Component.literal(p_307429_).withColor(p_307536_)));
   }

   static boolean isSelfOwnedServer(RealmsServer p_86684_) {
      return Minecraft.getInstance().isLocalPlayer(p_86684_.ownerUUID);
   }

   private boolean isSelfOwnedNonExpiredServer(RealmsServer p_86689_) {
      return isSelfOwnedServer(p_86689_) && !p_86689_.expired;
   }

   private void renderEnvironment(GuiGraphics p_294591_, String p_295050_, int p_294351_) {
      p_294591_.pose().pushPose();
      p_294591_.pose().translate((float)(this.width / 2 - 25), 20.0F, 0.0F);
      p_294591_.pose().mulPose(Axis.ZP.rotationDegrees(-20.0F));
      p_294591_.pose().scale(1.5F, 1.5F, 1.5F);
      p_294591_.drawString(this.font, p_295050_, 0, 0, p_294351_, false);
      p_294591_.pose().popPose();
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
      public void render(
         GuiGraphics p_305850_,
         int p_305808_,
         int p_305963_,
         int p_306050_,
         int p_306209_,
         int p_305982_,
         int p_306247_,
         int p_306293_,
         boolean p_306089_,
         float p_306015_
      ) {
         p_305850_.blitSprite(RealmsMainScreen.NEW_REALM_SPRITE, p_306050_ - 5, p_305963_ + p_305982_ / 2 - 10, 40, 20);
         int $$10 = p_305963_ + p_305982_ / 2 - 9 / 2;
         p_305850_.drawString(RealmsMainScreen.this.font, START_SNAPSHOT_REALM, p_306050_ + 40 - 2, $$10 - 5, 8388479);
         p_305850_.drawString(
            RealmsMainScreen.this.font, Component.translatable("mco.snapshot.description", this.parent.name), p_306050_ + 40 - 2, $$10 + 5, -8355712
         );
         this.tooltip.refreshTooltipForNextRenderPass(p_306089_, this.isFocused(), new ScreenRectangle(p_306050_, p_305963_, p_306209_, p_305982_));
      }

      @Override
      public boolean mouseClicked(double p_306312_, double p_306107_, int p_306043_) {
         this.addSnapshotRealm();
         return true;
      }

      @Override
      public boolean keyPressed(int p_306133_, int p_305857_, int p_306123_) {
         if (CommonInputs.selected(p_306133_)) {
            this.addSnapshotRealm();
            return true;
         } else {
            return super.keyPressed(p_306133_, p_305857_, p_306123_);
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
                     p_315808_ -> RealmsMainScreen.this.minecraft.setScreen(new RealmsCreateRealmScreen(RealmsMainScreen.this, this.parent.id))
                  )
                  .addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose)
                  .build()
            );
      }

      @Override
      public Component getNarration() {
         return Component.translatable(
            "gui.narrate.button", CommonComponents.joinForNarration(START_SNAPSHOT_REALM, Component.translatable("mco.snapshot.description", this.parent.name))
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   class ButtonEntry extends RealmsMainScreen.Entry {
      private final Button button;

      public ButtonEntry(Button p_275726_) {
         this.button = p_275726_;
      }

      @Override
      public boolean mouseClicked(double p_275240_, double p_275616_, int p_275528_) {
         this.button.mouseClicked(p_275240_, p_275616_, p_275528_);
         return super.mouseClicked(p_275240_, p_275616_, p_275528_);
      }

      @Override
      public boolean keyPressed(int p_275630_, int p_275328_, int p_275519_) {
         return this.button.keyPressed(p_275630_, p_275328_, p_275519_) ? true : super.keyPressed(p_275630_, p_275328_, p_275519_);
      }

      @Override
      public void render(
         GuiGraphics p_283542_,
         int p_282029_,
         int p_281480_,
         int p_281377_,
         int p_283160_,
         int p_281920_,
         int p_283267_,
         int p_281282_,
         boolean p_281269_,
         float p_282372_
      ) {
         this.button.setPosition(RealmsMainScreen.this.width / 2 - 75, p_281480_ + 4);
         this.button.render(p_283542_, p_283267_, p_281282_, p_282372_);
      }

      @Override
      public void setFocused(boolean p_309684_) {
         super.setFocused(p_309684_);
         this.button.setFocused(p_309684_);
      }

      @Override
      public Component getNarration() {
         return this.button.getMessage();
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class CrossButton extends ImageButton {
      private static final WidgetSprites SPRITES = new WidgetSprites(
         ResourceLocation.withDefaultNamespace("widget/cross_button"), ResourceLocation.withDefaultNamespace("widget/cross_button_highlighted")
      );

      protected CrossButton(Button.OnPress p_275420_, Component p_275193_) {
         super(0, 0, 14, 14, SPRITES, p_275420_);
         this.setTooltip(Tooltip.create(p_275193_));
      }
   }

   @OnlyIn(Dist.CLIENT)
   class EmptyEntry extends RealmsMainScreen.Entry {
      @Override
      public void render(
         GuiGraphics p_302489_,
         int p_302486_,
         int p_302498_,
         int p_302485_,
         int p_302492_,
         int p_302487_,
         int p_302488_,
         int p_302496_,
         boolean p_302491_,
         float p_302497_
      ) {
      }

      @Override
      public Component getNarration() {
         return Component.empty();
      }
   }

   @OnlyIn(Dist.CLIENT)
   abstract class Entry extends ObjectSelectionList.Entry<RealmsMainScreen.Entry> {
      protected static final int STATUS_LIGHT_WIDTH = 10;
      private static final int STATUS_LIGHT_HEIGHT = 28;
      protected static final int PADDING_X = 7;
      protected static final int PADDING_Y = 2;

      protected void renderStatusLights(RealmsServer p_305936_, GuiGraphics p_306216_, int p_306276_, int p_305985_, int p_306182_, int p_306221_) {
         int $$6 = p_306276_ - 10 - 7;
         int $$7 = p_305985_ + 2;
         if (p_305936_.expired) {
            this.drawRealmStatus(p_306216_, $$6, $$7, p_306182_, p_306221_, RealmsMainScreen.EXPIRED_SPRITE, () -> RealmsMainScreen.SERVER_EXPIRED_TOOLTIP);
         } else if (p_305936_.state == RealmsServer.State.CLOSED) {
            this.drawRealmStatus(p_306216_, $$6, $$7, p_306182_, p_306221_, RealmsMainScreen.CLOSED_SPRITE, () -> RealmsMainScreen.SERVER_CLOSED_TOOLTIP);
         } else if (RealmsMainScreen.isSelfOwnedServer(p_305936_) && p_305936_.daysLeft < 7) {
            this.drawRealmStatus(
               p_306216_,
               $$6,
               $$7,
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
            this.drawRealmStatus(p_306216_, $$6, $$7, p_306182_, p_306221_, RealmsMainScreen.OPEN_SPRITE, () -> RealmsMainScreen.SERVER_OPEN_TOOLTIP);
         }
      }

      private void drawRealmStatus(
         GuiGraphics p_306047_, int p_306257_, int p_306190_, int p_306269_, int p_306067_, ResourceLocation p_306301_, Supplier<Component> p_306129_
      ) {
         p_306047_.blitSprite(p_306301_, p_306257_, p_306190_, 10, 28);
         if (RealmsMainScreen.this.realmSelectionList.isMouseOver(p_306269_, p_306067_)
            && p_306269_ >= p_306257_
            && p_306269_ <= p_306257_ + 10
            && p_306067_ >= p_306190_
            && p_306067_ <= p_306190_ + 28) {
            RealmsMainScreen.this.setTooltipForNextRenderPass(p_306129_.get());
         }
      }

      protected void renderThirdLine(GuiGraphics p_307382_, int p_307598_, int p_307521_, RealmsServer p_307617_) {
         int $$4 = this.textX(p_307521_);
         int $$5 = this.firstLineY(p_307598_);
         int $$6 = this.thirdLineY($$5);
         if (!RealmsMainScreen.isSelfOwnedServer(p_307617_)) {
            p_307382_.drawString(RealmsMainScreen.this.font, p_307617_.owner, $$4, this.thirdLineY($$5), -8355712, false);
         } else if (p_307617_.expired) {
            Component $$7 = p_307617_.expiredTrial ? RealmsMainScreen.TRIAL_EXPIRED_TEXT : RealmsMainScreen.SUBSCRIPTION_EXPIRED_TEXT;
            p_307382_.drawString(RealmsMainScreen.this.font, $$7, $$4, $$6, -2142128, false);
         }
      }

      protected void renderClampedString(GuiGraphics p_306102_, String p_305897_, int p_305934_, int p_306080_, int p_305845_, int p_306320_) {
         int $$6 = p_305845_ - p_305934_;
         if (RealmsMainScreen.this.font.width(p_305897_) > $$6) {
            String $$7 = RealmsMainScreen.this.font.plainSubstrByWidth(p_305897_, $$6 - RealmsMainScreen.this.font.width("... "));
            p_306102_.drawString(RealmsMainScreen.this.font, $$7 + "...", p_305934_, p_306080_, p_306320_, false);
         } else {
            p_306102_.drawString(RealmsMainScreen.this.font, p_305897_, p_305934_, p_306080_, p_306320_, false);
         }
      }

      protected int versionTextX(int p_306042_, int p_306287_, Component p_305839_) {
         return p_306042_ + p_306287_ - RealmsMainScreen.this.font.width(p_305839_) - 20;
      }

      protected int firstLineY(int p_306168_) {
         return p_306168_ + 1;
      }

      protected int lineHeight() {
         return 2 + 9;
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
      private static final ResourceLocation[] NOTIFICATION_ICONS = new ResourceLocation[]{
         ResourceLocation.withDefaultNamespace("notification/1"),
         ResourceLocation.withDefaultNamespace("notification/2"),
         ResourceLocation.withDefaultNamespace("notification/3"),
         ResourceLocation.withDefaultNamespace("notification/4"),
         ResourceLocation.withDefaultNamespace("notification/5"),
         ResourceLocation.withDefaultNamespace("notification/more")
      };
      private static final int UNKNOWN_COUNT = Integer.MAX_VALUE;
      private static final int SIZE = 20;
      private static final int SPRITE_SIZE = 14;
      private int notificationCount;

      public NotificationButton(Component p_296209_, ResourceLocation p_295363_, Button.OnPress p_294154_) {
         super(20, 20, p_296209_, 14, 14, p_295363_, p_294154_, null);
      }

      int notificationCount() {
         return this.notificationCount;
      }

      public void setNotificationCount(int p_295599_) {
         this.notificationCount = p_295599_;
      }

      @Override
      public void renderWidget(GuiGraphics p_295969_, int p_294986_, int p_294741_, float p_295116_) {
         super.renderWidget(p_295969_, p_294986_, p_294741_, p_295116_);
         if (this.active && this.notificationCount != 0) {
            this.drawNotificationCounter(p_295969_);
         }
      }

      private void drawNotificationCounter(GuiGraphics p_295995_) {
         p_295995_.blitSprite(NOTIFICATION_ICONS[Math.min(this.notificationCount, 6) - 1], this.getX() + this.getWidth() - 5, this.getY() - 3, 8, 8);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class NotificationMessageEntry extends RealmsMainScreen.Entry {
      private static final int SIDE_MARGINS = 40;
      private static final int OUTLINE_COLOR = -12303292;
      private final Component text;
      private final int frameItemHeight;
      private final List<AbstractWidget> children = new ArrayList<>();
      @Nullable
      private final RealmsMainScreen.CrossButton dismissButton;
      private final MultiLineTextWidget textWidget;
      private final GridLayout gridLayout;
      private final FrameLayout textFrame;
      private int lastEntryWidth = -1;

      public NotificationMessageEntry(Component p_275215_, int p_302495_, RealmsNotification p_275494_) {
         this.text = p_275215_;
         this.frameItemHeight = p_302495_;
         this.gridLayout = new GridLayout();
         int $$3 = 7;
         this.gridLayout.addChild(ImageWidget.sprite(20, 20, RealmsMainScreen.INFO_SPRITE), 0, 0, this.gridLayout.newCellSettings().padding(7, 7, 0, 0));
         this.gridLayout.addChild(SpacerElement.width(40), 0, 0);
         this.textFrame = this.gridLayout.addChild(new FrameLayout(0, 9 * 3 * (p_302495_ - 1)), 0, 1, this.gridLayout.newCellSettings().paddingTop(7));
         this.textWidget = this.textFrame
            .addChild(
               new MultiLineTextWidget(p_275215_, RealmsMainScreen.this.font).setCentered(true),
               this.textFrame.newChildLayoutSettings().alignHorizontallyCenter().alignVerticallyTop()
            );
         this.gridLayout.addChild(SpacerElement.width(40), 0, 2);
         if (p_275494_.dismissable()) {
            this.dismissButton = this.gridLayout
               .addChild(
                  new RealmsMainScreen.CrossButton(
                     p_275478_ -> RealmsMainScreen.this.dismissNotification(p_275494_.uuid()), Component.translatable("mco.notification.dismiss")
                  ),
                  0,
                  2,
                  this.gridLayout.newCellSettings().alignHorizontallyRight().padding(0, 7, 7, 0)
               );
         } else {
            this.dismissButton = null;
         }

         this.gridLayout.visitWidgets(this.children::add);
      }

      @Override
      public boolean keyPressed(int p_275646_, int p_275453_, int p_275621_) {
         return this.dismissButton != null && this.dismissButton.keyPressed(p_275646_, p_275453_, p_275621_)
            ? true
            : super.keyPressed(p_275646_, p_275453_, p_275621_);
      }

      private void updateEntryWidth(int p_275670_) {
         if (this.lastEntryWidth != p_275670_) {
            this.refreshLayout(p_275670_);
            this.lastEntryWidth = p_275670_;
         }
      }

      private void refreshLayout(int p_275267_) {
         int $$1 = p_275267_ - 80;
         this.textFrame.setMinWidth($$1);
         this.textWidget.setMaxWidth($$1);
         this.gridLayout.arrangeElements();
      }

      @Override
      public void renderBack(
         GuiGraphics p_281374_,
         int p_282622_,
         int p_283656_,
         int p_281830_,
         int p_281651_,
         int p_283685_,
         int p_281784_,
         int p_282510_,
         boolean p_283146_,
         float p_283324_
      ) {
         super.renderBack(p_281374_, p_282622_, p_283656_, p_281830_, p_281651_, p_283685_, p_281784_, p_282510_, p_283146_, p_283324_);
         p_281374_.renderOutline(p_281830_ - 2, p_283656_ - 2, p_281651_, 36 * this.frameItemHeight - 2, -12303292);
      }

      @Override
      public void render(
         GuiGraphics p_281768_,
         int p_275375_,
         int p_275358_,
         int p_275447_,
         int p_275694_,
         int p_275477_,
         int p_275710_,
         int p_275677_,
         boolean p_275542_,
         float p_275323_
      ) {
         this.gridLayout.setPosition(p_275447_, p_275358_);
         this.updateEntryWidth(p_275694_ - 4);
         this.children.forEach(p_280688_ -> p_280688_.render(p_281768_, p_275710_, p_275677_, p_275323_));
      }

      @Override
      public boolean mouseClicked(double p_275209_, double p_275338_, int p_275560_) {
         if (this.dismissButton != null) {
            this.dismissButton.mouseClicked(p_275209_, p_275338_, p_275560_);
         }

         return super.mouseClicked(p_275209_, p_275338_, p_275560_);
      }

      @Override
      public Component getNarration() {
         return this.text;
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
      public void render(
         GuiGraphics p_305927_,
         int p_306097_,
         int p_306073_,
         int p_306079_,
         int p_306272_,
         int p_306279_,
         int p_306334_,
         int p_305824_,
         boolean p_306096_,
         float p_306323_
      ) {
         int $$10 = this.textX(p_306079_);
         int $$11 = this.firstLineY(p_306073_);
         RealmsUtil.renderPlayerFace(p_305927_, p_306079_, p_306073_, 32, this.server.ownerUUID);
         Component $$12 = RealmsMainScreen.getVersionComponent(this.server.activeVersion, -8355712);
         int $$13 = this.versionTextX(p_306079_, p_306272_, $$12);
         this.renderClampedString(p_305927_, this.server.getName(), $$10, $$11, $$13, -8355712);
         if ($$12 != CommonComponents.EMPTY) {
            p_305927_.drawString(RealmsMainScreen.this.font, $$12, $$13, $$11, -8355712, false);
         }

         p_305927_.drawString(RealmsMainScreen.this.font, this.server.getDescription(), $$10, this.secondLineY($$11), -8355712, false);
         this.renderThirdLine(p_305927_, p_306073_, p_306079_, this.server);
         this.renderStatusLights(this.server, p_305927_, p_306079_ + p_306272_, p_306073_, p_306334_, p_305824_);
         this.tooltip.refreshTooltipForNextRenderPass(p_306096_, this.isFocused(), new ScreenRectangle(p_306079_, p_306073_, p_306272_, p_306279_));
      }

      @Override
      public Component getNarration() {
         return Component.literal(this.server.name);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class RealmSelectionList extends RealmsObjectSelectionList<RealmsMainScreen.Entry> {
      public RealmSelectionList() {
         super(RealmsMainScreen.this.width, RealmsMainScreen.this.height, 0, 36);
      }

      public void setSelected(@Nullable RealmsMainScreen.Entry p_86849_) {
         super.setSelected(p_86849_);
         RealmsMainScreen.this.updateButtonStates();
      }

      @Override
      public int getMaxPosition() {
         return this.getItemCount() * 36;
      }

      @Override
      public int getRowWidth() {
         return 300;
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
      private static final int SKIN_HEAD_LARGE_WIDTH = 36;
      private final RealmsServer serverData;
      private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

      public ServerEntry(RealmsServer p_86856_) {
         this.serverData = p_86856_;
         boolean $$1 = RealmsMainScreen.isSelfOwnedServer(p_86856_);
         if (RealmsMainScreen.isSnapshot() && $$1 && p_86856_.isSnapshotRealm()) {
            this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.paired", p_86856_.parentWorldName)));
         } else if (!$$1 && p_86856_.needsDowngrade()) {
            this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.friendsRealm.downgrade", p_86856_.activeVersion)));
         }
      }

      @Override
      public void render(
         GuiGraphics p_283093_,
         int p_281645_,
         int p_283047_,
         int p_283525_,
         int p_282321_,
         int p_282391_,
         int p_281913_,
         int p_282475_,
         boolean p_282378_,
         float p_282843_
      ) {
         if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
            p_283093_.blitSprite(RealmsMainScreen.NEW_REALM_SPRITE, p_283525_ - 5, p_283047_ + p_282391_ / 2 - 10, 40, 20);
            int $$10 = p_283047_ + p_282391_ / 2 - 9 / 2;
            p_283093_.drawString(RealmsMainScreen.this.font, RealmsMainScreen.SERVER_UNITIALIZED_TEXT, p_283525_ + 40 - 2, $$10, 8388479);
         } else {
            this.renderStatusLights(this.serverData, p_283093_, p_283525_ + 36, p_283047_, p_281913_, p_282475_);
            RealmsUtil.renderPlayerFace(p_283093_, p_283525_, p_283047_, 32, this.serverData.ownerUUID);
            this.renderFirstLine(p_283093_, p_283047_, p_283525_, p_282321_);
            this.renderSecondLine(p_283093_, p_283047_, p_283525_);
            this.renderThirdLine(p_283093_, p_283047_, p_283525_, this.serverData);
            boolean $$11 = this.renderOnlinePlayers(p_283093_, p_283047_, p_283525_, p_282321_, p_282391_, p_281913_, p_282475_);
            this.renderStatusLights(this.serverData, p_283093_, p_283525_ + p_282321_, p_283047_, p_281913_, p_282475_);
            if (!$$11) {
               this.tooltip.refreshTooltipForNextRenderPass(p_282378_, this.isFocused(), new ScreenRectangle(p_283525_, p_283047_, p_282321_, p_282391_));
            }
         }
      }

      private void renderFirstLine(GuiGraphics p_307578_, int p_307385_, int p_307677_, int p_307273_) {
         int $$4 = this.textX(p_307677_);
         int $$5 = this.firstLineY(p_307385_);
         Component $$6 = RealmsMainScreen.getVersionComponent(this.serverData.activeVersion, this.serverData.isCompatible());
         int $$7 = this.versionTextX(p_307677_, p_307273_, $$6);
         this.renderClampedString(p_307578_, this.serverData.getName(), $$4, $$5, $$7, -1);
         if ($$6 != CommonComponents.EMPTY && !this.serverData.isMinigameActive()) {
            p_307578_.drawString(RealmsMainScreen.this.font, $$6, $$7, $$5, -8355712, false);
         }
      }

      private void renderSecondLine(GuiGraphics p_307602_, int p_307253_, int p_307666_) {
         int $$3 = this.textX(p_307666_);
         int $$4 = this.firstLineY(p_307253_);
         int $$5 = this.secondLineY($$4);
         String $$6 = this.serverData.getMinigameName();
         if (this.serverData.isMinigameActive() && $$6 != null) {
            Component $$7 = Component.literal($$6).withStyle(ChatFormatting.GRAY);
            p_307602_.drawString(RealmsMainScreen.this.font, Component.translatable("mco.selectServer.minigameName", $$7).withColor(-171), $$3, $$5, -1, false);
         } else {
            p_307602_.drawString(RealmsMainScreen.this.font, this.serverData.getDescription(), $$3, this.secondLineY($$4), -8355712, false);
         }
      }

      private boolean renderOnlinePlayers(GuiGraphics p_350774_, int p_350924_, int p_350405_, int p_350844_, int p_351009_, int p_350267_, int p_351054_) {
         List<ProfileResult> $$7 = RealmsMainScreen.this.onlinePlayersPerRealm.getProfileResultsFor(this.serverData.id);
         if (!$$7.isEmpty()) {
            int $$8 = p_350405_ + p_350844_ - 21;
            int $$9 = p_350924_ + p_351009_ - 9 - 2;
            int $$10 = $$8;

            for (int $$11 = 0; $$11 < $$7.size(); $$11++) {
               $$10 -= 9 + ($$11 == 0 ? 0 : 3);
               PlayerFaceRenderer.draw(p_350774_, Minecraft.getInstance().getSkinManager().getInsecureSkin($$7.get($$11).profile()), $$10, $$9, 9);
            }

            if (p_350267_ >= $$10 && p_350267_ <= $$8 && p_351054_ >= $$9 && p_351054_ <= $$9 + 9) {
               p_350774_.renderTooltip(
                  RealmsMainScreen.this.font,
                  List.of(ONLINE_PLAYERS_TOOLTIP_HEADER),
                  Optional.of(new ClientActivePlayersTooltip.ActivePlayersTooltip($$7)),
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
         RealmsCreateRealmScreen $$0 = new RealmsCreateRealmScreen(RealmsMainScreen.this, this.serverData);
         RealmsMainScreen.this.minecraft.setScreen($$0);
      }

      @Override
      public boolean mouseClicked(double p_86858_, double p_86859_, int p_86860_) {
         if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
            this.createUnitializedRealm();
         } else if (RealmsMainScreen.this.shouldPlayButtonBeActive(this.serverData)) {
            if (Util.getMillis() - RealmsMainScreen.this.lastClickTime < 250L && this.isFocused()) {
               this.playRealm();
            }

            RealmsMainScreen.this.lastClickTime = Util.getMillis();
         }

         return true;
      }

      @Override
      public boolean keyPressed(int p_279120_, int p_279121_, int p_279296_) {
         if (CommonInputs.selected(p_279120_)) {
            if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
               this.createUnitializedRealm();
               return true;
            }

            if (RealmsMainScreen.this.shouldPlayButtonBeActive(this.serverData)) {
               this.playRealm();
               return true;
            }
         }

         return super.keyPressed(p_279120_, p_279121_, p_279296_);
      }

      @Override
      public Component getNarration() {
         return (Component)(this.serverData.state == RealmsServer.State.UNINITIALIZED
            ? RealmsMainScreen.UNITIALIZED_WORLD_NARRATION
            : Component.translatable("narrator.select", this.serverData.name));
      }

      public RealmsServer getServer() {
         return this.serverData;
      }
   }
}
