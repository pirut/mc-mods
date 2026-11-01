package com.mojang.blaze3d.font;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FT_Vector;
import org.lwjgl.util.freetype.FreeType;

@Environment(EnvType.CLIENT)
public class TrueTypeGlyphProvider implements GlyphProvider {
   @Nullable
   private ByteBuffer fontMemory;
   @Nullable
   private FT_Face face;
   final float oversample;
   private final CodepointMap<TrueTypeGlyphProvider.GlyphEntry> glyphs = new CodepointMap<>(
      TrueTypeGlyphProvider.GlyphEntry[]::new, TrueTypeGlyphProvider.GlyphEntry[][]::new
   );

   public TrueTypeGlyphProvider(ByteBuffer fontMemory, FT_Face face, float size, float oversample, float shiftX, float shiftY, String skip) {
      this.fontMemory = fontMemory;
      this.face = face;
      this.oversample = oversample;
      IntSet intSet = new IntArraySet();
      skip.codePoints().forEach(intSet::add);
      int i = Math.round(size * oversample);
      FreeType.FT_Set_Pixel_Sizes(face, i, i);
      float f = shiftX * oversample;
      float g = -shiftY * oversample;
      MemoryStack memoryStack = MemoryStack.stackPush();

      try {
         FT_Vector fT_Vector = FreeTypeUtil.setVector(FT_Vector.malloc(memoryStack), f, g);
         FreeType.FT_Set_Transform(face, null, fT_Vector);
         IntBuffer intBuffer = memoryStack.mallocInt(1);
         int j = (int)FreeType.FT_Get_First_Char(face, intBuffer);

         while (true) {
            int k = intBuffer.get(0);
            if (k == 0) {
               break;
            }

            if (!intSet.contains(j)) {
               this.glyphs.put(j, new TrueTypeGlyphProvider.GlyphEntry(k));
            }

            j = (int)FreeType.FT_Get_Next_Char(face, j, intBuffer);
         }
      } catch (Throwable var18) {
         if (memoryStack != null) {
            try {
               memoryStack.close();
            } catch (Throwable var17) {
               var18.addSuppressed(var17);
            }
         }

         throw var18;
      }

      if (memoryStack != null) {
         memoryStack.close();
      }
   }

   @Nullable
   @Override
   public UnbakedGlyph getGlyph(int character) {
      TrueTypeGlyphProvider.GlyphEntry glyphEntry = this.glyphs.get(character);
      return glyphEntry != null ? this.getOrLoadGlyphInfo(character, glyphEntry) : null;
   }

   private UnbakedGlyph getOrLoadGlyphInfo(int character, TrueTypeGlyphProvider.GlyphEntry glyphEntry) {
      UnbakedGlyph unbakedGlyph = glyphEntry.glyph;
      if (unbakedGlyph == null) {
         FT_Face fT_Face = this.validateFontOpen();
         synchronized (fT_Face) {
            unbakedGlyph = glyphEntry.glyph;
            if (unbakedGlyph == null) {
               unbakedGlyph = this.loadGlyph(character, fT_Face, glyphEntry.index);
               glyphEntry.glyph = unbakedGlyph;
            }
         }
      }

      return unbakedGlyph;
   }

   private UnbakedGlyph loadGlyph(int character, FT_Face face, int index) {
      int i = FreeType.FT_Load_Glyph(face, index, 4194312);
      if (i != 0) {
         FreeTypeUtil.assertError(i, String.format(Locale.ROOT, "Loading glyph U+%06X", character));
      }

      FT_GlyphSlot fT_GlyphSlot = face.glyph();
      if (fT_GlyphSlot == null) {
         throw new NullPointerException(String.format(Locale.ROOT, "Glyph U+%06X not initialized", character));
      } else {
         float f = FreeTypeUtil.x(fT_GlyphSlot.advance());
         FT_Bitmap fT_Bitmap = fT_GlyphSlot.bitmap();
         int j = fT_GlyphSlot.bitmap_left();
         int k = fT_GlyphSlot.bitmap_top();
         int l = fT_Bitmap.width();
         int m = fT_Bitmap.rows();
         return (UnbakedGlyph)(l > 0 && m > 0 ? new TrueTypeGlyphProvider.Glyph(j, k, l, m, f, index) : new EmptyGlyph(f / this.oversample));
      }
   }

   FT_Face validateFontOpen() {
      if (this.fontMemory != null && this.face != null) {
         return this.face;
      } else {
         throw new IllegalStateException("Provider already closed");
      }
   }

   @Override
   public void close() {
      if (this.face != null) {
         synchronized (FreeTypeUtil.LIBRARY_LOCK) {
            FreeTypeUtil.checkError(FreeType.FT_Done_Face(this.face), "Deleting face");
         }

         this.face = null;
      }

      MemoryUtil.memFree(this.fontMemory);
      this.fontMemory = null;
   }

   @Override
   public IntSet getSupportedGlyphs() {
      return this.glyphs.keySet();
   }

   @Environment(EnvType.CLIENT)
   class Glyph implements UnbakedGlyph {
      final int width;
      final int height;
      final float bearingX;
      final float bearingY;
      private final GlyphInfo info;
      final int index;

      Glyph(final float bearingX, final float bearingY, final int width, final int height, final float advance, final int index) {
         this.width = width;
         this.height = height;
         this.info = GlyphInfo.simple(advance / TrueTypeGlyphProvider.this.oversample);
         this.bearingX = bearingX / TrueTypeGlyphProvider.this.oversample;
         this.bearingY = bearingY / TrueTypeGlyphProvider.this.oversample;
         this.index = index;
      }

      @Override
      public GlyphInfo info() {
         return this.info;
      }

      @Override
      public BakedGlyph bake(UnbakedGlyph.Stitcher stitcher) {
         return stitcher.stitch(
            this.info,
            new GlyphBitmap() {
               @Override
               public int getPixelWidth() {
                  return Glyph.this.width;
               }

               @Override
               public int getPixelHeight() {
                  return Glyph.this.height;
               }

               @Override
               public float getOversample() {
                  return TrueTypeGlyphProvider.this.oversample;
               }

               @Override
               public float getBearingLeft() {
                  return Glyph.this.bearingX;
               }

               @Override
               public float getBearingTop() {
                  return Glyph.this.bearingY;
               }

               @Override
               public void upload(int x, int y, GpuTexture texture) {
                  FT_Face fT_Face = TrueTypeGlyphProvider.this.validateFontOpen();

                  try (NativeImage nativeImage = new NativeImage(NativeImage.Format.LUMINANCE, Glyph.this.width, Glyph.this.height, false)) {
                     if (nativeImage.copyFromFont(fT_Face, Glyph.this.index)) {
                        RenderSystem.getDevice()
                           .createCommandEncoder()
                           .writeToTexture(texture, nativeImage, 0, 0, x, y, Glyph.this.width, Glyph.this.height, 0, 0);
                     }
                  }
               }

               @Override
               public boolean isColored() {
                  return false;
               }
            }
         );
      }
   }

   @Environment(EnvType.CLIENT)
   static class GlyphEntry {
      final int index;
      @Nullable
      volatile UnbakedGlyph glyph;

      GlyphEntry(int index) {
         this.index = index;
      }
   }
}
