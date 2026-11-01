package net.minecraft;

import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public record TracingExecutor(ExecutorService service) implements Executor {
   public Executor forName(String p_372983_) {
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         return p_372951_ -> this.service.execute(() -> {
            Thread thread = Thread.currentThread();
            String s = thread.getName();
            thread.setName(p_372983_);

            try {
               Zone zone = TracyClient.beginZone(p_372983_, SharedConstants.IS_RUNNING_IN_IDE);

               try {
                  p_372951_.run();
               } catch (Throwable var12) {
                  if (zone != null) {
                     try {
                        zone.close();
                     } catch (Throwable var11) {
                        var12.addSuppressed(var11);
                     }
                  }

                  throw var12;
               }

               if (zone != null) {
                  zone.close();
               }
            } finally {
               thread.setName(s);
            }
         });
      } else {
         return (Executor)(TracyClient.isAvailable() ? p_372837_ -> this.service.execute(() -> {
            Zone zone = TracyClient.beginZone(p_372983_, SharedConstants.IS_RUNNING_IN_IDE);

            try {
               p_372837_.run();
            } catch (Throwable var6) {
               if (zone != null) {
                  try {
                     zone.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (zone != null) {
               zone.close();
            }
         }) : this.service);
      }
   }

   @Override
   public void execute(Runnable p_373097_) {
      this.service.execute(wrapUnnamed(p_373097_));
   }

   public void shutdownAndAwait(long p_372802_, TimeUnit p_373037_) {
      this.service.shutdown();

      boolean flag;
      try {
         flag = this.service.awaitTermination(p_372802_, p_373037_);
      } catch (InterruptedException var6) {
         flag = false;
      }

      if (!flag) {
         this.service.shutdownNow();
      }
   }

   private static Runnable wrapUnnamed(Runnable p_372957_) {
      return !TracyClient.isAvailable() ? p_372957_ : () -> {
         Zone zone = TracyClient.beginZone("task", SharedConstants.IS_RUNNING_IN_IDE);

         try {
            p_372957_.run();
         } catch (Throwable var5) {
            if (zone != null) {
               try {
                  zone.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }
            }

            throw var5;
         }

         if (zone != null) {
            zone.close();
         }
      };
   }
}
