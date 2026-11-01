package net.neoforged.neoforge.event.entity.player;

import java.util.Objects;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;

public class PlayerRespawnPositionEvent extends PlayerEvent {
   private DimensionTransition dimensionTransition;
   private final DimensionTransition originalDimensionTransition;
   private final boolean fromEndFight;
   private boolean copyOriginalSpawnPosition;

   public PlayerRespawnPositionEvent(ServerPlayer player, DimensionTransition dimensionTransition, boolean fromEndFight) {
      super(player);
      this.dimensionTransition = dimensionTransition;
      this.originalDimensionTransition = dimensionTransition;
      this.fromEndFight = fromEndFight;
      this.copyOriginalSpawnPosition = !this.originalDimensionTransition.missingRespawnBlock();
   }

   public DimensionTransition getDimensionTransition() {
      return this.dimensionTransition;
   }

   public void setDimensionTransition(DimensionTransition dimensionTransition) {
      this.dimensionTransition = dimensionTransition;
   }

   public void setRespawnLevel(ResourceKey<Level> respawnLevelResourceKey) {
      MinecraftServer server = Objects.requireNonNull(this.getEntity().getServer(), "The player is not in a ServerLevel somehow?");
      ServerLevel level = Objects.requireNonNull(server.getLevel(respawnLevelResourceKey), "Level " + respawnLevelResourceKey + " does not exist!");
      DimensionTransition dt = this.getDimensionTransition();
      this.setDimensionTransition(new DimensionTransition(level, dt.pos(), dt.speed(), dt.yRot(), dt.xRot(), dt.postDimensionTransition()));
   }

   public DimensionTransition getOriginalDimensionTransition() {
      return this.originalDimensionTransition;
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
