package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.RealmsDownloadLatestWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class DownloadTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.download.preparing");
   private final long realmId;
   private final int slot;
   private final Screen lastScreen;
   private final String downloadName;

   public DownloadTask(long p_90320_, int p_90321_, String p_90322_, Screen p_90323_) {
      this.realmId = p_90320_;
      this.slot = p_90321_;
      this.lastScreen = p_90323_;
      this.downloadName = p_90322_;
   }

   @Override
   public void run() {
      RealmsClient $$0 = RealmsClient.create();
      int $$1 = 0;

      while ($$1 < 25) {
         try {
            if (this.aborted()) {
               return;
            }

            WorldDownload $$2 = $$0.requestDownloadInfo(this.realmId, this.slot);
            pause(1L);
            if (this.aborted()) {
               return;
            }

            setScreen(new RealmsDownloadLatestWorldScreen(this.lastScreen, $$2, this.downloadName, p_90325_ -> {}));
            return;
         } catch (RetryCallException var4) {
            if (this.aborted()) {
               return;
            }

            pause(var4.delaySeconds);
            $$1++;
         } catch (RealmsServiceException var5) {
            if (this.aborted()) {
               return;
            }

            LOGGER.error("Couldn't download world data", var5);
            setScreen(new RealmsGenericErrorScreen(var5, this.lastScreen));
            return;
         } catch (Exception var6) {
            if (this.aborted()) {
               return;
            }

            LOGGER.error("Couldn't download world data", var6);
            this.error(var6);
            return;
         }
      }
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }
}
