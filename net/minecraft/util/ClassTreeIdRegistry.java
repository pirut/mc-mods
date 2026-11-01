package net.minecraft.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.Util;

public class ClassTreeIdRegistry {
   public static final int NO_ID_VALUE = -1;
   private final Object2IntMap<Class<?>> classToLastIdCache = Util.make(new Object2IntOpenHashMap(), p_326384_ -> p_326384_.defaultReturnValue(-1));

   public int getLastIdFor(Class<?> p_326222_) {
      int $$1 = this.classToLastIdCache.getInt(p_326222_);
      if ($$1 != -1) {
         return $$1;
      } else {
         Class<?> $$2 = p_326222_;

         while (($$2 = $$2.getSuperclass()) != Object.class) {
            int $$3 = this.classToLastIdCache.getInt($$2);
            if ($$3 != -1) {
               return $$3;
            }
         }

         return -1;
      }
   }

   public int getCount(Class<?> p_325981_) {
      return this.getLastIdFor(p_325981_) + 1;
   }

   public int define(Class<?> p_326354_) {
      int $$1 = this.getLastIdFor(p_326354_);
      int $$2 = $$1 == -1 ? 0 : $$1 + 1;
      this.classToLastIdCache.put(p_326354_, $$2);
      return $$2;
   }
}
