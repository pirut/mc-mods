package net.neoforged.neoforge.transfer.transaction;

import java.util.ArrayList;
import org.jspecify.annotations.Nullable;

public abstract class SnapshotJournal<T> {
   private static final Object NO_SNAPSHOT = new Object();
   private final ArrayList<T> snapshots = new ArrayList<>();
   @Nullable
   private T originalState = (T)null;

   protected abstract T createSnapshot();

   protected abstract void revertToSnapshot(T var1);

   protected void releaseSnapshot(T snapshot) {
   }

   protected void onRootCommit(T originalState) {
   }

   public void updateSnapshots(TransactionContext transaction) {
      int currentDepth = transaction.depth();
      this.snapshots.ensureCapacity(currentDepth);

      for (int i = this.snapshots.size(); i <= currentDepth; i++) {
         this.snapshots.add((T)NO_SNAPSHOT);
      }

      if (this.snapshots.get(currentDepth) == NO_SNAPSHOT) {
         this.snapshots.set(currentDepth, this.createSnapshot());
         Transaction transactionImpl = (Transaction)transaction;
         transactionImpl.validateOpen();
         transactionImpl.journalsToClose.add(this);
      }
   }

   void onClose(Transaction transaction, boolean wasAborted) {
      int currentDepth = transaction.depth();
      T snapshot = this.snapshots.remove(currentDepth);
      if (wasAborted) {
         this.revertToSnapshot(snapshot);
         this.releaseSnapshot(snapshot);
      } else if (currentDepth <= 0) {
         if (this.originalState == null) {
            this.originalState = snapshot;
            transaction.manager.rootCommitQueue.add(this);
         } else {
            this.releaseSnapshot(snapshot);
         }
      } else if (this.snapshots.get(currentDepth - 1) == NO_SNAPSHOT) {
         this.snapshots.set(currentDepth - 1, snapshot);
         transaction.manager.getOpenTransaction(currentDepth - 1).journalsToClose.add(this);
      } else {
         this.releaseSnapshot(snapshot);
      }
   }

   void callOnRootCommit() {
      T originalState = this.originalState;
      this.originalState = null;
      this.onRootCommit(originalState);
      this.releaseSnapshot(originalState);
   }
}
