package net.minecraft.world.level.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction8;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ticks.SavedTick;
import org.slf4j.Logger;

public class UpgradeData {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final UpgradeData EMPTY = new UpgradeData(EmptyBlockGetter.INSTANCE);
   private static final String TAG_INDICES = "Indices";
   private static final Direction8[] DIRECTIONS = Direction8.values();
   private final EnumSet<Direction8> sides = EnumSet.noneOf(Direction8.class);
   private final List<SavedTick<Block>> neighborBlockTicks = Lists.newArrayList();
   private final List<SavedTick<Fluid>> neighborFluidTicks = Lists.newArrayList();
   private final int[][] index;
   static final Map<Block, UpgradeData.BlockFixer> MAP = new IdentityHashMap<>();
   static final Set<UpgradeData.BlockFixer> CHUNKY_FIXERS = Sets.newHashSet();

   private UpgradeData(LevelHeightAccessor p_156506_) {
      this.index = new int[p_156506_.getSectionsCount()][];
   }

   public UpgradeData(CompoundTag p_156508_, LevelHeightAccessor p_156509_) {
      this(p_156509_);
      if (p_156508_.contains("Indices", 10)) {
         CompoundTag $$2 = p_156508_.getCompound("Indices");

         for (int $$3 = 0; $$3 < this.index.length; $$3++) {
            String $$4 = String.valueOf($$3);
            if ($$2.contains($$4, 11)) {
               this.index[$$3] = $$2.getIntArray($$4);
            }
         }
      }

      int $$5 = p_156508_.getInt("Sides");

      for (Direction8 $$6 : Direction8.values()) {
         if (($$5 & 1 << $$6.ordinal()) != 0) {
            this.sides.add($$6);
         }
      }

      loadTicks(
         p_156508_,
         "neighbor_block_ticks",
         p_258983_ -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryParse(p_258983_)).or(() -> Optional.of(Blocks.AIR)),
         this.neighborBlockTicks
      );
      loadTicks(
         p_156508_,
         "neighbor_fluid_ticks",
         p_258986_ -> BuiltInRegistries.FLUID.getOptional(ResourceLocation.tryParse(p_258986_)).or(() -> Optional.of(Fluids.EMPTY)),
         this.neighborFluidTicks
      );
   }

   private static <T> void loadTicks(CompoundTag p_208133_, String p_208134_, Function<String, Optional<T>> p_208135_, List<SavedTick<T>> p_208136_) {
      if (p_208133_.contains(p_208134_, 9)) {
         for (Tag $$5 : p_208133_.getList(p_208134_, 10)) {
            SavedTick.loadTick((CompoundTag)$$5, p_208135_).ifPresent(p_208136_::add);
         }
      }
   }

   public void upgrade(LevelChunk p_63342_) {
      this.upgradeInside(p_63342_);

      for (Direction8 $$1 : DIRECTIONS) {
         upgradeSides(p_63342_, $$1);
      }

      Level $$2 = p_63342_.getLevel();
      this.neighborBlockTicks.forEach(p_208142_ -> {
         Block $$2x = p_208142_.type() == Blocks.AIR ? $$2.getBlockState(p_208142_.pos()).getBlock() : p_208142_.type();
         $$2.scheduleTick(p_208142_.pos(), $$2x, p_208142_.delay(), p_208142_.priority());
      });
      this.neighborFluidTicks.forEach(p_208125_ -> {
         Fluid $$2x = p_208125_.type() == Fluids.EMPTY ? $$2.getFluidState(p_208125_.pos()).getType() : p_208125_.type();
         $$2.scheduleTick(p_208125_.pos(), $$2x, p_208125_.delay(), p_208125_.priority());
      });
      CHUNKY_FIXERS.forEach(p_208122_ -> p_208122_.processChunk($$2));
   }

   private static void upgradeSides(LevelChunk p_63344_, Direction8 p_63345_) {
      Level $$2 = p_63344_.getLevel();
      if (p_63344_.getUpgradeData().sides.remove(p_63345_)) {
         Set<Direction> $$3 = p_63345_.getDirections();
         int $$4 = 0;
         int $$5 = 15;
         boolean $$6 = $$3.contains(Direction.EAST);
         boolean $$7 = $$3.contains(Direction.WEST);
         boolean $$8 = $$3.contains(Direction.SOUTH);
         boolean $$9 = $$3.contains(Direction.NORTH);
         boolean $$10 = $$3.size() == 1;
         ChunkPos $$11 = p_63344_.getPos();
         int $$12 = $$11.getMinBlockX() + (!$$10 || !$$9 && !$$8 ? ($$7 ? 0 : 15) : 1);
         int $$13 = $$11.getMinBlockX() + (!$$10 || !$$9 && !$$8 ? ($$7 ? 0 : 15) : 14);
         int $$14 = $$11.getMinBlockZ() + (!$$10 || !$$6 && !$$7 ? ($$9 ? 0 : 15) : 1);
         int $$15 = $$11.getMinBlockZ() + (!$$10 || !$$6 && !$$7 ? ($$9 ? 0 : 15) : 14);
         Direction[] $$16 = Direction.values();
         BlockPos.MutableBlockPos $$17 = new BlockPos.MutableBlockPos();

         for (BlockPos $$18 : BlockPos.betweenClosed($$12, $$2.getMinBuildHeight(), $$14, $$13, $$2.getMaxBuildHeight() - 1, $$15)) {
            BlockState $$19 = $$2.getBlockState($$18);
            BlockState $$20 = $$19;

            for (Direction $$21 : $$16) {
               $$17.setWithOffset($$18, $$21);
               $$20 = updateState($$20, $$21, $$2, $$18, $$17);
            }

            Block.updateOrDestroy($$19, $$20, $$2, $$18, 18);
         }
      }
   }

   private static BlockState updateState(BlockState p_63336_, Direction p_63337_, LevelAccessor p_63338_, BlockPos p_63339_, BlockPos p_63340_) {
      return MAP.getOrDefault(p_63336_.getBlock(), UpgradeData.BlockFixers.DEFAULT)
         .updateShape(p_63336_, p_63337_, p_63338_.getBlockState(p_63340_), p_63338_, p_63339_, p_63340_);
   }

   private void upgradeInside(LevelChunk p_63348_) {
      BlockPos.MutableBlockPos $$1 = new BlockPos.MutableBlockPos();
      BlockPos.MutableBlockPos $$2 = new BlockPos.MutableBlockPos();
      ChunkPos $$3 = p_63348_.getPos();
      LevelAccessor $$4 = p_63348_.getLevel();

      for (int $$5 = 0; $$5 < this.index.length; $$5++) {
         LevelChunkSection $$6 = p_63348_.getSection($$5);
         int[] $$7 = this.index[$$5];
         this.index[$$5] = null;
         if ($$7 != null && $$7.length > 0) {
            Direction[] $$8 = Direction.values();
            PalettedContainer<BlockState> $$9 = $$6.getStates();
            int $$10 = p_63348_.getSectionYFromSectionIndex($$5);
            int $$11 = SectionPos.sectionToBlockCoord($$10);

            for (int $$12 : $$7) {
               int $$13 = $$12 & 15;
               int $$14 = $$12 >> 8 & 15;
               int $$15 = $$12 >> 4 & 15;
               $$1.set($$3.getMinBlockX() + $$13, $$11 + $$14, $$3.getMinBlockZ() + $$15);
               BlockState $$16 = $$9.get($$12);
               BlockState $$17 = $$16;

               for (Direction $$18 : $$8) {
                  $$2.setWithOffset($$1, $$18);
                  if (SectionPos.blockToSectionCoord($$1.getX()) == $$3.x && SectionPos.blockToSectionCoord($$1.getZ()) == $$3.z) {
                     $$17 = updateState($$17, $$18, $$4, $$1, $$2);
                  }
               }

               Block.updateOrDestroy($$16, $$17, $$4, $$1, 18);
            }
         }
      }

      for (int $$19 = 0; $$19 < this.index.length; $$19++) {
         if (this.index[$$19] != null) {
            LOGGER.warn("Discarding update data for section {} for chunk ({} {})", new Object[]{$$4.getSectionYFromSectionIndex($$19), $$3.x, $$3.z});
         }

         this.index[$$19] = null;
      }
   }

   public boolean isEmpty() {
      for (int[] $$0 : this.index) {
         if ($$0 != null) {
            return false;
         }
      }

      return this.sides.isEmpty();
   }

   public CompoundTag write() {
      CompoundTag $$0 = new CompoundTag();
      CompoundTag $$1 = new CompoundTag();

      for (int $$2 = 0; $$2 < this.index.length; $$2++) {
         String $$3 = String.valueOf($$2);
         if (this.index[$$2] != null && this.index[$$2].length != 0) {
            $$1.putIntArray($$3, this.index[$$2]);
         }
      }

      if (!$$1.isEmpty()) {
         $$0.put("Indices", $$1);
      }

      int $$4 = 0;

      for (Direction8 $$5 : this.sides) {
         $$4 |= 1 << $$5.ordinal();
      }

      $$0.putByte("Sides", (byte)$$4);
      if (!this.neighborBlockTicks.isEmpty()) {
         ListTag $$6 = new ListTag();
         this.neighborBlockTicks.forEach(p_208147_ -> $$6.add(p_208147_.save(p_258984_ -> BuiltInRegistries.BLOCK.getKey(p_258984_).toString())));
         $$0.put("neighbor_block_ticks", $$6);
      }

      if (!this.neighborFluidTicks.isEmpty()) {
         ListTag $$7 = new ListTag();
         this.neighborFluidTicks.forEach(p_208139_ -> $$7.add(p_208139_.save(p_258985_ -> BuiltInRegistries.FLUID.getKey(p_258985_).toString())));
         $$0.put("neighbor_fluid_ticks", $$7);
      }

      return $$0;
   }

   public interface BlockFixer {
      BlockState updateShape(BlockState var1, Direction var2, BlockState var3, LevelAccessor var4, BlockPos var5, BlockPos var6);

      default void processChunk(LevelAccessor p_63351_) {
      }
   }

   static enum BlockFixers implements UpgradeData.BlockFixer {
      BLACKLIST(
         Blocks.OBSERVER,
         Blocks.NETHER_PORTAL,
         Blocks.WHITE_CONCRETE_POWDER,
         Blocks.ORANGE_CONCRETE_POWDER,
         Blocks.MAGENTA_CONCRETE_POWDER,
         Blocks.LIGHT_BLUE_CONCRETE_POWDER,
         Blocks.YELLOW_CONCRETE_POWDER,
         Blocks.LIME_CONCRETE_POWDER,
         Blocks.PINK_CONCRETE_POWDER,
         Blocks.GRAY_CONCRETE_POWDER,
         Blocks.LIGHT_GRAY_CONCRETE_POWDER,
         Blocks.CYAN_CONCRETE_POWDER,
         Blocks.PURPLE_CONCRETE_POWDER,
         Blocks.BLUE_CONCRETE_POWDER,
         Blocks.BROWN_CONCRETE_POWDER,
         Blocks.GREEN_CONCRETE_POWDER,
         Blocks.RED_CONCRETE_POWDER,
         Blocks.BLACK_CONCRETE_POWDER,
         Blocks.ANVIL,
         Blocks.CHIPPED_ANVIL,
         Blocks.DAMAGED_ANVIL,
         Blocks.DRAGON_EGG,
         Blocks.GRAVEL,
         Blocks.SAND,
         Blocks.RED_SAND,
         Blocks.OAK_SIGN,
         Blocks.SPRUCE_SIGN,
         Blocks.BIRCH_SIGN,
         Blocks.ACACIA_SIGN,
         Blocks.CHERRY_SIGN,
         Blocks.JUNGLE_SIGN,
         Blocks.DARK_OAK_SIGN,
         Blocks.OAK_WALL_SIGN,
         Blocks.SPRUCE_WALL_SIGN,
         Blocks.BIRCH_WALL_SIGN,
         Blocks.ACACIA_WALL_SIGN,
         Blocks.JUNGLE_WALL_SIGN,
         Blocks.DARK_OAK_WALL_SIGN,
         Blocks.OAK_HANGING_SIGN,
         Blocks.SPRUCE_HANGING_SIGN,
         Blocks.BIRCH_HANGING_SIGN,
         Blocks.ACACIA_HANGING_SIGN,
         Blocks.JUNGLE_HANGING_SIGN,
         Blocks.DARK_OAK_HANGING_SIGN,
         Blocks.OAK_WALL_HANGING_SIGN,
         Blocks.SPRUCE_WALL_HANGING_SIGN,
         Blocks.BIRCH_WALL_HANGING_SIGN,
         Blocks.ACACIA_WALL_HANGING_SIGN,
         Blocks.JUNGLE_WALL_HANGING_SIGN,
         Blocks.DARK_OAK_WALL_HANGING_SIGN
      ) {
         @Override
         public BlockState updateShape(
            BlockState p_63394_, Direction p_63395_, BlockState p_63396_, LevelAccessor p_63397_, BlockPos p_63398_, BlockPos p_63399_
         ) {
            return p_63394_;
         }
      },
      DEFAULT {
         @Override
         public BlockState updateShape(
            BlockState p_63405_, Direction p_63406_, BlockState p_63407_, LevelAccessor p_63408_, BlockPos p_63409_, BlockPos p_63410_
         ) {
            return p_63405_.updateShape(p_63406_, p_63408_.getBlockState(p_63410_), p_63408_, p_63409_, p_63410_);
         }
      },
      CHEST(Blocks.CHEST, Blocks.TRAPPED_CHEST) {
         @Override
         public BlockState updateShape(
            BlockState p_63416_, Direction p_63417_, BlockState p_63418_, LevelAccessor p_63419_, BlockPos p_63420_, BlockPos p_63421_
         ) {
            if (p_63418_.is(p_63416_.getBlock())
               && p_63417_.getAxis().isHorizontal()
               && p_63416_.getValue(ChestBlock.TYPE) == ChestType.SINGLE
               && p_63418_.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
               Direction $$6 = p_63416_.getValue(ChestBlock.FACING);
               if (p_63417_.getAxis() != $$6.getAxis() && $$6 == p_63418_.getValue(ChestBlock.FACING)) {
                  ChestType $$7 = p_63417_ == $$6.getClockWise() ? ChestType.LEFT : ChestType.RIGHT;
                  p_63419_.setBlock(p_63421_, p_63418_.setValue(ChestBlock.TYPE, $$7.getOpposite()), 18);
                  if ($$6 == Direction.NORTH || $$6 == Direction.EAST) {
                     BlockEntity $$8 = p_63419_.getBlockEntity(p_63420_);
                     BlockEntity $$9 = p_63419_.getBlockEntity(p_63421_);
                     if ($$8 instanceof ChestBlockEntity && $$9 instanceof ChestBlockEntity) {
                        ChestBlockEntity.swapContents((ChestBlockEntity)$$8, (ChestBlockEntity)$$9);
                     }
                  }

                  return p_63416_.setValue(ChestBlock.TYPE, $$7);
               }
            }

            return p_63416_;
         }
      },
      LEAVES(
         true,
         Blocks.ACACIA_LEAVES,
         Blocks.CHERRY_LEAVES,
         Blocks.BIRCH_LEAVES,
         Blocks.DARK_OAK_LEAVES,
         Blocks.JUNGLE_LEAVES,
         Blocks.OAK_LEAVES,
         Blocks.SPRUCE_LEAVES
      ) {
         private final ThreadLocal<List<ObjectSet<BlockPos>>> queue = ThreadLocal.withInitial(() -> Lists.newArrayListWithCapacity(7));

         @Override
         public BlockState updateShape(
            BlockState p_63432_, Direction p_63433_, BlockState p_63434_, LevelAccessor p_63435_, BlockPos p_63436_, BlockPos p_63437_
         ) {
            BlockState $$6 = p_63432_.updateShape(p_63433_, p_63435_.getBlockState(p_63437_), p_63435_, p_63436_, p_63437_);
            if (p_63432_ != $$6) {
               int $$7 = $$6.getValue(BlockStateProperties.DISTANCE);
               List<ObjectSet<BlockPos>> $$8 = this.queue.get();
               if ($$8.isEmpty()) {
                  for (int $$9 = 0; $$9 < 7; $$9++) {
                     $$8.add(new ObjectOpenHashSet());
                  }
               }

               $$8.get($$7).add(p_63436_.immutable());
            }

            return p_63432_;
         }

         @Override
         public void processChunk(LevelAccessor p_63430_) {
            BlockPos.MutableBlockPos $$1 = new BlockPos.MutableBlockPos();
            List<ObjectSet<BlockPos>> $$2 = this.queue.get();

            for (int $$3 = 2; $$3 < $$2.size(); $$3++) {
               int $$4 = $$3 - 1;
               ObjectSet<BlockPos> $$5 = $$2.get($$4);
               ObjectSet<BlockPos> $$6 = $$2.get($$3);
               ObjectIterator var8 = $$5.iterator();

               while (var8.hasNext()) {
                  BlockPos $$7 = (BlockPos)var8.next();
                  BlockState $$8 = p_63430_.getBlockState($$7);
                  if ($$8.getValue(BlockStateProperties.DISTANCE) >= $$4) {
                     p_63430_.setBlock($$7, $$8.setValue(BlockStateProperties.DISTANCE, $$4), 18);
                     if ($$3 != 7) {
                        for (Direction $$9 : DIRECTIONS) {
                           $$1.setWithOffset($$7, $$9);
                           BlockState $$10 = p_63430_.getBlockState($$1);
                           if ($$10.hasProperty(BlockStateProperties.DISTANCE) && $$8.getValue(BlockStateProperties.DISTANCE) > $$3) {
                              $$6.add($$1.immutable());
                           }
                        }
                     }
                  }
               }
            }

            $$2.clear();
         }
      },
      STEM_BLOCK(Blocks.MELON_STEM, Blocks.PUMPKIN_STEM) {
         @Override
         public BlockState updateShape(
            BlockState p_63443_, Direction p_63444_, BlockState p_63445_, LevelAccessor p_63446_, BlockPos p_63447_, BlockPos p_63448_
         ) {
            if (p_63443_.getValue(StemBlock.AGE) == 7) {
               Block $$6 = p_63443_.is(Blocks.PUMPKIN_STEM) ? Blocks.PUMPKIN : Blocks.MELON;
               if (p_63445_.is($$6)) {
                  return (p_63443_.is(Blocks.PUMPKIN_STEM) ? Blocks.ATTACHED_PUMPKIN_STEM : Blocks.ATTACHED_MELON_STEM)
                     .defaultBlockState()
                     .setValue(HorizontalDirectionalBlock.FACING, p_63444_);
               }
            }

            return p_63443_;
         }
      };

      public static final Direction[] DIRECTIONS = Direction.values();

      BlockFixers(Block... p_63380_) {
         this(false, p_63380_);
      }

      BlockFixers(boolean p_63369_, Block... p_63370_) {
         for (Block $$2 : p_63370_) {
            UpgradeData.MAP.put($$2, this);
         }

         if (p_63369_) {
            UpgradeData.CHUNKY_FIXERS.add(this);
         }
      }
   }
}
