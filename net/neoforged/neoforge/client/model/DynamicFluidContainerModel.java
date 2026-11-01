package net.neoforged.neoforge.client.model;

import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.geometry.StandaloneGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DynamicFluidContainerModel implements IUnbakedGeometry<DynamicFluidContainerModel> {
   private static final Transformation FLUID_TRANSFORM = new Transformation(
      new Vector3f(), new Quaternionf(), new Vector3f(1.0F, 1.0F, 1.002F), new Quaternionf()
   );
   private static final Transformation COVER_TRANSFORM = new Transformation(
      new Vector3f(), new Quaternionf(), new Vector3f(1.0F, 1.0F, 1.004F), new Quaternionf()
   );
   private final Fluid fluid;
   private final boolean flipGas;
   private final boolean coverIsMask;
   private final boolean applyFluidLuminosity;

   private DynamicFluidContainerModel(Fluid fluid, boolean flipGas, boolean coverIsMask, boolean applyFluidLuminosity) {
      this.fluid = fluid;
      this.flipGas = flipGas;
      this.coverIsMask = coverIsMask;
      this.applyFluidLuminosity = applyFluidLuminosity;
   }

   public static RenderTypeGroup getLayerRenderTypes(boolean unlit) {
      return new RenderTypeGroup(
         RenderType.translucent(), unlit ? NeoForgeRenderTypes.ITEM_UNSORTED_UNLIT_TRANSLUCENT.get() : NeoForgeRenderTypes.ITEM_UNSORTED_TRANSLUCENT.get()
      );
   }

   public DynamicFluidContainerModel withFluid(Fluid newFluid) {
      return new DynamicFluidContainerModel(newFluid, this.flipGas, this.coverIsMask, this.applyFluidLuminosity);
   }

   @Override
   public BakedModel bake(
      IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides
   ) {
      Material particleLocation = context.hasMaterial("particle") ? context.getMaterial("particle") : null;
      Material baseLocation = context.hasMaterial("base") ? context.getMaterial("base") : null;
      Material fluidMaskLocation = context.hasMaterial("fluid") ? context.getMaterial("fluid") : null;
      Material coverLocation = context.hasMaterial("cover") ? context.getMaterial("cover") : null;
      TextureAtlasSprite baseSprite = baseLocation != null ? spriteGetter.apply(baseLocation) : null;
      TextureAtlasSprite fluidSprite = this.fluid != Fluids.EMPTY
         ? spriteGetter.apply(ClientHooks.getBlockMaterial(IClientFluidTypeExtensions.of(this.fluid).getStillTexture()))
         : null;
      TextureAtlasSprite coverSprite = coverLocation == null || this.coverIsMask && baseLocation == null ? null : spriteGetter.apply(coverLocation);
      TextureAtlasSprite particleSprite = particleLocation != null ? spriteGetter.apply(particleLocation) : null;
      if (particleSprite == null) {
         particleSprite = fluidSprite;
      }

      if (particleSprite == null) {
         particleSprite = baseSprite;
      }

      if (particleSprite == null && !this.coverIsMask) {
         particleSprite = coverSprite;
      }

      if (this.flipGas && this.fluid != Fluids.EMPTY && this.fluid.getFluidType().isLighterThanAir()) {
         modelState = new SimpleModelState(modelState.getRotation().compose(new Transformation(null, new Quaternionf(0.0F, 0.0F, 1.0F, 0.0F), null, null)));
      }

      StandaloneGeometryBakingContext itemContext = StandaloneGeometryBakingContext.builder(context)
         .withGui3d(false)
         .withUseBlockLight(false)
         .build(ResourceLocation.fromNamespaceAndPath("neoforge", "dynamic_fluid_container"));
      CompositeModel.Baked.Builder modelBuilder = CompositeModel.Baked.builder(
         itemContext,
         particleSprite,
         new DynamicFluidContainerModel.ContainedFluidOverrideHandler(overrides, baker, itemContext, this),
         context.getTransforms()
      );
      RenderTypeGroup normalRenderTypes = getLayerRenderTypes(false);
      if (baseLocation != null && baseSprite != null) {
         List<BlockElement> unbaked = UnbakedGeometryHelper.createUnbakedItemElements(0, baseSprite);
         List<BakedQuad> quads = UnbakedGeometryHelper.bakeElements(unbaked, $ -> baseSprite, modelState);
         modelBuilder.addQuads(normalRenderTypes, quads);
      }

      if (fluidMaskLocation != null && fluidSprite != null) {
         TextureAtlasSprite templateSprite = spriteGetter.apply(fluidMaskLocation);
         if (templateSprite != null) {
            SimpleModelState transformedState = new SimpleModelState(modelState.getRotation().compose(FLUID_TRANSFORM), modelState.isUvLocked());
            List<BlockElement> unbaked = UnbakedGeometryHelper.createUnbakedItemMaskElements(1, templateSprite);
            List<BakedQuad> quads = UnbakedGeometryHelper.bakeElements(unbaked, $ -> fluidSprite, transformedState);
            boolean emissive = this.applyFluidLuminosity && this.fluid.getFluidType().getLightLevel() > 0;
            RenderTypeGroup renderTypes = getLayerRenderTypes(emissive);
            if (emissive) {
               QuadTransformers.settingMaxEmissivity().processInPlace(quads);
            }

            modelBuilder.addQuads(renderTypes, quads);
         }
      }

      if (coverSprite != null) {
         TextureAtlasSprite sprite = this.coverIsMask ? baseSprite : coverSprite;
         if (sprite != null) {
            SimpleModelState transformedState = new SimpleModelState(modelState.getRotation().compose(COVER_TRANSFORM), modelState.isUvLocked());
            List<BlockElement> unbaked = UnbakedGeometryHelper.createUnbakedItemMaskElements(2, coverSprite);
            List<BakedQuad> quads = UnbakedGeometryHelper.bakeElements(unbaked, $ -> sprite, transformedState);
            modelBuilder.addQuads(normalRenderTypes, quads);
         }
      }

      modelBuilder.setParticle(particleSprite);
      return modelBuilder.build();
   }

   public static class Colors implements ItemColor {
      @Override
      public int getColor(ItemStack stack, int tintIndex) {
         return tintIndex != 1
            ? -1
            : FluidUtil.getFluidContained(stack).map(fluidStack -> IClientFluidTypeExtensions.of(fluidStack.getFluid()).getTintColor(fluidStack)).orElse(-1);
      }
   }

   private static final class ContainedFluidOverrideHandler extends ItemOverrides {
      private final Map<String, BakedModel> cache = Maps.newHashMap();
      private final ItemOverrides nested;
      private final ModelBaker baker;
      private final IGeometryBakingContext owner;
      private final DynamicFluidContainerModel parent;

      private ContainedFluidOverrideHandler(ItemOverrides nested, ModelBaker baker, IGeometryBakingContext owner, DynamicFluidContainerModel parent) {
         this.nested = nested;
         this.baker = baker;
         this.owner = owner;
         this.parent = parent;
      }

      @Override
      public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
         BakedModel overridden = this.nested.resolve(originalModel, stack, level, entity, seed);
         return overridden != originalModel ? overridden : FluidUtil.getFluidContained(stack).map(fluidStack -> {
            Fluid fluid = fluidStack.getFluid();
            String name = BuiltInRegistries.FLUID.getKey(fluid).toString();
            if (!this.cache.containsKey(name)) {
               DynamicFluidContainerModel unbaked = this.parent.withFluid(fluid);
               BakedModel bakedModel = unbaked.bake(this.owner, this.baker, Material::sprite, BlockModelRotation.X0_Y0, this);
               this.cache.put(name, bakedModel);
               return bakedModel;
            } else {
               return this.cache.get(name);
            }
         }).orElse(originalModel);
      }
   }

   public static final class Loader implements IGeometryLoader<DynamicFluidContainerModel> {
      public static final DynamicFluidContainerModel.Loader INSTANCE = new DynamicFluidContainerModel.Loader();

      private Loader() {
      }

      public DynamicFluidContainerModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
         if (!jsonObject.has("fluid")) {
            throw new RuntimeException("Bucket model requires 'fluid' value.");
         } else {
            ResourceLocation fluidName = ResourceLocation.parse(jsonObject.get("fluid").getAsString());
            Fluid fluid = BuiltInRegistries.FLUID.get(fluidName);
            boolean flip = GsonHelper.getAsBoolean(jsonObject, "flip_gas", false);
            boolean coverIsMask = GsonHelper.getAsBoolean(jsonObject, "cover_is_mask", true);
            boolean applyFluidLuminosity = GsonHelper.getAsBoolean(jsonObject, "apply_fluid_luminosity", true);
            return new DynamicFluidContainerModel(fluid, flip, coverIsMask, applyFluidLuminosity);
         }
      }
   }
}
