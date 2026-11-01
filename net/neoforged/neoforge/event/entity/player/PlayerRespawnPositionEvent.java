package net.neoforged.neoforge.event.entity.player;

import java.util.Objects;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;

public class PlayerRespawnPositionEvent extends PlayerEvent {
   private TeleportTransition teleportTransition;
   private final TeleportTransition originalTeleportTransition;
   private final boolean fromEndFight;
   private boolean copyOriginalSpawnPosition;

   public PlayerRespawnPositionEvent(ServerPlayer player, TeleportTransition teleportTransition, boolean fromEndFight) {
      super(player);
      this.teleportTransition = teleportTransition;
      this.originalTeleportTransition = teleportTransition;
      this.fromEndFight = fromEndFight;
      this.copyOriginalSpawnPosition = !this.originalTeleportTransition.missingRespawnBlock();
   }

   public TeleportTransition getTeleportTransition() {
      return this.teleportTransition;
   }

   public void setTeleportTransition(TeleportTransition teleportTransition) {
      this.teleportTransition = teleportTransition;
   }

   public void setRespawnLevel(ResourceKey<Level> respawnLevelResourceKey) {
      MinecraftServer server = Objects.requireNonNull(this.getEntity().level().getServer(), "The player is not in a ServerLevel somehow?");
      ServerLevel level = Objects.requireNonNull(server.getLevel(respawnLevelResourceKey), "Level " + respawnLevelResourceKey + " does not exist!");
      TeleportTransition dt = this.getTeleportTransition();
      this.setTeleportTransition(
         new TeleportTransition(level, dt.position(), dt.deltaMovement(), dt.yRot(), dt.xRot(), dt.relatives(), dt.postTeleportTransition())
      );
   }

   public TeleportTransition getOriginalTeleportTransition() {
      return this.originalTeleportTransition;
   }

   public boolean copyOriginalSpawnPosition() {
      return this.copyOriginalSpawnPosition;
   }

   public void setCopyOriginalSpawnPosition(boolean copyOriginalSpawnPosition) {
      this.copyOriginalSpawnPosition = copyOriginalSpawnPosition;
   }

   public boolean isFromEndFight() {
      return this.fromEndFight;
   }
}
