package net.minecraft.client.resources.model;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.IModelBakerExtension;

@OnlyIn(Dist.CLIENT)
public interface ModelBaker extends IModelBakerExtension {
   UnbakedModel getModel(ResourceLocation var1);

   @Deprecated
   @Nullable
   BakedModel bake(ResourceLocation var1, ModelState var2);
}
