package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class OpenServerTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.configure.world.opening");
   private final RealmsServer serverData;
   private final Screen returnScreen;
   private final boolean join;
   private final Minecraft minecraft;

   public OpenServerTask(RealmsServer p_181344_, Screen p_181345_, boolean p_181347_, Minecraft p_181348_) {
      this.serverData = p_181344_;
      this.returnScreen = p_181345_;
      this.join = p_181347_;
      this.minecraft = p_181348_;
   }

   @Override
   public void run() {
      RealmsClient $$0 = RealmsClient.create();

      for (int $$1 = 0; $$1 < 25; $$1++) {
         if (this.aborted()) {
            return;
         }

         try {
            boolean $$2 = $$0.open(this.serverData.id);
            if ($$2) {
               this.minecraft.execute(() -> {
                  if (this.returnScreen instanceof RealmsConfigureWorldScreen) {
                     ((RealmsConfigureWorldScreen)this.returnScreen).stateChanged();
                  }

                  this.serverData.state = RealmsServer.State.OPEN;
                  if (this.join) {
                     RealmsMainScreen.play(this.serverData, this.returnScreen);
                  } else {
                     this.minecraft.setScreen(this.returnScreen);
                  }
               });
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

            LOGGER.error("Failed to open server", var5);
            this.error(var5);
         }
      }
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }
}
