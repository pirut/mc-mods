package net.minecraft.client.renderer.chunk;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public final class TranslucencyPointOfView {
   private int x;
   private int y;
   private int z;

   public static TranslucencyPointOfView of(Vec3 pos, long chunkPos) {
      return new TranslucencyPointOfView().set(pos, chunkPos);
   }

   public TranslucencyPointOfView set(Vec3 pos, long chunkPos) {
      this.x = getCoordinate(pos.x(), SectionPos.x(chunkPos));
      this.y = getCoordinate(pos.y(), SectionPos.y(chunkPos));
      this.z = getCoordinate(pos.z(), SectionPos.z(chunkPos));
      return this;
   }

   private static int getCoordinate(double coord, int chunkCoord) {
      int i = SectionPos.blockToSectionCoord(coord) - chunkCoord;
      return Mth.clamp(i, -1, 1);
   }

   public boolean isAxisAligned() {
      return this.x == 0 || this.y == 0 || this.z == 0;
   }

   @Override
   public boolean equals(Object object) {
      if (object == this) {
         return true;
      } else {
         return !(object instanceof TranslucencyPointOfView translucencyPointOfView)
            ? false
            : this.x == translucencyPointOfView.x && this.y == translucencyPointOfView.y && this.z == translucencyPointOfView.z;
      }
   }
}
