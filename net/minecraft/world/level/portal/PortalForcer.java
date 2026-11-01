package net.minecraft.world.level.portal;

import java.util.Comparator;
import java.util.Optional;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;

public class PortalForcer {
   public static final int TICKET_RADIUS = 3;
   private static final int NETHER_PORTAL_RADIUS = 16;
   private static final int OVERWORLD_PORTAL_RADIUS = 128;
   private static final int FRAME_HEIGHT = 5;
   private static final int FRAME_WIDTH = 4;
   private static final int FRAME_BOX = 3;
   private static final int FRAME_HEIGHT_START = -1;
   private static final int FRAME_HEIGHT_END = 4;
   private static final int FRAME_WIDTH_START = -1;
   private static final int FRAME_WIDTH_END = 3;
   private static final int FRAME_BOX_START = -1;
   private static final int FRAME_BOX_END = 2;
   private static final int NOTHING_FOUND = -1;
   protected final ServerLevel level;

   public PortalForcer(ServerLevel p_77650_) {
      this.level = p_77650_;
   }

   public Optional<BlockPos> findClosestPortalPosition(BlockPos p_352378_, boolean p_352309_, WorldBorder p_352374_) {
      PoiManager $$3 = this.level.getPoiManager();
      int $$4 = p_352309_ ? 16 : 128;
      $$3.ensureLoadedAndValid(this.level, p_352378_, $$4);
      return $$3.getInSquare(p_230634_ -> p_230634_.is(PoiTypes.NETHER_PORTAL), p_352378_, $$4, PoiManager.Occupancy.ANY)
         .map(PoiRecord::getPos)
         .filter(p_352374_::isWithinBounds)
         .filter(p_352047_ -> this.level.getBlockState(p_352047_).hasProperty(BlockStateProperties.HORIZONTAL_AXIS))
         .min(Comparator.<BlockPos>comparingDouble(p_352046_ -> p_352046_.distSqr(p_352378_)).thenComparingInt(Vec3i::getY));
   }

   public Optional<BlockUtil.FoundRectangle> createPortal(BlockPos p_77667_, Direction.Axis p_77668_) {
      Direction $$2 = Direction.get(Direction.AxisDirection.POSITIVE, p_77668_);
      double $$3 = -1.0;
      BlockPos $$4 = null;
      double $$5 = -1.0;
      BlockPos $$6 = null;
      WorldBorder $$7 = this.level.getWorldBorder();
      int $$8 = Math.min(this.level.getMaxBuildHeight(), this.level.getMinBuildHeight() + this.level.getLogicalHeight()) - 1;
      int $$9 = 1;
      BlockPos.MutableBlockPos $$10 = p_77667_.mutable();

      for (BlockPos.MutableBlockPos $$11 : BlockPos.spiralAround(p_77667_, 16, Direction.EAST, Direction.SOUTH)) {
         int $$12 = Math.min($$8, this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, $$11.getX(), $$11.getZ()));
         if ($$7.isWithinBounds($$11) && $$7.isWithinBounds($$11.move($$2, 1))) {
            $$11.move($$2.getOpposite(), 1);

            for (int $$13 = $$12; $$13 >= this.level.getMinBuildHeight(); $$13--) {
               $$11.setY($$13);
               if (this.canPortalReplaceBlock($$11)) {
                  int $$14 = $$13;

                  while ($$13 > this.level.getMinBuildHeight() && this.canPortalReplaceBlock($$11.move(Direction.DOWN))) {
                     $$13--;
                  }

                  if ($$13 + 4 <= $$8) {
                     int $$15 = $$14 - $$13;
                     if ($$15 <= 0 || $$15 >= 3) {
                        $$11.setY($$13);
                        if (this.canHostFrame($$11, $$10, $$2, 0)) {
                           double $$16 = p_77667_.distSqr($$11);
                           if (this.canHostFrame($$11, $$10, $$2, -1) && this.canHostFrame($$11, $$10, $$2, 1) && ($$3 == -1.0 || $$3 > $$16)) {
                              $$3 = $$16;
                              $$4 = $$11.immutable();
                           }

                           if ($$3 == -1.0 && ($$5 == -1.0 || $$5 > $$16)) {
                              $$5 = $$16;
                              $$6 = $$11.immutable();
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if ($$3 == -1.0 && $$5 != -1.0) {
         $$4 = $$6;
         $$3 = $$5;
      }

      if ($$3 == -1.0) {
         int $$17 = Math.max(this.level.getMinBuildHeight() - -1, 70);
         int $$18 = $$8 - 9;
         if ($$18 < $$17) {
            return Optional.empty();
         }

         $$4 = new BlockPos(p_77667_.getX() - $$2.getStepX() * 1, Mth.clamp(p_77667_.getY(), $$17, $$18), p_77667_.getZ() - $$2.getStepZ() * 1).immutable();
         $$4 = $$7.clampToBounds($$4);
         Direction $$19 = $$2.getClockWise();

         for (int $$20 = -1; $$20 < 2; $$20++) {
            for (int $$21 = 0; $$21 < 2; $$21++) {
               for (int $$22 = -1; $$22 < 3; $$22++) {
                  BlockState $$23 = $$22 < 0 ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState();
                  $$10.setWithOffset($$4, $$21 * $$2.getStepX() + $$20 * $$19.getStepX(), $$22, $$21 * $$2.getStepZ() + $$20 * $$19.getStepZ());
                  this.level.setBlockAndUpdate($$10, $$23);
               }
            }
         }
      }

      for (int $$24 = -1; $$24 < 3; $$24++) {
         for (int $$25 = -1; $$25 < 4; $$25++) {
            if ($$24 == -1 || $$24 == 2 || $$25 == -1 || $$25 == 3) {
               $$10.setWithOffset($$4, $$24 * $$2.getStepX(), $$25, $$24 * $$2.getStepZ());
               this.level.setBlock($$10, Blocks.OBSIDIAN.defaultBlockState(), 3);
            }
         }
      }

      BlockState $$26 = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, p_77668_);

      for (int $$27 = 0; $$27 < 2; $$27++) {
         for (int $$28 = 0; $$28 < 3; $$28++) {
            $$10.setWithOffset($$4, $$27 * $$2.getStepX(), $$28, $$27 * $$2.getStepZ());
            this.level.setBlock($$10, $$26, 18);
         }
      }

      return Optional.of(new BlockUtil.FoundRectangle($$4.immutable(), 2, 3));
   }

   private boolean canPortalReplaceBlock(BlockPos.MutableBlockPos p_248971_) {
      BlockState $$1 = this.level.getBlockState(p_248971_);
      return $$1.canBeReplaced() && $$1.getFluidState().isEmpty();
   }

   private boolean canHostFrame(BlockPos p_77662_, BlockPos.MutableBlockPos p_77663_, Direction p_77664_, int p_77665_) {
      Direction $$4 = p_77664_.getClockWise();

      for (int $$5 = -1; $$5 < 3; $$5++) {
         for (int $$6 = -1; $$6 < 4; $$6++) {
            p_77663_.setWithOffset(p_77662_, p_77664_.getStepX() * $$5 + $$4.getStepX() * p_77665_, $$6, p_77664_.getStepZ() * $$5 + $$4.getStepZ() * p_77665_);
            if ($$6 < 0 && !this.level.getBlockState(p_77663_).isSolid()) {
               return false;
            }

            if ($$6 >= 0 && !this.canPortalReplaceBlock(p_77663_)) {
               return false;
            }
         }
      }

      return true;
   }
}
