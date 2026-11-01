package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class ChunkBorderRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private static final int CELL_BORDER = FastColor.ARGB32.color(255, 0, 155, 155);
   private static final int YELLOW = FastColor.ARGB32.color(255, 255, 255, 0);

   public ChunkBorderRenderer(Minecraft p_113356_) {
      this.minecraft = p_113356_;
   }

   @Override
   public void render(PoseStack p_113358_, MultiBufferSource p_113359_, double p_113360_, double p_113361_, double p_113362_) {
      Entity $$5 = this.minecraft.gameRenderer.getMainCamera().getEntity();
      float $$6 = (float)(this.minecraft.level.getMinBuildHeight() - p_113361_);
      float $$7 = (float)(this.minecraft.level.getMaxBuildHeight() - p_113361_);
      ChunkPos $$8 = $$5.chunkPosition();
      float $$9 = (float)($$8.getMinBlockX() - p_113360_);
      float $$10 = (float)($$8.getMinBlockZ() - p_113362_);
      VertexConsumer $$11 = p_113359_.getBuffer(RenderType.debugLineStrip(1.0));
      Matrix4f $$12 = p_113358_.last().pose();

      for (int $$13 = -16; $$13 <= 32; $$13 += 16) {
         for (int $$14 = -16; $$14 <= 32; $$14 += 16) {
            $$11.addVertex($$12, $$9 + $$13, $$6, $$10 + $$14).setColor(1.0F, 0.0F, 0.0F, 0.0F);
            $$11.addVertex($$12, $$9 + $$13, $$6, $$10 + $$14).setColor(1.0F, 0.0F, 0.0F, 0.5F);
            $$11.addVertex($$12, $$9 + $$13, $$7, $$10 + $$14).setColor(1.0F, 0.0F, 0.0F, 0.5F);
            $$11.addVertex($$12, $$9 + $$13, $$7, $$10 + $$14).setColor(1.0F, 0.0F, 0.0F, 0.0F);
         }
      }

      for (int $$15 = 2; $$15 < 16; $$15 += 2) {
         int $$16 = $$15 % 4 == 0 ? CELL_BORDER : YELLOW;
         $$11.addVertex($$12, $$9 + $$15, $$6, $$10).setColor(1.0F, 1.0F, 0.0F, 0.0F);
         $$11.addVertex($$12, $$9 + $$15, $$6, $$10).setColor($$16);
         $$11.addVertex($$12, $$9 + $$15, $$7, $$10).setColor($$16);
         $$11.addVertex($$12, $$9 + $$15, $$7, $$10).setColor(1.0F, 1.0F, 0.0F, 0.0F);
         $$11.addVertex($$12, $$9 + $$15, $$6, $$10 + 16.0F).setColor(1.0F, 1.0F, 0.0F, 0.0F);
         $$11.addVertex($$12, $$9 + $$15, $$6, $$10 + 16.0F).setColor($$16);
         $$11.addVertex($$12, $$9 + $$15, $$7, $$10 + 16.0F).setColor($$16);
         $$11.addVertex($$12, $$9 + $$15, $$7, $$10 + 16.0F).setColor(1.0F, 1.0F, 0.0F, 0.0F);
      }

      for (int $$17 = 2; $$17 < 16; $$17 += 2) {
         int $$18 = $$17 % 4 == 0 ? CELL_BORDER : YELLOW;
         $$11.addVertex($$12, $$9, $$6, $$10 + $$17).setColor(1.0F, 1.0F, 0.0F, 0.0F);
         $$11.addVertex($$12, $$9, $$6, $$10 + $$17).setColor($$18);
         $$11.addVertex($$12, $$9, $$7, $$10 + $$17).setColor($$18);
         $$11.addVertex($$12, $$9, $$7, $$10 + $$17).setColor(1.0F, 1.0F, 0.0F, 0.0F);
         $$11.addVertex($$12, $$9 + 16.0F, $$6, $$10 + $$17).setColor(1.0F, 1.0F, 0.0F, 0.0F);
         $$11.addVertex($$12, $$9 + 16.0F, $$6, $$10 + $$17).setColor($$18);
         $$11.addVertex($$12, $$9 + 16.0F, $$7, $$10 + $$17).setColor($$18);
         $$11.addVertex($$12, $$9 + 16.0F, $$7, $$10 + $$17).setColor(1.0F, 1.0F, 0.0F, 0.0F);
      }

      for (int $$19 = this.minecraft.level.getMinBuildHeight(); $$19 <= this.minecraft.level.getMaxBuildHeight(); $$19 += 2) {
         float $$20 = (float)($$19 - p_113361_);
         int $$21 = $$19 % 8 == 0 ? CELL_BORDER : YELLOW;
         $$11.addVertex($$12, $$9, $$20, $$10).setColor(1.0F, 1.0F, 0.0F, 0.0F);
         $$11.addVertex($$12, $$9, $$20, $$10).setColor($$21);
         $$11.addVertex($$12, $$9, $$20, $$10 + 16.0F).setColor($$21);
         $$11.addVertex($$12, $$9 + 16.0F, $$20, $$10 + 16.0F).setColor($$21);
         $$11.addVertex($$12, $$9 + 16.0F, $$20, $$10).setColor($$21);
         $$11.addVertex($$12, $$9, $$20, $$10).setColor($$21);
         $$11.addVertex($$12, $$9, $$20, $$10).setColor(1.0F, 1.0F, 0.0F, 0.0F);
      }

      $$11 = p_113359_.getBuffer(RenderType.debugLineStrip(2.0));

      for (int $$22 = 0; $$22 <= 16; $$22 += 16) {
         for (int $$23 = 0; $$23 <= 16; $$23 += 16) {
            $$11.addVertex($$12, $$9 + $$22, $$6, $$10 + $$23).setColor(0.25F, 0.25F, 1.0F, 0.0F);
            $$11.addVertex($$12, $$9 + $$22, $$6, $$10 + $$23).setColor(0.25F, 0.25F, 1.0F, 1.0F);
            $$11.addVertex($$12, $$9 + $$22, $$7, $$10 + $$23).setColor(0.25F, 0.25F, 1.0F, 1.0F);
            $$11.addVertex($$12, $$9 + $$22, $$7, $$10 + $$23).setColor(0.25F, 0.25F, 1.0F, 0.0F);
         }
      }

      for (int $$24 = this.minecraft.level.getMinBuildHeight(); $$24 <= this.minecraft.level.getMaxBuildHeight(); $$24 += 16) {
         float $$25 = (float)($$24 - p_113361_);
         $$11.addVertex($$12, $$9, $$25, $$10).setColor(0.25F, 0.25F, 1.0F, 0.0F);
         $$11.addVertex($$12, $$9, $$25, $$10).setColor(0.25F, 0.25F, 1.0F, 1.0F);
         $$11.addVertex($$12, $$9, $$25, $$10 + 16.0F).setColor(0.25F, 0.25F, 1.0F, 1.0F);
         $$11.addVertex($$12, $$9 + 16.0F, $$25, $$10 + 16.0F).setColor(0.25F, 0.25F, 1.0F, 1.0F);
         $$11.addVertex($$12, $$9 + 16.0F, $$25, $$10).setColor(0.25F, 0.25F, 1.0F, 1.0F);
         $$11.addVertex($$12, $$9, $$25, $$10).setColor(0.25F, 0.25F, 1.0F, 1.0F);
         $$11.addVertex($$12, $$9, $$25, $$10).setColor(0.25F, 0.25F, 1.0F, 0.0F);
      }
   }
}
