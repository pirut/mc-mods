package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

public class EndGatewayBlock extends BaseEntityBlock implements Portal {
   public static final MapCodec<EndGatewayBlock> CODEC = simpleCodec(EndGatewayBlock::new);

   @Override
   public MapCodec<EndGatewayBlock> codec() {
      return CODEC;
   }

   public EndGatewayBlock(BlockBehaviour.Properties p_52999_) {
      super(p_52999_);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_153193_, BlockState p_153194_) {
      return new TheEndGatewayBlockEntity(p_153193_, p_153194_);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_153189_, BlockState p_153190_, BlockEntityType<T> p_153191_) {
      return createTickerHelper(
         p_153191_, BlockEntityType.END_GATEWAY, p_153189_.isClientSide ? TheEndGatewayBlockEntity::beamAnimationTick : TheEndGatewayBlockEntity::portalTick
      );
   }

   @Override
   public void animateTick(BlockState p_221097_, Level p_221098_, BlockPos p_221099_, RandomSource p_221100_) {
      BlockEntity $$4 = p_221098_.getBlockEntity(p_221099_);
      if ($$4 instanceof TheEndGatewayBlockEntity) {
         int $$5 = ((TheEndGatewayBlockEntity)$$4).getParticleAmount();

         for (int $$6 = 0; $$6 < $$5; $$6++) {
            double $$7 = p_221099_.getX() + p_221100_.nextDouble();
            double $$8 = p_221099_.getY() + p_221100_.nextDouble();
            double $$9 = p_221099_.getZ() + p_221100_.nextDouble();
            double $$10 = (p_221100_.nextDouble() - 0.5) * 0.5;
            double $$11 = (p_221100_.nextDouble() - 0.5) * 0.5;
            double $$12 = (p_221100_.nextDouble() - 0.5) * 0.5;
            int $$13 = p_221100_.nextInt(2) * 2 - 1;
            if (p_221100_.nextBoolean()) {
               $$9 = p_221099_.getZ() + 0.5 + 0.25 * $$13;
               $$12 = p_221100_.nextFloat() * 2.0F * $$13;
            } else {
               $$7 = p_221099_.getX() + 0.5 + 0.25 * $$13;
               $$10 = p_221100_.nextFloat() * 2.0F * $$13;
            }

            p_221098_.addParticle(ParticleTypes.PORTAL, $$7, $$8, $$9, $$10, $$11, $$12);
         }
      }
   }

   @Override
   public ItemStack getCloneItemStack(LevelReader p_304768_, BlockPos p_53004_, BlockState p_53005_) {
      return ItemStack.EMPTY;
   }

   @Override
   protected boolean canBeReplaced(BlockState p_53012_, Fluid p_53013_) {
      return false;
   }

   @Override
   protected void entityInside(BlockState p_350647_, Level p_350785_, BlockPos p_350610_, Entity p_350849_) {
      if (p_350849_.canUsePortal(false)
         && !p_350785_.isClientSide
         && p_350785_.getBlockEntity(p_350610_) instanceof TheEndGatewayBlockEntity $$5
         && !$$5.isCoolingDown()) {
         p_350849_.setAsInsidePortal(this, p_350610_);
         TheEndGatewayBlockEntity.triggerCooldown(p_350785_, p_350610_, p_350647_, $$5);
      }
   }

   @Nullable
   @Override
   public DimensionTransition getPortalDestination(ServerLevel p_350958_, Entity p_350650_, BlockPos p_350525_) {
      if (p_350958_.getBlockEntity(p_350525_) instanceof TheEndGatewayBlockEntity $$4) {
         Vec3 $$6 = $$4.getPortalPosition(p_350958_, p_350525_);
         return $$6 != null
            ? new DimensionTransition(
               p_350958_, $$6, calculateExitMovement(p_350650_), p_350650_.getYRot(), p_350650_.getXRot(), DimensionTransition.PLACE_PORTAL_TICKET
            )
            : null;
      } else {
         return null;
      }
   }

   private static Vec3 calculateExitMovement(Entity p_352063_) {
      return p_352063_ instanceof ThrownEnderpearl ? new Vec3(0.0, -1.0, 0.0) : p_352063_.getDeltaMovement();
   }
}
