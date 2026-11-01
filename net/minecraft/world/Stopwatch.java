package net.minecraft.world;

public record Stopwatch(long creationTime, long accumulatedElapsedTime) {
   public Stopwatch(long p_455889_) {
      this(p_455889_, 0L);
   }

   public long elapsedMilliseconds(long p_455601_) {
      long i = p_455601_ - this.creationTime;
      return this.accumulatedElapsedTime + i;
   }

   public double elapsedSeconds(long p_456011_) {
      return this.elapsedMilliseconds(p_456011_) / 1000.0;
   }
}
