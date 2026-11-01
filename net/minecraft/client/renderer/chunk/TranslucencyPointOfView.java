package net.minecraft.client.renderer.chunk;

import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TranslucencyPointOfView {
   private int x;
   private int y;
   private int z;

   public static TranslucencyPointOfView of(Vec3 p_427320_, long p_427332_) {
      return new TranslucencyPointOfView().set(p_427320_, p_427332_);
   }

   public TranslucencyPointOfView set(Vec3 p_427369_, long p_427451_) {
      this.x = getCoordinate(p_427369_.x(), SectionPos.x(p_427451_));
      this.y = getCoordinate(p_427369_.y(), SectionPos.y(p_427451_));
      this.z = getCoordinate(p_427369_.z(), SectionPos.z(p_427451_));
      return this;
   }

   private static int getCoordinate(double p_427327_, int p_427507_) {
      int i = SectionPos.blockToSectionCoord(p_427327_) - p_427507_;
      return Mth.clamp(i, -1, 1);
   }

   public boolean isAxisAligned() {
      return this.x == 0 || this.y == 0 || this.z == 0;
   }

   @Override
   public boolean equals(Object p_427466_) {
      if (p_427466_ == this) {
         return true;
      } else {
         return p_427466_ instanceof TranslucencyPointOfView translucencypointofview
            ? this.x == translucencypointofview.x && this.y == translucencypointofview.y && this.z == translucencypointofview.z
            : false;
      }
   }
}
