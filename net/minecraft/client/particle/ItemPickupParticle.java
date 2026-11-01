package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemPickupParticle extends Particle {
   protected static final int LIFE_TIME = 3;
   private final Entity target;
   protected int life;
   protected final EntityRenderState itemRenderState;
   protected double targetX;
   protected double targetY;
   protected double targetZ;
   protected double targetXOld;
   protected double targetYOld;
   protected double targetZOld;

   public ItemPickupParticle(ClientLevel p_107025_, EntityRenderState p_446014_, Entity p_107026_, Vec3 p_382893_) {
      super(p_107025_, p_446014_.x, p_446014_.y, p_446014_.z, p_382893_.x, p_382893_.y, p_382893_.z);
      this.target = p_107026_;
      this.itemRenderState = p_446014_;
      this.itemRenderState.outlineColor = 0;
      this.updatePosition();
      this.saveOldPosition();
   }

   @Override
   public void tick() {
      this.life++;
      if (this.life == 3) {
         this.remove();
      }

      this.saveOldPosition();
      this.updatePosition();
   }

   @Override
   public ParticleRenderType getGroup() {
      return ParticleRenderType.ITEM_PICKUP;
   }

   private void updatePosition() {
      this.targetX = this.target.getX();
      this.targetY = (this.target.getY() + this.target.getEyeY()) / 2.0;
      this.targetZ = this.target.getZ();
   }

   private void saveOldPosition() {
      this.targetXOld = this.targetX;
      this.targetYOld = this.targetY;
      this.targetZOld = this.targetZ;
   }
}
