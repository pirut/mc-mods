package net.minecraft.world.level.entity;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public class EntityTickList {
   private Int2ObjectMap<Entity> active = new Int2ObjectLinkedOpenHashMap();
   private Int2ObjectMap<Entity> passive = new Int2ObjectLinkedOpenHashMap();
   @Nullable
   private Int2ObjectMap<Entity> iterated;

   private void ensureActiveIsNotIterated() {
      if (this.iterated == this.active) {
         this.passive.clear();
         ObjectIterator int2ObjectMap = Int2ObjectMaps.fastIterable(this.active).iterator();

         while (int2ObjectMap.hasNext()) {
            Entry<Entity> entry = (Entry<Entity>)int2ObjectMap.next();
            this.passive.put(entry.getIntKey(), (Entity)entry.getValue());
         }

         Int2ObjectMap<Entity> int2ObjectMapx = this.active;
         this.active = this.passive;
         this.passive = int2ObjectMapx;
      }
   }

   public void add(Entity entity) {
      this.ensureActiveIsNotIterated();
      this.active.put(entity.getId(), entity);
   }

   public void remove(Entity entity) {
      this.ensureActiveIsNotIterated();
      this.active.remove(entity.getId());
   }

   public boolean contains(Entity entity) {
      return this.active.containsKey(entity.getId());
   }

   public void forEach(Consumer<Entity> entity) {
      if (this.iterated != null) {
         throw new UnsupportedOperationException("Only one concurrent iteration supported");
      } else {
         this.iterated = this.active;

         try {
            ObjectIterator var2 = this.active.values().iterator();

            while (var2.hasNext()) {
               Entity entity2 = (Entity)var2.next();
               entity.accept(entity2);
            }
         } finally {
            this.iterated = null;
         }
      }
   }
}
