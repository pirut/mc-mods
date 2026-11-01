package net.minecraft.world.level.redstone;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;

public class Orientation {
   public static final StreamCodec<ByteBuf, Orientation> STREAM_CODEC = ByteBufCodecs.idMapper(Orientation::fromIndex, Orientation::getIndex);
   private static final Orientation[] ORIENTATIONS = Util.make(() -> {
      Orientation[] aorientation = new Orientation[48];
      generateContext(new Orientation(Direction.UP, Direction.NORTH, Orientation.SideBias.LEFT), aorientation);
      return aorientation;
   });
   private final Direction up;
   private final Direction front;
   private final Direction side;
   private final Orientation.SideBias sideBias;
   private final int index;
   private final List<Direction> neighbors;
   private final List<Direction> horizontalNeighbors;
   private final List<Direction> verticalNeighbors;
   private final Map<Direction, Orientation> withFront = new EnumMap<>(Direction.class);
   private final Map<Direction, Orientation> withUp = new EnumMap<>(Direction.class);
   private final Map<Orientation.SideBias, Orientation> withSideBias = new EnumMap<>(Orientation.SideBias.class);

   private Orientation(Direction p_362594_, Direction p_362639_, Orientation.SideBias p_361294_) {
      this.up = p_362594_;
      this.front = p_362639_;
      this.sideBias = p_361294_;
      this.index = generateIndex(p_362594_, p_362639_, p_361294_);
      Vec3i vec3i = p_362639_.getUnitVec3i().cross(p_362594_.getUnitVec3i());
      Direction direction = Direction.getNearest(vec3i, null);
      Objects.requireNonNull(direction);
      if (this.sideBias == Orientation.SideBias.RIGHT) {
         this.side = direction;
      } else {
         this.side = direction.getOpposite();
      }

      this.neighbors = List.of(this.front.getOpposite(), this.front, this.side, this.side.getOpposite(), this.up.getOpposite(), this.up);
      this.horizontalNeighbors = this.neighbors.stream().filter(p_365498_ -> p_365498_.getAxis() != this.up.getAxis()).toList();
      this.verticalNeighbors = this.neighbors.stream().filter(p_360839_ -> p_360839_.getAxis() == this.up.getAxis()).toList();
   }

   public static Orientation of(Direction p_364419_, Direction p_364902_, Orientation.SideBias p_361055_) {
      return ORIENTATIONS[generateIndex(p_364419_, p_364902_, p_361055_)];
   }

   public Orientation withUp(Direction p_362408_) {
      return this.withUp.get(p_362408_);
   }

   public Orientation withFront(Direction p_364130_) {
      return this.withFront.get(p_364130_);
   }

   public Orientation withFrontPreserveUp(Direction p_366441_) {
      return p_366441_.getAxis() == this.up.getAxis() ? this : this.withFront.get(p_366441_);
   }

   public Orientation withFrontAdjustSideBias(Direction p_360736_) {
      Orientation orientation = this.withFront(p_360736_);
      return this.front == orientation.side ? orientation.withMirror() : orientation;
   }

   public Orientation withSideBias(Orientation.SideBias p_361549_) {
      return this.withSideBias.get(p_361549_);
   }

   public Orientation withMirror() {
      return this.withSideBias(this.sideBias.getOpposite());
   }

   public Direction getFront() {
      return this.front;
   }

   public Direction getUp() {
      return this.up;
   }

   public Direction getSide() {
      return this.side;
   }

   public Orientation.SideBias getSideBias() {
      return this.sideBias;
   }

   public List<Direction> getDirections() {
      return this.neighbors;
   }

   public List<Direction> getHorizontalDirections() {
      return this.horizontalNeighbors;
   }

   public List<Direction> getVerticalDirections() {
      return this.verticalNeighbors;
   }

   @Override
   public String toString() {
      return "[up=" + this.up + ",front=" + this.front + ",sideBias=" + this.sideBias + "]";
   }

   public int getIndex() {
      return this.index;
   }

   public static Orientation fromIndex(int p_360364_) {
      return ORIENTATIONS[p_360364_];
   }

   public static Orientation random(RandomSource p_362443_) {
      return Util.getRandom(ORIENTATIONS, p_362443_);
   }

   private static Orientation generateContext(Orientation p_360409_, Orientation[] p_363858_) {
      if (p_363858_[p_360409_.getIndex()] != null) {
         return p_363858_[p_360409_.getIndex()];
      } else {
         p_363858_[p_360409_.getIndex()] = p_360409_;

         for (Orientation.SideBias orientation$sidebias : Orientation.SideBias.values()) {
            p_360409_.withSideBias.put(orientation$sidebias, generateContext(new Orientation(p_360409_.up, p_360409_.front, orientation$sidebias), p_363858_));
         }

         for (Direction direction1 : Direction.values()) {
            Direction direction = p_360409_.up;
            if (direction1 == p_360409_.up) {
               direction = p_360409_.front.getOpposite();
            }

            if (direction1 == p_360409_.up.getOpposite()) {
               direction = p_360409_.front;
            }

            p_360409_.withFront.put(direction1, generateContext(new Orientation(direction, direction1, p_360409_.sideBias), p_363858_));
         }

         for (Direction direction2 : Direction.values()) {
            Direction direction3 = p_360409_.front;
            if (direction2 == p_360409_.front) {
               direction3 = p_360409_.up.getOpposite();
            }

            if (direction2 == p_360409_.front.getOpposite()) {
               direction3 = p_360409_.up;
            }

            p_360409_.withUp.put(direction2, generateContext(new Orientation(direction2, direction3, p_360409_.sideBias), p_363858_));
         }

         return p_360409_;
      }
   }

   @VisibleForTesting
   protected static int generateIndex(Direction p_363983_, Direction p_362587_, Orientation.SideBias p_364035_) {
      if (p_363983_.getAxis() == p_362587_.getAxis()) {
         throw new IllegalStateException("Up-vector and front-vector can not be on the same axis");
      } else {
         int i;
         if (p_363983_.getAxis() == Direction.Axis.Y) {
            i = p_362587_.getAxis() == Direction.Axis.X ? 1 : 0;
         } else {
            i = p_362587_.getAxis() == Direction.Axis.Y ? 1 : 0;
         }

         int j = i << 1 | p_362587_.getAxisDirection().ordinal();
         return ((p_363983_.ordinal() << 2) + j << 1) + p_364035_.ordinal();
      }
   }

   public static enum SideBias {
      LEFT("left"),
      RIGHT("right");

      private final String name;

      private SideBias(String p_362570_) {
         this.name = p_362570_;
      }

      public Orientation.SideBias getOpposite() {
         return this == LEFT ? RIGHT : LEFT;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }
}
