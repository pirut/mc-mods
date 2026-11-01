package net.minecraft.world.entity.ai.goal.target;

import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class DefendVillageTargetGoal extends TargetGoal {
   private final IronGolem golem;
   @Nullable
   private LivingEntity potentialTarget;
   private final TargetingConditions attackTargeting = TargetingConditions.forCombat().range(64.0);

   public DefendVillageTargetGoal(IronGolem p_26029_) {
      super(p_26029_, false, true);
      this.golem = p_26029_;
      this.setFlags(EnumSet.of(Goal.Flag.TARGET));
   }

   @Override
   public boolean canUse() {
      AABB $$0 = this.golem.getBoundingBox().inflate(10.0, 8.0, 10.0);
      List<? extends LivingEntity> $$1 = this.golem.level().getNearbyEntities(Villager.class, this.attackTargeting, this.golem, $$0);
      List<Player> $$2 = this.golem.level().getNearbyPlayers(this.attackTargeting, this.golem, $$0);

      for (LivingEntity $$3 : $$1) {
         Villager $$4 = (Villager)$$3;

         for (Player $$5 : $$2) {
            int $$6 = $$4.getPlayerReputation($$5);
            if ($$6 <= -100) {
               this.potentialTarget = $$5;
            }
         }
      }

      return this.potentialTarget == null
         ? false
         : !(this.potentialTarget instanceof Player) || !this.potentialTarget.isSpectator() && !((Player)this.potentialTarget).isCreative();
   }

   @Override
   public void start() {
      this.golem.setTarget(this.potentialTarget);
      super.start();
   }
}
