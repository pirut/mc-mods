package net.neoforged.neoforge.client;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class DimensionSpecialEffectsManager {
   private static ImmutableMap<ResourceLocation, DimensionSpecialEffects> EFFECTS;
   private static DimensionSpecialEffects DEFAULT_EFFECTS;

   public static DimensionSpecialEffects getForType(ResourceLocation type) {
      return (DimensionSpecialEffects)EFFECTS.getOrDefault(type, DEFAULT_EFFECTS);
   }

   @Internal
   public static void init() {
      HashMap<ResourceLocation, DimensionSpecialEffects> effects = new HashMap<>();
      DEFAULT_EFFECTS = preRegisterVanillaEffects(effects);
      RegisterDimensionSpecialEffectsEvent event = new RegisterDimensionSpecialEffectsEvent(effects);
      ModLoader.postEventWrapContainerInModOrder(event);
      EFFECTS = ImmutableMap.copyOf(effects);
   }

   private static DimensionSpecialEffects preRegisterVanillaEffects(Map<ResourceLocation, DimensionSpecialEffects> effects) {
      DimensionSpecialEffects.OverworldEffects overworldEffects = new DimensionSpecialEffects.OverworldEffects();
      effects.put(BuiltinDimensionTypes.OVERWORLD_EFFECTS, overworldEffects);
      effects.put(BuiltinDimensionTypes.NETHER_EFFECTS, new DimensionSpecialEffects.NetherEffects());
      effects.put(BuiltinDimensionTypes.END_EFFECTS, new DimensionSpecialEffects.EndEffects());
      return overworldEffects;
   }

   private DimensionSpecialEffectsManager() {
   }
}
