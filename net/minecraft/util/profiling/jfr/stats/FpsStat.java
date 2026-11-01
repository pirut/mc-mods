package net.minecraft.util.profiling.jfr.stats;

import jdk.jfr.consumer.RecordedEvent;

public record FpsStat(int fps) {
   public static FpsStat from(RecordedEvent p_455408_, String p_454965_) {
      return new FpsStat(p_455408_.getInt(p_454965_));
   }
}
