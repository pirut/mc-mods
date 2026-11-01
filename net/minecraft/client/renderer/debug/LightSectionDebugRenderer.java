package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.time.Duration;
import java.time.Instant;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class LightSectionDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final Duration REFRESH_INTERVAL = Duration.ofMillis(500L);
   private static final int RADIUS = 10;
   private static final Vector4f LIGHT_AND_BLOCKS_COLOR = new Vector4f(1.0F, 1.0F, 0.0F, 0.25F);
   private static final Vector4f LIGHT_ONLY_COLOR = new Vector4f(0.25F, 0.125F, 0.0F, 0.125F);
   private final Minecraft minecraft;
   private final LightLayer lightLayer;
   private Instant lastUpdateTime = Instant.now();
   @Nullable
   private LightSectionDebugRenderer.SectionData data;

   public LightSectionDebugRenderer(Minecraft p_283340_, LightLayer p_283096_) {
      this.minecraft = p_283340_;
      this.lightLayer = p_283096_;
   }

   @Override
   public void render(PoseStack p_281418_, MultiBufferSource p_282487_, double p_282164_, double p_282565_, double p_281615_) {
      Instant $$5 = Instant.now();
      if (this.data == null || Duration.between(this.lastUpdateTime, $$5).compareTo(REFRESH_INTERVAL) > 0) {
         this.lastUpdateTime = $$5;
         this.data = new LightSectionDebugRenderer.SectionData(
            this.minecraft.level.getLightEngine(), SectionPos.of(this.minecraft.player.blockPosition()), 10, this.lightLayer
         );
      }

      renderEdges(p_281418_, this.data.lightAndBlocksShape, this.data.minPos, p_282487_, p_282164_, p_282565_, p_281615_, LIGHT_AND_BLOCKS_COLOR);
      renderEdges(p_281418_, this.data.lightShape, this.data.minPos, p_282487_, p_282164_, p_282565_, p_281615_, LIGHT_ONLY_COLOR);
      VertexConsumer $$6 = p_282487_.getBuffer(RenderType.debugSectionQuads());
      renderFaces(p_281418_, this.data.lightAndBlocksShape, this.data.minPos, $$6, p_282164_, p_282565_, p_281615_, LIGHT_AND_BLOCKS_COLOR);
      renderFaces(p_281418_, this.data.lightShape, this.data.minPos, $$6, p_282164_, p_282565_, p_281615_, LIGHT_ONLY_COLOR);
   }

   private static void renderFaces(
      PoseStack p_283088_,
      DiscreteVoxelShape p_281747_,
      SectionPos p_282941_,
      VertexConsumer p_283103_,
      double p_281419_,
      double p_282520_,
      double p_281976_,
      Vector4f p_282342_
   ) {
      p_281747_.forAllFaces((p_282087_, p_283360_, p_282854_, p_282233_) -> {
         int $$11 = p_283360_ + p_282941_.getX();
         int $$12 = p_282854_ + p_282941_.getY();
         int $$13 = p_282233_ + p_282941_.getZ();
         renderFace(p_283088_, p_283103_, p_282087_, p_281419_, p_282520_, p_281976_, $$11, $$12, $$13, p_282342_);
      });
   }

   private static void renderEdges(
      PoseStack p_282890_,
      DiscreteVoxelShape p_282950_,
      SectionPos p_281925_,
      MultiBufferSource p_281516_,
      double p_281554_,
      double p_283233_,
      double p_281690_,
      Vector4f p_282916_
   ) {
      p_282950_.forAllEdges((p_283441_, p_283631_, p_282083_, p_281900_, p_281481_, p_283547_) -> {
         int $$13 = p_283441_ + p_281925_.getX();
         int $$14 = p_283631_ + p_281925_.getY();
         int $$15 = p_282083_ + p_281925_.getZ();
         int $$16 = p_281900_ + p_281925_.getX();
         int $$17 = p_281481_ + p_281925_.getY();
         int $$18 = p_283547_ + p_281925_.getZ();
         VertexConsumer $$19 = p_281516_.getBuffer(RenderType.debugLineStrip(1.0));
         renderEdge(p_282890_, $$19, p_281554_, p_283233_, p_281690_, $$13, $$14, $$15, $$16, $$17, $$18, p_282916_);
      }, true);
   }

   private static void renderFace(
      PoseStack p_283612_,
      VertexConsumer p_281996_,
      Direction p_282340_,
      double p_281988_,
      double p_282440_,
      double p_282235_,
      int p_282751_,
      int p_282270_,
      int p_282159_,
      Vector4f p_283316_
   ) {
      float $$10 = (float)(SectionPos.sectionToBlockCoord(p_282751_) - p_281988_);
      float $$11 = (float)(SectionPos.sectionToBlockCoord(p_282270_) - p_282440_);
      float $$12 = (float)(SectionPos.sectionToBlockCoord(p_282159_) - p_282235_);
      LevelRenderer.renderFace(
         p_283612_,
         p_281996_,
         p_282340_,
         $$10,
         $$11,
         $$12,
         $$10 + 16.0F,
         $$11 + 16.0F,
         $$12 + 16.0F,
         p_283316_.x(),
         p_283316_.y(),
         p_283316_.z(),
         p_283316_.w()
      );
   }

   private static void renderEdge(
      PoseStack p_283045_,
      VertexConsumer p_282888_,
      double p_283424_,
      double p_283677_,
      double p_283390_,
      int p_281439_,
      int p_282106_,
      int p_282462_,
      int p_282216_,
      int p_281474_,
      int p_281542_,
      Vector4f p_283667_
   ) {
      float $$12 = (float)(SectionPos.sectionToBlockCoord(p_281439_) - p_283424_);
      float $$13 = (float)(SectionPos.sectionToBlockCoord(p_282106_) - p_283677_);
      float $$14 = (float)(SectionPos.sectionToBlockCoord(p_282462_) - p_283390_);
      float $$15 = (float)(SectionPos.sectionToBlockCoord(p_282216_) - p_283424_);
      float $$16 = (float)(SectionPos.sectionToBlockCoord(p_281474_) - p_283677_);
      float $$17 = (float)(SectionPos.sectionToBlockCoord(p_281542_) - p_283390_);
      Matrix4f $$18 = p_283045_.last().pose();
      p_282888_.addVertex($$18, $$12, $$13, $$14).setColor(p_283667_.x(), p_283667_.y(), p_283667_.z(), 1.0F);
      p_282888_.addVertex($$18, $$15, $$16, $$17).setColor(p_283667_.x(), p_283667_.y(), p_283667_.z(), 1.0F);
   }

   @OnlyIn(Dist.CLIENT)
   static final class SectionData {
      final DiscreteVoxelShape lightAndBlocksShape;
      final DiscreteVoxelShape lightShape;
      final SectionPos minPos;

      SectionData(LevelLightEngine p_283220_, SectionPos p_282370_, int p_282804_, LightLayer p_283151_) {
         int $$4 = p_282804_ * 2 + 1;
         this.lightAndBlocksShape = new BitSetDiscreteVoxelShape($$4, $$4, $$4);
         this.lightShape = new BitSetDiscreteVoxelShape($$4, $$4, $$4);

         for (int $$5 = 0; $$5 < $$4; $$5++) {
            for (int $$6 = 0; $$6 < $$4; $$6++) {
               for (int $$7 = 0; $$7 < $$4; $$7++) {
                  SectionPos $$8 = SectionPos.of(p_282370_.x() + $$7 - p_282804_, p_282370_.y() + $$6 - p_282804_, p_282370_.z() + $$5 - p_282804_);
                  LayerLightSectionStorage.SectionType $$9 = p_283220_.getDebugSectionType(p_283151_, $$8);
                  if ($$9 == LayerLightSectionStorage.SectionType.LIGHT_AND_DATA) {
                     this.lightAndBlocksShape.fill($$7, $$6, $$5);
                     this.lightShape.fill($$7, $$6, $$5);
                  } else if ($$9 == LayerLightSectionStorage.SectionType.LIGHT_ONLY) {
                     this.lightShape.fill($$7, $$6, $$5);
                  }
               }
            }
         }

         this.minPos = SectionPos.of(p_282370_.x() - p_282804_, p_282370_.y() - p_282804_, p_282370_.z() - p_282804_);
      }
   }
}
