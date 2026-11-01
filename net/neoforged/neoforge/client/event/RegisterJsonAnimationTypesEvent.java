package net.neoforged.neoforge.client.event;

import com.google.common.collect.ImmutableMap.Builder;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.entity.animation.AnimationTarget;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterJsonAnimationTypesEvent extends Event implements IModBusEvent {
   private final Builder<Identifier, AnimationTarget> targets;
   private final Builder<Identifier, AnimationChannel.Interpolation> interpolations;
   private final Map<Identifier, AnimationTarget> registeredTargets = new HashMap<>();
   private final Map<Identifier, AnimationChannel.Interpolation> registeredInterpolations = new HashMap<>();

   @Internal
   public RegisterJsonAnimationTypesEvent(Builder<Identifier, AnimationTarget> targets, Builder<Identifier, AnimationChannel.Interpolation> interpolations) {
      this.targets = targets;
      this.interpolations = interpolations;
   }

   public void registerTarget(Identifier key, AnimationTarget target) {
      checkDuplicate("target", key, this.registeredTargets, target);
      this.targets.put(key, target);
   }

   public void registerInterpolation(Identifier key, AnimationChannel.Interpolation interpolation) {
      checkDuplicate("interpolation", key, this.registeredInterpolations, interpolation);
      this.interpolations.put(key, interpolation);
   }

   private static <T> void checkDuplicate(String what, Identifier key, Map<Identifier, T> by, T obj) {
      T prevObj = by.putIfAbsent(key, obj);
      if (prevObj != null) {
         throw new IllegalStateException("Duplicate " + what + " registration for " + key + ". " + obj + " tried to overwrite " + prevObj + ".");
      }
   }
}
