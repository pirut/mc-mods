package net.minecraft.world.level.timers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

@FunctionalInterface
public interface TimerCallback<T> {
   void handle(T var1, TimerQueue<T> var2, long var3);

   public abstract static class Serializer<T, C extends TimerCallback<T>> {
      private final ResourceLocation id;
      private final Class<?> cls;

      public Serializer(ResourceLocation p_82219_, Class<?> p_82220_) {
         this.id = p_82219_;
         this.cls = p_82220_;
      }

      public ResourceLocation getId() {
         return this.id;
      }

      public Class<?> getCls() {
         return this.cls;
      }

      public abstract void serialize(CompoundTag var1, C var2);

      public abstract C deserialize(CompoundTag var1);
   }
}
