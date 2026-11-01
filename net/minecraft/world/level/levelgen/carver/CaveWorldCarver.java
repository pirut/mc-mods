package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;

public class CaveWorldCarver extends WorldCarver<CaveCarverConfiguration> {
   public CaveWorldCarver(Codec<CaveCarverConfiguration> codec) {
      super(codec);
   }

   public boolean isStartChunk(CaveCarverConfiguration caveCarverConfiguration, RandomSource randomSource) {
      return randomSource.nextFloat() <= caveCarverConfiguration.probability;
   }

   public boolean carve(
      CarvingContext carvingContext,
      CaveCarverConfiguration caveCarverConfiguration,
      ChunkAccess chunkAccess,
      Function<BlockPos, Holder<Biome>> function,
      RandomSource randomSource,
      Aquifer aquifer,
      ChunkPos chunkPos,
      CarvingMask carvingMask
   ) {
      int i = SectionPos.sectionToBlockCoord(this.getRange() * 2 - 1);
      int j = randomSource.nextInt(randomSource.nextInt(randomSource.nextInt(this.getCaveBound()) + 1) + 1);

      for (int k = 0; k < j; k++) {
         double d = chunkPos.getBlockX(randomSource.nextInt(16));
         double e = caveCarverConfiguration.y.sample(randomSource, carvingContext);
         double f = chunkPos.getBlockZ(randomSource.nextInt(16));
         double g = caveCarverConfiguration.horizontalRadiusMultiplier.sample(randomSource);
         double h = caveCarverConfiguration.verticalRadiusMultiplier.sample(randomSource);
         double l = caveCarverConfiguration.floorLevel.sample(randomSource);
         WorldCarver.CarveSkipChecker carveSkipChecker = (carvingContextx, ex, fx, gx, ix) -> shouldSkip(ex, fx, gx, l);
         int m = 1;
         if (randomSource.nextInt(4) == 0) {
            double n = caveCarverConfiguration.yScale.sample(randomSource);
            float o = 1.0F + randomSource.nextFloat() * 6.0F;
            this.createRoom(carvingContext, caveCarverConfiguration, chunkAccess, function, aquifer, d, e, f, o, n, carvingMask, carveSkipChecker);
            m += randomSource.nextInt(4);
         }

         for (int p = 0; p < m; p++) {
            float q = randomSource.nextFloat() * (float) (Math.PI * 2);
            float o = (randomSource.nextFloat() - 0.5F) / 4.0F;
            float r = this.getThickness(randomSource);
            int s = i - randomSource.nextInt(i / 4);
            int t = 0;
            this.createTunnel(
               carvingContext,
               caveCarverConfiguration,
               chunkAccess,
               function,
               randomSource.nextLong(),
               aquifer,
               d,
               e,
               f,
               g,
               h,
               r,
               q,
               o,
               0,
               s,
               this.getYScale(),
               carvingMask,
               carveSkipChecker
            );
         }
      }

      return true;
   }

   protected int getCaveBound() {
      return 15;
   }

   protected float getThickness(RandomSource random) {
      float f = random.nextFloat() * 2.0F + random.nextFloat();
      if (random.nextInt(10) == 0) {
         f *= random.nextFloat() * random.nextFloat() * 3.0F + 1.0F;
      }

      return f;
   }

   protected double getYScale() {
      return 1.0;
   }

   protected void createRoom(
      CarvingContext context,
      CaveCarverConfiguration config,
      ChunkAccess chunk,
      Function<BlockPos, Holder<Biome>> biomeAccessor,
      Aquifer aquifer,
      double x,
      double y,
      double z,
      float radius,
      double horizontalVerticalRatio,
      CarvingMask carvingMask,
      WorldCarver.CarveSkipChecker skipChecker
   ) {
      double d = 1.5 + Mth.sin((float) (Math.PI / 2)) * radius;
      double e = d * horizontalVerticalRatio;
      this.carveEllipsoid(context, config, chunk, biomeAccessor, aquifer, x + 1.0, y, z, d, e, carvingMask, skipChecker);
   }

   protected void createTunnel(
      CarvingContext context,
      CaveCarverConfiguration config,
      ChunkAccess chunk,
      Function<BlockPos, Holder<Biome>> biomeAccessor,
      long seed,
      Aquifer aquifer,
      double x,
      double y,
      double z,
      double horizontalRadiusMultiplier,
      double verticalRadiusMultiplier,
      float thickness,
      float yaw,
      float pitch,
      int branchIndex,
      int branchCount,
      double horizontalVerticalRatio,
      CarvingMask carvingMask,
      WorldCarver.CarveSkipChecker skipChecker
   ) {
      RandomSource randomSource = RandomSource.create(seed);
      int i = randomSource.nextInt(branchCount / 2) + branchCount / 4;
      boolean bl = randomSource.nextInt(6) == 0;
      float f = 0.0F;
      float g = 0.0F;

      for (int j = branchIndex; j < branchCount; j++) {
         double d = 1.5 + Mth.sin((float) Math.PI * j / branchCount) * thickness;
         double e = d * horizontalVerticalRatio;
         float h = Mth.cos(pitch);
         x += Mth.cos(yaw) * h;
         y += Mth.sin(pitch);
         z += Mth.sin(yaw) * h;
         pitch *= bl ? 0.92F : 0.7F;
         pitch += g * 0.1F;
         yaw += f * 0.1F;
         g *= 0.9F;
         f *= 0.75F;
         g += (randomSource.nextFloat() - randomSource.nextFloat()) * randomSource.nextFloat() * 2.0F;
         f += (randomSource.nextFloat() - randomSource.nextFloat()) * randomSource.nextFloat() * 4.0F;
         if (j == i && thickness > 1.0F) {
            this.createTunnel(
               context,
               config,
               chunk,
               biomeAccessor,
               randomSource.nextLong(),
               aquifer,
               x,
               y,
               z,
               horizontalRadiusMultiplier,
               verticalRadiusMultiplier,
               randomSource.nextFloat() * 0.5F + 0.5F,
               yaw - (float) (Math.PI / 2),
               pitch / 3.0F,
               j,
               branchCount,
               1.0,
               carvingMask,
               skipChecker
            );
            this.createTunnel(
               context,
               config,
               chunk,
               biomeAccessor,
               randomSource.nextLong(),
               aquifer,
               x,
               y,
               z,
               horizontalRadiusMultiplier,
               verticalRadiusMultiplier,
               randomSource.nextFloat() * 0.5F + 0.5F,
               yaw + (float) (Math.PI / 2),
               pitch / 3.0F,
               j,
               branchCount,
               1.0,
               carvingMask,
               skipChecker
            );
            return;
         }

         if (randomSource.nextInt(4) != 0) {
            if (!canReach(chunk.getPos(), x, z, j, branchCount, thickness)) {
               return;
            }

            this.carveEllipsoid(
               context, config, chunk, biomeAccessor, aquifer, x, y, z, d * horizontalRadiusMultiplier, e * verticalRadiusMultiplier, carvingMask, skipChecker
            );
         }
      }
   }

   private static boolean shouldSkip(double relative, double relativeY, double relativeZ, double minrelativeY) {
      return relativeY <= minrelativeY ? true : relative * relative + relativeY * relativeY + relativeZ * relativeZ >= 1.0;
   }
}
