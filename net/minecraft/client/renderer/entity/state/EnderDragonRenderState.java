package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
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

   public DragonFlightHistory.Sample getHistoricalPos(int p_364384_) {
      return this.flightHistory.get(p_364384_, this.partialTicks);
   }

   public float getHeadPartYOffset(int p_361799_, DragonFlightHistory.Sample p_363414_, DragonFlightHistory.Sample p_363400_) {
      double d0;
      if (this.isLandingOrTakingOff) {
         d0 = p_361799_ / Math.max(this.distanceToEgg / 4.0, 1.0);
      } else if (this.isSitting) {
         d0 = p_361799_;
      } else if (p_361799_ == 6) {
         d0 = 0.0;
      } else {
         d0 = p_363400_.y() - p_363414_.y();
      }

      return (float)d0;
   }
}
