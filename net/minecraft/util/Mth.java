package net.minecraft.util;

import java.util.Locale;
import java.util.UUID;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.math.Fraction;
import org.apache.commons.lang3.math.NumberUtils;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Mth {
   private static final long UUID_VERSION = 61440L;
   private static final long UUID_VERSION_TYPE_4 = 16384L;
   private static final long UUID_VARIANT = -4611686018427387904L;
   private static final long UUID_VARIANT_2 = Long.MIN_VALUE;
   public static final float PI = (float) Math.PI;
   public static final float HALF_PI = (float) (Math.PI / 2);
   public static final float TWO_PI = (float) (Math.PI * 2);
   public static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
   public static final float RAD_TO_DEG = 180.0F / (float)Math.PI;
   public static final float EPSILON = 1.0E-5F;
   public static final float SQRT_OF_TWO = sqrt(2.0F);
   public static final Vector3f Y_AXIS = new Vector3f(0.0F, 1.0F, 0.0F);
   public static final Vector3f X_AXIS = new Vector3f(1.0F, 0.0F, 0.0F);
   public static final Vector3f Z_AXIS = new Vector3f(0.0F, 0.0F, 1.0F);
   private static final int SIN_QUANTIZATION = 65536;
   private static final int SIN_MASK = 65535;
   private static final int COS_OFFSET = 16384;
   private static final double SIN_SCALE = 10430.378350470453;
   private static final float[] SIN = Util.make(new float[65536], fs -> {
      for (int ix = 0; ix < fs.length; ix++) {
         fs[ix] = (float)Math.sin(ix / 10430.378350470453);
      }
   });
   private static final RandomSource RANDOM = RandomSource.createThreadSafe();
   private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[]{
      0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9
   };
   private static final double ONE_SIXTH = 0.16666666666666666;
   private static final int FRAC_EXP = 8;
   private static final int LUT_SIZE = 257;
   private static final double FRAC_BIAS = Double.longBitsToDouble(4805340802404319232L);
   private static final double[] ASIN_TAB = new double[257];
   private static final double[] COS_TAB = new double[257];

   public static float sin(double value) {
      return SIN[(int)((long)(value * 10430.378350470453) & 65535L)];
   }

   public static float cos(double value) {
      return SIN[(int)((long)(value * 10430.378350470453 + 16384.0) & 65535L)];
   }

   public static float sqrt(float value) {
      return (float)Math.sqrt(value);
   }

   public static int floor(float value) {
      int i = (int)value;
      return value < i ? i - 1 : i;
   }

   public static int floor(double value) {
      int i = (int)value;
      return value < i ? i - 1 : i;
   }

   public static long lfloor(double value) {
      long l = (long)value;
      return value < l ? l - 1L : l;
   }

   public static float abs(float value) {
      return Math.abs(value);
   }

   public static int abs(int value) {
      return Math.abs(value);
   }

   public static int ceil(float value) {
      int i = (int)value;
      return value > i ? i + 1 : i;
   }

   public static int ceil(double value) {
      int i = (int)value;
      return value > i ? i + 1 : i;
   }

   public static long ceilLong(double value) {
      long l = (long)value;
      return value > l ? l + 1L : l;
   }

   public static int clamp(int value, int min, int max) {
      return Math.min(Math.max(value, min), max);
   }

   public static long clamp(long value, long min, long max) {
      return Math.min(Math.max(value, min), max);
   }

   public static float clamp(float value, float min, float max) {
      return value < min ? min : Math.min(value, max);
   }

   public static double clamp(double value, double min, double max) {
      return value < min ? min : Math.min(value, max);
   }

   public static double clampedLerp(double start, double end, double delta) {
      if (start < 0.0) {
         return end;
      } else {
         return start > 1.0 ? delta : lerp(start, end, delta);
      }
   }

   public static float clampedLerp(float start, float end, float delta) {
      if (start < 0.0F) {
         return end;
      } else {
         return start > 1.0F ? delta : lerp(start, end, delta);
      }
   }

   public static int absMax(int value1, int value2) {
      return Math.max(Math.abs(value1), Math.abs(value2));
   }

   public static float absMax(float value1, float value2) {
      return Math.max(Math.abs(value1), Math.abs(value2));
   }

   public static double absMax(double x, double y) {
      return Math.max(Math.abs(x), Math.abs(y));
   }

   public static int chessboardDistance(int x1, int y1, int x2, int y2) {
      return absMax(x2 - x1, y2 - y1);
   }

   public static int floorDiv(int dividend, int divisor) {
      return Math.floorDiv(dividend, divisor);
   }

   public static int nextInt(RandomSource random, int minimum, int maximum) {
      return minimum >= maximum ? minimum : random.nextInt(maximum - minimum + 1) + minimum;
   }

   public static float nextFloat(RandomSource random, float minimum, float maximum) {
      return minimum >= maximum ? minimum : random.nextFloat() * (maximum - minimum) + minimum;
   }

   public static double nextDouble(RandomSource random, double minimum, double maximum) {
      return minimum >= maximum ? minimum : random.nextDouble() * (maximum - minimum) + minimum;
   }

   public static boolean equal(float x, float y) {
      return Math.abs(y - x) < 1.0E-5F;
   }

   public static boolean equal(double x, double y) {
      return Math.abs(y - x) < 1.0E-5F;
   }

   public static int positiveModulo(int x, int y) {
      return Math.floorMod(x, y);
   }

   public static float positiveModulo(float numerator, float denominator) {
      return (numerator % denominator + denominator) % denominator;
   }

   public static double positiveModulo(double numerator, double denominator) {
      return (numerator % denominator + denominator) % denominator;
   }

   public static boolean isMultipleOf(int number, int multiple) {
      return number % multiple == 0;
   }

   public static byte packDegrees(float degrees) {
      return (byte)floor(degrees * 256.0F / 360.0F);
   }

   public static float unpackDegrees(byte degrees) {
      return degrees * 360 / 256.0F;
   }

   public static int wrapDegrees(int angle) {
      int i = angle % 360;
      if (i >= 180) {
         i -= 360;
      }

      if (i < -180) {
         i += 360;
      }

      return i;
   }

   public static float wrapDegrees(long angle) {
      float f = (float)(angle % 360L);
      if (f >= 180.0F) {
         f -= 360.0F;
      }

      if (f < -180.0F) {
         f += 360.0F;
      }

      return f;
   }

   public static float wrapDegrees(float value) {
      float f = value % 360.0F;
      if (f >= 180.0F) {
         f -= 360.0F;
      }

      if (f < -180.0F) {
         f += 360.0F;
      }

      return f;
   }

   public static double wrapDegrees(double value) {
      double d = value % 360.0;
      if (d >= 180.0) {
         d -= 360.0;
      }

      if (d < -180.0) {
         d += 360.0;
      }

      return d;
   }

   public static float degreesDifference(float start, float end) {
      return wrapDegrees(end - start);
   }

   public static float degreesDifferenceAbs(float start, float end) {
      return abs(degreesDifference(start, end));
   }

   public static float rotateIfNecessary(float rotationToAdjust, float actualRotation, float maxDifference) {
      float f = degreesDifference(rotationToAdjust, actualRotation);
      float g = clamp(f, -maxDifference, maxDifference);
      return actualRotation - g;
   }

   public static float approach(float value, float limit, float stepSize) {
      stepSize = abs(stepSize);
      return value < limit ? clamp(value + stepSize, value, limit) : clamp(value - stepSize, limit, value);
   }

   public static float approachDegrees(float angle, float limit, float stepSize) {
      float f = degreesDifference(angle, limit);
      return approach(angle, angle + f, stepSize);
   }

   public static int getInt(String value, int defaultValue) {
      return NumberUtils.toInt(value, defaultValue);
   }

   public static int smallestEncompassingPowerOfTwo(int value) {
      int i = value - 1;
      i |= i >> 1;
      i |= i >> 2;
      i |= i >> 4;
      i |= i >> 8;
      i |= i >> 16;
      return i + 1;
   }

   public static int smallestSquareSide(int itemCount) {
      if (itemCount < 0) {
         throw new IllegalArgumentException("itemCount must be greater than or equal to zero");
      } else {
         return ceil(Math.sqrt(itemCount));
      }
   }

   public static boolean isPowerOfTwo(int value) {
      return value != 0 && (value & value - 1) == 0;
   }

   public static int ceillog2(int value) {
      value = isPowerOfTwo(value) ? value : smallestEncompassingPowerOfTwo(value);
      return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int)(value * 125613361L >> 27) & 31];
   }

   public static int log2(int value) {
      return ceillog2(value) - (isPowerOfTwo(value) ? 0 : 1);
   }

   public static float frac(float number) {
      return number - floor(number);
   }

   public static double frac(double number) {
      return number - lfloor(number);
   }

   @Deprecated
   public static long getSeed(Vec3i pos) {
      return getSeed(pos.getX(), pos.getY(), pos.getZ());
   }

   @Deprecated
   public static long getSeed(int x, int y, int z) {
      long l = x * 3129871 ^ z * 116129781L ^ y;
      l = l * l * 42317861L + l * 11L;
      return l >> 16;
   }

   public static UUID createInsecureUUID(RandomSource random) {
      long l = random.nextLong() & -61441L | 16384L;
      long m = random.nextLong() & 4611686018427387903L | Long.MIN_VALUE;
      return new UUID(l, m);
   }

   public static UUID createInsecureUUID() {
      return createInsecureUUID(RANDOM);
   }

   public static double inverseLerp(double delta, double start, double end) {
      return (delta - start) / (end - start);
   }

   public static float inverseLerp(float delta, float start, float end) {
      return (delta - start) / (end - start);
   }

   public static boolean rayIntersectsAABB(Vec3 start, Vec3 end, AABB boundingBox) {
      double d = (boundingBox.minX + boundingBox.maxX) * 0.5;
      double e = (boundingBox.maxX - boundingBox.minX) * 0.5;
      double f = start.x - d;
      if (Math.abs(f) > e && f * end.x >= 0.0) {
         return false;
      } else {
         double g = (boundingBox.minY + boundingBox.maxY) * 0.5;
         double h = (boundingBox.maxY - boundingBox.minY) * 0.5;
         double i = start.y - g;
         if (Math.abs(i) > h && i * end.y >= 0.0) {
            return false;
         } else {
            double j = (boundingBox.minZ + boundingBox.maxZ) * 0.5;
            double k = (boundingBox.maxZ - boundingBox.minZ) * 0.5;
            double l = start.z - j;
            if (Math.abs(l) > k && l * end.z >= 0.0) {
               return false;
            } else {
               double m = Math.abs(end.x);
               double n = Math.abs(end.y);
               double o = Math.abs(end.z);
               double p = end.y * l - end.z * i;
               if (Math.abs(p) > h * o + k * n) {
                  return false;
               } else {
                  p = end.z * f - end.x * l;
                  if (Math.abs(p) > e * o + k * m) {
                     return false;
                  } else {
                     p = end.x * i - end.y * f;
                     return Math.abs(p) < e * n + h * m;
                  }
               }
            }
         }
      }
   }

   public static double atan2(double y, double x) {
      double d = x * x + y * y;
      if (Double.isNaN(d)) {
         return Double.NaN;
      } else {
         boolean bl = y < 0.0;
         if (bl) {
            y = -y;
         }

         boolean bl2 = x < 0.0;
         if (bl2) {
            x = -x;
         }

         boolean bl3 = y > x;
         if (bl3) {
            double e = x;
            x = y;
            y = e;
         }

         double e = fastInvSqrt(d);
         x *= e;
         y *= e;
         double f = FRAC_BIAS + y;
         int i = (int)Double.doubleToRawLongBits(f);
         double g = ASIN_TAB[i];
         double h = COS_TAB[i];
         double j = f - FRAC_BIAS;
         double k = y * h - x * j;
         double l = (6.0 + k * k) * k * 0.16666666666666666;
         double m = g + l;
         if (bl3) {
            m = (Math.PI / 2) - m;
         }

         if (bl2) {
            m = Math.PI - m;
         }

         if (bl) {
            m = -m;
         }

         return m;
      }
   }

   public static float invSqrt(float number) {
      return org.joml.Math.invsqrt(number);
   }

   public static double invSqrt(double number) {
      return org.joml.Math.invsqrt(number);
   }

   @Deprecated
   public static double fastInvSqrt(double number) {
      double d = 0.5 * number;
      long l = Double.doubleToRawLongBits(number);
      l = 6910469410427058090L - (l >> 1);
      number = Double.longBitsToDouble(l);
      return number * (1.5 - d * number * number);
   }

   public static float fastInvCubeRoot(float number) {
      int i = Float.floatToIntBits(number);
      i = 1419967116 - i / 3;
      float f = Float.intBitsToFloat(i);
      f = 0.6666667F * f + 1.0F / (3.0F * f * f * number);
      return 0.6666667F * f + 1.0F / (3.0F * f * f * number);
   }

   public static int hsvToRgb(float hue, float saturation, float value) {
      return hsvToArgb(hue, saturation, value, 0);
   }

   public static int hsvToArgb(float hue, float saturation, float value, int alpha) {
      int i = (int)(hue * 6.0F) % 6;
      float f = hue * 6.0F - i;
      float g = value * (1.0F - saturation);
      float h = value * (1.0F - f * saturation);
      float j = value * (1.0F - (1.0F - f) * saturation);
      float k;
      float l;
      float m;
      switch (i) {
         case 0:
            k = value;
            l = j;
            m = g;
            break;
         case 1:
            k = h;
            l = value;
            m = g;
            break;
         case 2:
            k = g;
            l = value;
            m = j;
            break;
         case 3:
            k = g;
            l = h;
            m = value;
            break;
         case 4:
            k = j;
            l = g;
            m = value;
            break;
         case 5:
            k = value;
            l = g;
            m = h;
            break;
         default:
            throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + hue + ", " + saturation + ", " + value);
      }

      return ARGB.color(alpha, clamp((int)(k * 255.0F), 0, 255), clamp((int)(l * 255.0F), 0, 255), clamp((int)(m * 255.0F), 0, 255));
   }

   public static int murmurHash3Mixer(int input) {
      input ^= input >>> 16;
      input *= -2048144789;
      input ^= input >>> 13;
      input *= -1028477387;
      return input ^ input >>> 16;
   }

   public static int binarySearch(int min, int max, IntPredicate isTargetBeforeOrAt) {
      int i = max - min;

      while (i > 0) {
         int j = i / 2;
         int k = min + j;
         if (isTargetBeforeOrAt.test(k)) {
            i = j;
         } else {
            min = k + 1;
            i -= j + 1;
         }
      }

      return min;
   }

   public static int lerpInt(float delta, int start, int end) {
      return start + floor(delta * (end - start));
   }

   public static int lerpDiscrete(float delta, int start, int end) {
      int i = end - start;
      return start + floor(delta * (i - 1)) + (delta > 0.0F ? 1 : 0);
   }

   public static float lerp(float delta, float start, float end) {
      return start + delta * (end - start);
   }

   public static Vec3 lerp(double delta, Vec3 start, Vec3 end) {
      return new Vec3(lerp(delta, start.x, end.x), lerp(delta, start.y, end.y), lerp(delta, start.z, end.z));
   }

   public static double lerp(double delta, double start, double end) {
      return start + delta * (end - start);
   }

   public static double lerp2(double delta1, double delta2, double start1, double end1, double start2, double end2) {
      return lerp(delta2, lerp(delta1, start1, end1), lerp(delta1, start2, end2));
   }

   public static double lerp3(
      double delta1,
      double delta2,
      double delta3,
      double start1,
      double end1,
      double start2,
      double end2,
      double start3,
      double end3,
      double start4,
      double end4
   ) {
      return lerp(delta3, lerp2(delta1, delta2, start1, end1, start2, end2), lerp2(delta1, delta2, start3, end3, start4, end4));
   }

   public static float catmullrom(float delta, float controlPoint1, float controlPoint2, float controlPoint3, float controlPoint4) {
      return 0.5F
         * (
            2.0F * controlPoint2
               + (controlPoint3 - controlPoint1) * delta
               + (2.0F * controlPoint1 - 5.0F * controlPoint2 + 4.0F * controlPoint3 - controlPoint4) * delta * delta
               + (3.0F * controlPoint2 - controlPoint1 - 3.0F * controlPoint3 + controlPoint4) * delta * delta * delta
         );
   }

   public static double smoothstep(double input) {
      return input * input * input * (input * (input * 6.0 - 15.0) + 10.0);
   }

   public static double smoothstepDerivative(double input) {
      return 30.0 * input * input * (input - 1.0) * (input - 1.0);
   }

   public static int sign(double x) {
      if (x == 0.0) {
         return 0;
      } else {
         return x > 0.0 ? 1 : -1;
      }
   }

   public static float rotLerp(float delta, float start, float end) {
      return start + delta * wrapDegrees(end - start);
   }

   public static double rotLerp(double delta, double start, double end) {
      return start + delta * wrapDegrees(end - start);
   }

   public static float rotLerpRad(float delta, float start, float end) {
      float f = end - start;

      while (f < (float) -Math.PI) {
         f += (float) (Math.PI * 2);
      }

      while (f >= (float) Math.PI) {
         f -= (float) (Math.PI * 2);
      }

      return start + delta * f;
   }

   public static float triangleWave(float input, float period) {
      return (Math.abs(input % period - period * 0.5F) - period * 0.25F) / (period * 0.25F);
   }

   public static float square(float value) {
      return value * value;
   }

   public static float cube(float value) {
      return value * value * value;
   }

   public static double square(double value) {
      return value * value;
   }

   public static int square(int value) {
      return value * value;
   }

   public static long square(long value) {
      return value * value;
   }

   public static double clampedMap(double input, double inputMin, double inputMax, double outputMin, double outputMax) {
      return clampedLerp(inverseLerp(input, inputMin, inputMax), outputMin, outputMax);
   }

   public static float clampedMap(float input, float inputMin, float inputMax, float outputMin, float outputMax) {
      return clampedLerp(inverseLerp(input, inputMin, inputMax), outputMin, outputMax);
   }

   public static double map(double input, double inputMin, double inputMax, double outputMin, double outputMax) {
      return lerp(inverseLerp(input, inputMin, inputMax), outputMin, outputMax);
   }

   public static float map(float input, float inputMin, float inputMax, float outputMin, float outputMax) {
      return lerp(inverseLerp(input, inputMin, inputMax), outputMin, outputMax);
   }

   public static double wobble(double input) {
      return input + (2.0 * RandomSource.create(floor(input * 3000.0)).nextDouble() - 1.0) * 1.0E-7 / 2.0;
   }

   public static int roundToward(int value, int factor) {
      return positiveCeilDiv(value, factor) * factor;
   }

   public static int positiveCeilDiv(int x, int y) {
      return -Math.floorDiv(-x, y);
   }

   public static int randomBetweenInclusive(RandomSource random, int minInclusive, int maxInclusive) {
      return random.nextInt(maxInclusive - minInclusive + 1) + minInclusive;
   }

   public static float randomBetween(RandomSource random, float minInclusive, float maxExclusive) {
      return random.nextFloat() * (maxExclusive - minInclusive) + minInclusive;
   }

   public static float normal(RandomSource random, float mean, float deviation) {
      return mean + (float)random.nextGaussian() * deviation;
   }

   public static double lengthSquared(double xDistance, double yDistance) {
      return xDistance * xDistance + yDistance * yDistance;
   }

   public static double length(double xDistance, double yDistance) {
      return Math.sqrt(lengthSquared(xDistance, yDistance));
   }

   public static float length(float xDistance, float yDistance) {
      return (float)Math.sqrt(lengthSquared(xDistance, yDistance));
   }

   public static double lengthSquared(double xDistance, double yDistance, double zDistance) {
      return xDistance * xDistance + yDistance * yDistance + zDistance * zDistance;
   }

   public static double length(double xDistance, double yDistance, double zDistance) {
      return Math.sqrt(lengthSquared(xDistance, yDistance, zDistance));
   }

   public static float lengthSquared(float xDistance, float yDistance, float zDistance) {
      return xDistance * xDistance + yDistance * yDistance + zDistance * zDistance;
   }

   public static int quantize(double value, int factor) {
      return floor(value / factor) * factor;
   }

   public static IntStream outFromOrigin(int input, int lowerBound, int upperBound) {
      return outFromOrigin(input, lowerBound, upperBound, 1);
   }

   public static IntStream outFromOrigin(int input, int lowerBound, int upperBound, int steps) {
      if (lowerBound > upperBound) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "upperBound %d expected to be > lowerBound %d", upperBound, lowerBound));
      } else if (steps < 1) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "step size expected to be >= 1, was %d", steps));
      } else {
         int i = clamp(input, lowerBound, upperBound);
         return IntStream.iterate(i, l -> {
            int m = Math.abs(i - l);
            return i - m >= lowerBound || i + m <= upperBound;
         }, m -> {
            boolean bl = m <= i;
            int n = Math.abs(i - m);
            boolean bl2 = i + n + steps <= upperBound;
            if (!bl || !bl2) {
               int o = i - n - (bl ? steps : 0);
               if (o >= lowerBound) {
                  return o;
               }
            }

            return i + n + steps;
         });
      }
   }

   public static Quaternionf rotationAroundAxis(Vector3f axis, Quaternionf cameraOrientation, Quaternionf output) {
      float f = axis.dot(cameraOrientation.x, cameraOrientation.y, cameraOrientation.z);
      return output.set(axis.x * f, axis.y * f, axis.z * f, cameraOrientation.w).normalize();
   }

   public static int mulAndTruncate(Fraction fraction, int factor) {
      return fraction.getNumerator() * factor / fraction.getDenominator();
   }

   static {
      for (int i = 0; i < 257; i++) {
         double d = i / 256.0;
         double e = Math.asin(d);
         COS_TAB[i] = Math.cos(e);
         ASIN_TAB[i] = e;
      }
   }
}
