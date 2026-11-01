package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.EnumSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.EndPortalRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractEndPortalRenderer<T extends TheEndPortalBlockEntity, S extends EndPortalRenderState> implements BlockEntityRenderer<T, S> {
   public static final Identifier END_SKY_LOCATION = Identifier.withDefaultNamespace("textures/environment/end_sky.png");
   public static final Identifier END_PORTAL_LOCATION = Identifier.withDefaultNamespace("textures/entity/end_portal.png");

   public void extractRenderState(T p_446233_, S p_446143_, float p_447080_, Vec3 p_445461_, @Nullable ModelFeatureRenderer.CrumblingOverlay p_445911_) {
      BlockEntityRenderer.super.extractRenderState(p_446233_, p_446143_, p_447080_, p_445461_, p_445911_);
      p_446143_.facesToShow.clear();

      for (Direction direction : Direction.values()) {
         if (p_446233_.shouldRenderFace(direction)) {
            p_446143_.facesToShow.add(direction);
         }
      }
   }

   public void submit(S p_446622_, PoseStack p_446303_, SubmitNodeCollector p_447279_, CameraRenderState p_451548_) {
      p_447279_.submitCustomGeometry(
         p_446303_, this.renderType(), (p_446067_, p_445990_) -> this.renderCube(p_446622_.facesToShow, p_446067_.pose(), p_445990_)
      );
   }

   private void renderCube(EnumSet<Direction> p_447096_, Matrix4f p_446366_, VertexConsumer p_445456_) {
      float f = this.getOffsetDown();
      float f1 = this.getOffsetUp();
      this.renderFace(p_447096_, p_446366_, p_445456_, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, Direction.SOUTH);
      this.renderFace(p_447096_, p_446366_, p_445456_, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, Direction.NORTH);
      this.renderFace(p_447096_, p_446366_, p_445456_, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, Direction.EAST);
      this.renderFace(p_447096_, p_446366_, p_445456_, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, Direction.WEST);
      this.renderFace(p_447096_, p_446366_, p_445456_, 0.0F, 1.0F, f, f, 0.0F, 0.0F, 1.0F, 1.0F, Direction.DOWN);
      this.renderFace(p_447096_, p_446366_, p_445456_, 0.0F, 1.0F, f1, f1, 1.0F, 1.0F, 0.0F, 0.0F, Direction.UP);
   }

   private void renderFace(
      EnumSet<Direction> p_445895_,
      Matrix4f p_446601_,
      VertexConsumer p_445861_,
      float p_445518_,
      float p_445398_,
      float p_446610_,
      float p_446422_,
      float p_446126_,
      float p_447222_,
      float p_446048_,
      float p_447075_,
      Direction p_446642_
   ) {
      if (p_445895_.contains(p_446642_)) {
         p_445861_.addVertex(p_446601_, p_445518_, p_446610_, p_446126_);
         p_445861_.addVertex(p_446601_, p_445398_, p_446610_, p_447222_);
         p_445861_.addVertex(p_446601_, p_445398_, p_446422_, p_446048_);
         p_445861_.addVertex(p_446601_, p_445518_, p_446422_, p_447075_);
      }
   }

   protected float getOffsetUp() {
      return 0.75F;
   }

   protected float getOffsetDown() {
      return 0.375F;
   }

   protected RenderType renderType() {
      return RenderTypes.endPortal();
   }
}
