package net.minecraft.world.entity.ai.goal;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public class MoveBackToVillageGoal extends RandomStrollGoal {
   private static final int MAX_XZ_DIST = 10;
   private static final int MAX_Y_DIST = 7;

   public MoveBackToVillageGoal(PathfinderMob p_25568_, double p_25569_, boolean p_25570_) {
      super(p_25568_, p_25569_, 10, p_25570_);
   }

   @Override
   public boolean canUse() {
      ServerLevel $$0 = (ServerLevel)this.mob.level();
      BlockPos $$1 = this.mob.blockPosition();
      return $$0.isVillage($$1) ? false : super.canUse();
   }

   @Nullable
   @Override
   protected Vec3 getPosition() {
      ServerLevel $$0 = (ServerLevel)this.mob.level();
      BlockPos $$1 = this.mob.blockPosition();
      SectionPos $$2 = SectionPos.of($$1);
      SectionPos $$3 = BehaviorUtils.findSectionClosestToVillage($$0, $$2, 2);
      return $$3 != $$2 ? DefaultRandomPos.getPosTowards(this.mob, 10, 7, Vec3.atBottomCenterOf($$3.center()), (float) (Math.PI / 2)) : null;
   }
}
