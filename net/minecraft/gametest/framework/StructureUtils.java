package net.minecraft.gametest.framework;

import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class StructureUtils {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int DEFAULT_Y_SEARCH_RADIUS = 10;
   public static final String DEFAULT_TEST_STRUCTURES_DIR = "gameteststructures";
   public static String testStructuresDir = "gameteststructures";

   public static Rotation getRotationForRotationSteps(int p_127836_) {
      switch (p_127836_) {
         case 0:
            return Rotation.NONE;
         case 1:
            return Rotation.CLOCKWISE_90;
         case 2:
            return Rotation.CLOCKWISE_180;
         case 3:
            return Rotation.COUNTERCLOCKWISE_90;
         default:
            throw new IllegalArgumentException("rotationSteps must be a value from 0-3. Got value " + p_127836_);
      }
   }

   public static int getRotationStepsForRotation(Rotation p_177752_) {
      switch (p_177752_) {
         case NONE:
            return 0;
         case CLOCKWISE_90:
            return 1;
         case CLOCKWISE_180:
            return 2;
         case COUNTERCLOCKWISE_90:
            return 3;
         default:
            throw new IllegalArgumentException("Unknown rotation value, don't know how many steps it represents: " + p_177752_);
      }
   }

   public static AABB getStructureBounds(StructureBlockEntity p_127848_) {
      return AABB.of(getStructureBoundingBox(p_127848_));
   }

   public static BoundingBox getStructureBoundingBox(StructureBlockEntity p_127905_) {
      BlockPos $$1 = getStructureOrigin(p_127905_);
      BlockPos $$2 = getTransformedFarCorner($$1, p_127905_.getStructureSize(), p_127905_.getRotation());
      return BoundingBox.fromCorners($$1, $$2);
   }

   public static BlockPos getStructureOrigin(StructureBlockEntity p_308940_) {
      return p_308940_.getBlockPos().offset(p_308940_.getStructurePos());
   }

   public static void addCommandBlockAndButtonToStartTest(BlockPos p_127876_, BlockPos p_127877_, Rotation p_127878_, ServerLevel p_127879_) {
      BlockPos $$4 = StructureTemplate.transform(p_127876_.offset(p_127877_), Mirror.NONE, p_127878_, p_127876_);
      p_127879_.setBlockAndUpdate($$4, Blocks.COMMAND_BLOCK.defaultBlockState());
      CommandBlockEntity $$5 = (CommandBlockEntity)p_127879_.getBlockEntity($$4);
      $$5.getCommandBlock().setCommand("test runclosest");
      BlockPos $$6 = StructureTemplate.transform($$4.offset(0, 0, -1), Mirror.NONE, p_127878_, $$4);
      p_127879_.setBlockAndUpdate($$6, Blocks.STONE_BUTTON.defaultBlockState().rotate(p_127878_));
   }

   public static void createNewEmptyStructureBlock(String p_177765_, BlockPos p_177766_, Vec3i p_177767_, Rotation p_177768_, ServerLevel p_177769_) {
      BoundingBox $$5 = getStructureBoundingBox(p_177766_.above(), p_177767_, p_177768_);
      clearSpaceForStructure($$5, p_177769_);
      p_177769_.setBlockAndUpdate(p_177766_, Blocks.STRUCTURE_BLOCK.defaultBlockState());
      StructureBlockEntity $$6 = (StructureBlockEntity)p_177769_.getBlockEntity(p_177766_);
      $$6.setIgnoreEntities(false);
      $$6.setStructureName(ResourceLocation.parse(p_177765_));
      $$6.setStructureSize(p_177767_);
      $$6.setMode(StructureMode.SAVE);
      $$6.setShowBoundingBox(true);
   }

   public static StructureBlockEntity prepareTestStructure(GameTestInfo p_312267_, BlockPos p_309600_, Rotation p_309541_, ServerLevel p_309609_) {
      Vec3i $$4 = p_309609_.getStructureManager()
         .get(ResourceLocation.parse(p_312267_.getStructureName()))
         .orElseThrow(() -> new IllegalStateException("Missing test structure: " + p_312267_.getStructureName()))
         .getSize();
      BoundingBox $$5 = getStructureBoundingBox(p_309600_, $$4, p_309541_);
      BlockPos $$6;
      if (p_309541_ == Rotation.NONE) {
         $$6 = p_309600_;
      } else if (p_309541_ == Rotation.CLOCKWISE_90) {
         $$6 = p_309600_.offset($$4.getZ() - 1, 0, 0);
      } else if (p_309541_ == Rotation.CLOCKWISE_180) {
         $$6 = p_309600_.offset($$4.getX() - 1, 0, $$4.getZ() - 1);
      } else {
         if (p_309541_ != Rotation.COUNTERCLOCKWISE_90) {
            throw new IllegalArgumentException("Invalid rotation: " + p_309541_);
         }

         $$6 = p_309600_.offset(0, 0, $$4.getX() - 1);
      }

      forceLoadChunks($$5, p_309609_);
      clearSpaceForStructure($$5, p_309609_);
      return createStructureBlock(p_312267_, $$6.below(), p_309541_, p_309609_);
   }

   public static void encaseStructure(AABB p_326863_, ServerLevel p_326882_, boolean p_326950_) {
      BlockPos $$3 = BlockPos.containing(p_326863_.minX, p_326863_.minY, p_326863_.minZ).offset(-1, 0, -1);
      BlockPos $$4 = BlockPos.containing(p_326863_.maxX, p_326863_.maxY, p_326863_.maxZ);
      BlockPos.betweenClosedStream($$3, $$4).forEach(p_326745_ -> {
         boolean $$5 = p_326745_.getX() == $$3.getX() || p_326745_.getX() == $$4.getX() || p_326745_.getZ() == $$3.getZ() || p_326745_.getZ() == $$4.getZ();
         boolean $$6 = p_326745_.getY() == $$4.getY();
         if ($$5 || $$6 && p_326950_) {
            p_326882_.setBlockAndUpdate(p_326745_, Blocks.BARRIER.defaultBlockState());
         }
      });
   }

   public static void removeBarriers(AABB p_326925_, ServerLevel p_326815_) {
      BlockPos $$2 = BlockPos.containing(p_326925_.minX, p_326925_.minY, p_326925_.minZ).offset(-1, 0, -1);
      BlockPos $$3 = BlockPos.containing(p_326925_.maxX, p_326925_.maxY, p_326925_.maxZ);
      BlockPos.betweenClosedStream($$2, $$3).forEach(p_326740_ -> {
         boolean $$4 = p_326740_.getX() == $$2.getX() || p_326740_.getX() == $$3.getX() || p_326740_.getZ() == $$2.getZ() || p_326740_.getZ() == $$3.getZ();
         boolean $$5 = p_326740_.getY() == $$3.getY();
         if (p_326815_.getBlockState(p_326740_).is(Blocks.BARRIER) && ($$4 || $$5)) {
            p_326815_.setBlockAndUpdate(p_326740_, Blocks.AIR.defaultBlockState());
         }
      });
   }

   private static void forceLoadChunks(BoundingBox p_308909_, ServerLevel p_127859_) {
      p_308909_.intersectingChunks().forEach(p_308480_ -> p_127859_.setChunkForced(p_308480_.x, p_308480_.z, true));
   }

   public static void clearSpaceForStructure(BoundingBox p_127850_, ServerLevel p_127852_) {
      int $$2 = p_127850_.minY() - 1;
      BoundingBox $$3 = new BoundingBox(
         p_127850_.minX() - 2, p_127850_.minY() - 3, p_127850_.minZ() - 3, p_127850_.maxX() + 3, p_127850_.maxY() + 20, p_127850_.maxZ() + 3
      );
      BlockPos.betweenClosedStream($$3).forEach(p_177748_ -> clearBlock($$2, p_177748_, p_127852_));
      p_127852_.getBlockTicks().clearArea($$3);
      p_127852_.clearBlockEvents($$3);
      AABB $$4 = AABB.of($$3);
      List<Entity> $$5 = p_127852_.getEntitiesOfClass(Entity.class, $$4, p_177750_ -> !(p_177750_ instanceof Player));
      $$5.forEach(Entity::discard);
   }

   public static BlockPos getTransformedFarCorner(BlockPos p_308915_, Vec3i p_309132_, Rotation p_308895_) {
      BlockPos $$3 = p_308915_.offset(p_309132_).offset(-1, -1, -1);
      return StructureTemplate.transform($$3, Mirror.NONE, p_308895_, p_308915_);
   }

   public static BoundingBox getStructureBoundingBox(BlockPos p_177761_, Vec3i p_177762_, Rotation p_177763_) {
      BlockPos $$3 = getTransformedFarCorner(p_177761_, p_177762_, p_177763_);
      BoundingBox $$4 = BoundingBox.fromCorners(p_177761_, $$3);
      int $$5 = Math.min($$4.minX(), $$4.maxX());
      int $$6 = Math.min($$4.minZ(), $$4.maxZ());
      return $$4.move(p_177761_.getX() - $$5, 0, p_177761_.getZ() - $$6);
   }

   public static Optional<BlockPos> findStructureBlockContainingPos(BlockPos p_127854_, int p_127855_, ServerLevel p_127856_) {
      return findStructureBlocks(p_127854_, p_127855_, p_127856_).filter(p_177756_ -> doesStructureContain(p_177756_, p_127854_, p_127856_)).findFirst();
   }

   public static Optional<BlockPos> findNearestStructureBlock(BlockPos p_127907_, int p_127908_, ServerLevel p_127909_) {
      Comparator<BlockPos> $$3 = Comparator.comparingInt(p_177759_ -> p_177759_.distManhattan(p_127907_));
      return findStructureBlocks(p_127907_, p_127908_, p_127909_).min($$3);
   }

   public static Stream<BlockPos> findStructureByTestFunction(BlockPos p_340830_, int p_340828_, ServerLevel p_340893_, String p_341292_) {
      return findStructureBlocks(p_340830_, p_340828_, p_340893_)
         .map(p_340630_ -> (StructureBlockEntity)p_340893_.getBlockEntity(p_340630_))
         .filter(Objects::nonNull)
         .filter(p_340628_ -> Objects.equals(p_340628_.getStructureName(), p_341292_))
         .map(BlockEntity::getBlockPos)
         .map(BlockPos::immutable);
   }

   public static Stream<BlockPos> findStructureBlocks(BlockPos p_127911_, int p_127912_, ServerLevel p_127913_) {
      BoundingBox $$3 = getBoundingBoxAtGround(p_127911_, p_127912_, p_127913_);
      return BlockPos.betweenClosedStream($$3).filter(p_319470_ -> p_127913_.getBlockState(p_319470_).is(Blocks.STRUCTURE_BLOCK)).map(BlockPos::immutable);
   }

   private static StructureBlockEntity createStructureBlock(GameTestInfo p_312256_, BlockPos p_127892_, Rotation p_127893_, ServerLevel p_127894_) {
      p_127894_.setBlockAndUpdate(p_127892_, Blocks.STRUCTURE_BLOCK.defaultBlockState());
      StructureBlockEntity $$4 = (StructureBlockEntity)p_127894_.getBlockEntity(p_127892_);
      $$4.setMode(StructureMode.LOAD);
      $$4.setRotation(p_127893_);
      $$4.setIgnoreEntities(false);
      $$4.setStructureName(ResourceLocation.parse(p_312256_.getStructureName()));
      $$4.setMetaData(p_312256_.getTestName());
      if (!$$4.loadStructureInfo(p_127894_)) {
         throw new RuntimeException("Failed to load structure info for test: " + p_312256_.getTestName() + ". Structure name: " + p_312256_.getStructureName());
      } else {
         return $$4;
      }
   }

   private static BoundingBox getBoundingBoxAtGround(BlockPos p_341291_, int p_341122_, ServerLevel p_341148_) {
      BlockPos $$3 = BlockPos.containing(p_341291_.getX(), p_341148_.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, p_341291_).getY(), p_341291_.getZ());
      return new BoundingBox($$3).inflatedBy(p_341122_, 10, p_341122_);
   }

   public static Stream<BlockPos> lookedAtStructureBlockPos(BlockPos p_320206_, Entity p_320494_, ServerLevel p_320139_) {
      int $$3 = 200;
      Vec3 $$4 = p_320494_.getEyePosition();
      Vec3 $$5 = $$4.add(p_320494_.getLookAngle().scale(200.0));
      return findStructureBlocks(p_320206_, 200, p_320139_)
         .map(p_319477_ -> p_320139_.getBlockEntity(p_319477_, BlockEntityType.STRUCTURE_BLOCK))
         .flatMap(Optional::stream)
         .filter(p_319475_ -> getStructureBounds(p_319475_).clip($$4, $$5).isPresent())
         .map(BlockEntity::getBlockPos)
         .sorted(Comparator.comparing(p_320206_::distSqr))
         .limit(1L);
   }

   private static void clearBlock(int p_127842_, BlockPos p_127843_, ServerLevel p_127844_) {
      BlockState $$3;
      if (p_127843_.getY() < p_127842_) {
         $$3 = Blocks.STONE.defaultBlockState();
      } else {
         $$3 = Blocks.AIR.defaultBlockState();
      }

      BlockInput $$5 = new BlockInput($$3, Collections.emptySet(), null);
      $$5.place(p_127844_, p_127843_, 2);
      p_127844_.blockUpdated(p_127843_, $$3.getBlock());
   }

   private static boolean doesStructureContain(BlockPos p_127868_, BlockPos p_127869_, ServerLevel p_127870_) {
      StructureBlockEntity $$3 = (StructureBlockEntity)p_127870_.getBlockEntity(p_127868_);
      return getStructureBoundingBox($$3).isInside(p_127869_);
   }
}
