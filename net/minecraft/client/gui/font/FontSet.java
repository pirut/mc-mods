package net.minecraft.client.gui.font;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class FontSet implements AutoCloseable {
   private static final float LARGE_FORWARD_ADVANCE = 32.0F;
   private static final BakedGlyph INVISIBLE_MISSING_GLYPH = new BakedGlyph() {
      @Override
      public GlyphInfo info() {
         return SpecialGlyphs.MISSING;
      }

      @Nullable
      @Override
      public TextRenderable.Styled createGlyph(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
         return null;
      }
   };
   final GlyphStitcher stitcher;
   final UnbakedGlyph.Stitcher wrappedStitcher = new UnbakedGlyph.Stitcher() {
      @Override
      public BakedGlyph stitch(GlyphInfo info, GlyphBitmap bitmap) {
         return Objects.requireNonNullElse(FontSet.this.stitcher.stitch(info, bitmap), FontSet.this.missingGlyph);
      }

      @Override
      public BakedGlyph getMissing() {
         return FontSet.this.missingGlyph;
      }
   };
   private List<GlyphProvider.Conditional> allProviders = List.of();
   private List<GlyphProvider> activeProviders = List.of();
   private final Int2ObjectMap<IntList> glyphsByWidth = new Int2ObjectOpenHashMap();
   private final CodepointMap<FontSet.SelectedGlyphs> glyphCache = new CodepointMap<>(FontSet.SelectedGlyphs[]::new, FontSet.SelectedGlyphs[][]::new);
   private final IntFunction<FontSet.SelectedGlyphs> glyphGetter = this::computeGlyphInfo;
   BakedGlyph missingGlyph = INVISIBLE_MISSING_GLYPH;
   private final Supplier<BakedGlyph> missingGlyphGetter = () -> this.missingGlyph;
   private final FontSet.SelectedGlyphs missingSelectedGlyphs = new FontSet.SelectedGlyphs(this.missingGlyphGetter, this.missingGlyphGetter);
   @Nullable
   private EffectGlyph whiteGlyph;
   private final GlyphSource anyGlyphs = new FontSet.Source(false);
   private final GlyphSource nonFishyGlyphs = new FontSet.Source(true);

   public FontSet(GlyphStitcher stitcher) {
      this.stitcher = stitcher;
   }

   public void reload(List<GlyphProvider.Conditional> allProviders, Set<FontOption> options) {
      this.allProviders = allProviders;
      this.reload(options);
   }

   public void reload(Set<FontOption> options) {
      this.activeProviders = List.of();
      this.resetTextures();
      this.activeProviders = this.selectProviders(this.allProviders, options);
   }

   private void resetTextures() {
      this.stitcher.reset();
      this.glyphCache.clear();
      this.glyphsByWidth.clear();
      this.missingGlyph = Objects.requireNonNull(SpecialGlyphs.MISSING.bake(this.stitcher));
      this.whiteGlyph = SpecialGlyphs.WHITE.bake(this.stitcher);
   }

   private List<GlyphProvider> selectProviders(List<GlyphProvider.Conditional> providers, Set<FontOption> options) {
      IntSet intSet = new IntOpenHashSet();
      List<GlyphProvider> list = new ArrayList<>();

      for (GlyphProvider.Conditional conditional : providers) {
         if (conditional.filter().apply(options)) {
            list.add(conditional.provider());
            intSet.addAll(conditional.provider().getSupportedGlyphs());
         }
      }

      Set<GlyphProvider> set = Sets.newHashSet();
      intSet.forEach(i -> {
         for (GlyphProvider glyphProvider : list) {
            UnbakedGlyph unbakedGlyph = glyphProvider.getGlyph(i);
            if (unbakedGlyph != null) {
               set.add(glyphProvider);
               if (unbakedGlyph.info() != SpecialGlyphs.MISSING) {
                  ((IntList)this.glyphsByWidth.computeIfAbsent(Mth.ceil(unbakedGlyph.info().getAdvance(false)), ix -> new IntArrayList())).add(i);
               }
               break;
            }
         }
      });
      return list.stream().filter(set::contains).toList();
   }

   @Override
   public void close() {
      this.stitcher.close();
   }

   private static boolean hasFishyAdvance(GlyphInfo glyph) {
      float f = glyph.getAdvance(false);
      if (!(f < 0.0F) && !(f > 32.0F)) {
         float g = glyph.getAdvance(true);
         return g < 0.0F || g > 32.0F;
      } else {
         return true;
      }
   }

   private FontSet.SelectedGlyphs computeGlyphInfo(int character) {
      FontSet.DelayedBake delayedBake = null;

      for (GlyphProvider glyphProvider : this.activeProviders) {
         UnbakedGlyph unbakedGlyph = glyphProvider.getGlyph(character);
         if (unbakedGlyph != null) {
            if (delayedBake == null) {
               delayedBake = new FontSet.DelayedBake(unbakedGlyph);
            }

            if (!hasFishyAdvance(unbakedGlyph.info())) {
               if (delayedBake.unbaked == unbakedGlyph) {
                  return new FontSet.SelectedGlyphs(delayedBake, delayedBake);
               }

               return new FontSet.SelectedGlyphs(delayedBake, new FontSet.DelayedBake(unbakedGlyph));
            }
         }
      }

      return delayedBake != null ? new FontSet.SelectedGlyphs(delayedBake, this.missingGlyphGetter) : this.missingSelectedGlyphs;
   }

   FontSet.SelectedGlyphs getGlyph(int character) {
      return this.glyphCache.computeIfAbsent(character, this.glyphGetter);
   }

   public BakedGlyph getRandomGlyph(RandomSource random, int width) {
      IntList intList = (IntList)this.glyphsByWidth.get(width);
      return intList != null && !intList.isEmpty() ? this.getGlyph(intList.getInt(random.nextInt(intList.size()))).nonFishy().get() : this.missingGlyph;
   }

   public EffectGlyph whiteGlyph() {
      return Objects.requireNonNull(this.whiteGlyph);
   }

   public GlyphSource source(boolean filterFishyGlyphs) {
      return filterFishyGlyphs ? this.nonFishyGlyphs : this.anyGlyphs;
   }

   @Environment(EnvType.CLIENT)
   class DelayedBake implements Supplier<BakedGlyph> {
      final UnbakedGlyph unbaked;
      @Nullable
      private BakedGlyph baked;

      DelayedBake(final UnbakedGlyph unbaked) {
         this.unbaked = unbaked;
      }

      public BakedGlyph get() {
         if (this.baked == null) {
            this.baked = this.unbaked.bake(FontSet.this.wrappedStitcher);
         }

         return this.baked;
      }
   }

   @Environment(EnvType.CLIENT)
   record SelectedGlyphs(Supplier<BakedGlyph> any, Supplier<BakedGlyph> nonFishy) {
      Supplier<BakedGlyph> select(boolean filterFishy) {
         return filterFishy ? this.nonFishy : this.any;
      }
   }

   @Environment(EnvType.CLIENT)
   public class Source implements GlyphSource {
      private final boolean filterFishyGlyphs;

      public Source(final boolean filterFishyGlyphs) {
         this.filterFishyGlyphs = filterFishyGlyphs;
      }

      @Override
      public BakedGlyph getGlyph(int index) {
         return FontSet.this.getGlyph(index).select(this.filterFishyGlyphs).get();
      }

      @Override
      public BakedGlyph getRandomGlyph(RandomSource random, int width) {
         return FontSet.this.getRandomGlyph(random, width);
      }
   }
}
