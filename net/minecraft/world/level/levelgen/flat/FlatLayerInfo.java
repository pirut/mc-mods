package net.minecraft.world.level.levelgen.flat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;

public class FlatLayerInfo {
   public static final Codec<FlatLayerInfo> CODEC = RecordCodecBuilder.create(
      p_360250_ -> p_360250_.group(
            Codec.intRange(0, DimensionType.Y_SIZE).fieldOf("height").forGetter(FlatLayerInfo::getHeight),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").orElse(Blocks.AIR).forGetter(p_161902_ -> p_161902_.getBlockState().getBlock())
         )
         .apply(p_360250_, FlatLayerInfo::new)
   );
   private final Block block;
   private final int height;

   public FlatLayerInfo(int p_70335_, Block p_70336_) {
      this.height = p_70335_;
      this.block = p_70336_;
   }

   public int getHeight() {
      return this.height;
   }

   public BlockState getBlockState() {
      return this.block.defaultBlockState();
   }

   public FlatLayerInfo heightLimited(int p_426224_) {
      return this.height > p_426224_ ? new FlatLayerInfo(p_426224_, this.block) : this;
   }

   @Override
   public String toString() {
      return (this.height != 1 ? this.height + "*" : "") + BuiltInRegistries.BLOCK.getKey(this.block);
   }
}
