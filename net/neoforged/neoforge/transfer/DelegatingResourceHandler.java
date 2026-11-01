package net.neoforged.neoforge.transfer;

import java.util.Objects;
import java.util.function.Supplier;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class DelegatingResourceHandler<T extends Resource> implements ResourceHandler<T> {
   protected final Supplier<ResourceHandler<T>> delegate;

   public DelegatingResourceHandler(ResourceHandler<T> delegate) {
      this.delegate = () -> delegate;
   }

   public DelegatingResourceHandler(Supplier<ResourceHandler<T>> delegate) {
      this.delegate = delegate;
   }

   @Override
   public int size() {
      return this.getDelegate().size();
   }

   protected int convertIndex(int index) {
      Objects.checkIndex(index, this.size());
      return index;
   }

   @Override
   public T getResource(int index) {
      return this.getDelegate().getResource(this.convertIndex(index));
   }

   @Override
   public long getAmountAsLong(int index) {
      return this.getDelegate().getAmountAsLong(this.convertIndex(index));
   }

   @Override
   public long getCapacityAsLong(int index, T resource) {
      return this.getDelegate().getCapacityAsLong(this.convertIndex(index), resource);
   }

   @Override
   public boolean isValid(int index, T resource) {
      return resource.isEmpty() ? true : this.getDelegate().isValid(this.convertIndex(index), resource);
   }

   @Override
   public int insert(int index, T resource, int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      return this.getDelegate().insert(this.convertIndex(index), resource, amount, transaction);
   }

   @Override
   public int insert(T resource, int amount, TransactionContext transaction) {
      return this.getDelegate().insert(resource, amount, transaction);
   }

   @Override
   public int extract(int index, T resource, int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      return this.getDelegate().extract(this.convertIndex(index), resource, amount, transaction);
   }

   @Override
   public int extract(T resource, int amount, TransactionContext transaction) {
      return this.getDelegate().extract(resource, amount, transaction);
   }

   public ResourceHandler<T> getDelegate() {
      return this.delegate.get();
   }
}
