package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsJoinInformation;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.RealmsBrokenWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoConnectTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.gui.screens.RealmsTermsScreen;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GetServerDetailsTask extends LongRunningTask {
   private static final Component APPLYING_PACK_TEXT = Component.translatable("multiplayer.applyingPack");
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.connect.connecting");
   private final RealmsServer server;
   private final Screen lastScreen;

   public GetServerDetailsTask(Screen p_90333_, RealmsServer p_90334_) {
      this.lastScreen = p_90333_;
      this.server = p_90334_;
   }

   @Override
   public void run() {
      RealmsJoinInformation realmsjoininformation;
      try {
         realmsjoininformation = this.fetchServerAddress();
      } catch (CancellationException var4) {
         LOGGER.info("User aborted connecting to realms");
         return;
      } catch (RealmsServiceException var5) {
         switch (var5.realmsError.errorCode()) {
            case 6002:
               setScreen(new RealmsTermsScreen(this.lastScreen, this.server));
               return;
            case 6006:
               boolean flag1 = Minecraft.getInstance().isLocalPlayer(this.server.ownerUUID);
               setScreen(
                  (Screen)(flag1
                     ? new RealmsBrokenWorldScreen(this.lastScreen, this.server.id, this.server.isMinigameActive())
                     : new RealmsGenericErrorScreen(
                        Component.translatable("mco.brokenworld.nonowner.title"), Component.translatable("mco.brokenworld.nonowner.error"), this.lastScreen
                     ))
               );
               return;
            default:
               this.error(var5);
               LOGGER.error("Couldn't connect to world", var5);
               return;
         }
      } catch (TimeoutException var6) {
         this.error(Component.translatable("mco.errorMessage.connectionFailure"));
         return;
      } catch (Exception var7) {
         LOGGER.error("Couldn't connect to world", var7);
         this.error(var7);
         return;
      }

      if (realmsjoininformation.address() == null) {
         this.error(Component.translatable("mco.errorMessage.connectionFailure"));
      } else {
         boolean flag = realmsjoininformation.resourcePackUrl() != null && realmsjoininformation.resourcePackHash() != null;
         Screen screen = (Screen)(flag
            ? this.resourcePackDownloadConfirmationScreen(realmsjoininformation, generatePackId(this.server), this::connectScreen)
            : this.connectScreen(realmsjoininformation));
         setScreen(screen);
      }
   }

   private static UUID generatePackId(RealmsServer p_314978_) {
      return p_314978_.minigameName != null
         ? UUID.nameUUIDFromBytes(("minigame:" + p_314978_.minigameName).getBytes(StandardCharsets.UTF_8))
         : UUID.nameUUIDFromBytes(("realms:" + Objects.requireNonNullElse(p_314978_.name, "") + ":" + p_314978_.activeSlot).getBytes(StandardCharsets.UTF_8));
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }

   private RealmsJoinInformation fetchServerAddress() throws RealmsServiceException, TimeoutException, CancellationException {
      RealmsClient realmsclient = RealmsClient.getOrCreate();

      for (int i = 0; i < 40; i++) {
         if (this.aborted()) {
            throw new CancellationException();
         }

         try {
            return realmsclient.join(this.server.id);
         } catch (RetryCallException var4) {
            pause(var4.delaySeconds);
         }
      }

      throw new TimeoutException();
   }

   public RealmsLongRunningMcoTaskScreen connectScreen(RealmsJoinInformation p_425842_) {
      return new RealmsLongRunningMcoConnectTaskScreen(this.lastScreen, p_425842_, new ConnectTask(this.lastScreen, this.server, p_425842_));
   }

   private PopupScreen resourcePackDownloadConfirmationScreen(
      RealmsJoinInformation p_426203_, UUID p_315014_, Function<RealmsJoinInformation, Screen> p_167641_
   ) {
      Component component = Component.translatable("mco.configure.world.resourcepack.question");
      return RealmsPopups.infoPopupScreen(this.lastScreen, component, p_425129_ -> {
         setScreen(new GenericMessageScreen(APPLYING_PACK_TEXT));
         this.scheduleResourcePackDownload(p_426203_, p_315014_).thenRun(() -> setScreen(p_167641_.apply(p_426203_))).exceptionally(p_314377_ -> {
            Minecraft.getInstance().getDownloadedPackSource().cleanupAfterDisconnect();
            LOGGER.error("Failed to download resource pack from {}", p_426203_, p_314377_);
            setScreen(new RealmsGenericErrorScreen(Component.translatable("mco.download.resourcePack.fail"), this.lastScreen));
            return null;
         });
      });
   }

   private CompletableFuture<?> scheduleResourcePackDownload(RealmsJoinInformation p_425828_, UUID p_314972_) {
      try {
         if (p_425828_.resourcePackUrl() == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("resourcePackUrl was null"));
         } else if (p_425828_.resourcePackHash() == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("resourcePackHash was null"));
         } else {
            DownloadedPackSource downloadedpacksource = Minecraft.getInstance().getDownloadedPackSource();
            CompletableFuture<Void> completablefuture = downloadedpacksource.waitForPackFeedback(p_314972_);
            downloadedpacksource.allowServerPacks();
            downloadedpacksource.pushPack(p_314972_, new URL(p_425828_.resourcePackUrl()), p_425828_.resourcePackHash());
            return completablefuture;
         }
      } catch (Exception var5) {
         return CompletableFuture.failedFuture(var5);
      }
   }
}
