package net.minecraft.server.jsonrpc.security;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AttributeKey;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Sharable
public class AuthenticationHandler extends ChannelDuplexHandler {
   private final Logger LOGGER = LogUtils.getLogger();
   private static final AttributeKey<Boolean> AUTHENTICATED_KEY = AttributeKey.valueOf("authenticated");
   private static final AttributeKey<Boolean> ATTR_WEBSOCKET_ALLOWED = AttributeKey.valueOf("websocket_auth_allowed");
   private static final String SUBPROTOCOL_VALUE = "minecraft-v1";
   private static final String SUBPROTOCOL_HEADER_PREFIX = "minecraft-v1,";
   public static final String BEARER_PREFIX = "Bearer ";
   private final SecurityConfig securityConfig;
   private final Set<String> allowedOrigins;

   public AuthenticationHandler(SecurityConfig p_449746_, String p_457847_) {
      this.securityConfig = p_449746_;
      this.allowedOrigins = Sets.newHashSet(p_457847_.split(","));
   }

   public void channelRead(ChannelHandlerContext p_449854_, Object p_449952_) throws Exception {
      String s = this.getClientIp(p_449854_);
      if (p_449952_ instanceof HttpRequest httprequest) {
         AuthenticationHandler.SecurityCheckResult authenticationhandler$securitycheckresult = this.performSecurityChecks(httprequest);
         if (!authenticationhandler$securitycheckresult.isAllowed()) {
            this.LOGGER.debug("Authentication rejected for connection with ip {}: {}", s, authenticationhandler$securitycheckresult.getReason());
            p_449854_.channel().attr(AUTHENTICATED_KEY).set(false);
            this.sendUnauthorizedResponse(p_449854_, authenticationhandler$securitycheckresult.getReason());
            return;
         }

         p_449854_.channel().attr(AUTHENTICATED_KEY).set(true);
         if (authenticationhandler$securitycheckresult.isTokenSentInSecWebsocketProtocol()) {
            p_449854_.channel().attr(ATTR_WEBSOCKET_ALLOWED).set(Boolean.TRUE);
         }
      }

      Boolean obool = (Boolean)p_449854_.channel().attr(AUTHENTICATED_KEY).get();
      if (Boolean.TRUE.equals(obool)) {
         super.channelRead(p_449854_, p_449952_);
      } else {
         this.LOGGER.debug("Dropping unauthenticated connection with ip {}", s);
         p_449854_.close();
      }
   }

   public void write(ChannelHandlerContext p_457970_, Object p_458189_, ChannelPromise p_457554_) throws Exception {
      if (p_458189_ instanceof HttpResponse httpresponse
         && httpresponse.status().code() == HttpResponseStatus.SWITCHING_PROTOCOLS.code()
         && p_457970_.channel().attr(ATTR_WEBSOCKET_ALLOWED).get() != null
         && ((Boolean)p_457970_.channel().attr(ATTR_WEBSOCKET_ALLOWED).get()).equals(Boolean.TRUE)) {
         httpresponse.headers().set(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, "minecraft-v1");
      }

      super.write(p_457970_, p_458189_, p_457554_);
   }

   private AuthenticationHandler.SecurityCheckResult performSecurityChecks(HttpRequest p_449434_) {
      String s = this.parseTokenInAuthorizationHeader(p_449434_);
      if (s != null) {
         return this.isValidApiKey(s)
            ? AuthenticationHandler.SecurityCheckResult.allowed()
            : AuthenticationHandler.SecurityCheckResult.denied("Invalid API key");
      } else {
         String s1 = this.parseTokenInSecWebsocketProtocolHeader(p_449434_);
         if (s1 != null) {
            if (!this.isAllowedOriginHeader(p_449434_)) {
               return AuthenticationHandler.SecurityCheckResult.denied("Origin Not Allowed");
            } else {
               return this.isValidApiKey(s1)
                  ? AuthenticationHandler.SecurityCheckResult.allowed(true)
                  : AuthenticationHandler.SecurityCheckResult.denied("Invalid API key");
            }
         } else {
            return AuthenticationHandler.SecurityCheckResult.denied("Missing API key");
         }
      }
   }

   private boolean isAllowedOriginHeader(HttpRequest p_458033_) {
      String s = p_458033_.headers().get(HttpHeaderNames.ORIGIN);
      return s != null && !s.isEmpty() ? this.allowedOrigins.contains(s) : false;
   }

   @Nullable
   private String parseTokenInAuthorizationHeader(HttpRequest p_457531_) {
      String s = p_457531_.headers().get(HttpHeaderNames.AUTHORIZATION);
      return s != null && s.startsWith("Bearer ") ? s.substring("Bearer ".length()).trim() : null;
   }

   @Nullable
   private String parseTokenInSecWebsocketProtocolHeader(HttpRequest p_457754_) {
      String s = p_457754_.headers().get(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL);
      return s != null && s.startsWith("minecraft-v1,") ? s.substring("minecraft-v1,".length()).trim() : null;
   }

   public boolean isValidApiKey(String p_449247_) {
      if (p_449247_.isEmpty()) {
         return false;
      } else {
         byte[] abyte = p_449247_.getBytes(StandardCharsets.UTF_8);
         byte[] abyte1 = this.securityConfig.secretKey().getBytes(StandardCharsets.UTF_8);
         return MessageDigest.isEqual(abyte, abyte1);
      }
   }

   private String getClientIp(ChannelHandlerContext p_449294_) {
      InetSocketAddress inetsocketaddress = (InetSocketAddress)p_449294_.channel().remoteAddress();
      return inetsocketaddress.getAddress().getHostAddress();
   }

   private void sendUnauthorizedResponse(ChannelHandlerContext p_449686_, String p_449786_) {
      String s = "{\"error\":\"Unauthorized\",\"message\":\"" + p_449786_ + "\"}";
      byte[] abyte = s.getBytes(StandardCharsets.UTF_8);
      DefaultFullHttpResponse defaultfullhttpresponse = new DefaultFullHttpResponse(
         HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED, Unpooled.wrappedBuffer(abyte)
      );
      defaultfullhttpresponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
      defaultfullhttpresponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, abyte.length);
      defaultfullhttpresponse.headers().set(HttpHeaderNames.CONNECTION, "close");
      p_449686_.writeAndFlush(defaultfullhttpresponse).addListener(p_449756_ -> p_449686_.close());
   }

   static class SecurityCheckResult {
      private final boolean allowed;
      private final String reason;
      private final boolean tokenSentInSecWebsocketProtocol;

      private SecurityCheckResult(boolean p_449797_, String p_449798_, boolean p_457511_) {
         this.allowed = p_449797_;
         this.reason = p_449798_;
         this.tokenSentInSecWebsocketProtocol = p_457511_;
      }

      public static AuthenticationHandler.SecurityCheckResult allowed() {
         return new AuthenticationHandler.SecurityCheckResult(true, null, false);
      }

      public static AuthenticationHandler.SecurityCheckResult allowed(boolean p_457814_) {
         return new AuthenticationHandler.SecurityCheckResult(true, null, p_457814_);
      }

      public static AuthenticationHandler.SecurityCheckResult denied(String p_449267_) {
         return new AuthenticationHandler.SecurityCheckResult(false, p_449267_, false);
      }

      public boolean isAllowed() {
         return this.allowed;
      }

      public String getReason() {
         return this.reason;
      }

      public boolean isTokenSentInSecWebsocketProtocol() {
         return this.tokenSentInSecWebsocketProtocol;
      }
   }
}
