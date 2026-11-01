package net.neoforged.neoforge.event.level;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class ExplosionEvent extends Event {
   private final Level level;
   private final ServerExplosion explosion;

   public ExplosionEvent(Level level, ServerExplosion explosion) {
      this.level = level;
      this.explosion = explosion;
   }

   public Level getLevel() {
      return this.level;
   }

   public ServerExplosion getExplosion() {
      return this.explosion;
   }

   public static class Detonate extends ExplosionEvent {
      private final List<Entity> entityList;
      private final List<BlockPos> blockList;

      public Detonate(Level level, ServerExplosion explosion, List<Entity> entityList, List<BlockPos> blockList) {
         super(level, explosion);
         this.entityList = entityList;
         this.blockList = blockList;
      }

      public List<BlockPos> getAffectedBlocks() {
         return this.blockList;
      }

      public List<Entity> getAffectedEntities() {
         return this.entityList;
      }
   }

   public static class Start extends ExplosionEvent implements ICancellableEvent {
      public Start(Level level, ServerExplosion explosion) {
         super(level, explosion);
      }
   }
}
