package net.minecraft.gametest.framework;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class StructureUtils {
   public static final int DEFAULT_Y_SEARCH_RADIUS = 10;
   public static final String DEFAULT_TEST_STRUCTURES_DIR = "Minecraft.Server/src/test/convertables/data";
   public static Path testStructuresDir = Paths.get("Minecraft.Server/src/test/convertables/data");

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

   public static TestInstanceBlockEntity createNewEmptyTest(
      Identifier p_468497_, BlockPos p_397076_, Vec3i p_397736_, Rotation p_397442_, ServerLevel p_397074_
   ) {
      BoundingBox boundingbox = getStructureBoundingBox(TestInstanceBlockEntity.getStructurePos(p_397076_), p_397736_, p_397442_);
      clearSpaceForStructure(boundingbox, p_397074_);
      p_397074_.setBlockAndUpdate(p_397076_, Blocks.TEST_INSTANCE_BLOCK.defaultBlockState());
      TestInstanceBlockEntity testinstanceblockentity = (TestInstanceBlockEntity)p_397074_.getBlockEntity(p_397076_);
      ResourceKey<GameTestInstance> resourcekey = ResourceKey.create(Registries.TEST_INSTANCE, p_468497_);
      testinstanceblockentity.set(
         new TestInstanceBlockEntity.Data(Optional.of(resourcekey), p_397736_, p_397442_, false, TestInstanceBlockEntity.Status.CLEARED, Optional.empty())
      );
      return testinstanceblockentity;
   }

   public static void clearSpaceForStructure(BoundingBox p_127850_, ServerLevel p_127852_) {
      int i = p_127850_.minY() - 1;
      BlockPos.betweenClosedStream(p_127850_).forEach(p_177748_ -> clearBlock(i, p_177748_, p_127852_));
      p_127852_.getBlockTicks().clearArea(p_127850_);
      p_127852_.clearBlockEvents(p_127850_);
      AABB aabb = AABB.of(p_127850_);
      List<Entity> list = p_127852_.getEntitiesOfClass(Entity.class, aabb, p_177750_ -> !(p_177750_ instanceof Player));
      list.forEach(Entity::discard);
   }

   public static BlockPos getTransformedFarCorner(BlockPos p_308915_, Vec3i p_309132_, Rotation p_308895_) {
      BlockPos blockpos = p_308915_.offset(p_309132_).offset(-1, -1, -1);
      return StructureTemplate.transform(blockpos, Mirror.NONE, p_308895_, p_308915_);
   }

   public static BoundingBox getStructureBoundingBox(BlockPos p_177761_, Vec3i p_177762_, Rotation p_177763_) {
      BlockPos blockpos = getTransformedFarCorner(p_177761_, p_177762_, p_177763_);
      BoundingBox boundingbox = BoundingBox.fromCorners(p_177761_, blockpos);
      int i = Math.min(boundingbox.minX(), boundingbox.maxX());
      int j = Math.min(boundingbox.minZ(), boundingbox.maxZ());
      return boundingbox.move(p_177761_.getX() - i, 0, p_177761_.getZ() - j);
   }

   public static Optional<BlockPos> findTestContainingPos(BlockPos p_397180_, int p_397344_, ServerLevel p_397768_) {
      return findTestBlocks(p_397180_, p_397344_, p_397768_).filter(p_177756_ -> doesStructureContain(p_177756_, p_397180_, p_397768_)).findFirst();
   }

   public static Optional<BlockPos> findNearestTest(BlockPos p_397163_, int p_397842_, ServerLevel p_397581_) {
      Comparator<BlockPos> comparator = Comparator.comparingInt(p_177759_ -> p_177759_.distManhattan(p_397163_));
      return findTestBlocks(p_397163_, p_397842_, p_397581_).min(comparator);
   }

   public static Stream<BlockPos> findTestBlocks(BlockPos p_127911_, int p_127912_, ServerLevel p_127913_) {
      return p_127913_.getPoiManager()
         .findAll(p_417693_ -> p_417693_.is(PoiTypes.TEST_INSTANCE), p_417694_ -> true, p_127911_, p_127912_, PoiManager.Occupancy.ANY)
         .map(BlockPos::immutable);
   }

   public static Stream<BlockPos> lookedAtTestPos(BlockPos p_397600_, Entity p_397674_, ServerLevel p_397378_) {
      int i = 250;
      Vec3 vec3 = p_397674_.getEyePosition();
      Vec3 vec31 = vec3.add(p_397674_.getLookAngle().scale(250.0));
      return findTestBlocks(p_397600_, 250, p_397378_)
         .map(p_396415_ -> p_397378_.getBlockEntity(p_396415_, BlockEntityType.TEST_INSTANCE_BLOCK))
         .flatMap(Optional::stream)
         .filter(p_396413_ -> p_396413_.getStructureBounds().clip(vec3, vec31).isPresent())
         .map(BlockEntity::getBlockPos)
         .sorted(Comparator.comparing(p_397600_::distSqr))
         .limit(1L);
   }

   private static void clearBlock(int p_127842_, BlockPos p_127843_, ServerLevel p_127844_) {
      BlockState blockstate;
      if (p_127843_.getY() < p_127842_) {
         blockstate = Blocks.STONE.defaultBlockState();
      } else {
         blockstate = Blocks.AIR.defaultBlockState();
      }

      BlockInput blockinput = new BlockInput(blockstate, Collections.emptySet(), null);
      blockinput.place(p_127844_, p_127843_, 818);
      p_127844_.updateNeighborsAt(p_127843_, blockstate.getBlock());
   }

   private static boolean doesStructureContain(BlockPos p_127868_, BlockPos p_127869_, ServerLevel p_127870_) {
      return p_127870_.getBlockEntity(p_127868_) instanceof TestInstanceBlockEntity testinstanceblockentity
         ? testinstanceblockentity.getStructureBoundingBox().isInside(p_127869_)
         : false;
   }
}
