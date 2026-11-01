package net.neoforged.neoforge.client.model;

import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.util.context.ContextMap;

@FunctionalInterface
public interface ExtendedUnbakedGeometry extends UnbakedGeometry {
   @Override
   default QuadCollection bake(TextureSlots p_405831_, ModelBaker p_405026_, ModelState p_405122_, ModelDebugName p_405635_) {
      return this.bake(p_405831_, p_405026_, p_405122_, p_405635_, ContextMap.EMPTY);
   }

   @Override
   QuadCollection bake(TextureSlots var1, ModelBaker var2, ModelState var3, ModelDebugName var4, ContextMap var5);
}
