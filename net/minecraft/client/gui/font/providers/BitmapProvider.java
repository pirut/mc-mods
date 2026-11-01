package net.minecraft.client.gui.font.providers;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class BitmapProvider implements GlyphProvider {
   static final Logger LOGGER = LogUtils.getLogger();
   private final NativeImage image;
   private final CodepointMap<BitmapProvider.Glyph> glyphs;

   BitmapProvider(NativeImage p_285380_, CodepointMap<BitmapProvider.Glyph> p_285445_) {
      this.image = p_285380_;
      this.glyphs = p_285445_;
   }

   @Override
   public void close() {
      this.image.close();
   }

   @Nullable
   @Override
   public GlyphInfo getGlyph(int p_232638_) {
      return this.glyphs.get(p_232638_);
   }

   @Override
   public IntSet getSupportedGlyphs() {
      return IntSets.unmodifiable(this.glyphs.keySet());
   }

   @OnlyIn(Dist.CLIENT)
   public record Definition(ResourceLocation file, int height, int ascent, int[][] codepointGrid) implements GlyphProviderDefinition {
      private static final Codec<int[][]> CODEPOINT_GRID_CODEC = Codec.STRING.listOf().xmap(p_286900_ -> {
         int $$1 = p_286900_.size();
         int[][] $$2 = new int[$$1][];

         for (int $$3 = 0; $$3 < $$1; $$3++) {
            $$2[$$3] = ((String)p_286900_.get($$3)).codePoints().toArray();
         }

         return $$2;
      }, p_286828_ -> {
         List<String> $$1 = new ArrayList<>(p_286828_.length);

         for (int[] $$2 : p_286828_) {
            $$1.add(new String($$2, 0, $$2.length));
         }

         return $$1;
      }).validate(BitmapProvider.Definition::validateDimensions);
      public static final MapCodec<BitmapProvider.Definition> CODEC = RecordCodecBuilder.mapCodec(
            p_286905_ -> p_286905_.group(
                  ResourceLocation.CODEC.fieldOf("file").forGetter(BitmapProvider.Definition::file),
                  Codec.INT.optionalFieldOf("height", 8).forGetter(BitmapProvider.Definition::height),
                  Codec.INT.fieldOf("ascent").forGetter(BitmapProvider.Definition::ascent),
                  CODEPOINT_GRID_CODEC.fieldOf("chars").forGetter(BitmapProvider.Definition::codepointGrid)
               )
               .apply(p_286905_, BitmapProvider.Definition::new)
         )
         .validate(BitmapProvider.Definition::validate);

      private static DataResult<int[][]> validateDimensions(int[][] p_286348_) {
         int $$1 = p_286348_.length;
         if ($$1 == 0) {
            return DataResult.error(() -> "Expected to find data in codepoint grid");
         } else {
            int[] $$2 = p_286348_[0];
            int $$3 = $$2.length;
            if ($$3 == 0) {
               return DataResult.error(() -> "Expected to find data in codepoint grid");
            } else {
               for (int $$4 = 1; $$4 < $$1; $$4++) {
                  int[] $$5 = p_286348_[$$4];
                  if ($$5.length != $$3) {
                     return DataResult.error(
                        () -> "Lines in codepoint grid have to be the same length (found: "
                           + $$5.length
                           + " codepoints, expected: "
                           + $$3
                           + "), pad with \\u0000"
                     );
                  }
               }

               return DataResult.success(p_286348_);
            }
         }
      }

      private static DataResult<BitmapProvider.Definition> validate(BitmapProvider.Definition p_286662_) {
         return p_286662_.ascent > p_286662_.height
            ? DataResult.error(() -> "Ascent " + p_286662_.ascent + " higher than height " + p_286662_.height)
            : DataResult.success(p_286662_);
      }

      @Override
      public GlyphProviderType type() {
         return GlyphProviderType.BITMAP;
      }

      @Override
      public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack() {
         return Either.left(this::load);
      }

      private GlyphProvider load(ResourceManager p_286694_) throws IOException {
         ResourceLocation $$1 = this.file.withPrefix("textures/");

         BitmapProvider var22;
         try (InputStream $$2 = p_286694_.open($$1)) {
            NativeImage $$3 = NativeImage.read(NativeImage.Format.RGBA, $$2);
            int $$4 = $$3.getWidth();
            int $$5 = $$3.getHeight();
            int $$6 = $$4 / this.codepointGrid[0].length;
            int $$7 = $$5 / this.codepointGrid.length;
            float $$8 = (float)this.height / $$7;
            CodepointMap<BitmapProvider.Glyph> $$9 = new CodepointMap<>(BitmapProvider.Glyph[]::new, BitmapProvider.Glyph[][]::new);

            for (int $$10 = 0; $$10 < this.codepointGrid.length; $$10++) {
               int $$11 = 0;

               for (int $$12 : this.codepointGrid[$$10]) {
                  int $$13 = $$11++;
                  if ($$12 != 0) {
                     int $$14 = this.getActualGlyphWidth($$3, $$6, $$7, $$13, $$10);
                     BitmapProvider.Glyph $$15 = $$9.put(
                        $$12, new BitmapProvider.Glyph($$8, $$3, $$13 * $$6, $$10 * $$7, $$6, $$7, (int)(0.5 + $$14 * $$8) + 1, this.ascent)
                     );
                     if ($$15 != null) {
                        BitmapProvider.LOGGER.warn("Codepoint '{}' declared multiple times in {}", Integer.toHexString($$12), $$1);
                     }
                  }
               }
            }

            var22 = new BitmapProvider($$3, $$9);
         }

         return var22;
      }

      private int getActualGlyphWidth(NativeImage p_286449_, int p_286656_, int p_286554_, int p_286657_, int p_286307_) {
         int $$5;
         for ($$5 = p_286656_ - 1; $$5 >= 0; $$5--) {
            int $$6 = p_286657_ * p_286656_ + $$5;

            for (int $$7 = 0; $$7 < p_286554_; $$7++) {
               int $$8 = p_286307_ * p_286554_ + $$7;
               if (p_286449_.getLuminanceOrAlpha($$6, $$8) != 0) {
                  return $$5 + 1;
               }
            }
         }

         return $$5 + 1;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Glyph(float scale, NativeImage image, int offsetX, int offsetY, int width, int height, int advance, int ascent) implements GlyphInfo {
      @Override
      public float getAdvance() {
         return this.advance;
      }

      @Override
      public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> p_232640_) {
         return p_232640_.apply(new SheetGlyphInfo() {
            @Override
            public float getOversample() {
               return 1.0F / Glyph.this.scale;
            }

            @Override
            public int getPixelWidth() {
               return Glyph.this.width;
            }

            @Override
            public int getPixelHeight() {
               return Glyph.this.height;
            }

            @Override
            public float getBearingTop() {
               return Glyph.this.ascent;
            }

            @Override
            public void upload(int p_232658_, int p_232659_) {
               Glyph.this.image.upload(0, p_232658_, p_232659_, Glyph.this.offsetX, Glyph.this.offsetY, Glyph.this.width, Glyph.this.height, false, false);
            }

            @Override
            public boolean isColored() {
               return Glyph.this.image.format().components() > 1;
            }
         });
      }
   }
}
