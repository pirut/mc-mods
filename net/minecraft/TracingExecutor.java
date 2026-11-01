package net.minecraft;

import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public record TracingExecutor(ExecutorService service) implements Executor {
   public Executor forName(String name) {
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         return runnable -> this.service.execute(() -> {
            Thread thread = Thread.currentThread();
            String string2 = thread.getName();
            thread.setName(name);

            try {
               Zone zone = TracyClient.beginZone(name, SharedConstants.IS_RUNNING_IN_IDE);

               try {
                  runnable.run();
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
               thread.setName(string2);
            }
         });
      } else {
         return (Executor)(TracyClient.isAvailable() ? runnable -> this.service.execute(() -> {
            Zone zone = TracyClient.beginZone(name, SharedConstants.IS_RUNNING_IN_IDE);

            try {
               runnable.run();
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
   public void execute(Runnable runnable) {
      this.service.execute(wrapUnnamed(runnable));
   }

   public void shutdownAndAwait(long timeout, TimeUnit unit) {
      this.service.shutdown();

      boolean bl;
      try {
         bl = this.service.awaitTermination(timeout, unit);
      } catch (InterruptedException var6) {
         bl = false;
      }

      if (!bl) {
         this.service.shutdownNow();
      }
   }

   private static Runnable wrapUnnamed(Runnable task) {
      return !TracyClient.isAvailable() ? task : () -> {
         Zone zone = TracyClient.beginZone("task", SharedConstants.IS_RUNNING_IN_IDE);

         try {
            task.run();
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
