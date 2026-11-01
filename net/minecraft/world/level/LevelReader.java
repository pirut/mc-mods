package net.minecraft.world.level;

import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public interface LevelReader extends BlockAndTintGetter, CollisionGetter, SignalGetter, BiomeManager.NoiseBiomeSource {
   @Nullable
   ChunkAccess getChunk(int x, int z, ChunkStatus chunkStatus, boolean requireChunk);

   @Deprecated
   boolean hasChunk(int chunkX, int chunkZ);

   int getHeight(Heightmap.Types heightmapType, int x, int z);

   default int getHeight(Heightmap.Types heightmapType, BlockPos pos) {
      return this.getHeight(heightmapType, pos.getX(), pos.getZ());
   }

   int getSkyDarken();

   BiomeManager getBiomeManager();

   default Holder<Biome> getBiome(BlockPos pos) {
      return this.getBiomeManager().getBiome(pos);
   }

   default Stream<BlockState> getBlockStatesIfLoaded(AABB aabb) {
      int i = Mth.floor(aabb.minX);
      int j = Mth.floor(aabb.maxX);
      int k = Mth.floor(aabb.minY);
      int l = Mth.floor(aabb.maxY);
      int m = Mth.floor(aabb.minZ);
      int n = Mth.floor(aabb.maxZ);
      return this.hasChunksAt(i, k, m, j, l, n) ? this.getBlockStates(aabb) : Stream.empty();
   }

   @Override
   default int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
      return colorResolver.getColor(this.getBiome(blockPos).value(), blockPos.getX(), blockPos.getZ());
   }

   @Override
   default Holder<Biome> getNoiseBiome(int i, int j, int k) {
      ChunkAccess chunkAccess = this.getChunk(QuartPos.toSection(i), QuartPos.toSection(k), ChunkStatus.BIOMES, false);
      return chunkAccess != null ? chunkAccess.getNoiseBiome(i, j, k) : this.getUncachedNoiseBiome(i, j, k);
   }

   Holder<Biome> getUncachedNoiseBiome(int x, int y, int z);

   boolean isClientSide();

   int getSeaLevel();

   DimensionType dimensionType();

   @Override
   default int getMinY() {
      return this.dimensionType().minY();
   }

   @Override
   default int getHeight() {
      return this.dimensionType().height();
   }

   default BlockPos getHeightmapPos(Heightmap.Types heightmapType, BlockPos pos) {
      return new BlockPos(pos.getX(), this.getHeight(heightmapType, pos.getX(), pos.getZ()), pos.getZ());
   }

   default boolean isEmptyBlock(BlockPos pos) {
      return this.getBlockState(pos).isAir();
   }

   default boolean canSeeSkyFromBelowWater(BlockPos pos) {
      if (pos.getY() >= this.getSeaLevel()) {
         return this.canSeeSky(pos);
      } else {
         BlockPos blockPos = new BlockPos(pos.getX(), this.getSeaLevel(), pos.getZ());
         if (!this.canSeeSky(blockPos)) {
            return false;
         } else {
            for (BlockPos var4 = blockPos.below(); var4.getY() > pos.getY(); var4 = var4.below()) {
               BlockState blockState = this.getBlockState(var4);
               if (blockState.getLightBlock() > 0 && !blockState.liquid()) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   default float getPathfindingCostFromLightLevels(BlockPos pos) {
      return this.getLightLevelDependentMagicValue(pos) - 0.5F;
   }

   @Deprecated
   default float getLightLevelDependentMagicValue(BlockPos pos) {
      float f = this.getMaxLocalRawBrightness(pos) / 15.0F;
      float g = f / (4.0F - 3.0F * f);
      return Mth.lerp(this.dimensionType().ambientLight(), g, 1.0F);
   }

   default ChunkAccess getChunk(BlockPos pos) {
      return this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
   }

   default ChunkAccess getChunk(int chunkX, int chunkZ) {
      return this.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
   }

   default ChunkAccess getChunk(int chunkX, int chunkZ, ChunkStatus chunkStatus) {
      return this.getChunk(chunkX, chunkZ, chunkStatus, true);
   }

   @Nullable
   @Override
   default BlockGetter getChunkForCollisions(int chunkX, int chunkZ) {
      return this.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, false);
   }

   default boolean isWaterAt(BlockPos pos) {
      return this.getFluidState(pos).is(FluidTags.WATER);
   }

   default boolean containsAnyLiquid(AABB bb) {
      int i = Mth.floor(bb.minX);
      int j = Mth.ceil(bb.maxX);
      int k = Mth.floor(bb.minY);
      int l = Mth.ceil(bb.maxY);
      int m = Mth.floor(bb.minZ);
      int n = Mth.ceil(bb.maxZ);
      BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

      for (int o = i; o < j; o++) {
         for (int p = k; p < l; p++) {
            for (int q = m; q < n; q++) {
               BlockState blockState = this.getBlockState(mutableBlockPos.set(o, p, q));
               if (!blockState.getFluidState().isEmpty()) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   default int getMaxLocalRawBrightness(BlockPos pos) {
      return this.getMaxLocalRawBrightness(pos, this.getSkyDarken());
   }

   default int getMaxLocalRawBrightness(BlockPos pos, int amount) {
      return pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000 ? this.getRawBrightness(pos, amount) : 15;
   }

   @Deprecated
   default boolean hasChunkAt(int x, int z) {
      return this.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
   }

   @Deprecated
   default boolean hasChunkAt(BlockPos pos) {
      return this.hasChunkAt(pos.getX(), pos.getZ());
   }

   @Deprecated
   default boolean hasChunksAt(BlockPos from, BlockPos to) {
      return this.hasChunksAt(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ());
   }

   @Deprecated
   default boolean hasChunksAt(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
      return toY >= this.getMinY() && fromY <= this.getMaxY() ? this.hasChunksAt(fromX, fromZ, toX, toZ) : false;
   }

   @Deprecated
   default boolean hasChunksAt(int fromX, int fromZ, int toX, int toZ) {
      int i = SectionPos.blockToSectionCoord(fromX);
      int j = SectionPos.blockToSectionCoord(toX);
      int k = SectionPos.blockToSectionCoord(fromZ);
      int l = SectionPos.blockToSectionCoord(toZ);

      for (int m = i; m <= j; m++) {
         for (int n = k; n <= l; n++) {
            if (!this.hasChunk(m, n)) {
               return false;
            }
         }
      }

      return true;
   }

   RegistryAccess registryAccess();

   FeatureFlagSet enabledFeatures();

   default <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<? extends T>> registryKey) {
      Registry<T> registry = this.registryAccess().lookupOrThrow(registryKey);
      return registry.filterFeatures(this.enabledFeatures());
   }

   EnvironmentAttributeReader environmentAttributes();
}
