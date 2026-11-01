package net.minecraft.client.renderer.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelDataManager;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RenderSectionRegion implements BlockAndTintGetter {
   public static final int RADIUS = 1;
   public static final int SIZE = 3;
   private final int minSectionX;
   private final int minSectionY;
   private final int minSectionZ;
   private final SectionCopy[] sections;
   private final Level level;
   private final Long2ObjectFunction<ModelData> modelDataSnapshot;

   @Deprecated
   RenderSectionRegion(Level p_427508_, int p_427409_, int p_427310_, int p_427483_, SectionCopy[] p_427286_) {
      this(p_427508_, p_427409_, p_427310_, p_427483_, p_427286_, ModelDataManager.EMPTY_SNAPSHOT);
   }

   RenderSectionRegion(Level p_427508_, int p_427409_, int p_427310_, int p_427483_, SectionCopy[] p_427286_, Long2ObjectFunction<ModelData> modelDataSnapshot) {
      this.level = p_427508_;
      this.minSectionX = p_427409_;
      this.minSectionY = p_427310_;
      this.minSectionZ = p_427483_;
      this.sections = p_427286_;
      this.modelDataSnapshot = modelDataSnapshot;
   }

   @Override
   public BlockState getBlockState(BlockPos p_427463_) {
      return this.getSection(
            SectionPos.blockToSectionCoord(p_427463_.getX()),
            SectionPos.blockToSectionCoord(p_427463_.getY()),
            SectionPos.blockToSectionCoord(p_427463_.getZ())
         )
         .getBlockState(p_427463_);
   }

   @Override
   public FluidState getFluidState(BlockPos p_427285_) {
      return this.getSection(
            SectionPos.blockToSectionCoord(p_427285_.getX()),
            SectionPos.blockToSectionCoord(p_427285_.getY()),
            SectionPos.blockToSectionCoord(p_427285_.getZ())
         )
         .getBlockState(p_427285_)
         .getFluidState();
   }

   @Override
   public float getShade(Direction p_427438_, boolean p_427509_) {
      return this.level.getShade(p_427438_, p_427509_);
   }

   @Override
   public LevelLightEngine getLightEngine() {
      return this.level.getLightEngine();
   }

   @Nullable
   @Override
   public BlockEntity getBlockEntity(BlockPos p_427443_) {
      return this.getSection(
            SectionPos.blockToSectionCoord(p_427443_.getX()),
            SectionPos.blockToSectionCoord(p_427443_.getY()),
            SectionPos.blockToSectionCoord(p_427443_.getZ())
         )
         .getBlockEntity(p_427443_);
   }

   private SectionCopy getSection(int p_427464_, int p_427322_, int p_427454_) {
      return this.sections[index(this.minSectionX, this.minSectionY, this.minSectionZ, p_427464_, p_427322_, p_427454_)];
   }

   @Override
   public int getBlockTint(BlockPos p_427329_, ColorResolver p_427340_) {
      return this.level.getBlockTint(p_427329_, p_427340_);
   }

   @Override
   public int getMinY() {
      return this.level.getMinY();
   }

   @Override
   public int getHeight() {
      return this.level.getHeight();
   }

   @Override
   public float getShade(float normalX, float normalY, float normalZ, boolean shade) {
      return this.level.getShade(normalX, normalY, normalZ, shade);
   }

   @Override
   public ModelData getModelData(BlockPos pos) {
      return (ModelData)this.modelDataSnapshot.get(pos.asLong());
   }

   @Override
   public AuxiliaryLightManager getAuxLightManager(ChunkPos pos) {
      return this.getSection(pos.x, this.minSectionY, pos.z).wrapped.getAuxLightManager(pos);
   }

   public static int index(int p_427335_, int p_427442_, int p_427497_, int p_427301_, int p_427248_, int p_427503_) {
      return p_427301_ - p_427335_ + (p_427248_ - p_427442_) * 3 + (p_427503_ - p_427497_) * 3 * 3;
   }
}
