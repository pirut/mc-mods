package net.neoforged.neoforge.event.entity.living;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.EntityEvent;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class MobSpawnEvent extends EntityEvent {
   private final ServerLevelAccessor level;
   private final double x;
   private final double y;
   private final double z;

   @Internal
   protected MobSpawnEvent(Mob mob, ServerLevelAccessor level, double x, double y, double z) {
      super(mob);
      this.level = level;
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public Mob getEntity() {
      return (Mob)super.getEntity();
   }

   public ServerLevelAccessor getLevel() {
      return this.level;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public static class PositionCheck extends MobSpawnEvent {
      @Nullable
      private final BaseSpawner spawner;
      private final MobSpawnType spawnType;
      private MobSpawnEvent.PositionCheck.Result result = MobSpawnEvent.PositionCheck.Result.DEFAULT;

      public PositionCheck(Mob mob, ServerLevelAccessor level, MobSpawnType spawnType, @Nullable BaseSpawner spawner) {
         super(mob, level, mob.getX(), mob.getY(), mob.getZ());
         this.spawnType = spawnType;
         this.spawner = spawner;
      }

      @Nullable
      public BaseSpawner getSpawner() {
         return this.spawner;
      }

      public MobSpawnType getSpawnType() {
         return this.spawnType;
      }

      public void setResult(MobSpawnEvent.PositionCheck.Result result) {
         this.result = result;
      }

      public MobSpawnEvent.PositionCheck.Result getResult() {
         return this.result;
      }

      public static enum Result {
         SUCCEED,
         DEFAULT,
         FAIL;
      }
   }

   public static class SpawnPlacementCheck extends Event {
      private final EntityType<?> entityType;
      private final ServerLevelAccessor level;
      private final MobSpawnType spawnType;
      private final BlockPos pos;
      private final RandomSource random;
      private final boolean defaultResult;
      private MobSpawnEvent.SpawnPlacementCheck.Result result = MobSpawnEvent.SpawnPlacementCheck.Result.DEFAULT;

      @Internal
      public SpawnPlacementCheck(
         EntityType<?> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random, boolean defaultResult
      ) {
         this.entityType = entityType;
         this.level = level;
         this.spawnType = spawnType;
         this.pos = pos;
         this.random = random;
         this.defaultResult = defaultResult;
      }

      public EntityType<?> getEntityType() {
         return this.entityType;
      }

      public ServerLevelAccessor getLevel() {
         return this.level;
      }

      public MobSpawnType getSpawnType() {
         return this.spawnType;
      }

      public BlockPos getPos() {
         return this.pos;
      }

      public RandomSource getRandom() {
         return this.random;
      }

      public boolean getDefaultResult() {
         return this.defaultResult;
      }

      public void setResult(MobSpawnEvent.SpawnPlacementCheck.Result result) {
         this.result = result;
      }

      public MobSpawnEvent.SpawnPlacementCheck.Result getResult() {
         return this.result;
      }

      public boolean getPlacementCheckResult() {
         return this.result == MobSpawnEvent.SpawnPlacementCheck.Result.SUCCEED
            ? true
            : this.result == MobSpawnEvent.SpawnPlacementCheck.Result.DEFAULT && this.getDefaultResult();
      }

      public static enum Result {
         SUCCEED,
         DEFAULT,
         FAIL;
      }
   }
}
