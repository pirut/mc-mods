package net.neoforged.neoforge.transfer.item;

import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class TransactionalRandom extends SnapshotJournal<Long> {
   private long seed = RandomSupport.generateUniqueSeed();

   protected Long createSnapshot() {
      return this.seed;
   }

   protected void revertToSnapshot(Long snapshot) {
      this.seed = snapshot;
   }

   public double nextDouble(TransactionContext transaction) {
      this.updateSnapshots(transaction);
      SingleThreadedRandomSource random = new SingleThreadedRandomSource(this.seed);
      double rand = random.nextDouble();
      this.seed = random.nextLong();
      return rand;
   }
}
