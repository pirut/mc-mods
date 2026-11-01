package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WorldGenAttemptRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final List<BlockPos> toRender = Lists.newArrayList();
   private final List<Float> scales = Lists.newArrayList();
   private final List<Float> alphas = Lists.newArrayList();
   private final List<Float> reds = Lists.newArrayList();
   private final List<Float> greens = Lists.newArrayList();
   private final List<Float> blues = Lists.newArrayList();

   public void addPos(BlockPos p_113738_, float p_113739_, float p_113740_, float p_113741_, float p_113742_, float p_113743_) {
      this.toRender.add(p_113738_);
      this.scales.add(p_113739_);
      this.alphas.add(p_113743_);
      this.reds.add(p_113740_);
      this.greens.add(p_113741_);
      this.blues.add(p_113742_);
   }

   @Override
   public void render(PoseStack p_113732_, MultiBufferSource p_113733_, double p_113734_, double p_113735_, double p_113736_) {
      VertexConsumer $$5 = p_113733_.getBuffer(RenderType.debugFilledBox());

      for (int $$6 = 0; $$6 < this.toRender.size(); $$6++) {
         BlockPos $$7 = this.toRender.get($$6);
         Float $$8 = this.scales.get($$6);
         float $$9 = $$8 / 2.0F;
         LevelRenderer.addChainedFilledBoxVertices(
            p_113732_,
            $$5,
            $$7.getX() + 0.5F - $$9 - p_113734_,
            $$7.getY() + 0.5F - $$9 - p_113735_,
            $$7.getZ() + 0.5F - $$9 - p_113736_,
            $$7.getX() + 0.5F + $$9 - p_113734_,
            $$7.getY() + 0.5F + $$9 - p_113735_,
            $$7.getZ() + 0.5F + $$9 - p_113736_,
            this.reds.get($$6),
            this.greens.get($$6),
            this.blues.get($$6),
            this.alphas.get($$6)
         );
      }
   }
}
