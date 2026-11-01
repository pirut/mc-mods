package net.neoforged.neoforge.event.tick;

import java.util.function.BooleanSupplier;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

public abstract class LevelTickEvent extends Event {
   private final BooleanSupplier hasTime;
   private final Level level;

   protected LevelTickEvent(BooleanSupplier hasTime, Level level) {
      this.hasTime = hasTime;
      this.level = level;
   }

   public boolean hasTime() {
      return this.hasTime.getAsBoolean();
   }

   public Level getLevel() {
      return this.level;
   }

   public static class Post extends LevelTickEvent {
      public Post(BooleanSupplier haveTime, Level level) {
         super(haveTime, level);
      }
   }

   public static class Pre extends LevelTickEvent {
      public Pre(BooleanSupplier haveTime, Level level) {
         super(haveTime, level);
      }
   }
}
