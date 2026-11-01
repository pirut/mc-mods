package net.neoforged.neoforge.event.entity.player;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public class PlayerSpawnPhantomsEvent extends PlayerEvent {
   private int phantomsToSpawn;
   private PlayerSpawnPhantomsEvent.Result result = PlayerSpawnPhantomsEvent.Result.DEFAULT;

   public PlayerSpawnPhantomsEvent(Player player, int phantomsToSpawn) {
      super(player);
      this.phantomsToSpawn = phantomsToSpawn;
   }

   public int getPhantomsToSpawn() {
      return this.phantomsToSpawn;
   }

   public void setPhantomsToSpawn(int phantomsToSpawn) {
      this.phantomsToSpawn = phantomsToSpawn;
   }

   public void setResult(PlayerSpawnPhantomsEvent.Result result) {
      this.result = result;
   }

   public PlayerSpawnPhantomsEvent.Result getResult() {
      return this.result;
   }

   public boolean shouldSpawnPhantoms(ServerLevel level, BlockPos pos) {
      return this.getResult() == PlayerSpawnPhantomsEvent.Result.ALLOW
         ? true
         : this.getResult() == PlayerSpawnPhantomsEvent.Result.DEFAULT
            && (!level.dimensionType().hasSkyLight() || pos.getY() >= level.getSeaLevel() && level.canSeeSky(pos));
   }

   public static enum Result {
      ALLOW,
      DEFAULT,
      DENY;
   }
}
