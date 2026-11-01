package net.neoforged.neoforge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.neoforged.neoforge.common.util.Lazy;

public enum NeoForgeRenderTypes {
   BLOCK_ITEM_LAYERED_SOLID(() -> getItemLayeredSolid(TextureAtlas.LOCATION_BLOCKS)),
   BLOCK_ITEM_LAYERED_CUTOUT(() -> getItemLayeredCutout(TextureAtlas.LOCATION_BLOCKS)),
   BLOCK_ITEM_LAYERED_TRANSLUCENT(() -> getItemLayeredTranslucent(TextureAtlas.LOCATION_BLOCKS)),
   BLOCK_ITEM_UNSORTED_TRANSLUCENT(() -> getUnsortedTranslucent(TextureAtlas.LOCATION_BLOCKS)),
   BLOCK_ITEM_UNLIT_TRANSLUCENT(() -> getUnlitTranslucent(TextureAtlas.LOCATION_BLOCKS)),
   BLOCK_ITEM_UNSORTED_UNLIT_TRANSLUCENT(() -> getUnlitUnsortedTranslucent(TextureAtlas.LOCATION_BLOCKS)),
   ITEM_LAYERED_SOLID(() -> getItemLayeredSolid(TextureAtlas.LOCATION_ITEMS)),
   ITEM_LAYERED_CUTOUT(() -> getItemLayeredCutout(TextureAtlas.LOCATION_ITEMS)),
   ITEM_LAYERED_TRANSLUCENT(() -> getItemLayeredTranslucent(TextureAtlas.LOCATION_ITEMS)),
   ITEM_UNSORTED_TRANSLUCENT(() -> getUnsortedTranslucent(TextureAtlas.LOCATION_ITEMS)),
   ITEM_UNLIT_TRANSLUCENT(() -> getUnlitTranslucent(TextureAtlas.LOCATION_ITEMS)),
   ITEM_UNSORTED_UNLIT_TRANSLUCENT(() -> getUnlitUnsortedTranslucent(TextureAtlas.LOCATION_ITEMS));

   private static final Supplier<GpuSampler> LINEAR_FILTERING_SAMPLER = () -> RenderSystem.getSamplerCache()
      .getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.NEAREST, false);
   private final Supplier<RenderType> renderTypeSupplier;

   public static RenderType getItemLayeredSolid(Identifier textureLocation) {
      return NeoForgeRenderTypes.Internal.LAYERED_ITEM_SOLID.apply(textureLocation);
   }

   public static RenderType getItemLayeredCutout(Identifier textureLocation) {
      return NeoForgeRenderTypes.Internal.LAYERED_ITEM_CUTOUT.apply(textureLocation);
   }

   public static RenderType getItemLayeredTranslucent(Identifier textureLocation) {
      return NeoForgeRenderTypes.Internal.LAYERED_ITEM_TRANSLUCENT.apply(textureLocation);
   }

   public static RenderType getUnsortedTranslucent(Identifier textureLocation) {
      return NeoForgeRenderTypes.Internal.UNSORTED_TRANSLUCENT.apply(textureLocation);
   }

   public static RenderType getUnlitTranslucent(Identifier textureLocation) {
      return NeoForgeRenderTypes.Internal.UNLIT_TRANSLUCENT_SORTED.apply(textureLocation);
   }

   public static RenderType getUnlitUnsortedTranslucent(Identifier textureLocation) {
      return NeoForgeRenderTypes.Internal.UNLIT_TRANSLUCENT_UNSORTED.apply(textureLocation);
   }

   public static RenderType getEntityCutoutMipped(Identifier textureLocation) {
      return NeoForgeRenderTypes.Internal.LAYERED_ITEM_CUTOUT_MIPPED.apply(textureLocation);
   }

   public static RenderType getTextFiltered(Identifier locationIn) {
      return NeoForgeRenderTypes.Internal.TEXT_FILTERED.apply(locationIn);
   }

   public static RenderType getTextIntensityFiltered(Identifier locationIn) {
      return NeoForgeRenderTypes.Internal.TEXT_INTENSITY_FILTERED.apply(locationIn);
   }

   public static RenderType getTextPolygonOffsetFiltered(Identifier locationIn) {
      return NeoForgeRenderTypes.Internal.TEXT_POLYGON_OFFSET_FILTERED.apply(locationIn);
   }

   public static RenderType getTextIntensityPolygonOffsetFiltered(Identifier locationIn) {
      return NeoForgeRenderTypes.Internal.TEXT_INTENSITY_POLYGON_OFFSET_FILTERED.apply(locationIn);
   }

   public static RenderType getTextSeeThroughFiltered(Identifier locationIn) {
      return NeoForgeRenderTypes.Internal.TEXT_SEETHROUGH_FILTERED.apply(locationIn);
   }

   public static RenderType getTextIntensitySeeThroughFiltered(Identifier locationIn) {
      return NeoForgeRenderTypes.Internal.TEXT_INTENSITY_SEETHROUGH_FILTERED.apply(locationIn);
   }

   private NeoForgeRenderTypes(Supplier<RenderType> renderTypeSupplier) {
      this.renderTypeSupplier = Lazy.of(renderTypeSupplier);
   }

   public RenderType get() {
      return this.renderTypeSupplier.get();
   }

   private static final class Internal {
      public static Function<Identifier, RenderType> UNSORTED_TRANSLUCENT = Util.memoize(NeoForgeRenderTypes.Internal::unsortedTranslucent);
      public static Function<Identifier, RenderType> UNLIT_TRANSLUCENT_SORTED = Util.memoize(tex -> unlitTranslucent(tex, true));
      public static Function<Identifier, RenderType> UNLIT_TRANSLUCENT_UNSORTED = Util.memoize(tex -> unlitTranslucent(tex, false));
      public static Function<Identifier, RenderType> LAYERED_ITEM_SOLID = Util.memoize(NeoForgeRenderTypes.Internal::layeredItemSolid);
      public static Function<Identifier, RenderType> LAYERED_ITEM_CUTOUT = Util.memoize(NeoForgeRenderTypes.Internal::layeredItemCutout);
      public static Function<Identifier, RenderType> LAYERED_ITEM_CUTOUT_MIPPED = Util.memoize(NeoForgeRenderTypes.Internal::layeredItemCutoutMipped);
      public static Function<Identifier, RenderType> LAYERED_ITEM_TRANSLUCENT = Util.memoize(NeoForgeRenderTypes.Internal::layeredItemTranslucent);
      public static Function<Identifier, RenderType> TEXT_FILTERED = Util.memoize(NeoForgeRenderTypes.Internal::getTextFiltered);
      public static Function<Identifier, RenderType> TEXT_INTENSITY_FILTERED = Util.memoize(NeoForgeRenderTypes.Internal::getTextIntensityFiltered);
      public static Function<Identifier, RenderType> TEXT_POLYGON_OFFSET_FILTERED = Util.memoize(NeoForgeRenderTypes.Internal::getTextPolygonOffsetFiltered);
      public static Function<Identifier, RenderType> TEXT_INTENSITY_POLYGON_OFFSET_FILTERED = Util.memoize(
         NeoForgeRenderTypes.Internal::getTextIntensityPolygonOffsetFiltered
      );
      public static Function<Identifier, RenderType> TEXT_SEETHROUGH_FILTERED = Util.memoize(NeoForgeRenderTypes.Internal::getTextSeeThroughFiltered);
      public static Function<Identifier, RenderType> TEXT_INTENSITY_SEETHROUGH_FILTERED = Util.memoize(
         NeoForgeRenderTypes.Internal::getTextIntensitySeeThroughFiltered
      );

      private static RenderType unsortedTranslucent(Identifier textureLocation) {
         RenderSetup renderState = RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT)
            .withTexture("Sampler0", textureLocation)
            .useLightmap()
            .useOverlay()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .affectsCrumbling()
            .createRenderSetup();
         return RenderType.create("neoforge_entity_unsorted_translucent", renderState);
      }

      private static RenderType unlitTranslucent(Identifier textureLocation, boolean sortingEnabled) {
         RenderSetup.RenderSetupBuilder renderStateBuilder = RenderSetup.builder(NeoForgeRenderPipelines.ENTITY_UNLIT_TRANSLUCENT)
            .withTexture("Sampler0", textureLocation)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE);
         if (sortingEnabled) {
            renderStateBuilder.sortOnUpload();
         }

         return RenderType.create("neoforge_entity_unlit_translucent", renderStateBuilder.createRenderSetup());
      }

      private static RenderType layeredItemSolid(Identifier locationIn) {
         RenderSetup rendertype$state = RenderSetup.builder(RenderPipelines.ENTITY_SOLID)
            .withTexture("Sampler0", locationIn)
            .useLightmap()
            .useOverlay()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .affectsCrumbling()
            .createRenderSetup();
         return RenderType.create("neoforge_item_entity_solid", rendertype$state);
      }

      private static RenderType layeredItemCutout(Identifier locationIn) {
         RenderSetup rendertype$state = RenderSetup.builder(RenderPipelines.ENTITY_CUTOUT)
            .withTexture("Sampler0", locationIn)
            .useLightmap()
            .useOverlay()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .affectsCrumbling()
            .createRenderSetup();
         return RenderType.create("neoforge_item_entity_cutout", rendertype$state);
      }

      private static RenderType layeredItemCutoutMipped(Identifier locationIn) {
         RenderSetup rendertype$state = RenderSetup.builder(NeoForgeRenderPipelines.ENTITY_SMOOTH_CUTOUT_CULL)
            .withTexture("Sampler0", locationIn)
            .useLightmap()
            .useOverlay()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .affectsCrumbling()
            .createRenderSetup();
         return RenderType.create("neoforge_item_entity_cutout_mipped", rendertype$state);
      }

      private static RenderType layeredItemTranslucent(Identifier locationIn) {
         RenderSetup rendertype$state = RenderSetup.builder(NeoForgeRenderPipelines.ENTITY_TRANSLUCENT_CULL)
            .withTexture("Sampler0", locationIn)
            .useLightmap()
            .useOverlay()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .affectsCrumbling()
            .sortOnUpload()
            .createRenderSetup();
         return RenderType.create("neoforge_item_entity_translucent_cull", rendertype$state);
      }

      private static RenderType getTextFiltered(Identifier locationIn) {
         RenderSetup rendertype$state = RenderSetup.builder(RenderPipelines.TEXT)
            .withTexture("Sampler0", locationIn, NeoForgeRenderTypes.LINEAR_FILTERING_SAMPLER)
            .useLightmap()
            .createRenderSetup();
         return RenderType.create("neoforge_text", rendertype$state);
      }

      private static RenderType getTextIntensityFiltered(Identifier locationIn) {
         RenderSetup rendertype$state = RenderSetup.builder(RenderPipelines.TEXT_INTENSITY)
            .withTexture("Sampler0", locationIn, NeoForgeRenderTypes.LINEAR_FILTERING_SAMPLER)
            .useLightmap()
            .createRenderSetup();
         return RenderType.create("neoforge_text_intensity", rendertype$state);
      }

      private static RenderType getTextPolygonOffsetFiltered(Identifier locationIn) {
         RenderSetup rendertype$state = RenderSetup.builder(RenderPipelines.TEXT_POLYGON_OFFSET)
            .withTexture("Sampler0", locationIn, NeoForgeRenderTypes.LINEAR_FILTERING_SAMPLER)
            .useLightmap()
            .createRenderSetup();
         return RenderType.create("neoforge_text_polygon_offset", rendertype$state);
      }

      private static RenderType getTextIntensityPolygonOffsetFiltered(Identifier locationIn) {
         RenderSetup rendertype$state = RenderSetup.builder(RenderPipelines.TEXT_INTENSITY)
            .withTexture("Sampler0", locationIn, NeoForgeRenderTypes.LINEAR_FILTERING_SAMPLER)
            .useLightmap()
            .createRenderSetup();
         return RenderType.create("neoforge_text_intensity_polygon_offset", rendertype$state);
      }

      private static RenderType getTextSeeThroughFiltered(Identifier locationIn) {
         RenderSetup rendertype$state = RenderSetup.builder(RenderPipelines.TEXT_SEE_THROUGH)
            .withTexture("Sampler0", locationIn, NeoForgeRenderTypes.LINEAR_FILTERING_SAMPLER)
            .useLightmap()
            .createRenderSetup();
         return RenderType.create("neoforge_text_see_through", rendertype$state);
      }

      private static RenderType getTextIntensitySeeThroughFiltered(Identifier locationIn) {
         RenderSetup rendertype$state = RenderSetup.builder(RenderPipelines.TEXT_INTENSITY_SEE_THROUGH)
            .withTexture("Sampler0", locationIn, NeoForgeRenderTypes.LINEAR_FILTERING_SAMPLER)
            .useLightmap()
            .createRenderSetup();
         return RenderType.create("neoforge_text_intensity_see_through", rendertype$state);
      }
   }
}
