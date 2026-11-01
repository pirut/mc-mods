package net.minecraft.client.resources.model;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;

@Environment(EnvType.CLIENT)
public class WeightedVariants implements BlockStateModel {
   private final WeightedList<BlockStateModel> list;
   private final TextureAtlasSprite particleIcon;

   public WeightedVariants(WeightedList<BlockStateModel> list) {
      this.list = list;
      BlockStateModel blockStateModel = list.unwrap().getFirst().value();
      this.particleIcon = blockStateModel.particleIcon();
   }

   @Override
   public TextureAtlasSprite particleIcon() {
      return this.particleIcon;
   }

   @Override
   public void collectParts(RandomSource random, List<BlockModelPart> output) {
      this.list.getRandomOrThrow(random).collectParts(random, output);
   }

   @Environment(EnvType.CLIENT)
   public record Unbaked(WeightedList<BlockStateModel.Unbaked> entries) implements BlockStateModel.Unbaked {
      @Override
      public BlockStateModel bake(ModelBaker baker) {
         return new WeightedVariants(this.entries.map(unbaked -> unbaked.bake(baker)));
      }

      @Override
      public void resolveDependencies(ResolvableModel.Resolver resolver) {
         this.entries.unwrap().forEach(weighted -> weighted.value().resolveDependencies(resolver));
      }
   }
}
