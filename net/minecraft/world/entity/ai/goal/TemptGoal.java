package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class TemptGoal extends Goal {
   private static final TargetingConditions TEMPT_TARGETING = TargetingConditions.forNonCombat().ignoreLineOfSight();
   private static final double DEFAULT_STOP_DISTANCE = 2.5;
   private final TargetingConditions targetingConditions;
   protected final Mob mob;
   protected final double speedModifier;
   private double px;
   private double py;
   private double pz;
   private double pRotX;
   private double pRotY;
   @Nullable
   protected Player player;
   private int calmDown;
   private boolean isRunning;
   private final Predicate<ItemStack> items;
   private final boolean canScare;
   private final double stopDistance;

   public TemptGoal(PathfinderMob p_25939_, double p_25940_, Predicate<ItemStack> p_335901_, boolean p_25942_) {
      this((Mob)p_25939_, p_25940_, p_335901_, p_25942_, 2.5);
   }

   public TemptGoal(PathfinderMob p_415747_, double p_415923_, Predicate<ItemStack> p_416157_, boolean p_415791_, double p_416070_) {
      this((Mob)p_415747_, p_415923_, p_416157_, p_415791_, p_416070_);
   }

   TemptGoal(Mob p_415998_, double p_415799_, Predicate<ItemStack> p_416498_, boolean p_416090_, double p_416515_) {
      this.mob = p_415998_;
      this.speedModifier = p_415799_;
      this.items = p_416498_;
      this.canScare = p_416090_;
      this.stopDistance = p_416515_;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      this.targetingConditions = TEMPT_TARGETING.copy().selector((p_376921_, p_376788_) -> this.shouldFollow(p_376921_));
   }

   @Override
   public boolean canUse() {
      if (this.calmDown > 0) {
         this.calmDown--;
         return false;
      } else {
         this.player = getServerLevel(this.mob).getNearestPlayer(this.targetingConditions.range(this.mob.getAttributeValue(Attributes.TEMPT_RANGE)), this.mob);
         return this.player != null;
      }
   }

   private boolean shouldFollow(LivingEntity p_148139_) {
      return this.items.test(p_148139_.getMainHandItem()) || this.items.test(p_148139_.getOffhandItem());
   }

   @Override
   public boolean canContinueToUse() {
      if (this.canScare()) {
         if (this.mob.distanceToSqr(this.player) < 36.0) {
            if (this.player.distanceToSqr(this.px, this.py, this.pz) > 0.010000000000000002) {
               return false;
            }

            if (Math.abs(this.player.getXRot() - this.pRotX) > 5.0 || Math.abs(this.player.getYRot() - this.pRotY) > 5.0) {
               return false;
            }
         } else {
            this.px = this.player.getX();
            this.py = this.player.getY();
            this.pz = this.player.getZ();
         }

         this.pRotX = this.player.getXRot();
         this.pRotY = this.player.getYRot();
      }

      return this.canUse();
   }

   protected boolean canScare() {
      return this.canScare;
   }

   @Override
   public void start() {
      this.px = this.player.getX();
      this.py = this.player.getY();
      this.pz = this.player.getZ();
      this.isRunning = true;
   }

   @Override
   public void stop() {
      this.player = null;
      this.stopNavigation();
      this.calmDown = reducedTickDelay(100);
      this.isRunning = false;
   }

   @Override
   public void tick() {
      this.mob.getLookControl().setLookAt(this.player, this.mob.getMaxHeadYRot() + 20, this.mob.getMaxHeadXRot());
      if (this.mob.distanceToSqr(this.player) < this.stopDistance * this.stopDistance) {
         this.stopNavigation();
      } else {
         this.navigateTowards(this.player);
      }
   }

   protected void stopNavigation() {
      this.mob.getNavigation().stop();
   }

   protected void navigateTowards(Player p_416293_) {
      this.mob.getNavigation().moveTo(p_416293_, this.speedModifier);
   }

   public boolean isRunning() {
      return this.isRunning;
   }

   public static class ForNonPathfinders extends TemptGoal {
      public ForNonPathfinders(Mob p_415602_, double p_416457_, Predicate<ItemStack> p_415698_, boolean p_416268_, double p_416020_) {
         super(p_415602_, p_416457_, p_415698_, p_416268_, p_416020_);
      }

      @Override
      protected void stopNavigation() {
         this.mob.getMoveControl().setWait();
      }

      @Override
      protected void navigateTowards(Player p_415789_) {
         Vec3 vec3 = p_415789_.getEyePosition().subtract(this.mob.position()).scale(this.mob.getRandom().nextDouble()).add(this.mob.position());
         this.mob.getMoveControl().setWantedPosition(vec3.x, vec3.y, vec3.z, this.speedModifier);
      }
   }
}
