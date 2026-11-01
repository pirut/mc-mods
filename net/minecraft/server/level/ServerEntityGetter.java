package net.minecraft.server.level;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public interface ServerEntityGetter extends EntityGetter {
   ServerLevel getLevel();

   @Nullable
   default Player getNearestPlayer(TargetingConditions targetingConditions, LivingEntity source) {
      return this.getNearestEntity(this.players(), targetingConditions, source, source.getX(), source.getY(), source.getZ());
   }

   @Nullable
   default Player getNearestPlayer(TargetingConditions targetingConditions, LivingEntity source, double x, double y, double z) {
      return this.getNearestEntity(this.players(), targetingConditions, source, x, y, z);
   }

   @Nullable
   default Player getNearestPlayer(TargetingConditions targetingConditions, double x, double y, double z) {
      return this.getNearestEntity(this.players(), targetingConditions, null, x, y, z);
   }

   @Nullable
   default <T extends LivingEntity> T getNearestEntity(
      Class<? extends T> entityClass, TargetingConditions targetingConditions, @Nullable LivingEntity source, double x, double y, double z, AABB area
   ) {
      return this.getNearestEntity(this.getEntitiesOfClass(entityClass, area, livingEntity -> true), targetingConditions, source, x, y, z);
   }

   @Nullable
   default LivingEntity getNearestEntity(
      TagKey<EntityType<?>> tag, TargetingConditions targetingConditions, @Nullable LivingEntity source, double x, double y, double z, AABB area
   ) {
      double d = Double.MAX_VALUE;
      LivingEntity livingEntity = null;

      for (LivingEntity livingEntity2 : this.getEntitiesOfClass(LivingEntity.class, area, livingEntityx -> livingEntityx.getType().is(tag))) {
         if (targetingConditions.test(this.getLevel(), source, livingEntity2)) {
            double e = livingEntity2.distanceToSqr(x, y, z);
            if (e < d) {
               d = e;
               livingEntity = livingEntity2;
            }
         }
      }

      return livingEntity;
   }

   @Nullable
   default <T extends LivingEntity> T getNearestEntity(
      List<? extends T> entities, TargetingConditions targetingConditions, @Nullable LivingEntity source, double x, double y, double z
   ) {
      double d = -1.0;
      T livingEntity = null;

      for (T livingEntity2 : entities) {
         if (targetingConditions.test(this.getLevel(), source, livingEntity2)) {
            double e = livingEntity2.distanceToSqr(x, y, z);
            if (d == -1.0 || e < d) {
               d = e;
               livingEntity = livingEntity2;
            }
         }
      }

      return livingEntity;
   }

   default List<Player> getNearbyPlayers(TargetingConditions targetingConditions, LivingEntity source, AABB area) {
      List<Player> list = new ArrayList<>();

      for (Player player : this.players()) {
         if (area.contains(player.getX(), player.getY(), player.getZ()) && targetingConditions.test(this.getLevel(), source, player)) {
            list.add(player);
         }
      }

      return list;
   }

   default <T extends LivingEntity> List<T> getNearbyEntities(Class<T> entityClass, TargetingConditions targetingConditions, LivingEntity source, AABB area) {
      List<T> list = this.getEntitiesOfClass(entityClass, area, livingEntityx -> true);
      List<T> list2 = new ArrayList<>();

      for (T livingEntity : list) {
         if (targetingConditions.test(this.getLevel(), source, livingEntity)) {
            list2.add(livingEntity);
         }
      }

      return list2;
   }
}
