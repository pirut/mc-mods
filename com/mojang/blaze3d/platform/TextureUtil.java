package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class TextureUtil {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int MIN_MIPMAP_LEVEL = 0;
   private static final int DEFAULT_IMAGE_BUFFER_SIZE = 8192;

   public static int generateTextureId() {
      RenderSystem.assertOnRenderThreadOrInit();
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         int[] $$0 = new int[ThreadLocalRandom.current().nextInt(15) + 1];
         GlStateManager._genTextures($$0);
         int $$1 = GlStateManager._genTexture();
         GlStateManager._deleteTextures($$0);
         return $$1;
      } else {
         return GlStateManager._genTexture();
      }
   }

   public static void releaseTextureId(int p_85282_) {
      RenderSystem.assertOnRenderThreadOrInit();
      GlStateManager._deleteTexture(p_85282_);
   }

   public static void prepareImage(int p_85284_, int p_85285_, int p_85286_) {
      prepareImage(NativeImage.InternalGlFormat.RGBA, p_85284_, 0, p_85285_, p_85286_);
   }

   public static void prepareImage(NativeImage.InternalGlFormat p_85293_, int p_85294_, int p_85295_, int p_85296_) {
      prepareImage(p_85293_, p_85294_, 0, p_85295_, p_85296_);
   }

   public static void prepareImage(int p_85288_, int p_85289_, int p_85290_, int p_85291_) {
      prepareImage(NativeImage.InternalGlFormat.RGBA, p_85288_, p_85289_, p_85290_, p_85291_);
   }

   public static void prepareImage(NativeImage.InternalGlFormat p_85298_, int p_85299_, int p_85300_, int p_85301_, int p_85302_) {
      RenderSystem.assertOnRenderThreadOrInit();
      bind(p_85299_);
      if (p_85300_ >= 0) {
         GlStateManager._texParameter(3553, 33085, p_85300_);
         GlStateManager._texParameter(3553, 33082, 0);
         GlStateManager._texParameter(3553, 33083, p_85300_);
         GlStateManager._texParameter(3553, 34049, 0.0F);
      }

      for (int $$5 = 0; $$5 <= p_85300_; $$5++) {
         GlStateManager._texImage2D(3553, $$5, p_85298_.glFormat(), p_85301_ >> $$5, p_85302_ >> $$5, 0, 6408, 5121, null);
      }
   }

   private static void bind(int p_85310_) {
      RenderSystem.assertOnRenderThreadOrInit();
      GlStateManager._bindTexture(p_85310_);
   }

   public static ByteBuffer readResource(InputStream p_85304_) throws IOException {
      ReadableByteChannel $$1 = Channels.newChannel(p_85304_);
      return $$1 instanceof SeekableByteChannel $$2 ? readResource($$1, (int)$$2.size() + 1) : readResource($$1, 8192);
   }

   private static ByteBuffer readResource(ReadableByteChannel p_273208_, int p_273297_) throws IOException {
      ByteBuffer $$2 = MemoryUtil.memAlloc(p_273297_);

      try {
         while (p_273208_.read($$2) != -1) {
            if (!$$2.hasRemaining()) {
               $$2 = MemoryUtil.memRealloc($$2, $$2.capacity() * 2);
            }
         }

         return $$2;
      } catch (IOException var4) {
         MemoryUtil.memFree($$2);
         throw var4;
      }
   }

   public static void writeAsPNG(Path p_261923_, String p_262070_, int p_261655_, int p_261576_, int p_261966_, int p_261775_) {
      writeAsPNG(p_261923_, p_262070_, p_261655_, p_261576_, p_261966_, p_261775_, null);
   }

   public static void writeAsPNG(
      Path p_285286_, String p_285408_, int p_285400_, int p_285244_, int p_285373_, int p_285206_, @Nullable IntUnaryOperator p_284988_
   ) {
      RenderSystem.assertOnRenderThread();
      bind(p_285400_);

      for (int $$7 = 0; $$7 <= p_285244_; $$7++) {
         int $$8 = p_285373_ >> $$7;
         int $$9 = p_285206_ >> $$7;

         try (NativeImage $$10 = new NativeImage($$8, $$9, false)) {
            $$10.downloadTexture($$7, false);
            if (p_284988_ != null) {
               $$10.applyToAllPixels(p_284988_);
            }

            Path $$11 = p_285286_.resolve(p_285408_ + "_" + $$7 + ".png");
            $$10.writeToFile($$11);
            LOGGER.debug("Exported png to: {}", $$11.toAbsolutePath());
         } catch (IOException var15) {
            LOGGER.debug("Unable to write: ", var15);
         }
      }
   }

   public static Path getDebugTexturePath(Path p_262015_) {
      return p_262015_.resolve("screenshots").resolve("debug");
   }

   public static Path getDebugTexturePath() {
      return getDebugTexturePath(Path.of("."));
   }
}
