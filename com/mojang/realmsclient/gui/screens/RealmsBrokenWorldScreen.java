package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsWorldSlotButton;
import com.mojang.realmsclient.util.RealmsTextureManager;
import com.mojang.realmsclient.util.task.OpenServerTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsBrokenWorldScreen extends RealmsScreen {
   private static final ResourceLocation SLOT_FRAME_SPRITE = ResourceLocation.withDefaultNamespace("widget/slot_frame");
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int DEFAULT_BUTTON_WIDTH = 80;
   private final Screen lastScreen;
   @Nullable
   private RealmsServer serverData;
   private final long serverId;
   private final Component[] message = new Component[]{
      Component.translatable("mco.brokenworld.message.line1"), Component.translatable("mco.brokenworld.message.line2")
   };
   private int leftX;
   private final List<Integer> slotsThatHasBeenDownloaded = Lists.newArrayList();
   private int animTick;

   public RealmsBrokenWorldScreen(Screen p_88296_, long p_88298_, boolean p_88299_) {
      super(p_88299_ ? Component.translatable("mco.brokenworld.minigame.title") : Component.translatable("mco.brokenworld.title"));
      this.lastScreen = p_88296_;
      this.serverId = p_88298_;
   }

   @Override
   public void init() {
      this.leftX = this.width / 2 - 150;
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_BACK, p_300624_ -> this.onClose()).bounds((this.width - 150) / 2, row(13) - 5, 150, 20).build()
      );
      if (this.serverData == null) {
         this.fetchServerData(this.serverId);
      } else {
         this.addButtons();
      }
   }

   @Override
   public Component getNarrationMessage() {
      return ComponentUtils.formatList(Stream.concat(Stream.of(this.title), Stream.of(this.message)).collect(Collectors.toList()), CommonComponents.SPACE);
   }

   private void addButtons() {
      for (Entry<Integer, RealmsWorldOptions> $$0 : this.serverData.slots.entrySet()) {
         int $$1 = $$0.getKey();
         boolean $$2 = $$1 != this.serverData.activeSlot || this.serverData.isMinigameActive();
         Button $$3;
         if ($$2) {
            $$3 = Button.builder(
                  Component.translatable("mco.brokenworld.play"),
                  p_305620_ -> this.minecraft
                     .setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new SwitchSlotTask(this.serverData.id, $$1, this::doSwitchOrReset)))
               )
               .bounds(this.getFramePositionX($$1), row(8), 80, 20)
               .build();
            $$3.active = !this.serverData.slots.get($$1).empty;
         } else {
            $$3 = Button.builder(
                  Component.translatable("mco.brokenworld.download"),
                  p_344120_ -> this.minecraft
                     .setScreen(
                        RealmsPopups.infoPopupScreen(
                           this, Component.translatable("mco.configure.world.restore.download.question.line1"), p_344118_ -> this.downloadWorld($$1)
                        )
                     )
               )
               .bounds(this.getFramePositionX($$1), row(8), 80, 20)
               .build();
         }

         if (this.slotsThatHasBeenDownloaded.contains($$1)) {
            $$3.active = false;
            $$3.setMessage(Component.translatable("mco.brokenworld.downloaded"));
         }

         this.addRenderableWidget($$3);
      }
   }

   @Override
   public void tick() {
      this.animTick++;
   }

   @Override
   public void render(GuiGraphics p_282934_, int p_88317_, int p_88318_, float p_88319_) {
      super.render(p_282934_, p_88317_, p_88318_, p_88319_);
      p_282934_.drawCenteredString(this.font, this.title, this.width / 2, 17, -1);

      for (int $$4 = 0; $$4 < this.message.length; $$4++) {
         p_282934_.drawCenteredString(this.font, this.message[$$4], this.width / 2, row(-1) + 3 + $$4 * 12, -6250336);
      }

      if (this.serverData != null) {
         for (Entry<Integer, RealmsWorldOptions> $$5 : this.serverData.slots.entrySet()) {
            if ($$5.getValue().templateImage != null && $$5.getValue().templateId != -1L) {
               this.drawSlotFrame(
                  p_282934_,
                  this.getFramePositionX($$5.getKey()),
                  row(1) + 5,
                  p_88317_,
                  p_88318_,
                  this.serverData.activeSlot == $$5.getKey() && !this.isMinigame(),
                  $$5.getValue().getSlotName($$5.getKey()),
                  $$5.getKey(),
                  $$5.getValue().templateId,
                  $$5.getValue().templateImage,
                  $$5.getValue().empty
               );
            } else {
               this.drawSlotFrame(
                  p_282934_,
                  this.getFramePositionX($$5.getKey()),
                  row(1) + 5,
                  p_88317_,
                  p_88318_,
                  this.serverData.activeSlot == $$5.getKey() && !this.isMinigame(),
                  $$5.getValue().getSlotName($$5.getKey()),
                  $$5.getKey(),
                  -1L,
                  null,
                  $$5.getValue().empty
               );
            }
         }
      }
   }

   private int getFramePositionX(int p_88302_) {
      return this.leftX + (p_88302_ - 1) * 110;
   }

   private void fetchServerData(long p_88314_) {
      new Thread(() -> {
         RealmsClient $$1 = RealmsClient.create();

         try {
            this.serverData = $$1.getOwnRealm(p_88314_);
            this.addButtons();
         } catch (RealmsServiceException var5) {
            LOGGER.error("Couldn't get own world", var5);
            this.minecraft.setScreen(new RealmsGenericErrorScreen(var5, this.lastScreen));
         }
      }).start();
   }

   public void doSwitchOrReset() {
      new Thread(
            () -> {
               RealmsClient $$0 = RealmsClient.create();
               if (this.serverData.state == RealmsServer.State.CLOSED) {
                  this.minecraft
                     .execute(
                        () -> this.minecraft
                           .setScreen(new RealmsLongRunningMcoTaskScreen(this, new OpenServerTask(this.serverData, this, true, this.minecraft)))
                     );
               } else {
                  try {
                     RealmsServer $$1 = $$0.getOwnRealm(this.serverId);
                     this.minecraft.execute(() -> RealmsMainScreen.play($$1, this));
                  } catch (RealmsServiceException var3) {
                     LOGGER.error("Couldn't get own world", var3);
                     this.minecraft.execute(() -> this.minecraft.setScreen(this.lastScreen));
                  }
               }
            }
         )
         .start();
   }

   private void downloadWorld(int p_88336_) {
      RealmsClient $$1 = RealmsClient.create();

      try {
         WorldDownload $$2 = $$1.requestDownloadInfo(this.serverData.id, p_88336_);
         RealmsDownloadLatestWorldScreen $$3 = new RealmsDownloadLatestWorldScreen(this, $$2, this.serverData.getWorldName(p_88336_), p_329645_ -> {
            if (p_329645_) {
               this.slotsThatHasBeenDownloaded.add(p_88336_);
               this.clearWidgets();
               this.addButtons();
            } else {
               this.minecraft.setScreen(this);
            }
         });
         this.minecraft.setScreen($$3);
      } catch (RealmsServiceException var5) {
         LOGGER.error("Couldn't download world data", var5);
         this.minecraft.setScreen(new RealmsGenericErrorScreen(var5, this));
      }
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   private boolean isMinigame() {
      return this.serverData != null && this.serverData.isMinigameActive();
   }

   private void drawSlotFrame(
      GuiGraphics p_281929_,
      int p_283393_,
      int p_281553_,
      int p_283523_,
      int p_282823_,
      boolean p_283032_,
      String p_283498_,
      int p_283330_,
      long p_283588_,
      @Nullable String p_282484_,
      boolean p_282283_
   ) {
      ResourceLocation $$11;
      if (p_282283_) {
         $$11 = RealmsWorldSlotButton.EMPTY_SLOT_LOCATION;
      } else if (p_282484_ != null && p_283588_ != -1L) {
         $$11 = RealmsTextureManager.worldTemplate(String.valueOf(p_283588_), p_282484_);
      } else if (p_283330_ == 1) {
         $$11 = RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_1;
      } else if (p_283330_ == 2) {
         $$11 = RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_2;
      } else if (p_283330_ == 3) {
         $$11 = RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_3;
      } else {
         $$11 = RealmsTextureManager.worldTemplate(String.valueOf(this.serverData.minigameId), this.serverData.minigameImage);
      }

      if (!p_283032_) {
         p_281929_.setColor(0.56F, 0.56F, 0.56F, 1.0F);
      } else if (p_283032_) {
         float $$17 = 0.9F + 0.1F * Mth.cos(this.animTick * 0.2F);
         p_281929_.setColor($$17, $$17, $$17, 1.0F);
      }

      p_281929_.blit($$11, p_283393_ + 3, p_281553_ + 3, 0.0F, 0.0F, 74, 74, 74, 74);
      if (p_283032_) {
         p_281929_.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      } else {
         p_281929_.setColor(0.56F, 0.56F, 0.56F, 1.0F);
      }

      p_281929_.blitSprite(SLOT_FRAME_SPRITE, p_283393_, p_281553_, 80, 80);
      p_281929_.drawCenteredString(this.font, p_283498_, p_283393_ + 40, p_281553_ + 66, -1);
      p_281929_.setColor(1.0F, 1.0F, 1.0F, 1.0F);
   }
}
