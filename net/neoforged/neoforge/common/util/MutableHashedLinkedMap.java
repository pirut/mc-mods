package net.neoforged.neoforge.common.util;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiPredicate;
import net.neoforged.neoforge.common.util.strategy.BasicStrategy;
import org.jetbrains.annotations.Nullable;

public class MutableHashedLinkedMap<K, V> implements Iterable<Map.Entry<K, V>> {
   private final Strategy<? super K> strategy;
   private final Map<K, MutableHashedLinkedMap<K, V>.Entry> entries;
   private final MutableHashedLinkedMap.MergeFunction<K, V> merge;
   private final BiPredicate<K, V> insertTest;
   private MutableHashedLinkedMap<K, V>.Entry head = null;
   private MutableHashedLinkedMap<K, V>.Entry last = null;
   private transient int changes = 0;

   public MutableHashedLinkedMap() {
      this(BasicStrategy.BASIC);
   }

   public MutableHashedLinkedMap(Strategy<? super K> strategy) {
      this(strategy, (k, v1, v2) -> v2);
   }

   public MutableHashedLinkedMap(Strategy<? super K> strategy, MutableHashedLinkedMap.MergeFunction<K, V> merge) {
      this(strategy, merge, (k, v) -> true);
   }

   public MutableHashedLinkedMap(Strategy<? super K> strategy, BiPredicate<K, V> insertTest) {
      this(strategy, (k, v1, v2) -> v2, insertTest);
   }

   public MutableHashedLinkedMap(Strategy<? super K> strategy, MutableHashedLinkedMap.MergeFunction<K, V> merge, BiPredicate<K, V> insertTest) {
      this.strategy = strategy;
      this.entries = new Object2ObjectOpenCustomHashMap(strategy);
      this.merge = merge;
      this.insertTest = insertTest;
   }

   @Nullable
   public V put(K key, V value) {
      MutableHashedLinkedMap<K, V>.Entry old = this.entries.get(key);
      if (!this.insertTest.test(key, value)) {
         return old == null ? null : old.value;
      } else if (old != null) {
         V ret = old.value;
         old.value = this.merge.apply(key, ret, value);
         return ret;
      } else {
         this.changes++;
         MutableHashedLinkedMap<K, V>.Entry self = new MutableHashedLinkedMap.Entry(key, value);
         MutableHashedLinkedMap<K, V>.Entry l = this.last;
         self.previous = l;
         if (l == null) {
            this.head = self;
         } else {
            l.next = self;
         }

         this.last = self;
         this.entries.put(key, self);
         return null;
      }
   }

   public boolean contains(K key) {
      return this.entries.containsKey(key);
   }

   public boolean isEmpty() {
      return this.entries.isEmpty();
   }

   @Nullable
   public V remove(K key) {
      MutableHashedLinkedMap<K, V>.Entry ret = this.entries.remove(key);
      if (ret == null) {
         return null;
      } else {
         this.remove(ret);
         return ret.getValue();
      }
   }

   @Nullable
   public V get(K key) {
      MutableHashedLinkedMap<K, V>.Entry entry = this.entries.get(key);
      return entry == null ? null : entry.getValue();
   }

   @Override
   public Iterator<Map.Entry<K, V>> iterator() {
      return new Iterator<Map.Entry<K, V>>() {
         private MutableHashedLinkedMap<K, V>.Entry current = MutableHashedLinkedMap.this.head;
         private MutableHashedLinkedMap<K, V>.Entry last = null;
         private int expectedChanges = MutableHashedLinkedMap.this.changes;

         @Override
         public boolean hasNext() {
            return this.current != null;
         }

         public Map.Entry<K, V> next() {
            if (MutableHashedLinkedMap.this.changes != this.expectedChanges) {
               throw new ConcurrentModificationException();
            } else if (!this.hasNext()) {
               throw new NoSuchElementException();
            } else {
               this.last = this.current;
               this.current = this.current.next;
               return this.last;
            }
         }

         @Override
         public void remove() {
            if (this.last == null) {
               throw new IllegalStateException("Invalid remove() call, must call next() first");
            } else if (MutableHashedLinkedMap.this.changes != this.expectedChanges) {
               throw new ConcurrentModificationException();
            } else {
               MutableHashedLinkedMap<K, V>.Entry removed = MutableHashedLinkedMap.this.entries.remove(this.last.getKey());
               if (removed != this.last) {
                  throw new ConcurrentModificationException();
               } else {
                  this.expectedChanges++;
                  MutableHashedLinkedMap.this.remove(this.last);
                  this.last = null;
               }
            }
         }
      };
   }

   @Nullable
   public V putFirst(K key, V value) {
      return this.head != null ? this.putBefore(this.head.getKey(), key, value) : this.put(key, value);
   }

   @Nullable
   public V putAfter(K after, K key, V value) {
      MutableHashedLinkedMap<K, V>.Entry target = this.entries.get(after);
      if (target == null) {
         return this.put(key, value);
      } else {
         V ret = null;
         MutableHashedLinkedMap<K, V>.Entry entry = this.entries.get(key);
         if (!this.insertTest.test(key, value)) {
            return entry == null ? null : entry.value;
         } else {
            if (entry != null) {
               ret = entry.value;
               entry.value = this.merge.apply(key, ret, value);
               this.remove(entry);
            } else {
               entry = new MutableHashedLinkedMap.Entry(key, value);
               this.entries.put(key, entry);
            }

            this.changes++;
            entry.previous = target;
            if (target.next == null) {
               this.last = target;
            } else {
               target.next.previous = entry;
            }

            entry.next = target.next;
            target.next = entry;
            return ret;
         }
      }
   }

   @Nullable
   public V putBefore(K before, K key, V value) {
      MutableHashedLinkedMap<K, V>.Entry target = this.entries.get(before);
      if (target == null) {
         return this.put(key, value);
      } else {
         V ret = null;
         MutableHashedLinkedMap<K, V>.Entry entry = this.entries.get(key);
         if (!this.insertTest.test(key, value)) {
            return entry == null ? null : entry.value;
         } else {
            if (entry != null) {
               ret = entry.value;
               entry.value = this.merge.apply(key, ret, value);
               this.remove(entry);
            } else {
               entry = new MutableHashedLinkedMap.Entry(key, value);
               this.entries.put(key, entry);
            }

            this.changes++;
            entry.previous = target.previous;
            if (target.previous == null) {
               this.head = entry;
            } else {
               target.previous.next = entry;
            }

            entry.next = target;
            target.previous = entry;
            return ret;
         }
      }
   }

   private void remove(MutableHashedLinkedMap<K, V>.Entry e) {
      this.changes++;
      MutableHashedLinkedMap<K, V>.Entry previous = e.previous;
      if (this.head == e) {
         this.head = e.next;
      } else if (e.previous != null) {
         e.previous.next = e.next;
         e.previous = null;
      }

      if (this.last == e) {
         this.last = previous;
      } else if (e.next != null) {
         e.next.previous = previous;
         e.next = null;
      }
   }

   private class Entry implements Map.Entry<K, V> {
      private final K key;
      private V value;
      private MutableHashedLinkedMap<K, V>.Entry previous;
      private MutableHashedLinkedMap<K, V>.Entry next;

      private Entry(K key, V value) {
         this.key = key;
         this.value = value;
      }

      @Override
      public K getKey() {
         return this.key;
      }

      @Override
      public V getValue() {
         return this.value;
      }

      @Override
      public V setValue(V value) {
         V old = this.value;
         this.value = value;
         return old;
      }

      @Override
      public String toString() {
         return "Entry[" + this.key + ", " + this.value + "]";
      }

      @Override
      public boolean equals(Object o) {
         return !(o instanceof Map.Entry<?, ?> e)
            ? false
            : (this.key == null ? e.getKey() == null : this.key.equals(e.getKey()))
               && (this.value == null ? e.getValue() == null : this.value.equals(e.getValue()));
      }

      @Override
      public int hashCode() {
         return (this.key == null ? 0 : MutableHashedLinkedMap.this.strategy.hashCode(this.key)) ^ (this.value == null ? 0 : this.value.hashCode());
      }
   }

   public interface MergeFunction<Key, Value> {
      Value apply(Key var1, Value var2, Value var3);
   }
}
