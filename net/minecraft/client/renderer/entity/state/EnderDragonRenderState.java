package net.minecraft.client.renderer.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class EnderDragonRenderState extends EntityRenderState {
   public float flapTime;
   public float deathTime;
   public boolean hasRedOverlay;
   @Nullable
   public Vec3 beamOffset;
   public boolean isLandingOrTakingOff;
   public boolean isSitting;
   public double distanceToEgg;
   public float partialTicks;
   public final DragonFlightHistory flightHistory = new DragonFlightHistory();

   public DragonFlightHistory.Sample getHistoricalPos(int index) {
      return this.flightHistory.get(index, this.partialTicks);
   }

   public float getHeadPartYOffset(int part, DragonFlightHistory.Sample start, DragonFlightHistory.Sample current) {
      double d;
      if (this.isLandingOrTakingOff) {
         d = part / Math.max(this.distanceToEgg / 4.0, 1.0);
      } else if (this.isSitting) {
         d = part;
      } else if (part == 6) {
         d = 0.0;
      } else {
         d = current.y() - start.y();
      }

      return (float)d;
   }
}
