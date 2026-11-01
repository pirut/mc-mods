package net.minecraft.client.renderer.chunk;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class RenderSectionRegion implements BlockAndTintGetter {
   public static final int RADIUS = 1;
   public static final int SIZE = 3;
   private final int minSectionX;
   private final int minSectionY;
   private final int minSectionZ;
   private final SectionCopy[] sections;
   private final Level level;

   RenderSectionRegion(Level level, int minSectionX, int minSectionY, int minSectionZ, SectionCopy[] sections) {
      this.level = level;
      this.minSectionX = minSectionX;
      this.minSectionY = minSectionY;
      this.minSectionZ = minSectionZ;
      this.sections = sections;
   }

   @Override
   public BlockState getBlockState(BlockPos pos) {
      return this.getSection(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getY()), SectionPos.blockToSectionCoord(pos.getZ()))
         .getBlockState(pos);
   }

   @Override
   public FluidState getFluidState(BlockPos pos) {
      return this.getSection(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getY()), SectionPos.blockToSectionCoord(pos.getZ()))
         .getBlockState(pos)
         .getFluidState();
   }

   @Override
   public float getShade(Direction direction, boolean shade) {
      return this.level.getShade(direction, shade);
   }

   @Override
   public LevelLightEngine getLightEngine() {
      return this.level.getLightEngine();
   }

   @Nullable
   @Override
   public BlockEntity getBlockEntity(BlockPos pos) {
      return this.getSection(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getY()), SectionPos.blockToSectionCoord(pos.getZ()))
         .getBlockEntity(pos);
   }

   private SectionCopy getSection(int x, int y, int z) {
      return this.sections[index(this.minSectionX, this.minSectionY, this.minSectionZ, x, y, z)];
   }

   @Override
   public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
      return this.level.getBlockTint(blockPos, colorResolver);
   }

   @Override
   public int getMinY() {
      return this.level.getMinY();
   }

   @Override
   public int getHeight() {
      return this.level.getHeight();
   }

   public static int index(int minX, int minY, int minZ, int x, int y, int z) {
      return x - minX + (y - minY) * 3 + (z - minZ) * 3 * 3;
   }
}
