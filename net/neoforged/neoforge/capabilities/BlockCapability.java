package net.neoforged.neoforge.capabilities;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public final class BlockCapability<T, C> extends BaseCapability<T, C> {
   private static final CapabilityRegistry<BlockCapability<?, ?>> registry = new CapabilityRegistry<>(BlockCapability::new);
   final Map<Block, List<IBlockCapabilityProvider<T, C>>> providers = new IdentityHashMap<>();
   private TriState proxyable = TriState.DEFAULT;

   public static <T, C> BlockCapability<T, C> create(Identifier name, Class<T> typeClass, Class<C> contextClass) {
      return (BlockCapability<T, C>)registry.create(name, typeClass, contextClass);
   }

   public static <T> BlockCapability<T, Void> createVoid(Identifier name, Class<T> typeClass) {
      return create(name, typeClass, void.class);
   }

   public static <T> BlockCapability<T, Direction> createSided(Identifier name, Class<T> typeClass) {
      return create(name, typeClass, Direction.class);
   }

   public static synchronized List<BlockCapability<?, ?>> getAll() {
      return registry.getAll();
   }

   public static synchronized List<BlockCapability<?, ?>> getAllProxyable() {
      return registry.getAll().stream().filter(BlockCapability::isProxyable).toList();
   }

   public boolean isProxyable() {
      return this.proxyable.isTrue();
   }

   private BlockCapability(Identifier name, Class<T> typeClass, Class<C> contextClass) {
      super(name, typeClass, contextClass);
   }

   void setProxyable(boolean proxyable) {
      if (CapabilityHooks.initFinished) {
         throw new IllegalStateException("Cannot call setProxyable after the RegisterCapabilitiesEvent has been fired.");
      } else {
         switch (this.proxyable) {
            case DEFAULT:
               this.proxyable = proxyable ? TriState.TRUE : TriState.FALSE;
               break;
            case TRUE:
               if (!proxyable) {
                  throw new IllegalStateException("Cannot make capability %s non-proxyable because it was already set to be proxyable.".formatted(this.name()));
               }
               break;
            case FALSE:
               if (proxyable) {
                  throw new IllegalStateException("Cannot make capability %s proxyable because it was already set to be non-proxyable.".formatted(this.name()));
               }
         }
      }
   }

   @Internal
   @Nullable
   public T getCapability(Level level, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity blockEntity, C context) {
      pos = pos.immutable();
      if (blockEntity == null) {
         if (state == null) {
            state = level.getBlockState(pos);
         }

         if (state.hasBlockEntity()) {
            blockEntity = level.getBlockEntity(pos);
         }
      } else if (state == null) {
         state = blockEntity.getBlockState();
      }

      for (IBlockCapabilityProvider<T, C> provider : this.providers.getOrDefault(state.getBlock(), List.of())) {
         T ret = provider.getCapability(level, pos, state, blockEntity, context);
         if (ret != null) {
            return ret;
         }
      }

      return null;
   }
}
