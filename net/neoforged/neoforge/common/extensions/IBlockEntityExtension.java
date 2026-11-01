package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelDataManager;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

public interface IBlockEntityExtension {
   private BlockEntity self() {
      return (BlockEntity)this;
   }

   default void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
      CompoundTag compoundtag = pkt.getTag();
      if (!compoundtag.isEmpty()) {
         this.self().loadWithComponents(compoundtag, lookupProvider);
      }
   }

   default void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
      this.self().loadWithComponents(tag, lookupProvider);
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
      if (level != null && level.isClientSide) {
         ModelDataManager modelDataManager = level.getModelDataManager();
         if (modelDataManager != null) {
            modelDataManager.requestRefresh(te);
         }
      }
   }

   default ModelData getModelData() {
      return ModelData.EMPTY;
   }

   default boolean hasCustomOutlineRendering(Player player) {
      return false;
   }

   @NonExtendable
   default void invalidateCapabilities() {
      BlockEntity be = this.self();
      Level level = be.getLevel();
      if (level != null) {
         level.invalidateCapabilities(be.getBlockPos());
      }
   }
}
