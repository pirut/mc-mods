package net.neoforged.neoforge.client.gui.map;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.MapDecorationTextureManager;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public interface IMapDecorationRenderer {
   boolean render(
      MapDecoration var1, PoseStack var2, MultiBufferSource var3, MapItemSavedData var4, MapDecorationTextureManager var5, boolean var6, int var7, int var8
   );
}
