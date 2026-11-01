package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.StructuresDebugPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StructureRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private final Map<ResourceKey<Level>, Map<String, BoundingBox>> postMainBoxes = Maps.newIdentityHashMap();
   private final Map<ResourceKey<Level>, Map<String, StructuresDebugPayload.PieceInfo>> postPieces = Maps.newIdentityHashMap();
   private static final int MAX_RENDER_DIST = 500;

   public StructureRenderer(Minecraft p_113680_) {
      this.minecraft = p_113680_;
   }

   @Override
   public void render(PoseStack p_113688_, MultiBufferSource p_113689_, double p_113690_, double p_113691_, double p_113692_) {
      Camera $$5 = this.minecraft.gameRenderer.getMainCamera();
      ResourceKey<Level> $$6 = this.minecraft.level.dimension();
      BlockPos $$7 = BlockPos.containing($$5.getPosition().x, 0.0, $$5.getPosition().z);
      VertexConsumer $$8 = p_113689_.getBuffer(RenderType.lines());
      if (this.postMainBoxes.containsKey($$6)) {
         for (BoundingBox $$9 : this.postMainBoxes.get($$6).values()) {
            if ($$7.closerThan($$9.getCenter(), 500.0)) {
               LevelRenderer.renderLineBox(
                  p_113688_,
                  $$8,
                  $$9.minX() - p_113690_,
                  $$9.minY() - p_113691_,
                  $$9.minZ() - p_113692_,
                  $$9.maxX() + 1 - p_113690_,
                  $$9.maxY() + 1 - p_113691_,
                  $$9.maxZ() + 1 - p_113692_,
                  1.0F,
                  1.0F,
                  1.0F,
                  1.0F,
                  1.0F,
                  1.0F,
                  1.0F
               );
            }
         }
      }

      Map<String, StructuresDebugPayload.PieceInfo> $$10 = this.postPieces.get($$6);
      if ($$10 != null) {
         for (StructuresDebugPayload.PieceInfo $$11 : $$10.values()) {
            BoundingBox $$12 = $$11.boundingBox();
            if ($$7.closerThan($$12.getCenter(), 500.0)) {
               if ($$11.isStart()) {
                  LevelRenderer.renderLineBox(
                     p_113688_,
                     $$8,
                     $$12.minX() - p_113690_,
                     $$12.minY() - p_113691_,
                     $$12.minZ() - p_113692_,
                     $$12.maxX() + 1 - p_113690_,
                     $$12.maxY() + 1 - p_113691_,
                     $$12.maxZ() + 1 - p_113692_,
                     0.0F,
                     1.0F,
                     0.0F,
                     1.0F,
                     0.0F,
                     1.0F,
                     0.0F
                  );
               } else {
                  LevelRenderer.renderLineBox(
                     p_113688_,
                     $$8,
                     $$12.minX() - p_113690_,
                     $$12.minY() - p_113691_,
                     $$12.minZ() - p_113692_,
                     $$12.maxX() + 1 - p_113690_,
                     $$12.maxY() + 1 - p_113691_,
                     $$12.maxZ() + 1 - p_113692_,
                     0.0F,
                     0.0F,
                     1.0F,
                     1.0F,
                     0.0F,
                     0.0F,
                     1.0F
                  );
               }
            }
         }
      }
   }

   public void addBoundingBox(BoundingBox p_113683_, List<StructuresDebugPayload.PieceInfo> p_113684_, ResourceKey<Level> p_295656_) {
      this.postMainBoxes.computeIfAbsent(p_295656_, p_294379_ -> new HashMap<>()).put(p_113683_.toString(), p_113683_);
      Map<String, StructuresDebugPayload.PieceInfo> $$3 = this.postPieces.computeIfAbsent(p_295656_, p_294187_ -> new HashMap<>());

      for (StructuresDebugPayload.PieceInfo $$4 : p_113684_) {
         $$3.put($$4.boundingBox().toString(), $$4);
      }
   }

   @Override
   public void clear() {
      this.postMainBoxes.clear();
      this.postPieces.clear();
   }
}
