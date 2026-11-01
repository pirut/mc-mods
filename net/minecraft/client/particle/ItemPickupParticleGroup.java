package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

@Environment(EnvType.CLIENT)
public class ItemPickupParticleGroup extends ParticleGroup<ItemPickupParticle> {
   public ItemPickupParticleGroup(ParticleEngine particleEngine) {
      super(particleEngine);
   }

   @Override
   public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTick) {
      return new ItemPickupParticleGroup.State(
         this.particles
            .stream()
            .map(itemPickupParticle -> ItemPickupParticleGroup.ParticleInstance.fromParticle(itemPickupParticle, camera, partialTick))
            .toList()
      );
   }

   @Environment(EnvType.CLIENT)
   record ParticleInstance(EntityRenderState itemRenderState, double xOffset, double yOffset, double zOffset) {

      public static ItemPickupParticleGroup.ParticleInstance fromParticle(ItemPickupParticle particle, Camera camera, float partialTick) {
         float f = (particle.life + partialTick) / 3.0F;
         f *= f;
         double d = Mth.lerp((double)partialTick, particle.targetXOld, particle.targetX);
         double e = Mth.lerp((double)partialTick, particle.targetYOld, particle.targetY);
         double g = Mth.lerp((double)partialTick, particle.targetZOld, particle.targetZ);
         double h = Mth.lerp((double)f, particle.itemRenderState.x, d);
         double i = Mth.lerp((double)f, particle.itemRenderState.y, e);
         double j = Mth.lerp((double)f, particle.itemRenderState.z, g);
         Vec3 vec3 = camera.position();
         return new ItemPickupParticleGroup.ParticleInstance(particle.itemRenderState, h - vec3.x(), i - vec3.y(), j - vec3.z());
      }
   }

   @Environment(EnvType.CLIENT)
   record State(List<ItemPickupParticleGroup.ParticleInstance> instances) implements ParticleGroupRenderState {
      @Override
      public void submit(SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
         PoseStack poseStack = new PoseStack();
         EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

         for (ItemPickupParticleGroup.ParticleInstance particleInstance : this.instances) {
            entityRenderDispatcher.submit(
               particleInstance.itemRenderState,
               cameraRenderState,
               particleInstance.xOffset,
               particleInstance.yOffset,
               particleInstance.zOffset,
               poseStack,
               submitNodeCollector
            );
         }
      }
   }
}
