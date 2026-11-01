package net.minecraft.client.renderer.debug;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Collections;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CollisionBoxRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private double lastUpdateTime = Double.MIN_VALUE;
   private List<VoxelShape> shapes = Collections.emptyList();

   public CollisionBoxRenderer(Minecraft p_113404_) {
      this.minecraft = p_113404_;
   }

   @Override
   public void render(PoseStack p_113408_, MultiBufferSource p_113409_, double p_113410_, double p_113411_, double p_113412_) {
      double $$5 = Util.getNanos();
      if ($$5 - this.lastUpdateTime > 1.0E8) {
         this.lastUpdateTime = $$5;
         Entity $$6 = this.minecraft.gameRenderer.getMainCamera().getEntity();
         this.shapes = ImmutableList.copyOf($$6.level().getCollisions($$6, $$6.getBoundingBox().inflate(6.0)));
      }

      VertexConsumer $$7 = p_113409_.getBuffer(RenderType.lines());

      for (VoxelShape $$8 : this.shapes) {
         LevelRenderer.renderVoxelShape(p_113408_, $$7, $$8, -p_113410_, -p_113411_, -p_113412_, 1.0F, 1.0F, 1.0F, 1.0F, true);
      }
   }
}
