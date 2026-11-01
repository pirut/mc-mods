package net.neoforged.neoforge.transfer.transaction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import org.jspecify.annotations.Nullable;

final class TransactionManager {
   private static final ThreadLocal<TransactionManager> MANAGERS = ThreadLocal.withInitial(TransactionManager::new);
   final Thread thread = Thread.currentThread();
   final List<Transaction> stack = new ArrayList<>();
   int currentDepth = -1;
   final Queue<SnapshotJournal<?>> rootCommitQueue = new ArrayDeque<>();
   boolean processingRootCommitQueue = false;

   static TransactionManager getManagerForThread() {
      return MANAGERS.get();
   }

   Transaction open(@Nullable TransactionContext parent, Class<?> callerClass) {
      if (parent != null) {
         Transaction parentImpl = (Transaction)parent;
         this.validateCurrentTransaction(parentImpl);
         parentImpl.validateOpen();
      } else if (this.currentDepth >= 0) {
         String currentRoot = this.getOpenTransaction(0).getDebugName();
         throw new IllegalStateException(
            "A root transaction of `" + currentRoot + "` is already active on this thread " + this.thread + " when `" + callerClass + "` tried to open."
         );
      }

      Transaction current;
      if (this.stack.size() == ++this.currentDepth) {
         current = new Transaction(this, this.currentDepth, callerClass);
         this.stack.add(current);
      } else {
         current = this.stack.get(this.currentDepth);
         current.callerClass = callerClass;
      }

      current.open = true;
      return current;
   }

   Transaction getOpenTransaction(int depth) {
      this.validateCurrentThread();
      if (depth < 0) {
         throw new IndexOutOfBoundsException("Depth may not be negative.");
      } else if (depth > this.currentDepth) {
         throw new IndexOutOfBoundsException("There is no open transaction for depth `" + depth + "`");
      } else {
         Transaction transaction = this.stack.get(depth);
         transaction.validateOpen();
         return transaction;
      }
   }

   void validateCurrentThread() {
      if (Thread.currentThread() != this.thread) {
         String errorMessage = String.format(
            "Attempted to access transaction state from thread %s, but this transaction is only valid on thread %s.",
            Thread.currentThread().getName(),
            this.thread.getName()
         );
         throw new IllegalStateException(errorMessage);
      }
   }

   void validateCurrentTransaction(Transaction transaction) {
      this.validateCurrentThread();
      if (this.currentDepth == -1 || this.stack.get(this.currentDepth) != transaction) {
         String errorMessage = String.format(
            "Transaction function was called on a transaction (%s) with depth `%d`, but the current transaction (%s) has depth `%d`.",
            transaction.getDebugName(),
            transaction.depth(),
            this.stack.get(this.currentDepth).getDebugName(),
            this.currentDepth
         );
         throw new IllegalStateException(errorMessage);
      }
   }

   @Nullable
   RuntimeException processRootCommitQueue(@Nullable RuntimeException closeException) {
      if (this.processingRootCommitQueue) {
         return closeException;
      } else {
         this.processingRootCommitQueue = true;

         while (!this.rootCommitQueue.isEmpty()) {
            SnapshotJournal<?> journal = this.rootCommitQueue.remove();

            try {
               journal.callOnRootCommit();
            } catch (Exception var4) {
               if (closeException == null) {
                  closeException = new RuntimeException("Encountered an exception while invoking a journal's onRootCommit method.", var4);
               } else {
                  closeException.addSuppressed(var4);
               }
            }
         }

         this.processingRootCommitQueue = false;
         return closeException;
      }
   }

   private TransactionManager() {
   }
}
