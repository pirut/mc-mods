package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.model.TextureSlots;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.UnbakedGeometryExtension;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface UnbakedGeometry extends UnbakedGeometryExtension {
   UnbakedGeometry EMPTY = (p_405112_, p_405354_, p_405672_, p_405151_) -> QuadCollection.EMPTY;

   @Deprecated
   QuadCollection bake(TextureSlots var1, ModelBaker var2, ModelState var3, ModelDebugName var4);
}
