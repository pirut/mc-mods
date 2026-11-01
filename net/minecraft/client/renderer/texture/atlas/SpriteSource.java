package net.minecraft.client.renderer.texture.atlas;

import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SpriteSource {
   FileToIdConverter TEXTURE_ID_CONVERTER = new FileToIdConverter("textures", ".png");

   void run(ResourceManager var1, SpriteSource.Output var2);

   SpriteSourceType type();

   @OnlyIn(Dist.CLIENT)
   public interface Output {
      default void add(ResourceLocation p_261841_, Resource p_261651_) {
         this.add(p_261841_, p_293684_ -> p_293684_.loadSprite(p_261841_, p_261651_));
      }

      void add(ResourceLocation var1, SpriteSource.SpriteSupplier var2);

      void removeAll(Predicate<ResourceLocation> var1);
   }

   @OnlyIn(Dist.CLIENT)
   public interface SpriteSupplier extends Function<SpriteResourceLoader, SpriteContents> {
      default void discard() {
      }
   }
}
