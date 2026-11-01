package net.minecraft.world.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface Leashable {
   String LEASH_TAG = "leash";
   double LEASH_TOO_FAR_DIST = 12.0;
   double LEASH_ELASTIC_DIST = 6.0;
   double MAXIMUM_ALLOWED_LEASHED_DIST = 16.0;
   Vec3 AXIS_SPECIFIC_ELASTICITY = new Vec3(0.8, 0.2, 0.8);
   float SPRING_DAMPENING = 0.7F;
   double TORSIONAL_ELASTICITY = 10.0;
   double STIFFNESS = 0.11;
   List<Vec3> ENTITY_ATTACHMENT_POINT = ImmutableList.of(new Vec3(0.0, 0.5, 0.5));
   List<Vec3> LEASHER_ATTACHMENT_POINT = ImmutableList.of(new Vec3(0.0, 0.5, 0.0));
   List<Vec3> SHARED_QUAD_ATTACHMENT_POINTS = ImmutableList.of(
      new Vec3(-0.5, 0.5, 0.5), new Vec3(-0.5, 0.5, -0.5), new Vec3(0.5, 0.5, -0.5), new Vec3(0.5, 0.5, 0.5)
   );

   @Nullable
   Leashable.LeashData getLeashData();

   void setLeashData(@Nullable Leashable.LeashData var1);

   default boolean isLeashed() {
      return this.getLeashData() != null && this.getLeashData().leashHolder != null;
   }

   default boolean mayBeLeashed() {
      return this.getLeashData() != null;
   }

   default boolean canHaveALeashAttachedTo(Entity p_418026_) {
      if (this == p_418026_) {
         return false;
      } else {
         return this.leashDistanceTo(p_418026_) > this.leashSnapDistance() ? false : this.canBeLeashed();
      }
   }

   default double leashDistanceTo(Entity p_418359_) {
      return p_418359_.getBoundingBox().getCenter().distanceTo(((Entity)this).getBoundingBox().getCenter());
   }

   default boolean canBeLeashed() {
      return true;
   }

   default void setDelayedLeashHolderId(int p_352387_) {
      this.setLeashData(new Leashable.LeashData(p_352387_));
      dropLeash((Entity & Leashable)this, false, false);
   }

   default void readLeashData(ValueInput p_422278_) {
      Leashable.LeashData leashable$leashdata = p_422278_.<Leashable.LeashData>read("leash", Leashable.LeashData.CODEC).orElse(null);
      if (this.getLeashData() != null && leashable$leashdata == null) {
         this.removeLeash();
      }

      this.setLeashData(leashable$leashdata);
   }

   default void writeLeashData(ValueOutput p_422090_, @Nullable Leashable.LeashData p_352363_) {
      p_422090_.storeNullable("leash", Leashable.LeashData.CODEC, p_352363_);
   }

   private static <E extends Entity & Leashable> void restoreLeashFromSave(E p_352354_, Leashable.LeashData p_352106_) {
      if (p_352106_.delayedLeashInfo != null && p_352354_.level() instanceof ServerLevel serverlevel) {
         Optional<UUID> optional1 = p_352106_.delayedLeashInfo.left();
         Optional<BlockPos> optional = p_352106_.delayedLeashInfo.right();
         if (optional1.isPresent()) {
            Entity entity = serverlevel.getEntity(optional1.get());
            if (entity != null) {
               setLeashedTo(p_352354_, entity, true);
               return;
            }
         } else if (optional.isPresent()) {
            setLeashedTo(p_352354_, LeashFenceKnotEntity.getOrCreateKnot(serverlevel, optional.get()), true);
            return;
         }

         if (p_352354_.tickCount > 100) {
            p_352354_.spawnAtLocation(serverlevel, Items.LEAD);
            p_352354_.setLeashData(null);
         }
      }
   }

   default void dropLeash() {
      dropLeash((Entity & Leashable)this, true, true);
   }

   default void removeLeash() {
      dropLeash((Entity & Leashable)this, true, false);
   }

   default void onLeashRemoved() {
   }

   private static <E extends Entity & Leashable> void dropLeash(E p_352163_, boolean p_352286_, boolean p_352272_) {
      Leashable.LeashData leashable$leashdata = p_352163_.getLeashData();
      if (leashable$leashdata != null && leashable$leashdata.leashHolder != null) {
         p_352163_.setLeashData(null);
         p_352163_.onLeashRemoved();
         if (p_352163_.level() instanceof ServerLevel serverlevel) {
            if (p_352272_) {
               p_352163_.spawnAtLocation(serverlevel, Items.LEAD);
            }

            if (p_352286_) {
               serverlevel.getChunkSource().sendToTrackingPlayers(p_352163_, new ClientboundSetEntityLinkPacket(p_352163_, null));
            }

            leashable$leashdata.leashHolder.notifyLeasheeRemoved(p_352163_);
         }
      }
   }

   static <E extends Entity & Leashable> void tickLeash(ServerLevel p_376374_, E p_352082_) {
      Leashable.LeashData leashable$leashdata = p_352082_.getLeashData();
      if (leashable$leashdata != null && leashable$leashdata.delayedLeashInfo != null) {
         restoreLeashFromSave(p_352082_, leashable$leashdata);
      }

      if (leashable$leashdata != null && leashable$leashdata.leashHolder != null) {
         if (!p_352082_.canInteractWithLevel() || !leashable$leashdata.leashHolder.canInteractWithLevel()) {
            if (p_376374_.getGameRules().get(GameRules.ENTITY_DROPS)) {
               p_352082_.dropLeash();
            } else {
               p_352082_.removeLeash();
            }
         }

         Entity entity = p_352082_.getLeashHolder();
         if (entity != null && entity.level() == p_352082_.level()) {
            double d0 = p_352082_.leashDistanceTo(entity);
            p_352082_.whenLeashedTo(entity);
            if (d0 > p_352082_.leashSnapDistance()) {
               p_376374_.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.LEAD_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
               p_352082_.leashTooFarBehaviour();
            } else if (d0 > p_352082_.leashElasticDistance() - entity.getBbWidth() - p_352082_.getBbWidth()
               && p_352082_.checkElasticInteractions(entity, leashable$leashdata)) {
               p_352082_.onElasticLeashPull();
            } else {
               p_352082_.closeRangeLeashBehaviour(entity);
            }

            p_352082_.setYRot((float)(p_352082_.getYRot() - leashable$leashdata.angularMomentum));
            leashable$leashdata.angularMomentum = leashable$leashdata.angularMomentum * angularFriction(p_352082_);
         }
      }
   }

   default void onElasticLeashPull() {
      Entity entity = (Entity)this;
      entity.checkFallDistanceAccumulation();
   }

   default double leashSnapDistance() {
      return 12.0;
   }

   default double leashElasticDistance() {
      return 6.0;
   }

   static <E extends Entity & Leashable> float angularFriction(E p_418015_) {
      if (p_418015_.onGround()) {
         BlockPos groundPos = p_418015_.getBlockPosBelowThatAffectsMyMovement();
         return p_418015_.level().getBlockState(groundPos).getFriction(p_418015_.level(), groundPos, p_418015_) * 0.91F;
      } else {
         return p_418015_.isInLiquid() ? 0.8F : 0.91F;
      }
   }

   default void whenLeashedTo(Entity p_418473_) {
      p_418473_.notifyLeashHolder(this);
   }

   default void leashTooFarBehaviour() {
      this.dropLeash();
   }

   default void closeRangeLeashBehaviour(Entity p_352073_) {
   }

   default boolean checkElasticInteractions(Entity p_418481_, Leashable.LeashData p_418158_) {
      boolean flag = p_418481_.supportQuadLeashAsHolder() && this.supportQuadLeash();
      List<Leashable.Wrench> list = computeElasticInteraction(
         (Entity & Leashable)this,
         p_418481_,
         flag ? SHARED_QUAD_ATTACHMENT_POINTS : ENTITY_ATTACHMENT_POINT,
         flag ? SHARED_QUAD_ATTACHMENT_POINTS : LEASHER_ATTACHMENT_POINT
      );
      if (list.isEmpty()) {
         return false;
      } else {
         Leashable.Wrench leashable$wrench = Leashable.Wrench.accumulate(list).scale(flag ? 0.25 : 1.0);
         p_418158_.angularMomentum = p_418158_.angularMomentum + 10.0 * leashable$wrench.torque();
         Vec3 vec3 = getHolderMovement(p_418481_).subtract(((Entity)this).getKnownMovement());
         ((Entity)this).addDeltaMovement(leashable$wrench.force().multiply(AXIS_SPECIFIC_ELASTICITY).add(vec3.scale(0.11)));
         return true;
      }
   }

   private static Vec3 getHolderMovement(Entity p_426117_) {
      return p_426117_ instanceof Mob mob && mob.isNoAi() ? Vec3.ZERO : p_426117_.getKnownMovement();
   }

   private static <E extends Entity & Leashable> List<Leashable.Wrench> computeElasticInteraction(
      E p_418297_, Entity p_418456_, List<Vec3> p_418351_, List<Vec3> p_418397_
   ) {
      double d0 = p_418297_.leashElasticDistance();
      Vec3 vec3 = getHolderMovement(p_418297_);
      float f = p_418297_.getYRot() * (float) (Math.PI / 180.0);
      Vec3 vec31 = new Vec3(p_418297_.getBbWidth(), p_418297_.getBbHeight(), p_418297_.getBbWidth());
      float f1 = p_418456_.getYRot() * (float) (Math.PI / 180.0);
      Vec3 vec32 = new Vec3(p_418456_.getBbWidth(), p_418456_.getBbHeight(), p_418456_.getBbWidth());
      List<Leashable.Wrench> list = new ArrayList<>();

      for (int i = 0; i < p_418351_.size(); i++) {
         Vec3 vec33 = p_418351_.get(i).multiply(vec31).yRot(-f);
         Vec3 vec34 = p_418297_.position().add(vec33);
         Vec3 vec35 = p_418397_.get(i).multiply(vec32).yRot(-f1);
         Vec3 vec36 = p_418456_.position().add(vec35);
         computeDampenedSpringInteraction(vec36, vec34, d0, vec3, vec33).ifPresent(list::add);
      }

      return list;
   }

   private static Optional<Leashable.Wrench> computeDampenedSpringInteraction(Vec3 p_418217_, Vec3 p_418024_, double p_418081_, Vec3 p_418190_, Vec3 p_418069_) {
      double d0 = p_418024_.distanceTo(p_418217_);
      if (d0 < p_418081_) {
         return Optional.empty();
      } else {
         Vec3 vec3 = p_418217_.subtract(p_418024_).normalize().scale(d0 - p_418081_);
         double d1 = Leashable.Wrench.torqueFromForce(p_418069_, vec3);
         boolean flag = p_418190_.dot(vec3) >= 0.0;
         if (flag) {
            vec3 = vec3.scale(0.3F);
         }

         return Optional.of(new Leashable.Wrench(vec3, d1));
      }
   }

   default boolean supportQuadLeash() {
      return false;
   }

   default Vec3[] getQuadLeashOffsets() {
      return createQuadLeashOffsets((Entity)this, 0.0, 0.5, 0.5, 0.5);
   }

   static Vec3[] createQuadLeashOffsets(Entity p_418142_, double p_418244_, double p_418078_, double p_418298_, double p_418121_) {
      float f = p_418142_.getBbWidth();
      double d0 = p_418244_ * f;
      double d1 = p_418078_ * f;
      double d2 = p_418298_ * f;
      double d3 = p_418121_ * p_418142_.getBbHeight();
      return new Vec3[]{new Vec3(-d2, d3, d1 + d0), new Vec3(-d2, d3, -d1 + d0), new Vec3(d2, d3, -d1 + d0), new Vec3(d2, d3, d1 + d0)};
   }

   default Vec3 getLeashOffset(float p_418480_) {
      return this.getLeashOffset();
   }

   default Vec3 getLeashOffset() {
      Entity entity = (Entity)this;
      return new Vec3(0.0, entity.getEyeHeight(), entity.getBbWidth() * 0.4F);
   }

   default void setLeashedTo(Entity p_352411_, boolean p_352183_) {
      if (this != p_352411_) {
         setLeashedTo((Entity & Leashable)this, p_352411_, p_352183_);
      }
   }

   private static <E extends Entity & Leashable> void setLeashedTo(E p_352280_, Entity p_352109_, boolean p_352239_) {
      Leashable.LeashData leashable$leashdata = p_352280_.getLeashData();
      if (leashable$leashdata == null) {
         leashable$leashdata = new Leashable.LeashData(p_352109_);
         p_352280_.setLeashData(leashable$leashdata);
      } else {
         Entity entity = leashable$leashdata.leashHolder;
         leashable$leashdata.setLeashHolder(p_352109_);
         if (entity != null && entity != p_352109_) {
            entity.notifyLeasheeRemoved(p_352280_);
         }
      }

      if (p_352239_ && p_352280_.level() instanceof ServerLevel serverlevel) {
         serverlevel.getChunkSource().sendToTrackingPlayers(p_352280_, new ClientboundSetEntityLinkPacket(p_352280_, p_352109_));
      }

      if (p_352280_.isPassenger()) {
         p_352280_.stopRiding();
      }
   }

   @Nullable
   default Entity getLeashHolder() {
      return getLeashHolder((Entity & Leashable)this);
   }

   @Nullable
   private static <E extends Entity & Leashable> Entity getLeashHolder(E p_352466_) {
      Leashable.LeashData leashable$leashdata = p_352466_.getLeashData();
      if (leashable$leashdata == null) {
         return null;
      } else {
         if (leashable$leashdata.delayedLeashHolderId != 0 && p_352466_.level().isClientSide()) {
            Entity entity = p_352466_.level().getEntity(leashable$leashdata.delayedLeashHolderId);
            if (entity instanceof Entity) {
               leashable$leashdata.setLeashHolder(entity);
            }
         }

         return leashable$leashdata.leashHolder;
      }
   }

   static List<Leashable> leashableLeashedTo(Entity p_418021_) {
      return leashableInArea(p_418021_, p_418528_ -> p_418528_.getLeashHolder() == p_418021_);
   }

   static List<Leashable> leashableInArea(Entity p_418133_, Predicate<Leashable> p_418334_) {
      return leashableInArea(p_418133_.level(), p_418133_.getBoundingBox().getCenter(), p_418334_);
   }

   static List<Leashable> leashableInArea(Level p_418478_, Vec3 p_418494_, Predicate<Leashable> p_418347_) {
      double d0 = 32.0;
      AABB aabb = AABB.ofSize(p_418494_, 32.0, 32.0, 32.0);
      return p_418478_.getEntitiesOfClass(Entity.class, aabb, p_418131_ -> p_418131_ instanceof Leashable leashable && p_418347_.test(leashable))
         .stream()
         .map(Leashable.class::cast)
         .toList();
   }

   public static final class LeashData {
      public static final Codec<Leashable.LeashData> CODEC = Codec.xor(UUIDUtil.CODEC.fieldOf("UUID").codec(), BlockPos.CODEC)
         .xmap(
            Leashable.LeashData::new,
            p_412912_ -> {
               if (p_412912_.leashHolder instanceof LeashFenceKnotEntity leashfenceknotentity) {
                  return Either.right(leashfenceknotentity.getPos());
               } else {
                  return p_412912_.leashHolder != null
                     ? Either.left(p_412912_.leashHolder.getUUID())
                     : Objects.requireNonNull(p_412912_.delayedLeashInfo, "Invalid LeashData had no attachment");
               }
            }
         );
      int delayedLeashHolderId;
      @Nullable
      public Entity leashHolder;
      @Nullable
      public Either<UUID, BlockPos> delayedLeashInfo;
      public double angularMomentum;

      private LeashData(Either<UUID, BlockPos> p_352282_) {
         this.delayedLeashInfo = p_352282_;
      }

      LeashData(Entity p_352066_) {
         this.leashHolder = p_352066_;
      }

      LeashData(int p_352297_) {
         this.delayedLeashHolderId = p_352297_;
      }

      public void setLeashHolder(Entity p_352464_) {
         this.leashHolder = p_352464_;
         this.delayedLeashInfo = null;
         this.delayedLeashHolderId = 0;
      }
   }

   public record Wrench(Vec3 force, double torque) {
      static Leashable.Wrench ZERO = new Leashable.Wrench(Vec3.ZERO, 0.0);

      static double torqueFromForce(Vec3 p_418322_, Vec3 p_418094_) {
         return p_418322_.z * p_418094_.x - p_418322_.x * p_418094_.z;
      }

      static Leashable.Wrench accumulate(List<Leashable.Wrench> p_418210_) {
         if (p_418210_.isEmpty()) {
            return ZERO;
         } else {
            double d0 = 0.0;
            double d1 = 0.0;
            double d2 = 0.0;
            double d3 = 0.0;

            for (Leashable.Wrench leashable$wrench : p_418210_) {
               Vec3 vec3 = leashable$wrench.force;
               d0 += vec3.x;
               d1 += vec3.y;
               d2 += vec3.z;
               d3 += leashable$wrench.torque;
            }

            return new Leashable.Wrench(new Vec3(d0, d1, d2), d3);
         }
      }

      public Leashable.Wrench scale(double p_418466_) {
         return new Leashable.Wrench(this.force.scale(p_418466_), this.torque * p_418466_);
      }
   }
}
