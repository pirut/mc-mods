package net.minecraft.client.gui.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;

@OnlyIn(Dist.CLIENT)
public record GlyphRenderTypes(
   RenderType normal,
   RenderType seeThrough,
   RenderType polygonOffset,
   RenderPipeline guiPipeline,
   RenderType normalBlur,
   RenderType seeThroughBlur,
   RenderType polygonOffsetBlur
) {
   @Deprecated
   public GlyphRenderTypes(RenderType normal, RenderType seeThrough, RenderType polygonOffset, RenderPipeline guiPipeline) {
      this(normal, seeThrough, polygonOffset, guiPipeline, normal, seeThrough, polygonOffset);
   }

   public static GlyphRenderTypes createForIntensityTexture(Identifier p_468890_) {
      return new GlyphRenderTypes(
         RenderTypes.textIntensity(p_468890_),
         RenderTypes.textIntensitySeeThrough(p_468890_),
         RenderTypes.textIntensityPolygonOffset(p_468890_),
         RenderPipelines.GUI_TEXT_INTENSITY,
         NeoForgeRenderTypes.getTextIntensityFiltered(p_468890_),
         NeoForgeRenderTypes.getTextIntensitySeeThroughFiltered(p_468890_),
         NeoForgeRenderTypes.getTextIntensityPolygonOffsetFiltered(p_468890_)
      );
   }

   public static GlyphRenderTypes createForColorTexture(Identifier p_467758_) {
      return new GlyphRenderTypes(
         RenderTypes.text(p_467758_),
         RenderTypes.textSeeThrough(p_467758_),
         RenderTypes.textPolygonOffset(p_467758_),
         RenderPipelines.GUI_TEXT,
         NeoForgeRenderTypes.getTextFiltered(p_467758_),
         NeoForgeRenderTypes.getTextSeeThroughFiltered(p_467758_),
         NeoForgeRenderTypes.getTextPolygonOffsetFiltered(p_467758_)
      );
   }

   public RenderType select(Font.DisplayMode p_285259_) {
      return this.select(p_285259_, false);
   }

   public RenderType select(Font.DisplayMode p_285259_, boolean blur) {
      return switch (p_285259_) {
         case NORMAL -> blur ? this.normalBlur : this.normal;
         case SEE_THROUGH -> blur ? this.seeThroughBlur : this.seeThrough;
         case POLYGON_OFFSET -> blur ? this.polygonOffsetBlur : this.polygonOffset;
      };
   }
}
