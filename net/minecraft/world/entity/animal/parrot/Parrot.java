package net.minecraft.world.entity.animal.parrot;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowMobGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LandOnOwnersShoulderGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.registries.datamaps.builtin.ParrotImitation;
import org.jspecify.annotations.Nullable;

public class Parrot extends ShoulderRidingEntity implements FlyingAnimal {
   private static final EntityDataAccessor<Integer> DATA_VARIANT_ID = SynchedEntityData.defineId(Parrot.class, EntityDataSerializers.INT);
   private static final Predicate<Mob> NOT_PARROT_PREDICATE = new Predicate<Mob>() {
      public boolean test(@Nullable Mob p_478094_) {
         return p_478094_ != null
            && (
               Parrot.MOB_SOUND_MAP.containsKey(p_478094_.getType())
                  || p_478094_.getType().builtInRegistryHolder().getData(NeoForgeDataMaps.PARROT_IMITATIONS) != null
            );
      }
   };
   @Deprecated
   static final Map<EntityType<?>, SoundEvent> MOB_SOUND_MAP = Util.make(Maps.newHashMap(), p_480096_ -> {
      p_480096_.put(EntityType.BLAZE, SoundEvents.PARROT_IMITATE_BLAZE);
      p_480096_.put(EntityType.BOGGED, SoundEvents.PARROT_IMITATE_BOGGED);
      p_480096_.put(EntityType.BREEZE, SoundEvents.PARROT_IMITATE_BREEZE);
      p_480096_.put(EntityType.CAMEL_HUSK, SoundEvents.PARROT_IMITATE_CAMEL_HUSK);
      p_480096_.put(EntityType.CAVE_SPIDER, SoundEvents.PARROT_IMITATE_SPIDER);
      p_480096_.put(EntityType.CREAKING, SoundEvents.PARROT_IMITATE_CREAKING);
      p_480096_.put(EntityType.CREEPER, SoundEvents.PARROT_IMITATE_CREEPER);
      p_480096_.put(EntityType.DROWNED, SoundEvents.PARROT_IMITATE_DROWNED);
      p_480096_.put(EntityType.ELDER_GUARDIAN, SoundEvents.PARROT_IMITATE_ELDER_GUARDIAN);
      p_480096_.put(EntityType.ENDER_DRAGON, SoundEvents.PARROT_IMITATE_ENDER_DRAGON);
      p_480096_.put(EntityType.ENDERMITE, SoundEvents.PARROT_IMITATE_ENDERMITE);
      p_480096_.put(EntityType.EVOKER, SoundEvents.PARROT_IMITATE_EVOKER);
      p_480096_.put(EntityType.GHAST, SoundEvents.PARROT_IMITATE_GHAST);
      p_480096_.put(EntityType.HAPPY_GHAST, SoundEvents.EMPTY);
      p_480096_.put(EntityType.GUARDIAN, SoundEvents.PARROT_IMITATE_GUARDIAN);
      p_480096_.put(EntityType.HOGLIN, SoundEvents.PARROT_IMITATE_HOGLIN);
      p_480096_.put(EntityType.HUSK, SoundEvents.PARROT_IMITATE_HUSK);
      p_480096_.put(EntityType.ILLUSIONER, SoundEvents.PARROT_IMITATE_ILLUSIONER);
      p_480096_.put(EntityType.MAGMA_CUBE, SoundEvents.PARROT_IMITATE_MAGMA_CUBE);
      p_480096_.put(EntityType.PARCHED, SoundEvents.PARROT_IMITATE_PARCHED);
      p_480096_.put(EntityType.PHANTOM, SoundEvents.PARROT_IMITATE_PHANTOM);
      p_480096_.put(EntityType.PIGLIN, SoundEvents.PARROT_IMITATE_PIGLIN);
      p_480096_.put(EntityType.PIGLIN_BRUTE, SoundEvents.PARROT_IMITATE_PIGLIN_BRUTE);
      p_480096_.put(EntityType.PILLAGER, SoundEvents.PARROT_IMITATE_PILLAGER);
      p_480096_.put(EntityType.RAVAGER, SoundEvents.PARROT_IMITATE_RAVAGER);
      p_480096_.put(EntityType.SHULKER, SoundEvents.PARROT_IMITATE_SHULKER);
      p_480096_.put(EntityType.SILVERFISH, SoundEvents.PARROT_IMITATE_SILVERFISH);
      p_480096_.put(EntityType.SKELETON, SoundEvents.PARROT_IMITATE_SKELETON);
      p_480096_.put(EntityType.SLIME, SoundEvents.PARROT_IMITATE_SLIME);
      p_480096_.put(EntityType.SPIDER, SoundEvents.PARROT_IMITATE_SPIDER);
      p_480096_.put(EntityType.STRAY, SoundEvents.PARROT_IMITATE_STRAY);
      p_480096_.put(EntityType.VEX, SoundEvents.PARROT_IMITATE_VEX);
      p_480096_.put(EntityType.VINDICATOR, SoundEvents.PARROT_IMITATE_VINDICATOR);
      p_480096_.put(EntityType.WARDEN, SoundEvents.PARROT_IMITATE_WARDEN);
      p_480096_.put(EntityType.WITCH, SoundEvents.PARROT_IMITATE_WITCH);
      p_480096_.put(EntityType.WITHER, SoundEvents.PARROT_IMITATE_WITHER);
      p_480096_.put(EntityType.WITHER_SKELETON, SoundEvents.PARROT_IMITATE_WITHER_SKELETON);
      p_480096_.put(EntityType.ZOGLIN, SoundEvents.PARROT_IMITATE_ZOGLIN);
      p_480096_.put(EntityType.ZOMBIE, SoundEvents.PARROT_IMITATE_ZOMBIE);
      p_480096_.put(EntityType.ZOMBIE_HORSE, SoundEvents.PARROT_IMITATE_ZOMBIE_HORSE);
      p_480096_.put(EntityType.ZOMBIE_NAUTILUS, SoundEvents.PARROT_IMITATE_ZOMBIE_NAUTILUS);
      p_480096_.put(EntityType.ZOMBIE_VILLAGER, SoundEvents.PARROT_IMITATE_ZOMBIE_VILLAGER);
   });
   public float flap;
   public float flapSpeed;
   public float oFlapSpeed;
   public float oFlap;
   private float flapping = 1.0F;
   private float nextFlap = 1.0F;
   private boolean partyParrot;
   @Nullable
   private BlockPos jukebox;

   public Parrot(EntityType<? extends Parrot> p_478196_, Level p_481628_) {
      super(p_478196_, p_481628_);
      this.moveControl = new FlyingMoveControl(this, 10, false);
      this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
      this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
      this.setPathfindingMalus(PathType.COCOA, -1.0F);
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_480706_, DifficultyInstance p_481783_, EntitySpawnReason p_479894_, @Nullable SpawnGroupData p_480556_
   ) {
      this.setVariant(Util.getRandom(Parrot.Variant.values(), p_480706_.getRandom()));
      if (p_480556_ == null) {
         p_480556_ = new AgeableMob.AgeableMobGroupData(false);
      }

      return super.finalizeSpawn(p_480706_, p_481783_, p_479894_, p_480556_);
   }

   @Override
   public boolean isBaby() {
      return false;
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new TamableAnimal.TamableAnimalPanicGoal(1.25));
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
      this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0, 5.0F, 1.0F));
      this.goalSelector.addGoal(2, new Parrot.ParrotWanderGoal(this, 1.0));
      this.goalSelector.addGoal(3, new LandOnOwnersShoulderGoal(this));
      this.goalSelector.addGoal(3, new FollowMobGoal(this, 1.0, 3.0F, 7.0F));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes()
         .add(Attributes.MAX_HEALTH, 6.0)
         .add(Attributes.FLYING_SPEED, 0.4F)
         .add(Attributes.MOVEMENT_SPEED, 0.2F)
         .add(Attributes.ATTACK_DAMAGE, 3.0);
   }

   @Override
   protected PathNavigation createNavigation(Level p_479270_) {
      FlyingPathNavigation flyingpathnavigation = new FlyingPathNavigation(this, p_479270_);
      flyingpathnavigation.setCanOpenDoors(false);
      flyingpathnavigation.setCanFloat(true);
      return flyingpathnavigation;
   }

   @Override
   public void aiStep() {
      if (this.jukebox == null || !this.jukebox.closerToCenterThan(this.position(), 3.46) || !this.level().getBlockState(this.jukebox).is(Blocks.JUKEBOX)) {
         this.partyParrot = false;
         this.jukebox = null;
      }

      if (this.level().random.nextInt(400) == 0) {
         imitateNearbyMobs(this.level(), this);
      }

      super.aiStep();
      this.calculateFlapping();
   }

   @Override
   public void setRecordPlayingNearby(BlockPos p_481562_, boolean p_480032_) {
      this.jukebox = p_481562_;
      this.partyParrot = p_480032_;
   }

   public boolean isPartyParrot() {
      return this.partyParrot;
   }

   private void calculateFlapping() {
      this.oFlap = this.flap;
      this.oFlapSpeed = this.flapSpeed;
      this.flapSpeed = this.flapSpeed + (!this.onGround() && !this.isPassenger() ? 4 : -1) * 0.3F;
      this.flapSpeed = Mth.clamp(this.flapSpeed, 0.0F, 1.0F);
      if (!this.onGround() && this.flapping < 1.0F) {
         this.flapping = 1.0F;
      }

      this.flapping *= 0.9F;
      Vec3 vec3 = this.getDeltaMovement();
      if (!this.onGround() && vec3.y < 0.0) {
         this.setDeltaMovement(vec3.multiply(1.0, 0.6, 1.0));
      }

      this.flap = this.flap + this.flapping * 2.0F;
   }

   public static boolean imitateNearbyMobs(Level p_478231_, Entity p_479555_) {
      if (p_479555_.isAlive() && !p_479555_.isSilent() && p_478231_.random.nextInt(2) == 0) {
         List<Mob> list = p_478231_.getEntitiesOfClass(Mob.class, p_479555_.getBoundingBox().inflate(20.0), NOT_PARROT_PREDICATE);
         if (!list.isEmpty()) {
            Mob mob = list.get(p_478231_.random.nextInt(list.size()));
            if (!mob.isSilent()) {
               SoundEvent soundevent = getImitatedSound(mob.getType());
               p_478231_.playSound(
                  null, p_479555_.getX(), p_479555_.getY(), p_479555_.getZ(), soundevent, p_479555_.getSoundSource(), 0.7F, getPitch(p_478231_.random)
               );
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   @Override
   public InteractionResult mobInteract(Player p_481441_, InteractionHand p_480148_) {
      ItemStack itemstack = p_481441_.getItemInHand(p_480148_);
      if (!this.isTame() && itemstack.is(ItemTags.PARROT_FOOD)) {
         this.usePlayerItem(p_481441_, p_480148_, itemstack);
         if (!this.isSilent()) {
            this.level()
               .playSound(
                  null,
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  SoundEvents.PARROT_EAT,
                  this.getSoundSource(),
                  1.0F,
                  1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F
               );
         }

         if (!this.level().isClientSide()) {
            if (this.random.nextInt(10) == 0 && !EventHooks.onAnimalTame(this, p_481441_)) {
               this.tame(p_481441_);
               this.level().broadcastEntityEvent(this, (byte)7);
            } else {
               this.level().broadcastEntityEvent(this, (byte)6);
            }
         }

         return InteractionResult.SUCCESS;
      } else if (!itemstack.is(ItemTags.PARROT_POISONOUS_FOOD)) {
         if (!this.isFlying() && this.isTame() && this.isOwnedBy(p_481441_)) {
            if (!this.level().isClientSide()) {
               this.setOrderedToSit(!this.isOrderedToSit());
            }

            return InteractionResult.SUCCESS;
         } else {
            return super.mobInteract(p_481441_, p_480148_);
         }
      } else {
         this.usePlayerItem(p_481441_, p_480148_, itemstack);
         this.addEffect(new MobEffectInstance(MobEffects.POISON, 900));
         if (p_481441_.isCreative() || !this.isInvulnerable()) {
            this.hurt(this.damageSources().playerAttack(p_481441_), Float.MAX_VALUE);
         }

         return InteractionResult.SUCCESS;
      }
   }

   @Override
   public boolean isFood(ItemStack p_481303_) {
      return false;
   }

   public static boolean checkParrotSpawnRules(
      EntityType<Parrot> p_478645_, LevelAccessor p_481185_, EntitySpawnReason p_477949_, BlockPos p_482014_, RandomSource p_478370_
   ) {
      return p_481185_.getBlockState(p_482014_.below()).is(BlockTags.PARROTS_SPAWNABLE_ON) && isBrightEnoughToSpawn(p_481185_, p_482014_);
   }

   @Override
   protected void checkFallDamage(double p_480007_, boolean p_481100_, BlockState p_481336_, BlockPos p_480586_) {
   }

   @Override
   public boolean canMate(Animal p_480047_) {
      return false;
   }

   @Nullable
   @Override
   public AgeableMob getBreedOffspring(ServerLevel p_480790_, AgeableMob p_478481_) {
      return null;
   }

   @Nullable
   @Override
   public SoundEvent getAmbientSound() {
      return getAmbient(this.level(), this.level().random);
   }

   public static SoundEvent getAmbient(Level p_478607_, RandomSource p_481595_) {
      if (p_478607_.getDifficulty() != Difficulty.PEACEFUL && p_481595_.nextInt(1000) == 0) {
         HashSet<EntityType<?>> entities = new HashSet<>(MOB_SOUND_MAP.keySet());
         Registry<EntityType<?>> registry = p_478607_.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
         registry.getDataMap(NeoForgeDataMaps.PARROT_IMITATIONS).keySet().forEach(key -> entities.add(registry.getValue((ResourceKey<EntityType<?>>)key)));
         List<EntityType<?>> list = Lists.newArrayList(entities);
         return getImitatedSound(list.get(p_481595_.nextInt(list.size())));
      } else {
         return SoundEvents.PARROT_AMBIENT;
      }
   }

   private static SoundEvent getImitatedSound(EntityType<?> p_480374_) {
      ParrotImitation imitation = p_480374_.builtInRegistryHolder().getData(NeoForgeDataMaps.PARROT_IMITATIONS);
      return imitation != null ? imitation.sound() : SoundEvents.PARROT_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_481775_) {
      return SoundEvents.PARROT_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.PARROT_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos p_479707_, BlockState p_479443_) {
      this.playSound(SoundEvents.PARROT_STEP, 0.15F, 1.0F);
   }

   @Override
   protected boolean isFlapping() {
      return this.flyDist > this.nextFlap;
   }

   @Override
   protected void onFlap() {
      this.playSound(SoundEvents.PARROT_FLY, 0.15F, 1.0F);
      this.nextFlap = this.flyDist + this.flapSpeed / 2.0F;
   }

   @Override
   public float getVoicePitch() {
      return getPitch(this.random);
   }

   public static float getPitch(RandomSource p_481106_) {
      return (p_481106_.nextFloat() - p_481106_.nextFloat()) * 0.2F + 1.0F;
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.NEUTRAL;
   }

   @Override
   public boolean isPushable() {
      return true;
   }

   @Override
   protected void doPush(Entity p_480691_) {
      if (!(p_480691_ instanceof Player)) {
         super.doPush(p_480691_);
      }
   }

   @Override
   public boolean hurtServer(ServerLevel p_478766_, DamageSource p_478034_, float p_478531_) {
      if (this.isInvulnerableTo(p_478766_, p_478034_)) {
         return false;
      } else {
         this.setOrderedToSit(false);
         return super.hurtServer(p_478766_, p_478034_, p_478531_);
      }
   }

   public Parrot.Variant getVariant() {
      return Parrot.Variant.byId(this.entityData.get(DATA_VARIANT_ID));
   }

   private void setVariant(Parrot.Variant p_478520_) {
      this.entityData.set(DATA_VARIANT_ID, p_478520_.id);
   }

   @Nullable
   @Override
   public <T> T get(DataComponentType<? extends T> p_478123_) {
      return p_478123_ == DataComponents.PARROT_VARIANT ? castComponentValue((DataComponentType<T>)p_478123_, this.getVariant()) : super.get(p_478123_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_481626_) {
      this.applyImplicitComponentIfPresent(p_481626_, DataComponents.PARROT_VARIANT);
      super.applyImplicitComponents(p_481626_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_479590_, T p_481550_) {
      if (p_479590_ == DataComponents.PARROT_VARIANT) {
         this.setVariant(castComponentValue(DataComponents.PARROT_VARIANT, p_481550_));
         return true;
      } else {
         return super.applyImplicitComponent(p_479590_, p_481550_);
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_478252_) {
      super.defineSynchedData(p_478252_);
      p_478252_.define(DATA_VARIANT_ID, Parrot.Variant.DEFAULT.id);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_480745_) {
      super.addAdditionalSaveData(p_480745_);
      p_480745_.store("Variant", Parrot.Variant.LEGACY_CODEC, this.getVariant());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_478571_) {
      super.readAdditionalSaveData(p_478571_);
      this.setVariant(p_478571_.<Parrot.Variant>read("Variant", Parrot.Variant.LEGACY_CODEC).orElse(Parrot.Variant.DEFAULT));
   }

   @Override
   public boolean isFlying() {
      return !this.onGround();
   }

   @Override
   protected boolean canFlyToOwner() {
      return true;
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0, 0.5F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
   }

   static class ParrotWanderGoal extends WaterAvoidingRandomFlyingGoal {
      public ParrotWanderGoal(PathfinderMob p_478188_, double p_481787_) {
         super(p_478188_, p_481787_);
      }

      @Nullable
      @Override
      protected Vec3 getPosition() {
         Vec3 vec3 = null;
         if (this.mob.isInWater()) {
            vec3 = LandRandomPos.getPos(this.mob, 15, 15);
         }

         if (this.mob.getRandom().nextFloat() >= this.probability) {
            vec3 = this.getTreePos();
         }

         return vec3 == null ? super.getPosition() : vec3;
      }

      @Nullable
      private Vec3 getTreePos() {
         BlockPos blockpos = this.mob.blockPosition();
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
         BlockPos.MutableBlockPos blockpos$mutableblockpos1 = new BlockPos.MutableBlockPos();

         for (BlockPos blockpos1 : BlockPos.betweenClosed(
            Mth.floor(this.mob.getX() - 3.0),
            Mth.floor(this.mob.getY() - 6.0),
            Mth.floor(this.mob.getZ() - 3.0),
            Mth.floor(this.mob.getX() + 3.0),
            Mth.floor(this.mob.getY() + 6.0),
            Mth.floor(this.mob.getZ() + 3.0)
         )) {
            if (!blockpos.equals(blockpos1)) {
               BlockState blockstate = this.mob.level().getBlockState(blockpos$mutableblockpos1.setWithOffset(blockpos1, Direction.DOWN));
               boolean flag = blockstate.getBlock() instanceof LeavesBlock || blockstate.is(BlockTags.LOGS);
               if (flag
                  && this.mob.level().isEmptyBlock(blockpos1)
                  && this.mob.level().isEmptyBlock(blockpos$mutableblockpos.setWithOffset(blockpos1, Direction.UP))) {
                  return Vec3.atBottomCenterOf(blockpos1);
               }
            }
         }

         return null;
      }
   }

   public static enum Variant implements StringRepresentable {
      RED_BLUE(0, "red_blue"),
      BLUE(1, "blue"),
      GREEN(2, "green"),
      YELLOW_BLUE(3, "yellow_blue"),
      GRAY(4, "gray");

      public static final Parrot.Variant DEFAULT = RED_BLUE;
      private static final IntFunction<Parrot.Variant> BY_ID = ByIdMap.continuous(Parrot.Variant::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
      public static final Codec<Parrot.Variant> CODEC = StringRepresentable.fromEnum(Parrot.Variant::values);
      @Deprecated
      public static final Codec<Parrot.Variant> LEGACY_CODEC = Codec.INT.xmap(BY_ID::apply, Parrot.Variant::getId);
      public static final StreamCodec<ByteBuf, Parrot.Variant> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Parrot.Variant::getId);
      final int id;
      private final String name;

      private Variant(int p_480712_, String p_479623_) {
         this.id = p_480712_;
         this.name = p_479623_;
      }

      public int getId() {
         return this.id;
      }

      public static Parrot.Variant byId(int p_481036_) {
         return BY_ID.apply(p_481036_);
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
