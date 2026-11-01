package net.minecraft.client.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class KeyframeAnimation {
   private final AnimationDefinition definition;
   private final List<KeyframeAnimation.Entry> entries;

   private KeyframeAnimation(AnimationDefinition p_427337_, List<KeyframeAnimation.Entry> p_427495_) {
      this.definition = p_427337_;
      this.entries = p_427495_;
   }

   static KeyframeAnimation bake(ModelPart p_427435_, AnimationDefinition p_427268_) {
      List<KeyframeAnimation.Entry> list = new ArrayList<>();
      Function<String, ModelPart> function = p_427435_.createPartLookup();

      for (Map.Entry<String, List<AnimationChannel>> entry : p_427268_.boneAnimations().entrySet()) {
         String s = entry.getKey();
         List<AnimationChannel> list1 = entry.getValue();
         ModelPart modelpart = function.apply(s);
         if (modelpart == null) {
            throw new IllegalArgumentException("Cannot animate " + s + ", which does not exist in model");
         }

         for (AnimationChannel animationchannel : list1) {
            list.add(new KeyframeAnimation.Entry(modelpart, animationchannel.target(), animationchannel.keyframes()));
         }
      }

      return new KeyframeAnimation(p_427268_, List.copyOf(list));
   }

   public void applyStatic() {
      this.apply(0L, 1.0F);
   }

   public void applyWalk(float p_427283_, float p_427496_, float p_427289_, float p_427296_) {
      long i = (long)(p_427283_ * 50.0F * p_427289_);
      float f = Math.min(p_427496_ * p_427296_, 1.0F);
      this.apply(i, f);
   }

   public void apply(AnimationState p_427382_, float p_427471_) {
      this.apply(p_427382_, p_427471_, 1.0F);
   }

   public void apply(AnimationState p_427490_, float p_427480_, float p_427356_) {
      p_427490_.ifStarted(p_427385_ -> this.apply((long)((float)p_427385_.getTimeInMillis(p_427480_) * p_427356_), 1.0F));
   }

   public void apply(long p_427232_, float p_427346_) {
      float f = this.getElapsedSeconds(p_427232_);
      Vector3f vector3f = new Vector3f();

      for (KeyframeAnimation.Entry keyframeanimation$entry : this.entries) {
         keyframeanimation$entry.apply(f, p_427346_, vector3f);
      }
   }

   private float getElapsedSeconds(long p_427362_) {
      float f = (float)p_427362_ / 1000.0F;
      return this.definition.looping() ? f % this.definition.lengthInSeconds() : f;
   }

   @OnlyIn(Dist.CLIENT)
   record Entry(ModelPart part, AnimationChannel.Target target, Keyframe[] keyframes) {
      public void apply(float p_427355_, float p_427277_, Vector3f p_427458_) {
         int i = Math.max(0, Mth.binarySearch(0, this.keyframes.length, p_427328_ -> p_427355_ <= this.keyframes[p_427328_].timestamp()) - 1);
         int j = Math.min(this.keyframes.length - 1, i + 1);
         Keyframe keyframe = this.keyframes[i];
         Keyframe keyframe1 = this.keyframes[j];
         float f = p_427355_ - keyframe.timestamp();
         float f1;
         if (j != i) {
            f1 = Mth.clamp(f / (keyframe1.timestamp() - keyframe.timestamp()), 0.0F, 1.0F);
         } else {
            f1 = 0.0F;
         }

         keyframe1.interpolation().apply(p_427458_, f1, this.keyframes, i, j, p_427277_);
         this.target.apply(this.part, p_427458_);
      }
   }
}
