package net.neoforged.neoforge.transfer.transaction;

import org.jspecify.annotations.Nullable;

public final class RootCommitJournal extends SnapshotJournal<Void> {
   private final Runnable rootCommitCallback;

   public RootCommitJournal(Runnable rootCommitCallback) {
      this.rootCommitCallback = rootCommitCallback;
   }

   @Nullable
   protected Void createSnapshot() {
      return null;
   }

   protected void revertToSnapshot(@Nullable Void snapshot) {
   }

   protected void onRootCommit(@Nullable Void originalState) {
      this.rootCommitCallback.run();
   }
}
