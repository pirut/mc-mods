package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
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
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EndGatewayBlock extends BaseEntityBlock implements Portal {
   public static final MapCodec<EndGatewayBlock> CODEC = simpleCodec(EndGatewayBlock::new);

   @Override
   public MapCodec<EndGatewayBlock> codec() {
      return CODEC;
   }

   protected EndGatewayBlock(BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new TheEndGatewayBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
      return createTickerHelper(
         blockEntityType,
         BlockEntityType.END_GATEWAY,
         level.isClientSide() ? TheEndGatewayBlockEntity::beamAnimationTick : TheEndGatewayBlockEntity::portalTick
      );
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity instanceof TheEndGatewayBlockEntity) {
         int i = ((TheEndGatewayBlockEntity)blockEntity).getParticleAmount();

         for (int j = 0; j < i; j++) {
            double d = pos.getX() + random.nextDouble();
            double e = pos.getY() + random.nextDouble();
            double f = pos.getZ() + random.nextDouble();
            double g = (random.nextDouble() - 0.5) * 0.5;
            double h = (random.nextDouble() - 0.5) * 0.5;
            double k = (random.nextDouble() - 0.5) * 0.5;
            int l = random.nextInt(2) * 2 - 1;
            if (random.nextBoolean()) {
               f = pos.getZ() + 0.5 + 0.25 * l;
               k = random.nextFloat() * 2.0F * l;
            } else {
               d = pos.getX() + 0.5 + 0.25 * l;
               g = random.nextFloat() * 2.0F * l;
            }

            level.addParticle(ParticleTypes.PORTAL, d, e, f, g, h, k);
         }
      }
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
      return ItemStack.EMPTY;
   }

   @Override
   protected boolean canBeReplaced(BlockState state, Fluid fluid) {
      return false;
   }

   @Override
   protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean intersects) {
      if (entity.canUsePortal(false)
         && !level.isClientSide()
         && level.getBlockEntity(pos) instanceof TheEndGatewayBlockEntity theEndGatewayBlockEntity
         && !theEndGatewayBlockEntity.isCoolingDown()) {
         entity.setAsInsidePortal(this, pos);
         TheEndGatewayBlockEntity.triggerCooldown(level, pos, state, theEndGatewayBlockEntity);
      }
   }

   @Nullable
   @Override
   public TeleportTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
      if (level.getBlockEntity(pos) instanceof TheEndGatewayBlockEntity theEndGatewayBlockEntity) {
         Vec3 vec3 = theEndGatewayBlockEntity.getPortalPosition(level, pos);
         if (vec3 == null) {
            return null;
         } else {
            return entity instanceof ThrownEnderpearl
               ? new TeleportTransition(level, vec3, Vec3.ZERO, 0.0F, 0.0F, Set.of(), TeleportTransition.PLACE_PORTAL_TICKET)
               : new TeleportTransition(
                  level, vec3, Vec3.ZERO, 0.0F, 0.0F, Relative.union(Relative.DELTA, Relative.ROTATION), TeleportTransition.PLACE_PORTAL_TICKET
               );
         }
      } else {
         return null;
      }
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }
}
