package net.minecraft.world.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JavaOps;
import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class LivingEntity extends Entity implements Attackable, WaypointTransmitter {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String TAG_ACTIVE_EFFECTS = "active_effects";
   public static final String TAG_ATTRIBUTES = "attributes";
   public static final String TAG_SLEEPING_POS = "sleeping_pos";
   public static final String TAG_EQUIPMENT = "equipment";
   public static final String TAG_BRAIN = "Brain";
   public static final String TAG_FALL_FLYING = "FallFlying";
   public static final String TAG_HURT_TIME = "HurtTime";
   public static final String TAG_DEATH_TIME = "DeathTime";
   public static final String TAG_HURT_BY_TIMESTAMP = "HurtByTimestamp";
   public static final String TAG_HEALTH = "Health";
   private static final Identifier SPEED_MODIFIER_POWDER_SNOW_ID = Identifier.withDefaultNamespace("powder_snow");
   private static final Identifier SPRINTING_MODIFIER_ID = Identifier.withDefaultNamespace("sprinting");
   private static final AttributeModifier SPEED_MODIFIER_SPRINTING = new AttributeModifier(
      SPRINTING_MODIFIER_ID, 0.3F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
   );
   public static final int EQUIPMENT_SLOT_OFFSET = 98;
   public static final int ARMOR_SLOT_OFFSET = 100;
   public static final int BODY_ARMOR_OFFSET = 105;
   public static final int SADDLE_OFFSET = 106;
   public static final int PLAYER_HURT_EXPERIENCE_TIME = 100;
   private static final int DAMAGE_SOURCE_TIMEOUT = 40;
   public static final double MIN_MOVEMENT_DISTANCE = 0.003;
   public static final double DEFAULT_BASE_GRAVITY = 0.08;
   public static final int DEATH_DURATION = 20;
   protected static final float INPUT_FRICTION = 0.98F;
   private static final int TICKS_PER_ELYTRA_FREE_FALL_EVENT = 10;
   private static final int FREE_FALL_EVENTS_PER_ELYTRA_BREAK = 2;
   public static final float BASE_JUMP_POWER = 0.42F;
   protected static final float DEFAULT_KNOCKBACK = 0.4F;
   protected static final int INVULNERABLE_DURATION = 20;
   private static final double MAX_LINE_OF_SIGHT_TEST_RANGE = 128.0;
   protected static final int LIVING_ENTITY_FLAG_IS_USING = 1;
   protected static final int LIVING_ENTITY_FLAG_OFF_HAND = 2;
   protected static final int LIVING_ENTITY_FLAG_SPIN_ATTACK = 4;
   protected static final EntityDataAccessor<Byte> DATA_LIVING_ENTITY_FLAGS = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Float> DATA_HEALTH_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<List<ParticleOptions>> DATA_EFFECT_PARTICLES = SynchedEntityData.defineId(
      LivingEntity.class, EntityDataSerializers.PARTICLES
   );
   private static final EntityDataAccessor<Boolean> DATA_EFFECT_AMBIENCE_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_ARROW_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_STINGER_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Optional<BlockPos>> SLEEPING_POS_ID = SynchedEntityData.defineId(
      LivingEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS
   );
   private static final int PARTICLE_FREQUENCY_WHEN_INVISIBLE = 15;
   protected static final EntityDimensions SLEEPING_DIMENSIONS = EntityDimensions.fixed(0.2F, 0.2F).withEyeHeight(0.2F);
   public static final float EXTRA_RENDER_CULLING_SIZE_WITH_BIG_HAT = 0.5F;
   public static final float DEFAULT_BABY_SCALE = 0.5F;
   private static final float WATER_FLOAT_IMPULSE = 0.04F;
   public static final Predicate<LivingEntity> PLAYER_NOT_WEARING_DISGUISE_ITEM = livingEntity -> {
      if (livingEntity instanceof Player player) {
         ItemStack itemStack = player.getItemBySlot(EquipmentSlot.HEAD);
         return !itemStack.is(ItemTags.GAZE_DISGUISE_EQUIPMENT);
      } else {
         return true;
      }
   };
   private static final Dynamic<?> EMPTY_BRAIN = new Dynamic(JavaOps.INSTANCE, Map.of("memories", Map.of()));
   private final AttributeMap attributes;
   private final CombatTracker combatTracker = new CombatTracker(this);
   private final Map<Holder<MobEffect>, MobEffectInstance> activeEffects = Maps.newHashMap();
   private final Map<EquipmentSlot, ItemStack> lastEquipmentItems = Util.makeEnumMap(EquipmentSlot.class, equipmentSlot -> ItemStack.EMPTY);
   public boolean swinging;
   private boolean discardFriction = false;
   public InteractionHand swingingArm;
   public int swingTime;
   public int removeArrowTime;
   public int removeStingerTime;
   public int hurtTime;
   public int hurtDuration;
   public int deathTime;
   public float oAttackAnim;
   public float attackAnim;
   protected int attackStrengthTicker;
   protected int itemSwapTicker;
   public final WalkAnimationState walkAnimation = new WalkAnimationState();
   public float yBodyRot;
   public float yBodyRotO;
   public float yHeadRot;
   public float yHeadRotO;
   public final ElytraAnimationState elytraAnimationState = new ElytraAnimationState(this);
   @Nullable
   protected EntityReference<Player> lastHurtByPlayer;
   protected int lastHurtByPlayerMemoryTime;
   protected boolean dead;
   protected int noActionTime;
   protected float lastHurt;
   protected boolean jumping;
   public float xxa;
   public float yya;
   public float zza;
   protected InterpolationHandler interpolation = new InterpolationHandler(this);
   protected double lerpYHeadRot;
   protected int lerpHeadSteps;
   private boolean effectsDirty = true;
   @Nullable
   private EntityReference<LivingEntity> lastHurtByMob;
   private int lastHurtByMobTimestamp;
   @Nullable
   private LivingEntity lastHurtMob;
   private int lastHurtMobTimestamp;
   private float speed;
   private int noJumpDelay;
   private float absorptionAmount;
   protected ItemStack useItem = ItemStack.EMPTY;
   protected int useItemRemaining;
   protected int fallFlyTicks;
   private long lastKineticHitFeedbackTime = -2147483648L;
   private BlockPos lastPos;
   private Optional<BlockPos> lastClimbablePos = Optional.empty();
   @Nullable
   private DamageSource lastDamageSource;
   private long lastDamageStamp;
   protected int autoSpinAttackTicks;
   protected float autoSpinAttackDmg;
   @Nullable
   protected ItemStack autoSpinAttackItemStack;
   @Nullable
   protected Object2LongMap<Entity> recentKineticEnemies;
   private float swimAmount;
   private float swimAmountO;
   protected Brain<?> brain;
   private boolean skipDropExperience;
   private final EnumMap<EquipmentSlot, Reference2ObjectMap<Enchantment, Set<EnchantmentLocationBasedEffect>>> activeLocationDependentEnchantments = new EnumMap<>(
      EquipmentSlot.class
   );
   protected final EntityEquipment equipment;
   private Waypoint.Icon locatorBarIcon = new Waypoint.Icon();

   protected LivingEntity(EntityType<? extends LivingEntity> entityType, Level level) {
      super(entityType, level);
      this.attributes = new AttributeMap(DefaultAttributes.getSupplier(entityType));
      this.setHealth(this.getMaxHealth());
      this.equipment = this.createEquipment();
      this.blocksBuilding = true;
      this.reapplyPosition();
      this.setYRot(this.random.nextFloat() * (float) (Math.PI * 2));
      this.yHeadRot = this.getYRot();
      this.brain = this.makeBrain(EMPTY_BRAIN);
   }

   @Nullable
   @Override
   public LivingEntity asLivingEntity() {
      return this;
   }

   @Contract(
      pure = true
   )
   protected EntityEquipment createEquipment() {
      return new EntityEquipment();
   }

   public Brain<?> getBrain() {
      return this.brain;
   }

   protected Brain.Provider<?> brainProvider() {
      return Brain.provider(ImmutableList.of(), ImmutableList.of());
   }

   protected Brain<?> makeBrain(Dynamic<?> dynamic) {
      return this.brainProvider().makeBrain(dynamic);
   }

   @Override
   public void kill(ServerLevel level) {
      this.hurtServer(level, this.damageSources().genericKill(), Float.MAX_VALUE);
   }

   public boolean canAttackType(EntityType<?> entityType) {
      return true;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(DATA_LIVING_ENTITY_FLAGS, (byte)0);
      builder.define(DATA_EFFECT_PARTICLES, List.of());
      builder.define(DATA_EFFECT_AMBIENCE_ID, false);
      builder.define(DATA_ARROW_COUNT_ID, 0);
      builder.define(DATA_STINGER_COUNT_ID, 0);
      builder.define(DATA_HEALTH_ID, 1.0F);
      builder.define(SLEEPING_POS_ID, Optional.empty());
   }

   public static AttributeSupplier.Builder createLivingAttributes() {
      return AttributeSupplier.builder()
         .add(Attributes.MAX_HEALTH)
         .add(Attributes.KNOCKBACK_RESISTANCE)
         .add(Attributes.MOVEMENT_SPEED)
         .add(Attributes.ARMOR)
         .add(Attributes.ARMOR_TOUGHNESS)
         .add(Attributes.MAX_ABSORPTION)
         .add(Attributes.STEP_HEIGHT)
         .add(Attributes.SCALE)
         .add(Attributes.GRAVITY)
         .add(Attributes.SAFE_FALL_DISTANCE)
         .add(Attributes.FALL_DAMAGE_MULTIPLIER)
         .add(Attributes.JUMP_STRENGTH)
         .add(Attributes.OXYGEN_BONUS)
         .add(Attributes.BURNING_TIME)
         .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)
         .add(Attributes.WATER_MOVEMENT_EFFICIENCY)
         .add(Attributes.MOVEMENT_EFFICIENCY)
         .add(Attributes.ATTACK_KNOCKBACK)
         .add(Attributes.CAMERA_DISTANCE)
         .add(Attributes.WAYPOINT_TRANSMIT_RANGE);
   }

   @Override
   protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
      if (!this.isInWater()) {
         this.updateInWaterStateAndDoWaterCurrentPushing();
      }

      if (this.level() instanceof ServerLevel serverLevel && onGround && this.fallDistance > 0.0) {
         this.onChangedBlock(serverLevel, pos);
         double d = Math.max(0, Mth.floor(this.calculateFallPower(this.fallDistance)));
         if (d > 0.0 && !state.isAir()) {
            double e = this.getX();
            double f = this.getY();
            double g = this.getZ();
            BlockPos blockPos = this.blockPosition();
            if (pos.getX() != blockPos.getX() || pos.getZ() != blockPos.getZ()) {
               double h = e - pos.getX() - 0.5;
               double i = g - pos.getZ() - 0.5;
               double j = Math.max(Math.abs(h), Math.abs(i));
               e = pos.getX() + 0.5 + h / j * 0.5;
               g = pos.getZ() + 0.5 + i / j * 0.5;
            }

            double h = Math.min(0.2F + d / 15.0, 2.5);
            int k = (int)(150.0 * h);
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), e, f, g, k, 0.0, 0.0, 0.0, 0.15F);
         }
      }

      super.checkFallDamage(y, onGround, state, pos);
      if (onGround) {
         this.lastClimbablePos = Optional.empty();
      }
   }

   public boolean canBreatheUnderwater() {
      return this.getType().is(EntityTypeTags.CAN_BREATHE_UNDER_WATER);
   }

   public float getSwimAmount(float partialTicks) {
      return Mth.lerp(partialTicks, this.swimAmountO, this.swimAmount);
   }

   public boolean hasLandedInLiquid() {
      return this.getDeltaMovement().y() < 1.0E-5F && this.isInLiquid();
   }

   @Override
   public void baseTick() {
      this.oAttackAnim = this.attackAnim;
      if (this.firstTick) {
         this.getSleepingPos().ifPresent(this::setPosToBed);
      }

      if (this.level() instanceof ServerLevel serverLevel) {
         EnchantmentHelper.tickEffects(serverLevel, this);
      }

      super.baseTick();
      ProfilerFiller profilerFiller = Profiler.get();
      profilerFiller.push("livingEntityBaseTick");
      if (this.isAlive() && this.level() instanceof ServerLevel serverLevel2) {
         boolean bl = this instanceof Player;
         if (this.isInWall()) {
            this.hurtServer(serverLevel2, this.damageSources().inWall(), 1.0F);
         } else if (bl && !serverLevel2.getWorldBorder().isWithinBounds(this.getBoundingBox())) {
            double d = serverLevel2.getWorldBorder().getDistanceToBorder(this) + serverLevel2.getWorldBorder().getSafeZone();
            if (d < 0.0) {
               double e = serverLevel2.getWorldBorder().getDamagePerBlock();
               if (e > 0.0) {
                  this.hurtServer(serverLevel2, this.damageSources().outOfBorder(), Math.max(1, Mth.floor(-d * e)));
               }
            }
         }

         if (this.isEyeInFluid(FluidTags.WATER)
            && !serverLevel2.getBlockState(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ())).is(Blocks.BUBBLE_COLUMN)) {
            boolean bl2 = !this.canBreatheUnderwater() && !MobEffectUtil.hasWaterBreathing(this) && (!bl || !((Player)this).getAbilities().invulnerable);
            if (bl2) {
               this.setAirSupply(this.decreaseAirSupply(this.getAirSupply()));
               if (this.shouldTakeDrowningDamage()) {
                  this.setAirSupply(0);
                  serverLevel2.broadcastEntityEvent(this, (byte)67);
                  this.hurtServer(serverLevel2, this.damageSources().drown(), 2.0F);
               }
            } else if (this.getAirSupply() < this.getMaxAirSupply() && MobEffectUtil.shouldEffectsRefillAirsupply(this)) {
               this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
            }

            if (this.isPassenger() && this.getVehicle() != null && this.getVehicle().dismountsUnderwater()) {
               this.stopRiding();
            }
         } else if (this.getAirSupply() < this.getMaxAirSupply()) {
            this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
         }

         BlockPos blockPos = this.blockPosition();
         if (!Objects.equal(this.lastPos, blockPos)) {
            this.lastPos = blockPos;
            this.onChangedBlock(serverLevel2, blockPos);
         }
      }

      if (this.hurtTime > 0) {
         this.hurtTime--;
      }

      if (this.invulnerableTime > 0 && !(this instanceof ServerPlayer)) {
         this.invulnerableTime--;
      }

      if (this.isDeadOrDying() && this.level().shouldTickDeath(this)) {
         this.tickDeath();
      }

      if (this.lastHurtByPlayerMemoryTime > 0) {
         this.lastHurtByPlayerMemoryTime--;
      } else {
         this.lastHurtByPlayer = null;
      }

      if (this.lastHurtMob != null && !this.lastHurtMob.isAlive()) {
         this.lastHurtMob = null;
      }

      LivingEntity livingEntity = this.getLastHurtByMob();
      if (livingEntity != null) {
         if (!livingEntity.isAlive()) {
            this.setLastHurtByMob(null);
         } else if (this.tickCount - this.lastHurtByMobTimestamp > 100) {
            this.setLastHurtByMob(null);
         }
      }

      this.tickEffects();
      this.yHeadRotO = this.yHeadRot;
      this.yBodyRotO = this.yBodyRot;
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
      profilerFiller.pop();
   }

   protected boolean shouldTakeDrowningDamage() {
      return this.getAirSupply() <= -20;
   }

   @Override
   protected float getBlockSpeedFactor() {
      return Mth.lerp((float)this.getAttributeValue(Attributes.MOVEMENT_EFFICIENCY), super.getBlockSpeedFactor(), 1.0F);
   }

   public float getLuck() {
      return 0.0F;
   }

   protected void removeFrost() {
      AttributeInstance attributeInstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
      if (attributeInstance != null) {
         if (attributeInstance.getModifier(SPEED_MODIFIER_POWDER_SNOW_ID) != null) {
            attributeInstance.removeModifier(SPEED_MODIFIER_POWDER_SNOW_ID);
         }
      }
   }

   protected void tryAddFrost() {
      if (!this.getBlockStateOnLegacy().isAir()) {
         int i = this.getTicksFrozen();
         if (i > 0) {
            AttributeInstance attributeInstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attributeInstance == null) {
               return;
            }

            float f = -0.05F * this.getPercentFrozen();
            attributeInstance.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_POWDER_SNOW_ID, f, AttributeModifier.Operation.ADD_VALUE));
         }
      }
   }

   protected void onChangedBlock(ServerLevel level, BlockPos pos) {
      EnchantmentHelper.runLocationChangedEffects(level, this);
   }

   public boolean isBaby() {
      return false;
   }

   public float getAgeScale() {
      return this.isBaby() ? 0.5F : 1.0F;
   }

   public final float getScale() {
      AttributeMap attributeMap = this.getAttributes();
      return attributeMap == null ? 1.0F : this.sanitizeScale((float)attributeMap.getValue(Attributes.SCALE));
   }

   protected float sanitizeScale(float scale) {
      return scale;
   }

   public boolean isAffectedByFluids() {
      return true;
   }

   protected void tickDeath() {
      this.deathTime++;
      if (this.deathTime >= 20 && !this.level().isClientSide() && !this.isRemoved()) {
         this.level().broadcastEntityEvent(this, (byte)60);
         this.remove(Entity.RemovalReason.KILLED);
      }
   }

   public boolean shouldDropExperience() {
      return !this.isBaby();
   }

   protected boolean shouldDropLoot(ServerLevel level) {
      return !this.isBaby() && level.getGameRules().get(GameRules.MOB_DROPS);
   }

   protected int decreaseAirSupply(int currentAir) {
      AttributeInstance attributeInstance = this.getAttribute(Attributes.OXYGEN_BONUS);
      double d;
      if (attributeInstance != null) {
         d = attributeInstance.getValue();
      } else {
         d = 0.0;
      }

      return d > 0.0 && this.random.nextDouble() >= 1.0 / (d + 1.0) ? currentAir : currentAir - 1;
   }

   protected int increaseAirSupply(int currentAir) {
      return Math.min(currentAir + 4, this.getMaxAirSupply());
   }

   public final int getExperienceReward(ServerLevel level, @Nullable Entity killer) {
      return EnchantmentHelper.processMobExperience(level, killer, this, this.getBaseExperienceReward(level));
   }

   protected int getBaseExperienceReward(ServerLevel level) {
      return 0;
   }

   protected boolean isAlwaysExperienceDropper() {
      return false;
   }

   @Nullable
   public LivingEntity getLastHurtByMob() {
      return EntityReference.getLivingEntity(this.lastHurtByMob, this.level());
   }

   @Nullable
   public Player getLastHurtByPlayer() {
      return EntityReference.getPlayer(this.lastHurtByPlayer, this.level());
   }

   @Override
   public LivingEntity getLastAttacker() {
      return this.getLastHurtByMob();
   }

   public int getLastHurtByMobTimestamp() {
      return this.lastHurtByMobTimestamp;
   }

   public void setLastHurtByPlayer(Player player, int memoryTime) {
      this.setLastHurtByPlayer(EntityReference.of(player), memoryTime);
   }

   public void setLastHurtByPlayer(UUID uuid, int memoryTime) {
      this.setLastHurtByPlayer(EntityReference.of(uuid), memoryTime);
   }

   private void setLastHurtByPlayer(EntityReference<Player> player, int memoryTime) {
      this.lastHurtByPlayer = player;
      this.lastHurtByPlayerMemoryTime = memoryTime;
   }

   public void setLastHurtByMob(@Nullable LivingEntity livingEntity) {
      this.lastHurtByMob = EntityReference.of(livingEntity);
      this.lastHurtByMobTimestamp = this.tickCount;
   }

   @Nullable
   public LivingEntity getLastHurtMob() {
      return this.lastHurtMob;
   }

   public int getLastHurtMobTimestamp() {
      return this.lastHurtMobTimestamp;
   }

   public void setLastHurtMob(Entity entity) {
      if (entity instanceof LivingEntity) {
         this.lastHurtMob = (LivingEntity)entity;
      } else {
         this.lastHurtMob = null;
      }

      this.lastHurtMobTimestamp = this.tickCount;
   }

   public int getNoActionTime() {
      return this.noActionTime;
   }

   public void setNoActionTime(int idleTime) {
      this.noActionTime = idleTime;
   }

   public boolean shouldDiscardFriction() {
      return this.discardFriction;
   }

   public void setDiscardFriction(boolean discardFriction) {
      this.discardFriction = discardFriction;
   }

   protected boolean doesEmitEquipEvent(EquipmentSlot slot) {
      return true;
   }

   public void onEquipItem(EquipmentSlot slot, ItemStack oldItem, ItemStack newItem) {
      if (!this.level().isClientSide() && !this.isSpectator()) {
         if (!ItemStack.isSameItemSameComponents(oldItem, newItem) && !this.firstTick) {
            Equippable equippable = newItem.get(DataComponents.EQUIPPABLE);
            if (!this.isSilent() && equippable != null && slot == equippable.slot()) {
               this.level()
                  .playSeededSound(
                     null,
                     this.getX(),
                     this.getY(),
                     this.getZ(),
                     this.getEquipSound(slot, newItem, equippable),
                     this.getSoundSource(),
                     1.0F,
                     1.0F,
                     this.random.nextLong()
                  );
            }

            if (this.doesEmitEquipEvent(slot)) {
               this.gameEvent(equippable != null ? GameEvent.EQUIP : GameEvent.UNEQUIP);
            }
         }
      }
   }

   protected Holder<SoundEvent> getEquipSound(EquipmentSlot slot, ItemStack stack, Equippable equippable) {
      return equippable.equipSound();
   }

   @Override
   public void remove(Entity.RemovalReason reason) {
      if ((reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) && this.level() instanceof ServerLevel serverLevel) {
         this.triggerOnDeathMobEffects(serverLevel, reason);
      }

      super.remove(reason);
      this.brain.clearMemories();
   }

   @Override
   public void onRemoval(Entity.RemovalReason reason) {
      super.onRemoval(reason);
      if (this.level() instanceof ServerLevel serverLevel) {
         serverLevel.getWaypointManager().untrackWaypoint((WaypointTransmitter)this);
      }
   }

   protected void triggerOnDeathMobEffects(ServerLevel level, Entity.RemovalReason removalReason) {
      for (MobEffectInstance mobEffectInstance : this.getActiveEffects()) {
         mobEffectInstance.onMobRemoved(level, this, removalReason);
      }

      this.activeEffects.clear();
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput output) {
      output.putFloat("Health", this.getHealth());
      output.putShort("HurtTime", (short)this.hurtTime);
      output.putInt("HurtByTimestamp", this.lastHurtByMobTimestamp);
      output.putShort("DeathTime", (short)this.deathTime);
      output.putFloat("AbsorptionAmount", this.getAbsorptionAmount());
      output.store("attributes", AttributeInstance.Packed.LIST_CODEC, this.getAttributes().pack());
      if (!this.activeEffects.isEmpty()) {
         output.store("active_effects", MobEffectInstance.CODEC.listOf(), List.copyOf(this.activeEffects.values()));
      }

      output.putBoolean("FallFlying", this.isFallFlying());
      this.getSleepingPos().ifPresent(blockPos -> output.store("sleeping_pos", BlockPos.CODEC, blockPos));
      DataResult<Dynamic<?>> dataResult = this.brain.serializeStart(NbtOps.INSTANCE).map(tag -> new Dynamic(NbtOps.INSTANCE, tag));
      dataResult.resultOrPartial(LOGGER::error).ifPresent(dynamic -> output.store("Brain", Codec.PASSTHROUGH, dynamic));
      if (this.lastHurtByPlayer != null) {
         this.lastHurtByPlayer.store(output, "last_hurt_by_player");
         output.putInt("last_hurt_by_player_memory_time", this.lastHurtByPlayerMemoryTime);
      }

      if (this.lastHurtByMob != null) {
         this.lastHurtByMob.store(output, "last_hurt_by_mob");
         output.putInt("ticks_since_last_hurt_by_mob", this.tickCount - this.lastHurtByMobTimestamp);
      }

      if (!this.equipment.isEmpty()) {
         output.store("equipment", EntityEquipment.CODEC, this.equipment);
      }

      if (this.locatorBarIcon.hasData()) {
         output.store("locator_bar_icon", Waypoint.Icon.CODEC, this.locatorBarIcon);
      }
   }

   @Nullable
   public ItemEntity drop(ItemStack stack, boolean randomizeMotion, boolean includeThrower) {
      if (stack.isEmpty()) {
         return null;
      } else if (this.level().isClientSide()) {
         this.swing(InteractionHand.MAIN_HAND);
         return null;
      } else {
         ItemEntity itemEntity = this.createItemStackToDrop(stack, randomizeMotion, includeThrower);
         if (itemEntity != null) {
            this.level().addFreshEntity(itemEntity);
         }

         return itemEntity;
      }
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
      this.internalSetAbsorptionAmount(input.getFloatOr("AbsorptionAmount", 0.0F));
      if (this.level() != null && !this.level().isClientSide()) {
         input.<List<AttributeInstance.Packed>>read("attributes", AttributeInstance.Packed.LIST_CODEC).ifPresent(this.getAttributes()::apply);
      }

      List<MobEffectInstance> list = input.<List<MobEffectInstance>>read("active_effects", MobEffectInstance.CODEC.listOf()).orElse(List.of());
      this.activeEffects.clear();

      for (MobEffectInstance mobEffectInstance : list) {
         this.activeEffects.put(mobEffectInstance.getEffect(), mobEffectInstance);
         this.effectsDirty = true;
      }

      this.setHealth(input.getFloatOr("Health", this.getMaxHealth()));
      this.hurtTime = input.getShortOr("HurtTime", (short)0);
      this.deathTime = input.getShortOr("DeathTime", (short)0);
      this.lastHurtByMobTimestamp = input.getIntOr("HurtByTimestamp", 0);
      input.getString("Team").ifPresent(string -> {
         Scoreboard scoreboard = this.level().getScoreboard();
         PlayerTeam playerTeam = scoreboard.getPlayerTeam(string);
         boolean bl = playerTeam != null && scoreboard.addPlayerToTeam(this.getStringUUID(), playerTeam);
         if (!bl) {
            LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", string);
         }
      });
      this.setSharedFlag(7, input.getBooleanOr("FallFlying", false));
      input.<BlockPos>read("sleeping_pos", BlockPos.CODEC).ifPresentOrElse(blockPos -> {
         this.setSleepingPos(blockPos);
         this.entityData.set(DATA_POSE, Pose.SLEEPING);
         if (!this.firstTick) {
            this.setPosToBed(blockPos);
         }
      }, this::clearSleepingPos);
      input.<Dynamic>read("Brain", Codec.PASSTHROUGH).ifPresent(dynamic -> this.brain = this.makeBrain((Dynamic<?>)dynamic));
      this.lastHurtByPlayer = EntityReference.read(input, "last_hurt_by_player");
      this.lastHurtByPlayerMemoryTime = input.getIntOr("last_hurt_by_player_memory_time", 0);
      this.lastHurtByMob = EntityReference.read(input, "last_hurt_by_mob");
      this.lastHurtByMobTimestamp = input.getIntOr("ticks_since_last_hurt_by_mob", 0) + this.tickCount;
      this.equipment.setAll(input.<EntityEquipment>read("equipment", EntityEquipment.CODEC).orElseGet(EntityEquipment::new));
      this.locatorBarIcon = input.<Waypoint.Icon>read("locator_bar_icon", Waypoint.Icon.CODEC).orElseGet(Waypoint.Icon::new);
   }

   @Override
   public void updateDataBeforeSync() {
      super.updateDataBeforeSync();
      this.updateDirtyEffects();
   }

   protected void tickEffects() {
      if (this.level() instanceof ServerLevel serverLevel) {
         Iterator<Holder<MobEffect>> iterator = this.activeEffects.keySet().iterator();

         try {
            while (iterator.hasNext()) {
               Holder<MobEffect> holder = iterator.next();
               MobEffectInstance mobEffectInstance = this.activeEffects.get(holder);
               if (!mobEffectInstance.tickServer(serverLevel, this, () -> this.onEffectUpdated(mobEffectInstance, true, null))) {
                  iterator.remove();
                  this.onEffectsRemoved(List.of(mobEffectInstance));
               } else if (mobEffectInstance.getDuration() % 600 == 0) {
                  this.onEffectUpdated(mobEffectInstance, false, null);
               }
            }
         } catch (ConcurrentModificationException var6) {
         }
      } else {
         for (MobEffectInstance mobEffectInstance2 : this.activeEffects.values()) {
            mobEffectInstance2.tickClient();
         }

         List<ParticleOptions> list = this.entityData.get(DATA_EFFECT_PARTICLES);
         if (!list.isEmpty()) {
            boolean bl = this.entityData.get(DATA_EFFECT_AMBIENCE_ID);
            int i = this.isInvisible() ? 15 : 4;
            int j = bl ? 5 : 1;
            if (this.random.nextInt(i * j) == 0) {
               this.level().addParticle(Util.getRandom(list, this.random), this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 1.0, 1.0, 1.0);
            }
         }
      }
   }

   private void updateDirtyEffects() {
      if (this.effectsDirty) {
         this.updateInvisibilityStatus();
         this.updateGlowingStatus();
         this.effectsDirty = false;
      }
   }

   protected void updateInvisibilityStatus() {
      if (this.activeEffects.isEmpty()) {
         this.removeEffectParticles();
         this.setInvisible(false);
      } else {
         this.setInvisible(this.hasEffect(MobEffects.INVISIBILITY));
         this.updateSynchronizedMobEffectParticles();
      }
   }

   private void updateSynchronizedMobEffectParticles() {
      List<ParticleOptions> list = this.activeEffects
         .values()
         .stream()
         .filter(MobEffectInstance::isVisible)
         .map(MobEffectInstance::getParticleOptions)
         .toList();
      this.entityData.set(DATA_EFFECT_PARTICLES, list);
      this.entityData.set(DATA_EFFECT_AMBIENCE_ID, areAllEffectsAmbient(this.activeEffects.values()));
   }

   private void updateGlowingStatus() {
      boolean bl = this.isCurrentlyGlowing();
      if (this.getSharedFlag(6) != bl) {
         this.setSharedFlag(6, bl);
      }
   }

   public double getVisibilityPercent(@Nullable Entity lookingEntity) {
      double d = 1.0;
      if (this.isDiscrete()) {
         d *= 0.8;
      }

      if (this.isInvisible()) {
         float f = this.getArmorCoverPercentage();
         if (f < 0.1F) {
            f = 0.1F;
         }

         d *= 0.7 * f;
      }

      if (lookingEntity != null) {
         ItemStack itemStack = this.getItemBySlot(EquipmentSlot.HEAD);
         EntityType<?> entityType = lookingEntity.getType();
         if (entityType == EntityType.SKELETON && itemStack.is(Items.SKELETON_SKULL)
            || entityType == EntityType.ZOMBIE && itemStack.is(Items.ZOMBIE_HEAD)
            || entityType == EntityType.PIGLIN && itemStack.is(Items.PIGLIN_HEAD)
            || entityType == EntityType.PIGLIN_BRUTE && itemStack.is(Items.PIGLIN_HEAD)
            || entityType == EntityType.CREEPER && itemStack.is(Items.CREEPER_HEAD)) {
            d *= 0.5;
         }
      }

      return d;
   }

   public boolean canAttack(LivingEntity target) {
      return target instanceof Player && this.level().getDifficulty() == Difficulty.PEACEFUL ? false : target.canBeSeenAsEnemy();
   }

   public boolean canBeSeenAsEnemy() {
      return !this.isInvulnerable() && this.canBeSeenByAnyone();
   }

   public boolean canBeSeenByAnyone() {
      return !this.isSpectator() && this.isAlive();
   }

   public static boolean areAllEffectsAmbient(Collection<MobEffectInstance> potionEffects) {
      for (MobEffectInstance mobEffectInstance : potionEffects) {
         if (mobEffectInstance.isVisible() && !mobEffectInstance.isAmbient()) {
            return false;
         }
      }

      return true;
   }

   protected void removeEffectParticles() {
      this.entityData.set(DATA_EFFECT_PARTICLES, List.of());
   }

   public boolean removeAllEffects() {
      if (this.level().isClientSide()) {
         return false;
      } else if (this.activeEffects.isEmpty()) {
         return false;
      } else {
         Map<Holder<MobEffect>, MobEffectInstance> map = Maps.newHashMap(this.activeEffects);
         this.activeEffects.clear();
         this.onEffectsRemoved(map.values());
         return true;
      }
   }

   public Collection<MobEffectInstance> getActiveEffects() {
      return this.activeEffects.values();
   }

   public Map<Holder<MobEffect>, MobEffectInstance> getActiveEffectsMap() {
      return this.activeEffects;
   }

   public boolean hasEffect(Holder<MobEffect> effect) {
      return this.activeEffects.containsKey(effect);
   }

   @Nullable
   public MobEffectInstance getEffect(Holder<MobEffect> effect) {
      return this.activeEffects.get(effect);
   }

   public float getEffectBlendFactor(Holder<MobEffect> effect, float partialTick) {
      MobEffectInstance mobEffectInstance = this.getEffect(effect);
      return mobEffectInstance != null ? mobEffectInstance.getBlendFactor(this, partialTick) : 0.0F;
   }

   public final boolean addEffect(MobEffectInstance effectInstance) {
      return this.addEffect(effectInstance, null);
   }

   public boolean addEffect(MobEffectInstance effectInstance, @Nullable Entity entity) {
      if (!this.canBeAffected(effectInstance)) {
         return false;
      } else {
         MobEffectInstance mobEffectInstance = this.activeEffects.get(effectInstance.getEffect());
         boolean bl = false;
         if (mobEffectInstance == null) {
            this.activeEffects.put(effectInstance.getEffect(), effectInstance);
            this.onEffectAdded(effectInstance, entity);
            bl = true;
            effectInstance.onEffectAdded(this);
         } else if (mobEffectInstance.update(effectInstance)) {
            this.onEffectUpdated(mobEffectInstance, true, entity);
            bl = true;
         }

         effectInstance.onEffectStarted(this);
         return bl;
      }
   }

   public boolean canBeAffected(MobEffectInstance effectInstance) {
      if (this.getType().is(EntityTypeTags.IMMUNE_TO_INFESTED)) {
         return !effectInstance.is(MobEffects.INFESTED);
      } else if (this.getType().is(EntityTypeTags.IMMUNE_TO_OOZING)) {
         return !effectInstance.is(MobEffects.OOZING);
      } else {
         return !this.getType().is(EntityTypeTags.IGNORES_POISON_AND_REGEN)
            ? true
            : !effectInstance.is(MobEffects.REGENERATION) && !effectInstance.is(MobEffects.POISON);
      }
   }

   public void forceAddEffect(MobEffectInstance instance, @Nullable Entity entity) {
      if (this.canBeAffected(instance)) {
         MobEffectInstance mobEffectInstance = this.activeEffects.put(instance.getEffect(), instance);
         if (mobEffectInstance == null) {
            this.onEffectAdded(instance, entity);
         } else {
            instance.copyBlendState(mobEffectInstance);
            this.onEffectUpdated(instance, true, entity);
         }
      }
   }

   public boolean isInvertedHealAndHarm() {
      return this.getType().is(EntityTypeTags.INVERTED_HEALING_AND_HARM);
   }

   @Nullable
   public final MobEffectInstance removeEffectNoUpdate(Holder<MobEffect> effect) {
      return this.activeEffects.remove(effect);
   }

   public boolean removeEffect(Holder<MobEffect> effect) {
      MobEffectInstance mobEffectInstance = this.removeEffectNoUpdate(effect);
      if (mobEffectInstance != null) {
         this.onEffectsRemoved(List.of(mobEffectInstance));
         return true;
      } else {
         return false;
      }
   }

   protected void onEffectAdded(MobEffectInstance effectInstance, @Nullable Entity entity) {
      if (!this.level().isClientSide()) {
         this.effectsDirty = true;
         effectInstance.getEffect().value().addAttributeModifiers(this.getAttributes(), effectInstance.getAmplifier());
         this.sendEffectToPassengers(effectInstance);
      }
   }

   public void sendEffectToPassengers(MobEffectInstance effectInstance) {
      for (Entity entity : this.getPassengers()) {
         if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), effectInstance, false));
         }
      }
   }

   protected void onEffectUpdated(MobEffectInstance effectInstance, boolean forced, @Nullable Entity entity) {
      if (!this.level().isClientSide()) {
         this.effectsDirty = true;
         if (forced) {
            MobEffect mobEffect = effectInstance.getEffect().value();
            mobEffect.removeAttributeModifiers(this.getAttributes());
            mobEffect.addAttributeModifiers(this.getAttributes(), effectInstance.getAmplifier());
            this.refreshDirtyAttributes();
         }

         this.sendEffectToPassengers(effectInstance);
      }
   }

   protected void onEffectsRemoved(Collection<MobEffectInstance> effects) {
      if (!this.level().isClientSide()) {
         this.effectsDirty = true;

         for (MobEffectInstance mobEffectInstance : effects) {
            mobEffectInstance.getEffect().value().removeAttributeModifiers(this.getAttributes());

            for (Entity entity : this.getPassengers()) {
               if (entity instanceof ServerPlayer serverPlayer) {
                  serverPlayer.connection.send(new ClientboundRemoveMobEffectPacket(this.getId(), mobEffectInstance.getEffect()));
               }
            }
         }

         this.refreshDirtyAttributes();
      }
   }

   private void refreshDirtyAttributes() {
      Set<AttributeInstance> set = this.getAttributes().getAttributesToUpdate();

      for (AttributeInstance attributeInstance : set) {
         this.onAttributeUpdated(attributeInstance.getAttribute());
      }

      set.clear();
   }

   protected void onAttributeUpdated(Holder<Attribute> attribute) {
      if (attribute.is(Attributes.MAX_HEALTH)) {
         float f = this.getMaxHealth();
         if (this.getHealth() > f) {
            this.setHealth(f);
         }
      } else if (attribute.is(Attributes.MAX_ABSORPTION)) {
         float f = this.getMaxAbsorption();
         if (this.getAbsorptionAmount() > f) {
            this.setAbsorptionAmount(f);
         }
      } else if (attribute.is(Attributes.SCALE)) {
         this.refreshDimensions();
      } else if (attribute.is(Attributes.WAYPOINT_TRANSMIT_RANGE) && this.level() instanceof ServerLevel serverLevel) {
         ServerWaypointManager serverWaypointManager = serverLevel.getWaypointManager();
         if (this.attributes.getValue(attribute) > 0.0) {
            serverWaypointManager.trackWaypoint((WaypointTransmitter)this);
         } else {
            serverWaypointManager.untrackWaypoint((WaypointTransmitter)this);
         }
      }
   }

   public void heal(float healAmount) {
      float f = this.getHealth();
      if (f > 0.0F) {
         this.setHealth(f + healAmount);
      }
   }

   public float getHealth() {
      return this.entityData.get(DATA_HEALTH_ID);
   }

   public void setHealth(float health) {
      this.entityData.set(DATA_HEALTH_ID, Mth.clamp(health, 0.0F, this.getMaxHealth()));
   }

   public boolean isDeadOrDying() {
      return this.getHealth() <= 0.0F;
   }

   @Override
   public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
      if (this.isInvulnerableTo(level, damageSource)) {
         return false;
      } else if (this.isDeadOrDying()) {
         return false;
      } else if (damageSource.is(DamageTypeTags.IS_FIRE) && this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
         return false;
      } else {
         if (this.isSleeping()) {
            this.stopSleeping();
         }

         this.noActionTime = 0;
         if (amount < 0.0F) {
            amount = 0.0F;
         }

         ItemStack itemStack = this.getUseItem();
         float g = this.applyItemBlocking(level, damageSource, amount);
         amount -= g;
         boolean bl = g > 0.0F;
         if (damageSource.is(DamageTypeTags.IS_FREEZING) && this.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
            amount *= 5.0F;
         }

         if (damageSource.is(DamageTypeTags.DAMAGES_HELMET) && !this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            this.hurtHelmet(damageSource, amount);
            amount *= 0.75F;
         }

         if (Float.isNaN(amount) || Float.isInfinite(amount)) {
            amount = Float.MAX_VALUE;
         }

         boolean bl2 = true;
         if (this.invulnerableTime > 10.0F && !damageSource.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
            if (amount <= this.lastHurt) {
               return false;
            }

            this.actuallyHurt(level, damageSource, amount - this.lastHurt);
            this.lastHurt = amount;
            bl2 = false;
         } else {
            this.lastHurt = amount;
            this.invulnerableTime = 20;
            this.actuallyHurt(level, damageSource, amount);
            this.hurtDuration = 10;
            this.hurtTime = this.hurtDuration;
         }

         this.resolveMobResponsibleForDamage(damageSource);
         this.resolvePlayerResponsibleForDamage(damageSource);
         if (bl2) {
            BlocksAttacks blocksAttacks = itemStack.get(DataComponents.BLOCKS_ATTACKS);
            if (bl && blocksAttacks != null) {
               blocksAttacks.onBlocked(level, this);
            } else {
               level.broadcastDamageEvent(this, damageSource);
            }

            if (!damageSource.is(DamageTypeTags.NO_IMPACT) && (!bl || amount > 0.0F)) {
               this.markHurt();
            }

            if (!damageSource.is(DamageTypeTags.NO_KNOCKBACK)) {
               double d = 0.0;
               double e = 0.0;
               if (damageSource.getDirectEntity() instanceof Projectile projectile) {
                  DoubleDoubleImmutablePair doubleDoubleImmutablePair = projectile.calculateHorizontalHurtKnockbackDirection(this, damageSource);
                  d = -doubleDoubleImmutablePair.leftDouble();
                  e = -doubleDoubleImmutablePair.rightDouble();
               } else if (damageSource.getSourcePosition() != null) {
                  d = damageSource.getSourcePosition().x() - this.getX();
                  e = damageSource.getSourcePosition().z() - this.getZ();
               }

               this.knockback(0.4F, d, e);
               if (!bl) {
                  this.indicateDamage(d, e);
               }
            }
         }

         if (this.isDeadOrDying()) {
            if (!this.checkTotemDeathProtection(damageSource)) {
               if (bl2) {
                  this.makeSound(this.getDeathSound());
                  this.playSecondaryHurtSound(damageSource);
               }

               this.die(damageSource);
            }
         } else if (bl2) {
            this.playHurtSound(damageSource);
            this.playSecondaryHurtSound(damageSource);
         }

         boolean bl3 = !bl || amount > 0.0F;
         if (bl3) {
            this.lastDamageSource = damageSource;
            this.lastDamageStamp = this.level().getGameTime();

            for (MobEffectInstance mobEffectInstance : this.getActiveEffects()) {
               mobEffectInstance.onMobHurt(level, this, damageSource, amount);
            }
         }

         if (this instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.ENTITY_HURT_PLAYER.trigger(serverPlayer, damageSource, amount, amount, bl);
            if (g > 0.0F && g < 3.4028235E37F) {
               serverPlayer.awardStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(g * 10.0F));
            }
         }

         if (damageSource.getEntity() instanceof ServerPlayer serverPlayerx) {
            CriteriaTriggers.PLAYER_HURT_ENTITY.trigger(serverPlayerx, this, damageSource, amount, amount, bl);
         }

         return bl3;
      }
   }

   public float applyItemBlocking(ServerLevel level, DamageSource damageSource, float damageAmount) {
      if (damageAmount <= 0.0F) {
         return 0.0F;
      } else {
         ItemStack itemStack = this.getItemBlockingWith();
         if (itemStack == null) {
            return 0.0F;
         } else {
            BlocksAttacks blocksAttacks = itemStack.get(DataComponents.BLOCKS_ATTACKS);
            if (blocksAttacks != null && !blocksAttacks.bypassedBy().map(damageSource::is).orElse(false)) {
               if (damageSource.getDirectEntity() instanceof AbstractArrow abstractArrow && abstractArrow.getPierceLevel() > 0) {
                  return 0.0F;
               } else {
                  Vec3 vec3 = damageSource.getSourcePosition();
                  double d;
                  if (vec3 != null) {
                     Vec3 vec32 = this.calculateViewVector(0.0F, this.getYHeadRot());
                     Vec3 vec33 = vec3.subtract(this.position());
                     vec33 = new Vec3(vec33.x, 0.0, vec33.z).normalize();
                     d = Math.acos(vec33.dot(vec32));
                  } else {
                     d = (float) Math.PI;
                  }

                  float f = blocksAttacks.resolveBlockedDamage(damageSource, damageAmount, d);
                  blocksAttacks.hurtBlockingItem(this.level(), itemStack, this, this.getUsedItemHand(), f);
                  if (f > 0.0F && !damageSource.is(DamageTypeTags.IS_PROJECTILE) && damageSource.getDirectEntity() instanceof LivingEntity livingEntity) {
                     this.blockUsingItem(level, livingEntity);
                  }

                  return f;
               }
            } else {
               return 0.0F;
            }
         }
      }
   }

   private void playSecondaryHurtSound(DamageSource damageSource) {
      if (damageSource.is(DamageTypes.THORNS)) {
         SoundSource soundSource = this instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
         this.level().playSound(null, this.position().x, this.position().y, this.position().z, SoundEvents.THORNS_HIT, soundSource);
      }
   }

   protected void resolveMobResponsibleForDamage(DamageSource damageSource) {
      if (damageSource.getEntity() instanceof LivingEntity livingEntity
         && !damageSource.is(DamageTypeTags.NO_ANGER)
         && (!damageSource.is(DamageTypes.WIND_CHARGE) || !this.getType().is(EntityTypeTags.NO_ANGER_FROM_WIND_CHARGE))) {
         this.setLastHurtByMob(livingEntity);
      }
   }

   @Nullable
   protected Player resolvePlayerResponsibleForDamage(DamageSource damageSource) {
      Entity entity = damageSource.getEntity();
      if (entity instanceof Player player) {
         this.setLastHurtByPlayer(player, 100);
      } else if (entity instanceof Wolf wolf && wolf.isTame()) {
         if (wolf.getOwnerReference() != null) {
            this.setLastHurtByPlayer(wolf.getOwnerReference().getUUID(), 100);
         } else {
            this.lastHurtByPlayer = null;
            this.lastHurtByPlayerMemoryTime = 0;
         }
      }

      return EntityReference.getPlayer(this.lastHurtByPlayer, this.level());
   }

   protected void blockUsingItem(ServerLevel level, LivingEntity entity) {
      entity.blockedByItem(this);
   }

   protected void blockedByItem(LivingEntity entity) {
      entity.knockback(0.5, entity.getX() - this.getX(), entity.getZ() - this.getZ());
   }

   private boolean checkTotemDeathProtection(DamageSource damageSource) {
      if (damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      } else {
         ItemStack itemStack = null;
         DeathProtection deathProtection = null;

         for (InteractionHand interactionHand : InteractionHand.values()) {
            ItemStack itemStack2 = this.getItemInHand(interactionHand);
            deathProtection = itemStack2.get(DataComponents.DEATH_PROTECTION);
            if (deathProtection != null) {
               itemStack = itemStack2.copy();
               itemStack2.shrink(1);
               break;
            }
         }

         if (itemStack != null) {
            if (this instanceof ServerPlayer serverPlayer) {
               serverPlayer.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
               CriteriaTriggers.USED_TOTEM.trigger(serverPlayer, itemStack);
               itemStack.causeUseVibration(this, GameEvent.ITEM_INTERACT_FINISH);
            }

            this.setHealth(1.0F);
            deathProtection.applyEffects(itemStack, this);
            this.level().broadcastEntityEvent(this, (byte)35);
         }

         return deathProtection != null;
      }
   }

   @Nullable
   public DamageSource getLastDamageSource() {
      if (this.level().getGameTime() - this.lastDamageStamp > 40L) {
         this.lastDamageSource = null;
      }

      return this.lastDamageSource;
   }

   protected void playHurtSound(DamageSource source) {
      this.makeSound(this.getHurtSound(source));
   }

   public void makeSound(@Nullable SoundEvent sound) {
      if (sound != null) {
         this.playSound(sound, this.getSoundVolume(), this.getVoicePitch());
      }
   }

   private void breakItem(ItemStack stack) {
      if (!stack.isEmpty()) {
         Holder<SoundEvent> holder = stack.get(DataComponents.BREAK_SOUND);
         if (holder != null && !this.isSilent()) {
            this.level()
               .playLocalSound(
                  this.getX(), this.getY(), this.getZ(), holder.value(), this.getSoundSource(), 0.8F, 0.8F + this.level().random.nextFloat() * 0.4F, false
               );
         }

         this.spawnItemParticles(stack, 5);
      }
   }

   public void die(DamageSource damageSource) {
      if (!this.isRemoved() && !this.dead) {
         Entity entity = damageSource.getEntity();
         LivingEntity livingEntity = this.getKillCredit();
         if (livingEntity != null) {
            livingEntity.awardKillScore(this, damageSource);
         }

         if (this.isSleeping()) {
            this.stopSleeping();
         }

         this.stopUsingItem();
         if (!this.level().isClientSide() && this.hasCustomName()) {
            LOGGER.info("Named entity {} died: {}", this, this.getCombatTracker().getDeathMessage().getString());
         }

         this.dead = true;
         this.getCombatTracker().recheckStatus();
         if (this.level() instanceof ServerLevel serverLevel) {
            if (entity == null || entity.killedEntity(serverLevel, this, damageSource)) {
               this.gameEvent(GameEvent.ENTITY_DIE);
               this.dropAllDeathLoot(serverLevel, damageSource);
               this.createWitherRose(livingEntity);
            }

            this.level().broadcastEntityEvent(this, (byte)3);
         }

         this.setPose(Pose.DYING);
      }
   }

   protected void createWitherRose(@Nullable LivingEntity entitySource) {
      if (this.level() instanceof ServerLevel serverLevel) {
         boolean var6 = false;
         if (entitySource instanceof WitherBoss) {
            if (serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
               BlockPos blockPos = this.blockPosition();
               BlockState blockState = Blocks.WITHER_ROSE.defaultBlockState();
               if (this.level().getBlockState(blockPos).isAir() && blockState.canSurvive(this.level(), blockPos)) {
                  this.level().setBlock(blockPos, blockState, 3);
                  var6 = true;
               }
            }

            if (!var6) {
               ItemEntity itemEntity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), new ItemStack(Items.WITHER_ROSE));
               this.level().addFreshEntity(itemEntity);
            }
         }
      }
   }

   protected void dropAllDeathLoot(ServerLevel level, DamageSource damageSource) {
      boolean bl = this.lastHurtByPlayerMemoryTime > 0;
      if (this.shouldDropLoot(level)) {
         this.dropFromLootTable(level, damageSource, bl);
         this.dropCustomDeathLoot(level, damageSource, bl);
      }

      this.dropEquipment(level);
      this.dropExperience(level, damageSource.getEntity());
   }

   protected void dropEquipment(ServerLevel level) {
   }

   protected void dropExperience(ServerLevel level, @Nullable Entity entity) {
      if (!this.wasExperienceConsumed()
         && (
            this.isAlwaysExperienceDropper()
               || this.lastHurtByPlayerMemoryTime > 0 && this.shouldDropExperience() && level.getGameRules().get(GameRules.MOB_DROPS)
         )) {
         ExperienceOrb.award(level, this.position(), this.getExperienceReward(level, entity));
      }
   }

   protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
   }

   public long getLootTableSeed() {
      return 0L;
   }

   protected float getKnockback(Entity attacker, DamageSource damageSource) {
      float f = (float)this.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
      return this.level() instanceof ServerLevel serverLevel
         ? EnchantmentHelper.modifyKnockback(serverLevel, this.getWeaponItem(), attacker, damageSource, f) / 2.0F
         : f / 2.0F;
   }

   protected void dropFromLootTable(ServerLevel level, DamageSource damageSource, boolean playerKill) {
      Optional<ResourceKey<LootTable>> optional = this.getLootTable();
      if (!optional.isEmpty()) {
         this.dropFromLootTable(level, damageSource, playerKill, optional.get());
      }
   }

   public void dropFromLootTable(ServerLevel level, DamageSource damageSource, boolean playerKill, ResourceKey<LootTable> lootTable) {
      this.dropFromLootTable(level, damageSource, playerKill, lootTable, itemStack -> this.spawnAtLocation(level, itemStack));
   }

   public void dropFromLootTable(
      ServerLevel level, DamageSource damageSource, boolean playerKill, ResourceKey<LootTable> lootTable, Consumer<ItemStack> dropConsumer
   ) {
      LootTable lootTable2 = level.getServer().reloadableRegistries().getLootTable(lootTable);
      LootParams.Builder builder = new LootParams.Builder(level)
         .withParameter(LootContextParams.THIS_ENTITY, this)
         .withParameter(LootContextParams.ORIGIN, this.position())
         .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)
         .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, damageSource.getEntity())
         .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, damageSource.getDirectEntity());
      Player player = this.getLastHurtByPlayer();
      if (playerKill && player != null) {
         builder = builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player).withLuck(player.getLuck());
      }

      LootParams lootParams = builder.create(LootContextParamSets.ENTITY);
      lootTable2.getRandomItems(lootParams, this.getLootTableSeed(), dropConsumer);
   }

   public boolean dropFromEntityInteractLootTable(
      ServerLevel level, ResourceKey<LootTable> lootTable, @Nullable Entity entity, ItemStack tool, BiConsumer<ServerLevel, ItemStack> dropConsumer
   ) {
      return this.dropFromLootTable(
         level,
         lootTable,
         builder -> builder.withParameter(LootContextParams.TARGET_ENTITY, this)
            .withOptionalParameter(LootContextParams.INTERACTING_ENTITY, entity)
            .withParameter(LootContextParams.TOOL, tool)
            .create(LootContextParamSets.ENTITY_INTERACT),
         dropConsumer
      );
   }

   public boolean dropFromGiftLootTable(ServerLevel level, ResourceKey<LootTable> lootTable, BiConsumer<ServerLevel, ItemStack> dropConsumer) {
      return this.dropFromLootTable(
         level,
         lootTable,
         builder -> builder.withParameter(LootContextParams.ORIGIN, this.position())
            .withParameter(LootContextParams.THIS_ENTITY, this)
            .create(LootContextParamSets.GIFT),
         dropConsumer
      );
   }

   protected void dropFromShearingLootTable(
      ServerLevel level, ResourceKey<LootTable> lootTable, ItemStack shears, BiConsumer<ServerLevel, ItemStack> dropConsumer
   ) {
      this.dropFromLootTable(
         level,
         lootTable,
         builder -> builder.withParameter(LootContextParams.ORIGIN, this.position())
            .withParameter(LootContextParams.THIS_ENTITY, this)
            .withParameter(LootContextParams.TOOL, shears)
            .create(LootContextParamSets.SHEARING),
         dropConsumer
      );
   }

   protected boolean dropFromLootTable(
      ServerLevel level,
      ResourceKey<LootTable> lootTable,
      Function<LootParams.Builder, LootParams> paramsBuilder,
      BiConsumer<ServerLevel, ItemStack> dropConsumer
   ) {
      LootTable lootTable2 = level.getServer().reloadableRegistries().getLootTable(lootTable);
      LootParams lootParams = paramsBuilder.apply(new LootParams.Builder(level));
      List<ItemStack> list = lootTable2.getRandomItems(lootParams);
      if (!list.isEmpty()) {
         list.forEach(itemStack -> dropConsumer.accept(level, itemStack));
         return true;
      } else {
         return false;
      }
   }

   public void knockback(double strength, double x, double z) {
      strength *= 1.0 - this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
      if (!(strength <= 0.0)) {
         this.needsSync = true;
         Vec3 vec3 = this.getDeltaMovement();

         while (x * x + z * z < 1.0E-5F) {
            x = (this.random.nextDouble() - this.random.nextDouble()) * 0.01;
            z = (this.random.nextDouble() - this.random.nextDouble()) * 0.01;
         }

         Vec3 vec32 = new Vec3(x, 0.0, z).normalize().scale(strength);
         this.setDeltaMovement(vec3.x / 2.0 - vec32.x, this.onGround() ? Math.min(0.4, vec3.y / 2.0 + strength) : vec3.y, vec3.z / 2.0 - vec32.z);
      }
   }

   public void indicateDamage(double xDistance, double zDistance) {
   }

   @Nullable
   protected SoundEvent getHurtSound(DamageSource damageSource) {
      return SoundEvents.GENERIC_HURT;
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      return SoundEvents.GENERIC_DEATH;
   }

   private SoundEvent getFallDamageSound(int height) {
      return height > 4 ? this.getFallSounds().big() : this.getFallSounds().small();
   }

   public void skipDropExperience() {
      this.skipDropExperience = true;
   }

   public boolean wasExperienceConsumed() {
      return this.skipDropExperience;
   }

   public float getHurtDir() {
      return 0.0F;
   }

   protected AABB getHitbox() {
      AABB aABB = this.getBoundingBox();
      Entity entity = this.getVehicle();
      if (entity != null) {
         Vec3 vec3 = entity.getPassengerRidingPosition(this);
         return aABB.setMinY(Math.max(vec3.y, aABB.minY));
      } else {
         return aABB;
      }
   }

   public Map<Enchantment, Set<EnchantmentLocationBasedEffect>> activeLocationDependentEnchantments(EquipmentSlot slot) {
      return (Map<Enchantment, Set<EnchantmentLocationBasedEffect>>)this.activeLocationDependentEnchantments
         .computeIfAbsent(slot, equipmentSlot -> new Reference2ObjectArrayMap());
   }

   public void lungeForwardMaybe() {
      if (this.level() instanceof ServerLevel serverLevel) {
         EnchantmentHelper.doLungeEffects(serverLevel, this);
      }
   }

   public LivingEntity.Fallsounds getFallSounds() {
      return new LivingEntity.Fallsounds(SoundEvents.GENERIC_SMALL_FALL, SoundEvents.GENERIC_BIG_FALL);
   }

   public Optional<BlockPos> getLastClimbablePos() {
      return this.lastClimbablePos;
   }

   public boolean onClimbable() {
      if (this.isSpectator()) {
         return false;
      } else {
         BlockPos blockPos = this.blockPosition();
         BlockState blockState = this.getInBlockState();
         if (this.isFallFlying() && blockState.is(BlockTags.CAN_GLIDE_THROUGH)) {
            return false;
         } else if (blockState.is(BlockTags.CLIMBABLE)) {
            this.lastClimbablePos = Optional.of(blockPos);
            return true;
         } else if (blockState.getBlock() instanceof TrapDoorBlock && this.trapdoorUsableAsLadder(blockPos, blockState)) {
            this.lastClimbablePos = Optional.of(blockPos);
            return true;
         } else {
            return false;
         }
      }
   }

   private boolean trapdoorUsableAsLadder(BlockPos pos, BlockState state) {
      if (!state.getValue(TrapDoorBlock.OPEN)) {
         return false;
      } else {
         BlockState blockState = this.level().getBlockState(pos.below());
         return blockState.is(Blocks.LADDER) && blockState.getValue(LadderBlock.FACING) == state.getValue(TrapDoorBlock.FACING);
      }
   }

   @Override
   public boolean isAlive() {
      return !this.isRemoved() && this.getHealth() > 0.0F;
   }

   public boolean isLookingAtMe(LivingEntity entity, double tolerance, boolean scaleByDistance, boolean visual, double... yValues) {
      Vec3 vec3 = entity.getViewVector(1.0F).normalize();

      for (double d : yValues) {
         Vec3 vec32 = new Vec3(this.getX() - entity.getX(), d - entity.getEyeY(), this.getZ() - entity.getZ());
         double e = vec32.length();
         vec32 = vec32.normalize();
         double f = vec3.dot(vec32);
         if (f > 1.0 - tolerance / (scaleByDistance ? e : 1.0)
            && entity.hasLineOfSight(this, visual ? ClipContext.Block.VISUAL : ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, d)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public int getMaxFallDistance() {
      return this.getComfortableFallDistance(0.0F);
   }

   protected final int getComfortableFallDistance(float health) {
      return Mth.floor(health + 3.0F);
   }

   @Override
   public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource) {
      boolean bl = super.causeFallDamage(fallDistance, damageMultiplier, damageSource);
      int i = this.calculateFallDamage(fallDistance, damageMultiplier);
      if (i > 0) {
         this.playSound(this.getFallDamageSound(i), 1.0F, 1.0F);
         this.playBlockFallSound();
         this.hurt(damageSource, i);
         return true;
      } else {
         return bl;
      }
   }

   protected int calculateFallDamage(double fallDistance, float damageMultiplier) {
      if (this.getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
         return 0;
      } else {
         double d = this.calculateFallPower(fallDistance);
         return Mth.floor(d * damageMultiplier * this.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER));
      }
   }

   private double calculateFallPower(double fallDistance) {
      return fallDistance + 1.0E-6 - this.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
   }

   protected void playBlockFallSound() {
      if (!this.isSilent()) {
         int i = Mth.floor(this.getX());
         int j = Mth.floor(this.getY() - 0.2F);
         int k = Mth.floor(this.getZ());
         BlockState blockState = this.level().getBlockState(new BlockPos(i, j, k));
         if (!blockState.isAir()) {
            SoundType soundType = blockState.getSoundType();
            this.playSound(soundType.getFallSound(), soundType.getVolume() * 0.5F, soundType.getPitch() * 0.75F);
         }
      }
   }

   @Override
   public void animateHurt(float yaw) {
      this.hurtDuration = 10;
      this.hurtTime = this.hurtDuration;
   }

   public int getArmorValue() {
      return Mth.floor(this.getAttributeValue(Attributes.ARMOR));
   }

   protected void hurtArmor(DamageSource damageSource, float damageAmount) {
   }

   protected void hurtHelmet(DamageSource damageSource, float damageAmount) {
   }

   protected void doHurtEquipment(DamageSource damageSource, float damageAmount, EquipmentSlot... slots) {
      if (!(damageAmount <= 0.0F)) {
         int i = (int)Math.max(1.0F, damageAmount / 4.0F);

         for (EquipmentSlot equipmentSlot : slots) {
            ItemStack itemStack = this.getItemBySlot(equipmentSlot);
            Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
            if (equippable != null && equippable.damageOnHurt() && itemStack.isDamageableItem() && itemStack.canBeHurtBy(damageSource)) {
               itemStack.hurtAndBreak(i, this, equipmentSlot);
            }
         }
      }
   }

   protected float getDamageAfterArmorAbsorb(DamageSource damageSource, float damageAmount) {
      if (!damageSource.is(DamageTypeTags.BYPASSES_ARMOR)) {
         this.hurtArmor(damageSource, damageAmount);
         damageAmount = CombatRules.getDamageAfterAbsorb(
            this, damageAmount, damageSource, this.getArmorValue(), (float)this.getAttributeValue(Attributes.ARMOR_TOUGHNESS)
         );
      }

      return damageAmount;
   }

   protected float getDamageAfterMagicAbsorb(DamageSource damageSource, float damageAmount) {
      if (damageSource.is(DamageTypeTags.BYPASSES_EFFECTS)) {
         return damageAmount;
      } else {
         if (this.hasEffect(MobEffects.RESISTANCE) && !damageSource.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            int i = (this.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
            int j = 25 - i;
            float f = damageAmount * j;
            float g = damageAmount;
            damageAmount = Math.max(f / 25.0F, 0.0F);
            float h = g - damageAmount;
            if (h > 0.0F && h < 3.4028235E37F) {
               if (this instanceof ServerPlayer) {
                  ((ServerPlayer)this).awardStat(Stats.DAMAGE_RESISTED, Math.round(h * 10.0F));
               } else if (damageSource.getEntity() instanceof ServerPlayer) {
                  ((ServerPlayer)damageSource.getEntity()).awardStat(Stats.DAMAGE_DEALT_RESISTED, Math.round(h * 10.0F));
               }
            }
         }

         if (damageAmount <= 0.0F) {
            return 0.0F;
         } else if (damageSource.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            return damageAmount;
         } else {
            float k;
            if (this.level() instanceof ServerLevel serverLevel) {
               k = EnchantmentHelper.getDamageProtection(serverLevel, this, damageSource);
            } else {
               k = 0.0F;
            }

            if (k > 0.0F) {
               damageAmount = CombatRules.getDamageAfterMagicAbsorb(damageAmount, k);
            }

            return damageAmount;
         }
      }
   }

   protected void actuallyHurt(ServerLevel level, DamageSource damageSource, float amount) {
      if (!this.isInvulnerableTo(level, damageSource)) {
         amount = this.getDamageAfterArmorAbsorb(damageSource, amount);
         amount = this.getDamageAfterMagicAbsorb(damageSource, amount);
         float var10 = Math.max(amount - this.getAbsorptionAmount(), 0.0F);
         this.setAbsorptionAmount(this.getAbsorptionAmount() - (amount - var10));
         float g = amount - var10;
         if (g > 0.0F && g < 3.4028235E37F && damageSource.getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(g * 10.0F));
         }

         if (var10 != 0.0F) {
            this.getCombatTracker().recordDamage(damageSource, var10);
            this.setHealth(this.getHealth() - var10);
            this.setAbsorptionAmount(this.getAbsorptionAmount() - var10);
            this.gameEvent(GameEvent.ENTITY_DAMAGE);
         }
      }
   }

   public CombatTracker getCombatTracker() {
      return this.combatTracker;
   }

   @Nullable
   public LivingEntity getKillCredit() {
      if (this.lastHurtByPlayer != null) {
         return this.lastHurtByPlayer.getEntity(this.level(), Player.class);
      } else {
         return this.lastHurtByMob != null ? this.lastHurtByMob.getEntity(this.level(), LivingEntity.class) : null;
      }
   }

   public final float getMaxHealth() {
      return (float)this.getAttributeValue(Attributes.MAX_HEALTH);
   }

   public final float getMaxAbsorption() {
      return (float)this.getAttributeValue(Attributes.MAX_ABSORPTION);
   }

   public final int getArrowCount() {
      return this.entityData.get(DATA_ARROW_COUNT_ID);
   }

   public final void setArrowCount(int count) {
      this.entityData.set(DATA_ARROW_COUNT_ID, count);
   }

   public final int getStingerCount() {
      return this.entityData.get(DATA_STINGER_COUNT_ID);
   }

   public final void setStingerCount(int stingerCount) {
      this.entityData.set(DATA_STINGER_COUNT_ID, stingerCount);
   }

   private int getCurrentSwingDuration() {
      ItemStack itemStack = this.getItemInHand(InteractionHand.MAIN_HAND);
      int i = itemStack.getSwingAnimation().duration();
      if (MobEffectUtil.hasDigSpeed(this)) {
         return i - (1 + MobEffectUtil.getDigSpeedAmplification(this));
      } else {
         return this.hasEffect(MobEffects.MINING_FATIGUE) ? i + (1 + this.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) * 2 : i;
      }
   }

   public void swing(InteractionHand hand) {
      this.swing(hand, false);
   }

   public void swing(InteractionHand hand, boolean updateSelf) {
      if (!this.swinging || this.swingTime >= this.getCurrentSwingDuration() / 2 || this.swingTime < 0) {
         this.swingTime = -1;
         this.swinging = true;
         this.swingingArm = hand;
         if (this.level() instanceof ServerLevel) {
            ClientboundAnimatePacket clientboundAnimatePacket = new ClientboundAnimatePacket(this, hand == InteractionHand.MAIN_HAND ? 0 : 3);
            ServerChunkCache serverChunkCache = ((ServerLevel)this.level()).getChunkSource();
            if (updateSelf) {
               serverChunkCache.sendToTrackingPlayersAndSelf(this, clientboundAnimatePacket);
            } else {
               serverChunkCache.sendToTrackingPlayers(this, clientboundAnimatePacket);
            }
         }
      }
   }

   @Override
   public void handleDamageEvent(DamageSource damageSource) {
      this.walkAnimation.setSpeed(1.5F);
      this.invulnerableTime = 20;
      this.hurtDuration = 10;
      this.hurtTime = this.hurtDuration;
      SoundEvent soundEvent = this.getHurtSound(damageSource);
      if (soundEvent != null) {
         this.playSound(soundEvent, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
      }

      this.lastDamageSource = damageSource;
      this.lastDamageStamp = this.level().getGameTime();
   }

   @Override
   public void handleEntityEvent(byte id) {
      switch (id) {
         case 2:
            this.onKineticHit();
            break;
         case 3:
            SoundEvent soundEvent = this.getDeathSound();
            if (soundEvent != null) {
               this.playSound(soundEvent, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }

            if (!(this instanceof Player)) {
               this.setHealth(0.0F);
               this.die(this.damageSources().generic());
            }
            break;
         case 46:
            int i = 128;

            for (int j = 0; j < 128; j++) {
               double d = j / 127.0;
               float f = (this.random.nextFloat() - 0.5F) * 0.2F;
               float g = (this.random.nextFloat() - 0.5F) * 0.2F;
               float h = (this.random.nextFloat() - 0.5F) * 0.2F;
               double e = Mth.lerp(d, this.xo, this.getX()) + (this.random.nextDouble() - 0.5) * this.getBbWidth() * 2.0;
               double k = Mth.lerp(d, this.yo, this.getY()) + this.random.nextDouble() * this.getBbHeight();
               double l = Mth.lerp(d, this.zo, this.getZ()) + (this.random.nextDouble() - 0.5) * this.getBbWidth() * 2.0;
               this.level().addParticle(ParticleTypes.PORTAL, e, k, l, f, g, h);
            }
            break;
         case 47:
            this.breakItem(this.getItemBySlot(EquipmentSlot.MAINHAND));
            break;
         case 48:
            this.breakItem(this.getItemBySlot(EquipmentSlot.OFFHAND));
            break;
         case 49:
            this.breakItem(this.getItemBySlot(EquipmentSlot.HEAD));
            break;
         case 50:
            this.breakItem(this.getItemBySlot(EquipmentSlot.CHEST));
            break;
         case 51:
            this.breakItem(this.getItemBySlot(EquipmentSlot.LEGS));
            break;
         case 52:
            this.breakItem(this.getItemBySlot(EquipmentSlot.FEET));
            break;
         case 54:
            HoneyBlock.showJumpParticles(this);
            break;
         case 55:
            this.swapHandItems();
            break;
         case 60:
            this.makePoofParticles();
            break;
         case 65:
            this.breakItem(this.getItemBySlot(EquipmentSlot.BODY));
            break;
         case 67:
            this.makeDrownParticles();
            break;
         case 68:
            this.breakItem(this.getItemBySlot(EquipmentSlot.SADDLE));
            break;
         default:
            super.handleEntityEvent(id);
      }
   }

   public float getTicksSinceLastKineticHitFeedback(float partialTick) {
      return this.lastKineticHitFeedbackTime < 0L ? 0.0F : (float)(this.level().getGameTime() - this.lastKineticHitFeedbackTime) + partialTick;
   }

   public void makePoofParticles() {
      for (int i = 0; i < 20; i++) {
         double d = this.random.nextGaussian() * 0.02;
         double e = this.random.nextGaussian() * 0.02;
         double f = this.random.nextGaussian() * 0.02;
         double g = 10.0;
         this.level().addParticle(ParticleTypes.POOF, this.getRandomX(1.0) - d * 10.0, this.getRandomY() - e * 10.0, this.getRandomZ(1.0) - f * 10.0, d, e, f);
      }
   }

   private void makeDrownParticles() {
      Vec3 vec3 = this.getDeltaMovement();

      for (int i = 0; i < 8; i++) {
         double d = this.random.triangle(0.0, 1.0);
         double e = this.random.triangle(0.0, 1.0);
         double f = this.random.triangle(0.0, 1.0);
         this.level().addParticle(ParticleTypes.BUBBLE, this.getX() + d, this.getY() + e, this.getZ() + f, vec3.x, vec3.y, vec3.z);
      }
   }

   private void onKineticHit() {
      if (this.level().getGameTime() - this.lastKineticHitFeedbackTime > 10L) {
         this.lastKineticHitFeedbackTime = this.level().getGameTime();
         KineticWeapon kineticWeapon = this.useItem.get(DataComponents.KINETIC_WEAPON);
         if (kineticWeapon != null) {
            kineticWeapon.makeLocalHitSound(this);
         }
      }
   }

   private void swapHandItems() {
      ItemStack itemStack = this.getItemBySlot(EquipmentSlot.OFFHAND);
      this.setItemSlot(EquipmentSlot.OFFHAND, this.getItemBySlot(EquipmentSlot.MAINHAND));
      this.setItemSlot(EquipmentSlot.MAINHAND, itemStack);
   }

   @Override
   protected void onBelowWorld() {
      this.hurt(this.damageSources().fellOutOfWorld(), 4.0F);
   }

   protected void updateSwingTime() {
      int i = this.getCurrentSwingDuration();
      if (this.swinging) {
         this.swingTime++;
         if (this.swingTime >= i) {
            this.swingTime = 0;
            this.swinging = false;
         }
      } else {
         this.swingTime = 0;
      }

      this.attackAnim = (float)this.swingTime / i;
   }

   @Nullable
   public AttributeInstance getAttribute(Holder<Attribute> attribute) {
      return this.getAttributes().getInstance(attribute);
   }

   public double getAttributeValue(Holder<Attribute> attribute) {
      return this.getAttributes().getValue(attribute);
   }

   public double getAttributeBaseValue(Holder<Attribute> attribute) {
      return this.getAttributes().getBaseValue(attribute);
   }

   public AttributeMap getAttributes() {
      return this.attributes;
   }

   public ItemStack getMainHandItem() {
      return this.getItemBySlot(EquipmentSlot.MAINHAND);
   }

   public ItemStack getOffhandItem() {
      return this.getItemBySlot(EquipmentSlot.OFFHAND);
   }

   public ItemStack getItemHeldByArm(HumanoidArm arm) {
      return this.getMainArm() == arm ? this.getMainHandItem() : this.getOffhandItem();
   }

   @Override
   public ItemStack getWeaponItem() {
      return this.getMainHandItem();
   }

   public AttackRange entityAttackRange() {
      AttackRange attackRange = this.getActiveItem().get(DataComponents.ATTACK_RANGE);
      return attackRange != null ? attackRange : AttackRange.defaultFor(this);
   }

   public ItemStack getActiveItem() {
      return this.isUsingItem() ? this.getUseItem() : this.getMainHandItem();
   }

   public boolean isHolding(Item item) {
      return this.isHolding(itemStack -> itemStack.is(item));
   }

   public boolean isHolding(Predicate<ItemStack> predicate) {
      return predicate.test(this.getMainHandItem()) || predicate.test(this.getOffhandItem());
   }

   public ItemStack getItemInHand(InteractionHand hand) {
      if (hand == InteractionHand.MAIN_HAND) {
         return this.getItemBySlot(EquipmentSlot.MAINHAND);
      } else if (hand == InteractionHand.OFF_HAND) {
         return this.getItemBySlot(EquipmentSlot.OFFHAND);
      } else {
         throw new IllegalArgumentException("Invalid hand " + hand);
      }
   }

   public void setItemInHand(InteractionHand hand, ItemStack stack) {
      if (hand == InteractionHand.MAIN_HAND) {
         this.setItemSlot(EquipmentSlot.MAINHAND, stack);
      } else {
         if (hand != InteractionHand.OFF_HAND) {
            throw new IllegalArgumentException("Invalid hand " + hand);
         }

         this.setItemSlot(EquipmentSlot.OFFHAND, stack);
      }
   }

   public boolean hasItemInSlot(EquipmentSlot slot) {
      return !this.getItemBySlot(slot).isEmpty();
   }

   public boolean canUseSlot(EquipmentSlot slot) {
      return true;
   }

   public ItemStack getItemBySlot(EquipmentSlot slot) {
      return this.equipment.get(slot);
   }

   public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
      this.onEquipItem(slot, this.equipment.set(slot, stack), stack);
   }

   public float getArmorCoverPercentage() {
      int i = 0;
      int j = 0;

      for (EquipmentSlot equipmentSlot : EquipmentSlotGroup.ARMOR) {
         if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            ItemStack itemStack = this.getItemBySlot(equipmentSlot);
            if (!itemStack.isEmpty()) {
               j++;
            }

            i++;
         }
      }

      return i > 0 ? (float)j / i : 0.0F;
   }

   @Override
   public void setSprinting(boolean sprinting) {
      super.setSprinting(sprinting);
      AttributeInstance attributeInstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
      attributeInstance.removeModifier(SPEED_MODIFIER_SPRINTING.id());
      if (sprinting) {
         attributeInstance.addTransientModifier(SPEED_MODIFIER_SPRINTING);
      }
   }

   protected float getSoundVolume() {
      return 1.0F;
   }

   public float getVoicePitch() {
      return this.isBaby()
         ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.5F
         : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
   }

   protected boolean isImmobile() {
      return this.isDeadOrDying();
   }

   @Override
   public void push(Entity entity) {
      if (!this.isSleeping()) {
         super.push(entity);
      }
   }

   private void dismountVehicle(Entity vehicle) {
      Vec3 vec3;
      if (this.isRemoved()) {
         vec3 = this.position();
      } else if (!vehicle.isRemoved() && !this.level().getBlockState(vehicle.blockPosition()).is(BlockTags.PORTALS)) {
         vec3 = vehicle.getDismountLocationForPassenger(this);
      } else {
         double d = Math.max(this.getY(), vehicle.getY());
         vec3 = new Vec3(this.getX(), d, this.getZ());
         boolean bl = this.getBbWidth() <= 4.0F && this.getBbHeight() <= 4.0F;
         if (bl) {
            double e = this.getBbHeight() / 2.0;
            Vec3 vec32 = vec3.add(0.0, e, 0.0);
            VoxelShape voxelShape = Shapes.create(AABB.ofSize(vec32, this.getBbWidth(), this.getBbHeight(), this.getBbWidth()));
            vec3 = this.level()
               .findFreePosition(this, voxelShape, vec32, this.getBbWidth(), this.getBbHeight(), this.getBbWidth())
               .map(vec3x -> vec3x.add(0.0, -e, 0.0))
               .orElse(vec3);
         }
      }

      this.dismountTo(vec3.x, vec3.y, vec3.z);
   }

   @Override
   public boolean shouldShowName() {
      return this.isCustomNameVisible();
   }

   protected float getJumpPower() {
      return this.getJumpPower(1.0F);
   }

   protected float getJumpPower(float multiplier) {
      return (float)this.getAttributeValue(Attributes.JUMP_STRENGTH) * multiplier * this.getBlockJumpFactor() + this.getJumpBoostPower();
   }

   public float getJumpBoostPower() {
      return this.hasEffect(MobEffects.JUMP_BOOST) ? 0.1F * (this.getEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1.0F) : 0.0F;
   }

   @VisibleForTesting
   public void jumpFromGround() {
      float f = this.getJumpPower();
      if (!(f <= 1.0E-5F)) {
         Vec3 vec3 = this.getDeltaMovement();
         this.setDeltaMovement(vec3.x, Math.max((double)f, vec3.y), vec3.z);
         if (this.isSprinting()) {
            float g = this.getYRot() * (float) (Math.PI / 180.0);
            this.addDeltaMovement(new Vec3(-Mth.sin(g) * 0.2, 0.0, Mth.cos(g) * 0.2));
         }

         this.needsSync = true;
      }
   }

   protected void goDownInWater() {
      this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04F, 0.0));
   }

   protected void jumpInLiquid(TagKey<Fluid> fluidTag) {
      this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.04F, 0.0));
   }

   protected float getWaterSlowDown() {
      return 0.8F;
   }

   public boolean canStandOnFluid(FluidState fluidState) {
      return false;
   }

   @Override
   protected double getDefaultGravity() {
      return this.getAttributeValue(Attributes.GRAVITY);
   }

   protected double getEffectiveGravity() {
      boolean bl = this.getDeltaMovement().y <= 0.0;
      return bl && this.hasEffect(MobEffects.SLOW_FALLING) ? Math.min(this.getGravity(), 0.01) : this.getGravity();
   }

   public void travel(Vec3 travelVector) {
      if (this.shouldTravelInFluid(this.level().getFluidState(this.blockPosition()))) {
         this.travelInFluid(travelVector);
      } else if (this.isFallFlying()) {
         this.travelFallFlying(travelVector);
      } else {
         this.travelInAir(travelVector);
      }
   }

   protected boolean shouldTravelInFluid(FluidState fluidState) {
      return (this.isInWater() || this.isInLava()) && this.isAffectedByFluids() && !this.canStandOnFluid(fluidState);
   }

   protected void travelFlying(Vec3 relative, float amount) {
      this.travelFlying(relative, 0.02F, 0.02F, amount);
   }

   protected void travelFlying(Vec3 relative, float inWaterAmount, float inLavaAmount, float amount) {
      if (this.isInWater()) {
         this.moveRelative(inWaterAmount, relative);
         this.move(MoverType.SELF, this.getDeltaMovement());
         this.setDeltaMovement(this.getDeltaMovement().scale(0.8F));
      } else if (this.isInLava()) {
         this.moveRelative(inLavaAmount, relative);
         this.move(MoverType.SELF, this.getDeltaMovement());
         this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
      } else {
         this.moveRelative(amount, relative);
         this.move(MoverType.SELF, this.getDeltaMovement());
         this.setDeltaMovement(this.getDeltaMovement().scale(0.91F));
      }
   }

   private void travelInAir(Vec3 travelVector) {
      BlockPos blockPos = this.getBlockPosBelowThatAffectsMyMovement();
      float f = this.onGround() ? this.level().getBlockState(blockPos).getBlock().getFriction() : 1.0F;
      float g = f * 0.91F;
      Vec3 vec3 = this.handleRelativeFrictionAndCalculateMovement(travelVector, f);
      double d = vec3.y;
      MobEffectInstance mobEffectInstance = this.getEffect(MobEffects.LEVITATION);
      if (mobEffectInstance != null) {
         d += (0.05 * (mobEffectInstance.getAmplifier() + 1) - vec3.y) * 0.2;
      } else if (!this.level().isClientSide() || this.level().hasChunkAt(blockPos)) {
         d -= this.getEffectiveGravity();
      } else if (this.getY() > this.level().getMinY()) {
         d = -0.1;
      } else {
         d = 0.0;
      }

      if (this.shouldDiscardFriction()) {
         this.setDeltaMovement(vec3.x, d, vec3.z);
      } else {
         float h = this instanceof FlyingAnimal ? g : 0.98F;
         this.setDeltaMovement(vec3.x * g, d * h, vec3.z * g);
      }
   }

   private void travelInFluid(Vec3 travelVector) {
      boolean bl = this.getDeltaMovement().y <= 0.0;
      double d = this.getY();
      double e = this.getEffectiveGravity();
      if (this.isInWater()) {
         this.travelInWater(travelVector, e, bl, d);
         this.floatInWaterWhileRidden();
      } else {
         this.travelInLava(travelVector, e, bl, d);
      }
   }

   protected void travelInWater(Vec3 travelVector, double gravity, boolean movingDown, double y) {
      float f = this.isSprinting() ? 0.9F : this.getWaterSlowDown();
      float g = 0.02F;
      float h = (float)this.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
      if (!this.onGround()) {
         h *= 0.5F;
      }

      if (h > 0.0F) {
         f += (0.54600006F - f) * h;
         g += (this.getSpeed() - g) * h;
      }

      if (this.hasEffect(MobEffects.DOLPHINS_GRACE)) {
         f = 0.96F;
      }

      this.moveRelative(g, travelVector);
      this.move(MoverType.SELF, this.getDeltaMovement());
      Vec3 vec3 = this.getDeltaMovement();
      if (this.horizontalCollision && this.onClimbable()) {
         vec3 = new Vec3(vec3.x, 0.2, vec3.z);
      }

      vec3 = vec3.multiply(f, 0.8F, f);
      this.setDeltaMovement(this.getFluidFallingAdjustedMovement(gravity, movingDown, vec3));
      this.jumpOutOfFluid(y);
   }

   private void travelInLava(Vec3 travelVector, double gravity, boolean movingDown, double y) {
      this.moveRelative(0.02F, travelVector);
      this.move(MoverType.SELF, this.getDeltaMovement());
      if (this.getFluidHeight(FluidTags.LAVA) <= this.getFluidJumpThreshold()) {
         this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.8F, 0.5));
         Vec3 vec3 = this.getFluidFallingAdjustedMovement(gravity, movingDown, this.getDeltaMovement());
         this.setDeltaMovement(vec3);
      } else {
         this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
      }

      if (gravity != 0.0) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, -gravity / 4.0, 0.0));
      }

      this.jumpOutOfFluid(y);
   }

   private void jumpOutOfFluid(double power) {
      Vec3 vec3 = this.getDeltaMovement();
      if (this.horizontalCollision && this.isFree(vec3.x, vec3.y + 0.6F - this.getY() + power, vec3.z)) {
         this.setDeltaMovement(vec3.x, 0.3F, vec3.z);
      }
   }

   private void floatInWaterWhileRidden() {
      boolean bl = this.getType().is(EntityTypeTags.CAN_FLOAT_WHILE_RIDDEN);
      if (bl && this.isVehicle() && this.getFluidHeight(FluidTags.WATER) > this.getFluidJumpThreshold()) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.04F, 0.0));
      }
   }

   private void travelFallFlying(Vec3 travelVector) {
      if (this.onClimbable()) {
         this.travelInAir(travelVector);
         this.stopFallFlying();
      } else {
         Vec3 vec3 = this.getDeltaMovement();
         double d = vec3.horizontalDistance();
         this.setDeltaMovement(this.updateFallFlyingMovement(vec3));
         this.move(MoverType.SELF, this.getDeltaMovement());
         if (!this.level().isClientSide()) {
            double e = this.getDeltaMovement().horizontalDistance();
            this.handleFallFlyingCollisions(d, e);
         }
      }
   }

   public void stopFallFlying() {
      this.setSharedFlag(7, true);
      this.setSharedFlag(7, false);
   }

   private Vec3 updateFallFlyingMovement(Vec3 deltaMovement) {
      Vec3 vec3 = this.getLookAngle();
      float f = this.getXRot() * (float) (Math.PI / 180.0);
      double d = Math.sqrt(vec3.x * vec3.x + vec3.z * vec3.z);
      double e = deltaMovement.horizontalDistance();
      double g = this.getEffectiveGravity();
      double h = Mth.square(Math.cos(f));
      deltaMovement = deltaMovement.add(0.0, g * (-1.0 + h * 0.75), 0.0);
      if (deltaMovement.y < 0.0 && d > 0.0) {
         double i = deltaMovement.y * -0.1 * h;
         deltaMovement = deltaMovement.add(vec3.x * i / d, i, vec3.z * i / d);
      }

      if (f < 0.0F && d > 0.0) {
         double i = e * -Mth.sin(f) * 0.04;
         deltaMovement = deltaMovement.add(-vec3.x * i / d, i * 3.2, -vec3.z * i / d);
      }

      if (d > 0.0) {
         deltaMovement = deltaMovement.add((vec3.x / d * e - deltaMovement.x) * 0.1, 0.0, (vec3.z / d * e - deltaMovement.z) * 0.1);
      }

      return deltaMovement.multiply(0.99F, 0.98F, 0.99F);
   }

   private void handleFallFlyingCollisions(double oldSpeed, double newSpeed) {
      if (this.horizontalCollision) {
         double d = oldSpeed - newSpeed;
         float f = (float)(d * 10.0 - 3.0);
         if (f > 0.0F) {
            this.playSound(this.getFallDamageSound((int)f), 1.0F, 1.0F);
            this.hurt(this.damageSources().flyIntoWall(), f);
         }
      }
   }

   private void travelRidden(Player player, Vec3 travelVector) {
      Vec3 vec3 = this.getRiddenInput(player, travelVector);
      this.tickRidden(player, vec3);
      if (this.canSimulateMovement()) {
         this.setSpeed(this.getRiddenSpeed(player));
         this.travel(vec3);
      } else {
         this.setDeltaMovement(Vec3.ZERO);
      }
   }

   protected void tickRidden(Player player, Vec3 travelVector) {
   }

   protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
      return travelVector;
   }

   protected float getRiddenSpeed(Player player) {
      return this.getSpeed();
   }

   public void calculateEntityAnimation(boolean includeHeight) {
      float f = (float)Mth.length(this.getX() - this.xo, includeHeight ? this.getY() - this.yo : 0.0, this.getZ() - this.zo);
      if (!this.isPassenger() && this.isAlive()) {
         this.updateWalkAnimation(f);
      } else {
         this.walkAnimation.stop();
      }
   }

   protected void updateWalkAnimation(float partialTick) {
      float f = Math.min(partialTick * 4.0F, 1.0F);
      this.walkAnimation.update(f, 0.4F, this.isBaby() ? 3.0F : 1.0F);
   }

   private Vec3 handleRelativeFrictionAndCalculateMovement(Vec3 deltaMovement, float friction) {
      this.moveRelative(this.getFrictionInfluencedSpeed(friction), deltaMovement);
      this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
      this.move(MoverType.SELF, this.getDeltaMovement());
      Vec3 vec3 = this.getDeltaMovement();
      if ((this.horizontalCollision || this.jumping) && (this.onClimbable() || this.wasInPowderSnow && PowderSnowBlock.canEntityWalkOnPowderSnow(this))) {
         vec3 = new Vec3(vec3.x, 0.2, vec3.z);
      }

      return vec3;
   }

   public Vec3 getFluidFallingAdjustedMovement(double gravity, boolean isFalling, Vec3 deltaMovement) {
      if (gravity != 0.0 && !this.isSprinting()) {
         double d;
         if (isFalling && Math.abs(deltaMovement.y - 0.005) >= 0.003 && Math.abs(deltaMovement.y - gravity / 16.0) < 0.003) {
            d = -0.003;
         } else {
            d = deltaMovement.y - gravity / 16.0;
         }

         return new Vec3(deltaMovement.x, d, deltaMovement.z);
      } else {
         return deltaMovement;
      }
   }

   private Vec3 handleOnClimbable(Vec3 deltaMovement) {
      if (this.onClimbable()) {
         this.resetFallDistance();
         float f = 0.15F;
         double d = Mth.clamp(deltaMovement.x, -0.15F, 0.15F);
         double e = Mth.clamp(deltaMovement.z, -0.15F, 0.15F);
         double g = Math.max(deltaMovement.y, -0.15F);
         if (g < 0.0 && !this.getInBlockState().is(Blocks.SCAFFOLDING) && this.isSuppressingSlidingDownLadder() && this instanceof Player) {
            g = 0.0;
         }

         deltaMovement = new Vec3(d, g, e);
      }

      return deltaMovement;
   }

   private float getFrictionInfluencedSpeed(float friction) {
      return this.onGround() ? this.getSpeed() * (0.21600002F / (friction * friction * friction)) : this.getFlyingSpeed();
   }

   protected float getFlyingSpeed() {
      return this.getControllingPassenger() instanceof Player ? this.getSpeed() * 0.1F : 0.02F;
   }

   public float getSpeed() {
      return this.speed;
   }

   public void setSpeed(float speed) {
      this.speed = speed;
   }

   public boolean doHurtTarget(ServerLevel level, Entity source) {
      this.setLastHurtMob(source);
      return false;
   }

   public void causeExtraKnockback(Entity entity, float knockback, Vec3 deltaMovement) {
      if (knockback > 0.0F && entity instanceof LivingEntity livingEntity) {
         livingEntity.knockback(knockback, Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)), -Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)));
         this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
      }
   }

   protected void playAttackSound() {
   }

   @Override
   public void tick() {
      super.tick();
      this.updatingUsingItem();
      this.updateSwimAmount();
      if (!this.level().isClientSide()) {
         int i = this.getArrowCount();
         if (i > 0) {
            if (this.removeArrowTime <= 0) {
               this.removeArrowTime = 20 * (30 - i);
            }

            this.removeArrowTime--;
            if (this.removeArrowTime <= 0) {
               this.setArrowCount(i - 1);
            }
         }

         int j = this.getStingerCount();
         if (j > 0) {
            if (this.removeStingerTime <= 0) {
               this.removeStingerTime = 20 * (30 - j);
            }

            this.removeStingerTime--;
            if (this.removeStingerTime <= 0) {
               this.setStingerCount(j - 1);
            }
         }

         this.detectEquipmentUpdates();
         if (this.tickCount % 20 == 0) {
            this.getCombatTracker().recheckStatus();
         }

         if (this.isSleeping() && (!this.canInteractWithLevel() || !this.checkBedExists())) {
            this.stopSleeping();
         }
      }

      if (!this.isRemoved()) {
         this.aiStep();
      }

      double d = this.getX() - this.xo;
      double e = this.getZ() - this.zo;
      float f = (float)(d * d + e * e);
      float g = this.yBodyRot;
      if (f > 0.0025000002F) {
         float h = (float)Mth.atan2(e, d) * (180.0F / (float)Math.PI) - 90.0F;
         float k = Mth.abs(Mth.wrapDegrees(this.getYRot()) - h);
         if (95.0F < k && k < 265.0F) {
            g = h - 180.0F;
         } else {
            g = h;
         }
      }

      if (this.attackAnim > 0.0F) {
         g = this.getYRot();
      }

      ProfilerFiller profilerFiller = Profiler.get();
      profilerFiller.push("headTurn");
      this.tickHeadTurn(g);
      profilerFiller.pop();
      profilerFiller.push("rangeChecks");

      while (this.getYRot() - this.yRotO < -180.0F) {
         this.yRotO -= 360.0F;
      }

      while (this.getYRot() - this.yRotO >= 180.0F) {
         this.yRotO += 360.0F;
      }

      while (this.yBodyRot - this.yBodyRotO < -180.0F) {
         this.yBodyRotO -= 360.0F;
      }

      while (this.yBodyRot - this.yBodyRotO >= 180.0F) {
         this.yBodyRotO += 360.0F;
      }

      while (this.getXRot() - this.xRotO < -180.0F) {
         this.xRotO -= 360.0F;
      }

      while (this.getXRot() - this.xRotO >= 180.0F) {
         this.xRotO += 360.0F;
      }

      while (this.yHeadRot - this.yHeadRotO < -180.0F) {
         this.yHeadRotO -= 360.0F;
      }

      while (this.yHeadRot - this.yHeadRotO >= 180.0F) {
         this.yHeadRotO += 360.0F;
      }

      profilerFiller.pop();
      if (this.isFallFlying()) {
         this.fallFlyTicks++;
      } else {
         this.fallFlyTicks = 0;
      }

      if (this.isSleeping()) {
         this.setXRot(0.0F);
      }

      this.refreshDirtyAttributes();
      this.elytraAnimationState.tick();
   }

   public boolean wasRecentlyStabbed(Entity entity, int contactCooldownTicks) {
      if (this.recentKineticEnemies == null) {
         return false;
      } else {
         return this.recentKineticEnemies.containsKey(entity)
            ? this.level().getGameTime() - this.recentKineticEnemies.getLong(entity) < contactCooldownTicks
            : false;
      }
   }

   public void rememberStabbedEntity(Entity entity) {
      if (this.recentKineticEnemies != null) {
         this.recentKineticEnemies.put(entity, this.level().getGameTime());
      }
   }

   public int stabbedEntities(Predicate<Entity> predicate) {
      return this.recentKineticEnemies == null ? 0 : (int)this.recentKineticEnemies.keySet().stream().filter(predicate).count();
   }

   public boolean stabAttack(EquipmentSlot slot, Entity entity, float damageAmount, boolean damage, boolean knockback, boolean dismount) {
      if (!(this.level() instanceof ServerLevel serverLevel)) {
         return false;
      } else {
         ItemStack itemStack = this.getItemBySlot(slot);
         DamageSource damageSource = itemStack.getDamageSource(this, () -> this.damageSources().mobAttack(this));
         float f = EnchantmentHelper.modifyDamage(serverLevel, itemStack, entity, damageSource, damageAmount);
         Vec3 vec3 = entity.getDeltaMovement();
         boolean bl2 = damage && entity.hurtServer(serverLevel, damageSource, f);
         boolean bl = knockback | bl2;
         if (knockback) {
            this.causeExtraKnockback(entity, 0.4F + this.getKnockback(entity, damageSource), vec3);
         }

         if (dismount && entity.isPassenger()) {
            bl = true;
            entity.stopRiding();
         }

         if (entity instanceof LivingEntity livingEntity) {
            itemStack.hurtEnemy(livingEntity, this);
         }

         if (bl2) {
            EnchantmentHelper.doPostAttackEffects(serverLevel, entity, damageSource);
         }

         if (!bl) {
            return false;
         } else {
            this.setLastHurtMob(entity);
            this.playAttackSound();
            return true;
         }
      }
   }

   public void onAttack() {
   }

   private void detectEquipmentUpdates() {
      Map<EquipmentSlot, ItemStack> map = this.collectEquipmentChanges();
      if (map != null) {
         this.handleHandSwap(map);
         if (!map.isEmpty()) {
            this.handleEquipmentChanges(map);
         }
      }
   }

   @Nullable
   private Map<EquipmentSlot, ItemStack> collectEquipmentChanges() {
      Map<EquipmentSlot, ItemStack> map = null;

      for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
         ItemStack itemStack = this.lastEquipmentItems.get(equipmentSlot);
         ItemStack itemStack2 = this.getItemBySlot(equipmentSlot);
         if (this.equipmentHasChanged(itemStack, itemStack2)) {
            if (map == null) {
               map = Maps.newEnumMap(EquipmentSlot.class);
            }

            map.put(equipmentSlot, itemStack2);
            AttributeMap attributeMap = this.getAttributes();
            if (!itemStack.isEmpty()) {
               this.stopLocationBasedEffects(itemStack, equipmentSlot, attributeMap);
            }
         }
      }

      if (map != null) {
         for (Entry<EquipmentSlot, ItemStack> entry : map.entrySet()) {
            EquipmentSlot equipmentSlot2 = entry.getKey();
            ItemStack itemStack2 = entry.getValue();
            if (!itemStack2.isEmpty() && !itemStack2.isBroken()) {
               itemStack2.forEachModifier(equipmentSlot2, (holder, attributeModifier) -> {
                  AttributeInstance attributeInstance = this.attributes.getInstance(holder);
                  if (attributeInstance != null) {
                     attributeInstance.removeModifier(attributeModifier.id());
                     attributeInstance.addTransientModifier(attributeModifier);
                  }
               });
               if (this.level() instanceof ServerLevel serverLevel) {
                  EnchantmentHelper.runLocationChangedEffects(serverLevel, itemStack2, this, equipmentSlot2);
               }
            }
         }
      }

      return map;
   }

   public boolean equipmentHasChanged(ItemStack oldItem, ItemStack newItem) {
      return !ItemStack.matches(newItem, oldItem);
   }

   private void handleHandSwap(Map<EquipmentSlot, ItemStack> hands) {
      ItemStack itemStack = hands.get(EquipmentSlot.MAINHAND);
      ItemStack itemStack2 = hands.get(EquipmentSlot.OFFHAND);
      if (itemStack != null
         && itemStack2 != null
         && ItemStack.matches(itemStack, this.lastEquipmentItems.get(EquipmentSlot.OFFHAND))
         && ItemStack.matches(itemStack2, this.lastEquipmentItems.get(EquipmentSlot.MAINHAND))) {
         ((ServerLevel)this.level()).getChunkSource().sendToTrackingPlayers(this, new ClientboundEntityEventPacket(this, (byte)55));
         hands.remove(EquipmentSlot.MAINHAND);
         hands.remove(EquipmentSlot.OFFHAND);
         this.lastEquipmentItems.put(EquipmentSlot.MAINHAND, itemStack.copy());
         this.lastEquipmentItems.put(EquipmentSlot.OFFHAND, itemStack2.copy());
      }
   }

   private void handleEquipmentChanges(Map<EquipmentSlot, ItemStack> equipments) {
      List<Pair<EquipmentSlot, ItemStack>> list = Lists.newArrayListWithCapacity(equipments.size());
      equipments.forEach((equipmentSlot, itemStack) -> {
         ItemStack itemStack2 = itemStack.copy();
         list.add(Pair.of(equipmentSlot, itemStack2));
         this.lastEquipmentItems.put(equipmentSlot, itemStack2);
      });
      ((ServerLevel)this.level()).getChunkSource().sendToTrackingPlayers(this, new ClientboundSetEquipmentPacket(this.getId(), list));
   }

   protected void tickHeadTurn(float yBodyRot) {
      float f = Mth.wrapDegrees(yBodyRot - this.yBodyRot);
      this.yBodyRot += f * 0.3F;
      float g = Mth.wrapDegrees(this.getYRot() - this.yBodyRot);
      float h = this.getMaxHeadRotationRelativeToBody();
      if (Math.abs(g) > h) {
         this.yBodyRot = this.yBodyRot + (g - Mth.sign(g) * h);
      }
   }

   protected float getMaxHeadRotationRelativeToBody() {
      return 50.0F;
   }

   public void aiStep() {
      if (this.noJumpDelay > 0) {
         this.noJumpDelay--;
      }

      if (this.isInterpolating()) {
         this.getInterpolation().interpolate();
      } else if (!this.canSimulateMovement()) {
         this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
      }

      if (this.lerpHeadSteps > 0) {
         this.lerpHeadRotationStep(this.lerpHeadSteps, this.lerpYHeadRot);
         this.lerpHeadSteps--;
      }

      this.equipment.tick(this);
      Vec3 vec3 = this.getDeltaMovement();
      double d = vec3.x;
      double e = vec3.y;
      double f = vec3.z;
      if (this.getType().equals(EntityType.PLAYER)) {
         if (vec3.horizontalDistanceSqr() < 9.0E-6) {
            d = 0.0;
            f = 0.0;
         }
      } else {
         if (Math.abs(vec3.x) < 0.003) {
            d = 0.0;
         }

         if (Math.abs(vec3.z) < 0.003) {
            f = 0.0;
         }
      }

      if (Math.abs(vec3.y) < 0.003) {
         e = 0.0;
      }

      this.setDeltaMovement(d, e, f);
      ProfilerFiller profilerFiller = Profiler.get();
      profilerFiller.push("ai");
      this.applyInput();
      if (this.isImmobile()) {
         this.jumping = false;
         this.xxa = 0.0F;
         this.zza = 0.0F;
      } else if (this.isEffectiveAi() && !this.level().isClientSide()) {
         profilerFiller.push("newAi");
         this.serverAiStep();
         profilerFiller.pop();
      }

      profilerFiller.pop();
      profilerFiller.push("jump");
      if (this.jumping && this.isAffectedByFluids()) {
         double g;
         if (this.isInLava()) {
            g = this.getFluidHeight(FluidTags.LAVA);
         } else {
            g = this.getFluidHeight(FluidTags.WATER);
         }

         boolean bl = this.isInWater() && g > 0.0;
         double h = this.getFluidJumpThreshold();
         if (!bl || this.onGround() && !(g > h)) {
            if (!this.isInLava() || this.onGround() && !(g > h)) {
               if ((this.onGround() || bl && g <= h) && this.noJumpDelay == 0) {
                  this.jumpFromGround();
                  this.noJumpDelay = 10;
               }
            } else {
               this.jumpInLiquid(FluidTags.LAVA);
            }
         } else {
            this.jumpInLiquid(FluidTags.WATER);
         }
      } else {
         this.noJumpDelay = 0;
      }

      profilerFiller.pop();
      profilerFiller.push("travel");
      if (this.isFallFlying()) {
         this.updateFallFlying();
      }

      AABB aABB = this.getBoundingBox();
      Vec3 vec32 = new Vec3(this.xxa, this.yya, this.zza);
      if (this.hasEffect(MobEffects.SLOW_FALLING) || this.hasEffect(MobEffects.LEVITATION)) {
         this.resetFallDistance();
      }

      if (this.getControllingPassenger() instanceof Player player && this.isAlive()) {
         this.travelRidden(player, vec32);
      } else if (this.canSimulateMovement() && this.isEffectiveAi()) {
         this.travel(vec32);
      }

      if (!this.level().isClientSide() || this.isLocalInstanceAuthoritative()) {
         this.applyEffectsFromBlocks();
      }

      if (this.level().isClientSide()) {
         this.calculateEntityAnimation(this instanceof FlyingAnimal);
      }

      profilerFiller.pop();
      if (this.level() instanceof ServerLevel serverLevel) {
         profilerFiller.push("freezing");
         if (!this.isInPowderSnow || !this.canFreeze()) {
            this.setTicksFrozen(Math.max(0, this.getTicksFrozen() - 2));
         }

         this.removeFrost();
         this.tryAddFrost();
         if (this.tickCount % 40 == 0 && this.isFullyFrozen() && this.canFreeze()) {
            this.hurtServer(serverLevel, this.damageSources().freeze(), 1.0F);
         }

         profilerFiller.pop();
      }

      profilerFiller.push("push");
      if (this.autoSpinAttackTicks > 0) {
         this.autoSpinAttackTicks--;
         this.checkAutoSpinAttack(aABB, this.getBoundingBox());
      }

      this.pushEntities();
      profilerFiller.pop();
      if (this.level() instanceof ServerLevel serverLevel && this.isSensitiveToWater() && this.isInWaterOrRain()) {
         this.hurtServer(serverLevel, this.damageSources().drown(), 1.0F);
      }
   }

   protected void applyInput() {
      this.xxa *= 0.98F;
      this.zza *= 0.98F;
   }

   public boolean isSensitiveToWater() {
      return false;
   }

   public boolean isJumping() {
      return this.jumping;
   }

   protected void updateFallFlying() {
      this.checkFallDistanceAccumulation();
      if (!this.level().isClientSide()) {
         if (!this.canGlide()) {
            this.setSharedFlag(7, false);
            return;
         }

         int i = this.fallFlyTicks + 1;
         if (i % 10 == 0) {
            int j = i / 10;
            if (j % 2 == 0) {
               List<EquipmentSlot> list = EquipmentSlot.VALUES
                  .stream()
                  .filter(equipmentSlotx -> canGlideUsing(this.getItemBySlot(equipmentSlotx), equipmentSlotx))
                  .toList();
               EquipmentSlot equipmentSlot = Util.getRandom(list, this.random);
               this.getItemBySlot(equipmentSlot).hurtAndBreak(1, this, equipmentSlot);
            }

            this.gameEvent(GameEvent.ELYTRA_GLIDE);
         }
      }
   }

   protected boolean canGlide() {
      if (!this.onGround() && !this.isPassenger() && !this.hasEffect(MobEffects.LEVITATION)) {
         for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
            if (canGlideUsing(this.getItemBySlot(equipmentSlot), equipmentSlot)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   protected void serverAiStep() {
   }

   protected void pushEntities() {
      List<Entity> list = this.level().getPushableEntities(this, this.getBoundingBox());
      if (!list.isEmpty()) {
         if (this.level() instanceof ServerLevel serverLevel) {
            int i = serverLevel.getGameRules().get(GameRules.MAX_ENTITY_CRAMMING);
            if (i > 0 && list.size() > i - 1 && this.random.nextInt(4) == 0) {
               int j = 0;

               for (Entity entity : list) {
                  if (!entity.isPassenger()) {
                     j++;
                  }
               }

               if (j > i - 1) {
                  this.hurtServer(serverLevel, this.damageSources().cramming(), 6.0F);
               }
            }
         }

         for (Entity entity2 : list) {
            this.doPush(entity2);
         }
      }
   }

   protected void checkAutoSpinAttack(AABB boundingBoxBeforeSpin, AABB boundingBoxAfterSpin) {
      AABB aABB = boundingBoxBeforeSpin.minmax(boundingBoxAfterSpin);
      List<Entity> list = this.level().getEntities(this, aABB);
      if (!list.isEmpty()) {
         for (Entity entity : list) {
            if (entity instanceof LivingEntity) {
               this.doAutoAttackOnTouch((LivingEntity)entity);
               this.autoSpinAttackTicks = 0;
               this.setDeltaMovement(this.getDeltaMovement().scale(-0.2));
               break;
            }
         }
      } else if (this.horizontalCollision) {
         this.autoSpinAttackTicks = 0;
      }

      if (!this.level().isClientSide() && this.autoSpinAttackTicks <= 0) {
         this.setLivingEntityFlag(4, false);
         this.autoSpinAttackDmg = 0.0F;
         this.autoSpinAttackItemStack = null;
      }
   }

   protected void doPush(Entity entity) {
      entity.push(this);
   }

   protected void doAutoAttackOnTouch(LivingEntity target) {
   }

   public boolean isAutoSpinAttack() {
      return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 4) != 0;
   }

   @Override
   public void stopRiding() {
      Entity entity = this.getVehicle();
      super.stopRiding();
      if (entity != null && entity != this.getVehicle() && !this.level().isClientSide()) {
         this.dismountVehicle(entity);
      }
   }

   @Override
   public void rideTick() {
      super.rideTick();
      this.resetFallDistance();
   }

   @Override
   public InterpolationHandler getInterpolation() {
      return this.interpolation;
   }

   @Override
   public void lerpHeadTo(float yaw, int pitch) {
      this.lerpYHeadRot = yaw;
      this.lerpHeadSteps = pitch;
   }

   public void setJumping(boolean jumping) {
      this.jumping = jumping;
   }

   public void onItemPickup(ItemEntity itemEntity) {
      Entity entity = itemEntity.getOwner();
      if (entity instanceof ServerPlayer) {
         CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_ENTITY.trigger((ServerPlayer)entity, itemEntity.getItem(), this);
      }
   }

   public void take(Entity entity, int amount) {
      if (!entity.isRemoved()
         && !this.level().isClientSide()
         && (entity instanceof ItemEntity || entity instanceof AbstractArrow || entity instanceof ExperienceOrb)) {
         ((ServerLevel)this.level()).getChunkSource().sendToTrackingPlayers(entity, new ClientboundTakeItemEntityPacket(entity.getId(), this.getId(), amount));
      }
   }

   public boolean hasLineOfSight(Entity entity) {
      return this.hasLineOfSight(entity, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity.getEyeY());
   }

   public boolean hasLineOfSight(Entity entity, ClipContext.Block block, ClipContext.Fluid fluid, double y) {
      if (entity.level() != this.level()) {
         return false;
      } else {
         Vec3 vec3 = new Vec3(this.getX(), this.getEyeY(), this.getZ());
         Vec3 vec32 = new Vec3(entity.getX(), y, entity.getZ());
         return vec32.distanceTo(vec3) > 128.0 ? false : this.level().clip(new ClipContext(vec3, vec32, block, fluid, this)).getType() == HitResult.Type.MISS;
      }
   }

   @Override
   public float getViewYRot(float partialTick) {
      return partialTick == 1.0F ? this.yHeadRot : Mth.rotLerp(partialTick, this.yHeadRotO, this.yHeadRot);
   }

   public float getAttackAnim(float partialTick) {
      float f = this.attackAnim - this.oAttackAnim;
      if (f < 0.0F) {
         f++;
      }

      return this.oAttackAnim + f * partialTick;
   }

   @Override
   public boolean isPickable() {
      return !this.isRemoved();
   }

   @Override
   public boolean isPushable() {
      return this.isAlive() && !this.isSpectator() && !this.onClimbable();
   }

   @Override
   public float getYHeadRot() {
      return this.yHeadRot;
   }

   @Override
   public void setYHeadRot(float yHeadRot) {
      this.yHeadRot = yHeadRot;
   }

   @Override
   public void setYBodyRot(float yBodyRot) {
      this.yBodyRot = yBodyRot;
   }

   @Override
   public Vec3 getRelativePortalPosition(Direction.Axis axis, BlockUtil.FoundRectangle rectangle) {
      return resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(axis, rectangle));
   }

   public static Vec3 resetForwardDirectionOfRelativePortalPosition(Vec3 relativePortalPosition) {
      return new Vec3(relativePortalPosition.x, relativePortalPosition.y, 0.0);
   }

   public float getAbsorptionAmount() {
      return this.absorptionAmount;
   }

   public final void setAbsorptionAmount(float absorptionAmount) {
      this.internalSetAbsorptionAmount(Mth.clamp(absorptionAmount, 0.0F, this.getMaxAbsorption()));
   }

   protected void internalSetAbsorptionAmount(float absorptionAmount) {
      this.absorptionAmount = absorptionAmount;
   }

   public void onEnterCombat() {
   }

   public void onLeaveCombat() {
   }

   protected void updateEffectVisibility() {
      this.effectsDirty = true;
   }

   public abstract HumanoidArm getMainArm();

   public boolean isUsingItem() {
      return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
   }

   public InteractionHand getUsedItemHand() {
      return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
   }

   private void updatingUsingItem() {
      if (this.isUsingItem()) {
         if (ItemStack.isSameItem(this.getItemInHand(this.getUsedItemHand()), this.useItem)) {
            this.useItem = this.getItemInHand(this.getUsedItemHand());
            this.updateUsingItem(this.useItem);
         } else {
            this.stopUsingItem();
         }
      }
   }

   @Nullable
   private ItemEntity createItemStackToDrop(ItemStack stack, boolean randomizeMotion, boolean includeThrower) {
      if (stack.isEmpty()) {
         return null;
      } else {
         double d = this.getEyeY() - 0.3F;
         ItemEntity itemEntity = new ItemEntity(this.level(), this.getX(), d, this.getZ(), stack);
         itemEntity.setPickUpDelay(40);
         if (includeThrower) {
            itemEntity.setThrower(this);
         }

         if (randomizeMotion) {
            float f = this.random.nextFloat() * 0.5F;
            float g = this.random.nextFloat() * (float) (Math.PI * 2);
            itemEntity.setDeltaMovement(-Mth.sin(g) * f, 0.2F, Mth.cos(g) * f);
         } else {
            float f = 0.3F;
            float g = Mth.sin(this.getXRot() * (float) (Math.PI / 180.0));
            float h = Mth.cos(this.getXRot() * (float) (Math.PI / 180.0));
            float i = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
            float j = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
            float k = this.random.nextFloat() * (float) (Math.PI * 2);
            float l = 0.02F * this.random.nextFloat();
            itemEntity.setDeltaMovement(
               -i * h * 0.3F + Math.cos(k) * l, -g * 0.3F + 0.1F + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F, j * h * 0.3F + Math.sin(k) * l
            );
         }

         return itemEntity;
      }
   }

   protected void updateUsingItem(ItemStack usingItem) {
      usingItem.onUseTick(this.level(), this, this.getUseItemRemainingTicks());
      if (--this.useItemRemaining == 0 && !this.level().isClientSide() && !usingItem.useOnRelease()) {
         this.completeUsingItem();
      }
   }

   private void updateSwimAmount() {
      this.swimAmountO = this.swimAmount;
      if (this.isVisuallySwimming()) {
         this.swimAmount = Math.min(1.0F, this.swimAmount + 0.09F);
      } else {
         this.swimAmount = Math.max(0.0F, this.swimAmount - 0.09F);
      }
   }

   protected void setLivingEntityFlag(int key, boolean value) {
      int i = this.entityData.get(DATA_LIVING_ENTITY_FLAGS);
      if (value) {
         i |= key;
      } else {
         i &= ~key;
      }

      this.entityData.set(DATA_LIVING_ENTITY_FLAGS, (byte)i);
   }

   public void startUsingItem(InteractionHand hand) {
      ItemStack itemStack = this.getItemInHand(hand);
      if (!itemStack.isEmpty() && !this.isUsingItem()) {
         this.useItem = itemStack;
         this.useItemRemaining = itemStack.getUseDuration(this);
         if (!this.level().isClientSide()) {
            this.setLivingEntityFlag(1, true);
            this.setLivingEntityFlag(2, hand == InteractionHand.OFF_HAND);
            this.useItem.causeUseVibration(this, GameEvent.ITEM_INTERACT_START);
            if (this.useItem.has(DataComponents.KINETIC_WEAPON)) {
               this.recentKineticEnemies = new Object2LongOpenHashMap();
            }
         }
      }
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> dataAccessor) {
      super.onSyncedDataUpdated(dataAccessor);
      if (SLEEPING_POS_ID.equals(dataAccessor)) {
         if (this.level().isClientSide()) {
            this.getSleepingPos().ifPresent(this::setPosToBed);
         }
      } else if (DATA_LIVING_ENTITY_FLAGS.equals(dataAccessor) && this.level().isClientSide()) {
         if (this.isUsingItem() && this.useItem.isEmpty()) {
            this.useItem = this.getItemInHand(this.getUsedItemHand());
            if (!this.useItem.isEmpty()) {
               this.useItemRemaining = this.useItem.getUseDuration(this);
            }
         } else if (!this.isUsingItem() && !this.useItem.isEmpty()) {
            this.useItem = ItemStack.EMPTY;
            this.useItemRemaining = 0;
         }
      }
   }

   @Override
   public void lookAt(EntityAnchorArgument.Anchor anchor, Vec3 target) {
      super.lookAt(anchor, target);
      this.yHeadRotO = this.yHeadRot;
      this.yBodyRot = this.yHeadRot;
      this.yBodyRotO = this.yBodyRot;
   }

   @Override
   public float getPreciseBodyRotation(float partialTick) {
      return Mth.lerp(partialTick, this.yBodyRotO, this.yBodyRot);
   }

   public void spawnItemParticles(ItemStack stack, int amount) {
      for (int i = 0; i < amount; i++) {
         Vec3 vec3 = new Vec3((this.random.nextFloat() - 0.5) * 0.1, this.random.nextFloat() * 0.1 + 0.1, 0.0);
         vec3 = vec3.xRot(-this.getXRot() * (float) (Math.PI / 180.0));
         vec3 = vec3.yRot(-this.getYRot() * (float) (Math.PI / 180.0));
         double d = -this.random.nextFloat() * 0.6 - 0.3;
         Vec3 vec32 = new Vec3((this.random.nextFloat() - 0.5) * 0.3, d, 0.6);
         vec32 = vec32.xRot(-this.getXRot() * (float) (Math.PI / 180.0));
         vec32 = vec32.yRot(-this.getYRot() * (float) (Math.PI / 180.0));
         vec32 = vec32.add(this.getX(), this.getEyeY(), this.getZ());
         this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack), vec32.x, vec32.y, vec32.z, vec3.x, vec3.y + 0.05, vec3.z);
      }
   }

   protected void completeUsingItem() {
      if (!this.level().isClientSide() || this.isUsingItem()) {
         InteractionHand interactionHand = this.getUsedItemHand();
         if (!this.useItem.equals(this.getItemInHand(interactionHand))) {
            this.releaseUsingItem();
         } else {
            if (!this.useItem.isEmpty() && this.isUsingItem()) {
               ItemStack itemStack = this.useItem.finishUsingItem(this.level(), this);
               if (itemStack != this.useItem) {
                  this.setItemInHand(interactionHand, itemStack);
               }

               this.stopUsingItem();
            }
         }
      }
   }

   public void handleExtraItemsCreatedOnUse(ItemStack stack) {
   }

   public ItemStack getUseItem() {
      return this.useItem;
   }

   public int getUseItemRemainingTicks() {
      return this.useItemRemaining;
   }

   public int getTicksUsingItem() {
      return this.isUsingItem() ? this.useItem.getUseDuration(this) - this.getUseItemRemainingTicks() : 0;
   }

   public float getTicksUsingItem(float partialTick) {
      return !this.isUsingItem() ? 0.0F : this.getTicksUsingItem() + partialTick;
   }

   public void releaseUsingItem() {
      ItemStack itemStack = this.getItemInHand(this.getUsedItemHand());
      if (!this.useItem.isEmpty() && ItemStack.isSameItem(itemStack, this.useItem)) {
         this.useItem = itemStack;
         this.useItem.releaseUsing(this.level(), this, this.getUseItemRemainingTicks());
         if (this.useItem.useOnRelease()) {
            this.updatingUsingItem();
         }
      }

      this.stopUsingItem();
   }

   public void stopUsingItem() {
      if (!this.level().isClientSide()) {
         boolean bl = this.isUsingItem();
         this.recentKineticEnemies = null;
         this.setLivingEntityFlag(1, false);
         if (bl) {
            this.useItem.causeUseVibration(this, GameEvent.ITEM_INTERACT_FINISH);
         }
      }

      this.useItem = ItemStack.EMPTY;
      this.useItemRemaining = 0;
   }

   public boolean isBlocking() {
      return this.getItemBlockingWith() != null;
   }

   @Nullable
   public ItemStack getItemBlockingWith() {
      if (!this.isUsingItem()) {
         return null;
      } else {
         BlocksAttacks blocksAttacks = this.useItem.get(DataComponents.BLOCKS_ATTACKS);
         if (blocksAttacks != null) {
            int i = this.useItem.getItem().getUseDuration(this.useItem, this) - this.useItemRemaining;
            if (i >= blocksAttacks.blockDelayTicks()) {
               return this.useItem;
            }
         }

         return null;
      }
   }

   public boolean isSuppressingSlidingDownLadder() {
      return this.isShiftKeyDown();
   }

   public boolean isFallFlying() {
      return this.getSharedFlag(7);
   }

   @Override
   public boolean isVisuallySwimming() {
      return super.isVisuallySwimming() || !this.isFallFlying() && this.hasPose(Pose.FALL_FLYING);
   }

   public int getFallFlyingTicks() {
      return this.fallFlyTicks;
   }

   public boolean randomTeleport(double x, double y, double z, boolean broadcastTeleport) {
      double d = this.getX();
      double e = this.getY();
      double f = this.getZ();
      double g = y;
      boolean bl = false;
      BlockPos blockPos = BlockPos.containing(x, y, z);
      Level level = this.level();
      if (level.hasChunkAt(blockPos)) {
         boolean bl2 = false;

         while (!bl2 && blockPos.getY() > level.getMinY()) {
            BlockPos blockPos2 = blockPos.below();
            BlockState blockState = level.getBlockState(blockPos2);
            if (blockState.blocksMotion()) {
               bl2 = true;
            } else {
               g--;
               blockPos = blockPos2;
            }
         }

         if (bl2) {
            this.teleportTo(x, g, z);
            if (level.noCollision(this) && !level.containsAnyLiquid(this.getBoundingBox())) {
               bl = true;
            }
         }
      }

      if (!bl) {
         this.teleportTo(d, e, f);
         return false;
      } else {
         if (broadcastTeleport) {
            level.broadcastEntityEvent(this, (byte)46);
         }

         if (this instanceof PathfinderMob pathfinderMob) {
            pathfinderMob.getNavigation().stop();
         }

         return true;
      }
   }

   public boolean isAffectedByPotions() {
      return !this.isDeadOrDying();
   }

   public boolean attackable() {
      return true;
   }

   public void setRecordPlayingNearby(BlockPos jukebox, boolean partyParrot) {
   }

   public boolean canPickUpLoot() {
      return false;
   }

   @Override
   public final EntityDimensions getDimensions(Pose pose) {
      return pose == Pose.SLEEPING ? SLEEPING_DIMENSIONS : this.getDefaultDimensions(pose).scale(this.getScale());
   }

   protected EntityDimensions getDefaultDimensions(Pose pose) {
      return this.getType().getDimensions().scale(this.getAgeScale());
   }

   public ImmutableList<Pose> getDismountPoses() {
      return ImmutableList.of(Pose.STANDING);
   }

   public AABB getLocalBoundsForPose(Pose pose) {
      EntityDimensions entityDimensions = this.getDimensions(pose);
      return new AABB(
         -entityDimensions.width() / 2.0F,
         0.0,
         -entityDimensions.width() / 2.0F,
         entityDimensions.width() / 2.0F,
         entityDimensions.height(),
         entityDimensions.width() / 2.0F
      );
   }

   protected boolean wouldNotSuffocateAtTargetPose(Pose pose) {
      AABB aABB = this.getDimensions(pose).makeBoundingBox(this.position());
      return this.level().noBlockCollision(this, aABB);
   }

   @Override
   public boolean canUsePortal(boolean allowPassengers) {
      return super.canUsePortal(allowPassengers) && !this.isSleeping();
   }

   public Optional<BlockPos> getSleepingPos() {
      return this.entityData.get(SLEEPING_POS_ID);
   }

   public void setSleepingPos(BlockPos pos) {
      this.entityData.set(SLEEPING_POS_ID, Optional.of(pos));
   }

   public void clearSleepingPos() {
      this.entityData.set(SLEEPING_POS_ID, Optional.empty());
   }

   public boolean isSleeping() {
      return this.getSleepingPos().isPresent();
   }

   public void startSleeping(BlockPos pos) {
      if (this.isPassenger()) {
         this.stopRiding();
      }

      BlockState blockState = this.level().getBlockState(pos);
      if (blockState.getBlock() instanceof BedBlock) {
         this.level().setBlock(pos, blockState.setValue(BedBlock.OCCUPIED, true), 3);
      }

      this.setPose(Pose.SLEEPING);
      this.setPosToBed(pos);
      this.setSleepingPos(pos);
      this.setDeltaMovement(Vec3.ZERO);
      this.needsSync = true;
   }

   private void setPosToBed(BlockPos pos) {
      this.setPos(pos.getX() + 0.5, pos.getY() + 0.6875, pos.getZ() + 0.5);
   }

   private boolean checkBedExists() {
      return this.getSleepingPos().map(blockPos -> this.level().getBlockState(blockPos).getBlock() instanceof BedBlock).orElse(false);
   }

   public void stopSleeping() {
      this.getSleepingPos().filter(this.level()::hasChunkAt).ifPresent(blockPos -> {
         BlockState blockState = this.level().getBlockState(blockPos);
         if (blockState.getBlock() instanceof BedBlock) {
            Direction direction = blockState.getValue(BedBlock.FACING);
            this.level().setBlock(blockPos, blockState.setValue(BedBlock.OCCUPIED, false), 3);
            Vec3 vec3x = BedBlock.findStandUpPosition(this.getType(), this.level(), blockPos, direction, this.getYRot()).orElseGet(() -> {
               BlockPos blockPos2 = blockPos.above();
               return new Vec3(blockPos2.getX() + 0.5, blockPos2.getY() + 0.1, blockPos2.getZ() + 0.5);
            });
            Vec3 vec32 = Vec3.atBottomCenterOf(blockPos).subtract(vec3x).normalize();
            float f = (float)Mth.wrapDegrees(Mth.atan2(vec32.z, vec32.x) * 180.0F / (float)Math.PI - 90.0);
            this.setPos(vec3x.x, vec3x.y, vec3x.z);
            this.setYRot(f);
            this.setXRot(0.0F);
         }
      });
      Vec3 vec3 = this.position();
      this.setPose(Pose.STANDING);
      this.setPos(vec3.x, vec3.y, vec3.z);
      this.clearSleepingPos();
   }

   @Nullable
   public Direction getBedOrientation() {
      BlockPos blockPos = this.getSleepingPos().orElse(null);
      return blockPos != null ? BedBlock.getBedOrientation(this.level(), blockPos) : null;
   }

   @Override
   public boolean isInWall() {
      return !this.isSleeping() && super.isInWall();
   }

   public ItemStack getProjectile(ItemStack weaponStack) {
      return ItemStack.EMPTY;
   }

   private static byte entityEventForEquipmentBreak(EquipmentSlot slot) {
      return switch (slot) {
         case MAINHAND -> 47;
         case OFFHAND -> 48;
         case HEAD -> 49;
         case CHEST -> 50;
         case FEET -> 52;
         case LEGS -> 51;
         case BODY -> 65;
         case SADDLE -> 68;
      };
   }

   public void onEquippedItemBroken(Item item, EquipmentSlot slot) {
      this.level().broadcastEntityEvent(this, entityEventForEquipmentBreak(slot));
      this.stopLocationBasedEffects(this.getItemBySlot(slot), slot, this.attributes);
   }

   private void stopLocationBasedEffects(ItemStack stack, EquipmentSlot slot, AttributeMap attributeMap) {
      stack.forEachModifier(slot, (holder, attributeModifier) -> {
         AttributeInstance attributeInstance = attributeMap.getInstance(holder);
         if (attributeInstance != null) {
            attributeInstance.removeModifier(attributeModifier);
         }
      });
      EnchantmentHelper.stopLocationBasedEffects(stack, this, slot);
   }

   public final boolean canEquipWithDispenser(ItemStack stack) {
      if (this.isAlive() && !this.isSpectator()) {
         Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
         if (equippable != null && equippable.dispensable()) {
            EquipmentSlot equipmentSlot = equippable.slot();
            return this.canUseSlot(equipmentSlot) && equippable.canBeEquippedBy(this.getType())
               ? this.getItemBySlot(equipmentSlot).isEmpty() && this.canDispenserEquipIntoSlot(equipmentSlot)
               : false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot) {
      return true;
   }

   public final EquipmentSlot getEquipmentSlotForItem(ItemStack stack) {
      Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
      return equippable != null && this.canUseSlot(equippable.slot()) ? equippable.slot() : EquipmentSlot.MAINHAND;
   }

   public final boolean isEquippableInSlot(ItemStack stack, EquipmentSlot slot) {
      Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
      return equippable == null
         ? slot == EquipmentSlot.MAINHAND && this.canUseSlot(EquipmentSlot.MAINHAND)
         : slot == equippable.slot() && this.canUseSlot(equippable.slot()) && equippable.canBeEquippedBy(this.getType());
   }

   private static SlotAccess createEquipmentSlotAccess(LivingEntity entity, EquipmentSlot slot) {
      return slot != EquipmentSlot.HEAD && slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND
         ? SlotAccess.forEquipmentSlot(entity, slot, itemStack -> itemStack.isEmpty() || entity.getEquipmentSlotForItem(itemStack) == slot)
         : SlotAccess.forEquipmentSlot(entity, slot);
   }

   @Nullable
   private static EquipmentSlot getEquipmentSlot(int index) {
      if (index == 100 + EquipmentSlot.HEAD.getIndex()) {
         return EquipmentSlot.HEAD;
      } else if (index == 100 + EquipmentSlot.CHEST.getIndex()) {
         return EquipmentSlot.CHEST;
      } else if (index == 100 + EquipmentSlot.LEGS.getIndex()) {
         return EquipmentSlot.LEGS;
      } else if (index == 100 + EquipmentSlot.FEET.getIndex()) {
         return EquipmentSlot.FEET;
      } else if (index == 98) {
         return EquipmentSlot.MAINHAND;
      } else if (index == 99) {
         return EquipmentSlot.OFFHAND;
      } else if (index == 105) {
         return EquipmentSlot.BODY;
      } else {
         return index == 106 ? EquipmentSlot.SADDLE : null;
      }
   }

   @Nullable
   @Override
   public SlotAccess getSlot(int index) {
      EquipmentSlot equipmentSlot = getEquipmentSlot(index);
      return equipmentSlot != null ? createEquipmentSlotAccess(this, equipmentSlot) : super.getSlot(index);
   }

   @Override
   public boolean canFreeze() {
      if (this.isSpectator()) {
         return false;
      } else {
         for (EquipmentSlot equipmentSlot : EquipmentSlotGroup.ARMOR) {
            if (this.getItemBySlot(equipmentSlot).is(ItemTags.FREEZE_IMMUNE_WEARABLES)) {
               return false;
            }
         }

         return super.canFreeze();
      }
   }

   @Override
   public boolean isCurrentlyGlowing() {
      return !this.level().isClientSide() && this.hasEffect(MobEffects.GLOWING) || super.isCurrentlyGlowing();
   }

   @Override
   public float getVisualRotationYInDegrees() {
      return this.yBodyRot;
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket packet) {
      double d = packet.getX();
      double e = packet.getY();
      double f = packet.getZ();
      float g = packet.getYRot();
      float h = packet.getXRot();
      this.syncPacketPositionCodec(d, e, f);
      this.yBodyRot = packet.getYHeadRot();
      this.yHeadRot = packet.getYHeadRot();
      this.yBodyRotO = this.yBodyRot;
      this.yHeadRotO = this.yHeadRot;
      this.setId(packet.getId());
      this.setUUID(packet.getUUID());
      this.absSnapTo(d, e, f, g, h);
      this.setDeltaMovement(packet.getMovement());
   }

   public float getSecondsToDisableBlocking() {
      ItemStack itemStack = this.getWeaponItem();
      Weapon weapon = itemStack.get(DataComponents.WEAPON);
      return weapon != null && itemStack == this.getActiveItem() ? weapon.disableBlockingForSeconds() : 0.0F;
   }

   @Override
   public float maxUpStep() {
      float f = (float)this.getAttributeValue(Attributes.STEP_HEIGHT);
      return this.getControllingPassenger() instanceof Player ? Math.max(f, 1.0F) : f;
   }

   @Override
   public Vec3 getPassengerRidingPosition(Entity entity) {
      return this.position().add(this.getPassengerAttachmentPoint(entity, this.getDimensions(this.getPose()), this.getScale() * this.getAgeScale()));
   }

   protected void lerpHeadRotationStep(int lerpHeadSteps, double lerpYHeadRot) {
      this.yHeadRot = (float)Mth.rotLerp(1.0 / lerpHeadSteps, (double)this.yHeadRot, lerpYHeadRot);
   }

   @Override
   public void igniteForTicks(int ticks) {
      super.igniteForTicks(Mth.ceil(ticks * this.getAttributeValue(Attributes.BURNING_TIME)));
   }

   public boolean hasInfiniteMaterials() {
      return false;
   }

   public boolean isInvulnerableTo(ServerLevel level, DamageSource damageSource) {
      return this.isInvulnerableToBase(damageSource) || EnchantmentHelper.isImmuneToDamage(level, this, damageSource);
   }

   public static boolean canGlideUsing(ItemStack stack, EquipmentSlot slot) {
      if (!stack.has(DataComponents.GLIDER)) {
         return false;
      } else {
         Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
         return equippable != null && slot == equippable.slot() && !stack.nextDamageWillBreak();
      }
   }

   @VisibleForTesting
   public int getLastHurtByPlayerMemoryTime() {
      return this.lastHurtByPlayerMemoryTime;
   }

   @Override
   public boolean isTransmittingWaypoint() {
      return this.getAttributeValue(Attributes.WAYPOINT_TRANSMIT_RANGE) > 0.0;
   }

   @Override
   public Optional<WaypointTransmitter.Connection> makeWaypointConnectionWith(ServerPlayer player) {
      if (this.firstTick || player == this) {
         return Optional.empty();
      } else if (WaypointTransmitter.doesSourceIgnoreReceiver(this, player)) {
         return Optional.empty();
      } else {
         Waypoint.Icon icon = this.locatorBarIcon.cloneAndAssignStyle(this);
         if (WaypointTransmitter.isReallyFar(this, player)) {
            return Optional.of(new WaypointTransmitter.EntityAzimuthConnection(this, icon, player));
         } else {
            return !WaypointTransmitter.isChunkVisible(this.chunkPosition(), player)
               ? Optional.of(new WaypointTransmitter.EntityChunkConnection(this, icon, player))
               : Optional.of(new WaypointTransmitter.EntityBlockConnection(this, icon, player));
         }
      }
   }

   @Override
   public Waypoint.Icon waypointIcon() {
      return this.locatorBarIcon;
   }

   public record Fallsounds(SoundEvent small, SoundEvent big) {
   }
}
