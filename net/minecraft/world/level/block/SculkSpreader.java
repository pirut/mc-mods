package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class SculkSpreader {
   public static final int MAX_GROWTH_RATE_RADIUS = 24;
   public static final int MAX_CHARGE = 1000;
   public static final float MAX_DECAY_FACTOR = 0.5F;
   private static final int MAX_CURSORS = 32;
   public static final int SHRIEKER_PLACEMENT_RATE = 11;
   final boolean isWorldGeneration;
   private final TagKey<Block> replaceableBlocks;
   private final int growthSpawnCost;
   private final int noGrowthRadius;
   private final int chargeDecayRate;
   private final int additionalDecayRate;
   private List<SculkSpreader.ChargeCursor> cursors = new ArrayList<>();
   private static final Logger LOGGER = LogUtils.getLogger();

   public SculkSpreader(boolean p_222248_, TagKey<Block> p_222249_, int p_222250_, int p_222251_, int p_222252_, int p_222253_) {
      this.isWorldGeneration = p_222248_;
      this.replaceableBlocks = p_222249_;
      this.growthSpawnCost = p_222250_;
      this.noGrowthRadius = p_222251_;
      this.chargeDecayRate = p_222252_;
      this.additionalDecayRate = p_222253_;
   }

   public static SculkSpreader createLevelSpreader() {
      return new SculkSpreader(false, BlockTags.SCULK_REPLACEABLE, 10, 4, 10, 5);
   }

   public static SculkSpreader createWorldGenSpreader() {
      return new SculkSpreader(true, BlockTags.SCULK_REPLACEABLE_WORLD_GEN, 50, 1, 5, 10);
   }

   public TagKey<Block> replaceableBlocks() {
      return this.replaceableBlocks;
   }

   public int growthSpawnCost() {
      return this.growthSpawnCost;
   }

   public int noGrowthRadius() {
      return this.noGrowthRadius;
   }

   public int chargeDecayRate() {
      return this.chargeDecayRate;
   }

   public int additionalDecayRate() {
      return this.additionalDecayRate;
   }

   public boolean isWorldGeneration() {
      return this.isWorldGeneration;
   }

   @VisibleForTesting
   public List<SculkSpreader.ChargeCursor> getCursors() {
      return this.cursors;
   }

   public void clear() {
      this.cursors.clear();
   }

   public void load(CompoundTag p_222270_) {
      if (p_222270_.contains("cursors", 9)) {
         this.cursors.clear();
         List<SculkSpreader.ChargeCursor> $$1 = (List<SculkSpreader.ChargeCursor>)SculkSpreader.ChargeCursor.CODEC
            .listOf()
            .parse(new Dynamic(NbtOps.INSTANCE, p_222270_.getList("cursors", 10)))
            .resultOrPartial(LOGGER::error)
            .orElseGet(ArrayList::new);
         int $$2 = Math.min($$1.size(), 32);

         for (int $$3 = 0; $$3 < $$2; $$3++) {
            this.addCursor($$1.get($$3));
         }
      }
   }

   public void save(CompoundTag p_222276_) {
      SculkSpreader.ChargeCursor.CODEC
         .listOf()
         .encodeStart(NbtOps.INSTANCE, this.cursors)
         .resultOrPartial(LOGGER::error)
         .ifPresent(p_222273_ -> p_222276_.put("cursors", p_222273_));
   }

   public void addCursors(BlockPos p_222267_, int p_222268_) {
      while (p_222268_ > 0) {
         int $$2 = Math.min(p_222268_, 1000);
         this.addCursor(new SculkSpreader.ChargeCursor(p_222267_, $$2));
         p_222268_ -= $$2;
      }
   }

   private void addCursor(SculkSpreader.ChargeCursor p_222261_) {
      if (this.cursors.size() < 32) {
         this.cursors.add(p_222261_);
      }
   }

   public void updateCursors(LevelAccessor p_222256_, BlockPos p_222257_, RandomSource p_222258_, boolean p_222259_) {
      if (!this.cursors.isEmpty()) {
         List<SculkSpreader.ChargeCursor> $$4 = new ArrayList<>();
         Map<BlockPos, SculkSpreader.ChargeCursor> $$5 = new HashMap<>();
         Object2IntMap<BlockPos> $$6 = new Object2IntOpenHashMap();

         for (SculkSpreader.ChargeCursor $$7 : this.cursors) {
            $$7.update(p_222256_, p_222257_, p_222258_, this, p_222259_);
            if ($$7.charge <= 0) {
               p_222256_.levelEvent(3006, $$7.getPos(), 0);
            } else {
               BlockPos $$8 = $$7.getPos();
               $$6.computeInt($$8, (p_222264_, p_222265_) -> (p_222265_ == null ? 0 : p_222265_) + $$7.charge);
               SculkSpreader.ChargeCursor $$9 = $$5.get($$8);
               if ($$9 == null) {
                  $$5.put($$8, $$7);
                  $$4.add($$7);
               } else if (!this.isWorldGeneration() && $$7.charge + $$9.charge <= 1000) {
                  $$9.mergeWith($$7);
               } else {
                  $$4.add($$7);
                  if ($$7.charge < $$9.charge) {
                     $$5.put($$8, $$7);
                  }
               }
            }
         }

         ObjectIterator var16 = $$6.object2IntEntrySet().iterator();

         while (var16.hasNext()) {
            Entry<BlockPos> $$10 = (Entry<BlockPos>)var16.next();
            BlockPos $$11 = (BlockPos)$$10.getKey();
            int $$12 = $$10.getIntValue();
            SculkSpreader.ChargeCursor $$13 = $$5.get($$11);
            Collection<Direction> $$14 = $$13 == null ? null : $$13.getFacingData();
            if ($$12 > 0 && $$14 != null) {
               int $$15 = (int)(Math.log1p($$12) / 2.3F) + 1;
               int $$16 = ($$15 << 6) + MultifaceBlock.pack($$14);
               p_222256_.levelEvent(3006, $$11, $$16);
            }
         }

         this.cursors = $$4;
      }
   }

   public static class ChargeCursor {
      private static final ObjectArrayList<Vec3i> NON_CORNER_NEIGHBOURS = Util.make(
         new ObjectArrayList(18),
         p_222338_ -> BlockPos.betweenClosedStream(new BlockPos(-1, -1, -1), new BlockPos(1, 1, 1))
            .filter(p_222336_ -> (p_222336_.getX() == 0 || p_222336_.getY() == 0 || p_222336_.getZ() == 0) && !p_222336_.equals(BlockPos.ZERO))
            .map(BlockPos::immutable)
            .forEach(p_222338_::add)
      );
      public static final int MAX_CURSOR_DECAY_DELAY = 1;
      private BlockPos pos;
      int charge;
      private int updateDelay;
      private int decayDelay;
      @Nullable
      private Set<Direction> facings;
      private static final Codec<Set<Direction>> DIRECTION_SET = Direction.CODEC
         .listOf()
         .xmap(p_222340_ -> Sets.newEnumSet(p_222340_, Direction.class), Lists::newArrayList);
      public static final Codec<SculkSpreader.ChargeCursor> CODEC = RecordCodecBuilder.create(
         p_222330_ -> p_222330_.group(
               BlockPos.CODEC.fieldOf("pos").forGetter(SculkSpreader.ChargeCursor::getPos),
               Codec.intRange(0, 1000).fieldOf("charge").orElse(0).forGetter(SculkSpreader.ChargeCursor::getCharge),
               Codec.intRange(0, 1).fieldOf("decay_delay").orElse(1).forGetter(SculkSpreader.ChargeCursor::getDecayDelay),
               Codec.intRange(0, Integer.MAX_VALUE).fieldOf("update_delay").orElse(0).forGetter(p_222346_ -> p_222346_.updateDelay),
               DIRECTION_SET.lenientOptionalFieldOf("facings").forGetter(p_222343_ -> Optional.ofNullable(p_222343_.getFacingData()))
            )
            .apply(p_222330_, SculkSpreader.ChargeCursor::new)
      );

      private ChargeCursor(BlockPos p_222299_, int p_222300_, int p_222301_, int p_222302_, Optional<Set<Direction>> p_222303_) {
         this.pos = p_222299_;
         this.charge = p_222300_;
         this.decayDelay = p_222301_;
         this.updateDelay = p_222302_;
         this.facings = p_222303_.orElse(null);
      }

      public ChargeCursor(BlockPos p_222296_, int p_222297_) {
         this(p_222296_, p_222297_, 1, 0, Optional.empty());
      }

      public BlockPos getPos() {
         return this.pos;
      }

      public int getCharge() {
         return this.charge;
      }

      public int getDecayDelay() {
         return this.decayDelay;
      }

      @Nullable
      public Set<Direction> getFacingData() {
         return this.facings;
      }

      private boolean shouldUpdate(LevelAccessor p_222326_, BlockPos p_222327_, boolean p_222328_) {
         if (this.charge <= 0) {
            return false;
         } else if (p_222328_) {
            return true;
         } else {
            return p_222326_ instanceof ServerLevel $$3 ? $$3.shouldTickBlocksAt(p_222327_) : false;
         }
      }

      public void update(LevelAccessor p_222312_, BlockPos p_222313_, RandomSource p_222314_, SculkSpreader p_222315_, boolean p_222316_) {
         if (this.shouldUpdate(p_222312_, p_222313_, p_222315_.isWorldGeneration)) {
            if (this.updateDelay > 0) {
               this.updateDelay--;
            } else {
               BlockState $$5 = p_222312_.getBlockState(this.pos);
               SculkBehaviour $$6 = getBlockBehaviour($$5);
               if (p_222316_ && $$6.attemptSpreadVein(p_222312_, this.pos, $$5, this.facings, p_222315_.isWorldGeneration())) {
                  if ($$6.canChangeBlockStateOnSpread()) {
                     $$5 = p_222312_.getBlockState(this.pos);
                     $$6 = getBlockBehaviour($$5);
                  }

                  p_222312_.playSound(null, this.pos, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1.0F, 1.0F);
               }

               this.charge = $$6.attemptUseCharge(this, p_222312_, p_222313_, p_222314_, p_222315_, p_222316_);
               if (this.charge <= 0) {
                  $$6.onDischarged(p_222312_, $$5, this.pos, p_222314_);
               } else {
                  BlockPos $$7 = getValidMovementPos(p_222312_, this.pos, p_222314_);
                  if ($$7 != null) {
                     $$6.onDischarged(p_222312_, $$5, this.pos, p_222314_);
                     this.pos = $$7.immutable();
                     if (p_222315_.isWorldGeneration() && !this.pos.closerThan(new Vec3i(p_222313_.getX(), this.pos.getY(), p_222313_.getZ()), 15.0)) {
                        this.charge = 0;
                        return;
                     }

                     $$5 = p_222312_.getBlockState($$7);
                  }

                  if ($$5.getBlock() instanceof SculkBehaviour) {
                     this.facings = MultifaceBlock.availableFaces($$5);
                  }

                  this.decayDelay = $$6.updateDecayDelay(this.decayDelay);
                  this.updateDelay = $$6.getSculkSpreadDelay();
               }
            }
         }
      }

      void mergeWith(SculkSpreader.ChargeCursor p_222332_) {
         this.charge = this.charge + p_222332_.charge;
         p_222332_.charge = 0;
         this.updateDelay = Math.min(this.updateDelay, p_222332_.updateDelay);
      }

      private static SculkBehaviour getBlockBehaviour(BlockState p_222334_) {
         return p_222334_.getBlock() instanceof SculkBehaviour $$1 ? $$1 : SculkBehaviour.DEFAULT;
      }

      private static List<Vec3i> getRandomizedNonCornerNeighbourOffsets(RandomSource p_222306_) {
         return Util.shuffledCopy(NON_CORNER_NEIGHBOURS, p_222306_);
      }

      @Nullable
      private static BlockPos getValidMovementPos(LevelAccessor p_222308_, BlockPos p_222309_, RandomSource p_222310_) {
         BlockPos.MutableBlockPos $$3 = p_222309_.mutable();
         BlockPos.MutableBlockPos $$4 = p_222309_.mutable();

         for (Vec3i $$5 : getRandomizedNonCornerNeighbourOffsets(p_222310_)) {
            $$4.setWithOffset(p_222309_, $$5);
            BlockState $$6 = p_222308_.getBlockState($$4);
            if ($$6.getBlock() instanceof SculkBehaviour && isMovementUnobstructed(p_222308_, p_222309_, $$4)) {
               $$3.set($$4);
               if (SculkVeinBlock.hasSubstrateAccess(p_222308_, $$6, $$4)) {
                  break;
               }
            }
         }

         return $$3.equals(p_222309_) ? null : $$3;
      }

      private static boolean isMovementUnobstructed(LevelAccessor p_222318_, BlockPos p_222319_, BlockPos p_222320_) {
         if (p_222319_.distManhattan(p_222320_) == 1) {
            return true;
         } else {
            BlockPos $$3 = p_222320_.subtract(p_222319_);
            Direction $$4 = Direction.fromAxisAndDirection(
               Direction.Axis.X, $$3.getX() < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE
            );
            Direction $$5 = Direction.fromAxisAndDirection(
               Direction.Axis.Y, $$3.getY() < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE
            );
            Direction $$6 = Direction.fromAxisAndDirection(
               Direction.Axis.Z, $$3.getZ() < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE
            );
            if ($$3.getX() == 0) {
               return isUnobstructed(p_222318_, p_222319_, $$5) || isUnobstructed(p_222318_, p_222319_, $$6);
            } else {
               return $$3.getY() == 0
                  ? isUnobstructed(p_222318_, p_222319_, $$4) || isUnobstructed(p_222318_, p_222319_, $$6)
                  : isUnobstructed(p_222318_, p_222319_, $$4) || isUnobstructed(p_222318_, p_222319_, $$5);
            }
         }
      }

      private static boolean isUnobstructed(LevelAccessor p_222322_, BlockPos p_222323_, Direction p_222324_) {
         BlockPos $$3 = p_222323_.relative(p_222324_);
         return !p_222322_.getBlockState($$3).isFaceSturdy(p_222322_, $$3, p_222324_.getOpposite());
      }
   }
}
