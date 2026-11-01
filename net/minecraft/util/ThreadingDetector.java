package net.minecraft.util;

import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import org.slf4j.Logger;

public class ThreadingDetector {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final String name;
   private final Semaphore lock = new Semaphore(1);
   private final Lock stackTraceLock = new ReentrantLock();
   @Nullable
   private volatile Thread threadThatFailedToAcquire;
   @Nullable
   private volatile ReportedException fullException;

   public ThreadingDetector(String p_199415_) {
      this.name = p_199415_;
   }

   public void checkAndLock() {
      boolean $$0 = false;

      try {
         this.stackTraceLock.lock();
         if (!this.lock.tryAcquire()) {
            this.threadThatFailedToAcquire = Thread.currentThread();
            $$0 = true;
            this.stackTraceLock.unlock();

            try {
               this.lock.acquire();
            } catch (InterruptedException var6) {
               Thread.currentThread().interrupt();
            }

            throw this.fullException;
         }
      } finally {
         if (!$$0) {
            this.stackTraceLock.unlock();
         }
      }
   }

   public void checkAndUnlock() {
      try {
         this.stackTraceLock.lock();
         Thread $$0 = this.threadThatFailedToAcquire;
         if ($$0 != null) {
            ReportedException $$1 = makeThreadingException(this.name, $$0);
            this.fullException = $$1;
            this.lock.release();
            throw $$1;
         }

         this.lock.release();
      } finally {
         this.stackTraceLock.unlock();
      }
   }

   public static ReportedException makeThreadingException(String p_199418_, @Nullable Thread p_199419_) {
      String $$2 = Stream.of(Thread.currentThread(), p_199419_).filter(Objects::nonNull).map(ThreadingDetector::stackTrace).collect(Collectors.joining("\n"));
      String $$3 = "Accessing " + p_199418_ + " from multiple threads";
      CrashReport $$4 = new CrashReport($$3, new IllegalStateException($$3));
      CrashReportCategory $$5 = $$4.addCategory("Thread dumps");
      $$5.setDetail("Thread dumps", $$2);
      LOGGER.error("Thread dumps: \n" + $$2);
      return new ReportedException($$4);
   }

   private static String stackTrace(Thread p_199421_) {
      return p_199421_.getName() + ": \n\tat " + Arrays.stream(p_199421_.getStackTrace()).map(Object::toString).collect(Collectors.joining("\n\tat "));
   }
}
