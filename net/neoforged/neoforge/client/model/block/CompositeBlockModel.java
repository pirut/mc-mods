package net.neoforged.neoforge.client.model.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.jspecify.annotations.Nullable;

public class CompositeBlockModel implements DynamicBlockStateModel {
   private final List<BlockStateModel> models;
   private final TextureAtlasSprite particleIcon;

   public CompositeBlockModel(List<BlockStateModel> models) {
      this.models = models;
      this.particleIcon = models.getFirst().particleIcon();
   }

   @Nullable
   @Override
   public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
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

         return new CompositeBlockModel.GeometryKey(subKeys, this);
      }
   }

   @Override
   public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockModelPart> parts) {
      long seed = random.nextLong();

      for (BlockStateModel model : this.models) {
         random.setSeed(seed);
         model.collectParts(level, pos, state, random, parts);
      }
   }

   @Override
   public TextureAtlasSprite particleIcon() {
      return this.particleIcon;
   }

   private record GeometryKey(List<Object> subKeys, CompositeBlockModel composite) {
   }

   public record Unbaked(List<BlockStateModel.Unbaked> models) implements CustomUnbakedBlockStateModel {
      public static final MapCodec<CompositeBlockModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(BlockStateModel.Unbaked.CODEC.listOf(1, Integer.MAX_VALUE).fieldOf("models").forGetter(CompositeBlockModel.Unbaked::models))
            .apply(instance, CompositeBlockModel.Unbaked::new)
      );

      public Unbaked(List<BlockStateModel.Unbaked> models) {
         if (models.isEmpty()) {
            throw new IllegalArgumentException("Composite model requires at least one submodel");
         } else {
            this.models = models;
         }
      }

      @Override
      public BlockStateModel bake(ModelBaker baker) {
         return new CompositeBlockModel(this.models.stream().map(m -> m.bake(baker)).toList());
      }

      @Override
      public void resolveDependencies(ResolvableModel.Resolver resolver) {
         for (BlockStateModel.Unbaked model : this.models) {
            model.resolveDependencies(resolver);
         }
      }

      @Override
      public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
         return MAP_CODEC;
      }
   }
}
