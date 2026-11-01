package net.minecraft.world.entity.ai.goal;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.util.profiling.ProfilerFiller;

public class GoalSelector {
   private static final WrappedGoal NO_GOAL = new WrappedGoal(Integer.MAX_VALUE, new Goal() {
      @Override
      public boolean canUse() {
         return false;
      }
   }) {
      @Override
      public boolean isRunning() {
         return false;
      }
   };
   private final Map<Goal.Flag, WrappedGoal> lockedFlags = new EnumMap<>(Goal.Flag.class);
   private final Set<WrappedGoal> availableGoals = new ObjectLinkedOpenHashSet();
   private final Supplier<ProfilerFiller> profiler;
   private final EnumSet<Goal.Flag> disabledFlags = EnumSet.noneOf(Goal.Flag.class);

   public GoalSelector(Supplier<ProfilerFiller> p_25351_) {
      this.profiler = p_25351_;
   }

   public void addGoal(int p_25353_, Goal p_25354_) {
      this.availableGoals.add(new WrappedGoal(p_25353_, p_25354_));
   }

   @VisibleForTesting
   public void removeAllGoals(Predicate<Goal> p_262575_) {
      this.availableGoals.removeIf(p_262564_ -> p_262575_.test(p_262564_.getGoal()));
   }

   public void removeGoal(Goal p_25364_) {
      for (WrappedGoal $$1 : this.availableGoals) {
         if ($$1.getGoal() == p_25364_ && $$1.isRunning()) {
            $$1.stop();
         }
      }

      this.availableGoals.removeIf(p_25378_ -> p_25378_.getGoal() == p_25364_);
   }

   private static boolean goalContainsAnyFlags(WrappedGoal p_186076_, EnumSet<Goal.Flag> p_186077_) {
      for (Goal.Flag $$2 : p_186076_.getFlags()) {
         if (p_186077_.contains($$2)) {
            return true;
         }
      }

      return false;
   }

   private static boolean goalCanBeReplacedForAllFlags(WrappedGoal p_186079_, Map<Goal.Flag, WrappedGoal> p_186080_) {
      for (Goal.Flag $$2 : p_186079_.getFlags()) {
         if (!p_186080_.getOrDefault($$2, NO_GOAL).canBeReplacedBy(p_186079_)) {
            return false;
         }
      }

      return true;
   }

   public void tick() {
      ProfilerFiller $$0 = this.profiler.get();
      $$0.push("goalCleanup");

      for (WrappedGoal $$1 : this.availableGoals) {
         if ($$1.isRunning() && (goalContainsAnyFlags($$1, this.disabledFlags) || !$$1.canContinueToUse())) {
            $$1.stop();
         }
      }

      this.lockedFlags.entrySet().removeIf(p_316007_ -> !p_316007_.getValue().isRunning());
      $$0.pop();
      $$0.push("goalUpdate");

      for (WrappedGoal $$2 : this.availableGoals) {
         if (!$$2.isRunning() && !goalContainsAnyFlags($$2, this.disabledFlags) && goalCanBeReplacedForAllFlags($$2, this.lockedFlags) && $$2.canUse()) {
            for (Goal.Flag $$3 : $$2.getFlags()) {
               WrappedGoal $$4 = this.lockedFlags.getOrDefault($$3, NO_GOAL);
               $$4.stop();
               this.lockedFlags.put($$3, $$2);
            }

            $$2.start();
         }
      }

      $$0.pop();
      this.tickRunningGoals(true);
   }

   public void tickRunningGoals(boolean p_186082_) {
      ProfilerFiller $$1 = this.profiler.get();
      $$1.push("goalTick");

      for (WrappedGoal $$2 : this.availableGoals) {
         if ($$2.isRunning() && (p_186082_ || $$2.requiresUpdateEveryTick())) {
            $$2.tick();
         }
      }

      $$1.pop();
   }

   public Set<WrappedGoal> getAvailableGoals() {
      return this.availableGoals;
   }

   public void disableControlFlag(Goal.Flag p_25356_) {
      this.disabledFlags.add(p_25356_);
   }

   public void enableControlFlag(Goal.Flag p_25375_) {
      this.disabledFlags.remove(p_25375_);
   }

   public void setControlFlag(Goal.Flag p_25361_, boolean p_25362_) {
      if (p_25362_) {
         this.enableControlFlag(p_25361_);
      } else {
         this.disableControlFlag(p_25361_);
      }
   }
}
