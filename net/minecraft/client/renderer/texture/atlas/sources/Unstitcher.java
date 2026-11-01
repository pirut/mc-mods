package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Unstitcher implements SpriteSource {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<Unstitcher> CODEC = RecordCodecBuilder.mapCodec(
      p_262047_ -> p_262047_.group(
            ResourceLocation.CODEC.fieldOf("resource").forGetter(p_261910_ -> p_261910_.resource),
            ExtraCodecs.nonEmptyList(Unstitcher.Region.CODEC.listOf()).fieldOf("regions").forGetter(p_261944_ -> p_261944_.regions),
            Codec.DOUBLE.optionalFieldOf("divisor_x", 1.0).forGetter(p_261601_ -> p_261601_.xDivisor),
            Codec.DOUBLE.optionalFieldOf("divisor_y", 1.0).forGetter(p_262039_ -> p_262039_.yDivisor)
         )
         .apply(p_262047_, Unstitcher::new)
   );
   private final ResourceLocation resource;
   private final List<Unstitcher.Region> regions;
   private final double xDivisor;
   private final double yDivisor;

   public Unstitcher(ResourceLocation p_261679_, List<Unstitcher.Region> p_261974_, double p_262181_, double p_261631_) {
      this.resource = p_261679_;
      this.regions = p_261974_;
      this.xDivisor = p_262181_;
      this.yDivisor = p_261631_;
   }

   @Override
   public void run(ResourceManager p_261498_, SpriteSource.Output p_261828_) {
      ResourceLocation $$2 = TEXTURE_ID_CONVERTER.idToFile(this.resource);
      Optional<Resource> $$3 = p_261498_.getResource($$2);
      if ($$3.isPresent()) {
         LazyLoadedImage $$4 = new LazyLoadedImage($$2, $$3.get(), this.regions.size());

         for (Unstitcher.Region $$5 : this.regions) {
            p_261828_.add($$5.sprite, new Unstitcher.RegionInstance($$4, $$5, this.xDivisor, this.yDivisor));
         }
      } else {
         LOGGER.warn("Missing sprite: {}", $$2);
      }
   }

   @Override
   public SpriteSourceType type() {
      return SpriteSources.UNSTITCHER;
   }

   @OnlyIn(Dist.CLIENT)
   public record Region(ResourceLocation sprite, double x, double y, double width, double height) {
      public static final Codec<Unstitcher.Region> CODEC = RecordCodecBuilder.create(
         p_261521_ -> p_261521_.group(
               ResourceLocation.CODEC.fieldOf("sprite").forGetter(Unstitcher.Region::sprite),
               Codec.DOUBLE.fieldOf("x").forGetter(Unstitcher.Region::x),
               Codec.DOUBLE.fieldOf("y").forGetter(Unstitcher.Region::y),
               Codec.DOUBLE.fieldOf("width").forGetter(Unstitcher.Region::width),
               Codec.DOUBLE.fieldOf("height").forGetter(Unstitcher.Region::height)
            )
            .apply(p_261521_, Unstitcher.Region::new)
      );
   }

   @OnlyIn(Dist.CLIENT)
   public static class RegionInstance implements SpriteSource.SpriteSupplier {
      private final LazyLoadedImage image;
      private final Unstitcher.Region region;
      private final double xDivisor;
      private final double yDivisor;

      public RegionInstance(LazyLoadedImage p_266678_, Unstitcher.Region p_267197_, double p_266911_, double p_266789_) {
         this.image = p_266678_;
         this.region = p_267197_;
         this.xDivisor = p_266911_;
         this.yDivisor = p_266789_;
      }

      public SpriteContents apply(SpriteResourceLoader p_296272_) {
         try {
            NativeImage $$1 = this.image.get();
            double $$2 = $$1.getWidth() / this.xDivisor;
            double $$3 = $$1.getHeight() / this.yDivisor;
            int $$4 = Mth.floor(this.region.x * $$2);
            int $$5 = Mth.floor(this.region.y * $$3);
            int $$6 = Mth.floor(this.region.width * $$2);
            int $$7 = Mth.floor(this.region.height * $$3);
            NativeImage $$8 = new NativeImage(NativeImage.Format.RGBA, $$6, $$7, false);
            $$1.copyRect($$8, $$4, $$5, 0, 0, $$6, $$7, false, false);
            return new SpriteContents(this.region.sprite, new FrameSize($$6, $$7), $$8, ResourceMetadata.EMPTY);
         } catch (Exception var16) {
            Unstitcher.LOGGER.error("Failed to unstitch region {}", this.region.sprite, var16);
         } finally {
            this.image.release();
         }

         return MissingTextureAtlasSprite.create();
      }

      @Override
      public void discard() {
         this.image.release();
      }
   }
}
