package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemPickupParticleGroup extends ParticleGroup<ItemPickupParticle> {
   public ItemPickupParticleGroup(ParticleEngine p_446584_) {
      super(p_446584_);
   }

   @Override
   public ParticleGroupRenderState extractRenderState(Frustum p_446194_, Camera p_446787_, float p_446033_) {
      return new ItemPickupParticleGroup.State(
         this.particles.stream().map(p_445748_ -> ItemPickupParticleGroup.ParticleInstance.fromParticle(p_445748_, p_446787_, p_446033_)).toList()
      );
   }

   @OnlyIn(Dist.CLIENT)
   record ParticleInstance(EntityRenderState itemRenderState, double xOffset, double yOffset, double zOffset) {
      public static ItemPickupParticleGroup.ParticleInstance fromParticle(ItemPickupParticle p_446209_, Camera p_447306_, float p_447073_) {
         float f = (p_446209_.life + p_447073_) / 3.0F;
         f *= f;
         double d0 = Mth.lerp((double)p_447073_, p_446209_.targetXOld, p_446209_.targetX);
         double d1 = Mth.lerp((double)p_447073_, p_446209_.targetYOld, p_446209_.targetY);
         double d2 = Mth.lerp((double)p_447073_, p_446209_.targetZOld, p_446209_.targetZ);
         double d3 = Mth.lerp((double)f, p_446209_.itemRenderState.x, d0);
         double d4 = Mth.lerp((double)f, p_446209_.itemRenderState.y, d1);
         double d5 = Mth.lerp((double)f, p_446209_.itemRenderState.z, d2);
         Vec3 vec3 = p_447306_.position();
         return new ItemPickupParticleGroup.ParticleInstance(p_446209_.itemRenderState, d3 - vec3.x(), d4 - vec3.y(), d5 - vec3.z());
      }
   }

   @OnlyIn(Dist.CLIENT)
   record State(List<ItemPickupParticleGroup.ParticleInstance> instances) implements ParticleGroupRenderState {
      @Override
      public void submit(SubmitNodeCollector p_446688_, CameraRenderState p_451251_) {
         PoseStack posestack = new PoseStack();
         EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

         for (ItemPickupParticleGroup.ParticleInstance itempickupparticlegroup$particleinstance : this.instances) {
            entityrenderdispatcher.submit(
               itempickupparticlegroup$particleinstance.itemRenderState,
               p_451251_,
               itempickupparticlegroup$particleinstance.xOffset,
               itempickupparticlegroup$particleinstance.yOffset,
               itempickupparticlegroup$particleinstance.zOffset,
               posestack,
               p_446688_
            );
         }
      }
   }
}
