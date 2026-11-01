package net.neoforged.neoforge.client.model.ao;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

class FullFaceCalculator {
   private static final boolean DISABLE_LIGHTMAP_BLENDING_FIX = Boolean.getBoolean("neoforge.ao.disableLightmapBlendingFix");
   final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
   private ModelBlockRenderer.Cache cache;
   private final AoCalculatedFace[] aoFaces = new AoCalculatedFace[24];
   private int calculatedAoFaces;

   FullFaceCalculator() {
      for (int i = 0; i < 24; i++) {
         this.aoFaces[i] = new AoCalculatedFace();
      }

      this.calculatedAoFaces = 0;
   }

   void startBlock(ModelBlockRenderer.Cache cache) {
      this.calculatedAoFaces = 0;
      this.cache = cache;
   }

   AoCalculatedFace calculateFace(
      BlockAndTintGetter level, BlockState renderedState, BlockPos renderedPos, Direction direction, boolean shade, boolean sampleOutside
   ) {
      int cacheIndex = direction.get3DDataValue();
      if (sampleOutside) {
         cacheIndex += 6;
      }

      if (shade) {
         cacheIndex += 12;
      }

      if ((this.calculatedAoFaces & 1 << cacheIndex) != 0) {
         return this.aoFaces[cacheIndex];
      } else {
         AoCalculatedFace fullFace = this.aoFaces[cacheIndex];
         this.calculateFaceUncached(fullFace, level, renderedState, renderedPos, direction, shade, sampleOutside);
         this.calculatedAoFaces |= 1 << cacheIndex;
         return fullFace;
      }
   }

   private void calculateFaceUncached(
      AoCalculatedFace out, BlockAndTintGetter level, BlockState renderedState, BlockPos renderedPos, Direction direction, boolean shade, boolean sampleOutside
   ) {
      BlockPos samplePos = sampleOutside ? renderedPos.relative(direction) : renderedPos;
      ModelBlockRenderer.AdjacencyInfo adjacencyInfo = ModelBlockRenderer.AdjacencyInfo.fromFacing(direction);
      BlockPos.MutableBlockPos scratchPos = this.scratchPos;
      scratchPos.setWithOffset(samplePos, adjacencyInfo.corners[0]);
      BlockState sideState0 = level.getBlockState(scratchPos);
      int sideLightmap0 = this.cache.getLightColor(sideState0, level, scratchPos);
      float sideBrightness0 = this.cache.getShadeBrightness(sideState0, level, scratchPos);
      boolean sideClear0 = !sideState0.isViewBlocking(level, scratchPos) || sideState0.getLightBlock() == 0;
      scratchPos.setWithOffset(samplePos, adjacencyInfo.corners[1]);
      BlockState sideState1 = level.getBlockState(scratchPos);
      int sideLightmap1 = this.cache.getLightColor(sideState1, level, scratchPos);
      float sideBrightness1 = this.cache.getShadeBrightness(sideState1, level, scratchPos);
      boolean sideClear1 = !sideState1.isViewBlocking(level, scratchPos) || sideState1.getLightBlock() == 0;
      scratchPos.setWithOffset(samplePos, adjacencyInfo.corners[2]);
      BlockState sideState2 = level.getBlockState(scratchPos);
      int sideLightmap2 = this.cache.getLightColor(sideState2, level, scratchPos);
      float sideBrightness2 = this.cache.getShadeBrightness(sideState2, level, scratchPos);
      boolean sideClear2 = !sideState2.isViewBlocking(level, scratchPos) || sideState2.getLightBlock() == 0;
      scratchPos.setWithOffset(samplePos, adjacencyInfo.corners[3]);
      BlockState sideState3 = level.getBlockState(scratchPos);
      int sideLightmap3 = this.cache.getLightColor(sideState3, level, scratchPos);
      float sideBrightness3 = this.cache.getShadeBrightness(sideState3, level, scratchPos);
      boolean sideClear3 = !sideState3.isViewBlocking(level, scratchPos) || sideState3.getLightBlock() == 0;
      float cornerBrightness0;
      int cornerLightmap0;
      boolean cornerClear0;
      if (!sideClear2 && !sideClear0) {
         cornerBrightness0 = sideBrightness0;
         cornerLightmap0 = sideLightmap0;
         cornerClear0 = false;
      } else {
         scratchPos.setWithOffset(samplePos, adjacencyInfo.corners[0]).move(adjacencyInfo.corners[2]);
         BlockState cornerState0 = level.getBlockState(scratchPos);
         cornerBrightness0 = this.cache.getShadeBrightness(cornerState0, level, scratchPos);
         cornerLightmap0 = this.cache.getLightColor(cornerState0, level, scratchPos);
         cornerClear0 = !cornerState0.isViewBlocking(level, scratchPos) || cornerState0.getLightBlock() == 0;
      }

      int cornerLightmap1;
      boolean cornerClear1;
      float cornerBrightness1;
      if (!sideClear3 && !sideClear0) {
         cornerBrightness1 = sideBrightness0;
         cornerLightmap1 = sideLightmap0;
         cornerClear1 = false;
      } else {
         scratchPos.setWithOffset(samplePos, adjacencyInfo.corners[0]).move(adjacencyInfo.corners[3]);
         BlockState cornerState1 = level.getBlockState(scratchPos);
         cornerBrightness1 = this.cache.getShadeBrightness(cornerState1, level, scratchPos);
         cornerLightmap1 = this.cache.getLightColor(cornerState1, level, scratchPos);
         cornerClear1 = !cornerState1.isViewBlocking(level, scratchPos) || cornerState1.getLightBlock() == 0;
      }

      int cornerLightmap2;
      boolean cornerClear2;
      float cornerBrightness2;
      if (!sideClear2 && !sideClear1) {
         cornerBrightness2 = sideBrightness1;
         cornerLightmap2 = sideLightmap1;
         cornerClear2 = false;
      } else {
         scratchPos.setWithOffset(samplePos, adjacencyInfo.corners[1]).move(adjacencyInfo.corners[2]);
         BlockState cornerState2 = level.getBlockState(scratchPos);
         cornerBrightness2 = this.cache.getShadeBrightness(cornerState2, level, scratchPos);
         cornerLightmap2 = this.cache.getLightColor(cornerState2, level, scratchPos);
         cornerClear2 = !cornerState2.isViewBlocking(level, scratchPos) || cornerState2.getLightBlock() == 0;
      }

      int cornerLightmap3;
      boolean cornerClear3;
      float cornerBrightness3;
      if (!sideClear3 && !sideClear1) {
         cornerBrightness3 = sideBrightness1;
         cornerLightmap3 = sideLightmap1;
         cornerClear3 = false;
      } else {
         scratchPos.setWithOffset(samplePos, adjacencyInfo.corners[1]).move(adjacencyInfo.corners[3]);
         BlockState cornerState3 = level.getBlockState(scratchPos);
         cornerBrightness3 = this.cache.getShadeBrightness(cornerState3, level, scratchPos);
         cornerLightmap3 = this.cache.getLightColor(cornerState3, level, scratchPos);
         cornerClear3 = !cornerState3.isViewBlocking(level, scratchPos) || cornerState3.getLightBlock() == 0;
      }

      BlockState insideState = sampleOutside ? level.getBlockState(samplePos) : renderedState;
      float insideBrightness = this.cache.getShadeBrightness(insideState, level, samplePos);
      int insideLightmap = this.cache.getLightColor(insideState, level, samplePos);
      boolean insideClear = !insideState.isViewBlocking(level, samplePos) || insideState.getLightBlock() == 0;
      float levelBrightness = level.getShade(direction, shade);
      out.brightness0 = (sideBrightness3 + sideBrightness0 + cornerBrightness1 + insideBrightness) * 0.25F * levelBrightness;
      out.brightness1 = (sideBrightness2 + sideBrightness0 + cornerBrightness0 + insideBrightness) * 0.25F * levelBrightness;
      out.brightness2 = (sideBrightness2 + sideBrightness1 + cornerBrightness2 + insideBrightness) * 0.25F * levelBrightness;
      out.brightness3 = (sideBrightness3 + sideBrightness1 + cornerBrightness3 + insideBrightness) * 0.25F * levelBrightness;
      out.lightmap0 = blend(sideLightmap3, sideLightmap0, cornerLightmap1, insideLightmap, sideClear3, sideClear0, cornerClear1, insideClear);
      out.lightmap1 = blend(sideLightmap2, sideLightmap0, cornerLightmap0, insideLightmap, sideClear2, sideClear0, cornerClear0, insideClear);
      out.lightmap2 = blend(sideLightmap2, sideLightmap1, cornerLightmap2, insideLightmap, sideClear2, sideClear1, cornerClear2, insideClear);
      out.lightmap3 = blend(sideLightmap3, sideLightmap1, cornerLightmap3, insideLightmap, sideClear3, sideClear1, cornerClear3, insideClear);
   }

   private static int blend(
      int sideLightmapA,
      int sideLightmapB,
      int cornerLightmap,
      int insideLightmap,
      boolean sideClearA,
      boolean sideClearB,
      boolean cornerClear,
      boolean insideClear
   ) {
      if (DISABLE_LIGHTMAP_BLENDING_FIX) {
         if (sideLightmapA == 0) {
            sideLightmapA = insideLightmap;
         }

         if (sideLightmapB == 0) {
            sideLightmapB = insideLightmap;
         }

         if (cornerLightmap == 0) {
            cornerLightmap = insideLightmap;
         }
      } else {
         int sideBlockA = LightTexture.blockWithFraction(sideLightmapA);
         int sideBlockB = LightTexture.blockWithFraction(sideLightmapB);
         int cornerBlock = LightTexture.blockWithFraction(cornerLightmap);
         int insideBlock = LightTexture.blockWithFraction(insideLightmap);
         int sideSkyA = LightTexture.skyWithFraction(sideLightmapA);
         int sideSkyB = LightTexture.skyWithFraction(sideLightmapB);
         int cornerSky = LightTexture.skyWithFraction(cornerLightmap);
         int insideSky = LightTexture.skyWithFraction(insideLightmap);
         int minBlock = 65536;
         int minSky = 65536;
         if (sideClearA) {
            minBlock = sideBlockA;
            minSky = sideSkyA;
         }

         if (sideClearB) {
            minBlock = Math.min(minBlock, sideBlockB);
            minSky = Math.min(minSky, sideSkyB);
         }

         if (cornerClear) {
            minBlock = Math.min(minBlock, cornerBlock);
            minSky = Math.min(minSky, cornerSky);
         }

         if (insideClear) {
            minBlock = Math.min(minBlock, insideBlock);
            minSky = Math.min(minSky, insideSky);
         }

         minBlock &= 65535;
         minSky &= 65535;
         sideLightmapA = LightTexture.packWithFraction(Math.max(minBlock, sideBlockA), Math.max(minSky, sideSkyA));
         sideLightmapB = LightTexture.packWithFraction(Math.max(minBlock, sideBlockB), Math.max(minSky, sideSkyB));
         cornerLightmap = LightTexture.packWithFraction(Math.max(minBlock, cornerBlock), Math.max(minSky, cornerSky));
         insideLightmap = LightTexture.packWithFraction(Math.max(minBlock, insideBlock), Math.max(minSky, insideSky));
      }

      return sideLightmapA + sideLightmapB + cornerLightmap + insideLightmap >> 2 & 16711935;
   }
}
