package net.neoforged.neoforge.client.model.item;

import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SpriteGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.color.item.FluidContentsTint;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.ComposedModelState;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public class DynamicFluidContainerModel implements ItemModel {
   private static final Transformation FLUID_TRANSFORM = new Transformation(
      new Vector3f(), new Quaternionf(), new Vector3f(1.0F, 1.0F, 1.002F), new Quaternionf()
   );
   private static final Transformation COVER_TRANSFORM = new Transformation(
      new Vector3f(), new Quaternionf(), new Vector3f(1.0F, 1.0F, 1.004F), new Quaternionf()
   );
   private static final ModelDebugName DEBUG_NAME = () -> "DynamicFluidContainerModel";
   private static final RenderTypeGroup RENDER_TYPES_DEFAULT = new RenderTypeGroup(ChunkSectionLayer.TRANSLUCENT, NeoForgeRenderTypes::getUnsortedTranslucent);
   private static final RenderTypeGroup RENDER_TYPES_UNLIT = new RenderTypeGroup(ChunkSectionLayer.TRANSLUCENT, NeoForgeRenderTypes::getUnlitTranslucent);
   private final DynamicFluidContainerModel.Unbaked unbakedModel;
   private final ItemModel.BakingContext bakingContext;
   private final ItemTransforms itemTransforms;
   private final Map<Fluid, ItemModel> cache = new IdentityHashMap<>();

   private static RenderTypeGroup getLayerRenderTypes(boolean unlit) {
      return unlit ? RENDER_TYPES_UNLIT : RENDER_TYPES_DEFAULT;
   }

   private DynamicFluidContainerModel(DynamicFluidContainerModel.Unbaked unbakedModel, ItemModel.BakingContext bakingContext) {
      this.unbakedModel = unbakedModel;
      this.bakingContext = bakingContext;
      ResolvedModel baseItemModel = bakingContext.blockModelBaker().getModel(Identifier.withDefaultNamespace("item/generated"));
      if (baseItemModel == null) {
         throw new IllegalStateException("Failed to access item/generated model");
      } else {
         this.itemTransforms = baseItemModel.getTopTransforms();
      }
   }

   private ItemModel bakeModelForFluid(Fluid fluid) {
      SpriteGetter sprites = this.bakingContext.blockModelBaker().sprites();
      Material particleLocation = this.unbakedModel.textures.particle.map(ClientHooks::getItemMaterial).orElse(null);
      Material baseLocation = this.unbakedModel.textures.base.map(ClientHooks::getItemMaterial).orElse(null);
      Material fluidMaskLocation = this.unbakedModel.textures.fluid.map(ClientHooks::getItemMaterial).orElse(null);
      Material coverLocation = this.unbakedModel.textures.cover.map(ClientHooks::getItemMaterial).orElse(null);
      TextureAtlasSprite baseSprite = baseLocation != null ? sprites.get(baseLocation, DEBUG_NAME) : null;
      TextureAtlasSprite fluidSprite = fluid != Fluids.EMPTY
         ? sprites.get(ClientHooks.getBlockMaterial(IClientFluidTypeExtensions.of(fluid).getStillTexture()), DEBUG_NAME)
         : null;
      TextureAtlasSprite coverSprite = coverLocation == null || this.unbakedModel.coverIsMask && baseLocation == null
         ? null
         : sprites.get(coverLocation, DEBUG_NAME);
      TextureAtlasSprite particleSprite = particleLocation != null ? sprites.get(particleLocation, DEBUG_NAME) : null;
      if (particleSprite == null) {
         particleSprite = fluidSprite;
      }

      if (particleSprite == null) {
         particleSprite = baseSprite;
      }

      if (particleSprite == null && !this.unbakedModel.coverIsMask) {
         particleSprite = coverSprite;
      }

      ModelState state = BlockModelRotation.IDENTITY;
      if (this.unbakedModel.flipGas && fluid != Fluids.EMPTY && fluid.getFluidType().isLighterThanAir()) {
         state = new ComposedModelState(state, new Transformation(null, new Quaternionf(0.0F, 0.0F, 1.0F, 0.0F), null, null));
      }

      List<ItemModel> subModels = new ArrayList<>();
      ModelRenderProperties renderProperties = new ModelRenderProperties(false, particleSprite, this.itemTransforms);
      RenderTypeGroup normalRenderTypes = getLayerRenderTypes(false);
      if (baseLocation != null) {
         List<BlockElement> unbaked = UnbakedElementsHelper.createUnbakedItemElements(0, baseSprite);
         List<BakedQuad> quads = UnbakedElementsHelper.bakeElements(unbaked, $ -> baseSprite, state);
         Function<ItemStack, RenderType> renderType = RenderTypeHelper.detectItemModelRenderType(quads, normalRenderTypes);
         subModels.add(new BlockModelWrapper(List.of(), quads, renderProperties, renderType));
      }

      if (fluidMaskLocation != null && fluidSprite != null) {
         TextureAtlasSprite templateSprite = sprites.get(fluidMaskLocation, DEBUG_NAME);
         ComposedModelState transformedState = new ComposedModelState(state, FLUID_TRANSFORM);
         List<BlockElement> unbaked = UnbakedElementsHelper.createUnbakedItemMaskElements(0, templateSprite);
         List<BakedQuad> quads = UnbakedElementsHelper.bakeElements(unbaked, $ -> fluidSprite, transformedState);
         boolean emissive = this.unbakedModel.applyFluidLuminosity && fluid.getFluidType().getLightLevel() > 0;
         Function<ItemStack, RenderType> renderType = RenderTypeHelper.detectItemModelRenderType(quads, getLayerRenderTypes(emissive));
         if (emissive) {
            quads = new ArrayList<>(quads);
            quads.replaceAll(DynamicFluidContainerModel::setMaxEmissivity);
         }

         subModels.add(new BlockModelWrapper(List.of(FluidContentsTint.INSTANCE), quads, renderProperties, renderType));
      }

      if (coverSprite != null) {
         TextureAtlasSprite sprite = this.unbakedModel.coverIsMask ? baseSprite : coverSprite;
         ComposedModelState transformedState = new ComposedModelState(state, COVER_TRANSFORM);
         List<BlockElement> unbaked = UnbakedElementsHelper.createUnbakedItemMaskElements(0, coverSprite);
         List<BakedQuad> quads = UnbakedElementsHelper.bakeElements(unbaked, $ -> sprite, transformedState);
         Function<ItemStack, RenderType> renderType = RenderTypeHelper.detectItemModelRenderType(quads, normalRenderTypes);
         subModels.add(new BlockModelWrapper(List.of(), quads, renderProperties, renderType));
      }

      return new CompositeModel(subModels);
   }

   private static BakedQuad setMaxEmissivity(BakedQuad quad) {
      return new BakedQuad(
         quad.position0(),
         quad.position1(),
         quad.position2(),
         quad.position3(),
         quad.packedUV0(),
         quad.packedUV1(),
         quad.packedUV2(),
         quad.packedUV3(),
         quad.tintIndex(),
         quad.direction(),
         quad.sprite(),
         quad.shade(),
         15,
         quad.bakedNormals(),
         quad.bakedColors(),
         quad.hasAmbientOcclusion()
      );
   }

   @Override
   public void update(
      ItemStackRenderState renderState,
      ItemStack stack,
      ItemModelResolver modelResolver,
      ItemDisplayContext displayContext,
      @Nullable ClientLevel level,
      @Nullable ItemOwner owner,
      int seed
   ) {
      FluidStack fluidStack = FluidUtil.getFirstStackContained(stack);
      Fluid fluid = fluidStack.isEmpty() ? this.unbakedModel.fluid : fluidStack.getFluid();
      this.cache.computeIfAbsent(fluid, this::bakeModelForFluid).update(renderState, stack, modelResolver, displayContext, level, owner, seed);
   }

   public record Textures(Optional<Identifier> particle, Optional<Identifier> base, Optional<Identifier> fluid, Optional<Identifier> cover) {
      public static final Codec<DynamicFluidContainerModel.Textures> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                  Identifier.CODEC.optionalFieldOf("particle").forGetter(DynamicFluidContainerModel.Textures::particle),
                  Identifier.CODEC.optionalFieldOf("base").forGetter(DynamicFluidContainerModel.Textures::base),
                  Identifier.CODEC.optionalFieldOf("fluid").forGetter(DynamicFluidContainerModel.Textures::fluid),
                  Identifier.CODEC.optionalFieldOf("cover").forGetter(DynamicFluidContainerModel.Textures::cover)
               )
               .apply(instance, DynamicFluidContainerModel.Textures::new)
         )
         .validate(
            textures -> !textures.particle.isPresent() && !textures.base.isPresent() && !textures.fluid.isPresent() && !textures.cover.isPresent()
               ? DataResult.error(() -> "Dynamic fluid container model requires at least one particle, base, fluid or cover texture.")
               : DataResult.success(textures)
         );
   }

   public record Unbaked(DynamicFluidContainerModel.Textures textures, Fluid fluid, boolean flipGas, boolean coverIsMask, boolean applyFluidLuminosity)
      implements ItemModel.Unbaked {
      public static final MapCodec<DynamicFluidContainerModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               DynamicFluidContainerModel.Textures.CODEC.fieldOf("textures").forGetter(DynamicFluidContainerModel.Unbaked::textures),
               BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(DynamicFluidContainerModel.Unbaked::fluid),
               Codec.BOOL.optionalFieldOf("flip_gas", false).forGetter(DynamicFluidContainerModel.Unbaked::flipGas),
               Codec.BOOL.optionalFieldOf("cover_is_mask", true).forGetter(DynamicFluidContainerModel.Unbaked::coverIsMask),
               Codec.BOOL.optionalFieldOf("apply_fluid_luminosity", true).forGetter(DynamicFluidContainerModel.Unbaked::applyFluidLuminosity)
            )
            .apply(instance, DynamicFluidContainerModel.Unbaked::new)
      );

      @Override
      public MapCodec<? extends ItemModel.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public ItemModel bake(ItemModel.BakingContext bakingContext) {
         return new DynamicFluidContainerModel(this, bakingContext);
      }

      @Override
      public void resolveDependencies(ResolvableModel.Resolver resolver) {
      }
   }
}
