package net.neoforged.neoforge.event.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gamerules.GameRules;

public class EntityMobGriefingEvent extends EntityEvent {
   private final boolean isMobGriefingEnabled;
   private boolean canGrief;

   public EntityMobGriefingEvent(ServerLevel level, Entity entity) {
      super(entity);
      this.isMobGriefingEnabled = level.getGameRules().get(GameRules.MOB_GRIEFING);
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
