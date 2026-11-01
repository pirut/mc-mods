package net.neoforged.neoforge.event.entity.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.neoforged.bus.api.ICancellableEvent;

public class PermissionsChangedEvent extends PlayerEvent implements ICancellableEvent {
   private final LevelBasedPermissionSet newLevel;
   private final LevelBasedPermissionSet oldLevel;

   public PermissionsChangedEvent(ServerPlayer player, LevelBasedPermissionSet newLevel, LevelBasedPermissionSet oldLevel) {
      super(player);
      this.oldLevel = oldLevel;
      this.newLevel = newLevel;
   }

   public LevelBasedPermissionSet getNewLevel() {
      return this.newLevel;
   }

   public LevelBasedPermissionSet getOldLevel() {
      return this.oldLevel;
   }
}
