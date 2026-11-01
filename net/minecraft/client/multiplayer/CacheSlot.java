package net.minecraft.client.multiplayer;

import java.util.function.Function;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CacheSlot<C extends CacheSlot.Cleaner<C>, D> {
   private final Function<C, D> operation;
   @Nullable
   private C context;
   @Nullable
   private D value;

   public CacheSlot(Function<C, D> p_399634_) {
      this.operation = p_399634_;
   }

   public D compute(C p_399903_) {
      if (p_399903_ == this.context && this.value != null) {
         return this.value;
      } else {
         D d = this.operation.apply(p_399903_);
         this.value = d;
         this.context = p_399903_;
         p_399903_.registerForCleaning(this);
         return d;
      }
   }

   public void clear() {
      this.value = null;
      this.context = null;
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface Cleaner<C extends CacheSlot.Cleaner<C>> {
      void registerForCleaning(CacheSlot<C, ?> var1);
   }
}
