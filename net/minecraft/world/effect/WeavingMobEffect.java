package net.minecraft.world.effect;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

class WeavingMobEffect extends MobEffect {
   private final ToIntFunction<RandomSource> maxCobwebs;

   protected WeavingMobEffect(MobEffectCategory p_338733_, int p_338278_, ToIntFunction<RandomSource> p_338263_) {
      super(p_338733_, p_338278_, ParticleTypes.ITEM_COBWEB);
      this.maxCobwebs = p_338263_;
   }

   @Override
   public void onMobRemoved(LivingEntity p_338209_, int p_338446_, Entity.RemovalReason p_338624_) {
      if (p_338624_ == Entity.RemovalReason.KILLED && (p_338209_ instanceof Player || p_338209_.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))) {
         this.spawnCobwebsRandomlyAround(p_338209_.level(), p_338209_.getRandom(), p_338209_.getOnPos());
      }
   }

   private void spawnCobwebsRandomlyAround(Level p_338396_, RandomSource p_338709_, BlockPos p_338472_) {
      Set<BlockPos> $$3 = Sets.newHashSet();
      int $$4 = this.maxCobwebs.applyAsInt(p_338709_);

      for (BlockPos $$5 : BlockPos.randomInCube(p_338709_, 15, p_338472_, 1)) {
         BlockPos $$6 = $$5.below();
         if (!$$3.contains($$5) && p_338396_.getBlockState($$5).canBeReplaced() && p_338396_.getBlockState($$6).isFaceSturdy(p_338396_, $$6, Direction.UP)) {
            $$3.add($$5.immutable());
            if ($$3.size() >= $$4) {
               break;
            }
         }
      }

      for (BlockPos $$7 : $$3) {
         p_338396_.setBlock($$7, Blocks.COBWEB.defaultBlockState(), 3);
         p_338396_.levelEvent(3018, $$7, 0);
      }
   }
}
