package net.neoforged.neoforge.event.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public class EntityMobGriefingEvent extends EntityEvent {
   private final boolean isMobGriefingEnabled;
   private boolean canGrief;

   public EntityMobGriefingEvent(Level level, Entity entity) {
      super(entity);
      this.isMobGriefingEnabled = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
      this.canGrief = this.isMobGriefingEnabled;
   }

   public boolean isMobGriefingEnabled() {
      return this.isMobGriefingEnabled;
   }

   public void setCanGrief(boolean canGrief) {
      this.canGrief = canGrief;
   }

   public boolean canGrief() {
      return this.canGrief;
   }
}
