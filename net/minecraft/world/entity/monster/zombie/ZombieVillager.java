package net.minecraft.world.entity.monster.zombie;

import com.google.common.annotations.VisibleForTesting;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.gossip.GossipContainer;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerDataHolder;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

public class ZombieVillager extends Zombie implements VillagerDataHolder {
   private static final EntityDataAccessor<Boolean> DATA_CONVERTING_ID = SynchedEntityData.defineId(ZombieVillager.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<VillagerData> DATA_VILLAGER_DATA = SynchedEntityData.defineId(
      ZombieVillager.class, EntityDataSerializers.VILLAGER_DATA
   );
   private static final int VILLAGER_CONVERSION_WAIT_MIN = 3600;
   private static final int VILLAGER_CONVERSION_WAIT_MAX = 6000;
   private static final int MAX_SPECIAL_BLOCKS_COUNT = 14;
   private static final int SPECIAL_BLOCK_RADIUS = 4;
   private static final int NOT_CONVERTING = -1;
   private static final int DEFAULT_XP = 0;
   private static final Set<EntitySpawnReason> REASONS_NOT_TO_SET_TYPE = EnumSet.of(
      EntitySpawnReason.LOAD,
      EntitySpawnReason.DIMENSION_TRAVEL,
      EntitySpawnReason.CONVERSION,
      EntitySpawnReason.SPAWN_ITEM_USE,
      EntitySpawnReason.SPAWNER,
      EntitySpawnReason.TRIAL_SPAWNER
   );
   private int villagerConversionTime;
   @Nullable
   private UUID conversionStarter;
   @Nullable
   private GossipContainer gossips;
   @Nullable
   private MerchantOffers tradeOffers;
   private int villagerXp = 0;

   public ZombieVillager(EntityType<? extends ZombieVillager> p_481402_, Level p_479747_) {
      super(p_481402_, p_479747_);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_479355_) {
      super.defineSynchedData(p_479355_);
      p_479355_.define(DATA_CONVERTING_ID, false);
      p_479355_.define(DATA_VILLAGER_DATA, this.initializeVillagerData());
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_482043_) {
      super.addAdditionalSaveData(p_482043_);
      p_482043_.store("VillagerData", VillagerData.CODEC, this.getVillagerData());
      p_482043_.storeNullable("Offers", MerchantOffers.CODEC, this.tradeOffers);
      p_482043_.storeNullable("Gossips", GossipContainer.CODEC, this.gossips);
      p_482043_.putInt("ConversionTime", this.isConverting() ? this.villagerConversionTime : -1);
      p_482043_.storeNullable("ConversionPlayer", UUIDUtil.CODEC, this.conversionStarter);
      p_482043_.putInt("Xp", this.villagerXp);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_480520_) {
      super.readAdditionalSaveData(p_480520_);
      this.entityData.set(DATA_VILLAGER_DATA, p_480520_.<VillagerData>read("VillagerData", VillagerData.CODEC).orElseGet(this::initializeVillagerData));
      this.tradeOffers = p_480520_.<MerchantOffers>read("Offers", MerchantOffers.CODEC).orElse(null);
      this.gossips = p_480520_.<GossipContainer>read("Gossips", GossipContainer.CODEC).orElse(null);
      int i = p_480520_.getIntOr("ConversionTime", -1);
      if (i != -1) {
         UUID uuid = p_480520_.<UUID>read("ConversionPlayer", UUIDUtil.CODEC).orElse(null);
         this.startConverting(uuid, i);
      } else {
         this.getEntityData().set(DATA_CONVERTING_ID, false);
         this.villagerConversionTime = -1;
      }

      this.villagerXp = p_480520_.getIntOr("Xp", 0);
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_482360_, DifficultyInstance p_482357_, EntitySpawnReason p_482369_, @Nullable SpawnGroupData p_482358_
   ) {
      if (!REASONS_NOT_TO_SET_TYPE.contains(p_482369_)) {
         this.setVillagerData(this.getVillagerData().withType(p_482360_.registryAccess(), VillagerType.byBiome(p_482360_.getBiome(this.blockPosition()))));
      }

      return super.finalizeSpawn(p_482360_, p_482357_, p_482369_, p_482358_);
   }

   private VillagerData initializeVillagerData() {
      Optional<Holder.Reference<VillagerProfession>> optional = BuiltInRegistries.VILLAGER_PROFESSION.getRandom(this.random);
      VillagerData villagerdata = Villager.createDefaultVillagerData();
      if (optional.isPresent()) {
         villagerdata = villagerdata.withProfession(optional.get());
      }

      return villagerdata;
   }

   @Override
   public void tick() {
      if (!this.level().isClientSide() && this.isAlive() && this.isConverting()) {
         int i = this.getConversionProgress();
         this.villagerConversionTime -= i;
         if (this.villagerConversionTime <= 0 && EventHooks.canLivingConvert(this, EntityType.VILLAGER, timer -> this.villagerConversionTime = timer)) {
            this.finishConversion((ServerLevel)this.level());
         }
      }

      super.tick();
   }

   @Override
   public InteractionResult mobInteract(Player p_478614_, InteractionHand p_480747_) {
      ItemStack itemstack = p_478614_.getItemInHand(p_480747_);
      if (itemstack.is(Items.GOLDEN_APPLE)) {
         if (this.hasEffect(MobEffects.WEAKNESS)) {
            itemstack.consume(1, p_478614_);
            if (!this.level().isClientSide()) {
               this.startConverting(p_478614_.getUUID(), this.random.nextInt(2401) + 3600);
            }

            return InteractionResult.SUCCESS_SERVER;
         } else {
            return InteractionResult.CONSUME;
         }
      } else {
         return super.mobInteract(p_478614_, p_480747_);
      }
   }

   @Override
   protected boolean convertsInWater() {
      return false;
   }

   @Override
   public boolean removeWhenFarAway(double p_479135_) {
      return !this.isConverting() && this.villagerXp == 0;
   }

   public boolean isConverting() {
      return this.getEntityData().get(DATA_CONVERTING_ID);
   }

   private void startConverting(@Nullable UUID p_480632_, int p_479980_) {
      this.conversionStarter = p_480632_;
      this.villagerConversionTime = p_479980_;
      this.getEntityData().set(DATA_CONVERTING_ID, true);
      this.removeEffect(MobEffects.WEAKNESS);
      this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, p_479980_, Math.min(this.level().getDifficulty().getId() - 1, 0)));
      this.level().broadcastEntityEvent(this, (byte)16);
   }

   @Override
   public void handleEntityEvent(byte p_480934_) {
      if (p_480934_ == 16) {
         if (!this.isSilent()) {
            this.level()
               .playLocalSound(
                  this.getX(),
                  this.getEyeY(),
                  this.getZ(),
                  SoundEvents.ZOMBIE_VILLAGER_CURE,
                  this.getSoundSource(),
                  1.0F + this.random.nextFloat(),
                  this.random.nextFloat() * 0.7F + 0.3F,
                  false
               );
         }
      } else {
         super.handleEntityEvent(p_480934_);
      }
   }

   private void finishConversion(ServerLevel p_479576_) {
      this.convertTo(
         EntityType.VILLAGER,
         ConversionParams.single(this, false, false),
         p_480457_ -> {
            for (EquipmentSlot equipmentslot : this.dropPreservedEquipment(
               p_479576_, p_479517_ -> !EnchantmentHelper.has(p_479517_, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)
            )) {
               SlotAccess slotaccess = p_480457_.getSlot(equipmentslot.getIndex() + 300);
               if (slotaccess != null) {
                  slotaccess.set(this.getItemBySlot(equipmentslot));
               }
            }

            p_480457_.setVillagerData(this.getVillagerData());
            if (this.gossips != null) {
               p_480457_.setGossips(this.gossips);
            }

            if (this.tradeOffers != null) {
               p_480457_.setOffers(this.tradeOffers.copy());
            }

            p_480457_.setVillagerXp(this.villagerXp);
            p_480457_.finalizeSpawn(p_479576_, p_479576_.getCurrentDifficultyAt(p_480457_.blockPosition()), EntitySpawnReason.CONVERSION, null);
            p_480457_.refreshBrain(p_479576_);
            if (this.conversionStarter != null) {
               Player player = p_479576_.getPlayerByUUID(this.conversionStarter);
               if (player instanceof ServerPlayer) {
                  CriteriaTriggers.CURED_ZOMBIE_VILLAGER.trigger((ServerPlayer)player, this, p_480457_);
                  p_479576_.onReputationEvent(ReputationEventType.ZOMBIE_VILLAGER_CURED, player, p_480457_);
               }
            }

            p_480457_.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
            if (!this.isSilent()) {
               p_479576_.levelEvent(null, 1027, this.blockPosition(), 0);
            }

            EventHooks.onLivingConvert(this, p_480457_);
         }
      );
   }

   @VisibleForTesting
   public void setVillagerConversionTime(int p_481687_) {
      this.villagerConversionTime = p_481687_;
   }

   private int getConversionProgress() {
      int i = 1;
      if (this.random.nextFloat() < 0.01F) {
         int j = 0;
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

         for (int k = (int)this.getX() - 4; k < (int)this.getX() + 4 && j < 14; k++) {
            for (int l = (int)this.getY() - 4; l < (int)this.getY() + 4 && j < 14; l++) {
               for (int i1 = (int)this.getZ() - 4; i1 < (int)this.getZ() + 4 && j < 14; i1++) {
                  BlockState blockstate = this.level().getBlockState(blockpos$mutableblockpos.set(k, l, i1));
                  if (blockstate.is(Blocks.IRON_BARS) || blockstate.getBlock() instanceof BedBlock) {
                     if (this.random.nextFloat() < 0.3F) {
                        i++;
                     }

                     j++;
                  }
               }
            }
         }
      }

      return i;
   }

   @Override
   public float getVoicePitch() {
      return this.isBaby()
         ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 2.0F
         : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
   }

   @Override
   public SoundEvent getAmbientSound() {
      return SoundEvents.ZOMBIE_VILLAGER_AMBIENT;
   }

   @Override
   public SoundEvent getHurtSound(DamageSource p_479574_) {
      return SoundEvents.ZOMBIE_VILLAGER_HURT;
   }

   @Override
   public SoundEvent getDeathSound() {
      return SoundEvents.ZOMBIE_VILLAGER_DEATH;
   }

   @Override
   public SoundEvent getStepSound() {
      return SoundEvents.ZOMBIE_VILLAGER_STEP;
   }

   public void setTradeOffers(MerchantOffers p_479377_) {
      this.tradeOffers = p_479377_;
   }

   public void setGossips(GossipContainer p_481889_) {
      this.gossips = p_481889_;
   }

   @Override
   public void setVillagerData(VillagerData p_478316_) {
      VillagerData villagerdata = this.getVillagerData();
      if (!villagerdata.profession().equals(p_478316_.profession())) {
         this.tradeOffers = null;
      }

      this.entityData.set(DATA_VILLAGER_DATA, p_478316_);
   }

   @Override
   public VillagerData getVillagerData() {
      return this.entityData.get(DATA_VILLAGER_DATA);
   }

   public int getVillagerXp() {
      return this.villagerXp;
   }

   public void setVillagerXp(int p_481250_) {
      this.villagerXp = p_481250_;
   }

   @Nullable
   @Override
   public <T> T get(DataComponentType<? extends T> p_479721_) {
      return p_479721_ == DataComponents.VILLAGER_VARIANT
         ? castComponentValue((DataComponentType<T>)p_479721_, this.getVillagerData().type())
         : super.get(p_479721_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_478219_) {
      this.applyImplicitComponentIfPresent(p_478219_, DataComponents.VILLAGER_VARIANT);
      super.applyImplicitComponents(p_478219_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_481033_, T p_478194_) {
      if (p_481033_ == DataComponents.VILLAGER_VARIANT) {
         Holder<VillagerType> holder = castComponentValue(DataComponents.VILLAGER_VARIANT, p_478194_);
         this.setVillagerData(this.getVillagerData().withType(holder));
         return true;
      } else {
         return super.applyImplicitComponent(p_481033_, p_478194_);
      }
   }
}
