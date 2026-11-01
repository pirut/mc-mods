package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class EndPortalBlock extends BaseEntityBlock implements Portal {
   public static final MapCodec<EndPortalBlock> CODEC = simpleCodec(EndPortalBlock::new);
   private static final VoxelShape SHAPE = Block.column(16.0, 6.0, 12.0);

   @Override
   public MapCodec<EndPortalBlock> codec() {
      return CODEC;
   }

   protected EndPortalBlock(BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new TheEndPortalBlockEntity(pos, state);
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Override
   protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
      return state.getShape(level, pos);
   }

   @Override
   protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean intersects) {
      if (entity.canUsePortal(false)) {
         if (!level.isClientSide() && level.dimension() == Level.END && entity instanceof ServerPlayer serverPlayer && !serverPlayer.seenCredits) {
            serverPlayer.showEndCredits();
         } else {
            entity.setAsInsidePortal(this, pos);
         }
      }
   }

   @Nullable
   @Override
   public TeleportTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
      LevelData.RespawnData respawnData = level.getRespawnData();
      ResourceKey<Level> resourceKey = level.dimension();
      boolean bl = resourceKey == Level.END;
      ResourceKey<Level> resourceKey2 = bl ? respawnData.dimension() : Level.END;
      BlockPos blockPos = bl ? respawnData.pos() : ServerLevel.END_SPAWN_POINT;
      ServerLevel serverLevel = level.getServer().getLevel(resourceKey2);
      if (serverLevel == null) {
         return null;
      } else {
         Vec3 vec3 = blockPos.getBottomCenter();
         float f;
         float g;
         Set<Relative> set;
         if (!bl) {
            EndPlatformFeature.createEndPlatform(serverLevel, BlockPos.containing(vec3).below(), true);
            f = Direction.WEST.toYRot();
            g = 0.0F;
            set = Relative.union(Relative.DELTA, Set.of(Relative.X_ROT));
            if (entity instanceof ServerPlayer) {
               vec3 = vec3.subtract(0.0, 1.0, 0.0);
            }
         } else {
            f = respawnData.yaw();
            g = respawnData.pitch();
            set = Relative.union(Relative.DELTA, Relative.ROTATION);
            if (entity instanceof ServerPlayer serverPlayer) {
               return serverPlayer.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
            }

            vec3 = entity.adjustSpawnLocation(serverLevel, blockPos).getBottomCenter();
         }

         return new TeleportTransition(
            serverLevel, vec3, Vec3.ZERO, f, g, set, TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET)
         );
      }
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      double d = pos.getX() + random.nextDouble();
      double e = pos.getY() + 0.8;
      double f = pos.getZ() + random.nextDouble();
      level.addParticle(ParticleTypes.SMOKE, d, e, f, 0.0, 0.0, 0.0);
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
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }
}
