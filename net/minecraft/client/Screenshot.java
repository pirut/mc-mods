package net.minecraft.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.ScreenshotEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Screenshot {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String SCREENSHOT_DIR = "screenshots";

   public static void grab(File p_92290_, RenderTarget p_92293_, Consumer<Component> p_92294_) {
      grab(p_92290_, null, p_92293_, 1, p_92294_);
   }

   public static void grab(File p_92296_, @Nullable String p_92297_, RenderTarget p_92300_, int p_423482_, Consumer<Component> p_92301_) {
      takeScreenshot(
         p_92300_,
         p_423482_,
         p_465362_ -> {
            File file1 = new File(p_92296_, "screenshots");
            file1.mkdir();
            File file2;
            if (p_92297_ == null) {
               file2 = getFile(file1);
            } else {
               file2 = new File(file1, p_92297_);
            }

            ScreenshotEvent event = ClientHooks.onScreenshot(p_465362_, file2);
            if (event.isCanceled()) {
               p_92301_.accept(event.getCancelMessage());
            } else {
               File target = event.getScreenshotFile();
               Util.ioPool()
                  .execute(
                     () -> {
                        try {
                           NativeImage $$4x = p_465362_;

                           try {
                              $$4x.writeToFile(target);
                              if (event.getResultMessage() != null) {
                                 p_92301_.accept(event.getResultMessage());
                              } else {
                                 Component component = Component.literal(target.getName())
                                    .withStyle(ChatFormatting.UNDERLINE)
                                    .withStyle(p_392495_ -> p_392495_.withClickEvent(new ClickEvent.OpenFile(target.getAbsoluteFile())));
                                 p_92301_.accept(Component.translatable("screenshot.success", component));
                              }
                           } catch (Throwable var8) {
                              if (p_465362_ != null) {
                                 try {
                                    $$4x.close();
                                 } catch (Throwable var7x) {
                                    var8.addSuppressed(var7x);
                                 }
                              }

                              throw var8;
                           }

                           if (p_465362_ != null) {
                              p_465362_.close();
                           }
                        } catch (Exception var9) {
                           LOGGER.warn("Couldn't save screenshot", var9);
                           p_92301_.accept(Component.translatable("screenshot.failure", var9.getMessage()));
                        }
                     }
                  );
            }
         }
      );
   }

   public static void takeScreenshot(RenderTarget p_92282_, Consumer<NativeImage> p_406336_) {
      takeScreenshot(p_92282_, 1, p_406336_);
   }

   public static void takeScreenshot(RenderTarget p_423538_, int p_423674_, Consumer<NativeImage> p_423666_) {
      int i = p_423538_.width;
      int j = p_423538_.height;
      GpuTexture gputexture = p_423538_.getColorTexture();
      if (gputexture == null) {
         throw new IllegalStateException("Tried to capture screenshot of an incomplete framebuffer");
      } else if (i % p_423674_ == 0 && j % p_423674_ == 0) {
         GpuBuffer gpubuffer = RenderSystem.getDevice().createBuffer(() -> "Screenshot buffer", 9, (long)i * j * gputexture.getFormat().pixelSize());
         CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
         RenderSystem.getDevice()
            .createCommandEncoder()
            .copyTextureToBuffer(
               gputexture,
               gpubuffer,
               0L,
               () -> {
                  try (GpuBuffer.MappedView gpubuffer$mappedview = commandencoder.mapBuffer(gpubuffer, true, false)) {
                     int k = j / p_423674_;
                     int l = i / p_423674_;
                     NativeImage nativeimage = new NativeImage(l, k, false);

                     for (int i1 = 0; i1 < k; i1++) {
                        for (int j1 = 0; j1 < l; j1++) {
                           if (p_423674_ == 1) {
                              int i3 = gpubuffer$mappedview.data().getInt((j1 + i1 * i) * gputexture.getFormat().pixelSize());
                              nativeimage.setPixelABGR(j1, j - i1 - 1, i3 | 0xFF000000);
                           } else {
                              int k1 = 0;
                              int l1 = 0;
                              int i2 = 0;

                              for (int j2 = 0; j2 < p_423674_; j2++) {
                                 for (int k2 = 0; k2 < p_423674_; k2++) {
                                    int l2 = gpubuffer$mappedview.data()
                                       .getInt((j1 * p_423674_ + j2 + (i1 * p_423674_ + k2) * i) * gputexture.getFormat().pixelSize());
                                    k1 += ARGB.red(l2);
                                    l1 += ARGB.green(l2);
                                    i2 += ARGB.blue(l2);
                                 }
                              }

                              int j3 = p_423674_ * p_423674_;
                              nativeimage.setPixelABGR(j1, k - i1 - 1, ARGB.color(255, k1 / j3, l1 / j3, i2 / j3));
                           }
                        }
                     }

                     p_423666_.accept(nativeimage);
                  }

                  gpubuffer.close();
               },
               0
            );
      } else {
         throw new IllegalArgumentException("Image size is not divisible by downscale factor");
      }
   }

   private static File getFile(File p_92288_) {
      String s = Util.getFilenameFormattedDateTime();
      int i = 1;

      while (true) {
         File file1 = new File(p_92288_, s + (i == 1 ? "" : "_" + i) + ".png");
         if (!file1.exists()) {
            return file1;
         }

         i++;
      }
   }
}
