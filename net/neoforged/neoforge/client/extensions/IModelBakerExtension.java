package net.neoforged.neoforge.client.extensions;

import java.util.function.Function;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface IModelBakerExtension {
   @Nullable
   UnbakedModel getTopLevelModel(ModelResourceLocation var1);

   @Nullable
   BakedModel bake(ResourceLocation var1, ModelState var2, Function<Material, TextureAtlasSprite> var3);

   @Nullable
   BakedModel bakeUncached(UnbakedModel var1, ModelState var2, Function<Material, TextureAtlasSprite> var3);

   Function<Material, TextureAtlasSprite> getModelTextureGetter();
}
