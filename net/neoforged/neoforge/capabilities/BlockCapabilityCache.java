package net.neoforged.neoforge.capabilities;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

public final class BlockCapabilityCache<T, C> {
   private final BlockCapability<T, C> capability;
   private final ServerLevel level;
   private final BlockPos pos;
   private final C context;
   private boolean cacheValid = false;
   @Nullable
   private T cachedCap = (T)null;
   private boolean canQuery = true;
   private final ICapabilityInvalidationListener listener;

   public static <T, C> BlockCapabilityCache<T, C> create(BlockCapability<T, C> capability, ServerLevel level, BlockPos pos, C context) {
      return create(capability, level, pos, context, () -> true, () -> {});
   }

   public static <T, C> BlockCapabilityCache<T, C> create(
      BlockCapability<T, C> capability, ServerLevel level, BlockPos pos, C context, BooleanSupplier isValid, Runnable invalidationListener
   ) {
      Objects.requireNonNull(capability);
      Objects.requireNonNull(isValid);
      Objects.requireNonNull(invalidationListener);
      pos = pos.immutable();
      BlockCapabilityCache<T, C> cache = new BlockCapabilityCache<>(capability, level, pos, context, isValid, invalidationListener);
      level.registerCapabilityListener(pos, cache.listener);
      return cache;
   }

   private BlockCapabilityCache(
      BlockCapability<T, C> capability, ServerLevel level, BlockPos pos, C context, BooleanSupplier isValid, Runnable invalidationListener
   ) {
      this.capability = capability;
      this.level = level;
      this.pos = pos;
      this.context = context;
      this.listener = () -> {
         if (!this.cacheValid) {
            return isValid.getAsBoolean();
         } else {
            this.canQuery = false;
            this.cacheValid = false;
            this.cachedCap = null;
            if (isValid.getAsBoolean()) {
               invalidationListener.run();
               this.canQuery = true;
               return true;
            } else {
               return false;
            }
         }
      };
   }

   public ServerLevel level() {
      return this.level;
   }

   public BlockPos pos() {
      return this.pos;
   }

   public C context() {
      return this.context;
   }

   @Nullable
   public T getCapability() {
      if (!this.canQuery) {
         throw new IllegalStateException("Do not call getCapability on an invalid cache or from the invalidation listener!");
      } else {
         if (!this.cacheValid) {
            if (!this.level.isLoaded(this.pos)) {
               this.cachedCap = null;
            } else {
               this.cachedCap = this.level.getCapability(this.capability, this.pos, this.context);
            }

            this.cacheValid = true;
         }

         return this.cachedCap;
      }
   }
}
