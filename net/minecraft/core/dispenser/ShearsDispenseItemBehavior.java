package net.minecraft.core.dispenser;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.IShearable;

public class ShearsDispenseItemBehavior extends OptionalDispenseItemBehavior {
   @Override
   protected ItemStack execute(BlockSource p_302443_, ItemStack p_123581_) {
      ServerLevel serverlevel = p_302443_.level();
      if (!serverlevel.isClientSide()) {
         BlockPos blockpos = p_302443_.pos().relative(p_302443_.state().getValue(DispenserBlock.FACING));
         this.setSuccess(
            CommonHooks.tryDispenseShearsHarvestBlock(p_302443_, p_123581_, serverlevel, blockpos)
               || tryShearBeehive(serverlevel, p_123581_, blockpos)
               || tryShearEntity(serverlevel, blockpos, p_123581_)
         );
         if (this.isSuccess()) {
            p_123581_.hurtAndBreak(1, serverlevel, null, p_348118_ -> {});
         }
      }

      return p_123581_;
   }

   private static boolean tryShearBeehive(ServerLevel p_123577_, ItemStack p_432828_, BlockPos p_123578_) {
      BlockState blockstate = p_123577_.getBlockState(p_123578_);
      if (blockstate.is(BlockTags.BEEHIVES, p_202454_ -> p_202454_.hasProperty(BeehiveBlock.HONEY_LEVEL) && p_202454_.getBlock() instanceof BeehiveBlock)) {
         int i = blockstate.getValue(BeehiveBlock.HONEY_LEVEL);
         if (i >= 5) {
            p_123577_.playSound(null, p_123578_, SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
            BeehiveBlock.dropHoneycomb(p_123577_, p_432828_, blockstate, p_123577_.getBlockEntity(p_123578_), null, p_123578_);
            ((BeehiveBlock)blockstate.getBlock())
               .releaseBeesAndResetHoneyLevel(p_123577_, blockstate, p_123578_, null, BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED);
            p_123577_.gameEvent(null, GameEvent.SHEAR, p_123578_);
            return true;
         }
      }

      return false;
   }

   private static boolean tryShearEntity(ServerLevel p_418032_, BlockPos p_418277_, ItemStack p_418413_) {
      for (Entity entity : p_418032_.getEntitiesOfClass(Entity.class, new AABB(p_418277_), EntitySelector.NO_SPECTATORS)) {
         if (entity.shearOffAllLeashConnections(null)) {
            return true;
         }

         if (entity instanceof IShearable shearable && shearable.isShearable(null, p_418413_, p_418032_, p_418277_)) {
            shearable.onSheared(null, p_418413_, p_418032_, p_418277_).forEach(drop -> shearable.spawnShearedDrop(p_418032_, p_418277_, drop));
            p_418032_.gameEvent(null, GameEvent.SHEAR, p_418277_);
            return true;
         }
      }

      return false;
   }
}
