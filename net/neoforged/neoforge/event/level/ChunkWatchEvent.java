package net.neoforged.neoforge.event.level;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.Event;

public abstract class ChunkWatchEvent extends Event {
   private final ServerLevel level;
   private final ServerPlayer player;
   private final ChunkPos pos;

   public ChunkWatchEvent(ServerPlayer player, ChunkPos pos, ServerLevel level) {
      this.player = player;
      this.pos = pos;
      this.level = level;
   }

   public ServerPlayer getPlayer() {
      return this.player;
   }

   public ChunkPos getPos() {
      return this.pos;
   }

   public ServerLevel getLevel() {
      return this.level;
   }

   public static class Sent extends ChunkWatchEvent {
      private final LevelChunk chunk;

      public Sent(ServerPlayer player, LevelChunk chunk, ServerLevel level) {
         super(player, chunk.getPos(), level);
         this.chunk = chunk;
      }

      public LevelChunk getChunk() {
         return this.chunk;
      }
   }

   public static class UnWatch extends ChunkWatchEvent {
      public UnWatch(ServerPlayer player, ChunkPos pos, ServerLevel level) {
         super(player, pos, level);
      }
   }

   public static class Watch extends ChunkWatchEvent {
      private final LevelChunk chunk;

      public Watch(ServerPlayer player, LevelChunk chunk, ServerLevel level) {
         super(player, chunk.getPos(), level);
         this.chunk = chunk;
      }

      public LevelChunk getChunk() {
         return this.chunk;
      }
   }
}
