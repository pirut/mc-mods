package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Locale;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PathfindingRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Map<Integer, Path> pathMap = Maps.newHashMap();
   private final Map<Integer, Float> pathMaxDist = Maps.newHashMap();
   private final Map<Integer, Long> creationMap = Maps.newHashMap();
   private static final long TIMEOUT = 5000L;
   private static final float MAX_RENDER_DIST = 80.0F;
   private static final boolean SHOW_OPEN_CLOSED = true;
   private static final boolean SHOW_OPEN_CLOSED_COST_MALUS = false;
   private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_TEXT = false;
   private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_BOX = true;
   private static final boolean SHOW_GROUND_LABELS = true;
   private static final float TEXT_SCALE = 0.02F;

   public void addPath(int p_113612_, Path p_113613_, float p_113614_) {
      this.pathMap.put(p_113612_, p_113613_);
      this.creationMap.put(p_113612_, Util.getMillis());
      this.pathMaxDist.put(p_113612_, p_113614_);
   }

   @Override
   public void render(PoseStack p_113629_, MultiBufferSource p_113630_, double p_113631_, double p_113632_, double p_113633_) {
      if (!this.pathMap.isEmpty()) {
         long $$5 = Util.getMillis();

         for (Integer $$6 : this.pathMap.keySet()) {
            Path $$7 = this.pathMap.get($$6);
            float $$8 = this.pathMaxDist.get($$6);
            renderPath(p_113629_, p_113630_, $$7, $$8, true, true, p_113631_, p_113632_, p_113633_);
         }

         for (Integer $$9 : this.creationMap.keySet().toArray(new Integer[0])) {
            if ($$5 - this.creationMap.get($$9) > 5000L) {
               this.pathMap.remove($$9);
               this.creationMap.remove($$9);
            }
         }
      }
   }

   public static void renderPath(
      PoseStack p_270399_,
      MultiBufferSource p_270359_,
      Path p_270189_,
      float p_270841_,
      boolean p_270481_,
      boolean p_270748_,
      double p_270187_,
      double p_270252_,
      double p_270371_
   ) {
      renderPathLine(p_270399_, p_270359_.getBuffer(RenderType.debugLineStrip(6.0)), p_270189_, p_270187_, p_270252_, p_270371_);
      BlockPos $$9 = p_270189_.getTarget();
      if (distanceToCamera($$9, p_270187_, p_270252_, p_270371_) <= 80.0F) {
         DebugRenderer.renderFilledBox(
            p_270399_,
            p_270359_,
            new AABB($$9.getX() + 0.25F, $$9.getY() + 0.25F, $$9.getZ() + 0.25, $$9.getX() + 0.75F, $$9.getY() + 0.75F, $$9.getZ() + 0.75F)
               .move(-p_270187_, -p_270252_, -p_270371_),
            0.0F,
            1.0F,
            0.0F,
            0.5F
         );

         for (int $$10 = 0; $$10 < p_270189_.getNodeCount(); $$10++) {
            Node $$11 = p_270189_.getNode($$10);
            if (distanceToCamera($$11.asBlockPos(), p_270187_, p_270252_, p_270371_) <= 80.0F) {
               float $$12 = $$10 == p_270189_.getNextNodeIndex() ? 1.0F : 0.0F;
               float $$13 = $$10 == p_270189_.getNextNodeIndex() ? 0.0F : 1.0F;
               DebugRenderer.renderFilledBox(
                  p_270399_,
                  p_270359_,
                  new AABB(
                        $$11.x + 0.5F - p_270841_,
                        $$11.y + 0.01F * $$10,
                        $$11.z + 0.5F - p_270841_,
                        $$11.x + 0.5F + p_270841_,
                        $$11.y + 0.25F + 0.01F * $$10,
                        $$11.z + 0.5F + p_270841_
                     )
                     .move(-p_270187_, -p_270252_, -p_270371_),
                  $$12,
                  0.0F,
                  $$13,
                  0.5F
               );
            }
         }
      }

      Path.DebugData $$14 = p_270189_.debugData();
      if (p_270481_ && $$14 != null) {
         for (Node $$15 : $$14.closedSet()) {
            if (distanceToCamera($$15.asBlockPos(), p_270187_, p_270252_, p_270371_) <= 80.0F) {
               DebugRenderer.renderFilledBox(
                  p_270399_,
                  p_270359_,
                  new AABB(
                        $$15.x + 0.5F - p_270841_ / 2.0F,
                        $$15.y + 0.01F,
                        $$15.z + 0.5F - p_270841_ / 2.0F,
                        $$15.x + 0.5F + p_270841_ / 2.0F,
                        $$15.y + 0.1,
                        $$15.z + 0.5F + p_270841_ / 2.0F
                     )
                     .move(-p_270187_, -p_270252_, -p_270371_),
                  1.0F,
                  0.8F,
                  0.8F,
                  0.5F
               );
            }
         }

         for (Node $$16 : $$14.openSet()) {
            if (distanceToCamera($$16.asBlockPos(), p_270187_, p_270252_, p_270371_) <= 80.0F) {
               DebugRenderer.renderFilledBox(
                  p_270399_,
                  p_270359_,
                  new AABB(
                        $$16.x + 0.5F - p_270841_ / 2.0F,
                        $$16.y + 0.01F,
                        $$16.z + 0.5F - p_270841_ / 2.0F,
                        $$16.x + 0.5F + p_270841_ / 2.0F,
                        $$16.y + 0.1,
                        $$16.z + 0.5F + p_270841_ / 2.0F
                     )
                     .move(-p_270187_, -p_270252_, -p_270371_),
                  0.8F,
                  1.0F,
                  1.0F,
                  0.5F
               );
            }
         }
      }

      if (p_270748_) {
         for (int $$17 = 0; $$17 < p_270189_.getNodeCount(); $$17++) {
            Node $$18 = p_270189_.getNode($$17);
            if (distanceToCamera($$18.asBlockPos(), p_270187_, p_270252_, p_270371_) <= 80.0F) {
               DebugRenderer.renderFloatingText(
                  p_270399_, p_270359_, String.valueOf($$18.type), $$18.x + 0.5, $$18.y + 0.75, $$18.z + 0.5, -1, 0.02F, true, 0.0F, true
               );
               DebugRenderer.renderFloatingText(
                  p_270399_,
                  p_270359_,
                  String.format(Locale.ROOT, "%.2f", $$18.costMalus),
                  $$18.x + 0.5,
                  $$18.y + 0.25,
                  $$18.z + 0.5,
                  -1,
                  0.02F,
                  true,
                  0.0F,
                  true
               );
            }
         }
      }
   }

   public static void renderPathLine(PoseStack p_270666_, VertexConsumer p_270602_, Path p_270511_, double p_270524_, double p_270163_, double p_270176_) {
      for (int $$6 = 0; $$6 < p_270511_.getNodeCount(); $$6++) {
         Node $$7 = p_270511_.getNode($$6);
         if (!(distanceToCamera($$7.asBlockPos(), p_270524_, p_270163_, p_270176_) > 80.0F)) {
            float $$8 = (float)$$6 / p_270511_.getNodeCount() * 0.33F;
            int $$9 = $$6 == 0 ? 0 : Mth.hsvToRgb($$8, 0.9F, 0.9F);
            int $$10 = $$9 >> 16 & 0xFF;
            int $$11 = $$9 >> 8 & 0xFF;
            int $$12 = $$9 & 0xFF;
            p_270602_.addVertex(p_270666_.last(), (float)($$7.x - p_270524_ + 0.5), (float)($$7.y - p_270163_ + 0.5), (float)($$7.z - p_270176_ + 0.5))
               .setColor($$10, $$11, $$12, 255);
         }
      }
   }

   private static float distanceToCamera(BlockPos p_113635_, double p_113636_, double p_113637_, double p_113638_) {
      return (float)(Math.abs(p_113635_.getX() - p_113636_) + Math.abs(p_113635_.getY() - p_113637_) + Math.abs(p_113635_.getZ() - p_113638_));
   }
}
