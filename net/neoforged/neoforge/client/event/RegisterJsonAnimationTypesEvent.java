package net.neoforged.neoforge.client.event;

import com.google.common.collect.ImmutableMap.Builder;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.entity.animation.AnimationTarget;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterJsonAnimationTypesEvent extends Event implements IModBusEvent {
   private final Builder<ResourceLocation, AnimationTarget> targets;
   private final Builder<ResourceLocation, AnimationChannel.Interpolation> interpolations;
   private final Map<ResourceLocation, AnimationTarget> registeredTargets = new HashMap<>();
   private final Map<ResourceLocation, AnimationChannel.Interpolation> registeredInterpolations = new HashMap<>();

   @Internal
   public RegisterJsonAnimationTypesEvent(
      Builder<ResourceLocation, AnimationTarget> targets, Builder<ResourceLocation, AnimationChannel.Interpolation> interpolations
   ) {
      this.targets = targets;
      this.interpolations = interpolations;
   }

   public void registerTarget(ResourceLocation key, AnimationTarget target) {
      checkDuplicate("target", key, this.registeredTargets, target);
      this.targets.put(key, target);
   }

   public void registerInterpolation(ResourceLocation key, AnimationChannel.Interpolation interpolation) {
      checkDuplicate("interpolation", key, this.registeredInterpolations, interpolation);
      this.interpolations.put(key, interpolation);
   }

   private static <T> void checkDuplicate(String what, ResourceLocation key, Map<ResourceLocation, T> by, T obj) {
      T prevObj = by.putIfAbsent(key, obj);
      if (prevObj != null) {
         throw new IllegalStateException("Duplicate " + what + " registration for " + key + ". " + obj + " tried to overwrite " + prevObj + ".");
      }
   }
}
