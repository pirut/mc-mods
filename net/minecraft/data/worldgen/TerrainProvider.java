package net.minecraft.data.worldgen;

import net.minecraft.util.BoundedFloatFunction;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.NoiseRouterData;

public class TerrainProvider {
   private static final float DEEP_OCEAN_CONTINENTALNESS = -0.51F;
   private static final float OCEAN_CONTINENTALNESS = -0.4F;
   private static final float PLAINS_CONTINENTALNESS = 0.1F;
   private static final float BEACH_CONTINENTALNESS = -0.15F;
   private static final BoundedFloatFunction<Float> NO_TRANSFORM = BoundedFloatFunction.IDENTITY;
   private static final BoundedFloatFunction<Float> AMPLIFIED_OFFSET = BoundedFloatFunction.createUnlimited(f -> f < 0.0F ? f : f * 2.0F);
   private static final BoundedFloatFunction<Float> AMPLIFIED_FACTOR = BoundedFloatFunction.createUnlimited(f -> 1.25F - 6.25F / (f + 5.0F));
   private static final BoundedFloatFunction<Float> AMPLIFIED_JAGGEDNESS = BoundedFloatFunction.createUnlimited(f -> f * 2.0F);

   public static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> overworldOffset(I continents, I erosion, I ridgesFolded, boolean amplified) {
      BoundedFloatFunction<Float> boundedFloatFunction = amplified ? AMPLIFIED_OFFSET : NO_TRANSFORM;
      CubicSpline<C, I> cubicSpline = buildErosionOffsetSpline(
         erosion, ridgesFolded, -0.15F, 0.0F, 0.0F, 0.1F, 0.0F, -0.03F, false, false, boundedFloatFunction
      );
      CubicSpline<C, I> cubicSpline2 = buildErosionOffsetSpline(
         erosion, ridgesFolded, -0.1F, 0.03F, 0.1F, 0.1F, 0.01F, -0.03F, false, false, boundedFloatFunction
      );
      CubicSpline<C, I> cubicSpline3 = buildErosionOffsetSpline(
         erosion, ridgesFolded, -0.1F, 0.03F, 0.1F, 0.7F, 0.01F, -0.03F, true, true, boundedFloatFunction
      );
      CubicSpline<C, I> cubicSpline4 = buildErosionOffsetSpline(
         erosion, ridgesFolded, -0.05F, 0.03F, 0.1F, 1.0F, 0.01F, 0.01F, true, true, boundedFloatFunction
      );
      return CubicSpline.<C, I>builder(continents, boundedFloatFunction)
         .addPoint(-1.1F, 0.044F)
         .addPoint(-1.02F, -0.2222F)
         .addPoint(-0.51F, -0.2222F)
         .addPoint(-0.44F, -0.12F)
         .addPoint(-0.18F, -0.12F)
         .addPoint(-0.16F, cubicSpline)
         .addPoint(-0.15F, cubicSpline)
         .addPoint(-0.1F, cubicSpline2)
         .addPoint(0.25F, cubicSpline3)
         .addPoint(1.0F, cubicSpline4)
         .build();
   }

   public static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> overworldFactor(I continents, I erosion, I ridges, I ridgesFolded, boolean amplified) {
      BoundedFloatFunction<Float> boundedFloatFunction = amplified ? AMPLIFIED_FACTOR : NO_TRANSFORM;
      return CubicSpline.<C, I>builder(continents, NO_TRANSFORM)
         .addPoint(-0.19F, 3.95F)
         .addPoint(-0.15F, getErosionFactor(erosion, ridges, ridgesFolded, 6.25F, true, NO_TRANSFORM))
         .addPoint(-0.1F, getErosionFactor(erosion, ridges, ridgesFolded, 5.47F, true, boundedFloatFunction))
         .addPoint(0.03F, getErosionFactor(erosion, ridges, ridgesFolded, 5.08F, true, boundedFloatFunction))
         .addPoint(0.06F, getErosionFactor(erosion, ridges, ridgesFolded, 4.69F, false, boundedFloatFunction))
         .build();
   }

   public static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> overworldJaggedness(
      I continents, I erosion, I ridges, I ridgesFolded, boolean amplified
   ) {
      BoundedFloatFunction<Float> boundedFloatFunction = amplified ? AMPLIFIED_JAGGEDNESS : NO_TRANSFORM;
      float f = 0.65F;
      return CubicSpline.<C, I>builder(continents, boundedFloatFunction)
         .addPoint(-0.11F, 0.0F)
         .addPoint(0.03F, buildErosionJaggednessSpline(erosion, ridges, ridgesFolded, 1.0F, 0.5F, 0.0F, 0.0F, boundedFloatFunction))
         .addPoint(0.65F, buildErosionJaggednessSpline(erosion, ridges, ridgesFolded, 1.0F, 1.0F, 1.0F, 0.0F, boundedFloatFunction))
         .build();
   }

   private static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> buildErosionJaggednessSpline(
      I erosion,
      I ridges,
      I ridgesFolded,
      float highErosionHighWeirdness,
      float lowErosionHighWeirdness,
      float highErosionMidWeirdness,
      float lowErosionMidWeirdness,
      BoundedFloatFunction<Float> transform
   ) {
      float f = -0.5775F;
      CubicSpline<C, I> cubicSpline = buildRidgeJaggednessSpline(ridges, ridgesFolded, highErosionHighWeirdness, highErosionMidWeirdness, transform);
      CubicSpline<C, I> cubicSpline2 = buildRidgeJaggednessSpline(ridges, ridgesFolded, lowErosionHighWeirdness, lowErosionMidWeirdness, transform);
      return CubicSpline.<C, I>builder(erosion, transform)
         .addPoint(-1.0F, cubicSpline)
         .addPoint(-0.78F, cubicSpline2)
         .addPoint(-0.5775F, cubicSpline2)
         .addPoint(-0.375F, 0.0F)
         .build();
   }

   private static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> buildRidgeJaggednessSpline(
      I ridges, I ridgesFolded, float highWeirdnessMagnitude, float midWeirdnessMagnitude, BoundedFloatFunction<Float> transform
   ) {
      float f = NoiseRouterData.peaksAndValleys(0.4F);
      float g = NoiseRouterData.peaksAndValleys(0.56666666F);
      float h = (f + g) / 2.0F;
      CubicSpline.Builder<C, I> builder = CubicSpline.builder(ridgesFolded, transform);
      builder.addPoint(f, 0.0F);
      if (midWeirdnessMagnitude > 0.0F) {
         builder.addPoint(h, buildWeirdnessJaggednessSpline(ridges, midWeirdnessMagnitude, transform));
      } else {
         builder.addPoint(h, 0.0F);
      }

      if (highWeirdnessMagnitude > 0.0F) {
         builder.addPoint(1.0F, buildWeirdnessJaggednessSpline(ridges, highWeirdnessMagnitude, transform));
      } else {
         builder.addPoint(1.0F, 0.0F);
      }

      return builder.build();
   }

   private static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> buildWeirdnessJaggednessSpline(
      I ridges, float magnitude, BoundedFloatFunction<Float> transform
   ) {
      float f = 0.63F * magnitude;
      float g = 0.3F * magnitude;
      return CubicSpline.<C, I>builder(ridges, transform).addPoint(-0.01F, f).addPoint(0.01F, g).build();
   }

   private static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> getErosionFactor(
      I erosion, I ridges, I ridgesFolded, float value, boolean higherValues, BoundedFloatFunction<Float> transform
   ) {
      CubicSpline<C, I> cubicSpline = CubicSpline.<C, I>builder(ridges, transform).addPoint(-0.2F, 6.3F).addPoint(0.2F, value).build();
      CubicSpline.Builder<C, I> builder = CubicSpline.<C, I>builder(erosion, transform)
         .addPoint(-0.6F, cubicSpline)
         .addPoint(-0.5F, CubicSpline.<C, I>builder(ridges, transform).addPoint(-0.05F, 6.3F).addPoint(0.05F, 2.67F).build())
         .addPoint(-0.35F, cubicSpline)
         .addPoint(-0.25F, cubicSpline)
         .addPoint(-0.1F, CubicSpline.<C, I>builder(ridges, transform).addPoint(-0.05F, 2.67F).addPoint(0.05F, 6.3F).build())
         .addPoint(0.03F, cubicSpline);
      if (higherValues) {
         CubicSpline<C, I> cubicSpline2 = CubicSpline.<C, I>builder(ridges, transform).addPoint(0.0F, value).addPoint(0.1F, 0.625F).build();
         CubicSpline<C, I> cubicSpline3 = CubicSpline.<C, I>builder(ridgesFolded, transform).addPoint(-0.9F, value).addPoint(-0.69F, cubicSpline2).build();
         builder.addPoint(0.35F, value).addPoint(0.45F, cubicSpline3).addPoint(0.55F, cubicSpline3).addPoint(0.62F, value);
      } else {
         CubicSpline<C, I> cubicSpline2 = CubicSpline.<C, I>builder(ridgesFolded, transform).addPoint(-0.7F, cubicSpline).addPoint(-0.15F, 1.37F).build();
         CubicSpline<C, I> cubicSpline3 = CubicSpline.<C, I>builder(ridgesFolded, transform).addPoint(0.45F, cubicSpline).addPoint(0.7F, 1.56F).build();
         builder.addPoint(0.05F, cubicSpline3).addPoint(0.4F, cubicSpline3).addPoint(0.45F, cubicSpline2).addPoint(0.55F, cubicSpline2).addPoint(0.58F, value);
      }

      return builder.build();
   }

   private static float calculateSlope(float y1, float y2, float x1, float x2) {
      return (y2 - y1) / (x2 - x1);
   }

   private static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> buildMountainRidgeSplineWithPoints(
      I ridgesFolded, float magnitude, boolean useMaxSlope, BoundedFloatFunction<Float> transform
   ) {
      CubicSpline.Builder<C, I> builder = CubicSpline.builder(ridgesFolded, transform);
      float f = -0.7F;
      float g = -1.0F;
      float h = mountainContinentalness(-1.0F, magnitude, -0.7F);
      float i = 1.0F;
      float j = mountainContinentalness(1.0F, magnitude, -0.7F);
      float k = calculateMountainRidgeZeroContinentalnessPoint(magnitude);
      float l = -0.65F;
      if (-0.65F < k && k < 1.0F) {
         float m = mountainContinentalness(-0.65F, magnitude, -0.7F);
         float n = -0.75F;
         float o = mountainContinentalness(-0.75F, magnitude, -0.7F);
         float p = calculateSlope(h, o, -1.0F, -0.75F);
         builder.addPoint(-1.0F, h, p);
         builder.addPoint(-0.75F, o);
         builder.addPoint(-0.65F, m);
         float q = mountainContinentalness(k, magnitude, -0.7F);
         float r = calculateSlope(q, j, k, 1.0F);
         float s = 0.01F;
         builder.addPoint(k - 0.01F, q);
         builder.addPoint(k, q, r);
         builder.addPoint(1.0F, j, r);
      } else {
         float m = calculateSlope(h, j, -1.0F, 1.0F);
         if (useMaxSlope) {
            builder.addPoint(-1.0F, Math.max(0.2F, h));
            builder.addPoint(0.0F, Mth.lerp(0.5F, h, j), m);
         } else {
            builder.addPoint(-1.0F, h, m);
         }

         builder.addPoint(1.0F, j, m);
      }

      return builder.build();
   }

   private static float mountainContinentalness(float heightFactor, float magnitude, float cutoffHeight) {
      float f = 1.17F;
      float g = 0.46082947F;
      float h = 1.0F - (1.0F - magnitude) * 0.5F;
      float i = 0.5F * (1.0F - magnitude);
      float j = (heightFactor + 1.17F) * 0.46082947F;
      float k = j * h - i;
      return heightFactor < cutoffHeight ? Math.max(k, -0.2222F) : Math.max(k, 0.0F);
   }

   private static float calculateMountainRidgeZeroContinentalnessPoint(float input) {
      float f = 1.17F;
      float g = 0.46082947F;
      float h = 1.0F - (1.0F - input) * 0.5F;
      float i = 0.5F * (1.0F - input);
      return i / (0.46082947F * h) - 1.17F;
   }

   public static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> buildErosionOffsetSpline(
      I erosion,
      I ridgesFolded,
      float ridgeBaseOffset,
      float ridgeMidOffset,
      float ridgePeakOffset,
      float magnitude,
      float ridgeInnerOffset,
      float ridgeOuterOffset,
      boolean extended,
      boolean useMaxSlope,
      BoundedFloatFunction<Float> transform
   ) {
      float f = 0.6F;
      float g = 0.5F;
      float h = 0.5F;
      CubicSpline<C, I> cubicSpline = buildMountainRidgeSplineWithPoints(ridgesFolded, Mth.lerp(magnitude, 0.6F, 1.5F), useMaxSlope, transform);
      CubicSpline<C, I> cubicSpline2 = buildMountainRidgeSplineWithPoints(ridgesFolded, Mth.lerp(magnitude, 0.6F, 1.0F), useMaxSlope, transform);
      CubicSpline<C, I> cubicSpline3 = buildMountainRidgeSplineWithPoints(ridgesFolded, magnitude, useMaxSlope, transform);
      CubicSpline<C, I> cubicSpline4 = ridgeSpline(
         ridgesFolded, ridgeBaseOffset - 0.15F, 0.5F * magnitude, Mth.lerp(0.5F, 0.5F, 0.5F) * magnitude, 0.5F * magnitude, 0.6F * magnitude, 0.5F, transform
      );
      CubicSpline<C, I> cubicSpline5 = ridgeSpline(
         ridgesFolded, ridgeBaseOffset, ridgeInnerOffset * magnitude, ridgeMidOffset * magnitude, 0.5F * magnitude, 0.6F * magnitude, 0.5F, transform
      );
      CubicSpline<C, I> cubicSpline6 = ridgeSpline(
         ridgesFolded, ridgeBaseOffset, ridgeInnerOffset, ridgeInnerOffset, ridgeMidOffset, ridgePeakOffset, 0.5F, transform
      );
      CubicSpline<C, I> cubicSpline7 = ridgeSpline(
         ridgesFolded, ridgeBaseOffset, ridgeInnerOffset, ridgeInnerOffset, ridgeMidOffset, ridgePeakOffset, 0.5F, transform
      );
      CubicSpline<C, I> cubicSpline8 = CubicSpline.<C, I>builder(ridgesFolded, transform)
         .addPoint(-1.0F, ridgeBaseOffset)
         .addPoint(-0.4F, cubicSpline6)
         .addPoint(0.0F, ridgePeakOffset + 0.07F)
         .build();
      CubicSpline<C, I> cubicSpline9 = ridgeSpline(ridgesFolded, -0.02F, ridgeOuterOffset, ridgeOuterOffset, ridgeMidOffset, ridgePeakOffset, 0.0F, transform);
      CubicSpline.Builder<C, I> builder = CubicSpline.<C, I>builder(erosion, transform)
         .addPoint(-0.85F, cubicSpline)
         .addPoint(-0.7F, cubicSpline2)
         .addPoint(-0.4F, cubicSpline3)
         .addPoint(-0.35F, cubicSpline4)
         .addPoint(-0.1F, cubicSpline5)
         .addPoint(0.2F, cubicSpline6);
      if (extended) {
         builder.addPoint(0.4F, cubicSpline7).addPoint(0.45F, cubicSpline8).addPoint(0.55F, cubicSpline8).addPoint(0.58F, cubicSpline7);
      }

      builder.addPoint(0.7F, cubicSpline9);
      return builder.build();
   }

   private static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> ridgeSpline(
      I ridgesFolded, float y1, float y2, float y3, float y4, float y5, float minSmoothing, BoundedFloatFunction<Float> transform
   ) {
      float f = Math.max(0.5F * (y2 - y1), minSmoothing);
      float g = 5.0F * (y3 - y2);
      return CubicSpline.<C, I>builder(ridgesFolded, transform)
         .addPoint(-1.0F, y1, f)
         .addPoint(-0.4F, y2, Math.min(f, g))
         .addPoint(0.0F, y3, g)
         .addPoint(0.4F, y4, 2.0F * (y4 - y3))
         .addPoint(1.0F, y5, 0.7F * (y5 - y4))
         .build();
   }
}
