package net.neoforged.neoforge.client.gui.map;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;

public interface IMapDecorationRenderer {
   boolean render(
      MapRenderState.MapDecorationRenderState var1,
      PoseStack var2,
      SubmitNodeCollector var3,
      MapRenderState var4,
      TextureAtlas var5,
      boolean var6,
      int var7,
      int var8
   );
}
