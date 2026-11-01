package net.neoforged.neoforge.event.level;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class ExplosionEvent extends Event {
   private final Level level;
   private final Explosion explosion;

   public ExplosionEvent(Level level, Explosion explosion) {
      this.level = level;
      this.explosion = explosion;
   }

   public Level getLevel() {
      return this.level;
   }

   public Explosion getExplosion() {
      return this.explosion;
   }

   public static class Detonate extends ExplosionEvent {
      private final List<Entity> entityList;

      public Detonate(Level level, Explosion explosion, List<Entity> entityList) {
         super(level, explosion);
         this.entityList = entityList;
      }

      public List<BlockPos> getAffectedBlocks() {
         return this.getExplosion().getToBlow();
      }

      public List<Entity> getAffectedEntities() {
         return this.entityList;
      }
   }

   public static class Start extends ExplosionEvent implements ICancellableEvent {
      public Start(Level level, Explosion explosion) {
         super(level, explosion);
      }
   }
}
