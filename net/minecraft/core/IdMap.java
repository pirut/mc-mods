package net.minecraft.core;

import javax.annotation.Nullable;

public interface IdMap<T> extends Iterable<T> {
   int DEFAULT = -1;

   int getId(T var1);

   @Nullable
   T byId(int var1);

   default T byIdOrThrow(int p_200958_) {
      T $$1 = this.byId(p_200958_);
      if ($$1 == null) {
         throw new IllegalArgumentException("No value with id " + p_200958_);
      } else {
         return $$1;
      }
   }

   default int getIdOrThrow(T p_320059_) {
      int $$1 = this.getId(p_320059_);
      if ($$1 == -1) {
         throw new IllegalArgumentException("Can't find id for '" + p_320059_ + "' in map " + this);
      } else {
         return $$1;
      }
   }

   int size();
}
