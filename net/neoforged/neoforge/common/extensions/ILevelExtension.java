package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.model.data.ModelDataManager;
import org.jspecify.annotations.Nullable;

public interface ILevelExtension {
   String TRANSLATION_PREFIX = "dimension";

   private Level self() {
      return (Level)this;
   }

   double getMaxEntityRadius();

   double increaseMaxEntityRadius(double var1);

   @Nullable
   default ModelDataManager getModelDataManager() {
      return null;
   }

   @Nullable
   default <T, C> T getCapability(BlockCapability<T, C> cap, BlockPos pos, C context) {
      return cap.getCapability(this.self(), pos, null, null, context);
   }

   @Nullable
   default <T, C> T getCapability(BlockCapability<T, C> cap, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity blockEntity, C context) {
      return cap.getCapability(this.self(), pos, state, blockEntity, context);
   }

   @Nullable
   default <T> T getCapability(BlockCapability<T, Void> cap, BlockPos pos) {
      return cap.getCapability(this.self(), pos, null, null, null);
   }

   @Nullable
   default <T> T getCapability(BlockCapability<T, Void> cap, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity blockEntity) {
      return cap.getCapability(this.self(), pos, state, blockEntity, null);
   }

   default void invalidateCapabilities(BlockPos pos) {
   }

   default void invalidateCapabilities(ChunkPos pos) {
   }

   default String getDescriptionKey() {
      return this.self().dimension().identifier().toLanguageKey("dimension");
   }

   default Component getDescription() {
      return Component.translatableWithFallback(this.getDescriptionKey(), this.self().dimension().identifier().toString());
   }
}
