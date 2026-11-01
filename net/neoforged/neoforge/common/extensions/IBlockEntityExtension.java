package net.neoforged.neoforge.common.extensions;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelDataManager;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

public interface IBlockEntityExtension {
   private BlockEntity self() {
      return (BlockEntity)this;
   }

   default void onDataPacket(Connection net, ValueInput valueInput) {
      this.self().loadWithComponents(valueInput);
   }

   default void handleUpdateTag(ValueInput input) {
      this.self().loadWithComponents(input);
   }

   CompoundTag getPersistentData();

   default void onChunkUnloaded() {
   }

   default void onLoad() {
      this.requestModelDataUpdate();
   }

   default void requestModelDataUpdate() {
      BlockEntity te = this.self();
      Level level = te.getLevel();
      if (level != null && level.isClientSide()) {
         ModelDataManager modelDataManager = level.getModelDataManager();
         if (modelDataManager != null) {
            modelDataManager.requestRefresh(te);
         }
      }
   }

   default ModelData getModelData() {
      return ModelData.EMPTY;
   }

   @NonExtendable
   default void invalidateCapabilities() {
      BlockEntity be = this.self();
      Level level = be.getLevel();
      if (level != null) {
         level.invalidateCapabilities(be.getBlockPos());
      }
   }

   default boolean hasCustomOutlineRendering(Player player) {
      return false;
   }
}
