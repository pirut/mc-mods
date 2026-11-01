package net.minecraft.world.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface EntityGetter {
   List<Entity> getEntities(@Nullable Entity var1, AABB var2, Predicate<? super Entity> var3);

   <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> var1, AABB var2, Predicate<? super T> var3);

   default <T extends Entity> List<T> getEntitiesOfClass(Class<T> p_45979_, AABB p_45980_, Predicate<? super T> p_45981_) {
      return this.getEntities(EntityTypeTest.forClass(p_45979_), p_45980_, p_45981_);
   }

   List<? extends Player> players();

   default List<Entity> getEntities(@Nullable Entity p_45934_, AABB p_45935_) {
      return this.getEntities(p_45934_, p_45935_, EntitySelector.NO_SPECTATORS);
   }

   default boolean isUnobstructed(@Nullable Entity p_45939_, VoxelShape p_45940_) {
      if (p_45940_.isEmpty()) {
         return true;
      } else {
         for (Entity $$2 : this.getEntities(p_45939_, p_45940_.bounds())) {
            if (!$$2.isRemoved()
               && $$2.blocksBuilding
               && (p_45939_ == null || !$$2.isPassengerOfSameVehicle(p_45939_))
               && Shapes.joinIsNotEmpty(p_45940_, Shapes.create($$2.getBoundingBox()), BooleanOp.AND)) {
               return false;
            }
         }

         return true;
      }
   }

   default <T extends Entity> List<T> getEntitiesOfClass(Class<T> p_45977_, AABB p_45978_) {
      return this.getEntitiesOfClass(p_45977_, p_45978_, EntitySelector.NO_SPECTATORS);
   }

   default List<VoxelShape> getEntityCollisions(@Nullable Entity p_186451_, AABB p_186452_) {
      if (p_186452_.getSize() < 1.0E-7) {
         return List.of();
      } else {
         Predicate<Entity> $$2 = p_186451_ == null ? EntitySelector.CAN_BE_COLLIDED_WITH : EntitySelector.NO_SPECTATORS.and(p_186451_::canCollideWith);
         List<Entity> $$3 = this.getEntities(p_186451_, p_186452_.inflate(1.0E-7), $$2);
         if ($$3.isEmpty()) {
            return List.of();
         } else {
            Builder<VoxelShape> $$4 = ImmutableList.builderWithExpectedSize($$3.size());

            for (Entity $$5 : $$3) {
               $$4.add(Shapes.create($$5.getBoundingBox()));
            }

            return $$4.build();
         }
      }
   }

   @Nullable
   default Player getNearestPlayer(double p_45919_, double p_45920_, double p_45921_, double p_45922_, @Nullable Predicate<Entity> p_45923_) {
      double $$5 = -1.0;
      Player $$6 = null;

      for (Player $$7 : this.players()) {
         if (p_45923_ == null || p_45923_.test($$7)) {
            double $$8 = $$7.distanceToSqr(p_45919_, p_45920_, p_45921_);
            if ((p_45922_ < 0.0 || $$8 < p_45922_ * p_45922_) && ($$5 == -1.0 || $$8 < $$5)) {
               $$5 = $$8;
               $$6 = $$7;
            }
         }
      }

      return $$6;
   }

   @Nullable
   default Player getNearestPlayer(Entity p_45931_, double p_45932_) {
      return this.getNearestPlayer(p_45931_.getX(), p_45931_.getY(), p_45931_.getZ(), p_45932_, false);
   }

   @Nullable
   default Player getNearestPlayer(double p_45925_, double p_45926_, double p_45927_, double p_45928_, boolean p_45929_) {
      Predicate<Entity> $$5 = p_45929_ ? EntitySelector.NO_CREATIVE_OR_SPECTATOR : EntitySelector.NO_SPECTATORS;
      return this.getNearestPlayer(p_45925_, p_45926_, p_45927_, p_45928_, $$5);
   }

   default boolean hasNearbyAlivePlayer(double p_45915_, double p_45916_, double p_45917_, double p_45918_) {
      for (Player $$4 : this.players()) {
         if (EntitySelector.NO_SPECTATORS.test($$4) && EntitySelector.LIVING_ENTITY_STILL_ALIVE.test($$4)) {
            double $$5 = $$4.distanceToSqr(p_45915_, p_45916_, p_45917_);
            if (p_45918_ < 0.0 || $$5 < p_45918_ * p_45918_) {
               return true;
            }
         }
      }

      return false;
   }

   @Nullable
   default Player getNearestPlayer(TargetingConditions p_45947_, LivingEntity p_45948_) {
      return this.getNearestEntity(this.players(), p_45947_, p_45948_, p_45948_.getX(), p_45948_.getY(), p_45948_.getZ());
   }

   @Nullable
   default Player getNearestPlayer(TargetingConditions p_45950_, LivingEntity p_45951_, double p_45952_, double p_45953_, double p_45954_) {
      return this.getNearestEntity(this.players(), p_45950_, p_45951_, p_45952_, p_45953_, p_45954_);
   }

   @Nullable
   default Player getNearestPlayer(TargetingConditions p_45942_, double p_45943_, double p_45944_, double p_45945_) {
      return this.getNearestEntity(this.players(), p_45942_, null, p_45943_, p_45944_, p_45945_);
   }

   @Nullable
   default <T extends LivingEntity> T getNearestEntity(
      Class<? extends T> p_45964_,
      TargetingConditions p_45965_,
      @Nullable LivingEntity p_45966_,
      double p_45967_,
      double p_45968_,
      double p_45969_,
      AABB p_45970_
   ) {
      return this.getNearestEntity(this.getEntitiesOfClass(p_45964_, p_45970_, p_186454_ -> true), p_45965_, p_45966_, p_45967_, p_45968_, p_45969_);
   }

   @Nullable
   default <T extends LivingEntity> T getNearestEntity(
      List<? extends T> p_45983_, TargetingConditions p_45984_, @Nullable LivingEntity p_45985_, double p_45986_, double p_45987_, double p_45988_
   ) {
      double $$6 = -1.0;
      T $$7 = null;

      for (T $$8 : p_45983_) {
         if (p_45984_.test(p_45985_, $$8)) {
            double $$9 = $$8.distanceToSqr(p_45986_, p_45987_, p_45988_);
            if ($$6 == -1.0 || $$9 < $$6) {
               $$6 = $$9;
               $$7 = $$8;
            }
         }
      }

      return $$7;
   }

   default List<Player> getNearbyPlayers(TargetingConditions p_45956_, LivingEntity p_45957_, AABB p_45958_) {
      List<Player> $$3 = Lists.newArrayList();

      for (Player $$4 : this.players()) {
         if (p_45958_.contains($$4.getX(), $$4.getY(), $$4.getZ()) && p_45956_.test(p_45957_, $$4)) {
            $$3.add($$4);
         }
      }

      return $$3;
   }

   default <T extends LivingEntity> List<T> getNearbyEntities(Class<T> p_45972_, TargetingConditions p_45973_, LivingEntity p_45974_, AABB p_45975_) {
      List<T> $$4 = this.getEntitiesOfClass(p_45972_, p_45975_, p_186450_ -> true);
      List<T> $$5 = Lists.newArrayList();

      for (T $$6 : $$4) {
         if (p_45973_.test(p_45974_, $$6)) {
            $$5.add($$6);
         }
      }

      return $$5;
   }

   @Nullable
   default Player getPlayerByUUID(UUID p_46004_) {
      for (int $$1 = 0; $$1 < this.players().size(); $$1++) {
         Player $$2 = this.players().get($$1);
         if (p_46004_.equals($$2.getUUID())) {
            return $$2;
         }
      }

      return null;
   }
}
