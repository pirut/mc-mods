package net.minecraft.client.multiplayer;

import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class CacheSlot<C extends CacheSlot.Cleaner<C>, D> {
   private final Function<C, D> operation;
   @Nullable
   private C context;
   @Nullable
   private D value;

   public CacheSlot(Function<C, D> operation) {
      this.operation = operation;
   }

   public D compute(C context) {
      if (context == this.context && this.value != null) {
         return this.value;
      } else {
         D object = this.operation.apply(context);
         this.value = object;
         this.context = context;
         context.registerForCleaning(this);
         return object;
      }
   }

   public void clear() {
      this.value = null;
      this.context = null;
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface Cleaner<C extends CacheSlot.Cleaner<C>> {
      void registerForCleaning(CacheSlot<C, ?> cacheSlot);
   }
}
