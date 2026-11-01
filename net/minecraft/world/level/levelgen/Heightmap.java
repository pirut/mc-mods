package net.minecraft.world.level.levelgen;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.BitStorage;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.slf4j.Logger;

public class Heightmap {
   private static final Logger LOGGER = LogUtils.getLogger();
   static final Predicate<BlockState> NOT_AIR = p_284913_ -> !p_284913_.isAir();
   static final Predicate<BlockState> MATERIAL_MOTION_BLOCKING = BlockBehaviour.BlockStateBase::blocksMotion;
   private final BitStorage data;
   private final Predicate<BlockState> isOpaque;
   private final ChunkAccess chunk;

   public Heightmap(ChunkAccess p_64237_, Heightmap.Types p_64238_) {
      this.isOpaque = p_64238_.isOpaque();
      this.chunk = p_64237_;
      int $$2 = Mth.ceillog2(p_64237_.getHeight() + 1);
      this.data = new SimpleBitStorage($$2, 256);
   }

   public static void primeHeightmaps(ChunkAccess p_64257_, Set<Heightmap.Types> p_64258_) {
      int $$2 = p_64258_.size();
      ObjectList<Heightmap> $$3 = new ObjectArrayList($$2);
      ObjectListIterator<Heightmap> $$4 = $$3.iterator();
      int $$5 = p_64257_.getHighestSectionPosition() + 16;
      BlockPos.MutableBlockPos $$6 = new BlockPos.MutableBlockPos();

      for (int $$7 = 0; $$7 < 16; $$7++) {
         for (int $$8 = 0; $$8 < 16; $$8++) {
            for (Heightmap.Types $$9 : p_64258_) {
               $$3.add(p_64257_.getOrCreateHeightmapUnprimed($$9));
            }

            for (int $$10 = $$5 - 1; $$10 >= p_64257_.getMinBuildHeight(); $$10--) {
               $$6.set($$7, $$10, $$8);
               BlockState $$11 = p_64257_.getBlockState($$6);
               if (!$$11.is(Blocks.AIR)) {
                  while ($$4.hasNext()) {
                     Heightmap $$12 = (Heightmap)$$4.next();
                     if ($$12.isOpaque.test($$11)) {
                        $$12.setHeight($$7, $$8, $$10 + 1);
                        $$4.remove();
                     }
                  }

                  if ($$3.isEmpty()) {
                     break;
                  }

                  $$4.back($$2);
               }
            }
         }
      }
   }

   public boolean update(int p_64250_, int p_64251_, int p_64252_, BlockState p_64253_) {
      int $$4 = this.getFirstAvailable(p_64250_, p_64252_);
      if (p_64251_ <= $$4 - 2) {
         return false;
      } else {
         if (this.isOpaque.test(p_64253_)) {
            if (p_64251_ >= $$4) {
               this.setHeight(p_64250_, p_64252_, p_64251_ + 1);
               return true;
            }
         } else if ($$4 - 1 == p_64251_) {
            BlockPos.MutableBlockPos $$5 = new BlockPos.MutableBlockPos();

            for (int $$6 = p_64251_ - 1; $$6 >= this.chunk.getMinBuildHeight(); $$6--) {
               $$5.set(p_64250_, $$6, p_64252_);
               if (this.isOpaque.test(this.chunk.getBlockState($$5))) {
                  this.setHeight(p_64250_, p_64252_, $$6 + 1);
                  return true;
               }
            }

            this.setHeight(p_64250_, p_64252_, this.chunk.getMinBuildHeight());
            return true;
         }

         return false;
      }
   }

   public int getFirstAvailable(int p_64243_, int p_64244_) {
      return this.getFirstAvailable(getIndex(p_64243_, p_64244_));
   }

   public int getHighestTaken(int p_158369_, int p_158370_) {
      return this.getFirstAvailable(getIndex(p_158369_, p_158370_)) - 1;
   }

   private int getFirstAvailable(int p_64241_) {
      return this.data.get(p_64241_) + this.chunk.getMinBuildHeight();
   }

   private void setHeight(int p_64246_, int p_64247_, int p_64248_) {
      this.data.set(getIndex(p_64246_, p_64247_), p_64248_ - this.chunk.getMinBuildHeight());
   }

   public void setRawData(ChunkAccess p_158365_, Heightmap.Types p_158366_, long[] p_158367_) {
      long[] $$3 = this.data.getRaw();
      if ($$3.length == p_158367_.length) {
         System.arraycopy(p_158367_, 0, $$3, 0, p_158367_.length);
      } else {
         LOGGER.warn(
            "Ignoring heightmap data for chunk " + p_158365_.getPos() + ", size does not match; expected: " + $$3.length + ", got: " + p_158367_.length
         );
         primeHeightmaps(p_158365_, EnumSet.of(p_158366_));
      }
   }

   public long[] getRawData() {
      return this.data.getRaw();
   }

   private static int getIndex(int p_64266_, int p_64267_) {
      return p_64266_ + p_64267_ * 16;
   }

   public static enum Types implements StringRepresentable {
      WORLD_SURFACE_WG("WORLD_SURFACE_WG", Heightmap.Usage.WORLDGEN, Heightmap.NOT_AIR),
      WORLD_SURFACE("WORLD_SURFACE", Heightmap.Usage.CLIENT, Heightmap.NOT_AIR),
      OCEAN_FLOOR_WG("OCEAN_FLOOR_WG", Heightmap.Usage.WORLDGEN, Heightmap.MATERIAL_MOTION_BLOCKING),
      OCEAN_FLOOR("OCEAN_FLOOR", Heightmap.Usage.LIVE_WORLD, Heightmap.MATERIAL_MOTION_BLOCKING),
      MOTION_BLOCKING("MOTION_BLOCKING", Heightmap.Usage.CLIENT, p_284915_ -> p_284915_.blocksMotion() || !p_284915_.getFluidState().isEmpty()),
      MOTION_BLOCKING_NO_LEAVES(
         "MOTION_BLOCKING_NO_LEAVES",
         Heightmap.Usage.LIVE_WORLD,
         p_284914_ -> (p_284914_.blocksMotion() || !p_284914_.getFluidState().isEmpty()) && !(p_284914_.getBlock() instanceof LeavesBlock)
      );

      public static final Codec<Heightmap.Types> CODEC = StringRepresentable.fromEnum(Heightmap.Types::values);
      private final String serializationKey;
      private final Heightmap.Usage usage;
      private final Predicate<BlockState> isOpaque;

      private Types(String p_64284_, Heightmap.Usage p_64285_, Predicate<BlockState> p_64286_) {
         this.serializationKey = p_64284_;
         this.usage = p_64285_;
         this.isOpaque = p_64286_;
      }

      public String getSerializationKey() {
         return this.serializationKey;
      }

      public boolean sendToClient() {
         return this.usage == Heightmap.Usage.CLIENT;
      }

      public boolean keepAfterWorldgen() {
         return this.usage != Heightmap.Usage.WORLDGEN;
      }

      public Predicate<BlockState> isOpaque() {
         return this.isOpaque;
      }

      @Override
      public String getSerializedName() {
         return this.serializationKey;
      }
   }

   public static enum Usage {
      WORLDGEN,
      LIVE_WORLD,
      CLIENT;
   }
}
