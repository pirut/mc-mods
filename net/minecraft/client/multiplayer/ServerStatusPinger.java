package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.status.ClientStatusPacketListener;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ServerStatusPinger {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component CANT_CONNECT_MESSAGE = Component.translatable("multiplayer.status.cannot_connect").withColor(-65536);
   private final List<Connection> connections = Collections.synchronizedList(Lists.newArrayList());

   public void pingServer(final ServerData p_105460_, final Runnable p_105461_, final Runnable p_316316_) throws UnknownHostException {
      final ServerAddress $$3 = ServerAddress.parseString(p_105460_.ip);
      Optional<InetSocketAddress> $$4 = ServerNameResolver.DEFAULT.resolveAddress($$3).map(ResolvedServerAddress::asInetSocketAddress);
      if ($$4.isEmpty()) {
         this.onPingFailed(ConnectScreen.UNKNOWN_HOST_MESSAGE, p_105460_);
      } else {
         final InetSocketAddress $$5 = $$4.get();
         final Connection $$6 = Connection.connectToServer($$5, false, null);
         this.connections.add($$6);
         p_105460_.motd = Component.translatable("multiplayer.status.pinging");
         p_105460_.playerList = Collections.emptyList();
         ClientStatusPacketListener $$7 = new ClientStatusPacketListener() {
            private boolean success;
            private boolean receivedPing;
            private long pingStart;

            @Override
            public void handleStatusResponse(ClientboundStatusResponsePacket p_105489_) {
               if (this.receivedPing) {
                  $$6.disconnect(Component.translatable("multiplayer.status.unrequested"));
               } else {
                  this.receivedPing = true;
                  ServerStatus $$1 = p_105489_.status();
                  p_105460_.motd = $$1.description();
                  $$1.version().ifPresentOrElse(p_273307_ -> {
                     p_105460_.version = Component.literal(p_273307_.name());
                     p_105460_.protocol = p_273307_.protocol();
                  }, () -> {
                     p_105460_.version = Component.translatable("multiplayer.status.old");
                     p_105460_.protocol = 0;
                  });
                  $$1.players().ifPresentOrElse(p_273230_ -> {
                     p_105460_.status = ServerStatusPinger.formatPlayerCount(p_273230_.online(), p_273230_.max());
                     p_105460_.players = p_273230_;
                     if (!p_273230_.sample().isEmpty()) {
                        List<Component> $$2 = new ArrayList<>(p_273230_.sample().size());

                        for (GameProfile $$3xx : p_273230_.sample()) {
                           $$2.add(Component.literal($$3xx.getName()));
                        }

                        if (p_273230_.sample().size() < p_273230_.online()) {
                           $$2.add(Component.translatable("multiplayer.status.and_more", p_273230_.online() - p_273230_.sample().size()));
                        }

                        p_105460_.playerList = $$2;
                     } else {
                        p_105460_.playerList = List.of();
                     }
                  }, () -> p_105460_.status = Component.translatable("multiplayer.status.unknown").withStyle(ChatFormatting.DARK_GRAY));
                  $$1.favicon().ifPresent(p_302312_ -> {
                     if (!Arrays.equals(p_302312_.iconBytes(), p_105460_.getIconBytes())) {
                        p_105460_.setIconBytes(ServerData.validateIcon(p_302312_.iconBytes()));
                        p_105461_.run();
                     }
                  });
                  this.pingStart = Util.getMillis();
                  $$6.send(new ServerboundPingRequestPacket(this.pingStart));
                  this.success = true;
               }
            }

            @Override
            public void handlePongResponse(ClientboundPongResponsePacket p_320841_) {
               long $$1 = this.pingStart;
               long $$2 = Util.getMillis();
               p_105460_.ping = $$2 - $$1;
               $$6.disconnect(Component.translatable("multiplayer.status.finished"));
               p_316316_.run();
            }

            @Override
            public void onDisconnect(DisconnectionDetails p_350468_) {
               if (!this.success) {
                  ServerStatusPinger.this.onPingFailed(p_350468_.reason(), p_105460_);
                  ServerStatusPinger.this.pingLegacyServer($$5, $$3, p_105460_);
               }
            }

            @Override
            public boolean isAcceptingMessages() {
               return $$6.isConnected();
            }
         };

         try {
            $$6.initiateServerboundStatusConnection($$3.getHost(), $$3.getPort(), $$7);
            $$6.send(ServerboundStatusRequestPacket.INSTANCE);
         } catch (Throwable var10) {
            LOGGER.error("Failed to ping server {}", $$3, var10);
         }
      }
   }

   void onPingFailed(Component p_171815_, ServerData p_171816_) {
      LOGGER.error("Can't ping {}: {}", p_171816_.ip, p_171815_.getString());
      p_171816_.motd = CANT_CONNECT_MESSAGE;
      p_171816_.status = CommonComponents.EMPTY;
   }

   void pingLegacyServer(InetSocketAddress p_171812_, final ServerAddress p_295436_, final ServerData p_171813_) {
      ((Bootstrap)((Bootstrap)((Bootstrap)new Bootstrap().group((EventLoopGroup)Connection.NETWORK_WORKER_GROUP.get()))
               .handler(new ChannelInitializer<Channel>() {
                  protected void initChannel(Channel p_105498_) {
                     try {
                        p_105498_.config().setOption(ChannelOption.TCP_NODELAY, true);
                     } catch (ChannelException var3) {
                     }

                     p_105498_.pipeline()
                        .addLast(new ChannelHandler[]{new LegacyServerPinger(p_295436_, (p_315832_, p_315833_, p_315834_, p_315835_, p_315836_) -> {
                           p_171813_.setState(ServerData.State.INCOMPATIBLE);
                           p_171813_.version = Component.literal(p_315833_);
                           p_171813_.motd = Component.literal(p_315834_);
                           p_171813_.status = ServerStatusPinger.formatPlayerCount(p_315835_, p_315836_);
                           p_171813_.players = new ServerStatus.Players(p_315836_, p_315835_, List.of());
                        })});
                  }
               }))
            .channel(NioSocketChannel.class))
         .connect(p_171812_.getAddress(), p_171812_.getPort());
   }

   public static Component formatPlayerCount(int p_105467_, int p_105468_) {
      Component $$2 = Component.literal(Integer.toString(p_105467_)).withStyle(ChatFormatting.GRAY);
      Component $$3 = Component.literal(Integer.toString(p_105468_)).withStyle(ChatFormatting.GRAY);
      return Component.translatable("multiplayer.status.player_count", $$2, $$3).withStyle(ChatFormatting.DARK_GRAY);
   }

   public void tick() {
      synchronized (this.connections) {
         Iterator<Connection> $$0 = this.connections.iterator();

         while ($$0.hasNext()) {
            Connection $$1 = $$0.next();
            if ($$1.isConnected()) {
               $$1.tick();
            } else {
               $$0.remove();
               $$1.handleDisconnection();
            }
         }
      }
   }

   public void removeAll() {
      synchronized (this.connections) {
         Iterator<Connection> $$0 = this.connections.iterator();

         while ($$0.hasNext()) {
            Connection $$1 = $$0.next();
            if ($$1.isConnected()) {
               $$0.remove();
               $$1.disconnect(Component.translatable("multiplayer.status.cancelled"));
            }
         }
      }
   }
}
