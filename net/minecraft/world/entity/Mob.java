package net.minecraft.world.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.debug.DebugBrainDump;
import net.minecraft.util.debug.DebugGoalInfo;
import net.minecraft.util.debug.DebugPathInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.providers.VanillaEnchantmentProviders;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ContainerSingleItem;
import org.jspecify.annotations.Nullable;

public abstract class Mob extends LivingEntity implements EquipmentUser, Leashable, Targeting {
   private static final EntityDataAccessor<Byte> DATA_MOB_FLAGS_ID = SynchedEntityData.defineId(Mob.class, EntityDataSerializers.BYTE);
   private static final int MOB_FLAG_NO_AI = 1;
   private static final int MOB_FLAG_LEFTHANDED = 2;
   private static final int MOB_FLAG_AGGRESSIVE = 4;
   protected static final int PICKUP_REACH = 1;
   private static final Vec3i ITEM_PICKUP_REACH = new Vec3i(1, 0, 1);
   private static final List<EquipmentSlot> EQUIPMENT_POPULATION_ORDER = List.of(
      EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
   );
   public static final float MAX_WEARING_ARMOR_CHANCE = 0.15F;
   public static final float WEARING_ARMOR_UPGRADE_MATERIAL_CHANCE = 0.1087F;
   public static final float WEARING_ARMOR_UPGRADE_MATERIAL_ATTEMPTS = 3.0F;
   public static final float MAX_PICKUP_LOOT_CHANCE = 0.55F;
   public static final float MAX_ENCHANTED_ARMOR_CHANCE = 0.5F;
   public static final float MAX_ENCHANTED_WEAPON_CHANCE = 0.25F;
   public static final int UPDATE_GOAL_SELECTOR_EVERY_N_TICKS = 2;
   private static final double DEFAULT_ATTACK_REACH = Math.sqrt(2.04F) - 0.6F;
   private static final boolean DEFAULT_CAN_PICK_UP_LOOT = false;
   private static final boolean DEFAULT_PERSISTENCE_REQUIRED = false;
   private static final boolean DEFAULT_LEFT_HANDED = false;
   private static final boolean DEFAULT_NO_AI = false;
   protected static final Identifier RANDOM_SPAWN_BONUS_ID = Identifier.withDefaultNamespace("random_spawn_bonus");
   public static final String TAG_DROP_CHANCES = "drop_chances";
   public static final String TAG_LEFT_HANDED = "LeftHanded";
   public static final String TAG_CAN_PICK_UP_LOOT = "CanPickUpLoot";
   public static final String TAG_NO_AI = "NoAI";
   public int ambientSoundTime;
   protected int xpReward;
   protected LookControl lookControl;
   protected MoveControl moveControl;
   protected JumpControl jumpControl;
   private final BodyRotationControl bodyRotationControl;
   protected PathNavigation navigation;
   protected final GoalSelector goalSelector;
   protected final GoalSelector targetSelector;
   @Nullable
   private LivingEntity target;
   private final Sensing sensing;
   private DropChances dropChances = DropChances.DEFAULT;
   private boolean canPickUpLoot = false;
   private boolean persistenceRequired = false;
   private final Map<PathType, Float> pathfindingMalus = Maps.newEnumMap(PathType.class);
   private Optional<ResourceKey<LootTable>> lootTable = Optional.empty();
   private long lootTableSeed;
   @Nullable
   private Leashable.LeashData leashData;
   private BlockPos homePosition = BlockPos.ZERO;
   private int homeRadius = -1;

   protected Mob(EntityType<? extends Mob> entityType, Level level) {
      super(entityType, level);
      this.goalSelector = new GoalSelector();
      this.targetSelector = new GoalSelector();
      this.lookControl = new LookControl(this);
      this.moveControl = new MoveControl(this);
      this.jumpControl = new JumpControl(this);
      this.bodyRotationControl = this.createBodyControl();
      this.navigation = this.createNavigation(level);
      this.sensing = new Sensing(this);
      if (level instanceof ServerLevel) {
         this.registerGoals();
      }
   }

   protected void registerGoals() {
   }

   public static AttributeSupplier.Builder createMobAttributes() {
      return LivingEntity.createLivingAttributes().add(Attributes.FOLLOW_RANGE, 16.0);
   }

   protected PathNavigation createNavigation(Level level) {
      return new GroundPathNavigation(this, level);
   }

   protected boolean shouldPassengersInheritMalus() {
      return false;
   }

   public float getPathfindingMalus(PathType pathType) {
      Mob mob2;
      if (this.getControlledVehicle() instanceof Mob mob && mob.shouldPassengersInheritMalus()) {
         mob2 = mob;
      } else {
         mob2 = this;
      }

      Float float_ = mob2.pathfindingMalus.get(pathType);
      return float_ == null ? pathType.getMalus() : float_;
   }

   public void setPathfindingMalus(PathType pathType, float malus) {
      this.pathfindingMalus.put(pathType, malus);
   }

   public void onPathfindingStart() {
   }

   public void onPathfindingDone() {
   }

   protected BodyRotationControl createBodyControl() {
      return new BodyRotationControl(this);
   }

   public LookControl getLookControl() {
      return this.lookControl;
   }

   public MoveControl getMoveControl() {
      return this.getControlledVehicle() instanceof Mob mob ? mob.getMoveControl() : this.moveControl;
   }

   public JumpControl getJumpControl() {
      return this.jumpControl;
   }

   public PathNavigation getNavigation() {
      return this.getControlledVehicle() instanceof Mob mob ? mob.getNavigation() : this.navigation;
   }

   @Nullable
   @Override
   public LivingEntity getControllingPassenger() {
      Entity entity = this.getFirstPassenger();
      return !this.isNoAi() && entity instanceof Mob mob && entity.canControlVehicle() ? mob : null;
   }

   public Sensing getSensing() {
      return this.sensing;
   }

   @Nullable
   @Override
   public LivingEntity getTarget() {
      return this.target;
   }

   @Nullable
   protected final LivingEntity getTargetFromBrain() {
      return this.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
   }

   public void setTarget(@Nullable LivingEntity target) {
      this.target = target;
   }

   @Override
   public boolean canAttackType(EntityType<?> entityType) {
      return entityType != EntityType.GHAST;
   }

   public boolean canUseNonMeleeWeapon(ItemStack stack) {
      return false;
   }

   public void ate() {
      this.gameEvent(GameEvent.EAT);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_MOB_FLAGS_ID, (byte)0);
   }

   public int getAmbientSoundInterval() {
      return 80;
   }

   public void playAmbientSound() {
      this.makeSound(this.getAmbientSound());
   }

   @Override
   public void baseTick() {
      super.baseTick();
      ProfilerFiller profilerFiller = Profiler.get();
      profilerFiller.push("mobBaseTick");
      if (this.isAlive() && this.random.nextInt(1000) < this.ambientSoundTime++) {
         this.resetAmbientSoundTime();
         this.playAmbientSound();
      }

      profilerFiller.pop();
   }

   @Override
   protected void playHurtSound(DamageSource source) {
      this.resetAmbientSoundTime();
      super.playHurtSound(source);
   }

   private void resetAmbientSoundTime() {
      this.ambientSoundTime = -this.getAmbientSoundInterval();
   }

   @Override
   protected int getBaseExperienceReward(ServerLevel level) {
      if (this.xpReward > 0) {
         int i = this.xpReward;

         for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
            if (equipmentSlot.canIncreaseExperience()) {
               ItemStack itemStack = this.getItemBySlot(equipmentSlot);
               if (!itemStack.isEmpty() && this.dropChances.byEquipment(equipmentSlot) <= 1.0F) {
                  i += 1 + this.random.nextInt(3);
               }
            }
         }

         return i;
      } else {
         return this.xpReward;
      }
   }

   public void spawnAnim() {
      if (this.level().isClientSide()) {
         this.makePoofParticles();
      } else {
         this.level().broadcastEntityEvent(this, (byte)20);
      }
   }

   @Override
   public void handleEntityEvent(byte id) {
      if (id == 20) {
         this.spawnAnim();
      } else {
         super.handleEntityEvent(id);
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && this.tickCount % 5 == 0) {
         this.updateControlFlags();
      }
   }

   protected void updateControlFlags() {
      boolean bl = !(this.getControllingPassenger() instanceof Mob);
      boolean bl2 = !(this.getVehicle() instanceof AbstractBoat);
      this.goalSelector.setControlFlag(Goal.Flag.MOVE, bl);
      this.goalSelector.setControlFlag(Goal.Flag.JUMP, bl && bl2);
      this.goalSelector.setControlFlag(Goal.Flag.LOOK, bl);
   }

   @Override
   protected void tickHeadTurn(float yBodyRot) {
      this.bodyRotationControl.clientTick();
   }

   @Nullable
   protected SoundEvent getAmbientSound() {
      return null;
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putBoolean("CanPickUpLoot", this.canPickUpLoot());
      output.putBoolean("PersistenceRequired", this.persistenceRequired);
      if (!this.dropChances.equals(DropChances.DEFAULT)) {
         output.store("drop_chances", DropChances.CODEC, this.dropChances);
      }

      this.writeLeashData(output, this.leashData);
      if (this.hasHome()) {
         output.putInt("home_radius", this.homeRadius);
         output.store("home_pos", BlockPos.CODEC, this.homePosition);
      }

      output.putBoolean("LeftHanded", this.isLeftHanded());
      this.lootTable.ifPresent(resourceKey -> output.store("DeathLootTable", LootTable.KEY_CODEC, resourceKey));
      if (this.lootTableSeed != 0L) {
         output.putLong("DeathLootTableSeed", this.lootTableSeed);
      }

      if (this.isNoAi()) {
         output.putBoolean("NoAI", this.isNoAi());
      }
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      this.setCanPickUpLoot(input.getBooleanOr("CanPickUpLoot", false));
      this.persistenceRequired = input.getBooleanOr("PersistenceRequired", false);
      this.dropChances = input.<DropChances>read("drop_chances", DropChances.CODEC).orElse(DropChances.DEFAULT);
      this.readLeashData(input);
      this.homeRadius = input.getIntOr("home_radius", -1);
      if (this.homeRadius >= 0) {
         this.homePosition = input.<BlockPos>read("home_pos", BlockPos.CODEC).orElse(BlockPos.ZERO);
      }

      this.setLeftHanded(input.getBooleanOr("LeftHanded", false));
      this.lootTable = input.read("DeathLootTable", LootTable.KEY_CODEC);
      this.lootTableSeed = input.getLongOr("DeathLootTableSeed", 0L);
      this.setNoAi(input.getBooleanOr("NoAI", false));
   }

   @Override
   protected void dropFromLootTable(ServerLevel level, DamageSource damageSource, boolean playerKill) {
      super.dropFromLootTable(level, damageSource, playerKill);
      this.lootTable = Optional.empty();
   }

   @Override
   public final Optional<ResourceKey<LootTable>> getLootTable() {
      return this.lootTable.isPresent() ? this.lootTable : super.getLootTable();
   }

   @Override
   public long getLootTableSeed() {
      return this.lootTableSeed;
   }

   public void setZza(float amount) {
      this.zza = amount;
   }

   public void setYya(float amount) {
      this.yya = amount;
   }

   public void setXxa(float amount) {
      this.xxa = amount;
   }

   @Override
   public void setSpeed(float speed) {
      super.setSpeed(speed);
      this.setZza(speed);
   }

   public void stopInPlace() {
      this.getNavigation().stop();
      this.setXxa(0.0F);
      this.setYya(0.0F);
      this.setSpeed(0.0F);
      this.setDeltaMovement(0.0, 0.0, 0.0);
      this.resetAngularLeashMomentum();
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (this.getType().is(EntityTypeTags.BURN_IN_DAYLIGHT)) {
         this.burnUndead();
      }

      ProfilerFiller profilerFiller = Profiler.get();
      profilerFiller.push("looting");
      if (this.level() instanceof ServerLevel serverLevel
         && this.canPickUpLoot()
         && this.isAlive()
         && !this.dead
         && serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
         Vec3i vec3i = this.getPickupReach();

         for (ItemEntity itemEntity : this.level()
            .getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(vec3i.getX(), vec3i.getY(), vec3i.getZ()))) {
            if (!itemEntity.isRemoved()
               && !itemEntity.getItem().isEmpty()
               && !itemEntity.hasPickUpDelay()
               && this.wantsToPickUp(serverLevel, itemEntity.getItem())) {
               this.pickUpItem(serverLevel, itemEntity);
            }
         }
      }

      profilerFiller.pop();
   }

   protected EquipmentSlot sunProtectionSlot() {
      return EquipmentSlot.HEAD;
   }

   private void burnUndead() {
      if (this.isAlive() && this.isSunBurnTick()) {
         EquipmentSlot equipmentSlot = this.sunProtectionSlot();
         ItemStack itemStack = this.getItemBySlot(equipmentSlot);
         if (!itemStack.isEmpty()) {
            if (itemStack.isDamageableItem()) {
               Item item = itemStack.getItem();
               itemStack.setDamageValue(itemStack.getDamageValue() + this.random.nextInt(2));
               if (itemStack.getDamageValue() >= itemStack.getMaxDamage()) {
                  this.onEquippedItemBroken(item, equipmentSlot);
                  this.setItemSlot(equipmentSlot, ItemStack.EMPTY);
               }
            }
         } else {
            this.igniteForSeconds(8.0F);
         }
      }
   }

   private boolean isSunBurnTick() {
      if (!this.level().isClientSide() && this.level().environmentAttributes().getValue(EnvironmentAttributes.MONSTERS_BURN, this.position())) {
         float f = this.getLightLevelDependentMagicValue();
         BlockPos blockPos = BlockPos.containing(this.getX(), this.getEyeY(), this.getZ());
         boolean bl = this.isInWaterOrRain() || this.isInPowderSnow || this.wasInPowderSnow;
         if (f > 0.5F && this.random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F && !bl && this.level().canSeeSky(blockPos)) {
            return true;
         }
      }

      return false;
   }

   protected Vec3i getPickupReach() {
      return ITEM_PICKUP_REACH;
   }

   protected void pickUpItem(ServerLevel level, ItemEntity entity) {
      ItemStack itemStack = entity.getItem();
      ItemStack itemStack2 = this.equipItemIfPossible(level, itemStack.copy());
      if (!itemStack2.isEmpty()) {
         this.onItemPickup(entity);
         this.take(entity, itemStack2.getCount());
         itemStack.shrink(itemStack2.getCount());
         if (itemStack.isEmpty()) {
            entity.discard();
         }
      }
   }

   public ItemStack equipItemIfPossible(ServerLevel level, ItemStack stack) {
      EquipmentSlot equipmentSlot = this.getEquipmentSlotForItem(stack);
      if (!this.isEquippableInSlot(stack, equipmentSlot)) {
         return ItemStack.EMPTY;
      } else {
         ItemStack itemStack = this.getItemBySlot(equipmentSlot);
         boolean bl = this.canReplaceCurrentItem(stack, itemStack, equipmentSlot);
         if (equipmentSlot.isArmor() && !bl) {
            equipmentSlot = EquipmentSlot.MAINHAND;
            itemStack = this.getItemBySlot(equipmentSlot);
            bl = itemStack.isEmpty();
         }

         if (bl && this.canHoldItem(stack)) {
            double d = this.dropChances.byEquipment(equipmentSlot);
            if (!itemStack.isEmpty() && Math.max(this.random.nextFloat() - 0.1F, 0.0F) < d) {
               this.spawnAtLocation(level, itemStack);
            }

            ItemStack itemStack2 = equipmentSlot.limit(stack);
            this.setItemSlotAndDropWhenKilled(equipmentSlot, itemStack2);
            return itemStack2;
         } else {
            return ItemStack.EMPTY;
         }
      }
   }

   protected void setItemSlotAndDropWhenKilled(EquipmentSlot slot, ItemStack stack) {
      this.setItemSlot(slot, stack);
      this.setGuaranteedDrop(slot);
      this.persistenceRequired = true;
   }

   protected boolean canShearEquipment(Player player) {
      return !this.isVehicle();
   }

   public void setGuaranteedDrop(EquipmentSlot slot) {
      this.dropChances = this.dropChances.withGuaranteedDrop(slot);
   }

   protected boolean canReplaceCurrentItem(ItemStack newItem, ItemStack currentItem, EquipmentSlot slot) {
      if (currentItem.isEmpty()) {
         return true;
      } else if (slot.isArmor()) {
         return this.compareArmor(newItem, currentItem, slot);
      } else {
         return slot == EquipmentSlot.MAINHAND ? this.compareWeapons(newItem, currentItem, slot) : false;
      }
   }

   private boolean compareArmor(ItemStack newItem, ItemStack currentItem, EquipmentSlot slot) {
      if (EnchantmentHelper.has(currentItem, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
         return false;
      } else {
         double d = this.getApproximateAttributeWith(newItem, Attributes.ARMOR, slot);
         double e = this.getApproximateAttributeWith(currentItem, Attributes.ARMOR, slot);
         double f = this.getApproximateAttributeWith(newItem, Attributes.ARMOR_TOUGHNESS, slot);
         double g = this.getApproximateAttributeWith(currentItem, Attributes.ARMOR_TOUGHNESS, slot);
         if (d != e) {
            return d > e;
         } else {
            return f != g ? f > g : this.canReplaceEqualItem(newItem, currentItem);
         }
      }
   }

   private boolean compareWeapons(ItemStack newItem, ItemStack currentItem, EquipmentSlot slot) {
      TagKey<Item> tagKey = this.getPreferredWeaponType();
      if (tagKey != null) {
         if (currentItem.is(tagKey) && !newItem.is(tagKey)) {
            return false;
         }

         if (!currentItem.is(tagKey) && newItem.is(tagKey)) {
            return true;
         }
      }

      double d = this.getApproximateAttributeWith(newItem, Attributes.ATTACK_DAMAGE, slot);
      double e = this.getApproximateAttributeWith(currentItem, Attributes.ATTACK_DAMAGE, slot);
      return d != e ? d > e : this.canReplaceEqualItem(newItem, currentItem);
   }

   private double getApproximateAttributeWith(ItemStack item, Holder<Attribute> attribute, EquipmentSlot slot) {
      double d = this.getAttributes().hasAttribute(attribute) ? this.getAttributeBaseValue(attribute) : 0.0;
      ItemAttributeModifiers itemAttributeModifiers = item.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
      return itemAttributeModifiers.compute(attribute, d, slot);
   }

   public boolean canReplaceEqualItem(ItemStack candidate, ItemStack existing) {
      Set<Entry<Holder<Enchantment>>> set = existing.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet();
      Set<Entry<Holder<Enchantment>>> set2 = candidate.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet();
      if (set2.size() != set.size()) {
         return set2.size() > set.size();
      } else {
         int i = candidate.getDamageValue();
         int j = existing.getDamageValue();
         return i != j ? i < j : candidate.has(DataComponents.CUSTOM_NAME) && !existing.has(DataComponents.CUSTOM_NAME);
      }
   }

   public boolean canHoldItem(ItemStack stack) {
      return true;
   }

   public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
      return this.canHoldItem(stack);
   }

   @Nullable
   public TagKey<Item> getPreferredWeaponType() {
      return null;
   }

   public boolean removeWhenFarAway(double distanceToClosestPlayer) {
      return true;
   }

   public boolean requiresCustomPersistence() {
      return this.isPassenger();
   }

   @Override
   public void checkDespawn() {
      if (this.level().getDifficulty() == Difficulty.PEACEFUL && !this.getType().isAllowedInPeaceful()) {
         this.discard();
      } else if (!this.isPersistenceRequired() && !this.requiresCustomPersistence()) {
         Entity entity = this.level().getNearestPlayer(this, -1.0);
         if (entity != null) {
            double d = entity.distanceToSqr(this);
            int i = this.getType().getCategory().getDespawnDistance();
            int j = i * i;
            if (d > j && this.removeWhenFarAway(d)) {
               this.discard();
            }

            int k = this.getType().getCategory().getNoDespawnDistance();
            int l = k * k;
            if (this.noActionTime > 600 && this.random.nextInt(800) == 0 && d > l && this.removeWhenFarAway(d)) {
               this.discard();
            } else if (d < l) {
               this.noActionTime = 0;
            }
         }
      } else {
         this.noActionTime = 0;
      }
   }

   @Override
   protected final void serverAiStep() {
      this.noActionTime++;
      ProfilerFiller profilerFiller = Profiler.get();
      profilerFiller.push("sensing");
      this.sensing.tick();
      profilerFiller.pop();
      int i = this.tickCount + this.getId();
      if (i % 2 != 0 && this.tickCount > 1) {
         profilerFiller.push("targetSelector");
         this.targetSelector.tickRunningGoals(false);
         profilerFiller.pop();
         profilerFiller.push("goalSelector");
         this.goalSelector.tickRunningGoals(false);
         profilerFiller.pop();
      } else {
         profilerFiller.push("targetSelector");
         this.targetSelector.tick();
         profilerFiller.pop();
         profilerFiller.push("goalSelector");
         this.goalSelector.tick();
         profilerFiller.pop();
      }

      profilerFiller.push("navigation");
      this.navigation.tick();
      profilerFiller.pop();
      profilerFiller.push("mob tick");
      this.customServerAiStep((ServerLevel)this.level());
      profilerFiller.pop();
      profilerFiller.push("controls");
      profilerFiller.push("move");
      this.moveControl.tick();
      profilerFiller.popPush("look");
      this.lookControl.tick();
      profilerFiller.popPush("jump");
      this.jumpControl.tick();
      profilerFiller.pop();
      profilerFiller.pop();
   }

   protected void customServerAiStep(ServerLevel level) {
   }

   public int getMaxHeadXRot() {
      return 40;
   }

   public int getMaxHeadYRot() {
      return 75;
   }

   protected void clampHeadRotationToBody() {
      float f = this.getMaxHeadYRot();
      float g = this.getYHeadRot();
      float h = Mth.wrapDegrees(this.yBodyRot - g);
      float i = Mth.clamp(Mth.wrapDegrees(this.yBodyRot - g), -f, f);
      float j = g + h - i;
      this.setYHeadRot(j);
   }

   public int getHeadRotSpeed() {
      return 10;
   }

   public void lookAt(Entity entity, float maxYRotIncrease, float maxXRotIncrease) {
      double d = entity.getX() - this.getX();
      double e = entity.getZ() - this.getZ();
      double f;
      if (entity instanceof LivingEntity livingEntity) {
         f = livingEntity.getEyeY() - this.getEyeY();
      } else {
         f = (entity.getBoundingBox().minY + entity.getBoundingBox().maxY) / 2.0 - this.getEyeY();
      }

      double g = Math.sqrt(d * d + e * e);
      float h = (float)(Mth.atan2(e, d) * 180.0F / (float)Math.PI) - 90.0F;
      float i = (float)(-(Mth.atan2(f, g) * 180.0F / (float)Math.PI));
      this.setXRot(this.rotlerp(this.getXRot(), i, maxXRotIncrease));
      this.setYRot(this.rotlerp(this.getYRot(), h, maxYRotIncrease));
   }

   private float rotlerp(float angle, float targetAngle, float maxIncrease) {
      float f = Mth.wrapDegrees(targetAngle - angle);
      if (f > maxIncrease) {
         f = maxIncrease;
      }

      if (f < -maxIncrease) {
         f = -maxIncrease;
      }

      return angle + f;
   }

   public static boolean checkMobSpawnRules(
      EntityType<? extends Mob> entityType, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random
   ) {
      BlockPos blockPos = pos.below();
      return EntitySpawnReason.isSpawner(spawnReason) || level.getBlockState(blockPos).isValidSpawn(level, blockPos, entityType);
   }

   public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason spawnReason) {
      return true;
   }

   public boolean checkSpawnObstruction(LevelReader level) {
      return !level.containsAnyLiquid(this.getBoundingBox()) && level.isUnobstructed(this);
   }

   public int getMaxSpawnClusterSize() {
      return 4;
   }

   public boolean isMaxGroupSizeReached(int size) {
      return false;
   }

   @Override
   public int getMaxFallDistance() {
      if (this.getTarget() == null) {
         return this.getComfortableFallDistance(0.0F);
      } else {
         int i = (int)(this.getHealth() - this.getMaxHealth() * 0.33F);
         i -= (3 - this.level().getDifficulty().getId()) * 4;
         if (i < 0) {
            i = 0;
         }

         return this.getComfortableFallDistance(i);
      }
   }

   public ItemStack getBodyArmorItem() {
      return this.getItemBySlot(EquipmentSlot.BODY);
   }

   public boolean isSaddled() {
      return this.hasValidEquippableItemForSlot(EquipmentSlot.SADDLE);
   }

   public boolean isWearingBodyArmor() {
      return this.hasValidEquippableItemForSlot(EquipmentSlot.BODY);
   }

   private boolean hasValidEquippableItemForSlot(EquipmentSlot slot) {
      return this.hasItemInSlot(slot) && this.isEquippableInSlot(this.getItemBySlot(slot), slot);
   }

   public void setBodyArmorItem(ItemStack stack) {
      this.setItemSlotAndDropWhenKilled(EquipmentSlot.BODY, stack);
   }

   public Container createEquipmentSlotContainer(EquipmentSlot slot) {
      return new ContainerSingleItem() {
         @Override
         public ItemStack getTheItem() {
            return Mob.this.getItemBySlot(slot);
         }

         @Override
         public void setTheItem(ItemStack item) {
            Mob.this.setItemSlot(slot, item);
            if (!item.isEmpty()) {
               Mob.this.setGuaranteedDrop(slot);
               Mob.this.setPersistenceRequired();
            }
         }

         @Override
         public void setChanged() {
         }

         @Override
         public boolean stillValid(Player player) {
            return player.getVehicle() == Mob.this || player.isWithinEntityInteractionRange(Mob.this, 4.0);
         }
      };
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
      super.dropCustomDeathLoot(level, damageSource, recentlyHit);

      for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
         ItemStack itemStack = this.getItemBySlot(equipmentSlot);
         float f = this.dropChances.byEquipment(equipmentSlot);
         if (f != 0.0F) {
            boolean bl = this.dropChances.isPreserved(equipmentSlot);
            if (damageSource.getEntity() instanceof LivingEntity livingEntity && this.level() instanceof ServerLevel serverLevel) {
               f = EnchantmentHelper.processEquipmentDropChance(serverLevel, livingEntity, damageSource, f);
            }

            if (!itemStack.isEmpty()
               && !EnchantmentHelper.has(itemStack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)
               && (recentlyHit || bl)
               && this.random.nextFloat() < f) {
               if (!bl && itemStack.isDamageableItem()) {
                  itemStack.setDamageValue(itemStack.getMaxDamage() - this.random.nextInt(1 + this.random.nextInt(Math.max(itemStack.getMaxDamage() - 3, 1))));
               }

               this.spawnAtLocation(level, itemStack);
               this.setItemSlot(equipmentSlot, ItemStack.EMPTY);
            }
         }
      }
   }

   public DropChances getDropChances() {
      return this.dropChances;
   }

   public void dropPreservedEquipment(ServerLevel level) {
      this.dropPreservedEquipment(level, itemStack -> true);
   }

   public Set<EquipmentSlot> dropPreservedEquipment(ServerLevel level, Predicate<ItemStack> filter) {
      Set<EquipmentSlot> set = new HashSet<>();

      for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
         ItemStack itemStack = this.getItemBySlot(equipmentSlot);
         if (!itemStack.isEmpty()) {
            if (!filter.test(itemStack)) {
               set.add(equipmentSlot);
            } else if (this.dropChances.isPreserved(equipmentSlot)) {
               this.setItemSlot(equipmentSlot, ItemStack.EMPTY);
               this.spawnAtLocation(level, itemStack);
            }
         }
      }

      return set;
   }

   private LootParams createEquipmentParams(ServerLevel level) {
      return new LootParams.Builder(level)
         .withParameter(LootContextParams.ORIGIN, this.position())
         .withParameter(LootContextParams.THIS_ENTITY, this)
         .create(LootContextParamSets.EQUIPMENT);
   }

   public void equip(EquipmentTable equipmentTable) {
      this.equip(equipmentTable.lootTable(), equipmentTable.slotDropChances());
   }

   public void equip(ResourceKey<LootTable> equipmentLootTable, Map<EquipmentSlot, Float> slotDropChances) {
      if (this.level() instanceof ServerLevel serverLevel) {
         this.equip(equipmentLootTable, this.createEquipmentParams(serverLevel), slotDropChances);
      }
   }

   protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
      if (random.nextFloat() < 0.15F * difficulty.getSpecialMultiplier()) {
         int i = random.nextInt(3);

         for (int j = 1; j <= 3.0F; j++) {
            if (random.nextFloat() < 0.1087F) {
               i++;
            }
         }

         float f = this.level().getDifficulty() == Difficulty.HARD ? 0.1F : 0.25F;
         boolean bl = true;

         for (EquipmentSlot equipmentSlot : EQUIPMENT_POPULATION_ORDER) {
            ItemStack itemStack = this.getItemBySlot(equipmentSlot);
            if (!bl && random.nextFloat() < f) {
               break;
            }

            bl = false;
            if (itemStack.isEmpty()) {
               Item item = getEquipmentForSlot(equipmentSlot, i);
               if (item != null) {
                  this.setItemSlot(equipmentSlot, new ItemStack(item));
               }
            }
         }
      }
   }

   @Nullable
   public static Item getEquipmentForSlot(EquipmentSlot slot, int chance) {
      switch (slot) {
         case HEAD:
            if (chance == 0) {
               return Items.LEATHER_HELMET;
            } else if (chance == 1) {
               return Items.COPPER_HELMET;
            } else if (chance == 2) {
               return Items.GOLDEN_HELMET;
            } else if (chance == 3) {
               return Items.CHAINMAIL_HELMET;
            } else if (chance == 4) {
               return Items.IRON_HELMET;
            } else if (chance == 5) {
               return Items.DIAMOND_HELMET;
            }
         case CHEST:
            if (chance == 0) {
               return Items.LEATHER_CHESTPLATE;
            } else if (chance == 1) {
               return Items.COPPER_CHESTPLATE;
            } else if (chance == 2) {
               return Items.GOLDEN_CHESTPLATE;
            } else if (chance == 3) {
               return Items.CHAINMAIL_CHESTPLATE;
            } else if (chance == 4) {
               return Items.IRON_CHESTPLATE;
            } else if (chance == 5) {
               return Items.DIAMOND_CHESTPLATE;
            }
         case LEGS:
            if (chance == 0) {
               return Items.LEATHER_LEGGINGS;
            } else if (chance == 1) {
               return Items.COPPER_LEGGINGS;
            } else if (chance == 2) {
               return Items.GOLDEN_LEGGINGS;
            } else if (chance == 3) {
               return Items.CHAINMAIL_LEGGINGS;
            } else if (chance == 4) {
               return Items.IRON_LEGGINGS;
            } else if (chance == 5) {
               return Items.DIAMOND_LEGGINGS;
            }
         case FEET:
            if (chance == 0) {
               return Items.LEATHER_BOOTS;
            } else if (chance == 1) {
               return Items.COPPER_BOOTS;
            } else if (chance == 2) {
               return Items.GOLDEN_BOOTS;
            } else if (chance == 3) {
               return Items.CHAINMAIL_BOOTS;
            } else if (chance == 4) {
               return Items.IRON_BOOTS;
            } else if (chance == 5) {
               return Items.DIAMOND_BOOTS;
            }
         default:
            return null;
      }
   }

   protected void populateDefaultEquipmentEnchantments(ServerLevelAccessor level, RandomSource random, DifficultyInstance difficulty) {
      this.enchantSpawnedWeapon(level, random, difficulty);

      for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
         if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            this.enchantSpawnedArmor(level, random, equipmentSlot, difficulty);
         }
      }
   }

   protected void enchantSpawnedWeapon(ServerLevelAccessor level, RandomSource random, DifficultyInstance difficulty) {
      this.enchantSpawnedEquipment(level, EquipmentSlot.MAINHAND, random, 0.25F, difficulty);
   }

   protected void enchantSpawnedArmor(ServerLevelAccessor level, RandomSource random, EquipmentSlot slot, DifficultyInstance difficulty) {
      this.enchantSpawnedEquipment(level, slot, random, 0.5F, difficulty);
   }

   private void enchantSpawnedEquipment(ServerLevelAccessor level, EquipmentSlot slot, RandomSource random, float enchantChance, DifficultyInstance difficulty) {
      ItemStack itemStack = this.getItemBySlot(slot);
      if (!itemStack.isEmpty() && random.nextFloat() < enchantChance * difficulty.getSpecialMultiplier()) {
         EnchantmentHelper.enchantItemFromProvider(itemStack, level.registryAccess(), VanillaEnchantmentProviders.MOB_SPAWN_EQUIPMENT, difficulty, random);
         this.setItemSlot(slot, itemStack);
      }
   }

   @Nullable
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData spawnGroupData
   ) {
      RandomSource randomSource = level.getRandom();
      AttributeInstance attributeInstance = Objects.requireNonNull(this.getAttribute(Attributes.FOLLOW_RANGE));
      if (!attributeInstance.hasModifier(RANDOM_SPAWN_BONUS_ID)) {
         attributeInstance.addPermanentModifier(
            new AttributeModifier(RANDOM_SPAWN_BONUS_ID, randomSource.triangle(0.0, 0.11485000000000001), AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
         );
      }

      this.setLeftHanded(randomSource.nextFloat() < 0.05F);
      return spawnGroupData;
   }

   public void setPersistenceRequired() {
      this.persistenceRequired = true;
   }

   @Override
   public void setDropChance(EquipmentSlot slot, float dropChance) {
      this.dropChances = this.dropChances.withEquipmentChance(slot, dropChance);
   }

   @Override
   public boolean canPickUpLoot() {
      return this.canPickUpLoot;
   }

   public void setCanPickUpLoot(boolean canPickUpLoot) {
      this.canPickUpLoot = canPickUpLoot;
   }

   @Override
   protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot) {
      return this.canPickUpLoot();
   }

   public boolean isPersistenceRequired() {
      return this.persistenceRequired;
   }

   @Override
   public InteractionResult interact(Player player, InteractionHand hand) {
      if (!this.isAlive()) {
         return InteractionResult.PASS;
      } else {
         InteractionResult interactionResult = this.checkAndHandleImportantInteractions(player, hand);
         if (interactionResult.consumesAction()) {
            this.gameEvent(GameEvent.ENTITY_INTERACT, player);
            return interactionResult;
         } else {
            InteractionResult interactionResult2 = super.interact(player, hand);
            if (interactionResult2 != InteractionResult.PASS) {
               return interactionResult2;
            } else {
               interactionResult = this.mobInteract(player, hand);
               if (interactionResult.consumesAction()) {
                  this.gameEvent(GameEvent.ENTITY_INTERACT, player);
                  return interactionResult;
               } else {
                  return InteractionResult.PASS;
               }
            }
         }
      }
   }

   private InteractionResult checkAndHandleImportantInteractions(Player player, InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (itemStack.is(Items.NAME_TAG)) {
         InteractionResult interactionResult = itemStack.interactLivingEntity(player, this, hand);
         if (interactionResult.consumesAction()) {
            return interactionResult;
         }
      }

      if (itemStack.getItem() instanceof SpawnEggItem spawnEggItem) {
         if (this.level() instanceof ServerLevel) {
            Optional<Mob> optional = spawnEggItem.spawnOffspringFromSpawnEgg(
               player, this, (EntityType<? extends Mob>)this.getType(), (ServerLevel)this.level(), this.position(), itemStack
            );
            optional.ifPresent(mob -> this.onOffspringSpawnedFromEgg(player, mob));
            if (optional.isEmpty()) {
               return InteractionResult.PASS;
            }
         }

         return InteractionResult.SUCCESS_SERVER;
      } else {
         return InteractionResult.PASS;
      }
   }

   protected void onOffspringSpawnedFromEgg(Player player, Mob child) {
   }

   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      return InteractionResult.PASS;
   }

   protected void usePlayerItem(Player player, InteractionHand hand, ItemStack stack) {
      int i = stack.getCount();
      UseRemainder useRemainder = stack.get(DataComponents.USE_REMAINDER);
      stack.consume(1, player);
      if (useRemainder != null) {
         ItemStack itemStack = useRemainder.convertIntoRemainder(stack, i, player.hasInfiniteMaterials(), player::handleExtraItemsCreatedOnUse);
         player.setItemInHand(hand, itemStack);
      }
   }

   public boolean isWithinHome() {
      return this.isWithinHome(this.blockPosition());
   }

   public boolean isWithinHome(BlockPos pos) {
      return this.homeRadius == -1 ? true : this.homePosition.distSqr(pos) < this.homeRadius * this.homeRadius;
   }

   public boolean isWithinHome(Vec3 pos) {
      return this.homeRadius == -1 ? true : this.homePosition.distToCenterSqr(pos) < this.homeRadius * this.homeRadius;
   }

   public void setHomeTo(BlockPos pos, int radius) {
      this.homePosition = pos;
      this.homeRadius = radius;
   }

   public BlockPos getHomePosition() {
      return this.homePosition;
   }

   public int getHomeRadius() {
      return this.homeRadius;
   }

   public void clearHome() {
      this.homeRadius = -1;
   }

   public boolean hasHome() {
      return this.homeRadius != -1;
   }

   @Nullable
   public <T extends Mob> T convertTo(
      EntityType<T> entityType, ConversionParams conversionParams, EntitySpawnReason spawnReason, ConversionParams.AfterConversion<T> afterConversion
   ) {
      if (this.isRemoved()) {
         return null;
      } else {
         T mob = (T)entityType.create(this.level(), spawnReason);
         if (mob == null) {
            return null;
         } else {
            conversionParams.type().convert(this, mob, conversionParams);
            afterConversion.finalizeConversion(mob);
            if (this.level() instanceof ServerLevel serverLevel) {
               serverLevel.addFreshEntity(mob);
            }

            if (conversionParams.type().shouldDiscardAfterConversion()) {
               this.discard();
            }

            return mob;
         }
      }
   }

   @Nullable
   public <T extends Mob> T convertTo(EntityType<T> entityType, ConversionParams conversionParams, ConversionParams.AfterConversion<T> afterConversion) {
      return this.convertTo(entityType, conversionParams, EntitySpawnReason.CONVERSION, afterConversion);
   }

   @Nullable
   @Override
   public Leashable.LeashData getLeashData() {
      return this.leashData;
   }

   private void resetAngularLeashMomentum() {
      if (this.leashData != null) {
         this.leashData.angularMomentum = 0.0;
      }
   }

   @Override
   public void setLeashData(@Nullable Leashable.LeashData leashData) {
      this.leashData = leashData;
   }

   @Override
   public void onLeashRemoved() {
      if (this.getLeashData() == null) {
         this.clearHome();
      }
   }

   @Override
   public void leashTooFarBehaviour() {
      Leashable.super.leashTooFarBehaviour();
      this.goalSelector.disableControlFlag(Goal.Flag.MOVE);
   }

   @Override
   public boolean canBeLeashed() {
      return !(this instanceof Enemy);
   }

   @Override
   public boolean startRiding(Entity vehicle, boolean force, boolean sendGameEvent) {
      boolean bl = super.startRiding(vehicle, force, sendGameEvent);
      if (bl && this.isLeashed()) {
         this.dropLeash();
      }

      return bl;
   }

   @Override
   public boolean isEffectiveAi() {
      return super.isEffectiveAi() && !this.isNoAi();
   }

   public void setNoAi(boolean noAi) {
      byte b = this.entityData.get(DATA_MOB_FLAGS_ID);
      this.entityData.set(DATA_MOB_FLAGS_ID, noAi ? (byte)(b | 1) : (byte)(b & -2));
   }

   public void setLeftHanded(boolean leftHanded) {
      byte b = this.entityData.get(DATA_MOB_FLAGS_ID);
      this.entityData.set(DATA_MOB_FLAGS_ID, leftHanded ? (byte)(b | 2) : (byte)(b & -3));
   }

   public void setAggressive(boolean aggressive) {
      byte b = this.entityData.get(DATA_MOB_FLAGS_ID);
      this.entityData.set(DATA_MOB_FLAGS_ID, aggressive ? (byte)(b | 4) : (byte)(b & -5));
   }

   public boolean isNoAi() {
      return (this.entityData.get(DATA_MOB_FLAGS_ID) & 1) != 0;
   }

   public boolean isLeftHanded() {
      return (this.entityData.get(DATA_MOB_FLAGS_ID) & 2) != 0;
   }

   public boolean isAggressive() {
      return (this.entityData.get(DATA_MOB_FLAGS_ID) & 4) != 0;
   }

   public void setBaby(boolean baby) {
   }

   @Override
   public HumanoidArm getMainArm() {
      return this.isLeftHanded() ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
   }

   public boolean isWithinMeleeAttackRange(LivingEntity entity) {
      AttackRange attackRange = this.getActiveItem().get(DataComponents.ATTACK_RANGE);
      double d;
      double e;
      if (attackRange == null) {
         d = DEFAULT_ATTACK_REACH;
         e = 0.0;
      } else {
         d = attackRange.effectiveMaxRange(this);
         e = attackRange.effectiveMinRange(this);
      }

      AABB aABB = entity.getHitbox();
      return this.getAttackBoundingBox(d).intersects(aABB) && (e <= 0.0 || !this.getAttackBoundingBox(e).intersects(aABB));
   }

   protected AABB getAttackBoundingBox(double range) {
      Entity entity = this.getVehicle();
      AABB aABB3;
      if (entity != null) {
         AABB aABB = entity.getBoundingBox();
         AABB aABB2 = this.getBoundingBox();
         aABB3 = new AABB(
            Math.min(aABB2.minX, aABB.minX),
            aABB2.minY,
            Math.min(aABB2.minZ, aABB.minZ),
            Math.max(aABB2.maxX, aABB.maxX),
            aABB2.maxY,
            Math.max(aABB2.maxZ, aABB.maxZ)
         );
      } else {
         aABB3 = this.getBoundingBox();
      }

      return aABB3.inflate(range, 0.0, range);
   }

   @Override
   public boolean doHurtTarget(ServerLevel level, Entity source) {
      float f = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
      ItemStack itemStack = this.getWeaponItem();
      DamageSource damageSource = itemStack.getDamageSource(this, () -> this.damageSources().mobAttack(this));
      f = EnchantmentHelper.modifyDamage(level, itemStack, source, damageSource, f);
      f += itemStack.getItem().getAttackDamageBonus(source, f, damageSource);
      Vec3 vec3 = source.getDeltaMovement();
      boolean bl = source.hurtServer(level, damageSource, f);
      if (bl) {
         this.causeExtraKnockback(source, this.getKnockback(source, damageSource), vec3);
         if (source instanceof LivingEntity livingEntity) {
            itemStack.hurtEnemy(livingEntity, this);
         }

         EnchantmentHelper.doPostAttackEffects(level, source, damageSource);
         this.setLastHurtMob(source);
         this.playAttackSound();
      }

      this.lungeForwardMaybe();
      return bl;
   }

   @Override
   protected void jumpInLiquid(TagKey<Fluid> fluidTag) {
      if (this.getNavigation().canFloat()) {
         super.jumpInLiquid(fluidTag);
      } else {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.3, 0.0));
      }
   }

   @VisibleForTesting
   public void removeFreeWill() {
      this.removeAllGoals(goal -> true);
      this.getBrain().removeAllBehaviors();
   }

   public void removeAllGoals(Predicate<Goal> filter) {
      this.goalSelector.removeAllGoals(filter);
   }

   @Override
   protected void removeAfterChangingDimensions() {
      super.removeAfterChangingDimensions();

      for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
         ItemStack itemStack = this.getItemBySlot(equipmentSlot);
         if (!itemStack.isEmpty()) {
            itemStack.setCount(0);
         }
      }
   }

   @Nullable
   @Override
   public ItemStack getPickResult() {
      SpawnEggItem spawnEggItem = SpawnEggItem.byId(this.getType());
      return spawnEggItem == null ? null : new ItemStack(spawnEggItem);
   }

   @Override
   protected void onAttributeUpdated(Holder<Attribute> attribute) {
      super.onAttributeUpdated(attribute);
      if (attribute.is(Attributes.FOLLOW_RANGE) || attribute.is(Attributes.TEMPT_RANGE)) {
         this.getNavigation().updatePathfinderMaxVisitedNodes();
      }
   }

   @Override
   public void registerDebugValues(ServerLevel level, DebugValueSource.Registration registration) {
      registration.register(DebugSubscriptions.ENTITY_PATHS, () -> {
         Path path = this.getNavigation().getPath();
         return path != null && path.debugData() != null ? new DebugPathInfo(path.copy(), this.getNavigation().getMaxDistanceToWaypoint()) : null;
      });
      registration.register(
         DebugSubscriptions.GOAL_SELECTORS,
         () -> {
            Set<WrappedGoal> set = this.goalSelector.getAvailableGoals();
            List<DebugGoalInfo.DebugGoal> list = new ArrayList<>(set.size());
            set.forEach(
               wrappedGoal -> list.add(
                  new DebugGoalInfo.DebugGoal(wrappedGoal.getPriority(), wrappedGoal.isRunning(), wrappedGoal.getGoal().getClass().getSimpleName())
               )
            );
            return new DebugGoalInfo(list);
         }
      );
      if (!this.brain.isBrainDead()) {
         registration.register(DebugSubscriptions.BRAINS, () -> DebugBrainDump.takeBrainDump(level, this));
      }
   }

   public float chargeSpeedModifier() {
      return 1.0F;
   }
}
