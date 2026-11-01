package net.minecraft.client.multiplayer;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientExplosionTracker {
   private static final int MAX_PARTICLES_PER_TICK = 512;
   private final List<ClientExplosionTracker.ExplosionInfo> explosions = new ArrayList<>();

   public void track(Vec3 p_437250_, float p_437295_, int p_437260_, WeightedList<ExplosionParticleInfo> p_437251_) {
      if (!p_437251_.isEmpty()) {
         this.explosions.add(new ClientExplosionTracker.ExplosionInfo(p_437250_, p_437295_, p_437260_, p_437251_));
      }
   }

   public void tick(ClientLevel p_437249_) {
      if (Minecraft.getInstance().options.particles().get() != ParticleStatus.ALL) {
         this.explosions.clear();
      } else {
         int i = WeightedRandom.getTotalWeight(this.explosions, ClientExplosionTracker.ExplosionInfo::blockCount);
         int j = Math.min(i, 512);

         for (int k = 0; k < j; k++) {
            WeightedRandom.getRandomItem(p_437249_.getRandom(), this.explosions, i, ClientExplosionTracker.ExplosionInfo::blockCount)
               .ifPresent(p_437218_ -> this.addParticle(p_437249_, p_437218_));
         }

         this.explosions.clear();
      }
   }

   private void addParticle(ClientLevel p_437346_, ClientExplosionTracker.ExplosionInfo p_437327_) {
      RandomSource randomsource = p_437346_.getRandom();
      Vec3 vec3 = p_437327_.center();
      Vec3 vec31 = new Vec3(randomsource.nextFloat() * 2.0F - 1.0F, randomsource.nextFloat() * 2.0F - 1.0F, randomsource.nextFloat() * 2.0F - 1.0F).normalize();
      float f = (float)Math.cbrt(randomsource.nextFloat()) * p_437327_.radius();
      Vec3 vec32 = vec31.scale(f);
      Vec3 vec33 = vec3.add(vec32);
      if (p_437346_.getBlockState(BlockPos.containing(vec33)).isAir()) {
         float f1 = 0.5F / (f / p_437327_.radius() + 0.1F) * randomsource.nextFloat() * randomsource.nextFloat() + 0.3F;
         ExplosionParticleInfo explosionparticleinfo = p_437327_.blockParticles.getRandomOrThrow(randomsource);
         Vec3 vec34 = vec3.add(vec32.scale(explosionparticleinfo.scaling()));
         Vec3 vec35 = vec31.scale(f1 * explosionparticleinfo.speed());
         p_437346_.addParticle(explosionparticleinfo.particle(), vec34.x(), vec34.y(), vec34.z(), vec35.x(), vec35.y(), vec35.z());
      }
   }

   @OnlyIn(Dist.CLIENT)
   record ExplosionInfo(Vec3 center, float radius, int blockCount, WeightedList<ExplosionParticleInfo> blockParticles) {
   }
}
