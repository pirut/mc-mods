package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.IdMapper;
import net.minecraft.core.Vec3i;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class StructureTemplate {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String PALETTE_TAG = "palette";
   public static final String PALETTE_LIST_TAG = "palettes";
   public static final String ENTITIES_TAG = "entities";
   public static final String BLOCKS_TAG = "blocks";
   public static final String BLOCK_TAG_POS = "pos";
   public static final String BLOCK_TAG_STATE = "state";
   public static final String BLOCK_TAG_NBT = "nbt";
   public static final String ENTITY_TAG_POS = "pos";
   public static final String ENTITY_TAG_BLOCKPOS = "blockPos";
   public static final String ENTITY_TAG_NBT = "nbt";
   public static final String SIZE_TAG = "size";
   private final List<StructureTemplate.Palette> palettes = Lists.newArrayList();
   private final List<StructureTemplate.StructureEntityInfo> entityInfoList = Lists.newArrayList();
   private Vec3i size = Vec3i.ZERO;
   private String author = "?";

   public Vec3i getSize() {
      return this.size;
   }

   public void setAuthor(String author) {
      this.author = author;
   }

   public String getAuthor() {
      return this.author;
   }

   public void fillFromWorld(Level level, BlockPos pos, Vec3i size, boolean includeEntities, List<Block> ignoredBlocks) {
      if (size.getX() >= 1 && size.getY() >= 1 && size.getZ() >= 1) {
         BlockPos blockPos = pos.offset(size).offset(-1, -1, -1);
         List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
         List<StructureTemplate.StructureBlockInfo> list2 = Lists.newArrayList();
         List<StructureTemplate.StructureBlockInfo> list3 = Lists.newArrayList();
         BlockPos blockPos2 = new BlockPos(Math.min(pos.getX(), blockPos.getX()), Math.min(pos.getY(), blockPos.getY()), Math.min(pos.getZ(), blockPos.getZ()));
         BlockPos blockPos3 = new BlockPos(Math.max(pos.getX(), blockPos.getX()), Math.max(pos.getY(), blockPos.getY()), Math.max(pos.getZ(), blockPos.getZ()));
         this.size = size;

         try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(LOGGER)) {
            for (BlockPos blockPos4 : BlockPos.betweenClosed(blockPos2, blockPos3)) {
               BlockPos blockPos5 = blockPos4.subtract(blockPos2);
               BlockState blockState = level.getBlockState(blockPos4);
               if (!ignoredBlocks.stream().anyMatch(blockState::is)) {
                  BlockEntity blockEntity = level.getBlockEntity(blockPos4);
                  StructureTemplate.StructureBlockInfo structureBlockInfo;
                  if (blockEntity != null) {
                     TagValueOutput tagValueOutput = TagValueOutput.createWithContext(scopedCollector, level.registryAccess());
                     blockEntity.saveWithId(tagValueOutput);
                     structureBlockInfo = new StructureTemplate.StructureBlockInfo(blockPos5, blockState, tagValueOutput.buildResult());
                  } else {
                     structureBlockInfo = new StructureTemplate.StructureBlockInfo(blockPos5, blockState, null);
                  }

                  addToLists(structureBlockInfo, list, list2, list3);
               }
            }

            List<StructureTemplate.StructureBlockInfo> list4 = buildInfoList(list, list2, list3);
            this.palettes.clear();
            this.palettes.add(new StructureTemplate.Palette(list4));
            if (includeEntities) {
               this.fillEntityList(level, blockPos2, blockPos3, scopedCollector);
            } else {
               this.entityInfoList.clear();
            }
         }
      }
   }

   private static void addToLists(
      StructureTemplate.StructureBlockInfo blockInfo,
      List<StructureTemplate.StructureBlockInfo> normalBlocks,
      List<StructureTemplate.StructureBlockInfo> blocksWithNbt,
      List<StructureTemplate.StructureBlockInfo> blocksWithSpecialShape
   ) {
      if (blockInfo.nbt != null) {
         blocksWithNbt.add(blockInfo);
      } else if (!blockInfo.state.getBlock().hasDynamicShape() && blockInfo.state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
         normalBlocks.add(blockInfo);
      } else {
         blocksWithSpecialShape.add(blockInfo);
      }
   }

   private static List<StructureTemplate.StructureBlockInfo> buildInfoList(
      List<StructureTemplate.StructureBlockInfo> normalBlocks,
      List<StructureTemplate.StructureBlockInfo> blocksWithNbt,
      List<StructureTemplate.StructureBlockInfo> blocksWithSpecialShape
   ) {
      Comparator<StructureTemplate.StructureBlockInfo> comparator = Comparator.<StructureTemplate.StructureBlockInfo>comparingInt(
            structureBlockInfo -> structureBlockInfo.pos.getY()
         )
         .thenComparingInt(structureBlockInfo -> structureBlockInfo.pos.getX())
         .thenComparingInt(structureBlockInfo -> structureBlockInfo.pos.getZ());
      normalBlocks.sort(comparator);
      blocksWithSpecialShape.sort(comparator);
      blocksWithNbt.sort(comparator);
      List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
      list.addAll(normalBlocks);
      list.addAll(blocksWithSpecialShape);
      list.addAll(blocksWithNbt);
      return list;
   }

   private void fillEntityList(Level level, BlockPos pos1, BlockPos pos2, ProblemReporter problemReporter) {
      List<Entity> list = level.getEntitiesOfClass(Entity.class, AABB.encapsulatingFullBlocks(pos1, pos2), entityx -> !(entityx instanceof Player));
      this.entityInfoList.clear();

      for (Entity entity : list) {
         Vec3 vec3 = new Vec3(entity.getX() - pos1.getX(), entity.getY() - pos1.getY(), entity.getZ() - pos1.getZ());
         TagValueOutput tagValueOutput = TagValueOutput.createWithContext(problemReporter.forChild(entity.problemPath()), entity.registryAccess());
         entity.save(tagValueOutput);
         BlockPos blockPos;
         if (entity instanceof Painting painting) {
            blockPos = painting.getPos().subtract(pos1);
         } else {
            blockPos = BlockPos.containing(vec3);
         }

         this.entityInfoList.add(new StructureTemplate.StructureEntityInfo(vec3, blockPos, tagValueOutput.buildResult().copy()));
      }
   }

   public List<StructureTemplate.StructureBlockInfo> filterBlocks(BlockPos pos, StructurePlaceSettings settings, Block block) {
      return this.filterBlocks(pos, settings, block, true);
   }

   public List<StructureTemplate.JigsawBlockInfo> getJigsaws(BlockPos pos, Rotation rotation) {
      if (this.palettes.isEmpty()) {
         return new ArrayList<>();
      } else {
         StructurePlaceSettings structurePlaceSettings = new StructurePlaceSettings().setRotation(rotation);
         List<StructureTemplate.JigsawBlockInfo> list = structurePlaceSettings.getRandomPalette(this.palettes, pos).jigsaws();
         List<StructureTemplate.JigsawBlockInfo> list2 = new ArrayList<>(list.size());

         for (StructureTemplate.JigsawBlockInfo jigsawBlockInfo : list) {
            StructureTemplate.StructureBlockInfo structureBlockInfo = jigsawBlockInfo.info;
            list2.add(
               jigsawBlockInfo.withInfo(
                  new StructureTemplate.StructureBlockInfo(
                     calculateRelativePosition(structurePlaceSettings, structureBlockInfo.pos()).offset(pos),
                     structureBlockInfo.state.rotate(structurePlaceSettings.getRotation()),
                     structureBlockInfo.nbt
                  )
               )
            );
         }

         return list2;
      }
   }

   public ObjectArrayList<StructureTemplate.StructureBlockInfo> filterBlocks(
      BlockPos pos, StructurePlaceSettings settings, Block block, boolean relativePosition
   ) {
      ObjectArrayList<StructureTemplate.StructureBlockInfo> objectArrayList = new ObjectArrayList();
      BoundingBox boundingBox = settings.getBoundingBox();
      if (this.palettes.isEmpty()) {
         return objectArrayList;
      } else {
         for (StructureTemplate.StructureBlockInfo structureBlockInfo : settings.getRandomPalette(this.palettes, pos).blocks(block)) {
            BlockPos blockPos = relativePosition ? calculateRelativePosition(settings, structureBlockInfo.pos).offset(pos) : structureBlockInfo.pos;
            if (boundingBox == null || boundingBox.isInside(blockPos)) {
               objectArrayList.add(
                  new StructureTemplate.StructureBlockInfo(blockPos, structureBlockInfo.state.rotate(settings.getRotation()), structureBlockInfo.nbt)
               );
            }
         }

         return objectArrayList;
      }
   }

   public BlockPos calculateConnectedPosition(StructurePlaceSettings decorator, BlockPos start, StructurePlaceSettings settings, BlockPos end) {
      BlockPos blockPos = calculateRelativePosition(decorator, start);
      BlockPos blockPos2 = calculateRelativePosition(settings, end);
      return blockPos.subtract(blockPos2);
   }

   public static BlockPos calculateRelativePosition(StructurePlaceSettings decorator, BlockPos pos) {
      return transform(pos, decorator.getMirror(), decorator.getRotation(), decorator.getRotationPivot());
   }

   public boolean placeInWorld(
      ServerLevelAccessor serverLevel, BlockPos offset, BlockPos pos, StructurePlaceSettings settings, RandomSource random, @Block.UpdateFlags int flags
   ) {
      if (this.palettes.isEmpty()) {
         return false;
      } else {
         List<StructureTemplate.StructureBlockInfo> list = settings.getRandomPalette(this.palettes, offset).blocks();
         if ((!list.isEmpty() || !settings.isIgnoreEntities() && !this.entityInfoList.isEmpty())
            && this.size.getX() >= 1
            && this.size.getY() >= 1
            && this.size.getZ() >= 1) {
            BoundingBox boundingBox = settings.getBoundingBox();
            List<BlockPos> list2 = Lists.newArrayListWithCapacity(settings.shouldApplyWaterlogging() ? list.size() : 0);
            List<BlockPos> list3 = Lists.newArrayListWithCapacity(settings.shouldApplyWaterlogging() ? list.size() : 0);
            List<Pair<BlockPos, CompoundTag>> list4 = Lists.newArrayListWithCapacity(list.size());
            int i = Integer.MAX_VALUE;
            int j = Integer.MAX_VALUE;
            int k = Integer.MAX_VALUE;
            int l = Integer.MIN_VALUE;
            int m = Integer.MIN_VALUE;
            int n = Integer.MIN_VALUE;
            List<StructureTemplate.StructureBlockInfo> list5 = processBlockInfos(serverLevel, offset, pos, settings, list);

            try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(LOGGER)) {
               for (StructureTemplate.StructureBlockInfo structureBlockInfo : list5) {
                  BlockPos blockPos = structureBlockInfo.pos;
                  if (boundingBox == null || boundingBox.isInside(blockPos)) {
                     FluidState fluidState = settings.shouldApplyWaterlogging() ? serverLevel.getFluidState(blockPos) : null;
                     BlockState blockState = structureBlockInfo.state.mirror(settings.getMirror()).rotate(settings.getRotation());
                     if (structureBlockInfo.nbt != null) {
                        serverLevel.setBlock(blockPos, Blocks.BARRIER.defaultBlockState(), 820);
                     }

                     if (serverLevel.setBlock(blockPos, blockState, flags)) {
                        i = Math.min(i, blockPos.getX());
                        j = Math.min(j, blockPos.getY());
                        k = Math.min(k, blockPos.getZ());
                        l = Math.max(l, blockPos.getX());
                        m = Math.max(m, blockPos.getY());
                        n = Math.max(n, blockPos.getZ());
                        list4.add(Pair.of(blockPos, structureBlockInfo.nbt));
                        if (structureBlockInfo.nbt != null) {
                           BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);
                           if (blockEntity != null) {
                              if (!SharedConstants.DEBUG_STRUCTURE_EDIT_MODE && blockEntity instanceof RandomizableContainer) {
                                 structureBlockInfo.nbt.putLong("LootTableSeed", random.nextLong());
                              }

                              blockEntity.loadWithComponents(
                                 TagValueInput.create(scopedCollector.forChild(blockEntity.problemPath()), serverLevel.registryAccess(), structureBlockInfo.nbt)
                              );
                           }
                        }

                        if (fluidState != null) {
                           if (blockState.getFluidState().isSource()) {
                              list3.add(blockPos);
                           } else if (blockState.getBlock() instanceof LiquidBlockContainer) {
                              ((LiquidBlockContainer)blockState.getBlock()).placeLiquid(serverLevel, blockPos, blockState, fluidState);
                              if (!fluidState.isSource()) {
                                 list2.add(blockPos);
                              }
                           }
                        }
                     }
                  }
               }

               boolean bl = true;
               Direction[] directions = new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

               while (bl && !list2.isEmpty()) {
                  bl = false;
                  Iterator<BlockPos> iterator = list2.iterator();

                  while (iterator.hasNext()) {
                     BlockPos blockPos2 = iterator.next();
                     FluidState fluidState2 = serverLevel.getFluidState(blockPos2);

                     for (int o = 0; o < directions.length && !fluidState2.isSource(); o++) {
                        BlockPos blockPos3 = blockPos2.relative(directions[o]);
                        FluidState fluidState3 = serverLevel.getFluidState(blockPos3);
                        if (fluidState3.isSource() && !list3.contains(blockPos3)) {
                           fluidState2 = fluidState3;
                        }
                     }

                     if (fluidState2.isSource()) {
                        BlockState blockState2 = serverLevel.getBlockState(blockPos2);
                        Block block = blockState2.getBlock();
                        if (block instanceof LiquidBlockContainer) {
                           ((LiquidBlockContainer)block).placeLiquid(serverLevel, blockPos2, blockState2, fluidState2);
                           bl = true;
                           iterator.remove();
                        }
                     }
                  }
               }

               if (i <= l) {
                  if (!settings.getKnownShape()) {
                     DiscreteVoxelShape discreteVoxelShape = new BitSetDiscreteVoxelShape(l - i + 1, m - j + 1, n - k + 1);
                     int p = i;
                     int q = j;
                     int ox = k;

                     for (Pair<BlockPos, CompoundTag> pair : list4) {
                        BlockPos blockPos4 = (BlockPos)pair.getFirst();
                        discreteVoxelShape.fill(blockPos4.getX() - p, blockPos4.getY() - q, blockPos4.getZ() - ox);
                     }

                     updateShapeAtEdge(serverLevel, flags, discreteVoxelShape, p, q, ox);
                  }

                  for (Pair<BlockPos, CompoundTag> pair2 : list4) {
                     BlockPos blockPos5 = (BlockPos)pair2.getFirst();
                     if (!settings.getKnownShape()) {
                        BlockState blockState2 = serverLevel.getBlockState(blockPos5);
                        BlockState blockState3 = Block.updateFromNeighbourShapes(blockState2, serverLevel, blockPos5);
                        if (blockState2 != blockState3) {
                           serverLevel.setBlock(blockPos5, blockState3, flags & -2 | 16);
                        }

                        serverLevel.updateNeighborsAt(blockPos5, blockState3.getBlock());
                     }

                     if (pair2.getSecond() != null) {
                        BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos5);
                        if (blockEntity != null) {
                           blockEntity.setChanged();
                        }
                     }
                  }
               }

               if (!settings.isIgnoreEntities()) {
                  this.placeEntities(
                     serverLevel,
                     offset,
                     settings.getMirror(),
                     settings.getRotation(),
                     settings.getRotationPivot(),
                     boundingBox,
                     settings.shouldFinalizeEntities(),
                     scopedCollector
                  );
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public static void updateShapeAtEdge(LevelAccessor level, @Block.UpdateFlags int flags, DiscreteVoxelShape shape, BlockPos pos) {
      updateShapeAtEdge(level, flags, shape, pos.getX(), pos.getY(), pos.getZ());
   }

   public static void updateShapeAtEdge(LevelAccessor level, @Block.UpdateFlags int flags, DiscreteVoxelShape shape, int x, int y, int z) {
      BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
      BlockPos.MutableBlockPos mutableBlockPos2 = new BlockPos.MutableBlockPos();
      shape.forAllFaces(
         (direction, m, n, o) -> {
            mutableBlockPos.set(x + m, y + n, z + o);
            mutableBlockPos2.setWithOffset(mutableBlockPos, direction);
            BlockState blockState = level.getBlockState(mutableBlockPos);
            BlockState blockState2 = level.getBlockState(mutableBlockPos2);
            BlockState blockState3 = blockState.updateShape(level, level, mutableBlockPos, direction, mutableBlockPos2, blockState2, level.getRandom());
            if (blockState != blockState3) {
               level.setBlock(mutableBlockPos, blockState3, flags & -2);
            }

            BlockState blockState4 = blockState2.updateShape(
               level, level, mutableBlockPos2, direction.getOpposite(), mutableBlockPos, blockState3, level.getRandom()
            );
            if (blockState2 != blockState4) {
               level.setBlock(mutableBlockPos2, blockState4, flags & -2);
            }
         }
      );
   }

   public static List<StructureTemplate.StructureBlockInfo> processBlockInfos(
      ServerLevelAccessor serverLevel, BlockPos offset, BlockPos pos, StructurePlaceSettings settings, List<StructureTemplate.StructureBlockInfo> blockInfos
   ) {
      List<StructureTemplate.StructureBlockInfo> list = new ArrayList<>();
      List<StructureTemplate.StructureBlockInfo> list2 = new ArrayList<>();

      for (StructureTemplate.StructureBlockInfo structureBlockInfo : blockInfos) {
         BlockPos blockPos = calculateRelativePosition(settings, structureBlockInfo.pos).offset(offset);
         StructureTemplate.StructureBlockInfo structureBlockInfo2 = new StructureTemplate.StructureBlockInfo(
            blockPos, structureBlockInfo.state, structureBlockInfo.nbt != null ? structureBlockInfo.nbt.copy() : null
         );
         Iterator<StructureProcessor> iterator = settings.getProcessors().iterator();

         while (structureBlockInfo2 != null && iterator.hasNext()) {
            structureBlockInfo2 = iterator.next().processBlock(serverLevel, offset, pos, structureBlockInfo, structureBlockInfo2, settings);
         }

         if (structureBlockInfo2 != null) {
            list2.add(structureBlockInfo2);
            list.add(structureBlockInfo);
         }
      }

      for (StructureProcessor structureProcessor : settings.getProcessors()) {
         list2 = structureProcessor.finalizeProcessing(serverLevel, offset, pos, list, list2, settings);
      }

      return list2;
   }

   private void placeEntities(
      ServerLevelAccessor level,
      BlockPos pos,
      Mirror mirror,
      Rotation rotation,
      BlockPos offset,
      @Nullable BoundingBox boundingBox,
      boolean finalize,
      ProblemReporter problemReporter
   ) {
      for (StructureTemplate.StructureEntityInfo structureEntityInfo : this.entityInfoList) {
         BlockPos blockPos = transform(structureEntityInfo.blockPos, mirror, rotation, offset).offset(pos);
         if (boundingBox == null || boundingBox.isInside(blockPos)) {
            CompoundTag compoundTag = structureEntityInfo.nbt.copy();
            Vec3 vec3 = transform(structureEntityInfo.pos, mirror, rotation, offset);
            Vec3 vec32 = vec3.add(pos.getX(), pos.getY(), pos.getZ());
            ListTag listTag = new ListTag();
            listTag.add(DoubleTag.valueOf(vec32.x));
            listTag.add(DoubleTag.valueOf(vec32.y));
            listTag.add(DoubleTag.valueOf(vec32.z));
            compoundTag.put("Pos", listTag);
            compoundTag.remove("UUID");
            createEntityIgnoreException(problemReporter, level, compoundTag).ifPresent(entity -> {
               float f = entity.rotate(rotation);
               f += entity.mirror(mirror) - entity.getYRot();
               entity.snapTo(vec32.x, vec32.y, vec32.z, f, entity.getXRot());
               entity.setYBodyRot(f);
               entity.setYHeadRot(f);
               if (finalize && entity instanceof Mob mob) {
                  mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(vec32)), EntitySpawnReason.STRUCTURE, null);
               }

               level.addFreshEntityWithPassengers(entity);
            });
         }
      }
   }

   private static Optional<Entity> createEntityIgnoreException(ProblemReporter problemReporter, ServerLevelAccessor level, CompoundTag tag) {
      try {
         return EntityType.create(TagValueInput.create(problemReporter, level.registryAccess(), tag), level.getLevel(), EntitySpawnReason.STRUCTURE);
      } catch (Exception var4) {
         return Optional.empty();
      }
   }

   public Vec3i getSize(Rotation rotation) {
      switch (rotation) {
         case COUNTERCLOCKWISE_90:
         case CLOCKWISE_90:
            return new Vec3i(this.size.getZ(), this.size.getY(), this.size.getX());
         default:
            return this.size;
      }
   }

   public static BlockPos transform(BlockPos targetPos, Mirror mirror, Rotation rotation, BlockPos offset) {
      int i = targetPos.getX();
      int j = targetPos.getY();
      int k = targetPos.getZ();
      boolean bl = true;
      switch (mirror) {
         case LEFT_RIGHT:
            k = -k;
            break;
         case FRONT_BACK:
            i = -i;
            break;
         default:
            bl = false;
      }

      int l = offset.getX();
      int m = offset.getZ();
      switch (rotation) {
         case COUNTERCLOCKWISE_90:
            return new BlockPos(l - m + k, j, l + m - i);
         case CLOCKWISE_90:
            return new BlockPos(l + m - k, j, m - l + i);
         case CLOCKWISE_180:
            return new BlockPos(l + l - i, j, m + m - k);
         default:
            return bl ? new BlockPos(i, j, k) : targetPos;
      }
   }

   public static Vec3 transform(Vec3 target, Mirror mirror, Rotation rotation, BlockPos centerOffset) {
      double d = target.x;
      double e = target.y;
      double f = target.z;
      boolean bl = true;
      switch (mirror) {
         case LEFT_RIGHT:
            f = 1.0 - f;
            break;
         case FRONT_BACK:
            d = 1.0 - d;
            break;
         default:
            bl = false;
      }

      int i = centerOffset.getX();
      int j = centerOffset.getZ();
      switch (rotation) {
         case COUNTERCLOCKWISE_90:
            return new Vec3(i - j + f, e, i + j + 1 - d);
         case CLOCKWISE_90:
            return new Vec3(i + j + 1 - f, e, j - i + d);
         case CLOCKWISE_180:
            return new Vec3(i + i + 1 - d, e, j + j + 1 - f);
         default:
            return bl ? new Vec3(d, e, f) : target;
      }
   }

   public BlockPos getZeroPositionWithTransform(BlockPos targetPos, Mirror mirror, Rotation rotation) {
      return getZeroPositionWithTransform(targetPos, mirror, rotation, this.getSize().getX(), this.getSize().getZ());
   }

   public static BlockPos getZeroPositionWithTransform(BlockPos pos, Mirror mirror, Rotation rotation, int sizeX, int sizeZ) {
      sizeX--;
      sizeZ--;
      int i = mirror == Mirror.FRONT_BACK ? sizeX : 0;
      int j = mirror == Mirror.LEFT_RIGHT ? sizeZ : 0;
      BlockPos blockPos = pos;
      switch (rotation) {
         case COUNTERCLOCKWISE_90:
            blockPos = pos.offset(j, 0, sizeX - i);
            break;
         case CLOCKWISE_90:
            blockPos = pos.offset(sizeZ - j, 0, i);
            break;
         case CLOCKWISE_180:
            blockPos = pos.offset(sizeX - i, 0, sizeZ - j);
            break;
         case NONE:
            blockPos = pos.offset(i, 0, j);
      }

      return blockPos;
   }

   public BoundingBox getBoundingBox(StructurePlaceSettings settings, BlockPos startPos) {
      return this.getBoundingBox(startPos, settings.getRotation(), settings.getRotationPivot(), settings.getMirror());
   }

   public BoundingBox getBoundingBox(BlockPos startPos, Rotation rotation, BlockPos pivotPos, Mirror mirror) {
      return getBoundingBox(startPos, rotation, pivotPos, mirror, this.size);
   }

   @VisibleForTesting
   protected static BoundingBox getBoundingBox(BlockPos startPos, Rotation rotation, BlockPos pivotPos, Mirror mirror, Vec3i size) {
      Vec3i vec3i = size.offset(-1, -1, -1);
      BlockPos blockPos = transform(BlockPos.ZERO, mirror, rotation, pivotPos);
      BlockPos blockPos2 = transform(BlockPos.ZERO.offset(vec3i), mirror, rotation, pivotPos);
      return BoundingBox.fromCorners(blockPos, blockPos2).move(startPos);
   }

   public CompoundTag save(CompoundTag tag) {
      if (this.palettes.isEmpty()) {
         tag.put("blocks", new ListTag());
         tag.put("palette", new ListTag());
      } else {
         List<StructureTemplate.SimplePalette> list = Lists.newArrayList();
         StructureTemplate.SimplePalette simplePalette = new StructureTemplate.SimplePalette();
         list.add(simplePalette);

         for (int i = 1; i < this.palettes.size(); i++) {
            list.add(new StructureTemplate.SimplePalette());
         }

         ListTag listTag = new ListTag();
         List<StructureTemplate.StructureBlockInfo> list2 = this.palettes.get(0).blocks();

         for (int j = 0; j < list2.size(); j++) {
            StructureTemplate.StructureBlockInfo structureBlockInfo = list2.get(j);
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.put("pos", this.newIntegerList(structureBlockInfo.pos.getX(), structureBlockInfo.pos.getY(), structureBlockInfo.pos.getZ()));
            int k = simplePalette.idFor(structureBlockInfo.state);
            compoundTag.putInt("state", k);
            if (structureBlockInfo.nbt != null) {
               compoundTag.put("nbt", structureBlockInfo.nbt);
            }

            listTag.add(compoundTag);

            for (int l = 1; l < this.palettes.size(); l++) {
               StructureTemplate.SimplePalette simplePalette2 = list.get(l);
               simplePalette2.addMapping(this.palettes.get(l).blocks().get(j).state, k);
            }
         }

         tag.put("blocks", listTag);
         if (list.size() == 1) {
            ListTag listTag2 = new ListTag();

            for (BlockState blockState : simplePalette) {
               listTag2.add(NbtUtils.writeBlockState(blockState));
            }

            tag.put("palette", listTag2);
         } else {
            ListTag listTag2 = new ListTag();

            for (StructureTemplate.SimplePalette simplePalette3 : list) {
               ListTag listTag3 = new ListTag();

               for (BlockState blockState2 : simplePalette3) {
                  listTag3.add(NbtUtils.writeBlockState(blockState2));
               }

               listTag2.add(listTag3);
            }

            tag.put("palettes", listTag2);
         }
      }

      ListTag listTag4 = new ListTag();

      for (StructureTemplate.StructureEntityInfo structureEntityInfo : this.entityInfoList) {
         CompoundTag compoundTag2 = new CompoundTag();
         compoundTag2.put("pos", this.newDoubleList(structureEntityInfo.pos.x, structureEntityInfo.pos.y, structureEntityInfo.pos.z));
         compoundTag2.put(
            "blockPos", this.newIntegerList(structureEntityInfo.blockPos.getX(), structureEntityInfo.blockPos.getY(), structureEntityInfo.blockPos.getZ())
         );
         if (structureEntityInfo.nbt != null) {
            compoundTag2.put("nbt", structureEntityInfo.nbt);
         }

         listTag4.add(compoundTag2);
      }

      tag.put("entities", listTag4);
      tag.put("size", this.newIntegerList(this.size.getX(), this.size.getY(), this.size.getZ()));
      return NbtUtils.addCurrentDataVersion(tag);
   }

   public void load(HolderGetter<Block> blockGetter, CompoundTag tag) {
      this.palettes.clear();
      this.entityInfoList.clear();
      ListTag listTag = tag.getListOrEmpty("size");
      this.size = new Vec3i(listTag.getIntOr(0, 0), listTag.getIntOr(1, 0), listTag.getIntOr(2, 0));
      ListTag listTag2 = tag.getListOrEmpty("blocks");
      Optional<ListTag> optional = tag.getList("palettes");
      if (optional.isPresent()) {
         for (int i = 0; i < optional.get().size(); i++) {
            this.loadPalette(blockGetter, optional.get().getListOrEmpty(i), listTag2);
         }
      } else {
         this.loadPalette(blockGetter, tag.getListOrEmpty("palette"), listTag2);
      }

      tag.getListOrEmpty("entities")
         .compoundStream()
         .forEach(
            compoundTag -> {
               ListTag listTagx = compoundTag.getListOrEmpty("pos");
               Vec3 vec3 = new Vec3(listTagx.getDoubleOr(0, 0.0), listTagx.getDoubleOr(1, 0.0), listTagx.getDoubleOr(2, 0.0));
               ListTag listTag2x = compoundTag.getListOrEmpty("blockPos");
               BlockPos blockPos = new BlockPos(listTag2x.getIntOr(0, 0), listTag2x.getIntOr(1, 0), listTag2x.getIntOr(2, 0));
               compoundTag.getCompound("nbt")
                  .ifPresent(compoundTagx -> this.entityInfoList.add(new StructureTemplate.StructureEntityInfo(vec3, blockPos, compoundTagx)));
            }
         );
   }

   private void loadPalette(HolderGetter<Block> blockGetter, ListTag paletteTag, ListTag blocksTag) {
      StructureTemplate.SimplePalette simplePalette = new StructureTemplate.SimplePalette();

      for (int i = 0; i < paletteTag.size(); i++) {
         simplePalette.addMapping(NbtUtils.readBlockState(blockGetter, paletteTag.getCompoundOrEmpty(i)), i);
      }

      List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
      List<StructureTemplate.StructureBlockInfo> list2 = Lists.newArrayList();
      List<StructureTemplate.StructureBlockInfo> list3 = Lists.newArrayList();
      blocksTag.compoundStream().forEach(compoundTag -> {
         ListTag listTag = compoundTag.getListOrEmpty("pos");
         BlockPos blockPos = new BlockPos(listTag.getIntOr(0, 0), listTag.getIntOr(1, 0), listTag.getIntOr(2, 0));
         BlockState blockState = simplePalette.stateFor(compoundTag.getIntOr("state", 0));
         CompoundTag compoundTag2 = compoundTag.getCompound("nbt").orElse(null);
         StructureTemplate.StructureBlockInfo structureBlockInfo = new StructureTemplate.StructureBlockInfo(blockPos, blockState, compoundTag2);
         addToLists(structureBlockInfo, list, list2, list3);
      });
      List<StructureTemplate.StructureBlockInfo> list4 = buildInfoList(list, list2, list3);
      this.palettes.add(new StructureTemplate.Palette(list4));
   }

   private ListTag newIntegerList(int... values) {
      ListTag listTag = new ListTag();

      for (int i : values) {
         listTag.add(IntTag.valueOf(i));
      }

      return listTag;
   }

   private ListTag newDoubleList(double... values) {
      ListTag listTag = new ListTag();

      for (double d : values) {
         listTag.add(DoubleTag.valueOf(d));
      }

      return listTag;
   }

   public static JigsawBlockEntity.JointType getJointType(CompoundTag tag, BlockState state) {
      return tag.<JigsawBlockEntity.JointType>read("joint", JigsawBlockEntity.JointType.CODEC).orElseGet(() -> getDefaultJointType(state));
   }

   public static JigsawBlockEntity.JointType getDefaultJointType(BlockState state) {
      return JigsawBlock.getFrontFacing(state).getAxis().isHorizontal() ? JigsawBlockEntity.JointType.ALIGNED : JigsawBlockEntity.JointType.ROLLABLE;
   }

   public record JigsawBlockInfo(
      StructureTemplate.StructureBlockInfo info,
      JigsawBlockEntity.JointType jointType,
      Identifier name,
      ResourceKey<StructureTemplatePool> pool,
      Identifier target,
      int placementPriority,
      int selectionPriority
   ) {

      public static StructureTemplate.JigsawBlockInfo of(StructureTemplate.StructureBlockInfo structureBlockInfo) {
         CompoundTag compoundTag = Objects.requireNonNull(structureBlockInfo.nbt(), () -> structureBlockInfo + " nbt was null");
         return new StructureTemplate.JigsawBlockInfo(
            structureBlockInfo,
            StructureTemplate.getJointType(compoundTag, structureBlockInfo.state()),
            compoundTag.<Identifier>read("name", Identifier.CODEC).orElse(JigsawBlockEntity.EMPTY_ID),
            compoundTag.<ResourceKey<StructureTemplatePool>>read("pool", JigsawBlockEntity.POOL_CODEC).orElse(Pools.EMPTY),
            compoundTag.<Identifier>read("target", Identifier.CODEC).orElse(JigsawBlockEntity.EMPTY_ID),
            compoundTag.getIntOr("placement_priority", 0),
            compoundTag.getIntOr("selection_priority", 0)
         );
      }

      @Override
      public String toString() {
         return String.format(
            Locale.ROOT,
            "<JigsawBlockInfo | %s | %s | name: %s | pool: %s | target: %s | placement: %d | selection: %d | %s>",
            this.info.pos,
            this.info.state,
            this.name,
            this.pool.identifier(),
            this.target,
            this.placementPriority,
            this.selectionPriority,
            this.info.nbt
         );
      }

      public StructureTemplate.JigsawBlockInfo withInfo(StructureTemplate.StructureBlockInfo info) {
         return new StructureTemplate.JigsawBlockInfo(info, this.jointType, this.name, this.pool, this.target, this.placementPriority, this.selectionPriority);
      }
   }

   public static final class Palette {
      private final List<StructureTemplate.StructureBlockInfo> blocks;
      private final Map<Block, List<StructureTemplate.StructureBlockInfo>> cache = Maps.newHashMap();
      @Nullable
      private List<StructureTemplate.JigsawBlockInfo> cachedJigsaws;

      Palette(List<StructureTemplate.StructureBlockInfo> blocks) {
         this.blocks = blocks;
      }

      public List<StructureTemplate.JigsawBlockInfo> jigsaws() {
         if (this.cachedJigsaws == null) {
            this.cachedJigsaws = this.blocks(Blocks.JIGSAW).stream().map(StructureTemplate.JigsawBlockInfo::of).toList();
         }

         return this.cachedJigsaws;
      }

      public List<StructureTemplate.StructureBlockInfo> blocks() {
         return this.blocks;
      }

      public List<StructureTemplate.StructureBlockInfo> blocks(Block block) {
         return this.cache
            .computeIfAbsent(
               block, blockx -> this.blocks.stream().filter(structureBlockInfo -> structureBlockInfo.state.is(blockx)).collect(Collectors.toList())
            );
      }
   }

   static class SimplePalette implements Iterable<BlockState> {
      public static final BlockState DEFAULT_BLOCK_STATE = Blocks.AIR.defaultBlockState();
      private final IdMapper<BlockState> ids = new IdMapper<>(16);
      private int lastId;

      public int idFor(BlockState state) {
         int i = this.ids.getId(state);
         if (i == -1) {
            i = this.lastId++;
            this.ids.addMapping(state, i);
         }

         return i;
      }

      @Nullable
      public BlockState stateFor(int id) {
         BlockState blockState = this.ids.byId(id);
         return blockState == null ? DEFAULT_BLOCK_STATE : blockState;
      }

      @Override
      public Iterator<BlockState> iterator() {
         return this.ids.iterator();
      }

      public void addMapping(BlockState state, int id) {
         this.ids.addMapping(state, id);
      }
   }

   public record StructureBlockInfo(BlockPos pos, BlockState state, @Nullable CompoundTag nbt) {

      @Override
      public String toString() {
         return String.format(Locale.ROOT, "<StructureBlockInfo | %s | %s | %s>", this.pos, this.state, this.nbt);
      }
   }

   public static class StructureEntityInfo {
      public final Vec3 pos;
      public final BlockPos blockPos;
      public final CompoundTag nbt;

      public StructureEntityInfo(Vec3 pos, BlockPos blockPos, CompoundTag nbt) {
         this.pos = pos;
         this.blockPos = blockPos;
         this.nbt = nbt;
      }
   }
}
