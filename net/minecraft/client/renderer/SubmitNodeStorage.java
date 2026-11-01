package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class SubmitNodeStorage implements SubmitNodeCollector {
   private final Int2ObjectAVLTreeMap<SubmitNodeCollection> submitsPerOrder = new Int2ObjectAVLTreeMap();

   public SubmitNodeCollection order(int i) {
      return (SubmitNodeCollection)this.submitsPerOrder.computeIfAbsent(i, ix -> new SubmitNodeCollection(this));
   }

   @Override
   public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
      this.order(0).submitShadow(poseStack, radius, pieces);
   }

   @Override
   public void submitNameTag(
      PoseStack poseStack,
      @Nullable Vec3 pos,
      int yOffset,
      Component text,
      boolean seethrough,
      int packedLight,
      double distanceToCameraSq,
      CameraRenderState cameraRenderState
   ) {
      this.order(0).submitNameTag(poseStack, pos, yOffset, text, seethrough, packedLight, distanceToCameraSq, cameraRenderState);
   }

   @Override
   public void submitText(
      PoseStack poseStack,
      float x,
      float y,
      FormattedCharSequence string,
      boolean dropShadow,
      Font.DisplayMode displayMode,
      int packedLight,
      int color,
      int backgroundColor,
      int outlineColor
   ) {
      this.order(0).submitText(poseStack, x, y, string, dropShadow, displayMode, packedLight, color, backgroundColor, outlineColor);
   }

   @Override
   public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
      this.order(0).submitFlame(poseStack, renderState, rotation);
   }

   @Override
   public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
      this.order(0).submitLeash(poseStack, leashState);
   }

   @Override
   public <S> void submitModel(
      Model<? super S> model,
      S renderState,
      PoseStack poseStack,
      RenderType renderType,
      int packedLight,
      int packedOverlay,
      int tintColor,
      @Nullable TextureAtlasSprite sprite,
      int outlineColor,
      @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
   ) {
      this.order(0).submitModel(model, renderState, poseStack, renderType, packedLight, packedOverlay, tintColor, sprite, outlineColor, crumblingOverlay);
   }

   @Override
   public void submitModelPart(
      ModelPart modelPart,
      PoseStack poseStack,
      RenderType renderType,
      int packedLight,
      int packedOverlay,
      @Nullable TextureAtlasSprite sprite,
      boolean sheeted,
      boolean hasFoil,
      int tintColor,
      @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
      int outlineColor
   ) {
      this.order(0)
         .submitModelPart(modelPart, poseStack, renderType, packedLight, packedOverlay, sprite, sheeted, hasFoil, tintColor, crumblingOverlay, outlineColor);
   }

   @Override
   public void submitBlock(PoseStack poseStack, BlockState blockState, int packedLight, int packedOverlay, int outlineColor) {
      this.order(0).submitBlock(poseStack, blockState, packedLight, packedOverlay, outlineColor);
   }

   @Override
   public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState renderState) {
      this.order(0).submitMovingBlock(poseStack, renderState);
   }

   @Override
   public void submitBlockModel(
      PoseStack poseStack, RenderType renderType, BlockStateModel model, float r, float g, float b, int packedLight, int packedOverlay, int outlineColor
   ) {
      this.order(0).submitBlockModel(poseStack, renderType, model, r, g, b, packedLight, packedOverlay, outlineColor);
   }

   @Override
   public void submitItem(
      PoseStack poseStack,
      ItemDisplayContext displayContext,
      int packedLight,
      int packedOverlay,
      int outlineColor,
      int[] tintLayers,
      List<BakedQuad> quads,
      RenderType renderType,
      ItemStackRenderState.FoilType foilType
   ) {
      this.order(0).submitItem(poseStack, displayContext, packedLight, packedOverlay, outlineColor, tintLayers, quads, renderType, foilType);
   }

   @Override
   public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
      this.order(0).submitCustomGeometry(poseStack, renderType, renderer);
   }

   @Override
   public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer renderer) {
      this.order(0).submitParticleGroup(renderer);
   }

   public void clear() {
      this.submitsPerOrder.values().forEach(SubmitNodeCollection::clear);
   }

   public void endFrame() {
      this.submitsPerOrder.values().removeIf(submitNodeCollection -> !submitNodeCollection.wasUsed());
      this.submitsPerOrder.values().forEach(SubmitNodeCollection::endFrame);
   }

   public Int2ObjectAVLTreeMap<SubmitNodeCollection> getSubmitsPerOrder() {
      return this.submitsPerOrder;
   }

   @Environment(EnvType.CLIENT)
   public record BlockModelSubmit(
      PoseStack.Pose pose, RenderType renderType, BlockStateModel model, float r, float g, float b, int lightCoords, int overlayCoords, int outlineColor
   ) {
   }

   @Environment(EnvType.CLIENT)
   public record BlockSubmit(PoseStack.Pose pose, BlockState state, int lightCoords, int overlayCoords, int outlineColor) {
   }

   @Environment(EnvType.CLIENT)
   public record CustomGeometrySubmit(PoseStack.Pose pose, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
   }

   @Environment(EnvType.CLIENT)
   public record FlameSubmit(PoseStack.Pose pose, EntityRenderState entityRenderState, Quaternionf rotation) {
   }

   @Environment(EnvType.CLIENT)
   public record ItemSubmit(
      PoseStack.Pose pose,
      ItemDisplayContext displayContext,
      int lightCoords,
      int overlayCoords,
      int outlineColor,
      int[] tintLayers,
      List<BakedQuad> quads,
      RenderType renderType,
      ItemStackRenderState.FoilType foilType
   ) {
   }

   @Environment(EnvType.CLIENT)
   public record LeashSubmit(Matrix4f pose, EntityRenderState.LeashState leashState) {
   }

   @Environment(EnvType.CLIENT)
   public record ModelPartSubmit(
      PoseStack.Pose pose,
      ModelPart modelPart,
      int lightCoords,
      int overlayCoords,
      @Nullable TextureAtlasSprite sprite,
      boolean sheeted,
      boolean hasFoil,
      int tintedColor,
      @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
      int outlineColor
   ) {
   }

   @Environment(EnvType.CLIENT)
   public record ModelSubmit<S>(
      PoseStack.Pose pose,
      Model<? super S> model,
      S state,
      int lightCoords,
      int overlayCoords,
      int tintedColor,
      @Nullable TextureAtlasSprite sprite,
      int outlineColor,
      @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
   ) {
   }

   @Environment(EnvType.CLIENT)
   public record MovingBlockSubmit(Matrix4f pose, MovingBlockRenderState movingBlockRenderState) {
   }

   @Environment(EnvType.CLIENT)
   public record NameTagSubmit(Matrix4f pose, float x, float y, Component text, int lightCoords, int color, int backgroundColor, double distanceToCameraSq) {
   }

   @Environment(EnvType.CLIENT)
   public record ShadowSubmit(Matrix4f pose, float radius, List<EntityRenderState.ShadowPiece> pieces) {
   }

   @Environment(EnvType.CLIENT)
   public record TextSubmit(
      Matrix4f pose,
      float x,
      float y,
      FormattedCharSequence string,
      boolean dropShadow,
      Font.DisplayMode displayMode,
      int lightCoords,
      int color,
      int backgroundColor,
      int outlineColor
   ) {
   }

   @Environment(EnvType.CLIENT)
   public record TranslucentModelSubmit<S>(SubmitNodeStorage.ModelSubmit<S> modelSubmit, RenderType renderType, Vector3f position) {
   }
}
