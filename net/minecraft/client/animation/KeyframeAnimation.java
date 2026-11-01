package net.minecraft.client.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class KeyframeAnimation {
   private final AnimationDefinition definition;
   private final List<KeyframeAnimation.Entry> entries;

   private KeyframeAnimation(AnimationDefinition definition, List<KeyframeAnimation.Entry> entries) {
      this.definition = definition;
      this.entries = entries;
   }

   static KeyframeAnimation bake(ModelPart root, AnimationDefinition definition) {
      List<KeyframeAnimation.Entry> list = new ArrayList<>();
      Function<String, ModelPart> function = root.createPartLookup();

      for (Map.Entry<String, List<AnimationChannel>> entry : definition.boneAnimations().entrySet()) {
         String string = entry.getKey();
         List<AnimationChannel> list2 = entry.getValue();
         ModelPart modelPart = function.apply(string);
         if (modelPart == null) {
            throw new IllegalArgumentException("Cannot animate " + string + ", which does not exist in model");
         }

         for (AnimationChannel animationChannel : list2) {
            list.add(new KeyframeAnimation.Entry(modelPart, animationChannel.target(), animationChannel.keyframes()));
         }
      }

      return new KeyframeAnimation(definition, List.copyOf(list));
   }

   public void applyStatic() {
      this.apply(0L, 1.0F);
   }

   public void applyWalk(float walkAnimationPos, float walkAnimationSpeed, float timeMultiplier, float speedMultiplier) {
      long l = (long)(walkAnimationPos * 50.0F * timeMultiplier);
      float f = Math.min(walkAnimationSpeed * speedMultiplier, 1.0F);
      this.apply(l, f);
   }

   public void apply(AnimationState animationState, float ageInTicks) {
      this.apply(animationState, ageInTicks, 1.0F);
   }

   public void apply(AnimationState animationState, float ageInTicks, float speedMultiplier) {
      animationState.ifStarted(animationStatex -> this.apply((long)((float)animationStatex.getTimeInMillis(ageInTicks) * speedMultiplier), 1.0F));
   }

   public void apply(long timeInMillis, float scale) {
      float f = this.getElapsedSeconds(timeInMillis);
      Vector3f vector3f = new Vector3f();

      for (KeyframeAnimation.Entry entry : this.entries) {
         entry.apply(f, scale, vector3f);
      }
   }

   private float getElapsedSeconds(long timeInMillis) {
      float f = (float)timeInMillis / 1000.0F;
      return this.definition.looping() ? f % this.definition.lengthInSeconds() : f;
   }

   @Environment(EnvType.CLIENT)
   record Entry(ModelPart part, AnimationChannel.Target target, Keyframe[] keyframes) {
      public void apply(float elapsedSeconds, float scale, Vector3f scratchVector) {
         int i = Math.max(0, Mth.binarySearch(0, this.keyframes.length, ix -> elapsedSeconds <= this.keyframes[ix].timestamp()) - 1);
         int j = Math.min(this.keyframes.length - 1, i + 1);
         Keyframe keyframe = this.keyframes[i];
         Keyframe keyframe2 = this.keyframes[j];
         float f = elapsedSeconds - keyframe.timestamp();
         float g;
         if (j != i) {
            g = Mth.clamp(f / (keyframe2.timestamp() - keyframe.timestamp()), 0.0F, 1.0F);
         } else {
            g = 0.0F;
         }

         keyframe2.interpolation().apply(scratchVector, g, this.keyframes, i, j, scale);
         this.target.apply(this.part, scratchVector);
      }
   }
}
