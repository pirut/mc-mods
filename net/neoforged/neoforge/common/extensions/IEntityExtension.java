package net.neoforged.neoforge.common.extensions;

import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.attachment.AttachmentInternals;
import net.neoforged.neoforge.common.SoundAction;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.network.payload.AdvancedAddEntityPayload;
import org.jetbrains.annotations.Nullable;

public interface IEntityExtension extends INBTSerializable<CompoundTag> {
   private Entity self() {
      return (Entity)this;
   }

   @Deprecated(
      forRemoval = true,
      since = "21.0"
   )
   default void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
      this.self().load(nbt);
   }

   @Deprecated(
      forRemoval = true,
      since = "21.0"
   )
   default CompoundTag serializeNBT(HolderLookup.Provider provider) {
      CompoundTag ret = new CompoundTag();
      String id = this.self().getEncodeId();
      if (id != null) {
         ret.putString("id", this.self().getEncodeId());
      }

      return this.self().saveWithoutId(ret);
   }

   @Nullable
   Collection<ItemEntity> captureDrops();

   Collection<ItemEntity> captureDrops(@Nullable Collection<ItemEntity> var1);

   CompoundTag getPersistentData();

   default boolean shouldRiderSit() {
      return true;
   }

   @Nullable
   default ItemStack getPickedResult(HitResult target) {
      return this.self().getPickResult();
   }

   default boolean canRiderInteract() {
      return false;
   }

   default boolean canBeRiddenUnderFluidType(FluidType type, Entity rider) {
      return type.canRideVehicleUnder(this.self(), rider);
   }

   boolean canTrample(BlockState var1, BlockPos var2, float var3);

   default MobCategory getClassification(boolean forSpawnCount) {
      return this.self().getType().getCategory();
   }

   boolean isAddedToLevel();

   void onAddedToLevel();

   void onRemovedFromLevel();

   void revive();

   default boolean isMultipartEntity() {
      return false;
   }

   @Nullable
   default PartEntity<?>[] getParts() {
      return null;
   }

   double getFluidTypeHeight(FluidType var1);

   FluidType getMaxHeightFluidType();

   default boolean isInFluidType(FluidState state) {
      return this.isInFluidType(state.getFluidType());
   }

   default boolean isInFluidType(FluidType type) {
      return this.getFluidTypeHeight(type) > 0.0;
   }

   default boolean isInFluidType(BiPredicate<FluidType, Double> predicate) {
      return this.isInFluidType(predicate, false);
   }

   boolean isInFluidType(BiPredicate<FluidType, Double> var1, boolean var2);

   boolean isInFluidType();

   FluidType getEyeInFluidType();

   default boolean isEyeInFluidType(FluidType type) {
      return type == this.getEyeInFluidType();
   }

   default boolean canStartSwimming() {
      return !this.getEyeInFluidType().isAir()
         && this.canSwimInFluidType(this.getEyeInFluidType())
         && this.canSwimInFluidType(this.self().level().getFluidState(this.self().blockPosition()).getFluidType());
   }

   default double getFluidMotionScale(FluidType type) {
      return type.motionScale(this.self());
   }

   default boolean isPushedByFluid(FluidType type) {
      return this.self().isPushedByFluid() && type.canPushEntity(this.self());
   }

   default boolean canSwimInFluidType(FluidType type) {
      return type.canSwim(this.self());
   }

   default boolean canFluidExtinguish(FluidType type) {
      return type.canExtinguish(this.self());
   }

   default float getFluidFallDistanceModifier(FluidType type) {
      return type.getFallDistanceModifier(this.self());
   }

   default boolean canHydrateInFluidType(FluidType type) {
      return type.canHydrate(this.self());
   }

   @Nullable
   default SoundEvent getSoundFromFluidType(FluidType type, SoundAction action) {
      return type.getSound(this.self(), action);
   }

   default boolean hasCustomOutlineRendering(Player player) {
      return false;
   }

   default void sendPairingData(ServerPlayer serverPlayer, Consumer<CustomPacketPayload> bundleBuilder) {
      if (this instanceof IEntityWithComplexSpawn) {
         bundleBuilder.accept(new AdvancedAddEntityPayload(this.self()));
      }
   }

   default void copyAttachmentsFrom(Entity other, boolean isDeath) {
      AttachmentInternals.copyEntityAttachments(other, this.self(), isDeath);
   }
}
