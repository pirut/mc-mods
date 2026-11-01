package net.minecraft.world.level.block;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class TorchBlock extends BaseTorchBlock {
   protected static final MapCodec<SimpleParticleType> PARTICLE_OPTIONS_FIELD = BuiltInRegistries.PARTICLE_TYPE
      .byNameCodec()
      .comapFlatMap(
         p_304958_ -> p_304958_ instanceof SimpleParticleType $$1 ? DataResult.success($$1) : DataResult.error(() -> "Not a SimpleParticleType: " + p_304958_),
         p_304720_ -> p_304720_
      )
      .fieldOf("particle_options");
   public static final MapCodec<TorchBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_308842_ -> p_308842_.group(PARTICLE_OPTIONS_FIELD.forGetter(p_304762_ -> p_304762_.flameParticle), propertiesCodec()).apply(p_308842_, TorchBlock::new)
   );
   protected final SimpleParticleType flameParticle;

   @Override
   public MapCodec<? extends TorchBlock> codec() {
      return CODEC;
   }

   public TorchBlock(SimpleParticleType p_304940_, BlockBehaviour.Properties p_57491_) {
      super(p_57491_);
      this.flameParticle = p_304940_;
   }

   @Override
   public void animateTick(BlockState p_222593_, Level p_222594_, BlockPos p_222595_, RandomSource p_222596_) {
      double $$4 = p_222595_.getX() + 0.5;
      double $$5 = p_222595_.getY() + 0.7;
      double $$6 = p_222595_.getZ() + 0.5;
      p_222594_.addParticle(ParticleTypes.SMOKE, $$4, $$5, $$6, 0.0, 0.0, 0.0);
      p_222594_.addParticle(this.flameParticle, $$4, $$5, $$6, 0.0, 0.0, 0.0);
   }
}
