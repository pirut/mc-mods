package net.minecraft.util;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import java.util.Arrays;
import java.util.Iterator;
import javax.annotation.Nullable;
import net.minecraft.core.IdMap;

public class CrudeIncrementalIntIdentityHashBiMap<K> implements IdMap<K> {
   private static final int NOT_FOUND = -1;
   private static final Object EMPTY_SLOT = null;
   private static final float LOADFACTOR = 0.8F;
   private K[] keys;
   private int[] values;
   private K[] byId;
   private int nextId;
   private int size;

   private CrudeIncrementalIntIdentityHashBiMap(int p_13553_) {
      this.keys = (K[])(new Object[p_13553_]);
      this.values = new int[p_13553_];
      this.byId = (K[])(new Object[p_13553_]);
   }

   private CrudeIncrementalIntIdentityHashBiMap(K[] p_199841_, int[] p_199842_, K[] p_199843_, int p_199844_, int p_199845_) {
      this.keys = p_199841_;
      this.values = p_199842_;
      this.byId = p_199843_;
      this.nextId = p_199844_;
      this.size = p_199845_;
   }

   public static <A> CrudeIncrementalIntIdentityHashBiMap<A> create(int p_184238_) {
      return new CrudeIncrementalIntIdentityHashBiMap((int)(p_184238_ / 0.8F));
   }

   @Override
   public int getId(@Nullable K p_13558_) {
      return this.getValue(this.indexOf(p_13558_, this.hash(p_13558_)));
   }

   @Nullable
   @Override
   public K byId(int p_13556_) {
      return p_13556_ >= 0 && p_13556_ < this.byId.length ? this.byId[p_13556_] : null;
   }

   private int getValue(int p_13568_) {
      return p_13568_ == -1 ? -1 : this.values[p_13568_];
   }

   public boolean contains(K p_144610_) {
      return this.getId(p_144610_) != -1;
   }

   public boolean contains(int p_144608_) {
      return this.byId(p_144608_) != null;
   }

   public int add(K p_13570_) {
      int $$1 = this.nextId();
      this.addMapping(p_13570_, $$1);
      return $$1;
   }

   private int nextId() {
      while (this.nextId < this.byId.length && this.byId[this.nextId] != null) {
         this.nextId++;
      }

      return this.nextId;
   }

   private void grow(int p_13572_) {
      K[] $$1 = this.keys;
      int[] $$2 = this.values;
      CrudeIncrementalIntIdentityHashBiMap<K> $$3 = new CrudeIncrementalIntIdentityHashBiMap<>(p_13572_);

      for (int $$4 = 0; $$4 < $$1.length; $$4++) {
         if ($$1[$$4] != null) {
            $$3.addMapping($$1[$$4], $$2[$$4]);
         }
      }

      this.keys = $$3.keys;
      this.values = $$3.values;
      this.byId = $$3.byId;
      this.nextId = $$3.nextId;
      this.size = $$3.size;
   }

   public void addMapping(K p_13560_, int p_13561_) {
      int $$2 = Math.max(p_13561_, this.size + 1);
      if ($$2 >= this.keys.length * 0.8F) {
         int $$3 = this.keys.length << 1;

         while ($$3 < p_13561_) {
            $$3 <<= 1;
         }

         this.grow($$3);
      }

      int $$4 = this.findEmpty(this.hash(p_13560_));
      this.keys[$$4] = p_13560_;
      this.values[$$4] = p_13561_;
      this.byId[p_13561_] = p_13560_;
      this.size++;
      if (p_13561_ == this.nextId) {
         this.nextId++;
      }
   }

   private int hash(@Nullable K p_13574_) {
      return (Mth.murmurHash3Mixer(System.identityHashCode(p_13574_)) & 2147483647) % this.keys.length;
   }

   private int indexOf(@Nullable K p_13564_, int p_13565_) {
      for (int $$2 = p_13565_; $$2 < this.keys.length; $$2++) {
         if (this.keys[$$2] == p_13564_) {
            return $$2;
         }

         if (this.keys[$$2] == EMPTY_SLOT) {
            return -1;
         }
      }

      for (int $$3 = 0; $$3 < p_13565_; $$3++) {
         if (this.keys[$$3] == p_13564_) {
            return $$3;
         }

         if (this.keys[$$3] == EMPTY_SLOT) {
            return -1;
         }
      }

      return -1;
   }

   private int findEmpty(int p_13576_) {
      for (int $$1 = p_13576_; $$1 < this.keys.length; $$1++) {
         if (this.keys[$$1] == EMPTY_SLOT) {
            return $$1;
         }
      }

      for (int $$2 = 0; $$2 < p_13576_; $$2++) {
         if (this.keys[$$2] == EMPTY_SLOT) {
            return $$2;
         }
      }

      throw new RuntimeException("Overflowed :(");
   }

   @Override
   public Iterator<K> iterator() {
      return Iterators.filter(Iterators.forArray(this.byId), Predicates.notNull());
   }

   public void clear() {
      Arrays.fill(this.keys, null);
      Arrays.fill(this.byId, null);
      this.nextId = 0;
      this.size = 0;
   }

   @Override
   public int size() {
      return this.size;
   }

   public CrudeIncrementalIntIdentityHashBiMap<K> copy() {
      return new CrudeIncrementalIntIdentityHashBiMap<>(
         (K[])((Object[])this.keys.clone()), (int[])this.values.clone(), (K[])((Object[])this.byId.clone()), this.nextId, this.size
      );
   }
}
