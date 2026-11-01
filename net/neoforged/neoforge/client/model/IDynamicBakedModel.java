package net.neoforged.neoforge.client.model;

import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

public interface IDynamicBakedModel extends BakedModel {
   @Override
   default List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
      return this.getQuads(state, side, rand, ModelData.EMPTY, null);
   }

   @Override
   List<BakedQuad> getQuads(@Nullable BlockState var1, @Nullable Direction var2, RandomSource var3, ModelData var4, @Nullable RenderType var5);
}
