package net.minecraft.client.animation;

import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class KeyframeAnimations {
   public static void animate(HierarchicalModel<?> p_232320_, AnimationDefinition p_232321_, long p_232322_, float p_232323_, Vector3f p_253861_) {
      float $$5 = getElapsedSeconds(p_232321_, p_232322_);

      for (Entry<String, List<AnimationChannel>> $$6 : p_232321_.boneAnimations().entrySet()) {
         Optional<ModelPart> $$7 = p_232320_.getAnyDescendantWithName($$6.getKey());
         List<AnimationChannel> $$8 = $$6.getValue();
         $$7.ifPresent(p_232330_ -> $$8.forEach(p_288241_ -> {
            Keyframe[] $$5x = p_288241_.keyframes();
            int $$6x = Math.max(0, Mth.binarySearch(0, $$5x.length, p_232315_ -> $$5 <= $$5x[p_232315_].timestamp()) - 1);
            int $$7x = Math.min($$5x.length - 1, $$6x + 1);
            Keyframe $$8x = $$5x[$$6x];
            Keyframe $$9 = $$5x[$$7x];
            float $$10 = $$5 - $$8x.timestamp();
            float $$11;
            if ($$7x != $$6x) {
               $$11 = Mth.clamp($$10 / ($$9.timestamp() - $$8x.timestamp()), 0.0F, 1.0F);
            } else {
               $$11 = 0.0F;
            }

            $$9.interpolation().apply(p_253861_, $$11, $$5x, $$6x, $$7x, p_232323_);
            p_288241_.target().apply(p_232330_, p_253861_);
         }));
      }
   }

   private static float getElapsedSeconds(AnimationDefinition p_232317_, long p_232318_) {
      float $$2 = (float)p_232318_ / 1000.0F;
      return p_232317_.looping() ? $$2 % p_232317_.lengthInSeconds() : $$2;
   }

   public static Vector3f posVec(float p_253691_, float p_254046_, float p_254461_) {
      return new Vector3f(p_253691_, -p_254046_, p_254461_);
   }

   public static Vector3f degreeVec(float p_254402_, float p_253917_, float p_254397_) {
      return new Vector3f(p_254402_ * (float) (Math.PI / 180.0), p_253917_ * (float) (Math.PI / 180.0), p_254397_ * (float) (Math.PI / 180.0));
   }

   public static Vector3f scaleVec(double p_253806_, double p_253647_, double p_254396_) {
      return new Vector3f((float)(p_253806_ - 1.0), (float)(p_253647_ - 1.0), (float)(p_254396_ - 1.0));
   }
}
