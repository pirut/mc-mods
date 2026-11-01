package net.minecraft.util.thread;

import com.mojang.logging.LogUtils;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;

public class NamedThreadFactory implements ThreadFactory {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ThreadGroup group;
   private final AtomicInteger threadNumber = new AtomicInteger(1);
   private final String namePrefix;

   public NamedThreadFactory(String p_146346_) {
      SecurityManager $$1 = System.getSecurityManager();
      this.group = $$1 != null ? $$1.getThreadGroup() : Thread.currentThread().getThreadGroup();
      this.namePrefix = p_146346_ + "-";
   }

   @Override
   public Thread newThread(Runnable p_146352_) {
      Thread $$1 = new Thread(this.group, p_146352_, this.namePrefix + this.threadNumber.getAndIncrement(), 0L);
      $$1.setUncaughtExceptionHandler((p_146349_, p_146350_) -> {
         LOGGER.error("Caught exception in thread {} from {}", p_146349_, p_146352_);
         LOGGER.error("", p_146350_);
      });
      if ($$1.getPriority() != 5) {
         $$1.setPriority(5);
      }

      return $$1;
   }
}
