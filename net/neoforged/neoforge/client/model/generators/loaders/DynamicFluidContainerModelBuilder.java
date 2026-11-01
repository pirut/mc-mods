package net.neoforged.neoforge.client.model.generators.loaders;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.model.generators.CustomLoaderBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class DynamicFluidContainerModelBuilder<T extends ModelBuilder<T>> extends CustomLoaderBuilder<T> {
   private ResourceLocation fluid;
   private Boolean flipGas;
   private Boolean applyTint;
   private Boolean coverIsMask;
   private Boolean applyFluidLuminosity;

   public static <T extends ModelBuilder<T>> DynamicFluidContainerModelBuilder<T> begin(T parent, ExistingFileHelper existingFileHelper) {
      return new DynamicFluidContainerModelBuilder<>(parent, existingFileHelper);
   }

   protected DynamicFluidContainerModelBuilder(T parent, ExistingFileHelper existingFileHelper) {
      super(ResourceLocation.fromNamespaceAndPath("neoforge", "fluid_container"), parent, existingFileHelper, false);
   }

   public DynamicFluidContainerModelBuilder<T> fluid(Fluid fluid) {
      Preconditions.checkNotNull(fluid, "fluid must not be null");
      this.fluid = BuiltInRegistries.FLUID.getKey(fluid);
      return this;
   }

   public DynamicFluidContainerModelBuilder<T> flipGas(boolean flip) {
      this.flipGas = flip;
      return this;
   }

   public DynamicFluidContainerModelBuilder<T> applyTint(boolean tint) {
      this.applyTint = tint;
      return this;
   }

   public DynamicFluidContainerModelBuilder<T> coverIsMask(boolean coverIsMask) {
      this.coverIsMask = coverIsMask;
      return this;
   }

   public DynamicFluidContainerModelBuilder<T> applyFluidLuminosity(boolean applyFluidLuminosity) {
      this.applyFluidLuminosity = applyFluidLuminosity;
      return this;
   }

   @Override
   public JsonObject toJson(JsonObject json) {
      json = super.toJson(json);
      Preconditions.checkNotNull(this.fluid, "fluid must not be null");
      json.addProperty("fluid", this.fluid.toString());
      if (this.flipGas != null) {
         json.addProperty("flip_gas", this.flipGas);
      }

      if (this.applyTint != null) {
         json.addProperty("apply_tint", this.applyTint);
      }

      if (this.coverIsMask != null) {
         json.addProperty("cover_is_mask", this.coverIsMask);
      }

      if (this.applyFluidLuminosity != null) {
         json.addProperty("apply_fluid_luminosity", this.applyFluidLuminosity);
      }

      return json;
   }
}
