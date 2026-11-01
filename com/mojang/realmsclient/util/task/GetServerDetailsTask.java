package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerAddress;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.RealmsBrokenWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTickTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.gui.screens.RealmsTermsScreen;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
      RealmsServerAddress $$0;
      try {
         $$0 = this.fetchServerAddress();
      } catch (CancellationException var4) {
         LOGGER.info("User aborted connecting to realms");
         return;
      } catch (RealmsServiceException var5) {
         switch (var5.realmsError.errorCode()) {
            case 6002:
               setScreen(new RealmsTermsScreen(this.lastScreen, this.server));
               return;
            case 6006:
               boolean $$3 = Minecraft.getInstance().isLocalPlayer(this.server.ownerUUID);
               setScreen(
                  (Screen)($$3
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

      boolean $$7 = $$0.resourcePackUrl != null && $$0.resourcePackHash != null;
      Screen $$8 = (Screen)($$7 ? this.resourcePackDownloadConfirmationScreen($$0, generatePackId(this.server), this::connectScreen) : this.connectScreen($$0));
      setScreen($$8);
   }

   private static UUID generatePackId(RealmsServer p_314978_) {
      return p_314978_.minigameName != null
         ? UUID.nameUUIDFromBytes(("minigame:" + p_314978_.minigameName).getBytes(StandardCharsets.UTF_8))
         : UUID.nameUUIDFromBytes(("realms:" + p_314978_.name + ":" + p_314978_.activeSlot).getBytes(StandardCharsets.UTF_8));
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }

   private RealmsServerAddress fetchServerAddress() throws RealmsServiceException, TimeoutException, CancellationException {
      RealmsClient $$0 = RealmsClient.create();

      for (int $$1 = 0; $$1 < 40; $$1++) {
         if (this.aborted()) {
            throw new CancellationException();
         }

         try {
            return $$0.join(this.server.id);
         } catch (RetryCallException var4) {
            pause(var4.delaySeconds);
         }
      }

      throw new TimeoutException();
   }

   public RealmsLongRunningMcoTaskScreen connectScreen(RealmsServerAddress p_167638_) {
      return new RealmsLongRunningMcoTickTaskScreen(this.lastScreen, new ConnectTask(this.lastScreen, this.server, p_167638_));
   }

   private PopupScreen resourcePackDownloadConfirmationScreen(RealmsServerAddress p_167640_, UUID p_315014_, Function<RealmsServerAddress, Screen> p_167641_) {
      Component $$3 = Component.translatable("mco.configure.world.resourcepack.question");
      return RealmsPopups.infoPopupScreen(this.lastScreen, $$3, p_344136_ -> {
         setScreen(new GenericMessageScreen(APPLYING_PACK_TEXT));
         this.scheduleResourcePackDownload(p_167640_, p_315014_).thenRun(() -> setScreen(p_167641_.apply(p_167640_))).exceptionally(p_314377_ -> {
            Minecraft.getInstance().getDownloadedPackSource().cleanupAfterDisconnect();
            LOGGER.error("Failed to download resource pack from {}", p_167640_, p_314377_);
            setScreen(new RealmsGenericErrorScreen(Component.translatable("mco.download.resourcePack.fail"), this.lastScreen));
            return null;
         });
      });
   }

   private CompletableFuture<?> scheduleResourcePackDownload(RealmsServerAddress p_167652_, UUID p_314972_) {
      try {
         DownloadedPackSource $$2 = Minecraft.getInstance().getDownloadedPackSource();
         CompletableFuture<Void> $$3 = $$2.waitForPackFeedback(p_314972_);
         $$2.allowServerPacks();
         $$2.pushPack(p_314972_, new URL(p_167652_.resourcePackUrl), p_167652_.resourcePackHash);
         return $$3;
      } catch (Exception var5) {
         CompletableFuture<Void> $$5 = new CompletableFuture<>();
         $$5.completeExceptionally(var5);
         return $$5;
      }
   }
}
