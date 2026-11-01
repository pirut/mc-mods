package net.minecraft.server.jsonrpc;

import com.google.common.collect.Sets;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.security.AuthenticationHandler;
import net.minecraft.server.jsonrpc.websocket.JsonToWebSocketEncoder;
import net.minecraft.server.jsonrpc.websocket.WebSocketToJsonCodec;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ManagementServer {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final HostAndPort hostAndPort;
   final AuthenticationHandler authenticationHandler;
   @Nullable
   private Channel serverChannel;
   private final NioEventLoopGroup nioEventLoopGroup;
   private final Set<Connection> connections = Sets.newIdentityHashSet();

   public ManagementServer(HostAndPort p_443590_, AuthenticationHandler p_449812_) {
      this.hostAndPort = p_443590_;
      this.authenticationHandler = p_449812_;
      this.nioEventLoopGroup = new NioEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("Management server IO #%d").setDaemon(true).build());
   }

   public ManagementServer(HostAndPort p_449277_, AuthenticationHandler p_449321_, NioEventLoopGroup p_449075_) {
      this.hostAndPort = p_449277_;
      this.authenticationHandler = p_449321_;
      this.nioEventLoopGroup = p_449075_;
   }

   public void onConnected(Connection p_442614_) {
      synchronized (this.connections) {
         this.connections.add(p_442614_);
      }
   }

   public void onDisconnected(Connection p_443459_) {
      synchronized (this.connections) {
         this.connections.remove(p_443459_);
      }
   }

   public void startWithoutTls(MinecraftApi p_449388_) {
      this.start(p_449388_, null);
   }

   public void startWithTls(MinecraftApi p_449085_, SslContext p_449911_) {
      this.start(p_449085_, p_449911_);
   }

   private void start(final MinecraftApi p_449532_, @Nullable final SslContext p_449912_) {
      final JsonRpcLogger jsonrpclogger = new JsonRpcLogger();
      ChannelFuture channelfuture = ((ServerBootstrap)((ServerBootstrap)((ServerBootstrap)new ServerBootstrap().handler(new LoggingHandler(LogLevel.DEBUG)))
               .channel(NioServerSocketChannel.class))
            .childHandler(
               new ChannelInitializer<Channel>() {
                  protected void initChannel(Channel p_442866_) {
                     try {
                        p_442866_.config().setOption(ChannelOption.TCP_NODELAY, true);
                     } catch (ChannelException var3) {
                     }

                     ChannelPipeline channelpipeline = p_442866_.pipeline();
                     if (p_449912_ != null) {
                        channelpipeline.addLast(new ChannelHandler[]{p_449912_.newHandler(p_442866_.alloc())});
                     }

                     channelpipeline.addLast(new ChannelHandler[]{new HttpServerCodec()})
                        .addLast(new ChannelHandler[]{new HttpObjectAggregator(65536)})
                        .addLast(new ChannelHandler[]{ManagementServer.this.authenticationHandler})
                        .addLast(new ChannelHandler[]{new WebSocketServerProtocolHandler("/")})
                        .addLast(new ChannelHandler[]{new WebSocketToJsonCodec()})
                        .addLast(new ChannelHandler[]{new JsonToWebSocketEncoder()})
                        .addLast(new ChannelHandler[]{new Connection(p_442866_, ManagementServer.this, p_449532_, jsonrpclogger)});
                  }
               }
            )
            .group(this.nioEventLoopGroup)
            .localAddress(this.hostAndPort.getHost(), this.hostAndPort.getPort()))
         .bind();
      this.serverChannel = channelfuture.channel();
      channelfuture.syncUninterruptibly();
      LOGGER.info("Json-RPC Management connection listening on {}:{}", this.hostAndPort.getHost(), this.getPort());
   }

   public void stop(boolean p_449619_) throws InterruptedException {
      if (this.serverChannel != null) {
         this.serverChannel.close().sync();
         this.serverChannel = null;
      }

      this.connections.clear();
      if (p_449619_) {
         this.nioEventLoopGroup.shutdownGracefully().sync();
      }
   }

   public void tick() {
      this.forEachConnection(Connection::tick);
   }

   public int getPort() {
      return this.serverChannel != null ? ((InetSocketAddress)this.serverChannel.localAddress()).getPort() : this.hostAndPort.getPort();
   }

   void forEachConnection(Consumer<Connection> p_443350_) {
      synchronized (this.connections) {
         this.connections.forEach(p_443350_);
      }
   }
}
