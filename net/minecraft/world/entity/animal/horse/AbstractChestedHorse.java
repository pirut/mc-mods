package net.minecraft.world.entity.animal.horse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public abstract class AbstractChestedHorse extends AbstractHorse {
   private static final EntityDataAccessor<Boolean> DATA_ID_CHEST = SynchedEntityData.defineId(AbstractChestedHorse.class, EntityDataSerializers.BOOLEAN);
   private final EntityDimensions babyDimensions;

   protected AbstractChestedHorse(EntityType<? extends AbstractChestedHorse> p_30485_, Level p_30486_) {
      super(p_30485_, p_30486_);
      this.canGallop = false;
      this.babyDimensions = p_30485_.getDimensions()
         .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, p_30485_.getHeight() - 0.15625F, 0.0F))
         .scale(0.5F);
   }

   @Override
   protected void randomizeAttributes(RandomSource p_218803_) {
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(generateMaxHealth(p_218803_::nextInt));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_326402_) {
      super.defineSynchedData(p_326402_);
      p_326402_.define(DATA_ID_CHEST, false);
   }

   public static AttributeSupplier.Builder createBaseChestedHorseAttributes() {
      return createBaseHorseAttributes().add(Attributes.MOVEMENT_SPEED, 0.175F).add(Attributes.JUMP_STRENGTH, 0.5);
   }

   public boolean hasChest() {
      return this.entityData.get(DATA_ID_CHEST);
   }

   public void setChest(boolean p_30505_) {
      this.entityData.set(DATA_ID_CHEST, p_30505_);
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose p_316142_) {
      return this.isBaby() ? this.babyDimensions : super.getDefaultDimensions(p_316142_);
   }

   @Override
   protected void dropEquipment() {
      super.dropEquipment();
      if (this.hasChest()) {
         if (!this.level().isClientSide) {
            this.spawnAtLocation(Blocks.CHEST);
         }

         this.setChest(false);
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag p_30496_) {
      super.addAdditionalSaveData(p_30496_);
      p_30496_.putBoolean("ChestedHorse", this.hasChest());
      if (this.hasChest()) {
         ListTag $$1 = new ListTag();

         for (int $$2 = 1; $$2 < this.inventory.getContainerSize(); $$2++) {
            ItemStack $$3 = this.inventory.getItem($$2);
            if (!$$3.isEmpty()) {
               CompoundTag $$4 = new CompoundTag();
               $$4.putByte("Slot", (byte)($$2 - 1));
               $$1.add($$3.save(this.registryAccess(), $$4));
            }
         }

         p_30496_.put("Items", $$1);
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag p_30488_) {
      super.readAdditionalSaveData(p_30488_);
      this.setChest(p_30488_.getBoolean("ChestedHorse"));
      this.createInventory();
      if (this.hasChest()) {
         ListTag $$1 = p_30488_.getList("Items", 10);

         for (int $$2 = 0; $$2 < $$1.size(); $$2++) {
            CompoundTag $$3 = $$1.getCompound($$2);
            int $$4 = $$3.getByte("Slot") & 255;
            if ($$4 < this.inventory.getContainerSize() - 1) {
               this.inventory.setItem($$4 + 1, ItemStack.parse(this.registryAccess(), $$3).orElse(ItemStack.EMPTY));
            }
         }
      }

      this.syncSaddleToClients();
   }

   @Override
   public SlotAccess getSlot(int p_149479_) {
      return p_149479_ == 499 ? new SlotAccess() {
         @Override
         public ItemStack get() {
            return AbstractChestedHorse.this.hasChest() ? new ItemStack(Items.CHEST) : ItemStack.EMPTY;
         }

         @Override
         public boolean set(ItemStack p_149485_) {
            if (p_149485_.isEmpty()) {
               if (AbstractChestedHorse.this.hasChest()) {
                  AbstractChestedHorse.this.setChest(false);
                  AbstractChestedHorse.this.createInventory();
               }

               return true;
            } else if (p_149485_.is(Items.CHEST)) {
               if (!AbstractChestedHorse.this.hasChest()) {
                  AbstractChestedHorse.this.setChest(true);
                  AbstractChestedHorse.this.createInventory();
               }

               return true;
            } else {
               return false;
            }
         }
      } : super.getSlot(p_149479_);
   }

   @Override
   public InteractionResult mobInteract(Player p_30493_, InteractionHand p_30494_) {
      boolean $$2 = !this.isBaby() && this.isTamed() && p_30493_.isSecondaryUseActive();
      if (!this.isVehicle() && !$$2) {
         ItemStack $$3 = p_30493_.getItemInHand(p_30494_);
         if (!$$3.isEmpty()) {
            if (this.isFood($$3)) {
               return this.fedFood(p_30493_, $$3);
            }

            if (!this.isTamed()) {
               this.makeMad();
               return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            if (!this.hasChest() && $$3.is(Items.CHEST)) {
               this.equipChest(p_30493_, $$3);
               return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
         }

         return super.mobInteract(p_30493_, p_30494_);
      } else {
         return super.mobInteract(p_30493_, p_30494_);
      }
   }

   private void equipChest(Player p_250937_, ItemStack p_251558_) {
      this.setChest(true);
      this.playChestEquipsSound();
      p_251558_.consume(1, p_250937_);
      this.createInventory();
   }

   protected void playChestEquipsSound() {
      this.playSound(SoundEvents.DONKEY_CHEST, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
   }

   @Override
   public int getInventoryColumns() {
      return this.hasChest() ? 5 : 0;
   }
}
