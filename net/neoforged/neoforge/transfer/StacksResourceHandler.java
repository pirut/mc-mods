package net.neoforged.neoforge.transfer;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public abstract class StacksResourceHandler<S, T extends Resource> implements ResourceHandler<T>, ValueIOSerializable {
   public static final String VALUE_IO_KEY = "stacks";
   protected final S emptyStack;
   protected NonNullList<S> stacks;
   protected final Codec<NonNullList<S>> codec;
   private final ArrayList<StacksResourceHandler<S, T>.StackJournal> snapshotJournals;

   protected StacksResourceHandler(int size, S emptyStack, Codec<S> stackCodec) {
      this(NonNullList.withSize(size, emptyStack), emptyStack, stackCodec);
   }

   protected StacksResourceHandler(NonNullList<S> stacks, S emptyStack, Codec<S> stackCodec) {
      this.emptyStack = emptyStack;
      this.stacks = this.mutableCopyOf(stacks);
      this.codec = stackCodec.listOf().xmap(this::mutableCopyOf, Function.identity());
      this.snapshotJournals = new ArrayList<>(this.stacks.size());
      this.updateStacksSize();
   }

   private NonNullList<S> mutableCopyOf(Collection<S> list) {
      return NonNullList.of(this.emptyStack, (S[])list.toArray(Object[]::new));
   }

   protected void setStacks(NonNullList<S> stacks) {
      this.stacks = this.mutableCopyOf(stacks);
      this.updateStacksSize();
   }

   private void updateStacksSize() {
      this.snapshotJournals.ensureCapacity(this.stacks.size());

      while (this.snapshotJournals.size() < this.stacks.size()) {
         this.snapshotJournals.add(new StacksResourceHandler.StackJournal(this.snapshotJournals.size()));
      }

      if (this.snapshotJournals.size() > this.stacks.size()) {
         this.snapshotJournals.subList(this.stacks.size(), this.snapshotJournals.size()).clear();
      }
   }

   @Override
   public void serialize(ValueOutput output) {
      output.store("stacks", this.codec, this.stacks);
   }

   @Override
   public void deserialize(ValueInput input) {
      input.<NonNullList>read("stacks", this.codec).ifPresent(l -> {
         this.stacks = (NonNullList<S>)l;
         this.updateStacksSize();
      });
   }

   public void set(int index, T resource, int amount) {
      TransferPreconditions.checkNonNegative(amount);
      if (resource.isEmpty() && amount > 0) {
         throw new IllegalArgumentException("Resource is empty but the amount is positive: " + amount);
      } else {
         S oldContents = this.stacks.set(index, this.getStackFrom(resource, amount));
         this.onContentsChanged(index, oldContents);
      }
   }

   protected abstract T getResourceFrom(S var1);

   protected abstract int getAmountFrom(S var1);

   protected abstract S getStackFrom(T var1, int var2);

   protected abstract S copyOf(S var1);

   protected boolean matches(S stack, T resource) {
      return this.getResourceFrom(stack).equals(resource);
   }

   @Override
   public boolean isValid(int index, T resource) {
      return true;
   }

   protected abstract int getCapacity(int var1, T var2);

   protected void onContentsChanged(int index, S previousContents) {
   }

   public NonNullList<S> copyToList() {
      return this.mutableCopyOf(this.stacks);
   }

   @Override
   public int size() {
      return this.stacks.size();
   }

   @Override
   public T getResource(int index) {
      Objects.checkIndex(index, this.size());
      return this.getResourceFrom(this.stacks.get(index));
   }

   @Override
   public long getAmountAsLong(int index) {
      Objects.checkIndex(index, this.size());
      return this.getAmountFrom(this.stacks.get(index));
   }

   @Override
   public long getCapacityAsLong(int index, T resource) {
      Objects.checkIndex(index, this.size());
      return !resource.isEmpty() && !this.isValid(index, resource) ? 0L : this.getCapacity(index, resource);
   }

   @Override
   public int insert(int index, T resource, int amount, TransactionContext transaction) {
      Objects.checkIndex(index, this.size());
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      S currentStack = this.stacks.get(index);
      int currentAmount = this.getAmountFrom(currentStack);
      if ((currentAmount == 0 || this.matches(currentStack, resource)) && this.isValid(index, resource)) {
         int inserted = Math.min(amount, this.getCapacity(index, resource) - currentAmount);
         if (inserted > 0) {
            this.snapshotJournals.get(index).updateSnapshots(transaction);
            this.stacks.set(index, this.getStackFrom(resource, currentAmount + inserted));
            return inserted;
         }
      }

      return 0;
   }

   @Override
   public int extract(int index, T resource, int amount, TransactionContext transaction) {
      Objects.checkIndex(index, this.size());
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      S currentStack = this.stacks.get(index);
      if (this.matches(currentStack, resource)) {
         int currentAmount = this.getAmountFrom(currentStack);
         int extracted = Math.min(amount, currentAmount);
         if (extracted > 0) {
            this.snapshotJournals.get(index).updateSnapshots(transaction);
            this.stacks.set(index, this.getStackFrom(resource, currentAmount - extracted));
            return extracted;
         }
      }

      return 0;
   }

   private class StackJournal extends SnapshotJournal<S> {
      private final int index;

      private StackJournal(int index) {
         this.index = index;
      }

      @Override
      protected S createSnapshot() {
         return StacksResourceHandler.this.copyOf(StacksResourceHandler.this.stacks.get(this.index));
      }

      @Override
      protected void revertToSnapshot(S snapshot) {
         StacksResourceHandler.this.stacks.set(this.index, snapshot);
      }

      @Override
      protected void onRootCommit(S originalState) {
         StacksResourceHandler.this.onContentsChanged(this.index, originalState);
      }
   }
}
