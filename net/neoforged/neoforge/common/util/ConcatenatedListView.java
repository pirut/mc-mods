package net.neoforged.neoforge.common.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Supplier;

public class ConcatenatedListView<T> implements List<T> {
   private final List<? extends List<? extends T>> lists;

   @SafeVarargs
   public static <T> ConcatenatedListView<T> of(List<T>... lists) {
      return new ConcatenatedListView<>(List.of(lists));
   }

   public static <T> List<T> of(List<? extends List<? extends T>> members) {
      return (List<T>)(switch (members.size()) {
         case 0 -> List.of();
         case 1 -> Collections.unmodifiableList((List<? extends T>)members.get(0));
         default -> new ConcatenatedListView(members);
      });
   }

   private ConcatenatedListView(List<? extends List<? extends T>> lists) {
      this.lists = lists;
   }

   @Override
   public int size() {
      int size = 0;

      for (List<? extends T> list : this.lists) {
         size += list.size();
      }

      return size;
   }

   @Override
   public boolean isEmpty() {
      for (List<? extends T> list : this.lists) {
         if (!list.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public boolean contains(Object o) {
      for (List<? extends T> list : this.lists) {
         if (list.contains(o)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public T get(int index) {
      for (List<? extends T> list : this.lists) {
         int size = list.size();
         if (index < size) {
            return (T)list.get(index);
         }

         index -= size;
      }

      throw new IndexOutOfBoundsException(index);
   }

   @Override
   public int indexOf(Object o) {
      int offset = 0;

      for (List<? extends T> list : this.lists) {
         int foundIndex = list.indexOf(o);
         if (foundIndex >= 0) {
            return offset + foundIndex;
         }

         offset += list.size();
      }

      return -1;
   }

   @Override
   public int lastIndexOf(Object o) {
      int offset = 0;

      for (List<? extends T> list : Lists.reverse(this.lists)) {
         int foundIndex = list.lastIndexOf(o);
         if (foundIndex >= 0) {
            return offset + foundIndex;
         }

         offset += list.size();
      }

      return -1;
   }

   @Override
   public Iterator<T> iterator() {
      return Iterables.unmodifiableIterable(Iterables.concat(this.lists)).iterator();
   }

   @Override
   public Spliterator<T> spliterator() {
      return Iterables.unmodifiableIterable(Iterables.concat(this.lists)).spliterator();
   }

   private <C extends Collection<T>> C concatenate(Supplier<C> collectionFactory) {
      C concat = (C)collectionFactory.get();

      for (List<? extends T> list : this.lists) {
         concat.addAll(list);
      }

      return concat;
   }

   @Override
   public Object[] toArray() {
      return ((ArrayList)this.concatenate(ArrayList::new)).toArray();
   }

   @Override
   public <T1> T1[] toArray(T1[] a) {
      return ((ArrayList)this.concatenate(ArrayList::new)).toArray(a);
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      return ((HashSet)this.concatenate(HashSet::new)).containsAll(c);
   }

   @Override
   public boolean add(T t) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void add(int index, T element) {
      throw new UnsupportedOperationException();
   }

   @Override
   public T set(int index, T element) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean addAll(Collection<? extends T> c) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean addAll(int index, Collection<? extends T> c) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean remove(Object o) {
      throw new UnsupportedOperationException();
   }

   @Override
   public T remove(int index) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void clear() {
      throw new UnsupportedOperationException();
   }

   @Override
   public ListIterator<T> listIterator() {
      throw new UnsupportedOperationException();
   }

   @Override
   public ListIterator<T> listIterator(int index) {
      throw new UnsupportedOperationException();
   }

   @Override
   public List<T> subList(int fromIndex, int toIndex) {
      throw new UnsupportedOperationException();
   }
}
