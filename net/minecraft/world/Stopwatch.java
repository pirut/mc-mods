package net.minecraft.world;

public record Stopwatch(long creationTime, long accumulatedElapsedTime) {
   public Stopwatch(long creationTime) {
      this(creationTime, 0L);
   }

   public long elapsedMilliseconds(long currentTime) {
      long l = currentTime - this.creationTime;
      return this.accumulatedElapsedTime + l;
   }

   public double elapsedSeconds(long currentTime) {
      return this.elapsedMilliseconds(currentTime) / 1000.0;
   }
}
