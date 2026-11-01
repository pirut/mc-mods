package net.neoforged.neoforge.client;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterNamedRenderTypesEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class NamedRenderTypeManager {
   private static ImmutableMap<ResourceLocation, RenderTypeGroup> RENDER_TYPES;

   public static RenderTypeGroup get(ResourceLocation name) {
      return (RenderTypeGroup)RENDER_TYPES.getOrDefault(name, RenderTypeGroup.EMPTY);
   }

   @Internal
   public static void init() {
      HashMap<ResourceLocation, RenderTypeGroup> renderTypes = new HashMap<>();
      preRegisterVanillaRenderTypes(renderTypes);
      RegisterNamedRenderTypesEvent event = new RegisterNamedRenderTypesEvent(renderTypes);
      ModLoader.postEventWrapContainerInModOrder(event);
      RENDER_TYPES = ImmutableMap.copyOf(renderTypes);
   }

   private static void preRegisterVanillaRenderTypes(Map<ResourceLocation, RenderTypeGroup> blockRenderTypes) {
      blockRenderTypes.put(
         ResourceLocation.withDefaultNamespace("solid"), new RenderTypeGroup(RenderType.solid(), NeoForgeRenderTypes.ITEM_LAYERED_SOLID.get())
      );
      blockRenderTypes.put(
         ResourceLocation.withDefaultNamespace("cutout"), new RenderTypeGroup(RenderType.cutout(), NeoForgeRenderTypes.ITEM_LAYERED_CUTOUT.get())
      );
      blockRenderTypes.put(
         ResourceLocation.withDefaultNamespace("cutout_mipped"), new RenderTypeGroup(RenderType.cutoutMipped(), NeoForgeRenderTypes.ITEM_LAYERED_CUTOUT.get())
      );
      blockRenderTypes.put(
         ResourceLocation.withDefaultNamespace("cutout_mipped_all"),
         new RenderTypeGroup(RenderType.cutoutMipped(), NeoForgeRenderTypes.ITEM_LAYERED_CUTOUT_MIPPED.get())
      );
      blockRenderTypes.put(
         ResourceLocation.withDefaultNamespace("translucent"),
         new RenderTypeGroup(RenderType.translucent(), NeoForgeRenderTypes.ITEM_LAYERED_TRANSLUCENT.get())
      );
      blockRenderTypes.put(
         ResourceLocation.withDefaultNamespace("tripwire"), new RenderTypeGroup(RenderType.tripwire(), NeoForgeRenderTypes.ITEM_LAYERED_TRANSLUCENT.get())
      );
   }

   private NamedRenderTypeManager() {
   }
}
