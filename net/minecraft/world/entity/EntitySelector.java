package net.minecraft.world.entity;

import com.google.common.base.Predicates;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Team;

public final class EntitySelector {
   public static final Predicate<Entity> ENTITY_STILL_ALIVE = Entity::isAlive;
   public static final Predicate<Entity> LIVING_ENTITY_STILL_ALIVE = p_20442_ -> p_20442_.isAlive() && p_20442_ instanceof LivingEntity;
   public static final Predicate<Entity> ENTITY_NOT_BEING_RIDDEN = p_20440_ -> p_20440_.isAlive() && !p_20440_.isVehicle() && !p_20440_.isPassenger();
   public static final Predicate<Entity> CONTAINER_ENTITY_SELECTOR = p_20438_ -> p_20438_ instanceof Container && p_20438_.isAlive();
   public static final Predicate<Entity> NO_CREATIVE_OR_SPECTATOR = p_20436_ -> !(p_20436_ instanceof Player)
      || !p_20436_.isSpectator() && !((Player)p_20436_).isCreative();
   public static final Predicate<Entity> NO_SPECTATORS = p_20434_ -> !p_20434_.isSpectator();
   public static final Predicate<Entity> CAN_BE_COLLIDED_WITH = NO_SPECTATORS.and(Entity::canBeCollidedWith);

   private EntitySelector() {
   }

   public static Predicate<Entity> withinDistance(double p_20411_, double p_20412_, double p_20413_, double p_20414_) {
      double $$4 = p_20414_ * p_20414_;
      return p_20420_ -> p_20420_ != null && p_20420_.distanceToSqr(p_20411_, p_20412_, p_20413_) <= $$4;
   }

   public static Predicate<Entity> pushableBy(Entity p_20422_) {
      Team $$1 = p_20422_.getTeam();
      Team.CollisionRule $$2 = $$1 == null ? Team.CollisionRule.ALWAYS : $$1.getCollisionRule();
      return (Predicate<Entity>)($$2 == Team.CollisionRule.NEVER
         ? Predicates.alwaysFalse()
         : NO_SPECTATORS.and(
            p_20430_ -> {
               if (!p_20430_.isPushable()) {
                  return false;
               } else if (!p_20422_.level().isClientSide || p_20430_ instanceof Player && ((Player)p_20430_).isLocalPlayer()) {
                  Team $$4 = p_20430_.getTeam();
                  Team.CollisionRule $$5 = $$4 == null ? Team.CollisionRule.ALWAYS : $$4.getCollisionRule();
                  if ($$5 == Team.CollisionRule.NEVER) {
                     return false;
                  } else {
                     boolean $$6 = $$1 != null && $$1.isAlliedTo($$4);
                     return ($$2 == Team.CollisionRule.PUSH_OWN_TEAM || $$5 == Team.CollisionRule.PUSH_OWN_TEAM) && $$6
                        ? false
                        : $$2 != Team.CollisionRule.PUSH_OTHER_TEAMS && $$5 != Team.CollisionRule.PUSH_OTHER_TEAMS || $$6;
                  }
               } else {
                  return false;
               }
            }
         ));
   }

   public static Predicate<Entity> notRiding(Entity p_20432_) {
      return p_20425_ -> {
         while (p_20425_.isPassenger()) {
            p_20425_ = p_20425_.getVehicle();
            if (p_20425_ == p_20432_) {
               return false;
            }
         }

         return true;
      };
   }

   public static class MobCanWearArmorEntitySelector implements Predicate<Entity> {
      private final ItemStack itemStack;

      public MobCanWearArmorEntitySelector(ItemStack p_20445_) {
         this.itemStack = p_20445_;
      }

      public boolean test(@Nullable Entity p_20447_) {
         if (!p_20447_.isAlive()) {
            return false;
         } else {
            return !(p_20447_ instanceof LivingEntity $$1) ? false : $$1.canTakeItem(this.itemStack);
         }
      }
   }
}
