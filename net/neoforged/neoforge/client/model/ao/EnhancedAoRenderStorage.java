package net.neoforged.neoforge.client.model.ao;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.config.NeoForgeClientConfig;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Vector3fc;
import org.slf4j.Logger;

public class EnhancedAoRenderStorage extends ModelBlockRenderer.AmbientOcclusionRenderStorage {
   private static final boolean COMPARE_WITH_VANILLA = Boolean.getBoolean("neoforge.ao.compareWithVanilla");
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ThreadLocal<EnhancedAoRenderStorage.AoObjectCache> AO_OBJECT_CACHE = ThreadLocal.withInitial(
      () -> new EnhancedAoRenderStorage.AoObjectCache(new FullFaceCalculator(), new float[4])
   );
   private final FullFaceCalculator calculator;
   private final float[] weights;
   private BakedQuad currentQuad;
   private static final float AO_EPS = 1.0E-4F;
   private static final float AVERAGE_WEIGHT = 0.75F;
   private static final float MAX_WEIGHT = 0.25F;

   public static ModelBlockRenderer.AmbientOcclusionRenderStorage newInstance() {
      return (ModelBlockRenderer.AmbientOcclusionRenderStorage)(NeoForgeClientConfig.INSTANCE.enhancedLighting.getAsBoolean()
         ? new EnhancedAoRenderStorage()
         : new ModelBlockRenderer.AmbientOcclusionRenderStorage());
   }

   public static void applyFlatQuadBrightness(BlockAndTintGetter level, BakedQuad quad, ModelBlockRenderer.CommonRenderStorage storage) {
      if (NeoForgeClientConfig.INSTANCE.enhancedLighting.getAsBoolean()) {
         int quadNormal = -1;

         for (int vertex = 0; vertex < 4; vertex++) {
            int normal = quad.bakedNormals().normal(vertex);
            if (BakedNormals.isUnspecified(normal)) {
               if (quadNormal == -1) {
                  quadNormal = BakedNormals.computeQuadNormal(quad.position0(), quad.position1(), quad.position2(), quad.position3());
               }

               normal = quadNormal;
            }

            storage.brightness[vertex] = level.getShade(BakedNormals.unpackX(normal), BakedNormals.unpackY(normal), BakedNormals.unpackZ(normal), quad.shade());
         }
      } else {
         float f = level.getShade(quad.direction(), quad.shade());
         storage.brightness[0] = f;
         storage.brightness[1] = f;
         storage.brightness[2] = f;
         storage.brightness[3] = f;
      }
   }

   public EnhancedAoRenderStorage() {
      EnhancedAoRenderStorage.AoObjectCache cache = AO_OBJECT_CACHE.get();
      this.calculator = cache.calculator;
      this.weights = cache.weights;
      this.calculator.startBlock(this.cache);
   }

   @Override
   public void captureQuad(BakedQuad quad) {
      this.currentQuad = quad;
   }

   @Override
   public void calculate(BlockAndTintGetter level, BlockState state, BlockPos pos, Direction direction, boolean shade) {
      if (this.currentQuad == null) {
         throw new IllegalStateException("Make sure to pass the quad via captureQuad before calling calculate.");
      } else {
         boolean isAxisAligned = switch (direction) {
            case DOWN, UP -> this.faceShape[ModelBlockRenderer.SizeInfo.DOWN.index] == this.faceShape[ModelBlockRenderer.SizeInfo.UP.index];
            case NORTH, SOUTH -> this.faceShape[ModelBlockRenderer.SizeInfo.NORTH.index] == this.faceShape[ModelBlockRenderer.SizeInfo.SOUTH.index];
            case WEST, EAST -> this.faceShape[ModelBlockRenderer.SizeInfo.WEST.index] == this.faceShape[ModelBlockRenderer.SizeInfo.EAST.index];
         };
         if (isAxisAligned) {
            this.calculateAxisAligned(level, state, pos, direction, shade);
         } else {
            this.calculateIrregular(level, state, pos, shade);
         }
      }
   }

   private void calculateAxisAligned(BlockAndTintGetter level, BlockState state, BlockPos pos, Direction direction, boolean shade) {
      AoCalculatedFace fullFace = this.calculator.calculateFace(level, state, pos, direction, shade, this.faceCubic);
      AoFace aoFace = AoFace.fromDirection(direction);
      float[] weights = this.weights;

      for (int vertex = 0; vertex < 4; vertex++) {
         Vector3fc vertPos = this.currentQuad.position(vertex);
         aoFace.computeCornerWeights(weights, vertPos.x(), vertPos.y(), vertPos.z());
         this.brightness[vertex] = interpolateBrightness(fullFace, weights);
         this.lightmap[vertex] = interpolateLightmap(fullFace, weights);
      }

      if (COMPARE_WITH_VANILLA) {
         float[] emulatedBrightness = (float[])this.brightness.clone();
         int[] emulatedLightmap = (int[])this.lightmap.clone();
         super.calculate(level, state, pos, direction, shade);

         for (int vertex = 0; vertex < 4; vertex++) {
            if (!Mth.equal(emulatedBrightness[vertex], this.brightness[vertex]) || emulatedLightmap[vertex] != this.lightmap[vertex]) {
               LOGGER.warn(
                  "Emulated vanilla AO differs from actual AO at vertex {} of face {}, while lighting {}@{}\nVanilla: lightmap = {}, brightness = {}\nEmulated: lightmap = {}, brightness = {}\n",
                  new Object[]{
                     vertex,
                     direction,
                     state.getBlock(),
                     pos,
                     this.lightmap[vertex],
                     this.brightness[vertex],
                     emulatedLightmap[vertex],
                     emulatedBrightness[vertex]
                  }
               );
               break;
            }
         }

         System.arraycopy(emulatedBrightness, 0, this.brightness, 0, 4);
         System.arraycopy(emulatedLightmap, 0, this.lightmap, 0, 4);
      }
   }

   private void calculateIrregular(BlockAndTintGetter level, BlockState state, BlockPos pos, boolean shade) {
      int quadNormal = -1;

      for (int vertex = 0; vertex < 4; vertex++) {
         int normal = this.currentQuad.bakedNormals().normal(vertex);
         if (BakedNormals.isUnspecified(normal)) {
            if (quadNormal == -1) {
               quadNormal = BakedNormals.computeQuadNormal(
                  this.currentQuad.position0(), this.currentQuad.position1(), this.currentQuad.position2(), this.currentQuad.position3()
               );
            }

            normal = quadNormal;
         }

         float weightedBrightness = 0.0F;
         int weightedLightmap = 0;
         float maxBrightness = 0.0F;
         int maxLightmap = 0;

         for (int axis = 0; axis < 3; axis++) {
            float normalComponent = BakedNormals.unpackComponent(normal, axis);
            if (normalComponent != 0.0F) {
               Direction direction = switch (axis) {
                  case 0 -> normalComponent > 0.0F ? Direction.EAST : Direction.WEST;
                  case 1 -> normalComponent > 0.0F ? Direction.UP : Direction.DOWN;
                  case 2 -> normalComponent > 0.0F ? Direction.SOUTH : Direction.NORTH;
                  default -> throw new AssertionError();
               };
               AoFace aoFace = AoFace.fromDirection(direction);
               Vector3fc vertPos = this.currentQuad.position(vertex);
               float depth = aoFace.computeDepth(vertPos.x(), vertPos.y(), vertPos.z());
               boolean sampleOutside = depth < 1.0E-4F || state.isCollisionShapeFullBlock(level, pos);
               AoCalculatedFace fullFace = this.calculator.calculateFace(level, state, pos, direction, shade, sampleOutside);
               float[] weights = this.weights;
               aoFace.computeCornerWeights(weights, vertPos.x(), vertPos.y(), vertPos.z());
               float brightness = interpolateBrightness(fullFace, weights);
               int lightmap = interpolateLightmap(fullFace, weights);
               float axisWeight = normalComponent * normalComponent;
               weightedBrightness += brightness * axisWeight;
               weightedLightmap = lerpLightmap(weightedLightmap, 1.0F, lightmap, axisWeight);
               maxBrightness = Math.max(maxBrightness, brightness);
               maxLightmap = maxLightmap(maxLightmap, lightmap);
            }
         }

         this.brightness[vertex] = Math.clamp(weightedBrightness * 0.75F + maxBrightness * 0.25F, 0.0F, 1.0F);
         this.lightmap[vertex] = lerpLightmap(weightedLightmap, 0.75F, maxLightmap, 0.25F);
      }
   }

   private static float interpolateBrightness(AoCalculatedFace in, float[] weights) {
      return Math.clamp(in.brightness0 * weights[0] + in.brightness1 * weights[1] + in.brightness2 * weights[2] + in.brightness3 * weights[3], 0.0F, 1.0F);
   }

   private static int interpolateLightmap(AoCalculatedFace in, float[] weights) {
      return blend(in.lightmap0, in.lightmap1, in.lightmap2, in.lightmap3, weights[0], weights[1], weights[2], weights[3]);
   }

   private static int lerpLightmap(int lightmap1, float w1, int lightmap2, float w2) {
      int block1 = LightTexture.blockWithFraction(lightmap1);
      int block2 = LightTexture.blockWithFraction(lightmap2);
      int block = 0xFF & Math.round(block1 * w1 + block2 * w2);
      int sky1 = LightTexture.skyWithFraction(lightmap1);
      int sky2 = LightTexture.skyWithFraction(lightmap2);
      int sky = 0xFF & Math.round(sky1 * w1 + sky2 * w2);
      return LightTexture.packWithFraction(block, sky);
   }

   static int maxLightmap(int lightmap1, int lightmap2) {
      return LightTexture.packWithFraction(
         Math.max(LightTexture.blockWithFraction(lightmap1), LightTexture.blockWithFraction(lightmap2)),
         Math.max(LightTexture.skyWithFraction(lightmap1), LightTexture.skyWithFraction(lightmap2))
      );
   }

   private record AoObjectCache(FullFaceCalculator calculator, float[] weights) {
   }
}
