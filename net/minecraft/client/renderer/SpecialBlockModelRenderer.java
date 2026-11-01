package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;

@Environment(EnvType.CLIENT)
public class SpecialBlockModelRenderer {
   public static final SpecialBlockModelRenderer EMPTY = new SpecialBlockModelRenderer(Map.of());
   private final Map<Block, SpecialModelRenderer<?>> renderers;

   public SpecialBlockModelRenderer(Map<Block, SpecialModelRenderer<?>> renderers) {
      this.renderers = renderers;
   }

   public static SpecialBlockModelRenderer vanilla(SpecialModelRenderer.BakingContext context) {
      return new SpecialBlockModelRenderer(SpecialModelRenderers.createBlockRenderers(context));
   }

   public void renderByBlock(
      Block block,
      ItemDisplayContext displayContext,
      PoseStack poseStack,
      SubmitNodeCollector nodeCollector,
      int packedLight,
      int packedOverlay,
      int outlneColor
   ) {
      SpecialModelRenderer<?> specialModelRenderer = this.renderers.get(block);
      if (specialModelRenderer != null) {
         specialModelRenderer.submit(null, displayContext, poseStack, nodeCollector, packedLight, packedOverlay, false, outlneColor);
      }
   }
}
