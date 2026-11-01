package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BeaconRenderer<T extends BlockEntity & BeaconBeamOwner> implements BlockEntityRenderer<T, BeaconRenderState> {
   public static final Identifier BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/beacon_beam.png");
   public static final int MAX_RENDER_Y = 2048;
   private static final float BEAM_SCALE_THRESHOLD = 96.0F;
   public static final float SOLID_BEAM_RADIUS = 0.2F;
   public static final float BEAM_GLOW_RADIUS = 0.25F;

   public BeaconRenderState createRenderState() {
      return new BeaconRenderState();
   }

   public void extractRenderState(
      T p_447130_, BeaconRenderState p_446076_, float p_445698_, Vec3 p_445714_, @Nullable ModelFeatureRenderer.CrumblingOverlay p_447097_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_447130_, p_446076_, p_445698_, p_445714_, p_447097_);
      extract(p_447130_, p_446076_, p_445698_, p_445714_);
   }

   public static <T extends BlockEntity & BeaconBeamOwner> void extract(T p_447269_, BeaconRenderState p_445501_, float p_445917_, Vec3 p_446712_) {
      p_445501_.animationTime = p_447269_.getLevel() != null ? Math.floorMod(p_447269_.getLevel().getGameTime(), 40) + p_445917_ : 0.0F;
      p_445501_.sections = p_447269_.getBeamSections()
         .stream()
         .map(p_445220_ -> new BeaconRenderState.Section(p_445220_.getColor(), p_445220_.getHeight()))
         .toList();
      float f = (float)p_446712_.subtract(p_445501_.blockPos.getCenter()).horizontalDistance();
      LocalPlayer localplayer = Minecraft.getInstance().player;
      p_445501_.beamRadiusScale = localplayer != null && localplayer.isScoping() ? 1.0F : Math.max(1.0F, f / 96.0F);
   }

   public void submit(BeaconRenderState p_447169_, PoseStack p_440263_, SubmitNodeCollector p_439020_, CameraRenderState p_450986_) {
      int i = 0;

      for (int j = 0; j < p_447169_.sections.size(); j++) {
         BeaconRenderState.Section beaconrenderstate$section = p_447169_.sections.get(j);
         submitBeaconBeam(
            p_440263_,
            p_439020_,
            p_447169_.beamRadiusScale,
            p_447169_.animationTime,
            i,
            j == p_447169_.sections.size() - 1 ? 2048 : beaconrenderstate$section.height(),
            beaconrenderstate$section.color()
         );
         i += beaconrenderstate$section.height();
      }
   }

   private static void submitBeaconBeam(
      PoseStack p_439706_, SubmitNodeCollector p_440494_, float p_440687_, float p_439936_, int p_440035_, int p_438971_, int p_439741_
   ) {
      submitBeaconBeam(p_439706_, p_440494_, BEAM_LOCATION, 1.0F, p_439936_, p_440035_, p_438971_, p_439741_, 0.2F * p_440687_, 0.25F * p_440687_);
   }

   public static void submitBeaconBeam(
      PoseStack p_439608_,
      SubmitNodeCollector p_439393_,
      Identifier p_469462_,
      float p_439343_,
      float p_440072_,
      int p_439067_,
      int p_440009_,
      int p_440282_,
      float p_446644_,
      float p_445495_
   ) {
      int i = p_439067_ + p_440009_;
      p_439608_.pushPose();
      p_439608_.translate(0.5, 0.0, 0.5);
      float f = p_440009_ < 0 ? p_440072_ : -p_440072_;
      float f1 = Mth.frac(f * 0.2F - Mth.floor(f * 0.1F));
      p_439608_.pushPose();
      p_439608_.mulPose(Axis.YP.rotationDegrees(p_440072_ * 2.25F - 45.0F));
      float f2 = 0.0F;
      float f4 = 0.0F;
      float f5 = -p_446644_;
      float f6 = 0.0F;
      float f7 = 0.0F;
      float f8 = -p_446644_;
      float f9 = 0.0F;
      float f10 = 1.0F;
      float f11 = -1.0F + f1;
      float f12 = p_440009_ * p_439343_ * (0.5F / p_446644_) + f11;
      p_439393_.submitCustomGeometry(
         p_439608_,
         RenderTypes.beaconBeam(p_469462_, false),
         (p_440236_, p_440655_) -> renderPart(
            p_440236_, p_440655_, p_440282_, p_439067_, i, 0.0F, p_446644_, p_446644_, 0.0F, f5, 0.0F, 0.0F, f8, 0.0F, 1.0F, f12, f11
         )
      );
      p_439608_.popPose();
      float f2_f = -p_445495_;
      float f3 = -p_445495_;
      float f4_f = -p_445495_;
      float f5_f = -p_445495_;
      f9 = 0.0F;
      f10 = 1.0F;
      float f11_f = -1.0F + f1;
      float f12_f = p_440009_ * p_439343_ + f11;
      p_439393_.submitCustomGeometry(
         p_439608_,
         RenderTypes.beaconBeam(p_469462_, true),
         (p_440366_, p_440684_) -> renderPart(
            p_440366_,
            p_440684_,
            ARGB.color(32, p_440282_),
            p_439067_,
            i,
            f2_f,
            f3,
            p_445495_,
            f4_f,
            f5_f,
            p_445495_,
            p_445495_,
            p_445495_,
            0.0F,
            1.0F,
            f12_f,
            f11_f
         )
      );
      p_439608_.popPose();
   }

   private static void renderPart(
      PoseStack.Pose p_438950_,
      VertexConsumer p_112157_,
      int p_112162_,
      int p_112163_,
      int p_351014_,
      float p_112158_,
      float p_112159_,
      float p_112160_,
      float p_112161_,
      float p_112164_,
      float p_112165_,
      float p_112166_,
      float p_112167_,
      float p_112168_,
      float p_112169_,
      float p_112170_,
      float p_112171_
   ) {
      renderQuad(p_438950_, p_112157_, p_112162_, p_112163_, p_351014_, p_112158_, p_112159_, p_112160_, p_112161_, p_112168_, p_112169_, p_112170_, p_112171_);
      renderQuad(p_438950_, p_112157_, p_112162_, p_112163_, p_351014_, p_112166_, p_112167_, p_112164_, p_112165_, p_112168_, p_112169_, p_112170_, p_112171_);
      renderQuad(p_438950_, p_112157_, p_112162_, p_112163_, p_351014_, p_112160_, p_112161_, p_112166_, p_112167_, p_112168_, p_112169_, p_112170_, p_112171_);
      renderQuad(p_438950_, p_112157_, p_112162_, p_112163_, p_351014_, p_112164_, p_112165_, p_112158_, p_112159_, p_112168_, p_112169_, p_112170_, p_112171_);
   }

   private static void renderQuad(
      PoseStack.Pose p_323955_,
      VertexConsumer p_112122_,
      int p_112127_,
      int p_112128_,
      int p_350566_,
      float p_112123_,
      float p_112124_,
      float p_112125_,
      float p_112126_,
      float p_112129_,
      float p_112130_,
      float p_112131_,
      float p_112132_
   ) {
      addVertex(p_323955_, p_112122_, p_112127_, p_350566_, p_112123_, p_112124_, p_112130_, p_112131_);
      addVertex(p_323955_, p_112122_, p_112127_, p_112128_, p_112123_, p_112124_, p_112130_, p_112132_);
      addVertex(p_323955_, p_112122_, p_112127_, p_112128_, p_112125_, p_112126_, p_112129_, p_112132_);
      addVertex(p_323955_, p_112122_, p_112127_, p_350566_, p_112125_, p_112126_, p_112129_, p_112131_);
   }

   private static void addVertex(
      PoseStack.Pose p_324495_, VertexConsumer p_253894_, int p_254357_, int p_350652_, float p_253871_, float p_253841_, float p_254568_, float p_254361_
   ) {
      p_253894_.addVertex(p_324495_, p_253871_, (float)p_350652_, p_253841_)
         .setColor(p_254357_)
         .setUv(p_254568_, p_254361_)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(p_324495_, 0.0F, 1.0F, 0.0F);
   }

   @Override
   public boolean shouldRenderOffScreen() {
      return true;
   }

   @Override
   public int getViewDistance() {
      return Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
   }

   @Override
   public boolean shouldRender(T p_173534_, Vec3 p_173535_) {
      return Vec3.atCenterOf(p_173534_.getBlockPos()).multiply(1.0, 0.0, 1.0).closerThan(p_173535_.multiply(1.0, 0.0, 1.0), this.getViewDistance());
   }

   @Override
   public AABB getRenderBoundingBox(T blockEntity) {
      BlockPos pos = blockEntity.getBlockPos();
      return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, 2048.0, pos.getZ() + 1.0);
   }
}
