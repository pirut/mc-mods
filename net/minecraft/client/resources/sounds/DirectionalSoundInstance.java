package net.minecraft.client.resources.sounds;

import net.minecraft.client.Camera;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DirectionalSoundInstance extends AbstractTickableSoundInstance {
   private final Camera camera;
   private final float xAngle;
   private final float yAngle;

   public DirectionalSoundInstance(SoundEvent p_437331_, SoundSource p_437342_, RandomSource p_437353_, Camera p_437415_, float p_437269_, float p_437261_) {
      super(p_437331_, p_437342_, p_437353_);
      this.camera = p_437415_;
      this.xAngle = p_437269_;
      this.yAngle = p_437261_;
      this.setPosition();
   }

   private void setPosition() {
      Vec3 vec3 = Vec3.directionFromRotation(this.xAngle, this.yAngle).scale(10.0);
      this.x = this.camera.position().x + vec3.x;
      this.y = this.camera.position().y + vec3.y;
      this.z = this.camera.position().z + vec3.z;
      this.attenuation = SoundInstance.Attenuation.NONE;
   }

   @Override
   public void tick() {
      this.setPosition();
   }
}
