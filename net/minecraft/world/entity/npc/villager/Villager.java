package net.minecraft.world.entity.npc.villager;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.SpawnUtil;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ReputationEventHandler;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import net.minecraft.world.entity.ai.gossip.GossipContainer;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.GolemSensor;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.SpecialPlantable;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Villager extends AbstractVillager implements ReputationEventHandler, VillagerDataHolder {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final EntityDataAccessor<VillagerData> DATA_VILLAGER_DATA = SynchedEntityData.defineId(Villager.class, EntityDataSerializers.VILLAGER_DATA);
   public static final int BREEDING_FOOD_THRESHOLD = 12;
   public static final Map<Item, Integer> FOOD_POINTS = ImmutableMap.of(Items.BREAD, 4, Items.POTATO, 1, Items.CARROT, 1, Items.BEETROOT, 1);
   private static final int TRADES_PER_LEVEL = 2;
   private static final int MAX_GOSSIP_TOPICS = 10;
   private static final int GOSSIP_COOLDOWN = 1200;
   private static final int GOSSIP_DECAY_INTERVAL = 24000;
   private static final int HOW_FAR_AWAY_TO_TALK_TO_OTHER_VILLAGERS_ABOUT_GOLEMS = 10;
   private static final int HOW_MANY_VILLAGERS_NEED_TO_AGREE_TO_SPAWN_A_GOLEM = 5;
   private static final long TIME_SINCE_SLEEPING_FOR_GOLEM_SPAWNING = 24000L;
   @VisibleForTesting
   public static final float SPEED_MODIFIER = 0.5F;
   private static final int DEFAULT_XP = 0;
   private static final byte DEFAULT_FOOD_LEVEL = 0;
   private static final int DEFAULT_LAST_RESTOCK = 0;
   private static final int DEFAULT_LAST_GOSSIP_DECAY = 0;
   private static final int DEFAULT_RESTOCKS_TODAY = 0;
   private static final boolean DEFAULT_ASSIGN_PROFESSION_WHEN_SPAWNED = false;
   private int updateMerchantTimer;
   private boolean increaseProfessionLevelOnUpdate;
   @Nullable
   private Player lastTradedPlayer;
   private boolean chasing;
   private int foodLevel = 0;
   private final GossipContainer gossips = new GossipContainer();
   private long lastGossipTime;
   private long lastGossipDecayTime = 0L;
   private int villagerXp = 0;
   private long lastRestockGameTime = 0L;
   private int numberOfRestocksToday = 0;
   private long lastRestockCheckDay;
   private boolean assignProfessionWhenSpawned = false;
   private static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
      MemoryModuleType.HOME,
      MemoryModuleType.JOB_SITE,
      MemoryModuleType.POTENTIAL_JOB_SITE,
      MemoryModuleType.MEETING_POINT,
      MemoryModuleType.NEAREST_LIVING_ENTITIES,
      MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
      MemoryModuleType.VISIBLE_VILLAGER_BABIES,
      MemoryModuleType.NEAREST_PLAYERS,
      MemoryModuleType.NEAREST_VISIBLE_PLAYER,
      MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER,
      MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM,
      MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS,
      new MemoryModuleType[]{
         MemoryModuleType.WALK_TARGET,
         MemoryModuleType.LOOK_TARGET,
         MemoryModuleType.INTERACTION_TARGET,
         MemoryModuleType.BREED_TARGET,
         MemoryModuleType.PATH,
         MemoryModuleType.DOORS_TO_CLOSE,
         MemoryModuleType.NEAREST_BED,
         MemoryModuleType.HURT_BY,
         MemoryModuleType.HURT_BY_ENTITY,
         MemoryModuleType.NEAREST_HOSTILE,
         MemoryModuleType.SECONDARY_JOB_SITE,
         MemoryModuleType.HIDING_PLACE,
         MemoryModuleType.HEARD_BELL_TIME,
         MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
         MemoryModuleType.LAST_SLEPT,
         MemoryModuleType.LAST_WOKEN,
         MemoryModuleType.LAST_WORKED_AT_POI,
         MemoryModuleType.GOLEM_DETECTED_RECENTLY
      }
   );
   private static final ImmutableList<SensorType<? extends Sensor<? super Villager>>> SENSOR_TYPES = ImmutableList.of(
      SensorType.NEAREST_LIVING_ENTITIES,
      SensorType.NEAREST_PLAYERS,
      SensorType.NEAREST_ITEMS,
      SensorType.NEAREST_BED,
      SensorType.HURT_BY,
      SensorType.VILLAGER_HOSTILES,
      SensorType.VILLAGER_BABIES,
      SensorType.SECONDARY_POIS,
      SensorType.GOLEM_DETECTED
   );
   public static final Map<MemoryModuleType<GlobalPos>, BiPredicate<Villager, Holder<PoiType>>> POI_MEMORIES = ImmutableMap.of(
      MemoryModuleType.HOME,
      (BiPredicate<Villager, Holder>)(p_479185_, p_477956_) -> p_477956_.is(PoiTypes.HOME),
      MemoryModuleType.JOB_SITE,
      (BiPredicate<Villager, Holder>)(p_482097_, p_479094_) -> p_482097_.getVillagerData().profession().value().heldJobSite().test(p_479094_),
      MemoryModuleType.POTENTIAL_JOB_SITE,
      (BiPredicate<Villager, Holder>)(p_479743_, p_478940_) -> VillagerProfession.ALL_ACQUIRABLE_JOBS.test(p_478940_),
      MemoryModuleType.MEETING_POINT,
      (BiPredicate<Villager, Holder>)(p_482165_, p_481107_) -> p_481107_.is(PoiTypes.MEETING)
   );

   public Villager(EntityType<? extends Villager> p_478620_, Level p_480064_) {
      this(p_478620_, p_480064_, VillagerType.PLAINS);
   }

   public Villager(EntityType<? extends Villager> p_481873_, Level p_478412_, ResourceKey<VillagerType> p_481206_) {
      this(p_481873_, p_478412_, p_478412_.registryAccess().getOrThrow(p_481206_));
   }

   public Villager(EntityType<? extends Villager> p_478041_, Level p_478682_, Holder<VillagerType> p_478388_) {
      super(p_478041_, p_478682_);
      this.getNavigation().setCanOpenDoors(true);
      this.getNavigation().setCanFloat(true);
      this.getNavigation().setRequiredPathLength(48.0F);
      this.setCanPickUpLoot(true);
      this.setVillagerData(this.getVillagerData().withType(p_478388_).withProfession(p_478682_.registryAccess(), VillagerProfession.NONE));
   }

   @Override
   public Brain<Villager> getBrain() {
      return (Brain<Villager>)super.getBrain();
   }

   @Override
   protected Brain.Provider<Villager> brainProvider() {
      return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
   }

   @Override
   protected Brain<?> makeBrain(Dynamic<?> p_478906_) {
      Brain<Villager> brain = this.brainProvider().makeBrain(p_478906_);
      this.registerBrainGoals(brain);
      return brain;
   }

   public void refreshBrain(ServerLevel p_478680_) {
      Brain<Villager> brain = this.getBrain();
      brain.stopAll(p_478680_, this);
      this.brain = brain.copyWithoutBehaviors();
      this.registerBrainGoals(this.getBrain());
   }

   private void registerBrainGoals(Brain<Villager> p_480202_) {
      Holder<VillagerProfession> holder = this.getVillagerData().profession();
      if (this.isBaby()) {
         p_480202_.setSchedule(EnvironmentAttributes.BABY_VILLAGER_ACTIVITY);
         p_480202_.addActivity(Activity.PLAY, VillagerGoalPackages.getPlayPackage(0.5F));
      } else {
         p_480202_.setSchedule(EnvironmentAttributes.VILLAGER_ACTIVITY);
         p_480202_.addActivityWithConditions(
            Activity.WORK, VillagerGoalPackages.getWorkPackage(holder, 0.5F), ImmutableSet.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT))
         );
      }

      p_480202_.addActivity(Activity.CORE, VillagerGoalPackages.getCorePackage(holder, 0.5F));
      p_480202_.addActivityWithConditions(
         Activity.MEET, VillagerGoalPackages.getMeetPackage(holder, 0.5F), ImmutableSet.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT))
      );
      p_480202_.addActivity(Activity.REST, VillagerGoalPackages.getRestPackage(holder, 0.5F));
      p_480202_.addActivity(Activity.IDLE, VillagerGoalPackages.getIdlePackage(holder, 0.5F));
      p_480202_.addActivity(Activity.PANIC, VillagerGoalPackages.getPanicPackage(holder, 0.5F));
      p_480202_.addActivity(Activity.PRE_RAID, VillagerGoalPackages.getPreRaidPackage(holder, 0.5F));
      p_480202_.addActivity(Activity.RAID, VillagerGoalPackages.getRaidPackage(holder, 0.5F));
      p_480202_.addActivity(Activity.HIDE, VillagerGoalPackages.getHidePackage(holder, 0.5F));
      p_480202_.setCoreActivities(ImmutableSet.of(Activity.CORE));
      p_480202_.setDefaultActivity(Activity.IDLE);
      p_480202_.setActiveActivityIfPossible(Activity.IDLE);
      p_480202_.updateActivityFromSchedule(this.level().environmentAttributes(), this.level().getGameTime(), this.position());
   }

   @Override
   protected void ageBoundaryReached() {
      super.ageBoundaryReached();
      if (this.level() instanceof ServerLevel) {
         this.refreshBrain((ServerLevel)this.level());
      }
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5);
   }

   public boolean assignProfessionWhenSpawned() {
      return this.assignProfessionWhenSpawned;
   }

   @Override
   protected void customServerAiStep(ServerLevel p_480084_) {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("villagerBrain");
      this.getBrain().tick(p_480084_, this);
      profilerfiller.pop();
      if (this.assignProfessionWhenSpawned) {
         this.assignProfessionWhenSpawned = false;
      }

      if (!this.isTrading() && this.updateMerchantTimer > 0) {
         this.updateMerchantTimer--;
         if (this.updateMerchantTimer <= 0) {
            if (this.increaseProfessionLevelOnUpdate) {
               this.increaseMerchantCareer(p_480084_);
               this.increaseProfessionLevelOnUpdate = false;
            }

            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
         }
      }

      if (this.lastTradedPlayer != null) {
         p_480084_.onReputationEvent(ReputationEventType.TRADE, this.lastTradedPlayer, this);
         p_480084_.broadcastEntityEvent(this, (byte)14);
         this.lastTradedPlayer = null;
      }

      if (!this.isNoAi() && this.random.nextInt(100) == 0) {
         Raid raid = p_480084_.getRaidAt(this.blockPosition());
         if (raid != null && raid.isActive() && !raid.isOver()) {
            p_480084_.broadcastEntityEvent(this, (byte)42);
         }
      }

      if (this.getVillagerData().profession().is(VillagerProfession.NONE) && this.isTrading()) {
         this.stopTrading();
      }

      super.customServerAiStep(p_480084_);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.getUnhappyCounter() > 0) {
         this.setUnhappyCounter(this.getUnhappyCounter() - 1);
      }

      this.maybeDecayGossip();
   }

   @Override
   public InteractionResult mobInteract(Player p_480051_, InteractionHand p_478071_) {
      ItemStack itemstack = p_480051_.getItemInHand(p_478071_);
      if (itemstack.is(Items.VILLAGER_SPAWN_EGG) || !this.isAlive() || this.isTrading() || this.isSleeping() || p_480051_.isSecondaryUseActive()) {
         return super.mobInteract(p_480051_, p_478071_);
      } else if (this.isBaby()) {
         this.setUnhappy();
         return InteractionResult.SUCCESS;
      } else {
         if (!this.level().isClientSide()) {
            boolean flag = this.getOffers().isEmpty();
            if (p_478071_ == InteractionHand.MAIN_HAND) {
               if (flag) {
                  this.setUnhappy();
               }

               p_480051_.awardStat(Stats.TALKED_TO_VILLAGER);
            }

            if (flag) {
               return InteractionResult.CONSUME;
            }

            this.startTrading(p_480051_);
         }

         return InteractionResult.SUCCESS;
      }
   }

   private void setUnhappy() {
      this.setUnhappyCounter(40);
      if (!this.level().isClientSide()) {
         this.makeSound(SoundEvents.VILLAGER_NO);
      }
   }

   private void startTrading(Player p_479399_) {
      this.updateSpecialPrices(p_479399_);
      this.setTradingPlayer(p_479399_);
      this.openTradingScreen(p_479399_, this.getDisplayName(), this.getVillagerData().level());
   }

   @Override
   public void setTradingPlayer(@Nullable Player p_478881_) {
      boolean flag = this.getTradingPlayer() != null && p_478881_ == null;
      super.setTradingPlayer(p_478881_);
      if (flag) {
         this.stopTrading();
      }
   }

   @Override
   protected void stopTrading() {
      super.stopTrading();
      this.resetSpecialPrices();
   }

   private void resetSpecialPrices() {
      if (!this.level().isClientSide()) {
         for (MerchantOffer merchantoffer : this.getOffers()) {
            merchantoffer.resetSpecialPriceDiff();
         }
      }
   }

   @Override
   public boolean canRestock() {
      return true;
   }

   public void restock() {
      this.updateDemand();

      for (MerchantOffer merchantoffer : this.getOffers()) {
         merchantoffer.resetUses();
      }

      this.resendOffersToTradingPlayer();
      this.lastRestockGameTime = this.level().getGameTime();
      this.numberOfRestocksToday++;
   }

   private void resendOffersToTradingPlayer() {
      MerchantOffers merchantoffers = this.getOffers();
      Player player = this.getTradingPlayer();
      if (player != null && !merchantoffers.isEmpty()) {
         player.sendMerchantOffers(
            player.containerMenu.containerId, merchantoffers, this.getVillagerData().level(), this.getVillagerXp(), this.showProgressBar(), this.canRestock()
         );
      }
   }

   private boolean needsToRestock() {
      for (MerchantOffer merchantoffer : this.getOffers()) {
         if (merchantoffer.needsRestock()) {
            return true;
         }
      }

      return false;
   }

   private boolean allowedToRestock() {
      return this.numberOfRestocksToday == 0 || this.numberOfRestocksToday < 2 && this.level().getGameTime() > this.lastRestockGameTime + 2400L;
   }

   public boolean shouldRestock(ServerLevel p_481130_) {
      long i = this.lastRestockGameTime + 12000L;
      long j = this.level().getGameTime();
      boolean flag = j > i;
      long k = p_481130_.getDayCount();
      flag |= this.lastRestockCheckDay > 0L && k > this.lastRestockCheckDay;
      this.lastRestockCheckDay = k;
      if (flag) {
         this.lastRestockGameTime = j;
         this.resetNumberOfRestocks();
      }

      return this.allowedToRestock() && this.needsToRestock();
   }

   private void catchUpDemand() {
      int i = 2 - this.numberOfRestocksToday;
      if (i > 0) {
         for (MerchantOffer merchantoffer : this.getOffers()) {
            merchantoffer.resetUses();
         }
      }

      for (int j = 0; j < i; j++) {
         this.updateDemand();
      }

      this.resendOffersToTradingPlayer();
   }

   private void updateDemand() {
      for (MerchantOffer merchantoffer : this.getOffers()) {
         merchantoffer.updateDemand();
      }
   }

   private void updateSpecialPrices(Player p_481671_) {
      int i = this.getPlayerReputation(p_481671_);
      if (i != 0) {
         for (MerchantOffer merchantoffer : this.getOffers()) {
            merchantoffer.addToSpecialPriceDiff(-Mth.floor(i * merchantoffer.getPriceMultiplier()));
         }
      }

      if (p_481671_.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
         MobEffectInstance mobeffectinstance = p_481671_.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
         int k = mobeffectinstance.getAmplifier();

         for (MerchantOffer merchantoffer1 : this.getOffers()) {
            double d0 = 0.3 + 0.0625 * k;
            int j = (int)Math.floor(d0 * merchantoffer1.getBaseCostA().getCount());
            merchantoffer1.addToSpecialPriceDiff(-Math.max(j, 1));
         }
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_481137_) {
      super.defineSynchedData(p_481137_);
      p_481137_.define(DATA_VILLAGER_DATA, createDefaultVillagerData());
   }

   public static VillagerData createDefaultVillagerData() {
      return new VillagerData(
         BuiltInRegistries.VILLAGER_TYPE.getOrThrow(VillagerType.PLAINS), BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NONE), 1
      );
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_479608_) {
      super.addAdditionalSaveData(p_479608_);
      p_479608_.store("VillagerData", VillagerData.CODEC, this.getVillagerData());
      p_479608_.putByte("FoodLevel", (byte)this.foodLevel);
      p_479608_.store("Gossips", GossipContainer.CODEC, this.gossips);
      p_479608_.putInt("Xp", this.villagerXp);
      p_479608_.putLong("LastRestock", this.lastRestockGameTime);
      p_479608_.putLong("LastGossipDecay", this.lastGossipDecayTime);
      p_479608_.putInt("RestocksToday", this.numberOfRestocksToday);
      if (this.assignProfessionWhenSpawned) {
         p_479608_.putBoolean("AssignProfessionWhenSpawned", true);
      }
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_480754_) {
      super.readAdditionalSaveData(p_480754_);
      this.entityData.set(DATA_VILLAGER_DATA, p_480754_.<VillagerData>read("VillagerData", VillagerData.CODEC).orElseGet(Villager::createDefaultVillagerData));
      this.foodLevel = p_480754_.getByteOr("FoodLevel", (byte)0);
      this.gossips.clear();
      p_480754_.<GossipContainer>read("Gossips", GossipContainer.CODEC).ifPresent(this.gossips::putAll);
      this.villagerXp = p_480754_.getIntOr("Xp", 0);
      this.lastRestockGameTime = p_480754_.getLongOr("LastRestock", 0L);
      this.lastGossipDecayTime = p_480754_.getLongOr("LastGossipDecay", 0L);
      if (this.level() instanceof ServerLevel) {
         this.refreshBrain((ServerLevel)this.level());
      }

      this.numberOfRestocksToday = p_480754_.getIntOr("RestocksToday", 0);
      this.assignProfessionWhenSpawned = p_480754_.getBooleanOr("AssignProfessionWhenSpawned", false);
   }

   @Override
   public boolean removeWhenFarAway(double p_480800_) {
      return false;
   }

   @Nullable
   @Override
   protected SoundEvent getAmbientSound() {
      if (this.isSleeping()) {
         return null;
      } else {
         return this.isTrading() ? SoundEvents.VILLAGER_TRADE : SoundEvents.VILLAGER_AMBIENT;
      }
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_481926_) {
      return SoundEvents.VILLAGER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.VILLAGER_DEATH;
   }

   public void playWorkSound() {
      this.makeSound(this.getVillagerData().profession().value().workSound());
   }

   @Override
   public void setVillagerData(VillagerData p_479728_) {
      VillagerData villagerdata = this.getVillagerData();
      if (!villagerdata.profession().equals(p_479728_.profession())) {
         this.offers = null;
      }

      this.entityData.set(DATA_VILLAGER_DATA, p_479728_);
   }

   @Override
   public VillagerData getVillagerData() {
      return this.entityData.get(DATA_VILLAGER_DATA);
   }

   @Override
   protected void rewardTradeXp(MerchantOffer p_480721_) {
      int i = 3 + this.random.nextInt(4);
      this.villagerXp = this.villagerXp + p_480721_.getXp();
      this.lastTradedPlayer = this.getTradingPlayer();
      if (this.shouldIncreaseLevel()) {
         this.updateMerchantTimer = 40;
         this.increaseProfessionLevelOnUpdate = true;
         i += 5;
      }

      if (p_480721_.shouldRewardExp()) {
         this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5, this.getZ(), i));
      }
   }

   @Override
   public void setLastHurtByMob(@Nullable LivingEntity p_479240_) {
      if (p_479240_ != null && this.level() instanceof ServerLevel) {
         ((ServerLevel)this.level()).onReputationEvent(ReputationEventType.VILLAGER_HURT, p_479240_, this);
         if (this.isAlive() && p_479240_ instanceof Player) {
            this.level().broadcastEntityEvent(this, (byte)13);
         }
      }

      super.setLastHurtByMob(p_479240_);
   }

   @Override
   public void die(DamageSource p_481224_) {
      LOGGER.info("Villager {} died, message: '{}'", this, p_481224_.getLocalizedDeathMessage(this).getString());
      Entity entity = p_481224_.getEntity();
      if (entity != null) {
         this.tellWitnessesThatIWasMurdered(entity);
      }

      this.releaseAllPois();
      super.die(p_481224_);
   }

   private void releaseAllPois() {
      this.releasePoi(MemoryModuleType.HOME);
      this.releasePoi(MemoryModuleType.JOB_SITE);
      this.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
      this.releasePoi(MemoryModuleType.MEETING_POINT);
   }

   private void tellWitnessesThatIWasMurdered(Entity p_479983_) {
      if (this.level() instanceof ServerLevel serverlevel) {
         Optional<NearestVisibleLivingEntities> optional = this.brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
         if (!optional.isEmpty()) {
            optional.get()
               .findAll(ReputationEventHandler.class::isInstance)
               .forEach(p_480861_ -> serverlevel.onReputationEvent(ReputationEventType.VILLAGER_KILLED, p_479983_, (ReputationEventHandler)p_480861_));
         }
      }
   }

   public void releasePoi(MemoryModuleType<GlobalPos> p_479162_) {
      if (this.level() instanceof ServerLevel) {
         MinecraftServer minecraftserver = ((ServerLevel)this.level()).getServer();
         this.brain.getMemory(p_479162_).ifPresent(p_479304_ -> {
            ServerLevel serverlevel = minecraftserver.getLevel(p_479304_.dimension());
            if (serverlevel != null) {
               PoiManager poimanager = serverlevel.getPoiManager();
               Optional<Holder<PoiType>> optional = poimanager.getType(p_479304_.pos());
               BiPredicate<Villager, Holder<PoiType>> bipredicate = POI_MEMORIES.get(p_479162_);
               if (optional.isPresent() && bipredicate.test(this, optional.get())) {
                  poimanager.release(p_479304_.pos());
                  serverlevel.debugSynchronizers().updatePoi(p_479304_.pos());
               }
            }
         });
      }
   }

   @Override
   public boolean canBreed() {
      return this.foodLevel + this.countFoodPointsInInventory() >= 12 && !this.isSleeping() && this.getAge() == 0;
   }

   private boolean hungry() {
      return this.foodLevel < 12;
   }

   private void eatUntilFull() {
      if (this.hungry() && this.countFoodPointsInInventory() != 0) {
         for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack itemstack = this.getInventory().getItem(i);
            if (!itemstack.isEmpty()) {
               Integer integer = FOOD_POINTS.get(itemstack.getItem());
               if (integer != null) {
                  int j = itemstack.getCount();

                  for (int k = j; k > 0; k--) {
                     this.foodLevel = this.foodLevel + integer;
                     this.getInventory().removeItem(i, 1);
                     if (!this.hungry()) {
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   public int getPlayerReputation(Player p_480297_) {
      return this.gossips.getReputation(p_480297_.getUUID(), p_481598_ -> true);
   }

   private void digestFood(int p_481573_) {
      this.foodLevel -= p_481573_;
   }

   public void eatAndDigestFood() {
      this.eatUntilFull();
      this.digestFood(12);
   }

   public void setOffers(MerchantOffers p_480816_) {
      this.offers = p_480816_;
   }

   private boolean shouldIncreaseLevel() {
      int i = this.getVillagerData().level();
      return VillagerData.canLevelUp(i) && this.villagerXp >= VillagerData.getMaxXpPerLevel(i);
   }

   private void increaseMerchantCareer(ServerLevel p_479507_) {
      this.setVillagerData(this.getVillagerData().withLevel(this.getVillagerData().level() + 1));
      this.updateTrades(p_479507_);
   }

   @Override
   protected Component getTypeName() {
      return this.getVillagerData().profession().value().name();
   }

   @Override
   public void handleEntityEvent(byte p_480680_) {
      if (p_480680_ == 12) {
         this.addParticlesAroundSelf(ParticleTypes.HEART);
      } else if (p_480680_ == 13) {
         this.addParticlesAroundSelf(ParticleTypes.ANGRY_VILLAGER);
      } else if (p_480680_ == 14) {
         this.addParticlesAroundSelf(ParticleTypes.HAPPY_VILLAGER);
      } else if (p_480680_ == 42) {
         this.addParticlesAroundSelf(ParticleTypes.SPLASH);
      } else {
         super.handleEntityEvent(p_480680_);
      }
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_479460_, DifficultyInstance p_481788_, EntitySpawnReason p_480000_, @Nullable SpawnGroupData p_481743_
   ) {
      if (p_480000_ == EntitySpawnReason.BREEDING) {
         this.setVillagerData(this.getVillagerData().withProfession(p_479460_.registryAccess(), VillagerProfession.NONE));
      }

      if (p_480000_ == EntitySpawnReason.COMMAND
         || p_480000_ == EntitySpawnReason.SPAWN_ITEM_USE
         || EntitySpawnReason.isSpawner(p_480000_)
         || p_480000_ == EntitySpawnReason.DISPENSER) {
         this.setVillagerData(this.getVillagerData().withType(p_479460_.registryAccess(), VillagerType.byBiome(p_479460_.getBiome(this.blockPosition()))));
      }

      if (p_480000_ == EntitySpawnReason.STRUCTURE) {
         this.assignProfessionWhenSpawned = true;
      }

      return super.finalizeSpawn(p_479460_, p_481788_, p_480000_, p_481743_);
   }

   @Nullable
   public Villager getBreedOffspring(ServerLevel p_481397_, AgeableMob p_479476_) {
      double d0 = this.random.nextDouble();
      Holder<VillagerType> holder;
      if (d0 < 0.5) {
         holder = p_481397_.registryAccess().getOrThrow(VillagerType.byBiome(p_481397_.getBiome(this.blockPosition())));
      } else if (d0 < 0.75) {
         holder = this.getVillagerData().type();
      } else {
         holder = ((Villager)p_479476_).getVillagerData().type();
      }

      Villager villager = new Villager(EntityType.VILLAGER, p_481397_, holder);
      villager.finalizeSpawn(p_481397_, p_481397_.getCurrentDifficultyAt(villager.blockPosition()), EntitySpawnReason.BREEDING, null);
      return villager;
   }

   @Override
   public void thunderHit(ServerLevel p_480886_, LightningBolt p_478420_) {
      if (p_480886_.getDifficulty() != Difficulty.PEACEFUL && EventHooks.canLivingConvert(this, EntityType.WITCH, timer -> {})) {
         LOGGER.info("Villager {} was struck by lightning {}.", this, p_478420_);
         Witch witch = this.convertTo(EntityType.WITCH, ConversionParams.single(this, false, false), p_481535_ -> {
            p_481535_.finalizeSpawn(p_480886_, p_480886_.getCurrentDifficultyAt(p_481535_.blockPosition()), EntitySpawnReason.CONVERSION, null);
            EventHooks.onLivingConvert(this, p_481535_);
            p_481535_.setPersistenceRequired();
            this.releaseAllPois();
         });
         if (witch == null) {
            super.thunderHit(p_480886_, p_478420_);
         }
      } else {
         super.thunderHit(p_480886_, p_478420_);
      }
   }

   @Override
   protected void pickUpItem(ServerLevel p_479118_, ItemEntity p_481393_) {
      InventoryCarrier.pickUpItem(p_479118_, this, this, p_481393_);
   }

   @Override
   public boolean wantsToPickUp(ServerLevel p_479148_, ItemStack p_478154_) {
      Item item = p_478154_.getItem();
      boolean isFarmerDesiredSeed = p_478154_.getItem() instanceof SpecialPlantable specialPlantable
         && specialPlantable.villagerCanPlantItem(this)
         && this.getVillagerData()
            .profession()
            .value()
            .secondaryPoi()
            .stream()
            .anyMatch(secondaryPoi -> secondaryPoi.defaultBlockState().is(Tags.Blocks.VILLAGER_FARMLANDS));
      return (p_478154_.is(ItemTags.VILLAGER_PICKS_UP) || this.getVillagerData().profession().value().requestedItems().contains(item) || isFarmerDesiredSeed)
         && this.getInventory().canAddItem(p_478154_);
   }

   public boolean hasExcessFood() {
      return this.countFoodPointsInInventory() >= 24;
   }

   public boolean wantsMoreFood() {
      return this.countFoodPointsInInventory() < 12;
   }

   private int countFoodPointsInInventory() {
      SimpleContainer simplecontainer = this.getInventory();
      return FOOD_POINTS.entrySet().stream().mapToInt(p_480390_ -> simplecontainer.countItem(p_480390_.getKey()) * p_480390_.getValue()).sum();
   }

   public boolean hasFarmSeeds() {
      return this.getInventory().hasAnyMatching(p_479579_ -> p_479579_.is(ItemTags.VILLAGER_PLANTABLE_SEEDS));
   }

   @Override
   protected void updateTrades(ServerLevel p_480053_) {
      VillagerData villagerdata = this.getVillagerData();
      ResourceKey<VillagerProfession> resourcekey = villagerdata.profession().unwrapKey().orElse(null);
      if (resourcekey != null) {
         Int2ObjectMap<VillagerTrades.ItemListing[]> int2objectmap;
         if (this.level().enabledFeatures().contains(FeatureFlags.TRADE_REBALANCE)) {
            Int2ObjectMap<VillagerTrades.ItemListing[]> int2objectmap1 = VillagerTrades.EXPERIMENTAL_TRADES.get(resourcekey);
            int2objectmap = int2objectmap1 != null ? int2objectmap1 : VillagerTrades.TRADES.get(resourcekey);
         } else {
            int2objectmap = VillagerTrades.TRADES.get(resourcekey);
         }

         if (int2objectmap != null && !int2objectmap.isEmpty()) {
            VillagerTrades.ItemListing[] avillagertrades$itemlisting = (VillagerTrades.ItemListing[])int2objectmap.get(villagerdata.level());
            if (avillagertrades$itemlisting != null) {
               MerchantOffers merchantoffers = this.getOffers();
               this.addOffersFromItemListings(p_480053_, merchantoffers, avillagertrades$itemlisting, 2);
               if (SharedConstants.DEBUG_UNLOCK_ALL_TRADES && villagerdata.level() < int2objectmap.size()) {
                  this.increaseMerchantCareer(p_480053_);
               }
            }
         }
      }
   }

   public void gossip(ServerLevel p_480518_, Villager p_481408_, long p_481255_) {
      if ((p_481255_ < this.lastGossipTime || p_481255_ >= this.lastGossipTime + 1200L)
         && (p_481255_ < p_481408_.lastGossipTime || p_481255_ >= p_481408_.lastGossipTime + 1200L)) {
         this.gossips.transferFrom(p_481408_.gossips, this.random, 10);
         this.lastGossipTime = p_481255_;
         p_481408_.lastGossipTime = p_481255_;
         this.spawnGolemIfNeeded(p_480518_, p_481255_, 5);
      }
   }

   private void maybeDecayGossip() {
      long i = this.level().getGameTime();
      if (this.lastGossipDecayTime == 0L) {
         this.lastGossipDecayTime = i;
      } else if (i >= this.lastGossipDecayTime + 24000L) {
         this.gossips.decay();
         this.lastGossipDecayTime = i;
      }
   }

   public void spawnGolemIfNeeded(ServerLevel p_478562_, long p_478317_, int p_478151_) {
      if (this.wantsToSpawnGolem(p_478317_)) {
         AABB aabb = this.getBoundingBox().inflate(10.0, 10.0, 10.0);
         List<Villager> list = p_478562_.getEntitiesOfClass(Villager.class, aabb);
         List<Villager> list1 = list.stream().filter(p_480731_ -> p_480731_.wantsToSpawnGolem(p_478317_)).limit(5L).toList();
         if (list1.size() >= p_478151_
            && !SpawnUtil.trySpawnMob(
                  EntityType.IRON_GOLEM, EntitySpawnReason.MOB_SUMMONED, p_478562_, this.blockPosition(), 10, 8, 6, SpawnUtil.Strategy.LEGACY_IRON_GOLEM, false
               )
               .isEmpty()) {
            list.forEach(GolemSensor::golemDetected);
         }
      }
   }

   public boolean wantsToSpawnGolem(long p_480855_) {
      return !this.golemSpawnConditionsMet(this.level().getGameTime()) ? false : !this.brain.hasMemoryValue(MemoryModuleType.GOLEM_DETECTED_RECENTLY);
   }

   @Override
   public void onReputationEventFrom(ReputationEventType p_479275_, Entity p_480135_) {
      if (p_479275_ == ReputationEventType.ZOMBIE_VILLAGER_CURED) {
         this.gossips.add(p_480135_.getUUID(), GossipType.MAJOR_POSITIVE, 20);
         this.gossips.add(p_480135_.getUUID(), GossipType.MINOR_POSITIVE, 25);
      } else if (p_479275_ == ReputationEventType.TRADE) {
         this.gossips.add(p_480135_.getUUID(), GossipType.TRADING, 2);
      } else if (p_479275_ == ReputationEventType.VILLAGER_HURT) {
         this.gossips.add(p_480135_.getUUID(), GossipType.MINOR_NEGATIVE, 25);
      } else if (p_479275_ == ReputationEventType.VILLAGER_KILLED) {
         this.gossips.add(p_480135_.getUUID(), GossipType.MAJOR_NEGATIVE, 25);
      }
   }

   @Override
   public int getVillagerXp() {
      return this.villagerXp;
   }

   public void setVillagerXp(int p_478202_) {
      this.villagerXp = p_478202_;
   }

   private void resetNumberOfRestocks() {
      this.catchUpDemand();
      this.numberOfRestocksToday = 0;
   }

   public GossipContainer getGossips() {
      return this.gossips;
   }

   public void setGossips(GossipContainer p_479689_) {
      this.gossips.putAll(p_479689_);
   }

   @Override
   public void startSleeping(BlockPos p_478890_) {
      super.startSleeping(p_478890_);
      this.brain.setMemory(MemoryModuleType.LAST_SLEPT, this.level().getGameTime());
      this.brain.eraseMemory(MemoryModuleType.WALK_TARGET);
      this.brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
   }

   @Override
   public void stopSleeping() {
      super.stopSleeping();
      this.brain.setMemory(MemoryModuleType.LAST_WOKEN, this.level().getGameTime());
   }

   private boolean golemSpawnConditionsMet(long p_479888_) {
      Optional<Long> optional = this.brain.getMemory(MemoryModuleType.LAST_SLEPT);
      return optional.filter(p_479120_ -> p_479888_ - p_479120_ < 24000L).isPresent();
   }

   @Nullable
   @Override
   public <T> T get(DataComponentType<? extends T> p_481007_) {
      return p_481007_ == DataComponents.VILLAGER_VARIANT
         ? castComponentValue((DataComponentType<T>)p_481007_, this.getVillagerData().type())
         : super.get(p_481007_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_478383_) {
      this.applyImplicitComponentIfPresent(p_478383_, DataComponents.VILLAGER_VARIANT);
      super.applyImplicitComponents(p_478383_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_480609_, T p_482110_) {
      if (p_480609_ == DataComponents.VILLAGER_VARIANT) {
         Holder<VillagerType> holder = castComponentValue(DataComponents.VILLAGER_VARIANT, p_482110_);
         this.setVillagerData(this.getVillagerData().withType(holder));
         return true;
      } else {
         return super.applyImplicitComponent(p_480609_, p_482110_);
      }
   }
}
