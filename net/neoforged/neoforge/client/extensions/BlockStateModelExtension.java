package net.neoforged.neoforge.client.extensions;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jspecify.annotations.Nullable;

public interface BlockStateModelExtension {
   private BlockStateModel self() {
      return (BlockStateModel)this;
   }

   @Nullable
   default Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
      return null;
   }

   default void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockModelPart> parts) {
      this.self().collectParts(random, parts);
   }

   @NonExtendable
   default List<BlockModelPart> collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
      List<BlockModelPart> parts = new ObjectArrayList();
      this.collectParts(level, pos, state, random, parts);
      return parts;
   }

   default TextureAtlasSprite particleIcon(BlockAndTintGetter level, BlockPos pos, BlockState state) {
      return this.self().particleIcon();
   }
}
