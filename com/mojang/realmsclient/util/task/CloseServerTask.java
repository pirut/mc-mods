package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class CloseServerTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.configure.world.closing");
   private final RealmsServer serverData;
   private final RealmsConfigureWorldScreen configureScreen;

   public CloseServerTask(RealmsServer p_90302_, RealmsConfigureWorldScreen p_90303_) {
      this.serverData = p_90302_;
      this.configureScreen = p_90303_;
   }

   @Override
   public void run() {
      RealmsClient $$0 = RealmsClient.create();

      for (int $$1 = 0; $$1 < 25; $$1++) {
         if (this.aborted()) {
            return;
         }

         try {
            boolean $$2 = $$0.close(this.serverData.id);
            if ($$2) {
               this.configureScreen.stateChanged();
               this.serverData.state = RealmsServer.State.CLOSED;
               setScreen(this.configureScreen);
               break;
            }
         } catch (RetryCallException var4) {
            if (this.aborted()) {
               return;
            }

            pause(var4.delaySeconds);
         } catch (Exception var5) {
            if (this.aborted()) {
               return;
            }

            LOGGER.error("Failed to close server", var5);
            this.error(var5);
         }
      }
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }
}
