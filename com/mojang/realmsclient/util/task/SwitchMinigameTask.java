package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SwitchMinigameTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.minigame.world.starting.screen.title");
   private final long realmId;
   private final WorldTemplate worldTemplate;
   private final RealmsConfigureWorldScreen lastScreen;

   public SwitchMinigameTask(long p_90451_, WorldTemplate p_90452_, RealmsConfigureWorldScreen p_90453_) {
      this.realmId = p_90451_;
      this.worldTemplate = p_90452_;
      this.lastScreen = p_90453_;
   }

   @Override
   public void run() {
      RealmsClient $$0 = RealmsClient.create();

      for (int $$1 = 0; $$1 < 25; $$1++) {
         try {
            if (this.aborted()) {
               return;
            }

            if ($$0.putIntoMinigameMode(this.realmId, this.worldTemplate.id)) {
               setScreen(this.lastScreen);
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

            LOGGER.error("Couldn't start mini game!");
            this.error(var5);
         }
      }
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }
}
