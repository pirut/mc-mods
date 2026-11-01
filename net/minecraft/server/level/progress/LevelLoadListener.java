package net.minecraft.server.level.progress;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public interface LevelLoadListener {
   static LevelLoadListener compose(LevelLoadListener listener1, LevelLoadListener listener2) {
      return new LevelLoadListener() {
         @Override
         public void start(LevelLoadListener.Stage stage, int totalChunks) {
            listener1.start(stage, totalChunks);
            listener2.start(stage, totalChunks);
         }

         @Override
         public void update(LevelLoadListener.Stage stage, int readyChunks, int totalChunks) {
            listener1.update(stage, readyChunks, totalChunks);
            listener2.update(stage, readyChunks, totalChunks);
         }

         @Override
         public void finish(LevelLoadListener.Stage stage) {
            listener1.finish(stage);
            listener2.finish(stage);
         }

         @Override
         public void updateFocus(ResourceKey<Level> dimension, ChunkPos chunkPos) {
            listener1.updateFocus(dimension, chunkPos);
            listener2.updateFocus(dimension, chunkPos);
         }
      };
   }

   void start(LevelLoadListener.Stage stage, int totalChunks);

   void update(LevelLoadListener.Stage stage, int readyChunks, int totalChunks);

   void finish(LevelLoadListener.Stage stage);

   void updateFocus(ResourceKey<Level> dimension, ChunkPos chunkPos);

   public static enum Stage {
      START_SERVER,
      PREPARE_GLOBAL_SPAWN,
      LOAD_INITIAL_CHUNKS,
      LOAD_PLAYER_CHUNKS;
   }
}
