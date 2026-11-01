package net.minecraft.world.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class EnderEyeItem extends Item {
   public EnderEyeItem(Item.Properties p_41180_) {
      super(p_41180_);
   }

   @Override
   public InteractionResult useOn(UseOnContext p_41182_) {
      Level $$1 = p_41182_.getLevel();
      BlockPos $$2 = p_41182_.getClickedPos();
      BlockState $$3 = $$1.getBlockState($$2);
      if (!$$3.is(Blocks.END_PORTAL_FRAME) || $$3.getValue(EndPortalFrameBlock.HAS_EYE)) {
         return InteractionResult.PASS;
      } else if ($$1.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         BlockState $$4 = $$3.setValue(EndPortalFrameBlock.HAS_EYE, true);
         Block.pushEntitiesUp($$3, $$4, $$1, $$2);
         $$1.setBlock($$2, $$4, 2);
         $$1.updateNeighbourForOutputSignal($$2, Blocks.END_PORTAL_FRAME);
         p_41182_.getItemInHand().shrink(1);
         $$1.levelEvent(1503, $$2, 0);
         BlockPattern.BlockPatternMatch $$5 = EndPortalFrameBlock.getOrCreatePortalShape().find($$1, $$2);
         if ($$5 != null) {
            BlockPos $$6 = $$5.getFrontTopLeft().offset(-3, 0, -3);

            for (int $$7 = 0; $$7 < 3; $$7++) {
               for (int $$8 = 0; $$8 < 3; $$8++) {
                  $$1.setBlock($$6.offset($$7, 0, $$8), Blocks.END_PORTAL.defaultBlockState(), 2);
               }
            }

            $$1.globalLevelEvent(1038, $$6.offset(1, 0, 1), 0);
         }

         return InteractionResult.CONSUME;
      }
   }

   @Override
   public int getUseDuration(ItemStack p_326954_, LivingEntity p_346241_) {
      return 0;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level p_41184_, Player p_41185_, InteractionHand p_41186_) {
      ItemStack $$3 = p_41185_.getItemInHand(p_41186_);
      BlockHitResult $$4 = getPlayerPOVHitResult(p_41184_, p_41185_, ClipContext.Fluid.NONE);
      if ($$4.getType() == HitResult.Type.BLOCK && p_41184_.getBlockState($$4.getBlockPos()).is(Blocks.END_PORTAL_FRAME)) {
         return InteractionResultHolder.pass($$3);
      } else {
         p_41185_.startUsingItem(p_41186_);
         if (p_41184_ instanceof ServerLevel $$5) {
            BlockPos $$6 = $$5.findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, p_41185_.blockPosition(), 100, false);
            if ($$6 != null) {
               EyeOfEnder $$7 = new EyeOfEnder(p_41184_, p_41185_.getX(), p_41185_.getY(0.5), p_41185_.getZ());
               $$7.setItem($$3);
               $$7.signalTo($$6);
               p_41184_.gameEvent(GameEvent.PROJECTILE_SHOOT, $$7.position(), GameEvent.Context.of(p_41185_));
               p_41184_.addFreshEntity($$7);
               if (p_41185_ instanceof ServerPlayer $$8) {
                  CriteriaTriggers.USED_ENDER_EYE.trigger($$8, $$6);
               }

               float $$9 = Mth.lerp(p_41184_.random.nextFloat(), 0.33F, 0.5F);
               p_41184_.playSound(null, p_41185_.getX(), p_41185_.getY(), p_41185_.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 1.0F, $$9);
               $$3.consume(1, p_41185_);
               p_41185_.awardStat(Stats.ITEM_USED.get(this));
               p_41185_.swing(p_41186_, true);
               return InteractionResultHolder.success($$3);
            }
         }

         return InteractionResultHolder.consume($$3);
      }
   }
}
