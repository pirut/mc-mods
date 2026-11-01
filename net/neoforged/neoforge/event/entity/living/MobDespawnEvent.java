package net.neoforged.neoforge.event.entity.living;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.ApiStatus.Internal;

public class MobDespawnEvent extends MobSpawnEvent {
   protected MobDespawnEvent.Result result = MobDespawnEvent.Result.DEFAULT;

   @Internal
   public MobDespawnEvent(Mob mob, ServerLevelAccessor level) {
      super(mob, level, mob.getX(), mob.getY(), mob.getZ());
   }

   public void setResult(MobDespawnEvent.Result result) {
      this.result = result;
   }

   public MobDespawnEvent.Result getResult() {
      return this.result;
   }

   public static enum Result {
      ALLOW,
      DEFAULT,
      DENY;
   }
}
