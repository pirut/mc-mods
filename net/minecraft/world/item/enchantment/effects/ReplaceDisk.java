package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.phys.Vec3;

public record ReplaceDisk(
   LevelBasedValue radius,
   LevelBasedValue height,
   Vec3i offset,
   Optional<BlockPredicate> predicate,
   BlockStateProvider blockState,
   Optional<Holder<GameEvent>> triggerGameEvent
) implements EnchantmentEntityEffect {
   public static final MapCodec<ReplaceDisk> CODEC = RecordCodecBuilder.mapCodec(
      p_353035_ -> p_353035_.group(
            LevelBasedValue.CODEC.fieldOf("radius").forGetter(ReplaceDisk::radius),
            LevelBasedValue.CODEC.fieldOf("height").forGetter(ReplaceDisk::height),
            Vec3i.CODEC.optionalFieldOf("offset", Vec3i.ZERO).forGetter(ReplaceDisk::offset),
            BlockPredicate.CODEC.optionalFieldOf("predicate").forGetter(ReplaceDisk::predicate),
            BlockStateProvider.CODEC.fieldOf("block_state").forGetter(ReplaceDisk::blockState),
            GameEvent.CODEC.optionalFieldOf("trigger_game_event").forGetter(ReplaceDisk::triggerGameEvent)
         )
         .apply(p_353035_, ReplaceDisk::new)
   );

   @Override
   public void apply(ServerLevel p_353045_, int p_353076_, EnchantedItemInUse p_353050_, Entity p_353038_, Vec3 p_353044_) {
      BlockPos $$5 = BlockPos.containing(p_353044_).offset(this.offset);
      RandomSource $$6 = p_353038_.getRandom();
      int $$7 = (int)this.radius.calculate(p_353076_);
      int $$8 = (int)this.height.calculate(p_353076_);

      for (BlockPos $$9 : BlockPos.betweenClosed($$5.offset(-$$7, 0, -$$7), $$5.offset($$7, Math.min($$8 - 1, 0), $$7))) {
         if ($$9.distToCenterSqr(p_353044_.x(), $$9.getY() + 0.5, p_353044_.z()) < Mth.square($$7)
            && this.predicate.map(p_353051_ -> p_353051_.test(p_353045_, $$9)).orElse(true)
            && p_353045_.setBlockAndUpdate($$9, this.blockState.getState($$6, $$9))) {
            this.triggerGameEvent.ifPresent(p_353037_ -> p_353045_.gameEvent(p_353038_, (Holder<GameEvent>)p_353037_, $$9));
         }
      }
   }

   @Override
   public MapCodec<ReplaceDisk> codec() {
      return CODEC;
   }
}
