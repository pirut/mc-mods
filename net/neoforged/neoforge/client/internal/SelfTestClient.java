package net.neoforged.neoforge.client.internal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.SelfTest;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class SelfTestClient {
   public static void initClient() {
      String clientSelfTestDestination = System.getenv("NEOFORGE_CLIENT_SELFTEST");
      if (clientSelfTestDestination != null) {
         NeoForge.EVENT_BUS.addListener(e -> {
            if (!(Minecraft.getInstance().getOverlay() instanceof LoadingOverlay)) {
               if (Minecraft.getInstance().isRunning()) {
                  SelfTest.writeSelfTestReport(clientSelfTestDestination);
                  Minecraft.getInstance().stop();
               }
            }
         });
      }
   }
}
