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
   default Player getNearestPlayer(TargetingConditions p_376784_, LivingEntity p_376102_) {
      return this.getNearestEntity(this.players(), p_376784_, p_376102_, p_376102_.getX(), p_376102_.getY(), p_376102_.getZ());
   }

   @Nullable
   default Player getNearestPlayer(TargetingConditions p_376537_, LivingEntity p_376530_, double p_376306_, double p_376408_, double p_376302_) {
      return this.getNearestEntity(this.players(), p_376537_, p_376530_, p_376306_, p_376408_, p_376302_);
   }

   @Nullable
   default Player getNearestPlayer(TargetingConditions p_376724_, double p_376513_, double p_376719_, double p_376130_) {
      return this.getNearestEntity(this.players(), p_376724_, null, p_376513_, p_376719_, p_376130_);
   }

   @Nullable
   default <T extends LivingEntity> T getNearestEntity(
      Class<? extends T> p_376776_,
      TargetingConditions p_376521_,
      @Nullable LivingEntity p_376321_,
      double p_376605_,
      double p_376504_,
      double p_376604_,
      AABB p_376622_
   ) {
      return this.getNearestEntity(this.getEntitiesOfClass(p_376776_, p_376622_, p_376664_ -> true), p_376521_, p_376321_, p_376605_, p_376504_, p_376604_);
   }

   @Nullable
   default LivingEntity getNearestEntity(
      TagKey<EntityType<?>> p_437299_,
      TargetingConditions p_437268_,
      @Nullable LivingEntity p_437233_,
      double p_437231_,
      double p_437391_,
      double p_437403_,
      AABB p_437396_
   ) {
      double d0 = Double.MAX_VALUE;
      LivingEntity livingentity = null;

      for (LivingEntity livingentity1 : this.getEntitiesOfClass(LivingEntity.class, p_437396_, p_437182_ -> p_437182_.getType().is(p_437299_))) {
         if (p_437268_.test(this.getLevel(), p_437233_, livingentity1)) {
            double d1 = livingentity1.distanceToSqr(p_437231_, p_437391_, p_437403_);
            if (d1 < d0) {
               d0 = d1;
               livingentity = livingentity1;
            }
         }
      }

      return livingentity;
   }

   @Nullable
   default <T extends LivingEntity> T getNearestEntity(
      List<? extends T> p_376261_, TargetingConditions p_376489_, @Nullable LivingEntity p_376192_, double p_376174_, double p_376483_, double p_376256_
   ) {
      double d0 = -1.0;
      T t = null;

      for (T t1 : p_376261_) {
         if (p_376489_.test(this.getLevel(), p_376192_, t1)) {
            double d1 = t1.distanceToSqr(p_376174_, p_376483_, p_376256_);
            if (d0 == -1.0 || d1 < d0) {
               d0 = d1;
               t = t1;
            }
         }
      }

      return t;
   }

   default List<Player> getNearbyPlayers(TargetingConditions p_376095_, LivingEntity p_376466_, AABB p_376149_) {
      List<Player> list = new ArrayList<>();

      for (Player player : this.players()) {
         if (p_376149_.contains(player.getX(), player.getY(), player.getZ()) && p_376095_.test(this.getLevel(), p_376466_, player)) {
            list.add(player);
         }
      }

      return list;
   }

   default <T extends LivingEntity> List<T> getNearbyEntities(Class<T> p_376620_, TargetingConditions p_376110_, LivingEntity p_376539_, AABB p_376547_) {
      List<T> list = this.getEntitiesOfClass(p_376620_, p_376547_, p_376145_ -> true);
      List<T> list1 = new ArrayList<>();

      for (T t : list) {
         if (p_376110_.test(this.getLevel(), p_376539_, t)) {
            list1.add(t);
         }
      }

      return list1;
   }
}
