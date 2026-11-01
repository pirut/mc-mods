package net.neoforged.neoforge.client.renderstate;

import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

@FunctionalInterface
public interface MapDecorationRenderStateModifier {
   void accept(MapItemSavedData var1, MapRenderState var2, MapRenderState.MapDecorationRenderState var3);
}
