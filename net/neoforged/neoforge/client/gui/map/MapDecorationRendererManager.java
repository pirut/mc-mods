package net.neoforged.neoforge.client.gui.map;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.neoforged.fml.ModLoader;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class MapDecorationRendererManager {
   private static final Map<MapDecorationType, IMapDecorationRenderer> RENDERERS = new IdentityHashMap<>();
   private static boolean initialized = false;

   private MapDecorationRendererManager() {
   }

   public static boolean render(
      MapRenderState.MapDecorationRenderState decorationRenderState,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      MapRenderState mapRenderState,
      TextureAtlas decorationSprites,
      boolean inItemFrame,
      int packedLight,
      int index
   ) {
      IMapDecorationRenderer decorationRenderer = RENDERERS.get(decorationRenderState.type.value());
      return decorationRenderer != null
         ? decorationRenderer.render(decorationRenderState, poseStack, submitNodeCollector, mapRenderState, decorationSprites, inItemFrame, packedLight, index)
         : false;
   }

   public static void init() {
      if (initialized) {
         throw new IllegalStateException("Duplicate initialization of MapDecorationRendererManager");
      } else {
         initialized = true;
         ModLoader.postEvent(new RegisterMapDecorationRenderersEvent(RENDERERS));
      }
   }
}
