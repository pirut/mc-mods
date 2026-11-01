package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jspecify.annotations.Nullable;

public record BlockPredicate(
   Optional<HolderSet<Block>> blocks, Optional<StatePropertiesPredicate> properties, Optional<NbtPredicate> nbt, DataComponentMatchers components
) {
   public static final Codec<BlockPredicate> CODEC = RecordCodecBuilder.create(
      p_468066_ -> p_468066_.group(
            RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf("blocks").forGetter(BlockPredicate::blocks),
            StatePropertiesPredicate.CODEC.optionalFieldOf("state").forGetter(BlockPredicate::properties),
            NbtPredicate.CODEC.optionalFieldOf("nbt").forGetter(BlockPredicate::nbt),
            DataComponentMatchers.CODEC.forGetter(BlockPredicate::components)
         )
         .apply(p_468066_, BlockPredicate::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, BlockPredicate> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.optional(ByteBufCodecs.holderSet(Registries.BLOCK)),
      BlockPredicate::blocks,
      ByteBufCodecs.optional(StatePropertiesPredicate.STREAM_CODEC),
      BlockPredicate::properties,
      ByteBufCodecs.optional(NbtPredicate.STREAM_CODEC),
      BlockPredicate::nbt,
      DataComponentMatchers.STREAM_CODEC,
      BlockPredicate::components,
      BlockPredicate::new
   );

   public boolean matches(ServerLevel p_469136_, BlockPos p_469514_) {
      if (!p_469136_.isLoaded(p_469514_)) {
         return false;
      } else if (!this.matchesState(p_469136_.getBlockState(p_469514_))) {
         return false;
      } else {
         if (this.nbt.isPresent() || !this.components.isEmpty()) {
            BlockEntity blockentity = p_469136_.getBlockEntity(p_469514_);
            if (this.nbt.isPresent() && !matchesBlockEntity(p_469136_, blockentity, this.nbt.get())) {
               return false;
            }

            if (!this.components.isEmpty() && !matchesComponents(blockentity, this.components)) {
               return false;
            }
         }

         return true;
      }
   }

   public boolean matches(BlockInWorld p_469393_) {
      return !this.matchesState(p_469393_.getState())
         ? false
         : !this.nbt.isPresent() || matchesBlockEntity(p_469393_.getLevel(), p_469393_.getEntity(), this.nbt.get());
   }

   private boolean matchesState(BlockState p_469413_) {
      return this.blocks.isPresent() && !p_469413_.is(this.blocks.get()) ? false : !this.properties.isPresent() || this.properties.get().matches(p_469413_);
   }

   private static boolean matchesBlockEntity(LevelReader p_467159_, @Nullable BlockEntity p_469044_, NbtPredicate p_469945_) {
      return p_469044_ != null && p_469945_.matches(p_469044_.saveWithFullMetadata(p_467159_.registryAccess()));
   }

   private static boolean matchesComponents(@Nullable BlockEntity p_467904_, DataComponentMatchers p_468310_) {
      return p_467904_ != null && p_468310_.test((DataComponentGetter)p_467904_.collectComponents());
   }

   public boolean requiresNbt() {
      return this.nbt.isPresent();
   }

   public static class Builder {
      private Optional<HolderSet<Block>> blocks = Optional.empty();
      private Optional<StatePropertiesPredicate> properties = Optional.empty();
      private Optional<NbtPredicate> nbt = Optional.empty();
      private DataComponentMatchers components = DataComponentMatchers.ANY;

      private Builder() {
      }

      public static BlockPredicate.Builder block() {
         return new BlockPredicate.Builder();
      }

      public BlockPredicate.Builder of(HolderGetter<Block> p_467926_, Block... p_468537_) {
         return this.of(p_467926_, Arrays.asList(p_468537_));
      }

      public BlockPredicate.Builder of(HolderGetter<Block> p_468584_, Collection<Block> p_469644_) {
         this.blocks = Optional.of(HolderSet.direct(Block::builtInRegistryHolder, p_469644_));
         return this;
      }

      public BlockPredicate.Builder of(HolderGetter<Block> p_470008_, TagKey<Block> p_467755_) {
         this.blocks = Optional.of(p_470008_.getOrThrow(p_467755_));
         return this;
      }

      public BlockPredicate.Builder hasNbt(CompoundTag p_470009_) {
         this.nbt = Optional.of(new NbtPredicate(p_470009_));
         return this;
      }

      public BlockPredicate.Builder setProperties(StatePropertiesPredicate.Builder p_467510_) {
         this.properties = p_467510_.build();
         return this;
      }

      public BlockPredicate.Builder components(DataComponentMatchers p_468551_) {
         this.components = p_468551_;
         return this;
      }

      public BlockPredicate build() {
         return new BlockPredicate(this.blocks, this.properties, this.nbt, this.components);
      }
   }
}
