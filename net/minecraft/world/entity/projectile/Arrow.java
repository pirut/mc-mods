package net.minecraft.world.entity.projectile;

import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;

public class Arrow extends AbstractArrow {
   private static final int EXPOSED_POTION_DECAY_TIME = 600;
   private static final int NO_EFFECT_COLOR = -1;
   private static final EntityDataAccessor<Integer> ID_EFFECT_COLOR = SynchedEntityData.defineId(Arrow.class, EntityDataSerializers.INT);
   private static final byte EVENT_POTION_PUFF = 0;

   public Arrow(EntityType<? extends Arrow> p_36858_, Level p_36859_) {
      super(p_36858_, p_36859_);
   }

   public Arrow(Level p_36861_, double p_345459_, double p_346135_, double p_345618_, ItemStack p_309188_, @Nullable ItemStack p_345233_) {
      super(EntityType.ARROW, p_345459_, p_346135_, p_345618_, p_36861_, p_309188_, p_345233_);
      this.updateColor();
   }

   public Arrow(Level p_36866_, LivingEntity p_345746_, ItemStack p_309200_, @Nullable ItemStack p_345461_) {
      super(EntityType.ARROW, p_345746_, p_36866_, p_309200_, p_345461_);
      this.updateColor();
   }

   private PotionContents getPotionContents() {
      return this.getPickupItemStackOrigin().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
   }

   private void setPotionContents(PotionContents p_331534_) {
      this.getPickupItemStackOrigin().set(DataComponents.POTION_CONTENTS, p_331534_);
      this.updateColor();
   }

   @Override
   protected void setPickupItemStack(ItemStack p_331667_) {
      super.setPickupItemStack(p_331667_);
      this.updateColor();
   }

   private void updateColor() {
      PotionContents $$0 = this.getPotionContents();
      this.entityData.set(ID_EFFECT_COLOR, $$0.equals(PotionContents.EMPTY) ? -1 : $$0.getColor());
   }

   public void addEffect(MobEffectInstance p_36871_) {
      this.setPotionContents(this.getPotionContents().withEffectAdded(p_36871_));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_326324_) {
      super.defineSynchedData(p_326324_);
      p_326324_.define(ID_EFFECT_COLOR, -1);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide) {
         if (this.inGround) {
            if (this.inGroundTime % 5 == 0) {
               this.makeParticle(1);
            }
         } else {
            this.makeParticle(2);
         }
      } else if (this.inGround && this.inGroundTime != 0 && !this.getPotionContents().equals(PotionContents.EMPTY) && this.inGroundTime >= 600) {
         this.level().broadcastEntityEvent(this, (byte)0);
         this.setPickupItemStack(new ItemStack(Items.ARROW));
      }
   }

   private void makeParticle(int p_36877_) {
      int $$1 = this.getColor();
      if ($$1 != -1 && p_36877_ > 0) {
         for (int $$2 = 0; $$2 < p_36877_; $$2++) {
            this.level()
               .addParticle(
                  ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, $$1), this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 0.0, 0.0, 0.0
               );
         }
      }
   }

   public int getColor() {
      return this.entityData.get(ID_EFFECT_COLOR);
   }

   @Override
   protected void doPostHurtEffects(LivingEntity p_36873_) {
      super.doPostHurtEffects(p_36873_);
      Entity $$1 = this.getEffectSource();
      PotionContents $$2 = this.getPotionContents();
      if ($$2.potion().isPresent()) {
         for (MobEffectInstance $$3 : $$2.potion().get().value().getEffects()) {
            p_36873_.addEffect(
               new MobEffectInstance(
                  $$3.getEffect(), Math.max($$3.mapDuration(p_268168_ -> p_268168_ / 8), 1), $$3.getAmplifier(), $$3.isAmbient(), $$3.isVisible()
               ),
               $$1
            );
         }
      }

      for (MobEffectInstance $$4 : $$2.customEffects()) {
         p_36873_.addEffect($$4, $$1);
      }
   }

   @Override
   protected ItemStack getDefaultPickupItem() {
      return new ItemStack(Items.ARROW);
   }

   @Override
   public void handleEntityEvent(byte p_36869_) {
      if (p_36869_ == 0) {
         int $$1 = this.getColor();
         if ($$1 != -1) {
            float $$2 = ($$1 >> 16 & 0xFF) / 255.0F;
            float $$3 = ($$1 >> 8 & 0xFF) / 255.0F;
            float $$4 = ($$1 >> 0 & 0xFF) / 255.0F;

            for (int $$5 = 0; $$5 < 20; $$5++) {
               this.level()
                  .addParticle(
                     ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, $$2, $$3, $$4),
                     this.getRandomX(0.5),
                     this.getRandomY(),
                     this.getRandomZ(0.5),
                     0.0,
                     0.0,
                     0.0
                  );
            }
         }
      } else {
         super.handleEntityEvent(p_36869_);
      }
   }
}
