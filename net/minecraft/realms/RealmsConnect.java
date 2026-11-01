package net.minecraft.realms;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.RealmsServer;
import java.net.InetSocketAddress;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.client.resources.server.ServerPackManager;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsConnect {
   static final Logger LOGGER = LogUtils.getLogger();
   final Screen onlineScreen;
   volatile boolean aborted;
   @Nullable
   Connection connection;

   public RealmsConnect(Screen p_120693_) {
      this.onlineScreen = p_120693_;
   }

   public void connect(final RealmsServer p_175032_, ServerAddress p_175033_) {
      final Minecraft $$2 = Minecraft.getInstance();
      $$2.prepareForMultiplayer();
      $$2.getNarrator().sayNow(Component.translatable("mco.connect.success"));
      final String $$3 = p_175033_.getHost();
      final int $$4 = p_175033_.getPort();
      (new Thread("Realms-connect-task") {
            @Override
            public void run() {
               InetSocketAddress $$0 = null;

               try {
                  $$0 = new InetSocketAddress($$3, $$4);
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.this.connection = Connection.connectToServer($$0, $$2.options.useNativeTransport(), $$2.getDebugOverlay().getBandwidthLogger());
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  ClientHandshakePacketListenerImpl $$1 = new ClientHandshakePacketListenerImpl(
                     RealmsConnect.this.connection, $$2, p_175032_.toServerData($$3), RealmsConnect.this.onlineScreen, false, null, p_120726_ -> {}, null
                  );
                  if (p_175032_.isMinigameActive()) {
                     $$1.setMinigameName(p_175032_.minigameName);
                  }

                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.this.connection.initiateServerboundPlayConnection($$3, $$4, $$1);
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.this.connection.send(new ServerboundHelloPacket($$2.getUser().getName(), $$2.getUser().getProfileId()));
                  $$2.updateReportEnvironment(ReportEnvironment.realm(p_175032_));
                  $$2.quickPlayLog().setWorldData(QuickPlayLog.Type.REALMS, String.valueOf(p_175032_.id), p_175032_.name);
                  $$2.getDownloadedPackSource().configureForServerControl(RealmsConnect.this.connection, ServerPackManager.PackPromptStatus.ALLOWED);
               } catch (Exception var5) {
                  $$2.getDownloadedPackSource().cleanupAfterDisconnect();
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.LOGGER.error("Couldn't connect to world", var5);
                  String $$3x = var5.toString();
                  if ($$0 != null) {
                     String $$4x = $$0 + ":" + $$4;
                     $$3x = $$3x.replaceAll($$4x, "");
                  }

                  DisconnectedRealmsScreen $$5 = new DisconnectedRealmsScreen(
                     RealmsConnect.this.onlineScreen, CommonComponents.CONNECT_FAILED, Component.translatable("disconnect.genericReason", $$3x)
                  );
                  $$2.execute(() -> $$2.setScreen($$5));
               }
            }
         })
         .start();
   }

   public void abort() {
      this.aborted = true;
      if (this.connection != null && this.connection.isConnected()) {
         this.connection.disconnect(Component.translatable("disconnect.genericReason"));
         this.connection.handleDisconnection();
      }
   }

   public void tick() {
      if (this.connection != null) {
         if (this.connection.isConnected()) {
            this.connection.tick();
         } else {
            this.connection.handleDisconnection();
         }
      }
   }
}
