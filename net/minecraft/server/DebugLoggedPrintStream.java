package net.minecraft.server;

import com.mojang.logging.LogUtils;
import java.io.OutputStream;
import org.slf4j.Logger;

public class DebugLoggedPrintStream extends LoggedPrintStream {
   private static final Logger LOGGER = LogUtils.getLogger();

   public DebugLoggedPrintStream(String p_135934_, OutputStream p_135935_) {
      super(p_135934_, p_135935_);
   }

   @Override
   protected void logLine(String p_135937_) {
      StackTraceElement[] $$1 = Thread.currentThread().getStackTrace();
      StackTraceElement $$2 = $$1[Math.min(3, $$1.length)];
      LOGGER.info("[{}]@.({}:{}): {}", new Object[]{this.name, $$2.getFileName(), $$2.getLineNumber(), p_135937_});
   }
}
