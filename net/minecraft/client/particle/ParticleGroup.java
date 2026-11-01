package net.minecraft.client.particle;

import com.google.common.collect.EvictingQueue;
import java.util.Iterator;
import java.util.Queue;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ParticleGroup<P extends Particle> {
   private static final int MAX_PARTICLES = 16384;
   protected final ParticleEngine engine;
   protected final Queue<P> particles = EvictingQueue.create(16384);

   public ParticleGroup(ParticleEngine p_445697_) {
      this.engine = p_445697_;
   }

   public boolean isEmpty() {
      return this.particles.isEmpty();
   }

   public void tickParticles() {
      if (!this.particles.isEmpty()) {
         Iterator<P> iterator = this.particles.iterator();

         while (iterator.hasNext()) {
            P p = iterator.next();
            this.tickParticle(p);
            if (!p.isAlive()) {
               p.getParticleLimit().ifPresent(p_446237_ -> this.engine.updateCount(p_446237_, -1));
               iterator.remove();
            }
         }
      }
   }

   private void tickParticle(Particle p_446556_) {
      try {
         p_446556_.tick();
      } catch (Throwable var5) {
         CrashReport crashreport = CrashReport.forThrowable(var5, "Ticking Particle");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Particle being ticked");
         crashreportcategory.setDetail("Particle", p_446556_::toString);
         crashreportcategory.setDetail("Particle Type", p_446556_.getGroup()::toString);
         throw new ReportedException(crashreport);
      }
   }

   public void add(Particle p_447148_) {
      this.particles.add((P)p_447148_);
   }

   public int size() {
      return this.particles.size();
   }

   public abstract ParticleGroupRenderState extractRenderState(Frustum var1, Camera var2, float var3);

   public Queue<P> getAll() {
      return this.particles;
   }
}
