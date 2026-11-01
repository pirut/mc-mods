package net.neoforged.neoforge.client;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterNamedRenderTypesEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class NamedRenderTypeManager {
   private static ImmutableMap<Identifier, RenderTypeGroup> RENDER_TYPES;

   public static RenderTypeGroup get(Identifier name) {
      return (RenderTypeGroup)RENDER_TYPES.getOrDefault(name, RenderTypeGroup.EMPTY);
   }

   @Internal
   public static void init() {
      HashMap<Identifier, RenderTypeGroup> renderTypes = new HashMap<>();
      preRegisterVanillaRenderTypes(renderTypes);
      RegisterNamedRenderTypesEvent event = new RegisterNamedRenderTypesEvent(renderTypes);
      ModLoader.postEventWrapContainerInModOrder(event);
      RENDER_TYPES = ImmutableMap.copyOf(renderTypes);
   }

   private static void preRegisterVanillaRenderTypes(Map<Identifier, RenderTypeGroup> blockRenderTypes) {
      blockRenderTypes.put(Identifier.withDefaultNamespace("solid"), new RenderTypeGroup(ChunkSectionLayer.SOLID, NeoForgeRenderTypes::getItemLayeredSolid));
      blockRenderTypes.put(Identifier.withDefaultNamespace("cutout"), new RenderTypeGroup(ChunkSectionLayer.CUTOUT, NeoForgeRenderTypes::getItemLayeredCutout));
      blockRenderTypes.put(
         Identifier.withDefaultNamespace("translucent"), new RenderTypeGroup(ChunkSectionLayer.TRANSLUCENT, NeoForgeRenderTypes::getItemLayeredTranslucent)
      );
      blockRenderTypes.put(
         Identifier.withDefaultNamespace("tripwire"), new RenderTypeGroup(ChunkSectionLayer.TRIPWIRE, NeoForgeRenderTypes::getItemLayeredTranslucent)
      );
   }

   private NamedRenderTypeManager() {
   }
}
