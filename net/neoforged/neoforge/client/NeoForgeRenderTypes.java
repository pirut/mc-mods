package net.neoforged.neoforge.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.Lazy;

public enum NeoForgeRenderTypes {
   ITEM_LAYERED_SOLID(() -> getItemLayeredSolid(TextureAtlas.LOCATION_BLOCKS)),
   ITEM_LAYERED_CUTOUT(() -> getItemLayeredCutout(TextureAtlas.LOCATION_BLOCKS)),
   ITEM_LAYERED_CUTOUT_MIPPED(() -> getItemLayeredCutoutMipped(TextureAtlas.LOCATION_BLOCKS)),
   ITEM_LAYERED_TRANSLUCENT(() -> getItemLayeredTranslucent(TextureAtlas.LOCATION_BLOCKS)),
   ITEM_UNSORTED_TRANSLUCENT(() -> getUnsortedTranslucent(TextureAtlas.LOCATION_BLOCKS)),
   ITEM_UNLIT_TRANSLUCENT(() -> getUnlitTranslucent(TextureAtlas.LOCATION_BLOCKS)),
   ITEM_UNSORTED_UNLIT_TRANSLUCENT(() -> getUnlitTranslucent(TextureAtlas.LOCATION_BLOCKS, false)),
   TRANSLUCENT_ON_PARTICLES_TARGET(() -> getTranslucentParticlesTarget(TextureAtlas.LOCATION_BLOCKS));

   public static boolean enableTextTextureLinearFiltering = false;
   private final Supplier<RenderType> renderTypeSupplier;

   public static RenderType getItemLayeredSolid(ResourceLocation textureLocation) {
      return NeoForgeRenderTypes.Internal.LAYERED_ITEM_SOLID.apply(textureLocation);
   }

   public static RenderType getItemLayeredCutout(ResourceLocation textureLocation) {
      return NeoForgeRenderTypes.Internal.LAYERED_ITEM_CUTOUT.apply(textureLocation);
   }

   public static RenderType getItemLayeredCutoutMipped(ResourceLocation textureLocation) {
      return NeoForgeRenderTypes.Internal.LAYERED_ITEM_CUTOUT_MIPPED.apply(textureLocation);
   }

   public static RenderType getItemLayeredTranslucent(ResourceLocation textureLocation) {
      return NeoForgeRenderTypes.Internal.LAYERED_ITEM_TRANSLUCENT.apply(textureLocation);
   }

   public static RenderType getUnsortedTranslucent(ResourceLocation textureLocation) {
      return NeoForgeRenderTypes.Internal.UNSORTED_TRANSLUCENT.apply(textureLocation);
   }

   public static RenderType getUnlitTranslucent(ResourceLocation textureLocation) {
      return NeoForgeRenderTypes.Internal.UNLIT_TRANSLUCENT_SORTED.apply(textureLocation);
   }

   public static RenderType getUnlitTranslucent(ResourceLocation textureLocation, boolean sortingEnabled) {
      return (sortingEnabled ? NeoForgeRenderTypes.Internal.UNLIT_TRANSLUCENT_SORTED : NeoForgeRenderTypes.Internal.UNLIT_TRANSLUCENT_UNSORTED)
         .apply(textureLocation);
   }

   public static RenderType getEntityCutoutMipped(ResourceLocation textureLocation) {
      return NeoForgeRenderTypes.Internal.LAYERED_ITEM_CUTOUT_MIPPED.apply(textureLocation);
   }

   public static RenderType getText(ResourceLocation locationIn) {
      return NeoForgeRenderTypes.Internal.TEXT.apply(locationIn);
   }

   public static RenderType getTextIntensity(ResourceLocation locationIn) {
      return NeoForgeRenderTypes.Internal.TEXT_INTENSITY.apply(locationIn);
   }

   public static RenderType getTextPolygonOffset(ResourceLocation locationIn) {
      return NeoForgeRenderTypes.Internal.TEXT_POLYGON_OFFSET.apply(locationIn);
   }

   public static RenderType getTextIntensityPolygonOffset(ResourceLocation locationIn) {
      return NeoForgeRenderTypes.Internal.TEXT_INTENSITY_POLYGON_OFFSET.apply(locationIn);
   }

   public static RenderType getTextSeeThrough(ResourceLocation locationIn) {
      return NeoForgeRenderTypes.Internal.TEXT_SEETHROUGH.apply(locationIn);
   }

   public static RenderType getTextIntensitySeeThrough(ResourceLocation locationIn) {
      return NeoForgeRenderTypes.Internal.TEXT_INTENSITY_SEETHROUGH.apply(locationIn);
   }

   public static RenderType getTranslucentParticlesTarget(ResourceLocation locationIn) {
      return NeoForgeRenderTypes.Internal.TRANSLUCENT_PARTICLES_TARGET.apply(locationIn);
   }

   private NeoForgeRenderTypes(Supplier<RenderType> renderTypeSupplier) {
      this.renderTypeSupplier = Lazy.of(renderTypeSupplier);
   }

   public RenderType get() {
      return this.renderTypeSupplier.get();
   }

   private static class CustomizableTextureState extends RenderStateShard.TextureStateShard {
      private final BooleanSupplier blurSupplier;
      private final BooleanSupplier mipmapSupplier;

      private CustomizableTextureState(ResourceLocation resLoc, BooleanSupplier blur, BooleanSupplier mipmap) {
         super(resLoc, blur.getAsBoolean(), mipmap.getAsBoolean());
         this.blurSupplier = blur;
         this.mipmapSupplier = mipmap;
      }

      @Override
      public void setupRenderState() {
         this.blur = this.blurSupplier.getAsBoolean();
         this.mipmap = this.mipmapSupplier.getAsBoolean();
         super.setupRenderState();
      }
   }

   private static class Internal {
      private static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_TRANSLUCENT_UNLIT_SHADER = new RenderStateShard.ShaderStateShard(
         ClientHooks.ClientEvents::getEntityTranslucentUnlitShader
      );
      public static Function<ResourceLocation, RenderType> UNSORTED_TRANSLUCENT = Util.memoize(NeoForgeRenderTypes.Internal::unsortedTranslucent);
      public static Function<ResourceLocation, RenderType> UNLIT_TRANSLUCENT_SORTED = Util.memoize(tex -> unlitTranslucent(tex, true));
      public static Function<ResourceLocation, RenderType> UNLIT_TRANSLUCENT_UNSORTED = Util.memoize(tex -> unlitTranslucent(tex, false));
      public static Function<ResourceLocation, RenderType> LAYERED_ITEM_SOLID = Util.memoize(NeoForgeRenderTypes.Internal::layeredItemSolid);
      public static Function<ResourceLocation, RenderType> LAYERED_ITEM_CUTOUT = Util.memoize(NeoForgeRenderTypes.Internal::layeredItemCutout);
      public static Function<ResourceLocation, RenderType> LAYERED_ITEM_CUTOUT_MIPPED = Util.memoize(NeoForgeRenderTypes.Internal::layeredItemCutoutMipped);
      public static Function<ResourceLocation, RenderType> LAYERED_ITEM_TRANSLUCENT = Util.memoize(NeoForgeRenderTypes.Internal::layeredItemTranslucent);
      public static Function<ResourceLocation, RenderType> TEXT = Util.memoize(NeoForgeRenderTypes.Internal::getText);
      public static Function<ResourceLocation, RenderType> TEXT_INTENSITY = Util.memoize(NeoForgeRenderTypes.Internal::getTextIntensity);
      public static Function<ResourceLocation, RenderType> TEXT_POLYGON_OFFSET = Util.memoize(NeoForgeRenderTypes.Internal::getTextPolygonOffset);
      public static Function<ResourceLocation, RenderType> TEXT_INTENSITY_POLYGON_OFFSET = Util.memoize(
         NeoForgeRenderTypes.Internal::getTextIntensityPolygonOffset
      );
      public static Function<ResourceLocation, RenderType> TEXT_SEETHROUGH = Util.memoize(NeoForgeRenderTypes.Internal::getTextSeeThrough);
      public static Function<ResourceLocation, RenderType> TEXT_INTENSITY_SEETHROUGH = Util.memoize(NeoForgeRenderTypes.Internal::getTextIntensitySeeThrough);
      public static Function<ResourceLocation, RenderType> TRANSLUCENT_PARTICLES_TARGET = Util.memoize(
         NeoForgeRenderTypes.Internal::getTranslucentParticlesTarget
      );

      private static RenderType unsortedTranslucent(ResourceLocation textureLocation) {
         boolean sortingEnabled = false;
         RenderType.CompositeState renderState = RenderType.CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(textureLocation, false, false))
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setCullState(RenderType.NO_CULL)
            .setLightmapState(RenderType.LIGHTMAP)
            .setOverlayState(RenderType.OVERLAY)
            .createCompositeState(true);
         return RenderType.create(
            "neoforge_entity_unsorted_translucent", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false, renderState
         );
      }

      private static RenderType unlitTranslucent(ResourceLocation textureLocation, boolean sortingEnabled) {
         RenderType.CompositeState renderState = RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_UNLIT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(textureLocation, false, false))
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setCullState(RenderType.NO_CULL)
            .setLightmapState(RenderType.LIGHTMAP)
            .setOverlayState(RenderType.OVERLAY)
            .createCompositeState(true);
         return RenderType.create(
            "neoforge_entity_unlit_translucent", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, sortingEnabled, renderState
         );
      }

      private static RenderType layeredItemSolid(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_ENTITY_SOLID_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
            .setTransparencyState(RenderType.NO_TRANSPARENCY)
            .setLightmapState(RenderType.LIGHTMAP)
            .setOverlayState(RenderType.OVERLAY)
            .createCompositeState(true);
         return RenderType.create("neoforge_item_entity_solid", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false, rendertype$state);
      }

      private static RenderType layeredItemCutout(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_ENTITY_CUTOUT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
            .setTransparencyState(RenderType.NO_TRANSPARENCY)
            .setLightmapState(RenderType.LIGHTMAP)
            .setOverlayState(RenderType.OVERLAY)
            .createCompositeState(true);
         return RenderType.create("neoforge_item_entity_cutout", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false, rendertype$state);
      }

      private static RenderType layeredItemCutoutMipped(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_ENTITY_SMOOTH_CUTOUT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, true))
            .setTransparencyState(RenderType.NO_TRANSPARENCY)
            .setLightmapState(RenderType.LIGHTMAP)
            .setOverlayState(RenderType.OVERLAY)
            .createCompositeState(true);
         return RenderType.create(
            "neoforge_item_entity_cutout_mipped", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false, rendertype$state
         );
      }

      private static RenderType layeredItemTranslucent(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(RenderType.LIGHTMAP)
            .setOverlayState(RenderType.OVERLAY)
            .createCompositeState(true);
         return RenderType.create(
            "neoforge_item_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, rendertype$state
         );
      }

      private static RenderType getText(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_TEXT_SHADER)
            .setTextureState(
               new NeoForgeRenderTypes.CustomizableTextureState(locationIn, () -> NeoForgeRenderTypes.enableTextTextureLinearFiltering, () -> false)
            )
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(RenderType.LIGHTMAP)
            .createCompositeState(false);
         return RenderType.create("neoforge_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, true, rendertype$state);
      }

      private static RenderType getTextIntensity(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_TEXT_INTENSITY_SHADER)
            .setTextureState(
               new NeoForgeRenderTypes.CustomizableTextureState(locationIn, () -> NeoForgeRenderTypes.enableTextTextureLinearFiltering, () -> false)
            )
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(RenderType.LIGHTMAP)
            .createCompositeState(false);
         return RenderType.create(
            "neoforge_text_intensity", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, true, rendertype$state
         );
      }

      private static RenderType getTextPolygonOffset(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_TEXT_SHADER)
            .setTextureState(
               new NeoForgeRenderTypes.CustomizableTextureState(locationIn, () -> NeoForgeRenderTypes.enableTextTextureLinearFiltering, () -> false)
            )
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(RenderType.LIGHTMAP)
            .setLayeringState(RenderType.POLYGON_OFFSET_LAYERING)
            .createCompositeState(false);
         return RenderType.create(
            "neoforge_text_polygon_offset", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, true, rendertype$state
         );
      }

      private static RenderType getTextIntensityPolygonOffset(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_TEXT_INTENSITY_SHADER)
            .setTextureState(
               new NeoForgeRenderTypes.CustomizableTextureState(locationIn, () -> NeoForgeRenderTypes.enableTextTextureLinearFiltering, () -> false)
            )
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(RenderType.LIGHTMAP)
            .setLayeringState(RenderType.POLYGON_OFFSET_LAYERING)
            .createCompositeState(false);
         return RenderType.create(
            "neoforge_text_intensity_polygon_offset",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            rendertype$state
         );
      }

      private static RenderType getTextSeeThrough(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_TEXT_SEE_THROUGH_SHADER)
            .setTextureState(
               new NeoForgeRenderTypes.CustomizableTextureState(locationIn, () -> NeoForgeRenderTypes.enableTextTextureLinearFiltering, () -> false)
            )
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(RenderType.LIGHTMAP)
            .setDepthTestState(RenderType.NO_DEPTH_TEST)
            .setWriteMaskState(RenderType.COLOR_WRITE)
            .createCompositeState(false);
         return RenderType.create(
            "neoforge_text_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, true, rendertype$state
         );
      }

      private static RenderType getTextIntensitySeeThrough(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_TEXT_INTENSITY_SEE_THROUGH_SHADER)
            .setTextureState(
               new NeoForgeRenderTypes.CustomizableTextureState(locationIn, () -> NeoForgeRenderTypes.enableTextTextureLinearFiltering, () -> false)
            )
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(RenderType.LIGHTMAP)
            .setDepthTestState(RenderType.NO_DEPTH_TEST)
            .setWriteMaskState(RenderType.COLOR_WRITE)
            .createCompositeState(false);
         return RenderType.create(
            "neoforge_text_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, true, rendertype$state
         );
      }

      private static RenderType getTranslucentParticlesTarget(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_TRANSLUCENT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, true))
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(RenderType.LIGHTMAP)
            .setOutputState(RenderType.PARTICLES_TARGET)
            .createCompositeState(true);
         return RenderType.create(
            "neoforge_translucent_particles_target", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 2097152, true, true, rendertype$state
         );
      }
   }
}
