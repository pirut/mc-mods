package net.minecraft.world.level;

import com.google.common.base.Suppliers;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PathNavigationRegion implements BlockGetter, CollisionGetter {
   protected final int centerX;
   protected final int centerZ;
   protected final ChunkAccess[][] chunks;
   protected boolean allEmpty;
   protected final Level level;
   private final Supplier<Holder<Biome>> plains;

   public PathNavigationRegion(Level p_47164_, BlockPos p_47165_, BlockPos p_47166_) {
      this.level = p_47164_;
      this.plains = Suppliers.memoize(() -> p_47164_.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS));
      this.centerX = SectionPos.blockToSectionCoord(p_47165_.getX());
      this.centerZ = SectionPos.blockToSectionCoord(p_47165_.getZ());
      int $$3 = SectionPos.blockToSectionCoord(p_47166_.getX());
      int $$4 = SectionPos.blockToSectionCoord(p_47166_.getZ());
      this.chunks = new ChunkAccess[$$3 - this.centerX + 1][$$4 - this.centerZ + 1];
      ChunkSource $$5 = p_47164_.getChunkSource();
      this.allEmpty = true;

      for (int $$6 = this.centerX; $$6 <= $$3; $$6++) {
         for (int $$7 = this.centerZ; $$7 <= $$4; $$7++) {
            this.chunks[$$6 - this.centerX][$$7 - this.centerZ] = $$5.getChunkNow($$6, $$7);
         }
      }

      for (int $$8 = SectionPos.blockToSectionCoord(p_47165_.getX()); $$8 <= SectionPos.blockToSectionCoord(p_47166_.getX()); $$8++) {
         for (int $$9 = SectionPos.blockToSectionCoord(p_47165_.getZ()); $$9 <= SectionPos.blockToSectionCoord(p_47166_.getZ()); $$9++) {
            ChunkAccess $$10 = this.chunks[$$8 - this.centerX][$$9 - this.centerZ];
            if ($$10 != null && !$$10.isYSpaceEmpty(p_47165_.getY(), p_47166_.getY())) {
               this.allEmpty = false;
               return;
            }
         }
      }
   }

   private ChunkAccess getChunk(BlockPos p_47186_) {
      return this.getChunk(SectionPos.blockToSectionCoord(p_47186_.getX()), SectionPos.blockToSectionCoord(p_47186_.getZ()));
   }

   private ChunkAccess getChunk(int p_47168_, int p_47169_) {
      int $$2 = p_47168_ - this.centerX;
      int $$3 = p_47169_ - this.centerZ;
      if ($$2 >= 0 && $$2 < this.chunks.length && $$3 >= 0 && $$3 < this.chunks[$$2].length) {
         ChunkAccess $$4 = this.chunks[$$2][$$3];
         return (ChunkAccess)($$4 != null ? $$4 : new EmptyLevelChunk(this.level, new ChunkPos(p_47168_, p_47169_), this.plains.get()));
      } else {
         return new EmptyLevelChunk(this.level, new ChunkPos(p_47168_, p_47169_), this.plains.get());
      }
   }

   @Override
   public WorldBorder getWorldBorder() {
      return this.level.getWorldBorder();
   }

   @Override
   public BlockGetter getChunkForCollisions(int p_47173_, int p_47174_) {
      return this.getChunk(p_47173_, p_47174_);
   }

   @Override
   public List<VoxelShape> getEntityCollisions(@Nullable Entity p_186557_, AABB p_186558_) {
      return List.of();
   }

   @Nullable
   @Override
   public BlockEntity getBlockEntity(BlockPos p_47180_) {
      ChunkAccess $$1 = this.getChunk(p_47180_);
      return $$1.getBlockEntity(p_47180_);
   }

   @Override
   public BlockState getBlockState(BlockPos p_47188_) {
      if (this.isOutsideBuildHeight(p_47188_)) {
         return Blocks.AIR.defaultBlockState();
      } else {
         ChunkAccess $$1 = this.getChunk(p_47188_);
         return $$1.getBlockState(p_47188_);
      }
   }

   @Override
   public FluidState getFluidState(BlockPos p_47171_) {
      if (this.isOutsideBuildHeight(p_47171_)) {
         return Fluids.EMPTY.defaultFluidState();
      } else {
         ChunkAccess $$1 = this.getChunk(p_47171_);
         return $$1.getFluidState(p_47171_);
      }
   }

   @Override
   public int getMinBuildHeight() {
      return this.level.getMinBuildHeight();
   }

   @Override
   public int getHeight() {
      return this.level.getHeight();
   }

   public ProfilerFiller getProfiler() {
      return this.level.getProfiler();
   }
}
