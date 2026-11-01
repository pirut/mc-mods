package net.minecraft.client.renderer.block.model.multipart;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MultiPartModel implements BlockStateModel, DynamicBlockStateModel {
   private final MultiPartModel.SharedBakedState shared;
   private final BlockState blockState;
   @Nullable
   private List<BlockStateModel> models;

   MultiPartModel(MultiPartModel.SharedBakedState p_409947_, BlockState p_410377_) {
      this.shared = p_409947_;
      this.blockState = p_410377_;
   }

   @Override
   public TextureAtlasSprite particleIcon() {
      return this.shared.particleIcon;
   }

   @Override
   public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource p_410101_, List<BlockModelPart> p_410550_) {
      if (this.models == null) {
         this.models = this.shared.selectModels(this.blockState);
      }

      long i = p_410101_.nextLong();

      for (BlockStateModel blockstatemodel : this.models) {
         p_410101_.setSeed(i);
         blockstatemodel.collectParts(level, pos, state, p_410101_, p_410550_);
      }
   }

   @Nullable
   @Override
   public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
      if (this.models == null) {
         this.models = this.shared.selectModels(this.blockState);
      }

      long seed = random.nextLong();
      if (this.models.size() == 1) {
         random.setSeed(seed);
         return this.models.getFirst().createGeometryKey(level, pos, state, random);
      } else {
         List<Object> subKeys = new ArrayList<>(this.models.size());

         for (BlockStateModel model : this.models) {
            random.setSeed(seed);
            Object subKey = model.createGeometryKey(level, pos, state, random);
            if (subKey == null) {
               return null;
            }

            subKeys.add(subKey);
         }

         return new MultiPartModel.GeometryKey(subKeys, this);
      }
   }

   private record GeometryKey(List<Object> subKeys, MultiPartModel multiPart) {
   }

   @OnlyIn(Dist.CLIENT)
   public record Selector<T>(Predicate<BlockState> condition, T model) {
      public <S> MultiPartModel.Selector<S> with(S p_409701_) {
         return new MultiPartModel.Selector<>(this.condition, p_409701_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static final class SharedBakedState {
      private final List<MultiPartModel.Selector<BlockStateModel>> selectors;
      final TextureAtlasSprite particleIcon;
      private final Map<BitSet, List<BlockStateModel>> subsets = new ConcurrentHashMap<>();

      private static BlockStateModel getFirstModel(List<MultiPartModel.Selector<BlockStateModel>> p_409686_) {
         if (p_409686_.isEmpty()) {
            throw new IllegalArgumentException("Model must have at least one selector");
         } else {
            return p_409686_.getFirst().model();
         }
      }

      public SharedBakedState(List<MultiPartModel.Selector<BlockStateModel>> p_410541_) {
         this.selectors = p_410541_;
         BlockStateModel blockstatemodel = getFirstModel(p_410541_);
         this.particleIcon = blockstatemodel.particleIcon();
      }

      public List<BlockStateModel> selectModels(BlockState p_410842_) {
         BitSet bitset = new BitSet();

         for (int i = 0; i < this.selectors.size(); i++) {
            if (this.selectors.get(i).condition.test(p_410842_)) {
               bitset.set(i);
            }
         }

         return this.subsets.computeIfAbsent(bitset, p_409792_ -> {
            Builder<BlockStateModel> builder = ImmutableList.builder();

            for (int j = 0; j < this.selectors.size(); j++) {
               if (p_409792_.get(j)) {
                  builder.add(this.selectors.get(j).model);
               }
            }

            return builder.build();
         });
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Unbaked implements BlockStateModel.UnbakedRoot {
      final List<MultiPartModel.Selector<BlockStateModel.Unbaked>> selectors;
      private final ModelBaker.SharedOperationKey<MultiPartModel.SharedBakedState> sharedStateKey = new ModelBaker.SharedOperationKey<MultiPartModel.SharedBakedState>(
         
      ) {
         public MultiPartModel.SharedBakedState compute(ModelBaker p_410269_) {
            Builder<MultiPartModel.Selector<BlockStateModel>> builder = ImmutableList.builderWithExpectedSize(Unbaked.this.selectors.size());

            for (MultiPartModel.Selector<BlockStateModel.Unbaked> selector : Unbaked.this.selectors) {
               builder.add(selector.with(selector.model.bake(p_410269_)));
            }

            return new MultiPartModel.SharedBakedState(builder.build());
         }
      };

      public Unbaked(List<MultiPartModel.Selector<BlockStateModel.Unbaked>> p_410734_) {
         this.selectors = p_410734_;
      }

      @Override
      public Object visualEqualityGroup(BlockState p_409924_) {
         IntList intlist = new IntArrayList();

         for (int i = 0; i < this.selectors.size(); i++) {
            if (this.selectors.get(i).condition.test(p_409924_)) {
               intlist.add(i);
            }
         }

         @OnlyIn(Dist.CLIENT)
         record Key(MultiPartModel.Unbaked model, IntList selectors) {
         }

         return new Key(this, intlist);
      }

      @Override
      public void resolveDependencies(ResolvableModel.Resolver p_410079_) {
         this.selectors.forEach(p_410388_ -> p_410388_.model.resolveDependencies(p_410079_));
      }

      @Override
      public BlockStateModel bake(BlockState p_410876_, ModelBaker p_410500_) {
         MultiPartModel.SharedBakedState multipartmodel$sharedbakedstate = p_410500_.compute(this.sharedStateKey);
         return new MultiPartModel(multipartmodel$sharedbakedstate, p_410876_);
      }
   }
}
