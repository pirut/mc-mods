package net.neoforged.neoforge.client.extensions.common;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

public interface IClientFluidTypeExtensions {
   IClientFluidTypeExtensions DEFAULT = new IClientFluidTypeExtensions() {};

   static IClientFluidTypeExtensions of(FluidState state) {
      return of(state.getFluidType());
   }

   static IClientFluidTypeExtensions of(Fluid fluid) {
      return of(fluid.getFluidType());
   }

   static IClientFluidTypeExtensions of(FluidType type) {
      return ClientExtensionsManager.FLUID_TYPE_EXTENSIONS.getOrDefault(type, DEFAULT);
   }

   default int getTintColor() {
      return -1;
   }

   default Identifier getStillTexture() {
      return MissingTextureAtlasSprite.getLocation();
   }

   default Identifier getFlowingTexture() {
      return MissingTextureAtlasSprite.getLocation();
   }

   @Nullable
   default Identifier getOverlayTexture() {
      return null;
   }

   @Nullable
   default Identifier getRenderOverlayTexture(Minecraft mc) {
      return null;
   }

   default void renderOverlay(Minecraft mc, PoseStack poseStack, MultiBufferSource buffers) {
      Identifier texture = this.getRenderOverlayTexture(mc);
      if (texture != null) {
         ScreenEffectRenderer.renderFluid(mc, poseStack, buffers, texture);
      }
   }

   default Vector4f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector4f fluidFogColor) {
      return fluidFogColor;
   }

   default void modifyFogRender(Camera camera, @Nullable FogEnvironment environment, float renderDistance, float partialTick, FogData fogData) {
   }

   default Identifier getStillTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
      return this.getStillTexture();
   }

   default Identifier getFlowingTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
      return this.getFlowingTexture();
   }

   default Identifier getOverlayTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
      return this.getOverlayTexture();
   }

   default int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
      return this.getTintColor();
   }

   default int getTintColor(FluidStack stack) {
      return this.getTintColor();
   }

   default Identifier getStillTexture(FluidStack stack) {
      return this.getStillTexture();
   }

   default Identifier getFlowingTexture(FluidStack stack) {
      return this.getFlowingTexture();
   }

   default Identifier getOverlayTexture(FluidStack stack) {
      return this.getOverlayTexture();
   }

   default boolean renderFluid(FluidState fluidState, BlockAndTintGetter getter, BlockPos pos, VertexConsumer vertexConsumer, BlockState blockState) {
      return false;
   }
}
