package net.minecraft.world.phys.shapes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.math.DoubleMath;
import com.google.common.math.IntMath;
import com.mojang.math.OctahedralGroup;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Shapes {
   public static final double EPSILON = 1.0E-7;
   public static final double BIG_EPSILON = 1.0E-6;
   private static final VoxelShape BLOCK = Util.make(() -> {
      DiscreteVoxelShape discreteVoxelShape = new BitSetDiscreteVoxelShape(1, 1, 1);
      discreteVoxelShape.fill(0, 0, 0);
      return new CubeVoxelShape(discreteVoxelShape);
   });
   private static final Vec3 BLOCK_CENTER = new Vec3(0.5, 0.5, 0.5);
   public static final VoxelShape INFINITY = box(
      Double.NEGATIVE_INFINITY,
      Double.NEGATIVE_INFINITY,
      Double.NEGATIVE_INFINITY,
      Double.POSITIVE_INFINITY,
      Double.POSITIVE_INFINITY,
      Double.POSITIVE_INFINITY
   );
   private static final VoxelShape EMPTY = new ArrayVoxelShape(
      new BitSetDiscreteVoxelShape(0, 0, 0),
      new DoubleArrayList(new double[]{0.0}),
      new DoubleArrayList(new double[]{0.0}),
      new DoubleArrayList(new double[]{0.0})
   );

   public static VoxelShape empty() {
      return EMPTY;
   }

   public static VoxelShape block() {
      return BLOCK;
   }

   public static VoxelShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      if (!(minX > maxX) && !(minY > maxY) && !(minZ > maxZ)) {
         return create(minX, minY, minZ, maxX, maxY, maxZ);
      } else {
         throw new IllegalArgumentException("The min values need to be smaller or equals to the max values");
      }
   }

   public static VoxelShape create(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      if (!(maxX - minX < 1.0E-7) && !(maxY - minY < 1.0E-7) && !(maxZ - minZ < 1.0E-7)) {
         int i = findBits(minX, maxX);
         int j = findBits(minY, maxY);
         int k = findBits(minZ, maxZ);
         if (i < 0 || j < 0 || k < 0) {
            return new ArrayVoxelShape(
               BLOCK.shape,
               DoubleArrayList.wrap(new double[]{minX, maxX}),
               DoubleArrayList.wrap(new double[]{minY, maxY}),
               DoubleArrayList.wrap(new double[]{minZ, maxZ})
            );
         } else if (i == 0 && j == 0 && k == 0) {
            return block();
         } else {
            int l = 1 << i;
            int m = 1 << j;
            int n = 1 << k;
            BitSetDiscreteVoxelShape bitSetDiscreteVoxelShape = BitSetDiscreteVoxelShape.withFilledBounds(
               l,
               m,
               n,
               (int)Math.round(minX * l),
               (int)Math.round(minY * m),
               (int)Math.round(minZ * n),
               (int)Math.round(maxX * l),
               (int)Math.round(maxY * m),
               (int)Math.round(maxZ * n)
            );
            return new CubeVoxelShape(bitSetDiscreteVoxelShape);
         }
      } else {
         return empty();
      }
   }

   public static VoxelShape create(AABB aabb) {
      return create(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
   }

   @VisibleForTesting
   protected static int findBits(double minBits, double maxBits) {
      if (!(minBits < -1.0E-7) && !(maxBits > 1.0000001)) {
         for (int i = 0; i <= 3; i++) {
            int j = 1 << i;
            double d = minBits * j;
            double e = maxBits * j;
            boolean bl = Math.abs(d - Math.round(d)) < 1.0E-7 * j;
            boolean bl2 = Math.abs(e - Math.round(e)) < 1.0E-7 * j;
            if (bl && bl2) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   protected static long lcm(int aa, int bb) {
      return (long)aa * (bb / IntMath.gcd(aa, bb));
   }

   public static VoxelShape or(VoxelShape shape1, VoxelShape shape2) {
      return join(shape1, shape2, BooleanOp.OR);
   }

   public static VoxelShape or(VoxelShape shape1, VoxelShape... others) {
      return Arrays.stream(others).reduce(shape1, Shapes::or);
   }

   public static VoxelShape join(VoxelShape shape1, VoxelShape shape2, BooleanOp function) {
      return joinUnoptimized(shape1, shape2, function).optimize();
   }

   public static VoxelShape joinUnoptimized(VoxelShape shape1, VoxelShape shape2, BooleanOp function) {
      if (function.apply(false, false)) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException());
      } else if (shape1 == shape2) {
         return function.apply(true, true) ? shape1 : empty();
      } else {
         boolean bl = function.apply(true, false);
         boolean bl2 = function.apply(false, true);
         if (shape1.isEmpty()) {
            return bl2 ? shape2 : empty();
         } else if (shape2.isEmpty()) {
            return bl ? shape1 : empty();
         } else {
            IndexMerger indexMerger = createIndexMerger(1, shape1.getCoords(Direction.Axis.X), shape2.getCoords(Direction.Axis.X), bl, bl2);
            IndexMerger indexMerger2 = createIndexMerger(
               indexMerger.size() - 1, shape1.getCoords(Direction.Axis.Y), shape2.getCoords(Direction.Axis.Y), bl, bl2
            );
            IndexMerger indexMerger3 = createIndexMerger(
               (indexMerger.size() - 1) * (indexMerger2.size() - 1), shape1.getCoords(Direction.Axis.Z), shape2.getCoords(Direction.Axis.Z), bl, bl2
            );
            BitSetDiscreteVoxelShape bitSetDiscreteVoxelShape = BitSetDiscreteVoxelShape.join(
               shape1.shape, shape2.shape, indexMerger, indexMerger2, indexMerger3, function
            );
            return (VoxelShape)(indexMerger instanceof DiscreteCubeMerger
                  && indexMerger2 instanceof DiscreteCubeMerger
                  && indexMerger3 instanceof DiscreteCubeMerger
               ? new CubeVoxelShape(bitSetDiscreteVoxelShape)
               : new ArrayVoxelShape(bitSetDiscreteVoxelShape, indexMerger.getList(), indexMerger2.getList(), indexMerger3.getList()));
         }
      }
   }

   public static boolean joinIsNotEmpty(VoxelShape shape1, VoxelShape shape2, BooleanOp resultOperator) {
      if (resultOperator.apply(false, false)) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException());
      } else {
         boolean bl = shape1.isEmpty();
         boolean bl2 = shape2.isEmpty();
         if (!bl && !bl2) {
            if (shape1 == shape2) {
               return resultOperator.apply(true, true);
            } else {
               boolean bl3 = resultOperator.apply(true, false);
               boolean bl4 = resultOperator.apply(false, true);

               for (Direction.Axis axis : AxisCycle.AXIS_VALUES) {
                  if (shape1.max(axis) < shape2.min(axis) - 1.0E-7) {
                     return bl3 || bl4;
                  }

                  if (shape2.max(axis) < shape1.min(axis) - 1.0E-7) {
                     return bl3 || bl4;
                  }
               }

               IndexMerger indexMerger = createIndexMerger(1, shape1.getCoords(Direction.Axis.X), shape2.getCoords(Direction.Axis.X), bl3, bl4);
               IndexMerger indexMerger2 = createIndexMerger(
                  indexMerger.size() - 1, shape1.getCoords(Direction.Axis.Y), shape2.getCoords(Direction.Axis.Y), bl3, bl4
               );
               IndexMerger indexMerger3 = createIndexMerger(
                  (indexMerger.size() - 1) * (indexMerger2.size() - 1), shape1.getCoords(Direction.Axis.Z), shape2.getCoords(Direction.Axis.Z), bl3, bl4
               );
               return joinIsNotEmpty(indexMerger, indexMerger2, indexMerger3, shape1.shape, shape2.shape, resultOperator);
            }
         } else {
            return resultOperator.apply(!bl, !bl2);
         }
      }
   }

   private static boolean joinIsNotEmpty(
      IndexMerger mergerX,
      IndexMerger mergerY,
      IndexMerger mergerZ,
      DiscreteVoxelShape primaryShape,
      DiscreteVoxelShape secondaryShape,
      BooleanOp resultOperator
   ) {
      return !mergerX.forMergedIndexes(
         (i, j, k) -> mergerY.forMergedIndexes(
            (kx, l, m) -> mergerZ.forMergedIndexes((mx, n, o) -> !resultOperator.apply(primaryShape.isFullWide(i, kx, mx), secondaryShape.isFullWide(j, l, n)))
         )
      );
   }

   public static double collide(Direction.Axis movementAxis, AABB collisionBox, Iterable<VoxelShape> possibleHits, double desiredOffset) {
      for (VoxelShape voxelShape : possibleHits) {
         if (Math.abs(desiredOffset) < 1.0E-7) {
            return 0.0;
         }

         desiredOffset = voxelShape.collide(movementAxis, collisionBox, desiredOffset);
      }

      return desiredOffset;
   }

   public static boolean blockOccludes(VoxelShape shape, VoxelShape adjacentShape, Direction side) {
      if (shape == block() && adjacentShape == block()) {
         return true;
      } else if (adjacentShape.isEmpty()) {
         return false;
      } else {
         Direction.Axis axis = side.getAxis();
         Direction.AxisDirection axisDirection = side.getAxisDirection();
         VoxelShape voxelShape = axisDirection == Direction.AxisDirection.POSITIVE ? shape : adjacentShape;
         VoxelShape voxelShape2 = axisDirection == Direction.AxisDirection.POSITIVE ? adjacentShape : shape;
         BooleanOp booleanOp = axisDirection == Direction.AxisDirection.POSITIVE ? BooleanOp.ONLY_FIRST : BooleanOp.ONLY_SECOND;
         return DoubleMath.fuzzyEquals(voxelShape.max(axis), 1.0, 1.0E-7)
            && DoubleMath.fuzzyEquals(voxelShape2.min(axis), 0.0, 1.0E-7)
            && !joinIsNotEmpty(new SliceShape(voxelShape, axis, voxelShape.shape.getSize(axis) - 1), new SliceShape(voxelShape2, axis, 0), booleanOp);
      }
   }

   public static boolean mergedFaceOccludes(VoxelShape shape, VoxelShape adjacentShape, Direction side) {
      if (shape != block() && adjacentShape != block()) {
         Direction.Axis axis = side.getAxis();
         Direction.AxisDirection axisDirection = side.getAxisDirection();
         VoxelShape voxelShape = axisDirection == Direction.AxisDirection.POSITIVE ? shape : adjacentShape;
         VoxelShape voxelShape2 = axisDirection == Direction.AxisDirection.POSITIVE ? adjacentShape : shape;
         if (!DoubleMath.fuzzyEquals(voxelShape.max(axis), 1.0, 1.0E-7)) {
            voxelShape = empty();
         }

         if (!DoubleMath.fuzzyEquals(voxelShape2.min(axis), 0.0, 1.0E-7)) {
            voxelShape2 = empty();
         }

         return !joinIsNotEmpty(
            block(),
            joinUnoptimized(new SliceShape(voxelShape, axis, voxelShape.shape.getSize(axis) - 1), new SliceShape(voxelShape2, axis, 0), BooleanOp.OR),
            BooleanOp.ONLY_FIRST
         );
      } else {
         return true;
      }
   }

   public static boolean faceShapeOccludes(VoxelShape voxelShape1, VoxelShape voxelShape2) {
      if (voxelShape1 == block() || voxelShape2 == block()) {
         return true;
      } else {
         return voxelShape1.isEmpty() && voxelShape2.isEmpty()
            ? false
            : !joinIsNotEmpty(block(), joinUnoptimized(voxelShape1, voxelShape2, BooleanOp.OR), BooleanOp.ONLY_FIRST);
      }
   }

   @VisibleForTesting
   protected static IndexMerger createIndexMerger(int size, DoubleList list1, DoubleList list2, boolean excludeUpper, boolean excludeLower) {
      int i = list1.size() - 1;
      int j = list2.size() - 1;
      if (list1 instanceof CubePointRange && list2 instanceof CubePointRange) {
         long l = lcm(i, j);
         if (size * l <= 256L) {
            return new DiscreteCubeMerger(i, j);
         }
      }

      if (list1.getDouble(i) < list2.getDouble(0) - 1.0E-7) {
         return new NonOverlappingMerger(list1, list2, false);
      } else if (list2.getDouble(j) < list1.getDouble(0) - 1.0E-7) {
         return new NonOverlappingMerger(list2, list1, true);
      } else {
         return (IndexMerger)(i == j && Objects.equals(list1, list2)
            ? new IdenticalMerger(list1)
            : new IndirectMerger(list1, list2, excludeUpper, excludeLower));
      }
   }

   public static VoxelShape rotate(VoxelShape shape, OctahedralGroup octohedralGroup) {
      return rotate(shape, octohedralGroup, BLOCK_CENTER);
   }

   public static VoxelShape rotate(VoxelShape shape, OctahedralGroup octohedralGroup, Vec3 pos) {
      if (octohedralGroup == OctahedralGroup.IDENTITY) {
         return shape;
      } else {
         DiscreteVoxelShape discreteVoxelShape = shape.shape.rotate(octohedralGroup);
         if (shape instanceof CubeVoxelShape && BLOCK_CENTER.equals(pos)) {
            return new CubeVoxelShape(discreteVoxelShape);
         } else {
            Direction.Axis axis = octohedralGroup.permutation().permuteAxis(Direction.Axis.X);
            Direction.Axis axis2 = octohedralGroup.permutation().permuteAxis(Direction.Axis.Y);
            Direction.Axis axis3 = octohedralGroup.permutation().permuteAxis(Direction.Axis.Z);
            DoubleList doubleList = shape.getCoords(axis);
            DoubleList doubleList2 = shape.getCoords(axis2);
            DoubleList doubleList3 = shape.getCoords(axis3);
            boolean bl = octohedralGroup.inverts(Direction.Axis.X);
            boolean bl2 = octohedralGroup.inverts(Direction.Axis.Y);
            boolean bl3 = octohedralGroup.inverts(Direction.Axis.Z);
            return new ArrayVoxelShape(
               discreteVoxelShape,
               flipAxisIfNeeded(doubleList, bl, pos.get(axis), pos.x),
               flipAxisIfNeeded(doubleList2, bl2, pos.get(axis2), pos.y),
               flipAxisIfNeeded(doubleList3, bl3, pos.get(axis3), pos.z)
            );
         }
      }
   }

   @VisibleForTesting
   static DoubleList flipAxisIfNeeded(DoubleList coords, boolean flip, double originalOrigin, double newOrigin) {
      if (!flip && originalOrigin == newOrigin) {
         return coords;
      } else {
         int i = coords.size();
         DoubleList doubleList = new DoubleArrayList(i);
         if (flip) {
            for (int j = i - 1; j >= 0; j--) {
               doubleList.add(-(coords.getDouble(j) - originalOrigin) + newOrigin);
            }
         } else {
            for (int j = 0; j >= 0 && j < i; j++) {
               doubleList.add(coords.getDouble(j) - originalOrigin + newOrigin);
            }
         }

         return doubleList;
      }
   }

   public static boolean equal(VoxelShape first, VoxelShape second) {
      return !joinIsNotEmpty(first, second, BooleanOp.NOT_SAME);
   }

   public static Map<Direction.Axis, VoxelShape> rotateHorizontalAxis(VoxelShape shape) {
      return rotateHorizontalAxis(shape, BLOCK_CENTER);
   }

   public static Map<Direction.Axis, VoxelShape> rotateHorizontalAxis(VoxelShape shape, Vec3 pos) {
      return Maps.newEnumMap(Map.of(Direction.Axis.Z, shape, Direction.Axis.X, rotate(shape, OctahedralGroup.BLOCK_ROT_Y_90, pos)));
   }

   public static Map<Direction.Axis, VoxelShape> rotateAllAxis(VoxelShape shape) {
      return rotateAllAxis(shape, BLOCK_CENTER);
   }

   public static Map<Direction.Axis, VoxelShape> rotateAllAxis(VoxelShape shape, Vec3 pos) {
      return Maps.newEnumMap(
         Map.of(
            Direction.Axis.Z,
            shape,
            Direction.Axis.X,
            rotate(shape, OctahedralGroup.BLOCK_ROT_Y_90, pos),
            Direction.Axis.Y,
            rotate(shape, OctahedralGroup.BLOCK_ROT_X_90, pos)
         )
      );
   }

   public static Map<Direction, VoxelShape> rotateHorizontal(VoxelShape shape) {
      return rotateHorizontal(shape, OctahedralGroup.IDENTITY, BLOCK_CENTER);
   }

   public static Map<Direction, VoxelShape> rotateHorizontal(VoxelShape shape, OctahedralGroup rotation) {
      return rotateHorizontal(shape, rotation, BLOCK_CENTER);
   }

   public static Map<Direction, VoxelShape> rotateHorizontal(VoxelShape shape, OctahedralGroup rotation, Vec3 origin) {
      return Maps.newEnumMap(
         Map.of(
            Direction.NORTH,
            rotate(shape, rotation),
            Direction.EAST,
            rotate(shape, OctahedralGroup.BLOCK_ROT_Y_90.compose(rotation), origin),
            Direction.SOUTH,
            rotate(shape, OctahedralGroup.BLOCK_ROT_Y_180.compose(rotation), origin),
            Direction.WEST,
            rotate(shape, OctahedralGroup.BLOCK_ROT_Y_270.compose(rotation), origin)
         )
      );
   }

   public static Map<Direction, VoxelShape> rotateAll(VoxelShape shape) {
      return rotateAll(shape, OctahedralGroup.IDENTITY, BLOCK_CENTER);
   }

   public static Map<Direction, VoxelShape> rotateAll(VoxelShape shape, Vec3 pos) {
      return rotateAll(shape, OctahedralGroup.IDENTITY, pos);
   }

   public static Map<Direction, VoxelShape> rotateAll(VoxelShape shape, OctahedralGroup rotation, Vec3 origin) {
      return Maps.newEnumMap(
         Map.of(
            Direction.NORTH,
            rotate(shape, rotation),
            Direction.EAST,
            rotate(shape, OctahedralGroup.BLOCK_ROT_Y_90.compose(rotation), origin),
            Direction.SOUTH,
            rotate(shape, OctahedralGroup.BLOCK_ROT_Y_180.compose(rotation), origin),
            Direction.WEST,
            rotate(shape, OctahedralGroup.BLOCK_ROT_Y_270.compose(rotation), origin),
            Direction.UP,
            rotate(shape, OctahedralGroup.BLOCK_ROT_X_270.compose(rotation), origin),
            Direction.DOWN,
            rotate(shape, OctahedralGroup.BLOCK_ROT_X_90.compose(rotation), origin)
         )
      );
   }

   public static Map<AttachFace, Map<Direction, VoxelShape>> rotateAttachFace(VoxelShape shape) {
      return rotateAttachFace(shape, OctahedralGroup.IDENTITY);
   }

   public static Map<AttachFace, Map<Direction, VoxelShape>> rotateAttachFace(VoxelShape shape, OctahedralGroup rortation) {
      return Map.of(
         AttachFace.WALL,
         rotateHorizontal(shape, rortation),
         AttachFace.FLOOR,
         rotateHorizontal(shape, OctahedralGroup.BLOCK_ROT_X_270.compose(rortation)),
         AttachFace.CEILING,
         rotateHorizontal(shape, OctahedralGroup.BLOCK_ROT_Y_180.compose(OctahedralGroup.BLOCK_ROT_X_90).compose(rortation))
      );
   }

   public interface DoubleLineConsumer {
      void consume(double d, double e, double f, double g, double h, double i);
   }
}
