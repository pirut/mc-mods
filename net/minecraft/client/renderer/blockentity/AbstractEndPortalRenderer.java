package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.EnumSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class AbstractEndPortalRenderer<T extends TheEndPortalBlockEntity, S extends EndPortalRenderState> implements BlockEntityRenderer<T, S> {
   public static final Identifier END_SKY_LOCATION = Identifier.withDefaultNamespace("textures/environment/end_sky.png");
   public static final Identifier END_PORTAL_LOCATION = Identifier.withDefaultNamespace("textures/entity/end_portal.png");

   public void extractRenderState(
      T theEndPortalBlockEntity, S endPortalRenderState, float f, Vec3 vec3, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
   ) {
      BlockEntityRenderer.super.extractRenderState(theEndPortalBlockEntity, endPortalRenderState, f, vec3, crumblingOverlay);
      endPortalRenderState.facesToShow.clear();

      for (Direction direction : Direction.values()) {
         if (theEndPortalBlockEntity.shouldRenderFace(direction)) {
            endPortalRenderState.facesToShow.add(direction);
         }
      }
   }

   public void submit(S endPortalRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
      submitNodeCollector.submitCustomGeometry(
         poseStack, this.renderType(), (pose, vertexConsumer) -> this.renderCube(endPortalRenderState.facesToShow, pose.pose(), vertexConsumer)
      );
   }

   private void renderCube(EnumSet<Direction> faces, Matrix4f pose, VertexConsumer consumer) {
      float f = this.getOffsetDown();
      float g = this.getOffsetUp();
      this.renderFace(faces, pose, consumer, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, Direction.SOUTH);
      this.renderFace(faces, pose, consumer, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, Direction.NORTH);
      this.renderFace(faces, pose, consumer, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, Direction.EAST);
      this.renderFace(faces, pose, consumer, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, Direction.WEST);
      this.renderFace(faces, pose, consumer, 0.0F, 1.0F, f, f, 0.0F, 0.0F, 1.0F, 1.0F, Direction.DOWN);
      this.renderFace(faces, pose, consumer, 0.0F, 1.0F, g, g, 1.0F, 1.0F, 0.0F, 0.0F, Direction.UP);
   }

   private void renderFace(
      EnumSet<Direction> faces,
      Matrix4f pose,
      VertexConsumer consumer,
      float x1,
      float x2,
      float y1,
      float y2,
      float z1,
      float z2,
      float z3,
      float z4,
      Direction direction
   ) {
      if (faces.contains(direction)) {
         consumer.addVertex(pose, x1, y1, z1);
         consumer.addVertex(pose, x2, y1, z2);
         consumer.addVertex(pose, x2, y2, z3);
         consumer.addVertex(pose, x1, y2, z4);
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
