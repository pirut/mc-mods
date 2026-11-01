package net.minecraft.world.level.block.sounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class AmbientDesertBlockSoundsPlayer {
   private static final int IDLE_SOUND_CHANCE = 2100;
   private static final int DRY_GRASS_SOUND_CHANCE = 200;
   private static final int DEAD_BUSH_SOUND_CHANCE = 130;
   private static final int DEAD_BUSH_SOUND_BADLANDS_DECREASED_CHANCE = 3;
   private static final int SURROUNDING_BLOCKS_PLAY_SOUND_THRESHOLD = 3;
   private static final int SURROUNDING_BLOCKS_DISTANCE_HORIZONTAL_CHECK = 8;
   private static final int SURROUNDING_BLOCKS_DISTANCE_VERTICAL_CHECK = 5;
   private static final int HORIZONTAL_DIRECTIONS = 4;

   public static void playAmbientSandSounds(Level p_416243_, BlockPos p_416099_, RandomSource p_415930_) {
      if (p_416243_.getBlockState(p_416099_.above()).is(Blocks.AIR) && p_415930_.nextInt(2100) == 0 && shouldPlayAmbientSandSound(p_416243_, p_416099_)) {
         p_416243_.playLocalSound(p_416099_.getX(), p_416099_.getY(), p_416099_.getZ(), SoundEvents.SAND_IDLE, SoundSource.AMBIENT, 1.0F, 1.0F, false);
      }
   }

   public static void playAmbientDryGrassSounds(Level p_415932_, BlockPos p_416532_, RandomSource p_416610_) {
      if (p_416610_.nextInt(200) == 0 && shouldPlayDesertDryVegetationBlockSounds(p_415932_, p_416532_.below())) {
         p_415932_.playPlayerSound(SoundEvents.DRY_GRASS, SoundSource.AMBIENT, 1.0F, 1.0F);
      }
   }

   public static void playAmbientDeadBushSounds(Level p_416134_, BlockPos p_416323_, RandomSource p_415564_) {
      if (p_415564_.nextInt(130) == 0) {
         BlockState blockstate = p_416134_.getBlockState(p_416323_.below());
         if ((blockstate.is(Blocks.RED_SAND) || blockstate.is(BlockTags.TERRACOTTA)) && p_415564_.nextInt(3) != 0) {
            return;
         }

         if (shouldPlayDesertDryVegetationBlockSounds(p_416134_, p_416323_.below())) {
            p_416134_.playLocalSound(p_416323_.getX(), p_416323_.getY(), p_416323_.getZ(), SoundEvents.DEAD_BUSH_IDLE, SoundSource.AMBIENT, 1.0F, 1.0F, false);
         }
      }
   }

   public static boolean shouldPlayDesertDryVegetationBlockSounds(Level p_415994_, BlockPos p_416398_) {
      return p_415994_.getBlockState(p_416398_).is(BlockTags.TRIGGERS_AMBIENT_DESERT_DRY_VEGETATION_BLOCK_SOUNDS)
         && p_415994_.getBlockState(p_416398_.below()).is(BlockTags.TRIGGERS_AMBIENT_DESERT_DRY_VEGETATION_BLOCK_SOUNDS);
   }

   private static boolean shouldPlayAmbientSandSound(Level p_415908_, BlockPos p_415910_) {
      int i = 0;
      int j = 0;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_415910_.mutable();

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         blockpos$mutableblockpos.set(p_415910_).move(direction, 8);
         if (columnContainsTriggeringBlock(p_415908_, blockpos$mutableblockpos) && i++ >= 3) {
            return true;
         }

         j++;
         int k = 4 - j;
         int l = k + i;
         boolean flag = l >= 3;
         if (!flag) {
            return false;
         }
      }

      return false;
   }

   private static boolean columnContainsTriggeringBlock(Level p_415875_, BlockPos.MutableBlockPos p_416428_) {
      int i = p_415875_.getHeight(Heightmap.Types.WORLD_SURFACE, p_416428_) - 1;
      if (Math.abs(i - p_416428_.getY()) > 5) {
         p_416428_.move(Direction.UP, 6);
         BlockState blockstate1 = p_415875_.getBlockState(p_416428_);
         p_416428_.move(Direction.DOWN);

         for (int j = 0; j < 10; j++) {
            BlockState blockstate = p_415875_.getBlockState(p_416428_);
            if (blockstate1.isAir() && canTriggerAmbientDesertSandSounds(blockstate)) {
               return true;
            }

            blockstate1 = blockstate;
            p_416428_.move(Direction.DOWN);
         }

         return false;
      } else {
         boolean flag = p_415875_.getBlockState(p_416428_.setY(i + 1)).isAir();
         return flag && canTriggerAmbientDesertSandSounds(p_415875_.getBlockState(p_416428_.setY(i)));
      }
   }

   private static boolean canTriggerAmbientDesertSandSounds(BlockState p_416367_) {
      return p_416367_.is(BlockTags.TRIGGERS_AMBIENT_DESERT_SAND_BLOCK_SOUNDS);
   }
}
