package net.neoforged.neoforge.event.entity.living;

import com.mojang.datafixers.util.Either;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public class FinalizeSpawnEvent extends MobSpawnEvent implements ICancellableEvent {
   private final MobSpawnType spawnType;
   @Nullable
   private final Either<BlockEntity, Entity> spawner;
   private DifficultyInstance difficulty;
   @Nullable
   private SpawnGroupData spawnData;

   @Internal
   public FinalizeSpawnEvent(
      Mob entity,
      ServerLevelAccessor level,
      double x,
      double y,
      double z,
      DifficultyInstance difficulty,
      MobSpawnType spawnType,
      @Nullable SpawnGroupData spawnData,
      @Nullable Either<BlockEntity, Entity> spawner
   ) {
      super(entity, level, x, y, z);
      this.difficulty = difficulty;
      this.spawnType = spawnType;
      this.spawnData = spawnData;
      this.spawner = spawner;
   }

   public DifficultyInstance getDifficulty() {
      return this.difficulty;
   }

   public void setDifficulty(DifficultyInstance inst) {
      this.difficulty = inst;
   }

   public MobSpawnType getSpawnType() {
      return this.spawnType;
   }

   @Nullable
   public SpawnGroupData getSpawnData() {
      return this.spawnData;
   }

   public void setSpawnData(@Nullable SpawnGroupData data) {
      this.spawnData = data;
   }

   @Nullable
   public Either<BlockEntity, Entity> getSpawner() {
      return this.spawner;
   }

   public void setSpawnCancelled(boolean cancel) {
      this.getEntity().setSpawnCancelled(cancel);
   }

   public boolean isSpawnCancelled() {
      return this.getEntity().isSpawnCancelled();
   }
}
