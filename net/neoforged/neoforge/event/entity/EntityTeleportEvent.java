package net.neoforged.neoforge.event.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public class EntityTeleportEvent extends EntityEvent implements ICancellableEvent {
   protected double targetX;
   protected double targetY;
   protected double targetZ;

   public EntityTeleportEvent(Entity entity, double targetX, double targetY, double targetZ) {
      super(entity);
      this.targetX = targetX;
      this.targetY = targetY;
      this.targetZ = targetZ;
   }

   public double getTargetX() {
      return this.targetX;
   }

   public void setTargetX(double targetX) {
      this.targetX = targetX;
   }

   public double getTargetY() {
      return this.targetY;
   }

   public void setTargetY(double targetY) {
      this.targetY = targetY;
   }

   public double getTargetZ() {
      return this.targetZ;
   }

   public void setTargetZ(double targetZ) {
      this.targetZ = targetZ;
   }

   public Vec3 getTarget() {
      return new Vec3(this.targetX, this.targetY, this.targetZ);
   }

   public double getPrevX() {
      return this.getEntity().getX();
   }

   public double getPrevY() {
      return this.getEntity().getY();
   }

   public double getPrevZ() {
      return this.getEntity().getZ();
   }

   public Vec3 getPrev() {
      return this.getEntity().position();
   }

   public static class ChorusFruit extends EntityTeleportEvent implements ICancellableEvent {
      private final LivingEntity entityLiving;

      public ChorusFruit(LivingEntity entity, double targetX, double targetY, double targetZ) {
         super(entity, targetX, targetY, targetZ);
         this.entityLiving = entity;
      }

      public LivingEntity getEntityLiving() {
         return this.entityLiving;
      }
   }

   public static class EnderEntity extends EntityTeleportEvent implements ICancellableEvent {
      private final LivingEntity entityLiving;

      public EnderEntity(LivingEntity entity, double targetX, double targetY, double targetZ) {
         super(entity, targetX, targetY, targetZ);
         this.entityLiving = entity;
      }

      public LivingEntity getEntityLiving() {
         return this.entityLiving;
      }
   }

   public static class EnderPearl extends EntityTeleportEvent implements ICancellableEvent {
      private final ServerPlayer player;
      private final ThrownEnderpearl pearlEntity;
      private float attackDamage;
      private final HitResult hitResult;

      @Internal
      public EnderPearl(
         ServerPlayer entity, double targetX, double targetY, double targetZ, ThrownEnderpearl pearlEntity, float attackDamage, HitResult hitResult
      ) {
         super(entity, targetX, targetY, targetZ);
         this.pearlEntity = pearlEntity;
         this.player = entity;
         this.attackDamage = attackDamage;
         this.hitResult = hitResult;
      }

      public ThrownEnderpearl getPearlEntity() {
         return this.pearlEntity;
      }

      public ServerPlayer getPlayer() {
         return this.player;
      }

      @Nullable
      public HitResult getHitResult() {
         return this.hitResult;
      }

      public float getAttackDamage() {
         return this.attackDamage;
      }

      public void setAttackDamage(float attackDamage) {
         this.attackDamage = attackDamage;
      }
   }

   public static class SpreadPlayersCommand extends EntityTeleportEvent implements ICancellableEvent {
      public SpreadPlayersCommand(Entity entity, double targetX, double targetY, double targetZ) {
         super(entity, targetX, targetY, targetZ);
      }
   }

   public static class TeleportCommand extends EntityTeleportEvent implements ICancellableEvent {
      public TeleportCommand(Entity entity, double targetX, double targetY, double targetZ) {
         super(entity, targetX, targetY, targetZ);
      }
   }
}
