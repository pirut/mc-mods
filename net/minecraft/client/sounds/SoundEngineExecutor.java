package net.minecraft.client.sounds;

import java.util.concurrent.locks.LockSupport;
import net.minecraft.util.thread.BlockableEventLoop;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SoundEngineExecutor extends BlockableEventLoop<Runnable> {
   private Thread thread = this.createThread();
   private volatile boolean shutdown;

   public SoundEngineExecutor() {
      super("Sound executor");
   }

   private Thread createThread() {
      Thread $$0 = new Thread(this::run);
      $$0.setDaemon(true);
      $$0.setName("Sound engine");
      $$0.start();
      return $$0;
   }

   @Override
   protected Runnable wrapRunnable(Runnable p_120341_) {
      return p_120341_;
   }

   @Override
   protected boolean shouldRun(Runnable p_120339_) {
      return !this.shutdown;
   }

   @Override
   protected Thread getRunningThread() {
      return this.thread;
   }

   private void run() {
      while (!this.shutdown) {
         this.managedBlock(() -> this.shutdown);
      }
   }

   @Override
   protected void waitForTasks() {
      LockSupport.park("waiting for tasks");
   }

   public void flush() {
      this.shutdown = true;
      this.thread.interrupt();

      try {
         this.thread.join();
      } catch (InterruptedException var2) {
         Thread.currentThread().interrupt();
      }

      this.dropAllTasks();
      this.shutdown = false;
      this.thread = this.createThread();
   }
}
