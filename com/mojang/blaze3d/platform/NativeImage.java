package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraft.util.FastColor;
import net.minecraft.util.PngInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageResize;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FreeType;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public final class NativeImage implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Set<StandardOpenOption> OPEN_OPTIONS = EnumSet.of(
      StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
   );
   private final NativeImage.Format format;
   private final int width;
   private final int height;
   private final boolean useStbFree;
   private long pixels;
   private final long size;

   public NativeImage(int p_84968_, int p_84969_, boolean p_84970_) {
      this(NativeImage.Format.RGBA, p_84968_, p_84969_, p_84970_);
   }

   public NativeImage(NativeImage.Format p_84972_, int p_84973_, int p_84974_, boolean p_84975_) {
      if (p_84973_ > 0 && p_84974_ > 0) {
         this.format = p_84972_;
         this.width = p_84973_;
         this.height = p_84974_;
         this.size = (long)p_84973_ * p_84974_ * p_84972_.components();
         this.useStbFree = false;
         if (p_84975_) {
            this.pixels = MemoryUtil.nmemCalloc(1L, this.size);
         } else {
            this.pixels = MemoryUtil.nmemAlloc(this.size);
         }

         if (this.pixels == 0L) {
            throw new IllegalStateException("Unable to allocate texture of size " + p_84973_ + "x" + p_84974_ + " (" + p_84972_.components() + " channels)");
         }
      } else {
         throw new IllegalArgumentException("Invalid texture size: " + p_84973_ + "x" + p_84974_);
      }
   }

   private NativeImage(NativeImage.Format p_84977_, int p_84978_, int p_84979_, boolean p_84980_, long p_84981_) {
      if (p_84978_ > 0 && p_84979_ > 0) {
         this.format = p_84977_;
         this.width = p_84978_;
         this.height = p_84979_;
         this.useStbFree = p_84980_;
         this.pixels = p_84981_;
         this.size = (long)p_84978_ * p_84979_ * p_84977_.components();
      } else {
         throw new IllegalArgumentException("Invalid texture size: " + p_84978_ + "x" + p_84979_);
      }
   }

   @Override
   public String toString() {
      return "NativeImage[" + this.format + " " + this.width + "x" + this.height + "@" + this.pixels + (this.useStbFree ? "S" : "N") + "]";
   }

   private boolean isOutsideBounds(int p_166423_, int p_166424_) {
      return p_166423_ < 0 || p_166423_ >= this.width || p_166424_ < 0 || p_166424_ >= this.height;
   }

   public static NativeImage read(InputStream p_85059_) throws IOException {
      return read(NativeImage.Format.RGBA, p_85059_);
   }

   public static NativeImage read(@Nullable NativeImage.Format p_85049_, InputStream p_85050_) throws IOException {
      ByteBuffer $$2 = null;

      NativeImage var3;
      try {
         $$2 = TextureUtil.readResource(p_85050_);
         $$2.rewind();
         var3 = read(p_85049_, $$2);
      } finally {
         MemoryUtil.memFree($$2);
         IOUtils.closeQuietly(p_85050_);
      }

      return var3;
   }

   public static NativeImage read(ByteBuffer p_85063_) throws IOException {
      return read(NativeImage.Format.RGBA, p_85063_);
   }

   public static NativeImage read(byte[] p_273041_) throws IOException {
      MemoryStack $$1 = MemoryStack.stackPush();

      NativeImage var3;
      try {
         ByteBuffer $$2 = $$1.malloc(p_273041_.length);
         $$2.put(p_273041_);
         $$2.rewind();
         var3 = read($$2);
      } catch (Throwable var5) {
         if ($$1 != null) {
            try {
               $$1.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if ($$1 != null) {
         $$1.close();
      }

      return var3;
   }

   public static NativeImage read(@Nullable NativeImage.Format p_85052_, ByteBuffer p_85053_) throws IOException {
      if (p_85052_ != null && !p_85052_.supportedByStb()) {
         throw new UnsupportedOperationException("Don't know how to read format " + p_85052_);
      } else if (MemoryUtil.memAddress(p_85053_) == 0L) {
         throw new IllegalArgumentException("Invalid buffer");
      } else {
         PngInfo.validateHeader(p_85053_);
         MemoryStack $$2 = MemoryStack.stackPush();

         NativeImage var7;
         try {
            IntBuffer $$3 = $$2.mallocInt(1);
            IntBuffer $$4 = $$2.mallocInt(1);
            IntBuffer $$5 = $$2.mallocInt(1);
            ByteBuffer $$6 = STBImage.stbi_load_from_memory(p_85053_, $$3, $$4, $$5, p_85052_ == null ? 0 : p_85052_.components);
            if ($$6 == null) {
               throw new IOException("Could not load image: " + STBImage.stbi_failure_reason());
            }

            var7 = new NativeImage(
               p_85052_ == null ? NativeImage.Format.getStbFormat($$5.get(0)) : p_85052_, $$3.get(0), $$4.get(0), true, MemoryUtil.memAddress($$6)
            );
         } catch (Throwable var9) {
            if ($$2 != null) {
               try {
                  $$2.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if ($$2 != null) {
            $$2.close();
         }

         return var7;
      }
   }

   private static void setFilter(boolean p_85082_, boolean p_85083_) {
      RenderSystem.assertOnRenderThreadOrInit();
      if (p_85082_) {
         GlStateManager._texParameter(3553, 10241, p_85083_ ? 9987 : 9729);
         GlStateManager._texParameter(3553, 10240, 9729);
      } else {
         GlStateManager._texParameter(3553, 10241, p_85083_ ? 9986 : 9728);
         GlStateManager._texParameter(3553, 10240, 9728);
      }
   }

   private void checkAllocated() {
      if (this.pixels == 0L) {
         throw new IllegalStateException("Image is not allocated.");
      }
   }

   @Override
   public void close() {
      if (this.pixels != 0L) {
         if (this.useStbFree) {
            STBImage.nstbi_image_free(this.pixels);
         } else {
            MemoryUtil.nmemFree(this.pixels);
         }
      }

      this.pixels = 0L;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public NativeImage.Format format() {
      return this.format;
   }

   public int getPixelRGBA(int p_84986_, int p_84987_) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixelRGBA only works on RGBA images; have %s", this.format));
      } else if (this.isOutsideBounds(p_84986_, p_84987_)) {
         throw new IllegalArgumentException(
            String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", p_84986_, p_84987_, this.width, this.height)
         );
      } else {
         this.checkAllocated();
         long $$2 = (p_84986_ + (long)p_84987_ * this.width) * 4L;
         return MemoryUtil.memGetInt(this.pixels + $$2);
      }
   }

   public void setPixelRGBA(int p_84989_, int p_84990_, int p_84991_) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "setPixelRGBA only works on RGBA images; have %s", this.format));
      } else if (this.isOutsideBounds(p_84989_, p_84990_)) {
         throw new IllegalArgumentException(
            String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", p_84989_, p_84990_, this.width, this.height)
         );
      } else {
         this.checkAllocated();
         long $$3 = (p_84989_ + (long)p_84990_ * this.width) * 4L;
         MemoryUtil.memPutInt(this.pixels + $$3, p_84991_);
      }
   }

   public NativeImage mappedCopy(IntUnaryOperator p_267084_) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "function application only works on RGBA images; have %s", this.format));
      } else {
         this.checkAllocated();
         NativeImage $$1 = new NativeImage(this.width, this.height, false);
         int $$2 = this.width * this.height;
         IntBuffer $$3 = MemoryUtil.memIntBuffer(this.pixels, $$2);
         IntBuffer $$4 = MemoryUtil.memIntBuffer($$1.pixels, $$2);

         for (int $$5 = 0; $$5 < $$2; $$5++) {
            $$4.put($$5, p_267084_.applyAsInt($$3.get($$5)));
         }

         return $$1;
      }
   }

   public void applyToAllPixels(IntUnaryOperator p_285490_) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "function application only works on RGBA images; have %s", this.format));
      } else {
         this.checkAllocated();
         int $$1 = this.width * this.height;
         IntBuffer $$2 = MemoryUtil.memIntBuffer(this.pixels, $$1);

         for (int $$3 = 0; $$3 < $$1; $$3++) {
            $$2.put($$3, p_285490_.applyAsInt($$2.get($$3)));
         }
      }
   }

   public int[] getPixelsRGBA() {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixelsRGBA only works on RGBA images; have %s", this.format));
      } else {
         this.checkAllocated();
         int[] $$0 = new int[this.width * this.height];
         MemoryUtil.memIntBuffer(this.pixels, this.width * this.height).get($$0);
         return $$0;
      }
   }

   public void setPixelLuminance(int p_166403_, int p_166404_, byte p_166405_) {
      RenderSystem.assertOnRenderThread();
      if (!this.format.hasLuminance()) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "setPixelLuminance only works on image with luminance; have %s", this.format));
      } else if (this.isOutsideBounds(p_166403_, p_166404_)) {
         throw new IllegalArgumentException(
            String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", p_166403_, p_166404_, this.width, this.height)
         );
      } else {
         this.checkAllocated();
         long $$3 = (p_166403_ + (long)p_166404_ * this.width) * this.format.components() + this.format.luminanceOffset() / 8;
         MemoryUtil.memPutByte(this.pixels + $$3, p_166405_);
      }
   }

   public byte getRedOrLuminance(int p_166409_, int p_166410_) {
      RenderSystem.assertOnRenderThread();
      if (!this.format.hasLuminanceOrRed()) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "no red or luminance in %s", this.format));
      } else if (this.isOutsideBounds(p_166409_, p_166410_)) {
         throw new IllegalArgumentException(
            String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", p_166409_, p_166410_, this.width, this.height)
         );
      } else {
         int $$2 = (p_166409_ + p_166410_ * this.width) * this.format.components() + this.format.luminanceOrRedOffset() / 8;
         return MemoryUtil.memGetByte(this.pixels + $$2);
      }
   }

   public byte getGreenOrLuminance(int p_166416_, int p_166417_) {
      RenderSystem.assertOnRenderThread();
      if (!this.format.hasLuminanceOrGreen()) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "no green or luminance in %s", this.format));
      } else if (this.isOutsideBounds(p_166416_, p_166417_)) {
         throw new IllegalArgumentException(
            String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", p_166416_, p_166417_, this.width, this.height)
         );
      } else {
         int $$2 = (p_166416_ + p_166417_ * this.width) * this.format.components() + this.format.luminanceOrGreenOffset() / 8;
         return MemoryUtil.memGetByte(this.pixels + $$2);
      }
   }

   public byte getBlueOrLuminance(int p_166419_, int p_166420_) {
      RenderSystem.assertOnRenderThread();
      if (!this.format.hasLuminanceOrBlue()) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "no blue or luminance in %s", this.format));
      } else if (this.isOutsideBounds(p_166419_, p_166420_)) {
         throw new IllegalArgumentException(
            String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", p_166419_, p_166420_, this.width, this.height)
         );
      } else {
         int $$2 = (p_166419_ + p_166420_ * this.width) * this.format.components() + this.format.luminanceOrBlueOffset() / 8;
         return MemoryUtil.memGetByte(this.pixels + $$2);
      }
   }

   public byte getLuminanceOrAlpha(int p_85088_, int p_85089_) {
      if (!this.format.hasLuminanceOrAlpha()) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "no luminance or alpha in %s", this.format));
      } else if (this.isOutsideBounds(p_85088_, p_85089_)) {
         throw new IllegalArgumentException(
            String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", p_85088_, p_85089_, this.width, this.height)
         );
      } else {
         int $$2 = (p_85088_ + p_85089_ * this.width) * this.format.components() + this.format.luminanceOrAlphaOffset() / 8;
         return MemoryUtil.memGetByte(this.pixels + $$2);
      }
   }

   public void blendPixel(int p_166412_, int p_166413_, int p_166414_) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new UnsupportedOperationException("Can only call blendPixel with RGBA format");
      } else {
         int $$3 = this.getPixelRGBA(p_166412_, p_166413_);
         float $$4 = FastColor.ABGR32.alpha(p_166414_) / 255.0F;
         float $$5 = FastColor.ABGR32.blue(p_166414_) / 255.0F;
         float $$6 = FastColor.ABGR32.green(p_166414_) / 255.0F;
         float $$7 = FastColor.ABGR32.red(p_166414_) / 255.0F;
         float $$8 = FastColor.ABGR32.alpha($$3) / 255.0F;
         float $$9 = FastColor.ABGR32.blue($$3) / 255.0F;
         float $$10 = FastColor.ABGR32.green($$3) / 255.0F;
         float $$11 = FastColor.ABGR32.red($$3) / 255.0F;
         float $$13 = 1.0F - $$4;
         float $$14 = $$4 * $$4 + $$8 * $$13;
         float $$15 = $$5 * $$4 + $$9 * $$13;
         float $$16 = $$6 * $$4 + $$10 * $$13;
         float $$17 = $$7 * $$4 + $$11 * $$13;
         if ($$14 > 1.0F) {
            $$14 = 1.0F;
         }

         if ($$15 > 1.0F) {
            $$15 = 1.0F;
         }

         if ($$16 > 1.0F) {
            $$16 = 1.0F;
         }

         if ($$17 > 1.0F) {
            $$17 = 1.0F;
         }

         int $$18 = (int)($$14 * 255.0F);
         int $$19 = (int)($$15 * 255.0F);
         int $$20 = (int)($$16 * 255.0F);
         int $$21 = (int)($$17 * 255.0F);
         this.setPixelRGBA(p_166412_, p_166413_, FastColor.ABGR32.color($$18, $$19, $$20, $$21));
      }
   }

   @Deprecated
   public int[] makePixelArray() {
      if (this.format != NativeImage.Format.RGBA) {
         throw new UnsupportedOperationException("can only call makePixelArray for RGBA images.");
      } else {
         this.checkAllocated();
         int[] $$0 = new int[this.getWidth() * this.getHeight()];

         for (int $$1 = 0; $$1 < this.getHeight(); $$1++) {
            for (int $$2 = 0; $$2 < this.getWidth(); $$2++) {
               int $$3 = this.getPixelRGBA($$2, $$1);
               $$0[$$2 + $$1 * this.getWidth()] = FastColor.ARGB32.color(
                  FastColor.ABGR32.alpha($$3), FastColor.ABGR32.red($$3), FastColor.ABGR32.green($$3), FastColor.ABGR32.blue($$3)
               );
            }
         }

         return $$0;
      }
   }

   public void upload(int p_85041_, int p_85042_, int p_85043_, boolean p_85044_) {
      this.upload(p_85041_, p_85042_, p_85043_, 0, 0, this.width, this.height, false, p_85044_);
   }

   public void upload(int p_85004_, int p_85005_, int p_85006_, int p_85007_, int p_85008_, int p_85009_, int p_85010_, boolean p_85011_, boolean p_85012_) {
      this.upload(p_85004_, p_85005_, p_85006_, p_85007_, p_85008_, p_85009_, p_85010_, false, false, p_85011_, p_85012_);
   }

   public void upload(
      int p_85014_,
      int p_85015_,
      int p_85016_,
      int p_85017_,
      int p_85018_,
      int p_85019_,
      int p_85020_,
      boolean p_85021_,
      boolean p_85022_,
      boolean p_85023_,
      boolean p_85024_
   ) {
      if (!RenderSystem.isOnRenderThreadOrInit()) {
         RenderSystem.recordRenderCall(
            () -> this._upload(p_85014_, p_85015_, p_85016_, p_85017_, p_85018_, p_85019_, p_85020_, p_85021_, p_85022_, p_85023_, p_85024_)
         );
      } else {
         this._upload(p_85014_, p_85015_, p_85016_, p_85017_, p_85018_, p_85019_, p_85020_, p_85021_, p_85022_, p_85023_, p_85024_);
      }
   }

   private void _upload(
      int p_85091_,
      int p_85092_,
      int p_85093_,
      int p_85094_,
      int p_85095_,
      int p_85096_,
      int p_85097_,
      boolean p_85098_,
      boolean p_85099_,
      boolean p_85100_,
      boolean p_85101_
   ) {
      try {
         RenderSystem.assertOnRenderThreadOrInit();
         this.checkAllocated();
         setFilter(p_85098_, p_85100_);
         if (p_85096_ == this.getWidth()) {
            GlStateManager._pixelStore(3314, 0);
         } else {
            GlStateManager._pixelStore(3314, this.getWidth());
         }

         GlStateManager._pixelStore(3316, p_85094_);
         GlStateManager._pixelStore(3315, p_85095_);
         this.format.setUnpackPixelStoreState();
         GlStateManager._texSubImage2D(3553, p_85091_, p_85092_, p_85093_, p_85096_, p_85097_, this.format.glFormat(), 5121, this.pixels);
         if (p_85099_) {
            GlStateManager._texParameter(3553, 10242, 33071);
            GlStateManager._texParameter(3553, 10243, 33071);
         }
      } finally {
         if (p_85101_) {
            this.close();
         }
      }
   }

   public void downloadTexture(int p_85046_, boolean p_85047_) {
      RenderSystem.assertOnRenderThread();
      this.checkAllocated();
      this.format.setPackPixelStoreState();
      GlStateManager._getTexImage(3553, p_85046_, this.format.glFormat(), 5121, this.pixels);
      if (p_85047_ && this.format.hasAlpha()) {
         for (int $$2 = 0; $$2 < this.getHeight(); $$2++) {
            for (int $$3 = 0; $$3 < this.getWidth(); $$3++) {
               this.setPixelRGBA($$3, $$2, this.getPixelRGBA($$3, $$2) | 255 << this.format.alphaOffset());
            }
         }
      }
   }

   public void downloadDepthBuffer(float p_166401_) {
      RenderSystem.assertOnRenderThread();
      if (this.format.components() != 1) {
         throw new IllegalStateException("Depth buffer must be stored in NativeImage with 1 component.");
      } else {
         this.checkAllocated();
         this.format.setPackPixelStoreState();
         GlStateManager._readPixels(0, 0, this.width, this.height, 6402, 5121, this.pixels);
      }
   }

   public void drawPixels() {
      RenderSystem.assertOnRenderThread();
      this.format.setUnpackPixelStoreState();
      GlStateManager._glDrawPixels(this.width, this.height, this.format.glFormat(), 5121, this.pixels);
   }

   public void writeToFile(File p_85057_) throws IOException {
      this.writeToFile(p_85057_.toPath());
   }

   public boolean copyFromFont(FT_Face p_320757_, int p_85070_) {
      if (this.format.components() != 1) {
         throw new IllegalArgumentException("Can only write fonts into 1-component images.");
      } else if (FreeTypeUtil.checkError(FreeType.FT_Load_Glyph(p_320757_, p_85070_, 4), "Loading glyph")) {
         return false;
      } else {
         FT_GlyphSlot $$2 = Objects.requireNonNull(p_320757_.glyph(), "Glyph not initialized");
         FT_Bitmap $$3 = $$2.bitmap();
         if ($$3.pixel_mode() != 2) {
            throw new IllegalStateException("Rendered glyph was not 8-bit grayscale");
         } else if ($$3.width() == this.getWidth() && $$3.rows() == this.getHeight()) {
            int $$4 = $$3.width() * $$3.rows();
            ByteBuffer $$5 = Objects.requireNonNull($$3.buffer($$4), "Glyph has no bitmap");
            MemoryUtil.memCopy(MemoryUtil.memAddress($$5), this.pixels, $$4);
            return true;
         } else {
            throw new IllegalArgumentException(
               String.format(
                  Locale.ROOT, "Glyph bitmap of size %sx%s does not match image of size: %sx%s", $$3.width(), $$3.rows(), this.getWidth(), this.getHeight()
               )
            );
         }
      }
   }

   public void writeToFile(Path p_85067_) throws IOException {
      if (!this.format.supportedByStb()) {
         throw new UnsupportedOperationException("Don't know how to write format " + this.format);
      } else {
         this.checkAllocated();

         try (WritableByteChannel $$1 = Files.newByteChannel(p_85067_, OPEN_OPTIONS)) {
            if (!this.writeToChannel($$1)) {
               throw new IOException("Could not write image to the PNG file \"" + p_85067_.toAbsolutePath() + "\": " + STBImage.stbi_failure_reason());
            }
         }
      }
   }

   public byte[] asByteArray() throws IOException {
      byte[] var3;
      try (
         ByteArrayOutputStream $$0 = new ByteArrayOutputStream();
         WritableByteChannel $$1 = Channels.newChannel($$0);
      ) {
         if (!this.writeToChannel($$1)) {
            throw new IOException("Could not write image to byte array: " + STBImage.stbi_failure_reason());
         }

         var3 = $$0.toByteArray();
      }

      return var3;
   }

   private boolean writeToChannel(WritableByteChannel p_85065_) throws IOException {
      NativeImage.WriteCallback $$1 = new NativeImage.WriteCallback(p_85065_);

      boolean var4;
      try {
         int $$2 = Math.min(this.getHeight(), Integer.MAX_VALUE / this.getWidth() / this.format.components());
         if ($$2 < this.getHeight()) {
            LOGGER.warn("Dropping image height from {} to {} to fit the size into 32-bit signed int", this.getHeight(), $$2);
         }

         if (STBImageWrite.nstbi_write_png_to_func($$1.address(), 0L, this.getWidth(), $$2, this.format.components(), this.pixels, 0) != 0) {
            $$1.throwIfException();
            return true;
         }

         var4 = false;
      } finally {
         $$1.free();
      }

      return var4;
   }

   public void copyFrom(NativeImage p_85055_) {
      if (p_85055_.format() != this.format) {
         throw new UnsupportedOperationException("Image formats don't match.");
      } else {
         int $$1 = this.format.components();
         this.checkAllocated();
         p_85055_.checkAllocated();
         if (this.width == p_85055_.width) {
            MemoryUtil.memCopy(p_85055_.pixels, this.pixels, Math.min(this.size, p_85055_.size));
         } else {
            int $$2 = Math.min(this.getWidth(), p_85055_.getWidth());
            int $$3 = Math.min(this.getHeight(), p_85055_.getHeight());

            for (int $$4 = 0; $$4 < $$3; $$4++) {
               int $$5 = $$4 * p_85055_.getWidth() * $$1;
               int $$6 = $$4 * this.getWidth() * $$1;
               MemoryUtil.memCopy(p_85055_.pixels + $$5, this.pixels + $$6, $$2);
            }
         }
      }
   }

   public void fillRect(int p_84998_, int p_84999_, int p_85000_, int p_85001_, int p_85002_) {
      for (int $$5 = p_84999_; $$5 < p_84999_ + p_85001_; $$5++) {
         for (int $$6 = p_84998_; $$6 < p_84998_ + p_85000_; $$6++) {
            this.setPixelRGBA($$6, $$5, p_85002_);
         }
      }
   }

   public void copyRect(int p_85026_, int p_85027_, int p_85028_, int p_85029_, int p_85030_, int p_85031_, boolean p_85032_, boolean p_85033_) {
      this.copyRect(this, p_85026_, p_85027_, p_85026_ + p_85028_, p_85027_ + p_85029_, p_85030_, p_85031_, p_85032_, p_85033_);
   }

   public void copyRect(
      NativeImage p_261644_, int p_262056_, int p_261490_, int p_261959_, int p_262110_, int p_261522_, int p_261505_, boolean p_261480_, boolean p_261622_
   ) {
      for (int $$9 = 0; $$9 < p_261505_; $$9++) {
         for (int $$10 = 0; $$10 < p_261522_; $$10++) {
            int $$11 = p_261480_ ? p_261522_ - 1 - $$10 : $$10;
            int $$12 = p_261622_ ? p_261505_ - 1 - $$9 : $$9;
            int $$13 = this.getPixelRGBA(p_262056_ + $$10, p_261490_ + $$9);
            p_261644_.setPixelRGBA(p_261959_ + $$11, p_262110_ + $$12, $$13);
         }
      }
   }

   public void flipY() {
      this.checkAllocated();
      int $$0 = this.format.components();
      int $$1 = this.getWidth() * $$0;
      long $$2 = MemoryUtil.nmemAlloc($$1);

      try {
         for (int $$3 = 0; $$3 < this.getHeight() / 2; $$3++) {
            int $$4 = $$3 * this.getWidth() * $$0;
            int $$5 = (this.getHeight() - 1 - $$3) * this.getWidth() * $$0;
            MemoryUtil.memCopy(this.pixels + $$4, $$2, $$1);
            MemoryUtil.memCopy(this.pixels + $$5, this.pixels + $$4, $$1);
            MemoryUtil.memCopy($$2, this.pixels + $$5, $$1);
         }
      } finally {
         MemoryUtil.nmemFree($$2);
      }
   }

   public void resizeSubRectTo(int p_85035_, int p_85036_, int p_85037_, int p_85038_, NativeImage p_85039_) {
      this.checkAllocated();
      if (p_85039_.format() != this.format) {
         throw new UnsupportedOperationException("resizeSubRectTo only works for images of the same format.");
      } else {
         int $$5 = this.format.components();
         STBImageResize.nstbir_resize_uint8(
            this.pixels + (p_85035_ + p_85036_ * this.getWidth()) * $$5,
            p_85037_,
            p_85038_,
            this.getWidth() * $$5,
            p_85039_.pixels,
            p_85039_.getWidth(),
            p_85039_.getHeight(),
            0,
            $$5
         );
      }
   }

   public void untrack() {
      DebugMemoryUntracker.untrack(this.pixels);
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Format {
      RGBA(4, 6408, true, true, true, false, true, 0, 8, 16, 255, 24, true),
      RGB(3, 6407, true, true, true, false, false, 0, 8, 16, 255, 255, true),
      LUMINANCE_ALPHA(2, 33319, false, false, false, true, true, 255, 255, 255, 0, 8, true),
      LUMINANCE(1, 6403, false, false, false, true, false, 0, 0, 0, 0, 255, true);

      final int components;
      private final int glFormat;
      private final boolean hasRed;
      private final boolean hasGreen;
      private final boolean hasBlue;
      private final boolean hasLuminance;
      private final boolean hasAlpha;
      private final int redOffset;
      private final int greenOffset;
      private final int blueOffset;
      private final int luminanceOffset;
      private final int alphaOffset;
      private final boolean supportedByStb;

      private Format(
         int p_85148_,
         int p_85149_,
         boolean p_85150_,
         boolean p_85151_,
         boolean p_85152_,
         boolean p_85153_,
         boolean p_85154_,
         int p_85155_,
         int p_85156_,
         int p_85157_,
         int p_85158_,
         int p_85159_,
         boolean p_85160_
      ) {
         this.components = p_85148_;
         this.glFormat = p_85149_;
         this.hasRed = p_85150_;
         this.hasGreen = p_85151_;
         this.hasBlue = p_85152_;
         this.hasLuminance = p_85153_;
         this.hasAlpha = p_85154_;
         this.redOffset = p_85155_;
         this.greenOffset = p_85156_;
         this.blueOffset = p_85157_;
         this.luminanceOffset = p_85158_;
         this.alphaOffset = p_85159_;
         this.supportedByStb = p_85160_;
      }

      public int components() {
         return this.components;
      }

      public void setPackPixelStoreState() {
         RenderSystem.assertOnRenderThread();
         GlStateManager._pixelStore(3333, this.components());
      }

      public void setUnpackPixelStoreState() {
         RenderSystem.assertOnRenderThreadOrInit();
         GlStateManager._pixelStore(3317, this.components());
      }

      public int glFormat() {
         return this.glFormat;
      }

      public boolean hasRed() {
         return this.hasRed;
      }

      public boolean hasGreen() {
         return this.hasGreen;
      }

      public boolean hasBlue() {
         return this.hasBlue;
      }

      public boolean hasLuminance() {
         return this.hasLuminance;
      }

      public boolean hasAlpha() {
         return this.hasAlpha;
      }

      public int redOffset() {
         return this.redOffset;
      }

      public int greenOffset() {
         return this.greenOffset;
      }

      public int blueOffset() {
         return this.blueOffset;
      }

      public int luminanceOffset() {
         return this.luminanceOffset;
      }

      public int alphaOffset() {
         return this.alphaOffset;
      }

      public boolean hasLuminanceOrRed() {
         return this.hasLuminance || this.hasRed;
      }

      public boolean hasLuminanceOrGreen() {
         return this.hasLuminance || this.hasGreen;
      }

      public boolean hasLuminanceOrBlue() {
         return this.hasLuminance || this.hasBlue;
      }

      public boolean hasLuminanceOrAlpha() {
         return this.hasLuminance || this.hasAlpha;
      }

      public int luminanceOrRedOffset() {
         return this.hasLuminance ? this.luminanceOffset : this.redOffset;
      }

      public int luminanceOrGreenOffset() {
         return this.hasLuminance ? this.luminanceOffset : this.greenOffset;
      }

      public int luminanceOrBlueOffset() {
         return this.hasLuminance ? this.luminanceOffset : this.blueOffset;
      }

      public int luminanceOrAlphaOffset() {
         return this.hasLuminance ? this.luminanceOffset : this.alphaOffset;
      }

      public boolean supportedByStb() {
         return this.supportedByStb;
      }

      static NativeImage.Format getStbFormat(int p_85168_) {
         switch (p_85168_) {
            case 1:
               return LUMINANCE;
            case 2:
               return LUMINANCE_ALPHA;
            case 3:
               return RGB;
            case 4:
            default:
               return RGBA;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum InternalGlFormat {
      RGBA(6408),
      RGB(6407),
      RG(33319),
      RED(6403);

      private final int glFormat;

      private InternalGlFormat(int p_85190_) {
         this.glFormat = p_85190_;
      }

      public int glFormat() {
         return this.glFormat;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class WriteCallback extends STBIWriteCallback {
      private final WritableByteChannel output;
      @Nullable
      private IOException exception;

      WriteCallback(WritableByteChannel p_85198_) {
         this.output = p_85198_;
      }

      public void invoke(long p_85204_, long p_85205_, int p_85206_) {
         ByteBuffer $$3 = getData(p_85205_, p_85206_);

         try {
            this.output.write($$3);
         } catch (IOException var8) {
            this.exception = var8;
         }
      }

      public void throwIfException() throws IOException {
         if (this.exception != null) {
            throw this.exception;
         }
      }
   }
}
