package net.minecraft.client.gui.font.providers;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

@OnlyIn(Dist.CLIENT)
public record TrueTypeGlyphProviderDefinition(ResourceLocation location, float size, float oversample, TrueTypeGlyphProviderDefinition.Shift shift, String skip)
   implements GlyphProviderDefinition {
   private static final Codec<String> SKIP_LIST_CODEC = Codec.withAlternative(Codec.STRING, Codec.STRING.listOf(), p_286852_ -> String.join("", p_286852_));
   public static final MapCodec<TrueTypeGlyphProviderDefinition> CODEC = RecordCodecBuilder.mapCodec(
      p_286284_ -> p_286284_.group(
            ResourceLocation.CODEC.fieldOf("file").forGetter(TrueTypeGlyphProviderDefinition::location),
            Codec.FLOAT.optionalFieldOf("size", 11.0F).forGetter(TrueTypeGlyphProviderDefinition::size),
            Codec.FLOAT.optionalFieldOf("oversample", 1.0F).forGetter(TrueTypeGlyphProviderDefinition::oversample),
            TrueTypeGlyphProviderDefinition.Shift.CODEC
               .optionalFieldOf("shift", TrueTypeGlyphProviderDefinition.Shift.NONE)
               .forGetter(TrueTypeGlyphProviderDefinition::shift),
            SKIP_LIST_CODEC.optionalFieldOf("skip", "").forGetter(TrueTypeGlyphProviderDefinition::skip)
         )
         .apply(p_286284_, TrueTypeGlyphProviderDefinition::new)
   );

   @Override
   public GlyphProviderType type() {
      return GlyphProviderType.TTF;
   }

   @Override
   public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack() {
      return Either.left(this::load);
   }

   private GlyphProvider load(ResourceManager p_286229_) throws IOException {
      FT_Face $$1 = null;
      ByteBuffer $$2 = null;

      try {
         TrueTypeGlyphProvider var20;
         try (InputStream $$3 = p_286229_.open(this.location.withPrefix("font/"))) {
            $$2 = TextureUtil.readResource($$3);
            $$2.flip();
            synchronized (FreeTypeUtil.LIBRARY_LOCK) {
               MemoryStack $$4 = MemoryStack.stackPush();

               try {
                  PointerBuffer $$5 = $$4.mallocPointer(1);
                  FreeTypeUtil.assertError(FreeType.FT_New_Memory_Face(FreeTypeUtil.getLibrary(), $$2, 0L, $$5), "Initializing font face");
                  $$1 = FT_Face.create($$5.get());
               } catch (Throwable var14) {
                  if ($$4 != null) {
                     try {
                        $$4.close();
                     } catch (Throwable var12) {
                        var14.addSuppressed(var12);
                     }
                  }

                  throw var14;
               }

               if ($$4 != null) {
                  $$4.close();
               }

               String $$6 = FreeType.FT_Get_Font_Format($$1);
               if (!"TrueType".equals($$6)) {
                  throw new IOException("Font is not in TTF format, was " + $$6);
               }

               FreeTypeUtil.assertError(FreeType.FT_Select_Charmap($$1, FreeType.FT_ENCODING_UNICODE), "Find unicode charmap");
               var20 = new TrueTypeGlyphProvider($$2, $$1, this.size, this.oversample, this.shift.x, this.shift.y, this.skip);
            }
         }

         return var20;
      } catch (Exception var17) {
         synchronized (FreeTypeUtil.LIBRARY_LOCK) {
            if ($$1 != null) {
               FreeType.FT_Done_Face($$1);
            }
         }

         MemoryUtil.memFree($$2);
         throw var17;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record Shift(float x, float y) {
      public static final TrueTypeGlyphProviderDefinition.Shift NONE = new TrueTypeGlyphProviderDefinition.Shift(0.0F, 0.0F);
      public static final Codec<TrueTypeGlyphProviderDefinition.Shift> CODEC = Codec.floatRange(-512.0F, 512.0F)
         .listOf()
         .comapFlatMap(
            p_337412_ -> Util.fixedSize(p_337412_, 2)
               .map(p_286746_ -> new TrueTypeGlyphProviderDefinition.Shift((Float)p_286746_.get(0), (Float)p_286746_.get(1))),
            p_286274_ -> List.of(p_286274_.x, p_286274_.y)
         );
   }
}
