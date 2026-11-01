package net.minecraft.world.entity.npc.villager;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import org.jspecify.annotations.Nullable;

public abstract class AbstractVillager extends AgeableMob implements InventoryCarrier, Npc, Merchant {
   private static final EntityDataAccessor<Integer> DATA_UNHAPPY_COUNTER = SynchedEntityData.defineId(AbstractVillager.class, EntityDataSerializers.INT);
   public static final int VILLAGER_SLOT_OFFSET = 300;
   private static final int VILLAGER_INVENTORY_SIZE = 8;
   @Nullable
   private Player tradingPlayer;
   @Nullable
   protected MerchantOffers offers;
   private final SimpleContainer inventory = new SimpleContainer(8);

   public AbstractVillager(EntityType<? extends AbstractVillager> p_480428_, Level p_480437_) {
      super(p_480428_, p_480437_);
      this.setPathfindingMalus(PathType.DANGER_FIRE, 16.0F);
      this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_480749_, DifficultyInstance p_481930_, EntitySpawnReason p_477981_, @Nullable SpawnGroupData p_481717_
   ) {
      if (p_481717_ == null) {
         p_481717_ = new AgeableMob.AgeableMobGroupData(false);
      }

      return super.finalizeSpawn(p_480749_, p_481930_, p_477981_, p_481717_);
   }

   public int getUnhappyCounter() {
      return this.entityData.get(DATA_UNHAPPY_COUNTER);
   }

   public void setUnhappyCounter(int p_477945_) {
      this.entityData.set(DATA_UNHAPPY_COUNTER, p_477945_);
   }

   @Override
   public int getVillagerXp() {
      return 0;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_481565_) {
      super.defineSynchedData(p_481565_);
      p_481565_.define(DATA_UNHAPPY_COUNTER, 0);
   }

   @Override
   public void setTradingPlayer(@Nullable Player p_480841_) {
      this.tradingPlayer = p_480841_;
   }

   @Nullable
   @Override
   public Player getTradingPlayer() {
      return this.tradingPlayer;
   }

   public boolean isTrading() {
      return this.tradingPlayer != null;
   }

   @Override
   public MerchantOffers getOffers() {
      if (this.level() instanceof ServerLevel serverlevel) {
         if (this.offers == null) {
            this.offers = new MerchantOffers();
            this.updateTrades(serverlevel);
         }

         return this.offers;
      } else {
         throw new IllegalStateException("Cannot load Villager offers on the client");
      }
   }

   @Override
   public void overrideOffers(@Nullable MerchantOffers p_479937_) {
   }

   @Override
   public void overrideXp(int p_480746_) {
   }

   @Override
   public void notifyTrade(MerchantOffer p_478567_) {
      p_478567_.increaseUses();
      this.ambientSoundTime = -this.getAmbientSoundInterval();
      this.rewardTradeXp(p_478567_);
      if (this.tradingPlayer instanceof ServerPlayer) {
         CriteriaTriggers.TRADE.trigger((ServerPlayer)this.tradingPlayer, this, p_478567_.getResult());
      }

      NeoForge.EVENT_BUS.post(new TradeWithVillagerEvent(this.tradingPlayer, p_478567_, this));
   }

   protected abstract void rewardTradeXp(MerchantOffer var1);

   @Override
   public boolean showProgressBar() {
      return true;
   }

   @Override
   public void notifyTradeUpdated(ItemStack p_482127_) {
      if (!this.level().isClientSide() && this.ambientSoundTime > -this.getAmbientSoundInterval() + 20) {
         this.ambientSoundTime = -this.getAmbientSoundInterval();
         this.makeSound(this.getTradeUpdatedSound(!p_482127_.isEmpty()));
      }
   }

   @Override
   public SoundEvent getNotifyTradeSound() {
      return SoundEvents.VILLAGER_YES;
   }

   protected SoundEvent getTradeUpdatedSound(boolean p_478224_) {
      return p_478224_ ? SoundEvents.VILLAGER_YES : SoundEvents.VILLAGER_NO;
   }

   public void playCelebrateSound() {
      this.makeSound(SoundEvents.VILLAGER_CELEBRATE);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_478541_) {
      super.addAdditionalSaveData(p_478541_);
      if (!this.level().isClientSide()) {
         MerchantOffers merchantoffers = this.getOffers();
         if (!merchantoffers.isEmpty()) {
            p_478541_.store("Offers", MerchantOffers.CODEC, merchantoffers);
         }
      }

      this.writeInventoryToTag(p_478541_);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_477915_) {
      super.readAdditionalSaveData(p_477915_);
      this.offers = p_477915_.<MerchantOffers>read("Offers", MerchantOffers.CODEC).orElse(null);
      this.readInventoryFromTag(p_477915_);
   }

   @Nullable
   @Override
   public Entity teleport(TeleportTransition p_480216_) {
      this.stopTrading();
      return super.teleport(p_480216_);
   }

   protected void stopTrading() {
      this.setTradingPlayer(null);
   }

   @Override
   public void die(DamageSource p_480989_) {
      super.die(p_480989_);
      this.stopTrading();
   }

   protected void addParticlesAroundSelf(ParticleOptions p_480291_) {
      for (int i = 0; i < 5; i++) {
         double d0 = this.random.nextGaussian() * 0.02;
         double d1 = this.random.nextGaussian() * 0.02;
         double d2 = this.random.nextGaussian() * 0.02;
         this.level().addParticle(p_480291_, this.getRandomX(1.0), this.getRandomY() + 1.0, this.getRandomZ(1.0), d0, d1, d2);
      }
   }

   @Override
   public boolean canBeLeashed() {
      return false;
   }

   @Override
   public SimpleContainer getInventory() {
      return this.inventory;
   }

   @Nullable
   @Override
   public SlotAccess getSlot(int p_478922_) {
      int i = p_478922_ - 300;
      return i >= 0 && i < this.inventory.getContainerSize() ? this.inventory.getSlot(i) : super.getSlot(p_478922_);
   }

   protected abstract void updateTrades(ServerLevel var1);

   protected void addOffersFromItemListings(ServerLevel p_480295_, MerchantOffers p_479439_, VillagerTrades.ItemListing[] p_479360_, int p_481834_) {
      ArrayList<VillagerTrades.ItemListing> arraylist = Lists.newArrayList(p_479360_);
      int i = 0;

      while (i < p_481834_ && !arraylist.isEmpty()) {
         MerchantOffer merchantoffer = arraylist.remove(this.random.nextInt(arraylist.size())).getOffer(p_480295_, this, this.random);
         if (merchantoffer != null) {
            p_479439_.add(merchantoffer);
            i++;
         }
      }
   }

   @Override
   public Vec3 getRopeHoldPosition(float p_478384_) {
      float f = Mth.lerp(p_478384_, this.yBodyRotO, this.yBodyRot) * (float) (Math.PI / 180.0);
      Vec3 vec3 = new Vec3(0.0, this.getBoundingBox().getYsize() - 1.0, 0.2);
      return this.getPosition(p_478384_).add(vec3.yRot(-f));
   }

   @Override
   public boolean isClientSide() {
      return this.level().isClientSide();
   }

   @Override
   public boolean stillValid(Player p_481031_) {
      return this.getTradingPlayer() == p_481031_ && this.isAlive() && p_481031_.isWithinEntityInteractionRange(this, 4.0);
   }
}
