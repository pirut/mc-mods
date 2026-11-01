package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class TntBlock extends Block {
   public static final MapCodec<TntBlock> CODEC = simpleCodec(TntBlock::new);
   public static final BooleanProperty UNSTABLE = BlockStateProperties.UNSTABLE;

   @Override
   public MapCodec<TntBlock> codec() {
      return CODEC;
   }

   public TntBlock(BlockBehaviour.Properties p_57422_) {
      super(p_57422_);
      this.registerDefaultState(this.defaultBlockState().setValue(UNSTABLE, false));
   }

   @Override
   protected void onPlace(BlockState p_57466_, Level p_57467_, BlockPos p_57468_, BlockState p_57469_, boolean p_57470_) {
      if (!p_57469_.is(p_57466_.getBlock()) && p_57467_.hasNeighborSignal(p_57468_) && this.onCaughtFire(p_57466_, p_57467_, p_57468_, null, null)) {
         p_57467_.removeBlock(p_57468_, false);
      }
   }

   @Override
   protected void neighborChanged(BlockState p_57457_, Level p_57458_, BlockPos p_57459_, Block p_57460_, @Nullable Orientation p_364510_, boolean p_57462_) {
      if (p_57458_.hasNeighborSignal(p_57459_) && this.onCaughtFire(p_57457_, p_57458_, p_57459_, null, null)) {
         p_57458_.removeBlock(p_57459_, false);
      }
   }

   @Override
   public BlockState playerWillDestroy(Level p_57445_, BlockPos p_57446_, BlockState p_57447_, Player p_57448_) {
      if (!p_57445_.isClientSide() && !p_57448_.getAbilities().instabuild && p_57447_.getValue(UNSTABLE)) {
         this.onCaughtFire(p_57447_, p_57445_, p_57446_, null, null);
      }

      return super.playerWillDestroy(p_57445_, p_57446_, p_57447_, p_57448_);
   }

   @Override
   public void wasExploded(ServerLevel p_364953_, BlockPos p_57442_, Explosion p_57443_) {
      if (p_364953_.getGameRules().get(GameRules.TNT_EXPLODES)) {
         PrimedTnt primedtnt = new PrimedTnt(p_364953_, p_57442_.getX() + 0.5, p_57442_.getY(), p_57442_.getZ() + 0.5, p_57443_.getIndirectSourceEntity());
         int i = primedtnt.getFuse();
         primedtnt.setFuse((short)(p_364953_.random.nextInt(i / 4) + i / 8));
         p_364953_.addFreshEntity(primedtnt);
      }
   }

   @Deprecated
   public static boolean prime(Level p_57437_, BlockPos p_57438_) {
      return prime(p_57437_, p_57438_, null);
   }

   @Deprecated
   private static boolean prime(Level p_394104_, BlockPos p_393490_, @Nullable LivingEntity p_411047_) {
      if (p_394104_ instanceof ServerLevel serverlevel && serverlevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
         PrimedTnt primedtnt = new PrimedTnt(p_394104_, p_393490_.getX() + 0.5, p_393490_.getY(), p_393490_.getZ() + 0.5, p_411047_);
         p_394104_.addFreshEntity(primedtnt);
         p_394104_.playSound(null, primedtnt.getX(), primedtnt.getY(), primedtnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
         p_394104_.gameEvent(p_411047_, GameEvent.PRIME_FUSE, p_393490_);
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack p_316149_, BlockState p_316217_, Level p_316520_, BlockPos p_316601_, Player p_316770_, InteractionHand p_316393_, BlockHitResult p_316532_
   ) {
      if (!p_316149_.is(Items.FLINT_AND_STEEL) && !p_316149_.is(Items.FIRE_CHARGE)) {
         return super.useItemOn(p_316149_, p_316217_, p_316520_, p_316601_, p_316770_, p_316393_, p_316532_);
      } else {
         if (this.onCaughtFire(p_316217_, p_316520_, p_316601_, p_316532_.getDirection(), p_316770_)) {
            p_316520_.setBlock(p_316601_, Blocks.AIR.defaultBlockState(), 11);
            Item item = p_316149_.getItem();
            if (p_316149_.is(Items.FLINT_AND_STEEL)) {
               p_316149_.hurtAndBreak(1, p_316770_, p_316393_.asEquipmentSlot());
            } else {
               p_316149_.consume(1, p_316770_);
            }

            p_316770_.awardStat(Stats.ITEM_USED.get(item));
         } else if (p_316520_ instanceof ServerLevel serverlevel && !serverlevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
            p_316770_.displayClientMessage(Component.translatable("block.minecraft.tnt.disabled"), true);
            return InteractionResult.PASS;
         }

         return InteractionResult.SUCCESS;
      }
   }

   @Override
   protected void onProjectileHit(Level p_57429_, BlockState p_57430_, BlockHitResult p_57431_, Projectile p_57432_) {
      if (p_57429_ instanceof ServerLevel serverlevel) {
         BlockPos blockpos = p_57431_.getBlockPos();
         Entity entity = p_57432_.getOwner();
         if (p_57432_.isOnFire()
            && p_57432_.mayInteract(serverlevel, blockpos)
            && this.onCaughtFire(p_57430_, p_57429_, blockpos, null, entity instanceof LivingEntity ? (LivingEntity)entity : null)) {
            p_57429_.removeBlock(blockpos, false);
         }
      }
   }

   @Override
   public boolean dropFromExplosion(Explosion p_57427_) {
      return false;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_57464_) {
      p_57464_.add(UNSTABLE);
   }

   @Override
   public boolean onCaughtFire(BlockState state, Level world, BlockPos pos, @Nullable Direction face, @Nullable LivingEntity igniter) {
      return prime(world, pos, igniter);
   }
}
