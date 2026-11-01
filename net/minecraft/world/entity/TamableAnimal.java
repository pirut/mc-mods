package net.minecraft.world.entity;

import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.scores.PlayerTeam;
import org.jspecify.annotations.Nullable;

public abstract class TamableAnimal extends Animal implements OwnableEntity {
   public static final int TELEPORT_WHEN_DISTANCE_IS_SQ = 144;
   private static final int MIN_HORIZONTAL_DISTANCE_FROM_TARGET_AFTER_TELEPORTING = 2;
   private static final int MAX_HORIZONTAL_DISTANCE_FROM_TARGET_AFTER_TELEPORTING = 3;
   private static final int MAX_VERTICAL_DISTANCE_FROM_TARGET_AFTER_TELEPORTING = 1;
   private static final boolean DEFAULT_ORDERED_TO_SIT = false;
   protected static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(TamableAnimal.class, EntityDataSerializers.BYTE);
   protected static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> DATA_OWNERUUID_ID = SynchedEntityData.defineId(
      TamableAnimal.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE
   );
   private boolean orderedToSit = false;

   protected TamableAnimal(EntityType<? extends TamableAnimal> p_21803_, Level p_21804_) {
      super(p_21803_, p_21804_);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_326034_) {
      super.defineSynchedData(p_326034_);
      p_326034_.define(DATA_FLAGS_ID, (byte)0);
      p_326034_.define(DATA_OWNERUUID_ID, Optional.empty());
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_422556_) {
      super.addAdditionalSaveData(p_422556_);
      EntityReference<LivingEntity> entityreference = this.getOwnerReference();
      EntityReference.store(entityreference, p_422556_, "Owner");
      p_422556_.putBoolean("Sitting", this.orderedToSit);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_421492_) {
      super.readAdditionalSaveData(p_421492_);
      EntityReference<LivingEntity> entityreference = EntityReference.readWithOldOwnerConversion(p_421492_, "Owner", this.level());
      if (entityreference != null) {
         try {
            this.entityData.set(DATA_OWNERUUID_ID, Optional.of(entityreference));
            this.setTame(true, false);
         } catch (Throwable var4) {
            this.setTame(false, true);
         }
      } else {
         this.entityData.set(DATA_OWNERUUID_ID, Optional.empty());
         this.setTame(false, true);
      }

      this.orderedToSit = p_421492_.getBooleanOr("Sitting", false);
      this.setInSittingPose(this.orderedToSit);
   }

   @Override
   public boolean canBeLeashed() {
      return true;
   }

   protected void spawnTamingParticles(boolean p_21835_) {
      ParticleOptions particleoptions = ParticleTypes.HEART;
      if (!p_21835_) {
         particleoptions = ParticleTypes.SMOKE;
      }

      for (int i = 0; i < 7; i++) {
         double d0 = this.random.nextGaussian() * 0.02;
         double d1 = this.random.nextGaussian() * 0.02;
         double d2 = this.random.nextGaussian() * 0.02;
         this.level().addParticle(particleoptions, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), d0, d1, d2);
      }
   }

   @Override
   public void handleEntityEvent(byte p_21807_) {
      if (p_21807_ == 7) {
         this.spawnTamingParticles(true);
      } else if (p_21807_ == 6) {
         this.spawnTamingParticles(false);
      } else {
         super.handleEntityEvent(p_21807_);
      }
   }

   public boolean isTame() {
      return (this.entityData.get(DATA_FLAGS_ID) & 4) != 0;
   }

   public void setTame(boolean p_21836_, boolean p_326134_) {
      byte b0 = this.entityData.get(DATA_FLAGS_ID);
      if (p_21836_) {
         this.entityData.set(DATA_FLAGS_ID, (byte)(b0 | 4));
      } else {
         this.entityData.set(DATA_FLAGS_ID, (byte)(b0 & -5));
      }

      if (p_326134_) {
         this.applyTamingSideEffects();
      }
   }

   protected void applyTamingSideEffects() {
   }

   public boolean isInSittingPose() {
      return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
   }

   public void setInSittingPose(boolean p_21838_) {
      byte b0 = this.entityData.get(DATA_FLAGS_ID);
      if (p_21838_) {
         this.entityData.set(DATA_FLAGS_ID, (byte)(b0 | 1));
      } else {
         this.entityData.set(DATA_FLAGS_ID, (byte)(b0 & -2));
      }
   }

   @Nullable
   @Override
   public EntityReference<LivingEntity> getOwnerReference() {
      return this.entityData.get(DATA_OWNERUUID_ID).orElse(null);
   }

   public void setOwner(@Nullable LivingEntity p_394333_) {
      this.entityData.set(DATA_OWNERUUID_ID, Optional.ofNullable(p_394333_).map(EntityReference::of));
   }

   public void setOwnerReference(@Nullable EntityReference<LivingEntity> p_393498_) {
      this.entityData.set(DATA_OWNERUUID_ID, Optional.ofNullable(p_393498_));
   }

   public void tame(Player p_21829_) {
      this.setTame(true, true);
      this.setOwner(p_21829_);
      if (p_21829_ instanceof ServerPlayer serverplayer) {
         CriteriaTriggers.TAME_ANIMAL.trigger(serverplayer, this);
      }
   }

   @Override
   public boolean canAttack(LivingEntity p_21822_) {
      return this.isOwnedBy(p_21822_) ? false : super.canAttack(p_21822_);
   }

   public boolean isOwnedBy(LivingEntity p_21831_) {
      return p_21831_ == this.getOwner();
   }

   public boolean wantsToAttack(LivingEntity p_21810_, LivingEntity p_21811_) {
      return true;
   }

   @Nullable
   @Override
   public PlayerTeam getTeam() {
      PlayerTeam playerteam = super.getTeam();
      if (playerteam != null) {
         return playerteam;
      } else {
         if (this.isTame()) {
            LivingEntity livingentity = this.getRootOwner();
            if (livingentity != null) {
               return livingentity.getTeam();
            }
         }

         return null;
      }
   }

   @Override
   protected boolean considersEntityAsAlly(Entity p_362625_) {
      if (this.isTame()) {
         LivingEntity livingentity = this.getRootOwner();
         if (p_362625_ == livingentity) {
            return true;
         }

         if (livingentity != null) {
            return livingentity.considersEntityAsAlly(p_362625_);
         }
      }

      return super.considersEntityAsAlly(p_362625_);
   }

   @Override
   public void die(DamageSource p_21809_) {
      Component deathMessage = this.getCombatTracker().getDeathMessage();
      super.die(p_21809_);
      if (this.dead
         && this.level() instanceof ServerLevel serverlevel
         && serverlevel.getGameRules().get(GameRules.SHOW_DEATH_MESSAGES)
         && this.getOwner() instanceof ServerPlayer serverplayer) {
         serverplayer.sendSystemMessage(deathMessage);
      }
   }

   public boolean isOrderedToSit() {
      return this.orderedToSit;
   }

   public void setOrderedToSit(boolean p_21840_) {
      this.orderedToSit = p_21840_;
   }

   public void tryToTeleportToOwner() {
      LivingEntity livingentity = this.getOwner();
      if (livingentity != null) {
         this.teleportToAroundBlockPos(livingentity.blockPosition());
      }
   }

   public boolean shouldTryTeleportToOwner() {
      LivingEntity livingentity = this.getOwner();
      return livingentity != null && this.distanceToSqr(this.getOwner()) >= 144.0;
   }

   private void teleportToAroundBlockPos(BlockPos p_350657_) {
      for (int i = 0; i < 10; i++) {
         int j = this.random.nextIntBetweenInclusive(-3, 3);
         int k = this.random.nextIntBetweenInclusive(-3, 3);
         if (Math.abs(j) >= 2 || Math.abs(k) >= 2) {
            int l = this.random.nextIntBetweenInclusive(-1, 1);
            if (this.maybeTeleportTo(p_350657_.getX() + j, p_350657_.getY() + l, p_350657_.getZ() + k)) {
               return;
            }
         }
      }
   }

   private boolean maybeTeleportTo(int p_350930_, int p_350303_, int p_350410_) {
      if (!this.canTeleportTo(new BlockPos(p_350930_, p_350303_, p_350410_))) {
         return false;
      } else {
         this.snapTo(p_350930_ + 0.5, p_350303_, p_350410_ + 0.5, this.getYRot(), this.getXRot());
         this.navigation.stop();
         return true;
      }
   }

   private boolean canTeleportTo(BlockPos p_350767_) {
      PathType pathtype = WalkNodeEvaluator.getPathTypeStatic(this, p_350767_);
      if (pathtype != PathType.WALKABLE) {
         return false;
      } else {
         BlockState blockstate = this.level().getBlockState(p_350767_.below());
         if (!this.canFlyToOwner() && blockstate.getBlock() instanceof LeavesBlock) {
            return false;
         } else {
            BlockPos blockpos = p_350767_.subtract(this.blockPosition());
            return this.level().noCollision(this, this.getBoundingBox().move(blockpos));
         }
      }
   }

   public final boolean unableToMoveToOwner() {
      return this.isOrderedToSit() || this.isPassenger() || this.mayBeLeashed() || this.getOwner() != null && this.getOwner().isSpectator();
   }

   protected boolean canFlyToOwner() {
      return false;
   }

   public class TamableAnimalPanicGoal extends PanicGoal {
      public TamableAnimalPanicGoal(double p_350422_, TagKey<DamageType> p_350418_) {
         super(TamableAnimal.this, p_350422_, p_350418_);
      }

      public TamableAnimalPanicGoal(double p_350932_) {
         super(TamableAnimal.this, p_350932_);
      }

      @Override
      public void tick() {
         if (!TamableAnimal.this.unableToMoveToOwner() && TamableAnimal.this.shouldTryTeleportToOwner()) {
            TamableAnimal.this.tryToTeleportToOwner();
         }

         super.tick();
      }
   }
}
