package net.minecraft.client.renderer.texture.atlas;

import com.mojang.serialization.MapCodec;
import java.util.function.Predicate;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.SpriteSourceExtension;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface SpriteSource extends SpriteSourceExtension {
   FileToIdConverter TEXTURE_ID_CONVERTER = new FileToIdConverter("textures", ".png");

   void run(ResourceManager var1, SpriteSource.Output var2);

   MapCodec<? extends SpriteSource> codec();

   @OnlyIn(Dist.CLIENT)
   public interface DiscardableLoader extends SpriteSource.Loader {
      default void discard() {
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface Loader {
      @Nullable
      SpriteContents get(SpriteResourceLoader var1);
   }

   @OnlyIn(Dist.CLIENT)
   public interface Output {
      default void add(Identifier p_467249_, Resource p_261651_) {
         this.add(p_467249_, p_465742_ -> p_465742_.loadSprite(p_467249_, p_261651_));
      }

      void add(Identifier var1, SpriteSource.DiscardableLoader var2);

      void removeAll(Predicate<Identifier> var1);
   }
}
