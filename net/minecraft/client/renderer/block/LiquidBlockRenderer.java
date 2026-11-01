package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@Environment(EnvType.CLIENT)
public class LiquidBlockRenderer {
   private static final float MAX_FLUID_HEIGHT = 0.8888889F;
   private final TextureAtlasSprite lavaStill;
   private final TextureAtlasSprite lavaFlowing;
   private final TextureAtlasSprite waterStill;
   private final TextureAtlasSprite waterFlowing;
   private final TextureAtlasSprite waterOverlay;

   public LiquidBlockRenderer(MaterialSet materials) {
      this.lavaStill = materials.get(ModelBakery.LAVA_STILL);
      this.lavaFlowing = materials.get(ModelBakery.LAVA_FLOW);
      this.waterStill = materials.get(ModelBakery.WATER_STILL);
      this.waterFlowing = materials.get(ModelBakery.WATER_FLOW);
      this.waterOverlay = materials.get(ModelBakery.WATER_OVERLAY);
   }

   private static boolean isNeighborSameFluid(FluidState firstState, FluidState secondState) {
      return secondState.getType().isSame(firstState.getType());
   }

   private static boolean isFaceOccludedByState(Direction face, float height, BlockState state) {
      VoxelShape voxelShape = state.getFaceOcclusionShape(face.getOpposite());
      if (voxelShape == Shapes.empty()) {
         return false;
      } else if (voxelShape == Shapes.block()) {
         boolean bl = height == 1.0F;
         return face != Direction.UP || bl;
      } else {
         VoxelShape voxelShape2 = Shapes.box(0.0, 0.0, 0.0, 1.0, height, 1.0);
         return Shapes.blockOccludes(voxelShape2, voxelShape, face);
      }
   }

   private static boolean isFaceOccludedByNeighbor(Direction face, float height, BlockState state) {
      return isFaceOccludedByState(face, height, state);
   }

   private static boolean isFaceOccludedBySelf(BlockState state, Direction face) {
      return isFaceOccludedByState(face.getOpposite(), 1.0F, state);
   }

   public static boolean shouldRenderFace(FluidState fluidState, BlockState blockState, Direction side, FluidState neighborFluid) {
      return !isFaceOccludedBySelf(blockState, side) && !isNeighborSameFluid(fluidState, neighborFluid);
   }

   public void tesselate(BlockAndTintGetter level, BlockPos pos, VertexConsumer buffer, BlockState blockState, FluidState fluidState) {
      boolean bl = fluidState.is(FluidTags.LAVA);
      TextureAtlasSprite textureAtlasSprite = bl ? this.lavaStill : this.waterStill;
      TextureAtlasSprite textureAtlasSprite2 = bl ? this.lavaFlowing : this.waterFlowing;
      int i = bl ? 16777215 : BiomeColors.getAverageWaterColor(level, pos);
      float f = (i >> 16 & 0xFF) / 255.0F;
      float g = (i >> 8 & 0xFF) / 255.0F;
      float h = (i & 0xFF) / 255.0F;
      BlockState blockState2 = level.getBlockState(pos.relative(Direction.DOWN));
      FluidState fluidState2 = blockState2.getFluidState();
      BlockState blockState3 = level.getBlockState(pos.relative(Direction.UP));
      FluidState fluidState3 = blockState3.getFluidState();
      BlockState blockState4 = level.getBlockState(pos.relative(Direction.NORTH));
      FluidState fluidState4 = blockState4.getFluidState();
      BlockState blockState5 = level.getBlockState(pos.relative(Direction.SOUTH));
      FluidState fluidState5 = blockState5.getFluidState();
      BlockState blockState6 = level.getBlockState(pos.relative(Direction.WEST));
      FluidState fluidState6 = blockState6.getFluidState();
      BlockState blockState7 = level.getBlockState(pos.relative(Direction.EAST));
      FluidState fluidState7 = blockState7.getFluidState();
      boolean bl2 = !isNeighborSameFluid(fluidState, fluidState3);
      boolean bl3 = shouldRenderFace(fluidState, blockState, Direction.DOWN, fluidState2) && !isFaceOccludedByNeighbor(Direction.DOWN, 0.8888889F, blockState2);
      boolean bl4 = shouldRenderFace(fluidState, blockState, Direction.NORTH, fluidState4);
      boolean bl5 = shouldRenderFace(fluidState, blockState, Direction.SOUTH, fluidState5);
      boolean bl6 = shouldRenderFace(fluidState, blockState, Direction.WEST, fluidState6);
      boolean bl7 = shouldRenderFace(fluidState, blockState, Direction.EAST, fluidState7);
      if (bl2 || bl3 || bl7 || bl6 || bl4 || bl5) {
         float j = level.getShade(Direction.DOWN, true);
         float k = level.getShade(Direction.UP, true);
         float l = level.getShade(Direction.NORTH, true);
         float m = level.getShade(Direction.WEST, true);
         Fluid fluid = fluidState.getType();
         float n = this.getHeight(level, fluid, pos, blockState, fluidState);
         float o;
         float p;
         float q;
         float r;
         if (n >= 1.0F) {
            o = 1.0F;
            p = 1.0F;
            q = 1.0F;
            r = 1.0F;
         } else {
            float s = this.getHeight(level, fluid, pos.north(), blockState4, fluidState4);
            float t = this.getHeight(level, fluid, pos.south(), blockState5, fluidState5);
            float u = this.getHeight(level, fluid, pos.east(), blockState7, fluidState7);
            float v = this.getHeight(level, fluid, pos.west(), blockState6, fluidState6);
            o = this.calculateAverageHeight(level, fluid, n, s, u, pos.relative(Direction.NORTH).relative(Direction.EAST));
            p = this.calculateAverageHeight(level, fluid, n, s, v, pos.relative(Direction.NORTH).relative(Direction.WEST));
            q = this.calculateAverageHeight(level, fluid, n, t, u, pos.relative(Direction.SOUTH).relative(Direction.EAST));
            r = this.calculateAverageHeight(level, fluid, n, t, v, pos.relative(Direction.SOUTH).relative(Direction.WEST));
         }

         float s = pos.getX() & 15;
         float t = pos.getY() & 15;
         float u = pos.getZ() & 15;
         float v = 0.001F;
         float w = bl3 ? 0.001F : 0.0F;
         if (bl2 && !isFaceOccludedByNeighbor(Direction.UP, Math.min(Math.min(p, r), Math.min(q, o)), blockState3)) {
            p -= 0.001F;
            r -= 0.001F;
            q -= 0.001F;
            o -= 0.001F;
            Vec3 vec3 = fluidState.getFlow(level, pos);
            float x;
            float z;
            float ab;
            float ad;
            float y;
            float aa;
            float ac;
            float ae;
            if (vec3.x == 0.0 && vec3.z == 0.0) {
               x = textureAtlasSprite.getU(0.0F);
               y = textureAtlasSprite.getV(0.0F);
               z = x;
               aa = textureAtlasSprite.getV(1.0F);
               ab = textureAtlasSprite.getU(1.0F);
               ac = aa;
               ad = ab;
               ae = y;
            } else {
               float af = (float)Mth.atan2(vec3.z, vec3.x) - (float) (Math.PI / 2);
               float ag = Mth.sin(af) * 0.25F;
               float ah = Mth.cos(af) * 0.25F;
               float ai = 0.5F;
               x = textureAtlasSprite2.getU(0.5F + (-ah - ag));
               y = textureAtlasSprite2.getV(0.5F + (-ah + ag));
               z = textureAtlasSprite2.getU(0.5F + (-ah + ag));
               aa = textureAtlasSprite2.getV(0.5F + (ah + ag));
               ab = textureAtlasSprite2.getU(0.5F + (ah + ag));
               ac = textureAtlasSprite2.getV(0.5F + (ah - ag));
               ad = textureAtlasSprite2.getU(0.5F + (ah - ag));
               ae = textureAtlasSprite2.getV(0.5F + (-ah - ag));
            }

            int aj = this.getLightColor(level, pos);
            float ag = k * f;
            float ah = k * g;
            float ai = k * h;
            this.vertex(buffer, s + 0.0F, t + p, u + 0.0F, ag, ah, ai, x, y, aj);
            this.vertex(buffer, s + 0.0F, t + r, u + 1.0F, ag, ah, ai, z, aa, aj);
            this.vertex(buffer, s + 1.0F, t + q, u + 1.0F, ag, ah, ai, ab, ac, aj);
            this.vertex(buffer, s + 1.0F, t + o, u + 0.0F, ag, ah, ai, ad, ae, aj);
            if (fluidState.shouldRenderBackwardUpFace(level, pos.above())) {
               this.vertex(buffer, s + 0.0F, t + p, u + 0.0F, ag, ah, ai, x, y, aj);
               this.vertex(buffer, s + 1.0F, t + o, u + 0.0F, ag, ah, ai, ad, ae, aj);
               this.vertex(buffer, s + 1.0F, t + q, u + 1.0F, ag, ah, ai, ab, ac, aj);
               this.vertex(buffer, s + 0.0F, t + r, u + 1.0F, ag, ah, ai, z, aa, aj);
            }
         }

         if (bl3) {
            float xx = textureAtlasSprite.getU0();
            float zx = textureAtlasSprite.getU1();
            float abx = textureAtlasSprite.getV0();
            float adx = textureAtlasSprite.getV1();
            int ak = this.getLightColor(level, pos.below());
            float aax = j * f;
            float acx = j * g;
            float aex = j * h;
            this.vertex(buffer, s, t + w, u + 1.0F, aax, acx, aex, xx, adx, ak);
            this.vertex(buffer, s, t + w, u, aax, acx, aex, xx, abx, ak);
            this.vertex(buffer, s + 1.0F, t + w, u, aax, acx, aex, zx, abx, ak);
            this.vertex(buffer, s + 1.0F, t + w, u + 1.0F, aax, acx, aex, zx, adx, ak);
         }

         int al = this.getLightColor(level, pos);

         for (Direction direction : Direction.Plane.HORIZONTAL) {
            float adx;
            float yx;
            float aax;
            float acx;
            float aex;
            float am;
            boolean bl8;
            switch (direction) {
               case NORTH:
                  adx = p;
                  yx = o;
                  aax = s;
                  aex = s + 1.0F;
                  acx = u + 0.001F;
                  am = u + 0.001F;
                  bl8 = bl4;
                  break;
               case SOUTH:
                  adx = q;
                  yx = r;
                  aax = s + 1.0F;
                  aex = s;
                  acx = u + 1.0F - 0.001F;
                  am = u + 1.0F - 0.001F;
                  bl8 = bl5;
                  break;
               case WEST:
                  adx = r;
                  yx = p;
                  aax = s + 0.001F;
                  aex = s + 0.001F;
                  acx = u + 1.0F;
                  am = u;
                  bl8 = bl6;
                  break;
               default:
                  adx = o;
                  yx = q;
                  aax = s + 1.0F - 0.001F;
                  aex = s + 1.0F - 0.001F;
                  acx = u;
                  am = u + 1.0F;
                  bl8 = bl7;
            }

            if (bl8 && !isFaceOccludedByNeighbor(direction, Math.max(adx, yx), level.getBlockState(pos.relative(direction)))) {
               BlockPos blockPos = pos.relative(direction);
               TextureAtlasSprite textureAtlasSprite3 = textureAtlasSprite2;
               if (!bl) {
                  Block block = level.getBlockState(blockPos).getBlock();
                  if (block instanceof HalfTransparentBlock || block instanceof LeavesBlock) {
                     textureAtlasSprite3 = this.waterOverlay;
                  }
               }

               float ai = textureAtlasSprite3.getU(0.0F);
               float an = textureAtlasSprite3.getU(0.5F);
               float ao = textureAtlasSprite3.getV((1.0F - adx) * 0.5F);
               float ap = textureAtlasSprite3.getV((1.0F - yx) * 0.5F);
               float aq = textureAtlasSprite3.getV(0.5F);
               float ar = direction.getAxis() == Direction.Axis.Z ? l : m;
               float as = k * ar * f;
               float at = k * ar * g;
               float au = k * ar * h;
               this.vertex(buffer, aax, t + adx, acx, as, at, au, ai, ao, al);
               this.vertex(buffer, aex, t + yx, am, as, at, au, an, ap, al);
               this.vertex(buffer, aex, t + w, am, as, at, au, an, aq, al);
               this.vertex(buffer, aax, t + w, acx, as, at, au, ai, aq, al);
               if (textureAtlasSprite3 != this.waterOverlay) {
                  this.vertex(buffer, aax, t + w, acx, as, at, au, ai, aq, al);
                  this.vertex(buffer, aex, t + w, am, as, at, au, an, aq, al);
                  this.vertex(buffer, aex, t + yx, am, as, at, au, an, ap, al);
                  this.vertex(buffer, aax, t + adx, acx, as, at, au, ai, ao, al);
               }
            }
         }
      }
   }

   private float calculateAverageHeight(BlockAndTintGetter level, Fluid fluid, float currentHeight, float height1, float height2, BlockPos pos) {
      if (!(height2 >= 1.0F) && !(height1 >= 1.0F)) {
         float[] fs = new float[2];
         if (height2 > 0.0F || height1 > 0.0F) {
            float f = this.getHeight(level, fluid, pos);
            if (f >= 1.0F) {
               return 1.0F;
            }

            this.addWeightedHeight(fs, f);
         }

         this.addWeightedHeight(fs, currentHeight);
         this.addWeightedHeight(fs, height2);
         this.addWeightedHeight(fs, height1);
         return fs[0] / fs[1];
      } else {
         return 1.0F;
      }
   }

   private void addWeightedHeight(float[] output, float height) {
      if (height >= 0.8F) {
         output[0] += height * 10.0F;
         output[1] += 10.0F;
      } else if (height >= 0.0F) {
         output[0] += height;
         output[1]++;
      }
   }

   private float getHeight(BlockAndTintGetter level, Fluid fluid, BlockPos pos) {
      BlockState blockState = level.getBlockState(pos);
      return this.getHeight(level, fluid, pos, blockState, blockState.getFluidState());
   }

   private float getHeight(BlockAndTintGetter level, Fluid fluid, BlockPos pos, BlockState blockState, FluidState fluidState) {
      if (fluid.isSame(fluidState.getType())) {
         BlockState blockState2 = level.getBlockState(pos.above());
         return fluid.isSame(blockState2.getFluidState().getType()) ? 1.0F : fluidState.getOwnHeight();
      } else {
         return !blockState.isSolid() ? 0.0F : -1.0F;
      }
   }

   private void vertex(VertexConsumer buffer, float x, float y, float z, float red, float green, float blue, float u, float v, int packedLight) {
      buffer.addVertex(x, y, z).setColor(red, green, blue, 1.0F).setUv(u, v).setLight(packedLight).setNormal(0.0F, 1.0F, 0.0F);
   }

   private int getLightColor(BlockAndTintGetter level, BlockPos pos) {
      int i = LevelRenderer.getLightColor(level, pos);
      int j = LevelRenderer.getLightColor(level, pos.above());
      int k = i & 0xFF;
      int l = j & 0xFF;
      int m = i >> 16 & 0xFF;
      int n = j >> 16 & 0xFF;
      return (k > l ? k : l) | (m > n ? m : n) << 16;
   }
}
