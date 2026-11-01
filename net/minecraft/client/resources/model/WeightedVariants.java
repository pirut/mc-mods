package net.minecraft.client.resources.model;

import java.util.List;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;

@OnlyIn(Dist.CLIENT)
public class WeightedVariants implements BlockStateModel, DynamicBlockStateModel {
   private final WeightedList<BlockStateModel> list;
   private final TextureAtlasSprite particleIcon;

   public WeightedVariants(WeightedList<BlockStateModel> p_410272_) {
      this.list = p_410272_;
      BlockStateModel blockstatemodel = p_410272_.unwrap().getFirst().value();
      this.particleIcon = blockstatemodel.particleIcon();
   }

   @Override
   public TextureAtlasSprite particleIcon() {
      return this.particleIcon;
   }

   @Override
   public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource p_409649_, List<BlockModelPart> p_410123_) {
      this.list.getRandomOrThrow(p_409649_).collectParts(level, pos, state, p_409649_, p_410123_);
   }

   @Override
   public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
      return this.list.getRandomOrThrow(random).createGeometryKey(level, pos, state, random);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked(WeightedList<BlockStateModel.Unbaked> entries) implements BlockStateModel.Unbaked {
      @Override
      public BlockStateModel bake(ModelBaker p_410875_) {
         return new WeightedVariants(this.entries.map(p_409707_ -> p_409707_.bake(p_410875_)));
      }

      @Override
      public void resolveDependencies(ResolvableModel.Resolver p_409900_) {
         this.entries.unwrap().forEach(p_409719_ -> p_409719_.value().resolveDependencies(p_409900_));
      }
   }
}
