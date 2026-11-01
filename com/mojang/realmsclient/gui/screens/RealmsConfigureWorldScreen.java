package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsWorldSlotButton;
import com.mojang.realmsclient.util.task.CloseServerTask;
import com.mojang.realmsclient.util.task.OpenServerTask;
import com.mojang.realmsclient.util.task.SwitchMinigameTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsConfigureWorldScreen extends RealmsScreen {
   private static final ResourceLocation EXPIRED_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/expired");
   private static final ResourceLocation EXPIRES_SOON_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/expires_soon");
   private static final ResourceLocation OPEN_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/open");
   private static final ResourceLocation CLOSED_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/closed");
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component WORLD_LIST_TITLE = Component.translatable("mco.configure.worlds.title");
   private static final Component TITLE = Component.translatable("mco.configure.world.title");
   private static final Component SERVER_EXPIRED_TOOLTIP = Component.translatable("mco.selectServer.expired");
   private static final Component SERVER_EXPIRING_SOON_TOOLTIP = Component.translatable("mco.selectServer.expires.soon");
   private static final Component SERVER_EXPIRING_IN_DAY_TOOLTIP = Component.translatable("mco.selectServer.expires.day");
   private static final Component SERVER_OPEN_TOOLTIP = Component.translatable("mco.selectServer.open");
   private static final Component SERVER_CLOSED_TOOLTIP = Component.translatable("mco.selectServer.closed");
   private static final int DEFAULT_BUTTON_WIDTH = 80;
   private static final int DEFAULT_BUTTON_OFFSET = 5;
   @Nullable
   private Component toolTip;
   private final RealmsMainScreen lastScreen;
   @Nullable
   private RealmsServer serverData;
   private final long serverId;
   private int leftX;
   private int rightX;
   private Button playersButton;
   private Button settingsButton;
   private Button subscriptionButton;
   private Button optionsButton;
   private Button backupButton;
   private Button resetWorldButton;
   private Button switchMinigameButton;
   private boolean stateChanged;
   private final List<RealmsWorldSlotButton> slotButtonList = Lists.newArrayList();

   public RealmsConfigureWorldScreen(RealmsMainScreen p_88411_, long p_88412_) {
      super(TITLE);
      this.lastScreen = p_88411_;
      this.serverId = p_88412_;
   }

   @Override
   public void init() {
      if (this.serverData == null) {
         this.fetchServerData(this.serverId);
      }

      this.leftX = this.width / 2 - 187;
      this.rightX = this.width / 2 + 190;
      this.playersButton = this.addRenderableWidget(
         Button.builder(
               Component.translatable("mco.configure.world.buttons.players"),
               p_280722_ -> this.minecraft.setScreen(new RealmsPlayerScreen(this, this.serverData))
            )
            .bounds(this.centerButton(0, 3), row(0), 100, 20)
            .build()
      );
      this.settingsButton = this.addRenderableWidget(
         Button.builder(
               Component.translatable("mco.configure.world.buttons.settings"),
               p_280716_ -> this.minecraft.setScreen(new RealmsSettingsScreen(this, this.serverData.clone()))
            )
            .bounds(this.centerButton(1, 3), row(0), 100, 20)
            .build()
      );
      this.subscriptionButton = this.addRenderableWidget(
         Button.builder(
               Component.translatable("mco.configure.world.buttons.subscription"),
               p_280725_ -> this.minecraft.setScreen(new RealmsSubscriptionInfoScreen(this, this.serverData.clone(), this.lastScreen))
            )
            .bounds(this.centerButton(2, 3), row(0), 100, 20)
            .build()
      );
      this.slotButtonList.clear();

      for (int $$0 = 1; $$0 < 5; $$0++) {
         this.slotButtonList.add(this.addSlotButton($$0));
      }

      this.switchMinigameButton = this.addRenderableWidget(
         Button.builder(
               Component.translatable("mco.configure.world.buttons.switchminigame"),
               p_280711_ -> this.minecraft
                  .setScreen(
                     new RealmsSelectWorldTemplateScreen(
                        Component.translatable("mco.template.title.minigame"), this::templateSelectionCallback, RealmsServer.WorldType.MINIGAME
                     )
                  )
            )
            .bounds(this.leftButton(0), row(13) - 5, 100, 20)
            .build()
      );
      this.optionsButton = this.addRenderableWidget(
         Button.builder(
               Component.translatable("mco.configure.world.buttons.options"),
               p_280720_ -> this.minecraft
                  .setScreen(
                     new RealmsSlotOptionsScreen(
                        this, this.serverData.slots.get(this.serverData.activeSlot).clone(), this.serverData.worldType, this.serverData.activeSlot
                     )
                  )
            )
            .bounds(this.leftButton(0), row(13) - 5, 90, 20)
            .build()
      );
      this.backupButton = this.addRenderableWidget(
         Button.builder(
               Component.translatable("mco.configure.world.backup"),
               p_280715_ -> this.minecraft.setScreen(new RealmsBackupScreen(this, this.serverData.clone(), this.serverData.activeSlot))
            )
            .bounds(this.leftButton(1), row(13) - 5, 90, 20)
            .build()
      );
      this.resetWorldButton = this.addRenderableWidget(
         Button.builder(
               Component.translatable("mco.configure.world.buttons.resetworld"),
               p_300627_ -> this.minecraft
                  .setScreen(
                     RealmsResetWorldScreen.forResetSlot(
                        this, this.serverData.clone(), () -> this.minecraft.execute(() -> this.minecraft.setScreen(this.getNewScreen()))
                     )
                  )
            )
            .bounds(this.leftButton(2), row(13) - 5, 90, 20)
            .build()
      );
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, p_307024_ -> this.onClose()).bounds(this.rightX - 80 + 8, row(13) - 5, 70, 20).build());
      this.backupButton.active = true;
      if (this.serverData == null) {
         this.hideMinigameButtons();
         this.hideRegularButtons();
         this.playersButton.active = false;
         this.settingsButton.active = false;
         this.subscriptionButton.active = false;
      } else {
         this.disableButtons();
         if (this.isMinigame()) {
            this.hideRegularButtons();
         } else {
            this.hideMinigameButtons();
         }
      }
   }

   private RealmsWorldSlotButton addSlotButton(int p_167386_) {
      int $$1 = this.frame(p_167386_);
      int $$2 = row(5) + 5;
      RealmsWorldSlotButton $$3 = new RealmsWorldSlotButton($$1, $$2, 80, 80, p_167386_, p_339255_ -> {
         RealmsWorldSlotButton.State $$2x = ((RealmsWorldSlotButton)p_339255_).getState();
         if ($$2x != null) {
            switch ($$2x.action) {
               case NOTHING:
                  break;
               case JOIN:
                  this.joinRealm(this.serverData);
                  break;
               case SWITCH_SLOT:
                  if ($$2x.minigame) {
                     this.switchToMinigame();
                  } else if ($$2x.empty) {
                     this.switchToEmptySlot(p_167386_, this.serverData);
                  } else {
                     this.switchToFullSlot(p_167386_, this.serverData);
                  }
                  break;
               default:
                  throw new IllegalStateException("Unknown action " + $$2x.action);
            }
         }
      });
      if (this.serverData != null) {
         $$3.setServerData(this.serverData);
      }

      return this.addRenderableWidget($$3);
   }

   private int leftButton(int p_88464_) {
      return this.leftX + p_88464_ * 95;
   }

   private int centerButton(int p_88466_, int p_88467_) {
      return this.width / 2 - (p_88467_ * 105 - 5) / 2 + p_88466_ * 105;
   }

   @Override
   public void render(GuiGraphics p_282982_, int p_281739_, int p_283097_, float p_282528_) {
      super.render(p_282982_, p_281739_, p_283097_, p_282528_);
      this.toolTip = null;
      p_282982_.drawCenteredString(this.font, WORLD_LIST_TITLE, this.width / 2, row(4), -1);
      if (this.serverData == null) {
         p_282982_.drawCenteredString(this.font, this.title, this.width / 2, 17, -1);
      } else {
         String $$4 = this.serverData.getName();
         int $$5 = this.font.width($$4);
         int $$6 = this.serverData.state == RealmsServer.State.CLOSED ? -6250336 : 8388479;
         int $$7 = this.font.width(this.title);
         p_282982_.drawCenteredString(this.font, this.title, this.width / 2, 12, -1);
         p_282982_.drawCenteredString(this.font, $$4, this.width / 2, 24, $$6);
         int $$8 = Math.min(this.centerButton(2, 3) + 80 - 11, this.width / 2 + $$5 / 2 + $$7 / 2 + 10);
         this.drawServerStatus(p_282982_, $$8, 7, p_281739_, p_283097_);
         if (this.isMinigame()) {
            String $$9 = this.serverData.getMinigameName();
            if ($$9 != null) {
               p_282982_.drawString(this.font, Component.translatable("mco.configure.world.minigame", $$9), this.leftX + 80 + 20 + 10, row(13), -1, false);
            }
         }
      }
   }

   private int frame(int p_88488_) {
      return this.leftX + (p_88488_ - 1) * 98;
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
      if (this.stateChanged) {
         this.lastScreen.resetScreen();
      }
   }

   private void fetchServerData(long p_88427_) {
      new Thread(() -> {
         RealmsClient $$1 = RealmsClient.create();

         try {
            RealmsServer $$2 = $$1.getOwnRealm(p_88427_);
            this.minecraft.execute(() -> {
               this.serverData = $$2;
               this.disableButtons();
               if (this.isMinigame()) {
                  this.show(this.switchMinigameButton);
               } else {
                  this.show(this.optionsButton);
                  this.show(this.backupButton);
                  this.show(this.resetWorldButton);
               }

               for (RealmsWorldSlotButton $$1x : this.slotButtonList) {
                  $$1x.setServerData($$2);
               }
            });
         } catch (RealmsServiceException var5) {
            LOGGER.error("Couldn't get own world", var5);
            this.minecraft.execute(() -> this.minecraft.setScreen(new RealmsGenericErrorScreen(var5, this.lastScreen)));
         }
      }).start();
   }

   private void disableButtons() {
      this.playersButton.active = !this.serverData.expired;
      this.settingsButton.active = !this.serverData.expired;
      this.subscriptionButton.active = true;
      this.switchMinigameButton.active = !this.serverData.expired;
      this.optionsButton.active = !this.serverData.expired;
      this.resetWorldButton.active = !this.serverData.expired;
   }

   private void joinRealm(RealmsServer p_88439_) {
      if (this.serverData.state == RealmsServer.State.OPEN) {
         RealmsMainScreen.play(p_88439_, this);
      } else {
         this.openTheWorld(true);
      }
   }

   private void switchToMinigame() {
      RealmsSelectWorldTemplateScreen $$0 = new RealmsSelectWorldTemplateScreen(
         Component.translatable("mco.template.title.minigame"), this::templateSelectionCallback, RealmsServer.WorldType.MINIGAME
      );
      $$0.setWarning(Component.translatable("mco.minigame.world.info.line1"), Component.translatable("mco.minigame.world.info.line2"));
      this.minecraft.setScreen($$0);
   }

   private void switchToFullSlot(int p_88421_, RealmsServer p_88422_) {
      this.minecraft
         .setScreen(
            RealmsPopups.infoPopupScreen(
               this,
               Component.translatable("mco.configure.world.slot.switch.question.line1"),
               p_344123_ -> {
                  this.stateChanged();
                  this.minecraft
                     .setScreen(
                        new RealmsLongRunningMcoTaskScreen(
                           this.lastScreen,
                           new SwitchSlotTask(p_88422_.id, p_88421_, () -> this.minecraft.execute(() -> this.minecraft.setScreen(this.getNewScreen())))
                        )
                     );
               }
            )
         );
   }

   private void switchToEmptySlot(int p_88469_, RealmsServer p_88470_) {
      this.minecraft
         .setScreen(
            RealmsPopups.infoPopupScreen(
               this,
               Component.translatable("mco.configure.world.slot.switch.question.line1"),
               p_344126_ -> {
                  this.stateChanged();
                  RealmsResetWorldScreen $$3 = RealmsResetWorldScreen.forEmptySlot(
                     this, p_88469_, p_88470_, () -> this.minecraft.execute(() -> this.minecraft.setScreen(this.getNewScreen()))
                  );
                  this.minecraft.setScreen($$3);
               }
            )
         );
   }

   private void drawServerStatus(GuiGraphics p_281709_, int p_88491_, int p_88492_, int p_88493_, int p_88494_) {
      if (this.serverData.expired) {
         this.drawRealmStatus(p_281709_, p_88491_, p_88492_, p_88493_, p_88494_, EXPIRED_SPRITE, () -> SERVER_EXPIRED_TOOLTIP);
      } else if (this.serverData.state == RealmsServer.State.CLOSED) {
         this.drawRealmStatus(p_281709_, p_88491_, p_88492_, p_88493_, p_88494_, CLOSED_SPRITE, () -> SERVER_CLOSED_TOOLTIP);
      } else if (this.serverData.state == RealmsServer.State.OPEN) {
         if (this.serverData.daysLeft < 7) {
            this.drawRealmStatus(
               p_281709_,
               p_88491_,
               p_88492_,
               p_88493_,
               p_88494_,
               EXPIRES_SOON_SPRITE,
               () -> {
                  if (this.serverData.daysLeft <= 0) {
                     return SERVER_EXPIRING_SOON_TOOLTIP;
                  } else {
                     return (Component)(this.serverData.daysLeft == 1
                        ? SERVER_EXPIRING_IN_DAY_TOOLTIP
                        : Component.translatable("mco.selectServer.expires.days", this.serverData.daysLeft));
                  }
               }
            );
         } else {
            this.drawRealmStatus(p_281709_, p_88491_, p_88492_, p_88493_, p_88494_, OPEN_SPRITE, () -> SERVER_OPEN_TOOLTIP);
         }
      }
   }

   private void drawRealmStatus(
      GuiGraphics p_299311_, int p_298280_, int p_298913_, int p_299030_, int p_299045_, ResourceLocation p_299141_, Supplier<Component> p_298702_
   ) {
      p_299311_.blitSprite(p_299141_, p_298280_, p_298913_, 10, 28);
      if (p_299030_ >= p_298280_ && p_299030_ <= p_298280_ + 9 && p_299045_ >= p_298913_ && p_299045_ <= p_298913_ + 27) {
         this.setTooltipForNextRenderPass(p_298702_.get());
      }
   }

   private boolean isMinigame() {
      return this.serverData != null && this.serverData.isMinigameActive();
   }

   private void hideRegularButtons() {
      this.hide(this.optionsButton);
      this.hide(this.backupButton);
      this.hide(this.resetWorldButton);
   }

   private void hide(Button p_88451_) {
      p_88451_.visible = false;
   }

   private void show(Button p_88485_) {
      p_88485_.visible = true;
   }

   private void hideMinigameButtons() {
      this.hide(this.switchMinigameButton);
   }

   public void saveSlotSettings(RealmsWorldOptions p_88445_) {
      RealmsWorldOptions $$1 = this.serverData.slots.get(this.serverData.activeSlot);
      p_88445_.templateId = $$1.templateId;
      p_88445_.templateImage = $$1.templateImage;
      RealmsClient $$2 = RealmsClient.create();

      try {
         $$2.updateSlot(this.serverData.id, this.serverData.activeSlot, p_88445_);
         this.serverData.slots.put(this.serverData.activeSlot, p_88445_);
      } catch (RealmsServiceException var5) {
         LOGGER.error("Couldn't save slot settings", var5);
         this.minecraft.setScreen(new RealmsGenericErrorScreen(var5, this));
         return;
      }

      this.minecraft.setScreen(this);
   }

   public void saveSettings(String p_88455_, String p_88456_) {
      String $$2 = StringUtil.isBlank(p_88456_) ? null : p_88456_;
      RealmsClient $$3 = RealmsClient.create();

      try {
         $$3.update(this.serverData.id, p_88455_, $$2);
         this.serverData.setName(p_88455_);
         this.serverData.setDescription($$2);
         this.stateChanged();
      } catch (RealmsServiceException var6) {
         LOGGER.error("Couldn't save settings", var6);
         this.minecraft.setScreen(new RealmsGenericErrorScreen(var6, this));
         return;
      }

      this.minecraft.setScreen(this);
   }

   public void openTheWorld(boolean p_88460_) {
      RealmsConfigureWorldScreen $$1 = this.getNewScreen();
      this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen($$1, new OpenServerTask(this.serverData, $$1, p_88460_, this.minecraft)));
   }

   public void closeTheWorld() {
      RealmsConfigureWorldScreen $$0 = this.getNewScreen();
      this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen($$0, new CloseServerTask(this.serverData, $$0)));
   }

   public void stateChanged() {
      this.stateChanged = true;
   }

   private void templateSelectionCallback(@Nullable WorldTemplate p_167395_) {
      if (p_167395_ != null && WorldTemplate.WorldTemplateType.MINIGAME == p_167395_.type) {
         this.stateChanged();
         this.minecraft
            .setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new SwitchMinigameTask(this.serverData.id, p_167395_, this.getNewScreen())));
      } else {
         this.minecraft.setScreen(this);
      }
   }

   public RealmsConfigureWorldScreen getNewScreen() {
      RealmsConfigureWorldScreen $$0 = new RealmsConfigureWorldScreen(this.lastScreen, this.serverId);
      $$0.stateChanged = this.stateChanged;
      return $$0;
   }
}
