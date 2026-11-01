package net.minecraft.world.effect;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;

class WeavingMobEffect extends MobEffect {
   private final ToIntFunction<RandomSource> maxCobwebs;

   protected WeavingMobEffect(MobEffectCategory p_338733_, int p_338278_, ToIntFunction<RandomSource> p_338263_) {
      super(p_338733_, p_338278_, ParticleTypes.ITEM_COBWEB);
      this.maxCobwebs = p_338263_;
   }

   @Override
   public void onMobRemoved(ServerLevel p_376925_, LivingEntity p_338209_, int p_338446_, Entity.RemovalReason p_338624_) {
      if (p_338624_ == Entity.RemovalReason.KILLED && (p_338209_ instanceof Player || p_376925_.getGameRules().get(GameRules.MOB_GRIEFING))) {
         this.spawnCobwebsRandomlyAround(p_376925_, p_338209_.getRandom(), p_338209_.blockPosition());
      }
   }

   private void spawnCobwebsRandomlyAround(ServerLevel p_376905_, RandomSource p_338709_, BlockPos p_338472_) {
      Set<BlockPos> set = Sets.newHashSet();
      int i = this.maxCobwebs.applyAsInt(p_338709_);

      for (BlockPos blockpos : BlockPos.randomInCube(p_338709_, 15, p_338472_, 1)) {
         BlockPos blockpos1 = blockpos.below();
         if (!set.contains(blockpos)
            && p_376905_.getBlockState(blockpos).canBeReplaced()
            && p_376905_.getBlockState(blockpos1).isFaceSturdy(p_376905_, blockpos1, Direction.UP)) {
            set.add(blockpos.immutable());
            if (set.size() >= i) {
               break;
            }
         }
      }

      for (BlockPos blockpos2 : set) {
         p_376905_.setBlock(blockpos2, Blocks.COBWEB.defaultBlockState(), 3);
         p_376905_.levelEvent(3018, blockpos2, 0);
      }
   }
}
