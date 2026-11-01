package com.mojang.realmsclient.util;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsUtil {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component RIGHT_NOW = Component.translatable("mco.util.time.now");
   private static final int MINUTES = 60;
   private static final int HOURS = 3600;
   private static final int DAYS = 86400;

   public static Component convertToAgePresentation(long p_287679_) {
      if (p_287679_ < 0L) {
         return RIGHT_NOW;
      } else {
         long i = p_287679_ / 1000L;
         if (i < 60L) {
            return Component.translatable("mco.time.secondsAgo", i);
         } else if (i < 3600L) {
            long l = i / 60L;
            return Component.translatable("mco.time.minutesAgo", l);
         } else if (i < 86400L) {
            long k = i / 3600L;
            return Component.translatable("mco.time.hoursAgo", k);
         } else {
            long j = i / 86400L;
            return Component.translatable("mco.time.daysAgo", j);
         }
      }
   }

   public static Component convertToAgePresentationFromInstant(Instant p_458984_) {
      return convertToAgePresentation(System.currentTimeMillis() - p_458984_.toEpochMilli());
   }

   public static void renderPlayerFace(GuiGraphics p_281255_, int p_281818_, int p_281791_, int p_282088_, UUID p_294599_) {
      PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo = Minecraft.getInstance()
         .playerSkinRenderCache()
         .getOrDefault(ResolvableProfile.createUnresolved(p_294599_));
      PlayerFaceRenderer.draw(p_281255_, playerskinrendercache$renderinfo.playerSkin(), p_281818_, p_281791_, p_282088_);
   }

   public static <T> CompletableFuture<T> supplyAsync(RealmsUtil.RealmsIoFunction<T> p_428701_, @Nullable Consumer<RealmsServiceException> p_428719_) {
      return CompletableFuture.supplyAsync(() -> {
         RealmsClient realmsclient = RealmsClient.getOrCreate();

         try {
            return p_428701_.apply(realmsclient);
         } catch (Throwable var5) {
            if (var5 instanceof RealmsServiceException realmsserviceexception) {
               if (p_428719_ != null) {
                  p_428719_.accept(realmsserviceexception);
               }
            } else {
               LOGGER.error("Unhandled exception", var5);
            }

            throw new RuntimeException(var5);
         }
      }, Util.nonCriticalIoPool());
   }

   public static CompletableFuture<Void> runAsync(RealmsUtil.RealmsIoConsumer p_428705_, @Nullable Consumer<RealmsServiceException> p_428685_) {
      return supplyAsync(p_428705_, p_428685_);
   }

   public static Consumer<RealmsServiceException> openScreenOnFailure(Function<RealmsServiceException, Screen> p_428692_) {
      Minecraft minecraft = Minecraft.getInstance();
      return p_428731_ -> minecraft.execute(() -> minecraft.setScreen(p_428692_.apply(p_428731_)));
   }

   public static Consumer<RealmsServiceException> openScreenAndLogOnFailure(Function<RealmsServiceException, Screen> p_428683_, String p_428704_) {
      return openScreenOnFailure(p_428683_).andThen(p_428690_ -> LOGGER.error(p_428704_, p_428690_));
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface RealmsIoConsumer extends RealmsUtil.RealmsIoFunction<Void> {
      void accept(RealmsClient var1) throws RealmsServiceException;

      default Void apply(RealmsClient p_428718_) throws RealmsServiceException {
         this.accept(p_428718_);
         return null;
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface RealmsIoFunction<T> {
      T apply(RealmsClient var1) throws RealmsServiceException;
   }
}
