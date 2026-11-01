package net.minecraft.util.debugchart;

import net.minecraft.util.debug.DebugSubscription;
import net.minecraft.util.debug.DebugSubscriptions;

public enum RemoteDebugSampleType {
   TICK_TIME(DebugSubscriptions.DEDICATED_SERVER_TICK_TIME);

   private final DebugSubscription<?> subscription;

   private RemoteDebugSampleType(DebugSubscription<?> p_449993_) {
      this.subscription = p_449993_;
   }

   public DebugSubscription<?> subscription() {
      return this.subscription;
   }
}
