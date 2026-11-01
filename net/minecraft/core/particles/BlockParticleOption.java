package net.minecraft.core.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jspecify.annotations.Nullable;

public class BlockParticleOption implements ParticleOptions {
   private static final Codec<BlockState> BLOCK_STATE_CODEC = Codec.withAlternative(
      BlockState.CODEC, BuiltInRegistries.BLOCK.byNameCodec(), Block::defaultBlockState
   );
   private final ParticleType<BlockParticleOption> type;
   private final BlockState state;
   @Nullable
   private final BlockPos pos;

   public static MapCodec<BlockParticleOption> codec(ParticleType<BlockParticleOption> p_123635_) {
      return BLOCK_STATE_CODEC.xmap(p_123638_ -> new BlockParticleOption(p_123635_, p_123638_), p_123633_ -> p_123633_.state).fieldOf("block_state");
   }

   public static StreamCodec<? super RegistryFriendlyByteBuf, BlockParticleOption> streamCodec(ParticleType<BlockParticleOption> p_320740_) {
      return StreamCodec.composite(
         ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY),
         option -> option.state,
         NeoForgeStreamCodecs.connectionAware(ByteBufCodecs.optional(BlockPos.STREAM_CODEC), NeoForgeStreamCodecs.uncheckedUnit(Optional.empty())),
         option -> Optional.ofNullable(option.pos),
         (state, pos) -> new BlockParticleOption(p_320740_, state, pos.orElse(null))
      );
   }

   public BlockParticleOption(ParticleType<BlockParticleOption> p_123629_, BlockState p_123630_) {
      this(p_123629_, p_123630_, null);
   }

   public BlockParticleOption(ParticleType<BlockParticleOption> p_123629_, BlockState p_123630_, @Nullable BlockPos pos) {
      this.type = p_123629_;
      this.state = p_123630_;
      this.pos = pos;
   }

   @Override
   public ParticleType<BlockParticleOption> getType() {
      return this.type;
   }

   public BlockState getState() {
      return this.state;
   }

   @Nullable
   public BlockPos getPos() {
      return this.pos;
   }
}
