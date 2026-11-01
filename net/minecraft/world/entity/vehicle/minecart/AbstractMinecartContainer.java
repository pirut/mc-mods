package net.minecraft.world.entity.vehicle.minecart;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractMinecartContainer extends AbstractMinecart implements ContainerEntity {
   private NonNullList<ItemStack> itemStacks = NonNullList.withSize(36, ItemStack.EMPTY);
   @Nullable
   private ResourceKey<LootTable> lootTable;
   private long lootTableSeed;

   protected AbstractMinecartContainer(EntityType<?> p_478558_, Level p_479468_) {
      super(p_478558_, p_479468_);
   }

   @Override
   public void destroy(ServerLevel p_479068_, DamageSource p_479600_) {
      super.destroy(p_479068_, p_479600_);
      this.chestVehicleDestroyed(p_479600_, p_479068_, this);
   }

   @Override
   public ItemStack getItem(int p_479564_) {
      return this.getChestVehicleItem(p_479564_);
   }

   @Override
   public ItemStack removeItem(int p_481685_, int p_479184_) {
      return this.removeChestVehicleItem(p_481685_, p_479184_);
   }

   @Override
   public ItemStack removeItemNoUpdate(int p_481088_) {
      return this.removeChestVehicleItemNoUpdate(p_481088_);
   }

   @Override
   public void setItem(int p_480304_, ItemStack p_479072_) {
      this.setChestVehicleItem(p_480304_, p_479072_);
   }

   @Override
   public SlotAccess getSlot(int p_480467_) {
      return this.getChestVehicleSlot(p_480467_);
   }

   @Override
   public void setChanged() {
   }

   @Override
   public boolean stillValid(Player p_479196_) {
      return this.isChestVehicleStillValid(p_479196_);
   }

   @Override
   public void remove(Entity.RemovalReason p_478170_) {
      if (!this.level().isClientSide() && p_478170_.shouldDestroy()) {
         Containers.dropContents(this.level(), this, this);
      }

      super.remove(p_478170_);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_481932_) {
      super.addAdditionalSaveData(p_481932_);
      this.addChestVehicleSaveData(p_481932_);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_478159_) {
      super.readAdditionalSaveData(p_478159_);
      this.readChestVehicleSaveData(p_478159_);
   }

   @Override
   public InteractionResult interact(Player p_481258_, InteractionHand p_478688_) {
      InteractionResult ret = super.interact(p_481258_, p_478688_);
      return ret.consumesAction() ? ret : this.interactWithContainerVehicle(p_481258_);
   }

   @Override
   protected Vec3 applyNaturalSlowdown(Vec3 p_478392_) {
      float f = 0.98F;
      if (this.lootTable == null) {
         int i = 15 - AbstractContainerMenu.getRedstoneSignalFromContainer(this);
         f += i * 0.001F;
      }

      if (this.isInWater()) {
         f *= 0.95F;
      }

      return p_478392_.multiply(f, 0.0, f);
   }

   @Override
   public void clearContent() {
      this.clearChestVehicleContent();
   }

   public void setLootTable(ResourceKey<LootTable> p_478976_, long p_479370_) {
      this.lootTable = p_478976_;
      this.lootTableSeed = p_479370_;
   }

   @Nullable
   @Override
   public AbstractContainerMenu createMenu(int p_478001_, Inventory p_478975_, Player p_478152_) {
      if (this.lootTable != null && p_478152_.isSpectator()) {
         return null;
      } else {
         this.unpackChestVehicleLootTable(p_478975_.player);
         return this.createMenu(p_478001_, p_478975_);
      }
   }

   protected abstract AbstractContainerMenu createMenu(int var1, Inventory var2);

   @Nullable
   @Override
   public ResourceKey<LootTable> getContainerLootTable() {
      return this.lootTable;
   }

   @Override
   public void setContainerLootTable(@Nullable ResourceKey<LootTable> p_481081_) {
      this.lootTable = p_481081_;
   }

   @Override
   public long getContainerLootTableSeed() {
      return this.lootTableSeed;
   }

   @Override
   public void setContainerLootTableSeed(long p_480912_) {
      this.lootTableSeed = p_480912_;
   }

   @Override
   public NonNullList<ItemStack> getItemStacks() {
      return this.itemStacks;
   }

   @Override
   public void clearItemStacks() {
      this.itemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
   }
}
