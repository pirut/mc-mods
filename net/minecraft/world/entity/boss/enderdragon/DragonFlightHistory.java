package net.minecraft.world.entity.boss.enderdragon;

import java.util.Arrays;
import net.minecraft.util.Mth;

public class DragonFlightHistory {
   public static final int LENGTH = 64;
   private static final int MASK = 63;
   private final DragonFlightHistory.Sample[] samples = new DragonFlightHistory.Sample[64];
   private int head = -1;

   public DragonFlightHistory() {
      Arrays.fill(this.samples, new DragonFlightHistory.Sample(0.0, 0.0F));
   }

   public void copyFrom(DragonFlightHistory p_365185_) {
      System.arraycopy(p_365185_.samples, 0, this.samples, 0, 64);
      this.head = p_365185_.head;
   }

   public void record(double p_362120_, float p_362390_) {
      DragonFlightHistory.Sample dragonflighthistory$sample = new DragonFlightHistory.Sample(p_362120_, p_362390_);
      if (this.head < 0) {
         Arrays.fill(this.samples, dragonflighthistory$sample);
      }

      if (++this.head == 64) {
         this.head = 0;
      }

      this.samples[this.head] = dragonflighthistory$sample;
   }

   public DragonFlightHistory.Sample get(int p_361245_) {
      return this.samples[this.head - p_361245_ & 63];
   }

   public DragonFlightHistory.Sample get(int p_361414_, float p_360752_) {
      DragonFlightHistory.Sample dragonflighthistory$sample = this.get(p_361414_);
      DragonFlightHistory.Sample dragonflighthistory$sample1 = this.get(p_361414_ + 1);
      return new DragonFlightHistory.Sample(
         Mth.lerp((double)p_360752_, dragonflighthistory$sample1.y, dragonflighthistory$sample.y),
         Mth.rotLerp(p_360752_, dragonflighthistory$sample1.yRot, dragonflighthistory$sample.yRot)
      );
   }

   public record Sample(double y, float yRot) {
   }
}
