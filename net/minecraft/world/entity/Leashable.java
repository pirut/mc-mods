package net.minecraft.world.entity;

import com.mojang.datafixers.util.Either;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.item.Items;

public interface Leashable {
   String LEASH_TAG = "leash";
   double LEASH_TOO_FAR_DIST = 10.0;
   double LEASH_ELASTIC_DIST = 6.0;

   @Nullable
   Leashable.LeashData getLeashData();

   void setLeashData(@Nullable Leashable.LeashData var1);

   default boolean isLeashed() {
      return this.getLeashData() != null && this.getLeashData().leashHolder != null;
   }

   default boolean mayBeLeashed() {
      return this.getLeashData() != null;
   }

   default boolean canHaveALeashAttachedToIt() {
      return this.canBeLeashed() && !this.isLeashed();
   }

   default boolean canBeLeashed() {
      return true;
   }

   default void setDelayedLeashHolderId(int p_352387_) {
      this.setLeashData(new Leashable.LeashData(p_352387_));
      dropLeash((Entity & Leashable)this, false, false);
   }

   @Nullable
   default Leashable.LeashData readLeashData(CompoundTag p_352410_) {
      if (p_352410_.contains("leash", 10)) {
         return new Leashable.LeashData(Either.left(p_352410_.getCompound("leash").getUUID("UUID")));
      } else {
         if (p_352410_.contains("leash", 11)) {
            Either<UUID, BlockPos> $$1 = NbtUtils.readBlockPos(p_352410_, "leash").<Either<UUID, BlockPos>>map(Either::right).orElse(null);
            if ($$1 != null) {
               return new Leashable.LeashData($$1);
            }
         }

         return null;
      }
   }

   default void writeLeashData(CompoundTag p_352349_, @Nullable Leashable.LeashData p_352363_) {
      if (p_352363_ != null) {
         Either<UUID, BlockPos> $$2 = p_352363_.delayedLeashInfo;
         if (p_352363_.leashHolder instanceof LeashFenceKnotEntity $$3) {
            $$2 = Either.right($$3.getPos());
         } else if (p_352363_.leashHolder != null) {
            $$2 = Either.left(p_352363_.leashHolder.getUUID());
         }

         if ($$2 != null) {
            p_352349_.put("leash", (Tag)$$2.map(p_352326_ -> {
               CompoundTag $$1 = new CompoundTag();
               $$1.putUUID("UUID", p_352326_);
               return $$1;
            }, NbtUtils::writeBlockPos));
         }
      }
   }

   private static <E extends Entity & Leashable> void restoreLeashFromSave(E p_352354_, Leashable.LeashData p_352106_) {
      if (p_352106_.delayedLeashInfo != null && p_352354_.level() instanceof ServerLevel $$2) {
         Optional<UUID> $$3 = p_352106_.delayedLeashInfo.left();
         Optional<BlockPos> $$4 = p_352106_.delayedLeashInfo.right();
         if ($$3.isPresent()) {
            Entity $$5 = $$2.getEntity($$3.get());
            if ($$5 != null) {
               setLeashedTo(p_352354_, $$5, true);
               return;
            }
         } else if ($$4.isPresent()) {
            setLeashedTo(p_352354_, LeashFenceKnotEntity.getOrCreateKnot($$2, $$4.get()), true);
            return;
         }

         if (p_352354_.tickCount > 100) {
            p_352354_.spawnAtLocation(Items.LEAD);
            p_352354_.setLeashData(null);
         }
      }
   }

   default void dropLeash(boolean p_352294_, boolean p_352456_) {
      dropLeash((Entity & Leashable)this, p_352294_, p_352456_);
   }

   private static <E extends Entity & Leashable> void dropLeash(E p_352163_, boolean p_352286_, boolean p_352272_) {
      Leashable.LeashData $$3 = p_352163_.getLeashData();
      if ($$3 != null && $$3.leashHolder != null) {
         p_352163_.setLeashData(null);
         if (!p_352163_.level().isClientSide && p_352272_) {
            p_352163_.spawnAtLocation(Items.LEAD);
         }

         if (p_352286_ && p_352163_.level() instanceof ServerLevel $$4) {
            $$4.getChunkSource().broadcast(p_352163_, new ClientboundSetEntityLinkPacket(p_352163_, null));
         }
      }
   }

   static <E extends Entity & Leashable> void tickLeash(E p_352082_) {
      Leashable.LeashData $$1 = p_352082_.getLeashData();
      if ($$1 != null && $$1.delayedLeashInfo != null) {
         restoreLeashFromSave(p_352082_, $$1);
      }

      if ($$1 != null && $$1.leashHolder != null) {
         if (!p_352082_.isAlive() || !$$1.leashHolder.isAlive()) {
            dropLeash(p_352082_, true, true);
         }

         Entity $$2 = p_352082_.getLeashHolder();
         if ($$2 != null && $$2.level() == p_352082_.level()) {
            float $$3 = p_352082_.distanceTo($$2);
            if (!p_352082_.handleLeashAtDistance($$2, $$3)) {
               return;
            }

            if ($$3 > 10.0) {
               p_352082_.leashTooFarBehaviour();
            } else if ($$3 > 6.0) {
               p_352082_.elasticRangeLeashBehaviour($$2, $$3);
               p_352082_.checkSlowFallDistance();
            } else {
               p_352082_.closeRangeLeashBehaviour($$2);
            }
         }
      }
   }

   default boolean handleLeashAtDistance(Entity p_352458_, float p_352101_) {
      return true;
   }

   default void leashTooFarBehaviour() {
      this.dropLeash(true, true);
   }

   default void closeRangeLeashBehaviour(Entity p_352073_) {
   }

   default void elasticRangeLeashBehaviour(Entity p_353036_, float p_353047_) {
      legacyElasticRangeLeashBehaviour((Entity & Leashable)this, p_353036_, p_353047_);
   }

   private static <E extends Entity & Leashable> void legacyElasticRangeLeashBehaviour(E p_353048_, Entity p_353039_, float p_353053_) {
      double $$3 = (p_353039_.getX() - p_353048_.getX()) / p_353053_;
      double $$4 = (p_353039_.getY() - p_353048_.getY()) / p_353053_;
      double $$5 = (p_353039_.getZ() - p_353048_.getZ()) / p_353053_;
      p_353048_.setDeltaMovement(
         p_353048_.getDeltaMovement().add(Math.copySign($$3 * $$3 * 0.4, $$3), Math.copySign($$4 * $$4 * 0.4, $$4), Math.copySign($$5 * $$5 * 0.4, $$5))
      );
   }

   default void setLeashedTo(Entity p_352411_, boolean p_352183_) {
      setLeashedTo((Entity & Leashable)this, p_352411_, p_352183_);
   }

   private static <E extends Entity & Leashable> void setLeashedTo(E p_352280_, Entity p_352109_, boolean p_352239_) {
      Leashable.LeashData $$3 = p_352280_.getLeashData();
      if ($$3 == null) {
         $$3 = new Leashable.LeashData(p_352109_);
         p_352280_.setLeashData($$3);
      } else {
         $$3.setLeashHolder(p_352109_);
      }

      if (p_352239_ && p_352280_.level() instanceof ServerLevel $$4) {
         $$4.getChunkSource().broadcast(p_352280_, new ClientboundSetEntityLinkPacket(p_352280_, p_352109_));
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
      Leashable.LeashData $$1 = p_352466_.getLeashData();
      if ($$1 == null) {
         return null;
      } else {
         if ($$1.delayedLeashHolderId != 0 && p_352466_.level().isClientSide) {
            Entity var3 = p_352466_.level().getEntity($$1.delayedLeashHolderId);
            if (var3 instanceof Entity) {
               $$1.setLeashHolder(var3);
            }
         }

         return $$1.leashHolder;
      }
   }

   public static final class LeashData {
      int delayedLeashHolderId;
      @Nullable
      public Entity leashHolder;
      @Nullable
      public Either<UUID, BlockPos> delayedLeashInfo;

      LeashData(Either<UUID, BlockPos> p_352282_) {
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
}
