package net.neoforged.neoforge.transfer.transaction;

import java.lang.StackWalker.Option;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

public final class Transaction implements AutoCloseable, TransactionContext {
   private static final StackWalker STACK_WALKER = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);
   final TransactionManager manager;
   private final int depth;
   boolean open = false;
   final List<SnapshotJournal<?>> journalsToClose = new ArrayList<>();
   Class<?> callerClass;

   public static Transaction openRoot() {
      return TransactionManager.getManagerForThread().open(null, STACK_WALKER.getCallerClass());
   }

   public static Transaction open(@Nullable TransactionContext parent) {
      return TransactionManager.getManagerForThread().open(parent, STACK_WALKER.getCallerClass());
   }

   public static Transaction.Lifecycle getLifecycle() {
      TransactionManager manager = TransactionManager.getManagerForThread();
      int currentDepth = manager.currentDepth;
      if (currentDepth == -1) {
         return manager.processingRootCommitQueue ? Transaction.Lifecycle.ROOT_CLOSING : Transaction.Lifecycle.NONE;
      } else {
         return manager.stack.get(currentDepth).open ? Transaction.Lifecycle.OPEN : Transaction.Lifecycle.CLOSING;
      }
   }

   @Deprecated
   @Nullable
   public static TransactionContext getCurrentOpenedTransaction() {
      TransactionManager manager = TransactionManager.getManagerForThread();
      if (manager.currentDepth == -1) {
         return null;
      } else {
         Transaction transaction = manager.stack.get(manager.currentDepth);
         if (transaction.open) {
            return transaction;
         } else {
            throw new IllegalStateException("`getCurrentOpenedTransaction()` cannot be called while a transaction is closing.");
         }
      }
   }

   public void commit() {
      this.close(false);
   }

   @Override
   public void close() {
      if (this.manager.currentDepth >= this.depth && this.open) {
         this.close(true);
      }
   }

   @Override
   public int depth() {
      this.manager.validateCurrentThread();
      return this.depth;
   }

   @Override
   public String toString() {
      return "Transaction[depth=%d, open=%s, thread=%s]".formatted(this.depth, this.open, this.manager.thread.getName());
   }

   Transaction(TransactionManager manager, int depth, Class<?> callerClass) {
      this.manager = manager;
      this.depth = depth;
      this.callerClass = callerClass;
   }

   void validateOpen() {
      if (!this.open) {
         throw new IllegalStateException("Transaction operation cannot be applied to a closed or closing transaction.");
      }
   }

   String getDebugName() {
      return this.callerClass.toString();
   }

   private void close(boolean wasAborted) {
      this.manager.validateCurrentTransaction(this);
      this.validateOpen();
      this.open = false;
      RuntimeException closeException = null;

      for (SnapshotJournal<?> journal : this.journalsToClose) {
         try {
            journal.onClose(this, wasAborted);
         } catch (Exception var6) {
            if (closeException == null) {
               closeException = new RuntimeException("Encountered an exception while invoking a transaction close callback.", var6);
            } else {
               closeException.addSuppressed(var6);
            }
         }
      }

      this.journalsToClose.clear();
      this.manager.currentDepth--;
      if (this.manager.currentDepth == -1) {
         closeException = this.manager.processRootCommitQueue(closeException);
      }

      if (closeException != null) {
         throw closeException;
      }
   }

   public static enum Lifecycle {
      NONE,
      OPEN,
      CLOSING,
      ROOT_CLOSING;
   }
}
