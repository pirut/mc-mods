package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class ShapeRenderer {
   public static void renderShape(PoseStack poseStack, VertexConsumer consumer, VoxelShape shape, double dx, double dy, double dz, int color, float lineWidth) {
      PoseStack.Pose pose = poseStack.last();
      shape.forAllEdges((h, j, k, l, m, n) -> {
         Vector3f vector3f = new Vector3f((float)(l - h), (float)(m - j), (float)(n - k)).normalize();
         consumer.addVertex(pose, (float)(h + dx), (float)(j + dy), (float)(k + dz)).setColor(color).setNormal(pose, vector3f).setLineWidth(lineWidth);
         consumer.addVertex(pose, (float)(l + dx), (float)(m + dy), (float)(n + dz)).setColor(color).setNormal(pose, vector3f).setLineWidth(lineWidth);
      });
   }
}
