package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.model.object.banner.BannerModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Unit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class BannerRenderer implements BlockEntityRenderer<BannerBlockEntity, BannerRenderState> {
   private static final int MAX_PATTERNS = 16;
   private static final float SIZE = 0.6666667F;
   private final MaterialSet materials;
   private final BannerModel standingModel;
   private final BannerModel wallModel;
   private final BannerFlagModel standingFlagModel;
   private final BannerFlagModel wallFlagModel;

   public BannerRenderer(BlockEntityRendererProvider.Context context) {
      this(context.entityModelSet(), context.materials());
   }

   public BannerRenderer(SpecialModelRenderer.BakingContext context) {
      this(context.entityModelSet(), context.materials());
   }

   public BannerRenderer(EntityModelSet modelSet, MaterialSet materials) {
      this.materials = materials;
      this.standingModel = new BannerModel(modelSet.bakeLayer(ModelLayers.STANDING_BANNER));
      this.wallModel = new BannerModel(modelSet.bakeLayer(ModelLayers.WALL_BANNER));
      this.standingFlagModel = new BannerFlagModel(modelSet.bakeLayer(ModelLayers.STANDING_BANNER_FLAG));
      this.wallFlagModel = new BannerFlagModel(modelSet.bakeLayer(ModelLayers.WALL_BANNER_FLAG));
   }

   public BannerRenderState createRenderState() {
      return new BannerRenderState();
   }

   public void extractRenderState(
      BannerBlockEntity bannerBlockEntity,
      BannerRenderState bannerRenderState,
      float f,
      Vec3 vec3,
      @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
   ) {
      BlockEntityRenderer.super.extractRenderState(bannerBlockEntity, bannerRenderState, f, vec3, crumblingOverlay);
      bannerRenderState.baseColor = bannerBlockEntity.getBaseColor();
      bannerRenderState.patterns = bannerBlockEntity.getPatterns();
      BlockState blockState = bannerBlockEntity.getBlockState();
      if (blockState.getBlock() instanceof BannerBlock) {
         bannerRenderState.angle = -RotationSegment.convertToDegrees(blockState.getValue(BannerBlock.ROTATION));
         bannerRenderState.standing = true;
      } else {
         bannerRenderState.angle = -blockState.getValue(WallBannerBlock.FACING).toYRot();
         bannerRenderState.standing = false;
      }

      long l = bannerBlockEntity.getLevel() != null ? bannerBlockEntity.getLevel().getGameTime() : 0L;
      BlockPos blockPos = bannerBlockEntity.getBlockPos();
      bannerRenderState.phase = ((float)Math.floorMod(blockPos.getX() * 7 + blockPos.getY() * 9 + blockPos.getZ() * 13 + l, 100L) + f) / 100.0F;
   }

   public void submit(BannerRenderState bannerRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
      BannerModel bannerModel;
      BannerFlagModel bannerFlagModel;
      if (bannerRenderState.standing) {
         bannerModel = this.standingModel;
         bannerFlagModel = this.standingFlagModel;
      } else {
         bannerModel = this.wallModel;
         bannerFlagModel = this.wallFlagModel;
      }

      submitBanner(
         this.materials,
         poseStack,
         submitNodeCollector,
         bannerRenderState.lightCoords,
         OverlayTexture.NO_OVERLAY,
         bannerRenderState.angle,
         bannerModel,
         bannerFlagModel,
         bannerRenderState.phase,
         bannerRenderState.baseColor,
         bannerRenderState.patterns,
         bannerRenderState.breakProgress,
         0
      );
   }

   public void submitSpecial(
      PoseStack poseStack,
      SubmitNodeCollector nodeCollector,
      int packedLight,
      int packedOverlay,
      DyeColor baseColor,
      BannerPatternLayers patterns,
      int outlineColor
   ) {
      submitBanner(
         this.materials,
         poseStack,
         nodeCollector,
         packedLight,
         packedOverlay,
         0.0F,
         this.standingModel,
         this.standingFlagModel,
         0.0F,
         baseColor,
         patterns,
         null,
         outlineColor
      );
   }

   private static void submitBanner(
      MaterialSet materials,
      PoseStack poseStack,
      SubmitNodeCollector nodeCollector,
      int packedLight,
      int packedOverlay,
      float rotation,
      BannerModel standingModel,
      BannerFlagModel standingFlagModel,
      float phase,
      DyeColor baseColor,
      BannerPatternLayers patterns,
      @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress,
      int outlineColor
   ) {
      poseStack.pushPose();
      poseStack.translate(0.5F, 0.0F, 0.5F);
      poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
      poseStack.scale(0.6666667F, -0.6666667F, -0.6666667F);
      Material material = ModelBakery.BANNER_BASE;
      nodeCollector.submitModel(
         standingModel,
         Unit.INSTANCE,
         poseStack,
         material.renderType(RenderTypes::entitySolid),
         packedLight,
         packedOverlay,
         -1,
         materials.get(material),
         outlineColor,
         breakProgress
      );
      submitPatterns(
         materials,
         poseStack,
         nodeCollector,
         packedLight,
         packedOverlay,
         standingFlagModel,
         phase,
         material,
         true,
         baseColor,
         patterns,
         false,
         breakProgress,
         outlineColor
      );
      poseStack.popPose();
   }

   public static <S> void submitPatterns(
      MaterialSet materials,
      PoseStack poseStack,
      SubmitNodeCollector nodeCollector,
      int packedLight,
      int packedOverlay,
      Model<S> flag,
      S renderState,
      Material material,
      boolean banner,
      DyeColor baseColor,
      BannerPatternLayers patterns,
      boolean withGlint,
      @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
      int outlineColor
   ) {
      nodeCollector.submitModel(
         flag,
         renderState,
         poseStack,
         material.renderType(RenderTypes::entitySolid),
         packedLight,
         packedOverlay,
         -1,
         materials.get(material),
         outlineColor,
         crumblingOverlay
      );
      if (withGlint) {
         nodeCollector.submitModel(
            flag, renderState, poseStack, RenderTypes.entityGlint(), packedLight, packedOverlay, -1, materials.get(material), 0, crumblingOverlay
         );
      }

      submitPatternLayer(
         materials,
         poseStack,
         nodeCollector,
         packedLight,
         packedOverlay,
         flag,
         renderState,
         banner ? Sheets.BANNER_BASE : Sheets.SHIELD_BASE,
         baseColor,
         crumblingOverlay
      );

      for (int i = 0; i < 16 && i < patterns.layers().size(); i++) {
         BannerPatternLayers.Layer layer = patterns.layers().get(i);
         Material material2 = banner ? Sheets.getBannerMaterial(layer.pattern()) : Sheets.getShieldMaterial(layer.pattern());
         submitPatternLayer(materials, poseStack, nodeCollector, packedLight, packedOverlay, flag, renderState, material2, layer.color(), null);
      }
   }

   private static <S> void submitPatternLayer(
      MaterialSet materials,
      PoseStack poseStack,
      SubmitNodeCollector nodeCollector,
      int packedLight,
      int packedOverlay,
      Model<S> flagModel,
      S sway,
      Material material,
      DyeColor color,
      @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
   ) {
      int i = color.getTextureDiffuseColor();
      nodeCollector.submitModel(
         flagModel,
         sway,
         poseStack,
         material.renderType(RenderTypes::entityNoOutline),
         packedLight,
         packedOverlay,
         i,
         materials.get(material),
         0,
         crumblingOverlay
      );
   }

   public void getExtents(Consumer<Vector3fc> output) {
      PoseStack poseStack = new PoseStack();
      poseStack.translate(0.5F, 0.0F, 0.5F);
      poseStack.scale(0.6666667F, -0.6666667F, -0.6666667F);
      this.standingModel.root().getExtentsForGui(poseStack, output);
      this.standingFlagModel.setupAnim(0.0F);
      this.standingFlagModel.root().getExtentsForGui(poseStack, output);
   }
}
