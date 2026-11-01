package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jspecify.annotations.Nullable;

public interface GameTestHelperExtension {
   private GameTestHelper self() {
      return (GameTestHelper)this;
   }

   default void fail(String message, BlockPos pos) {
      this.self().fail(Component.literal(message), pos);
   }

   default void fail(String message, Entity entity) {
      this.self().fail(Component.literal(message), entity);
   }

   @Nullable
   default <T, C> T getCapability(BlockCapability<T, C> cap, BlockPos pos, C context) {
      return this.self().getLevel().getCapability(cap, this.self().absolutePos(pos), context);
   }

   default <T, C> T requireCapability(BlockCapability<T, C> cap, BlockPos pos, C context) {
      T capability = this.getCapability(cap, pos, context);
      if (capability == null) {
         throw this.self().assertionException(pos, "Expected capability %s but there was none", cap);
      } else {
         return capability;
      }
   }
}
