package net.minecraft.world.level;

import com.google.common.collect.AbstractIterator;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockCollisions<T> extends AbstractIterator<T> {
   private final AABB box;
   private final CollisionContext context;
   private final Cursor3D cursor;
   private final BlockPos.MutableBlockPos pos;
   private final VoxelShape entityShape;
   private final CollisionGetter collisionGetter;
   private final boolean onlySuffocatingBlocks;
   @Nullable
   private BlockGetter cachedBlockGetter;
   private long cachedBlockGetterPos;
   private final BiFunction<BlockPos.MutableBlockPos, VoxelShape, T> resultProvider;

   public BlockCollisions(
      CollisionGetter p_286817_, @Nullable Entity p_286246_, AABB p_286624_, boolean p_286354_, BiFunction<BlockPos.MutableBlockPos, VoxelShape, T> p_286303_
   ) {
      this.context = p_286246_ == null ? CollisionContext.empty() : CollisionContext.of(p_286246_);
      this.pos = new BlockPos.MutableBlockPos();
      this.entityShape = Shapes.create(p_286624_);
      this.collisionGetter = p_286817_;
      this.box = p_286624_;
      this.onlySuffocatingBlocks = p_286354_;
      this.resultProvider = p_286303_;
      int $$5 = Mth.floor(p_286624_.minX - 1.0E-7) - 1;
      int $$6 = Mth.floor(p_286624_.maxX + 1.0E-7) + 1;
      int $$7 = Mth.floor(p_286624_.minY - 1.0E-7) - 1;
      int $$8 = Mth.floor(p_286624_.maxY + 1.0E-7) + 1;
      int $$9 = Mth.floor(p_286624_.minZ - 1.0E-7) - 1;
      int $$10 = Mth.floor(p_286624_.maxZ + 1.0E-7) + 1;
      this.cursor = new Cursor3D($$5, $$7, $$9, $$6, $$8, $$10);
   }

   @Nullable
   private BlockGetter getChunk(int p_186412_, int p_186413_) {
      int $$2 = SectionPos.blockToSectionCoord(p_186412_);
      int $$3 = SectionPos.blockToSectionCoord(p_186413_);
      long $$4 = ChunkPos.asLong($$2, $$3);
      if (this.cachedBlockGetter != null && this.cachedBlockGetterPos == $$4) {
         return this.cachedBlockGetter;
      } else {
         BlockGetter $$5 = this.collisionGetter.getChunkForCollisions($$2, $$3);
         this.cachedBlockGetter = $$5;
         this.cachedBlockGetterPos = $$4;
         return $$5;
      }
   }

   protected T computeNext() {
      while (this.cursor.advance()) {
         int $$0 = this.cursor.nextX();
         int $$1 = this.cursor.nextY();
         int $$2 = this.cursor.nextZ();
         int $$3 = this.cursor.getNextType();
         if ($$3 != 3) {
            BlockGetter $$4 = this.getChunk($$0, $$2);
            if ($$4 != null) {
               this.pos.set($$0, $$1, $$2);
               BlockState $$5 = $$4.getBlockState(this.pos);
               if ((!this.onlySuffocatingBlocks || $$5.isSuffocating($$4, this.pos))
                  && ($$3 != 1 || $$5.hasLargeCollisionShape())
                  && ($$3 != 2 || $$5.is(Blocks.MOVING_PISTON))) {
                  VoxelShape $$6 = $$5.getCollisionShape(this.collisionGetter, this.pos, this.context);
                  if ($$6 == Shapes.block()) {
                     if (this.box.intersects($$0, $$1, $$2, $$0 + 1.0, $$1 + 1.0, $$2 + 1.0)) {
                        return this.resultProvider.apply(this.pos, $$6.move($$0, $$1, $$2));
                     }
                  } else {
                     VoxelShape $$7 = $$6.move($$0, $$1, $$2);
                     if (!$$7.isEmpty() && Shapes.joinIsNotEmpty($$7, this.entityShape, BooleanOp.AND)) {
                        return this.resultProvider.apply(this.pos, $$7);
                     }
                  }
               }
            }
         }
      }

      return (T)this.endOfData();
   }
}
