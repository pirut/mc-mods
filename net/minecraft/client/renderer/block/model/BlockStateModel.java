package net.minecraft.client.renderer.block.model;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.WeightedVariants;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.BlockStateModelExtension;
import net.neoforged.neoforge.client.model.block.BlockStateModelHooks;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.jetbrains.annotations.ApiStatus.Internal;

@OnlyIn(Dist.CLIENT)
public interface BlockStateModel extends BlockStateModelExtension {
   @Deprecated
   void collectParts(RandomSource var1, List<BlockModelPart> var2);

   @Deprecated
   default List<BlockModelPart> collectParts(RandomSource p_410570_) {
      List<BlockModelPart> list = new ObjectArrayList();
      this.collectParts(p_410570_, list);
      return list;
   }

   @Deprecated
   TextureAtlasSprite particleIcon();

   @OnlyIn(Dist.CLIENT)
   public static class SimpleCachedUnbakedRoot implements BlockStateModel.UnbakedRoot {
      final BlockStateModel.Unbaked contents;
      private final ModelBaker.SharedOperationKey<BlockStateModel> bakingKey = new ModelBaker.SharedOperationKey<BlockStateModel>() {
         public BlockStateModel compute(ModelBaker p_409856_) {
            return SimpleCachedUnbakedRoot.this.contents.bake(p_409856_);
         }
      };

      public SimpleCachedUnbakedRoot(BlockStateModel.Unbaked p_410217_) {
         this.contents = p_410217_;
      }

      @Override
      public void resolveDependencies(ResolvableModel.Resolver p_410344_) {
         this.contents.resolveDependencies(p_410344_);
      }

      @Override
      public BlockStateModel bake(BlockState p_410257_, ModelBaker p_410233_) {
         return p_410233_.compute(this.bakingKey);
      }

      @Override
      public Object visualEqualityGroup(BlockState p_409960_) {
         return this;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface Unbaked extends ResolvableModel {
      Codec<Weighted<Variant>> ELEMENT_CODEC = RecordCodecBuilder.create(
         p_409971_ -> p_409971_.group(
               Variant.MAP_CODEC.forGetter(Weighted::value), ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", 1).forGetter(Weighted::weight)
            )
            .apply(p_409971_, Weighted::new)
      );
      @Internal
      Codec<Either<CustomUnbakedBlockStateModel, SingleVariant.Unbaked>> SINGLE_MODEL_CODEC = BlockStateModelHooks.makeSingleModelCodec().codec();
      @Internal
      Codec<Weighted<Either<CustomUnbakedBlockStateModel, SingleVariant.Unbaked>>> WEIGHTED_MODEL_CODEC = BlockStateModelHooks.makeElementCodec();
      Codec<WeightedVariants.Unbaked> HARDCODED_WEIGHTED_CODEC = ExtraCodecs.nonEmptyList(WEIGHTED_MODEL_CODEC.listOf())
         .flatComapMap(
            p_409703_ -> new WeightedVariants.Unbaked(
               WeightedList.of(Lists.transform(p_409703_, p_409617_ -> p_409617_.map(either -> (BlockStateModel.Unbaked)either.map(m -> m, m -> m))))
            ),
            p_409817_ -> {
               List<Weighted<BlockStateModel.Unbaked>> list = p_409817_.entries().unwrap();
               List<Weighted<Either<CustomUnbakedBlockStateModel, SingleVariant.Unbaked>>> list1 = new ArrayList<>(list.size());

               for (Weighted<BlockStateModel.Unbaked> weighted : list) {
                  switch ((BlockStateModel.Unbaked)weighted.value()) {
                     case CustomUnbakedBlockStateModel customModel:
                        list1.add(new Weighted<>(Either.left(customModel), weighted.weight()));
                        break;
                     case SingleVariant.Unbaked singlevariant$unbaked:
                        list1.add(new Weighted<>(Either.right(new SingleVariant.Unbaked(singlevariant$unbaked.variant())), weighted.weight()));
                        break;
                     default:
                        return DataResult.error(() -> "Only custom models or single variants are supported");
                  }
               }

               return DataResult.success(list1);
            }
         );
      Codec<BlockStateModel.Unbaked> CODEC = Codec.either(HARDCODED_WEIGHTED_CODEC, SINGLE_MODEL_CODEC)
         .flatComapMap(
            p_410308_ -> (BlockStateModel.Unbaked)p_410308_.map(p_410746_ -> p_410746_, p_410169_ -> (BlockStateModel.Unbaked)p_410169_.map(m -> m, m -> m)),
            p_410404_ -> {
               return switch (p_410404_) {
                  case CustomUnbakedBlockStateModel customModel -> DataResult.success(Either.right(Either.left(customModel)));
                  case SingleVariant.Unbaked singlevariant$unbaked -> DataResult.success(Either.right(Either.right(singlevariant$unbaked)));
                  case WeightedVariants.Unbaked weightedvariants$unbaked -> DataResult.success(Either.left(weightedvariants$unbaked));
                  default -> DataResult.error(() -> "Only a custom model or a single variant or a list of variants are supported");
               };
            }
         );

      BlockStateModel bake(ModelBaker var1);

      default BlockStateModel.UnbakedRoot asRoot() {
         return new BlockStateModel.SimpleCachedUnbakedRoot(this);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface UnbakedRoot extends ResolvableModel {
      BlockStateModel bake(BlockState var1, ModelBaker var2);

      Object visualEqualityGroup(BlockState var1);
   }
}
