package net.minecraft.world.entity.ai.goal;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;

public class GolemRandomStrollInVillageGoal extends RandomStrollGoal {
   private static final int POI_SECTION_SCAN_RADIUS = 2;
   private static final int VILLAGER_SCAN_RADIUS = 32;
   private static final int RANDOM_POS_XY_DISTANCE = 10;
   private static final int RANDOM_POS_Y_DISTANCE = 7;

   public GolemRandomStrollInVillageGoal(PathfinderMob p_25398_, double p_25399_) {
      super(p_25398_, p_25399_, 240, false);
   }

   @Nullable
   @Override
   protected Vec3 getPosition() {
      float $$0 = this.mob.level().random.nextFloat();
      if (this.mob.level().random.nextFloat() < 0.3F) {
         return this.getPositionTowardsAnywhere();
      } else {
         Vec3 $$1;
         if ($$0 < 0.7F) {
            $$1 = this.getPositionTowardsVillagerWhoWantsGolem();
            if ($$1 == null) {
               $$1 = this.getPositionTowardsPoi();
            }
         } else {
            $$1 = this.getPositionTowardsPoi();
            if ($$1 == null) {
               $$1 = this.getPositionTowardsVillagerWhoWantsGolem();
            }
         }

         return $$1 == null ? this.getPositionTowardsAnywhere() : $$1;
      }
   }

   @Nullable
   private Vec3 getPositionTowardsAnywhere() {
      return LandRandomPos.getPos(this.mob, 10, 7);
   }

   @Nullable
   private Vec3 getPositionTowardsVillagerWhoWantsGolem() {
      ServerLevel $$0 = (ServerLevel)this.mob.level();
      List<Villager> $$1 = $$0.getEntities(EntityType.VILLAGER, this.mob.getBoundingBox().inflate(32.0), this::doesVillagerWantGolem);
      if ($$1.isEmpty()) {
         return null;
      } else {
         Villager $$2 = $$1.get(this.mob.level().random.nextInt($$1.size()));
         Vec3 $$3 = $$2.position();
         return LandRandomPos.getPosTowards(this.mob, 10, 7, $$3);
      }
   }

   @Nullable
   private Vec3 getPositionTowardsPoi() {
      SectionPos $$0 = this.getRandomVillageSection();
      if ($$0 == null) {
         return null;
      } else {
         BlockPos $$1 = this.getRandomPoiWithinSection($$0);
         return $$1 == null ? null : LandRandomPos.getPosTowards(this.mob, 10, 7, Vec3.atBottomCenterOf($$1));
      }
   }

   @Nullable
   private SectionPos getRandomVillageSection() {
      ServerLevel $$0 = (ServerLevel)this.mob.level();
      List<SectionPos> $$1 = SectionPos.cube(SectionPos.of(this.mob), 2).filter(p_25402_ -> $$0.sectionsToVillage(p_25402_) == 0).collect(Collectors.toList());
      return $$1.isEmpty() ? null : $$1.get($$0.random.nextInt($$1.size()));
   }

   @Nullable
   private BlockPos getRandomPoiWithinSection(SectionPos p_25408_) {
      ServerLevel $$1 = (ServerLevel)this.mob.level();
      PoiManager $$2 = $$1.getPoiManager();
      List<BlockPos> $$3 = $$2.getInRange(p_217747_ -> true, p_25408_.center(), 8, PoiManager.Occupancy.IS_OCCUPIED)
         .map(PoiRecord::getPos)
         .collect(Collectors.toList());
      return $$3.isEmpty() ? null : $$3.get($$1.random.nextInt($$3.size()));
   }

   private boolean doesVillagerWantGolem(Villager p_25406_) {
      return p_25406_.wantsToSpawnGolem(this.mob.level().getGameTime());
   }
}
