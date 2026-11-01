package net.minecraft.util.profiling.jfr.stats;

import java.time.Duration;
import jdk.jfr.consumer.RecordedEvent;
import net.minecraft.world.level.ChunkPos;

public record StructureGenStat(Duration duration, ChunkPos chunkPos, String structureName, String level, boolean success) implements TimedStat {
   public static StructureGenStat from(RecordedEvent p_383225_) {
      return new StructureGenStat(
         p_383225_.getDuration(),
         new ChunkPos(p_383225_.getInt("chunkPosX"), p_383225_.getInt("chunkPosX")),
         p_383225_.getString("structure"),
         p_383225_.getString("level"),
         p_383225_.getBoolean("success")
      );
   }
}
