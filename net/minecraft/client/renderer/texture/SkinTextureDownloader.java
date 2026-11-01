package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Util;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class SkinTextureDownloader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SKIN_WIDTH = 64;
   private static final int SKIN_HEIGHT = 64;
   private static final int LEGACY_SKIN_HEIGHT = 32;
   private final Proxy proxy;
   private final TextureManager textureManager;
   private final Executor mainThreadExecutor;

   public SkinTextureDownloader(Proxy proxy, TextureManager textureManager, Executor mainThreadExecutor) {
      this.proxy = proxy;
      this.textureManager = textureManager;
      this.mainThreadExecutor = mainThreadExecutor;
   }

   public CompletableFuture<ClientAsset.Texture> downloadAndRegisterSkin(Identifier textureLocation, Path path, String url, boolean isLegacySkin) {
      ClientAsset.DownloadedTexture downloadedTexture = new ClientAsset.DownloadedTexture(textureLocation, url);
      return CompletableFuture.<NativeImage>supplyAsync(() -> {
         NativeImage nativeImage;
         try {
            nativeImage = this.downloadSkin(path, downloadedTexture.url());
         } catch (IOException var6) {
            throw new UncheckedIOException(var6);
         }

         return isLegacySkin ? processLegacySkin(nativeImage, downloadedTexture.url()) : nativeImage;
      }, Util.nonCriticalIoPool().forName("downloadTexture")).thenCompose(nativeImage -> this.registerTextureInManager(downloadedTexture, nativeImage));
   }

   private NativeImage downloadSkin(Path path, String url) throws IOException {
      if (Files.isRegularFile(path)) {
         LOGGER.debug("Loading HTTP texture from local cache ({})", path);

         NativeImage var18;
         try (InputStream inputStream = Files.newInputStream(path)) {
            var18 = NativeImage.read(inputStream);
         }

         return var18;
      } else {
         HttpURLConnection httpURLConnection = null;
         LOGGER.debug("Downloading HTTP texture from {} to {}", url, path);
         URI uRI = URI.create(url);

         NativeImage iOException;
         try {
            httpURLConnection = (HttpURLConnection)uRI.toURL().openConnection(this.proxy);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(false);
            httpURLConnection.connect();
            int i = httpURLConnection.getResponseCode();
            if (i / 100 != 2) {
               throw new IOException("Failed to open " + uRI + ", HTTP error code: " + i);
            }

            byte[] bs = httpURLConnection.getInputStream().readAllBytes();

            try {
               FileUtil.createDirectoriesSafe(path.getParent());
               Files.write(path, bs);
            } catch (IOException var14) {
               LOGGER.warn("Failed to cache texture {} in {}", url, path);
            }

            iOException = NativeImage.read(bs);
         } finally {
            if (httpURLConnection != null) {
               httpURLConnection.disconnect();
            }
         }

         return iOException;
      }
   }

   private CompletableFuture<ClientAsset.Texture> registerTextureInManager(ClientAsset.Texture texture, NativeImage image) {
      return CompletableFuture.supplyAsync(() -> {
         DynamicTexture dynamicTexture = new DynamicTexture(texture.texturePath()::toString, image);
         this.textureManager.register(texture.texturePath(), dynamicTexture);
         return texture;
      }, this.mainThreadExecutor);
   }

   private static NativeImage processLegacySkin(NativeImage image, String url) {
      int i = image.getHeight();
      int j = image.getWidth();
      if (j == 64 && (i == 32 || i == 64)) {
         boolean bl = i == 32;
         if (bl) {
            NativeImage nativeImage = new NativeImage(64, 64, true);
            nativeImage.copyFrom(image);
            image.close();
            image = nativeImage;
            nativeImage.fillRect(0, 32, 64, 32, 0);
            nativeImage.copyRect(4, 16, 16, 32, 4, 4, true, false);
            nativeImage.copyRect(8, 16, 16, 32, 4, 4, true, false);
            nativeImage.copyRect(0, 20, 24, 32, 4, 12, true, false);
            nativeImage.copyRect(4, 20, 16, 32, 4, 12, true, false);
            nativeImage.copyRect(8, 20, 8, 32, 4, 12, true, false);
            nativeImage.copyRect(12, 20, 16, 32, 4, 12, true, false);
            nativeImage.copyRect(44, 16, -8, 32, 4, 4, true, false);
            nativeImage.copyRect(48, 16, -8, 32, 4, 4, true, false);
            nativeImage.copyRect(40, 20, 0, 32, 4, 12, true, false);
            nativeImage.copyRect(44, 20, -8, 32, 4, 12, true, false);
            nativeImage.copyRect(48, 20, -16, 32, 4, 12, true, false);
            nativeImage.copyRect(52, 20, -8, 32, 4, 12, true, false);
         }

         setNoAlpha(image, 0, 0, 32, 16);
         if (bl) {
            doNotchTransparencyHack(image, 32, 0, 64, 32);
         }

         setNoAlpha(image, 0, 16, 64, 32);
         setNoAlpha(image, 16, 48, 48, 64);
         return image;
      } else {
         image.close();
         throw new IllegalStateException("Discarding incorrectly sized (" + j + "x" + i + ") skin texture from " + url);
      }
   }

   private static void doNotchTransparencyHack(NativeImage image, int minX, int minY, int maxX, int maxY) {
      for (int i = minX; i < maxX; i++) {
         for (int j = minY; j < maxY; j++) {
            int k = image.getPixel(i, j);
            if (ARGB.alpha(k) < 128) {
               return;
            }
         }
      }

      for (int i = minX; i < maxX; i++) {
         for (int jx = minY; jx < maxY; jx++) {
            image.setPixel(i, jx, image.getPixel(i, jx) & 16777215);
         }
      }
   }

   private static void setNoAlpha(NativeImage image, int minX, int minY, int maxX, int maxY) {
      for (int i = minX; i < maxX; i++) {
         for (int j = minY; j < maxY; j++) {
            image.setPixel(i, j, ARGB.opaque(image.getPixel(i, j)));
         }
      }
   }
}
