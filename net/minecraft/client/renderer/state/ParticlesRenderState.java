package net.minecraft.client.renderer.state;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ParticlesRenderState {
   public final List<ParticleGroupRenderState> particles = new ArrayList<>();

   public void reset() {
      this.particles.forEach(ParticleGroupRenderState::clear);
      this.particles.clear();
   }

   public void add(ParticleGroupRenderState p_451061_) {
      this.particles.add(p_451061_);
   }

   public void submit(SubmitNodeStorage p_451280_, CameraRenderState p_451054_) {
      for (ParticleGroupRenderState particlegrouprenderstate : this.particles) {
         particlegrouprenderstate.submit(p_451280_, p_451054_);
      }
   }
}
