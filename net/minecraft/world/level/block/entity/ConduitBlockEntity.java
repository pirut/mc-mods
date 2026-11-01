package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ConduitBlockEntity extends BlockEntity {
   private static final int BLOCK_REFRESH_RATE = 2;
   private static final int EFFECT_DURATION = 13;
   private static final float ROTATION_SPEED = -0.0375F;
   private static final int MIN_ACTIVE_SIZE = 16;
   private static final int MIN_KILL_SIZE = 42;
   private static final int KILL_RANGE = 8;
   private static final Block[] VALID_BLOCKS = new Block[]{Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.SEA_LANTERN, Blocks.DARK_PRISMARINE};
   public int tickCount;
   private float activeRotation;
   private boolean isActive;
   private boolean isHunting;
   private final List<BlockPos> effectBlocks = Lists.newArrayList();
   @Nullable
   private EntityReference<LivingEntity> destroyTarget;
   private long nextAmbientSoundActivation;

   public ConduitBlockEntity(BlockPos pos, BlockState blockState) {
      super(BlockEntityType.CONDUIT, pos, blockState);
   }

   @Override
   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.destroyTarget = EntityReference.read(input, "Target");
   }

   @Override
   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      EntityReference.store(this.destroyTarget, output, "Target");
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      return this.saveCustomOnly(registries);
   }

   public static void clientTick(Level level, BlockPos pos, BlockState state, ConduitBlockEntity blockEntity) {
      blockEntity.tickCount++;
      long l = level.getGameTime();
      List<BlockPos> list = blockEntity.effectBlocks;
      if (l % 40L == 0L) {
         blockEntity.isActive = updateShape(level, pos, list);
         updateHunting(blockEntity, list);
      }

      LivingEntity livingEntity = EntityReference.getLivingEntity(blockEntity.destroyTarget, level);
      animationTick(level, pos, list, livingEntity, blockEntity.tickCount);
      if (blockEntity.isActive()) {
         blockEntity.activeRotation++;
      }
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, ConduitBlockEntity blockEntity) {
      blockEntity.tickCount++;
      long l = level.getGameTime();
      List<BlockPos> list = blockEntity.effectBlocks;
      if (l % 40L == 0L) {
         boolean bl = updateShape(level, pos, list);
         if (bl != blockEntity.isActive) {
            SoundEvent soundEvent = bl ? SoundEvents.CONDUIT_ACTIVATE : SoundEvents.CONDUIT_DEACTIVATE;
            level.playSound(null, pos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
         }

         blockEntity.isActive = bl;
         updateHunting(blockEntity, list);
         if (bl) {
            applyEffects(level, pos, list);
            updateAndAttackTarget((ServerLevel)level, pos, state, blockEntity, list.size() >= 42);
         }
      }

      if (blockEntity.isActive()) {
         if (l % 80L == 0L) {
            level.playSound(null, pos, SoundEvents.CONDUIT_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
         }

         if (l > blockEntity.nextAmbientSoundActivation) {
            blockEntity.nextAmbientSoundActivation = l + 60L + level.getRandom().nextInt(40);
            level.playSound(null, pos, SoundEvents.CONDUIT_AMBIENT_SHORT, SoundSource.BLOCKS, 1.0F, 1.0F);
         }
      }
   }

   private static void updateHunting(ConduitBlockEntity blockEntity, List<BlockPos> positions) {
      blockEntity.setHunting(positions.size() >= 42);
   }

   private static boolean updateShape(Level level, BlockPos pos, List<BlockPos> positions) {
      positions.clear();

      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 1; k++) {
               BlockPos blockPos = pos.offset(i, j, k);
               if (!level.isWaterAt(blockPos)) {
                  return false;
               }
            }
         }
      }

      for (int i = -2; i <= 2; i++) {
         for (int j = -2; j <= 2; j++) {
            for (int kx = -2; kx <= 2; kx++) {
               int l = Math.abs(i);
               int m = Math.abs(j);
               int n = Math.abs(kx);
               if ((l > 1 || m > 1 || n > 1) && (i == 0 && (m == 2 || n == 2) || j == 0 && (l == 2 || n == 2) || kx == 0 && (l == 2 || m == 2))) {
                  BlockPos blockPos2 = pos.offset(i, j, kx);
                  BlockState blockState = level.getBlockState(blockPos2);

                  for (Block block : VALID_BLOCKS) {
                     if (blockState.is(block)) {
                        positions.add(blockPos2);
                     }
                  }
               }
            }
         }
      }

      return positions.size() >= 16;
   }

   private static void applyEffects(Level level, BlockPos pos, List<BlockPos> positions) {
      int i = positions.size();
      int j = i / 7 * 16;
      int k = pos.getX();
      int l = pos.getY();
      int m = pos.getZ();
      AABB aABB = new AABB(k, l, m, k + 1, l + 1, m + 1).inflate(j).expandTowards(0.0, level.getHeight(), 0.0);
      List<Player> list = level.getEntitiesOfClass(Player.class, aABB);
      if (!list.isEmpty()) {
         for (Player player : list) {
            if (pos.closerThan(player.blockPosition(), j) && player.isInWaterOrRain()) {
               player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 260, 0, true, true));
            }
         }
      }
   }

   private static void updateAndAttackTarget(ServerLevel level, BlockPos pos, BlockState state, ConduitBlockEntity blockEntity, boolean canDestroy) {
      EntityReference<LivingEntity> entityReference = updateDestroyTarget(blockEntity.destroyTarget, level, pos, canDestroy);
      LivingEntity livingEntity = EntityReference.getLivingEntity(entityReference, level);
      if (livingEntity != null) {
         level.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.CONDUIT_ATTACK_TARGET, SoundSource.BLOCKS, 1.0F, 1.0F);
         livingEntity.hurtServer(level, level.damageSources().magic(), 4.0F);
      }

      if (!Objects.equals(entityReference, blockEntity.destroyTarget)) {
         blockEntity.destroyTarget = entityReference;
         level.sendBlockUpdated(pos, state, state, 2);
      }
   }

   @Nullable
   private static EntityReference<LivingEntity> updateDestroyTarget(
      @Nullable EntityReference<LivingEntity> destroyTarget, ServerLevel level, BlockPos pos, boolean canDestroy
   ) {
      if (!canDestroy) {
         return null;
      } else if (destroyTarget == null) {
         return selectNewTarget(level, pos);
      } else {
         LivingEntity livingEntity = EntityReference.getLivingEntity(destroyTarget, level);
         return livingEntity != null && livingEntity.isAlive() && pos.closerThan(livingEntity.blockPosition(), 8.0) ? destroyTarget : null;
      }
   }

   @Nullable
   private static EntityReference<LivingEntity> selectNewTarget(ServerLevel level, BlockPos pos) {
      List<LivingEntity> list = level.getEntitiesOfClass(
         LivingEntity.class, getDestroyRangeAABB(pos), livingEntity -> livingEntity instanceof Enemy && livingEntity.isInWaterOrRain()
      );
      return list.isEmpty() ? null : EntityReference.of(Util.getRandom(list, level.random));
   }

   private static AABB getDestroyRangeAABB(BlockPos pos) {
      return new AABB(pos).inflate(8.0);
   }

   private static void animationTick(Level level, BlockPos pos, List<BlockPos> positions, @Nullable Entity entity, int tickCount) {
      RandomSource randomSource = level.random;
      double d = Mth.sin((tickCount + 35) * 0.1F) / 2.0F + 0.5F;
      d = (d * d + d) * 0.3F;
      Vec3 vec3 = new Vec3(pos.getX() + 0.5, pos.getY() + 1.5 + d, pos.getZ() + 0.5);

      for (BlockPos blockPos : positions) {
         if (randomSource.nextInt(50) == 0) {
            BlockPos blockPos2 = blockPos.subtract(pos);
            float f = -0.5F + randomSource.nextFloat() + blockPos2.getX();
            float g = -2.0F + randomSource.nextFloat() + blockPos2.getY();
            float h = -0.5F + randomSource.nextFloat() + blockPos2.getZ();
            level.addParticle(ParticleTypes.NAUTILUS, vec3.x, vec3.y, vec3.z, f, g, h);
         }
      }

      if (entity != null) {
         Vec3 vec32 = new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());
         float i = (-0.5F + randomSource.nextFloat()) * (3.0F + entity.getBbWidth());
         float j = -1.0F + randomSource.nextFloat() * entity.getBbHeight();
         float f = (-0.5F + randomSource.nextFloat()) * (3.0F + entity.getBbWidth());
         Vec3 vec33 = new Vec3(i, j, f);
         level.addParticle(ParticleTypes.NAUTILUS, vec32.x, vec32.y, vec32.z, vec33.x, vec33.y, vec33.z);
      }
   }

   public boolean isActive() {
      return this.isActive;
   }

   public boolean isHunting() {
      return this.isHunting;
   }

   private void setHunting(boolean isHunting) {
      this.isHunting = isHunting;
   }

   public float getActiveRotation(float partialTick) {
      return (this.activeRotation + partialTick) * -0.0375F;
   }
}
