package net.minecraft.world.entity.animal.turtle;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Turtle extends Animal {
   private static final EntityDataAccessor<Boolean> HAS_EGG = SynchedEntityData.defineId(Turtle.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> LAYING_EGG = SynchedEntityData.defineId(Turtle.class, EntityDataSerializers.BOOLEAN);
   private static final float BABY_SCALE = 0.3F;
   private static final EntityDimensions BABY_DIMENSIONS = EntityType.TURTLE
      .getDimensions()
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, EntityType.TURTLE.getHeight(), -0.25F))
      .scale(0.3F);
   private static final boolean DEFAULT_HAS_EGG = false;
   int layEggCounter;
   public static final TargetingConditions.Selector BABY_ON_LAND_SELECTOR = (p_481962_, p_479510_) -> p_481962_.isBaby() && !p_481962_.isInWater();
   BlockPos homePos = BlockPos.ZERO;
   @Nullable
   BlockPos travelPos;
   boolean goingHome;

   public Turtle(EntityType<? extends Turtle> p_480357_, Level p_480008_) {
      super(p_480357_, p_480008_);
      this.setPathfindingMalus(PathType.WATER, 0.0F);
      this.setPathfindingMalus(PathType.DOOR_IRON_CLOSED, -1.0F);
      this.setPathfindingMalus(PathType.DOOR_WOOD_CLOSED, -1.0F);
      this.setPathfindingMalus(PathType.DOOR_OPEN, -1.0F);
      this.moveControl = new Turtle.TurtleMoveControl(this);
   }

   public void setHomePos(BlockPos p_481812_) {
      this.homePos = p_481812_;
   }

   public boolean hasEgg() {
      return this.entityData.get(HAS_EGG);
   }

   void setHasEgg(boolean p_478382_) {
      this.entityData.set(HAS_EGG, p_478382_);
   }

   public boolean isLayingEgg() {
      return this.entityData.get(LAYING_EGG);
   }

   void setLayingEgg(boolean p_478953_) {
      this.layEggCounter = p_478953_ ? 1 : 0;
      this.entityData.set(LAYING_EGG, p_478953_);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_478357_) {
      super.defineSynchedData(p_478357_);
      p_478357_.define(HAS_EGG, false);
      p_478357_.define(LAYING_EGG, false);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_478469_) {
      super.addAdditionalSaveData(p_478469_);
      p_478469_.store("home_pos", BlockPos.CODEC, this.homePos);
      p_478469_.putBoolean("has_egg", this.hasEgg());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_480285_) {
      this.setHomePos(p_480285_.<BlockPos>read("home_pos", BlockPos.CODEC).orElse(this.blockPosition()));
      super.readAdditionalSaveData(p_480285_);
      this.setHasEgg(p_480285_.getBooleanOr("has_egg", false));
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_477950_, DifficultyInstance p_481603_, EntitySpawnReason p_479654_, @Nullable SpawnGroupData p_481163_
   ) {
      this.setHomePos(this.blockPosition());
      return super.finalizeSpawn(p_477950_, p_481603_, p_479654_, p_481163_);
   }

   public static boolean checkTurtleSpawnRules(
      EntityType<Turtle> p_480571_, LevelAccessor p_478756_, EntitySpawnReason p_481928_, BlockPos p_479335_, RandomSource p_478362_
   ) {
      return p_479335_.getY() < p_478756_.getSeaLevel() + 4 && TurtleEggBlock.onSand(p_478756_, p_479335_) && isBrightEnoughToSpawn(p_478756_, p_479335_);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new Turtle.TurtlePanicGoal(this, 1.2));
      this.goalSelector.addGoal(1, new Turtle.TurtleBreedGoal(this, 1.0));
      this.goalSelector.addGoal(1, new Turtle.TurtleLayEggGoal(this, 1.0));
      this.goalSelector.addGoal(2, new TemptGoal(this, 1.1, p_480792_ -> p_480792_.is(ItemTags.TURTLE_FOOD), false));
      this.goalSelector.addGoal(3, new Turtle.TurtleGoToWaterGoal(this, 1.0));
      this.goalSelector.addGoal(4, new Turtle.TurtleGoHomeGoal(this, 1.0));
      this.goalSelector.addGoal(7, new Turtle.TurtleTravelGoal(this, 1.0));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(9, new Turtle.TurtleRandomStrollGoal(this, 1.0, 100));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.STEP_HEIGHT, 1.0);
   }

   @Override
   public boolean isPushedByFluid() {
      return false;
   }

   @Override
   public int getAmbientSoundInterval() {
      return 200;
   }

   @Nullable
   @Override
   protected SoundEvent getAmbientSound() {
      return !this.isInWater() && this.onGround() && !this.isBaby() ? SoundEvents.TURTLE_AMBIENT_LAND : super.getAmbientSound();
   }

   @Override
   protected void playSwimSound(float p_481914_) {
      super.playSwimSound(p_481914_ * 1.5F);
   }

   @Override
   protected SoundEvent getSwimSound() {
      return SoundEvents.TURTLE_SWIM;
   }

   @Nullable
   @Override
   protected SoundEvent getHurtSound(DamageSource p_481724_) {
      return this.isBaby() ? SoundEvents.TURTLE_HURT_BABY : SoundEvents.TURTLE_HURT;
   }

   @Nullable
   @Override
   protected SoundEvent getDeathSound() {
      return this.isBaby() ? SoundEvents.TURTLE_DEATH_BABY : SoundEvents.TURTLE_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos p_480025_, BlockState p_481304_) {
      SoundEvent soundevent = this.isBaby() ? SoundEvents.TURTLE_SHAMBLE_BABY : SoundEvents.TURTLE_SHAMBLE;
      this.playSound(soundevent, 0.15F, 1.0F);
   }

   @Override
   public boolean canFallInLove() {
      return super.canFallInLove() && !this.hasEgg();
   }

   @Override
   protected float nextStep() {
      return this.moveDist + 0.15F;
   }

   @Override
   public float getAgeScale() {
      return this.isBaby() ? 0.3F : 1.0F;
   }

   @Override
   protected PathNavigation createNavigation(Level p_478374_) {
      return new Turtle.TurtlePathNavigation(this, p_478374_);
   }

   @Nullable
   @Override
   public AgeableMob getBreedOffspring(ServerLevel p_480191_, AgeableMob p_479556_) {
      return EntityType.TURTLE.create(p_480191_, EntitySpawnReason.BREEDING);
   }

   @Override
   public boolean isFood(ItemStack p_479291_) {
      return p_479291_.is(ItemTags.TURTLE_FOOD);
   }

   @Override
   public float getWalkTargetValue(BlockPos p_478157_, LevelReader p_479686_) {
      if (!this.goingHome && p_479686_.getFluidState(p_478157_).is(FluidTags.WATER)) {
         return 10.0F;
      } else {
         return TurtleEggBlock.onSand(p_479686_, p_478157_) ? 10.0F : p_479686_.getPathfindingCostFromLightLevels(p_478157_);
      }
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (this.isAlive() && this.isLayingEgg() && this.layEggCounter >= 1 && this.layEggCounter % 5 == 0) {
         BlockPos blockpos = this.blockPosition();
         if (TurtleEggBlock.onSand(this.level(), blockpos)) {
            this.level().levelEvent(2001, blockpos, Block.getId(this.level().getBlockState(blockpos.below())));
            this.gameEvent(GameEvent.ENTITY_ACTION);
         }
      }
   }

   @Override
   protected void ageBoundaryReached() {
      super.ageBoundaryReached();
      if (!this.isBaby() && this.level() instanceof ServerLevel serverlevel && serverlevel.getGameRules().get(GameRules.MOB_DROPS)) {
         this.dropFromGiftLootTable(serverlevel, BuiltInLootTables.TURTLE_GROW, this::spawnAtLocation);
      }
   }

   @Override
   protected void travelInWater(Vec3 p_479713_, double p_481524_, boolean p_480052_, double p_480388_) {
      this.moveRelative(0.1F, p_479713_);
      this.move(MoverType.SELF, this.getDeltaMovement());
      this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
      if (this.getTarget() == null && (!this.goingHome || !this.homePos.closerToCenterThan(this.position(), 20.0))) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
      }
   }

   @Override
   public boolean canBeLeashed() {
      return false;
   }

   @Override
   public void thunderHit(ServerLevel p_480048_, LightningBolt p_481935_) {
      this.hurtServer(p_480048_, this.damageSources().lightningBolt(), Float.MAX_VALUE);
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose p_480223_) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(p_480223_);
   }

   static class TurtleBreedGoal extends BreedGoal {
      private final Turtle turtle;

      TurtleBreedGoal(Turtle p_479204_, double p_479002_) {
         super(p_479204_, p_479002_);
         this.turtle = p_479204_;
      }

      @Override
      public boolean canUse() {
         return super.canUse() && !this.turtle.hasEgg();
      }

      @Override
      protected void breed() {
         ServerPlayer serverplayer = this.animal.getLoveCause();
         if (serverplayer == null && this.partner.getLoveCause() != null) {
            serverplayer = this.partner.getLoveCause();
         }

         if (serverplayer != null) {
            serverplayer.awardStat(Stats.ANIMALS_BRED);
            CriteriaTriggers.BRED_ANIMALS.trigger(serverplayer, this.animal, this.partner, null);
         }

         this.turtle.setHasEgg(true);
         this.animal.setAge(6000);
         this.partner.setAge(6000);
         this.animal.resetLove();
         this.partner.resetLove();
         RandomSource randomsource = this.animal.getRandom();
         if (getServerLevel(this.level).getGameRules().get(GameRules.MOB_DROPS)) {
            this.level.addFreshEntity(new ExperienceOrb(this.level, this.animal.getX(), this.animal.getY(), this.animal.getZ(), randomsource.nextInt(7) + 1));
         }
      }
   }

   static class TurtleGoHomeGoal extends Goal {
      private final Turtle turtle;
      private final double speedModifier;
      private boolean stuck;
      private int closeToHomeTryTicks;
      private static final int GIVE_UP_TICKS = 600;

      TurtleGoHomeGoal(Turtle p_481410_, double p_481154_) {
         this.turtle = p_481410_;
         this.speedModifier = p_481154_;
      }

      @Override
      public boolean canUse() {
         if (this.turtle.isBaby()) {
            return false;
         } else if (this.turtle.hasEgg()) {
            return true;
         } else {
            return this.turtle.getRandom().nextInt(reducedTickDelay(700)) != 0 ? false : !this.turtle.homePos.closerToCenterThan(this.turtle.position(), 64.0);
         }
      }

      @Override
      public void start() {
         this.turtle.goingHome = true;
         this.stuck = false;
         this.closeToHomeTryTicks = 0;
      }

      @Override
      public void stop() {
         this.turtle.goingHome = false;
      }

      @Override
      public boolean canContinueToUse() {
         return !this.turtle.homePos.closerToCenterThan(this.turtle.position(), 7.0) && !this.stuck && this.closeToHomeTryTicks <= this.adjustedTickDelay(600);
      }

      @Override
      public void tick() {
         BlockPos blockpos = this.turtle.homePos;
         boolean flag = blockpos.closerToCenterThan(this.turtle.position(), 16.0);
         if (flag) {
            this.closeToHomeTryTicks++;
         }

         if (this.turtle.getNavigation().isDone()) {
            Vec3 vec3 = Vec3.atBottomCenterOf(blockpos);
            Vec3 vec31 = DefaultRandomPos.getPosTowards(this.turtle, 16, 3, vec3, (float) (Math.PI / 10));
            if (vec31 == null) {
               vec31 = DefaultRandomPos.getPosTowards(this.turtle, 8, 7, vec3, (float) (Math.PI / 2));
            }

            if (vec31 != null && !flag && !this.turtle.level().getBlockState(BlockPos.containing(vec31)).is(Blocks.WATER)) {
               vec31 = DefaultRandomPos.getPosTowards(this.turtle, 16, 5, vec3, (float) (Math.PI / 2));
            }

            if (vec31 == null) {
               this.stuck = true;
               return;
            }

            this.turtle.getNavigation().moveTo(vec31.x, vec31.y, vec31.z, this.speedModifier);
         }
      }
   }

   static class TurtleGoToWaterGoal extends MoveToBlockGoal {
      private static final int GIVE_UP_TICKS = 1200;
      private final Turtle turtle;

      TurtleGoToWaterGoal(Turtle p_480409_, double p_479778_) {
         super(p_480409_, p_480409_.isBaby() ? 2.0 : p_479778_, 24);
         this.turtle = p_480409_;
         this.verticalSearchStart = -1;
      }

      @Override
      public boolean canContinueToUse() {
         return !this.turtle.isInWater() && this.tryTicks <= 1200 && this.isValidTarget(this.turtle.level(), this.blockPos);
      }

      @Override
      public boolean canUse() {
         if (this.turtle.isBaby() && !this.turtle.isInWater()) {
            return super.canUse();
         } else {
            return !this.turtle.goingHome && !this.turtle.isInWater() && !this.turtle.hasEgg() ? super.canUse() : false;
         }
      }

      @Override
      public boolean shouldRecalculatePath() {
         return this.tryTicks % 160 == 0;
      }

      @Override
      protected boolean isValidTarget(LevelReader p_478711_, BlockPos p_481331_) {
         return p_478711_.getBlockState(p_481331_).is(Blocks.WATER);
      }
   }

   static class TurtleLayEggGoal extends MoveToBlockGoal {
      private final Turtle turtle;

      TurtleLayEggGoal(Turtle p_480852_, double p_481758_) {
         super(p_480852_, p_481758_, 16);
         this.turtle = p_480852_;
      }

      @Override
      public boolean canUse() {
         return this.turtle.hasEgg() && this.turtle.homePos.closerToCenterThan(this.turtle.position(), 9.0) ? super.canUse() : false;
      }

      @Override
      public boolean canContinueToUse() {
         return super.canContinueToUse() && this.turtle.hasEgg() && this.turtle.homePos.closerToCenterThan(this.turtle.position(), 9.0);
      }

      @Override
      public void tick() {
         super.tick();
         BlockPos blockpos = this.turtle.blockPosition();
         if (!this.turtle.isInWater() && this.isReachedTarget()) {
            if (this.turtle.layEggCounter < 1) {
               this.turtle.setLayingEgg(true);
            } else if (this.turtle.layEggCounter > this.adjustedTickDelay(200)) {
               Level level = this.turtle.level();
               level.playSound(null, blockpos, SoundEvents.TURTLE_LAY_EGG, SoundSource.BLOCKS, 0.3F, 0.9F + level.random.nextFloat() * 0.2F);
               BlockPos blockpos1 = this.blockPos.above();
               BlockState blockstate = Blocks.TURTLE_EGG.defaultBlockState().setValue(TurtleEggBlock.EGGS, this.turtle.random.nextInt(4) + 1);
               level.setBlock(blockpos1, blockstate, 3);
               level.gameEvent(GameEvent.BLOCK_PLACE, blockpos1, GameEvent.Context.of(this.turtle, blockstate));
               this.turtle.setHasEgg(false);
               this.turtle.setLayingEgg(false);
               this.turtle.setInLoveTime(600);
            }

            if (this.turtle.isLayingEgg()) {
               this.turtle.layEggCounter++;
            }
         }
      }

      @Override
      protected boolean isValidTarget(LevelReader p_480022_, BlockPos p_480317_) {
         return !p_480022_.isEmptyBlock(p_480317_.above()) ? false : TurtleEggBlock.isSand(p_480022_, p_480317_);
      }
   }

   static class TurtleMoveControl extends MoveControl {
      private final Turtle turtle;

      TurtleMoveControl(Turtle p_478622_) {
         super(p_478622_);
         this.turtle = p_478622_;
      }

      private void updateSpeed() {
         if (this.turtle.isInWater()) {
            this.turtle.setDeltaMovement(this.turtle.getDeltaMovement().add(0.0, 0.005, 0.0));
            if (!this.turtle.homePos.closerToCenterThan(this.turtle.position(), 16.0)) {
               this.turtle.setSpeed(Math.max(this.turtle.getSpeed() / 2.0F, 0.08F));
            }

            if (this.turtle.isBaby()) {
               this.turtle.setSpeed(Math.max(this.turtle.getSpeed() / 3.0F, 0.06F));
            }
         } else if (this.turtle.onGround()) {
            this.turtle.setSpeed(Math.max(this.turtle.getSpeed() / 2.0F, 0.06F));
         }
      }

      @Override
      public void tick() {
         this.updateSpeed();
         if (this.operation == MoveControl.Operation.MOVE_TO && !this.turtle.getNavigation().isDone()) {
            double d0 = this.wantedX - this.turtle.getX();
            double d1 = this.wantedY - this.turtle.getY();
            double d2 = this.wantedZ - this.turtle.getZ();
            double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
            if (d3 < 1.0E-5F) {
               this.mob.setSpeed(0.0F);
            } else {
               d1 /= d3;
               float f = (float)(Mth.atan2(d2, d0) * 180.0 / (float) Math.PI) - 90.0F;
               this.turtle.setYRot(this.rotlerp(this.turtle.getYRot(), f, 90.0F));
               this.turtle.yBodyRot = this.turtle.getYRot();
               float f1 = (float)(this.speedModifier * this.turtle.getAttributeValue(Attributes.MOVEMENT_SPEED));
               this.turtle.setSpeed(Mth.lerp(0.125F, this.turtle.getSpeed(), f1));
               this.turtle.setDeltaMovement(this.turtle.getDeltaMovement().add(0.0, this.turtle.getSpeed() * d1 * 0.1, 0.0));
            }
         } else {
            this.turtle.setSpeed(0.0F);
         }
      }
   }

   static class TurtlePanicGoal extends PanicGoal {
      TurtlePanicGoal(Turtle p_481899_, double p_478767_) {
         super(p_481899_, p_478767_);
      }

      @Override
      public boolean canUse() {
         if (!this.shouldPanic()) {
            return false;
         } else {
            BlockPos blockpos = this.lookForWater(this.mob.level(), this.mob, 7);
            if (blockpos != null) {
               this.posX = blockpos.getX();
               this.posY = blockpos.getY();
               this.posZ = blockpos.getZ();
               return true;
            } else {
               return this.findRandomPosition();
            }
         }
      }
   }

   static class TurtlePathNavigation extends AmphibiousPathNavigation {
      TurtlePathNavigation(Turtle p_478984_, Level p_480744_) {
         super(p_478984_, p_480744_);
      }

      @Override
      public boolean isStableDestination(BlockPos p_480393_) {
         return this.mob instanceof Turtle turtle && turtle.travelPos != null
            ? this.level.getBlockState(p_480393_).is(Blocks.WATER)
            : !this.level.getBlockState(p_480393_.below()).isAir();
      }
   }

   static class TurtleRandomStrollGoal extends RandomStrollGoal {
      private final Turtle turtle;

      TurtleRandomStrollGoal(Turtle p_479039_, double p_478994_, int p_478347_) {
         super(p_479039_, p_478994_, p_478347_);
         this.turtle = p_479039_;
      }

      @Override
      public boolean canUse() {
         return !this.mob.isInWater() && !this.turtle.goingHome && !this.turtle.hasEgg() ? super.canUse() : false;
      }
   }

   static class TurtleTravelGoal extends Goal {
      private final Turtle turtle;
      private final double speedModifier;
      private boolean stuck;

      TurtleTravelGoal(Turtle p_478565_, double p_480298_) {
         this.turtle = p_478565_;
         this.speedModifier = p_480298_;
      }

      @Override
      public boolean canUse() {
         return !this.turtle.goingHome && !this.turtle.hasEgg() && this.turtle.isInWater();
      }

      @Override
      public void start() {
         int i = 512;
         int j = 4;
         RandomSource randomsource = this.turtle.random;
         int k = randomsource.nextInt(1025) - 512;
         int l = randomsource.nextInt(9) - 4;
         int i1 = randomsource.nextInt(1025) - 512;
         if (l + this.turtle.getY() > this.turtle.level().getSeaLevel() - 1) {
            l = 0;
         }

         this.turtle.travelPos = BlockPos.containing(k + this.turtle.getX(), l + this.turtle.getY(), i1 + this.turtle.getZ());
         this.stuck = false;
      }

      @Override
      public void tick() {
         if (this.turtle.travelPos == null) {
            this.stuck = true;
         } else if (this.turtle.getNavigation().isDone()) {
            Vec3 vec3 = Vec3.atBottomCenterOf(this.turtle.travelPos);
            Vec3 vec31 = DefaultRandomPos.getPosTowards(this.turtle, 16, 3, vec3, (float) (Math.PI / 10));
            if (vec31 == null) {
               vec31 = DefaultRandomPos.getPosTowards(this.turtle, 8, 7, vec3, (float) (Math.PI / 2));
            }

            if (vec31 != null) {
               int i = Mth.floor(vec31.x);
               int j = Mth.floor(vec31.z);
               int k = 34;
               if (!this.turtle.level().hasChunksAt(i - 34, j - 34, i + 34, j + 34)) {
                  vec31 = null;
               }
            }

            if (vec31 == null) {
               this.stuck = true;
               return;
            }

            this.turtle.getNavigation().moveTo(vec31.x, vec31.y, vec31.z, this.speedModifier);
         }
      }

      @Override
      public boolean canContinueToUse() {
         return !this.turtle.getNavigation().isDone() && !this.stuck && !this.turtle.goingHome && !this.turtle.isInLove() && !this.turtle.hasEgg();
      }

      @Override
      public void stop() {
         this.turtle.travelPos = null;
         super.stop();
      }
   }
}
