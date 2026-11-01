package net.minecraft.client.gui.font.providers;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.FastBufferedInputStream;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class UnihexProvider implements GlyphProvider {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int GLYPH_HEIGHT = 16;
   private static final int DIGITS_PER_BYTE = 2;
   private static final int DIGITS_FOR_WIDTH_8 = 32;
   private static final int DIGITS_FOR_WIDTH_16 = 64;
   private static final int DIGITS_FOR_WIDTH_24 = 96;
   private static final int DIGITS_FOR_WIDTH_32 = 128;
   private final CodepointMap<UnihexProvider.Glyph> glyphs;

   UnihexProvider(CodepointMap<UnihexProvider.Glyph> p_285457_) {
      this.glyphs = p_285457_;
   }

   @Nullable
   @Override
   public GlyphInfo getGlyph(int p_285239_) {
      return this.glyphs.get(p_285239_);
   }

   @Override
   public IntSet getSupportedGlyphs() {
      return this.glyphs.keySet();
   }

   @VisibleForTesting
   static void unpackBitsToBytes(IntBuffer p_285211_, int p_285508_, int p_285312_, int p_285412_) {
      int $$4 = 32 - p_285312_ - 1;
      int $$5 = 32 - p_285412_ - 1;

      for (int $$6 = $$4; $$6 >= $$5; $$6--) {
         if ($$6 < 32 && $$6 >= 0) {
            boolean $$7 = (p_285508_ >> $$6 & 1) != 0;
            p_285211_.put($$7 ? -1 : 0);
         } else {
            p_285211_.put(0);
         }
      }
   }

   static void unpackBitsToBytes(IntBuffer p_285283_, UnihexProvider.LineData p_285485_, int p_284940_, int p_284950_) {
      for (int $$4 = 0; $$4 < 16; $$4++) {
         int $$5 = p_285485_.line($$4);
         unpackBitsToBytes(p_285283_, $$5, p_284940_, p_284950_);
      }
   }

   @VisibleForTesting
   static void readFromStream(InputStream p_285315_, UnihexProvider.ReaderOutput p_285353_) throws IOException {
      int $$2 = 0;
      ByteList $$3 = new ByteArrayList(128);

      while (true) {
         boolean $$4 = copyUntil(p_285315_, $$3, 58);
         int $$5 = $$3.size();
         if ($$5 == 0 && !$$4) {
            return;
         }

         if (!$$4 || $$5 != 4 && $$5 != 5 && $$5 != 6) {
            throw new IllegalArgumentException("Invalid entry at line " + $$2 + ": expected 4, 5 or 6 hex digits followed by a colon");
         }

         int $$6 = 0;

         for (int $$7 = 0; $$7 < $$5; $$7++) {
            $$6 = $$6 << 4 | decodeHex($$2, $$3.getByte($$7));
         }

         $$3.clear();
         copyUntil(p_285315_, $$3, 10);
         int $$8 = $$3.size();

         UnihexProvider.LineData $$9 = switch ($$8) {
            case 32 -> UnihexProvider.ByteContents.read($$2, $$3);
            case 64 -> UnihexProvider.ShortContents.read($$2, $$3);
            case 96 -> UnihexProvider.IntContents.read24($$2, $$3);
            case 128 -> UnihexProvider.IntContents.read32($$2, $$3);
            default -> throw new IllegalArgumentException(
               "Invalid entry at line " + $$2 + ": expected hex number describing (8,16,24,32) x 16 bitmap, followed by a new line"
            );
         };
         p_285353_.accept($$6, $$9);
         $$2++;
         $$3.clear();
      }
   }

   static int decodeHex(int p_285205_, ByteList p_285268_, int p_285345_) {
      return decodeHex(p_285205_, p_285268_.getByte(p_285345_));
   }

   private static int decodeHex(int p_284952_, byte p_285036_) {
      return switch (p_285036_) {
         case 48 -> 0;
         case 49 -> 1;
         case 50 -> 2;
         case 51 -> 3;
         case 52 -> 4;
         case 53 -> 5;
         case 54 -> 6;
         case 55 -> 7;
         case 56 -> 8;
         case 57 -> 9;
         default -> throw new IllegalArgumentException("Invalid entry at line " + p_284952_ + ": expected hex digit, got " + (char)p_285036_);
         case 65 -> 10;
         case 66 -> 11;
         case 67 -> 12;
         case 68 -> 13;
         case 69 -> 14;
         case 70 -> 15;
      };
   }

   private static boolean copyUntil(InputStream p_284994_, ByteList p_285351_, int p_285177_) throws IOException {
      while (true) {
         int $$3 = p_284994_.read();
         if ($$3 == -1) {
            return false;
         }

         if ($$3 == p_285177_) {
            return true;
         }

         p_285351_.add((byte)$$3);
      }
   }

   @OnlyIn(Dist.CLIENT)
   record ByteContents(byte[] contents) implements UnihexProvider.LineData {
      @Override
      public int line(int p_285203_) {
         return this.contents[p_285203_] << 24;
      }

      static UnihexProvider.LineData read(int p_285080_, ByteList p_285481_) {
         byte[] $$2 = new byte[16];
         int $$3 = 0;

         for (int $$4 = 0; $$4 < 16; $$4++) {
            int $$5 = UnihexProvider.decodeHex(p_285080_, p_285481_, $$3++);
            int $$6 = UnihexProvider.decodeHex(p_285080_, p_285481_, $$3++);
            byte $$7 = (byte)($$5 << 4 | $$6);
            $$2[$$4] = $$7;
         }

         return new UnihexProvider.ByteContents($$2);
      }

      @Override
      public int bitWidth() {
         return 8;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Definition implements GlyphProviderDefinition {
      public static final MapCodec<UnihexProvider.Definition> CODEC = RecordCodecBuilder.mapCodec(
         p_286579_ -> p_286579_.group(
               ResourceLocation.CODEC.fieldOf("hex_file").forGetter(p_286591_ -> p_286591_.hexFile),
               UnihexProvider.OverrideRange.CODEC.listOf().fieldOf("size_overrides").forGetter(p_286528_ -> p_286528_.sizeOverrides)
            )
            .apply(p_286579_, UnihexProvider.Definition::new)
      );
      private final ResourceLocation hexFile;
      private final List<UnihexProvider.OverrideRange> sizeOverrides;

      private Definition(ResourceLocation p_286378_, List<UnihexProvider.OverrideRange> p_286770_) {
         this.hexFile = p_286378_;
         this.sizeOverrides = p_286770_;
      }

      @Override
      public GlyphProviderType type() {
         return GlyphProviderType.UNIHEX;
      }

      @Override
      public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack() {
         return Either.left(this::load);
      }

      private GlyphProvider load(ResourceManager p_286472_) throws IOException {
         UnihexProvider var3;
         try (InputStream $$1 = p_286472_.open(this.hexFile)) {
            var3 = this.loadData($$1);
         }

         return var3;
      }

      private UnihexProvider loadData(InputStream p_286795_) throws IOException {
         CodepointMap<UnihexProvider.LineData> $$1 = new CodepointMap<>(UnihexProvider.LineData[]::new, UnihexProvider.LineData[][]::new);
         UnihexProvider.ReaderOutput $$2 = $$1::put;

         UnihexProvider var17;
         try (ZipInputStream $$3 = new ZipInputStream(p_286795_)) {
            ZipEntry $$4;
            while (($$4 = $$3.getNextEntry()) != null) {
               String $$5 = $$4.getName();
               if ($$5.endsWith(".hex")) {
                  UnihexProvider.LOGGER.info("Found {}, loading", $$5);
                  UnihexProvider.readFromStream(new FastBufferedInputStream($$3), $$2);
               }
            }

            CodepointMap<UnihexProvider.Glyph> $$6 = new CodepointMap<>(UnihexProvider.Glyph[]::new, UnihexProvider.Glyph[][]::new);

            for (UnihexProvider.OverrideRange $$7 : this.sizeOverrides) {
               int $$8 = $$7.from;
               int $$9 = $$7.to;
               UnihexProvider.Dimensions $$10 = $$7.dimensions;

               for (int $$11 = $$8; $$11 <= $$9; $$11++) {
                  UnihexProvider.LineData $$12 = $$1.remove($$11);
                  if ($$12 != null) {
                     $$6.put($$11, new UnihexProvider.Glyph($$12, $$10.left, $$10.right));
                  }
               }
            }

            $$1.forEach((p_286721_, p_286722_) -> {
               int $$3x = p_286722_.calculateWidth();
               int $$4x = UnihexProvider.Dimensions.left($$3x);
               int $$5 = UnihexProvider.Dimensions.right($$3x);
               $$6.put(p_286721_, new UnihexProvider.Glyph(p_286722_, $$4x, $$5));
            });
            var17 = new UnihexProvider($$6);
         }

         return var17;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record Dimensions(int left, int right) {
      public static final MapCodec<UnihexProvider.Dimensions> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_285497_ -> p_285497_.group(
               Codec.INT.fieldOf("left").forGetter(UnihexProvider.Dimensions::left), Codec.INT.fieldOf("right").forGetter(UnihexProvider.Dimensions::right)
            )
            .apply(p_285497_, UnihexProvider.Dimensions::new)
      );
      public static final Codec<UnihexProvider.Dimensions> CODEC = MAP_CODEC.codec();

      public int pack() {
         return pack(this.left, this.right);
      }

      public static int pack(int p_285339_, int p_285120_) {
         return (p_285339_ & 0xFF) << 8 | p_285120_ & 0xFF;
      }

      public static int left(int p_285195_) {
         return (byte)(p_285195_ >> 8);
      }

      public static int right(int p_285419_) {
         return (byte)p_285419_;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Glyph(UnihexProvider.LineData contents, int left, int right) implements GlyphInfo {
      public int width() {
         return this.right - this.left + 1;
      }

      @Override
      public float getAdvance() {
         return this.width() / 2 + 1;
      }

      @Override
      public float getShadowOffset() {
         return 0.5F;
      }

      @Override
      public float getBoldOffset() {
         return 0.5F;
      }

      @Override
      public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> p_285377_) {
         return p_285377_.apply(new SheetGlyphInfo() {
            @Override
            public float getOversample() {
               return 2.0F;
            }

            @Override
            public int getPixelWidth() {
               return Glyph.this.width();
            }

            @Override
            public int getPixelHeight() {
               return 16;
            }

            @Override
            public void upload(int p_285473_, int p_285510_) {
               IntBuffer $$2 = MemoryUtil.memAllocInt(Glyph.this.width() * 16);
               UnihexProvider.unpackBitsToBytes($$2, Glyph.this.contents, Glyph.this.left, Glyph.this.right);
               $$2.rewind();
               GlStateManager.upload(0, p_285473_, p_285510_, Glyph.this.width(), 16, NativeImage.Format.RGBA, $$2, MemoryUtil::memFree);
            }

            @Override
            public boolean isColored() {
               return true;
            }
         });
      }
   }

   @OnlyIn(Dist.CLIENT)
   record IntContents(int[] contents, int bitWidth) implements UnihexProvider.LineData {
      private static final int SIZE_24 = 24;

      @Override
      public int line(int p_285172_) {
         return this.contents[p_285172_];
      }

      static UnihexProvider.LineData read24(int p_285362_, ByteList p_285123_) {
         int[] $$2 = new int[16];
         int $$3 = 0;
         int $$4 = 0;

         for (int $$5 = 0; $$5 < 16; $$5++) {
            int $$6 = UnihexProvider.decodeHex(p_285362_, p_285123_, $$4++);
            int $$7 = UnihexProvider.decodeHex(p_285362_, p_285123_, $$4++);
            int $$8 = UnihexProvider.decodeHex(p_285362_, p_285123_, $$4++);
            int $$9 = UnihexProvider.decodeHex(p_285362_, p_285123_, $$4++);
            int $$10 = UnihexProvider.decodeHex(p_285362_, p_285123_, $$4++);
            int $$11 = UnihexProvider.decodeHex(p_285362_, p_285123_, $$4++);
            int $$12 = $$6 << 20 | $$7 << 16 | $$8 << 12 | $$9 << 8 | $$10 << 4 | $$11;
            $$2[$$5] = $$12 << 8;
            $$3 |= $$12;
         }

         return new UnihexProvider.IntContents($$2, 24);
      }

      public static UnihexProvider.LineData read32(int p_285222_, ByteList p_285346_) {
         int[] $$2 = new int[16];
         int $$3 = 0;
         int $$4 = 0;

         for (int $$5 = 0; $$5 < 16; $$5++) {
            int $$6 = UnihexProvider.decodeHex(p_285222_, p_285346_, $$4++);
            int $$7 = UnihexProvider.decodeHex(p_285222_, p_285346_, $$4++);
            int $$8 = UnihexProvider.decodeHex(p_285222_, p_285346_, $$4++);
            int $$9 = UnihexProvider.decodeHex(p_285222_, p_285346_, $$4++);
            int $$10 = UnihexProvider.decodeHex(p_285222_, p_285346_, $$4++);
            int $$11 = UnihexProvider.decodeHex(p_285222_, p_285346_, $$4++);
            int $$12 = UnihexProvider.decodeHex(p_285222_, p_285346_, $$4++);
            int $$13 = UnihexProvider.decodeHex(p_285222_, p_285346_, $$4++);
            int $$14 = $$6 << 28 | $$7 << 24 | $$8 << 20 | $$9 << 16 | $$10 << 12 | $$11 << 8 | $$12 << 4 | $$13;
            $$2[$$5] = $$14;
            $$3 |= $$14;
         }

         return new UnihexProvider.IntContents($$2, 32);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface LineData {
      int line(int var1);

      int bitWidth();

      default int mask() {
         int $$0 = 0;

         for (int $$1 = 0; $$1 < 16; $$1++) {
            $$0 |= this.line($$1);
         }

         return $$0;
      }

      default int calculateWidth() {
         int $$0 = this.mask();
         int $$1 = this.bitWidth();
         int $$2;
         int $$3;
         if ($$0 == 0) {
            $$2 = 0;
            $$3 = $$1;
         } else {
            $$2 = Integer.numberOfLeadingZeros($$0);
            $$3 = 32 - Integer.numberOfTrailingZeros($$0) - 1;
         }

         return UnihexProvider.Dimensions.pack($$2, $$3);
      }
   }

   @OnlyIn(Dist.CLIENT)
   record OverrideRange(int from, int to, UnihexProvider.Dimensions dimensions) {
      private static final Codec<UnihexProvider.OverrideRange> RAW_CODEC = RecordCodecBuilder.create(
         p_285088_ -> p_285088_.group(
               ExtraCodecs.CODEPOINT.fieldOf("from").forGetter(UnihexProvider.OverrideRange::from),
               ExtraCodecs.CODEPOINT.fieldOf("to").forGetter(UnihexProvider.OverrideRange::to),
               UnihexProvider.Dimensions.MAP_CODEC.forGetter(UnihexProvider.OverrideRange::dimensions)
            )
            .apply(p_285088_, UnihexProvider.OverrideRange::new)
      );
      public static final Codec<UnihexProvider.OverrideRange> CODEC = RAW_CODEC.validate(
         p_285215_ -> p_285215_.from >= p_285215_.to
            ? DataResult.error(() -> "Invalid range: [" + p_285215_.from + ";" + p_285215_.to + "]")
            : DataResult.success(p_285215_)
      );
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface ReaderOutput {
      void accept(int var1, UnihexProvider.LineData var2);
   }

   @OnlyIn(Dist.CLIENT)
   record ShortContents(short[] contents) implements UnihexProvider.LineData {
      @Override
      public int line(int p_285158_) {
         return this.contents[p_285158_] << 16;
      }

      static UnihexProvider.LineData read(int p_285528_, ByteList p_284958_) {
         short[] $$2 = new short[16];
         int $$3 = 0;

         for (int $$4 = 0; $$4 < 16; $$4++) {
            int $$5 = UnihexProvider.decodeHex(p_285528_, p_284958_, $$3++);
            int $$6 = UnihexProvider.decodeHex(p_285528_, p_284958_, $$3++);
            int $$7 = UnihexProvider.decodeHex(p_285528_, p_284958_, $$3++);
            int $$8 = UnihexProvider.decodeHex(p_285528_, p_284958_, $$3++);
            short $$9 = (short)($$5 << 12 | $$6 << 8 | $$7 << 4 | $$8);
            $$2[$$4] = $$9;
         }

         return new UnihexProvider.ShortContents($$2);
      }

      @Override
      public int bitWidth() {
         return 16;
      }
   }
}
