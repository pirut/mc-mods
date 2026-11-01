package net.minecraft.client.renderer.block.model;

import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.BlockModelPartExtension;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface BlockModelPart extends BlockModelPartExtension {
   List<BakedQuad> getQuads(@Nullable Direction var1);

   @Deprecated
   boolean useAmbientOcclusion();

   TextureAtlasSprite particleIcon();

   @OnlyIn(Dist.CLIENT)
   public interface Unbaked extends ResolvableModel {
      BlockModelPart bake(ModelBaker var1);
   }
}
