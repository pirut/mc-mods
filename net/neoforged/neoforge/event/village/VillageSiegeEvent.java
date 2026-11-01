package net.neoforged.neoforge.event.village;

import net.minecraft.world.entity.ai.village.VillageSiege;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class VillageSiegeEvent extends Event implements ICancellableEvent {
   private final VillageSiege siege;
   private final Level level;
   private final Player player;
   private final Vec3 attemptedSpawnPos;

   public VillageSiegeEvent(VillageSiege siege, Level level, Player player, Vec3 attemptedSpawnPos) {
      this.siege = siege;
      this.level = level;
      this.player = player;
      this.attemptedSpawnPos = attemptedSpawnPos;
   }

   public VillageSiege getSiege() {
      return this.siege;
   }

   public Level getLevel() {
      return this.level;
   }

   public Player getPlayer() {
      return this.player;
   }

   public Vec3 getAttemptedSpawnPos() {
      return this.attemptedSpawnPos;
   }
}
