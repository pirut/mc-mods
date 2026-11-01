package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class HttpTexture extends SimpleTexture {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SKIN_WIDTH = 64;
   private static final int SKIN_HEIGHT = 64;
   private static final int LEGACY_SKIN_HEIGHT = 32;
   @Nullable
   private final File file;
   private final String urlString;
   private final boolean processLegacySkin;
   @Nullable
   private final Runnable onDownloaded;
   @Nullable
   private CompletableFuture<?> future;
   private boolean uploaded;

   public HttpTexture(@Nullable File p_118002_, String p_118003_, ResourceLocation p_118004_, boolean p_118005_, @Nullable Runnable p_118006_) {
      super(p_118004_);
      this.file = p_118002_;
      this.urlString = p_118003_;
      this.processLegacySkin = p_118005_;
      this.onDownloaded = p_118006_;
   }

   private void loadCallback(NativeImage p_118011_) {
      if (this.onDownloaded != null) {
         this.onDownloaded.run();
      }

      Minecraft.getInstance().execute(() -> {
         this.uploaded = true;
         if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> this.upload(p_118011_));
         } else {
            this.upload(p_118011_);
         }
      });
   }

   private void upload(NativeImage p_118021_) {
      TextureUtil.prepareImage(this.getId(), p_118021_.getWidth(), p_118021_.getHeight());
      p_118021_.upload(0, 0, 0, true);
   }

   @Override
   public void load(ResourceManager p_118009_) throws IOException {
      Minecraft.getInstance().execute(() -> {
         if (!this.uploaded) {
            try {
               super.load(p_118009_);
            } catch (IOException var3x) {
               LOGGER.warn("Failed to load texture: {}", this.location, var3x);
            }

            this.uploaded = true;
         }
      });
      if (this.future == null) {
         NativeImage $$2;
         if (this.file != null && this.file.isFile()) {
            LOGGER.debug("Loading http texture from local cache ({})", this.file);
            FileInputStream $$1 = new FileInputStream(this.file);
            $$2 = this.load($$1);
         } else {
            $$2 = null;
         }

         if ($$2 != null) {
            this.loadCallback($$2);
         } else {
            this.future = CompletableFuture.runAsync(() -> {
               HttpURLConnection $$0 = null;
               LOGGER.debug("Downloading http texture from {} to {}", this.urlString, this.file);

               try {
                  $$0 = (HttpURLConnection)new URL(this.urlString).openConnection(Minecraft.getInstance().getProxy());
                  $$0.setDoInput(true);
                  $$0.setDoOutput(false);
                  $$0.connect();
                  if ($$0.getResponseCode() / 100 == 2) {
                     InputStream $$1x;
                     if (this.file != null) {
                        FileUtils.copyInputStreamToFile($$0.getInputStream(), this.file);
                        $$1x = new FileInputStream(this.file);
                     } else {
                        $$1x = $$0.getInputStream();
                     }

                     Minecraft.getInstance().execute(() -> {
                        NativeImage $$1xx = this.load($$1x);
                        if ($$1xx != null) {
                           this.loadCallback($$1xx);
                        }
                     });
                     return;
                  }
               } catch (Exception var6) {
                  LOGGER.error("Couldn't download http texture", var6);
                  return;
               } finally {
                  if ($$0 != null) {
                     $$0.disconnect();
                  }
               }
            }, Util.backgroundExecutor());
         }
      }
   }

   @Nullable
   private NativeImage load(InputStream p_118019_) {
      NativeImage $$1 = null;

      try {
         $$1 = NativeImage.read(p_118019_);
         if (this.processLegacySkin) {
            $$1 = this.processLegacySkin($$1);
         }
      } catch (Exception var4) {
         LOGGER.warn("Error while loading the skin texture", var4);
      }

      return $$1;
   }

   @Nullable
   private NativeImage processLegacySkin(NativeImage p_118033_) {
      int $$1 = p_118033_.getHeight();
      int $$2 = p_118033_.getWidth();
      if ($$2 == 64 && ($$1 == 32 || $$1 == 64)) {
         boolean $$3 = $$1 == 32;
         if ($$3) {
            NativeImage $$4 = new NativeImage(64, 64, true);
            $$4.copyFrom(p_118033_);
            p_118033_.close();
            p_118033_ = $$4;
            $$4.fillRect(0, 32, 64, 32, 0);
            $$4.copyRect(4, 16, 16, 32, 4, 4, true, false);
            $$4.copyRect(8, 16, 16, 32, 4, 4, true, false);
            $$4.copyRect(0, 20, 24, 32, 4, 12, true, false);
            $$4.copyRect(4, 20, 16, 32, 4, 12, true, false);
            $$4.copyRect(8, 20, 8, 32, 4, 12, true, false);
            $$4.copyRect(12, 20, 16, 32, 4, 12, true, false);
            $$4.copyRect(44, 16, -8, 32, 4, 4, true, false);
            $$4.copyRect(48, 16, -8, 32, 4, 4, true, false);
            $$4.copyRect(40, 20, 0, 32, 4, 12, true, false);
            $$4.copyRect(44, 20, -8, 32, 4, 12, true, false);
            $$4.copyRect(48, 20, -16, 32, 4, 12, true, false);
            $$4.copyRect(52, 20, -8, 32, 4, 12, true, false);
         }

         setNoAlpha(p_118033_, 0, 0, 32, 16);
         if ($$3) {
            doNotchTransparencyHack(p_118033_, 32, 0, 64, 32);
         }

         setNoAlpha(p_118033_, 0, 16, 64, 32);
         setNoAlpha(p_118033_, 16, 48, 48, 64);
         return p_118033_;
      } else {
         p_118033_.close();
         LOGGER.warn("Discarding incorrectly sized ({}x{}) skin texture from {}", new Object[]{$$2, $$1, this.urlString});
         return null;
      }
   }

   private static void doNotchTransparencyHack(NativeImage p_118013_, int p_118014_, int p_118015_, int p_118016_, int p_118017_) {
      for (int $$5 = p_118014_; $$5 < p_118016_; $$5++) {
         for (int $$6 = p_118015_; $$6 < p_118017_; $$6++) {
            int $$7 = p_118013_.getPixelRGBA($$5, $$6);
            if (($$7 >> 24 & 0xFF) < 128) {
               return;
            }
         }
      }

      for (int $$8 = p_118014_; $$8 < p_118016_; $$8++) {
         for (int $$9 = p_118015_; $$9 < p_118017_; $$9++) {
            p_118013_.setPixelRGBA($$8, $$9, p_118013_.getPixelRGBA($$8, $$9) & 16777215);
         }
      }
   }

   private static void setNoAlpha(NativeImage p_118023_, int p_118024_, int p_118025_, int p_118026_, int p_118027_) {
      for (int $$5 = p_118024_; $$5 < p_118026_; $$5++) {
         for (int $$6 = p_118025_; $$6 < p_118027_; $$6++) {
            p_118023_.setPixelRGBA($$5, $$6, p_118023_.getPixelRGBA($$5, $$6) | 0xFF000000);
         }
      }
   }
}
