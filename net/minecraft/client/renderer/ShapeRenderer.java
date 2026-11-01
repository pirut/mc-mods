package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ShapeRenderer {
   public static void renderShape(
      PoseStack p_361053_, VertexConsumer p_364779_, VoxelShape p_362125_, double p_360355_, double p_363490_, double p_360995_, int p_380036_, float p_454676_
   ) {
      PoseStack.Pose posestack$pose = p_361053_.last();
      p_362125_.forAllEdges(
         (p_454255_, p_454256_, p_454257_, p_454258_, p_454259_, p_454260_) -> {
            Vector3f vector3f = new Vector3f((float)(p_454258_ - p_454255_), (float)(p_454259_ - p_454256_), (float)(p_454260_ - p_454257_)).normalize();
            p_364779_.addVertex(posestack$pose, (float)(p_454255_ + p_360355_), (float)(p_454256_ + p_363490_), (float)(p_454257_ + p_360995_))
               .setColor(p_380036_)
               .setNormal(posestack$pose, vector3f)
               .setLineWidth(p_454676_);
            p_364779_.addVertex(posestack$pose, (float)(p_454258_ + p_360355_), (float)(p_454259_ + p_363490_), (float)(p_454260_ + p_360995_))
               .setColor(p_380036_)
               .setNormal(posestack$pose, vector3f)
               .setLineWidth(p_454676_);
         }
      );
   }
}
