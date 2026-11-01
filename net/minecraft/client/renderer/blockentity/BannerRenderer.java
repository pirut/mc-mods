package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Consumer;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BannerRenderer implements BlockEntityRenderer<BannerBlockEntity, BannerRenderState> {
   private static final int MAX_PATTERNS = 16;
   private static final float SIZE = 0.6666667F;
   private final MaterialSet materials;
   private final BannerModel standingModel;
   private final BannerModel wallModel;
   private final BannerFlagModel standingFlagModel;
   private final BannerFlagModel wallFlagModel;

   public BannerRenderer(BlockEntityRendererProvider.Context p_173521_) {
      this(p_173521_.entityModelSet(), p_173521_.materials());
   }

   public BannerRenderer(SpecialModelRenderer.BakingContext p_435832_) {
      this(p_435832_.entityModelSet(), p_435832_.materials());
   }

   public BannerRenderer(EntityModelSet p_433084_, MaterialSet p_433697_) {
      this.materials = p_433697_;
      this.standingModel = new BannerModel(p_433084_.bakeLayer(ModelLayers.STANDING_BANNER));
      this.wallModel = new BannerModel(p_433084_.bakeLayer(ModelLayers.WALL_BANNER));
      this.standingFlagModel = new BannerFlagModel(p_433084_.bakeLayer(ModelLayers.STANDING_BANNER_FLAG));
      this.wallFlagModel = new BannerFlagModel(p_433084_.bakeLayer(ModelLayers.WALL_BANNER_FLAG));
   }

   public BannerRenderState createRenderState() {
      return new BannerRenderState();
   }

   public void extractRenderState(
      BannerBlockEntity p_446844_, BannerRenderState p_447143_, float p_446794_, Vec3 p_447357_, @Nullable ModelFeatureRenderer.CrumblingOverlay p_446651_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_446844_, p_447143_, p_446794_, p_447357_, p_446651_);
      p_447143_.baseColor = p_446844_.getBaseColor();
      p_447143_.patterns = p_446844_.getPatterns();
      BlockState blockstate = p_446844_.getBlockState();
      if (blockstate.getBlock() instanceof BannerBlock) {
         p_447143_.angle = -RotationSegment.convertToDegrees(blockstate.getValue(BannerBlock.ROTATION));
         p_447143_.standing = true;
      } else {
         p_447143_.angle = -blockstate.getValue(WallBannerBlock.FACING).toYRot();
         p_447143_.standing = false;
      }

      long i = p_446844_.getLevel() != null ? p_446844_.getLevel().getGameTime() : 0L;
      BlockPos blockpos = p_446844_.getBlockPos();
      p_447143_.phase = ((float)Math.floorMod(blockpos.getX() * 7 + blockpos.getY() * 9 + blockpos.getZ() * 13 + i, 100L) + p_446794_) / 100.0F;
   }

   public void submit(BannerRenderState p_445505_, PoseStack p_439672_, SubmitNodeCollector p_439893_, CameraRenderState p_451262_) {
      BannerModel bannermodel;
      BannerFlagModel bannerflagmodel;
      if (p_445505_.standing) {
         bannermodel = this.standingModel;
         bannerflagmodel = this.standingFlagModel;
      } else {
         bannermodel = this.wallModel;
         bannerflagmodel = this.wallFlagModel;
      }

      submitBanner(
         this.materials,
         p_439672_,
         p_439893_,
         p_445505_.lightCoords,
         OverlayTexture.NO_OVERLAY,
         p_445505_.angle,
         bannermodel,
         bannerflagmodel,
         p_445505_.phase,
         p_445505_.baseColor,
         p_445505_.patterns,
         p_445505_.breakProgress,
         0
      );
   }

   public void submitSpecial(
      PoseStack p_440281_, SubmitNodeCollector p_440300_, int p_439135_, int p_439629_, DyeColor p_440022_, BannerPatternLayers p_438875_, int p_451683_
   ) {
      submitBanner(
         this.materials,
         p_440281_,
         p_440300_,
         p_439135_,
         p_439629_,
         0.0F,
         this.standingModel,
         this.standingFlagModel,
         0.0F,
         p_440022_,
         p_438875_,
         null,
         p_451683_
      );
   }

   private static void submitBanner(
      MaterialSet p_439579_,
      PoseStack p_439517_,
      SubmitNodeCollector p_439053_,
      int p_440110_,
      int p_439479_,
      float p_439639_,
      BannerModel p_480239_,
      BannerFlagModel p_481682_,
      float p_440294_,
      DyeColor p_439197_,
      BannerPatternLayers p_440310_,
      @Nullable ModelFeatureRenderer.CrumblingOverlay p_440357_,
      int p_451702_
   ) {
      p_439517_.pushPose();
      p_439517_.translate(0.5F, 0.0F, 0.5F);
      p_439517_.mulPose(Axis.YP.rotationDegrees(p_439639_));
      p_439517_.scale(0.6666667F, -0.6666667F, -0.6666667F);
      Material material = ModelBakery.BANNER_BASE;
      p_439053_.submitModel(
         p_480239_,
         Unit.INSTANCE,
         p_439517_,
         material.renderType(RenderTypes::entitySolid),
         p_440110_,
         p_439479_,
         -1,
         p_439579_.get(material),
         p_451702_,
         p_440357_
      );
      submitPatterns(
         p_439579_, p_439517_, p_439053_, p_440110_, p_439479_, p_481682_, p_440294_, material, true, p_439197_, p_440310_, false, p_440357_, p_451702_
      );
      p_439517_.popPose();
   }

   public static <S> void submitPatterns(
      MaterialSet p_440183_,
      PoseStack p_440515_,
      SubmitNodeCollector p_439454_,
      int p_440371_,
      int p_440649_,
      Model<S> p_449908_,
      S p_449696_,
      Material p_439637_,
      boolean p_439954_,
      DyeColor p_439618_,
      BannerPatternLayers p_439876_,
      boolean p_451708_,
      @Nullable ModelFeatureRenderer.CrumblingOverlay p_442600_,
      int p_451682_
   ) {
      p_439454_.submitModel(
         p_449908_,
         p_449696_,
         p_440515_,
         p_439637_.renderType(RenderTypes::entitySolid),
         p_440371_,
         p_440649_,
         -1,
         p_440183_.get(p_439637_),
         p_451682_,
         p_442600_
      );
      if (p_451708_) {
         p_439454_.submitModel(p_449908_, p_449696_, p_440515_, RenderTypes.entityGlint(), p_440371_, p_440649_, -1, p_440183_.get(p_439637_), 0, p_442600_);
      }

      submitPatternLayer(
         p_440183_, p_440515_, p_439454_, p_440371_, p_440649_, p_449908_, p_449696_, p_439954_ ? Sheets.BANNER_BASE : Sheets.SHIELD_BASE, p_439618_, p_442600_
      );

      for (int i = 0; i < 16 && i < p_439876_.layers().size(); i++) {
         BannerPatternLayers.Layer bannerpatternlayers$layer = p_439876_.layers().get(i);
         Material material = p_439954_
            ? Sheets.getBannerMaterial(bannerpatternlayers$layer.pattern())
            : Sheets.getShieldMaterial(bannerpatternlayers$layer.pattern());
         submitPatternLayer(p_440183_, p_440515_, p_439454_, p_440371_, p_440649_, p_449908_, p_449696_, material, bannerpatternlayers$layer.color(), null);
      }
   }

   private static <S> void submitPatternLayer(
      MaterialSet p_439333_,
      PoseStack p_439424_,
      SubmitNodeCollector p_439806_,
      int p_439519_,
      int p_439952_,
      Model<S> p_449088_,
      S p_449737_,
      Material p_440466_,
      DyeColor p_440408_,
      @Nullable ModelFeatureRenderer.CrumblingOverlay p_443564_
   ) {
      int i = p_440408_.getTextureDiffuseColor();
      p_439806_.submitModel(
         p_449088_, p_449737_, p_439424_, p_440466_.renderType(RenderTypes::entityNoOutline), p_439519_, p_439952_, i, p_439333_.get(p_440466_), 0, p_443564_
      );
   }

   public void getExtents(Consumer<Vector3fc> p_470575_) {
      PoseStack posestack = new PoseStack();
      posestack.translate(0.5F, 0.0F, 0.5F);
      posestack.scale(0.6666667F, -0.6666667F, -0.6666667F);
      this.standingModel.root().getExtentsForGui(posestack, p_470575_);
      this.standingFlagModel.setupAnim(0.0F);
      this.standingFlagModel.root().getExtentsForGui(posestack, p_470575_);
   }

   public AABB getRenderBoundingBox(BannerBlockEntity blockEntity) {
      BlockPos pos = blockEntity.getBlockPos();
      boolean standing = blockEntity.getBlockState().getBlock() instanceof BannerBlock;
      return AABB.encapsulatingFullBlocks(pos, standing ? pos.above() : pos.below());
   }
}
