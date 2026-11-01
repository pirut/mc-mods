package com.mojang.blaze3d.platform;

import com.mojang.jtracy.MemoryPool;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraft.util.ARGB;
import net.minecraft.util.PngInfo;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
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

@Environment(EnvType.CLIENT)
public final class NativeImage implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final MemoryPool MEMORY_POOL = TracyClient.createMemoryPool("NativeImage");
   private static final Set<StandardOpenOption> OPEN_OPTIONS = EnumSet.of(
      StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
   );
   private final NativeImage.Format format;
   private final int width;
   private final int height;
   private final boolean useStbFree;
   private long pixels;
   private final long size;

   public NativeImage(int width, int height, boolean useCalloc) {
      this(NativeImage.Format.RGBA, width, height, useCalloc);
   }

   public NativeImage(NativeImage.Format format, int width, int height, boolean useCalloc) {
      if (width > 0 && height > 0) {
         this.format = format;
         this.width = width;
         this.height = height;
         this.size = (long)width * height * format.components();
         this.useStbFree = false;
         if (useCalloc) {
            this.pixels = MemoryUtil.nmemCalloc(1L, this.size);
         } else {
            this.pixels = MemoryUtil.nmemAlloc(this.size);
         }

         MEMORY_POOL.malloc(this.pixels, (int)this.size);
         if (this.pixels == 0L) {
            throw new IllegalStateException("Unable to allocate texture of size " + width + "x" + height + " (" + format.components() + " channels)");
         }
      } else {
         throw new IllegalArgumentException("Invalid texture size: " + width + "x" + height);
      }
   }

   public NativeImage(NativeImage.Format format, int width, int height, boolean useStbFree, long pixels) {
      if (width > 0 && height > 0) {
         this.format = format;
         this.width = width;
         this.height = height;
         this.useStbFree = useStbFree;
         this.pixels = pixels;
         this.size = (long)width * height * format.components();
      } else {
         throw new IllegalArgumentException("Invalid texture size: " + width + "x" + height);
      }
   }

   @Override
   public String toString() {
      return "NativeImage[" + this.format + " " + this.width + "x" + this.height + "@" + this.pixels + (this.useStbFree ? "S" : "N") + "]";
   }

   private boolean isOutsideBounds(int x, int y) {
      return x < 0 || x >= this.width || y < 0 || y >= this.height;
   }

   public static NativeImage read(InputStream textureStream) throws IOException {
      return read(NativeImage.Format.RGBA, textureStream);
   }

   public static NativeImage read(@Nullable NativeImage.Format format, InputStream textureStream) throws IOException {
      ByteBuffer byteBuffer = null;

      NativeImage var3;
      try {
         byteBuffer = TextureUtil.readResource(textureStream);
         var3 = read(format, byteBuffer);
      } finally {
         MemoryUtil.memFree(byteBuffer);
         IOUtils.closeQuietly(textureStream);
      }

      return var3;
   }

   public static NativeImage read(ByteBuffer textureData) throws IOException {
      return read(NativeImage.Format.RGBA, textureData);
   }

   public static NativeImage read(byte[] bytes) throws IOException {
      MemoryStack memoryStack = MemoryStack.stackGet();
      int i = memoryStack.getPointer();
      if (i < bytes.length) {
         ByteBuffer byteBuffer = MemoryUtil.memAlloc(bytes.length);

         NativeImage var13;
         try {
            var13 = putAndRead(byteBuffer, bytes);
         } finally {
            MemoryUtil.memFree(byteBuffer);
         }

         return var13;
      } else {
         MemoryStack memoryStack2 = MemoryStack.stackPush();

         NativeImage var5;
         try {
            ByteBuffer byteBuffer2 = memoryStack2.malloc(bytes.length);
            var5 = putAndRead(byteBuffer2, bytes);
         } catch (Throwable var11) {
            if (memoryStack2 != null) {
               try {
                  memoryStack2.close();
               } catch (Throwable var9) {
                  var11.addSuppressed(var9);
               }
            }

            throw var11;
         }

         if (memoryStack2 != null) {
            memoryStack2.close();
         }

         return var5;
      }
   }

   private static NativeImage putAndRead(ByteBuffer buffer, byte[] bytes) throws IOException {
      buffer.put(bytes);
      buffer.rewind();
      return read(buffer);
   }

   public static NativeImage read(@Nullable NativeImage.Format format, ByteBuffer textureData) throws IOException {
      if (format != null && !format.supportedByStb()) {
         throw new UnsupportedOperationException("Don't know how to read format " + format);
      } else if (MemoryUtil.memAddress(textureData) == 0L) {
         throw new IllegalArgumentException("Invalid buffer");
      } else {
         PngInfo.validateHeader(textureData);
         MemoryStack memoryStack = MemoryStack.stackPush();

         NativeImage var9;
         try {
            IntBuffer intBuffer = memoryStack.mallocInt(1);
            IntBuffer intBuffer2 = memoryStack.mallocInt(1);
            IntBuffer intBuffer3 = memoryStack.mallocInt(1);
            ByteBuffer byteBuffer = STBImage.stbi_load_from_memory(textureData, intBuffer, intBuffer2, intBuffer3, format == null ? 0 : format.components);
            if (byteBuffer == null) {
               throw new IOException("Could not load image: " + STBImage.stbi_failure_reason());
            }

            long l = MemoryUtil.memAddress(byteBuffer);
            MEMORY_POOL.malloc(l, byteBuffer.limit());
            var9 = new NativeImage(format == null ? NativeImage.Format.getStbFormat(intBuffer3.get(0)) : format, intBuffer.get(0), intBuffer2.get(0), true, l);
         } catch (Throwable var11) {
            if (memoryStack != null) {
               try {
                  memoryStack.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (memoryStack != null) {
            memoryStack.close();
         }

         return var9;
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

         MEMORY_POOL.free(this.pixels);
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

   private int getPixelABGR(int x, int y) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixelRGBA only works on RGBA images; have %s", this.format));
      } else if (this.isOutsideBounds(x, y)) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", x, y, this.width, this.height));
      } else {
         this.checkAllocated();
         long l = (x + (long)y * this.width) * 4L;
         return MemoryUtil.memGetInt(this.pixels + l);
      }
   }

   public int getPixel(int x, int y) {
      return ARGB.fromABGR(this.getPixelABGR(x, y));
   }

   public void setPixelABGR(int x, int y, int color) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "setPixelRGBA only works on RGBA images; have %s", this.format));
      } else if (this.isOutsideBounds(x, y)) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", x, y, this.width, this.height));
      } else {
         this.checkAllocated();
         long l = (x + (long)y * this.width) * 4L;
         MemoryUtil.memPutInt(this.pixels + l, color);
      }
   }

   public void setPixel(int x, int y, int color) {
      this.setPixelABGR(x, y, ARGB.toABGR(color));
   }

   public NativeImage mappedCopy(IntUnaryOperator function) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "function application only works on RGBA images; have %s", this.format));
      } else {
         this.checkAllocated();
         NativeImage nativeImage = new NativeImage(this.width, this.height, false);
         int i = this.width * this.height;
         IntBuffer intBuffer = MemoryUtil.memIntBuffer(this.pixels, i);
         IntBuffer intBuffer2 = MemoryUtil.memIntBuffer(nativeImage.pixels, i);

         for (int j = 0; j < i; j++) {
            int k = ARGB.fromABGR(intBuffer.get(j));
            int l = function.applyAsInt(k);
            intBuffer2.put(j, ARGB.toABGR(l));
         }

         return nativeImage;
      }
   }

   public int[] getPixelsABGR() {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixels only works on RGBA images; have %s", this.format));
      } else {
         this.checkAllocated();
         int[] is = new int[this.width * this.height];
         MemoryUtil.memIntBuffer(this.pixels, this.width * this.height).get(is);
         return is;
      }
   }

   public int[] getPixels() {
      int[] is = this.getPixelsABGR();

      for (int i = 0; i < is.length; i++) {
         is[i] = ARGB.fromABGR(is[i]);
      }

      return is;
   }

   public byte getLuminanceOrAlpha(int x, int y) {
      if (!this.format.hasLuminanceOrAlpha()) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "no luminance or alpha in %s", this.format));
      } else if (this.isOutsideBounds(x, y)) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", x, y, this.width, this.height));
      } else {
         int i = (x + y * this.width) * this.format.components() + this.format.luminanceOrAlphaOffset() / 8;
         return MemoryUtil.memGetByte(this.pixels + i);
      }
   }

   @Deprecated
   public int[] makePixelArray() {
      if (this.format != NativeImage.Format.RGBA) {
         throw new UnsupportedOperationException("can only call makePixelArray for RGBA images.");
      } else {
         this.checkAllocated();
         int[] is = new int[this.getWidth() * this.getHeight()];

         for (int i = 0; i < this.getHeight(); i++) {
            for (int j = 0; j < this.getWidth(); j++) {
               is[j + i * this.getWidth()] = this.getPixel(j, i);
            }
         }

         return is;
      }
   }

   public void writeToFile(File file) throws IOException {
      this.writeToFile(file.toPath());
   }

   public boolean copyFromFont(FT_Face face, int index) {
      if (this.format.components() != 1) {
         throw new IllegalArgumentException("Can only write fonts into 1-component images.");
      } else if (FreeTypeUtil.checkError(FreeType.FT_Load_Glyph(face, index, 4), "Loading glyph")) {
         return false;
      } else {
         FT_GlyphSlot fT_GlyphSlot = Objects.requireNonNull(face.glyph(), "Glyph not initialized");
         FT_Bitmap fT_Bitmap = fT_GlyphSlot.bitmap();
         if (fT_Bitmap.pixel_mode() != 2) {
            throw new IllegalStateException("Rendered glyph was not 8-bit grayscale");
         } else if (fT_Bitmap.width() == this.getWidth() && fT_Bitmap.rows() == this.getHeight()) {
            int i = fT_Bitmap.width() * fT_Bitmap.rows();
            ByteBuffer byteBuffer = Objects.requireNonNull(fT_Bitmap.buffer(i), "Glyph has no bitmap");
            MemoryUtil.memCopy(MemoryUtil.memAddress(byteBuffer), this.pixels, i);
            return true;
         } else {
            throw new IllegalArgumentException(
               String.format(
                  Locale.ROOT,
                  "Glyph bitmap of size %sx%s does not match image of size: %sx%s",
                  fT_Bitmap.width(),
                  fT_Bitmap.rows(),
                  this.getWidth(),
                  this.getHeight()
               )
            );
         }
      }
   }

   public void writeToFile(Path path) throws IOException {
      if (!this.format.supportedByStb()) {
         throw new UnsupportedOperationException("Don't know how to write format " + this.format);
      } else {
         this.checkAllocated();

         try (WritableByteChannel writableByteChannel = Files.newByteChannel(path, OPEN_OPTIONS)) {
            if (!this.writeToChannel(writableByteChannel)) {
               throw new IOException("Could not write image to the PNG file \"" + path.toAbsolutePath() + "\": " + STBImage.stbi_failure_reason());
            }
         }
      }
   }

   private boolean writeToChannel(WritableByteChannel channel) throws IOException {
      NativeImage.WriteCallback writeCallback = new NativeImage.WriteCallback(channel);

      boolean var4;
      try {
         int i = Math.min(this.getHeight(), Integer.MAX_VALUE / this.getWidth() / this.format.components());
         if (i < this.getHeight()) {
            LOGGER.warn("Dropping image height from {} to {} to fit the size into 32-bit signed int", this.getHeight(), i);
         }

         if (STBImageWrite.nstbi_write_png_to_func(writeCallback.address(), 0L, this.getWidth(), i, this.format.components(), this.pixels, 0) != 0) {
            writeCallback.throwIfException();
            return true;
         }

         var4 = false;
      } finally {
         writeCallback.free();
      }

      return var4;
   }

   public void copyFrom(NativeImage other) {
      if (other.format() != this.format) {
         throw new UnsupportedOperationException("Image formats don't match.");
      } else {
         int i = this.format.components();
         this.checkAllocated();
         other.checkAllocated();
         if (this.width == other.width) {
            MemoryUtil.memCopy(other.pixels, this.pixels, Math.min(this.size, other.size));
         } else {
            int j = Math.min(this.getWidth(), other.getWidth());
            int k = Math.min(this.getHeight(), other.getHeight());

            for (int l = 0; l < k; l++) {
               int m = l * other.getWidth() * i;
               int n = l * this.getWidth() * i;
               MemoryUtil.memCopy(other.pixels + m, this.pixels + n, j);
            }
         }
      }
   }

   public void fillRect(int x, int y, int width, int height, int value) {
      for (int i = y; i < y + height; i++) {
         for (int j = x; j < x + width; j++) {
            this.setPixel(j, i, value);
         }
      }
   }

   public void copyRect(int xFrom, int yFrom, int xToDelta, int yToDelta, int width, int height, boolean mirrorX, boolean mirrorY) {
      this.copyRect(this, xFrom, yFrom, xFrom + xToDelta, yFrom + yToDelta, width, height, mirrorX, mirrorY);
   }

   public void copyRect(NativeImage target, int xFrom, int yFrom, int xTo, int yTo, int width, int height, boolean mirrorX, boolean mirrorY) {
      for (int i = 0; i < height; i++) {
         for (int j = 0; j < width; j++) {
            int k = mirrorX ? width - 1 - j : j;
            int l = mirrorY ? height - 1 - i : i;
            int m = this.getPixelABGR(xFrom + j, yFrom + i);
            target.setPixelABGR(xTo + k, yTo + l, m);
         }
      }
   }

   public void resizeSubRectTo(int x, int y, int width, int height, NativeImage image) {
      this.checkAllocated();
      if (image.format() != this.format) {
         throw new UnsupportedOperationException("resizeSubRectTo only works for images of the same format.");
      } else {
         int i = this.format.components();
         STBImageResize.nstbir_resize_uint8(
            this.pixels + (x + y * this.getWidth()) * i, width, height, this.getWidth() * i, image.pixels, image.getWidth(), image.getHeight(), 0, i
         );
      }
   }

   public void untrack() {
      DebugMemoryUntracker.untrack(this.pixels);
   }

   public long getPointer() {
      return this.pixels;
   }

   @Environment(EnvType.CLIENT)
   public static enum Format {
      RGBA(4, true, true, true, false, true, 0, 8, 16, 255, 24, true),
      RGB(3, true, true, true, false, false, 0, 8, 16, 255, 255, true),
      LUMINANCE_ALPHA(2, false, false, false, true, true, 255, 255, 255, 0, 8, true),
      LUMINANCE(1, false, false, false, true, false, 0, 0, 0, 0, 255, true);

      final int components;
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
         final int components,
         final boolean hasRed,
         final boolean hasGreen,
         final boolean hasBlue,
         final boolean hasLuminance,
         final boolean hasAlpha,
         final int redOffset,
         final int greenOffset,
         final int blueOffset,
         final int luminanceOffset,
         final int alphaOffset,
         final boolean supportedByStb
      ) {
         this.components = components;
         this.hasRed = hasRed;
         this.hasGreen = hasGreen;
         this.hasBlue = hasBlue;
         this.hasLuminance = hasLuminance;
         this.hasAlpha = hasAlpha;
         this.redOffset = redOffset;
         this.greenOffset = greenOffset;
         this.blueOffset = blueOffset;
         this.luminanceOffset = luminanceOffset;
         this.alphaOffset = alphaOffset;
         this.supportedByStb = supportedByStb;
      }

      public int components() {
         return this.components;
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

      static NativeImage.Format getStbFormat(int channels) {
         switch (channels) {
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

   @Environment(EnvType.CLIENT)
   static class WriteCallback extends STBIWriteCallback {
      private final WritableByteChannel output;
      @Nullable
      private IOException exception;

      WriteCallback(WritableByteChannel output) {
         this.output = output;
      }

      public void invoke(long l, long m, int i) {
         ByteBuffer byteBuffer = getData(m, i);

         try {
            this.output.write(byteBuffer);
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
