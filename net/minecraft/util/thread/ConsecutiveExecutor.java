package net.minecraft.util.thread;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

public class ConsecutiveExecutor extends AbstractConsecutiveExecutor<Runnable> {
   public ConsecutiveExecutor(Executor p_371208_, String p_371460_) {
      super(new StrictQueue.QueueStrictQueue(new ConcurrentLinkedQueue<>()), p_371208_, p_371460_);
   }

   @Override
   public Runnable wrapRunnable(Runnable p_371601_) {
      return p_371601_;
   }
}
