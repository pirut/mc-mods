package net.minecraft.core;

import com.google.common.collect.Iterators;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public enum Direction implements StringRepresentable {
   DOWN(0, 1, -1, "down", Direction.AxisDirection.NEGATIVE, Direction.Axis.Y, new Vec3i(0, -1, 0)),
   UP(1, 0, -1, "up", Direction.AxisDirection.POSITIVE, Direction.Axis.Y, new Vec3i(0, 1, 0)),
   NORTH(2, 3, 2, "north", Direction.AxisDirection.NEGATIVE, Direction.Axis.Z, new Vec3i(0, 0, -1)),
   SOUTH(3, 2, 0, "south", Direction.AxisDirection.POSITIVE, Direction.Axis.Z, new Vec3i(0, 0, 1)),
   WEST(4, 5, 1, "west", Direction.AxisDirection.NEGATIVE, Direction.Axis.X, new Vec3i(-1, 0, 0)),
   EAST(5, 4, 3, "east", Direction.AxisDirection.POSITIVE, Direction.Axis.X, new Vec3i(1, 0, 0));

   public static final StringRepresentable.EnumCodec<Direction> CODEC = StringRepresentable.fromEnum(Direction::values);
   public static final Codec<Direction> VERTICAL_CODEC = CODEC.validate(Direction::verifyVertical);
   public static final IntFunction<Direction> BY_ID = ByIdMap.continuous(Direction::get3DDataValue, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
   public static final StreamCodec<ByteBuf, Direction> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Direction::get3DDataValue);
   private final int data3d;
   private final int oppositeIndex;
   private final int data2d;
   private final String name;
   private final Direction.Axis axis;
   private final Direction.AxisDirection axisDirection;
   private final Vec3i normal;
   private static final Direction[] VALUES = values();
   private static final Direction[] BY_3D_DATA = Arrays.stream(VALUES).sorted(Comparator.comparingInt(p_235687_ -> p_235687_.data3d)).toArray(Direction[]::new);
   private static final Direction[] BY_2D_DATA = Arrays.stream(VALUES)
      .filter(p_235685_ -> p_235685_.getAxis().isHorizontal())
      .sorted(Comparator.comparingInt(p_235683_ -> p_235683_.data2d))
      .toArray(Direction[]::new);

   private Direction(
      int p_122356_, int p_122357_, int p_122358_, String p_122359_, Direction.AxisDirection p_122360_, Direction.Axis p_122361_, Vec3i p_122362_
   ) {
      this.data3d = p_122356_;
      this.data2d = p_122358_;
      this.oppositeIndex = p_122357_;
      this.name = p_122359_;
      this.axis = p_122361_;
      this.axisDirection = p_122360_;
      this.normal = p_122362_;
   }

   public static Direction[] orderedByNearest(Entity p_122383_) {
      float $$1 = p_122383_.getViewXRot(1.0F) * (float) (Math.PI / 180.0);
      float $$2 = -p_122383_.getViewYRot(1.0F) * (float) (Math.PI / 180.0);
      float $$3 = Mth.sin($$1);
      float $$4 = Mth.cos($$1);
      float $$5 = Mth.sin($$2);
      float $$6 = Mth.cos($$2);
      boolean $$7 = $$5 > 0.0F;
      boolean $$8 = $$3 < 0.0F;
      boolean $$9 = $$6 > 0.0F;
      float $$10 = $$7 ? $$5 : -$$5;
      float $$11 = $$8 ? -$$3 : $$3;
      float $$12 = $$9 ? $$6 : -$$6;
      float $$13 = $$10 * $$4;
      float $$14 = $$12 * $$4;
      Direction $$15 = $$7 ? EAST : WEST;
      Direction $$16 = $$8 ? UP : DOWN;
      Direction $$17 = $$9 ? SOUTH : NORTH;
      if ($$10 > $$12) {
         if ($$11 > $$13) {
            return makeDirectionArray($$16, $$15, $$17);
         } else {
            return $$14 > $$11 ? makeDirectionArray($$15, $$17, $$16) : makeDirectionArray($$15, $$16, $$17);
         }
      } else if ($$11 > $$14) {
         return makeDirectionArray($$16, $$17, $$15);
      } else {
         return $$13 > $$11 ? makeDirectionArray($$17, $$15, $$16) : makeDirectionArray($$17, $$16, $$15);
      }
   }

   private static Direction[] makeDirectionArray(Direction p_122399_, Direction p_122400_, Direction p_122401_) {
      return new Direction[]{p_122399_, p_122400_, p_122401_, p_122401_.getOpposite(), p_122400_.getOpposite(), p_122399_.getOpposite()};
   }

   public static Direction rotate(Matrix4f p_254393_, Direction p_254252_) {
      Vec3i $$2 = p_254252_.getNormal();
      Vector4f $$3 = p_254393_.transform(new Vector4f($$2.getX(), $$2.getY(), $$2.getZ(), 0.0F));
      return getNearest($$3.x(), $$3.y(), $$3.z());
   }

   public static Collection<Direction> allShuffled(RandomSource p_235668_) {
      return Util.shuffledCopy(values(), p_235668_);
   }

   public static Stream<Direction> stream() {
      return Stream.of(VALUES);
   }

   public Quaternionf getRotation() {
      return switch (this) {
         case DOWN -> new Quaternionf().rotationX((float) Math.PI);
         case UP -> new Quaternionf();
         case NORTH -> new Quaternionf().rotationXYZ((float) (Math.PI / 2), 0.0F, (float) Math.PI);
         case SOUTH -> new Quaternionf().rotationX((float) (Math.PI / 2));
         case WEST -> new Quaternionf().rotationXYZ((float) (Math.PI / 2), 0.0F, (float) (Math.PI / 2));
         case EAST -> new Quaternionf().rotationXYZ((float) (Math.PI / 2), 0.0F, (float) (-Math.PI / 2));
      };
   }

   public int get3DDataValue() {
      return this.data3d;
   }

   public int get2DDataValue() {
      return this.data2d;
   }

   public Direction.AxisDirection getAxisDirection() {
      return this.axisDirection;
   }

   public static Direction getFacingAxis(Entity p_175358_, Direction.Axis p_175359_) {
      return switch (p_175359_) {
         case X -> EAST.isFacingAngle(p_175358_.getViewYRot(1.0F)) ? EAST : WEST;
         case Y -> p_175358_.getViewXRot(1.0F) < 0.0F ? UP : DOWN;
         case Z -> SOUTH.isFacingAngle(p_175358_.getViewYRot(1.0F)) ? SOUTH : NORTH;
      };
   }

   public Direction getOpposite() {
      return from3DDataValue(this.oppositeIndex);
   }

   public Direction getClockWise(Direction.Axis p_175363_) {
      return switch (p_175363_) {
         case X -> this != WEST && this != EAST ? this.getClockWiseX() : this;
         case Y -> this != UP && this != DOWN ? this.getClockWise() : this;
         case Z -> this != NORTH && this != SOUTH ? this.getClockWiseZ() : this;
      };
   }

   public Direction getCounterClockWise(Direction.Axis p_175365_) {
      return switch (p_175365_) {
         case X -> this != WEST && this != EAST ? this.getCounterClockWiseX() : this;
         case Y -> this != UP && this != DOWN ? this.getCounterClockWise() : this;
         case Z -> this != NORTH && this != SOUTH ? this.getCounterClockWiseZ() : this;
      };
   }

   public Direction getClockWise() {
      return switch (this) {
         case NORTH -> EAST;
         case SOUTH -> WEST;
         case WEST -> NORTH;
         case EAST -> SOUTH;
         default -> throw new IllegalStateException("Unable to get Y-rotated facing of " + this);
      };
   }

   private Direction getClockWiseX() {
      return switch (this) {
         case DOWN -> SOUTH;
         case UP -> NORTH;
         case NORTH -> DOWN;
         case SOUTH -> UP;
         default -> throw new IllegalStateException("Unable to get X-rotated facing of " + this);
      };
   }

   private Direction getCounterClockWiseX() {
      return switch (this) {
         case DOWN -> NORTH;
         case UP -> SOUTH;
         case NORTH -> UP;
         case SOUTH -> DOWN;
         default -> throw new IllegalStateException("Unable to get X-rotated facing of " + this);
      };
   }

   private Direction getClockWiseZ() {
      return switch (this) {
         case DOWN -> WEST;
         case UP -> EAST;
         default -> throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
         case WEST -> UP;
         case EAST -> DOWN;
      };
   }

   private Direction getCounterClockWiseZ() {
      return switch (this) {
         case DOWN -> EAST;
         case UP -> WEST;
         default -> throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
         case WEST -> DOWN;
         case EAST -> UP;
      };
   }

   public Direction getCounterClockWise() {
      return switch (this) {
         case NORTH -> WEST;
         case SOUTH -> EAST;
         case WEST -> SOUTH;
         case EAST -> NORTH;
         default -> throw new IllegalStateException("Unable to get CCW facing of " + this);
      };
   }

   public int getStepX() {
      return this.normal.getX();
   }

   public int getStepY() {
      return this.normal.getY();
   }

   public int getStepZ() {
      return this.normal.getZ();
   }

   public Vector3f step() {
      return new Vector3f(this.getStepX(), this.getStepY(), this.getStepZ());
   }

   public String getName() {
      return this.name;
   }

   public Direction.Axis getAxis() {
      return this.axis;
   }

   @Nullable
   public static Direction byName(@Nullable String p_122403_) {
      return CODEC.byName(p_122403_);
   }

   public static Direction from3DDataValue(int p_122377_) {
      return BY_3D_DATA[Mth.abs(p_122377_ % BY_3D_DATA.length)];
   }

   public static Direction from2DDataValue(int p_122408_) {
      return BY_2D_DATA[Mth.abs(p_122408_ % BY_2D_DATA.length)];
   }

   @Nullable
   public static Direction fromDelta(int p_278323_, int p_278296_, int p_278347_) {
      if (p_278323_ == 0) {
         if (p_278296_ == 0) {
            if (p_278347_ > 0) {
               return SOUTH;
            }

            if (p_278347_ < 0) {
               return NORTH;
            }
         } else if (p_278347_ == 0) {
            if (p_278296_ > 0) {
               return UP;
            }

            return DOWN;
         }
      } else if (p_278296_ == 0 && p_278347_ == 0) {
         if (p_278323_ > 0) {
            return EAST;
         }

         return WEST;
      }

      return null;
   }

   public static Direction fromYRot(double p_122365_) {
      return from2DDataValue(Mth.floor(p_122365_ / 90.0 + 0.5) & 3);
   }

   public static Direction fromAxisAndDirection(Direction.Axis p_122388_, Direction.AxisDirection p_122389_) {
      return switch (p_122388_) {
         case X -> p_122389_ == Direction.AxisDirection.POSITIVE ? EAST : WEST;
         case Y -> p_122389_ == Direction.AxisDirection.POSITIVE ? UP : DOWN;
         case Z -> p_122389_ == Direction.AxisDirection.POSITIVE ? SOUTH : NORTH;
      };
   }

   public float toYRot() {
      return (this.data2d & 3) * 90;
   }

   public static Direction getRandom(RandomSource p_235673_) {
      return Util.getRandom(VALUES, p_235673_);
   }

   public static Direction getNearest(double p_122367_, double p_122368_, double p_122369_) {
      return getNearest((float)p_122367_, (float)p_122368_, (float)p_122369_);
   }

   public static Direction getNearest(float p_122373_, float p_122374_, float p_122375_) {
      Direction $$3 = NORTH;
      float $$4 = Float.MIN_VALUE;

      for (Direction $$5 : VALUES) {
         float $$6 = p_122373_ * $$5.normal.getX() + p_122374_ * $$5.normal.getY() + p_122375_ * $$5.normal.getZ();
         if ($$6 > $$4) {
            $$4 = $$6;
            $$3 = $$5;
         }
      }

      return $$3;
   }

   public static Direction getNearest(Vec3 p_333951_) {
      return getNearest(p_333951_.x, p_333951_.y, p_333951_.z);
   }

   @Override
   public String toString() {
      return this.name;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   private static DataResult<Direction> verifyVertical(Direction p_194529_) {
      return p_194529_.getAxis().isVertical() ? DataResult.success(p_194529_) : DataResult.error(() -> "Expected a vertical direction");
   }

   public static Direction get(Direction.AxisDirection p_122391_, Direction.Axis p_122392_) {
      for (Direction $$2 : VALUES) {
         if ($$2.getAxisDirection() == p_122391_ && $$2.getAxis() == p_122392_) {
            return $$2;
         }
      }

      throw new IllegalArgumentException("No such direction: " + p_122391_ + " " + p_122392_);
   }

   public Vec3i getNormal() {
      return this.normal;
   }

   public boolean isFacingAngle(float p_122371_) {
      float $$1 = p_122371_ * (float) (Math.PI / 180.0);
      float $$2 = -Mth.sin($$1);
      float $$3 = Mth.cos($$1);
      return this.normal.getX() * $$2 + this.normal.getZ() * $$3 > 0.0F;
   }

   public static enum Axis implements StringRepresentable, Predicate<Direction> {
      X("x") {
         @Override
         public int choose(int p_122496_, int p_122497_, int p_122498_) {
            return p_122496_;
         }

         @Override
         public double choose(double p_122492_, double p_122493_, double p_122494_) {
            return p_122492_;
         }
      },
      Y("y") {
         @Override
         public int choose(int p_122510_, int p_122511_, int p_122512_) {
            return p_122511_;
         }

         @Override
         public double choose(double p_122506_, double p_122507_, double p_122508_) {
            return p_122507_;
         }
      },
      Z("z") {
         @Override
         public int choose(int p_122524_, int p_122525_, int p_122526_) {
            return p_122526_;
         }

         @Override
         public double choose(double p_122520_, double p_122521_, double p_122522_) {
            return p_122522_;
         }
      };

      public static final Direction.Axis[] VALUES = values();
      public static final StringRepresentable.EnumCodec<Direction.Axis> CODEC = StringRepresentable.fromEnum(Direction.Axis::values);
      private final String name;

      Axis(String p_122456_) {
         this.name = p_122456_;
      }

      @Nullable
      public static Direction.Axis byName(String p_122474_) {
         return CODEC.byName(p_122474_);
      }

      public String getName() {
         return this.name;
      }

      public boolean isVertical() {
         return this == Y;
      }

      public boolean isHorizontal() {
         return this == X || this == Z;
      }

      @Override
      public String toString() {
         return this.name;
      }

      public static Direction.Axis getRandom(RandomSource p_235689_) {
         return Util.getRandom(VALUES, p_235689_);
      }

      public boolean test(@Nullable Direction p_122472_) {
         return p_122472_ != null && p_122472_.getAxis() == this;
      }

      public Direction.Plane getPlane() {
         return switch (this) {
            case X, Z -> Direction.Plane.HORIZONTAL;
            case Y -> Direction.Plane.VERTICAL;
         };
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public abstract int choose(int var1, int var2, int var3);

      public abstract double choose(double var1, double var3, double var5);
   }

   public static enum AxisDirection {
      POSITIVE(1, "Towards positive"),
      NEGATIVE(-1, "Towards negative");

      private final int step;
      private final String name;

      private AxisDirection(int p_122538_, String p_122539_) {
         this.step = p_122538_;
         this.name = p_122539_;
      }

      public int getStep() {
         return this.step;
      }

      public String getName() {
         return this.name;
      }

      @Override
      public String toString() {
         return this.name;
      }

      public Direction.AxisDirection opposite() {
         return this == POSITIVE ? NEGATIVE : POSITIVE;
      }
   }

   public static enum Plane implements Iterable<Direction>, Predicate<Direction> {
      HORIZONTAL(new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}, new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}),
      VERTICAL(new Direction[]{Direction.UP, Direction.DOWN}, new Direction.Axis[]{Direction.Axis.Y});

      private final Direction[] faces;
      private final Direction.Axis[] axis;

      private Plane(Direction[] p_122555_, Direction.Axis[] p_122556_) {
         this.faces = p_122555_;
         this.axis = p_122556_;
      }

      public Direction getRandomDirection(RandomSource p_235691_) {
         return Util.getRandom(this.faces, p_235691_);
      }

      public Direction.Axis getRandomAxis(RandomSource p_235693_) {
         return Util.getRandom(this.axis, p_235693_);
      }

      public boolean test(@Nullable Direction p_122559_) {
         return p_122559_ != null && p_122559_.getAxis().getPlane() == this;
      }

      @Override
      public Iterator<Direction> iterator() {
         return Iterators.forArray(this.faces);
      }

      public Stream<Direction> stream() {
         return Arrays.stream(this.faces);
      }

      public List<Direction> shuffledCopy(RandomSource p_235695_) {
         return Util.shuffledCopy(this.faces, p_235695_);
      }

      public int length() {
         return this.faces.length;
      }
   }
}
