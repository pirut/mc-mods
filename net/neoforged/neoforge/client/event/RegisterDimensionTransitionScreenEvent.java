package net.neoforged.neoforge.client.event;

import com.mojang.datafixers.util.Pair;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.DimensionTransitionScreenManager;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterDimensionTransitionScreenEvent extends Event implements IModBusEvent {
   private final Map<Pair<ResourceKey<Level>, ResourceKey<Level>>, DimensionTransitionScreenManager.ReceivingLevelScreenFactory> conditionalDimensionEffects;
   private final Map<ResourceKey<Level>, DimensionTransitionScreenManager.ReceivingLevelScreenFactory> toEffects;
   private final Map<ResourceKey<Level>, DimensionTransitionScreenManager.ReceivingLevelScreenFactory> fromEffects;

   @Internal
   public RegisterDimensionTransitionScreenEvent(
      Map<Pair<ResourceKey<Level>, ResourceKey<Level>>, DimensionTransitionScreenManager.ReceivingLevelScreenFactory> conditionalDimensionEffects,
      Map<ResourceKey<Level>, DimensionTransitionScreenManager.ReceivingLevelScreenFactory> toEffects,
      Map<ResourceKey<Level>, DimensionTransitionScreenManager.ReceivingLevelScreenFactory> fromEffects
   ) {
      this.conditionalDimensionEffects = conditionalDimensionEffects;
      this.toEffects = toEffects;
      this.fromEffects = fromEffects;
   }

   public boolean registerIncomingEffect(ResourceKey<Level> dimension, DimensionTransitionScreenManager.ReceivingLevelScreenFactory screen) {
      return this.toEffects.putIfAbsent(dimension, screen) == null;
   }

   public boolean registerOutgoingEffect(ResourceKey<Level> dimension, DimensionTransitionScreenManager.ReceivingLevelScreenFactory screen) {
      return this.fromEffects.putIfAbsent(dimension, screen) == null;
   }

   public boolean registerConditionalEffect(
      ResourceKey<Level> toDimension, ResourceKey<Level> fromDimension, DimensionTransitionScreenManager.ReceivingLevelScreenFactory screen
   ) {
      return this.conditionalDimensionEffects.putIfAbsent(Pair.of(toDimension, fromDimension), screen) == null;
   }
}
