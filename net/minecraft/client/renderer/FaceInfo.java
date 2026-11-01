package net.minecraft.client.renderer;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public enum FaceInfo {
   DOWN(
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MAX_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MIN_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MIN_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MAX_Z)
   ),
   UP(
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MIN_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MAX_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MAX_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MIN_Z)
   ),
   NORTH(
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MIN_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MIN_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MIN_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MIN_Z)
   ),
   SOUTH(
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MAX_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MAX_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MAX_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MAX_Z)
   ),
   WEST(
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MIN_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MIN_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MAX_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MAX_Z)
   ),
   EAST(
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MAX_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MAX_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MIN_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MIN_Z)
   );

   private static final Map<Direction, FaceInfo> BY_FACING = Util.make(new EnumMap<>(Direction.class), p_470487_ -> {
      p_470487_.put(Direction.DOWN, DOWN);
      p_470487_.put(Direction.UP, UP);
      p_470487_.put(Direction.NORTH, NORTH);
      p_470487_.put(Direction.SOUTH, SOUTH);
      p_470487_.put(Direction.WEST, WEST);
      p_470487_.put(Direction.EAST, EAST);
   });
   private final FaceInfo.VertexInfo[] infos;

   public static FaceInfo fromFacing(Direction p_108985_) {
      return BY_FACING.get(p_108985_);
   }

   private FaceInfo(FaceInfo.VertexInfo... p_108981_) {
      this.infos = p_108981_;
   }

   public FaceInfo.VertexInfo getVertexInfo(int p_108983_) {
      return this.infos[p_108983_];
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Extent {
      MIN_X,
      MIN_Y,
      MIN_Z,
      MAX_X,
      MAX_Y,
      MAX_Z;

      public float select(Vector3fc p_470543_, Vector3fc p_470591_) {
         return switch (this) {
            case MIN_X -> p_470543_.x();
            case MIN_Y -> p_470543_.y();
            case MIN_Z -> p_470543_.z();
            case MAX_X -> p_470591_.x();
            case MAX_Y -> p_470591_.y();
            case MAX_Z -> p_470591_.z();
         };
      }

      public float select(float p_470551_, float p_470544_, float p_470721_, float p_470649_, float p_470659_, float p_470847_) {
         return switch (this) {
            case MIN_X -> p_470551_;
            case MIN_Y -> p_470544_;
            case MIN_Z -> p_470721_;
            case MAX_X -> p_470649_;
            case MAX_Y -> p_470659_;
            case MAX_Z -> p_470847_;
         };
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record VertexInfo(FaceInfo.Extent xFace, FaceInfo.Extent yFace, FaceInfo.Extent zFace) {
      public Vector3f select(Vector3fc p_470795_, Vector3fc p_470752_) {
         return new Vector3f(this.xFace.select(p_470795_, p_470752_), this.yFace.select(p_470795_, p_470752_), this.zFace.select(p_470795_, p_470752_));
      }
   }
}
