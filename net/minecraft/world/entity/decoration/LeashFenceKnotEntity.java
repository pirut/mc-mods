package net.minecraft.world.entity.decoration;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LeashFenceKnotEntity extends BlockAttachedEntity {
   public static final double OFFSET_Y = 0.375;

   public LeashFenceKnotEntity(EntityType<? extends LeashFenceKnotEntity> p_31828_, Level p_31829_) {
      super(p_31828_, p_31829_);
   }

   public LeashFenceKnotEntity(Level p_31831_, BlockPos p_31832_) {
      super(EntityType.LEASH_KNOT, p_31831_, p_31832_);
      this.setPos(p_31832_.getX(), p_31832_.getY(), p_31832_.getZ());
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_344730_) {
   }

   @Override
   protected void recalculateBoundingBox() {
      this.setPosRaw(this.pos.getX() + 0.5, this.pos.getY() + 0.375, this.pos.getZ() + 0.5);
      double $$0 = this.getType().getWidth() / 2.0;
      double $$1 = this.getType().getHeight();
      this.setBoundingBox(new AABB(this.getX() - $$0, this.getY(), this.getZ() - $$0, this.getX() + $$0, this.getY() + $$1, this.getZ() + $$0));
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double p_31835_) {
      return p_31835_ < 1024.0;
   }

   @Override
   public void dropItem(@Nullable Entity p_31837_) {
      this.playSound(SoundEvents.LEASH_KNOT_BREAK, 1.0F, 1.0F);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag p_31852_) {
   }

   @Override
   public void readAdditionalSaveData(CompoundTag p_31850_) {
   }

   @Override
   public InteractionResult interact(Player p_31842_, InteractionHand p_31843_) {
      if (this.level().isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         boolean $$2 = false;
         List<Leashable> $$3 = LeadItem.leashableInArea(this.level(), this.getPos(), p_353058_ -> {
            Entity $$2x = p_353058_.getLeashHolder();
            return $$2x == p_31842_ || $$2x == this;
         });

         for (Leashable $$4 : $$3) {
            if ($$4.getLeashHolder() == p_31842_) {
               $$4.setLeashedTo(this, true);
               $$2 = true;
            }
         }

         boolean $$5 = false;
         if (!$$2) {
            this.discard();
            if (p_31842_.getAbilities().instabuild) {
               for (Leashable $$6 : $$3) {
                  if ($$6.isLeashed() && $$6.getLeashHolder() == this) {
                     $$6.dropLeash(true, false);
                     $$5 = true;
                  }
               }
            }
         }

         if ($$2 || $$5) {
            this.gameEvent(GameEvent.BLOCK_ATTACH, p_31842_);
         }

         return InteractionResult.CONSUME;
      }
   }

   @Override
   public boolean survives() {
      return this.level().getBlockState(this.pos).is(BlockTags.FENCES);
   }

   public static LeashFenceKnotEntity getOrCreateKnot(Level p_31845_, BlockPos p_31846_) {
      int $$2 = p_31846_.getX();
      int $$3 = p_31846_.getY();
      int $$4 = p_31846_.getZ();

      for (LeashFenceKnotEntity $$6 : p_31845_.getEntitiesOfClass(
         LeashFenceKnotEntity.class, new AABB($$2 - 1.0, $$3 - 1.0, $$4 - 1.0, $$2 + 1.0, $$3 + 1.0, $$4 + 1.0)
      )) {
         if ($$6.getPos().equals(p_31846_)) {
            return $$6;
         }
      }

      LeashFenceKnotEntity $$7 = new LeashFenceKnotEntity(p_31845_, p_31846_);
      p_31845_.addFreshEntity($$7);
      return $$7;
   }

   public void playPlacementSound() {
      this.playSound(SoundEvents.LEASH_KNOT_PLACE, 1.0F, 1.0F);
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity p_352402_) {
      return new ClientboundAddEntityPacket(this, 0, this.getPos());
   }

   @Override
   public Vec3 getRopeHoldPosition(float p_31863_) {
      return this.getPosition(p_31863_).add(0.0, 0.2, 0.0);
   }

   @Override
   public ItemStack getPickResult() {
      return new ItemStack(Items.LEAD);
   }
}
