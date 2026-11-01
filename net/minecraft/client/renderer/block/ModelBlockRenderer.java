package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public class ModelBlockRenderer {
   private static final Direction[] DIRECTIONS = Direction.values();
   private final BlockColors blockColors;
   private static final int CACHE_SIZE = 100;
   static final ThreadLocal<ModelBlockRenderer.Cache> CACHE = ThreadLocal.withInitial(ModelBlockRenderer.Cache::new);

   public ModelBlockRenderer(BlockColors blockColors) {
      this.blockColors = blockColors;
   }

   public void tesselateBlock(
      BlockAndTintGetter level,
      List<BlockModelPart> parts,
      BlockState state,
      BlockPos pos,
      PoseStack poseStack,
      VertexConsumer consumer,
      boolean checkSides,
      int packedOverlay
   ) {
      if (!parts.isEmpty()) {
         boolean bl = Minecraft.useAmbientOcclusion() && state.getLightEmission() == 0 && parts.getFirst().useAmbientOcclusion();
         poseStack.translate(state.getOffset(pos));

         try {
            if (bl) {
               this.tesselateWithAO(level, parts, state, pos, poseStack, consumer, checkSides, packedOverlay);
            } else {
               this.tesselateWithoutAO(level, parts, state, pos, poseStack, consumer, checkSides, packedOverlay);
            }
         } catch (Throwable var13) {
            CrashReport crashReport = CrashReport.forThrowable(var13, "Tesselating block model");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Block model being tesselated");
            CrashReportCategory.populateBlockDetails(crashReportCategory, level, pos, state);
            crashReportCategory.setDetail("Using AO", bl);
            throw new ReportedException(crashReport);
         }
      }
   }

   private static boolean shouldRenderFace(BlockAndTintGetter level, BlockState state, boolean checkSides, Direction face, BlockPos pos) {
      if (!checkSides) {
         return true;
      } else {
         BlockState blockState = level.getBlockState(pos);
         return Block.shouldRenderFace(state, blockState, face);
      }
   }

   public void tesselateWithAO(
      BlockAndTintGetter level,
      List<BlockModelPart> parts,
      BlockState state,
      BlockPos pos,
      PoseStack poseStack,
      VertexConsumer consumer,
      boolean checkSides,
      int packedOverlay
   ) {
      ModelBlockRenderer.AmbientOcclusionRenderStorage ambientOcclusionRenderStorage = new ModelBlockRenderer.AmbientOcclusionRenderStorage();
      int i = 0;
      int j = 0;

      for (BlockModelPart blockModelPart : parts) {
         for (Direction direction : DIRECTIONS) {
            int k = 1 << direction.ordinal();
            boolean bl = (i & k) == 1;
            boolean bl2 = (j & k) == 1;
            if (!bl || bl2) {
               List<BakedQuad> list = blockModelPart.getQuads(direction);
               if (!list.isEmpty()) {
                  if (!bl) {
                     bl2 = shouldRenderFace(level, state, checkSides, direction, ambientOcclusionRenderStorage.scratchPos.setWithOffset(pos, direction));
                     i |= k;
                     if (bl2) {
                        j |= k;
                     }
                  }

                  if (bl2) {
                     this.renderModelFaceAO(level, state, pos, poseStack, consumer, list, ambientOcclusionRenderStorage, packedOverlay);
                  }
               }
            }
         }

         List<BakedQuad> list2 = blockModelPart.getQuads(null);
         if (!list2.isEmpty()) {
            this.renderModelFaceAO(level, state, pos, poseStack, consumer, list2, ambientOcclusionRenderStorage, packedOverlay);
         }
      }
   }

   public void tesselateWithoutAO(
      BlockAndTintGetter level,
      List<BlockModelPart> parts,
      BlockState state,
      BlockPos pos,
      PoseStack poseStack,
      VertexConsumer consumer,
      boolean checkSides,
      int packedOverlay
   ) {
      ModelBlockRenderer.CommonRenderStorage commonRenderStorage = new ModelBlockRenderer.CommonRenderStorage();
      int i = 0;
      int j = 0;

      for (BlockModelPart blockModelPart : parts) {
         for (Direction direction : DIRECTIONS) {
            int k = 1 << direction.ordinal();
            boolean bl = (i & k) == 1;
            boolean bl2 = (j & k) == 1;
            if (!bl || bl2) {
               List<BakedQuad> list = blockModelPart.getQuads(direction);
               if (!list.isEmpty()) {
                  BlockPos blockPos = commonRenderStorage.scratchPos.setWithOffset(pos, direction);
                  if (!bl) {
                     bl2 = shouldRenderFace(level, state, checkSides, direction, blockPos);
                     i |= k;
                     if (bl2) {
                        j |= k;
                     }
                  }

                  if (bl2) {
                     int l = commonRenderStorage.cache.getLightColor(state, level, blockPos);
                     this.renderModelFaceFlat(level, state, pos, l, packedOverlay, false, poseStack, consumer, list, commonRenderStorage);
                  }
               }
            }
         }

         List<BakedQuad> list2 = blockModelPart.getQuads(null);
         if (!list2.isEmpty()) {
            this.renderModelFaceFlat(level, state, pos, -1, packedOverlay, true, poseStack, consumer, list2, commonRenderStorage);
         }
      }
   }

   private void renderModelFaceAO(
      BlockAndTintGetter level,
      BlockState state,
      BlockPos pos,
      PoseStack poseStack,
      VertexConsumer consumer,
      List<BakedQuad> quads,
      ModelBlockRenderer.AmbientOcclusionRenderStorage renderStorage,
      int packedOverlay
   ) {
      for (BakedQuad bakedQuad : quads) {
         calculateShape(level, state, pos, bakedQuad, renderStorage);
         renderStorage.calculate(level, state, pos, bakedQuad.direction(), bakedQuad.shade());
         this.putQuadData(level, state, pos, consumer, poseStack.last(), bakedQuad, renderStorage, packedOverlay);
      }
   }

   private void putQuadData(
      BlockAndTintGetter level,
      BlockState state,
      BlockPos pos,
      VertexConsumer consumer,
      PoseStack.Pose pose,
      BakedQuad quad,
      ModelBlockRenderer.CommonRenderStorage renderStorage,
      int packedOverlay
   ) {
      int i = quad.tintIndex();
      float f;
      float g;
      float h;
      if (i != -1) {
         int j;
         if (renderStorage.tintCacheIndex == i) {
            j = renderStorage.tintCacheValue;
         } else {
            j = this.blockColors.getColor(state, level, pos, i);
            renderStorage.tintCacheIndex = i;
            renderStorage.tintCacheValue = j;
         }

         f = ARGB.redFloat(j);
         g = ARGB.greenFloat(j);
         h = ARGB.blueFloat(j);
      } else {
         f = 1.0F;
         g = 1.0F;
         h = 1.0F;
      }

      consumer.putBulkData(pose, quad, renderStorage.brightness, f, g, h, 1.0F, renderStorage.lightmap, packedOverlay);
   }

   private static void calculateShape(
      BlockAndTintGetter level, BlockState state, BlockPos pos, BakedQuad quad, ModelBlockRenderer.CommonRenderStorage renderStorage
   ) {
      float f = 32.0F;
      float g = 32.0F;
      float h = 32.0F;
      float i = -32.0F;
      float j = -32.0F;
      float k = -32.0F;

      for (int l = 0; l < 4; l++) {
         Vector3fc vector3fc = quad.position(l);
         float m = vector3fc.x();
         float n = vector3fc.y();
         float o = vector3fc.z();
         f = Math.min(f, m);
         g = Math.min(g, n);
         h = Math.min(h, o);
         i = Math.max(i, m);
         j = Math.max(j, n);
         k = Math.max(k, o);
      }

      if (renderStorage instanceof ModelBlockRenderer.AmbientOcclusionRenderStorage ambientOcclusionRenderStorage) {
         ambientOcclusionRenderStorage.faceShape[ModelBlockRenderer.SizeInfo.WEST.index] = f;
         ambientOcclusionRenderStorage.faceShape[ModelBlockRenderer.SizeInfo.EAST.index] = i;
         ambientOcclusionRenderStorage.faceShape[ModelBlockRenderer.SizeInfo.DOWN.index] = g;
         ambientOcclusionRenderStorage.faceShape[ModelBlockRenderer.SizeInfo.UP.index] = j;
         ambientOcclusionRenderStorage.faceShape[ModelBlockRenderer.SizeInfo.NORTH.index] = h;
         ambientOcclusionRenderStorage.faceShape[ModelBlockRenderer.SizeInfo.SOUTH.index] = k;
         ambientOcclusionRenderStorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_WEST.index] = 1.0F - f;
         ambientOcclusionRenderStorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_EAST.index] = 1.0F - i;
         ambientOcclusionRenderStorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_DOWN.index] = 1.0F - g;
         ambientOcclusionRenderStorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_UP.index] = 1.0F - j;
         ambientOcclusionRenderStorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_NORTH.index] = 1.0F - h;
         ambientOcclusionRenderStorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_SOUTH.index] = 1.0F - k;
      }

      float p = 1.0E-4F;
      float q = 0.9999F;

      renderStorage.facePartial = switch (quad.direction()) {
         case DOWN, UP -> f >= 1.0E-4F || h >= 1.0E-4F || i <= 0.9999F || k <= 0.9999F;
         case NORTH, SOUTH -> f >= 1.0E-4F || g >= 1.0E-4F || i <= 0.9999F || j <= 0.9999F;
         case WEST, EAST -> g >= 1.0E-4F || h >= 1.0E-4F || j <= 0.9999F || k <= 0.9999F;
      };

      renderStorage.faceCubic = switch (quad.direction()) {
         case DOWN -> g == j && (g < 1.0E-4F || state.isCollisionShapeFullBlock(level, pos));
         case UP -> g == j && (j > 0.9999F || state.isCollisionShapeFullBlock(level, pos));
         case NORTH -> h == k && (h < 1.0E-4F || state.isCollisionShapeFullBlock(level, pos));
         case SOUTH -> h == k && (k > 0.9999F || state.isCollisionShapeFullBlock(level, pos));
         case WEST -> f == i && (f < 1.0E-4F || state.isCollisionShapeFullBlock(level, pos));
         case EAST -> f == i && (i > 0.9999F || state.isCollisionShapeFullBlock(level, pos));
      };
   }

   private void renderModelFaceFlat(
      BlockAndTintGetter level,
      BlockState state,
      BlockPos pos,
      int packedLight,
      int packedOverlay,
      boolean repackLight,
      PoseStack poseStack,
      VertexConsumer consumer,
      List<BakedQuad> quads,
      ModelBlockRenderer.CommonRenderStorage renderStorage
   ) {
      for (BakedQuad bakedQuad : quads) {
         if (repackLight) {
            calculateShape(level, state, pos, bakedQuad, renderStorage);
            BlockPos blockPos = (BlockPos)(renderStorage.faceCubic ? renderStorage.scratchPos.setWithOffset(pos, bakedQuad.direction()) : pos);
            packedLight = renderStorage.cache.getLightColor(state, level, blockPos);
         }

         float f = level.getShade(bakedQuad.direction(), bakedQuad.shade());
         renderStorage.brightness[0] = f;
         renderStorage.brightness[1] = f;
         renderStorage.brightness[2] = f;
         renderStorage.brightness[3] = f;
         renderStorage.lightmap[0] = packedLight;
         renderStorage.lightmap[1] = packedLight;
         renderStorage.lightmap[2] = packedLight;
         renderStorage.lightmap[3] = packedLight;
         this.putQuadData(level, state, pos, consumer, poseStack.last(), bakedQuad, renderStorage, packedOverlay);
      }
   }

   public static void renderModel(
      PoseStack.Pose pose, VertexConsumer consumer, BlockStateModel model, float red, float green, float blue, int packedLight, int packedOverlay
   ) {
      for (BlockModelPart blockModelPart : model.collectParts(RandomSource.create(42L))) {
         for (Direction direction : DIRECTIONS) {
            renderQuadList(pose, consumer, red, green, blue, blockModelPart.getQuads(direction), packedLight, packedOverlay);
         }

         renderQuadList(pose, consumer, red, green, blue, blockModelPart.getQuads(null), packedLight, packedOverlay);
      }
   }

   private static void renderQuadList(
      PoseStack.Pose pose, VertexConsumer consumer, float red, float green, float blue, List<BakedQuad> quads, int packedLight, int packedOverlay
   ) {
      for (BakedQuad bakedQuad : quads) {
         float f;
         float g;
         float h;
         if (bakedQuad.isTinted()) {
            f = Mth.clamp(red, 0.0F, 1.0F);
            g = Mth.clamp(green, 0.0F, 1.0F);
            h = Mth.clamp(blue, 0.0F, 1.0F);
         } else {
            f = 1.0F;
            g = 1.0F;
            h = 1.0F;
         }

         consumer.putBulkData(pose, bakedQuad, f, g, h, 1.0F, packedLight, packedOverlay);
      }
   }

   public static void enableCaching() {
      CACHE.get().enable();
   }

   public static void clearCache() {
      CACHE.get().disable();
   }

   @Environment(EnvType.CLIENT)
   protected static enum AdjacencyInfo {
      DOWN(
         new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH},
         0.5F,
         true,
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.SOUTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.SOUTH
         }
      ),
      UP(
         new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH},
         1.0F,
         true,
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.SOUTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.SOUTH
         }
      ),
      NORTH(
         new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST},
         0.8F,
         true,
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_WEST
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_EAST
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_EAST
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_WEST
         }
      ),
      SOUTH(
         new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP},
         0.8F,
         true,
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.WEST
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.WEST
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.EAST
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.EAST
         }
      ),
      WEST(
         new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH},
         0.6F,
         true,
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.SOUTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.SOUTH
         }
      ),
      EAST(
         new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH},
         0.6F,
         true,
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.SOUTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.SOUTH
         }
      );

      final Direction[] corners;
      final boolean doNonCubicWeight;
      final ModelBlockRenderer.SizeInfo[] vert0Weights;
      final ModelBlockRenderer.SizeInfo[] vert1Weights;
      final ModelBlockRenderer.SizeInfo[] vert2Weights;
      final ModelBlockRenderer.SizeInfo[] vert3Weights;
      private static final ModelBlockRenderer.AdjacencyInfo[] BY_FACING = Util.make(new ModelBlockRenderer.AdjacencyInfo[6], adjacencyInfos -> {
         adjacencyInfos[Direction.DOWN.get3DDataValue()] = DOWN;
         adjacencyInfos[Direction.UP.get3DDataValue()] = UP;
         adjacencyInfos[Direction.NORTH.get3DDataValue()] = NORTH;
         adjacencyInfos[Direction.SOUTH.get3DDataValue()] = SOUTH;
         adjacencyInfos[Direction.WEST.get3DDataValue()] = WEST;
         adjacencyInfos[Direction.EAST.get3DDataValue()] = EAST;
      });

      private AdjacencyInfo(
         final Direction[] corners,
         final float shadeBrightness,
         final boolean doNonCubicWeight,
         final ModelBlockRenderer.SizeInfo[] vert0Weights,
         final ModelBlockRenderer.SizeInfo[] vert1Weights,
         final ModelBlockRenderer.SizeInfo[] vert2Weights,
         final ModelBlockRenderer.SizeInfo[] vert3Weights
      ) {
         this.corners = corners;
         this.doNonCubicWeight = doNonCubicWeight;
         this.vert0Weights = vert0Weights;
         this.vert1Weights = vert1Weights;
         this.vert2Weights = vert2Weights;
         this.vert3Weights = vert3Weights;
      }

      public static ModelBlockRenderer.AdjacencyInfo fromFacing(Direction facing) {
         return BY_FACING[facing.get3DDataValue()];
      }
   }

   @Environment(EnvType.CLIENT)
   static class AmbientOcclusionRenderStorage extends ModelBlockRenderer.CommonRenderStorage {
      final float[] faceShape = new float[ModelBlockRenderer.SizeInfo.COUNT];

      public AmbientOcclusionRenderStorage() {
      }

      public void calculate(BlockAndTintGetter level, BlockState state, BlockPos pos, Direction direction, boolean shade) {
         BlockPos blockPos = this.faceCubic ? pos.relative(direction) : pos;
         ModelBlockRenderer.AdjacencyInfo adjacencyInfo = ModelBlockRenderer.AdjacencyInfo.fromFacing(direction);
         BlockPos.MutableBlockPos mutableBlockPos = this.scratchPos;
         mutableBlockPos.setWithOffset(blockPos, adjacencyInfo.corners[0]);
         BlockState blockState = level.getBlockState(mutableBlockPos);
         int i = this.cache.getLightColor(blockState, level, mutableBlockPos);
         float f = this.cache.getShadeBrightness(blockState, level, mutableBlockPos);
         mutableBlockPos.setWithOffset(blockPos, adjacencyInfo.corners[1]);
         BlockState blockState2 = level.getBlockState(mutableBlockPos);
         int j = this.cache.getLightColor(blockState2, level, mutableBlockPos);
         float g = this.cache.getShadeBrightness(blockState2, level, mutableBlockPos);
         mutableBlockPos.setWithOffset(blockPos, adjacencyInfo.corners[2]);
         BlockState blockState3 = level.getBlockState(mutableBlockPos);
         int k = this.cache.getLightColor(blockState3, level, mutableBlockPos);
         float h = this.cache.getShadeBrightness(blockState3, level, mutableBlockPos);
         mutableBlockPos.setWithOffset(blockPos, adjacencyInfo.corners[3]);
         BlockState blockState4 = level.getBlockState(mutableBlockPos);
         int l = this.cache.getLightColor(blockState4, level, mutableBlockPos);
         float m = this.cache.getShadeBrightness(blockState4, level, mutableBlockPos);
         BlockState blockState5 = level.getBlockState(mutableBlockPos.setWithOffset(blockPos, adjacencyInfo.corners[0]).move(direction));
         boolean bl = !blockState5.isViewBlocking(level, mutableBlockPos) || blockState5.getLightBlock() == 0;
         BlockState blockState6 = level.getBlockState(mutableBlockPos.setWithOffset(blockPos, adjacencyInfo.corners[1]).move(direction));
         boolean bl2 = !blockState6.isViewBlocking(level, mutableBlockPos) || blockState6.getLightBlock() == 0;
         BlockState blockState7 = level.getBlockState(mutableBlockPos.setWithOffset(blockPos, adjacencyInfo.corners[2]).move(direction));
         boolean bl3 = !blockState7.isViewBlocking(level, mutableBlockPos) || blockState7.getLightBlock() == 0;
         BlockState blockState8 = level.getBlockState(mutableBlockPos.setWithOffset(blockPos, adjacencyInfo.corners[3]).move(direction));
         boolean bl4 = !blockState8.isViewBlocking(level, mutableBlockPos) || blockState8.getLightBlock() == 0;
         float n;
         int o;
         if (!bl3 && !bl) {
            n = f;
            o = i;
         } else {
            mutableBlockPos.setWithOffset(blockPos, adjacencyInfo.corners[0]).move(adjacencyInfo.corners[2]);
            BlockState blockState9 = level.getBlockState(mutableBlockPos);
            n = this.cache.getShadeBrightness(blockState9, level, mutableBlockPos);
            o = this.cache.getLightColor(blockState9, level, mutableBlockPos);
         }

         float p;
         int q;
         if (!bl4 && !bl) {
            p = f;
            q = i;
         } else {
            mutableBlockPos.setWithOffset(blockPos, adjacencyInfo.corners[0]).move(adjacencyInfo.corners[3]);
            BlockState blockState9 = level.getBlockState(mutableBlockPos);
            p = this.cache.getShadeBrightness(blockState9, level, mutableBlockPos);
            q = this.cache.getLightColor(blockState9, level, mutableBlockPos);
         }

         float r;
         int s;
         if (!bl3 && !bl2) {
            r = f;
            s = i;
         } else {
            mutableBlockPos.setWithOffset(blockPos, adjacencyInfo.corners[1]).move(adjacencyInfo.corners[2]);
            BlockState blockState9 = level.getBlockState(mutableBlockPos);
            r = this.cache.getShadeBrightness(blockState9, level, mutableBlockPos);
            s = this.cache.getLightColor(blockState9, level, mutableBlockPos);
         }

         float t;
         int u;
         if (!bl4 && !bl2) {
            t = f;
            u = i;
         } else {
            mutableBlockPos.setWithOffset(blockPos, adjacencyInfo.corners[1]).move(adjacencyInfo.corners[3]);
            BlockState blockState9 = level.getBlockState(mutableBlockPos);
            t = this.cache.getShadeBrightness(blockState9, level, mutableBlockPos);
            u = this.cache.getLightColor(blockState9, level, mutableBlockPos);
         }

         int v = this.cache.getLightColor(state, level, pos);
         mutableBlockPos.setWithOffset(pos, direction);
         BlockState blockState10 = level.getBlockState(mutableBlockPos);
         if (this.faceCubic || !blockState10.isSolidRender()) {
            v = this.cache.getLightColor(blockState10, level, mutableBlockPos);
         }

         float w = this.faceCubic
            ? this.cache.getShadeBrightness(level.getBlockState(blockPos), level, blockPos)
            : this.cache.getShadeBrightness(level.getBlockState(pos), level, pos);
         ModelBlockRenderer.AmbientVertexRemap ambientVertexRemap = ModelBlockRenderer.AmbientVertexRemap.fromFacing(direction);
         if (this.facePartial && adjacencyInfo.doNonCubicWeight) {
            float x = (m + f + p + w) * 0.25F;
            float y = (h + f + n + w) * 0.25F;
            float z = (h + g + r + w) * 0.25F;
            float aa = (m + g + t + w) * 0.25F;
            float ab = this.faceShape[adjacencyInfo.vert0Weights[0].index] * this.faceShape[adjacencyInfo.vert0Weights[1].index];
            float ac = this.faceShape[adjacencyInfo.vert0Weights[2].index] * this.faceShape[adjacencyInfo.vert0Weights[3].index];
            float ad = this.faceShape[adjacencyInfo.vert0Weights[4].index] * this.faceShape[adjacencyInfo.vert0Weights[5].index];
            float ae = this.faceShape[adjacencyInfo.vert0Weights[6].index] * this.faceShape[adjacencyInfo.vert0Weights[7].index];
            float af = this.faceShape[adjacencyInfo.vert1Weights[0].index] * this.faceShape[adjacencyInfo.vert1Weights[1].index];
            float ag = this.faceShape[adjacencyInfo.vert1Weights[2].index] * this.faceShape[adjacencyInfo.vert1Weights[3].index];
            float ah = this.faceShape[adjacencyInfo.vert1Weights[4].index] * this.faceShape[adjacencyInfo.vert1Weights[5].index];
            float ai = this.faceShape[adjacencyInfo.vert1Weights[6].index] * this.faceShape[adjacencyInfo.vert1Weights[7].index];
            float aj = this.faceShape[adjacencyInfo.vert2Weights[0].index] * this.faceShape[adjacencyInfo.vert2Weights[1].index];
            float ak = this.faceShape[adjacencyInfo.vert2Weights[2].index] * this.faceShape[adjacencyInfo.vert2Weights[3].index];
            float al = this.faceShape[adjacencyInfo.vert2Weights[4].index] * this.faceShape[adjacencyInfo.vert2Weights[5].index];
            float am = this.faceShape[adjacencyInfo.vert2Weights[6].index] * this.faceShape[adjacencyInfo.vert2Weights[7].index];
            float an = this.faceShape[adjacencyInfo.vert3Weights[0].index] * this.faceShape[adjacencyInfo.vert3Weights[1].index];
            float ao = this.faceShape[adjacencyInfo.vert3Weights[2].index] * this.faceShape[adjacencyInfo.vert3Weights[3].index];
            float ap = this.faceShape[adjacencyInfo.vert3Weights[4].index] * this.faceShape[adjacencyInfo.vert3Weights[5].index];
            float aq = this.faceShape[adjacencyInfo.vert3Weights[6].index] * this.faceShape[adjacencyInfo.vert3Weights[7].index];
            this.brightness[ambientVertexRemap.vert0] = Math.clamp(x * ab + y * ac + z * ad + aa * ae, 0.0F, 1.0F);
            this.brightness[ambientVertexRemap.vert1] = Math.clamp(x * af + y * ag + z * ah + aa * ai, 0.0F, 1.0F);
            this.brightness[ambientVertexRemap.vert2] = Math.clamp(x * aj + y * ak + z * al + aa * am, 0.0F, 1.0F);
            this.brightness[ambientVertexRemap.vert3] = Math.clamp(x * an + y * ao + z * ap + aa * aq, 0.0F, 1.0F);
            int ar = blend(l, i, q, v);
            int as = blend(k, i, o, v);
            int at = blend(k, j, s, v);
            int au = blend(l, j, u, v);
            this.lightmap[ambientVertexRemap.vert0] = blend(ar, as, at, au, ab, ac, ad, ae);
            this.lightmap[ambientVertexRemap.vert1] = blend(ar, as, at, au, af, ag, ah, ai);
            this.lightmap[ambientVertexRemap.vert2] = blend(ar, as, at, au, aj, ak, al, am);
            this.lightmap[ambientVertexRemap.vert3] = blend(ar, as, at, au, an, ao, ap, aq);
         } else {
            float x = (m + f + p + w) * 0.25F;
            float y = (h + f + n + w) * 0.25F;
            float z = (h + g + r + w) * 0.25F;
            float aa = (m + g + t + w) * 0.25F;
            this.lightmap[ambientVertexRemap.vert0] = blend(l, i, q, v);
            this.lightmap[ambientVertexRemap.vert1] = blend(k, i, o, v);
            this.lightmap[ambientVertexRemap.vert2] = blend(k, j, s, v);
            this.lightmap[ambientVertexRemap.vert3] = blend(l, j, u, v);
            this.brightness[ambientVertexRemap.vert0] = x;
            this.brightness[ambientVertexRemap.vert1] = y;
            this.brightness[ambientVertexRemap.vert2] = z;
            this.brightness[ambientVertexRemap.vert3] = aa;
         }

         float x = level.getShade(direction, shade);

         for (int av = 0; av < this.brightness.length; av++) {
            this.brightness[av] = this.brightness[av] * x;
         }
      }

      private static int blend(int color1, int color2, int color3, int currentBlockColor) {
         if (color1 == 0) {
            color1 = currentBlockColor;
         }

         if (color2 == 0) {
            color2 = currentBlockColor;
         }

         if (color3 == 0) {
            color3 = currentBlockColor;
         }

         return color1 + color2 + color3 + currentBlockColor >> 2 & 16711935;
      }

      private static int blend(
         int color1, int color2, int color3, int blockLight, float color1Weight, float color2Weight, float color3Weight, float blockLightWeight
      ) {
         int i = (int)(
               (color1 >> 16 & 0xFF) * color1Weight
                  + (color2 >> 16 & 0xFF) * color2Weight
                  + (color3 >> 16 & 0xFF) * color3Weight
                  + (blockLight >> 16 & 0xFF) * blockLightWeight
            )
            & 0xFF;
         int j = (int)(
               (color1 & 0xFF) * color1Weight + (color2 & 0xFF) * color2Weight + (color3 & 0xFF) * color3Weight + (blockLight & 0xFF) * blockLightWeight
            )
            & 0xFF;
         return i << 16 | j;
      }
   }

   @Environment(EnvType.CLIENT)
   static enum AmbientVertexRemap {
      DOWN(0, 1, 2, 3),
      UP(2, 3, 0, 1),
      NORTH(3, 0, 1, 2),
      SOUTH(0, 1, 2, 3),
      WEST(3, 0, 1, 2),
      EAST(1, 2, 3, 0);

      final int vert0;
      final int vert1;
      final int vert2;
      final int vert3;
      private static final ModelBlockRenderer.AmbientVertexRemap[] BY_FACING = Util.make(new ModelBlockRenderer.AmbientVertexRemap[6], ambientVertexRemaps -> {
         ambientVertexRemaps[Direction.DOWN.get3DDataValue()] = DOWN;
         ambientVertexRemaps[Direction.UP.get3DDataValue()] = UP;
         ambientVertexRemaps[Direction.NORTH.get3DDataValue()] = NORTH;
         ambientVertexRemaps[Direction.SOUTH.get3DDataValue()] = SOUTH;
         ambientVertexRemaps[Direction.WEST.get3DDataValue()] = WEST;
         ambientVertexRemaps[Direction.EAST.get3DDataValue()] = EAST;
      });

      private AmbientVertexRemap(final int vert0, final int vert1, final int vert2, final int vert3) {
         this.vert0 = vert0;
         this.vert1 = vert1;
         this.vert2 = vert2;
         this.vert3 = vert3;
      }

      public static ModelBlockRenderer.AmbientVertexRemap fromFacing(Direction facing) {
         return BY_FACING[facing.get3DDataValue()];
      }
   }

   @Environment(EnvType.CLIENT)
   static class Cache {
      private boolean enabled;
      private final Long2IntLinkedOpenHashMap colorCache = Util.make(() -> {
         Long2IntLinkedOpenHashMap long2IntLinkedOpenHashMap = new Long2IntLinkedOpenHashMap(100, 0.25F) {
            protected void rehash(int i) {
            }
         };
         long2IntLinkedOpenHashMap.defaultReturnValue(Integer.MAX_VALUE);
         return long2IntLinkedOpenHashMap;
      });
      private final Long2FloatLinkedOpenHashMap brightnessCache = Util.make(() -> {
         Long2FloatLinkedOpenHashMap long2FloatLinkedOpenHashMap = new Long2FloatLinkedOpenHashMap(100, 0.25F) {
            protected void rehash(int i) {
            }
         };
         long2FloatLinkedOpenHashMap.defaultReturnValue(Float.NaN);
         return long2FloatLinkedOpenHashMap;
      });
      private final LevelRenderer.BrightnessGetter cachedBrightnessGetter = (blockAndTintGetter, blockPos) -> {
         long l = blockPos.asLong();
         int i = this.colorCache.get(l);
         if (i != Integer.MAX_VALUE) {
            return i;
         } else {
            int j = LevelRenderer.BrightnessGetter.DEFAULT.packedBrightness(blockAndTintGetter, blockPos);
            if (this.colorCache.size() == 100) {
               this.colorCache.removeFirstInt();
            }

            this.colorCache.put(l, j);
            return j;
         }
      };

      private Cache() {
      }

      public void enable() {
         this.enabled = true;
      }

      public void disable() {
         this.enabled = false;
         this.colorCache.clear();
         this.brightnessCache.clear();
      }

      public int getLightColor(BlockState state, BlockAndTintGetter level, BlockPos pos) {
         return LevelRenderer.getLightColor(this.enabled ? this.cachedBrightnessGetter : LevelRenderer.BrightnessGetter.DEFAULT, level, state, pos);
      }

      public float getShadeBrightness(BlockState state, BlockAndTintGetter level, BlockPos pos) {
         long l = pos.asLong();
         if (this.enabled) {
            float f = this.brightnessCache.get(l);
            if (!Float.isNaN(f)) {
               return f;
            }
         }

         float f = state.getShadeBrightness(level, pos);
         if (this.enabled) {
            if (this.brightnessCache.size() == 100) {
               this.brightnessCache.removeFirstFloat();
            }

            this.brightnessCache.put(l, f);
         }

         return f;
      }
   }

   @Environment(EnvType.CLIENT)
   static class CommonRenderStorage {
      public final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
      public boolean faceCubic;
      public boolean facePartial;
      public final float[] brightness = new float[4];
      public final int[] lightmap = new int[4];
      public int tintCacheIndex = -1;
      public int tintCacheValue;
      public final ModelBlockRenderer.Cache cache = ModelBlockRenderer.CACHE.get();
   }

   @Environment(EnvType.CLIENT)
   protected static enum SizeInfo {
      DOWN(0),
      UP(1),
      NORTH(2),
      SOUTH(3),
      WEST(4),
      EAST(5),
      FLIP_DOWN(6),
      FLIP_UP(7),
      FLIP_NORTH(8),
      FLIP_SOUTH(9),
      FLIP_WEST(10),
      FLIP_EAST(11);

      public static final int COUNT = values().length;
      final int index;

      private SizeInfo(final int index) {
         this.index = index;
      }
   }
}
