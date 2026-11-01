package com.mojang.realmsclient.util;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class RealmsUtil {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component RIGHT_NOW = Component.translatable("mco.util.time.now");
   private static final int MINUTES = 60;
   private static final int HOURS = 3600;
   private static final int DAYS = 86400;

   public static Component convertToAgePresentation(long millis) {
      if (millis < 0L) {
         return RIGHT_NOW;
      } else {
         long l = millis / 1000L;
         if (l < 60L) {
            return Component.translatable("mco.time.secondsAgo", l);
         } else if (l < 3600L) {
            long m = l / 60L;
            return Component.translatable("mco.time.minutesAgo", m);
         } else if (l < 86400L) {
            long m = l / 3600L;
            return Component.translatable("mco.time.hoursAgo", m);
         } else {
            long m = l / 86400L;
            return Component.translatable("mco.time.daysAgo", m);
         }
      }
   }

   public static Component convertToAgePresentationFromInstant(Instant instant) {
      return convertToAgePresentation(System.currentTimeMillis() - instant.toEpochMilli());
   }

   public static void renderPlayerFace(GuiGraphics guiGraphics, int x, int y, int size, UUID playerUuid) {
      PlayerSkinRenderCache.RenderInfo renderInfo = Minecraft.getInstance()
         .playerSkinRenderCache()
         .getOrDefault(ResolvableProfile.createUnresolved(playerUuid));
      PlayerFaceRenderer.draw(guiGraphics, renderInfo.playerSkin(), x, y, size);
   }

   public static <T> CompletableFuture<T> supplyAsync(RealmsUtil.RealmsIoFunction<T> action, @Nullable Consumer<RealmsServiceException> onError) {
      return CompletableFuture.supplyAsync(() -> {
         RealmsClient realmsClient = RealmsClient.getOrCreate();

         try {
            return action.apply(realmsClient);
         } catch (Throwable var5) {
            if (var5 instanceof RealmsServiceException realmsServiceException) {
               if (onError != null) {
                  onError.accept(realmsServiceException);
               }
            } else {
               LOGGER.error("Unhandled exception", var5);
            }

            throw new RuntimeException(var5);
         }
      }, Util.nonCriticalIoPool());
   }

   public static CompletableFuture<Void> runAsync(RealmsUtil.RealmsIoConsumer action, @Nullable Consumer<RealmsServiceException> onError) {
      return supplyAsync(action, onError);
   }

   public static Consumer<RealmsServiceException> openScreenOnFailure(Function<RealmsServiceException, Screen> screenSupplier) {
      Minecraft minecraft = Minecraft.getInstance();
      return realmsServiceException -> minecraft.execute(() -> minecraft.setScreen(screenSupplier.apply(realmsServiceException)));
   }

   public static Consumer<RealmsServiceException> openScreenAndLogOnFailure(Function<RealmsServiceException, Screen> screenSupplier, String errorMessage) {
      return openScreenOnFailure(screenSupplier).andThen(realmsServiceException -> LOGGER.error(errorMessage, realmsServiceException));
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface RealmsIoConsumer extends RealmsUtil.RealmsIoFunction<Void> {
      void accept(RealmsClient realmsClient) throws RealmsServiceException;

      default Void apply(RealmsClient realmsClient) throws RealmsServiceException {
         this.accept(realmsClient);
         return null;
      }
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface RealmsIoFunction<T> {
      T apply(RealmsClient realmsClient) throws RealmsServiceException;
   }
}
