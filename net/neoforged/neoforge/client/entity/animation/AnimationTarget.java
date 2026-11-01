package net.neoforged.neoforge.client.entity.animation;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.KeyframeAnimations;
import org.joml.Vector3f;

public record AnimationTarget(AnimationChannel.Target channelTarget, AnimationKeyframeTarget keyframeTarget, AnimationKeyframeTarget inverseKeyframeTarget) {
   public static final AnimationTarget POSITION = new AnimationTarget(AnimationChannel.Targets.POSITION, KeyframeAnimations::posVec, KeyframeAnimations::posVec);
   public static final AnimationTarget ROTATION = new AnimationTarget(
      AnimationChannel.Targets.ROTATION, KeyframeAnimations::degreeVec, AnimationTarget::inverseDegreeVec
   );
   public static final AnimationTarget SCALE = new AnimationTarget(
      AnimationChannel.Targets.SCALE, KeyframeAnimations::scaleVec, AnimationTarget::inverseScaleVec
   );

   private static Vector3f inverseDegreeVec(float x, float y, float z) {
      return new Vector3f(x / (float) (Math.PI / 180.0), y / (float) (Math.PI / 180.0), z / (float) (Math.PI / 180.0));
   }

   private static Vector3f inverseScaleVec(double x, double y, double z) {
      return new Vector3f((float)(x + 1.0), (float)(y + 1.0), (float)(z + 1.0));
   }

   @Override
   public boolean equals(Object obj) {
      return this == obj;
   }

   @Override
   public int hashCode() {
      return System.identityHashCode(this);
   }
}
