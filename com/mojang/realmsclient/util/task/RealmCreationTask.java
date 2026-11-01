package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmCreationTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.create.world.wait");
   private final String name;
   private final String motd;
   private final long realmId;

   public RealmCreationTask(long p_320686_, String p_319849_, String p_320750_) {
      this.realmId = p_320686_;
      this.name = p_319849_;
      this.motd = p_320750_;
   }

   @Override
   public void run() {
      RealmsClient realmsclient = RealmsClient.getOrCreate();

      try {
         realmsclient.initializeRealm(this.realmId, this.name, this.motd);
      } catch (RealmsServiceException var3) {
         LOGGER.error("Couldn't create world", var3);
         this.error(var3);
      } catch (Exception var4) {
         LOGGER.error("Could not create world", var4);
         this.error(var4);
      }
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }
}
