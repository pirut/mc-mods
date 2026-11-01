package net.minecraft.client.gui.font;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FontSet implements AutoCloseable {
   private static final RandomSource RANDOM = RandomSource.create();
   private static final float LARGE_FORWARD_ADVANCE = 32.0F;
   private final TextureManager textureManager;
   private final ResourceLocation name;
   private BakedGlyph missingGlyph;
   private BakedGlyph whiteGlyph;
   private List<GlyphProvider.Conditional> allProviders = List.of();
   private List<GlyphProvider> activeProviders = List.of();
   private final CodepointMap<BakedGlyph> glyphs = new CodepointMap<>(BakedGlyph[]::new, BakedGlyph[][]::new);
   private final CodepointMap<FontSet.GlyphInfoFilter> glyphInfos = new CodepointMap<>(FontSet.GlyphInfoFilter[]::new, FontSet.GlyphInfoFilter[][]::new);
   private final Int2ObjectMap<IntList> glyphsByWidth = new Int2ObjectOpenHashMap();
   private final List<FontTexture> textures = Lists.newArrayList();

   public FontSet(TextureManager p_95062_, ResourceLocation p_95063_) {
      this.textureManager = p_95062_;
      this.name = p_95063_;
   }

   public void reload(List<GlyphProvider.Conditional> p_326339_, Set<FontOption> p_326154_) {
      this.allProviders = p_326339_;
      this.reload(p_326154_);
   }

   public void reload(Set<FontOption> p_326252_) {
      this.activeProviders = List.of();
      this.resetTextures();
      this.activeProviders = this.selectProviders(this.allProviders, p_326252_);
   }

   private void resetTextures() {
      this.closeTextures();
      this.glyphs.clear();
      this.glyphInfos.clear();
      this.glyphsByWidth.clear();
      this.missingGlyph = SpecialGlyphs.MISSING.bake(this::stitch);
      this.whiteGlyph = SpecialGlyphs.WHITE.bake(this::stitch);
   }

   private List<GlyphProvider> selectProviders(List<GlyphProvider.Conditional> p_326151_, Set<FontOption> p_326291_) {
      IntSet $$2 = new IntOpenHashSet();
      List<GlyphProvider> $$3 = new ArrayList<>();

      for (GlyphProvider.Conditional $$4 : p_326151_) {
         if ($$4.filter().apply(p_326291_)) {
            $$3.add($$4.provider());
            $$2.addAll($$4.provider().getSupportedGlyphs());
         }
      }

      Set<GlyphProvider> $$5 = Sets.newHashSet();
      $$2.forEach(p_232561_ -> {
         for (GlyphProvider $$3x : $$3) {
            GlyphInfo $$4x = $$3x.getGlyph(p_232561_);
            if ($$4x != null) {
               $$5.add($$3x);
               if ($$4x != SpecialGlyphs.MISSING) {
                  ((IntList)this.glyphsByWidth.computeIfAbsent(Mth.ceil($$4x.getAdvance(false)), p_232567_ -> new IntArrayList())).add(p_232561_);
               }
               break;
            }
         }
      });
      return $$3.stream().filter($$5::contains).toList();
   }

   @Override
   public void close() {
      this.closeTextures();
   }

   private void closeTextures() {
      for (FontTexture $$0 : this.textures) {
         $$0.close();
      }

      this.textures.clear();
   }

   private static boolean hasFishyAdvance(GlyphInfo p_243323_) {
      float $$1 = p_243323_.getAdvance(false);
      if (!($$1 < 0.0F) && !($$1 > 32.0F)) {
         float $$2 = p_243323_.getAdvance(true);
         return $$2 < 0.0F || $$2 > 32.0F;
      } else {
         return true;
      }
   }

   private FontSet.GlyphInfoFilter computeGlyphInfo(int p_243321_) {
      GlyphInfo $$1 = null;

      for (GlyphProvider $$2 : this.activeProviders) {
         GlyphInfo $$3 = $$2.getGlyph(p_243321_);
         if ($$3 != null) {
            if ($$1 == null) {
               $$1 = $$3;
            }

            if (!hasFishyAdvance($$3)) {
               return new FontSet.GlyphInfoFilter($$1, $$3);
            }
         }
      }

      return $$1 != null ? new FontSet.GlyphInfoFilter($$1, SpecialGlyphs.MISSING) : FontSet.GlyphInfoFilter.MISSING;
   }

   public GlyphInfo getGlyphInfo(int p_243235_, boolean p_243251_) {
      return this.glyphInfos.computeIfAbsent(p_243235_, this::computeGlyphInfo).select(p_243251_);
   }

   private BakedGlyph computeBakedGlyph(int p_232565_) {
      for (GlyphProvider $$1 : this.activeProviders) {
         GlyphInfo $$2 = $$1.getGlyph(p_232565_);
         if ($$2 != null) {
            return $$2.bake(this::stitch);
         }
      }

      return this.missingGlyph;
   }

   public BakedGlyph getGlyph(int p_95079_) {
      return this.glyphs.computeIfAbsent(p_95079_, this::computeBakedGlyph);
   }

   private BakedGlyph stitch(SheetGlyphInfo p_232557_) {
      for (FontTexture $$1 : this.textures) {
         BakedGlyph $$2 = $$1.add(p_232557_);
         if ($$2 != null) {
            return $$2;
         }
      }

      ResourceLocation $$3 = this.name.withSuffix("/" + this.textures.size());
      boolean $$4 = p_232557_.isColored();
      GlyphRenderTypes $$5 = $$4 ? GlyphRenderTypes.createForColorTexture($$3) : GlyphRenderTypes.createForIntensityTexture($$3);
      FontTexture $$6 = new FontTexture($$5, $$4);
      this.textures.add($$6);
      this.textureManager.register($$3, $$6);
      BakedGlyph $$7 = $$6.add(p_232557_);
      return $$7 == null ? this.missingGlyph : $$7;
   }

   public BakedGlyph getRandomGlyph(GlyphInfo p_95068_) {
      IntList $$1 = (IntList)this.glyphsByWidth.get(Mth.ceil(p_95068_.getAdvance(false)));
      return $$1 != null && !$$1.isEmpty() ? this.getGlyph($$1.getInt(RANDOM.nextInt($$1.size()))) : this.missingGlyph;
   }

   public ResourceLocation name() {
      return this.name;
   }

   public BakedGlyph whiteGlyph() {
      return this.whiteGlyph;
   }

   @OnlyIn(Dist.CLIENT)
   record GlyphInfoFilter(GlyphInfo glyphInfo, GlyphInfo glyphInfoNotFishy) {
      static final FontSet.GlyphInfoFilter MISSING = new FontSet.GlyphInfoFilter(SpecialGlyphs.MISSING, SpecialGlyphs.MISSING);

      GlyphInfo select(boolean p_243218_) {
         return p_243218_ ? this.glyphInfoNotFishy : this.glyphInfo;
      }
   }
}
