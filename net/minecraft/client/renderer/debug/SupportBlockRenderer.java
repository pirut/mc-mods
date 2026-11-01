package net.minecraft.client.renderer.debug;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleSupplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SupportBlockRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private double lastUpdateTime = Double.MIN_VALUE;
   private List<Entity> surroundEntities = Collections.emptyList();

   public SupportBlockRenderer(Minecraft p_286424_) {
      this.minecraft = p_286424_;
   }

   @Override
   public void render(PoseStack p_286297_, MultiBufferSource p_286436_, double p_286291_, double p_286388_, double p_286330_) {
      double $$5 = Util.getNanos();
      if ($$5 - this.lastUpdateTime > 1.0E8) {
         this.lastUpdateTime = $$5;
         Entity $$6 = this.minecraft.gameRenderer.getMainCamera().getEntity();
         this.surroundEntities = ImmutableList.copyOf($$6.level().getEntities($$6, $$6.getBoundingBox().inflate(16.0)));
      }

      Player $$7 = this.minecraft.player;
      if ($$7 != null && $$7.mainSupportingBlockPos.isPresent()) {
         this.drawHighlights(p_286297_, p_286436_, p_286291_, p_286388_, p_286330_, $$7, () -> 0.0, 1.0F, 0.0F, 0.0F);
      }

      for (Entity $$8 : this.surroundEntities) {
         if ($$8 != $$7) {
            this.drawHighlights(p_286297_, p_286436_, p_286291_, p_286388_, p_286330_, $$8, () -> this.getBias($$8), 0.0F, 1.0F, 0.0F);
         }
      }
   }

   private void drawHighlights(
      PoseStack p_286525_,
      MultiBufferSource p_286495_,
      double p_286696_,
      double p_286417_,
      double p_286386_,
      Entity p_286273_,
      DoubleSupplier p_286458_,
      float p_286487_,
      float p_286710_,
      float p_286793_
   ) {
      p_286273_.mainSupportingBlockPos.ifPresent(p_286428_ -> {
         double $$11 = p_286458_.getAsDouble();
         BlockPos $$12 = p_286273_.getOnPos();
         this.highlightPosition($$12, p_286525_, p_286696_, p_286417_, p_286386_, p_286495_, 0.02 + $$11, p_286487_, p_286710_, p_286793_);
         BlockPos $$13 = p_286273_.getOnPosLegacy();
         if (!$$13.equals($$12)) {
            this.highlightPosition($$13, p_286525_, p_286696_, p_286417_, p_286386_, p_286495_, 0.04 + $$11, 0.0F, 1.0F, 1.0F);
         }
      });
   }

   private double getBias(Entity p_286713_) {
      return 0.02 * (String.valueOf(p_286713_.getId() + 0.132453657).hashCode() % 1000) / 1000.0;
   }

   private void highlightPosition(
      BlockPos p_286268_,
      PoseStack p_286592_,
      double p_286463_,
      double p_286552_,
      double p_286660_,
      MultiBufferSource p_286314_,
      double p_286880_,
      float p_286918_,
      float p_286304_,
      float p_286672_
   ) {
      double $$10 = p_286268_.getX() - p_286463_ - 2.0 * p_286880_;
      double $$11 = p_286268_.getY() - p_286552_ - 2.0 * p_286880_;
      double $$12 = p_286268_.getZ() - p_286660_ - 2.0 * p_286880_;
      double $$13 = $$10 + 1.0 + 4.0 * p_286880_;
      double $$14 = $$11 + 1.0 + 4.0 * p_286880_;
      double $$15 = $$12 + 1.0 + 4.0 * p_286880_;
      LevelRenderer.renderLineBox(p_286592_, p_286314_.getBuffer(RenderType.lines()), $$10, $$11, $$12, $$13, $$14, $$15, p_286918_, p_286304_, p_286672_, 0.4F);
      LevelRenderer.renderVoxelShape(
         p_286592_,
         p_286314_.getBuffer(RenderType.lines()),
         this.minecraft
            .level
            .getBlockState(p_286268_)
            .getCollisionShape(this.minecraft.level, p_286268_, CollisionContext.empty())
            .move(p_286268_.getX(), p_286268_.getY(), p_286268_.getZ()),
         -p_286463_,
         -p_286552_,
         -p_286660_,
         p_286918_,
         p_286304_,
         p_286672_,
         1.0F,
         false
      );
   }
}
