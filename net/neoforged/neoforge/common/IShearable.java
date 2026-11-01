package net.neoforged.neoforge.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface IShearable {
   default boolean isShearable(@Nullable Player player, ItemStack item, Level level, BlockPos pos) {
      return !(this instanceof Shearable shearable && !shearable.readyForShearing());
   }

   default List<ItemStack> onSheared(@Nullable Player player, ItemStack item, Level level, BlockPos pos) {
      if (this instanceof LivingEntity entity && this instanceof Shearable shearable && level instanceof ServerLevel serverLevel) {
         Collection<ItemEntity> previous = entity.captureDrops(new ArrayList<>());
         shearable.shear(serverLevel, player == null ? SoundSource.BLOCKS : SoundSource.PLAYERS, item);
         return entity.captureDrops(previous).stream().map(ItemEntity::getItem).toList();
      } else {
         return Collections.emptyList();
      }
   }

   default void spawnShearedDrop(ServerLevel level, BlockPos pos, ItemStack drop) {
      if (this instanceof SnowGolem golem) {
         golem.spawnAtLocation(level, drop, golem.getEyeHeight());
      } else if (this instanceof Bogged bogged) {
         bogged.spawnAtLocation(level, drop, bogged.getBbHeight());
      } else if (this instanceof MushroomCow cow) {
         ItemEntity itemEntity = cow.spawnAtLocation(level, drop, cow.getBbHeight());
         if (itemEntity != null) {
            itemEntity.setNoPickUpDelay();
         }
      } else if (this instanceof Entity entity) {
         ItemEntity itemEntity = entity.spawnAtLocation(level, drop, 1.0F);
         if (itemEntity != null) {
            RandomSource rand = entity.getRandom();
            Vec3 newDelta = itemEntity.getDeltaMovement()
               .add((rand.nextFloat() - rand.nextFloat()) * 0.1F, rand.nextFloat() * 0.05F, (rand.nextFloat() - rand.nextFloat()) * 0.1F);
            itemEntity.setDeltaMovement(newDelta);
         }
      } else {
         level.addFreshEntity(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), drop));
      }
   }
}
