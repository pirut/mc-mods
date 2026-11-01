package net.minecraft.client.renderer.state;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeStorage;

@Environment(EnvType.CLIENT)
public class ParticlesRenderState {
   public final List<ParticleGroupRenderState> particles = new ArrayList<>();

   public void reset() {
      this.particles.forEach(ParticleGroupRenderState::clear);
      this.particles.clear();
   }

   public void add(ParticleGroupRenderState renderState) {
      this.particles.add(renderState);
   }

   public void submit(SubmitNodeStorage nodeStorage, CameraRenderState cameraRenderState) {
      for (ParticleGroupRenderState particleGroupRenderState : this.particles) {
         particleGroupRenderState.submit(nodeStorage, cameraRenderState);
      }
   }
}
